package com.vsphim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VsphimJsonParserTest {
    private val parser = VsphimJsonParser()

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/vsphim/$name")) {
            "Missing fixture vsphim/$name"
        }.readBytes().toString(Charsets.UTF_8)

    @Test
    fun `parses list response and flexible pagination`() {
        val response = parser.parseMovieList(fixture("movie-list.json"))

        assertNotNull(response)
        assertEquals(true, response?.status)
        assertEquals(2, response?.items?.size)
        assertEquals("phim-mau", response?.items?.first()?.slug)
        assertEquals(5, response?.pagination?.totalPages)
        assertEquals("20", response?.pagination?.totalItemsPerPage?.asText())
    }

    @Test
    fun `parses catalog wrapper and numeric or string ids`() {
        val response = parser.parseCatalog(fixture("catalog.json"))

        assertEquals("success", response?.status)
        assertEquals(2, response?.data?.items?.size)
        assertEquals(14, response?.data?.items?.first()?.id?.asInt())
        assertEquals("2026", response?.data?.items?.last()?.id?.asText())
    }

    @Test
    fun `parses detail metadata and nested servers`() {
        val response = parser.parseMovieDetail(fixture("movie-detail.json"))
        val movie = response?.movie

        assertNotNull(movie)
        assertEquals("series", movie?.type)
        assertEquals("Mô tả mẫu", movie?.content)
        assertEquals("Thể loại mẫu", movie?.category?.first()?.name)
        assertEquals(2, response?.episodes?.first()?.server_data?.size)
        assertEquals(
            "https://embed2.vsphim.com/embed/sample-1",
            response?.episodes?.first()?.server_data?.last()?.link_embed,
        )
    }

    @Test
    fun `maps and deduplicates embed episodes`() {
        val response = checkNotNull(parser.parseMovieDetail(fixture("movie-detail.json")))
        val playable = response.toPlayables()

        assertEquals(2, playable.size)
        assertEquals(listOf(2, 1), playable.map { it.episodeNumber })
        assertEquals("VIP — Tập 2", playable.first().name)
        assertTrue(playable.none { it.url.isBlank() })
    }

    @Test
    fun `combines nonblank category and country tags`() {
        val movie = checkNotNull(parser.parseMovieDetail(fixture("movie-detail.json"))).movie

        assertEquals(listOf("Thể loại mẫu", "Quốc gia mẫu"), movie?.tags())
    }

    @Test
    fun `returns empty list response without throwing`() {
        val response = parser.parseMovieList(fixture("empty-response.json"))

        assertNotNull(response)
        assertTrue(response?.items.orEmpty().isEmpty())
        assertEquals(0, response?.pagination?.totalItems)
    }

    @Test
    fun `returns null for malformed json`() {
        assertNull(parser.parseMovieList(fixture("malformed-response.json")))
    }
}
