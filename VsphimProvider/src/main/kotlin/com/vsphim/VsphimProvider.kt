package com.vsphim

import android.util.Log
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
class VsphimProvider : MainAPI() {
    private val resolver = VsphimDomainResolver()
    private val api = VsphimApiClient(resolver)

    override var mainUrl = resolver.mainUrl
    override var name = VsphimConstants.PROVIDER_NAME
    override var lang = "vi"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val mainPage = mainPageOf(*VsphimConstants.MAIN_PAGES.toTypedArray())

    private val posterHeaders = mapOf(
        "User-Agent" to VsphimConstants.USER_AGENT,
        "Referer" to mainUrl,
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? =
        runCatching {
            val response = api.getMainPage(request.data, page) ?: return null
            val items = response.items.mapNotNull { it.toSearchResponse() }
            newHomePageResponse(request, items, response.hasNext(page))
        }.getOrElse { error ->
            log("getMainPage failed for ${request.data}", error)
            null
        }

    override suspend fun search(query: String): List<SearchResponse> =
        runCatching {
            if (query.isBlank()) return emptyList()
            api.search(query.trim())?.items?.mapNotNull { it.toSearchResponse() }.orEmpty()
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

            if (movie.type.equals("single", ignoreCase = true)) {
                val source = playable.firstOrNull() ?: return null
                newMovieLoadResponse(title, detailUrl, TvType.Movie, source.url) {
                    posterUrl = movie.poster_url?.let(resolver::absoluteUrl)
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
                    posterUrl = movie.poster_url?.let(resolver::absoluteUrl)
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
            loadExtractor(data, mainUrl, subtitleCallback, callback)
        }.getOrElse { error ->
            log("loadLinks failed for $data", error)
            false
        }

    private fun VsphimMovieListItem.toSearchResponse(): SearchResponse? {
        val slug = slug?.trim().orEmpty()
        val title = name?.trim().orEmpty().ifEmpty { origin_name?.trim().orEmpty() }
        if (slug.isEmpty() || title.isEmpty()) return null

        return newMovieSearchResponse(
            title,
            resolver.absoluteUrl("${VsphimConstants.MOVIE_PATH}/$slug"),
            TvType.Movie,
        ) {
            posterUrl = poster_url?.let(resolver::absoluteUrl)
            posterHeaders = this@VsphimProvider.posterHeaders
        }
    }

    private fun VsphimPagination.hasNext(page: Int): Boolean =
        maxOf(currentPage, page) < totalPages

    private fun VsphimMovieListResponse.hasNext(page: Int): Boolean =
        pagination?.hasNext(page) ?: false

    private fun log(message: String, error: Throwable? = null) {
        if (error == null) {
            Log.i(VsphimConstants.PROVIDER_NAME, "${VsphimConstants.LOG_TAG} $message")
        } else {
            Log.e(VsphimConstants.PROVIDER_NAME, "${VsphimConstants.LOG_TAG} $message", error)
        }
    }
}
