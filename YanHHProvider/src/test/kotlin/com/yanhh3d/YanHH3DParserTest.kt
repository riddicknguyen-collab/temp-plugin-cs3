package com.yanhh3d

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the parser contract against local fixtures. Everything here runs on the JVM
 * with no network and no CloudStream classes.
 */
class YanHH3DParserTest {

    private val resolver = YanHH3DDomainResolver()
    private val parser = YanHH3DParser(resolver)

    private fun fixture(name: String): Document {
        val stream = checkNotNull(javaClass.getResourceAsStream("/yanhh3d/$name")) {
            "Missing fixture yanhh3d/$name"
        }
        return Jsoup.parse(stream.readBytes().toString(Charsets.UTF_8), YanHH3DConstants.DEFAULT_BASE_URL)
    }

    // ---------------------------------------------------------------- list

    @Test
    fun `parseList skips cards with a blank href`() {
        val items = parser.parseList(fixture("home.html"))

        assertEquals(6, items.size)
        assertTrue(items.none { it.title == "Phim Lỗi" })
    }

    @Test
    fun `parseList reads the lazy-loaded poster and the anchor title`() {
        val item = parser.parseList(fixture("home.html")).first()

        assertEquals("Tiên Nghịch", item.title)
        assertEquals("https://yanhh3d.pw/tien-nghich", item.url)
        // The image carries no src at all; the real URL is in data-src.
        assertEquals("https://yanhh3d.pw/storage/movies/tien-nghich.png", item.posterUrl)
        assertEquals("154/180 [4K]", item.currentEpisode)
        assertNull(item.qualityLabel)
    }

    @Test
    fun `parseList remaps the old domain on both the link and the poster`() {
        val item = parser.parseList(fixture("home.html"))[1]

        assertEquals("https://yanhh3d.pw/gia-thien", item.url)
        assertEquals("https://yanhh3d.pw/storage/movies/gia-thien.png", item.posterUrl)
    }

    @Test
    fun `parseList falls back to the card heading when the anchor has no title`() {
        val item = parser.parseList(fixture("home.html"))[2]

        assertEquals("Phàm Nhân Tu Tiên", item.title)
        assertEquals("https://yanhh3d.pw/pham-nhan-tu-tien", item.url)
        assertEquals("https://yanhh3d.pw/storage/movies/pham-nhan-tu-tien.png", item.posterUrl)
    }

    @Test
    fun `parseList still reads a plain src and a relative href`() {
        val item = parser.parseList(fixture("home.html"))[3]

        assertEquals("https://yanhh3d.pw/thon-phe-tinh-khong", item.url)
        assertEquals("https://yanhh3d.pw/storage/movies/thon-phe-tinh-khong.png", item.posterUrl)
        assertEquals("1080p", item.qualityLabel)
    }

    @Test
    fun `parseList survives a card with no image`() {
        val item = parser.parseList(fixture("home.html"))[4]

        assertEquals("Vô Thần", item.title)
        assertNull(item.posterUrl)
        assertNull(item.currentEpisode)
    }

    @Test
    fun `parseList resolves a protocol-relative poster`() {
        val item = parser.parseList(fixture("home.html"))[5]

        // A CDN host is not a YanHH3D domain, so only the scheme gets filled in.
        assertEquals("https://cdn.yanhh3d.pw/storage/movies/dau-pha-thuong-khung.png", item.posterUrl)
    }

    // -------------------------------------------------------------- detail

    @Test
    fun `parseDetail prefers canonical over og-url`() {
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/phim/khac")

        assertEquals("https://yanhh3d.pw/phim/dau-pha-thuong-khung", detail.url)
    }

    @Test
    fun `parseDetail reads the open graph metadata`() {
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/phim/dau-pha-thuong-khung")

        assertEquals("Đấu Phá Thương Khung", detail.title)
        assertEquals("https://yanhh3d.pw/uploads/poster/dau-pha-thuong-khung.jpg", detail.posterUrl)
        assertTrue(detail.description.orEmpty().startsWith("Tiêu Viêm mất sạch tu vi"))
    }

    @Test
    fun `parseDetail reads year status and genres from the visible text`() {
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/phim/dau-pha-thuong-khung")

        assertEquals(2023, detail.year)
        assertEquals("Đang chiếu", detail.status)
        assertEquals(listOf("Huyền Huyễn", "Tiên Hiệp"), detail.genres)
    }

    @Test
    fun `parseDetail falls back to the input url when the page has no canonical`() {
        val document = Jsoup.parse("<html><head></head><body></body></html>", YanHH3DConstants.DEFAULT_BASE_URL)

        val detail = parser.parseDetail(document, "https://yanhh3d.pw/phim/khong-metadata")

        assertEquals("https://yanhh3d.pw/phim/khong-metadata", detail.url)
        assertNull(detail.year)
        assertNull(detail.status)
        assertTrue(detail.genres.isEmpty())
        assertTrue(detail.episodes.isEmpty())
    }

    // ------------------------------------------------------------ episodes

    @Test
    fun `parseEpisodes deduplicates across server tabs and sorts numerically`() {
        val episodes = parser.parseEpisodes(fixture("detail.html"))

        assertEquals(5, episodes.size)
        assertEquals(listOf(1, 2, 3, 10, null), episodes.map { it.episodeNumber })
        // 10 must not sort between 1 and 2.
        assertEquals("Tập 10", episodes[3].name)
        // Anything without a number keeps to the end.
        assertEquals("Tập Đặc Biệt", episodes.last().name)
    }

    @Test
    fun `parseEpisodes stores path-only urls so a domain change cannot break history`() {
        val episodes = parser.parseEpisodes(fixture("detail.html"))

        assertEquals("/phim/dau-pha-thuong-khung/tap-1", episodes.first().url)
        // The old-domain anchor normalises to the same path form as the rest.
        assertEquals("/phim/dau-pha-thuong-khung/tap-3", episodes[2].url)
        assertTrue(episodes.none { it.url.startsWith("http") })
    }

    @Test
    fun `parseEpisodes skips anchors with a blank href`() {
        val episodes = parser.parseEpisodes(fixture("detail.html"))

        assertTrue(episodes.none { it.name == "Tập lỗi" })
    }

    // ------------------------------------------------------------- sources

    @Test
    fun `parseSources classifies hls embed and unknown entries`() {
        val sources = parser.parseSources(fixture("episode.html"))

        assertEquals(7, sources.size)
        assertEquals(
            listOf(
                YanSourceType.HLS,
                YanSourceType.HLS,
                YanSourceType.HLS,
                YanSourceType.HLS,
                YanSourceType.EMBED,
                YanSourceType.EMBED,
                YanSourceType.UNKNOWN,
            ),
            sources.map { it.type },
        )
    }

    @Test
    fun `parseSources detects quality from the server name and the url`() {
        val sources = parser.parseSources(fixture("episode.html")).associateBy { it.name }

        assertEquals(YanHH3DQualities.P1080, sources.getValue("HD 1080").quality)
        assertEquals(YanHH3DQualities.P2160, sources.getValue("4K").quality)
        assertEquals(YanHH3DQualities.P720, sources.getValue("720p").quality)
        assertEquals(YanHH3DQualities.UNKNOWN, sources.getValue("Server HLS").quality)
        assertEquals(YanHH3DQualities.UNKNOWN, sources.getValue("Abyss").quality)
    }

    @Test
    fun `parseSources skips blank data-src and deduplicates by url`() {
        val sources = parser.parseSources(fixture("episode.html"))

        assertTrue(sources.none { it.url.isBlank() })
        assertTrue(sources.none { it.name == "Server lỗi" })
        assertEquals(sources.size, sources.distinctBy { it.url }.size)
        assertTrue(sources.none { it.name.contains("dự phòng") })
    }

    // ------------------------------------------------------------ resolver

    @Test
    fun `resolver builds absolute urls from every input shape`() {
        assertEquals("https://yanhh3d.pw/phim/a", resolver.absoluteUrl("/phim/a"))
        assertEquals("https://yanhh3d.pw/phim/a", resolver.absoluteUrl("phim/a"))
        assertEquals("https://yanhh3d.pw/phim/a", resolver.absoluteUrl("https://yanhh3d.ac/phim/a"))
        assertEquals("https://cdn.example.com/a.jpg", resolver.absoluteUrl("//cdn.example.com/a.jpg"))
        assertEquals("", resolver.absoluteUrl("   "))
    }

    @Test
    fun `resolver leaves foreign hosts alone`() {
        val embed = "https://short.icu/abyss-1"

        assertEquals(embed, resolver.remapKnownDomain(embed))
        assertEquals(embed, resolver.normalizeInternalData(embed))
    }

    @Test
    fun `resolver reduces known domains to a path`() {
        assertEquals("/phim/a/tap-1", resolver.normalizeInternalData("https://yanhh3d.ac/phim/a/tap-1"))
        assertEquals("/phim/a/tap-1", resolver.normalizeInternalData("https://www.yanhh3d.love/phim/a/tap-1"))
        assertEquals("/phim/a/tap-1", resolver.normalizeInternalData("phim/a/tap-1"))
    }
}
