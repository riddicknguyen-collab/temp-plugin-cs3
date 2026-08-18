package com.yanhh3d

import android.util.Log
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Document
import java.net.URLEncoder

/**
 * CloudStream integration: HTTP requests, model mapping and error handling.
 * All HTML knowledge lives in [YanHH3DParser], all URL knowledge in [YanHH3DDomainResolver].
 *
 * Every public override swallows failures and degrades to an empty result, because a
 * throwing provider takes the whole home screen down with it.
 */
class YanHH3DProvider : MainAPI() {

    private val domainResolver = YanHH3DDomainResolver()
    private val parser = YanHH3DParser(domainResolver)

    override var mainUrl = domainResolver.mainUrl
    override var name = YanHH3DConstants.PROVIDER_NAME

    override var lang = "vi"

    override val hasMainPage = true

    // The site is Chinese donghua, so Anime has to be declared: CloudStream groups
    // providers by supportedTypes, and without it YanHH3D never shows up under the
    // animation media filter.
    override val supportedTypes = setOf(TvType.Anime, TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(*YanHH3DConstants.MAIN_PAGES.toTypedArray())

    private val defaultHeaders = mapOf("User-Agent" to YanHH3DConstants.USER_AGENT)

    /** CloudStream fetches posters with its own image loader, which sends none of our headers. */
    private val posterRequestHeaders = mapOf(
        "User-Agent" to YanHH3DConstants.USER_AGENT,
        "Referer" to mainUrl,
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? =
        runCatching {
            val url = withPage(domainResolver.absoluteUrl(request.data), page)
            log("getMainPage url=$url")

            val items = parser.parseList(fetch(url))
            log("parseList count=${items.size}")

            newHomePageResponse(
                request,
                items.map { it.toSearchResponse() },
                hasNext = items.isNotEmpty(),
            )
        }.getOrElse { error ->
            log("getMainPage failed for ${request.data}", error)
            null
        }

    override suspend fun search(query: String): List<SearchResponse> =
        runCatching {
            if (query.isBlank()) return emptyList()

            val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
            val url = "$mainUrl${YanHH3DConstants.SEARCH_PATH}" +
                "?${YanHH3DConstants.SEARCH_QUERY_PARAM}=$encoded"
            log("search url=$url")

            val items = parser.parseList(fetch(url))
            log("parseList count=${items.size}")

            items.map { it.toSearchResponse() }
        }.getOrElse { error ->
            log("search failed", error)
            emptyList()
        }

    /**
     * [url] may arrive as a path-only string or on an old domain, because CloudStream
     * replays whatever it stored in bookmarks and history.
     */
    override suspend fun load(url: String): LoadResponse? =
        runCatching {
            val pageUrl = domainResolver.absoluteUrl(url)
            log("load detail url=$pageUrl")

            val detail = parser.parseDetail(fetch(pageUrl), pageUrl)

            // The detail page carries no episode list, only play buttons into the watch
            // page, so the episodes cost one more request.
            val episodes = detail.watchUrl?.let { watchUrl ->
                val resolved = domainResolver.absoluteUrl(watchUrl)
                log("load watch url=$resolved")
                parser.parseEpisodes(fetch(resolved))
            }.orEmpty()
            log("parseEpisodes count=${episodes.size}")

            newTvSeriesLoadResponse(
                detail.title,
                detail.url,
                TvType.Anime,
                episodes.map { it.toEpisode() },
            ) {
                this.posterUrl = detail.posterUrl
                this.posterHeaders = posterRequestHeaders
                this.plot = detail.description
                this.year = detail.year
                this.tags = detail.genres
            }
        }.getOrElse { error ->
            log("load failed for $url", error)
            null
        }

    /**
     * Emits every direct HLS playlist the episode page exposes and hands embed hosts
     * to the CloudStream extractors, rather than picking a single "best" server: the
     * player is in a better position to choose a quality than we are.
     */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean =
        runCatching {
            val episodeUrl = domainResolver.absoluteUrl(data)
            log("loadLinks url=$episodeUrl")

            val sources = parser.parseSources(fetch(episodeUrl))
            log("source count=${sources.size}")

            var found = false
            sources.forEach { source ->
                when (source.type) {
                    YanSourceType.EMBED -> {
                        log("embed source=${source.name}")
                        if (loadExtractor(source.url, mainUrl, subtitleCallback, callback)) {
                            found = true
                        }
                    }

                    // Everything else is resolved by fetching it: the advertised URL
                    // says nothing reliable about what the server actually serves.
                    else -> {
                        val playback = resolvePlayback(source.url)
                        if (playback == null) {
                            log("source unresolved=${source.name}")
                        } else {
                            log("${if (playback.isPlaylist) "hls" else "video"} source=${source.name}")
                            callback(
                                newExtractorLink(
                                    source = name,
                                    name = "$name ${source.name}",
                                    url = playback.url,
                                    type = if (playback.isPlaylist) {
                                        ExtractorLinkType.M3U8
                                    } else {
                                        ExtractorLinkType.VIDEO
                                    },
                                ) {
                                    // The CDN rejects requests without these.
                                    this.referer = mainUrl
                                    this.quality = source.quality.takeIf {
                                        it != YanHH3DQualities.UNKNOWN
                                    } ?: parser.parseQuality(source.name, playback.url)
                                    this.headers = defaultHeaders
                                },
                            )
                            found = true
                        }
                    }
                }
            }
            found
        }.getOrElse { error ->
            log("loadLinks failed for $data", error)
            false
        }

    /**
     * Fetches what a server advertises and works out the playable manifest from it.
     * Logs enough on failure to tell a network error apart from an unrecognised page.
     */
    private suspend fun resolvePlayback(sourceUrl: String): YanPlayback? =
        runCatching {
            val body = app.get(sourceUrl, headers = defaultHeaders, referer = mainUrl).text
            parser.parsePlayback(body, sourceUrl).also { resolved ->
                if (resolved == null) {
                    log("source page unrecognised, ${body.length} chars, at $sourceUrl")
                }
            }
        }.getOrElse { error ->
            log("source page failed for $sourceUrl", error)
            null
        }

    private fun YanEpisode.toEpisode(): Episode {
        val source = this
        return newEpisode(source.url) {
            this.name = source.name
            this.episode = source.episodeNumber
        }
    }

    private fun YanMovieItem.toSearchResponse(): SearchResponse {
        val item = this
        return newTvSeriesSearchResponse(item.title, item.url, TvType.Anime) {
            this.posterUrl = item.posterUrl
            this.posterHeaders = posterRequestHeaders
        }
    }

    /** Page 1 uses the bare path; later pages append the site's `page` query parameter. */
    private fun withPage(url: String, page: Int): String {
        if (page <= 1) return url
        val separator = if (url.contains('?')) '&' else '?'
        return "$url$separator${YanHH3DConstants.PAGE_QUERY_PARAM}=$page"
    }

    private suspend fun fetch(url: String): Document =
        app.get(url, headers = defaultHeaders, referer = mainUrl).document

    private fun log(message: String, error: Throwable? = null) {
        if (error == null) {
            Log.i(YanHH3DConstants.PROVIDER_NAME, "${YanHH3DConstants.LOG_TAG} $message")
        } else {
            Log.e(YanHH3DConstants.PROVIDER_NAME, "${YanHH3DConstants.LOG_TAG} $message", error)
        }
    }
}
