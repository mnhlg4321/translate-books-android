# v4.23 — bản dev `4.23-dev.5` (code 280): UX tổ hợp từ 3 ô, bảng chọn tệp dùng chung, không lẹm tên tệp

Ngày 2026-10-18, nhánh `feature/v4.23`. Yêu cầu: `docs/V423_UX_COMBO_PICKER_WORK_REQUEST.md` (U1–U3). Provider giả, USD 0. Chỉ emulator-5554; điện thoại `15e84958` không bị chạm trong lượt này.

## 1. Bản dev
| Mục | Giá trị |
|---|---|
| Bản | `4.23-dev.5` / code 280, event `build-20261018-212435`, source `f887b2b3` |
| APK | `D:\App Translate Books\artifacts\builds\v4.23-dev.5\build-20261018-212435\TranslateBooks-v4.23-dev.5-code280.apk`, SHA-256 `56E300A454460E131032E2EA20199F055777B43ADB7359A13A31E0083DB7E5EB`, chứng chỉ `47F31389…EF3C155` |
| APK test | `v423-dev5-test-20261018`, `D62811F0…287020` (build từ `f887b2b3`) |
| Cài | `install-validated.ps1` emulator-5554, đọc lại code 280, signer `abebea4b` |

Vì sao là dev.5 chứ không phải dev.3: dev.3 (`96559ce5`) và dev.4 (`88f86721`) đã build và chạy test; mỗi bản lộ một lỗi hiển thị ở font 130% (tên tệp dài bị cắt ở giới hạn 3 dòng; nhãn nút "Chọn tất cả (đang lọc)" bị cắt trong nút cao cố định). Cả hai đã sửa; số bản không được dùng lại nên bản giao là dev.5. Báo cáo dev.4 giữ ở `artifacts/qa/v4.23-dev4-20261018/` (ảnh thể hiện lỗi cũ ở font 130%).

## 2. Thay đổi
- **U1:** `FilePickerModel` (lọc, tìm bỏ dấu, sắp gợi ý → số chương → tên, chọn một/nhiều, Hủy trả lại lựa chọn cũ), `FilePickerSheet` (bảng gần toàn màn hình: ô tìm, lọc Tất cả/Gợi ý/Đã chọn, vòng tròn chọn, "Đã chọn n", thanh dưới Hủy / Chọn (n); bấm dòng không đóng bảng; nhấn giữ hiện tên đầy đủ), `SelectedFilesSlot` (nhãn, chip trạng thái, nút Chọn/Đổi, mỗi tên một dòng, hơn 5 tên thì "và n tệp nữa").
- **U2:** Library > **Tổ hợp** (đổi tên từ "Bộ chương" trong luồng Dịch/Library): "+ Tạo tổ hợp mới", màn một cuộn với tên + ba ô RAW / Glossary / Pronoun (`ComboDraft`), "Ghép theo chương" ghi đủ tên ba tệp mỗi dòng với ✓ Đủ / ⚠ Thiếu Pronoun / ✕ Hai Glossary cùng chương N; chỉ ghép trong tệp đã chọn ở từng ô; một RAW + một Glossary + một Pronoun ghép thẳng không cần số chương; cảnh báo tệp không khớp chương nào (+ "Bỏ khỏi ô"); "Đổi" mở bảng chọn một; thanh dưới cố định Hủy / Lưu tổ hợp (n chương), lưu bị chặn thì cuộn tới dòng ✕; sửa tổ hợp đã lưu không ghép lại; danh sách ghi "3 chương · ✓ 3 đủ" và "Ch.1–3"; đường tắt tab RAW → Tạo tổ hợp điền sẵn ô RAW. Không đổi schema.
- **U3:** màn Dịch nguồn "Tổ hợp": chọn bằng bảng chung, ô đã chọn + "Sửa tổ hợp", mỗi chương ghi đủ RAW / Glossary / Pronoun, "Chọn tất cả / Bỏ chọn / Chưa dịch", dòng tick có viền màu nhấn; Prompt preview chọn chương bằng bảng chung. Rà họ lỗi (2.5): trong `ChapterSetPageFactory`, `TranslateSourceFactory`, `RawPageFactory`, `LibraryBulkUi`, `GlossaryPageFactory`, `PronounPageFactory`, `TranslatePageFactory` không còn nút cao cố định mang tên tệp và không còn `setItems` chọn tệp; các nút mới đều wrap_content. Hộp xác nhận chữ giữ `AlertDialog`.
- Khác với thiết kế: tên trong ô đã chọn không giới hạn 3 dòng (hiện đủ, không "…"); dòng trong bảng chọn tối đa 6 dòng. Lý do: ở font 130% tên 90+ ký tự cần hơn 3 dòng và bị cắt.

## 3. Hồi quy archive sạch (`f887b2b3`)
Engine 742 / 1 SKIP; app debug, release, benchmark 563 mỗi biến thể / 5 SKIP, 0 FAIL; lint 0 lỗi / 65 cảnh báo; AndroidTest compile PASS. ZIP `git archive` (`A23F843C…`) khác source ZIP của bản dev (`A112660C…`) đúng 67 tệp: thư mục QA dev.4 đã bị tôi dời khỏi worktree lúc build. Mọi tệp khác giống từng byte (đã so sánh), mã sản xuất không khác.

Test host mới: `FilePickerModelTest` 7, `ComboDraftTest` 9 (+ các test cũ cập nhật nhãn). `git diff dcde3320 -- editorial-engine 'app/.../Editorial*'` rỗng.

## 4. QA thiết bị (emulator, provider giả)
197 phát hiện / **181 PASS / 16 SKIP / 0 FAIL** / 0 INCOMPLETE: frozen 117/116/1, chunk 27/21/6, product 15/15/0, v4.22 13/6/7, v4.23 10/8/2, restart 15/15/0. Prefs `glossary_store`, `pronoun_store`, `settings` giữ nguyên hash; DB đổi như mọi lần QA (`device/app-data-hashes-*.txt`).

`V423ChapterSetInstrumentedTest` (viết lại cho luồng mới, dữ liệu có tên tệp dài 90+ ký tự):
- mở bảng chọn RAW, bấm một dòng: bảng **vẫn mở**, dòng đó đổi sang "đã chọn", tiêu đề "Đã chọn 1"; Hủy → ô không đổi; chọn 3 + "Xong (3)" → ô ghi "✓ Đã chọn 3" kèm đủ tên;
- không `TextView` tên nào có dấu "…" và chiều cao view ≥ chiều cao layout, cả trong ô lẫn bảng; lặp lại ở `font_scale 1.3` (đã khôi phục 1.0);
- ba ô (3 RAW, 4 Glossary gồm hai tệp cùng chương 2, 2 Pronoun) → dòng 1 ✓, dòng 2 ✕, dòng 3 có Pronoun; Lưu bị chặn; đổi Glossary dòng 2 bằng bảng chọn một (chọn hai lần vẫn một dòng được chọn) → Lưu; mở màn Dịch → danh sách chương ghi đủ ba tên;
- đường tắt từ tab RAW điền sẵn ô RAW.
`V423ChapterRunInstrumentedTest` (dịch 3 chương bằng provider giả, mỗi chương một Glossary, lỗi riêng chương 2 thì chạy tiếp, lỗi xác thực dừng) và `V423LibraryInstrumentedTest` vẫn đạt.

## 5. Ảnh (7 ảnh trong `media/`, đã xem từng ảnh ở dev.4/dev.5)
31 ô trống; 32 bảng chọn RAW tên dài; 33 bảng chọn Glossary (4 mục đã chọn, nhãn "Gợi ý · cùng chương N"); 34 bảng ghép ✓/⚠/✕ và ô đã chọn; 35 danh sách Tổ hợp; 36 màn Dịch nguồn Tổ hợp; 37 bảng chọn ở font 130% (nhãn "Chọn tất cả (đang lọc)" xuống dòng, tên đủ). Thiếu so với danh sách 8 ảnh của yêu cầu: ảnh "ô đã chọn sau khi bấm Chọn" nằm chung trong ảnh 34.

## 6. Giới hạn
- Chỉ trên emulator; bộ chọn tệp hệ thống, phản hồi cảm giác khi chạm trên điện thoại: NOT_MEASURED cho tới khi owner thử (E1–E5).
- Tên tổ hợp tự đặt có thể dài (EditText một dòng cuộn ngang).
- Chưa cài lên điện thoại. Cần duyệt riêng; sẽ sao lưu `run-as` từ `4.23-dev.2` (debuggable) trước khi cài.
