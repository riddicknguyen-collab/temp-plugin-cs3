package com.vsphim

import com.lagradost.cloudstream3.app
import kotlinx.coroutines.delay
import java.net.URLEncoder

class VsphimApiClient(
    private val resolver: VsphimDomainResolver = VsphimDomainResolver(),
    private val parser: VsphimJsonParser = VsphimJsonParser(),
) {
    private val headers = mapOf(
        "Accept" to "application/json",
        "User-Agent" to VsphimConstants.USER_AGENT,
    )

    suspend fun getMainPage(path: String, page: Int): VsphimMovieListResponse? =
        getMovieList(path, page)

    suspend fun getMovieList(
        page: Int,
        type: String? = null,
        status: String? = null,
    ): VsphimMovieListResponse? {
        val query = buildList {
            type?.let { add("${VsphimConstants.TYPE_PARAM}=${encode(it)}") }
            status?.let { add("${VsphimConstants.STATUS_PARAM}=${encode(it)}") }
        }.joinToString("&")
        return getMovieList(
            if (query.isEmpty()) VsphimConstants.LIST_PATH else "${VsphimConstants.LIST_PATH}?$query",
            page,
        )
    }

    suspend fun search(keyword: String, page: Int = 1, limit: Int = 24): VsphimMovieListResponse? {
        val path = "${VsphimConstants.SEARCH_PATH}?" +
            "${VsphimConstants.KEYWORD_PARAM}=${encode(keyword)}" +
            "&${VsphimConstants.LIMIT_PARAM}=$limit"
        return getMovieList(path, page)
    }

    suspend fun getMovie(urlOrSlug: String): VsphimMovieDetailResponse? {
        val url = if (urlOrSlug.contains("/api/phim/")) {
            resolver.absoluteUrl(urlOrSlug)
        } else {
            resolver.absoluteUrl("${VsphimConstants.MOVIE_PATH}/${urlOrSlug.trim('/')}")
        }
        return getJson(url, parser::parseMovieDetail)
    }

    suspend fun getGenres(): VsphimCatalogResponse? = getCatalog(VsphimConstants.GENRES_PATH)
    suspend fun getCountries(): VsphimCatalogResponse? = getCatalog(VsphimConstants.COUNTRIES_PATH)
    suspend fun getYears(): VsphimCatalogResponse? = getCatalog(VsphimConstants.YEARS_PATH)
    suspend fun getCodes(): VsphimCatalogResponse? = getCatalog(VsphimConstants.CODES_PATH)

    private suspend fun getCatalog(path: String): VsphimCatalogResponse? =
        getJson(resolver.absoluteUrl(path), parser::parseCatalog)

    private suspend fun getMovieList(path: String, page: Int): VsphimMovieListResponse? {
        val separator = if (path.contains('?')) '&' else '?'
        val pagePath = if (page > 1) "$path$separator${VsphimConstants.PAGE_PARAM}=$page" else path
        return getJson(resolver.absoluteUrl(pagePath), parser::parseMovieList)
    }

    private suspend fun <T> getJson(url: String, parse: (String) -> T?): T? {
        repeat(2) { attempt ->
            try {
                return parse(app.get(url, headers = headers, referer = resolver.mainUrl).text)
            } catch (error: Throwable) {
                if (attempt == 0 && error.isRateLimited()) {
                    delay(750)
                    return@repeat
                }
                return null
            }
        }
        return null
    }

    private fun Throwable.isRateLimited(): Boolean =
        message?.contains("429") == true || message?.contains("Too Many Requests", ignoreCase = true) == true

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
