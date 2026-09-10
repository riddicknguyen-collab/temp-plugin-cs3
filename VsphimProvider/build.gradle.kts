dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("junit:junit:4.13.2")
}

version = 10

cloudstream {
    description = "VSPHIM JSON API provider for a private CloudStream repo"
    authors = listOf("personal")
    status = 1
    tvTypes = listOf("NSFW", "Movie", "TvSeries")
    requiresResources = false
    language = "vi"
    iconUrl = "https://nguon.vsphim.com/favicon.ico"
}

android {
    namespace = "com.vsphim"
}
