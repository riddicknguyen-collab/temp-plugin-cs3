package com.yanhh3d

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * All YanHH3D HTML parsing. Pure Jsoup: no network, no CloudStream types, so every
 * rule here is covered by fixtures in `src/test/resources/yanhh3d`.
 *
 * Note the page split the site actually uses: a title's detail page carries metadata
 * and nothing else, and the episode list lives on the watch page its play buttons lead
 * to. [parseDetail] therefore reports a [YanDetail.watchUrl] and leaves episodes empty;
 * [parseEpisodes] runs against the watch page.
 *
 * Nothing force-unwraps a selector result; a missing field degrades to null or an
 * empty list rather than taking the provider down.
 */
class YanHH3DParser(
    private val domainResolver: YanHH3DDomainResolver,
) {
    private companion object {
        const val BASE64_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

        /** First line of every HLS playlist. */
        const val PLAYLIST_MARKER = "#EXTM3U"
    }

    /** Cards on the latest, category and search pages. */
    fun parseList(document: Document): List<YanMovieItem> =
        document.select(YanHH3DSelectors.MOVIE_ITEM).mapNotNull { item ->
            val anchor = item.selectFirst(YanHH3DSelectors.MOVIE_ITEM_LINK) ?: return@mapNotNull null

            val href = anchor.attr("href").trim()
            if (href.isEmpty()) return@mapNotNull null

            // The card anchor wraps only a play icon, so its text is usually empty and
            // the title attribute is what carries the name.
            val title = anchor.attr("title").trim()
                .ifEmpty { item.textOrNull(YanHH3DSelectors.MOVIE_ITEM_TITLE).orEmpty() }
                .ifEmpty { anchor.text().trim() }
            if (title.isEmpty()) return@mapNotNull null

            YanMovieItem(
                title = title,
                url = domainResolver.absoluteUrl(href),
                posterUrl = item.selectFirst(YanHH3DSelectors.MOVIE_ITEM_POSTER)
                    ?.let(::posterAttribute)
                    ?.let(domainResolver::absoluteUrl),
                currentEpisode = item.textOrNull(YanHH3DSelectors.MOVIE_ITEM_CURRENT_EPISODE),
                qualityLabel = item.textOrNull(YanHH3DSelectors.MOVIE_ITEM_QUALITY),
            )
        }

    /**
     * Title metadata. [inputUrl] is the URL we were asked to load and is only used when
     * the page carries neither a canonical link nor `og:url`.
     *
     * [YanDetail.episodes] is left empty on purpose: the caller has to fetch
     * [YanDetail.watchUrl] and run [parseEpisodes] on it.
     */
    fun parseDetail(document: Document, inputUrl: String): YanDetail {
        val canonical = document.attrOrNull(YanHH3DSelectors.CANONICAL, "href")
            ?: document.attrOrNull(YanHH3DSelectors.OG_URL, "content")

        return YanDetail(
            title = document.attrOrNull(YanHH3DSelectors.OG_TITLE, "content")
                ?: document.selectFirst("h1")?.text()?.trim()
                ?: document.title().trim(),
            url = domainResolver.absoluteUrl(canonical ?: inputUrl),
            posterUrl = document.attrOrNull(YanHH3DSelectors.OG_IMAGE, "content")
                ?.let(domainResolver::absoluteUrl),
            description = document.attrOrNull(YanHH3DSelectors.OG_DESCRIPTION, "content"),
            year = infoValue(document, YanHH3DLabels.YEAR)
                ?.let { YanHH3DPatterns.YEAR.find(it)?.value?.toIntOrNull() },
            status = infoValue(document, YanHH3DLabels.STATUS),
            genres = infoItem(document, YanHH3DLabels.GENRES)
                ?.select("a")
                ?.map { it.text().trim() }
                ?.filter(String::isNotEmpty)
                .orEmpty(),
            watchUrl = parseWatchUrl(document),
        )
    }

    /**
     * The play button for the preferred server. The detail page offers one button per
     * server; only [YanHH3DLabels.PREFERRED_SERVER] is wanted, and the first button is
     * the fallback if the site ever renames them.
     */
    fun parseWatchUrl(document: Document): String? {
        val buttons = document.select(YanHH3DSelectors.PLAY_BUTTON)
        val preferred = buttons.firstOrNull {
            it.text().contains(YanHH3DLabels.PREFERRED_SERVER, ignoreCase = true)
        } ?: buttons.firstOrNull()

        return preferred?.attr("href")?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(domainResolver::normalizeInternalData)
    }

    /**
     * Episodes from a watch page, taking only the preferred server's tab pane so the
     * list is not doubled by the other server. Deduplicated by URL and sorted by
     * episode number; entries whose number cannot be read keep their page order at the
     * end.
     *
     * URLs are stored path-only, so CloudStream watch history survives a domain change.
     */
    fun parseEpisodes(document: Document): List<YanEpisode> {
        val container = document.selectFirst(YanHH3DSelectors.EPISODE_CONTAINER) ?: return emptyList()

        val tabs = container.select(YanHH3DSelectors.EPISODE_TAB)
        val preferred = tabs.filter {
            it.text().contains(YanHH3DLabels.PREFERRED_SERVER, ignoreCase = true)
        }
        val paneIds = preferred.ifEmpty { tabs }
            .map { it.attr("href").trim().removePrefix("#") }
            .filter(String::isNotEmpty)

        // Without tabs the container itself holds the episode anchors.
        val anchors = if (paneIds.isEmpty()) {
            container.select(YanHH3DSelectors.EPISODE_LINK)
        } else {
            paneIds.flatMap { id -> container.select("#$id ${YanHH3DSelectors.EPISODE_LINK}") }
        }

        return anchors.mapNotNull { anchor ->
            val href = anchor.attr("href").trim()
            if (href.isEmpty() || href.startsWith("#")) return@mapNotNull null

            val label = anchor.textOrNull(YanHH3DSelectors.EPISODE_ORDER)
                ?: anchor.attr("title").trim().takeIf(String::isNotEmpty)
                ?: anchor.text().trim()
            if (label.isEmpty()) return@mapNotNull null

            YanEpisode(
                // The site labels episodes with the bare number.
                name = if (label.all(Char::isDigit)) "Tập $label" else label,
                url = domainResolver.normalizeInternalData(href),
                episodeNumber = parseEpisodeNumber(label, href),
            )
        }
            .distinctBy { it.url }
            .sortedWith(compareBy(nullsLast()) { it.episodeNumber })
    }

    /**
     * Playable entries on a watch page. Every `data-src` is returned, including ones
     * classified [YanSourceType.UNKNOWN], so a maintainer can see what the page offered
     * instead of wondering why a server vanished.
     */
    fun parseSources(document: Document): List<YanSource> =
        document.select(YanHH3DSelectors.SERVER_LINK).mapNotNull { element ->
            val raw = element.attr("data-src").trim()
            if (raw.isEmpty()) return@mapNotNull null

            val url = domainResolver.absoluteUrl(raw)
            val name = element.text().trim().ifEmpty { YanHH3DConstants.PROVIDER_NAME }
            val type = classify(url)

            YanSource(
                name = name,
                url = url,
                type = type,
                quality = if (type == YanSourceType.HLS) {
                    parseQuality(name, url)
                } else {
                    YanHH3DQualities.UNKNOWN
                },
            )
        }.distinctBy { it.url }

    /** Quality from the server label first, then the URL, per [YanHH3DQualities]. */
    fun parseQuality(name: String, url: String): Int {
        val text = "$name $url"
        return when {
            text.contains("4k", ignoreCase = true) || text.contains("2160") -> YanHH3DQualities.P2160
            text.contains("1080") -> YanHH3DQualities.P1080
            text.contains("720") -> YanHH3DQualities.P720
            text.contains("480") -> YanHH3DQualities.P480
            else -> YanHH3DQualities.UNKNOWN
        }
    }

    /**
     * Works out what a source's `data-src` actually serves and returns a playable
     * playlist URL, or null if it is neither shape.
     *
     * A `data-src` ends in `.m3u8` but usually serves an HTML player page whose config
     * is base64 JSON in `data-obf`; the playable manifest is that config's `pU` entry,
     * and its token is issued per request, so this has to run at playback time. Some
     * servers may hand back the manifest directly, so [body] is checked for that first
     * rather than assuming every source is a player page.
     */
    fun parsePlaylist(body: String, sourceUrl: String): String? {
        if (body.trimStart().startsWith(PLAYLIST_MARKER)) return sourceUrl

        val document = org.jsoup.Jsoup.parse(body)
        val blob = (
            document.selectFirst(YanHH3DSelectors.PLAYER_CONFIG)
                ?: document.selectFirst("[${YanHH3DSelectors.PLAYER_CONFIG_ATTRIBUTE}]")
            )
            ?.attr(YanHH3DSelectors.PLAYER_CONFIG_ATTRIBUTE)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return null

        val config = decodeBase64(blob) ?: return null

        return YanHH3DPatterns.PLAYER_PLAIN_URL.find(config)
            ?.groupValues?.get(1)
            // The blob is JSON, so its slashes arrive escaped.
            ?.replace("\\/", "/")
            ?.takeIf(String::isNotEmpty)
    }

    /**
     * Decoded by hand rather than with `java.util.Base64`, which needs API 26 while
     * this module targets 21, or `android.util.Base64`, which would put an Android
     * class in the parser and break the JVM tests. Accepts the URL-safe alphabet too.
     */
    private fun decodeBase64(input: String): String? {
        val bytes = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0

        for (character in input) {
            if (character == '=' || character.isWhitespace()) continue

            val value = BASE64_ALPHABET.indexOf(
                when (character) {
                    '-' -> '+'
                    '_' -> '/'
                    else -> character
                },
            )
            if (value < 0) return null

            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                bytes.write((buffer shr bits) and 0xFF)
            }
        }

        return String(bytes.toByteArray(), Charsets.UTF_8).takeIf(String::isNotEmpty)
    }

    private fun classify(url: String): YanSourceType = when {
        url.contains(".m3u8", ignoreCase = true) -> YanSourceType.HLS
        YanHH3DPatterns.EMBED_HINTS.any { url.contains(it, ignoreCase = true) } -> YanSourceType.EMBED
        else -> YanSourceType.UNKNOWN
    }

    private fun parseEpisodeNumber(name: String, url: String): Int? =
        YanHH3DPatterns.EPISODE_NUMBER.find(name)?.groupValues?.get(1)?.toIntOrNull()
            ?: YanHH3DPatterns.EPISODE_NUMBER.find(url.substringAfterLast('/'))
                ?.groupValues?.get(1)?.toIntOrNull()

    /**
     * The metadata row whose label matches, scoped to the info block. The sidebar menu
     * repeats words like "Thể loại", so searching the whole document would pick up the
     * site-wide genre menu instead of this title's genres.
     */
    private fun infoItem(document: Document, label: String): Element? =
        document.selectFirst(YanHH3DSelectors.DETAIL_INFO)
            ?.select(YanHH3DSelectors.DETAIL_INFO_ITEM)
            ?.firstOrNull { item ->
                item.textOrNull(YanHH3DSelectors.DETAIL_INFO_LABEL)
                    ?.startsWith(label, ignoreCase = true) == true
            }

    private fun infoValue(document: Document, label: String): String? =
        infoItem(document, label)?.textOrNull(YanHH3DSelectors.DETAIL_INFO_VALUE)

    /** First non-blank lazy-load or plain image attribute, in [YanHH3DSelectors.POSTER_ATTRIBUTES] order. */
    private fun posterAttribute(image: Element): String? =
        YanHH3DSelectors.POSTER_ATTRIBUTES.firstNotNullOfOrNull { attribute ->
            image.attr(attribute).trim().takeIf(String::isNotEmpty)
        }

    private fun Element.textOrNull(selector: String): String? =
        selectFirst(selector)?.text()?.trim()?.takeIf(String::isNotEmpty)

    private fun Document.attrOrNull(selector: String, attribute: String): String? =
        selectFirst(selector)?.attr(attribute)?.trim()?.takeIf(String::isNotEmpty)
}
