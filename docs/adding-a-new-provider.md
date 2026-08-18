# Thêm một provider mới

Hướng dẫn copy `YanHHProvider` thành một provider cho site khác. Viết dựa trên đúng những gì đã làm khi dựng YanHH3D, kể cả các lỗi đã gặp thật.

Ví dụ xuyên suốt: dựng provider tên **NewSite** cho `https://newsite.example`.

---

## 0. Kiến trúc bạn đang copy

Bốn lớp, tách bạch có lý do:

```text
NewSitePlugin.kt        entrypoint, chỉ register provider
NewSiteProvider.kt      CloudStream API, HTTP, map model, bắt lỗi
NewSiteParser.kt        Jsoup thuần, KHÔNG network, KHÔNG class CloudStream
NewSiteDomainResolver.kt mọi thứ liên quan URL và đổi domain
NewSiteConstants.kt     domain, selector, nhãn, regex, quality
NewSiteModels.kt        data class nội bộ
```

Lý do parser không được đụng class CloudStream: stub `com.lagradost:cloudstream3` là **compile-only**, không có trên test runtime classpath. Parser mà `import` một class CloudStream thì unit test chết `NoClassDefFoundError` ngay. Đây là lý do tồn tại `YanHH3DQualities` thay vì dùng thẳng `Qualities`.

---

## 1. Chuẩn bị máy (chỉ làm một lần)

Bỏ qua nếu đã build được project này.

```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e --silent
winget install --id Google.AndroidCLI -e
```

`android.exe` sau khi cài nằm ở `%LOCALAPPDATA%\Microsoft\WinGet\Packages\Google.AndroidCLI_*\android.exe`. Dùng nó cài SDK:

```powershell
android sdk install "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Lệnh này có thể kết thúc với exit code lạ dù tải xong; cứ kiểm tra thư mục thay vì tin exit code:

```powershell
Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk"    # phải thấy platforms, build-tools, licenses
```

Set biến môi trường user scope:

```powershell
$jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $sdk, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', $sdk, 'User')
```

Mở lại terminal rồi xác nhận:

```powershell
.\gradlew.bat --version    # phải thấy "Launcher JVM: 17.x"
```

**Phải là JDK 17.** AGP 8.7.3 trong [../build.gradle.kts](../build.gradle.kts) yêu cầu vậy.

---

## 2. Copy thư mục

Dùng **PowerShell 7 (`pwsh`)**, không dùng Windows PowerShell 5.1 — 5.1 ghi file mặc định không phải UTF-8 và sẽ làm hỏng mọi ký tự tiếng Việt trong constants.

```powershell
Copy-Item YanHHProvider NewSiteProvider -Recurse
Remove-Item NewSiteProvider\build -Recurse -Force -ErrorAction SilentlyContinue
```

Đổi tên thư mục package và fixture:

```powershell
Rename-Item NewSiteProvider\src\main\kotlin\com\yanhh3d newsite
Rename-Item NewSiteProvider\src\test\kotlin\com\yanhh3d newsite
Rename-Item NewSiteProvider\src\test\resources\yanhh3d newsite
```

Đổi tên file:

```powershell
Get-ChildItem NewSiteProvider -Recurse -Filter "YanHH3D*.kt" |
    Rename-Item -NewName { $_.Name -replace '^YanHH3D', 'NewSite' }
```

Đổi identifier bên trong. Thứ tự quan trọng: `YanHH3D` phải thay trước, nếu không `Yan` sẽ ăn nhầm vào nó:

```powershell
Get-ChildItem NewSiteProvider -Recurse -Include *.kt, *.kts | ForEach-Object {
    (Get-Content $_.FullName -Raw) `
        -replace 'com\.yanhh3d', 'com.newsite' `
        -replace 'YanHH3D', 'NewSite' `
        -replace 'YanMovieItem', 'NewSiteMovieItem' `
        -replace 'YanDetail', 'NewSiteDetail' `
        -replace 'YanEpisode', 'NewSiteEpisode' `
        -replace 'YanSource', 'NewSiteSource' `
        -replace 'yanhh3d/', 'newsite/' |
        Set-Content $_.FullName -NoNewline
}
```

`YanSourceType` tự động thành `NewSiteSourceType` vì `YanSource` là tiền tố của nó.

---

## 3. Sửa những chỗ nào

### `NewSiteProvider/build.gradle.kts`

| Dòng | Sửa thành gì |
| --- | --- |
| `version = 1` | Bắt đầu lại từ `1`. Tăng mỗi lần release. |
| `description` | Mô tả provider mới |
| `tvTypes` | `listOf("TvSeries")`, `listOf("Movie")`, `listOf("Anime")`... theo site |
| `language` | Mã ISO 2 ký tự, ví dụ `"vi"`, `"en"` |
| `iconUrl` | Favicon của site mới |
| `namespace` | **Bắt buộc đổi** thành `com.newsite`. Root build gán `com.example` cho mọi subproject; hai module trùng namespace là va nhau. |

Giữ nguyên `requiresResources = false` trừ khi bạn thêm layout/string Android.

### Tên thư mục module = tên plugin hiển thị

Tên hiển thị trong danh sách Extensions của CloudStream lấy từ **tên thư mục module**, không phải từ `MainAPI.name`. Đặt tên thư mục đúng thứ bạn muốn người dùng thấy. Kiểm tra sau khi build:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'NewSiteProvider\build\NewSiteProvider.cs3'))
$e = $z.Entries | Where-Object FullName -eq 'manifest.json'
(New-Object IO.StreamReader($e.Open())).ReadToEnd()
$z.Dispose()
```

Phải thấy `"pluginClassName":"com.newsite.NewSitePlugin"`.

### `NewSiteConstants.kt` — file bạn sửa nhiều nhất

| Hằng số | Nội dung |
| --- | --- |
| `PROVIDER_NAME` | Tên hiện trong app |
| `LOG_TAG` | Prefix log, giữ dạng `[NewSite]` |
| `DEFAULT_BASE_URL` | Domain hiện tại, **không** có dấu `/` cuối |
| `KNOWN_DOMAINS` | Domain hiện tại + mọi domain cũ đã biết |
| `USER_AGENT` | Giữ nguyên UA Chrome desktop trừ khi site chặn |
| `SEARCH_PATH` / `SEARCH_QUERY_PARAM` | Ví dụ `/tim-kiem` và `keyword` |
| `PAGE_QUERY_PARAM` | Thường là `page` |
| `MAIN_PAGES` | Danh sách `"/path" to "Tên hiển thị"`. **Thứ tự pair là path trước, tên sau** — `mainPageOf` dựng `MainPageData(name = second, data = first)`. |
| `NewSiteSelectors` | Toàn bộ CSS selector |
| `NewSiteLabels` | Nhãn text trên trang detail (`Trạng thái`, `Năm`, `Thể loại`...) |
| `NewSitePatterns` | Regex năm, số tập, và `EMBED_HINTS` |
| `NewSiteQualities` | **Đừng sửa số.** Chúng khớp `Qualities` của CloudStream: `Unknown=400`, `P480/720/1080/2160` = đúng số đó. |

### `NewSiteProvider.kt`

Chỉ 3 chỗ:

```kotlin
override var lang = "vi"                                    // đổi
override val supportedTypes = setOf(TvType.TvSeries, ...)   // đổi
newTvSeriesSearchResponse(...) / newTvSeriesLoadResponse(...)  // đổi sang Movie nếu site là phim lẻ
```

Phần còn lại (`getMainPage`, `search`, `load`, `loadLinks`) dùng lại được nguyên vẹn nếu site cùng kiểu.

### `NewSiteParser.kt`

Đây là phần thật sự phải viết lại. Xem mục 5.

---

## 4. Bốn luật không được phá khi copy

Đây là các ràng buộc của [../AGENTS.md](../AGENTS.md), phá là hỏng theo cách khó thấy:

1. **Parser không import class CloudStream, không gọi network.** Nếu không, unit test chết ngay.
2. **Domain literal chỉ nằm trong constants.** Kiểm tra bằng:
   ```powershell
   rg "newsite\.example" NewSiteProvider
   ```
   Chỉ được khớp `NewSiteConstants.kt` và `iconUrl` trong `build.gradle.kts`.
3. **Data mà CloudStream lưu (episode `data`) phải là path-only.** `normalizeInternalData()` lo việc này. Lưu URL tuyệt đối thì lịch sử xem chết ngay khi site đổi domain.
4. **Mọi `override` public phải `runCatching` và trả empty/null/false.** Provider ném exception là kéo sập cả màn hình chính của app.

---

## 5. Viết parser cho site mới — làm theo thứ tự này

Đừng viết parser trước rồi test sau. Thứ tự đã dùng cho YanHH3D:

**Bước 1 — Lấy HTML thật.**

```powershell
curl.exe -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0" `
    "https://newsite.example/danh-sach" -o NewSiteProvider\src\test\resources\newsite\home.html
```

Cắt bớt cho gọn, nhưng **giữ lại các ca xấu**: card thiếu poster, card `href` rỗng, URL còn ở domain cũ, poster dạng `//cdn...`, tập trùng giữa các server, tập không có số.

**Bước 2 — Viết test trước, để nó fail.**

```powershell
.\gradlew.bat NewSiteProvider:test    # phải fail
```

**Bước 3 — Cài selector vào `NewSiteSelectors`, rồi implement.**

**Bước 4 — Chạy tới xanh.**

```powershell
.\gradlew.bat NewSiteProvider:test
```

### Bẫy đã dính thật khi làm YanHH3D

Hàm dò nhãn trên trang detail ban đầu chọn element theo điều kiện *"text dài hơn nhãn"*. Trang có cấu trúc:

```html
<div class="row-line"><span class="type">Năm:</span> 2023</div>
```

Jsoup trả kết quả theo document order nên `<span>` nằm **sau** `.row-line`, và `"Năm:"` dài hơn `"Năm"` đúng một ký tự → lọt điều kiện → lấy nhầm span → giá trị rỗng → `year`, `status`, `genres` đều `null`.

Điều kiện đúng là *"còn nội dung thật sau khi bỏ nhãn"*, xem `labelledElement()` trong parser. Giữ nguyên cách này khi copy.

### Kiểm tra tiếng Việt không bị mojibake

Compiler có thể nuốt encoding mà build vẫn xanh, và tên danh mục sai sẽ hiện thẳng ra app. Kiểm bằng byte thật, đừng tin output của `javap` (JDK 17 in console bằng cp1252, luôn trông như hỏng):

```powershell
$expected = "M" + [char]0x1EDB + "i c" + [char]0x1EAD + "p nh" + [char]0x1EAD + "t"   # "Mới cập nhật"
$cls = [Text.Encoding]::UTF8.GetString([IO.File]::ReadAllBytes(
    "NewSiteProvider\build\tmp\kotlin-classes\debug\com\newsite\NewSiteConstants.class"))
$cls.Contains($expected)     # phải True
```

---

## 6. Build và test

`settings.gradle.kts` tự include mọi thư mục top-level có `build.gradle.kts` — **không cần sửa gì** để đăng ký module mới.

```powershell
.\gradlew.bat tasks --all | Select-String "NewSiteProvider:"   # xác nhận module đã được nhận
.\gradlew.bat NewSiteProvider:test                             # unit test parser
.\gradlew.bat NewSiteProvider:make                             # ra file .cs3
.\gradlew.bat make makePluginsJson                             # build tất cả + sinh plugins.json
```

Nếu không thấy `NewSiteProvider:make`, thư mục chưa có `build.gradle.kts`.

Cài thẳng qua ADB khi có thiết bị cắm dây:

```powershell
.\gradlew.bat NewSiteProvider:deployWithAdb
```

---

## 7. Phát hành

**Không cần sửa [../repo.json](../repo.json).** Nó chỉ trỏ tới `builds/plugins.json`, mà `plugins.json` được sinh tự động từ tất cả module. Thêm provider mới là nó tự có mặt.

Quy trình:

1. Tăng `version` trong `NewSiteProvider/build.gradle.kts`.
2. Ghi thay đổi vào [../CHANGELOG.md](../CHANGELOG.md).
3. Commit và push lên `main`.
4. Workflow [../.github/workflows/build.yml](../.github/workflows/build.yml) tự build và push `.cs3`, `plugins.json`, `repo.json` sang branch `builds`.
5. CloudStream tự thấy version mới ở lần refresh repo sau.

URL cần dán vào CloudStream (Settings → Extensions → Add Repository):

```text
https://raw.githubusercontent.com/riddicknguyen-collab/temp-plugin-cs3/builds/repo.json
```

**Repo phải là public.** `raw.githubusercontent.com` trả 404 cho repo private khi không có token, và CloudStream không gửi token.

Tăng `version` khi: đổi selector, đổi domain mặc định, đổi cách trích nguồn, hoặc đổi output build. Không tăng thì CloudStream không biết có bản mới.

---

## 8. Kiểm thử trong app

Unit test chỉ chứng minh parser khớp **fixture**, không chứng minh nó khớp **site đang sống**. Bắt buộc chạy tay:

- Trang danh sách chính có ra card không
- Ít nhất 3 danh mục
- Search có dấu cách và có dấu tiếng Việt
- Mở một phim: title, poster, mô tả, năm, thể loại, danh sách tập
- Tập sắp xếp `1, 2, 3, 10` chứ không phải `1, 10, 2`
- Mở tập đầu và tập mới nhất, ít nhất một nguồn HLS phát được
- Nguồn embed không làm crash app

Xem log để biết chết ở bước nào — provider log mọi bước:

```powershell
adb logcat -s NewSite
```

```text
[NewSite] getMainPage url=...
[NewSite] parseList count=0        ← count=0 nghĩa là selector đã hỏng
[NewSite] load detail url=...
[NewSite] parseEpisodes count=...
[NewSite] loadLinks url=...
[NewSite] source count=...
[NewSite] hls source=...
[NewSite] embed source=...
```

Selector hỏng thì lưu HTML thật vào `src/test/resources/newsite/`, sửa `NewSiteSelectors`, chạy lại test, rồi build. Không đoán selector khi chưa có fixture.

---

## 9. Lỗi hay gặp

| Triệu chứng | Nguyên nhân và cách xử lý |
| --- | --- |
| `Class 'com.lagradost...' was compiled with an incompatible version of Kotlin` | Stub `cloudstream3:pre-release` là tag di động, đã nhảy lên Kotlin mới hơn compiler. Nâng `kotlin-gradle-plugin` trong [../build.gradle.kts](../build.gradle.kts). Đã từng phải đi từ 2.1.0 lên 2.4.10. |
| `NewSiteProvider:make` không có trong `tasks --all` | Thiếu `NewSiteProvider/build.gradle.kts`. |
| `ERROR: JAVA_HOME is not set` | Chưa cài JDK hoặc chưa mở lại terminal sau khi set biến môi trường. |
| `WARNING: D8: error parsing kotlin metadata` | Vô hại. R8/D8 trong AGP 8.7.3 cũ hơn Kotlin 2.4 nên bỏ qua bước rewrite `@Metadata`. Dex vẫn đúng. |
| Unit test chết `NoClassDefFoundError` | Parser đã lỡ import class CloudStream. Gỡ ra, dùng hằng số riêng. |
| Tên danh mục hiện ra ký tự lạ trong app | File nguồn không phải UTF-8. Thường do sửa bằng PowerShell 5.1. Kiểm bằng script ở mục 5. |
| CI fail `Write access to repository not granted` (403) | Workflow thiếu `permissions: contents: write`. |
| CI fail ở bước `Checkout builds` | Branch `builds` chưa tồn tại. Tạo bằng: `git branch builds $(git commit-tree $(git hash-object -w -t tree /dev/null) -m "Initialize builds branch")` rồi `git push -u origin builds`. |
| CloudStream báo không tải được repo | Repo đang private, hoặc URL trỏ nhầm branch. |
| Video không phát dù có link | Thiếu `Referer`/`User-Agent`. Kiểm tra builder trong `newExtractorLink`. |

---

## 10. Tra chữ ký API CloudStream

API của CloudStream có đổi giữa các bản. Đừng chép chữ ký từ tài liệu cũ — đọc thẳng từ stub jar đang dùng:

```powershell
$jar = Get-ChildItem "$env:USERPROFILE\.gradle\caches" -Recurse -Filter "*cloudstream*.jar" |
    Where-Object Length -gt 100000 | Select-Object -First 1
$javap = "$env:JAVA_HOME\bin\javap.exe"

& $javap -classpath $jar.FullName com.lagradost.cloudstream3.MainAPIKt |
    Select-String "newTvSeriesLoadResponse|newEpisode|newHomePageResponse"

& $javap -classpath $jar.FullName com.lagradost.cloudstream3.utils.ExtractorApiKt |
    Select-String "newExtractorLink|loadExtractor"
```

Cách này đã dùng để xác nhận 3 điều: `newExtractorLink` là hàm suspend có builder lambda (không phải constructor như tài liệu cũ mô tả), giá trị enum `Qualities`, và thứ tự pair của `mainPageOf`.

---

## Tài liệu liên quan

- [../AGENTS.md](../AGENTS.md) — luật kiến trúc bắt buộc
- [plan.md](plan.md) — kế hoạch triển khai YanHH3D và trạng thái từng phase
- [YanHH3D_CloudStream_Plugin_PRD.md](YanHH3D_CloudStream_Plugin_PRD.md) — PRD gốc
