# VsphimProvider — kế hoạch triển khai và trạng thái (v5)

## Mục tiêu

Tạo provider CloudStream 3 Kotlin độc lập cho VSPHIM, dùng public JSON API tại
`https://nguon.vsphim.com/api`, giữ nguyên `YanHHProvider` và không phụ thuộc các route MacCMS đang trả 404.
Homepage dùng các nhóm lọc ổn định, mỗi nhóm hiển thị 20 phim mới cập nhật và mở được toàn bộ danh sách qua phân trang.

## Phạm vi hiện tại

- Module riêng: `VsphimProvider`.
- Trang chủ gồm các nhóm `Mới cập nhật`, `Phim lẻ`, `Phim bộ`, `Hoạt hình`, `TV Shows`, `Đang cập nhật` và `Hoàn thành`.
- Mỗi nhóm gọi `/api/danh-sach` với `limit=20`, sắp xếp `modified` giảm dần và giữ pagination khi người dùng mở nhóm.
- Dùng `/api/phim/{slug}` để làm giàu title, năm, poster, thumb và metadata trước khi tạo card; detail được cache trong phiên provider.
- Chi tiết phim, movie/series mapping và episode từ `episodes[].server_data[]`.
- Đọc trang player trong `link_embed`, lấy playlist HLS `master.m3u8` và truyền
  `Referer`/`User-Agent`/`Origin` cho CloudStream; vẫn fallback sang extractor nếu
  player page không nhận diện được.
- Jackson models/parser thuần JSON, không network và không import CloudStream.
- Public provider methods bắt lỗi và trả kết quả an toàn.
- Không WebView, bypass, download, rehost hoặc tự suy diễn HLS từ embed URL.

## Endpoint sử dụng

- `/api/danh-sach?limit=20&page=n` cho section mới cập nhật.
- `/api/danh-sach` với `limit=20`, `type` và `status` cho các section homepage.
- `/api/tim-kiem?keyword=...&limit=24&page=1`
- `/api/phim/{slug}`
- Các catalog `/api/the-loai`, `/api/quoc-gia`, `/api/nam`, `/api/code` được model hóa để mở rộng sau.

## Mapping CloudStream

- `single` → `newMovieLoadResponse`, chọn embed đầu tiên hợp lệ.
- `series`, `tvshows`, `hoathinh` → `newTvSeriesLoadResponse`.
- Nhiều server được flatten thành các episode có tên `<server> — <episode>` và dedupe theo embed URL.
- Search response dùng URL API detail `/api/phim/{slug}`; `load()` gọi trực tiếp API detail.
- `poster_url` được dùng làm poster; `thumb_url` được dùng làm `backgroundPosterUrl`, với fallback giữa hai trường.
- `loadLinks()` tải player page, parse playlist HLS trực tiếp; embed host không nhận
  diện được mới chuyển cho `loadExtractor()`.

Các nhóm homepage dùng bộ lọc `type/status` ổn định của API. Không dùng toàn bộ `/api/the-loai` để dựng section động vì
response live hiện chứa lượng lớn tag không phù hợp làm nhóm điều hướng.

## Kiểm thử và phát hành

```powershell
.\gradlew.bat VsphimProvider:test
.\gradlew.bat VsphimProvider:make
.\gradlew.bat YanHHProvider:test
.\gradlew.bat YanHHProvider:make
.\gradlew.bat make makePluginsJson
```

Manual acceptance: provider xuất hiện trong CloudStream, main pages phân trang đúng, search/detail hoạt động,
episode map đúng và ít nhất một `link_embed` được CloudStream extractor xử lý nếu host được hỗ trợ.

## Trạng thái

- [x] Khảo sát API và chốt phạm vi public API.
- [x] Tạo module/plugin metadata.
- [x] Tạo models, JSON parser, resolver và API client.
- [x] Implement CloudStream browse/search/detail/loadLinks và VSPHIM player playback.
- [x] Thêm homepage sections giới hạn 20 phim, sort theo `modified` và phân trang toàn bộ từng nhóm.
- [x] Làm giàu card bằng `/api/phim/{slug}`, map đầy đủ poster/thumb và thêm fallback ảnh.
- [x] Viết unit tests và fixtures.
- [x] Build `.cs3`, `plugins.json` và kiểm tra manifest/package.
- [ ] Manual verification trên CloudStream/ADB (SDK có `platform-tools\adb.exe`, nhưng hiện không có device/emulator kết nối).

## Kết quả triển khai

- `VsphimProvider:test`: pass, gồm test parser API và parser player/HLS.
- `VsphimProvider:make`: pass; sinh `VsphimProvider/build/VsphimProvider.cs3`.
- Manifest package xác nhận `com.vsphim.VsphimPlugin`.
- `makePluginsJson`: pass; manifest gồm `VsphimProvider`, `YanHHProvider` và `ExampleProvider`.
- VSPHIM v5: homepage gọi danh sách giới hạn 20, gọi detail `/api/phim/{slug}` để cập nhật card và dùng thumb làm background poster.
- Regression `YanHHProvider:test` hiện có 8 test fail do baseline đã kỳ vọng domain `yanhh3d.pw` trong khi constants hiện dùng `yanhh3d.ee`; không phát sinh từ module VSPHIM và không sửa theo phạm vi goal.
- Manual install/playback chưa thực hiện được vì chưa có device/emulator kết nối; `adb.exe` tồn tại tại Android SDK nhưng chưa nằm trong PATH.
