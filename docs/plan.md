# YanHH3D CloudStream Plugin Implementation Plan

> **For agentic workers:** Implement this plan task by task. Checkboxes are intentionally granular so progress can be resumed safely.

**Goal:** Implement a private CloudStream 3 Kotlin provider for YanHH3D in the existing CloudStream plugin template repo.

**Current Repo Reality:** The project already contains the CloudStream plugin template, Gradle wrapper, GitHub build workflow, and a working `ExampleProvider` sample module. The target folder `YanHHProvider` exists but is currently empty, so the first real implementation step is turning it into a Gradle module by adding `YanHHProvider/build.gradle.kts`.

**Architecture:** Keep `ExampleProvider` untouched as the reference module. Implement YanHH3D under `YanHHProvider` using focused Kotlin files: plugin entrypoint, provider, pure parser, domain resolver, constants, and models. Parser behavior should be covered by local HTML fixtures before wiring it into provider network calls.

**Tech Stack:** Kotlin, Gradle, Android library plugin, CloudStream 3 extension API, Jsoup, NiceHttp, GitHub Actions.

---

## Phase 0: Baseline Review

**Purpose:** Confirm the template state and avoid overwriting user-added files.

**Files:**
- Read: `AGENTS.md`
- Read: `docs/YanHH3D_CloudStream_Plugin_PRD.md`
- Read: `settings.gradle.kts`
- Read: `build.gradle.kts`
- Read: `ExampleProvider/build.gradle.kts`
- Read: `ExampleProvider/src/main/kotlin/com/example/ExamplePlugin.kt`
- Read: `ExampleProvider/src/main/kotlin/com/example/ExampleProvider.kt`

- [ ] Run `git status --short`.
- [ ] Run `rg --files`.
- [ ] Confirm `ExampleProvider:make` exists with `.\gradlew.bat tasks --all`.
- [ ] Build the sample with `.\gradlew.bat ExampleProvider:make`.
- [ ] Do not commit or modify unrelated template files during this phase.

**Done when:** The sample module builds or any template build issue is documented before YanHH3D work starts.

---

## Phase 1: Create YanHHProvider Module

**Purpose:** Make `YanHHProvider` a real CloudStream module using the existing template conventions.

**Files:**
- Create: `YanHHProvider/build.gradle.kts`
- Create: `YanHHProvider/src/main/AndroidManifest.xml`
- Create: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DPlugin.kt`
- Create: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DProvider.kt`

- [ ] Copy the minimal module shape from `ExampleProvider`.
- [ ] Create `YanHHProvider/build.gradle.kts` with `version = 1` and CloudStream metadata:

```kotlin
version = 1

cloudstream {
    description = "YanHH3D provider for a private CloudStream repo"
    authors = listOf("personal")
    status = 1
    tvTypes = listOf("TvSeries", "Movie")
    requiresResources = false
    language = "vi"
    iconUrl = "https://yanhh3d.love/favicon.ico"
}
```

- [ ] Create `AndroidManifest.xml` with the same minimal manifest style as the sample.
- [ ] Create `YanHH3DPlugin.kt` that registers `YanHH3DProvider()`.
- [ ] Create a minimal `YanHH3DProvider : MainAPI()` with name, lang, `mainUrl`, `hasMainPage`, and supported types.
- [ ] Run `.\gradlew.bat tasks --all` and confirm `YanHHProvider:make` appears.
- [ ] Run `.\gradlew.bat YanHHProvider:make`.

**Done when:** `YanHHProvider:make` builds an empty provider successfully.

---

## Phase 2: Constants, Models, Domain Resolver

**Purpose:** Centralize values that will change when YanHH3D changes domain or HTML.

**Files:**
- Create: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DConstants.kt`
- Create: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DModels.kt`
- Create: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DDomainResolver.kt`
- Modify: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DProvider.kt`

- [ ] Add `DEFAULT_BASE_URL = "https://yanhh3d.love"`.
- [ ] Add known domains: `yanhh3d.love`, `yanhh3d.ac`.
- [ ] Add desktop Chrome `USER_AGENT`.
- [ ] Add main pages from the PRD: `/moi-cap-nhat` and the eight `/the-loai/...` categories.
- [ ] Add selectors: `.flw-item`, `.detail-infor-content`, `div[class*=list-severs] a[data-src]`, canonical and OG meta selectors.
- [ ] Add internal models: `YanMovieItem`, `YanDetail`, `YanEpisode`, `YanSource`, `YanSourceType`.
- [ ] Add resolver methods: `absoluteUrl()`, `remapKnownDomain()`, `normalizeInternalData()`.
- [ ] Update provider `mainUrl` to use the resolver.
- [ ] Run `rg "yanhh3d\.love|yanhh3d\.ac" YanHHProvider` and verify domain literals only appear in constants and optional metadata.
- [ ] Run `.\gradlew.bat YanHHProvider:make`.

**Done when:** Domain, selectors, headers, categories, and models are isolated from provider logic.

---

## Phase 3: Parser Fixtures And Tests

**Purpose:** Lock parser expectations before implementation.

**Files:**
- Create: `YanHHProvider/src/test/kotlin/com/yanhh3d/YanHH3DParserTest.kt`
- Create: `YanHHProvider/src/test/resources/yanhh3d/home.html`
- Create: `YanHHProvider/src/test/resources/yanhh3d/detail.html`
- Create: `YanHHProvider/src/test/resources/yanhh3d/episode.html`
- Modify: `YanHHProvider/build.gradle.kts` if test dependencies are needed

- [ ] Add a home/list fixture with valid `.flw-item` cards, one old-domain URL, one missing poster, and one invalid empty `href`.
- [ ] Add a detail fixture with canonical URL, OG title/image/description, year/status/genre text, server tabs, duplicate episodes, numeric episodes, and a special episode.
- [ ] Add an episode fixture with 1080 HLS, 4K HLS, Abyss embed, empty `data-src`, and unknown non-HLS URL.
- [ ] Add tests for list parsing, detail parsing, episode sorting/deduplication, source classification, and quality detection.
- [ ] Run `.\gradlew.bat YanHHProvider:test`.

**Expected first run:** Tests fail because `YanHH3DParser` is not implemented yet.

**Done when:** Fixtures and failing tests clearly describe the parser contract.

---

## Phase 4: Pure Jsoup Parser

**Purpose:** Implement all HTML parsing with no network calls.

**Files:**
- Create: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DParser.kt`
- Modify: `YanHHProvider/src/test/kotlin/com/yanhh3d/YanHH3DParserTest.kt` only if CloudStream test constants differ

- [ ] Implement `parseList(document)` using `.flw-item`.
- [ ] Skip cards with missing/blank `href`.
- [ ] Use `a[title]` first and `a.text()` as title fallback.
- [ ] Normalize relative poster and page URLs through the domain resolver.
- [ ] Implement `parseDetail(document, inputUrl)` using canonical/OG fallback rules.
- [ ] Parse year, status, and genres from visible detail text without force unwraps.
- [ ] Implement `parseEpisodes(document)` using `.detail-infor-content`, server tab IDs, dedupe by URL, and numeric sort.
- [ ] Do not create duplicate 4K episodes in v1.
- [ ] Implement `parseSources(document)` using `data-src`.
- [ ] Classify `.m3u8` as HLS and Abyss/embed/player-like URLs as embed.
- [ ] Detect quality: 4K/2160, 1080, 720, 480, unknown.
- [ ] Run `.\gradlew.bat YanHHProvider:test`.
- [ ] Run `.\gradlew.bat YanHHProvider:make`.

**Done when:** Parser tests and module build pass.

---

## Phase 5: Main Page And Search

**Purpose:** Wire list parsing into CloudStream home/category/search flows.

**Files:**
- Modify: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DProvider.kt`

- [ ] Add `mainPage` entries from `YanHH3DConstants.MAIN_PAGES`.
- [ ] Implement `getMainPage(page, request)`:
  - page 1 uses the raw path.
  - page greater than 1 appends `?page=<page>`.
  - network requests use `User-Agent` and `Referer`.
  - list responses reuse `parser.parseList()`.
- [ ] Implement `search(query)`:
  - blank query returns `emptyList()`.
  - nonblank query uses `/search?keysearch=<UTF-8 encoded query>`.
  - results reuse `parser.parseList()`.
- [ ] Map list items to `newTvSeriesSearchResponse`.
- [ ] Catch failures and return empty results.
- [ ] Run `.\gradlew.bat YanHHProvider:make`.

**Done when:** Home, categories, and search compile against the current CloudStream API.

---

## Phase 6: Detail Load And Episode Mapping

**Purpose:** Show title metadata and playable episode list in CloudStream.

**Files:**
- Modify: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DProvider.kt`

- [ ] Implement `load(url)`.
- [ ] Resolve path-only and old-domain URLs through `YanHH3DDomainResolver`.
- [ ] Fetch the detail page with default headers.
- [ ] Convert `YanDetail` to `newTvSeriesLoadResponse`.
- [ ] Map `YanEpisode.url` to CloudStream episode `data`.
- [ ] Map `YanEpisode.name` and `episodeNumber`.
- [ ] Set poster, plot, year, and tags when present.
- [ ] Catch failures and return `null`.
- [ ] Run `.\gradlew.bat YanHHProvider:make`.

**Done when:** Detail load compiles and uses parser output rather than inline selectors.

---

## Phase 7: Video Source Loading

**Purpose:** Emit all playable direct HLS sources and delegate embed links.

**Files:**
- Modify: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DProvider.kt`
- Optional Modify: `YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DParser.kt`

- [ ] Implement `loadLinks(data, isCasting, subtitleCallback, callback)`.
- [ ] Resolve the episode URL through the domain resolver.
- [ ] Fetch the episode page with default headers.
- [ ] Use `parser.parseSources(document)`.
- [ ] For every HLS source, emit an `ExtractorLink` or `newExtractorLink` using the current CloudStream API.
- [ ] Include `Referer` and `User-Agent` headers on every direct HLS link.
- [ ] For embed sources, call `loadExtractor(source.url, referer, subtitleCallback, callback)`.
- [ ] Return `true` if at least one HLS is emitted or one embed is delegated.
- [ ] Return `false` on empty source list or exception.
- [ ] Only add `/stream/m3u8/<file>` fallback after real playback proves the original `.m3u8` needs it.
- [ ] Run `.\gradlew.bat YanHHProvider:make`.

**Done when:** `loadLinks()` returns all valid sources without throwing.

---

## Phase 8: Packaging And Repo Metadata

**Purpose:** Produce installable private CloudStream artifacts.

**Files:**
- Create: `repo.json`
- Optional Create: `CHANGELOG.md`
- Generated: `YanHHProvider/build/*.cs3`
- Generated: `build/plugins.json`

- [ ] Create root `repo.json` with a private `builds` branch plugin list URL.
- [ ] Replace `OWNER/REPO` before publishing.
- [ ] Add `CHANGELOG.md` with v1 notes.
- [ ] Run `.\gradlew.bat YanHHProvider:make`.
- [ ] Run `.\gradlew.bat makePluginsJson`.
- [ ] Validate JSON:

```powershell
Get-Content -Raw repo.json | ConvertFrom-Json | Out-Null
Get-Content -Raw build\plugins.json | ConvertFrom-Json | Out-Null
```

**Done when:** `.cs3`, `plugins.json`, and `repo.json` are valid.

---

## Phase 9: Manual CloudStream Verification

**Purpose:** Verify the provider in the actual app, because CloudStream APIs and live HTML can differ from fixtures.

**Files:**
- Modify parser/provider files only when a real issue is found.
- Add real minimal fixtures under `YanHHProvider/src/test/resources/yanhh3d/` for any fixed selector break.

- [ ] Install with `.\gradlew.bat YanHHProvider:deployWithAdb` or through the private repo URL.
- [ ] Confirm provider `YanHH3D` appears in CloudStream.
- [ ] Open latest page.
- [ ] Open at least three category pages.
- [ ] Search a query with spaces, for example `dau pha`.
- [ ] Open a title and confirm title, poster, description, and episodes.
- [ ] Open first and latest episodes.
- [ ] Confirm at least one HLS source starts playback.
- [ ] Confirm embed source delegation does not crash if such source exists.
- [ ] If a selector fails, save a minimal real HTML fixture, update constants/parser, run tests, then rebuild.

**Done when:** CloudStream can install the plugin and play at least one HLS source.

---

## Phase 10: Release Workflow

**Purpose:** Publish source on `main` and generated artifacts on `builds`.

**Files:**
- `.github/workflows/build.yml`
- `repo.json`
- Generated artifacts copied by workflow

- [ ] Verify `.github/workflows/build.yml` still matches the repo branch names.
- [ ] Ensure `builds` branch exists before relying on the workflow checkout step.
- [ ] Push source to `main`.
- [ ] Let the workflow run `./gradlew make makePluginsJson`.
- [ ] Confirm the workflow copies `**/build/*.cs3` and `build/plugins.json` to `builds`.
- [ ] Add this URL to CloudStream:

```text
https://raw.githubusercontent.com/OWNER/REPO/builds/repo.json
```

**Done when:** CloudStream installs YanHH3D from the private repository URL.

---

## Maintenance Checklist

- [ ] Domain changed: update `DEFAULT_BASE_URL`, add old domain to `KNOWN_DOMAINS`, run tests and build.
- [ ] List cards missing: update `YanHH3DSelectors.MOVIE_ITEM` and list fixture.
- [ ] Detail metadata missing: update canonical/OG/detail parser fixture and parser fallback.
- [ ] Episodes missing: inspect `.detail-infor-content`, tab anchors, server IDs, and episode anchors.
- [ ] Sources missing: inspect `data-src`, `data-url`, `data-link`, `href`, or `src`; update parser only after adding fixture.
- [ ] HLS not playing: verify direct URL, `Referer`, `Origin`, `User-Agent`, and only then consider normalized fallback.
- [ ] Before release: increment `version`, update changelog, run `YanHHProvider:test`, `YanHHProvider:make`, and `makePluginsJson`.

---

## Final Acceptance Checklist

- [ ] `AGENTS.md` reflects the actual repo structure.
- [ ] `ExampleProvider:make` still builds.
- [ ] `YanHHProvider:make` builds.
- [ ] `YanHHProvider` contains plugin, provider, parser, resolver, constants, models, tests, and fixtures.
- [ ] Domain and selectors are centralized.
- [ ] Parser tests pass.
- [ ] Home, categories, search, detail, episodes, HLS, and embed fallback are implemented.
- [ ] No WebView, CAPTCHA bypass, DRM bypass, login bypass, cookie harvesting, downloading, mirroring, or rehosting is added.
- [ ] `.cs3`, `plugins.json`, and `repo.json` are generated and valid.
- [ ] CloudStream installs the private repo and plays at least one HLS source.
