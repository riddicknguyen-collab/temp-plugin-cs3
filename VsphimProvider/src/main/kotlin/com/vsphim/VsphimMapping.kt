package com.vsphim

internal fun VsphimMovieDetail.tags(): List<String> =
    (category.mapNotNull { it.name?.trim() } + country.mapNotNull { it.name?.trim() })
        .filter { it.isNotEmpty() }
        .distinct()

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
