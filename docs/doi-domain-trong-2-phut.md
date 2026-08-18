# Checklist Doi Domain Trong 2 Phut

Dung cho truong hop YanHH3D chi doi domain, con cau truc HTML va luong phat phim van nhu cu.

Neu site doi ca selector hoac doi cach tra source, file nay khong du. Khi do can ra soat them [plan.md](plan.md) va code trong `YanHHProvider/`.

---

## 1. Sua domain chinh

Mo [YanHH3DConstants.kt](../YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DConstants.kt) va sua:

- `DEFAULT_BASE_URL` thanh domain moi
- them domain cu vao dau sach `KNOWN_DOMAINS` neu chua co

Vi du:

```kotlin
const val DEFAULT_BASE_URL = "https://yanhh3d.xxx"

val KNOWN_DOMAINS = listOf(
    "yanhh3d.xxx",
    "yanhh3d.pw",
    "yanhh3d.love",
    "yanhh3d.ac",
)
```

Muc dich:

- request moi se di vao domain moi
- bookmark va lich su cu van duoc remap
- episode data dang path-only van tiep tuc dung duoc

---

## 2. Sua icon neu can

Mo [YanHHProvider/build.gradle.kts](../YanHHProvider/build.gradle.kts) va cap nhat `iconUrl` neu favicon cung chuyen host.

Neu favicon van o host cu hoac van truy cap duoc thi co the bo qua buoc nay.

---

## 3. Kiem tra domain co bi hardcode o dau khac khong

Chay:

```powershell
rg "yanhh3d\.pw|yanhh3d\.love|yanhh3d\.ac|yanhh3d\.xxx" YanHHProvider docs
```

Ky vong:

- domain that su chi nam o `YanHH3DConstants.kt`
- `iconUrl` trong `YanHHProvider/build.gradle.kts`
- tai lieu trong `docs/`

Neu thay domain cu nam trong `YanHH3DProvider.kt` hoac `YanHH3DParser.kt` thi nen doi ve dung qua constants/resolver.

---

## 4. Tang version de CloudStream nhan ban moi

Mo [YanHHProvider/build.gradle.kts](../YanHHProvider/build.gradle.kts) va tang:

```kotlin
version = ...
```

Neu khong tang version, CloudStream thuong se khong hien update.

---

## 5. Ghi changelog

Mo [CHANGELOG.md](../CHANGELOG.md) va ghi ngan gon:

- doi `DEFAULT_BASE_URL`
- them domain cu vao `KNOWN_DOMAINS`
- doi `iconUrl` neu co

---

## 6. Build nhanh

Neu may da co JDK 17 va Android SDK:

```powershell
.\gradlew.bat YanHHProvider:test
.\gradlew.bat YanHHProvider:make
```

Can co:

- test parser van xanh
- plugin build ra `.cs3` thanh cong

---

## 7. Kiem tra trong app

Toi thieu test tay 3 diem:

- mo mot phim da bookmark tu domain cu
- search mot phim moi
- mo mot tap va phat it nhat 1 nguon HLS

Neu bookmark cu vao duoc va phim moi phat binh thuong, thi logic remap domain van on.

---

## 8. Commit va push

Trinh tu an toan:

```powershell
git add YanHHProvider/src/main/kotlin/com/yanhh3d/YanHH3DConstants.kt YanHHProvider/build.gradle.kts CHANGELOG.md
git commit -m "Update YanHH3D domain"
git push origin main
```

Workflow se tu build lai va day artifact sang branch `builds`.

---

## Khi nao checklist nay khong du

Can ra soat them parser/provider neu gap mot trong cac dau hieu sau:

- home hoac search ra rong
- detail page mat nam, the loai, mo ta
- danh sach tap bien mat
- `loadLinks()` khong tim thay source
- video co link nhung khong phat

Khi do, kha nang cao la site khong chi doi domain ma con doi HTML, selector, header, hoac luong playback.
