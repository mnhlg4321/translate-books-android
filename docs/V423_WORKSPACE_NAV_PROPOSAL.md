# Translate Books — Đề xuất cách làm việc theo "Tập"

**Trạng thái:** ĐÃ PHẢN BIỆN. Agent phản biện đối chiếu với mã `feature/v4.23` và lịch sử dự án, 2026-10-21.

**Nguồn:**
- Câu hỏi của owner (2026-10-21): cách làm việc hiện tại có hợp lý không; owner thấy cửa sổ trái kiểu ChatGPT dễ thao tác hơn.
- `QA_REPORT_v2_6_8.md`, `CHANGELOG.md`.
- Skill `ui-ux-pro-max`.

Đi kèm `docs/V423_REFINED_UI_DESIGN.md`, nói về phong cách hiển thị.

## 1. Owner làm việc thế nào

Owner làm theo **tập sách** (ATE_JP_VOL6, KUMO_VOL10…), mỗi tập 10–30 chương; mỗi chương có RAW, Glossary và Pronoun riêng. Vòng việc lặp lại:

1. Nhập tệp và ghép chương.
2. Dịch hàng loạt.
3. Xử lý chương lỗi.
4. Biên tập từng chương (DRAFT → FINAL).
5. Xuất.

## 2. Hiện trạng — cái gì ổn, cái gì vướng (đã kiểm theo mã)

| Việc | Hiện tại | Nhận xét |
|---|---|---|
| Dịch tiếp chương chưa dịch | **~3 chạm**: màn Dịch nhớ tổ hợp đã chọn (`TranslateSourceFactory:70,76`) và tự tick chương chưa dịch (`:147–149`). Từ Jobs có "Tiếp tục tổ hợp" (`JobsMainPageFactory:152`), 2 chạm | ổn |
| Retry chương lỗi | **2–3 chạm** trên thẻ lượt ở Jobs. Lượt đã ghi tên tổ hợp, nhưng chưa **lọc** được theo tổ hợp | thiếu bộ lọc |
| Sửa Glossary chương 5 | Library → Glossaries → tìm → Edit, ~4 chạm và phải rời màn đang làm | vướng vừa |
| **Biên tập chương 5** | Biên tập → + Tổ hợp mới → chọn job (danh sách **mọi** job, không lọc) → chọn Glossary → chọn Pronoun → Lưu → Chạy: **8–10 chạm**, chọn lại đúng những tệp đã ghép ở Library | **vướng lớn nhất** |
| Nhìn cả tập một chỗ | Không có. Trạng thái một chương rải ở Jobs, Biên tập, Library | thiếu |

**Gốc vấn đề:** hai khái niệm cùng tên "Tổ hợp" nhưng không nối với nhau.
- Library > Tổ hợp (`ChapterSetStore`) là bộ chương để dịch.
- Biên tập > Tổ hợp (`EditorialApiCombo`) là một chương để biên tập.

## 3. Bài học v2.6.7 và vì sao không làm ngăn kéo vuốt trên điện thoại

- v2.6.7 thay thanh tab bằng ngăn kéo trái chứa các đích chính. Owner thấy "chức năng như bị mất", nên v2.6.8 phải hoàn nguyên (`QA_REPORT_v2_6_8.md:76`; dòng cấm `MainActivity:639`).
- Trên Android 10+ dùng điều hướng bằng cử chỉ, **mép trái màn hình là cử chỉ Back của hệ thống**. Ngăn kéo vuốt từ mép trái sẽ tranh cử chỉ với Back. Muốn tránh thì phải xin vùng loại trừ cử chỉ, và vùng này bị giới hạn 200 dp mỗi mép.
- Thanh trái của ChatGPT dễ dùng vì nó là **danh sách việc** (các cuộc trò chuyện). Trên điện thoại, ChatGPT mở danh sách đó bằng nút ở tiêu đề, không bắt vuốt.

## 4. Đề xuất

### 4.1 Tab mới "Tập" — danh sách tập là màn chính (kiểu hộp thư)

```
Tập                                  ⚙
┌──────────────────────────────────────┐
│ 🔍 Tìm tập              + Tập mới      │
│ ĐANG CHẠY                              │
│  ATE_JP_VOL6   ▮▮▮▯▯▯ 4/14 · $0.12  ›  │
│ GẦN ĐÂY                                │
│  KUMO_VOL10    ✓ 18/18 · BT 6/18    ›  │
│  RAW_11        × 2 lỗi · 16/31      ›  │
└──────────────────────────────────────┘
Tập · Dịch · Công việc · Biên tập · Thư viện
```

- **Thanh dưới vẫn 5 tab:** **Tập · Dịch · Công việc · Biên tập · Thư viện**. Cài đặt chuyển lên biểu tượng ⚙ trên thanh trên, luôn nhìn thấy.
- Không bỏ tab nào trong v4.23; Dịch và Biên tập vẫn còn nguyên. Dịch tệp lẻ vẫn ở tab Dịch.
- "Tập" là tên hiển thị của Tổ hợp dịch (`ChapterSetStore`), kèm phụ đề "(bộ chương)" trong giai đoạn chuyển. **Không đổi dữ liệu.** Chữ "Tổ hợp" bên Biên tập giữ nguyên để không đổi hai khái niệm cùng lúc.

### 4.2 Màn một tập

```
‹  ATE_JP_VOL6 ▾                      ⋮
   14 chương · 10 đã dịch · 0 lỗi · BT 2
   [ Dịch 4 chương còn lại ]            ← một nút chính theo ngữ cảnh
──────────────────────────────────────────
Ch.1  001_ATE_JP_VOL6   ✓ Dịch  ✓ BT    ›
Ch.2  002_ATE_JP_VOL6   ✓ Dịch  — BT    ›
Ch.4  004_ATE_JP_VOL6   × Lỗi   Retry   ›
Ch.11 011_ATE_JP_VOL6   — Chưa dịch     ›
```

- **Bấm tên tập (▾)** mở danh sách chuyển nhanh: các tập đang chạy và gần đây, giống nút chuyển cuộc trò chuyện của ChatGPT bản điện thoại. Không vuốt mép.
- Nút chính đổi theo trạng thái tập:
  - còn chương chưa dịch → "Dịch N chương còn lại" (dùng lại `continueChapterSet`);
  - đang chạy → "Xem tiến trình";
  - có chương lỗi → "Retry N chương lỗi".
- Trạng thái từng chương tính từ dữ liệu sẵn có, không đổi schema:
  - RAW có hay không: `LibraryRawStore.find`;
  - đã dịch: `completedHashes`;
  - dở hoặc lỗi: `unfinishedJobs`;
  - biên tập: tổ hợp Biên tập theo `jobId`.
- **Không có "Đã xuất"**, vì app không ghi lại việc xuất. Chỉ hiện "Có FINAL".
- Trạng thái tính ngoài luồng UI, gộp truy vấn theo lô, có khung chờ. Không lặp lại kiểu O(N²) của `TranslateSourceFactory.refresh` hiện tại.

### 4.3 Trang chương

- **Đầu trang:** RAW / Glossary / Pronoun dạng hàng. Bấm vào để xem, đổi hoặc **sửa Glossary tại chỗ**, thay cho 4 chạm vòng qua Library.
- **Dịch:** "Đã xong · 3 chunk · $0.012" kèm [Xem] [Dịch lại]; hoặc "Lỗi: bị cắt" kèm [Retry].
- **Biên tập:** "Chưa chạy" kèm **[Biên tập chương này]**. App tự điền RAW, DRAFT (job), Glossary và Pronoun, rồi mở màn Biên tập:
  - tìm tổ hợp đã có qua `listCombos()` cùng `jobId`, có thì dùng lại;
  - nếu chưa có thì `insertCombo` với `sourceKind = SOURCE_JOB`, `jobId`, `glossaryId`, `pronounId`;
  - sau đó `openCombo(id)`.

  Gọi các hàm sẵn có, **không sửa tệp `Editorial*`**, nhưng **có ghi dữ liệu Biên tập**, nên cần owner duyệt. Thiếu Glossary hoặc Pronoun thì mở màn tổ hợp và báo rõ, không tự chạy.
  - Từ 8–10 chạm xuống **2 chạm**.
- **Kết quả:** "Có FINAL" kèm [Xem] [Xuất].

### 4.4 Công việc, Thư viện, màn rộng

- **Công việc:** thêm bộ lọc theo tập, và nút "Mở tập" trên mỗi lượt.
- **Thư viện:** giữ để quản lý tệp dùng chung (nhập hàng loạt, xuất, xóa).
- **Màn rộng (≥ 720 dp):** trong tab Tập dùng bố cục **danh sách | chi tiết** (danh sách tập bên trái, tập đang mở bên phải). Thanh dọc 112 dp hiện có giữ nguyên, không thêm cột thứ hai.

## 5. Ràng buộc và rủi ro

- **Ghi dữ liệu Biên tập từ mã mới** (mục 4.3) cần owner duyệt. Có thể làm W1 trước và hoãn phần này.
- **"Đã dịch" tính theo hash RAW**, không theo tập. Một RAW nằm trong hai tập, hoặc dịch lại với Glossary khác, có thể bị báo "đã dịch" sai. Màn chương ghi rõ job và Glossary đã dùng.
- **Đọc trạng thái Biên tập:** `latestRun` đọc `SELECT *` gồm cả toàn văn, nên chỉ gọi cho chương đang mở. Danh sách tập dùng truy vấn nhẹ riêng, và truy vấn này phụ thuộc schema bảng Biên tập.
- **Back:**
  - thêm Tập → chương vào `AppBackNavigationPolicy`;
  - hiện Files/Settings bấm Back sẽ về tab Dịch (`:24–26`); việc này giữ nguyên;
  - manifest chưa bật predictive back, cần lo khi lên targetSdk 36.
- **Ẩn tab sớm** là đúng kiểu lỗi v2.6.7. Không bỏ tab Dịch/Biên tập nếu chưa có owner duyệt riêng sau một thời gian dùng.

## 6. Lộ trình

| Bước | Nội dung | Rủi ro |
|---|---|---|
| W0 | Công việc: lọc theo tập, nút "Mở tập". Chỉ đổi chữ/nhãn | thấp |
| W1 | Tab **Tập** chỉ đọc: danh sách tập → màn tập (trạng thái chương, nút chính dùng lại `continueChapterSet`). Cài đặt lên ⚙. Không bỏ tab nào | vừa |
| W2 | Trang chương; sửa Glossary/Pronoun tại chỗ; "Biên tập chương này" (nếu owner duyệt ghi tổ hợp Biên tập) | vừa |
| W3 | Nút chuyển tập trên tiêu đề; danh sách \| chi tiết ở màn rộng | thấp–vừa |
| W4 | (Tùy chọn, cần duyệt riêng sau một kỳ dùng) Gộp Dịch/Biên tập vào Tập. Chỉ làm sau khi Back và `fillSettings` thôi phụ thuộc tab Dịch | cao |

## 7. Kiểm thử

- Unit test `AppBackNavigationPolicy` cho Tập → chương → Back, và khôi phục tab đã lưu khi bộ tab đổi.
- Mô hình trạng thái chương bằng Java thuần, có các ca: một RAW thuộc hai tập, thiếu Glossary/Pronoun, job dở, có tổ hợp Biên tập.
- Điền sẵn Biên tập: dùng lại tổ hợp cùng `jobId`, không tạo trùng.
- Thiết bị (emulator): mở tập 30 chương mà không có việc dài trên luồng UI (đo bằng trace section).
- Trạng thái trống: chưa có tập nào → "Tạo tập từ RAW" và "Dịch tệp lẻ".
