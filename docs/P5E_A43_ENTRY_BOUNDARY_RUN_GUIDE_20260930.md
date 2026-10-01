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

Audit mặc định của entry chỉ đặt tên theo hash candidate. `D:\P5E-private\.p5e-a43-audit\68df8061….json` đã tồn tại từ lần chạy 2026-09-30 18:11, nên lần chạy thứ hai với tên mặc định dừng ở biên ngoài bằng `P5E_A43_AUDIT_ALREADY_EXISTS_STOP` trước prompt (case 5 và 6 của `scripts/test-p5e-a43-entry-console.ps1`). Vì vậy đoạn trên dùng `-AuditPath` theo DecisionId và ghi kết quả ngoài cùng vào `.p5e-a43-outer-<DecisionId>.log` (CreateNew, không chứa key). Không sửa hay xóa file audit cũ. Va chạm audit là một failure mode đã tái hiện, không phải nguyên nhân đã chứng minh của lần owner-window gần nhất: bản ghi lần đó đã ghi audit path theo DecisionId. Nguyên nhân lịch sử vẫn UNRESOLVED; outer log là cách có stderr thật nếu nó tái diễn.

Đã kiểm vs chưa kiểm của đoạn lệnh trên: dạng gọi `$out = & powershell.exe @args 2>&1` + `-AuditPath` riêng + path có khoảng trắng đã chạy trong console thật (case 3, 5, 6 của test console, input synthetic). Phần ghi log CreateNew vào `D:\P5E-private`, `-LocalPropertiesPath`/`-BuildToolsVersion` thật và PrivateRoot thật chưa được persist như evidence test; chúng chỉ xuất hiện trong lần chạy thử không lưu của phiên làm việc.

## Scope event A4.3 RAW trình owner (dữ liệu có sẵn, không phải approval)

| Mục | Giá trị (nguồn: helper `998A5E45…`, manifest `FCDC4747…`, checklist) | Mức kiểm |
|---|---|---|
| Phạm vi | Một event, một primary call `L1_RAW_DISCOVERY`; 0 schema-repair, 0 network retry, không RECONCILE/L2/L3/build/install | Pin trong helper |
| Chương/nguồn | selector `p5e-fresh-mercedes-vol5-20260911-01`, chapter key `001` (VOL5); RAW 23,814 B `a308210e…`, GLOSSARY 3,249 B `4bc3e2dd…`, DRAFT 26,462 B `64adecd8…`, PRONOUN 452 B `4947ff91…`; pack `497786e1…`, profile `beec03a4…`, binding `845976b3…`, run `8466b95d…` | Pin trong helper; chưa quan sát lại trên máy |
| Candidate | entry `68DF8061…`, helper `998A5E45…`, command `510F2A9A…`, manifest `FCDC4747…` | Đã kiểm hash tại HEAD |
| Thiết bị/app | serial `15e84958`, signature token `abebea4b`, production `4.17-p5e.11`/code207 APK `2CCBB844…`, certificate `47F31389…`, test APK `058BE851…`, schema v24 | APK/test APK khớp ở Before ngày 09-26; chưa kiểm lại |
| DB | Pin helper `3563F44B…`; hash DB đo lần cuối 09-26 là `2CC23078…` (17,784,832 B, bản sao host còn trong event 09-26) | **Lệch, chưa giải quyết**: Before dừng cứng `P5E_COLLECTOR_PRELIVE_DATABASE_HASH_MISMATCH` nếu lệch |
| Route | OpenRouter `openai/gpt-5.6-luna`, upstream `openai`, route fingerprint `23149071…`; key nhập trực tiếp ở prompt ẩn, chỉ trong bộ nhớ | Route từng `MATCH`; account kiểm trong event |
| Trần (giữ nguyên như đã pin) | input ≤ 100,000 token, output ≤ 4,096, tổng ≤ 104,096, ≤ USD 0.05, ≤ 120,000 ms; hiệu lực authorization sinh mới lúc owner duyệt | Pin trong helper |
| Cơ sở của trần | Compact wire xấu nhất 2,785 B dưới ngưỡng 3,584 B; input cũ khoảng 23.7k token; hai generation bị hủy trước đây tốn USD 0.0047 và 0.0037 | Số đo thật; tokenizer không chính xác |
| Rủi ro còn lại | Reasoning token tính vào output cap (lần 2,048 token trước compact wire bị `finish=length`); một lần gọi cũ chạy 179.7 s không có phản hồi | Không loại trừ được offline |
| Điều kiện dừng | Dừng collector trước dispatch: không provider call, event đã dùng. Sau dispatch mọi kết quả không accepted (truncated, lỗi, deadline, UNKNOWN): không retry, không redispatch, không repair | Contract trong helper/parent |
| UNKNOWN | Ghi UNKNOWN, không suy ra `$0`. Đọc metadata generation (không gọi model) chỉ trong phạm vi owner cho phép riêng; RECONCILE L1 có quyền và trần riêng | Chưa có quyền M2b |
| Evidence giữ | Outer log, audit theo DecisionId, EVENT_PLAN, collector outcome/command log, Before/After + DB export, hash receipt, readback attempt/lifecycle, identity/cost phản hồi, kết quả verifier; chỉ hash/đường dẫn, không key/payload thô | Theo contract |

Còn thiếu và không tự suy ra: (1) quyết định và approval mới cho event này (decision/event/receipt đã dùng vẫn đóng); (2) quan sát chỉ-đọc M0 để biết DB/APK hiện tại so với pins; (3) quyền cho M2b và M4, và quyền toàn chuỗi/tổng USD ba chương, chưa xin vì cần cost/thời gian thật từ M2. Chương 001 VOL5 là chương pilot đã có của A4.3, chưa phải bộ ba chương nghiệm thu.

Chỉ chạy một lần. Để launcher điều phối các phase; không chạy ADB phụ hoặc command copy bằng tay. Nếu STOP/timeout/unknown, giữ diagnostic/receipt/event nguyên trạng, không thử lại. Thu typed result/After/verifier được phép để đánh giá A4.3; exit0 hoặc QA offline không tự chứng minh P5 exit/P6. Không đưa key/digest/raw provider output vào chat.

Tài liệu này là hướng dẫn thực hiện và phạm vi trình owner, không tự tạo approval. Không cần tạo receipt hoặc nạp biến Process bằng các lệnh rời. Không cần quay lại review kế hoạch chung; next action là quyết định tại prompt và một lần thực hiện, sau đó đánh giá evidence thực tế.