package com.vsphim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VsphimDomainResolverTest {
    private val resolver = VsphimDomainResolver()

    @Test
    fun `resolves api paths and preserves current domain`() {
        assertEquals(
            "https://nguon.vsphim.com/api/phim/phim-mau",
            resolver.absoluteUrl("/api/phim/phim-mau"),
        )
        assertEquals(
            "https://nguon.vsphim.com/api/phim/phim-mau",
            resolver.absoluteUrl("api/phim/phim-mau"),
        )
    }

    @Test
    fun `preserves foreign embed host`() {
        val embed = "https://embed2.vsphim.com/embed/sample-1"
        assertEquals(embed, resolver.absoluteUrl(embed))
        assertEquals(embed, resolver.remapKnownDomain(embed))
    }

    @Test
    fun `preserves query while adding page through api client path rules`() {
        assertEquals(
            "https://nguon.vsphim.com/api/danh-sach?type=series",
            resolver.absoluteUrl("/api/danh-sach?type=series"),
        )
    }

    @Test
    fun `homepage is split into twenty item paginated sections`() {
        assertEquals(7, VsphimConstants.MAIN_PAGES.size)
        assertTrue(VsphimConstants.MAIN_PAGES.all { it.first.contains("limit=20") })
    }
}
