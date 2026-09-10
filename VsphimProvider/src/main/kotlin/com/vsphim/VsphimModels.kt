package com.vsphim

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimMovieListResponse(
    val status: Boolean = false,
    val items: List<VsphimMovieListItem> = emptyList(),
    val pagination: VsphimPagination? = null,
    val pathImage: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimMovieListItem(
    @JsonProperty("_id") val id: Int? = null,
    val name: String? = null,
    val origin_name: String? = null,
    val slug: String? = null,
    val poster_url: String? = null,
    val thumb_url: String? = null,
    val year: Int? = null,
    val modified: VsphimTimestamp? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimPagination(
    val totalItems: Int = 0,
    val totalItemsPerPage: JsonNode? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimTimestamp(
    val time: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimCatalogResponse(
    val status: String? = null,
    val message: String? = null,
    val data: VsphimCatalogData? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimCatalogData(
    val items: List<VsphimCatalogItem> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimCatalogItem(
    @JsonProperty("_id") val id: JsonNode? = null,
    val name: String? = null,
    val slug: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimMovieDetailResponse(
    val status: Boolean = false,
    val msg: String? = null,
    val movie: VsphimMovieDetail? = null,
    val episodes: List<VsphimEpisodeServer> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimMovieDetail(
    @JsonProperty("_id") val id: Int? = null,
    val name: String? = null,
    val origin_name: String? = null,
    val slug: String? = null,
    val content: String? = null,
    val type: String? = null,
    val status: String? = null,
    val poster_url: String? = null,
    val thumb_url: String? = null,
    val is_copyright: Boolean? = null,
    val trailer_url: String? = null,
    val time: String? = null,
    val episode_current: String? = null,
    val episode_total: String? = null,
    val quality: String? = null,
    val lang: String? = null,
    val notify: String? = null,
    val showtimes: String? = null,
    val year: Int? = null,
    val view: Int? = null,
    val chieurap: Boolean? = null,
    val sub_docquyen: Boolean? = null,
    val actor: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val category: List<VsphimTaxonomy> = emptyList(),
    val country: List<VsphimTaxonomy> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimTaxonomy(
    val id: Int? = null,
    val name: String? = null,
    val slug: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimEpisodeServer(
    val server_name: String? = null,
    val server_data: List<VsphimEpisodeData> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VsphimEpisodeData(
    val name: String? = null,
    val slug: String? = null,
    val filename: String? = null,
    val link_embed: String? = null,
)

data class VsphimPlayable(
    val name: String,
    val url: String,
    val episodeNumber: Int?,
)
