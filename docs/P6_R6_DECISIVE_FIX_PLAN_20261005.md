# Kế hoạch sửa dứt điểm vòng "live → một lỗi hình thức mới → vá → live" (P6 R6, v4.18)

Ngày lập: 2026-10-05. Người lập: Claude (quản lý dự án). Yêu cầu owner: "Lại bắt đầu làm việc mãi mà không có kết quả, hãy sửa dứt điểm."
Baseline: branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD `349717d8` (đồng bộ origin). Kiểm `git archive` sạch: engine 405/405, app 341/341.

## 1. Chẩn đoán vì sao chưa có kết quả

| # | Lượt live | Phase dừng | Mã | Bản chất |
|---|---|---|---|---|
| 1 | G1#1 | L1 RAW | `REPAIR_L1_LEDGER_INVALID` (mất chi tiết) | thiếu chẩn đoán |
| 2 | G1#2 | L1 RAW | `L1_UNIT_UNKNOWN` | model chép hash id |
| 3 | V3 | RECONCILE | `L1_TEXT_REQUIRED` | prompt bảo "để rỗng", parser bắt buộc |
| 4 | S5 | L1 RAW | `L1_UNIT_UNKNOWN:coverage.0.from` | schema minLength 3 do app |
| 5 | T3 | RECONCILE | `L1_RAW_QUOTE_NOT_IN_ANCHOR` | furigana — khoảng trống app |
| 6 | U6 | RECONCILE | `L1_DRAFT_QUOTE_NOT_IN_ANCHOR` | model chép số dòng RAW vào neo DRAFT |
| 7 | W4 | RECONCILE | `L1_DRAFT_ANCHOR_UNUSED_FIELD:findings.0.draft.after` | model điền `after` vào trường không dùng của `LINES` (cả 3 finding) |

Tiến bộ thật có: RAW qua 4 lần liên tiếp; mỗi lỗi đã gỡ không tái phát; chi phí thấp (G1 11 call, USD 0.0936 / 1.00). Nhưng **7/7 lần dừng là lỗi hình thức/ghi sổ, 0 lần là lỗi nội dung**. Nguyên nhân gốc chung:

1. **Validator phân xử mọi thứ bằng "từ chối cả response"**, kể cả các trường app tự suy ra được hoặc không ai dùng (`draft.start/end/after` đã chỉ là gợi ý từ W1; `speakerRecords` không được dùng).
2. **Mỗi lượt live chỉ lộ được lỗi đầu tiên của một response**, nên sửa từng cái một. L1 có ~63 mã từ chối; L2/L3/receipt ~205 mã và **chưa chạy live lần nào** — cùng họ lỗi sẽ lặp lại ở đó.
3. **Quy tắc dừng cả nhóm khi một fixture bị từ chối** biến mỗi lỗi hình thức thành "không có kết quả gì".

## 2. Hướng sửa dứt điểm (3 thay đổi cùng lúc, không vá từng mã)

### Z1 — Phân loại toàn bộ luật từ chối một lần (L1 + L2 + L3 + final-read + receipt)

Lập bảng **mọi** mã từ chối với một trong hai nhãn:

- **SEMANTIC (giữ từ chối):** nội dung model khẳng định mà app không kiểm được hoặc kiểm thấy sai — trích dẫn không có trong nguồn, finding không neo RAW, coverage hở/chồng, tham chiếu candidate/finding không tồn tại, id thực thể trùng, dialogue change thiếu speaker proof (đã có cơ chế hoàn nguyên), output bị cắt.
- **BOOKKEEPING (chuẩn hóa, không từ chối):** giá trị app tự suy ra hoặc không dùng — trường không dùng trong union (`after` của `LINES`, `start/end` của `MISSING`), số dòng gợi ý lệch/ngoài phạm vi (kẹp, đã làm một phần), tham chiếu trùng (đã làm), bản ghi phụ có tham chiếu sai (`speakerRecords` đã làm; xét `protectedSpans`), khoảng trắng/NFC, thứ tự phần tử, trường MAY rỗng.

Quy tắc phân loại: *nếu bỏ/sửa giá trị đó không thay đổi bất kỳ khẳng định nội dung nào đã được kiểm chứng bằng nguồn, thì là BOOKKEEPING.* Mọi chuẩn hóa ghi vào `normalizations` kèm đường dẫn; không bịa, không đoán nội dung.

Đầu ra: `docs/P6_R6_VALIDATION_RULE_CLASSIFICATION.md` (bảng mã → phase → path → nhãn → hành động) + mã sửa cho **toàn bộ** hàng BOOKKEEPING trong một lần.

### Z2 — Kiểm thử đột biến từ response thật (offline, thay vì chờ live lộ lỗi)

Từ các response thật đã lưu (RAW ×4, RECONCILE ×4) và wire tổng hợp cho L2/L3, sinh biến thể theo các lệch đã thấy và sẽ thấy: điền trường không dùng, số dòng lệch ±1/±2/ngoài phạm vi, chép số dòng RAW cho DRAFT, trường MAY rỗng/bỏ, tham chiếu trùng, bản ghi phụ trỏ dòng trống, khoảng trắng/NFC/furigana, thứ tự đảo. Kỳ vọng mỗi biến thể: **PASS kèm normalization** hoặc **từ chối SEMANTIC** — không bao giờ từ chối BOOKKEEPING. Chạy cho cả 8 phase bằng parser production. L2/L3 được "cứng hóa" trước khi chạy live lần đầu.

Thêm: L2/L3 hiện chỉ dùng `json_object`; sinh **strict json_schema** từ `EditorialFieldSpec` cho các phase này (đã có golden-wire test chống lỗi kiểu minLength), để decoder chặn lệch ngay khi sinh.

### Z3 — Đổi quy tắc chạy G1: từ chối SEMANTIC là một kết quả đo, không phải lý do dừng nhóm

- Fixture bị từ chối **SEMANTIC** → ghi `STRUCTURAL_VALID=false`, `SEMANTIC_EVAL=FAIL(reason)`, **chạy tiếp fixture kế**.
- Fixture bị từ chối **BOOKKEEPING** (Z1 sót) → ghi nhận, chạy tiếp fixture kế; sau nhóm sửa offline bằng replay + Z2.
- **Dừng nhóm** chỉ khi: UNKNOWN, chạm trần nhóm, lỗi hạ tầng (mạng/route/fingerprint), hoặc **3 fixture liên tiếp** bị từ chối cùng một mã (dấu hiệu lỗi hệ thống).
- Kết quả: một lượt G1 luôn cho ra **bảng 8 fixture** — đó là "kết quả" mà các lượt trước chưa có.

## 3. Gói việc cho Codex (một lượt, theo thứ tự)

| Gói | Việc | PASS | Dừng |
|---|---|---|---|
| **Z1** | Bảng phân loại toàn bộ mã; hiện thực mọi hàng BOOKKEEPING (kể cả `draft.after/start/end` không dùng — lỗi W4); tăng `contractRevision` | Bảng đủ 100% mã của các parser; JVM xanh | — |
| **Z2** | Bộ đột biến từ 8 response thật + wire L2/L3 tổng hợp; strict schema cho L2/L3 từ FieldSpec | 0 biến thể bị từ chối vì BOOKKEEPING; replay-all cả 4 RECONCILE thật: lỗi W4 PASS, các response cũ hoặc PASS hoặc chỉ còn lỗi SEMANTIC đã biết | Phát hiện mã mới không phân loại được → thêm vào bảng, không bỏ qua |
| **Z3** | Sửa `scripts/p6/run_group.ps1` + runner theo quy tắc chạy mới; test quy tắc dừng (UNKNOWN, trần, 3 lỗi cùng mã, hạ tầng) | Test xanh | — |
| **Z4** | Build wrapper, archive, emulator: preflight, fake CHAIN 14/14, negative gate | Đạt, 0 call | — |
| **Z5** live | **G1 đủ 8 fixture + 2 lượt lặp** theo bảng R5 trong phần trần còn lại (USD 0.9064) với quy tắc Z3; chấm `score_run.py` | **Bảng G1 hoàn chỉnh**: theo fixture `STRUCTURAL_VALID`, `SEMANTIC_EVAL`, finding đúng/sai so với lỗi gieo, call/token/USD | Dừng theo Z3 |

**Cổng quyết định sau Z5 (để không lặp vô hạn):** nếu ≥ 6/8 fixture qua cấu trúc L1 → sang G2 (L2/L3). Nếu < 6/8 và lỗi chủ yếu ở hình thức RECONCILE → đổi kiến trúc RECONCILE sang đầu ra văn bản có thẻ như luồng dịch (đánh giá A/B trên cùng fixture, ngân sách riêng) thay vì tiếp tục vá JSON.

## 4. Chi phí

Z1–Z4: USD 0. Z5: tối đa phần còn lại của G1 (USD 0.9064, đã duyệt); ước tính thực ~USD 0.25 cho 24 call (~USD 0.01/call đo được).

## 5. Quyết định cần owner

**D-Z3:** thay quy tắc dừng của G1 bằng Z3 (từ chối là kết quả đo; chỉ dừng nhóm khi UNKNOWN/trần/hạ tầng/3 lỗi cùng mã liên tiếp). Khuyến nghị **đồng ý** — đây là thay đổi then chốt để một lượt live cho ra kết quả thay vì dừng ở lỗi đầu tiên. Z1–Z4 offline, không cần quyền thêm.

## 6. Quyết định owner

**D-Z3 đã được owner duyệt (2026-10-05, "đồng ý"):** quy tắc chạy G1 đổi theo Z3 — fixture bị từ chối (SEMANTIC hoặc BOOKKEEPING sót) được ghi là kết quả đo và chạy tiếp fixture kế; chỉ dừng nhóm khi UNKNOWN, chạm trần nhóm (phần còn lại USD 0.9064), lỗi hạ tầng (mạng/route/fingerprint), hoặc 3 fixture liên tiếp bị từ chối cùng một mã. Z1–Z4 làm offline trước; Z5 chạy đủ G1 (8 fixture + 2 lượt lặp) rồi dừng trước G2. 0 retry tự động, 0 repair call, không đụng pilot.


## 7. Independent Z5 review and next work package — 2026-10-05

This checkpoint supersedes the pending D-Z3 wording above. D-Z3 was approved and Z1–Z5 ran. G1 meets the structural progression gate; G2 still needs its separate owner decision. Do not rerun G1 merely to obtain better detection numbers.

### Verified baseline and corrections

- Reviewed source/report baseline: `006434da827563aa18331971f3a5c57e55e8264a`, matching the remote branch at review. Clean git-archive JVM tests: engine 422/422, app 342/342. APK remains p6.24/code237 from `3b570135`; this review neither built nor contacted a device. Historical installation claims are not a fresh installed-byte verification.
- The verifier change in `006434da` had a confirmed temporal oracle-probe bug: a leaked corrected term in RECONCILE input could be excused by that same call's response, a future response, or the final L1 report. Three negative regressions fail before the fix. The fix allows only earlier captured responses or an already available L1 predecessor, with positive tests for legitimate repetition. Python suite: 54/54. This is a bounded heuristic, not proof of absence of every possible answer leak or compliance with phase visibility.
- Read-only recheck of all 12 retained G1 outputs passes the repaired chronological probe. No actual leak is established in those runs. Original evidence is unchanged.
- Last ledger hash chain verifies: 82 entries, 41 settled calls, USD 0.28859445/1.00, remaining 0.71140555, pending 0. Successful 12 runs cost 0.15146940, not the result document's former 0.1502. Including the first failed attempt (0.04354415), session cost is 0.19501355. This review made zero calls and spent zero.
- The 3/7 single-target and 2/2/3-of-6 figures are provisional proximity matches, not adjudicated detection recall. L1-only fixed-text scorer FAIL is not a repair-quality verdict; repair evaluation is NOT_MEASURED. L2/L3 were NOT_REACHED in G1. Their new schema revisions lack live evidence; historical L2/L3 did run on October 2.
- Quote-based anchor derivation (`EditorialL1Ledger.java`, deriveRawUnits/deriveDraftAnchor) proves a quote occurs in the selected text, not that the observation diagnoses that occurrence correctly. Dropped speaker/protection metadata and anchor changes need semantic review, not automatic classification as harmless bookkeeping.
- Three accepted FINAL chapters and P7 remain unproven. Structural progress does not change the existing product acceptance.

### Next package: offline adjudication and a concrete G2 proposal

Purpose: finish interpreting existing evidence and freeze meaningful G2 measurements. No provider, emulator/pilot operation, new APK, prompt tuning, fixture relabeling, or architecture migration is part of this package.

1. **Evidence owner — coordinator.** Pin source, fixture/label hashes, prompt/schema revision, model and route from actual run metadata. Keep base and both repeats separate. Reconcile canonical plan, build state and snapshot; retain historical evidence without treating old next actions as current instructions.
2. **Detection review owner — one reviewer.** Review every seeded target and every L1 finding across the 12 outputs against RAW, DRAFT and applicable glossary/pronoun scope. The manual FINAL is independent reference only. For each target record detected/missed/uncertain, matching finding IDs, correct semantic explanation, RAW/DRAFT anchors before and after normalization, and a short rationale. Location alone is insufficient. For each unmatched finding classify confirmed additional defect, false positive, preference, or unresolved; do not assume the nominal clean control is infallible. Store book excerpts and detailed judgments privately, with safe aggregate results in docs.
3. **Normalizer review owner — independent bounded review.** Inspect each applied normalization in these outputs: anchor reassignment, speaker record drops, protected-span changes, list truncation and defaults. Distinguish observed changes from unexercised risks. Trace wire → parser → committed report → downstream input. Add synthetic end-to-end regression only for a demonstrated defect, including a negative example where a real quote describes a different alleged error. Do not weaken semantic checks to make old responses PASS. Major contract changes require a separate concrete proposal.
4. **Measurement owner — coordinator.** Specify detection scoring separately from repair scoring. Count unique targets once per run; separate extra real findings from false positives, with uncertainty explicit and denominators visible. Keep current frozen scorer outputs unchanged and append an independent detection assessment. Define L2 inherited-finding resolution versus independently discovered correction, and L3 residual-defect detection/correction versus new regressions. Freeze new rules before any new live run; retrospective G1 adjudication must be labeled retrospective.
5. **G2 proposal owner — coordinator.** Use the existing budget table and fixture set. Select and hash the exact retained L1 predecessor per L2 fixture before dispatch: base fx-a04 has no finding, while repeat 1 has one; do not silently select the successful repeat or present an empty ledger as evidence of following L1 findings. State which run and why; retain the unsuccessful runs in reported results. Confirm what each blind phase may see. L3 synthetic predecessors measure isolation, not an end-to-end success. List model/prompt/schema/source, fixtures, call maximum, USD cap, ledger, stop rules, zero automatic retry/repair, and evidence to retain. Existing proposal is 24 calls, <= USD 1.00 in its own G2 budget; remaining G1 funds do not authorize G2. Explain why real model behavior cannot be settled offline.
6. **Independent challenge and handoff.** Check that a legitimate paraphrase can count as detection, a nearby unrelated observation cannot, and correct abstention on ambiguity is not punished. Consider whether L2 independent discovery offsets weak L1; do not require perfect L1 before permitting useful G2 diagnostics. Compare with the translation flow only where a measured failure suggests an architecture change. Full-text output is an A/B hypothesis, not a proven fix for omissions or false positives.

Ownership: use Sonnet for bounded independent review only if actually available; otherwise report the limitation and self-review. One writer per file. Preserve unrelated working changes. Relevant files are this plan, `scripts/p6/verify_fixture_run.py` and its tests for a demonstrated verifier defect, and the existing canonical plan/build state/snapshot. Do not expand into release, pilot or unrelated UI work.

Required deliverables: private target/finding adjudication table; safe claim→evidence→verdict report with exact code references; normalization impact table; frozen G2 measurement definition and concrete budget proposal. Use the existing package rather than creating another release plan.

PASS: all 12 outputs accounted for; all seeded targets and all unmatched findings judged or explicitly unresolved; detection and repair metrics separated; any code fix has a failing-before/passing-after regression on clean committed source; G2 inputs, caps and stop rules are reviewable; no private text/credentials committed. FAIL/FAILED_REPAIRING: unsupported detection claims, known verifier failures or altered acceptance. Missing indispensable private evidence is NOT_MEASURED with the exact missing item, not a fabricated verdict. Completion does not require perfect model recall.

Stop after delivering the concrete G2 proposal for the owner's separate decision. No new live permission is created by this document. The sole next action is to complete this offline package.

### 7.1 Offline package completed — 2026-10-05

The required retrospective review is complete without provider/device work. The safe aggregate adjudication is in `docs/P6_R6_G1_SEMANTIC_ADJUDICATION_20261005.md`; private response bodies and source excerpts remain under `D:\P5E-private` and were not staged. All 12 outputs are accounted for. The review records 25 seeded-target opportunities, 10 semantic detections and 15 misses; this is detection only, while L1-only repair remains `NOT_MEASURED`. It classifies all 17 finding rows: 2 confirmed additional defects, 3 false positives, 1 preference and 1 unresolved relation finding.

Normalization review found 17 duplicate references removed, 13 invalid speaker side records dropped, zero DRAFT anchor derivations, zero anchor deviation, no protected-span adjustment, no list truncation and no exercised optional-field default. Furigana/NFC/trim matching was observed on the untranslated target and did not re-anchor or create proof. No response was altered or made valid, and acceptance was unchanged.

The concrete owner-decision package is `docs/P6_R6_G2_PROPOSAL_20261005.md`: 3 L2 fixtures using exact retained predecessors (including an explicitly justified fx-a04 repeat-1 selection) plus 5 L3 isolation fixtures, 24 calls, a separate USD 1.00 cap, detection/repair scoring, and typed stop rules. G1's remaining balance is not authorization for G2. The only next action is owner decision on that proposal; no live dispatch follows this package.
