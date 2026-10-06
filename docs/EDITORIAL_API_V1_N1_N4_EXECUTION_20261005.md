# Báo cáo thực thi Editorial API V1 — N1–N4 (offline)

Ngày chốt: 2026-10-06 (+07:00). Phạm vi tiếp tục chuỗi Claude đã hoàn tất N1–N3; N4 được kiểm chứng trên cùng HEAD và push riêng. Không gọi provider, không đọc khóa/account, không đụng pilot. Dừng trước N5 vì D-N4 chưa được duyệt.

## Quyết định và commit

- Câu duyệt D-N1, D-N2, D-N3 đã được ghi trong mục **Quyết định** của `docs/EDITORIAL_API_V1_WORK_REQUEST_N1_N4_20261005.md` tại commit `511960ee`; D-N4 vẫn chưa duyệt.
- N1 engine: `b2fb1a6d`.
- N2 app/store/provider/service/export/runner: `afbd34ce`.
- N3 tab Biên tập và presenter/UI tests: `195406bb`.
- Sửa tiếp theo đã có trên remote: `e3f8a784`, `18571c01` (giá model không biết; QUICK là E một lượt).
- N4: báo cáo, trạng thái và bằng chứng được commit/push tại `72b6178a`.

## Bằng chứng phải giữ

### N1–N3 và regression

N1–N3 được kiểm lại từ worktree tạm sạch `D:\P5E-builds\wt-api-n4-20261005`, source `18571c0138436a699bdcc67851b2afb42b60d60b`:

| Hạng mục | Kết quả |
|---|---:|
| `:editorial-engine:test` | 484/484, 0 fail |
| `:app:testDebugUnitTest` | 386/386, 0 fail |
| Python `scripts/p6` (`unittest discover`) | 71/71, 0 fail |
| `:app:assembleDebugAndroidTest --offline` | PASS |
| SAFE4 cũ trong các test trên | PASS |

`py_compile` cho Python `scripts/p6` PASS. Lần gọi engine test đầu tiên gặp JVM 8 do shell; chạy lại với JDK 21 mà wrapper yêu cầu đã PASS, không có thay đổi mã.

### N4 build và archive

Production đã được tạo bằng `scripts/build-and-save.ps1`; AndroidTest bằng `scripts/build-and-save-android-test.ps1` với `--offline`, cùng source archive:

| Payload | Giá trị |
|---|---|
| Version/code | `4.18-api.3` / `240` |
| Source | `18571c0138436a699bdcc67851b2afb42b60d60b` |
| APK SHA-256 | `9F66F0C9D69F19728DD7EBB77138B946CDAF6672081F468A7B52638E5E4957C8` |
| Source ZIP SHA-256 | `971373AF209C5FF137EA3F52BCB6008C734F06A012C80E5F1EC01EEFA2227A14` |
| AndroidTest SHA-256 | `20CBCF3C69AE4E81C26B31FB73BB0C6B94FDC1282AB2F5AE38F6444D812A9381` |
| Certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |
| Production archive | `D:\P5E-builds\wt-api-n4-20261005\artifacts\builds\v4.18-api.3\build-20261005-230303` |
| Production backup | `D:\P5E-builds\wt-api-n4-20261005\backup\builds\v4.18-api.3\build-20261005-230303` |
| AndroidTest archive | `D:\P5E-builds\wt-api-n4-20261005\artifacts\test-builds\v4.18-api.3\api-n4-18571c01-20261005-03` |
| AndroidTest backup | `D:\P5E-builds\wt-api-n4-20261005\backup\test-builds\v4.18-api.3\api-n4-18571c01-20261005-03` |

APK/source ZIP bytes match between artifact and backup. The archive contains the exact tracked source ZIP; the temporary worktree was clean when it was made.

### N4 emulator và fake provider

Chỉ dùng `emulator-5554`; pilot không được truy cập. Production preflight qua install guard khớp version/code/certificate; AndroidTest package readback có đúng SHA `20CBCF3C69AE4E81C26B31FB73BB0C6B94FDC1282AB2F5AE38F6444D812A9381`.

| Kiểm tra | Kết quả |
|---|---:|
| `EditorialApiBienTapFlowInstrumentedTest` | 3/3 PASS: đủ 4 nguồn với QUICK; thiếu Glossary/Pronoun với THOROUGH; export/readback SHA |
| Force-stop app rồi chạy lại hai combo | 2/2 PASS |
| `EditorialApiStoreInstrumentedTest` | 4/4 PASS: DB v26, reopen, migration, cascade |
| Actual provider calls / spend | 0 / USD 0 |

Ảnh chỉ chứa UI và văn bản mẫu/placeholder, giữ ngoài Git:

- `D:\P5E-private\editorial-api-n4-20261006\screenshots\editorial-tab-synthetic.png` — SHA-256 `DFF35377356E74CC0ADD88675A26E2FA2D4F1673BA30973A3A929B930D78F1FE`.
- `D:\P5E-private\editorial-api-n4-20261006\screenshots\editorial-combo-empty-synthetic.png` — SHA-256 `1921BE5D2B2B09CBDA17D4383B9606E63DE54411717D4D6DD9974DC923742BA3`.

### Fixture runner dry-run

Runner API V1 chạy `fx-a01` với `API_V1_QUICK`, fake provider cục bộ, group `API-N4-QUICK-20261005`. Evidence private: `D:\P5E-private\p6-runs\4a0e6d67-0f2c-4c52-9f8e-2c0d9f4f7a61`.

- `STRUCTURAL_VALID=1/1`, state `FINAL_OK`, `providerKind=FAKE_OFFLINE`.
- `actualProviderCalls=0`, fake calls 1; ledger 2 rows, pending UNKNOWN 0, settled USD 0.004 trong cap 0.50.
- Metadata ghi `contractRevision=EDITORIAL_API_V1.1`, source commit và APK code 240.
- `SEMANTIC_EVAL=FAIL` là kết quả đúng của scorer vì `fx-a01` là fixture có lỗi gieo; không dùng nó để tuyên bố semantic PASS và không sửa fixture/đáp án.

## Lệch so với yêu cầu

- N1–N3 đã được Claude hoàn tất và push trước khi tiếp quản; không tạo lại commit hay reset branch. N4 dùng archive đã tạo từ đúng HEAD sau khi kiểm tra hash, test output và parity hai nơi.
- AndroidTest archive ghi `installed=false` theo thiết kế archive-only; package đã có trên emulator và được xác minh bằng hash/readback trước instrumentation. Không có cài đặt pilot.
- Ảnh và response/final text fixture giữ ngoài Git.

## Kết luận và bước tiếp theo

N1–N4 offline đạt các gate kỹ thuật của work request. Không có provider call hay chi phí API. N5 chưa chạy; bước duy nhất tiếp theo là owner quyết định D-N4 (ngân sách/cap và phạm vi N5) trước mọi provider/device run.
