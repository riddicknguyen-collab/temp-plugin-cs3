package com.vsphim

import org.junit.Assert.assertEquals
import org.junit.Test

class VsphimMappingTest {
    @Test
    fun `keeps list card data when detail refresh is unavailable`() {
        val item = VsphimMovieListItem(
            id = 48951,
            name = "Phim mẫu",
            slug = "phim-mau",
            poster_url = "poster.jpg",
            thumb_url = "thumb.jpg",
            year = 2026,
        )

        assertEquals(item, item.withDetails(null))
    }
}
