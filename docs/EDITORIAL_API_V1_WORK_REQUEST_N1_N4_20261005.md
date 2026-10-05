# Yêu cầu làm việc — Editorial API V1, gói N1–N4 (offline)

Kế hoạch gốc: `docs/EDITORIAL_API_V1_PLAN_20261005.md` (đọc toàn bộ trước khi làm). Chỉ thực hiện khi owner đã duyệt D-N1, D-N2, D-N3 ở mục 9 của plan; ghi câu duyệt vào mục "Quyết định" cuối tài liệu này trước khi sửa mã.

Branch: `feature/v4.18-p5e-runner-repair-20260917` (không tạo branch/release/checklist mới). Baseline: HEAD hiện tại sau commit plan. Không gọi provider, không cài/đụng pilot, không chi tiền. Emulator `emulator-5554` chỉ dùng với fake provider. Không đọc/in/lưu API key. Không đưa văn bản sách vào Git. Giữ nguyên các file bẩn của owner (`.idea/*`, `docs/P5E_*` đang modified) — không stage.

## Mục tiêu

Một người dùng trên emulator tạo được tổ hợp RAW + DRAFT (+ Glossary/Pronoun tùy chọn), xác nhận, chạy luồng E/C với **fake provider**, nhận bản cuối, mở lại sau khi khởi động lại app và xuất TXT đúng. Bộ test và runner sẵn sàng cho A/B N5 mà không cần sửa thêm mã.

## N1 — Engine (`editorial-engine`, package mới `com.ml.tblandroidtxt.editorial.api`)

Không sửa lớp nào trong `editorial.pack` ngoài việc tái dùng tiện ích thuần (diff, chuẩn hóa) nếu cần.

1. `EditorialApiContract`: hằng `CONTRACT_REVISION = "EDITORIAL_API_V1.1"`, enum `Mode {QUICK, THOROUGH}`, enum `IssueKind` (10 giá trị ở plan §3), enum `RunState` (plan §4.4).
2. `QualityCore`: văn bản luật ở plan §2.1, lưu thành asset/resource có SHA-256 ghi vào metadata. Hai biến thể: `editPrompt()` và `checkPrompt()`. Không chép nguyên pack 4.1.3; không nhắc ledger, ID, hash, unit ref.
3. `EditPromptBuilder`: dựng system/user cho E: Quality Core + glossary đã lọc (plan §3.1, không giới hạn số mục) + pronoun đã lọc + `# RAW` + `# DRAFT` + hợp đồng đầu ra `<EDITED>…</EDITED>`, `<NOTES>…</NOTES>` tùy chọn, `<WRONG_PAIR>…</WRONG_PAIR>` chỉ khi hai bản rõ ràng khác chương. Có biến thể "không có glossary/pronoun".
4. `EditResponseParser`: trích EDITED (thẻ đầu tiên, cắt khoảng trắng hai đầu, giữ nguyên nội dung bên trong), NOTES (dòng `trích | loại | lý do`; dòng hỏng bỏ qua và đếm), WRONG_PAIR. Thiếu thẻ đóng + `finish_reason=length` → `TRUNCATED`; thiếu thẻ mở → `FORMAT`.
5. `EditGuards`: các guard ở plan §4.3 với ngưỡng trong một record cấu hình (mặc định 10%/5 dòng, 35%, danh sách ký hiệu). Trả danh sách cờ, không ném lỗi.
6. `CheckPromptBuilder` + JSON schema strict sinh từ một nguồn duy nhất (cùng nguyên tắc FieldSpec: prompt mô tả trường = schema = parser). C nhận RAW trước, EDITED, danh sách đoạn đã đổi (trước → sau, từ `EditorialTextDiff`), cờ guard. Không nhận NOTES của E.
7. `CheckResponseParser`: verdict + issues. Trường thừa bị bỏ qua; `fix` rỗng hợp lệ; `edited_quote` rỗng → bỏ issue và đếm. Không có mã từ chối nào cho lỗi ghi sổ — mọi bất thường thành bộ đếm.
8. `FixApplier`: chuẩn hóa khoảng trắng để khớp; áp `fix` khi `edited_quote` khớp đúng 1 lần và `fix` không rỗng; còn lại đánh dấu `NEEDS_REVIEW`. Trả văn bản mới + danh sách đã áp/không áp.
9. `EditorialApiFlow`: state machine thuần (không I/O): `E → guards → (THOROUGH) C → apply → (nếu có áp) C2 → state cuối`. Tối đa 1 retry kỹ thuật mỗi bước. `WRONG_PAIR` dừng sớm. Trả từng request đã dựng để app gửi qua provider interface. Ghi calls/tokens/USD theo bước.

Test N1 (JUnit, engine):
- Parser E: đủ thẻ; thiếu NOTES; NOTES hỏng; thẻ lồng/text trước thẻ; truncation; WRONG_PAIR; EDITED chứa 「」 và ký hiệu khung giữ nguyên byte.
- Guards: mỗi guard có ca đạt và ca vi phạm.
- Schema C = prompt C = parser C (test sinh từ cùng spec, như `EditorialFieldSpec`): mọi trường prompt nói MUST có trong `required`; ca trường thừa; ca thiếu `fix`.
- FixApplier: khớp 1 lần, 0 lần, 2 lần, khác khoảng trắng, fix rỗng.
- Flow: QUICK 1 call; THOROUGH PASS = 2 call; THOROUGH có áp sửa = 3 call; retry kỹ thuật; WRONG_PAIR; không bao giờ vượt 3 call + retry.
- Đột biến: lấy NOTES/issue mẫu dựng từ dạng lỗi P6 (trích có furigana, trích không khớp, dòng trống, trường thừa) → không có lần nào thành exception/stop.

## N2 — App (`app/`)

1. `OpenRouterEditorialApiProvider`: gửi request E (text, `max_tokens` = ước tính độ dài DRAFT × 1.6, tối thiểu 4096) và C (strict json_schema, reasoning minimal như P6) qua transport/settings hiện có. Ghi `model`, route/provider trả về, `finish_reason`, usage, chi phí thực vào kết quả bước. Interface `EditorialApiProvider` để thay bằng fake.
2. `FakeEditorialApiProvider` (chỉ test/androidTest): kịch bản cấu hình được (PASS, ISSUES có fix, truncation, WRONG_PAIR, NOTES hỏng).
3. Migration chỉ thêm bảng `editorial_combos`, `editorial_api_runs` (plan §4.4). Test migration từ DB phiên bản hiện tại không mất dữ liệu bảng cũ.
4. `EditorialComboRepository`, `EditorialApiRunRepository`: CRUD, snapshot nguồn (nội dung + SHA-256) lúc bắt đầu chạy, phát hiện nguồn đổi khi mở lại.
5. `EditorialApiRunService`: chạy `EditorialApiFlow` trên background (dùng cơ chế job/foreground hiện có của luồng Dịch nếu phù hợp), một chương một writer, kiểm trần USD/chương trước mỗi bước (ước tính worst-case), hủy được, khởi động lại app không tự gửi lại request đang dở (state `RUNNING` cũ → hiện "bị gián đoạn, chạy lại?").
6. Xuất TXT: chỉ `final_text`, UTF-8, ghi qua SAF, đọc lại so SHA-256.
7. Runner fixture cho API_V1: mở rộng runner instrumented hiện có với chế độ `API_V1_QUICK` / `API_V1_THOROUGH`; ghi `final.txt` để `score_run.py` chấm; retention response ngoài Git như P6; spend ledger mới (tên nhóm `N5`, trần do owner duyệt — chưa tạo ledger trong gói này). Metadata run phải có model, route, contract revision, Quality Core SHA-256, source commit, APK version.
8. `scripts/p6/score_run.py`: thêm cột "lỗi mới" là `NOT_MEASURED` (phân xử thủ công làm sau), tỷ lệ viết lại, cờ guard, số call. Test Python cho phần thêm.

Test N2: unit cho repository/service với fake provider (mọi đường state), migration, export round-trip, trần chi phí chặn trước khi gửi, restart giữa chừng.

## N3 — UI

Theo plan §5, dùng widget/kiểu hiện có (`a.primaryButton`, `fieldBlock`, AlertDialog…) giống luồng Dịch.

1. Tab Biên tập mới: danh sách tổ hợp + "+ Tổ hợp mới". Đường SAFE4 cũ chuyển vào mục thu gọn "Công cụ dev — SAFE4 legacy" (không xóa mã).
2. Màn tổ hợp: RAW/DRAFT từ SAF hoặc "Recent translated files"; Glossary/Pronoun từ `GlossaryStore`/`PronounStore` hoặc "Không dùng"; Chế độ; Model (mặc định theo Cài đặt); Trần USD/chương (mặc định 0.10); Lưu; Nhân bản; Xóa (xác nhận).
3. Màn xác nhận: tên file, 3 dòng đầu, số ký tự, glossary/pronoun hoặc cảnh báo thiếu, cảnh báo tỷ lệ độ dài, ước tính chi phí, Đổi / Chạy. Không ghép theo tên file, không chặn khi tên khác.
4. Tiến độ + hủy. Kết quả: Xem bản cuối, So sánh (diff), Cần xem, Xuất TXT, Chạy lại, Chi tiết kỹ thuật thu gọn.
5. Không có chuỗi "pack", "binding", "SAFE4_BLOCKED", "cấp phép" trong luồng người dùng (test chuỗi UI).

Test N3: unit cho presenter/state UI; androidTest tối thiểu: tạo tổ hợp → xác nhận → chạy fake → kết quả → xuất.

## N4 — Build và kiểm trên emulator

1. Build qua `scripts/build-and-save.ps1` (và `build-and-save-android-test.ps1` nếu cần), worktree tạm theo recipe; archive đủ ở `artifacts/` và `backup/`.
2. Kiểm sạch từ `git archive` đúng commit: engine, app unit, Python `scripts/p6`, androidTest compile. Test SAFE4 cũ vẫn PASS.
3. Cài emulator `emulator-5554` (không đụng pilot). Với fake provider: tạo tổ hợp có đủ 4 nguồn và tổ hợp thiếu cả Glossary/Pronoun; chạy QUICK và THOROUGH; force-stop rồi mở lại; xuất TXT và đọc lại; ảnh chụp từng màn (không chứa văn bản sách thật — dùng văn bản mẫu tự tạo).
4. Runner fixture API_V1 chạy khô (`dry-run`, fake provider) trên 1 fixture để chứng minh N5 không cần sửa mã.

## PASS / FAIL

PASS khi đủ:
- Mọi test ở trên PASS từ source sạch của commit báo cáo; test cũ không giảm.
- Trên emulator: hai tổ hợp chạy hết với fake provider, mở lại sau restart đúng bản cuối, file xuất có SHA-256 bằng `final_text`.
- Không gọi provider thật; spend ledger không đổi.
- Chuỗi UI người dùng không chứa thuật ngữ SAFE4.

FAIL nội bộ (sửa trong cùng gói, không phải BLOCKED): test/build/lint lỗi, emulator không cài được do lỗi build.

Dừng và báo owner chỉ khi: cần gọi provider thật, cần đụng pilot, cần sửa/xóa mã SAFE4 ngoài việc ẩn UI, hoặc cần đổi plan.

## Bằng chứng phải giữ và báo cáo

Tạo `docs/EDITORIAL_API_V1_N1_N4_EXECUTION_20261005.md` gồm: commit từng gói, số test (engine/app/Python/androidTest), APK version/code/SHA-256 và đường archive, danh sách ảnh chụp (đường dẫn ngoài Git nếu có văn bản), kết quả dry-run runner, các lệch so với yêu cầu này và lý do. Cập nhật `WORKSPACE_SNAPSHOT.md`, `BUILD_STATE.md` và §10 của `EDITORIAL_RECOVERY_V4_18.md` (next action: owner duyệt D-N4 để chạy N5). Push sau mỗi gói; không force-push; chỉ stage file đã review.

## Quyết định

(Codex ghi lại câu duyệt của owner cho D-N1, D-N2, D-N3 tại đây trước khi sửa mã.)
