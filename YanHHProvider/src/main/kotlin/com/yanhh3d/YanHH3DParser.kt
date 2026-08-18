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
                // Only playlists get rewritten; embeds must reach their host untouched.
                url = if (type == YanSourceType.HLS) normalizeHlsUrl(url) else url,
                type = type,
                quality = if (type == YanSourceType.HLS) {
                    // Read the quality off the advertised URL, which still has the path.
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
     * Moves a playlist URL onto the path the CDN actually serves it from. A URL that
     * does not match the expected shape is returned untouched rather than mangled.
     */
    private fun normalizeHlsUrl(url: String): String {
        val match = YanHH3DPatterns.HLS_PATH.find(url) ?: return url
        val (host, file, query) = match.destructured
        return "$host${YanHH3DPatterns.HLS_STREAM_PATH}$file$query"
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
