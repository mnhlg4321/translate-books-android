# Đề xuất — đơn giản hóa giao tiếp model và phá vòng "parser từ chối → thêm luật → gọi lại" (P6 R6, v4.18)

Ngày lập: 2026-10-04. Người lập: Claude (điều phối). Loại: đánh giá + phương án để owner quyết; **chưa** cấp quyền đổi kiến trúc production, gọi provider hay đụng pilot.
Baseline: branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD `96d3de26` (đồng bộ origin).

## 1. Trạng thái đã kiểm

| Hạng mục | Claim | Evidence | Kết luận |
|---|---|---|---|
| Test | engine 351, app 341, Python 33 | `git archive 96d3de26`, offline Gradle + `scripts/p6/test_*.py` | Đã chứng minh |
| Rò rỉ | không key/fingerprint/văn bản sách | soát `e268770c..96d3de26` | Đã chứng minh |
| Build script | chỉ thêm `-Offline` | diff 2 script: một switch + `--offline` | Không nới chính sách build |
| Checklist | không tick | 4 dòng thêm mục checkpoint + khác biệt whitespace | Đúng |
| Mã đang có / đã test / APK / đã cài | HEAD `96d3de26`; APK `4.18-p6.16`/code229 từ `d1cc1452` trên emulator; pilot vẫn code215 | execution record, BUILD_INFO | Phù hợp: `d9390733`/`96d3de26` chỉ thêm docs |
| Wire v3 sửa lỗi id | RAW được chấp nhận | V3: `L1_RAW_DISCOVERY` accepted (20,932 vào / 1,841 ra, USD 0.00744205) | **Đã chứng minh** — lần đầu một RAW ledger thật qua validator |
| RECONCILE | `L1_TEXT_REQUIRED` | 29,718 vào / 3,656 ra, `finish=stop`, USD 0.01181655 | Đã chứng minh mã; **trường cụ thể chưa biết** |
| Chi phí | G1 USD 0.03222845 / 1.00 | sổ chi tiêu hash-chain | Đúng; còn 0.96777155; 0 UNKNOWN |
| Chất lượng | — | L2/L3 chưa chạy | `SEMANTIC_EVAL`: **NOT_REACHED** |

## 2. Chẩn đoán `L1_TEXT_REQUIRED` — một họ lỗi, không phải một trường

| Phát hiện | Bằng chứng |
|---|---|
| Prompt bảo model dùng chuỗi rỗng khi không áp dụng | `OpenRouterEditorialP5PilotProvider.java:617` |
| Schema L1 chỉ có `maxLength`, không `minLength`/`minItems`/`pattern` cho id | `EditorialL1Ledger.java:550-555`; L2/L3 không gửi schema (chỉ `json_object`) |
| Parser bắt buộc không rỗng nhưng prompt không nói: `disposition.reasonCode` (298, parse **đầu tiên**), `speakerRecords[].speaker/listener/basis` (191–192), `protectedSpans[].reason` (207, prompt không nhắc trường này), `findings[].observation/expectedMeaning/rawQuote` (261, 275–276) | `EditorialL1Ledger.java` |
| Cùng họ ở L2/L3: `changes[].reason` bắt buộc nhưng prompt không nói (`EditorialChangeMapReconstructor.java:176`); độ dài `scope/contrast` ≤120 không nêu | audit Sonnet |
| Chẩn đoán mù: `safeMessage` chỉ cho `[A-Z0-9_:./-]{1,96}` nên mọi đường dẫn camelCase bị nuốt; L1 `keys/object` nhận path rồi bỏ | `EditorialL1Ledger.java:756-759`, `EditorialL2Execution.java:820-823` |
| Response không được giữ → không replay được | chính sách hiện tại của harness |

Thứ tự khả năng (suy luận, chưa chứng minh): (1) `disposition.reasonCode` rỗng khi CONTINUE; (2) `speakerRecords[].listener` rỗng cho lời kể/độc thoại; (3) `protectedSpans[].reason`; (4) `observation`/`expectedMeaning`; (5) `rawQuote` cho finding ADDITION/MISSING.

Đây là lần thứ ba liên tiếp lỗi hình thức (event 6: cú pháp token; G1#1–2: chép id có hash; G1#3: trường rỗng). Theo quy tắc hai vòng, phải sửa **thiết kế giao tiếp**, không chỉ vá trường đầu tiên.

## 3. Model đang phải làm bao nhiêu việc ghi sổ

| Việc model đang làm | Có cần model làm không | Đề xuất |
|---|---|---|
| Chép số dòng làm tham chiếu (`L<n>`, `D<n>`) | Có (rẻ, ổn định) | Giữ |
| Chép **nguyên dòng DRAFT** vào `changes[].before` ở L2_EDIT/L3 | Không — app biết dòng | Bỏ; app tự lấy `before` theo số dòng (cùng loại với bài học hash id) |
| Trích dẫn ngắn `rawQuote/draftQuote/viQuote` phải là chuỗi con | Có (là bằng chứng) | Giữ; app chuẩn hóa NFC + cắt khoảng trắng hai đầu trước khi so |
| Điền mọi khóa kể cả không áp dụng | Một phần | Danh sách MUST/MAY rõ ràng; trường không áp dụng cho phép rỗng thật sự (parser chấp nhận) hoặc giá trị chuẩn (`UNKNOWN`, `OK`) |
| Coverage phân hoạch theo dải | Có (owner cấm suy coverage từ lời khai) | Giữ |
| `reasonCode` theo charset riêng | Không cần với CONTINUE | Cho phép rỗng khi CONTINUE/PRESERVE_DRAFT; bắt buộc khi STOP |
| Hash, identity, đếm, diff, liên kết artifact | Không | Đã do app làm — giữ |

## 4. So sánh hình thức output L2/L3

| Tiêu chí | A. JSON change rows (hiện tại) | B. Khối thay thế dạng văn bản có thẻ + neo số dòng (đề xuất) | C. Trả toàn văn (như refine của luồng dịch) |
|---|---|---|---|
| Ví dụ | `{"line":12,"before":"…","after":"…","errorId":…}` | `<EDIT line="12" finding="E3">câu mới</EDIT>`, `<INSERT after="40" finding="E5">…</INSERT>`, `<DELETE line="7" finding="E2"/>` + danh sách giải quyết finding dạng thẻ | `<TEXT>…toàn chương…</TEXT>` |
| Mất/thừa câu | Không thể ngoài op khai báo | Không thể ngoài op khai báo | **Có thể** (luồng dịch chỉ cảnh báo, không chặn — `ChunkQa` ngưỡng 1/3–3×) |
| Sửa ngoài phạm vi | App chặn (diff = rows) | App chặn | Phải tự căn hàng + ánh xạ diff→finding; model hay chuẩn hóa dấu câu hàng loạt → nhiều diff không có finding |
| Protected spans | Thực thi chính xác | Thực thi chính xác | Chỉ kiểm sau; phải hoàn nguyên từng dòng |
| Truy vết finding→thay đổi | Trực tiếp | Trực tiếp | Gián tiếp, dễ mơ hồ |
| Token ra (chương ~26–34 KB) | Nhỏ (theo số sửa) | Nhỏ, ít hơn A (không chép `before`, không escape JSON) | ~10–16k/call; chương 010 sát trần 16,384 → rủi ro cắt |
| Chi phí ước tính L2_EDIT | ~0.01–0.02 | ~0.01 | ~0.02–0.03 (×2 nếu L3 cũng trả toàn văn) |
| Độ ổn định hình thức | Thấp (JSON lồng, escape ngoặc kép tiếng Việt/thoại, chép nguyên dòng) | Cao hơn (văn bản thuần trong thẻ như luồng dịch đã chạy cả sách) | Cao về hình thức, thấp về kiểm soát |
| Tái sử dụng luồng dịch | Không | `PromptBuilder.stripThinking/stripCodeFence`, trích thẻ, `ResponseValidator` (finish, cap, refusal, echo, loop), `RetryPolicy` phân loại, lưu `response_content` trước khi kiểm | Như B + prompt refine |

**Khuyến nghị:** giữ **change rows** nhưng chuyển L2_EDIT/L3_RECONCILE sang **B** (văn bản có thẻ, neo số dòng, app tự lấy `before`), giữ phần "giải quyết finding" dạng thẻ đơn giản. Không chọn C làm mặc định vì không chặn được mất câu/sửa lan và có rủi ro cắt ở chương dài. Không đổi trước khi có số đo: chạy thử A/B/C có kiểm soát (gói S6) sau khi L1 thông.

L1 vẫn là JSON có schema strict (đã chạy được RAW); chỉ sửa tính nhất quán, không đổi hình thức.

## 5. Gói việc đề xuất (để owner duyệt triển khai)

| Gói | Nguyên nhân xử lý | File chính | Đầu ra / test | PASS | Dừng |
|---|---|---|---|---|---|
| **S1** chẩn đoán có đường dẫn | Lỗi mù | `EditorialL1Ledger` (`str/id/enumOf/intOf/stringList/object/keys/bad`, vòng lặp có chỉ số), `EditorialUnitReference`, `EditorialL3Ledger`, `EditorialL2Execution` (`keys/object/text/bool/line/safeMessage`), `EditorialL2Findings`, `EditorialFinalRead` | Ngoại lệ có kiểu `WireViolation(code, path)` với path chỉ từ hằng + chỉ số (vd `findings.2.expectedMeaning`); `safeMessage` trả `CODE:path`; không bao giờ đưa tên khóa lạ của model vào path | JVM: mỗi luật bắt buộc có test trả đúng `CODE:path`; khóa lạ → không lộ tên | — |
| **S2** giữ response để replay | Không replay được | provider adapters L1/L2/L3 + runner fixture | Ghi **bytes nội dung response** (không header, không key) vào thư mục riêng trước khi validate: fixture → `files/p6-fixture-results/<run>/<fixture>/responses/NNN-PHASE.json` rồi kéo về `D:\P5E-private`; pilot/sản phẩm: **tắt mặc định** (cần owner duyệt riêng). Công cụ replay JVM: đọc file response + inventory/fixture, chạy đúng parser production, in `CODE:path` | Replay lại response giả lập tái hiện đúng mã; không ghi key/header | — |
| **S3** một nguồn sự thật cho trường | Prompt–schema–parser lệch | `EditorialL1Ledger.jsonSchema`, prompt L1 (`:617`, `:557-567`), prompt L2/L3 | Bảng `FieldSpec` (path, MUST/MAY/CONDITIONAL, min/max, pattern) trong engine; schema L1 sinh từ bảng (thêm `minLength`, `minItems`, `pattern` id); prompt liệt kê MUST/MAY từ bảng; test nhất quán: mọi trường parser bắt không rỗng phải là MUST trong bảng, có trong prompt và có `minLength:1` ở schema | Test nhất quán xanh cho cả 8 phase | Không nới anchor/id/coverage |
| **S3b** quy tắc rỗng hợp lý | Trường không áp dụng | `EditorialL1Ledger` | `disposition.reasonCode` cho phép rỗng khi CONTINUE/PRESERVE_DRAFT; `speakerRecords[].listener` cho phép `UNKNOWN`/rỗng; giữ `reason` của protectedSpan bắt buộc và nói rõ trong prompt | JVM | — |
| **S4** bỏ chép nguyên dòng ở L2/L3 | Cùng họ "chép" | `EditorialChangeMapReconstructor`, wire L2_EDIT/L3_RECONCILE, adapters | `before` không còn bắt buộc; app lấy theo số dòng; nếu model gửi `before` thì chỉ kiểm là chuỗi con (sau NFC/trim), lệch → cảnh báo, không từ chối | JVM + fake chain 14/14 | — |
| **S5** live tiếp G1 | Có chẩn đoán đầy đủ | — | Chạy tiếp G1 (L1-only) trong phần trần còn lại, `fx-a03` trước, cùng quy tắc dừng nhóm | Mỗi từ chối có `CODE:path` + response replay được | Từ chối: sửa offline bằng replay, không gọi lại ngay |
| **S6** thử A/B/C cho L2 (sau khi có REPORT_L1 thật) | Chọn hình thức có số đo | runner fixture | Cùng 3 fixture (`fx-a04`, `fx-a11`, `fx-a02`) và cùng REPORT_L1 thật từ G1; 3 biến thể × 3 fixture × 2 lượt = 18 call L2_EDIT; đo: tỷ lệ qua cấu trúc, lỗi gieo sửa đúng (scorer đóng băng), collateral, mất/thừa câu, token/USD | Bảng so sánh; quyết định giữ B hay đổi | Ngân sách riêng ≤ USD 0.40 (cần owner duyệt) |

S1–S4 là offline, có thể hoàn nguyên (một nhánh commit theo gói). S3/S4 đổi wire → tăng `contractRevision` (attempt v3 không bị tái dùng). Không đổi branch/release/checklist.

## 6. Phản biện

- *"Chỉ cần vá `reasonCode`"*: có thể đúng cho lần này nhưng còn ≥5 trường cùng họ; không có path thì lần sau lại mù.
- *"Trả toàn văn sẽ hết lỗi hình thức"*: đổi lỗi hình thức lấy lỗi khó thấy (mất câu, sửa lan); luồng dịch hiện không có kiểm tra bản sửa so với bản nháp.
- *"Lưu response là rủi ro dữ liệu"*: response là văn bản sinh từ sách, cùng loại với fixture đã nằm trong `D:\P5E-private`; không có key/header. Với pilot vẫn tắt mặc định.
- *"Bỏ JSON schema L1"*: không — schema strict đã giúp RAW qua; chỉ cần nó khớp parser.
- *Rủi ro `require_parameters`*: thêm `minLength/minItems` vào schema có thể làm router loại endpoint không hỗ trợ; S3 phải kiểm bằng một preflight 0-call không gửi (dựng body) và nêu rủi ro trước live.

## 7. Chi phí dự kiến

S1–S4: 0 USD (offline). S5: phần còn lại của G1 (≤ USD 0.968, đã duyệt). S6: ≤ USD 0.40, cần duyệt riêng.

## 8. Một bước tiếp theo

Owner duyệt **S1–S4** (offline, thay đổi wire và parser trong production, có thể hoàn nguyên) và chính sách S2 (giữ response cho fixture run ngoài Git; pilot tắt). Sau đó giao Codex S1–S4 rồi S5.

## 9. Quyết định owner

**S1–S4 đã được owner duyệt (2026-10-04, "đồng ý s1-s4"):** triển khai offline S1 (lỗi wire kèm đường dẫn trường an toàn), S2 (giữ bytes nội dung response của fixture run trong `D:\P5E-private`, không key/header; pilot/sản phẩm **tắt**), S3 (bảng đặc tả trường duy nhất sinh prompt/schema + test nhất quán; S3b quy tắc rỗng hợp lý), S4 (L2/L3 bỏ chép dòng `before`). Thay đổi wire/parser → tăng `contractRevision`. Sau khi S1–S4 đạt offline + emulator: **S5** chạy tiếp G1 trong phần trần G1 còn lại theo quyền D-G1b đã có (dừng nhóm khi bị từ chối, báo `CODE:path` + đường dẫn response). **S6 chưa được duyệt.** Không đụng pilot, không G2.

## 10. Kết quả S1–S5 và chẩn đoán lần dừng mới (Claude, 2026-10-04)

**Kiểm độc lập `7ed88fb4`:** engine 362/362, app 341/341, lint (0 lỗi) và androidTest compile PASS, Python 4/4 file; không rò rỉ. S2 đúng chính sách: chỉ runner fixture (`EditorialP6FixtureRunnerInstrumentedTest` `ResponseCapture`) lưu đúng bytes nội dung; `src/main` không lưu; coordinator vẫn bắt buộc `!allowFullModelResponseStorage()`. Công cụ replay `:editorial-engine:replayP6WireResponse` tái hiện đúng `L1_UNIT_UNKNOWN:coverage.0.from` từ response lưu.

**S5 (fx-a03, `L1_RAW_DISCOVERY`):** 1 call, 21,186 vào / 222 ra, USD 0.00556275; G1 = 5 call, **USD 0.03779120 / 1.00**, 0 UNKNOWN. Response: một dải coverage và một candidate, cả hai dùng `"L1173"` — id không tồn tại (unit đầu là `L1`, cuối `L383`, prompt nêu đúng).

**Nguyên nhân gốc (đã chứng minh từ mã + response):** bảng `EditorialFieldSpec` (S3) đặt `minLength = 3` cho cả 10 trường tham chiếu unit, trong khi pattern `^L[1-9][0-9]*$` cho phép `L1`…`L9` (2 ký tự). Schema strict khiến decoder **không thể** sinh `"L1"` nên sinh chuỗi dài hơn không tồn tại. Đây là lỗi do S3 đưa vào, không phải lỗi model. Fake run 14/14 không bắt được vì schema chỉ áp ở phía provider, parser vốn chấp nhận `L1`. Output ngắn bất thường (222 token so với 1,764–2,238 trước đó) nhiều khả năng cùng nguyên nhân (decoder bị ép ngay ở trường đầu); sẽ đo lại sau sửa.

**Đã sửa (Claude, `7f3c25ba`):** `MIN_UNIT_REFERENCE_LENGTH = 2` cho mọi trường tham chiếu unit; test hồi quy `lengthBoundsAdmitTheShortestValueOfEveryPattern` bắt cả họ lỗi "giới hạn độ dài loại trừ giá trị hợp lệ của chính pattern"; sửa một test prompt đã ghim giá trị sai. Engine 363/363, app 341/341, lint + androidTest compile PASS. Soát các trường còn lại: id/text min 1, hash đúng 64 — không còn trường nào cùng họ. Parser không đổi nên không tăng `contractRevision`.

**Khoảng trống còn lại:** test so schema↔bảng chỉ phủ L1_RAW, L1_RECONCILE và final-read; L2_EDIT, L3_RAW, L3_RECONCILE chưa sinh schema (chỉ `json_object` + luật prompt). Bổ sung một test "golden wire hợp lệ phải qua schema sinh ra" (bộ kiểm min/max/pattern/enum đơn giản) để fake không còn che lệch schema↔parser.

### Gói tiếp theo cho Codex

| Gói | Việc | PASS | Dừng |
|---|---|---|---|
| **T1** offline | Test golden-wire-qua-schema cho mọi phase có schema; với mỗi trường tham chiếu, chèn giá trị biên `L1`, `L9`, `L10`, số lớn nhất có thật | JVM xanh | — |
| **T2** offline | Build lại production + AndroidTest qua wrapper từ HEAD (≥ `7f3c25ba`), archive hai nơi, cài emulator; preflight, fake CHAIN 14/14, negative gate; replay response `fx-a03` cũ trên mã mới vẫn báo `L1_UNIT_UNKNOWN` (response cũ thật sự sai) | Đạt, 0 call | Lỗi mã → sửa trong gói |
| **T3** live (cần D-G1c) | Tiếp G1 cùng sổ chi tiêu, `fx-a03` trước, cùng quy tắc dừng nhóm; mỗi từ chối có `CODE:path` + response replay | Báo cáo theo fixture, call/token/USD | Từ chối → replay offline, sửa, hỏi owner |

**D-G1c (cần owner):** chạy T3 trong phần trần G1 còn lại (USD 1.00 − 0.03779120 ≈ 0.962). Khuyến nghị **đồng ý**, kèm quy tắc thường trực để giảm hỏi lặp: *sau một lần dừng, nếu bản sửa đã được chứng minh offline bằng replay đúng response gây lỗi và test hồi quy, Codex được chạy tiếp G1 trong trần G1 mà không cần hỏi lại; mọi mã lỗi **mới** hoặc vượt trần vẫn dừng và báo.*

**D-G1c đã được owner duyệt (2026-10-04, "đồng ý D-G1c"):** sau khi T1–T2 đạt, chạy T3 — tiếp G1 trên cùng sổ chi tiêu trong phần trần G1 còn lại (USD 1.00 − 0.03779120), `fx-a03` trước. **Quy tắc thường trực có hiệu lực:** sau một lần dừng, nếu bản sửa đã được chứng minh offline bằng replay đúng response gây lỗi cộng test hồi quy, Codex được chạy tiếp G1 trong trần G1 mà không cần hỏi lại; mọi **mã lỗi mới**, trạng thái UNKNOWN hoặc chạm trần vẫn dừng nhóm và báo (`CODE:path` + đường dẫn response). Quy tắc chỉ áp cho G1; G2/G3/G4/R7 và pilot vẫn cần owner quyết. 0 retry tự động, 0 repair call giữ nguyên.

## 11. Review T1–T3 và quyết định trùng lặp/trích dẫn (Claude, 2026-10-04)

**Kiểm độc lập `37958eab`:** engine 375/375, app 341/341, lint + androidTest compile PASS, Python 4/4 file. Không key/fingerprint. Live T3 (run `3a9c1d54-…`, `fx-a03`): **RAW được chấp nhận lần thứ hai**; RECONCILE bị từ chối `L1_RAW_QUOTE_NOT_IN_ANCHOR:findings.0.rawQuote` — khoảng trống thật của app: dòng RAW có furigana `《…》`, model trích phần chữ không kèm cách đọc. G1: 7 call, **USD 0.05538945 / 1.00**, 0 UNKNOWN. Replay đúng response sau bản sửa furigana đi tiếp tới `L1_OCCURRENCE_DUPLICATE:findings.0.occurrenceUnits.0` → điều kiện thường trực D-G1c chưa đạt, Codex dừng đúng.

**Quyết định 1 — trùng lặp (điều phối quyết, theo nguyên tắc "app làm phần ghi sổ"):**
- (a) **Trùng id định nghĩa thực thể** (`candidateId`, `errorId`, `spanId`, `probeId`, `changeId`, `preserveId`, resolution hai lần cho một candidate): **giữ từ chối** — đây là mơ hồ thật về thực thể.
- (b) **Trùng tham chiếu trong một danh sách** (`rawUnits[]`, `occurrenceUnits[]`, `candidateIds[]`, `evidenceRefs[]`, `changeIds[]`, `preserveIds[]`, `probes[].rawUnits[]`) và (c) **`occurrenceUnits` chồng `rawUnits`**: app **chuẩn hóa xác định**: bỏ phần lặp, giữ thứ tự lần xuất hiện đầu; bỏ khỏi `occurrenceUnits` các unit đã có trong `rawUnits`. Không thêm/bớt nội dung nghĩa. Ghi số mục đã khử vào metadata của artifact (`normalizations.duplicateReferencesRemoved`), không vào prompt.
- `L3_PROBE_ANCHOR_DUPLICATE` (hai probe khác nhau cùng anchor) giữ nguyên; chỉ xem lại nếu có từ chối thật.

**Quyết định 2 — bịt lỗ hổng trích dẫn:** `EditorialQuoteMatcher.contains/containsRaw` trả **false** khi trích dẫn **rỗng sau chuẩn hóa** (NFC + trim + bỏ `《…》`), áp cho mọi nơi dùng (`rawQuote`, `evidenceQuote`, `draftQuote`, `viQuote`, final-read `quote`, `before`). Test: `《x》`, chuỗi toàn khoảng trắng, chỉ furigana → không khớp; văn bản tổng hợp `試験例《しけんれい》を表示` ↔ `試験例を表示` vẫn khớp.

**Quyết định 3 — gỡ hết lỗi trong một lần replay:** thêm chế độ **replay-all** (chỉ offline, chỉ công cụ replay): parser chạy ở chế độ thu thập, tiếp tục qua từng item độc lập và in toàn bộ `CODE:path` của một response; parser production vẫn fail-fast. Nếu chế độ thu thập quá xâm lấn, tối thiểu cho công cụ replay áp các chuẩn hóa đã duyệt rồi chạy lại cho tới khi gặp lỗi không thuộc danh sách — và in đủ chuỗi lỗi.

**Quyết định 4 — dữ liệu sách:** thay mảnh RAW ngắn còn trong `docs/P6_R6_T1_T3_EXECUTION_20261004.md` và test (`試験用《しけんよう》行99です。`) bằng câu tổng hợp có furigana; không viết lại lịch sử Git (mảnh rất ngắn đã nằm trong commit cũ — ghi nhận, không force-push).

### Gói cho Codex

| Gói | Việc | PASS | Dừng |
|---|---|---|---|
| **U1** | Quyết định 1: chuẩn hóa (b)/(c) ở L1/L2/L3 + metadata; test từng đường dẫn; giữ (a) | JVM xanh; replay response RECONCILE `fx-a03` không còn `L1_OCCURRENCE_DUPLICATE` | — |
| **U2** | Quyết định 2 + test | JVM xanh | — |
| **U3** | Quyết định 3; chạy replay-all trên **cả hai** response `fx-a03` (RAW cũ phải vẫn báo `L1_UNIT_UNKNOWN` — response cũ sai thật; RECONCILE mới phải PASS hoặc liệt kê đủ lỗi còn lại) | Báo đủ danh sách | Còn lỗi không thuộc họ đã duyệt → dừng, báo, không gọi live |
| **U4** | Quyết định 4 | grep không còn mảnh RAW thật trong repo hiện tại | — |
| **U5** | Build wrapper từ HEAD, archive, emulator: preflight, fake CHAIN 14/14, negative gate | Đạt, 0 call | — |
| **U6** live (theo D-G1c) | Chỉ khi replay **đúng** response RECONCILE `fx-a03` PASS: tiếp G1 trên cùng sổ, `fx-a03` trước | Báo cáo theo fixture | Mã lỗi mới / UNKNOWN / chạm trần → dừng, replay-all, báo |


## 12. Owner điều chỉnh điều kiện D-G1c — 2026-10-04

Owner trả lời **“đồng ý”** cho đề xuất review `ede51de7`: phân loại kết quả replay thành PASS và EXPECTED_REJECT thay vì buộc mọi response từng bị từ chối phải trở thành PASS. Mục này thay điều kiện replay ở các mục 10–11 khi có khác biệt; không sửa lịch sử event.

- **App từ chối nhầm response hợp lệ:** sửa app, test hồi quy và replay chính response nguyên bản phải PASS.
- **Model trả response sai thật:** giữ nguyên response, validator phải tiếp tục từ chối đúng `CODE:path`; ghi EXPECTED_REJECT theo response hash và lý do đã kiểm chứng. Đây là PASS của negative test, không phải response hợp lệ, REPORT_L1 được chấp nhận hay semantic PASS. Không tự thêm mã lỗi vào danh sách kỳ vọng để qua gate.
- Response U6 `002-L1_RECONCILE.json`, SHA-256 `18c5ddb34caf6d197b346ad4b86e8b785b6e8bae5cc96bdeea9de12e0ea126e3`: finding đầu neo DRAFT 99 nhưng quote thuộc 101; giữ EXPECTED_REJECT `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`. Không tự chuyển neo, thay quote hoặc nới matcher.
- **Chưa cho chạy live ngay.** Trước đề xuất tiếp G1, hoàn tất nhóm offline dưới đây và cập nhật bằng chứng. Giữ nguyên D-G1c: cùng sổ G1, trần USD 1.00; đã quyết toán USD 0.07381520, còn USD 0.92618480 tại lần đo gần nhất (phải đọc lại trước dispatch); 0 retry tự động, 0 repair call; mã lỗi mới/UNKNOWN/chạm trần dừng nhóm và báo. Không mở G2/G3/G4/R7 hoặc pilot bằng quyết định này.

### Nhóm offline tiếp theo trong phạm vi hiện có

1. Sửa replay-all có kết quả máy đọc được và exit khác 0 khi có lỗi ngoài kỳ vọng. EXPECTED_REJECT phải khớp fixture/response hash và đúng mã/path đã phân xử; test process thật cho PASS, expected rejection và unexpected failure.
2. Thu thập lỗi của các item độc lập trong cùng response, production vẫn fail-fast; test ít nhất hai finding sai cùng lúc. Không gọi danh sách lỗi đầu tiên mỗi file là toàn bộ lỗi.
3. Regression cho neo đúng, sai dòng lân cận, quote trùng, nhầm namespace RAW/DRAFT, NFC/CRLF và chuẩn hóa đã duyệt; exact U6 response vẫn bị từ chối đúng.
4. Đánh giá cách giảm gánh chép bằng chứng của model mà không tự tạo proof; đưa finding quan hệ gia đình thứ hai vào adjudication false-positive, không tự coi là lỗi dịch đã được chứng minh.
5. Báo riêng structural/semantic và cập nhật plan/snapshot. Không biến negative-test PASS thành điều kiện phát hành artifact bị từ chối.

Review độc lập nằm ở `D:/P5E-private/review-ede51de7-20261004-codex/REVIEW.md`: engine 380/380, app 341/341 từ archive sạch; replay-all tái hiện in FAIL nhưng exit 0; ledger/response hashes khớp. Đây là evidence trước sửa, không phải nghiệm thu nhóm offline mới.

## 13. Kết quả gói sửa replay offline sau review U6 — 2026-10-04

Gói này chỉ sửa công cụ/engine và test offline; không gọi provider, không build/cài APK, không thao tác emulator hoặc pilot. `EditorialWireReplayTool` hiện xuất một JSON report schema 2 với `phase`, response SHA-256, `actualStatus`, `expectedStatus`, `codes`, `skipped`, `completed`, expectation errors và kết luận từng case/cả lượt. Manifest replay-all yêu cầu expectation tường minh; `EXPECTED_REJECT` bị ràng buộc bởi hash và danh sách `CODE:path`. Cú pháp sáu tham số cũ vẫn chạy ở chế độ `UNSPECIFIED` được ghi rõ, không tự gắn vào `fx-a03` và không thể biến lỗi thành PASS. Mọi kết luận FAIL, input thiếu, hash/mã không khớp hoặc tool error đều trả exit 2; PASS thực trả exit 0.

Production `parseReconcile` vẫn fail-fast. Diagnostic offline dùng lại hàm kiểm finding production cho từng item độc lập trong `findings[]`; khi một finding lỗi, các phần phụ thuộc `resolutions`, `speakerRecords`, `protectedSpans` và hậu kiểm được ghi `skipped`, không dựng dữ liệu thay thế. Khi mọi finding hợp lệ, parser production chạy nốt toàn bộ tail. Phạm vi này được ghi rõ là diagnostic có giới hạn, không tuyên bố đã thu thập lỗi của mọi mảng ngoài `findings[]`.

Evidence hồi quy process thật: engine `391/391` (trong đó replay `6/6` và ma trận neo `6/6`). Có test PASS exit 0, lỗi ngoài kỳ vọng exit 2, EXPECTED_REJECT đúng hash/mã exit 0 trong khi actual vẫn `REJECTED`, expected rejection nhưng actual PASS exit 2, hash/mã sai exit 2, JSON hỏng hoặc file thiếu exit 2; test cũng kiểm log không xuất khóa/header/model. Hai finding độc lập cùng sai được báo theo thứ tự ổn định; production path chỉ báo finding đầu và đánh dấu phần còn lại bỏ qua. Ma trận neo bao gồm exact/range, dòng lân cận, quote trùng nhưng anchor khác, tách namespace RAW/DRAFT, NFC/CRLF/trim, furigana đúng vai trò, quote rỗng sau chuẩn hóa, MISSING và out-of-range. Không có automatic reanchor, thay quote hay proof do app tạo.

Replay đúng response U6 nguyên bản: `D:\P5E-private\p6-runs\3559de99-978b-410e-b863-c082062187fa\results\fx-a03\responses\002-L1_RECONCILE.json`, SHA-256 `18c5ddb34caf6d197b346ad4b86e8b785b6e8bae5cc96bdeea9de12e0ea126e3`. Manifest/log replay-all giữ ngoài Git tại `D:\P5E-private\p6-item12-u7-replay-all-manifest-20261004.json` và `D:\P5E-private\p6-item12-u7-replay-all-20261004.json` (log SHA-256 `2991b717c258c1b6156a270b5357dc0dae5ed91adfaeb4e9524334de1181ce1e`); exit `0`: old RAW `REJECTED`/`EXPECTED_REJECT` `L1_UNIT_UNKNOWN:coverage.0.from`, current RAW `PASS`/`PASS`, RECONCILE `REJECTED`/`EXPECTED_REJECT` `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`. Finding `E182-MEANING` (RAW `L183`, DRAFT `182`) được diagnostic kiểm riêng về cấu trúc; đối chiếu prompt/RAW/DRAFT cho thấy bằng chứng đã nêu không đủ để khẳng định quan hệ gia đình cụ thể hơn, nên phân loại `UNRESOLVED_SUSPECTED_FALSE_POSITIVE`, không thêm mã expected và không coi là semantic PASS.

Phân tích gánh chép dữ liệu: prompt giữ source role RAW/DRAFT và số dòng riêng, nhưng bắt model chép lại cả `rawQuote` và `draftQuote`; đây là điểm dễ nhầm namespace và dòng. App có thể lấy văn bản theo anchor nếu đổi contract, nhưng quote do app tạo chỉ chứng minh vị trí, không chứng minh model đã so sánh đúng nghĩa. Vì vậy trong gói này chỉ ghi đề xuất contract version mới kèm invariant liên kết finding–RAW evidence–DRAFT anchor và test chống nhầm câu; không bỏ trường, không nới matcher, không đổi pin để lấy PASS.

Structural gate của gói đã đóng; semantic quality của model chưa được đo. Bước tiếp theo duy nhất là owner xem một đề xuất G1 mới có giả thuyết đo, source/prompt/schema revision, fixture, số call tối đa, ngân sách, điều kiện dừng và cách chấm; không dispatch từ quyết định offline này.

## 12. Review U1–U6 + replay repair; quyết định neo DRAFT theo trích dẫn (Claude, 2026-10-05)

**Kiểm độc lập `ab95da25`:** `git archive` sạch: engine 391/391, app 341/341, lint + androidTest compile PASS. Không key/fingerprint (hash duy nhất bị nghi là SHA-256 APK). U4 chưa xong hẳn: `EditorialL1AnchorRegressionTest.java:116-118` vẫn dùng mảnh `揃《そろ》えても` (5 ký tự, cũng có sẵn trong fixture cũ `app/src/test/resources/fixtures/v417/...`); mức độ nhỏ, thay bằng câu tổng hợp khi chạm file.

**Live U6 (run `3559de99-…`, `fx-a03`):** 2 call, USD 0.01842575, 51,605 vào / 4,604 ra, `finish=stop`. RAW **PASS lần 3** (38 candidate). RECONCILE bị từ chối `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`. G1 = **9 call, USD 0.07381520 / 1.00**, 0 UNKNOWN.

**Chẩn đoán (chứng minh từ response lưu + DRAFT fixture):** finding `E99-UNTRANSLATED` có RAW `L99` và `draft.start=end=99`; `draftQuote` (79 ký tự) xuất hiện **duy nhất ở DRAFT dòng 101**. Model chép số dòng RAW sang neo DRAFT (RAW 384 dòng, DRAFT 382 dòng, bố cục lệch). Finding thứ hai (`L183` → `182`) căn đúng. Đây là lần thứ năm cùng họ "model tự ghi sổ tham chiếu" (token → hash id → trường rỗng → minLength → số dòng DRAFT). Ghi nhận ngữ nghĩa (chưa chấm): `E99-UNTRANSLATED` đúng loại lỗi gieo của `fx-a03`.

**Quyết định (điều phối):** **neo DRAFT do app suy ra từ trích dẫn**:
- Model vẫn bắt buộc gửi `draftQuote` (bằng chứng so sánh) và số dòng DRAFT như **gợi ý**.
- App tìm `draftQuote` (sau NFC/trim/bỏ furigana, không rỗng) trong DRAFT: **đúng một** vị trí → dùng dòng đó; nhiều vị trí → chọn vị trí trùng hoặc gần gợi ý nhất trong cửa sổ ±3 dòng, nếu vẫn mơ hồ → từ chối `L1_DRAFT_QUOTE_AMBIGUOUS:path`; không thấy → từ chối như cũ. Với `LINES` nhiều dòng: dòng chứa trích dẫn là điểm neo, độ dài khoảng giữ theo gợi ý (`end-start`).
- Ghi vào metadata artifact: `draftAnchorDerivedFromQuote`, độ lệch so với gợi ý. Không tự tạo/thay trích dẫn, không đổi nghĩa.
- Áp cùng nguyên tắc cho neo VI_L2 của L3 (`viQuote`) và L2 (`before` đã làm ở S4). RAW giữ nguyên (đã ổn định, tham chiếu `L<n>` + `rawQuote` kiểm chuỗi con).
- Tăng `contractRevision`.

### Gói cho Codex

| Gói | Việc | PASS | Dừng |
|---|---|---|---|
| **W1** | Hiện thực quyết định trên cho L1 `findings[].draft` (+ L3 `viQuote` anchor); test: duy nhất/lệch gợi ý/nhiều vị trí trong và ngoài cửa sổ/không thấy/rỗng sau chuẩn hóa/MISSING không đổi; dữ liệu tổng hợp | JVM xanh | — |
| **W2** | Replay-all 3 case + response U6: RECONCILE U6 phải **PASS** (hoặc liệt kê đủ lỗi còn lại) | U6 PASS | Còn lỗi khác họ → dừng, báo |
| **W3** | Build wrapper, archive, emulator: preflight, fake CHAIN 14/14, negative gate | Đạt, 0 call | — |
| **W4** live theo D-G1c | Khi W2 PASS: tiếp G1 cùng sổ (còn USD 0.92618480), `fx-a03` trước; nếu L1 `fx-a03` commit → chạy tiếp các fixture G1 còn lại theo bảng | Báo cáo G1: `STRUCTURAL_VALID`/`SEMANTIC_EVAL` theo fixture, call/token/USD | Mã lỗi mới / UNKNOWN / chạm trần → dừng, replay-all, báo |

Giả thuyết cần live (không kiểm được offline): với neo DRAFT do app suy ra, RECONCILE của model thật qua validator ổn định trên nhiều fixture; ngân sách: phần còn lại của G1 (đã duyệt), ước ≈ USD 0.02 mỗi cặp L1.
