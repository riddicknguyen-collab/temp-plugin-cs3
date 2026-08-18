package com.yanhh3d

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the parser contract against fixtures taken from the live site. Everything here
 * runs on the JVM with no network and no CloudStream classes.
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
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/khac")

        assertEquals("https://yanhh3d.pw/nhat-tram-thuong-khung", detail.url)
    }

    @Test
    fun `parseDetail reads the open graph metadata`() {
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/nhat-tram-thuong-khung")

        assertEquals("Nhất Trảm Thương Khung [Cốc An] Thuyết Minh", detail.title)
        assertEquals("https://yanhh3d.pw/storage/movies/nhat-tram-thuong-khung.jpg", detail.posterUrl)
        assertTrue(detail.description.orEmpty().startsWith("Vốn sinh ra"))
    }

    @Test
    fun `parseDetail reads year and status from the info block`() {
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/nhat-tram-thuong-khung")

        assertEquals(2026, detail.year)
        assertEquals("Đang Chiếu", detail.status)
    }

    @Test
    fun `parseDetail takes genres from the info block and not the sidebar menu`() {
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/nhat-tram-thuong-khung")

        assertEquals(listOf("Huyền Huyễn", "Tiên Hiệp"), detail.genres)
        // The sidebar lists every genre on the site; none of it may leak in.
        assertTrue(detail.genres.none { it == "Cổ Trang" || it == "Hài Hước" || it == "Kiếm Hiệp" })
    }

    @Test
    fun `parseDetail picks the vietsub play button and stores it path-only`() {
        val detail = parser.parseDetail(fixture("detail.html"), "https://yanhh3d.pw/nhat-tram-thuong-khung")

        assertEquals("/sever2/nhat-tram-thuong-khung/tap-5", detail.watchUrl)
        // The detail page never carries episodes itself.
        assertTrue(detail.episodes.isEmpty())
    }

    @Test
    fun `parseDetail falls back to the input url when the page has no metadata`() {
        val document = Jsoup.parse("<html><head></head><body></body></html>", YanHH3DConstants.DEFAULT_BASE_URL)

        val detail = parser.parseDetail(document, "https://yanhh3d.pw/khong-metadata")

        assertEquals("https://yanhh3d.pw/khong-metadata", detail.url)
        assertNull(detail.year)
        assertNull(detail.status)
        assertNull(detail.watchUrl)
        assertTrue(detail.genres.isEmpty())
    }

    // ------------------------------------------------------------ episodes

    @Test
    fun `parseEpisodes takes only the preferred server tab`() {
        val episodes = parser.parseEpisodes(fixture("episode.html"))

        assertEquals(6, episodes.size)
        // Everything must come from the Vietsub pane, never the dubbed one.
        assertTrue(episodes.all { it.url.startsWith("/sever2/") })
    }

    @Test
    fun `parseEpisodes sorts numerically and keeps unnumbered entries last`() {
        val episodes = parser.parseEpisodes(fixture("episode.html"))

        assertEquals(listOf(1, 2, 3, 5, 10, null), episodes.map { it.episodeNumber })
        // 10 must not sort between 1 and 2.
        assertEquals("Tập 10", episodes[4].name)
        assertEquals("Đặc Biệt", episodes.last().name)
    }

    @Test
    fun `parseEpisodes names bare numbers and stores path-only urls`() {
        val episodes = parser.parseEpisodes(fixture("episode.html"))

        assertEquals("Tập 1", episodes.first().name)
        assertEquals("/sever2/nhat-tram-thuong-khung/tap-1", episodes.first().url)
        // The old-domain anchor normalises to the same path form as the rest.
        assertEquals("/sever2/nhat-tram-thuong-khung/tap-3", episodes[2].url)
        assertTrue(episodes.none { it.url.startsWith("http") })
    }

    @Test
    fun `parseEpisodes skips anchors with a blank href`() {
        val episodes = parser.parseEpisodes(fixture("episode.html"))

        assertTrue(episodes.none { it.name == "lỗi" })
    }

    @Test
    fun `parseEpisodes returns nothing when the container is missing`() {
        assertTrue(parser.parseEpisodes(fixture("detail.html")).isEmpty())
    }

    // ------------------------------------------------------------- sources

    @Test
    fun `parseSources classifies hls embed and unknown entries`() {
        val sources = parser.parseSources(fixture("episode.html"))

        assertEquals(5, sources.size)
        assertEquals(
            listOf(
                YanSourceType.HLS,
                YanSourceType.HLS,
                YanSourceType.UNKNOWN,
                YanSourceType.HLS,
                YanSourceType.EMBED,
            ),
            sources.map { it.type },
        )
    }

    @Test
    fun `parseSources detects quality from the server name`() {
        val sources = parser.parseSources(fixture("episode.html")).associateBy { it.name }

        assertEquals(YanHH3DQualities.P1080, sources.getValue("1080").quality)
        assertEquals(YanHH3DQualities.P1080, sources.getValue("1080-").quality)
        assertEquals(YanHH3DQualities.P2160, sources.getValue("4K").quality)
        // Neither a playlist nor a known embed, so no quality is claimed for it.
        assertEquals(YanHH3DQualities.UNKNOWN, sources.getValue("HD").quality)
        assertEquals(YanHH3DQualities.UNKNOWN, sources.getValue("Abyss").quality)
    }

    @Test
    fun `parseSources keeps every url exactly as published`() {
        val sources = parser.parseSources(fixture("episode.html")).associateBy { it.name }

        assertEquals(
            "https://scontent-sin2-9-xx.fbcdn.cloud/o2/v/t2/f2/m366/fcea5488.m3u8",
            sources.getValue("1080").url,
        )
        assertEquals("https://short.icu/abyss-nhat-tram-tap-5", sources.getValue("Abyss").url)
    }

    // -------------------------------------------------------------- player

    private fun fixtureText(name: String): String {
        val stream = checkNotNull(javaClass.getResourceAsStream("/yanhh3d/$name")) {
            "Missing fixture yanhh3d/$name"
        }
        return stream.readBytes().toString(Charsets.UTF_8)
    }

    @Test
    fun `parsePlayback reads the plain playlist out of the player config`() {
        val playback = parser.parsePlayback(fixtureText("player.html"), "https://cdn.example/a.m3u8")

        assertEquals(
            "https://scontent-sin2-9-xx.fbcdn.cloud/o2/v/t2/f2/m366/" +
                "fcea5488-32e0-484b-8c62-fdf54edb3971.m3u8/stream-plain?t=88bf081aa57e5ec4.1787057909",
            playback?.url,
        )
        assertEquals(true, playback?.isPlaylist)
    }

    @Test
    fun `parsePlayback keeps the source url when it already serves a manifest`() {
        val manifest = "#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:6.0,\nseg-1.ts\n"

        val playback = parser.parsePlayback(manifest, "https://cdn.example/a.m3u8")

        assertEquals("https://cdn.example/a.m3u8", playback?.url)
        assertEquals(true, playback?.isPlaylist)
    }

    @Test
    fun `parsePlayback picks up a progressive file when the server is not hls`() {
        val playback = parser.parsePlayback(
            fixtureText("player-progressive.html"),
            "https://cdn.example/play-fb-v8/play/123",
        )

        assertEquals(
            "https://video-sin6-2.xx.fbcdn.net/o1/v/t2/f2/m366/AQNXunvS4E1xFenxuskelL65.mp4" +
                "?_nc_cat=109&bitrate=1537189&tag=dash_h264-basic-gen2_720p",
            playback?.url,
        )
        assertEquals(false, playback?.isPlaylist)
        // The advertised name says only "HD", but the resolved URL still gives a height.
        assertEquals(YanHH3DQualities.P720, parser.parseQuality("HD", playback!!.url))
    }

    @Test
    fun `parsePlayback returns null when the page is neither shape`() {
        assertNull(parser.parsePlayback(fixtureText("episode.html"), "https://cdn.example/a.m3u8"))
        assertNull(parser.parsePlayback("", "https://cdn.example/a.m3u8"))
        assertNull(
            parser.parsePlayback(
                """<div id="player" data-obf="not base64 %%%"></div>""",
                "https://cdn.example/a.m3u8",
            ),
        )
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
