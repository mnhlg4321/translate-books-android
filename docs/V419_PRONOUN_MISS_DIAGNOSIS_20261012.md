# v4.19 — chẩn đoán lỗi sót xưng hô của luồng chunk (offline, USD 0)

Ngày 2026-10-12, nhánh `feature/v4.19`, baseline `998da341` (logic v4.18.0 / CP-IMPL-15, chưa đổi dòng mã sản xuất nào). Phạm vi: phase `X1_SUITE` và `X2_DIAGNOSE` của `EDITORIAL_PRONOUN_RECOVERY_V4_19.md`, kèm đề xuất cho `X3_PROPOSE`. Không gọi provider, không build APK, không chạm thiết bị. Văn bản sách và nhãn có văn bản nằm ở `D:\P5E-private\v419-pronoun\` (ngoài Git); tài liệu này chỉ có số đếm, id và hash.

## 1. Kết luận ngắn

1. **Phần lớn lỗi sót còn lại không nằm ở finder.** Trên 8 chương, 88 trong 106 dòng thoại mà owner sửa và có từ xưng hô thay đổi đã được finder gắn cờ (83,0%). Lượng chưa phủ nhỏ và rải ở ba loại cơ học (mục 4.1).
2. **Con số "13 SÓT của 006" và "7 SÓT của 008" đo dưới prompt cũ**: 006 chạy với CP-IMPL-9, 008 với CP-IMPL-11. Quy tắc tên-call (CP-IMPL-14) chỉ có trong lần chạy 011, nơi `cậu` → tên call sửa được 7/8 điểm. Prompt đang phát hành (CP-IMPL-15) **chưa từng được đo** trên 006 và 008. `RELEASE_NOTES.md` v4.18.0 đã ghi rằng không phải mọi ca gốc của 006/008 được đo lại; số liệu dưới đây làm rõ phạm vi: các giới hạn 006/008 là của prompt cũ, chưa phải số đo của bản đang chạy.
3. **13 trong 20 điểm SÓT thuần xưng hô (S2) là dòng mà FINAL của owner cũng không sửa** (7 ở 006, 6 ở 008). Nhãn SÓT của chúng đến từ RAW + hàng pronoun, không từ FINAL. Chỉ 7 điểm được FINAL xác nhận. Trước khi viết thêm mã hoặc tốn tiền đo lại, cần owner quyết định hàng pronoun hay bản FINAL là chuẩn cho 13 dòng này.
4. Các bản sửa finder được thử bằng số liệu đều có hiệu suất thấp (mục 5.3); không đề xuất làm ngay.

## 2. Đã dựng

| Thành phần | Vị trí | Ghi chú |
|---|---|---|
| Bộ đo sự kiện từng dòng (S1) | `app/src/test/java/com/ml/tblandroidtxt/EditorialPairPronounMissDiagnosisTest.java` | Opt-in (`DIAG_INPUT_ROOT`, `DIAG_OUT`). Với mỗi dòng DRAFT ghi: đoạn RAW, có thoại, các hàng phủ đoạn, hàng đã chiếu theo cue, hàng đủ self+call, xung đột, từ xưng hô, loại điểm finder thật gắn. Tự kiểm: quy tắc suy lại phải khớp finder thật trên **mọi** dòng — 1.447/1.447 khớp. Lần chạy đầu lệch 58 dòng ở 014 vì tôi chưa mô phỏng việc finder chỉ dùng hàng chọn theo cue từng chunk; đã sửa trước khi dùng số liệu |
| Công cụ phân tích và bộ kiểm | `scripts/chunk/pronoun_miss_diagnosis.py` (+ `test_pronoun_miss_diagnosis.py`, 24 test tổng hợp) | Lệnh `analyze`, `suite`, `verify-replay`. Từ vựng Python được test so với `AddressChecklist.WORDS` trong Java |
| Runner hồi quy | `scripts/chunk/run-pronoun-suite-replays.ps1` | Phát lại các phản hồi model đã lưu của 006 (C6E), 008 (C7A), 011 (C7D) qua mã hiện tại rồi so từng điểm với kết quả ghi trong bộ kiểm. Exit 1 nếu một mục bản sạch đổi hoặc một sửa sai đã biết lọt lại |
| Bộ lỗi đã biết | `docs/V419_PRONOUN_KNOWN_MISS_INDEX.json` (36 mục) | Chỉ mục công khai (id, tầng, nhãn, hash RAW/DRAFT, kết quả live và hiện hành, trạng thái FINAL tham chiếu). Sidecar có văn bản: `known-misses.json` |
| **Bản sạch (tách riêng)** | `docs/V419_PRONOUN_CLEAN_CONTROL_INDEX.json` (122 mục) | Điểm ĐÚNG: 90 giữ nguyên, 24 sửa đúng cần giữ, 8 sửa sai bị chặn đúng cần tiếp tục bị chặn. Sidecar: `clean-controls.json`. Công cụ từ chối ghi nếu hai phần dùng chung một mục hoặc bản sạch chứa nhãn khác ĐÚNG |
| Khoảng trống finder (công khai) | `editorial-engine/.../fix/PronounMissGapCharacterizationTest.java` | 6 test với văn bản bịa: 2 đối chứng đúng, 4 `knownGap` ghim hành vi hiện tại (hàng thiếu `call`, từ ngoài từ vựng như `mình`, tên/danh xưng sai thay cho call, đảo self/call). Commit sửa gốc sẽ lật các assertion tương ứng |
| Tóm tắt S1 | `docs/V419_PRONOUN_S1_SUMMARY.json` | Số đếm theo chương và theo lý do; không có văn bản |
| Trang đọc cho owner | `D:\P5E-private\v419-pronoun\reading-s2-s3.html` | 23 điểm (14 FINAL không sửa, 9 còn lại) kèm RAW, DRAFT, app hiện hành, FINAL, hàng áp dụng và ô phân xử |

Hash sidecar riêng (SHA-256): `line-facts.jsonl` `b12095a9…15c3e`; `known-misses.json` `af7b1c79…1277e`; `clean-controls.json` `90624e2e…92f03`; `s1-analysis.json` `928088d8…26de4`; `reading-s2-s3.html` `94c96131…52d`.

**Baseline hồi quy (trên mã chưa sửa):** `run-pronoun-suite-replays.ps1` cho 158/158 điểm giữ nguyên, 0 hồi quy, 0 cải thiện. Engine 712 test (1 skip), app 473 (3 skip), `scripts/chunk` 48, đều 0 lỗi.

## 3. Các tầng và số đo

Tầng: S1 finder không gắn cờ; S2 gắn cờ nhưng model giữ `=`; S3 guard chặn nhầm sửa đúng; S4 sửa sai lọt guard; S5 nhãn chưa chắc. Nhãn theo RAW + hàng pronoun (bộ chấm phân xử đã có); FINAL chỉ là tham chiếu.

### 3.1 S1 — độ phủ của finder (1.447 dòng DRAFT, 8 chương, so với dòng owner sửa trong FINAL)

| Chương | Dòng | Dòng có thoại | Dòng finder gắn cờ ADDRESS | Dòng owner sửa | …có từ xưng hô đổi | Đã gắn cờ | Chưa phủ (lý do) |
|---|---|---|---|---|---|---|---|
| 004 | 138 | 62 | 14 | 60 | 40 | 1 | 36 không thoại, 3 ngoài từ vựng |
| 005 | 167 | 51 | 12 | 31 | 9 | 9 | — |
| 006 | 247 | 118 | 65 | 38 | 21 | 18 | 2 không thoại, 1 ngoài từ vựng |
| 007 | 104 | 20 | 1 | 18 | 2 | 1 | 1 không thoại |
| 008 | 132 | 48 | 25 | 20 | 0 | 0 | — |
| 011 | 214 | 115 | 55 | 30 | 17 | 16 | 1 ngoài từ vựng |
| 014 | 273 | 156 | 58 | 52 | 37 | 18 | 6 không thoại, 11 xung đột hàng, 2 ngoài từ vựng |
| 017 | 172 | 97 | 75 | 40 | 27 | 25 | 2 không thoại |
| **Tổng** | 1.447 | 667 | 305 | 289 | 153 | **88 (57,5%)** | 47 không thoại, 11 xung đột, 7 ngoài từ vựng |

Số này khớp phép đo sẵn có của app (`EditorialPairC6CorpusMeasurementTest`: 98/171 = 57,3%; hai bên định nghĩa tập mẫu hơi khác). Nếu chỉ tính dòng có thoại: 88/106 = **83,0%**. "Từ xưng hô đổi" là ứng viên để đọc, không phải nhãn lỗi: nó gồm từ vựng cố định cộng self/call của các hàng trong chương.

Trong tập 153 dòng owner sửa có từ xưng hô đổi, các nguyên nhân cơ học sau không gây mất dòng nào: hàng thiếu `self` hoặc `call` (0 dòng), hàng không được chiếu theo cue (0), đoạn không có hàng phủ (0), từ nằm trong tập cho phép nhưng sai vai (0).

### 3.2 S2–S5 — kết quả đã phân xử, tính trên điểm ADDRESS

| Tầng | 006 (C6E, CP-IMPL-9) | 008 (C7A, CP-IMPL-11) | 011 (C7D, CP-IMPL-14) | Tham chiếu FINAL |
|---|---|---|---|---|
| S2 model giữ `=` | 13 | 6 | 1 | 7 FINAL cũng sửa, **13 FINAL không sửa** |
| S2B model đề xuất sai, bị chặn đúng, DRAFT vẫn sai | 0 | 1 | 0 | FINAL không sửa |
| S3 guard chặn nhầm | 1 (7:3, **nay FIXED** dưới mã hiện hành) | 0 | 1 (2:2, **vẫn mở**) | FINAL cùng sửa |
| S4 sửa sai lọt | 1 (14:5, nay bị chặn) | 0 (1:2 là điểm glossary, nay không còn là điểm) | 1 (12:8, nay bị chặn) | FINAL không sửa cả hai điểm ADDRESS |
| S5 chưa chắc | 5 | 0 | 3 | 4 FINAL sửa, 4 không |

Trong 16 điểm ADDRESS model sửa đúng, FINAL cũng sửa 15. Trong 88 điểm giữ đúng, FINAL đổi 11 (sửa ngoài luật, không tính là lỗi).

## 4. Chẩn đoán

### 4.1 Finder (S1)

- **Không thoại (47 dòng):** 36 dòng là ở chương 004: người kể ngôi một của Erika, hàng `私 / @NARRATOR` có `self=tôi`, DRAFT viết `mình`, owner đổi `mình` → `tôi`. Finder chỉ xét thoại nên bỏ qua. Dò thử một bộ phát hiện chung "ngôi một khác self của người kể trong lời kể": chương 004 trúng 36/44, nhưng **8 chương còn lại 0/72** (owner không đổi dòng nào). Bộ phát hiện chung vì thế sẽ nhiễu gần như toàn bộ ngoài 004; không đề xuất. Đây là lỗi riêng của bản DRAFT 004.
- **Xung đột hàng (11 dòng owner sửa, cả 014):** hai hàng cùng phủ một đoạn nên finder bỏ đoạn (an toàn có chủ ý). 014 và 017 có 69 dòng thoại thuộc đoạn xung đột, 43 dòng có từ xưng hô; chỉ 9 dòng có đúng một cặp (self, call) được gọi tên bằng vocative `from` trong RAW. Giải xung đột bằng vocative chỉ cứu tối đa 9 dòng.
- **Ngoài từ vựng (7 dòng):** 3 `mình` (004), 1 `tiền bối`→`chị`, 2 đổi tên riêng, 1 `mình` (011). Thử thêm `mình` vào từ vựng (chỉ tính khi mọi hàng phủ dòng đều không cho `mình`): chỉ thêm **2 điểm** trên 8 chương, **0 điểm** trùng dòng owner sửa. Không đáng làm.

### 4.2 Model (S2) — nguyên nhân chính của phần sót, nhưng chưa thể kết luận

- Độ mơ hồ số hàng **không phân biệt** nhóm sót và nhóm sửa đúng: trung bình 4,5 hàng/điểm ở S2, 4,4 ở điểm model sửa đúng, 3,4 ở điểm giữ đúng. Vocative `from` trong dòng RAW cũng không phân biệt (7/20 so với 3/16). Giả thuyết "quá nhiều hàng nên model giữ `=`" **không được số liệu ủng hộ**.
- Phân bố theo lần chạy: trong 16 điểm ADDRESS model sửa đúng, **cả 7 điểm `cậu` → tên call đều ở 011 (CP-IMPL-14)**; lần chạy 006 (CP-IMPL-9) sửa 3 điểm và lần chạy 008 (CP-IMPL-11) không sửa điểm ADDRESS nào. Các S2 của 006/008 vì vậy đo trước khi có quy tắc tên-call; bằng chứng gần nhất cho thấy loại `cậu` → tên có thể đã được xử lý, nhưng chỉ đo trên một chương.
- 008 chỉ có 2 hàng cho mỗi điểm và vẫn giữ nguyên 6/6; đồng thời FINAL cũng không sửa 6/6. Với 008, khó tách "model không tuân" khỏi "chuẩn của owner khác hàng pronoun".

### 4.3 Guard (S3) và sửa sai (S4)

- S3 chỉ còn 1 điểm mở (011 2:2): model đúng (`cậu` → `Erika`), nhưng `依里花が` là ngôi ba trong cùng câu nên khóa ngôi ba chặn. Một ca, đã ghi từ C7C; mở rộng cửa sổ cảnh sẽ làm 3:2 của 006 mở lại, nên không sửa.
- Ba sửa sai lọt đã biết (14:5, 12:8, glossary 1:2) đều không còn lọt dưới mã hiện hành; bộ kiểm giữ chúng ở trạng thái "phải còn bị chặn".

## 5. Đề xuất bản sửa (xếp theo giá trị/chi phí)

| # | Đề xuất | Loại | Kiểm bằng gì | Cần |
|---|---|---|---|---|
| A | **Owner đọc 23 điểm** ở `reading-s2-s3.html` và quyết định, riêng cho 14 điểm FINAL không sửa: hàng pronoun là luật (cần đổi) hay giữ như FINAL. Kết quả chuyển các điểm giữ lại sang bản sạch hoặc xác nhận chúng là lỗi thật | Không mã, USD 0 | Cập nhật nhãn trong bộ kiểm, chạy lại `suite` + `verify-replay` | Owner đọc |
| B | **Đo lại prompt đang phát hành trên 008 rồi 006** (một lần chạy mỗi chương, luna medium, không retry). Đây là việc duy nhất xác định S2 hiện nay còn bao nhiêu. Tham chiếu chi phí: C7A thực tế USD 0,0074; C6E USD 0,0319; mức giữ chỗ đo offline hiện nay 008 ≈ USD 0,0228, 006 ≈ USD 0,0868 | Đo, trả phí | Chấm theo RAW + hàng, bản sạch phải giữ nguyên, 0 dòng ngoài điểm, so với bộ kiểm | **Quyền chi riêng của owner**: chương, trần (gợi ý 008: USD 0,03; 006: USD 0,10 chỉ sau 008), số lời gọi, sổ mới. Không làm trước A |
| C | Không sửa mã finder trong vòng này trừ khi A/B cho thấy cần. Các ứng viên đã đo: thêm `mình` (2 điểm, 0 trúng), giải xung đột bằng vocative (≤ 9 dòng, chỉ 014/017), dò ngôi người kể (nhiễu 72/72 ngoài 004) | Mã | Test `knownGap` được lật cùng commit | — |
| D | Guard 011 2:2: chỉ sửa nếu có bằng chứng thứ hai ngoài cửa sổ cảnh (đã ghi từ C7C) | Mã, nhỏ | `verify-replay` (2:2 từ KEPT sang FIXED, 3:2 của 006 vẫn bị chặn) | Sau A |

Khuyến nghị: làm A trước (miễn phí, và quyết định nội dung của B), sau đó B trên 008 rồi quyết định có cần sửa mã không. Tôi không đề xuất viết bản sửa finder hay prompt khi chưa có số đo B, vì số liệu hiện có cho thấy finder đã phủ 83% dòng thoại và chưa biết phần S2 còn lại sau CP-IMPL-14.

## 6. Giới hạn

- FINAL là tham chiếu; "owner sửa" chỉ dùng để tìm ứng viên. Nhãn S2–S5 của 006/008/011 là nhãn đã phân xử trước đó (RAW + hàng) và không đổi; 14 điểm FINAL không sửa là phần nhãn cần owner xác nhận.
- Chỉ có 3 chương có phản hồi model đã lưu; S2 của 004, 005, 007, 014, 017 chưa đo. Replay không thể dự đoán model.
- Ghép FINAL với DRAFT bằng so khớp dòng (difflib) nên khối sửa không đều có thể lẫn; đã dùng đếm để so với đo sẵn có của app (57,3% / 57,5%).
- Bộ kiểm gắn với các lần chạy C6E/C7A/C7D; nếu chạy lại 006/008 sẽ cần thêm lượt (run) mới vào `RUNS`.
- Không có lời gọi provider, không có APK, không thiết bị; `app/src/main` và `editorial-engine/src/main` không đổi so với `998da341`.

## 7. Chạy lại

```powershell
# 1) dữ liệu từng dòng (ngoài Git)
$env:DIAG_INPUT_ROOT='D:\P5E-private\q2-inputs'; $env:DIAG_OUT='D:\P5E-private\v419-pronoun\line-facts.jsonl'
.\gradlew.bat :app:testDebugUnitTest --offline --rerun --tests *EditorialPairPronounMissDiagnosisTest*
# 2) phân tích, bộ kiểm và chỉ mục công khai
py -3 scripts/chunk/pronoun_miss_diagnosis.py analyze --facts <line-facts.jsonl> --final-root <5.FINAL> --out-private <...> --out-public docs/V419_PRONOUN_S1_SUMMARY.json
py -3 scripts/chunk/pronoun_miss_diagnosis.py suite --guard-dir D:\P5E-private\c6-guard-20261012 --facts <line-facts.jsonl> --final-root <5.FINAL> --out-private-dir D:\P5E-private\v419-pronoun --out-public-dir docs
# 3) hồi quy sau mỗi thay đổi finder/guard/prompt
powershell -File scripts/chunk/run-pronoun-suite-replays.ps1
```

## 8. Review độc lập của coordinator — 2026-10-13 (offline, USD 0)

**Đã kiểm trên archive sạch `2e7900d0`** (zip SHA-256 `4D601317…4501`): engine 712 (1 skip), app 473 (3 skip), `scripts/chunk` 48, 0 lỗi; `run-pronoun-suite-replays.ps1` 158/158 giữ nguyên, exit 0 (ghi vào thư mục riêng, không đè `replay-current` của phiên trước). Hash bốn sidecar riêng khớp mục 2. `app/src/main` và `editorial-engine/src/main` không đổi so với `998da341`. Chỉ mục công khai chỉ có id, hash, nhãn và số đếm (0 ký tự Nhật trong JSON; 5 ký tự trong báo cáo này là token cue). Các con số S1 ở mục 3.1 khớp `s1-analysis.json`.

**Sửa trong review (commit này):**

1. **Test opt-in im lặng được đếm là PASS.** `EditorialPairPronounMissDiagnosisTest` và 4 method của `EditorialPairC6CorpusMeasurementTest` (`return` khi thiếu biến môi trường) báo PASS dù không chạy gì; repo đã có quy ước `Assume.assumeTrue` (`Cs1PerformanceCountTest`, `EditorialAddressCheckCorpusTest`). Đã đổi cả họ năm method sang `Assume`. Kết quả mới của app trên archive sạch cộng đúng hai file này: 473 test, **465 PASS, 8 SKIP**, 0 lỗi (trước đây "473, 3 skip" đã tính 5 method rỗng là PASS). Replay chạy lại sau sửa: 158/158, exit 0.
2. Đường dẫn riêng ở mục 7 của plan bị hỏng do ký tự điều khiển (`\v`, `\r` trong chuỗi), đã sửa.

**Điều chỉnh cách đọc chẩn đoán:**

- **Tổng 83% che chênh lệch theo chương.** Finder phủ 18/37 dòng ở 014 (48,6%; 11 dòng mất do xung đột hàng) và 1/40 ở 004. 014 là chương nhiều hàng pronoun nhất nhưng chưa có lần chạy model nào được lưu, nên S2 của 014 (và 004, 005, 007, 017) là `NOT_MEASURED`. Kết luận "finder không phải nguyên nhân chính" chỉ đúng cho 006/008/011.
- **Ca `mình` ngoài từ vựng và ca xung đột hàng cùng một họ.** Kiểm lại từ `line-facts.jsonl`: ở 4/7 dòng ngoài từ vựng mà owner sửa (004 ×3, 011 ×1), ít nhất một hàng phủ dòng có `mình` ở self/call của nhân vật khác, nên phép thử "mọi hàng đều không cho `mình`" loại chúng (vì vậy 0 trúng). Cùng với 11 dòng xung đột ở 014, đây là hai biểu hiện của một thiếu sót thiết kế: finder không biết dòng thoại thuộc hàng nào (người nói → người nghe). Không mở rộng từ vựng; nếu sửa finder thì sửa ở việc gán hàng cho dòng, và phải đo trên bản sạch trước.
- **Đề xuất A cần đặt câu hỏi đúng.** FINAL là tham chiếu, không phải luật. Trong 14 điểm FINAL không sửa, 13 điểm có `cậu` trong DRAFT (đếm từ `known-misses.json`); dạng chung: hàng có `call` là **tên** (ví dụ `self=mình`, `call=<tên>`), DRAFT dùng đại từ ngôi hai `cậu`. Ở 008, model còn tự đổi tên → `cậu` 3 lần (khóa chặn), và FINAL giữ `cậu` ở cả 6 điểm. Như vậy câu hỏi thật là **ngữ nghĩa của `call` khi nó là tên**: tên chỉ dùng khi gọi (vocative) và `cậu` được phép làm đại từ ngôi hai, hay mọi `cậu` phải đổi thành tên. Hàng pronoun hiện không có chỗ ghi đại từ ngôi hai được phép kèm tên. Câu trả lời quyết định ba thứ: nhãn của 14 điểm, quy tắc tên-call của CP-IMPL-14 (nếu `cậu` được phép thì quy tắc này có thể sinh SAI ở 008/006), và việc có cần chạy B hay không.
- Vì vậy thứ tự A → B giữ nguyên, nhưng A là quyết định về luật/hợp đồng hàng pronoun, không phải chọn "hàng hay FINAL".

**Quyết định owner D-A (2026-10-13):** khi `call` của hàng là **tên**, `cậu` được giữ làm đại từ ngôi hai nếu `self` của hàng là `mình` hoặc `tớ`; tên chỉ bắt buộc ở lời gọi trực tiếp (vocative). Với `self` khác (`tôi`, `em`, …) luật cũ giữ nguyên cho tới khi owner quyết thêm. Hệ quả cần kiểm ở X3: nhãn SÓT của các điểm `cậu` có `self=mình/tớ` không ở lời gọi trực tiếp đổi thành ĐÚNG (chuyển sang bản sạch); quy tắc tên-call của CP-IMPL-14 phải không đổi các `cậu` đó.


## 9. X3 — áp D-A vào bộ kiểm (2026-10-13, offline, USD 0)

Baseline `132479dd`. Không gọi provider, không build, không đụng thiết bị; `app/src/main` và `editorial-engine/src/main` không đổi.

### 9.1 Gắn lại nhãn 23 điểm

Bảng luật công khai `docs/V419_PRONOUN_DA_RELABEL.json` (chỉ id + lý do, không câu sách). Công cụ `pronoun_miss_diagnosis.py suite --relabel` từ chối nếu id không có trong bộ kiểm, nhãn gốc không khớp, thiếu lý do, hoặc luật định gán nhãn khác ĐÚNG (luật chỉ chuyển điểm sang bản sạch, không tạo lỗi). Phán xử theo RAW + hàng pronoun; người nói/người nghe đọc từ RAW và các dòng RAW kề; FINAL chỉ tham chiếu.

| Kết quả | Điểm | Lý do |
|---|---|---|
| **SÓT → ĐÚNG, chuyển sang bản sạch (7)** | 006: 8:4, 9:1, 11:4, 11:5, 13:4, 14:2; 011: 2:2 | Erika ↔ Rea (self `mình`, call là tên). `cậu` ở vị trí đại từ ngôi hai; chỗ RAW gọi tên thì DRAFT đã có tên |
| Giữ SÓT, lỗi self (5) | 006: 2:3, 2:4, 4:3, 5:2, 13:1 | DRAFT tự xưng `tớ`/`tôi`, hàng ghi self `mình`. Ở 2:3, 5:2 chữ `cậu` đã hợp D-A, chỉ còn lỗi self |
| Giữ nhãn cũ, self khác `mình/tớ` (11) | 006: 5:4, 5:6 (Tatsuoka → Erika, self `tôi`, call `cô`), 7:3 (Erika → Mitsurugi, self `tôi`); 008: cả 7 điểm (Erika → Asanuma, self `tôi`); 011: 10:6 (`tôi`/`em`) | D-A không áp dụng |

Bộ kiểm sau X3: **29 lỗi đã biết** (trước 36): S2 15, S2B 1, S3 1, S4 3, S5 9. **129 điểm bản sạch** (trước 122): CLEAN_KEPT 97, CLEAN_FIXED_OK 24, CLEAN_WRONG_PROPOSAL_BLOCKED 8. Tổng vẫn 158. Sidecar cũ giữ ở `D:\P5E-private\v419-pronoun\pre-X3\`.

**Replay** (`run-pronoun-suite-replays.ps1`, mã hiện hành, phản hồi đã lưu của 006 C6E, 008 C7A, 011 C7D): **158/158 giữ nguyên, 0 hồi quy, 0 cải thiện**, exit 0. Không mục bản sạch nào đổi kết quả và không sửa sai cũ nào lọt lại.

Python `scripts/chunk` 53 test (thêm 5 test cho `--relabel`), 0 lỗi.

### 9.2 Chỗ CP-IMPL-14/15 trái D-A (test offline)

`editorial-engine/.../fix/NameCallRuleDaContradictionTest.java`, 6 test, tên giả (Aki, Rina, Ken), 6/6 PASS. Ba test `knownDaContradiction…` ghim hành vi hiện tại, commit thực hiện D-A phải lật chúng:

1. **Prompt** (`TargetedFixPrompt.java:35-37`): luật tên-call bảo thay mọi `cậu/bạn/mày/anh/chị/em` hướng tới người nghe bằng tên call khi người nghe là target của hàng. Không có ngoại lệ self `mình/tớ`, không giới hạn ở lời gọi trực tiếp. Đây là chỗ **đòi đổi `cậu` trái D-A**.
2. **Finder**: dòng thoại có `cậu` của hàng self `mình` + call là tên vẫn thành điểm dù không có lời gọi tên (model bị hỏi, prompt bảo đổi).
3. **Guard** (`AddressEditGuard`): dạng hợp lệ của hàng chỉ là self/call, nên câu trả lời đưa `cậu` vào cho hàng self `mình` (vd thay `bạn`) bị từ chối `ADDRESS_FORM_NOT_IN_APPLICABLE_ROW` dù hợp D-A. Guard **không** buộc đổi `cậu`; đổi `cậu` → tên vẫn được nhận (đúng với D-A: tên luôn hợp lệ).

Ba test `control…` phải giữ: lời gọi tên bị thay bằng `cậu` vẫn là điểm; đổi `cậu` → tên được nhận; sửa self giữ `cậu` được nhận.

### 9.3 Đề xuất bản sửa nhỏ nhất (chưa sửa mã sản xuất)

**Chỉ sửa prompt**, không sửa finder hay guard:

- Thêm vào luật tên-call: *"If the row's self is mình or tớ, cậu is a valid second-person pronoun for that row: use the call name only where the RAW calls the person by name (a direct vocative); otherwise return [id] = for that word."*

Lý do không sửa finder/guard: finder không biết dòng thuộc hàng nào (người nói → người nghe), nên một luật kiểu "có hàng `mình/tớ` phủ dòng thì bỏ qua `cậu`" sẽ xóa luôn điểm của lỗi thật. Đếm trên bộ kiểm: 006 5:4, 5:6 (Tatsuoka → Erika, `cậu` phải là `cô`) và 7:3 đều bị phủ bởi hàng `mình` của Erika ↔ Rea; chúng sẽ mất điểm. Nới guard cho `cậu` cũng mở đường cho đổi sai `cô` → `cậu` ở chính các dòng đó. Việc phân biệt người nói thuộc về model (đọc RAW), đúng thiết kế hiện tại.

**Cách đo trên bản sạch:**

1. Lật `knownDaContradictionPromptReplacesEveryAddressedCauWithTheCallName`; hai test `knownDa…` của finder và guard giữ nguyên (không sửa hai tầng đó).
2. Replay bộ kiểm: prompt không nằm trong đường replay (replay dùng phản hồi đã lưu), nên phải ra 158/158 không đổi. Đây là kiểm không hồi quy của finder/parser/applier/guard.
3. Trong bộ kiểm, đánh dấu **20 điểm bản sạch "tùy chọn theo D-A"**:
   - 13 điểm `CLEAN_KEPT` có `cậu` (006: 1:2, 1:4, 1:7, 2:1, 8:2, 8:4, 9:1, 11:4, 11:5, 13:4, 14:2, 14:3; 011: 2:2);
   - 7 điểm `CLEAN_FIXED_OK` `cậu` → tên của 011 (2:1, 2:3, 2:4, 2:5, 2:6, 3:3, 4:2).
   Với các điểm này, ở lần chạy thật sau, cả "giữ `cậu`" lẫn "đổi thành tên call" đều ĐÚNG; mọi dạng khác là hồi quy.
4. Hành vi model của prompt mới chỉ đo được bằng lần chạy thật (cần quyền chi riêng). Chương phù hợp: 006 (có cả điểm D-A tùy chọn, lỗi self `tớ`, và lỗi `cậu` → `cô` của Tatsuoka).

### 9.4 Gói đo B cho 008 (soạn sẵn, **chưa có quyền chi** — dừng ở đây)

Còn lỗi bỏ sót thật theo luật hiện hành: 7 điểm 008 (Erika → Asanuma, self `tôi`, call `Asanuma`; DRAFT dùng `cậu`) và 5 lỗi self `tớ/tôi` của 006. Riêng 008 đo được ngay: D-A không áp dụng cho self `tôi`, nên prompt hiện hành (CP-IMPL-15, có luật tên-call) là đúng luật cho 008 và chưa từng được đo trên 008 (C7A chạy CP-IMPL-11).

| Mục | Nội dung |
|---|---|
| Giả thuyết | Luật tên-call của CP-IMPL-14/15 làm model sửa `cậu` → `Asanuma` ở các điểm 008 mà CP-IMPL-11 bỏ sót (0/7 sửa ở C7A), không thêm sửa sai |
| Vì sao không đo offline | Hành vi model; replay chỉ phát lại phản hồi cũ |
| Phạm vi | Chỉ chương 008, cả chương một lần, `openai/gpt-5.6-luna` medium, **không retry**, tối đa **4 lời gọi** (C7A: chunk 1, 5, 6, 7 có điểm; chunk 2–4 không gọi), sổ mới `B008-<date>` trần **USD 0.03**, chỉ `emulator-5554` |
| Build | APK đang phát hành 4.18.0 / code 266 (logic CP-IMPL-15, khớp `main` mà `feature/v4.19` chưa đổi mã sản xuất) cùng AndroidTest runner tương ứng; không build mới |
| Ước tính | C7A thực tế USD 0.0074 (4 lời gọi, 14 122 vào / 3 230 ra); giữ chỗ xấu nhất với sàn 4 096 token ≈ USD 0.0228 |
| Chấm | 7 điểm `cậu` của 008 (ĐÚNG / SÓT / SAI theo RAW + hàng); bản sạch 008 phải giữ nguyên; 0 dòng ngoài điểm bị đổi; S2B 6:1 không được thành sửa sai lọt |
| Dừng | ước tính vượt trần, finish `length` hay lời gọi không có câu trả lời, UNKNOWN cost, lỗi hạ tầng |
| Lưu ý cho owner | FINAL của owner giữ `cậu` ở cả 6/6 điểm `cậu` này. Nếu owner định mở rộng D-A cho self `tôi`, nên quyết trước khi chi tiền, vì khi đó các điểm 008 chuyển thành bản sạch và B mất mục đích |

## 10. Review độc lập X3 — 2026-10-13 (offline, USD 0)

**Đã kiểm trên archive sạch `c57edd22`** (zip SHA-256 `9FB9B727…FEE2`): `scripts/chunk` 53, engine 718 (1 skip; `NameCallRuleDaContradictionTest` 6/6), 0 lỗi; app không đổi từ `dc612fb3` (465 PASS / 8 SKIP). Sidecar riêng sau X3 khớp chỉ mục công khai (29 và 129 mục, cùng id/nhãn/tầng/hash), hai phần không giao nhau, tổng 158. Replay trên archive sạch với bộ kiểm mới: 158/158 không đổi, exit 0 (ghi vào thư mục review riêng). Bảng `V419_PRONOUN_DA_RELABEL.json` chỉ chuyển điểm sang ĐÚNG khi đúng điều kiện D-A; các điểm giữ lại có lý do đúng (self sai, hoặc `call=cô` là đại từ chứ không phải tên). Mã sản xuất không đổi.

**Phát hiện làm đổi khuyến nghị về gói B.** Với hàng có `call` là tên và `self=tôi`, FINAL của owner **không theo `self`**: 006 7:3 (Erika → Fuyu, `self=tôi`, `call=Fuyu`) FINAL đổi `cậu` thành tên; 008 cả 7 điểm (Erika → Asanuma, `self=tôi`, `call=Asanuma`) FINAL giữ `cậu`. Với `self=mình`, FINAL cũng không nhất quán: 006 giữ `cậu` ở 6 điểm Erika ↔ Rea, 011 đổi sang tên ở 7 điểm cùng cặp. Như vậy không luật nào dựa trên `self` khớp được FINAL; giữa `cậu` và tên phần lớn là lựa chọn văn phong, chỉ một số quan hệ (hàng) owner muốn dùng tên. Hệ quả:

- Gói B 008 như soạn ở 9.4 sẽ đo việc model đổi `cậu` → `Asanuma`, điều FINAL bác 7/7. Chi tiền cho B không giúp đóng lỗi thật mà owner muốn; **không khuyến nghị chạy B**.
- Hai hướng hợp lý, cần owner chọn:
  - **(a) Mở rộng D-A cho mọi `self`:** khi `call` là tên, `cậu` là đại từ ngôi hai hợp lệ; tên chỉ bắt buộc ở lời gọi trực tiếp. Cả giữ `cậu` lẫn đổi thành tên đều ĐÚNG. 008 (7 điểm) và 006 7:3 chuyển sang bản sạch “tùy chọn”; X4 chỉ sửa prompt và điều kiện đơn giản hơn (không cần xét `self`). Mất khả năng ép dùng tên ở các hàng như Fuyu (1 điểm đã biết; mã hiện hành đã sửa điểm này nhờ khóa ngôi ba).
  - **(b) Khai theo từng hàng:** hàng muốn cho phép `cậu` ghi `call` dạng `Asanuma/cậu`. Finder và guard đã tách `call` thành token (`AddressChecklist.tokens`, `AddressEditGuard.forms`) nên coi cả hai dạng là hợp lệ mà không cần sửa mã; chỉ prompt phải hiểu dấu `/`. Chính xác hơn nhưng owner phải sửa file pronoun của từng chương.
- Cả hai hướng đều có thể kiểm offline trước (fixture tên giả + replay 158 điểm). Lần chạy trả phí tiếp theo, nếu có, nên đo prompt đã sửa trên 006 (có điểm tùy chọn, lỗi self `tớ`/`tôi` và lỗi `cậu` → `cô` của Tatsuoka), không phải prompt cũ trên 008.

Tài liệu đồng bộ trong commit review: BUILD_STATE và checklist bước 05 chưa ghi X3 ở `c57edd22`, nay đã ghi.
