# PRD — YanHH3D CloudStream Plugin cho dự án cá nhân

**Ngày lập tài liệu:** 2026-08-17  
**Mục tiêu:** Vibe-code hoàn chỉnh một plugin/provider YanHH3D dùng riêng trên CloudStream  
**Target runtime:** CloudStream 3 extension  
**Ngôn ngữ triển khai:** Kotlin  
**Kiểu phân phối:** Private/personal CloudStream repository  
**Source nghiên cứu chính:** `youngbi/repo/plugins/yanhh3d_plugin.js`  
**Domain tham chiếu trong source:** `https://yanhh3d.love`  
**Domain cũ xuất hiện trong comment/source:** `https://yanhh3d.ac`

---

## 0. Tóm tắt điều hành

Cần xây dựng một plugin CloudStream tên **YanHH3D** cho dự án cá nhân. Plugin này scrape HTML từ website YanHH3D để hiển thị nội dung trong CloudStream.

Plugin cần hỗ trợ đầy đủ các luồng:

```text
CloudStream
   ↓
YanHH3D provider
   ↓
Trang chủ / danh mục / tìm kiếm
   ↓
Trang chi tiết phim
   ↓
Danh sách tập
   ↓
Trang phát tập
   ↓
Tìm server video
   ↓
m3u8 trực tiếp hoặc embed host
   ↓
CloudStream player
```

Tài liệu này được viết để đưa trực tiếp cho một coding agent / vibe-code agent như Cursor, Codex, Claude Code, Continue, Aider hoặc một lập trình viên Kotlin có kinh nghiệm để triển khai plugin hoàn chỉnh.

---

# 1. Mục tiêu sản phẩm

## 1.1 Mục tiêu chính

Xây dựng một CloudStream provider riêng cho YanHH3D có khả năng:

- Hiển thị danh sách phim mới cập nhật.
- Hiển thị các danh mục chính.
- Tìm kiếm phim.
- Parse metadata phim.
- Parse danh sách tập.
- Parse link video từ trang tập.
- Phát trực tiếp link HLS `.m3u8` khi có.
- Fallback sang extractor host ngoài nếu link là embed được CloudStream hỗ trợ.
- Tự chịu được việc YanHH3D đổi domain.
- Có thể build ra file `.cs3`.
- Có thể publish vào repo CloudStream cá nhân thông qua `repo.json` và `plugins.json`.

## 1.2 Định nghĩa hoàn thành

Dự án được xem là hoàn thành khi:

- Plugin build thành công bằng Gradle.
- File `.cs3` được tạo.
- Provider xuất hiện trong CloudStream.
- Trang chủ load được.
- Ít nhất một danh mục load được.
- Search trả kết quả.
- Mở phim thấy title, poster, mô tả và danh sách tập.
- Tập được sắp xếp theo số.
- Chọn tập gọi được `loadLinks()`.
- `loadLinks()` tìm được link `.m3u8` nếu site có expose.
- Link HLS được gửi vào CloudStream với `Referer` và `User-Agent`.
- Embed host như Abyss được đưa qua `loadExtractor()` nếu CloudStream hỗ trợ.
- Có logging đủ để debug khi selector hỏng.
- Domain có thể đổi ở một config duy nhất.
- Có test fixture HTML để regression test parser.

---

# 2. Phạm vi

## 2.1 Trong phạm vi

- Tạo CloudStream extension/provider bằng Kotlin.
- Scrape HTML YanHH3D.
- Main page.
- Category page.
- Search.
- Detail page.
- Episode parsing.
- Video source parsing.
- HLS `.m3u8`.
- Embed fallback.
- Header handling.
- Domain resolver.
- Error handling.
- Build `.cs3`.
- Private repo JSON.
- Tài liệu bảo trì.
- Test dữ liệu HTML snapshot.

## 2.2 Ngoài phạm vi phiên bản đầu

Không làm trong v1:

- CAPTCHA bypass.
- DRM bypass.
- Login/account scraping.
- Cookie harvesting.
- Browser automation.
- WebView automation.
- Deobfuscation phức tạp nếu site chưa yêu cầu.
- Tải xuống video.
- Rehost nội dung.
- Public distribution.
- Scrape các site không liên quan.

Nếu YanHH3D thay đổi sang cơ chế có CAPTCHA, login, DRM hoặc anti-bot mạnh, cần tạo PRD mới cho giai đoạn đó.

---

# 3. Cơ sở nghiên cứu từ source YanHH3D JS

Source tham chiếu:

```text
https://github.com/youngbi/repo/blob/main/plugins/yanhh3d_plugin.js
```

Các thông tin quan trọng trong source:

## 3.1 Base URL

Source khai báo:

```javascript
BASEURL = "https://yanhh3d.love";
```

Trong comment có dấu vết:

```text
https://yanhh3d.ac/moi-cap-nhat?page=2
```

Suy luận triển khai:

- Domain YanHH3D có thể thay đổi.
- Không hardcode domain rải rác.
- Mọi URL runtime phải đi qua một domain resolver hoặc helper `absoluteUrl()`.

## 3.2 Home section

Source dùng:

```text
/moi-cap-nhat
```

Tên hiển thị:

```text
Phim Mới
```

Pagination:

```text
/moi-cap-nhat?page=2
```

## 3.3 Category menu

Source hardcode các danh mục:

```text
/the-loai/huyen-huyen@@Huyền Huyễn
/the-loai/xuyen-khong@@Xuyên Không
/the-loai/trung-sinh@@Trùng Sinh
/the-loai/tien-hiep@@Tiên Hiệp
/the-loai/co-trang@@Cổ Trang
/the-loai/hai-huoc@@Hài Hước
/the-loai/kiem-hiep@@Kiếm Hiệp
/the-loai/hien-dai@@Hiện Đại
```

## 3.4 Search

Source dựng URL search:

```text
/search?keysearch=<keyword>
```

Nếu có phân trang:

```text
/search?keysearch=<keyword>&page=<page>
```

## 3.5 List parser

Source parse list bằng selector:

```css
.flw-item
```

Trong mỗi item lấy:

```text
a[href]       -> URL phim
a[title]      -> title
img[src]      -> poster
.tick-rate    -> tập hiện tại / current label
.tick-dub     -> quality / dub label
```

## 3.6 Detail parser

Source detail lấy canonical ID từ:

```html
<link rel="canonical" href="...">
```

Fallback:

```html
<meta property="og:url" content="...">
```

Metadata:

```text
meta[property="og:url"]
meta[property="og:image"]
meta[property="og:title"]
meta[property="og:description"]
meta[property="video:duration"]
```

Các field lấy theo text tiếng Việt:

```text
Trạng thái:
Năm:
Thể loại:
Tập mới nhất:
```

## 3.7 Episode parser

Source dùng container:

```css
.detail-infor-content
```

Quy trình:

```text
.detail-infor-content
   ↓
li a[href] server/tab
   ↓
href của server tab, ví dụ #server-1
   ↓
parent.find(idserver).find("a")
   ↓
a[href] của từng tập
   ↓
div.text() làm tên tập
```

Source cũng tạo thêm phiên bản 4K bằng cách append:

```text
?type=4k
```

vào URL tập. Với CloudStream provider Kotlin, không bắt buộc tạo tập 4K riêng; tốt hơn là trả nhiều source trong `loadLinks()`.

## 3.8 Video parser

Source parse trang phát bằng selector:

```css
div[class*="list-severs"] a
```

Với mỗi link server:

```text
text()       -> tên server / quality
data-src     -> video URL hoặc embed URL
```

Phân loại:

```text
name chứa 4k + link .m3u8      -> 4K HLS
name chứa 1080 + link .m3u8    -> 1080 HLS
link .m3u8 khác                -> HLS khác
link chứa abyss                -> embed fallback
```

Source hiện ưu tiên:

```text
HD -> 4K -> any m3u8 -> Abyss
```

Nếu URL có `type=4k`:

```text
4K -> HD -> any m3u8 -> Abyss
```

Trong CloudStream, không nên chỉ chọn một link. Nên trả tất cả source hợp lệ để app/player tự chọn quality.

## 3.9 Header video

Source trả headers:

```text
Referer: BASEURL
User-Agent: Mozilla/5.0 ... Chrome/120 ...
```

Provider Kotlin phải giữ headers này cho HLS.

## 3.10 HLS URL normalization

Source JS có logic chuyển:

```text
https://HOST/some/path/file.m3u8
```

thành:

```text
https://HOST/stream/m3u8/file.m3u8
```

Không nên áp dụng mù quáng. Thiết kế đúng:

```text
1. Dùng URL gốc từ data-src.
2. Nếu phát lỗi hoặc host cần rewrite:
   dùng fallback /stream/m3u8/<filename>.
```

---

# 4. Người dùng mục tiêu

## 4.1 Người dùng chính

- Chủ dự án cá nhân.
- Có CloudStream trên Android hoặc Android TV.
- Muốn thêm nguồn YanHH3D vào repo riêng.
- Không cần public plugin.

## 4.2 Người dùng phụ

- Lập trình viên Kotlin nhận tài liệu để triển khai.
- Coding agent được giao nhiệm vụ tạo code.
- Người bảo trì sau này khi YanHH3D đổi domain/HTML.

---

# 5. Yêu cầu chức năng

## 5.1 Provider metadata

Provider phải khai báo:

```kotlin
override var name = "YanHH3D"
override var lang = "vi"
override val hasMainPage = true
override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)
```

Ưu tiên `TvType.TvSeries` vì YanHH3D chủ yếu là hoạt hình nhiều tập.

## 5.2 Plugin entrypoint

Tạo plugin class:

```kotlin
@CloudstreamPlugin
class YanHH3DPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(YanHH3DProvider())
    }
}
```

Yêu cầu:

- Provider chỉ được register một lần.
- Không crash khi load plugin.
- Không yêu cầu resource Android nếu không cần.

## 5.3 Main page

Main page phải có ít nhất:

```text
Mới cập nhật -> /moi-cap-nhat
```

Các category:

```text
Huyền Huyễn -> /the-loai/huyen-huyen
Xuyên Không -> /the-loai/xuyen-khong
Trùng Sinh -> /the-loai/trung-sinh
Tiên Hiệp -> /the-loai/tien-hiep
Cổ Trang -> /the-loai/co-trang
Hài Hước -> /the-loai/hai-huoc
Kiếm Hiệp -> /the-loai/kiem-hiep
Hiện Đại -> /the-loai/hien-dai
```

Pagination:

```text
page == 1:
  https://domain/path

page > 1:
  https://domain/path?page=page
```

## 5.4 List response

Từ mỗi `.flw-item`, tạo `SearchResponse`:

Mapping:

```text
title       = a[title] hoặc a.text()
url/data    = href absolute URL hoặc normalized path
posterUrl   = img[src]
quality     = tick-dub/tick-rate nếu phù hợp
```

Yêu cầu:

- Bỏ item thiếu href.
- Bỏ item thiếu title nếu không fallback được.
- Poster relative URL phải chuyển thành absolute URL.
- Không crash nếu `.tick-rate` hoặc `.tick-dub` thiếu.

## 5.5 Search

Endpoint:

```text
/search?keysearch=<query encoded>
```

Yêu cầu:

- Encode query bằng UTF-8.
- Tái sử dụng parser list.
- Không crash nếu query rỗng.
- Nếu query rỗng, trả list rỗng.

## 5.6 Load detail

Từ URL phim:

1. GET detail page.
2. Parse metadata:
   - title
   - canonical URL
   - poster
   - description
   - year
   - category/genre nếu có
   - status nếu có
3. Parse episodes.
4. Trả `LoadResponse`.

Yêu cầu:

- Nếu có episode, trả `newTvSeriesLoadResponse`.
- Nếu không có episode nhưng có play button, vẫn tạo một episode từ play button.
- Nếu thiếu poster/description/year, vẫn load được.
- ID nội bộ không nên phụ thuộc tuyệt đối vào domain.

## 5.7 Episode list

Parse từ `.detail-infor-content`.

Yêu cầu:

- Lấy tất cả server tab.
- Lấy tất cả episode anchor.
- Mỗi episode cần có:
  - data URL
  - name
  - episode number nếu parse được
- Deduplicate episode theo URL.
- Sort theo số tập tăng dần.
- Nếu không parse được số, giữ ở cuối hoặc giữ thứ tự gốc.
- Không tạo duplicate 4K episode trong v1; xử lý 4K ở `loadLinks()`.

## 5.8 Load video links

Từ URL tập:

1. GET episode page.
2. Tìm:

```css
div[class*="list-severs"] a[data-src]
```

3. Với mỗi anchor:
   - name = text
   - sourceUrl = attr `data-src`
4. Nếu sourceUrl là `.m3u8`, trả `ExtractorLink`.
5. Nếu sourceUrl là embed host được CloudStream hỗ trợ, gọi `loadExtractor()`.
6. Gắn headers.

Yêu cầu:

- Trả tất cả source hợp lệ.
- Quality detection:
  - 4K/2160p -> `P2160`
  - 1080 -> `P1080`
  - 720 -> `P720`
  - 480 -> `P480`
  - fallback -> `Unknown`
- Nếu không có source, return `false`.
- Nếu có ít nhất một source, return `true`.
- Không throw exception ra ngoài.

---

# 6. Yêu cầu phi chức năng

## 6.1 Bền vững trước đổi domain

Domain phải được tập trung tại:

```text
YanHH3DConstants.kt
YanHH3DDomainResolver.kt
```

Không hardcode `yanhh3d.love` ở nhiều file.

Yêu cầu:

- Có list candidate domains.
- Có current domain mặc định.
- Có hàm `absoluteUrl()`.
- Có hàm remap URL domain cũ sang domain hiện tại.
- Có thể đổi domain bằng một biến duy nhất.

## 6.2 Bền vững trước đổi HTML

Selector phải gom vào constants:

```kotlin
object YanHH3DSelectors {
    const val MOVIE_ITEM = ".flw-item"
    const val DETAIL_CONTAINER = ".detail-infor-content"
    const val SERVER_LINK = "div[class*=list-severs] a[data-src]"
    const val CANONICAL = "link[rel=canonical]"
    const val OG_TITLE = "meta[property=og:title]"
    const val OG_IMAGE = "meta[property=og:image]"
    const val OG_DESCRIPTION = "meta[property=og:description]"
}
```

Yêu cầu:

- Không viết selector rải rác.
- Parser phải chịu được field thiếu.
- Có test fixture để khi selector hỏng có thể sửa nhanh.

## 6.3 Logging

Có logging dạng prefix:

```text
[YanHH3D] resolveDomain
[YanHH3D] getMainPage url=
[YanHH3D] parseList count=
[YanHH3D] load detail url=
[YanHH3D] parseEpisodes count=
[YanHH3D] loadLinks url=
[YanHH3D] source count=
[YanHH3D] hls source=
[YanHH3D] embed source=
```

Không log:

- Cookie.
- Token.
- Toàn bộ header nhạy cảm.
- Dữ liệu người dùng không cần thiết.

## 6.4 Performance

- Không request detail pages khi parse list.
- Không gọi network song song quá nhiều trong list.
- `loadLinks()` chỉ request trang tập khi người dùng chọn tập.
- Parser chạy local bằng Jsoup.
- Không dùng WebView.

## 6.5 Error handling

Mọi method public override phải có `runCatching` hoặc try/catch hợp lý:

```kotlin
override suspend fun search(query: String): List<SearchResponse> {
    return runCatching {
        ...
    }.getOrElse {
        logError("search failed", it)
        emptyList()
    }
}
```

Không dùng `!!` với HTML selector.

## 6.6 Legal/compliance guardrails

Plugin chỉ phục vụ dự án cá nhân. Không thiết kế để bypass:

- DRM
- CAPTCHA
- paywall
- login
- geo restriction
- anti-bot protection

Plugin không host, upload, rehost hoặc download nội dung. Nó chỉ tích hợp các URL mà trang nguồn expose công khai cho trình duyệt.

---

# 7. Kiến trúc kỹ thuật

## 7.1 Module

Tên module:

```text
YanHH3D
```

## 7.2 File chính

```text
YanHH3DPlugin.kt
YanHH3DProvider.kt
YanHH3DParser.kt
YanHH3DDomainResolver.kt
YanHH3DModels.kt
YanHH3DConstants.kt
```

## 7.3 Trách nhiệm từng file

### `YanHH3DPlugin.kt`

- Entry point.
- Register provider.

### `YanHH3DProvider.kt`

- Implement CloudStream `MainAPI`.
- Gọi network.
- Gọi parser.
- Convert parser model sang CloudStream model.
- Gọi `loadExtractor()` khi cần.

### `YanHH3DParser.kt`

Pure parser:

- `parseList(document)`
- `parseDetail(document, url)`
- `parseEpisodes(document)`
- `parseSources(document)`
- `parseYear(document)`
- `parseQuality(name, url)`

Không chứa network.

### `YanHH3DDomainResolver.kt`

- Quản lý base URL.
- Remap domain cũ.
- Health check domain nếu cần.
- `absoluteUrl()`.
- `normalizeInternalData()`.

### `YanHH3DModels.kt`

Data class nội bộ:

```kotlin
data class YanMovieItem(
    val title: String,
    val url: String,
    val posterUrl: String?,
    val currentEpisode: String?,
    val qualityLabel: String?
)

data class YanDetail(
    val title: String,
    val url: String,
    val posterUrl: String?,
    val description: String?,
    val year: Int?,
    val status: String?,
    val genres: List<String>,
    val episodes: List<YanEpisode>
)

data class YanEpisode(
    val name: String,
    val url: String,
    val episodeNumber: Int?
)

data class YanSource(
    val name: String,
    val url: String,
    val type: YanSourceType,
    val quality: Int
)

enum class YanSourceType {
    HLS,
    EMBED,
    UNKNOWN
}
```

### `YanHH3DConstants.kt`

- User agent.
- Domain list.
- Selectors.
- Main page categories.
- Regex.

---

# 8. Data model và mapping CloudStream

## 8.1 List item -> SearchResponse

```text
YanMovieItem.title      -> SearchResponse.name
YanMovieItem.url        -> SearchResponse.url/data
YanMovieItem.posterUrl  -> SearchResponse.posterUrl
YanMovieItem.quality    -> SearchResponse.quality nếu mapping được
```

CloudStream helper ưu tiên:

```kotlin
newTvSeriesSearchResponse(...)
```

hoặc:

```kotlin
newMovieSearchResponse(...)
```

Nếu chưa chắc nội dung là movie hay series, dùng series cho YanHH3D.

## 8.2 Detail -> LoadResponse

Nếu có episodes:

```kotlin
newTvSeriesLoadResponse(
    name = title,
    url = canonicalOrInputUrl,
    type = TvType.TvSeries,
    episodes = episodes
)
```

Nếu không có episodes nhưng có play URL:

```kotlin
newMovieLoadResponse(...)
```

Tuy nhiên v1 nên ưu tiên series vì site là hoạt hình nhiều tập.

## 8.3 Episode -> CloudStream Episode

```text
YanEpisode.url            -> Episode.data
YanEpisode.name           -> Episode.name
YanEpisode.episodeNumber  -> Episode.episode
```

## 8.4 Source -> ExtractorLink

HLS:

```text
YanSource.url      -> ExtractorLink.url
YanSource.name     -> ExtractorLink.name
YanSource.quality  -> ExtractorLink.quality
headers            -> Referer/User-Agent
type               -> M3U8
```

Embed:

```text
loadExtractor(source.url, referer, subtitleCallback, callback)
```

---

# 9. URL handling

## 9.1 Absolute URL

Function:

```kotlin
fun absoluteUrl(input: String): String
```

Rules:

```text
input starts with http:
  return remapKnownDomain(input)

input starts with /:
  return mainUrl + input

else:
  return mainUrl + "/" + input
```

## 9.2 Remap known YanHH3D domain

Known domains:

```kotlin
listOf(
    "yanhh3d.love",
    "yanhh3d.ac"
)
```

If stored URL starts with old domain, replace host with current `mainUrl`.

## 9.3 Internal episode data

Preferred:

```text
store path-only URLs when possible
```

Example:

```text
/phim/example/tap-1
```

instead of:

```text
https://yanhh3d.love/phim/example/tap-1
```

This avoids breaking CloudStream watch history and bookmarks when domain changes.

---

# 10. HLS URL strategy

## 10.1 Direct URL first

For every `.m3u8` from `data-src`, first register original URL.

```text
data-src original
   ↓
ExtractorLink original
```

## 10.2 Optional normalized fallback

Also optionally register normalized fallback if URL matches:

```regex
(https?://[^/]+)/.+?/([^/]+\.m3u8)(?:\?.*)?$
```

Convert to:

```text
$host/stream/m3u8/$file
```

Only do this if:

- URL differs from original.
- It does not create duplicate.
- Name marks it as fallback, e.g. `YanHH3D 1080p Fallback`.

## 10.3 Avoid duplicate source spam

Use `distinctBy { url }`.

---

# 11. Build system

## 11.1 Recommended starting point

Use the official-style CloudStream plugin template:

```text
https://github.com/recloudstream/TestPlugins
```

If the template has changed, preserve its current Gradle conventions.

## 11.2 Module Gradle config

Expected `YanHH3D/build.gradle.kts` concept:

```kotlin
version = 1

cloudstream {
    language = "vi"
    authors = listOf("personal")
    description = "YanHH3D provider for personal CloudStream repo"
    status = 1
    tvTypes = listOf("TvSeries", "Movie")
    iconUrl = "https://yanhh3d.love/favicon.ico"
}
```

Adjust fields to match the current template API.

## 11.3 Build command

Linux/macOS:

```bash
./gradlew YanHH3D:make
```

Windows:

```powershell
.\gradlew.bat YanHH3D:make
```

Generate plugin list if needed:

```bash
./gradlew makePluginsJson
```

Deploy by ADB if supported:

```bash
./gradlew YanHH3D:deployWithAdb
```

---

# 12. Private repository distribution

## 12.1 `repo.json`

Create root-level `repo.json`:

```json
{
  "name": "Personal CloudStream Repo",
  "description": "Private CloudStream plugins for personal use",
  "manifestVersion": 1,
  "pluginLists": [
    "https://raw.githubusercontent.com/OWNER/REPO/builds/plugins.json"
  ]
}
```

Replace:

```text
OWNER
REPO
```

with personal GitHub account/repo.

## 12.2 Branch strategy

Recommended:

```text
main    -> source code
builds  -> compiled .cs3 + plugins.json + repo.json
```

## 12.3 GitHub Actions

Use template workflow if available. Required output:

```text
builds/YanHH3D.cs3
builds/plugins.json
builds/repo.json
```

## 12.4 CloudStream install flow

In CloudStream:

```text
Settings
  ↓
Extensions
  ↓
Add Repository
  ↓
paste repo.json raw URL
  ↓
install YanHH3D
```

---

# 13. Detailed implementation plan for vibe-code agent

## 13.1 Agent instruction

Use this prompt for the coding agent:

```text
You are implementing a private CloudStream 3 plugin named YanHH3D.

Use the current CloudStream TestPlugins-style template. Create a module named YanHH3D. Implement a Kotlin provider that scrapes YanHH3D HTML pages according to this PRD.

Do not use browser automation, CAPTCHA bypass, DRM bypass, login bypass, or WebView scraping. Only parse normal HTTP HTML responses and public attributes already present in the page.

Implement:
- YanHH3DPlugin.kt
- YanHH3DProvider.kt
- YanHH3DParser.kt
- YanHH3DDomainResolver.kt
- YanHH3DModels.kt
- YanHH3DConstants.kt
- unit tests for parser using HTML fixtures

Core selectors:
- .flw-item
- .detail-infor-content
- div[class*="list-severs"] a[data-src]
- link[rel=canonical]
- meta[property=og:url]
- meta[property=og:image]
- meta[property=og:title]
- meta[property=og:description]
- meta[property=video:duration]

Core routes:
- /moi-cap-nhat
- /moi-cap-nhat?page=N
- /search?keysearch=<encoded query>
- /the-loai/huyen-huyen
- /the-loai/xuyen-khong
- /the-loai/trung-sinh
- /the-loai/tien-hiep
- /the-loai/co-trang
- /the-loai/hai-huoc
- /the-loai/kiem-hiep
- /the-loai/hien-dai

Base domain must be configurable. Default to https://yanhh3d.love. Include https://yanhh3d.ac as old-domain remap candidate.

Return all HLS sources found in data-src. Detect quality from source name and URL. Pass embed links through loadExtractor() when they are not HLS and look supported.

Preserve Referer and User-Agent headers for HLS links.

Run Gradle build and fix compile errors. If CloudStream APIs differ from the sample names, adapt to the current imported API while preserving behavior.
```

## 13.2 Agent task breakdown

### Task 1 — Scaffold

- Fork/copy CloudStream plugin template.
- Add module `YanHH3D`.
- Register module in settings if required.
- Add Gradle config.
- Ensure empty plugin builds.

Acceptance:

```text
./gradlew YanHH3D:make
```

passes.

### Task 2 — Plugin entrypoint

Create `YanHH3DPlugin.kt`.

Acceptance:

- `.cs3` contains manifest.
- Plugin loads in CloudStream.
- Provider is visible.

### Task 3 — Constants and domain resolver

Create constants:

```kotlin
object YanHH3DConstants {
    const val DEFAULT_BASE_URL = "https://yanhh3d.love"
    const val USER_AGENT = "Mozilla/5.0 ..."
}
```

Create selectors object.

Create domain resolver.

Acceptance:

- All URL creation goes through resolver.
- No repeated literal `yanhh3d.love` outside constants.

### Task 4 — Parser list

Implement parser for `.flw-item`.

Acceptance:

- HTML fixture with multiple `.flw-item` returns matching item count.
- Missing poster does not crash.
- Relative image URL becomes absolute.

### Task 5 — Main page

Implement `getMainPage()`.

Acceptance:

- `/moi-cap-nhat` loads.
- page 2 adds `?page=2`.
- Category pages load.

### Task 6 — Search

Implement `search()`.

Acceptance:

- Query `dau pha` builds `/search?keysearch=dau+pha` or equivalent UTF-8 encoding.
- Results parse through list parser.

### Task 7 — Detail parser

Implement metadata parser.

Acceptance:

- Title from `og:title`.
- Poster from `og:image`.
- Description from `og:description`.
- Canonical from `link[rel=canonical]` or `og:url`.
- Year parsed by regex.

### Task 8 — Episode parser

Implement `.detail-infor-content` parser.

Acceptance:

- Server tabs parse.
- Episodes parse.
- Episode numbers sort ascending.
- Duplicate URLs are removed.
- No duplicate 4K episode in v1.

### Task 9 — Source parser

Implement `div[class*="list-severs"] a[data-src]`.

Acceptance:

- 4K `.m3u8` becomes HLS source with 2160p.
- 1080 `.m3u8` becomes HLS source with 1080p.
- generic `.m3u8` becomes HLS source unknown quality.
- Abyss URL becomes EMBED.

### Task 10 — loadLinks

Implement `loadLinks()`.

Acceptance:

- Returns true if at least one callback occurs.
- Direct HLS source includes Referer/User-Agent.
- Embed source calls `loadExtractor()`.
- Exceptions return false, not crash.

### Task 11 — Tests

Add parser tests with fixture HTML.

Acceptance:

- Unit tests pass locally.
- Fixtures include:
  - home/list page
  - search page
  - detail page
  - episode page with HLS links

### Task 12 — Packaging

Build `.cs3`.

Acceptance:

- `.cs3` file exists.
- `plugins.json` generated.
- `repo.json` valid.

---

# 14. Kotlin skeleton

## 14.1 Constants

```kotlin
object YanHH3DConstants {
    const val DEFAULT_BASE_URL = "https://yanhh3d.love"

    val KNOWN_DOMAINS = listOf(
        "yanhh3d.love",
        "yanhh3d.ac"
    )

    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

    val MAIN_PAGES = listOf(
        "/moi-cap-nhat" to "Mới cập nhật",
        "/the-loai/huyen-huyen" to "Huyền Huyễn",
        "/the-loai/xuyen-khong" to "Xuyên Không",
        "/the-loai/trung-sinh" to "Trùng Sinh",
        "/the-loai/tien-hiep" to "Tiên Hiệp",
        "/the-loai/co-trang" to "Cổ Trang",
        "/the-loai/hai-huoc" to "Hài Hước",
        "/the-loai/kiem-hiep" to "Kiếm Hiệp",
        "/the-loai/hien-dai" to "Hiện Đại",
    )
}

object YanHH3DSelectors {
    const val MOVIE_ITEM = ".flw-item"
    const val DETAIL_CONTAINER = ".detail-infor-content"
    const val SERVER_LINK = "div[class*=list-severs] a[data-src]"
    const val CANONICAL = "link[rel=canonical]"
    const val OG_URL = "meta[property=og:url]"
    const val OG_TITLE = "meta[property=og:title]"
    const val OG_IMAGE = "meta[property=og:image]"
    const val OG_DESCRIPTION = "meta[property=og:description]"
    const val VIDEO_DURATION = "meta[property=video:duration]"
}
```

## 14.2 Models

```kotlin
data class YanMovieItem(
    val title: String,
    val url: String,
    val posterUrl: String?,
    val currentEpisode: String?,
    val qualityLabel: String?
)

data class YanDetail(
    val title: String,
    val url: String,
    val posterUrl: String?,
    val description: String?,
    val year: Int?,
    val status: String?,
    val genres: List<String>,
    val episodes: List<YanEpisode>
)

data class YanEpisode(
    val name: String,
    val url: String,
    val episodeNumber: Int?
)

data class YanSource(
    val name: String,
    val url: String,
    val type: YanSourceType,
    val quality: Int
)

enum class YanSourceType {
    HLS,
    EMBED,
    UNKNOWN
}
```

## 14.3 Parser concept

```kotlin
class YanHH3DParser(
    private val domainResolver: YanHH3DDomainResolver
) {
    fun parseList(document: Document): List<YanMovieItem> {
        return document.select(YanHH3DSelectors.MOVIE_ITEM)
            .mapNotNull { item ->
                val anchor = item.selectFirst("a[href]") ?: return@mapNotNull null
                val href = anchor.attr("href").trim()
                if (href.isBlank()) return@mapNotNull null

                val title = anchor.attr("title").ifBlank { anchor.text() }.trim()
                if (title.isBlank()) return@mapNotNull null

                val poster = item.selectFirst("img[src]")
                    ?.attr("src")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { domainResolver.absoluteUrl(it) }

                YanMovieItem(
                    title = title,
                    url = domainResolver.absoluteUrl(href),
                    posterUrl = poster,
                    currentEpisode = item.selectFirst(".tick-rate")?.text()?.trim(),
                    qualityLabel = item.selectFirst(".tick-dub")?.text()?.trim()
                )
            }
    }

    fun parseSources(document: Document): List<YanSource> {
        return document.select(YanHH3DSelectors.SERVER_LINK)
            .mapNotNull { el ->
                val url = el.attr("data-src").trim()
                if (url.isBlank()) return@mapNotNull null

                val name = el.text().trim().ifBlank { "YanHH3D" }

                when {
                    url.contains(".m3u8", ignoreCase = true) -> {
                        YanSource(
                            name = name,
                            url = url,
                            type = YanSourceType.HLS,
                            quality = parseQuality(name, url)
                        )
                    }

                    url.contains("abyss", ignoreCase = true) -> {
                        YanSource(
                            name = name,
                            url = url,
                            type = YanSourceType.EMBED,
                            quality = Qualities.Unknown.value
                        )
                    }

                    else -> null
                }
            }
            .distinctBy { it.url }
    }

    private fun parseQuality(name: String, url: String): Int {
        val text = "$name $url"
        return when {
            text.contains("4k", ignoreCase = true) ||
                text.contains("2160", ignoreCase = true) -> Qualities.P2160.value

            text.contains("1080", ignoreCase = true) -> Qualities.P1080.value
            text.contains("720", ignoreCase = true) -> Qualities.P720.value
            text.contains("480", ignoreCase = true) -> Qualities.P480.value
            else -> Qualities.Unknown.value
        }
    }
}
```

This is concept code. The coding agent must adjust imports and exact helper signatures to the CloudStream template version being used.

---

# 15. Provider implementation requirements

## 15.1 `getMainPage()`

Pseudo-flow:

```text
request.data = category path
page = current page

url = baseUrl + path
if page > 1:
  url += "?page=page"

GET url
parseList(document)
convert to SearchResponse
return HomePageResponse
```

## 15.2 `search()`

Pseudo-flow:

```text
if query blank:
  return emptyList

encoded = URLEncoder.encode(query, UTF-8)
url = baseUrl + "/search?keysearch=" + encoded
GET url
parseList(document)
convert to SearchResponse
```

## 15.3 `load()`

Pseudo-flow:

```text
url = domainResolver.absoluteUrl(input)
GET url
detail = parser.parseDetail(document, url)
return TvSeriesLoadResponse(detail)
```

## 15.4 `loadLinks()`

Pseudo-flow:

```text
GET episode URL with referer
sources = parser.parseSources(document)

for source in sources:
  if HLS:
    callback(ExtractorLink with headers)
    also optionally callback normalized fallback
  if EMBED:
    loadExtractor(source.url, referer, subtitleCallback, callback)

return found
```

---

# 16. Quality and source naming

Recommended source names:

```text
YanHH3D 4K
YanHH3D 1080
YanHH3D HLS
YanHH3D Abyss
YanHH3D HLS Fallback
```

Rules:

- Keep provider name in source.
- Preserve server text from page.
- Do not create 10 duplicate names for same URL.
- Do not label unknown quality as 1080/4K.

---

# 17. Headers

All direct HLS links must include:

```text
Referer: <current YanHH3D base URL>
User-Agent: desktop Chrome UA
```

Optional extra headers if needed later:

```text
Origin: <current YanHH3D base URL>
Accept: */*
```

Do not add cookies unless confirmed necessary.

---

# 18. Test plan

## 18.1 Unit tests

Create fixtures:

```text
src/test/resources/yanhh3d/home.html
src/test/resources/yanhh3d/search.html
src/test/resources/yanhh3d/detail.html
src/test/resources/yanhh3d/episode.html
```

Test cases:

### List parser

- Given 3 `.flw-item`, returns 3 items.
- Missing poster still returns item.
- Relative poster becomes absolute.
- Empty href is skipped.

### Search parser

- Reuses list parser.
- Handles Vietnamese title.

### Detail parser

- Parses `og:title`.
- Parses `og:image`.
- Parses `og:description`.
- Parses canonical URL.
- Parses year from text.
- Does not crash when status/genre absent.

### Episode parser

- Parses all server tabs.
- Parses all episode URLs.
- Sorts by episode number.
- Removes duplicates.

### Source parser

- Finds `.m3u8`.
- Detects 4K.
- Detects 1080p.
- Detects Abyss embed.
- Skips unknown empty `data-src`.

## 18.2 Integration tests manual

Manual test matrix:

```text
Home latest page
Category Huyền Huyễn
Category Tiên Hiệp
Search Vietnamese query
Search with spaces
Open title with many episodes
Open first episode
Open latest episode
Open 1080 source
Open 4K source
Open source with only generic m3u8
Open source with Abyss fallback
Change domain constant
Open cached old-domain episode URL
```

## 18.3 Regression process when site changes

When plugin breaks:

1. Save current broken HTML page.
2. Compare with old fixture.
3. Check changed selectors:
   - `.flw-item`
   - `.detail-infor-content`
   - `div[class*="list-severs"]`
   - `data-src`
4. Update selector constants only.
5. Re-run unit tests.
6. Build `.cs3`.
7. Increment plugin version.

---

# 19. Release plan

## 19.1 Versioning

Start:

```text
version = 1
```

Increment whenever:

- parser selector changes
- domain default changes
- build output changes
- source extraction changes

## 19.2 Release artifacts

Each release should include:

```text
YanHH3D.cs3
plugins.json
repo.json
CHANGELOG.md optional
```

## 19.3 Changelog format

```markdown
## v1
- Initial YanHH3D provider.
- Home/category/search/detail/episode support.
- HLS and Abyss fallback.
- Domain resolver.
```

---

# 20. GitHub Actions acceptance

If using GitHub Actions, workflow must:

1. Checkout repo.
2. Set up JDK.
3. Run Gradle build.
4. Generate `.cs3`.
5. Generate `plugins.json`.
6. Publish to `builds` branch or release artifacts.

Acceptance:

- Raw `repo.json` URL can be added to CloudStream.
- CloudStream displays YanHH3D plugin.
- Install works.

---

# 21. Security and privacy

## 21.1 No credentials

Plugin must not:

- Ask for username/password.
- Store cookies manually.
- Send analytics.
- Log user queries unnecessarily.
- Exfiltrate URLs.

## 21.2 Network calls

Allowed:

```text
GET YanHH3D pages
GET direct HLS playlist through player
GET embed page through CloudStream extractor
```

Not allowed:

```text
POST login
CAPTCHA solve service
third-party tracking endpoint
unknown remote config from untrusted host
```

## 21.3 Remote config

If implementing remote domain config, use only a personal trusted raw GitHub URL.

Example:

```json
{
  "yanhh3d": {
    "baseUrl": "https://yanhh3d.love"
  }
}
```

Do not execute remote code. Config must be data only.

---

# 22. Maintenance SOP

## 22.1 Domain changed

Steps:

1. Verify new domain in browser.
2. Update `DEFAULT_BASE_URL`.
3. Add old domain to `KNOWN_DOMAINS`.
4. Build plugin.
5. Increment version.
6. Publish `.cs3` and `plugins.json`.

## 22.2 List page broken

Check selector:

```css
.flw-item
```

If changed, update:

```kotlin
YanHH3DSelectors.MOVIE_ITEM
```

Update fixture and tests.

## 22.3 Detail page broken

Check:

```css
link[rel=canonical]
meta[property=og:title]
meta[property=og:image]
meta[property=og:description]
.detail-infor-content
```

Update parser.

## 22.4 Episodes missing

Check:

```css
.detail-infor-content
li a[href]
target tab sections
```

Maybe tabs changed from anchors to buttons. Add fallback selector if needed.

## 22.5 No video links

Check episode page:

```css
div[class*="list-severs"] a[data-src]
```

If `data-src` moved to another attribute, update source parser.

Possible alternate attributes to inspect:

```text
data-url
data-link
href
src
```

Do not guess without fixture.

## 22.6 HLS not playing

Check:

- Is URL valid?
- Does playlist need `Referer`?
- Does playlist need `Origin`?
- Does old normalization `/stream/m3u8/<file>` still apply?
- Is player requesting segments with headers?

---

# 23. Risks

| Risk | Impact | Mitigation |
|---|---:|---|
| YanHH3D changes domain | High | Domain resolver + config |
| YanHH3D changes HTML classes | High | Selector constants + fixtures |
| Video host blocks missing referer | High | Add HLS headers |
| CloudStream API changes | Medium | Use current TestPlugins template |
| Embed host unsupported | Medium | Use `loadExtractor()` only when supported |
| ISP/DNS block | Medium | Document DNS issue; do not treat as parser bug |
| Too many duplicate sources | Low | `distinctBy { url }` |
| Wrong episode sort | Low | Numeric sort + fallback order |

---

# 24. Open questions for implementation

These do not block v1 but should be checked during coding:

1. Does the active YanHH3D domain still use `.flw-item`?
2. Does active episode page still expose `data-src` directly?
3. Does active HLS require normalized `/stream/m3u8/<file>` or original URL works?
4. Does CloudStream current template use `newExtractorLink()` signature exactly as expected?
5. Does current CloudStream dependency include an Abyss extractor?
6. Should `Origin` header be added in addition to `Referer`?

The coding agent should answer these by testing, not by assumptions.

---

# 25. Final checklist for coding agent

- [ ] Create `YanHH3D` module.
- [ ] Add Gradle config.
- [ ] Add plugin class.
- [ ] Add provider class.
- [ ] Add constants.
- [ ] Add domain resolver.
- [ ] Add parser models.
- [ ] Add list parser.
- [ ] Add main page.
- [ ] Add search.
- [ ] Add detail parser.
- [ ] Add episode parser.
- [ ] Add source parser.
- [ ] Add HLS callback.
- [ ] Add embed fallback.
- [ ] Add headers.
- [ ] Add optional HLS normalization fallback.
- [ ] Add fixtures.
- [ ] Add parser tests.
- [ ] Run build.
- [ ] Test in CloudStream.
- [ ] Generate `.cs3`.
- [ ] Generate `plugins.json`.
- [ ] Publish private repo.
- [ ] Add repo URL to CloudStream.
- [ ] Increment version after every release.

---

# 26. One-shot vibe-code prompt

Use this exact prompt to start implementation:

```text
Build a private CloudStream 3 Kotlin extension named YanHH3D.

Use the current recloudstream/TestPlugins-style template. Create a module called YanHH3D and implement a provider that scrapes YanHH3D according to the PRD in this repository.

The source behavior to reproduce comes from youngbi/repo/plugins/yanhh3d_plugin.js:
- default base URL: https://yanhh3d.love
- old domain seen: https://yanhh3d.ac
- latest page: /moi-cap-nhat
- search: /search?keysearch=<encoded query>
- list selector: .flw-item
- card fields: a[href], a[title], img[src], .tick-rate, .tick-dub
- detail metadata: canonical, og:url, og:image, og:title, og:description, video:duration
- episode container: .detail-infor-content
- video source selector: div[class*="list-severs"] a[data-src]
- source classification: 4K m3u8, 1080 m3u8, other m3u8, abyss embed
- direct HLS must include Referer and User-Agent

Implement:
- YanHH3DPlugin.kt
- YanHH3DProvider.kt
- YanHH3DParser.kt
- YanHH3DDomainResolver.kt
- YanHH3DModels.kt
- YanHH3DConstants.kt
- parser tests with HTML fixtures

Requirements:
- Do not use WebView, browser automation, CAPTCHA bypass, DRM bypass, login bypass, or cookie harvesting.
- Return all valid HLS sources instead of selecting only one.
- Call loadExtractor for supported embed URLs.
- Keep domain configurable.
- Store/remap URLs so old YanHH3D domains can be replaced by current domain.
- Use safe parsing, no selector force unwraps.
- Add logging with [YanHH3D] prefix.
- Build with Gradle and fix compile errors.
- If CloudStream API signatures differ from sample code, adapt to current template while preserving behavior.
- Produce a working .cs3 and repo.json/plugins.json for private installation.
```

---

# 27. Reference links

## Source analyzed

```text
https://github.com/youngbi/repo/blob/main/plugins/yanhh3d_plugin.js
```

## CloudStream plugin template

```text
https://github.com/recloudstream/TestPlugins
```

## CloudStream developer docs

```text
https://recloudstream.github.io/csdocs/devs/gettingstarted/
https://recloudstream.github.io/csdocs/devs/create-your-own-json-repository/
```

## CloudStream core repository

```text
https://github.com/recloudstream/cloudstream
```

---

# 28. Notes for personal use

This PRD is intentionally scoped for a private project. Before using or distributing any extension, verify the legality and terms of the content source in the relevant jurisdiction. The plugin should not host, mirror, download, decrypt, or bypass protected media. It should only parse public pages and pass public playback URLs to CloudStream in the same manner a normal browser page exposes them.
