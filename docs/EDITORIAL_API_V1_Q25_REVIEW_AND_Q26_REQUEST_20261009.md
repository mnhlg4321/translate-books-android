# Editorial API V1 — review canary Q2.5 và yêu cầu Q2.6 (2026-10-09)

Coordinator: Claude. Baseline: HEAD `8856c8c1` (đã push). APK `4.18-q2.2`/code 246, chỉ `emulator-5554`. Model: `openai/gpt-5.6-luna`, reasoning medium (D-Q2b).

## 1. Kiểm chứng

| Claim | Evidence coordinator tự kiểm | Kết luận |
|---|---|---|
| Canary V5-luna 007 dừng ở lượt 1, 1 call, USD 0.0080449 | Đọc `01-response.txt` riêng: Stop Receipt `INPUT_ARTIFACT_MISSING`, `L1_SOURCE_PREFLIGHT`; chỉ có `01-*` | Đã chứng minh; quy tắc canary hoạt động đúng (tốn USD 0.008 thay vì cả gói) |
| Request có đủ 4 khối file và một Project Instruction | `01-request.txt`: system 1 lần; 4 khối `=== FILE: … ===` | Đúng, nhưng tên khối là `RAW.txt`, `DRAFT.txt`, `GLOSSARY.csv`, `PRONOUN.csv` |
| Test: engine 572, app 439, `scripts/p6` 80, preflight 8/8, emulator 14/14 | Theo báo cáo; không chạy lại (lượt này không đổi mã) | Chấp nhận theo evidence |
| Chất lượng | Chưa có FINAL | NOT_MEASURED |

## 2. Chẩn đoán

- **Triệu chứng:** model báo thiếu "chain identity metadata `ID` và `SERIES`" và "pinned source manifest".
- **Nguyên nhân (đã chứng minh):** trong ChatGPT Project, owner đính kèm file tên gốc (`007_RAW_JAKUAKU_MONSTER_VOL1.txt`, `…_DRAFT.txt`, `007_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv`, `007_PRONOUN_JAKUAKU_MONSTER_VOL1.csv`) — pack suy `ID`=007, `SERIES`=JAKUAKU_MONSTER_VOL1 từ đó, và đặt tên đầu ra `[ID]_FINAL_QA_[SERIES].txt`. App gửi tên chung vì tên được ghi cứng ở `V5SourcePackPreflight.REQUIRED_NAMES` (`editorial-engine/.../api/V5SourcePackPreflight.java:14`) và `SOURCE_NAMES` của runner (`EditorialApiV1FixtureRunnerInstrumentedTest.java:47`); bản sao input `D:\P5E-private\q1-inputs\<ch>\` cũng đã mất tên gốc.
- **Họ lỗi:** đây là lần thứ hai cùng họ "đầu vào V5 chưa giống ChatGPT" (lần 1: glossary 4 cột). Theo quy tắc, phải xử lý cả họ, không vá từng điểm: Workflow 4.1.3 dòng 15 yêu cầu mỗi lượt có **Source Manifest: ID/series/version, tên file, số ký tự/dòng, neo đầu–cuối và SHA-256 nếu host cung cấp**; dòng 31 yêu cầu xác nhận đọc được bytes. Đây là việc ghi sổ mà app (host) phải cung cấp — đúng nguyên tắc "app làm bookkeeping, model làm nội dung".

## 3. Yêu cầu làm việc Q2.6

Branch giữ nguyên. Model `openai/gpt-5.6-luna` reasoning medium. Không đụng U1, pilot, chunk-pair. FINAL owner chỉ để chấm.

### Q2.6.1 — Danh tính nguồn đầy đủ cho V5 (offline)

1. `EditInputs.OriginalSourceFile` thêm `role` (RAW/DRAFT/GLOSSARY/PRONOUN) tách khỏi `name`. `V5SourcePackPreflight` kiểm theo **role** (đủ 4 role, mỗi role một file, schema như hiện có), không kiểm tên cố định; tên phải không rỗng và là tên gốc.
2. Thêm `chainId` và `series` vào input V5 (runner: lấy từ tên file gốc theo mẫu `NNN_…`/`…_JAKUAKU_MONSTER_VOL1`; app sau này: từ thư viện nguồn U1 — `chapter_key`, `series_hint`). Thiếu một trong hai → dừng trước provider với `V5_IDENTITY_MISSING`.
3. Lượt 1 bắt đầu bằng khối **HOST SOURCE MANIFEST** do app lập: `ID`, `SERIES`, `VERSION=V5-SAFE.4.1.3-FULL`, và mỗi file một dòng: role, tên gốc, bytes, ký tự, số dòng, dòng đầu/cuối không rỗng (neo), SHA-256 (app tính), `bytes_readable=yes`. Sau đó mới tới 4 khối file với **tên gốc**.
4. Runner: thư mục input riêng giữ file với tên gốc (copy lại từ `D:\Ebooks\JAKUAKU MONSTER\` theo manifest đã khóa ở Q1, không đổi nội dung). Ghi tên gốc + SHA-256 vào metadata run.
5. **Rà các điều kiện còn lại của pack trước khi chạy:** đọc mục "L1 SOURCE PREFLIGHT" (Workflow dòng 28–37), G14, G21, G22 và mọi mã `INPUT_*` trong pack; lập bảng "điều kiện → app cung cấp gì → test" trong báo cáo. Điều kiện nào app chưa cung cấp được thì ghi rõ, không chạy live tới khi có.
6. Test: lượt 1 render với file mẫu tự tạo có đúng tên gốc, đủ dòng manifest (ID, SERIES, VERSION, 4 file), SHA-256 khớp nội dung; preflight theo role; thiếu ID/SERIES → dừng trước provider. Chạy **toàn bộ** engine, app, `scripts/p6`.

### Q2.6.2 — Build và emulator (offline)

Build qua wrapper (code > 246), cài chỉ `emulator-5554`, test Editorial API tập trung như Q2.5.3 (dùng đúng danh sách lớp, không gọi bộ instrumented rộng).

### Q2.6.3 — Live (theo D-Q2c)

1. Canary V5-luna chương 007. Dừng ở bất kỳ lượt nào → dừng V5 và báo mã; **không** dừng E-luna-b.
2. `E-luna-b` (đã build ở Q2.5, chưa chạy) trên dev 004–008 — độc lập với V5, chạy song song bất kể canary.
3. Canary V5 đạt: V5-luna trên 004, 005, 006, 008.
4. Chấm bằng bộ chấm hiện tại; áp cổng "chất lượng tối thiểu 4.1.3" (mục 7 của request Q2.5). Chọn arm; holdout 011, 014, 017; xuất bản app + trang đọc vào `D:\P5E-private\q2-outputs\`. Dừng.

### Bằng chứng

Cập nhật `docs/EDITORIAL_API_V1_Q2_EXECUTION_20261008.md` (mục Q2.6): bảng điều kiện pack, commit, test, APK, hash lượt 1 + danh sách dòng manifest (không văn bản sách), kết quả canary, bảng dev/holdout, call/token/USD. Cập nhật §10, snapshot, BUILD_STATE. Push sau mỗi gói.

## 4. Cần owner

| ID | Nội dung | Khuyến nghị |
|---|---|---|
| D-Q2c | Cho phép Q2.6.3 trong ngân sách D-Q2b còn lại (trần Q2 USD 10.00, đã dùng USD 0.8455608): chạy lại canary V5 sau khi sửa danh tính nguồn, và chạy `E-luna-b` dev 004–008 không phụ thuộc canary | Duyệt — chi phí dự kiến < USD 1 với luna |

## 5. Quyết định đã nhận

Owner (chat, 2026-10-09): "duyệt D-Q2c".

Q2.6.3 chạy ngay sau khi Q2.6.1–Q2.6.2 PASS, không hỏi lại: canary V5-luna 007 lại sau khi sửa danh tính nguồn; `E-luna-b` dev 004–008 độc lập với canary; V5-luna 004/005/006/008 chỉ khi canary đạt; chấm cổng tối thiểu 4.1.3; holdout 011/014/017 với arm đã chọn. Model `openai/gpt-5.6-luna` reasoning medium; ngân sách D-Q2b (trần Q2 USD 10.00, đã dùng USD 0.8455608; chương V5 USD 1.50, E USD 0.10); chỉ `emulator-5554`. Dừng và hỏi nếu: bảng điều kiện pack còn điều kiện app chưa cung cấp được, canary V5 dừng lần nữa (báo mã, vẫn hoàn tất E-luna-b dev), UNKNOWN cost, lỗi hạ tầng, hoặc ước tính vượt trần.

## 6. Independent review và bước tiếp theo (2026-10-09)

Baseline review `011961f6`; APK/source `7cc4108b` code247. Q2.6 dispatch đã kết thúc; mục 5 không tự cấp quyền chạy lại. Không đổi model, prompt, gate hay membership từ kết quả review.

### Việc đã xác minh / sửa nhỏ

- Ledger hash-chain hợp lệ: 24 logical reservations, 48 entries, USD 1.17703995, pending 0; riêng Q2.6 là 13 logical/29 physical calls, USD 0.33147915. Hash APK/source ZIP ở hai archive và sáu file đọc/xuất khớp báo cáo. Không đọc lại thiết bị trong review.
- Chọn V5 và chạy holdout khi cả hai 0/5 đúng mục 7 của yêu cầu Q2.5 và D-Q2c; đây không phải tuyên bố chất lượng đạt.
- V5 004 dừng QA với CONTENT_UNACCOUNTED_CHANGE, không phải lỗi tên. DRAFT fallback phải ghi chất lượng candidate NOT_MEASURED, không diễn giải các số 0 thành kết quả biên tập.
- 017 có hai replacement và một insertion khi diff trực tiếp DRAFT→app; báo cáo "0 changed lines/no-change" đã sửa. Thêm diagnostic alignment/draftAppDiff chỉ chứa số dòng; không đổi điểm cũ hoặc acceptance. Test từ archive sạch `011961f6` cộng đúng hai file scorer/test: scorer 8/8, gate 7/7. Ba test mới lỗi trên baseline thiếu diagnostic, qua sau patch.

### Một bước tiếp theo: gói offline chẩn đoán chất lượng và chốt UX tên đầu ra

Không mở release/branch/checklist mới; cập nhật tiếp gói này. UI tổng thể U1 vẫn hoãn; thay đổi tên là phạm vi nhỏ được tách rõ, không được coi là cách sửa chất lượng.

| Ownership / file | Việc và đầu ra | Test / PASS |
|---|---|---|
| QA: `scripts/p6/score_vs_final.py`, `test_score_vs_final.py`; báo cáo này | Báo riêng dòng không căn được và diff DRAFT→app; giữ nguyên score/gate lịch sử; invalid response tách khỏi semantic | Insert/delete/replace/no-op; mọi metric cũ không đổi; không ghi văn bản sách vào Git |
| COORDINATOR: evidence riêng Q2.6 | Phân xử theo RAW các thay đổi 017; thiếu/sai/xưng hô trong 011/014; replay L2/L3 004 để tách thay đổi thật khỏi cáo buộc trong receipt | Có dòng nguồn/đầu ra và kết luận đúng/sai/hợp lệ khác reference/chưa chắc; không dùng similarity làm phán quyết nghĩa; không chỉnh prompt từ holdout mà vẫn gọi nó là holdout mới |
| APP: `EditorialApiExport`, `EditorialApiCombo`, `EditorialApiUiController`, `EditorialApiPageFactory`, store/codec liên quan | Đề xuất trường "Tên file kết quả" trước chạy, cho sửa và lưu cùng tổ hợp; dùng đúng tên đã chốt khi export. Tên xuất là metadata độc lập với identity/hash nội dung | Tên trống thì đề xuất; tên lỗi thì giữ form và báo tại ô trước provider; Unicode, ký tự cấm, đuôi .txt, reopen, trùng tên không tự ghi đè; đổi tên không đổi content hash hoặc làm mất run |
| ENGINE/APP source: `V5SourceIdentity`, `V5SourcePackPreflight`, `EditorialApiSourceLoader` | Chốt identity từ metadata/app hoặc người dùng xác nhận, không buộc suy từ cả bốn tên. Nguồn tên RAW.txt hợp lệ không bị cấm chỉ do generic khi identity rõ. Không đổi bytes/tên gốc trong provenance, không lấy tên FINAL làm bằng chứng RAW/DRAFT tương ứng | Bốn tên tùy ý + identity đầy đủ; nguồn khớp và nguồn lệch; hash/role thiếu vẫn chặn; ngăn path/control/injection; original names/identity/output name lưu riêng |
| QA/UI | Kiểm đường app thực dùng loader/provider, không lấy fixture runner làm bằng chứng UI | Hiện app UI chỉ có QUICK/THOROUGH; SourceLoader chưa cấp original files/identity cho V5. Phải ghi NOT_REACHED cho V5 qua UI tới khi có wiring/test đúng đường; không tự mở rộng toàn U1 |

Phụ thuộc: chẩn đoán và hợp đồng dữ liệu trước sửa UX/store; test host trước build; kiểm thiết bị và live theo quyền riêng. Scope/ID/SERIES trong pack là metadata host cung cấp, không yêu cầu model đặt tên hay tự hash. Tên đầu ra có chữ FINAL không tự cấp trạng thái chất lượng đạt.

Điểm dừng: báo cáo phân xử + thiết kế/tác vụ tên có test rõ, không provider/build/device/commit/push từ review này. Không đề xuất chạy lại toàn ma trận chỉ vì tên đã sửa. Nếu sau đó cần live, phải có giả thuyết chưa giải được offline, bản contract/build cụ thể và ngân sách per-call/nhóm mới được owner chấp thuận; số dư ledger không phải quyền dispatch.
