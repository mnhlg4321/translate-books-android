# v4.22 — Yêu cầu làm việc: Biên tập dùng được (giao diện, tiến trình, glossary nhận nhầm) và phát hành 4.22.0

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (mục 5, khối v4.22). Checklist: `release_checklists/v4.22.md`. Nhánh duy nhất: `feature/v4.22`, worktree `D:\P5E-builds\wt-v4.22` (từ `main` `7c44f3a2`, bản phát hành 4.21.0). Không làm trong `D:\App Translate Books`.

## 1. Vấn đề cần giải quyết (owner dùng 4.21.0 trên điện thoại, 2026-10-14)

| # | Owner thấy | Nguyên nhân trong mã |
|---|---|---|
| U1 | Màn tạo lượt rối; Glossary/Pronoun mặc định "Không dùng"; muốn 4 file là mặc định và đổi chế độ bằng ô tick | `EditorialApiPageFactory.java:129-146` (nút "Hai file"/"Job Dịch", hai dòng "Không dùng" + "Đổi"); `EditorialApiCombo` lưu `glossaryId`/`pronounId` rỗng mặc định |
| U2 | Màn kết quả "một mớ chữ" | `EditorialPairPageFactory`/`EditorialPairPresenter` in nguyên chuỗi luật nội bộ (`ADDRESS: Pronoun row … áp dụng trong scope…`) và mã trạng thái (`KEPT (MODEL_KEEP)`); không có tóm tắt, không có danh sách dòng đã đổi |
| U3 | Cài đặt Biên tập thiếu so với luồng dịch | Tổ hợp chỉ có tên, model, trần chi phí (`EditorialApiCombo.Settings`); `SettingsPageFactory` có các mục General/Translation/Provider/Prompt/Performance/Appearance nhưng không có mục Biên tập |
| P | Theo dõi tiến trình kém, không dùng lại luồng dịch | Biên tập chạy trên `Thread` gắn màn hình (`EditorialPairUiController.java:134`), không thông báo; luồng dịch có `TranslatorService` (foreground + notification, `TranslatorService.java:214`, `:917`) và tab Jobs (`JobsPageFactory`: thanh tiến độ, tạm dừng/chạy tiếp, chunk lỗi) |
| G | Bản cuối đổi "Hắn" → "Potimas" không cần thiết; điểm glossary giả | Chẩn đoán Q0 chương 013 (`D:\P5E-private\v422-q0\`): `FixPointFinder` (khối `missingTerms`, quanh dòng 118–150) tạo điểm GLOSSARY khi RAW có nguồn và DRAFT thiếu dạng đích: (a) `白` trong `白い悪魔` là màu; (b) `白いの` đã có hàng pronoun gọi "cô tóc trắng"; (c) DRAFT dùng đại từ "hắn" đúng nhưng bị ép thành tên |

Quyết định owner 2026-10-14: duyệt U1, U2, U3, P, G cho 4.22; chế độ "4 file" = RAW + DRAFT + Glossary + Pronoun (mặc định), "2 file" = chỉ RAW + DRAFT, nguồn "Job Dịch" giữ làm lựa chọn thêm. Q1 (phát hiện theo người nói) là yêu cầu riêng `docs/V422_Q1_SPEAKER_DESIGN_REQUEST.md`, không chặn 4.22.

## 2. Quyền

- Không gọi provider; mọi kiểm tra dùng provider giả, USD 0.
- Thiết bị: chỉ `emulator-5554`. **Cài lên điện thoại `15e84958` cần owner duyệt riêng cho từng lần** (mục 4, bước 6).
- Push `feature/v4.22` sau khi quét. Không tag, không merge cho tới khi owner duyệt phát hành (mục 4, bước 7).

## 3. Phạm vi và ràng buộc

- **Không đổi schema cơ sở dữ liệu** trong 4.22 (dữ liệu thật của owner trên điện thoại; bản phát hành không cho sao lưu bằng `run-as`). Mọi trạng thái mới lưu trong cột/JSON đã có (`settingsJson` của tổ hợp, `AppSettings`) hoặc SharedPreferences.
- Không đổi lõi chạy `EditorialPairRunService` ngoài phần G và bộ lọc loại kiểm tra (U3). Hợp đồng nâng `CP-IMPL-20` nếu prompt hoặc điểm thay đổi.
- Dùng lại thành phần sẵn có (`sectionCard`, thẻ job, notification channel, `PromptPreviewDialog`); không làm lại cả app, không đổi luồng dịch.
- Mọi chữ hiển thị bằng tiếng Việt; mã nội bộ chỉ hiện trong phần "Chi tiết".

## 4. Các bước

**1. U1 — Màn tạo lượt.**
- Mặc định chế độ 4 file: bốn dòng chọn file hiện sẵn (RAW, DRAFT, Glossary, Pronoun), mỗi dòng có nút "Chọn file" và tên file đã chọn.
- Ô tick "Chỉ dùng 2 file (không Glossary/Pronoun)"; khi tick, ẩn hai dòng Glossary/Pronoun. Nhớ lựa chọn lần trước (SharedPreferences).
- Nguồn DRAFT: "Chọn file" hoặc "Từ job dịch" (giữ chức năng `pickJob` hiện có).
- Trước khi chạy ở chế độ 4 file mà thiếu Glossary hoặc Pronoun: cảnh báo, cho chọn "Chọn file" hoặc "Chạy 2 file".
- Test: test UI/presenter host cho mặc định, ô tick, nhớ lựa chọn, cảnh báo.

**2. U2 — Màn kết quả.**
- Đầu trang: một dòng tóm tắt bằng số (ví dụ "1 dòng đã sửa · 19 điểm nghi ngờ · 18 giữ nguyên · 0 bị chặn · chi phí USD …"), trạng thái chương (đủ đoạn / dừng ở đoạn n).
- Danh sách "Dòng đã sửa": trước → sau, tô từ thay đổi. Bộ lọc: Đã sửa / Giữ nguyên / Bị chặn.
- Lý do viết bằng tiếng Việt (bảng ánh xạ cho mọi mã `ADDRESS_*`, `GLOSSARY_TARGET_REMOVED`, `PROTECTED_SYMBOL_CHANGE`, `MODEL_KEEP`, …; mã lạ hiện "Lý do khác"). Luật gốc, RAW và mã nội bộ chỉ trong "Chi tiết" thu gọn.
- Nút xuất và mở bản cuối giữ nguyên chức năng.
- Test: presenter host với kết quả tổng hợp (có/không sửa, bị chặn, chưa tới), test bảng ánh xạ phủ hết mã lý do hiện có (dò bằng hằng số/chuỗi trong engine).

**3. U3 — Mục "Biên tập" trong Settings.**
- Thêm mục vào `SettingsPageFactory.CATEGORIES`: model và mức suy luận (mặc định theo provider/model của luồng dịch nếu để trống), trần chi phí mỗi chương, bật/tắt từng loại kiểm tra (Xưng hô / Thuật ngữ / Chữ Nhật còn sót / Dấu câu theo RAW), xem trước prompt của một đoạn (dùng lại `PromptPreviewDialog`).
- Giá trị lưu ở `AppSettings` (mặc định = hành vi 4.21.0). Tổ hợp vẫn có thể ghi đè model/trần chi phí.
- Lõi: loại kiểm tra bị tắt thì service bỏ các điểm loại đó trước khi dựng prompt (chunk không còn điểm thì không gọi model); "Dấu câu" tắt thì không áp `RawPunctuation`.
- Test: host cho lưu/đọc settings, bộ lọc loại điểm trong service (fake provider đếm 0 lời gọi khi mọi loại tắt).

**4. P — Biên tập chạy như một job.**
- Dịch vụ foreground riêng cho Biên tập (theo mẫu `TranslatorService`: notification channel, `startForeground`, tiến độ "Đoạn i/n · USD …"), chạy `EditorialPairRunService` hiện có; UI chỉ quan sát.
- Tab Jobs có thêm thẻ cho lượt Biên tập (đọc từ store Biên tập sẵn có, không bảng mới): tên tổ hợp, thanh tiến độ theo đoạn, chi phí, trạng thái; nút "Hủy sau đoạn hiện tại" (dùng `cancel()` sẵn có), "Chạy tiếp" (chạy lại lượt; service đã bỏ qua đoạn xong), "Mở kết quả".
- Màn Biên tập hiển thị cùng thẻ tiến độ đó thay cho khối "Đang biên tập từng đoạn" hiện tại.
- Process bị dừng giữa chừng: khi mở lại, thẻ hiện "Đã dừng ở đoạn i/n — Chạy tiếp".
- Test: host cho trạng thái thẻ; androidTest cho service (fake provider) bắt đầu/hoàn tất/hủy/chạy tiếp, và force-stop giữa chừng rồi mở lại (cùng mẫu restart seed/verify hiện có).

**5. G — Glossary nhận nhầm.** Chọn quy tắc nhỏ nhất cho ba ca, đo trước khi chọn:
- (a) Nguồn một ký tự (như `白`) không tạo điểm khi trong RAW nó là một phần của từ dài hơn không phải tên (ví dụ tính từ `白い` + danh từ).
- (b) Vị trí trùng `from` của một hàng pronoun (như `白いの`) thì hàng pronoun quyết, không tạo điểm glossary.
- (c) Mục glossary loại nhân vật không tạo điểm "thiếu thuật ngữ" khi dòng DRAFT nhắc tới nhân vật đó bằng đại từ ngôi ba (hắn, anh ta, cô ấy, lão, …), tức là không ép đổi đại từ thành tên.
- Đo offline bằng finder thật trên 8 chương JAKUAKU (`D:\P5E-private\v421-p2\inputs`) và chương 013 Kumo (`D:\P5E-private\v422-q0\inputs`): số điểm GLOSSARY trước/sau; số điểm bị bỏ mà owner từng sửa thuật ngữ ở dòng đó trong FINAL (phải bằng 0 hoặc giải thích từng ca). Nâng `CP-IMPL-20`.
- Test: engine với câu tổng hợp cho (a), (b), (c) và đối chứng (tên thật bị thiếu vẫn tạo điểm).

**6. Bản dev cho owner bấm thử.** Sau khi 1–5 xanh trên archive sạch: build dev bằng `scripts/build-and-save.ps1` (series `4.22-dev`, versionCode > 271), lưu hai nơi; QA trên `emulator-5554` với provider giả (các nhóm như RC 4.21.0 + test mới); chụp ảnh màn tạo lượt, tiến trình, kết quả, Settings và tab Jobs. Dừng, gửi owner ảnh + đường dẫn APK. **Chỉ khi owner duyệt riêng**: cài lên điện thoại bằng `scripts/install-validated.ps1` (`-CheckOnly` trước, `install -r`, không gỡ, không xóa; bản phát hành không cho sao lưu `run-as`, nên owner tự xuất dữ liệu quan trọng trước).

**7. Phát hành 4.22.0** sau khi owner duyệt bản dev: làm như `docs/V421_E_RELEASE_WORK_REQUEST.md` (E1–E9: hồi quy, ghi chú phát hành, RC `-ExactReleaseVersion 4.22.0`, QA thiết bị, hiệu năng thật, ảnh/video, PreTag + `build-*`, tag/merge/push). Tag/merge chỉ khi owner duyệt ở bước này.

## 5. Tiêu chí

- PASS từng bước: test mới và cũ xanh trên archive sạch của commit (engine, app debug/release/benchmark, lint 0 lỗi, androidTest compile); không đổi schema DB; G có số liệu trước/sau và không mất điểm glossary owner từng sửa (hoặc giải thích).
- Owner là cổng chấp nhận giao diện: sau bước 6, owner xác nhận màn tạo lượt, kết quả, Settings, tiến trình dùng được.
- FAILED_REPAIRING cho lỗi cục bộ; sửa trong phạm vi mục 3.

## 6. Không làm

Không gọi provider, không đổi luồng dịch, không đổi schema DB, không triển khai Q1 trong yêu cầu này, không cài lên điện thoại khi chưa có duyệt riêng, không tag/merge trước duyệt phát hành, không thêm bộ kiểm/cổng đo mới.
