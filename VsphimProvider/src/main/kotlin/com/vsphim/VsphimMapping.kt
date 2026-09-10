package com.vsphim

internal fun VsphimMovieDetail.tags(): List<String> =
    (category.mapNotNull { it.name?.trim() } + country.mapNotNull { it.name?.trim() })
        .filter { it.isNotEmpty() }
        .distinct()

/**
 * List responses are intentionally small. Merge the detail response returned
 * by /api/phim/{slug} so cards keep the freshest title and both image variants.
 */
internal fun VsphimMovieListItem.withDetails(detail: VsphimMovieDetail?): VsphimMovieListItem =
    detail?.let {
        copy(
            id = it.id ?: id,
            name = it.name.nonBlankOr(name),
            origin_name = it.origin_name.nonBlankOr(origin_name),
            slug = it.slug.nonBlankOr(slug),
            poster_url = it.poster_url.nonBlankOr(poster_url),
            thumb_url = it.thumb_url.nonBlankOr(thumb_url),
            year = it.year ?: year,
            modified = it.modified ?: modified,
        )
    } ?: this

internal fun String?.nonBlankOr(fallback: String?): String? =
    this?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

internal fun VsphimMovieListResponse.sortedByModified(): VsphimMovieListResponse =
    copy(
        items = items.sortedWith(VsphimMovieNewestFirstComparator),
    )

private object VsphimMovieNewestFirstComparator : Comparator<VsphimMovieListItem> {
    override fun compare(first: VsphimMovieListItem, second: VsphimMovieListItem): Int {
        val modified = second.modified?.time.orEmpty().compareTo(first.modified?.time.orEmpty())
        if (modified != 0) return modified
        return (second.id ?: Int.MIN_VALUE).compareTo(first.id ?: Int.MIN_VALUE)
    }
}

internal fun VsphimMovieDetailResponse.toPlayables(): List<VsphimPlayable> =
    episodes.asSequence()
        .flatMap { server ->
            val serverName = server.server_name?.trim().orEmpty().ifEmpty { "VSPHIM" }
            server.server_data.asSequence().mapNotNull { item ->
                val url = item.link_embed?.trim().orEmpty()
                if (url.isEmpty()) return@mapNotNull null
                val episodeName = item.name?.trim().orEmpty()
                    .ifEmpty { item.filename?.trim().orEmpty() }
                    .ifEmpty { "Full" }
                VsphimPlayable(
                    name = "$serverName — $episodeName",
                    url = url,
                    episodeNumber = episodeNumber(episodeName),
                )
            }
        }
        .distinctBy { it.url }
        .toList()

private fun episodeNumber(value: String): Int? =
    Regex("(?i)(?:tập|tap|episode|ep)?\\s*(\\d+)").find(value)?.groupValues?.getOrNull(1)
        ?.toIntOrNull()
