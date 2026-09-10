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
        assertEquals(
            "https://nguon.vsphim.com/storage/images/phim-mau/thumb.jpg",
            response?.items?.first()?.thumb_url,
        )
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
        assertEquals(
            "https://nguon.vsphim.com/storage/images/phim-mau/poster.jpg",
            movie?.poster_url,
        )
        assertEquals(
            "https://nguon.vsphim.com/storage/images/phim-mau/thumb.jpg",
            movie?.thumb_url,
        )
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

    @Test
    fun `sorts list by newest modified time with id fallback`() {
        val response = checkNotNull(parser.parseMovieList(fixture("movie-list.json")))
        val sorted = response.sortedByModified()

        assertEquals(listOf("phim-mau", ""), sorted.items.map { it.slug })
    }

    @Test
    fun `merges detail metadata and image variants into list item`() {
        val item = VsphimMovieListItem(
            id = 10,
            name = "Old title",
            slug = "old-slug",
            poster_url = "old-poster.jpg",
            thumb_url = "old-thumb.jpg",
            year = 2025,
        )
        val detail = VsphimMovieDetail(
            id = 11,
            name = "Fresh title",
            slug = "fresh-slug",
            poster_url = "fresh-poster.jpg",
            thumb_url = "fresh-thumb.jpg",
            year = 2026,
        )

        val merged = item.withDetails(detail)

        assertEquals(11, merged.id)
        assertEquals("Fresh title", merged.name)
        assertEquals("fresh-slug", merged.slug)
        assertEquals("fresh-poster.jpg", merged.poster_url)
        assertEquals("fresh-thumb.jpg", merged.thumb_url)
        assertEquals(2026, merged.year)
    }

    @Test
    fun `handles null optional detail arrays`() {
        val movie = VsphimMovieDetail(
            actor = null,
            director = null,
            category = null,
            country = null,
        )
        val response = VsphimMovieDetailResponse(movie = movie, episodes = null)

        assertTrue(movie.tags().isEmpty())
        assertTrue(response.toPlayables().isEmpty())
    }
}
