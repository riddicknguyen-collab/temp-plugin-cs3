# VSPHIM API — tài liệu tham khảo

Tài liệu này là bản chép lại và chuẩn hóa từ [trang API chính thức của VSPHIM](https://nguon.vsphim.com/api-document), kết hợp với việc gọi thử các URL mẫu. Dùng làm tài liệu tham khảo nội bộ cho provider CloudStream.

**Ngày kiểm tra:** 2026-09-10
**Base URL:** `https://nguon.vsphim.com`
**Định dạng được công bố:** JSON, UTF-8
**Header nên gửi:** `Accept: application/json`

> Đây là tài liệu snapshot. API và dữ liệu live có thể thay đổi. Các kết quả “đang hoạt động” bên dưới là kết quả tại thời điểm kiểm tra, không phải cam kết SLA.

## 1. Quy tắc URL

### API chính

Các path public trong tài liệu được ghép như sau:

```text
https://nguon.vsphim.com/api + <path>
```

Ví dụ:

```text
GET https://nguon.vsphim.com/api/danh-sach?limit=20&page=1
```

Placeholder dạng `[slug]`, `[year]`, `[code]`, `[keyword]` phải được thay bằng giá trị đã URL-encode khi cần.

### API tương thích MacCMS

Các route bắt đầu bằng `/api.php` hoặc `/index.php` nằm ở root site, **không** thêm tiền tố `/api`:

```text
https://nguon.vsphim.com/api.php/...
https://nguon.vsphim.com/index.php/...
```

## 2. Danh sách endpoint

| Nhóm | Method | Endpoint | Trạng thái kiểm tra |
|---|---:|---|---|
| Trang chủ | GET | `/api/danh-sach/phim-moi-cap-nhat` | 200 JSON |
| Danh sách | GET | `/api/danh-sach` | 200 JSON |
| Tìm kiếm | GET | `/api/tim-kiem` | 200 JSON |
| Thể loại | GET | `/api/the-loai` | 200 JSON |
| Thể loại | GET | `/api/the-loai/[slug]` | 200 JSON |
| Quốc gia | GET | `/api/quoc-gia` | 200 JSON |
| Quốc gia | GET | `/api/quoc-gia/[slug]` | 200 JSON |
| Năm | GET | `/api/nam` | 200 JSON |
| Năm | GET | `/api/nam/[year]` | 200 JSON |
| Mã/showtimes | GET | `/api/code` | 200 JSON |
| Mã/showtimes | GET | `/api/code/[code]` | 200 JSON |
| Phim | GET | `/api/phim/[slug]` | 200 JSON |
| MacCMS VOD | GET | `/api.php/provide/vod` | 404 khi kiểm tra |
| MacCMS RSS XML | GET | `/api.php/provide/vod/from/snm3u8/at/xml` | 404 khi kiểm tra |
| MacCMS RSS XMLSEA | GET | `/api.php/provide/vod/from/snm3u8/at/xmlsea` | 404 khi kiểm tra |
| MacCMS bài viết | GET | `/api.php/provide/art` | 404 khi kiểm tra |
| MacCMS diễn viên | GET | `/api.php/provide/actor` | 404 khi kiểm tra |
| MacCMS Ajax | GET | `/index.php/ajax/data.html` | 404 khi kiểm tra |
| MacCMS collect | POST | `/api.php` | 404 khi kiểm tra |

Trang tài liệu còn hiển thị hai mục điều hướng `Diễn viên` và `Từ khóa`, nhưng metadata nhúng trong trang không cung cấp endpoint, method, query hoặc tham số cho hai mục này. Không nên coi chúng là API public đã được đặc tả.

## 3. Hợp đồng response của API chính

### 3.1. Response danh sách phim

Các endpoint danh sách, tìm kiếm, lọc theo thể loại/quốc gia/năm/mã thường trả:

```json
{
  "status": true,
  "items": [
    {
      "modified": { "time": "2026-08-04T00:40:56+07:00" },
      "_id": 48951,
      "name": "Tên phim",
      "origin_name": "Original title",
      "slug": "movie-slug",
      "poster_url": "https://nguon.vsphim.com/storage/images/.../poster.jpg",
      "thumb_url": "https://nguon.vsphim.com/storage/images/.../thumb.jpg",
      "year": 2026
    }
  ],
  "pagination": {
    "totalItems": 23682,
    "totalItemsPerPage": 20,
    "currentPage": 1,
    "totalPages": 1185
  }
}
```

Trường `pathImage` có thể xuất hiện ở response trang chủ. `totalItemsPerPage` đã quan sát thấy cả dạng number và string, vì vậy model client nên parse linh hoạt.

### 3.2. Response danh mục

Các endpoint `/the-loai`, `/quoc-gia`, `/nam`, `/code` trả wrapper khác:

```json
{
  "status": "success",
  "message": "",
  "data": {
    "items": [
      {
        "_id": 14,
        "name": "Nhật Bản",
        "slug": "nhat-ban"
      }
    ]
  }
}
```

Riêng `_id` của danh mục năm được trả dạng chuỗi, ví dụ `"2026"`; `_id` của thể loại/quốc gia/mã thường là số.

### 3.3. Response chi tiết phim

`GET /api/phim/[slug]` trả:

```json
{
  "status": true,
  "msg": "",
  "movie": {
    "created": { "time": "2025-11-14T05:16:09+07:00" },
    "modified": { "time": "2025-11-14T05:16:09+07:00" },
    "_id": 26415,
    "name": "Tên phim",
    "origin_name": "Original title",
    "slug": "movie-slug",
    "content": null,
    "type": "single",
    "status": "completed",
    "poster_url": "https://nguon.vsphim.com/storage/images/movie-slug/poster.jpg",
    "thumb_url": "https://nguon.vsphim.com/storage/images/movie-slug/thumb.jpg",
    "is_copyright": false,
    "trailer_url": null,
    "time": "1:01:07",
    "episode_current": "Full",
    "episode_total": "Full",
    "quality": "HD",
    "lang": "NoSub",
    "notify": null,
    "showtimes": null,
    "year": 2025,
    "view": 42,
    "chieurap": false,
    "sub_docquyen": false,
    "actor": ["Updating"],
    "director": [],
    "category": [
      { "id": 430, "name": "Thể loại", "slug": "the-loai" }
    ],
    "country": [
      { "id": 14, "name": "Quốc gia", "slug": "quoc-gia" }
    ]
  },
  "episodes": [
    {
      "server_name": "VIP",
      "server_data": [
        {
          "name": "Full",
          "slug": "full",
          "filename": "Full",
          "link_embed": "https://embed2.vsphim.com/embed/<token>"
        }
      ]
    }
  ]
}
```

Các key đã quan sát trong `movie`:

| Key | Kiểu thường gặp | Ý nghĩa |
|---|---|---|
| `created`, `modified` | object | Có key con `time` |
| `_id`, `year`, `view` | number | ID, năm, lượt xem |
| `name`, `origin_name`, `slug`, `type`, `status`, `poster_url`, `thumb_url`, `time`, `episode_current`, `episode_total`, `quality`, `lang` | string/null | Metadata phim |
| `content`, `trailer_url`, `notify`, `showtimes` | string/null | Mô tả và thông tin phụ |
| `is_copyright`, `chieurap`, `sub_docquyen` | boolean | Cờ trạng thái |
| `actor`, `director` | array | Diễn viên/đạo diễn; có thể rỗng hoặc chứa chuỗi placeholder |
| `category`, `country` | array<object> | Mỗi object có `id`, `name`, `slug` |

`episodes` là mảng server. Mỗi server có `server_name` và `server_data`; mỗi phần tử `server_data` có `name`, `slug`, `filename`, `link_embed`. Response chi tiết hiện cho thấy link embed, không nên giả định API này luôn trả URL HLS trực tiếp.

## 4. Endpoint public chi tiết

### 4.1. Phim mới cập nhật

```http
GET /api/danh-sach/phim-moi-cap-nhat
```

Mô tả theo trang nguồn: lấy danh sách phim hiển thị trên trang chủ, gồm phim mới nhất/phim hot. Trang không khai báo tham số cho endpoint này. Khi kiểm tra, endpoint trả danh sách rút gọn và phân trang mặc định 24 bản ghi; query `limit=1` không làm thay đổi `totalItemsPerPage` ở response trang chủ.

Ví dụ:

```bash
curl --request GET \
  --url 'https://nguon.vsphim.com/api/danh-sach/phim-moi-cap-nhat' \
  --header 'accept: application/json'
```

### 4.2. Danh sách phim theo bộ lọc

```http
GET /api/danh-sach
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `page` | Không | number | — | Số trang, mặc định `1` | `1` |
| `limit` | Không | number | — | Số phim/trang, mặc định `24` | `20` |
| `year` | Không | string | — | Lọc theo năm sản xuất | `2024` |
| `country` | Không | string | slug | Lọc theo quốc gia | `han-quoc` |
| `category` | Không | string | slug | Lọc theo thể loại | `hanh-dong` |
| `type` | Không | string | `single`, `series`, `hoathinh`, `tvshows` | Lọc theo loại phim | `series` |
| `status` | Không | string | `trailer`, `ongoing`, `completed` | Lọc theo trạng thái | `completed` |

Ví dụ:

```text
https://nguon.vsphim.com/api/danh-sach?page=1&limit=20&year=2024&country=han-quoc&category=hanh-dong&type=series&status=completed
```

### 4.3. Tìm kiếm phim

```http
GET /api/tim-kiem
```

| Tham số | Bắt buộc | Kiểu | Mô tả | Ví dụ |
|---|---:|---|---|---|
| `keyword` | Có | string | Từ khóa tìm kiếm | `avengers` |
| `limit` | Không | number | Số phim/trang | `20` |
| `page` | Không | number | Số trang | `1` |

Ví dụ:

```text
https://nguon.vsphim.com/api/tim-kiem?keyword=avengers&limit=20&page=1
```

### 4.4. Danh sách thể loại

```http
GET /api/the-loai
```

Không có tham số. Response là wrapper danh mục tại mục 3.2, với `data.items[]` gồm `_id`, `name`, `slug`.

### 4.5. Phim theo thể loại

```http
GET /api/the-loai/[slug]
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `slug` trong path | Có | string | — | Slug thể loại | `hanh-dong` |
| `limit` | Không | number | — | Số phim/trang | `20` |
| `page` | Không | number | — | Số trang | `1` |
| `year` | Không | string | — | Lọc theo năm | `2024` |
| `country` | Không | string | — | Lọc theo quốc gia | `han-quoc` |
| `type` | Không | string | `single`, `series`, `hoathinh`, `tvshows` | Lọc theo loại phim | `series` |
| `status` | Không | string | `trailer`, `ongoing`, `completed` | Lọc theo trạng thái | `completed` |

Ví dụ: `https://nguon.vsphim.com/api/the-loai/hanh-dong?limit=20&page=1`.

### 4.6. Danh sách quốc gia

```http
GET /api/quoc-gia
```

Không có tham số. Response là wrapper danh mục tại mục 3.2, với `data.items[]` gồm `_id`, `name`, `slug`.

### 4.7. Phim theo quốc gia

```http
GET /api/quoc-gia/[slug]
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `slug` trong path | Có | string | — | Slug quốc gia | `nhat-ban` |
| `limit` | Không | number | — | Số phim/trang | `20` |
| `page` | Không | number | — | Số trang | `1` |
| `year` | Không | string | — | Lọc theo năm | `2024` |
| `type` | Không | string | `single`, `series`, `hoathinh`, `tvshows` | Lọc theo loại phim | `series` |
| `status` | Không | string | `trailer`, `ongoing`, `completed` | Lọc theo trạng thái | `completed` |

Ví dụ: `https://nguon.vsphim.com/api/quoc-gia/nhat-ban?limit=20&page=1`.

### 4.8. Danh sách năm phát hành

```http
GET /api/nam
```

Không có tham số. Response là wrapper danh mục tại mục 3.2; item thường có `_id`, `name`, `slug` đều chứa giá trị năm, trong đó `_id` là string.

### 4.9. Phim theo năm phát hành

```http
GET /api/nam/[year]
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `year` trong path | Có | string | — | Năm phát hành | `2024` |
| `limit` | Không | number | — | Số phim/trang | `20` |
| `page` | Không | number | — | Số trang | `1` |
| `type` | Không | string | `single`, `series`, `hoathinh`, `tvshows` | Lọc theo loại phim | `series` |
| `status` | Không | string | `trailer`, `ongoing`, `completed` | Lọc theo trạng thái | `completed` |

Ví dụ: `https://nguon.vsphim.com/api/nam/2024?limit=20&page=1`.

### 4.10. Danh sách mã/showtimes

```http
GET /api/code
```

Không có tham số. Response là wrapper danh mục tại mục 3.2; item có `_id`, `name`, `slug`. Trang nguồn gọi đây là danh sách `code`/`showtimes`.

### 4.11. Phim theo mã

```http
GET /api/code/[code]
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `code` trong path | Có | string | — | Mã code | `netflix-2024` |
| `limit` | Không | number | — | Số phim/trang | `20` |
| `page` | Không | number | — | Số trang | `1` |
| `year` | Không | string | — | Lọc theo năm | `2024` |
| `country` | Không | string | slug | Lọc theo quốc gia | `han-quoc` |
| `category` | Không | string | slug | Lọc theo thể loại | `hanh-dong` |
| `type` | Không | string | `single`, `series`, `hoathinh`, `tvshows` | Lọc theo loại phim | `series` |
| `status` | Không | string | `trailer`, `ongoing`, `completed` | Lọc theo trạng thái | `completed` |

Ví dụ: `https://nguon.vsphim.com/api/code/netflix-2024?limit=20&page=1`.

### 4.12. Thông tin phim

```http
GET /api/phim/[slug]
```

| Tham số | Bắt buộc | Kiểu | Mô tả | Ví dụ |
|---|---:|---|---|---|
| `slug` trong path | Có | string | Slug phim | `ddh-357` |

Ví dụ: `https://nguon.vsphim.com/api/phim/ddh-357`. Response đầy đủ xem mục 3.3.

## 5. Endpoint tương thích MacCMS

Các endpoint sau được trang nguồn mô tả là để tương thích crawler/ứng dụng MacCMS. Tuy nhiên, mọi URL nhóm này đều trả `404` khi gọi trực tiếp trong lần kiểm tra ngày 2026-09-10. Giữ lại đặc tả để tham khảo lịch sử/tương thích, nhưng không dùng làm dependency bắt buộc cho provider nếu chưa xác minh lại.

### 5.1. Cung cấp VOD

```http
GET /api.php/provide/vod
```

Query được tài liệu hóa:

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `ac` | Không | string | `list`, `detail` | `list` là danh sách rút gọn; `detail` có trường play URL; mặc định `list` | `list` |
| `at` | Không | string | `json`, `xml`, `josn` | Định dạng; `josn` là typo legacy nhưng route chấp nhận như JSON | `json` |
| `pg` | Không | number | — | Trang phân trang | `1` |
| `limit` | Không | number | 1–200 | Số bản ghi/trang, mặc định `20` | `20` |
| `ids` | Không | string | — | Lọc ID, phân cách bằng dấu phẩy | `1,2,3` |
| `t` | Không | number | — | ID thể loại theo chuẩn MacCMS | `6` |
| `wd` | Không | string | — | Tìm theo tên, tên gốc hoặc slug | `hành động` |
| `h` | Không | number | — | Chỉ lấy bản ghi cập nhật trong N giờ gần nhất | `24` |
| `order` | Không | string | `asc`, `desc` | Thứ tự sắp xếp | `desc` |
| `by` | Không | string | `time`, `id` | Sắp theo thời gian cập nhật hoặc ID | `time` |

URL mẫu theo trang nguồn:

```text
https://nguon.vsphim.com/api.php/provide/vod?ac=list&pg=1&limit=20&at=json
```

Trang nguồn ghi chú rằng response JSON thực tế có thể được trả như chuỗi JSON với `Content-Type: text/html`. Khi `ac=detail`, dữ liệu có URL phát/tập đầy đủ hơn.

Các đường dẫn tương thích được trang mô tả thêm:

```text
/api.php/provide/vod/from/snm3u8
/api.php/provide/vod/at/json
/api.php/provide/vod/at/xml
```

### 5.2. VOD RSS XML

```http
GET /api.php/provide/vod/from/snm3u8/at/xml
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `ac` | Không | string | `detail`, `list` | Mặc định `detail`; `detail` có link tập | `detail` |
| `at` | Không | string | `xml`, `json` | Khi `ac` khác `detail`, chọn XML hoặc JSON paginator | `xml` |
| `page` | Không | number | — | Trang, mỗi trang 20 bản ghi | `1` |
| `ids` | Không | string | — | ID phim, phân cách bằng dấu phẩy | `1` |
| `wd` | Không | string | — | Tìm theo tên/tên gốc | `phim` |
| `h` | Không | number | — | Lọc bản ghi cập nhật trong N giờ | `48` |

URL mẫu:

```text
https://nguon.vsphim.com/api.php/provide/vod/from/snm3u8/at/xml?ac=detail&page=1
```

Response là XML khi chạy đúng. Trang nguồn cũng cảnh báo tab response của UI có thể báo lỗi vì code mẫu cố parse JSON.

### 5.3. VOD RSS XMLSEA

```http
GET /api.php/provide/vod/from/snm3u8/at/xmlsea
```

Đây là URL tương thích crawler `xmlsea`, được mô tả là xử lý giống RSS XML và nhận cùng nhóm query `ac`, `at`, `page`, `ids`, `wd`, `h`. Ví dụ mặc định:

```text
https://nguon.vsphim.com/api.php/provide/vod/from/snm3u8/at/xmlsea?page=1
```

Metadata của trang chỉ hiển thị tham số `page` với ví dụ `1`, nhưng phần mô tả nêu cả nhóm query ở trên.

### 5.4. Cung cấp bài viết

```http
GET /api.php/provide/art
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `at` | Không | string | `json`, `xml`, `josn` | Định dạng response | `json` |

Đường dẫn tương đương được mô tả: `/api.php/provide/art/at/xml`. Trang nguồn ghi endpoint hiện trả danh sách rỗng dạng placeholder.

### 5.5. Cung cấp diễn viên

```http
GET /api.php/provide/actor
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `ac` | Không | string | `list`, `detail` | Kiểu danh sách/chi tiết; mặc định `list` | `list` |
| `at` | Không | string | `json`, `xml`, `josn` | Định dạng | `json` |
| `pg` | Không | number | — | Số trang | `1` |
| `limit` | Không | number | 1–200 | Kích thước trang | `20` |
| `ids` | Không | string | — | ID diễn viên, phân cách bằng dấu phẩy | `1` |
| `wd` | Không | string | — | Tìm theo tên hoặc slug | `ngôi sao` |
| `h` | Không | number | — | Lọc cập nhật trong N giờ | `24` |

Đường dẫn dạng `/api.php/provide/actor/at/json|xml` cũng được mô tả.

### 5.6. Dữ liệu Ajax

```http
GET /index.php/ajax/data.html
```

| Tham số | Bắt buộc | Kiểu | Giá trị | Mô tả | Ví dụ |
|---|---:|---|---|---|---|
| `mid` | Không | number | `1`, `2`, `3` | `1` = phim, `2` = bài viết, `3` = topic; tài liệu ghi chỉ `mid=1` có dữ liệu | `1` |
| `page` | Không | number | tối đa 20 | Số trang | `1` |
| `limit` | Không | number | tối đa 50 | Số bản ghi | `20` |

URL mẫu: `https://nguon.vsphim.com/index.php/ajax/data.html?mid=1&page=1&limit=20`.

### 5.7. Thu thập API

```http
POST /api.php
```

Trang nguồn không công bố tham số request. Mô tả ghi endpoint chấp nhận POST hoặc GET nhưng method hiển thị là `POST`; hiện trả `403` JSON theo metadata trang, trong khi lần gọi trực tiếp ngày 2026-09-10 trả `404`. Đây là endpoint collect không mở public, không dùng trong provider.

## 6. Ví dụ client tối thiểu

### cURL

```bash
curl --request GET \
  --url 'https://nguon.vsphim.com/api/tim-kiem?keyword=avengers&limit=20&page=1' \
  --header 'accept: application/json'
```

### Kotlin/NiceHttp — phác thảo cho provider

```kotlin
val url = "https://nguon.vsphim.com/api/phim/$slug"
val response = app.get(
    url,
    headers = mapOf("Accept" to "application/json")
).parsedSafe<VsphimMovieResponse>()
```

Khi dùng trong CloudStream, nên giữ `Referer` và `User-Agent` theo chính sách của provider nếu API hoặc URL phát yêu cầu; không suy ra URL HLS từ `link_embed` nếu chưa có extractor/endpoint hợp lệ.

## 7. Ghi chú triển khai cho dự án YanHH3D

- API chính có thể dùng làm nguồn dữ liệu JSON thay thế cho các flow home, search, list, detail nếu muốn; list response đã đủ `slug`, poster, title và year.
- Detail response cung cấp metadata phim và server/tập trong `episodes[].server_data[]`.
- `link_embed` là URL embed, không phải bảo đảm là `.m3u8`. Flow phát cần extractor hoặc parser HTML hợp lệ.
- Không hard-code domain ngoài một constants/resolver. Base URL hiện tại là `https://nguon.vsphim.com`.
- Parse `status` linh hoạt vì endpoint list trả boolean `true`, còn endpoint danh mục trả string `"success"`.
- Parse `totalItemsPerPage` linh hoạt vì có thể là number hoặc string.
- Với bộ lọc không có kết quả, API đã quan sát thấy response HTTP 200 với `items: []` và `pagination.totalItems: 0`.
- UI tài liệu ghi “sắp xếp theo nhiều tiêu chí” ở phần giới thiệu, nhưng metadata endpoint `/api/danh-sach` không khai báo tham số sort/order/by. Chỉ dùng các tham số được liệt kê cho endpoint đó cho tới khi xác minh route live.

## 8. Nguồn và lịch sử kiểm tra

- Tài liệu gốc: [https://nguon.vsphim.com/api-document](https://nguon.vsphim.com/api-document)
- Metadata endpoint được nhúng trong HTML của trang, gồm `path`, `query`, `method`, `desc` và `parameters`.
- Các response schema trong tài liệu này được đối chiếu bằng request GET thực tế tới các endpoint `/api/*` với dữ liệu mẫu; response mẫu đã được rút gọn và thay nội dung phim bằng placeholder.
- Cần kiểm tra lại nhóm MacCMS trước mỗi lần sử dụng vì tại snapshot này các route đều không truy cập được.
