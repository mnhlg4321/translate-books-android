# Editorial Pronoun Recovery v4.19 — lỗi sót xưng hô của luồng chunk

Mở 2026-10-12 theo yêu cầu owner. Đây là canonical plan duy nhất của vòng v4.19; một branch (`feature/v4.19`), một checklist (`release_checklists/v4.19.md`). Các lượt làm việc sau resume chúng, không tạo branch/plan/checklist khác trừ khi owner đổi phạm vi. Nguồn thật của các số đo là `docs/EDITORIAL_CHUNK_PLAN.md` mục 6; file này chỉ điều hành vòng mới.

## 1. Mục tiêu

Giảm các lỗi xưng hô mà luồng Biên tập theo chunk (v4.18.0) còn **bỏ sót**, mà không làm tăng sửa sai và không sửa văn phong tùy tiện. Ưu tiên của owner: bắt vi phạm hàng pronoun (self/call/scope) trước, thuật ngữ glossary sau. FINAL của owner là tham chiếu, không phải luật; nhãn phân xử theo RAW + luật (ĐÚNG / SÓT / SAI / CHƯA CHẮC) và owner đọc là cổng quyết định.

## 2. Điểm xuất phát (đã xác minh)

| Hạng mục | Giá trị |
|---|---|
| Branch/baseline | `feature/v4.19` tách một lần từ `main` `998da341f4794987c6c309eb0fd1fbb3950348e6` (merge D-R3) trong worktree sạch `D:\P5E-builds\wt-v4.19`; workspace `D:\App Translate Books` giữ 163 thay đổi dở của owner, không đụng |
| Release trước | `v4.18.0` (tag `9d02976e`), APK `4.18.0`/code 266 dựng từ `82626ac8`; logic đang chạy là CP-IMPL-15 (`12824cee`) |
| Chương đã được owner chấp nhận | 006 (CP-IMPL-9), 008 (CP-IMPL-11), 011 (CP-IMPL-14) kèm giới hạn đã ghi; không đổi nhãn, không đổi ngưỡng |
| Lỗi sót còn mở (nguồn: `RELEASE_NOTES.md` v4.18.0 và `docs/EDITORIAL_CHUNK_PLAN.md` mục 6) | 006: 13 SÓT xưng hô; 008: 7 SÓT `cậu` (đúng ra là tên `Asanuma`); 011: 2:2 (sửa đúng bị khóa ngôi ba chặn nhầm), 12:2 (CHƯA CHẮC), 10:6 (sót do phương sai); 007 đoạn 5: rule đã gửi nhưng model sửa sai 2/2 ở CP-IMPL-3 (lịch sử); giới hạn finder: hàng chỉ có `self` hoặc chỉ có `call` không sinh điểm |
| Đo gần nhất theo tầng | độ phủ finder so với dòng owner sửa: 69% trên 8 chương (94% ở 006/011/014/017); chưa tách riêng nguyên nhân phần 31% còn lại và phần SÓT |

## 3. Phạm vi và cấm

Trong phạm vi: bộ kiểm lỗi đã biết, bản sạch đối chứng, chẩn đoán nguyên nhân theo tầng, đề xuất bản sửa, rồi (sau khi owner chọn) sửa offline, hồi quy, build, QA, phát hành theo 14 bước.

Ngoài phạm vi: căn/cắt/ghép chunk CS-1, StructuralGate, store/DB, đường V5 ba lượt, Translation/Glossary/Pronoun UI, pilot, thay đổi acceptance 3/3. Không auto-replace tên/xưng hô không qua model + guard; không thêm bước duyệt bắt buộc cho mỗi câu. Văn bản sách và nhãn có văn bản ở lại ngoài Git (`D:\P5E-private\...`); trong Git chỉ có dữ liệu tổng hợp, hash, đếm và mã.

**Provider: không gọi.** Ngân sách chi tiêu hiện tại **USD 0**. Bất kỳ lời gọi trả phí nào cần quyền chi riêng của owner (phạm vi, chương/đoạn, trần, số lời gọi, sổ mới); quyền cũ của v4.18 không chuyển sang.

## 4. Các tầng có thể gây sót (giả thuyết cần phân biệt bằng số đo)

| Tầng | Câu hỏi | Cách đo offline |
|---|---|---|
| S1 finder | Dòng owner/phân xử xác định sai nhưng finder **không gắn cờ** (không có điểm) | Chạy `FixPointFinder` trên 8 chương, so với dòng RAW-phân-xử/FINAL; mỗi dòng không phủ gán một lý do (hàng thiếu self/call, từ ngoài từ điển `AddressChecklist.WORDS`, đảo chiều self/call, ngoài scope, xung đột, không có dấu thoại…) |
| S2 model | Điểm đã gắn cờ nhưng model trả `=` | Nhãn SÓT với trạng thái `KEPT/MODEL_KEEP` trong các lần chạy đã lưu (không tái dự đoán model) |
| S3 guard | Model đề xuất đúng nhưng `AddressEditGuard` chặn | Nhãn SÓT/ĐÚNG với `KEPT/ADDRESS_*`; replay offline phản hồi đã lưu |
| S4 sai lọt | Model đề xuất sai và guard cho qua | Nhãn SAI với `FIXED` |
| S5 nhãn | Chưa đủ căn cứ RAW/luật | CHƯA CHẮC / UNADJUDICATED, không vào mẫu số |

## 5. Các phase theo thứ tự

1. `X0_DOCS` — plan này, checklist, BUILD_STATE, WORKSPACE_SNAPSHOT. 
2. `X1_SUITE` — bộ kiểm lỗi đã biết + **bản sạch** đối chứng tách riêng (sidecar riêng + chỉ mục hash trong Git + fixture tổng hợp). Đầu ra: số lỗi theo tầng, số điểm sạch phải giữ nguyên.
3. `X2_DIAGNOSE` — chẩn đoán S1–S5 trên dữ liệu đã lưu và trên 8 chương, 0 USD.
4. `X3_PROPOSE` — đề xuất bản sửa kèm cách kiểm offline và (nếu cần) một canary trả phí để owner duyệt; **dừng để owner chọn**.
5. `X4_FIX` — chỉ sau khi owner chọn: sửa nhỏ theo nhóm, test chặn hồi quy trên bộ lỗi đã biết **và** bản sạch.
6. `X5_LIVE` — chỉ nếu owner cấp quyền chi riêng.
7. `X6_RELEASE` — hồi quy đầy đủ, `scripts/build-and-save.ps1`, QA, tag/archive, closure theo `DEVELOPMENT_WORKFLOW.md`.

## 6. Tiêu chí

- Bộ kiểm có hai phần tách bạch: **lỗi đã biết** (mỗi mục: nguồn, key chương:điểm, tầng, hash RAW/DRAFT, nhãn, lý do) và **bản sạch** (điểm ĐÚNG/giữ nguyên, phải không bị sửa). Mục UNADJUDICATED không vào mẫu số.
- Mọi bản sửa sau này phải: sửa được ít nhất một lỗi đã biết ở tầng đúng; không làm đổi kết quả của bản sạch; giữ các SAI cũ bị chặn; kết quả replay 006 (C6B, C6E), 008, 011 (C7B, C7D) được so từng điểm trước/sau.
- Không có số nào được nâng thành "PASS chất lượng" từ fake provider hoặc replay; hành vi model chỉ đo được bằng lần chạy thật được cấp quyền.

## 7. Trạng thái (2026-10-12)

| Phase | Trạng thái | Bằng chứng |
|---|---|---|
| X0_DOCS | xong | commit `6d6b9294` |
| X1_SUITE | xong | 36 mục lỗi đã biết, 122 mục bản sạch (hai phần tách riêng, kiểm tách rời trong công cụ); chỉ mục công khai `docs/V419_PRONOUN_KNOWN_MISS_INDEX.json`, `docs/V419_PRONOUN_CLEAN_CONTROL_INDEX.json`; sidecar riêng `D:\P5E-private\v419-pronoun\`; runner `scripts/chunk/run-pronoun-suite-replays.ps1` (mã hiện hành: 158/158 giữ nguyên, 0 hồi quy) |
| X2_DIAGNOSE | xong | `docs/V419_PRONOUN_MISS_DIAGNOSIS_20261012.md`: finder phủ 83,0% dòng thoại owner sửa có từ xưng hô đổi (88/106; 57,5% nếu tính cả lời kể); số SÓT của 006/008 đo dưới prompt cũ; 13/20 điểm S2 FINAL cũng không sửa |
| X3_PROPOSE | đã trình, chờ owner | mục 5 của báo cáo (A: owner đọc 23 điểm; B: đo lại prompt hiện hành trên 008 rồi 006, cần quyền chi riêng; C, D: mã, chỉ nếu A/B cho thấy cần) |
| Review 2026-10-13 | xong | `docs/V419_PRONOUN_MISS_DIAGNOSIS_20261012.md` mục 8: kết quả kiểm lại trên archive sạch `2e7900d0`; năm method test opt-in đổi từ `return` sang `Assume` (app 465 PASS / 8 SKIP); 014 chỉ phủ 48,6% và S2 ngoài 006/008/011 là NOT_MEASURED; ca `mình` và xung đột hàng cùng họ gán dòng cho hàng; câu hỏi A là ngữ nghĩa của `call` khi là tên |
| Quyết định owner D-A (2026-10-13) | đã duyệt | khi `call` của hàng là **tên**, `cậu` được giữ làm đại từ ngôi hai nếu `self` của hàng là `mình` hoặc `tớ`; tên chỉ bắt buộc ở lời gọi trực tiếp (vocative). Với `self` khác (`tôi`, `em`, …) luật cũ giữ nguyên cho tới khi owner quyết thêm. Owner cũng duyệt push `feature/v4.19` và dòng trỏ v4.19 trong `AGENTS.md` |
| X3 áp D-A (2026-10-13) | xong (offline) | `docs/V419_PRONOUN_MISS_DIAGNOSIS_20261012.md` mục 9: 7 điểm chuyển sang bản sạch theo bảng `docs/V419_PRONOUN_DA_RELABEL.json`; bộ kiểm 29 lỗi đã biết / 129 bản sạch; replay 158/158 không đổi; test `NameCallRuleDaContradictionTest` chỉ ra prompt trái D-A; đề xuất chỉ sửa prompt; gói đo B 008 soạn sẵn, chờ quyền chi |
| X4_FIX, X5_LIVE, X6_RELEASE | chưa bắt đầu | — |

**Next action:** owner chọn: (1) duyệt bản sửa prompt D-A (X4, offline) và/hoặc (2) cấp quyền chi cho gói đo B 008 (mục 9.4 của báo cáo: 1 lần chạy, ≤ 4 lời gọi, trần USD 0.03); hoặc quyết định mở rộng D-A cho self `tôi` trước khi chi. Không provider trước khi có quyền chi riêng.
