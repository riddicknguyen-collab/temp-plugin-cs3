# Ghi chú lỗi VSPHIM sau v11

Ngày ghi nhận: 2026-09-10

## Hiện trạng người dùng báo lại

- Đã cài được plugin và thấy nguồn VSPHIM trong CloudStream.
- Card phim vẫn chưa hiển thị ảnh fanart khổ ngang như mong muốn.
- Khi chọn một phim, màn hình chỉ hiện tiêu đề; nội dung/detail không load.
- Cần kiểm tra lại khả năng mở toàn bộ từng nhóm để xem các trang phim tiếp theo.

## Những gì v11 đã thay đổi nhưng chưa được xác nhận runtime

- Card và detail ưu tiên `thumb_url`, fallback sang `poster_url`.
- `mainPage` dùng `MainPageData(..., horizontalImages = true)`.
- API list giữ `pagination.totalPages` để trả `HomePageResponse.hasNext`.
- Models chịu được một số mảng optional trả về `null`; API client giới hạn request
  detail đồng thời và retry nhẹ cho HTTP 429.

## Việc cần làm ở phiên sau

1. Xác nhận CloudStream đang chạy đúng artifact version 11 từ repository `builds`,
   không phải bản cache cũ. Ghi lại version trong màn hình quản lý plugin.
2. Lấy log CloudStream ngay lúc mở một card và lúc mở toàn bộ nhóm. Cần phân biệt
   lỗi ở `load()`, lỗi deserialize `/api/phim/{slug}`, lỗi HTTP 429/403, hay lỗi
   UI cache ảnh.
3. Kiểm tra object runtime của `SearchResponse` và `HomePageList`: giá trị ảnh
   thực tế có phải `thumb_url` và `isHorizontalImages = true` hay không.
4. Kiểm tra request trang 2 của từng nhóm: URL phải giữ `limit=20` và thêm
   `page=2`; response phải có `items` và `pagination.totalPages > 2` khi còn trang.
5. Dùng một slug cụ thể để kiểm tra `/api/phim/{slug}` độc lập với homepage,
   sau đó xác nhận `movie`, `episodes`, và `episodes[].server_data[].link_embed`
   trước khi sửa tiếp mapping/player.
6. Chỉ bump version sau khi có regression test tái hiện được lỗi; không kết luận
   v11 đã sửa xong fanart/detail nếu chưa kiểm tra trên CloudStream thật.

## Không nên làm lại nếu chưa có bằng chứng

- Không đổi lại endpoint category sang toàn bộ catalog `/api/the-loai`.
- Không bỏ `thumb_url` chỉ vì poster dọc còn xuất hiện trong cache cũ.
- Không suy diễn lỗi player nếu `load()` chưa trả được detail response.
