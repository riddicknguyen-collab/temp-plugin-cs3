package com.vsphim

object VsphimConstants {
    const val PROVIDER_NAME = "VSPHIM"
    const val LOG_TAG = "[VSPHIM]"
    const val DEFAULT_BASE_URL = "https://nguon.vsphim.com"
    const val API_PREFIX = "/api"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

    const val LATEST_PATH = "$API_PREFIX/danh-sach/phim-moi-cap-nhat"
    const val LIST_PATH = "$API_PREFIX/danh-sach"
    const val SEARCH_PATH = "$API_PREFIX/tim-kiem"
    const val MOVIE_PATH = "$API_PREFIX/phim"
    const val GENRES_PATH = "$API_PREFIX/the-loai"
    const val COUNTRIES_PATH = "$API_PREFIX/quoc-gia"
    const val YEARS_PATH = "$API_PREFIX/nam"
    const val CODES_PATH = "$API_PREFIX/code"

    const val PAGE_PARAM = "page"
    const val LIMIT_PARAM = "limit"
    const val HOME_PAGE_LIMIT = 20
    const val KEYWORD_PARAM = "keyword"
    const val TYPE_PARAM = "type"
    const val STATUS_PARAM = "status"

    /**
     * Each entry is a real API query, so CloudStream can request page 2, 3, ...
     * when the user opens a section. The API's default ordering is modified
     * descending; the provider also applies the same ordering defensively.
     */
    val MAIN_PAGES = listOf(
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "Mới cập nhật",
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT&type=single" to "Phim lẻ",
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT&type=series" to "Phim bộ",
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT&type=hoathinh" to "Hoạt hình",
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT&type=tvshows" to "TV Shows",
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT&status=ongoing" to "Đang cập nhật",
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT&status=completed" to "Hoàn thành",
    )

    val KNOWN_DOMAINS = setOf("nguon.vsphim.com")
}
