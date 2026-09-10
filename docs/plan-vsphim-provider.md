# VsphimProvider — kế hoạch triển khai và trạng thái

## Mục tiêu

Tạo provider CloudStream 3 Kotlin độc lập cho VSPHIM, dùng public JSON API tại
`https://nguon.vsphim.com/api`, giữ nguyên `YanHHProvider` và không phụ thuộc các route MacCMS đang trả 404.

## Phạm vi v1

- Module riêng: `VsphimProvider`.
- Trang chủ, các nhóm lọc ổn định, phân trang và tìm kiếm.
- Chi tiết phim, movie/series mapping và episode từ `episodes[].server_data[]`.
- Chuyển `link_embed` cho CloudStream `loadExtractor()`.
- Jackson models/parser thuần JSON, không network và không import CloudStream.
- Public provider methods bắt lỗi và trả kết quả an toàn.
- Không WebView, bypass, download, rehost hoặc tự suy diễn HLS từ embed URL.

## Endpoint sử dụng

- `/api/danh-sach/phim-moi-cap-nhat?page=n`
- `/api/danh-sach` với `type` và `status` cho các section.
- `/api/tim-kiem?keyword=...&limit=24&page=1`
- `/api/phim/{slug}`
- Các catalog `/api/the-loai`, `/api/quoc-gia`, `/api/nam`, `/api/code` được model hóa để mở rộng sau.

## Mapping CloudStream

- `single` → `newMovieLoadResponse`, chọn embed đầu tiên hợp lệ.
- `series`, `tvshows`, `hoathinh` → `newTvSeriesLoadResponse`.
- Nhiều server được flatten thành các episode có tên `<server> — <episode>` và dedupe theo embed URL.
- Search response dùng URL API detail `/api/phim/{slug}`; `load()` gọi trực tiếp API detail.
- `loadLinks()` gọi `loadExtractor()` và trả `false` nếu embed host không được hỗ trợ.

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
- [x] Implement CloudStream browse/search/detail/loadLinks.
- [x] Viết unit tests và fixtures.
- [x] Build `.cs3`, `plugins.json` và kiểm tra manifest/package.
- [ ] Manual verification trên CloudStream/ADB (SDK có `platform-tools\adb.exe`, nhưng hiện không có device/emulator kết nối).

## Kết quả triển khai

- `VsphimProvider:test`: pass, 10 tests.
- `VsphimProvider:make`: pass; sinh `VsphimProvider/build/VsphimProvider.cs3`.
- Manifest package xác nhận `com.vsphim.VsphimPlugin`.
- `makePluginsJson`: pass; manifest gồm `VsphimProvider`, `YanHHProvider` và `ExampleProvider`.
- Regression `YanHHProvider:test` hiện có 8 test fail do baseline đã kỳ vọng domain `yanhh3d.pw` trong khi constants hiện dùng `yanhh3d.ee`; không phát sinh từ module VSPHIM và không sửa theo phạm vi goal.
- Manual install/playback chưa thực hiện được vì chưa có device/emulator kết nối; `adb.exe` tồn tại tại Android SDK nhưng chưa nằm trong PATH.
