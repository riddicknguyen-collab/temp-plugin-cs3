# AGENTS.md

## Project Goal

Build a private CloudStream 3 Kotlin provider for YanHH3D. The provider should scrape normal public HTML pages, expose home/category/search/detail/episode flows, return HLS `.m3u8` sources with headers, and fall back to CloudStream extractors for supported embed hosts.

## Repository Structure

- `.github/` - GitHub Actions workflow for building plugins and publishing generated `.cs3` artifacts to the `builds` branch.
- `.vscode/` - Local editor settings for this template; do not rely on it for build behavior.
- `docs/` - Product requirements and implementation planning documents for the YanHH3D provider.
- `ExampleProvider/` - Working CloudStream sample plugin module; use it as the local reference for module shape, plugin entrypoint, resources, and Gradle metadata.
- `YanHHProvider/` - The YanHH3D provider module: plugin entrypoint, provider, parser, domain resolver, constants, models, and parser tests with HTML fixtures.
- `gradle/` - Gradle wrapper files; keep these in sync with the template and do not edit manually unless upgrading Gradle intentionally.
- `build.gradle.kts` - Root Gradle configuration shared by all plugin modules, including Android, Kotlin, CloudStream, Jsoup, and build defaults.
- `settings.gradle.kts` - Auto-includes every top-level directory that contains `build.gradle.kts`; a plugin folder is not a module until it has its own build file.
- `gradle.properties` - Gradle and Android build settings for this repo.
- `gradlew` - Unix Gradle wrapper entrypoint.
- `gradlew.bat` - Windows Gradle wrapper entrypoint.
- `repo.json` - Private CloudStream repository manifest; the URL added to CloudStream points at this file on the `builds` branch.
- `CHANGELOG.md` - Release notes per plugin version.
- `README.md` - Upstream CloudStream plugin template instructions.
- `.gitignore` - Ignore rules for build output and local files.

## Implementation Rules

- Use `ExampleProvider` as the template, but implement production code under `YanHHProvider`.
- Keep YanHH3D domain values in one constants/resolver file. Do not scatter `yanhh3d.love` or old domains through provider code.
- Keep CSS selectors in one constants object so live-site selector changes are small.
- Parser code should be pure Jsoup parsing with no network calls.
- Provider code should own CloudStream API integration, HTTP requests, and model mapping.
- Public provider methods should catch failures and return safe empty/null/false results.
- Do not use WebView, browser automation, CAPTCHA bypass, DRM bypass, login bypass, cookie harvesting, downloading, mirroring, or rehosting.
- Preserve `Referer` and `User-Agent` headers for direct HLS links.
- Prefer small focused Kotlin files over one large provider file.

## YanHHProvider Layout

```text
YanHHProvider/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/kotlin/com/yanhh3d/YanHH3DPlugin.kt
  src/main/kotlin/com/yanhh3d/YanHH3DProvider.kt
  src/main/kotlin/com/yanhh3d/YanHH3DParser.kt
  src/main/kotlin/com/yanhh3d/YanHH3DDomainResolver.kt
  src/main/kotlin/com/yanhh3d/YanHH3DModels.kt
  src/main/kotlin/com/yanhh3d/YanHH3DConstants.kt
  src/test/kotlin/com/yanhh3d/YanHH3DParserTest.kt
  src/test/resources/yanhh3d/*.html
```

## Build Prerequisites

- JDK 17 with `JAVA_HOME` set. AGP 8.7.3 requires it; the build fails outright without it.
- Android SDK with `platform-tools`, `platforms;android-35` and `build-tools;35.0.0`, and `ANDROID_HOME` set.
- The `cloudstream3:pre-release` stub is a moving tag. When it is rebuilt with a newer Kotlin, `compileDebugKotlin` fails with an incompatible-metadata error and `kotlin-gradle-plugin` in the root `build.gradle.kts` has to be raised to match.

Setup steps are in `docs/adding-a-new-provider.md`.

## Build And Test Commands

Use Windows commands in this workspace:

```powershell
.\gradlew.bat tasks --all
.\gradlew.bat YanHHProvider:test
.\gradlew.bat YanHHProvider:make
.\gradlew.bat ExampleProvider:make
.\gradlew.bat make makePluginsJson
```

If `YanHHProvider:make` is not listed, create `YanHHProvider/build.gradle.kts` first. `settings.gradle.kts` will auto-include it after the build file exists.

Unit tests run on the JVM with no CloudStream classes on the runtime classpath. That is why the parser must not import anything from `com.lagradost`, and why quality values live in `YanHH3DQualities` instead of `Qualities`.

## Release

- `main` holds source; the GitHub Actions workflow publishes `.cs3`, `plugins.json` and `repo.json` to the `builds` branch.
- Adding a provider needs no change to `repo.json`; `plugins.json` is generated from every module.
- Bump the module `version` whenever selectors, default domain, source extraction, or build output change, otherwise CloudStream will not offer the update.
- The repository must be public for CloudStream to read the raw URL.

## Planning Notes

- Step-by-step guide for adding another provider: `docs/adding-a-new-provider.md`.
- Implementation plan and its per-phase status: `docs/plan.md`.
- Product requirements: `docs/YanHH3D_CloudStream_Plugin_PRD.md`.
- Release notes per version: `CHANGELOG.md`.
- Keep `README.md` as template documentation; project-specific docs live in `docs/`.
