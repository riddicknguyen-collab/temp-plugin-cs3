package com.vsphim

import android.util.Log
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

class VsphimProvider : MainAPI() {
    private val resolver = VsphimDomainResolver()
    private val api = VsphimApiClient(resolver)
    private val detailCache = ConcurrentHashMap<String, VsphimMovieDetail>()
    private val detailRequestLimiter = Semaphore(4)

    override var mainUrl = resolver.mainUrl
    override var name = VsphimConstants.PROVIDER_NAME
    override var lang = "vi"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie, TvType.TvSeries)
    // The VSPHIM thumb is a landscape fanart image. Mark every homepage row as
    // horizontal so CloudStream uses the matching card layout and keeps the
    // row's "view all" navigation available for paginated sections.
    override val mainPage = mainPageOf(
        *VsphimConstants.MAIN_PAGES.map { (path, title) ->
            MainPageData(title, path, true)
        }.toTypedArray(),
    )

    private val posterHeaders = mapOf(
        "User-Agent" to VsphimConstants.USER_AGENT,
        "Referer" to mainUrl,
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? =
        runCatching {
            val response = api.getMainPage(request.data, page)?.sortedByModified() ?: return null
            val items = response.items
                .take(VsphimConstants.HOME_PAGE_LIMIT)
                .toSearchResponsesWithDetails()
            newHomePageResponse(request, items, response.hasNext(page))
        }.getOrElse { error ->
            log("getMainPage failed for ${request.data}", error)
            null
        }

    override suspend fun search(query: String): List<SearchResponse> =
        runCatching {
            if (query.isBlank()) return emptyList()
            api.search(query.trim())?.items.orEmpty().toSearchResponsesWithDetails()
        }.getOrElse { error ->
            log("search failed", error)
            emptyList()
        }

    override suspend fun load(url: String): LoadResponse? =
        runCatching {
            val detailUrl = resolver.absoluteUrl(url)
            val response = api.getMovie(detailUrl) ?: return null
            val movie = response.movie ?: return null
            val title = movie.name?.takeIf { it.isNotBlank() }
                ?: movie.origin_name?.takeIf { it.isNotBlank() }
                ?: return null
            val playable = response.toPlayables()

            if (!movie.type.isSeriesType()) {
                // Keep the detail page usable even when the server has no playable
                // source. The source can be resolved again when playback starts.
                val dataUrl = playable.firstOrNull()?.url ?: detailUrl
                newMovieLoadResponse(title, detailUrl, TvType.Movie, dataUrl) {
                    posterUrl = movie.thumbUrl(resolver)
                    backgroundPosterUrl = movie.thumbUrl(resolver)
                    posterHeaders = this@VsphimProvider.posterHeaders
                    plot = movie.content
                    year = movie.year
                    tags = movie.tags()
                }
            } else {
                newTvSeriesLoadResponse(
                    title,
                    detailUrl,
                    TvType.TvSeries,
                    playable.map { source ->
                        newEpisode(source.url) {
                            name = source.name
                            episode = source.episodeNumber
                        }
                    },
                ) {
                    posterUrl = movie.thumbUrl(resolver)
                    backgroundPosterUrl = movie.thumbUrl(resolver)
                    posterHeaders = this@VsphimProvider.posterHeaders
                    plot = movie.content
                    year = movie.year
                    tags = movie.tags()
                }
            }
        }.getOrElse { error ->
            log("load failed for $url", error)
            null
        }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean =
        runCatching {
            if (data.isBlank()) return false
            val episodeUrl = resolvePlaybackUrl(data) ?: return false
            val page = app.get(
                episodeUrl,
                headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml",
                    "User-Agent" to VsphimConstants.USER_AGENT,
                ),
                referer = mainUrl,
            )
            val playback = VsphimPlaybackParser.parse(page.text, episodeUrl)
            if (playback != null) {
                val mediaHeaders = buildMap {
                    put("User-Agent", VsphimConstants.USER_AGENT)
                    put("Referer", episodeUrl)
                    originOf(playback.url)?.let { put("Origin", it) }
                }
                callback(
                    newExtractorLink(
                        source = name,
                        name = "$name HLS",
                        url = playback.url,
                        type = ExtractorLinkType.M3U8,
                    ) {
                        referer = episodeUrl
                        headers = mediaHeaders
                    },
                )
                true
            } else {
                loadExtractor(episodeUrl, mainUrl, subtitleCallback, callback)
            }
        }.getOrElse { error ->
            log("loadLinks failed for $data", error)
            false
        }

    private fun VsphimMovieListItem.toSearchResponse(): SearchResponse? {
        val slug = slug?.trim().orEmpty()
        val title = name?.trim().orEmpty().ifEmpty { origin_name?.trim().orEmpty() }
        if (slug.isEmpty() || title.isEmpty()) return null

        val url = resolver.absoluteUrl("${VsphimConstants.MOVIE_PATH}/$slug")
        val fanart = (thumb_url.nonBlankOr(poster_url))?.let(resolver::absoluteUrl)
        return if (type.isSeriesType()) {
            newTvSeriesSearchResponse(title, url, TvType.TvSeries) {
                posterUrl = fanart
                posterHeaders = this@VsphimProvider.posterHeaders
            }
        } else {
            newMovieSearchResponse(title, url, TvType.Movie) {
                posterUrl = fanart
                posterHeaders = this@VsphimProvider.posterHeaders
            }
        }
    }

    private suspend fun List<VsphimMovieListItem>.toSearchResponsesWithDetails(): List<SearchResponse> =
        coroutineScope {
            map { item ->
                async {
                    detailRequestLimiter.withPermit {
                        runCatching {
                            val slug = item.slug?.trim().orEmpty()
                            val details = if (slug.isEmpty()) null else getMovieDetails(slug)
                            // The list endpoint already contains enough data to render a card.
                            // Keep that card when the optional detail refresh fails.
                            item.withDetails(details).toSearchResponse()
                        }.getOrNull()
                    }
                }
            }.awaitAll().filterNotNull()
        }

    private suspend fun getMovieDetails(slug: String): VsphimMovieDetail? {
        detailCache[slug]?.let { return it }
        val detail = api.getMovie(slug)?.movie ?: return null
        detailCache.putIfAbsent(slug, detail)
        return detail
    }

    private suspend fun resolvePlaybackUrl(data: String): String? {
        val value = resolver.absoluteUrl(data)
        if (!value.contains("${VsphimConstants.MOVIE_PATH}/")) return value
        return api.getMovie(value)?.toPlayables()?.firstOrNull()?.url
    }

    private fun VsphimMovieDetail.thumbUrl(resolver: VsphimDomainResolver): String? =
        thumb_url.nonBlankOr(poster_url)
            ?.let(resolver::absoluteUrl)

    private fun VsphimPagination.hasNext(page: Int): Boolean =
        maxOf(currentPage, page) < totalPages

    private fun VsphimMovieListResponse.hasNext(page: Int): Boolean =
        pagination?.hasNext(page) ?: false

    private fun originOf(url: String): String? = runCatching {
        val uri = java.net.URI(url)
        if (uri.scheme.isNullOrBlank() || uri.authority.isNullOrBlank()) null
        else "${uri.scheme}://${uri.authority}"
    }.getOrNull()

    private fun String?.isSeriesType(): Boolean =
        this.equals("series", ignoreCase = true) ||
            this.equals("hoathinh", ignoreCase = true) ||
            this.equals("tvshows", ignoreCase = true)

    private fun log(message: String, error: Throwable? = null) {
        if (error == null) {
            Log.i(VsphimConstants.PROVIDER_NAME, "${VsphimConstants.LOG_TAG} $message")
        } else {
            Log.e(VsphimConstants.PROVIDER_NAME, "${VsphimConstants.LOG_TAG} $message", error)
        }
    }
}
