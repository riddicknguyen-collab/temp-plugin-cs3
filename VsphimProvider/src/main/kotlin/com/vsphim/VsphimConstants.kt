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
     * Each entry is a real category API query, so CloudStream can request page 2,
     * 3, ... when the user opens a section. The category slugs are the same values
     * returned in each movie's `category` array. The API orders by modified
     * descending; the provider also applies the same ordering defensively.
     *
     * VSPHIM exposes a very large, noisy taxonomy. Keep the homepage to stable,
     * useful categories instead of creating hundreds of nearly empty sections.
     */
    val MAIN_PAGES = listOf(
        "$LIST_PATH?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "Mới cập nhật",
        "$GENRES_PATH/vietsub?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "Vietsub",
        "$GENRES_PATH/18-tuoi?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "18 tuổi",
        "$GENRES_PATH/hanh-dong?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "Hành động",
        "$GENRES_PATH/nhat-ban?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "Nhật Bản",
        "$GENRES_PATH/trung-quoc?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "Trung Quốc",
        "$GENRES_PATH/3d?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "3D",
        "$GENRES_PATH/4k?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "4K",
        "$GENRES_PATH/hd?$LIMIT_PARAM=$HOME_PAGE_LIMIT" to "HD",
    )

    val KNOWN_DOMAINS = setOf("nguon.vsphim.com")
}
