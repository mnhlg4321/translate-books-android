# P5E — yêu cầu local repair tiếp theo (SQL/collector/golden JVM)

Ngày: `2026-09-16`
Baseline: `31a09d02323cd317a9089411901849e6d51b8f66`
Status: `LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED / NO_APK_BUILD / NO_DEVICE / NO_PROVIDER / P6_NOT_READY`

## Mục tiêu và giới hạn

Request này thay thế kế hoạch local trước đây sau khi kiểm tra behavioral đã
phát hiện lỗi trong đường query SQLite → parser. H1 (runtime helper hash gate),
F2 transport, contract report/receipt và các phần H3/H4 đã có bằng chứng source
hoặc fixture riêng; không reimplement chúng từ đầu. Phạm vi mới chỉ sửa và kiểm
chứng các lỗi đã tái hiện:

- SCHEMA query dùng `SELECT user_version` như một cột, gây lỗi SQLite;
- LINEAGE query phát tag + 11 giá trị trong khi parser yêu cầu 17 cột;
- ATTEMPT dùng phép nối chuỗi với blob NULL, có thể biến cả dòng CLAIMED/
  RECOVERY thành NULL;
- thiếu đường kiểm chứng behavioral từ query thật qua collector/parser tới
  verifier;
- golden JVM test của production serializer chưa được thực thi.

Không được chạy APK build, `connectedDebugAndroidTest`, ADB, instrumentation,
device readback, credential, provider, RAW, authorization, install, uninstall,
clear-data, force-stop, retry, RECONCILE, tag, merge hoặc release. INPUT query
hiện đúng: tag + 6 giá trị = 7 cột; không mở lại lỗi INPUT.

## Quy trình bounded (48 bước)

### A. Resume và khóa phạm vi

1. Làm việc trong `D:\App Translate Books\App Translate Books-translation-profile`.
2. Đọc `BUILD_STATE.md` → `WORKSPACE_SNAPSHOT.md` → xác nhận Git/status;
   đọc `GIT_WORKFLOW.md`, `DEVELOPMENT_WORKFLOW.md` (đúng thứ tự 14 bước)
   và `EDITORIAL_RECOVERY_V4_18.md` trước khi sửa.
3. Xác nhận branch/HEAD/status; baseline đầu vào là ancestor, không reset hoặc
   làm sạch thay đổi của agent khác.
4. Xác nhận request này là tiếp nối P5E hiện hành, không tạo phase/release/
   checklist mới.
5. Ghi current state là `LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED`,
   `A4_3_NOT_ISSUED`, `RAW_NOT_RUN`, `P6_NOT_READY`.
6. Giữ nguyên H1 runtime hash gate, F2 qualification, artifact contract và
   manifest/APK pins; không sửa hash chỉ để làm xanh báo cáo.
7. Kiểm tra không có thay đổi ở production route/model, schema migration,
   canonical pack/profile/authority hoặc live command scope.
8. Chỉ dùng SQLite cục bộ, Java/JVM test và mock host data; mọi fixture đặt ở
   thư mục tạm duy nhất ngoài artifact/release directories.

### B. Tái hiện RED bằng nguồn thật

9. Dùng chính `Get-P5EConsistentDatabaseReadback` trong
   `scripts/p5e-raw-live-supervisor.ps1`, không sao chép query sang
   implementation khác để kiểm tra.
10. Tạo SQLite fixture tạm từ DDL hiện hành của project và đặt
    `PRAGMA user_version` bằng cú pháp SQLite hỗ trợ chính thức.
11. Chạy riêng SCHEMA statement trên fixture; lưu stderr/exit code ở local
    evidence và xác nhận lỗi `no such column: user_version` trước sửa.
12. Chạy riêng LINEAGE statement trên fixture; đếm tag và giá trị thực tế,
    xác nhận output có 12 cột trong khi parser hiện bắt buộc 17.
13. Chạy ATTEMPT với `CLAIMED`, `RECOVERY_REQUIRED`, `COMMITTED` và blob NULL/
    non-NULL; xác nhận concat NULL làm dòng output rỗng hoặc NULL.
14. Chạy INPUT fixture để xác nhận tag + 6 giá trị, tổng 7 cột, phù hợp parser;
    ghi rõ đây là control PASS.
15. Ghi command, SQLite version, fixture schema hash, output hash và kết quả
    RED; không dùng regex tìm tên hàm làm bằng chứng behavioral.

### C. Sửa query và null encoding

16. Sửa SCHEMA bằng cú pháp pragma/query có thật và tương thích sqlite3 runtime;
    không chèn schema version cố định từ PowerShell.
17. Đối chiếu LINEAGE với consumer/contract để chốt số field cần thiết; sửa
    query hoặc parser cho khớp, không mặc định 17 là đúng và không padding giả.
18. Đặt field-to-source map cho toàn bộ LINEAGE field order trong contract.
19. Sửa ATTEMPT serialization để NULL là sentinel có nghĩa (ví dụ `NULL`),
    không biến thành blank và không biến thành `0`.
20. Parser phải giữ unknown/NULL khác với số 0; null recovery không được thành
    zero counts hoặc acceptance.
21. Giữ exact bytes report/receipt khi có dữ liệu; collector không canonicalize
    hoặc thay đổi blob.
22. Giữ query read-only, transaction/WAL behavior và allowlist operation; không
    thêm write, cleanup hoặc schema repair.
23. Chạy `git diff --check`, bảo đảm diff chỉ chạm helper/test/contract cần thiết.

### D. Behavioral collector fixture bắt buộc

24. Chạy source thật qua sqlite3 trên fixture `UNUSED` với bốn INPUT rows và
    không attempt; parser phải trả lineage zero đúng kiểu.
25. Chạy fixture `CLAIMED` với report/receipt NULL và lifecycle đang mở; phải
    giữ ATTEMPT row, status và unknown blob lengths.
26. Chạy fixture `RECOVERY_REQUIRED` với cả report/receipt NULL,
    `metrics_json=''` (cột NOT NULL) và reason; phải giữ row và typed recovery.
    Fixture chỉ một blob NULL là negative inconsistency riêng; metrics NULL
    vi phạm DDL không được dùng làm happy path.
27. Chạy fixture `COMMITTED` với report/receipt thật; parser đọc đủ field,
    byte length/hex và verifier kiểm exact persisted bytes.
28. Chạy fixture có reconciliation/history rows để xác nhận LINEAGE counts đúng
    từng nguồn, không gộp hoặc bỏ sót join.
29. Chạy malformed-column fixtures: thiếu/thừa field, duplicate LINEAGE,
    invalid integer, invalid UTF-8 và sentinel sai; parser fail-closed.
30. Chạy Before và After mock cùng event/run identity; verifier từ chối
    cross-event path và chấp nhận same-event path.
31. Chạy mock collector failure trước launch; typed stop phải có launch `0`,
    không tạo redispatch suggestion.
32. Chạy mock timeout/process-death sau dispatch; After chỉ đọc cùng event,
    outcome `UNKNOWN`/`RECOVERY_REQUIRED`, không relaunch.
33. Xuất evidence gồm command, fixture identity, schema hash, query output hash,
    parser/verifier result và launch/provider/device counts.

### E. Golden JVM → host bridge

34. Thực thi targeted
    `EditorialP5PilotExecutionBoundaryTest` trên JVM bằng production serializer;
    đây là test/unit task, không yêu cầu APK build.
35. Sinh report/receipt bytes từ production serializer thật, không dùng synthetic
    producer làm nguồn golden.
36. Đưa bytes golden qua host validator với fixture identity truyền rõ như
    parameter; validator dùng đúng production contract source hash.
37. Assert valid pair được chấp nhận; wrong manifest, binding, bundle,
    predecessor, swapped pair, one-byte mutation, extra/missing field, BOM,
    invalid UTF-8 và noncanonical bytes bị từ chối.
    Cả hai blob cùng sai bundle/predecessor vẫn phải reject so với expected
    fixture identity; pair equality riêng không đủ.
38. Ghi command, JVM/runtime, test count, serializer source hash, generated-byte
    hashes, fixture identity và validator result; `executed=false` không được
    giữ sau khi bước này chạy thành công.
39. Nếu golden fail, giữ `LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED`, sửa đúng
    boundary rồi chỉ chạy lại test/fixture bị ảnh hưởng.

### F. Gate, provenance và bàn giao

40. Chỉ tuyên bố behavioral local PASS khi query thật, parser, collector,
    verifier và golden JVM đều có output/result cụ thể trên cùng final hashes;
    static names hoặc synthetic mutation riêng không đủ.
41. Cập nhật result JSON với trạng thái tách biệt: query/parser behavioral,
    golden JVM, host contract, owner readiness và dispatch readiness.
42. Provenance table bind helper, collector, serializer, contract, query fixture,
    test command, source commit và output hashes; không ghi credential/raw
    endpoint/account fingerprint.
    Refreeze theo thứ tự: final helper hash → command helper pin → command
    hash → packet/provenance. Nếu contract đổi thì cập nhật contract hash/pin
    trước helper hash. Chỉ chạy lại checks bị ảnh hưởng bởi lần đổi bytes cuối;
    không tự sửa manifest scope hoặc duy trì helper hash cũ sau sửa source.
43. Owner F1 expected fingerprint và account/readback/RAW permission vẫn là bước
    sau; local PASS không tạo authorization và không làm packet dispatch-ready.
44. Giữ stop conditions: mismatch pin/schema/identity, fixture ambiguity,
    validator failure, unknown prior outcome, collector/redaction failure đều
    dừng; không retry/refresh ID/redispatch.
45. Cập nhật `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md` và current P5E banners
    chỉ với evidence mới; historical evidence giữ nguyên.
46. Chạy secret scan, `git diff --check`, targeted JVM/fixture checks và xác nhận
    không có APK/device/provider action.
47. Báo cáo riêng local behavioral gate, owner-decision readiness và live/P5/P6
    readiness; không dùng “hoàn tất” chung.
48. Nếu chỉ sửa tài liệu hoặc test local và không tạo artifact, không chạy APK
    build, không tạo artifact/backup giả và không tạo commit rỗng.

## Provenance tối thiểu

Baseline hashes đầy đủ nằm trong `P5E_READINESS_AUDIT_20260916.md`; đây là
provenance đầu vào, không phải approval cho helper/command sau sửa.

| Field | Source bắt buộc | Trust/status hiện tại |
|---|---|---|
| helper bytes/hash | `scripts/p5e-raw-live-supervisor.ps1` | source thật; phải refreeze sau sửa |
| command pin/hash | `P5E_RAW_AUTHORIZATION_COMMAND.txt` | phụ thuộc helper hash mới; pending refreeze |
| serializer bytes | `EditorialP5PilotExecution` + targeted JVM test | source present, test pending |
| query output | sqlite3 fixture + exact query source | behavioral RED đã repro; repair pending |
| parser/verifier result | parser/validator source và output hashes | pending behavioral rerun |
| fixture identity | DDL/schema/data fixture hash và event label | local synthetic/mock only; không phải device fact |
| owner F1 fingerprint | trusted owner record, không phải actual runtime | chưa cung cấp; live pending |
| RAW/P5/P6 status | durable live evidence và canonical gates | `RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY` |

Owner packet sau local PASS phải để rõ `NOT_PROVIDED/NOT_APPROVED` cho:
người chịu trách nhiệm, nguồn expected fingerprint độc lập, bằng chứng quyền
truy cập nguồn đó, thời điểm xác minh, scope endpoint/account và quyết định
riêng cho memory-only account check, read-only collection, một RAW/GLOSSARY
dispatch. Owner review đúng hashes cuối. Không đưa giá trị fingerprint thực
vào Git/chat/log; không lấy actual vừa đọc làm expected; không có trusted
source thì dừng F1, không dựng provenance thay owner.

## Các sự cố cũ phải được giữ trong acceptance

| Sự cố/giới hạn | Ràng buộc cho lần làm tiếp |
|---|---|
| code196 mất DB; A3.2 preservation chưa chứng minh | Không connected installer/reset/uninstall; không nâng RECONSTRUCTED_ONLY thành restored; không suy preservation từ counts. |
| A3.2 model mismatch; A4 emitter thiếu field | Giữ route/model và strict emitter/parser pins; không tự remediation hay rerun instrumentation. |
| code191 timeout, response/cost chưa rõ | UNKNOWN vẫn UNKNOWN; không suy no-call/$0; không retry/ID refresh, same-event read-only recovery sau quyền riêng. |
| Transport/shell boundary và helper drift | Giữ F2/H1; chạy host tests bằng đúng powershell.exe của command, ghi runtime; kiểm mutation hash/path/reparse nếu code liên quan đổi. |
| Collector/readback hoặc transaction evidence thiếu | Không đặt transactionSemanticsPassed=true chỉ từ source hash; nối tới test execution phù hợp hoặc UNKNOWN. Tool availability/device SQLite vẫn chưa quan sát. |
| Budget/egress và reuse approval | Giữ timeout host 240000 ms, budget/RAW-GLOSSARY-only, DRAFT/PRONOUN hidden và no-redispatch theo manifest; A4.2 approval đã consumed. |

Đây là regression scope cho local fixtures và điều kiện bàn giao, không phải
chỉ thị chạy lại các sự cố trên device. Sau local PASS chỉ chuẩn bị packet;
RAW thành công cũng chưa mở P6: RECONCILE/L1 và P5 exit theo canonical còn riêng.

## QA phản biện độc lập (hai vòng)

Vòng 1 kiểm tra implementation: chạy source query thật trên mọi fixture ở bước
24–29, đối chiếu số cột với parser, kiểm NULL/sentinel, byte exactness và
golden JVM bridge. Vòng 2 chỉ đọc evidence của vòng 1 rồi cố tình đổi event,
bundle/predecessor, field count, NULL/blank/0, timeout và prior outcome; phải
reject ở boundary gần nhất. Hai vòng phải dùng cùng final helper/command/
serializer hashes và ghi command/result cụ thể.

Stop ngay nếu query còn lỗi SQLite, số cột không khớp, NULL bị nuốt hoặc đổi
thành `0`, metrics vi phạm schema, report/receipt sai expected identity không bị
reject, golden test chưa chạy/fail, fixture identity thiếu, validator output
không reproducible, hash refreeze không khớp, evidence bị redact sai, hoặc mock
timeout dẫn tới retry/refresh ID/redispatch. Stop cũng áp dụng khi phát hiện
credential/raw endpoint/account fingerprint trong output. Không chuyển lỗi
local thành owner blocker; sửa local và chạy lại đúng checks bị ảnh hưởng.

## Điều kiện kết thúc request

Request chỉ GREEN khi bước 16–40 có evidence thật, golden JVM đã chạy,
null/recovery rows được giữ đúng, query/parser schema khớp, và verifier kiểm
được bytes do production serializer sinh ra. Owner vẫn `PENDING`, A4.3 vẫn
`NOT_ISSUED`, RAW chưa chạy và P6 vẫn `NOT_READY`.
