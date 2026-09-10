package com.vsphim

import java.net.URI

data class VsphimPlayback(
    val url: String,
    val isPlaylist: Boolean = true,
)

/** Pure parsing for the VSPHIM player page returned in `link_embed`. */
object VsphimPlaybackParser {
    private val signedEnabled = Regex("""enableSignedUrl\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
    private val signedMaster = Regex("""signedMasterUrl\s*:\s*[\"']([^\"']+)[\"']""")
    private val baseUrl = Regex("""(?:const\s+)?baseUrl\s*=\s*[\"']([^\"']+)[\"']""")
    private val videoHash = Regex("""(?:const\s+)?videoHash\s*=\s*[\"']([^\"']+)[\"']""")
    private val directPlaylist = Regex("""(?:playerSource|videoSrc)\s*=\s*[\"']([^\"']+\.m3u8[^\"']*)[\"']""", RegexOption.IGNORE_CASE)

    fun parse(body: String, pageUrl: String): VsphimPlayback? {
        val content = body.trimStart()
        if (content.startsWith("#EXTM3U")) return VsphimPlayback(pageUrl)

        directPlaylist.find(body)?.groupValues?.getOrNull(1)?.let { playlist ->
            return VsphimPlayback(resolveUrl(playlist, pageUrl))
        }

        val signed = signedMaster.find(body)?.groupValues?.getOrNull(1).orEmpty()
        val useSigned = signedEnabled.find(body)?.groupValues?.getOrNull(1)
            ?.equals("true", ignoreCase = true) == true
        if (useSigned && signed.isNotBlank()) {
            return VsphimPlayback(resolveUrl(signed, pageUrl))
        }

        val base = baseUrl.find(body)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val hash = videoHash.find(body)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (base.isBlank() || hash.isBlank()) return null

        return VsphimPlayback("${base.trimEnd('/')}/stream/$hash/master.m3u8")
    }

    private fun resolveUrl(value: String, pageUrl: String): String {
        val normalized = value.replace("\\/", "/")
        return runCatching { URI(pageUrl).resolve(normalized).toString() }
            .getOrDefault(normalized)
    }
}
