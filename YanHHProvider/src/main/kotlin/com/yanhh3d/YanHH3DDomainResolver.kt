package com.yanhh3d

/**
 * The single place that knows which host YanHH3D currently lives on.
 *
 * Pure string handling, no network calls, so it can be unit tested and reused by
 * the parser. Point [baseUrl] at a new host and every URL the provider builds,
 * reads or stores follows.
 */
class YanHH3DDomainResolver(
    baseUrl: String = YanHH3DConstants.DEFAULT_BASE_URL,
) {
    /** Current base URL without a trailing slash, e.g. `https://yanhh3d.love`. */
    val mainUrl: String = baseUrl.trim().trimEnd('/')

    /**
     * The current host counts as known even if someone forgets to add it to
     * [YanHH3DConstants.KNOWN_DOMAINS] when moving the default.
     */
    private val knownHosts: Set<String> =
        (YanHH3DConstants.KNOWN_DOMAINS + mainUrl.substringAfter("://"))
            .map(::bareHost)
            .toSet()

    /**
     * Turns anything the page gave us into an absolute URL on the current host:
     * absolute URLs are remapped, protocol-relative ones get a scheme, paths are
     * prefixed. Blank input stays blank so callers can drop it.
     */
    fun absoluteUrl(input: String): String {
        val url = input.trim()
        return when {
            url.isEmpty() -> ""
            url.startsWith("//") -> remapKnownDomain("https:$url")
            url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true) -> remapKnownDomain(url)

            url.startsWith("/") -> mainUrl + url
            else -> "$mainUrl/$url"
        }
    }

    /**
     * Rewrites a URL saved under an older YanHH3D host onto the current one.
     * Foreign hosts (CDNs, embed players) are left untouched.
     */
    fun remapKnownDomain(input: String): String {
        val url = input.trim()
        val origin = HOST_REGEX.find(url) ?: return url
        if (!isKnownHost(origin.groupValues[2])) return url
        return mainUrl + url.substring(origin.value.length)
    }

    /**
     * Path-only form for anything CloudStream persists (episode data, bookmarks,
     * watch history), so a later domain change cannot invalidate it. URLs on
     * foreign hosts are returned unchanged, since their path alone is meaningless.
     */
    fun normalizeInternalData(input: String): String {
        val url = input.trim()
        if (url.isEmpty()) return ""

        val origin = HOST_REGEX.find(url) ?: return withLeadingSlash(url)
        if (!isKnownHost(origin.groupValues[2])) return url
        return withLeadingSlash(url.substring(origin.value.length))
    }

    private fun isKnownHost(host: String) = bareHost(host) in knownHosts

    private fun withLeadingSlash(path: String) = if (path.startsWith("/")) path else "/$path"

    private companion object {
        val HOST_REGEX = Regex("^(https?://)([^/?#]+)", RegexOption.IGNORE_CASE)

        /** Host without port or `www.`, lowercased, for comparison. */
        fun bareHost(host: String) = host.substringBefore(':').removePrefix("www.").lowercase()
    }
}
