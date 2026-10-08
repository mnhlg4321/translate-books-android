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
