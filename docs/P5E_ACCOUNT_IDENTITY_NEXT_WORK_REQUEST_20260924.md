# Yêu cầu làm việc — kiểm chứng account sau sửa identity lifecycle

Trạng thái tài liệu: READY_FOR_OWNER_DECISION / NOT_AUTHORIZED / NOT_EXECUTED.
Đây là yêu cầu cho một bước của P5E đang tiếp tục, không tạo release, branch hoặc track mới. Việc soạn tài liệu này không phải quyết định cho chạy thiết bị.

## 1. Mục tiêu và điểm kết thúc

Tiếp tục tại D:\App Translate Books, branch feature/v4.18-p5e-runner-repair-20260917. Dùng bản sửa d0ba3e49b608ce940f71426c4bf09225d1562546 để thực hiện, sau khi được owner duyệt, đúng một preflight và tối đa một account check. Kết quả cần đạt là receipt có kết luận MATCH, MISMATCH hoặc NOT_PROVEN và nguyên nhân cụ thể. Không đánh đồng việc chạy xong process với kiểm chứng thành công.

Canonical authority: EDITORIAL_RECOVERY_V4_18.md; trạng thái: BUILD_STATE.md, WORKSPACE_SNAPSHOT.md; checklist duy nhất: release_checklists/v4.18-editorial-v5-safe-4-1-3.md.

Đã đóng offline: lỗi class/method xuất hiện hai lần hợp lệ trong START/FINISH; lỗi chấp nhận dữ liệu sau terminal. QA lifecycle 25/25, runner 37/37, command chain 38/38 và helper self-test PASS. Không mở lại các lỗi này khi không có bằng chứng mới.

## 2. Provenance bắt buộc

Baseline implementation: d0ba3e49b608ce940f71426c4bf09225d1562546. Commit tài liệu về sau không tự làm mất hiệu lực QA; phải kiểm đúng byte của các file liên quan.

| Thành phần | SHA-256 |
|---|---|
| scripts/p5e-account-check-device-command.ps1 | 9B64078D5A2A6EA2C3A9618C4B1F8CDBB01D3CCF6C3120A2D7D4AFFF3F5E6331 |
| scripts/p5e-raw-live-supervisor.ps1 | B491DD4D26444ACA1234A1A03D8A932A2525FB7CD112F6B6F1520A735C631897 |
| scripts/p5e-account-check-device-preflight.ps1 | 09A33DC74820A1AA18B3EE47AA96862DC0AFA03067D5B46B433B74E4EB1AFA22 |
| scripts/p5e-account-check.ps1 | 96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65 |
| scripts/p5e-load-expected-digest.ps1 | 1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D |

Chi tiết QA và hash evidence: docs/P5E_ACCOUNT_IDENTITY_REPAIR_PROVENANCE_20260924.json. Loader đang có thay đổi working tree đã được pin; không reset về Git chỉ để làm sạch worktree.
Production APK giữ code207, SHA 2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD; test APK SHA 058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8. Không build/reinstall.

Event cũ signature-layout-adapter-01 đã CLOSED_STOP/NOT_PROVEN; giữ nguyên receipt, không diễn giải lại thành MATCH.
Event mới đề xuất duy nhất: D:\P5E-private\p5e-account-check-device-event-20260924-identity-lifecycle-01 . Tại lúc soạn, path chưa tồn tại; phải kiểm lại ngay trước chạy. Không tự tạo path khác nếu path này đã có.

## 3. Phạm vi quyết định của owner

Chỉ xin một quyết định cho toàn bộ event, không xin lại ở từng bước đã được duyệt. Mẫu quyết định để owner chủ động gửi khi đồng ý:

> Tôi duyệt yêu cầu P5E_ACCOUNT_IDENTITY_NEXT_WORK_REQUEST_20260924.md với command hash 9B64078D5A2A6EA2C3A9618C4B1F8CDBB01D3CCF6C3120A2D7D4AFFF3F5E6331 và helper hash B491DD4D26444ACA1234A1A03D8A932A2525FB7CD112F6B6F1520A735C631897. Cho phép đúng một event account-only tại D:\P5E-private\p5e-account-check-device-event-20260924-identity-lifecycle-01, serial 15e84958: tối đa 7 lệnh ADB preflight read-only, sau PASS tối đa một account check memory-only dùng key đang lưu trong app. Không provider, DB write, RAW, install hoặc retry.

Mẫu trên chưa phải quyết định đã nhận. Nếu owner đã gửi quyết định khớp trong phiên thì tiếp tục, không hỏi lại. Không dùng quyết định đã tiêu thụ của event cũ.

## 4. Các bước thực hiện

1. Đọc ba tài liệu trạng thái theo thứ tự canonical → BUILD_STATE → snapshot. Xác nhận branch/HEAD/status; bảo vệ thay đổi có sẵn. Không rà lại toàn bộ lịch sử không liên quan.
2. Kiểm hash các file trong bảng và các pin của provenance repair. Đọc QA đã lưu. Nếu khớp và PASS, không chạy lại QA chỉ để tăng số lần kiểm. Nếu sai, dừng trước device, chỉ xác định file sai; không tự sửa pin cho khớp byte chưa được QA.
3. Xác nhận quyết định owner đúng phạm vi mục 3. Nếu chưa có, bàn giao đúng quyết định còn thiếu, không tạo thêm một gói review khác.
4. Owner kết nối thiết bị serial 15e84958 và mở khóa thiết bị. Không chạy ADB chẩn đoán ngoài command, không đổi serial hoặc reconnect/retry tự động.
5. Dùng chính cửa sổ Windows PowerShell owner kiểm soát chứa expected value trong Process. Việc từng báo “đã nạp” không chứng minh cửa sổ mới vẫn có value. Agent không đọc/in/dump giá trị và không yêu cầu gửi vào chat.
6. Nếu owner biết đã đóng cửa sổ cũ, nạp lại bằng loader đã pin trong cửa sổ mới trước command. Kiểm hash loader rồi chạy trực tiếp `& '.\scripts\p5e-load-expected-digest.ps1'` trong chính PowerShell đó; không chạy loader trong process con. Owner tự nhập tại prompt kín. Không xin lại nhãn key, ngày hoặc provenance đã được chấp nhận nếu không có thay đổi thực tế.
7. Chạy đúng block mục 5 một lần, sau khi đã có quyết định. Block kiểm hash command và path chưa tồn tại trước khi gọi command. Không dùng default EvidenceDirectory vì default trỏ event cũ.
8. Để command thực hiện presence/shape, pin/path checks và tối đa 7 lệnh preflight. STOP tại bất kỳ gate nào thì không launch account runner.
9. Chỉ sau preflight PASS, command tự chạy account runner tối đa một lần. Giữ timeout hiện có: preflight 360000 ms, runner host 135000 ms, runner bên trong 120000 ms. Không mở terminal khác để chạy song song hoặc retry khi chưa thấy output.
10. Chờ kết thúc; đọc receipt JSON được redacted. Đối chiếu outcome, launch count, parser errors, identity/terminal/redaction/capture và scope counters. Không in instrumentation stdout/stderr, key, endpoint hoặc digest. Thiếu receipt thì ghi UNKNOWN/NOT_PROVEN, không suy luận launch=0.
11. Áp dụng bảng kết quả mục 6. Nếu STOP, ghi đúng gate và dữ kiện thiếu; không thử một event khác trong phiên để vượt STOP.
12. Cập nhật kết quả và các trường hiện hành của canonical/state/snapshot/checklist một lần sau kết quả terminal. Giữ nguyên receipt cũ, ghi hash receipt mới. Báo ngắn: kết quả, nguyên nhân, số launch, hành động đã/không làm, đúng một next action. Không đánh dấu release 05–09 chỉ vì account MATCH.

## 5. Command chỉ chạy sau quyết định owner

```powershell
Set-Location -LiteralPath 'D:\App Translate Books'
$commandFile = 'D:\App Translate Books\scripts\p5e-account-check-device-command.ps1'
$expectedCommandHash = '9B64078D5A2A6EA2C3A9618C4B1F8CDBB01D3CCF6C3120A2D7D4AFFF3F5E6331'
if ((Get-FileHash -LiteralPath $commandFile -Algorithm SHA256 -ErrorAction Stop).Hash -cne $expectedCommandHash) {
    throw 'OWNER_COMMAND_HASH_MISMATCH_STOP'
}
$eventEvidence = 'D:\P5E-private\p5e-account-check-device-event-20260924-identity-lifecycle-01'
if (Test-Path -LiteralPath $eventEvidence) { throw 'OWNER_EVENT_PATH_ALREADY_EXISTS_STOP' }
& 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe' -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $commandFile -EvidenceDirectory $eventEvidence
$eventExitCode = $LASTEXITCODE
Write-Host "P5E_ACCOUNT_CHECK_DEVICE_COMMAND_EXIT_CODE=$eventExitCode"
```

## 6. Kết quả và xử lý lỗi

| Kết quả | Diễn giải và hành động |
|---|---|
| MATCH, receipt hợp lệ | Đóng bước account với bằng chứng thực tế. Rà đúng gate A4.3 còn thiếu để đề xuất bước tiếp theo; chưa tự dispatch RAW hoặc mở P6. |
| MISMATCH, receipt hợp lệ | Account test hoàn thành nhưng predicate route/digest không khớp. Không kết luận key hỏng hoặc provider sai; chỉ review mapping/rotation/route liên quan. |
| NOT_PROVEN, identity/parser lỗi | Event đóng. Dùng typed errors để viết một fixture tái hiện và sửa offline; không nới gate hoặc dựng lại transcript thật từ phỏng đoán. Nếu typed receipt chưa đủ thì nêu chính xác dữ kiện còn thiếu và cách thu thập redacted trước một quyết định mới. |
| DEVICE_NOT_FOUND/unauthorized hoặc identity APK sai | Dừng trước account; owner xử lý kết nối hoặc xác định artifact. Không đổi serial, install hoặc tự retry. |
| Expected thiếu/sai shape | Không launch. Hướng dẫn đúng vấn đề Process của cửa sổ owner; không xin lại key qua chat hoặc tạo lại provenance. Nếu event đã tạo/đóng, không tái sử dụng path. |
| Timeout/nonzero/receipt thiếu | NOT_PROVEN hoặc UNKNOWN theo dữ kiện; giữ evidence, không kết luận “chưa chạy” và không retry. |
| Hash/path/reparse sai | Dừng trước device, xác định đúng file/path sai. Không bỏ gate, tự đổi hash hoặc đổi tên event. |

Command exit 0 có thể tương ứng MATCH hoặc MISMATCH. Luôn đọc accountResult và typedOutcome. MATCH chỉ chứng minh predicate của account test, không chứng minh provider phục vụ được, không cấp RAW authorization.

## 7. Quy tắc chống lan man

- Đầu ra cần là kết quả account có evidence; số tài liệu, số commit hoặc số lần QA không phải tiến triển nghiệp vụ.
- Một event, một quyết định, một báo cáo terminal. Không tạo branch/release/checklist mới hoặc mở lại việc đã PASS.
- Không chạy test, rebuild, reinstall hoặc rà tất cả tài liệu khi byte không đổi và không có lỗi mới.
- Nếu gặp lỗi local tái hiện được thì sửa/kiểm chứng ngay trong cùng phạm vi offline; không chỉ xuất thêm yêu cầu “hãy sửa” và kết thúc. Điều này không cho phép chạy lại device.
- Không hứa chắc thiết bị sẽ PASS từ kết quả synthetic; không gọi STOP là lỗi tài khoản.
- Giữ toàn bộ key và dữ liệu app; không clear-data, uninstall, ghi settings hoặc thay credential.
- Không tự mở A4.3, provider, DB readback/write, RAW, P5 exit hoặc P6.

QA trước bàn giao: hash runtime/provenance khớp; event path chưa tồn tại; block PowerShell parse được (không execute); không trỏ event cũ; tách rõ quyết định đề xuất với quyền đã cấp; có đường xử lý MATCH/MISMATCH/NOT_PROVEN và scope counters.