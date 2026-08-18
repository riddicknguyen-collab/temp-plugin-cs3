dependencies {
    // Parser tests run on the JVM against local HTML fixtures. Jsoup already comes
    // from the root build file; the CloudStream stubs are compile-only, which is why
    // the parser must stay free of CloudStream types.
    testImplementation("junit:junit:4.13.2")
}

// Use an integer for version numbers
version = 6

cloudstream {
    description = "YanHH3D provider for a private CloudStream repo"
    authors = listOf("personal")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1

    tvTypes = listOf("Anime", "TvSeries", "Movie")

    requiresResources = false
    language = "vi"

    iconUrl = "https://yanhh3d.pw/favicon.ico"
}

android {
    // The root build file applies "com.example" to every subproject; keep this
    // module in its own namespace so it never collides with ExampleProvider.
    namespace = "com.yanhh3d"
}
