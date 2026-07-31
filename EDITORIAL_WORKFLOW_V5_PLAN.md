# Kế hoạch tính năng Biên tập 3 lượt V5

## Mục tiêu

Thêm một chế độ **Biên tập** JP→VI cho từng chương. Đây là workflow điều phối có bằng chứng, không phải biến thể của nút Dịch hoặc tùy chọn Làm mượt.

Workflow đóng vai trò bất biến:

1. L1 audit RAW–DRAFT, chỉ xuất `REPORT_L1`.
2. L2 dựng hiểu biết/Scene Ledger từ RAW trước, sau đó mới dùng report và draft, xuất `VI_L2`.
3. L3 là context/run mới; hoàn thành RAW–VI_L2 độc lập trước khi mở `REPORT_L1`; xuất `FINAL_QA`.

RAW quyết định nội dung; Project Instruction quyết định chuẩn chất lượng; Workflow V5 quyết định thứ tự, input và gate.

## Quyết định UX: không bắt nạp lại bốn file cho mọi chapter

Đơn vị người dùng nhìn thấy là **Editorial Project** (series/volume) và **Chapter**.

```text
Editorial Project: Series A / Volume 03
  defaults đã chọn: Glossary, Pronoun, Workflow V5, nơi xuất, model policy
  ├─ Chapter 11: RAW + DRAFT + snapshot 4 input
  └─ Chapter 12: RAW + DRAFT + snapshot 4 input
```

Khi tạo project, người dùng chọn Glossary và Pronoun một lần từ Library hoặc từ file. Khi tạo chapter, app snapshot chính xác nội dung/hash của các default đó. Vì vậy chapter tiếp theo thường chỉ cần RAW và DRAFT.

Người dùng vẫn có thể override Glossary/Pronoun riêng cho một chapter; override đó được hiển thị rõ trước khi tạo snapshot.

## Nhập nhanh

### A. Tạo một chapter

Nút **+ Chapter** có ba cách nạp:

1. **Chọn RAW + DRAFT** (mặc định): kế thừa Glossary/Pronoun từ project.
2. **Chọn gói 4 file**: multi-select hoặc chọn thư mục/ZIP chứa RAW, DRAFT, Glossary, Pronoun.
3. **Dùng bản Dịch đã hoàn thành**: chọn một job Dịch trong app làm DRAFT; app tạo bản snapshot output, không tham chiếu output sống.

Sau khi chọn, luôn hiển thị màn hình xem trước mapping trước khi tạo chapter:

```text
Chapter gợi ý: Series A · Vol. 03 · Ch. 12             [Sửa]
RAW       ch12_raw_jp.txt                  ✓  48 KB
DRAFT     ch12_vi_draft.txt                ✓  51 KB
Glossary  Series A glossary (project)      ✓  snapshot mới
Pronoun   Series A pronoun (project)       ✓  snapshot mới

                         [Tạo chapter]
```

App không được tự chạy workflow hoặc tự xác nhận mapping chỉ từ tên file.

### B. Import hàng loạt cả volume

Nút **Nhập hàng loạt** nhận một folder, ZIP hoặc multi-select. App chia file thành các nhóm chapter, hiển thị bảng preview và chỉ tạo các chapter người dùng xác nhận.

```text
Ch. 11  RAW ✓  DRAFT ✓  kế thừa project defaults       READY
Ch. 12  RAW ✓  DRAFT ✓  kế thừa project defaults       READY
Ch. 13  RAW ✓  DRAFT —                              NEEDS FILE
```

Glossary/Pronoun chung của volume được chọn một lần và áp vào tất cả chapter được tạo. Nếu mỗi chapter có một profile riêng, hàng đó phải hiển thị explicit override thay vì tự suy đoán.

### C. Quy tắc nhận diện tên và metadata

Nhận diện chỉ là đề xuất có confidence, không phải quyết định ngầm:

- Bỏ các role marker quen thuộc để tìm common stem: `raw`, `jp`, `source`, `draft`, `vi`, `glossary`, `terms`, `pronoun`, `xungho`.
- Đọc pattern volume/chapter như `vol`, `v`, `volume`, `tap`, `ch`, `chapter`, `chuong` kèm số.
- Ưu tiên mapping theo common stem + role marker; chỉ dùng nội dung file để cảnh báo định dạng/encoding, không tự đổi role khi tên mơ hồ.
- Metadata gợi ý lấy từ tên folder và tên file; mọi field đều sửa được trước khi tạo.
- Mơ hồ, thiếu role, hai RAW/DRAFT cạnh tranh, hoặc glossary/pronoun không khớp schema => `NEEDS REVIEW`, không tạo chapter READY.

### D. Giảm thao tác về lâu dài

- Project template lưu provider/model policy, workflow pack, glossary/pronoun default, output folder và naming pattern.
- Nút **Thêm chapter kế tiếp** kế thừa template; người dùng chỉ thêm RAW+DRAFT.
- Từ màn Dịch, hành động **Gửi output sang Biên tập** tạo một chapter draft với output dịch làm DRAFT; người dùng chỉ bổ sung RAW hoặc chọn RAW cùng lúc.
- Một chapter chỉ snapshot input khi bấm `Tạo chapter`; các profile Library thay đổi sau đó không âm thầm thay đổi chapter.

## IA và UI

Giữ bốn điều hướng cấp một trên màn hình hẹp:

```text
Dịch | Biên tập | Thư viện | Cài đặt
```

Lịch sử job Dịch tiếp tục thuộc Dịch/Jobs. Danh sách project/chapter editorial nằm trong Biên tập để tránh lẫn `chunk job` với `chapter workflow`. Trên màn rộng có thể hiện Jobs như một panel phụ, không cần tab thứ năm.

### Màn hình 1: Danh sách Biên tập

- `+ Tạo project`, `+ Chapter`, `Nhập hàng loạt`.
- Current/Recent cards kế thừa Jobs: series-volume-chapter, state, gate đang chặn, cập nhật gần nhất, token/cost.
- Filter: `Đang chạy`, `Cần review`, `Sẵn sàng L1/L2/L3`, `Đã phát hành`, `Stale`.

### Màn hình 2: Chapter dashboard

- Card Input Snapshot: bốn role, profile/version, permission, checksum và nút thay input có cảnh báo invalidation.
- Timeline: `L1 Audit` → `L2 Biên tập` → `L3 QA độc lập` → `Phát hành`.
- Một nút chính duy nhất theo state: `Chạy L1`, `Chạy L2`, `Chạy L3 độc lập`, hoặc `Phát hành Final`.
- Card progress kế thừa dashboard Dịch nhưng đổi chunk thành scene/run/gate: phase, scene x/y, token, cost, elapsed, retry và gate open.
- Card evidence: REPORT_L1, VI_L2, FINAL_QA, manifest, diff, ledger/change registry.
- Activity Log giữ lại để thể hiện context barrier, state transition, retry và validator result.

### Màn hình 3: Run detail

- L1: scene ledger, issue registry, Severity, bốn gate audit, REPORT_L1.
- L2: status scene, `DRAFT ↔ VI_L2` diff, Global Change Register, bốn gate edit.
- L3: Change Set, Cross-scene Voice Audit, Final Read-through, Release Gate và `VI_L2 ↔ FINAL_QA` diff.
- MVP chỉ xem/copy/export; không có text editor nội bộ. Sửa trực tiếp một file output phải tạo version mới và invalid toàn bộ lượt phụ thuộc.

## Kế thừa hiện trạng kỹ thuật

Tái sử dụng:

- SAF picker, persisted permission và output-folder scan.
- Glossary/Pronoun Library và validation parser.
- provider/model catalog, cost estimate, retry policy, foreground service notification.
- UI helper `sectionCard`, buttons, status chip, progress dashboard, activity log và PageFactory.
- request/response hash, attempt timeline và cost metrics như nguyên liệu cho audit log.

Không tái sử dụng nguyên trạng:

- `TranslationJobState`: chỉ biểu đạt lifecycle kỹ thuật của một job dịch.
- `chunks`/`jobs`: không đủ role binding, scene, gate, output version và context barrier.
- `contextOverlapEnabled`/translation history: không được dùng để nối L2 sang L3.
- `refineAfter`: không tương đương L2 hoặc L3.

Tạo các thành phần mới: `EditorialPageFactory`, `EditorialRepository`, `EditorialStateMachine`, `EditorialContextBuilder`, `EditorialGateValidator`, `EditorialRunner` và `EditorialProjectStore`.

## Dữ liệu và state

```text
editorial_projects
editorial_chapters
editorial_assets        # role, URI, hash, snapshot content/version
editorial_runs          # L1/L2/L3, model/prompt/workflow hash, state
editorial_scenes        # Scene Ledger per run
editorial_issues        # L1 issue registry
editorial_changes       # L2 Global Change / L3 Change Set
editorial_gates         # gate evidence and validator state
editorial_requests      # request/response metadata, token/cost, retry
editorial_outputs       # output role, hash, path, source run
```

Chapter state:

```text
DRAFT_INPUT
→ L1_READY → L1_RUNNING → L1_CLOSED
→ L2_READY → L2_RUNNING → L2_CLOSED
→ L3_READY → L3_INDEPENDENT_RUNNING → L3_REPORT_REVIEW
→ RELEASE_READY → RELEASED
```

`STALE` là overlay state khi asset/profile/workflow snapshot thay đổi. Bất kỳ thay đổi RAW/DRAFT làm invalid L1–L3; thay glossary/pronoun làm invalid lượt chưa chạy và các output phụ thuộc. Không được overwrite output/evidence cũ.

## Context contract và validator

| Run | Allowed input | Bị cấm trước checkpoint |
|---|---|---|
| L1 | RAW, DRAFT, glossary, pronoun, workflow | bản dịch mới |
| L2 phase RAW | RAW, glossary, pronoun, workflow | REPORT_L1, DRAFT |
| L2 phase edit | RAW, DRAFT, glossary, pronoun, REPORT_L1 | — |
| L3 phase independent | RAW, VI_L2, glossary, pronoun, workflow | REPORT_L1, L2 ledger/history |
| L3 report review | các input L3 + REPORT_L1 | L2 hidden context/history |

Model phải trả JSON theo schema cho evidence; text output được render/export riêng. Validator kiểm file role/hash, schema, anchor, state order, output clean, duplicate/missing output, complete gates và release prerequisites. Validator không thể chứng minh tuyệt đối fidelity/voice; các kết luận ngôn ngữ phải giữ evidence và trạng thái review rõ ràng.

## Lộ trình

### P0 — Đặc tả và test fixture

- **Completed (2026-07-31):** versioned evidence contracts for L1/L2/L3; strict L2/L3 context allow-lists; chapter state machine; release-gate predicate; L1/L2/L3 structural validators; Canonical RAW Map with deterministic long-chapter anchors; asset role/checksum freshness checks; and fixtures for role ambiguity, voice, output truncation, stale asset, and long RAW anchors.
- P0 exit evidence: `EditorialWorkflowV5Test` passes 9/9; archive-first `4.16-dev.3`/code65 build passes 119 JVM tests and lint with 53 warnings/0 errors.

### P1 — Project/import và persistence

- Hoàn tất migration SQLite v10→v11 và repository cho Editorial Project, Chapter, asset snapshot, run, scene, gate và evidence; migration chỉ thêm bảng/index, không sửa dữ liệu Dịch hiện có.
- Hoàn tất tab Biên tập độc lập: tạo project, chọn nhiều TXT một lần, tự ghép RAW–DRAFT theo tên file, nhận GLOSSARY/PRONOUN chung hoặc profile đang chọn, preview/gate các cặp thiếu-mơ-hồ và chỉ lưu asset snapshot sau khi xác nhận.
- `Dùng output Dịch làm DRAFT`.
- Hoàn tất test mapping filename và ambiguity; invalidation/stale UI sẽ đi cùng runner.

### P2 — L1

- Hoàn tất context builder/runner/checkpoint L1 và segmented L1 cho chương dài có ngắt cảnh cấu trúc tương ứng: tạo RAW Map/Chapter Ledger trước DRAFT, ghép scene bằng marker có evidence, gọi model tuần tự, checkpoint từng scene, tổng hợp REPORT_L1 sau validator. Mapping mơ hồ/lệch hoặc một scene vẫn quá context bị chặn và giữ evidence; không cắt hay ghép theo tỷ lệ.
- Schema validator và Report detail/export.
- Recovery/retry theo scene hoặc phase an toàn.

### P3 — L2

- RAW-first barrier, VI_L2, diff và Global Change Register.
- Gate L2 và stale/retry behavior.

### P4 — L3/release

- Context-isolated L3, delayed REPORT_L1 unlock.
- Change Set, voice audit, final read-through, Release Gate, evidence bundle redacted.

### P5 — regression/QA

- Regression Dịch: import, batch, glossary/pronoun, resume/retry, dashboard và output không đổi hành vi.
- Android lifecycle/process-death, permissions, large input, exact state recovery.
- Paid API tests chỉ opt-in; offline fake-provider tests là bắt buộc.

## Rủi ro và quyết định giữ lại

- Không tự tin từ tên file không được biến thành mapping âm thầm: luôn preview/confirm.
- Không đưa API key vào settings snapshot export/evidence.
- Không tạo text editor ở MVP; mọi external edit phải version hóa.
- Không chạy L1/L2/L3 song song cùng một chapter.
- Một foreground execution toàn app là giới hạn chấp nhận được cho MVP; chạy song song chỉ xem xét sau khi có scheduler/queue và cost guard.
- Chapter dài không được nhét toàn bộ vào một request; scene segmentation phải giữ raw anchor và có chapter-level ledger summary.

## Tiêu chí MVP đạt

- Project defaults giúp tạo chapter chỉ với RAW+DRAFT trong trường hợp thông thường.
- Batch import preview chính xác các mapping; mọi mapping mơ hồ bị chặn để review.
- Mỗi run có input manifest, checksum, model/prompt/workflow hash, token/cost và output hash.
- L2 không chạy khi REPORT_L1 chưa hợp lệ; L3 không nhận REPORT_L1 trước checkpoint độc lập.
- FINAL_QA không export/release khi bất kỳ release gate nào open.
- Luồng Dịch hiện tại không đổi hành vi và vẫn qua regression riêng.
