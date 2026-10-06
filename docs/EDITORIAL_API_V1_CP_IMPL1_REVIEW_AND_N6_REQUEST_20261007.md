# Editorial API V1 — review CP-IMPL-1 và yêu cầu làm việc N6 (2026-10-07)

Coordinator: Claude. Baseline: branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD `ea33867a` (4 commit chưa push trên `37889b05`). Bản sửa trong tài liệu này được commit ngay sau `ea33867a`.

## 1. Kiểm chứng báo cáo CP-IMPL-1

| Claim | Evidence coordinator tự kiểm | Kết luận |
|---|---|---|
| Engine 551, app 431, androidTest compile PASS tại `26a2f40c` | `git archive --format=zip 26a2f40c` → giải nén vào `D:\P5E-builds\verify-cpimpl1`, `gradlew --offline :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac`: exit 0; XML: engine 551/0 lỗi, app 431/0 lỗi | Đã chứng minh |
| Python `scripts/p6` 72 PASS | Cùng archive, `py -m unittest discover -s scripts/p6`: Ran 72, OK | Đã chứng minh |
| Mã APK = `e2d54ef9`, `26a2f40c` chỉ thêm docs | `git log` khớp; không kiểm lại hash APK | Suy luận từ git, không kiểm lại artifact |
| Store/UI instrumented, process death | Báo cáo tự ghi NOT_RUN | NOT_RUN |
| Semantic | Không có model thật | NOT_MEASURED |

## 2. Phát hiện chính

1. **Lỗi sản phẩm N5 vẫn còn trong luồng toàn chương (đã sửa).** `EditorialApiFlow.acceptEdit` chỉ gắn cờ guard rồi kết thúc `FINAL_NOTES` với văn bản model trả. Run Kỹ `fx-a04` (N5) nhận bản 37/192 dòng và đưa nó thành bản cuối. `StructuralGate` của CP-IMPL-1 có ngưỡng chữ đúng (mất > 50% hoặc gấp > 2 lần là BLOCK) nhưng chỉ nối vào luồng cặp (`EditorialPairRunService`), không nối vào luồng toàn chương — luồng duy nhất đã chạy live.
   - Sửa nhỏ nhất: `EditorialApiFlow.sizeBlock` dùng lại `StructuralGate.check` và coi `CANDIDATE_EMPTY`/`CHARS_LOSS`/`CHARS_GROWTH` là lỗi kỹ thuật của bước E → retry kỹ thuật 1 lần (như hiện có) → vẫn lỗi thì `RETRY_REQUIRED`, giữ DRAFT. Không đổi ngưỡng.
   - Test mới: `aWellFormedEditThatLostMostOfTheTextIsNeverTheFinalText` (cả QUICK và THOROUGH), `aDoubledEditIsRetriedAndARetryWithTheWholeTextSucceeds`. Engine 553/553, app 431/431 trên working tree có bản sửa. Chưa chạy bản trước sửa với test mới (lỗi đã được chứng minh bằng chính run N5).
   - Replay số liệu 24 bản cuối N5 (chỉ đếm chữ, không in văn bản): đúng 1 bản bị chặn — B base `fx-a04`, 3094/15573 chữ (0.199); 23 bản còn lại tỷ lệ 0.997–1.003, không bị ảnh hưởng.
2. **CP-IMPL-1 đi trước thứ tự plan.** Plan mục 3.2/7 đặt chunking sau khi E đạt trên chương nguyên và sau P7. Phần đã làm có giá trị thật (chạy theo cặp RAW/bản dịch lấy thẳng từ job Dịch — đúng ý owner, không phải tự căn hai file), nhưng không làm tiến 3 chương FINAL: chưa chương nào trong 3 chương đại diện cần chunking. Quyết định: **giữ mã, đóng băng ở trạng thái offline**; không mở đo W/C (§9.4 của gói) cho tới sau N6.
3. **N5 đã trả lời câu hỏi chế độ trên số liệu:** Nhanh 19/25, Kỹ 19/25 (18 nếu không tính run cụt), Kỹ đắt hơn ~38% và là nhánh duy nhất sinh lỗi cấu trúc. Theo đúng quy tắc mục 6 của plan, nếu phân xử lỗi mới không loại Nhanh thì **mặc định = Nhanh**, Kỹ là tùy chọn.
4. **Lỗ hổng chất lượng còn lại là của model, không phải ghi sổ:** toàn bộ target bỏ sót thuộc hai lớp MISSING_SENTENCE (3–4) và ADDRESS_PROFILE (2) ở cả hai nhánh. Không chỉnh prompt trước N6 để giữ phép đo sạch; nếu owner gặp đúng hai lớp này trong chương thật thì đó là gói sửa tiếp theo.

## 3. Tiến độ tới mục tiêu

| Mốc | Trạng thái |
|---|---|
| Luồng toàn chương chạy live, lưu/mở lại/xuất | Có (N4 thiết bị + N5 live) |
| Bản cụt không còn thành FINAL | Đã sửa trên host; chưa build/thiết bị |
| Chế độ mặc định | Nhanh theo số liệu; chờ phân xử lỗi mới |
| 3 chương thật được owner chấp nhận | 0/3 — N6 chưa chạy, chưa được duyệt |
| P7 | Chưa bắt đầu |

Chi phí: lượt review này 0 call, USD 0. Ledger N5 còn USD 0.73436496 (không dùng cho N6 nếu owner không nói rõ).

## 4. Yêu cầu làm việc N6 (cho phiên Codex)

Đọc plan `docs/EDITORIAL_API_V1_PLAN_20261005.md`, `docs/EDITORIAL_API_V1_N5_RESULT_20261006.md` và tài liệu này. Branch giữ nguyên. Không đụng pilot. Không chỉnh Quality Core/prompt/ngưỡng trong gói này.

### 4A. Build và kiểm thiết bị (offline, không cần duyệt thêm)

1. Build production qua `scripts/build-and-save.ps1 -Series '4.18-api' -MinimumVersionCode 243 -Offline` từ commit có bản sửa (worktree tạm theo recipe), và test APK tương ứng; archive đủ `artifacts/` + `backup/`.
2. Cài `emulator-5554`. Chạy: toàn bộ test instrumented của Editorial API (whole + pair, gồm `EditorialPairStoreInstrumentedTest`, `EditorialPairUiInstrumentedTest`, process-death reopen) và full suite; ghi từng lớp PASS/FAIL. Lỗi mới so với 11 lỗi lịch sử phải sửa trong cùng gói.
3. PASS 4A: các lớp Editorial API PASS trên thiết bị; full suite không thêm lỗi ngoài 11 lỗi lịch sử đã biết; 0 provider call.

### 4B. Phân xử lỗi mới của N5 (offline, không cần duyệt thêm)

1. Với 24 run N5, liệt kê mọi dòng đổi ngoài target (diff DRAFT → final). Phân loại từng dòng: `IMPROVEMENT` (sửa lỗi có thật theo RAW), `NEUTRAL` (diễn đạt khác, không đổi nghĩa), `NEW_ERROR` kèm loại MEANING/OMISSION/NUMBER/NEGATION/SPEAKER/PRONOUN/GLOSSARY. Căn cứ duy nhất là RAW, glossary, pronoun; không dùng bản FINAL thủ công của owner.
2. Trên `fx-a02` trừ lỗi đã có sẵn trong DRAFT (G1: `E_L245_MEANING`).
3. Đầu ra: `docs/EDITORIAL_API_V1_N5_ADJUDICATION_20261007.md` — chỉ ID dòng, loại, lý do ngắn, hash response; **không** chép văn bản sách vào Git (trích dẫn để ở `D:\P5E-private\n5-adjudication\`). Kết luận cổng mục 6: số NEW_ERROR nặng theo nhánh, chọn chế độ mặc định theo đúng quy tắc.
4. Kết luận 4B **không** được tự đổi chế độ mặc định trong mã nếu kết quả là Kỹ; báo lại để owner quyết.

### 4C. Ba chương thật (live — chỉ khi owner duyệt D-N6 trong chat)

1. Owner chọn 3 chương đại diện và cung cấp RAW/DRAFT/Glossary/Pronoun ngoài Git (`D:\P5E-private\n6-inputs\`). Nạp qua UI tổ hợp như người dùng thật trên emulator.
2. Mỗi chương một run toàn chương, chế độ mặc định theo 4B (Nhanh nếu 4B chưa xong hoặc không loại Nhanh), model `openai/gpt-5.6-luna`, ledger mới `N6-<date>` với trần đúng bằng con số owner duyệt; trần chương 0.10. Không retry ngoài retry kỹ thuật có sẵn.
3. Mỗi chương: lưu, force-stop, mở lại, xuất TXT, đọc lại SHA-256; chép file xuất sang `D:\P5E-private\n6-outputs\` để owner đọc. Ghi model/route/revision/commit/APK/call/token/USD.
4. Dừng sau 3 chương. Báo cáo: trạng thái từng chương, chi phí, đường dẫn file cho owner đọc. Chấp nhận chương là quyết định của owner, không phải của phiên.

### Điểm dừng

Dừng và báo khi: cần gọi provider mà chưa có D-N6; cần đổi prompt/ngưỡng; một chương ra `RETRY_REQUIRED` hai lần liên tiếp; UNKNOWN cost; lỗi hạ tầng.

### Bằng chứng

`docs/EDITORIAL_API_V1_N6_EXECUTION_<date>.md`: commit, APK version/code/SHA-256, kết quả instrumented theo lớp, bảng phân xử 4B, (nếu có) bảng N6 theo chương. Cập nhật §10 canonical, snapshot, BUILD_STATE. Push sau mỗi gói, không force-push, chỉ stage file đã review.

## 5. Quyết định cần owner

| ID | Nội dung | Khuyến nghị |
|---|---|---|
| D-N6 | Cho phép 4C: 3 chương thật, toàn chương, chế độ mặc định theo 4B, ledger mới trần USD 0.30 (ước tính thực ≈ USD 0.03–0.06), chỉ emulator | Duyệt, kèm chọn 3 chương và đặt input vào `D:\P5E-private\n6-inputs\` |
| D-CP | Đóng băng chunk-pair CP-IMPL-1 ở offline tới sau N6 | Duyệt |

## 6. Quyết định đã nhận

Owner (chat, 2026-10-07): "duyệt D-N6 và D-CP".

- D-N6: được phép chạy 4C — 3 chương thật, luồng toàn chương, chế độ mặc định theo 4B (Nhanh nếu 4B chưa xong hoặc không loại Nhanh), model `openai/gpt-5.6-luna`, ledger mới `N6-<date>` trần USD 0.30, trần chương USD 0.10, chỉ `emulator-5554`, không pilot. Điều kiện bắt đầu 4C: 4A PASS (APK có bản sửa `277ffc79` đã cài và test thiết bị đạt) và input có trong `D:P5E-private
6-inputs` (owner đặt). Thiếu input thì dừng và hỏi owner, không tự chọn chương.
- D-CP: chunk-pair CP-IMPL-1 đóng băng offline tới sau N6; không đo W/C, không chi tiền cho luồng cặp.
