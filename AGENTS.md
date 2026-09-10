# AGENTS.md

## Project Goal

Maintain a personal CloudStream 3 Kotlin plugin repository with two production providers:

- `YanHHProvider` scrapes the public YanHH3D HTML site for home, category, search, detail, episode, and playback flows.
- `VsphimProvider` uses the public VSPHIM JSON API for catalog, search, detail, poster/thumb metadata, episodes, and HLS playback.

Both providers must return safe results when upstream sites fail, preserve the headers required by image/video hosts, and publish installable `.cs3` artifacts through the `builds` branch.

## Current Modules

| Module | Current version | Data source | Notes |
| --- | ---: | --- | --- |
| `VsphimProvider` | 7 | `https://nguon.vsphim.com/api` | Runtime types include NSFW, Movie, and TvSeries. Homepage sections use real category slugs and 20 items per page. |
| `YanHHProvider` | 8 | Public YanHH3D HTML | Current default domain is centralized in `YanHH3DConstants`; parser behavior is covered by HTML fixtures. |
| `ExampleProvider` | 1 | Template/sample | Keep as an upstream module-shape reference, not production code. |

When a module build file changes its version, update `CHANGELOG.md`, `README.md`, and the relevant plan document in the same change.

## Repository Structure

- `.github/workflows/build.yml` - builds every plugin and force-publishes `.cs3`, `plugins.json`, and `repo.json` to `builds`.
- `VsphimProvider/` - VSPHIM plugin entrypoint, provider, API client, JSON/playback parsers, resolver, constants, models, mapping helpers, tests, and JSON fixtures.
- `YanHHProvider/` - YanHH3D plugin entrypoint, provider, pure Jsoup parser, resolver, constants/selectors, models, tests, and HTML fixtures.
- `ExampleProvider/` - original CloudStream sample plugin.
- `docs/` - provider plans, VSPHIM API reference, YanHH3D PRD, release/setup guide, and captured upstream pages.
- `build.gradle.kts` - shared AGP/Kotlin/CloudStream dependencies, Android defaults, and generated plugin-URL cache busting.
- `settings.gradle.kts` - auto-includes each top-level directory containing `build.gradle.kts` unless disabled.
- `repo.json` - CloudStream repository manifest copied to `builds`; its plugin list uses raw GitHub.
- `CHANGELOG.md` - release notes for both providers.
- `README.md` - project install/build summary followed by the retained upstream template documentation.
- `gradle/`, `gradlew`, `gradlew.bat`, `gradle.properties` - Gradle wrapper and shared settings.

Generated outputs such as module `build/` directories, root `build/plugins.json`, and `.cs3` files must not be committed to `main`.

## Shared Implementation Rules

- Keep production changes inside the target provider module; do not use `ExampleProvider` for real provider code.
- Centralize domains, API paths, route parameters, and selectors in constants/resolver files. Do not scatter live or historical domains through provider logic.
- Keep parsers deterministic and network-free. Provider/API-client code owns HTTP calls and CloudStream model mapping.
- Keep parser tests independent of `com.lagradost` runtime classes so JVM tests can run without the CloudStream app.
- Public provider methods must catch upstream/network/parser failures and return safe empty, null, or false results.
- Preserve `User-Agent`, `Referer`, and `Origin` where required for posters, player pages, HLS manifests, and segments.
- Prefer focused Kotlin files and mapping helpers over one large provider file.
- Do not use WebView, browser automation, CAPTCHA bypass, DRM bypass, login bypass, cookie harvesting, downloading, mirroring, or rehosting.
- Do not raise Jackson above `2.13.1` without Android compatibility testing; the root build intentionally pins that version.
- Treat `cloudstream3:pre-release` and the CloudStream Gradle plugin as moving dependencies. Validate with refreshed dependencies when changing build tooling.

## VsphimProvider Rules And Layout

```text
VsphimProvider/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/kotlin/com/vsphim/VsphimPlugin.kt
  src/main/kotlin/com/vsphim/VsphimProvider.kt
  src/main/kotlin/com/vsphim/VsphimApiClient.kt
  src/main/kotlin/com/vsphim/VsphimJsonParser.kt
  src/main/kotlin/com/vsphim/VsphimPlaybackParser.kt
  src/main/kotlin/com/vsphim/VsphimDomainResolver.kt
  src/main/kotlin/com/vsphim/VsphimConstants.kt
  src/main/kotlin/com/vsphim/VsphimModels.kt
  src/main/kotlin/com/vsphim/VsphimMapping.kt
  src/test/kotlin/com/vsphim/*.kt
  src/test/resources/vsphim/*.json
```

Current VSPHIM behavior:

- `Mới cập nhật` uses `/api/danh-sach?limit=20&page=n`.
- Homepage category sections use `/api/the-loai/{slug}?limit=20&page=n`. The current stable sections are Vietsub, 18 tuổi, Hành động, Nhật Bản, Trung Quốc, 3D, 4K, and HD.
- Do not generate homepage sections from the entire `/api/the-loai` catalog; the live taxonomy contains thousands of noisy and nearly empty tags.
- Sort list responses by `modified.time` descending, with `_id` descending as fallback.
- Enrich homepage and search cards through `/api/phim/{slug}` and cache detail responses for the provider session.
- Use `poster_url` for card/detail posters and `thumb_url` for `backgroundPosterUrl`; fall back between them when one is blank.
- Flatten `episodes[].server_data[]`, retain the server name, and deduplicate by playable URL.
- Fetch `link_embed` as a player page. `VsphimPlaybackParser` may accept a direct playlist, signed master URL, or `baseUrl + videoHash`; otherwise use CloudStream extractors.

The VSPHIM API contract and observed response differences are documented in `docs/tham-khao/vsphim-api-reference.md`.

## YanHHProvider Rules And Layout

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

YanHH3D-specific rules:

- Keep all historical/current domains in `YanHH3DConstants.KNOWN_DOMAINS` and remap stored URLs through `YanHH3DDomainResolver`.
- Keep CSS selectors in `YanHH3DSelectors` so live markup changes remain localized.
- `YanHH3DParser` must remain pure Jsoup parsing with no network or CloudStream imports.
- The detail page and watch page have different responsibilities; consult current fixtures and captured pages before changing episode extraction.
- Preserve poster headers and playback context headers for direct media links.

The live-site implementation record is in `docs/plan.md`; the older PRD is useful context but is not the source of truth for current selectors.

## Build Prerequisites

- JDK 17 with `JAVA_HOME` set.
- Android SDK with `platform-tools`, `platforms;android-35`, and `build-tools;35.0.0`; set `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
- Gradle wrapper 8.12.
- Android Gradle Plugin 8.7.3.
- Kotlin Gradle Plugin 2.4.10.

The CloudStream Gradle plugin currently resolves from JitPack as `com.github.recloudstream.gradle:gradle:-SNAPSHOT`. JitPack occasionally returns a transient “Could not find ... -SNAPSHOT” error on a clean runner. Confirm the coordinate still exists and rerun before changing it; direct commit-version coordinates have not resolved reliably.

If the moving `cloudstream3:pre-release` stub is rebuilt with newer Kotlin metadata, `compileDebugKotlin` may require raising the root Kotlin plugin to a compatible version.

Setup details are in `docs/adding-a-new-provider.md`.

## Build And Test Commands

Use PowerShell commands in this workspace:

```powershell
.\gradlew.bat tasks --all
.\gradlew.bat VsphimProvider:test
.\gradlew.bat VsphimProvider:make
.\gradlew.bat YanHHProvider:test
.\gradlew.bat YanHHProvider:make
.\gradlew.bat ExampleProvider:make
.\gradlew.bat make makePluginsJson
```

Use this when reproducing a clean CI dependency resolution:

```powershell
.\gradlew.bat VsphimProvider:test --refresh-dependencies
```

Expected outputs:

- `VsphimProvider/build/VsphimProvider.cs3`
- `YanHHProvider/build/YanHHProvider.cs3`
- `ExampleProvider/build/ExampleProvider.cs3`
- `build/plugins.json`

`VsphimProvider:test` is the scoped regression suite for VSPHIM. The latest VSPHIM plan records that YanHHProvider currently has fixture/domain expectation drift, so do not attribute an existing YanHH test failure to a VSPHIM-only change without inspecting it.

## Release And Installation

- `main` stores source; `builds` stores generated artifacts and manifests.
- The workflow builds all modules with `make makePluginsJson`, copies artifacts to the `builds` checkout, and force-updates that branch.
- Root-level Markdown-only pushes matching the workflow's `*.md` ignore rule do not publish a new build. A version/code/build-file change is required for a release.
- Bump a module's integer `version` whenever provider behavior or its `.cs3` output changes; otherwise CloudStream will not offer an update.
- `makePluginsJson` appends `?cache=$GITHUB_SHA` to raw `.cs3` URLs. Keep this cache token: without it, raw/CDN caches may serve plugin metadata for a new version with an older binary.
- The canonical repository URL is:

```text
https://raw.githubusercontent.com/riddicknguyen-collab/temp-plugin-cs3/builds/repo.json
```

- `repo.json` points to raw `builds/plugins.json`; generated plugin entries point to raw `.cs3` artifacts with a commit cache token.
- The GitHub repository must remain public for CloudStream to read raw branch files.
- After publishing, verify both the `version` in `plugins.json` and `manifest.json` inside the downloaded `.cs3`. Matching metadata alone does not prove the binary cache is current.

## Documentation Map

- `docs/adding-a-new-provider.md` - setup, module scaffolding, testing, and release guide.
- `docs/plan-vsphim-provider.md` - VSPHIM v7 scope, category homepage behavior, and implementation status.
- `docs/tham-khao/vsphim-api-reference.md` - VSPHIM API endpoints and observed response schemas.
- `docs/plan.md` - YanHH3D implementation history and current caveats.
- `docs/YanHH3D_CloudStream_Plugin_PRD.md` - original YanHH3D product requirements.
- `docs/doi-domain-trong-2-phut.md` - domain-change procedure.
- `CHANGELOG.md` - provider release notes.

Before editing, run `git status --short` and preserve unrelated user changes. In particular, never stage an already modified workflow or generated artifact unless it is explicitly part of the requested change.
