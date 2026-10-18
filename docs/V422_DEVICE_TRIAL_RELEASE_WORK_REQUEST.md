# v4.22 — Yêu cầu làm việc: bản dev có R, thử trên điện thoại thật, phát hành 4.22.0

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (khối v4.22). Checklist: `release_checklists/v4.22.md`. Nhánh duy nhất: `feature/v4.22`, worktree `D:\P5E-builds\wt-v4.22`. Không làm trong `D:\App Translate Books` (thay đổi dở của owner; không switch/merge/commit ở đó). Yêu cầu này nối tiếp `docs/V422_WORK_REQUEST.md` mục 4 bước 6–7 và thay cho phần đó.

**Quyết định sản phẩm được phục vụ:** phát hành 4.22.0 hay chưa. Cổng quyết định là owner dùng bản có R trên điện thoại với chương thật. Mọi bước dưới đây hoặc đưa một bản đúng tới tay owner, hoặc đóng gói bản owner đã chấp nhận. Không thêm bộ kiểm, cổng đo hay đo trả tiền.

## 1. Hiện trạng (kiểm 2026-10-18)

| Mục | Giá trị | Nguồn |
|---|---|---|
| HEAD nhánh | `db71bef5` (R, `CP-IMPL-21`), đã có trên `origin/feature/v4.22` | `git log` |
| Điện thoại `15e84958` (OPPO CPH2691, Android 15) | `4.22-dev.2` / code 273, cài 2026-10-14 16:59, cờ `DEBUGGABLE` | `dumpsys package` (chỉ đọc) |
| Bản trên điện thoại chứa | U1, U2, U3, P, G, sửa thẻ Jobs `1ba67d1b` | snapshot |
| Bản trên điện thoại **thiếu** | sửa nhãn "Dòng n" (`2ee39476`), R (`db71bef5`) | snapshot |
| Bản build mới nhất | `4.22-dev.1` / code 274 từ `e8083883`: có sửa nhãn, không có R, tên trùng code 272 | `artifacts/qa/v4.22-dev-20261015/QA_REPORT.md` |
| Hồi quy archive sạch gần nhất | `48860789`: engine 742 / 1 SKIP, app 492 / 5 SKIP mỗi biến thể, lint 0 lỗi | snapshot |
| Sao lưu điện thoại gần nhất | `App Translate Books-device-backups\v4.21.0-code271\20261014-165840\` (trước khi cài dev.2) | thư mục sao lưu |
| Chi phí | 0 lời gọi provider, USD 0 | snapshot |

Ghi chú `AGENTS.md` trong workspace vẫn ghi điện thoại chạy 4.21.0: sai so với thực tế (dev.2). Không sửa file đó trong workspace (bước F9 ghi vào snapshot của nhánh).

## 2. Lỗ hổng tìm được khi nghiên cứu

| # | Lỗ hổng | Hệ quả nếu bỏ qua | Xử lý |
|---|---|---|---|
| H1 | `artifacts/builds` của worktree chỉ có `v4.22-dev.1`; `backup/builds` trống. `build-and-save.ps1` chọn số thứ tự từ archive của **chính repo đang build** | Build tiếp theo lấy tên `4.22-dev.2`, trùng tên bản code 273 đang ở trên điện thoại | F1: trỏ archive worktree tới archive của owner |
| H2 | `artifacts/qa/v4.22-dev-20261015/media/` chưa track (13 file, 2,4 MB, ảnh dữ liệu tổng hợp) | `build-and-save.ps1` từ chối build khi có file chưa track | F1: kiểm ảnh không có chữ sách rồi commit |
| H3 | R chỉ có test host (`RefusedProposalTest`, `EditorialPairAddressKeepServiceTest`); không có androidTest nào bấm "Áp dụng đề xuất" trên SQLite thật hay chụp mục "Đề xuất bị chặn" | Lỗi UI/SQLite của R chỉ lộ ra trên điện thoại owner | F3: thêm một test thiết bị và một ảnh |
| H4 | Bản dev debuggable; bản phát hành có `debuggable false` | Sau khi cài 4.22.0, không sao lưu được bằng `run-as` nữa | F5/F8: sao lưu trước **mỗi** lần cài, khi bản đang chạy còn debuggable; owner tự xuất dữ liệu quan trọng trước khi cài bản phát hành |
| H5 | Hiện chỉ điện thoại cắm máy, `emulator-5554` không chạy | Lệnh không ghi `-s` có thể chạy test thiết bị lên điện thoại owner (test xóa/ghi dữ liệu thử) | Mọi lệnh adb/Gradle ghi rõ serial; test thiết bị chỉ trên `emulator-5554` |
| H6 | `-ExactReleaseVersion 4.22.0` chỉ chạy trên nhánh tên `feature/v4.22.0` | Build RC bị từ chối | F7: worktree tạm, nhánh cục bộ `feature/v4.22.0` (như 4.21.0), không push, gỡ sau |
| H7 | versionCode: điện thoại 273, đã dùng 274 | RC có code thấp hơn bản trên máy thì không cài đè được | Dev mới ≥ 275; RC lớn hơn mọi code đã dùng |
| H8 | Chất lượng sửa, bộ chọn file hệ thống, thông báo foreground trên điện thoại: NOT_MEASURED | Ghi chú phát hành nói quá | F6: owner xác nhận từng mục trong phiếu thử; mục nào không thử thì ghi NOT_MEASURED |

## 3. Quyền

- Không gọi provider từ máy phát triển. Mọi test tự động dùng provider giả, USD 0.
- Test thiết bị tự động: chỉ `emulator-5554`. Không chạy test APK trên điện thoại.
- Điện thoại `15e84958`: chỉ đọc (`dumpsys`, `getprop`) khi chưa có duyệt. **Mỗi lần sao lưu + cài cần owner duyệt riêng trong chat** (F5, F8). Không gỡ, không xóa dữ liệu, không `pm clear`.
- Lượt Biên tập thật trên điện thoại là owner tự bấm bằng khóa API của owner. Agent không chạy, không đọc khóa, không đọc văn bản sách ra khỏi máy.
- Push `feature/v4.22` sau khi quét. Tag, merge, push `main` chỉ sau khi owner duyệt phát hành (F7).

## 4. Các bước

**F1 — Chuẩn bị build (sửa H1, H2).**
- So `artifacts/builds/v4.22-dev.1/build-20261015-090335` của worktree với bản ở `D:\App Translate Books\artifacts\builds\…` và `backup\builds\…` (SHA256SUMS). Giống hệt thì đổi tên thư mục cũ của worktree thành `artifacts/builds.local-20261015` (không xóa), rồi tạo junction `artifacts\builds` → `D:\App Translate Books\artifacts\builds` và `backup\builds` → `D:\App Translate Books\backup\builds`. Junction nằm trong đường dẫn đã ignore.
- Kiểm 13 file trong `artifacts/qa/v4.22-dev-20261015/media/` không chứa chữ sách (dữ liệu tổng hợp của walkthrough), rồi commit `docs(qa): ...`.
- PASS khi: `build-and-save.ps1` đọc được archive và chọn tên `4.22-dev.3`, code ≥ 275. Chạy bước chọn số mà không build (hoặc build rồi kiểm `BUILD_INFO.json`); nếu vẫn ra `4.22-dev.2` thì dừng, không build.

**F1b — S0 an toàn dữ liệu thư viện (owner 2026-10-18).** Làm toàn bộ `docs/V422_S0_LIBRARY_SAFETY_WORK_REQUEST.md` (S0.1–S0.6; S0.7 chạy trong F4) trước F2, để `4.22-dev.3` có cả R và S0.

**F2 — Hồi quy archive sạch của HEAD sẽ build.** `git archive` HEAD → thư mục tạm: `:editorial-engine:test`, `:app:test` (debug/release/benchmark), `:app:lintDebug`, `:app:compileDebugAndroidTestJavaWithJavac`, Python `scripts/p6`, `scripts/chunk`. PASS / SKIP / FAIL theo từng method; SKIP không tính là PASS. Log để ở `D:\P5E-private\v422-host-evidence\`. Lỗi cục bộ: `FAILED_REPAIRING`, sửa trên nhánh, chạy lại.

**F3 — Test thiết bị cho R (sửa H3).** Thêm vào androidTest (dữ liệu tổng hợp, provider giả trả một câu bị `AddressEditGuard` chặn):
- mở kết quả, thấy mục "Đề xuất bị chặn — chờ bạn quyết" với câu đề xuất và hàng pronoun được trích;
- bấm "Áp dụng đề xuất": đúng một dòng của bản cuối đổi, nhật ký có `PROPOSAL_APPLIED`, tệp xuất có dòng mới;
- force-stop rồi mở lại: thay đổi vẫn còn (SQLite thật), nút không áp dụng lần hai;
- hàng `call` dạng `A/B` được áp tự động, không hiện trong mục bị chặn;
- chụp ảnh mục bị chặn trước và sau khi áp dụng, đưa vào walkthrough `EditorialV422MediaWalkthroughInstrumentedTest`.
- PASS khi: test host và compile xanh trên archive sạch; test thiết bị PASS ở F4.

**F4 — Bản dev `4.22-dev.3` và QA emulator.**
- `scripts/build-and-save.ps1 -Series 4.22-dev -MinimumVersionCode 275` từ HEAD sạch (không file chưa track); test APK bằng `scripts/build-and-save-android-test.ps1`, ghim vào APK, source ZIP và cert của bản vừa build. Kiểm hai mirror.
- Bật `emulator-5554`; `scripts/install-validated.ps1 -Serial emulator-5554 -ApkPath … -ExpectedApkSha256 … -ExpectedApkCertificateSha256 …`; đọc lại version/signer.
- Chạy các nhóm như dev.2 (danh sách đóng băng, lớp chunk, sản phẩm, khởi động lại seed/verify, foreground) + test F3 + walkthrough ảnh. Tổng hợp bằng `scripts/qa/instrumentation_summary.py`. Khôi phục DB/journal của emulator và so hash.
- Xem từng ảnh. Báo cáo `artifacts/qa/v4.22-dev3-<date>/QA_REPORT.md` + `SUMMARY.json` (chỉ số, hash), mirror sang `backup/qa/`.
- Dừng. Gửi owner: đường dẫn APK, SHA-256, danh sách thay đổi so với dev.2 (nhãn dòng, R, ô `/`), ảnh, và câu hỏi duyệt F5.

**F5 — Cài lên điện thoại (chỉ khi owner duyệt riêng trong chat).**
- Sao lưu khi dev.2 (debuggable) còn chạy, theo cách lần 2026-10-14: `private-data.tar.gz` qua `run-as`, `base.apk` đang cài, `dumpsys`, `device.txt`, `BACKUP_SHA256SUMS.txt` vào `App Translate Books-device-backups\v4.22-dev.2-code273\<thời gian>\`. So hash DB trong bản sao lưu với DB đang chạy.
- `scripts/install-validated.ps1 -Serial 15e84958 -CheckOnly` (APK, SHA-256 và cert ghim như F4), rồi cài (`install -r`, không gỡ). Đọc lại version 4.22-dev.3, signer `abebea4b`. Mở app một lần, không crash. So hash DB trước/sau khi cài.
- Ghi snapshot. Không chạy gì khác trên điện thoại.

**F6 — Owner thử trên điện thoại (cổng chấp nhận).** Agent đưa owner phiếu thử dưới đây và ghi kết quả vào `artifacts/qa/v4.22-owner-trial-<date>/OWNER_TRIAL.md` (chỉ có/không, số đếm và lời owner tóm lại; không chép chữ sách). Mỗi lỗi owner báo: tìm nguyên nhân trong mã, sửa trên nhánh, quay lại F2 → F5 (mỗi lần cài là một lần duyệt mới).

| # | Owner làm | Đạt khi |
|---|---|---|
| T1 | Tạo lượt 4 file bằng bộ chọn file hệ thống (RAW, DRAFT, Glossary, Pronoun) | chọn được cả bốn từ bộ nhớ máy; tên file hiện đúng |
| T2 | Tick "Chỉ dùng 2 file", thoát và mở lại | lựa chọn được nhớ |
| T3 | Chạy một chương thật (ví dụ Kumo 013 mà owner đã có FINAL), rời app trong lúc chạy | thông báo foreground hiện "Đoạn i/n · USD …"; tab Jobs có thẻ; chạy xong khi quay lại |
| T4 | Bấm "Hủy sau đoạn hiện tại", rồi "Chạy tiếp" | dừng sau một đoạn; chạy tiếp không làm lại đoạn đã xong |
| T5 | Mở kết quả | dòng tóm tắt bằng số; "Dòng n" đúng số dòng; lý do bằng tiếng Việt |
| T6 | Nếu có mục "Đề xuất bị chặn": đọc và bấm "Áp dụng đề xuất" cho một câu đúng | đúng một dòng đổi; bản xuất có dòng đó |
| T7 | Sửa một hàng pronoun thành dạng `A/B` (ví dụ `Shiro-chan/em`), chạy lại chương | câu dùng dạng thứ hai được giữ hoặc sửa tự động, không bị chặn |
| T8 | Settings → Biên tập: tắt "Thuật ngữ", chạy lại | không còn điểm thuật ngữ |
| T9 | Xuất bản cuối và mở bằng app khác | tệp mở được, đúng nội dung |
| T10 | So bản cuối với RAW theo xưng hô (không so độ giống FINAL) | owner ghi: số dòng sửa đúng, sửa sai, lỗi xưng hô còn sót |
| T11 | Library → Files → "Xuất thư viện" bằng bộ chọn tệp hệ thống; mở ZIP trên máy tính | ZIP có đủ glossary/pronoun (CSV/TXT đọc được), không có API key |
| T12 | "Nhập thư viện" với ZIP vừa xuất | xem trước báo toàn "đã có"; không tạo trùng; Active không đổi |
| T13 | Nhập nhiều glossary mới | Active không đổi; thông báo nhắc bấm Use |

Chi phí T3, T7, T8 do owner quyết (khóa của owner, trần chi phí trong Settings). Mục owner không thử ghi NOT_MEASURED, không ghi PASS.

**F7 — Phát hành 4.22.0 (chỉ khi owner chấp nhận ở F6 và duyệt phát hành).** Làm theo `docs/V421_E_RELEASE_WORK_REQUEST.md` E1–E9, thay số phiên bản:
- E1 hồi quy archive sạch của commit sẽ build; E2 `RELEASE_NOTES.md` + `CHANGELOG.md` mục 4.22.0 (U1, U2, U3, P, G, R, `CP-IMPL-21`: lượt cũ phải tạo lượt mới; giới hạn: chưa biết người nói, đề xuất bị chặn cần owner bấm, mục NOT_MEASURED từ F6).
- E3 RC: worktree tạm, nhánh cục bộ `feature/v4.22.0` tại commit E2, `-ExactReleaseVersion 4.22.0`, versionCode lớn hơn mọi code đã dùng; test APK ghim vào RC.
- E4 QA emulator; E5 Macrobenchmark + Perfetto mới trên emulator (mã sản xuất đã đổi so với 4.21.0; ghi rõ là số emulator); E6 ảnh và video bằng walkthrough 4.22 (có ảnh F3).
- E7 checklist bước 05–09 + PreTag + event `build-*`; E8 tag `v4.22.0`, event `tag-v4.22.0`, bước 10–14, Complete gate, merge `--no-ff` vào `main` trong worktree tạm; E9 quét, push, `git ls-remote`.
- Điều kiện tag: F6 không có lỗi mở; E1, E4 không có FAIL chưa giải thích; mã sản xuất RC đúng bằng commit E1.

**F8 — Cài 4.22.0 lên điện thoại (duyệt riêng, sau F7).** Owner dùng "Xuất thư viện" (S0) và giữ ZIP ngoài điện thoại trước. Sao lưu `run-as` từ dev.3 (lần cuối còn làm được, H4), rồi cài như F5. Sau bước này, sao lưu chỉ còn qua chức năng xuất trong app.

**F9 — Trạng thái.** Sau mỗi nhóm (F1, F4, F5, F6, F7): cập nhật `WORKSPACE_SNAPSHOT.md` (thay giá trị, đúng một Next action; ghi bản đang ở điện thoại), `BUILD_STATE.md`, checklist bước tương ứng. Commit chỉ khi nội dung đổi.

## 5. Tiêu chí

- Bản trên điện thoại chứa đúng HEAD đã hồi quy (hash APK trong snapshot khớp `dumpsys` + build event).
- F3 PASS trên emulator với SQLite thật; walkthrough có ảnh mục đề xuất bị chặn.
- Mỗi lần cài có bản sao lưu với hash DB khớp, và hash DB trước/sau cài khớp.
- Owner ký vào phiếu F6 (T1–T10 có kết quả hoặc NOT_MEASURED). Đây là cổng duy nhất cho phát hành.
- USD 0 từ phía agent.

## 6. Không làm

Không triển khai Q1 (người nói), không đổi schema DB, không đổi luồng dịch, không gọi provider từ máy phát triển, không chạy test tự động trên điện thoại, không cài khi chưa duyệt, không tag/merge trước F6 + duyệt phát hành, không thêm bộ kiểm hay cổng đo mới, không sửa `AGENTS.md` trong workspace của owner.
