package com.yanhh3d

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * All YanHH3D HTML parsing. Pure Jsoup: no network, no CloudStream types, so every
 * rule here is covered by fixtures in `src/test/resources/yanhh3d`.
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

            val title = anchor.attr("title").trim().ifEmpty { anchor.text().trim() }
            if (title.isEmpty()) return@mapNotNull null

            YanMovieItem(
                title = title,
                url = domainResolver.absoluteUrl(href),
                posterUrl = item.selectFirst(YanHH3DSelectors.MOVIE_ITEM_POSTER)
                    ?.attr("src")
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let(domainResolver::absoluteUrl),
                currentEpisode = item.textOrNull(YanHH3DSelectors.MOVIE_ITEM_CURRENT_EPISODE),
                qualityLabel = item.textOrNull(YanHH3DSelectors.MOVIE_ITEM_QUALITY),
            )
        }

    /**
     * Title metadata plus episodes. [inputUrl] is the URL we were asked to load and is
     * only used when the page carries neither a canonical link nor `og:url`.
     */
    fun parseDetail(document: Document, inputUrl: String): YanDetail {
        val canonical = document.attrOrNull(YanHH3DSelectors.CANONICAL, "href")
            ?: document.attrOrNull(YanHH3DSelectors.OG_URL, "content")

        val genresRow = labelledElement(document, YanHH3DLabels.GENRES)

        return YanDetail(
            title = document.attrOrNull(YanHH3DSelectors.OG_TITLE, "content")
                ?: document.selectFirst("h1")?.text()?.trim()
                ?: document.title().trim(),
            url = domainResolver.absoluteUrl(canonical ?: inputUrl),
            posterUrl = document.attrOrNull(YanHH3DSelectors.OG_IMAGE, "content")
                ?.let(domainResolver::absoluteUrl),
            description = document.attrOrNull(YanHH3DSelectors.OG_DESCRIPTION, "content"),
            year = labelledValue(document, YanHH3DLabels.YEAR)
                ?.let { YanHH3DPatterns.YEAR.find(it)?.value?.toIntOrNull() },
            status = labelledValue(document, YanHH3DLabels.STATUS),
            genres = parseGenres(genresRow),
            episodes = parseEpisodes(document),
        )
    }

    /**
     * Episodes across every server tab, deduplicated by URL and sorted by episode
     * number. Entries whose number cannot be read keep their page order at the end.
     *
     * URLs are stored path-only, so CloudStream watch history survives a domain change.
     */
    fun parseEpisodes(document: Document): List<YanEpisode> {
        val container = document.selectFirst(YanHH3DSelectors.DETAIL_CONTAINER) ?: return emptyList()

        val tabIds = container.select("a[href^=#]")
            .map { it.attr("href").trim().removePrefix("#") }
            .filter(String::isNotEmpty)

        // Without tabs the container itself holds the episode anchors.
        val anchors = if (tabIds.isEmpty()) {
            container.select("a[href]")
        } else {
            tabIds.flatMap { id -> container.select("#$id a[href]") }
        }

        return anchors.mapNotNull { anchor ->
            val href = anchor.attr("href").trim()
            if (href.isEmpty() || href.startsWith("#")) return@mapNotNull null

            // The site wraps the label in a div; fall back to the anchor's own text.
            val name = anchor.selectFirst("div")?.text()?.trim()?.takeIf(String::isNotEmpty)
                ?: anchor.text().trim()
            if (name.isEmpty()) return@mapNotNull null

            YanEpisode(
                name = name,
                url = domainResolver.normalizeInternalData(href),
                episodeNumber = parseEpisodeNumber(name, href),
            )
        }
            .distinctBy { it.url }
            .sortedWith(compareBy(nullsLast()) { it.episodeNumber })
    }

    /**
     * Playable entries on an episode page. Every `data-src` is returned, including
     * ones classified [YanSourceType.UNKNOWN], so a maintainer can see what the page
     * offered instead of wondering why a server vanished.
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

    private fun classify(url: String): YanSourceType = when {
        url.contains(".m3u8", ignoreCase = true) -> YanSourceType.HLS
        YanHH3DPatterns.EMBED_HINTS.any { url.contains(it, ignoreCase = true) } -> YanSourceType.EMBED
        else -> YanSourceType.UNKNOWN
    }

    private fun parseEpisodeNumber(name: String, url: String): Int? =
        YanHH3DPatterns.EPISODE_NUMBER.find(name)?.groupValues?.get(1)?.toIntOrNull()
            ?: YanHH3DPatterns.EPISODE_NUMBER.find(url.substringAfterLast('/'))
                ?.groupValues?.get(1)?.toIntOrNull()

    private fun parseGenres(row: Element?): List<String> {
        if (row == null) return emptyList()

        val linked = row.select("a").map { it.text().trim() }.filter(String::isNotEmpty)
        if (linked.isNotEmpty()) return linked

        return labelledText(row, YanHH3DLabels.GENRES)
            ?.split(',', '|')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
    }

    /**
     * The innermost element that carries both [label] and a value. Jsoup returns
     * matches in document order, so the last element that still yields a value is the
     * row itself: ancestor blocks come earlier, and the bare `<span>` holding the
     * label comes later but yields nothing once the label is stripped off.
     */
    private fun labelledElement(document: Document, label: String): Element? =
        document.select(":contains($label)")
            .lastOrNull { labelledText(it, label) != null }

    private fun labelledValue(document: Document, label: String): String? =
        labelledElement(document, label)?.let { labelledText(it, label) }

    private fun labelledText(element: Element, label: String): String? {
        val text = element.text()
        val index = text.indexOf(label, ignoreCase = true)
        if (index < 0) return null

        return text.substring(index + label.length)
            .trimStart(':', ' ', ' ')
            .trim()
            .takeIf(String::isNotEmpty)
    }

    private fun Element.textOrNull(selector: String): String? =
        selectFirst(selector)?.text()?.trim()?.takeIf(String::isNotEmpty)

    private fun Document.attrOrNull(selector: String, attribute: String): String? =
        selectFirst(selector)?.attr(attribute)?.trim()?.takeIf(String::isNotEmpty)
}
