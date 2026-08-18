package com.yanhh3d

/**
 * Internal parser models. The provider maps these onto CloudStream types, which
 * keeps the parser independent of the CloudStream API.
 */

/** One card on a list, category or search page. */
data class YanMovieItem(
    val title: String,
    val url: String,
    val posterUrl: String?,
    val currentEpisode: String?,
    val qualityLabel: String?,
)

/**
 * A title page. The detail page holds metadata only; [watchUrl] points at the page
 * that actually lists the episodes, which is why [episodes] starts empty and is filled
 * in by the provider after a second request.
 */
data class YanDetail(
    val title: String,
    val url: String,
    val posterUrl: String?,
    val description: String?,
    val year: Int?,
    val status: String?,
    val genres: List<String>,
    val watchUrl: String?,
    val episodes: List<YanEpisode> = emptyList(),
)

data class YanEpisode(
    val name: String,
    val url: String,
    val episodeNumber: Int?,
)

/** One playable entry from an episode page. [quality] uses [YanHH3DQualities]. */
data class YanSource(
    val name: String,
    val url: String,
    val type: YanSourceType,
    val quality: Int,
)

/**
 * What a server's page turned out to serve once fetched. The advertised URL says
 * nothing reliable about this: one ending in `.m3u8` usually returns a player page,
 * and one that looks like nothing in particular can return a progressive MP4.
 */
data class YanPlayback(
    val url: String,
    /** True for an HLS playlist, false for a single progressive file. */
    val isPlaylist: Boolean,
)

enum class YanSourceType {
    /** Direct .m3u8 playlist; playable once Referer and User-Agent are attached. */
    HLS,

    /** Third-party player page; handed to a CloudStream extractor. */
    EMBED,

    UNKNOWN,
}
