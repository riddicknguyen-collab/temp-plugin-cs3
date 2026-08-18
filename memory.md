# Project Memory

This file is the long-term working memory for the YanHH3D CloudStream plugin project. Update it at the end of every meaningful session so the next session can resume without rediscovering the same context.

## Current Snapshot

- Project root: `D:\Github-myrepo\temp-plugin-cs3`
- Project type: CloudStream 3 plugin repository template.
- Target provider: `YanHH3D`.
- Target implementation module: `YanHHProvider`.
- Reference/sample module: `ExampleProvider`.
- Main PRD: `docs/YanHH3D_CloudStream_Plugin_PRD.md`.
- Main implementation plan: `docs/plan.md`.
- Agent instructions: `AGENTS.md`.
- `YanHHProvider/` exists but is currently empty; it becomes a Gradle module only after adding `YanHHProvider/build.gradle.kts`.

## Completed Work

- Read and summarized the YanHH3D PRD.
- Created the first implementation plan, then moved/reviewed it as `docs/plan.md` after the CloudStream sample project was added.
- Created `AGENTS.md` with:
  - project goal,
  - folder-by-folder comments,
  - implementation rules,
  - expected `YanHHProvider` layout,
  - build/test commands.
- Reviewed repo structure after sample project update:
  - `.github/workflows/build.yml` exists for build publishing.
  - `ExampleProvider/` is the working CloudStream sample module.
  - `settings.gradle.kts` auto-includes top-level folders that contain `build.gradle.kts`.
  - root `build.gradle.kts` applies Android, Kotlin, CloudStream, Jsoup, NiceHttp, and Jackson dependencies to subprojects.

## In Progress

- Documentation setup and project orientation.
- No YanHH3D Kotlin implementation has been created yet.
- No Gradle build verification has been run after adding documentation-only files.

## Next Recommended Steps

1. Run `.\gradlew.bat ExampleProvider:make` to confirm the sample template builds locally.
2. Create `YanHHProvider/build.gradle.kts` using `ExampleProvider/build.gradle.kts` as the local template.
3. Add minimal `YanHH3DPlugin.kt` and `YanHH3DProvider.kt`.
4. Run `.\gradlew.bat tasks --all` and verify `YanHHProvider:make` appears.
5. Run `.\gradlew.bat YanHHProvider:make`.
6. Continue with `docs/plan.md` Phase 2 onward.

## Key Technical Decisions

- Keep `ExampleProvider` untouched as a reference module.
- Implement production code under `YanHHProvider`, not `YanHH3D`.
- Store YanHH3D domain values in a constants/resolver file, not scattered through provider code.
- Store CSS selectors in one constants object.
- Keep parser code pure Jsoup with no network calls.
- Keep provider code responsible for CloudStream API integration, HTTP requests, and model mapping.
- Use local HTML fixtures for parser regression tests.
- Store path-only or remappable URLs where practical so old YanHH3D domains can be mapped to the current domain.
- Return all valid HLS sources from `loadLinks()` instead of selecting only one.
- Add `/stream/m3u8/<file>` fallback only after direct `.m3u8` playback is proven to fail.

## Guardrails

- Do not use WebView.
- Do not use browser automation for scraping.
- Do not bypass CAPTCHA, DRM, login, paywall, geo restriction, or anti-bot systems.
- Do not harvest cookies.
- Do not download, mirror, upload, host, or rehost media.
- Do not log cookies, tokens, or sensitive headers.
- Keep the project private/personal unless a separate compliance review is done.

## Build Commands

Use PowerShell commands in this workspace:

```powershell
.\gradlew.bat tasks --all
.\gradlew.bat ExampleProvider:make
.\gradlew.bat YanHHProvider:make
.\gradlew.bat makePluginsJson
```

`YanHHProvider:make` will not exist until `YanHHProvider/build.gradle.kts` is created.

## Repository Notes

- `settings.gradle.kts` automatically includes every top-level directory with a `build.gradle.kts`.
- `.github/workflows/build.yml` builds all plugins with `./gradlew make makePluginsJson` and copies `**/build/*.cs3` plus `build/plugins.json` to the `builds` checkout.
- The workflow expects a `builds` branch to exist.
- `README.md` is still the upstream CloudStream plugin template readme.

## Open Questions

- Does the active YanHH3D domain still use `.flw-item` for list cards?
- Does the active episode page still expose playable URLs in `data-src`?
- Does direct `.m3u8` playback work with only `Referer` and `User-Agent`, or is `Origin` also required?
- Does the current CloudStream dependency expose `newExtractorLink()` or require direct `ExtractorLink` construction?
- Does the current CloudStream build include an Abyss extractor?

## Session Update Protocol

At the end of each session:

1. Move completed items from `In Progress` or `Next Recommended Steps` into `Completed Work`.
2. Update `Current Snapshot` if folders, module names, branch strategy, or important file paths changed.
3. Add any new technical decisions to `Key Technical Decisions`.
4. Add any new risks or constraints to `Guardrails` or `Open Questions`.
5. Record failed commands and their cause if they affect the next session.
6. Keep this file concise; prefer durable facts over chat history.
