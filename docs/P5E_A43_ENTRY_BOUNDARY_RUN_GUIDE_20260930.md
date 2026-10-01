# A4.3 — bước thực hiện sau sửa cửa vào launcher

Bản sửa này giữ nguyên toàn bộ tham số qua dot-source (LibraryOnly, Execute, SDK/Java và các binding), và copy command thành .ps1 để PowerShell 5.1 thực thi. Không có live event trong lượt sửa. Đừng dùng launcher/hash cũ.

Candidate SHA-256: 68DF8061CECB22D64AA4B85245714AC3AEE1895EBC8846F1BF51FC13D2E170BA

Mở Windows PowerShell nhìn thấy được. Đoạn sau kiểm hash và mở đúng launcher một lần. Launcher hỏi quyết định cho đúng một event A4.3: serial 15e84958, Before/After read-only, account memory-only, fresh authorization, tối đa một RAW/GLOSSARY provider call, USD0.05 và DB allowlist theo manifest FCDC4747D54075B0518A92D837FC74C67D7F3804F9BAA4EFFC3CB34C7764A01D. Không retry/fallback/RECONCILE/build/install/P6. Nhập APPROVE_ONE_FRESH_EVENT chỉ khi đồng ý; prompt có thể che ký tự. Sau đó nhập key trực tiếp vào prompt hidden, không gửi chat.

```powershell
$p5eCandidate = 'D:\App Translate Books\scripts\p5e-a43-pre-reservation-launcher-entrypoint.ps1'
$p5eHash = '68DF8061CECB22D64AA4B85245714AC3AEE1895EBC8846F1BF51FC13D2E170BA'
if ((Get-FileHash -LiteralPath $p5eCandidate -Algorithm SHA256).Hash -cne $p5eHash) { throw 'CANDIDATE_HASH_MISMATCH_STOP' }
$p5eDecision = 'p5e-a43-owner-' + [Guid]::NewGuid().ToString('N')
$p5eAudit = Join-Path 'D:\P5E-private\.p5e-a43-audit' ($p5eDecision + '.json')
$p5eOuter = Join-Path 'D:\P5E-private' ('.p5e-a43-outer-' + $p5eDecision + '.log')
$p5eArgs = @(
    '-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass',
    '-File', $p5eCandidate, '-Execute',
    '-RepoRoot', 'D:\App Translate Books',
    '-PrivateRoot', 'D:\P5E-private',
    '-AndroidSdkPath', 'C:\Users\ADMIN\AppData\Local\Android\Sdk',
    '-LocalPropertiesPath', 'D:\App Translate Books\local.properties',
    '-JavaPath', 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe',
    '-BuildToolsVersion', '35.0.0',
    '-ExpectedCandidateSha256', $p5eHash,
    '-AuditPath', $p5eAudit,
    '-DecisionId', $p5eDecision
)
$p5eOut = & 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe' @p5eArgs 2>&1
$p5eExit = $LASTEXITCODE
$p5eBytes = [Text.UTF8Encoding]::new($false).GetBytes('EXIT=' + $p5eExit + [Environment]::NewLine + ($p5eOut | Out-String))
$p5eStream = [IO.File]::Open($p5eOuter, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
try { $p5eStream.Write($p5eBytes, 0, $p5eBytes.Length) } finally { $p5eStream.Dispose() }
$p5eOut
'OUTER_EXIT=' + $p5eExit
```

Audit mặc định của entry chỉ đặt tên theo hash candidate. `D:\P5E-private\.p5e-a43-audit\68df8061….json` đã tồn tại từ lần chạy 2026-09-30 18:11, nên lần chạy thứ hai với tên mặc định dừng ở biên ngoài bằng `P5E_A43_AUDIT_ALREADY_EXISTS_STOP` trước prompt (case 5 và 6 của `scripts/test-p5e-a43-entry-console.ps1`). Vì vậy đoạn trên bắt buộc `-AuditPath` theo DecisionId và ghi kết quả ngoài cùng vào `.p5e-a43-outer-<DecisionId>.log` (CreateNew, không chứa key). Không sửa hay xóa file audit cũ. Đây là cơ chế đủ để giải thích lần thoát trước prompt; chưa phải bằng chứng stderr thực tế của lần đó vì không có stderr nào được lưu.

Chỉ chạy một lần. Để launcher điều phối các phase; không chạy ADB phụ hoặc command copy bằng tay. Nếu STOP/timeout/unknown, giữ diagnostic/receipt/event nguyên trạng, không thử lại. Thu typed result/After/verifier được phép để đánh giá A4.3; exit0 hoặc QA offline không tự chứng minh P5 exit/P6. Không đưa key/digest/raw provider output vào chat.

Tài liệu này là hướng dẫn thực hiện và phạm vi trình owner, không tự tạo approval. Không cần tạo receipt hoặc nạp biến Process bằng các lệnh rời. Không cần quay lại review kế hoạch chung; next action là quyết định tại prompt và một lần thực hiện, sau đó đánh giá evidence thực tế.