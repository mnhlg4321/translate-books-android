# P6 R5 — bảng execution, ngân sách worst-case và câu hỏi Q1–Q5 (P4 repricing 2026-10-03)

Trạng thái: Q1–Q5 đã được owner duyệt ngày 2026-10-02. P4 đã kiểm tra lại giá chính thức và fixture/leak guards; **chưa có provider call nào**. R0–R4 và offline P0–P3 evidence ở `EDITORIAL_RECOVERY_V4_18.md` mục 10, `HANDOFF.md`, và `docs/P6_R6_R7_OFFLINE_EXECUTION_20261003.md`.

## 1. Cách tính

- **Giá P4 đã xác minh**: OpenRouter hiện niêm yết Standard cho model `openai/gpt-5.6-luna` với USD **0.20 / 1M token input không cache**, **0.25 / 1M cache-write**, **0.02 / 1M cache-read**, và **1.20 / 1M token output** (reasoning tính trong token ra). Tra cứu trang giá chính thức lúc **2026-10-03 03:29:53 UTC** ([OpenRouter — GPT-5.6 Luna pricing](https://openrouter.ai/openai/gpt-5.6-luna)); route production ghim upstream `openai`, không fallback. OpenRouter ghi OpenAI tự cache prompt từ 1,024 token và tính cache-write cho prefix mới; request hiện tại không đặt `prompt_cache_options.mode=explicit`, nên budget worst-case tiếp tục dùng mức cache-write cao hơn **USD 0.25/M** cho input, không trừ cache-read. Đây cũng khớp với giá reservation bảo thủ trong mã.
- **Trần vào**: 200,000 byte/call, cổng của engine đổi sang token bằng `ceil(byte/2)` ⇒ ≤ 100,000 token ⇒ USD 0.0250 theo worst-case cache-write. Mức thực tế đo được: 21k–27k token vào/call cho chương 001.
- **Trần ra** theo từng call như dưới; **thời gian** 180 s/call; **0 retry tự động, 0 repair call**; một call trạng thái UNKNOWN **không chạy lại** (chỉ owner quyết).
- Worst-case/call = 100,000 × 0.25/1M + trần ra × 1.20/1M. Nó luôn nhỏ hơn trần USD của chính call đó (cột cuối), nên trần USD không bao giờ là thứ bị chạm trước trần token.

| Call | Phase | Trần ra (token) | Worst-case USD | Trần USD/call |
|---|---|---:|---:|---:|
| 1 | L1_RAW_DISCOVERY (RAW+GLOSSARY) | 8,192 | 0.0348 | 0.05 |
| 2 | L1_RECONCILE (+ DRAFT, PRONOUN, khối candidate của app) | 16,384 | 0.0447 | 0.10 |
| 3 | L2_RAW_DISCOVERY (mù, phủ theo inventory) | 8,192 | 0.0348 | 0.05 |
| 4 | L2_EDIT (findingResolutions + op) | 16,384 | 0.0447 | 0.10 |
| 5 | L2_FINAL_READ (đọc đúng byte VI_L2) | 4,096 | 0.0299 | 0.03 |
| 6 | L3_RAW_FIRST_REAUDIT (mù) | 8,192 | 0.0348 | 0.05 |
| 7 | L3_RECONCILE (probe có anchor, defect còn lại từ L2) | 16,384 | 0.0447 | 0.10 |
| 8 | L3_FINAL_READ (đọc đúng byte FINAL) | 4,096 | 0.0299 | 0.03 |

Một chuỗi đầy đủ ledger-v2 = **8 call**, worst-case theo giá P4 **USD 0.298304**: hai call L1 = 0.0794912, ba call L2 = 0.1094064, ba call L3 = 0.1094064. App `EditorialP6GroupSpendLedger` dùng giá input USD 0.25 trong reservation, đúng với mức cache-write worst-case; không đổi quyền chi tiêu hay cấu hình provider. Trần trong app: L1 có hai phép cấp riêng (0.05 + 0.10), phần L2+L3 có trần chuỗi `ledgerRecommended()` = USD 0.36 (0.05+0.10+0.05+0.10 + hai lần đọc 0.03). Trần nhóm vẫn được cộng dồn và chặn trước dispatch.

## 2. Bảng execution (mục 4 của yêu cầu, điền số thật)

Quy ước: "L1-only" = RAW + RECONCILE trên fixture; "L2-only" / "L3-only" = harness **dựng predecessor nhất quán bằng chính mã production với provider giả** (không gọi provider cho phần dựng, không sửa DB pilot, không đi tắt qua validator); "chuỗi" = 8 call. Fixture là bản chương 001/003/005 trong `D:\P5E-private\p6-fixtures` (không lên Git). Chấm bằng `scripts/p6/score_run.py` với ngưỡng đã đóng băng (`docs/P6_R5_EVALUATION_PROTOCOL.md`).

| Nhóm | Fixture và cách chạy | Call | Worst-case | Trần xin | Đo gì |
|---|---|---:|---:|---:|---|
| **G1** L1-only | `fx-a03` (từ chưa dịch), `fx-a04` (đảo vai), `fx-a05` (thiếu câu), `fx-a07` (số), `fx-a08` (phủ định), `fx-a11` (6 lỗi > 4 finding), `fx-a02` (đối chứng sạch), `fx-a12` (mơ hồ) × 1 lượt = 16 call; thêm 2 lượt cho `fx-a11` và `fx-a04` = 8 call | 24 | 0.954 | **≤ 1.00** | ledger có đủ finding (không trần 4), anchor đúng dòng lỗi gieo, đối chứng không có finding sai, lưu bền |
| **G2** L2-only, L3-only, đối chứng | L2-only: `fx-a04`, `fx-a11`, `fx-a02` (predecessor REPORT_L1 phải được tạo qua P4/L1 production execution từ bốn source fixture, với fake provider; chỉ có anchor và quan sát trung tính, không dùng nhãn/đáp án) = 3 chuỗi × 3 call = 9; L3-only: `fx-a03`, `fx-a05`, `fx-a08`, `fx-a02`, `fx-a12` (VI_L2 dựng qua L1/L2 production execution bằng fake provider, lỗi fixture còn nguyên, receipt L2 hợp lệ) = 5 × 3 = 15 | 24 | 0.875 | **≤ 1.00** | L2 xử lý đủ finding và mọi occurrence; L3 tự tìm và sửa lỗi gieo trong VI_L2; đối chứng không bị sửa |
| **G3** holdout | `fx-h01` (chương 003), `fx-h02` (chương 005): chuỗi 8 call mỗi chương, **chạy một lần**, không chỉnh prompt sau đó | 16 | 0.597 | **≤ 0.75** | khái quát hóa trên chương chưa từng nhìn, lỗi gieo theo luật |
| **G4** chương 001 | `fx-a01`: chuỗi 8 call trên binding/run declaration mới (Q2); inventory là **191 unit** vì marker ảnh `[IMAGE: …]` bị loại theo `EditorialRawInventory` | 8 | 0.298 | **≤ 0.50** | `今回` còn, `踏破` đủ 4 chỗ, `嬢ちゃん` đúng hồ sơ; lưu bền, mở lại, xuất TXT |
| **Tổng R6** | | **72** | **2.724** | **≤ USD 3.25** | dự phòng 0.53 chỉ dùng khi owner đồng ý riêng |

Ngoài bảng (R7, xin riêng): hai chương còn lại, chuỗi 8 call mỗi chương = 16 call, worst-case USD 0.597 theo giá P4/cache-write worst-case, trần xin ≤ USD 0.75 (Q4).

Không nằm trong R6 và nói rõ để khỏi hiểu nhầm là đã phủ: các lớp lỗi `GLOSSARY_TERM` (`fx-a09`), `ADDRESS_PROFILE` gieo (`fx-a10`) và `EXTRA_SENTENCE` (`fx-a06`) có nhãn và bộ chấm nhưng không có call trong 72 call trên (hồ sơ xưng hô được phủ bởi `嬢ちゃん` ở `fx-a01`; muốn thêm ba fixture này cần khoảng 6 lượt fixture L1-only / 12 provider call, worst-case USD 0.477, vượt trần G1 khi cộng với G1 hiện tại).

Điều kiện chạy chung (mọi nhóm): trước nhóm đầu có một lượt kiểm **không gọi provider** (fixture, prompt, validator, bộ chấm) bằng APK đã archive trên emulator; key do owner tự cấp ở bề mặt chạy (app/emulator hoặc biến môi trường của owner) — Claude không đọc, in, ghi hay chuyển key; mỗi call có log redacted (token, USD, finish reason); dừng toàn nhóm ngay khi một call ra trạng thái UNKNOWN hoặc vượt trần; dừng nhóm tiếp theo nếu nhóm trước chạm trần USD. Báo cáo cuối luôn tách `STRUCTURAL_VALID` và `SEMANTIC_EVAL`.

## 3. Trước khi gọi bất kỳ call nào (R6.0, offline, không cần quyền)

1. Runner chạy fixture dùng đúng mã production (engine + provider + coordinator), ghi `final.txt` / `structural.json` đúng hợp đồng chạy. Bề mặt chạy chọn bằng một spike không gọi provider (provider adapter chạy được trong JVM hay phải instrumented).
2. Build bằng `scripts/build-and-save.ps1` + test build, archive hai nơi, chạy lại regression (engine, app unit, instrumented) trên emulator.
3. Test rò đáp án và hash fixture/nhãn được kiểm lại ngay trước lần chạy đầu.

## 4. Câu hỏi cho owner (hỏi một lần)

**Q1 — Bảng ngân sách §2.** Duyệt đúng bảng này (72 call, G1 ≤ 1.00, G2 ≤ 1.00, G3 ≤ 0.75, G4 ≤ 0.50, tổng R6 ≤ USD 3.25, 0 retry, UNKNOWN không chạy lại)? Có thể duyệt từng nhóm, theo thứ tự G1 → G2 → G4 → G3 (khuyến nghị: holdout chạy cuối vì chỉ chạy một lần). Cần nói rõ: model giữ nguyên `openai/gpt-5.6-luna` qua route đã khóa, reasoning `minimal`.

**Q2 — Chạy lại chương 001.** Khuyến nghị: tạo **binding/run declaration mới ngay trên project pilot** (giữ nguyên chuỗi cũ `7a5e3428→7483b211→e1a4c638→867a23f8` làm lịch sử `LEGACY_CONTRACT_V1`, lineage hiện tại chặn tái dùng `ALREADY_USED`). Lựa chọn khác: project mới (sạch hơn, nhưng mất liên hệ với bản FINAL đã chấp nhận).

**Q3 — Cài bản R6 lên pilot `15e84958`.** Khuyến nghị: có, **sau** khi emulator đạt regression, và **sau khi sao lưu DB pilot** (tôi sẽ lưu bản sao trước khi cài). Pilot đang chạy `4.18-p6.2/code215`; bản R6 sẽ có schema DB v25 giữ nguyên, thêm mã ledger; cài bằng `install-validated.ps1` với chữ ký đã ghim.

**Q4 — Hai chương còn lại cho R7** (từ `docs/P6_G6_CHAPTER_CANDIDATES_20261002.md`). Gợi ý: **007** (thoại dày, 8 hàng xưng hô, glossary lớn) và **010** (DRAFT dài nhất, đẩy giới hạn L2/L3); thay thế: 009 nếu ưu tiên xưng hô, 014 nếu ưu tiên RAW dài, 013 nếu cần chương ngắn.

**Q5 — Ba ô `UNCERTAIN` của R0.** Mặc định đang dùng (không chấm điểm): U1 `特権階級` bỏ chữ 階級 là tùy chọn/không bắt lỗi; U2 một đoạn RAW tách thành hai đoạn DRAFT không phải lỗi nhưng ánh xạ phải được giữ; U3 `踏破` chấp nhận bất kỳ cụm tiếng Việt nào giữ được hai thành tựu khác nhau (cụm trong bản FINAL của owner chỉ là một ví dụ). Cần owner xác nhận hoặc đổi từng ô; đổi sau khi chạy sẽ làm mất giá trị bảng chấm.

## 5. Giới hạn đã biết khi vào R6

- Chương dài hơn 600 unit dừng có kiểu (`L1_UNIT_LIMIT_EXCEEDED`), chưa có chunking; chương 001 có **191 unit** (không tính dòng marker ảnh `[IMAGE: …]` theo `EditorialRawInventory`), các chương ứng viên đều dưới ngưỡng này.
- Hợp đồng cho phép tối đa 1 vòng sửa sau final-read; bản v2.0 dùng 0 vòng: final-read còn defect thì dừng `CONTENT_L3_FINAL_READ_DEFECTS`, không phát hành FINAL.
- 22 test instrumented lịch sử (schema v24, seed lịch sử) fail từ trước, ngoài phạm vi R; sẽ ghi vào checklist, không che.
- Chất lượng nghĩa chỉ được kết luận sau G1–G4 bằng bộ chấm đã đóng băng; mọi số "đạt" trước đó chỉ là cấu trúc.
