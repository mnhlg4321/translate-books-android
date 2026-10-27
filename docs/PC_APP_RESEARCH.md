# Nghiên cứu: bản PC cùng phiên bản với app Android

> **Owner đã duyệt phương án A ngày 2026-10-27.** Việc đầu tiên là P0a: `docs/V424_P0A_GOLDEN_PROMPT_WORK_REQUEST.md`.

**Quyết định sản phẩm được phục vụ:** có làm bản Windows chạy song song với bản điện thoại hay không, và làm theo cách nào để mỗi tính năng chỉ phải viết một lần.

**Phạm vi tài liệu:** chỉ nghiên cứu và đề xuất. Không sửa mã, không gọi provider. Số liệu đo trên `feature/v4.23` tại `7650d9ff` (2026-10-27).

## 1. Hiện trạng mã (đo bằng script, không ước lượng)

App Android có 279 tệp Java, 43.150 dòng, trong `app/src/main/java/com/ml/tblandroidtxt/`.

| Nhóm | Tệp | Dòng | Chạy được trên PC? |
|---|---:|---:|---|
| Dịch — không import Android (prompt, chunk, validator, chi phí, client provider, mô hình trạng thái) | 85 | 7.436 | Khoảng 60 tệp chuyển được ngay; khoảng 25 tệp còn gọi lớp gắn Android (xem dưới) |
| Dịch — gắn Android nhưng không phải giao diện (lưu trữ, tệp, dịch vụ chạy nền) | 32 | 6.665 | Có, sau khi tách lớp thích ứng |
| Dịch — giao diện (`MainActivity` 4.739 dòng và các `*PageFactory`) | 20 | 9.145 | Không, phải viết lại |
| Biên tập (đang đóng băng) | 142 | 19.904 | Để sau |

**Điểm thuận lợi đã kiểm:**
- Thư viện mạng `okhttp3` chạy trên Java desktop. `org.json` có sẵn trong Android, còn trên PC phải thêm thư viện `org.json:json`. Test JVM của app đang dùng đúng thư viện đó (`app/build.gradle:80`). Hiện 730 test của app chạy trên JVM của PC, không cần điện thoại.
- `editorial-engine` đã là một module `java-library` thuần Java (163 tệp) mà app Android dùng lại. Mô hình "lõi chung, vỏ riêng" đã có sẵn trong dự án.
- Phần dựng prompt sạch: `PromptContextBuilder` (1.011 dòng), `PromptPlan` và `PromptBuilder` không phụ thuộc Android.
- Phản biện tìm ra khoảng 25 trong 85 tệp "không import Android" vẫn gọi lớp gắn Android cùng package. Đã kiểm lại vài chỗ:
  - `Chunker:307` và `CostEstimator:41` gọi `ModelCatalog`, lớp này dùng `Context`;
  - `CostEstimator:73` và `OpenAICompatibleClient` ghi qua `ObservabilityLog`, lớp này dùng `Context`;
  - `AppBuildInfo:5` đọc `BuildConfig`, lớp mà Android sinh ra cho riêng module app;
  - `AppSettings:7` có trường `EditorialPreferences`;
  - một số mô hình như `JobPicker`, `RunGroups` và `DrawerRecentModel` gọi `TranslationRepository` hoặc các `*Store`.

  Vì vậy phải tách trước `ModelCatalog`, `ObservabilityLog`, `FileUtil`, `AppBuildInfo` và phần Editorial trong `AppSettings`, rồi mới chuyển được trọn bộ.

**32 tệp gắn Android chỉ dùng ít API:**

| API | Số tệp | Thay trên PC bằng |
|---|---:|---|
| `Context` | 28 | đối tượng cấu hình thư mục dữ liệu |
| `SharedPreferences` | 10 | tệp cài đặt JSON |
| `Uri` / `ContentResolver` | 11 | `java.nio.file.Path` |
| `SQLiteDatabase` / `Cursor` / `ContentValues` | 4–6 | `sqlite-jdbc`, cùng một lược đồ DB |
| `Service` / `Intent` / `NotificationManager` | 4 | luồng nền trong app PC |

Tệp nặng nhất cần tách: `TranslatorService` (1.233 dòng), `TranslationRepository` (909), `FileUtil` (567), `TranslationEngine` (539).

**Công cụ PC owner đang có:** `D:\Apps\APP\TranslateBook-Windows` là bản Windows của dự án mã nguồn mở TranslateBooksWithLLMs ("TBL PC", Python, giao diện web ở `localhost:5000`). App Android ra đời từ đó, nhưng đã khác hẳn về prompt, lock Glossary/Pronoun, chunk, validator và trần chi phí. Vì vậy TBL PC **không** phải "cùng phiên bản".

## 2. Các phương án

| | Cách làm | Dùng lại mã Android | Rủi ro cho app điện thoại | Công sức | Kết luận |
|---|---|---|---|---|---|
| **A** | Tách lõi dịch thành module Java chung; app PC viết bằng Java với giao diện JavaFX, đóng gói bằng `jpackage` (kèm sẵn Java; `.msi` cần thêm WiX) | Phần lớn logic không phải giao diện (dưới 14k dòng); chỉ viết lại giao diện | Thấp, nếu tách lõi theo kiểu "chỉ di chuyển", có test chứng minh prompt không đổi | Vừa | **Khuyến nghị** |
| B | Chuyển cả hai sang Kotlin + Compose Multiplatform, dùng chung cả giao diện | Lõi + giao diện | Cao: viết lại 9.145 dòng giao diện đang chạy tốt, đổi ngôn ngữ | Lớn | Không: tốn gấp nhiều lần, đụng bản đang dùng |
| C | Lõi Java chung + máy chủ cục bộ + giao diện web trong trình duyệt (giống TBL PC) | Lõi | Thấp | Vừa | Khả thi, nhưng phải duy trì thêm HTML/JS và API cục bộ; không lợi hơn A |
| D | Chạy APK hiện có trên PC bằng giả lập (Android Studio Emulator, BlueStacks) | 100% | Không | Gần 0 | **Tạm dùng ngay được**, nhưng không phải app PC thật: chọn tệp khó, chậm |
| E | Dùng TBL PC (Python) | 0% | Không | — | Không: khác prompt, lock và trần chi phí |

**Vì sao chọn A:**
- Cùng ngôn ngữ, công cụ build và bộ test. Codex và người điều phối không phải học thêm gì.
- Một thay đổi ở prompt, validator hay chi phí tự áp dụng cho cả hai bản, nên hai bản luôn "cùng phiên bản" về chất lượng dịch.
- Chỉ phần giao diện là viết riêng. Đây cũng là phần nên khác nhau: PC có màn hình rộng, chuột và bàn phím; điện thoại có ngăn ☰ và cảm ứng.
- JavaFX hỗ trợ CSS, nên 5 giao diện màu (bảng mã màu U6) và cỡ chữ dùng lại được dưới dạng token.

## 3. Kiến trúc đề xuất

```
translation-core   (java-library, mới)
  ├─ prompt, chunk, validator, chi phí, client provider, mô hình trạng thái  ← chuyển nguyên từ app
  └─ cổng (interface): SettingsPort, DatabasePort, DocumentPort, SecretPort, RunHost, Clock/Log
app                (Android)  → hiện thực các cổng bằng SharedPreferences, SQLiteDatabase, Uri, Service
desktop            (JavaFX, mới) → hiện thực các cổng bằng tệp JSON, sqlite-jdbc, Path, luồng nền
editorial-engine   (giữ nguyên, đóng băng)
```

**Ghi chú kỹ thuật:**
- Lõi khai báo `org.json:json` cho PC. Riêng app Android không đóng gói thư viện này vào APK mà dùng bản có sẵn của Android (loại trừ trong `:app`), và phải kiểm danh sách thư viện APK không đổi.
- Lõi giữ package `com.ml.tblandroidtxt`. Đặt cùng package ở hai module chạy được trên classpath thường (Android và `jpackage` không dùng module JPMS), nên các thành phần package-private vẫn gọi được. Nếu sau này bật JPMS thì phải đổi.
- `AppBuildInfo` nhận phiên bản qua một đối tượng được truyền vào, thay cho `BuildConfig`.
- `sqlite-jdbc` mang theo phiên bản SQLite riêng. "Cùng lược đồ DB" phải có test mở chéo: DB tạo trên điện thoại mở được trên PC, kể cả `PRAGMA user_version` và chế độ WAL.
- DPAPI từ Java cần thư viện JNA (`Crypt32Util`).

**Quy tắc chung:**
- Cùng `versionName`, ví dụ `4.24.0`. `versionCode` chỉ là của Android.
- Cùng lược đồ DB và định dạng Glossary/Pronoun/instruction, nên tệp từ điện thoại mở được trên PC và ngược lại.
- **API key trên PC:** không lưu chữ thường trong tệp cài đặt. Dùng Windows DPAPI (mã hóa theo tài khoản Windows), hoặc tối thiểu một tệp riêng ngoài thư mục dữ liệu, không bao giờ nằm trong bản sao lưu hay bằng chứng QA.
- **Build PC theo chính sách artifact hiện có:** có script lưu bản build, đánh số và SHA-256 vào `artifacts/` và `backup/`.

## 4. Lộ trình theo giai đoạn (mỗi giai đoạn có điểm dừng)

| GĐ | Việc | Tiêu chí PASS | Đụng app điện thoại? |
|---|---|---|---|
| **P0a** | **Viết test "prompt vàng" trên commit chưa tách.** Từ bộ đầu vào mẫu `QA_` (RAW, Glossary, Pronoun, instruction, cài đặt) ghi lại nguyên system/user prompt, danh sách chunk và ước tính chi phí. Hiện chưa có test nào so nguyên văn prompt; `PromptContextQaTest` chỉ so số đếm | Tệp vàng được commit; test qua trên mã hiện tại | Không, chỉ thêm test |
| **P0b** | Tạo `translation-core`; chuyển khoảng 60 tệp không chạm lớp gắn Android; tách `ModelCatalog`, `ObservabilityLog`, `AppBuildInfo` và phần Editorial trong `AppSettings` bằng cổng | 730 test và test prompt vàng vẫn qua, giống từng byte. Danh sách thư viện APK không đổi. APK cài lên máy chạy như cũ | Có, đổi cấu trúc nhỏ |
| **P1a** | Tách cổng lưu trữ và tệp: `*Store`, `TranslationRepository`, `FileUtil`, `SettingsStore` | Như P0b, cộng test lõi chạy với cổng giả trên JVM | Có |
| **P1b** | Tách logic chạy / dừng / tiếp tục / checkpoint khỏi `TranslatorService` (1.233 dòng) và `TranslationEngine` vào lõi, để Service chỉ còn là vỏ Android. Đây là phần rủi ro nhất | Như P0b; thử Dừng/Tiếp tục/chunk lỗi trên điện thoại không tốn tiền | Có |
| **P2** | App PC tối thiểu: Cài đặt (provider, model, key, trần chi phí), Thư viện (nhập RAW/Glossary/Pronoun từ thư mục), dịch 1 chương có tiến trình, Dừng/Tiếp tục, danh sách job | Test PC so prompt với Android cho cùng đầu vào (offline). Một lượt dịch thật cần owner duyệt chi riêng | Không |
| **P3** | Dịch cả tập, Lịch sử, xuất tệp, 5 giao diện màu, đóng gói `.msi` | Cài trên PC sạch, chạy không cần cài Java; thử theo checklist | Không |
| Sau | Biên tập trên PC (khi Biên tập được mở lại); chuyển dữ liệu giữa PC và điện thoại | — | — |

**Khi nào bắt đầu:** sau khi v4.23 được chốt. P0/P1 đổi cấu trúc mã của app điện thoại, nên không trộn vào bản đang phát hành. Đề xuất làm trên nhánh `feature/v4.24` riêng, mỗi giai đoạn một gói việc cho Codex, và người điều phối kiểm độc lập như các gói trước.

**Trong lúc chờ:** nếu cần dịch trên PC ngay, dùng phương án D, tức chạy APK hiện tại trong Android Studio Emulator. Không cần sửa mã.

## 5. Rủi ro và cách chặn

| Rủi ro | Chặn bằng |
|---|---|
| Tách lõi làm đổi prompt hoặc hành vi dịch trên điện thoại | Test prompt vàng viết trước khi tách (P0a); mỗi bước tách nhỏ; cài thử trên điện thoại cần owner duyệt như mọi lần |
| Hai bản lệch tính năng theo thời gian | Mọi logic không phải giao diện nằm trong lõi; gói việc nào đổi lõi phải chạy test của cả hai bản |
| Lộ API key hoặc văn bản sách trên PC | DPAPI cho key; thư mục dữ liệu nằm ngoài Git; quy tắc bằng chứng QA giữ nguyên |
| `MainActivity` 4.739 dòng còn trộn logic với giao diện | Mỗi lần gặp logic trong giao diện thì chuyển vào lõi (đã có mẫu: `RunControlModel`, `DrawerRecentModel`, `RecommendationUndoModel`) |
| Đụng Biên tập đang đóng băng | Lõi dịch không gồm tệp `Editorial*`. Trường `EditorialPreferences` trong `AppSettings` được tách ra phía app, không sửa lớp Editorial; `editorial-engine` giữ nguyên |

## 6. Quyết định cần owner

1. **Chọn phương án A** (lõi chung + app PC JavaFX)? Khuyến nghị: có.
2. **Thời điểm:** bắt đầu P0 trên `feature/v4.24` sau khi chốt v4.23? Khuyến nghị: có. Chưa làm song song với v4.23.
3. **Phạm vi bản PC đầu tiên:** chỉ dịch (P2–P3), Biên tập để sau? Khuyến nghị: chỉ dịch.
