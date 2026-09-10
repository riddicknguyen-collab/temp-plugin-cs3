package com.vsphim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VsphimPlaybackParserTest {
    @Test
    fun parsesVspPlayerPageIntoMasterPlaylist() {
        val body = """
            <script>
                const playerOptions = { enableSignedUrl: false, signedMasterUrl: "" };
                const baseUrl = 'https://sv3.streamvsphim.top';
                const videoHash = 'abc-123';
                let playerSource = playerOptions.signedMasterUrl;
            </script>
        """.trimIndent()

        assertEquals(
            "https://sv3.streamvsphim.top/stream/abc-123/master.m3u8",
            VsphimPlaybackParser.parse(body, "http://sv3.streamvsphim.top/video/abc-123")?.url,
        )
    }

    @Test
    fun prefersSignedPlaylistWhenEnabled() {
        val body = "enableSignedUrl: true; signedMasterUrl: '/signed/master.m3u8';"

        assertEquals(
            "https://sv3.streamvsphim.top/signed/master.m3u8",
            VsphimPlaybackParser.parse(body, "https://sv3.streamvsphim.top/video/abc")?.url,
        )
    }

    @Test
    fun acceptsDirectPlaylistResponse() {
        assertEquals(
            "https://sv3.streamvsphim.top/stream/abc/master.m3u8",
            VsphimPlaybackParser.parse(
                "#EXTM3U\n#EXT-X-VERSION:3",
                "https://sv3.streamvsphim.top/stream/abc/master.m3u8",
            )?.url,
        )
    }

    @Test
    fun rejectsUnrecognisedPlayerPage() {
        assertNull(VsphimPlaybackParser.parse("<html><body>empty</body></html>", "https://example.test/video"))
    }
}
