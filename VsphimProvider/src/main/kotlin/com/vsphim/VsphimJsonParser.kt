package com.vsphim

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/** Pure JSON parsing. It has no HTTP or CloudStream dependency, so fixtures can test it on the JVM. */
class VsphimJsonParser(
    private val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
) {
    fun parseMovieList(body: String): VsphimMovieListResponse? =
        read(body, VsphimMovieListResponse::class.java)

    fun parseCatalog(body: String): VsphimCatalogResponse? =
        read(body, VsphimCatalogResponse::class.java)

    fun parseMovieDetail(body: String): VsphimMovieDetailResponse? =
        read(body, VsphimMovieDetailResponse::class.java)

    private fun <T> read(body: String, type: Class<T>): T? =
        runCatching { mapper.readValue(body, type) }.getOrNull()
}
