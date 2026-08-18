package com.yanhh3d

/**
 * Everything that changes when YanHH3D changes domain, routes or markup.
 * No other file in this module should hardcode these values.
 */
object YanHH3DConstants {
    const val PROVIDER_NAME = "YanHH3D"

    /** Prefix for every log line this provider writes. */
    const val LOG_TAG = "[YanHH3D]"

    const val DEFAULT_BASE_URL = "https://yanhh3d.pw"

    /**
     * Hosts YanHH3D has been served from, newest first. A URL stored under any of them
     * is remapped onto the current base URL, so bookmarks and watch history survive a
     * domain move. Add the outgoing domain here whenever [DEFAULT_BASE_URL] changes.
     */
    val KNOWN_DOMAINS = listOf(
        "yanhh3d.pw",
        "yanhh3d.love",
        "yanhh3d.ac",
    )

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

    const val SEARCH_PATH = "/search"
    const val SEARCH_QUERY_PARAM = "keysearch"
    const val PAGE_QUERY_PARAM = "page"

    /** Path to display name, in the order they should appear on the CloudStream home screen. */
    val MAIN_PAGES = listOf(
        "/moi-cap-nhat" to "Mới cập nhật",
        "/the-loai/huyen-huyen" to "Huyền Huyễn",
        "/the-loai/xuyen-khong" to "Xuyên Không",
        "/the-loai/trung-sinh" to "Trùng Sinh",
        "/the-loai/tien-hiep" to "Tiên Hiệp",
        "/the-loai/co-trang" to "Cổ Trang",
        "/the-loai/hai-huoc" to "Hài Hước",
        "/the-loai/kiem-hiep" to "Kiếm Hiệp",
        "/the-loai/hien-dai" to "Hiện Đại",
    )
}

/**
 * CSS selectors, kept together so a live-site markup change is a one-file edit.
 */
object YanHH3DSelectors {
    const val MOVIE_ITEM = ".flw-item"
    const val MOVIE_ITEM_LINK = "a[href]"
    const val MOVIE_ITEM_POSTER = "img"

    /**
     * The card image is lazy-loaded: the real URL sits in `data-src` and the element
     * carries no `src` at all. Read these in order and take the first non-blank one,
     * so both the lazy markup and a plain `src` keep working.
     */
    val POSTER_ATTRIBUTES = listOf("data-src", "src", "data-original", "data-lazy-src")

    /** The card repeats its title in a heading; used when the anchor has no `title`. */
    const val MOVIE_ITEM_TITLE = "h4"

    const val MOVIE_ITEM_CURRENT_EPISODE = ".tick-rate"
    const val MOVIE_ITEM_QUALITY = ".tick-dub"

    /**
     * Detail page. Metadata sits in rows of `span.item-head` (label) plus either a
     * `span.name` value or genre anchors. Scoped to the info block because the sidebar
     * menu repeats the same label words.
     */
    const val DETAIL_INFO = ".anisc-info"
    const val DETAIL_INFO_ITEM = ".item"
    const val DETAIL_INFO_LABEL = "span.item-head"
    const val DETAIL_INFO_VALUE = "span.name"

    /**
     * The detail page carries no episode list, only buttons into the watch page, one
     * per server. Episodes are parsed from the page these lead to.
     */
    const val PLAY_BUTTON = ".film-buttons a[href]"

    /** Watch page: episode list, one tab pane per server. */
    const val EPISODE_CONTAINER = ".detail-infor-content"
    const val EPISODE_TAB = "a[href^=#]"
    const val EPISODE_LINK = "a[href]"
    const val EPISODE_ORDER = ".ssli-order"

    const val SERVER_LINK = "div[class*=list-severs] a[data-src]"

    /**
     * A server's `data-src` ends in `.m3u8` but serves an HTML player page, whose
     * config blob holds the real playlist URL. Playing the advertised URL directly
     * fails with player error 3002 (`ERROR_CODE_PARSING_MANIFEST_MALFORMED`).
     */
    const val PLAYER_CONFIG = "#player[data-obf]"
    const val PLAYER_CONFIG_ATTRIBUTE = "data-obf"

    const val CANONICAL = "link[rel=canonical]"
    const val OG_URL = "meta[property=og:url]"
    const val OG_TITLE = "meta[property=og:title]"
    const val OG_IMAGE = "meta[property=og:image]"
    const val OG_DESCRIPTION = "meta[property=og:description]"
    const val VIDEO_DURATION = "meta[property=video:duration]"
}

/**
 * Vietnamese labels the detail page prints next to its metadata. Matched without the
 * trailing colon, because the site is not consistent about spacing around it.
 */
object YanHH3DLabels {
    const val STATUS = "Trạng thái"
    const val YEAR = "Năm"
    const val GENRES = "Thể loại"

    /**
     * The site publishes each title on two servers, "Thuyết Minh" (dubbed) and
     * "Vietsub". Only the Vietsub one is wanted, so both the watch-page button and the
     * episode tab are picked by this label.
     */
    const val PREFERRED_SERVER = "Vietsub"
}

object YanHH3DPatterns {
    /** A plausible release year, so a stray number in the same row cannot win. */
    val YEAR = Regex("(?:19|20)\\d{2}")

    val EPISODE_NUMBER = Regex("(\\d+)")

    /** Hosts or paths that mean "this is a player page, hand it to an extractor". */
    val EMBED_HINTS = listOf("abyss", "embed", "player", "short.icu")

    /**
     * The plain playlist URL inside the player page's config blob. The blob also
     * carries an AES-GCM encrypted variant and its key; only the plain one the site
     * already serves is used.
     */
    val PLAYER_PLAIN_URL = Regex("\"pU\"\\s*:\\s*\"([^\"]+)\"")

    /**
     * Some servers are not HLS at all: their page is a jwplayer setup whose source is a
     * single progressive MP4 held in a script variable.
     */
    val PROGRESSIVE_FILE = Regex("https?://[^\"'\\s]+\\.mp4[^\"'\\s]*")
}

/**
 * Quality values as plain ints so the parser stays free of CloudStream classes and
 * remains unit-testable. These mirror `com.lagradost.cloudstream3.utils.Qualities`
 * exactly (verified against the stub jar), so the provider can hand them straight
 * to an ExtractorLink.
 */
object YanHH3DQualities {
    const val UNKNOWN = 400
    const val P480 = 480
    const val P720 = 720
    const val P1080 = 1080
    const val P2160 = 2160
}
