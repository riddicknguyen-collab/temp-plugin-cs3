package com.vsphim

class VsphimDomainResolver(
    baseUrl: String = VsphimConstants.DEFAULT_BASE_URL,
) {
    val mainUrl = baseUrl.trim().trimEnd('/')

    private val knownHosts = (VsphimConstants.KNOWN_DOMAINS + hostOf(mainUrl))
        .map(::bareHost)
        .toSet()

    fun absoluteUrl(input: String): String {
        val value = input.trim()
        return when {
            value.isEmpty() -> ""
            value.startsWith("//") -> remapKnownDomain("https:$value")
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true) -> remapKnownDomain(value)
            value.startsWith("/") -> "$mainUrl$value"
            else -> "$mainUrl/$value"
        }
    }

    fun remapKnownDomain(input: String): String {
        val value = input.trim()
        val match = HOST_REGEX.find(value) ?: return value
        if (bareHost(match.groupValues[1]) !in knownHosts) return value
        return mainUrl + value.substring(match.value.length)
    }

    private fun hostOf(url: String): String = url.removePrefix("https://").removePrefix("http://")
        .substringBefore('/').substringBefore('?').substringBefore('#')

    private fun bareHost(host: String): String = host.substringBefore(':')
        .removePrefix("www.").lowercase()

    private companion object {
        val HOST_REGEX = Regex("^https?://([^/?#]+)", RegexOption.IGNORE_CASE)
    }
}
