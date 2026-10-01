# P5E — Tổng hợp lỗi, điểm dừng và nguyên nhân vòng lặp

Ngày tổng hợp: 2026-09-30
Phạm vi: hồ sơ P5E/A4.3 và các điểm chặn liên quan trong recovery v4.18.
Mục đích: gom các lỗi đã gặp trong nhiều tuần vào một tài liệu duy nhất để phân biệt việc đã sửa, việc chưa chứng minh và việc không được phép thử lại.

Tài liệu này chỉ dùng các trạng thái, mã lỗi, hash và đường dẫn hồ sơ đã được ghi nhận. Không chứa API key, endpoint, fingerprint, nội dung database, stdout/stderr native hoặc dữ liệu riêng tư từ thiết bị. Một mã STOP được nêu ở đây không tự động có nghĩa là key, thiết bị hoặc provider sai.

## Trạng thái hiện tại

Trạng thái điều khiển hiện tại là:

`OWNER_WINDOW_PRE_PROMPT_EXIT / EVIDENCE_INCOMPLETE / A4_3_OWNER_DECISION_NOT_RECEIVED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`

Lần gần nhất launcher được mở trong cửa sổ owner đã thoát trước prompt approval. Không tạo audit mới, reservation marker, owner root, receipt, command copy, child result hoặc event mới. Vì vậy provider/database/RAW chưa được đánh giá; không được ghi chúng là `0`.

Các hash packet hiện hành được canonical plan ghi nhận: manifest `FCDC4747…64A01D`, command `510F2A9A…E0D0D4`, helper `998A5E45…53C7BF`, guard `5F78F59F…C3DC3E`, exporter `813F6ED0…1CF99`, SQLite bridge `4598BFDF…A2111`, toolchain `C0AE7D43…D5C3C8`, certificate `47F31389…F3C155`, serial `15e84958`. Đây là các pin để đối chiếu, không phải giấy phép chạy.

## Các lỗi đã xảy ra

### 1. Ranh giới owner approval và cửa sổ PowerShell

- `P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP`: launcher dừng ở `APPROVAL`, chưa reservation, receipt, key hoặc child. Người dùng đã nhập literal sau khi launcher trả về dấu nhắc `PS`; input khi đó không còn đi vào process.
- Prompt test độc lập đã nhận đúng `APPROVE_ONE_FRESH_EVENT`, dài 23 ký tự, không có whitespace. Điều này chứng minh terminal có thể nhận input; chưa chứng minh attempt live trước đó đã nhận input.
- Một lần launcher child ghi `CHILD_INVOCATION_EXCEPTION / RemoteException`; không có child exit hoặc event result. Fixture PowerShell 5.1 đã tái hiện hiện tượng native child ghi stderr bị parent bắt thành `RemoteException`, nhưng không chứng minh nguyên nhân native của attempt thật.
- Một lần khác launcher dừng trước reservation với `CHILD_NOT_STARTED / STOP_REASON_UNRESOLVED`; catch đã che preflight/approval reason. Không có marker hoặc owner root.
- Lần owner-window gần nhất thoát trước prompt mà không sinh audit. Nguyên nhân chính xác của entry/argument boundary vẫn chưa được chứng minh.

Các lỗi vận hành launcher đã được xác định thêm trong quá trình review: wrapper từng trả exit `0` sau khi bắt lỗi child, hard-code một số counter trước khi action xảy ra, và dùng marker-root khác với root được dùng khi đọc evidence. Một command copy có đuôi `.txt` cũng không phù hợp với việc gọi bằng PowerShell `-File`, vốn yêu cầu script `.ps1`. Đây là lỗi quan sát và điều phối evidence, không phải bằng chứng provider hoặc account sai.

Hồ sơ: `P5E_A43_OWNER_KEY_INPUT_UNAVAILABLE_STOP_20260929.json`, `P5E_A43_LAUNCHER_CHILD_INVOCATION_EXCEPTION_20260929.json`, `P5E_A43_LAUNCHER_PRE_RESERVATION_STOP_20260930.json`, `P5E_A43_ENTRY_BOUNDARY_REPAIR_PROVENANCE_20260930.json` và trạng thái trong `EDITORIAL_RECOVERY_V4_18.md`.

### 2. Thiếu hoặc sai binding trước khi event được mở

Đã từng gặp `EXPECTED_PROCESS_VALUE_MISSING`, thiếu các binding receipt/command trong Process, thiếu `-JavaPath` trong synthetic invocation và thiếu các field kế hoạch owner. Một fixture cũng từng kiểm tra `OrderedDictionary` bằng `.Properties` của `PSObject`, tạo false missing-binding vì kiểu dữ liệu không được kiểm đúng; QA serialize sang `PSCustomObject` đã che lỗi này. Đây là lỗi chuẩn bị môi trường hoặc kiểm thử, không phải kết luận account sai.

Đường gọi dot-source từng làm clobber các tham số SDK/Java của caller trong `LibraryOnly` path. Việc sửa phải giữ nguyên argument của caller và truyền explicit path xuyên suốt; không được coi static parse là đủ.

Các fixture sau đó đã chứng minh invocation có đủ SDK, local properties, build tools, ADB, Java và apksigner; `EVENT_PLAN.json` được tạo và stderr rỗng. QA binding/atomicity/regression/DB lần lượt giữ các bằng chứng `262/262`, `28/28`, `175/175`, `56/56` trong các packet tương ứng.

### 3. Kiểm tra đường dẫn toolchain

Một bản sửa offline từng kiểm tra SDK bằng kiểu path sai (`Leaf` thay vì `Container`), làm dừng trước reservation. ADB, Java và apksigner cần kiểm tra `Leaf`; SDK cần kiểm tra `Container`. Gói sửa đã đạt `26/26` parent/pre-reservation và parse PowerShell 5.1.

Lỗi này cho thấy static pin check không đủ: phải kiểm thử hành vi của toàn parent launcher bằng fixture PS5.1.

### 4. Artifact/package/signature layout

- Collector từng trả `P5E_COLLECTOR_PACKAGE_VERSION_MISMATCH`; nguyên nhân đã được sửa bằng contract Android `versionName=4.17-p5e.11`, phân biệt với nhãn release v4.17-p5e.11.
- Parser signature ban đầu không nhận wrapper AOSP, dẫn đến `UNSUPPORTED_LAYOUT`; adapter current/past signer theo AOSP đã được thêm và QA offline pass.
- Một packet có đúng production candidate nhưng không có signature candidate nên dừng `UNSUPPORTED_LAYOUT`; không được đoán raw layout để bỏ qua gate.
- Identity runner từng trả số class/method không hợp lệ (`ACCOUNT_CHECK_CLASS_IDENTITY_COUNT_INVALID`, `ACCOUNT_CHECK_METHOD_IDENTITY_COUNT_INVALID`), sau đó contract được sửa và event route-corrected đạt `MATCH`.

Các lỗi này đã được xử lý trong các packet signature/layout, package-version và account-identity. Chúng vẫn là bằng chứng lịch sử; không được dùng để mở lại event cũ.

### 5. Account identity và route

Một event cũ trả `MISMATCH` ở mapping/route. Sau khi owner sửa lại route trong app điện thoại, event route-corrected trả `MATCH`, exit `0`, identity/terminal/capture/redaction pass. Kết quả này không chứng minh key sai ở event trước, cũng không chứng minh provider, RAW, DB write hay P5 exit.

Các event account trước đó có `NOT_PROVEN` do layout hoặc identity count. Không được gộp `MISMATCH`, `NOT_PROVEN` và `MATCH` thành một kết quả chung.

### 6. Collector ADB và package path

Các event live đã dừng fail-closed trước dispatch vì:

- `P5E_COLLECTOR_ADB_NONZERO` tại nhóm kiểm tra WAL/SHM/consistent read. Hai kết quả WAL/SHM exit `1` được phân loại là `ABSENT` hợp lệ; native reason của consistent-read exit `1` không có trong evidence redacted.
- `P5E_COLLECTOR_PACKAGE_VERSION_MISMATCH` trong event trước đó.
- `P5E_COLLECTOR_BINDING_TUPLE_MISMATCH` trong event 2026-09-26.
- `pm-path-production-before` dừng do wrapper/collector không nhận được output đầy đủ; không đủ bằng chứng để kết luận thiết bị, USB, package absence hoặc ADB authorization.

Event 2026-09-26 đã chạy đúng một lần, 20 read-only commands, không provider/credential/DB-write/RAW/redispatch. Main DB export device-host khớp hash trong evidence. Event đã đóng và không tái sử dụng.

### 7. Database readback và binding tuple

SQLite live trên Android không được dùng lại. Binary export, WAL/SHM semantics, snapshot stability và host readback đã được sửa offline; QA DB `56/56`. Binding tuple repair đạt QA `262/262`, regression `175/175` và Luna review không còn blocker/high.

Các kết quả này chỉ chứng minh toolchain host/fixture. Chưa có live Before/After của một event A4.3 thành công.

### 8. Preflight instrumentation và model/route history

Một preflight lịch sử có `INSTRUMENTATION_CODE: -1` và thiếu field `reconciliationCreated`; host từ chối evidence. Một artifact status-fix sau đó bị đánh dấu superseded vì emitter mapping vẫn thiếu. Một lần diagnostic ghi `providerMatch=true`, `modelMatch=false`, `endpointMatch=true`, `routeMatch=false`, rồi thiết bị mất kết nối trước readback đầy đủ. Đây là evidence quan sát, không phải acceptance.

Không được dùng các artifact superseded hoặc kết quả route/model lịch sử để chứng minh P5 hiện tại.

## Những gì đã sửa và có bằng chứng

- PowerShell 5.1 parent/pre-reservation fixture: `26/26 PASS`.
- Child invocation regression: `27/27 PASS`.
- Static launcher QA: `16/16 PASS`.
- Parent integration fixture: `21/21 PASS`, process exit `0/1` được kiểm tra trên synthetic child.
- Binding tuple: `262/262 PASS`.
- Regression: `175/175 PASS`.
- Host DB readback: `56/56 PASS`.
- Toolchain repair: `14/14 PASS`.
- Package version collector: `5/5 PASS`.
- Luna review của các gói gần nhất không còn blocker/high; các LOW/MEDIUM lịch sử đã được ghi riêng trong packet.

Các số PASS trên là evidence offline. Chúng không thay thế kết quả live A4.3.

## Những gì vẫn chưa chứng minh

1. Một launcher owner-interactive mới chạy trọn từ prompt, reservation, receipt, key prompt, child, event plan đến terminal result.
2. Một RAW/GLOSSARY predecessor được dispatch đúng allowlist.
3. REPORT_L1/receipt, Before/After/readback, verifier, budget và DB outcome của event A4.3 thành công.
4. Điều kiện P5 exit/L1 và các bước P6 L2/L3 trên dữ liệu thực.
5. Nguyên nhân native chính xác của các collector exit `1` lịch sử.
6. Nguyên nhân cụ thể của lần cửa sổ owner thoát trước prompt.

Vì vậy trạng thái đúng vẫn là `P5_EXIT_NOT_CLAIMED / P6_NOT_READY`.

## Phản biện nguyên nhân vòng lặp

Vòng lặp không xuất phát từ một lỗi duy nhất. Nó hình thành từ bốn điểm:

- QA kiểm từng helper, parser hoặc fixture nhưng nhiều lần chưa chạy đường tích hợp parent launcher giống cửa sổ owner.
- Mã catch và counter cũ che nguyên nhân gốc hoặc ghi giá trị trước khi action thật xảy ra; outer launcher còn có thể trả exit `0` sau caught failure. Một số báo cáo đã gần với false-green nếu đọc counter mà không xem evidence. Marker-root sai và command copy `.txt` cũng làm evidence bị đọc lệch hoặc child không được gọi đúng.
- Các event/receipt/packet single-use bị trộn trong cách diễn giải, dù contract yêu cầu đóng và không reuse sau STOP.
- Mỗi typed stop mở thêm một packet và một plan, trong khi mục tiêu sản phẩm vẫn thiếu đúng một live terminal result.

Phản biện an toàn: bỏ approval, bỏ identity, chạy lại event cũ hoặc suy ra provider/DB bằng việc không thấy file sẽ tạo kết quả không kiểm chứng được. Điều đó không phải cách rút ngắn tiến độ.

## Quy tắc kết thúc vòng lặp

- Một lỗi mới chỉ mở một work package; phải có fixture tái hiện, patch nhỏ, QA và kết luận trong cùng package.
- Không tạo packet mới nếu bytes không đổi.
- Không retry live, không reuse event/receipt/decision, không nhập key lần nữa sau khi process đã thoát.
- Không ghi counter live là `0` nếu action chưa tới boundary; dùng `UNKNOWN` hoặc `EVIDENCE_INCOMPLETE`.
- Chỉ chuyển tiếp sau khi có một terminal outcome rõ ràng. A4.3 PASS vẫn chưa tự động là P5 exit.
- Nếu owner-window entry vẫn lỗi, sửa đúng entry boundary offline và test parent end-to-end; không tiếp tục sửa parser, DB hoặc provider.

## Ưu tiên thực tế

1. Chốt chẩn đoán offline cho lần visible owner-window pre-prompt exit.
2. Chạy một fixture parent thật trên PowerShell 5.1 với cùng argument boundary và cửa sổ tương tác, không dùng key hoặc thiết bị.
3. Chỉ khi fixture chứng minh entry ổn định mới chuẩn bị một owner decision mới; không reuse audit/event cũ.
4. Nếu event live được mở và đạt terminal result, đánh giá Before/After/report/readback/verifier trước khi nói tới P5.
5. Chỉ sau P5 exit có bằng chứng mới bắt đầu P6; sau đó mới build/archive và device QA theo checklist release. Việc build phát triển theo yêu cầu riêng đã hoàn tất với `v4.18-dev.1` / code `208`; đó là artifact development được bàn giao, không phải bằng chứng P5 certification và không mở khóa P6.

## Tài liệu điều khiển

- `EDITORIAL_RECOVERY_V4_18.md` — canonical plan và next action.
- `BUILD_STATE.md` — trạng thái build/phase.
- `WORKSPACE_SNAPSHOT.md` — snapshot hiện tại.
- `docs/P5E_A43_PARENT_INTEGRATION_FINAL_PROVENANCE_20260930.json` và `docs/P5E_A43_PARENT_INTEGRATION_FINAL_REVIEW_20260930.md` — gói integration offline.
- `docs/P5E_A43_LAUNCHER_PRE_RESERVATION_STOP_20260930.json` — STOP trước reservation.
- `docs/P5E_A43_ENTRY_BOUNDARY_REPAIR_PROVENANCE_20260930.json` — repair entry boundary trước đó.
- `docs/P5E_A43_EVENT_RESULT_20260926.json` — event live đóng, không tái sử dụng.
- `docs/P5E_DB_HOST_READBACK_REPAIR_QA_20260925.json` — host DB QA.

Tài liệu này không thay đổi quyền chạy, không tạo owner approval và không mở event mới.

## Review quyết định hướng đi — 2026-09-30

Disposition 2026-10-01: owner đã chọn L1–L3 và bản cuối, yêu cầu viết lại plan. `EDITORIAL_RECOVERY_V4_18.md` đã được viết lại tại chỗ và là điều hành hiện hành; mục review này giữ căn cứ quyết định trước đó, không mở một plan cạnh tranh. Tài liệu nghiệm thu là `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md`; phương án C không được chọn. Không có runtime acceptance mới từ lần viết plan.

Owner yêu cầu chấm dứt vòng lặp build/review tốn thời gian. Review chỉ đọc source/evidence, dùng một sub-agent Luna cho ba lỗi quy trình và tiêu chí tiến độ; không chạy lại test, build, launcher, device hoặc provider. Branch thực tế `feature/v4.18-p5e-runner-repair-20260917`, HEAD `5e2c6120f4b188ef5c8f91a7696d7914e15365d7`; index và working tree đang khác nhau. Không commit candidate từ index cũ. APK code208 hiện có đã được đối chiếu BUILD_INFO và hash thực tế `DB3FE9056A3477FA18D4F9F44A18E1FCCA0F7195F95DD99C91F1BD60AA6FE465`.

### Kết luận có thể hành động

- Build đã thành công; A4.3 mắc ở đường kiểm chứng trước kết quả sản phẩm. Build thêm không có quan hệ nhân quả đã chứng minh với lỗi cửa sổ đóng trước prompt.
- Khoảng trống QA được xác nhận trực tiếp: `scripts/test-p5e-a43-entry-boundary.ps1:25-28` tạo process ẩn và redirect output; các probe dùng `-NonInteractive`, LibraryOnly hoặc thiếu arguments. `scripts/test-p5e-a43-pre-reservation-integration.ps1:210-211` thay ApprovalReader/KeyReader bằng seam; dòng 574 ghi candidateExecuted=false. Các bài này hữu ích trong phạm vi của chúng, nhưng không chứng minh đường mở cửa sổ và nhập liệu thực tế. Chưa xác định nguyên nhân chính xác của lần thoát cửa sổ thật; không kết luận lỗi quoting, provider hay thiết bị.
- Entry point kiểm pin, dot-source, khôi phục arguments và tạo contract trước khi gọi launcher có diagnostic. Cần quan sát được lỗi ở ranh giới ngoài này; thêm test helper phía trong không giải quyết khoảng trống đó.
- Nhiều banner current/historical và packet lặp tăng chi phí đọc, dễ chọn nhầm next action. Snapshot nên giữ trạng thái hiện tại gọn, lịch sử giữ trong evidence/Git. Không cần thêm cơ chế receipt cho việc review thông thường.
- A4.3 RAW PASS chưa phải hoàn thành: P5.4 còn cần L1 report/receipt được lưu và xác minh; P6 cần ít nhất ba chương L1–L3 hoàn tất hoặc dừng vì xung đột nội dung đã chứng minh; P7 còn regression/build/device QA. Không hứa một lần mở cửa sổ sẽ hoàn thiện v4.18.

### Ba phương án để owner lựa chọn

| Phương án | Kết quả cần đạt | Chi phí/ràng buộc |
|---|---|---|
| A — một lượt sửa cuối có giới hạn trên đường hiện tại (khuyến nghị trước) | Tái hiện lỗi launch offline; xác nhận cách mở process/arguments/console giống thật, có terminal result; sau đó mới xét một event live mới | Tận dụng pin/artifact hiện tại; có giới hạn dừng rõ, không tiếp tục sửa wrapper vô hạn |
| B — kiểm chứng trên môi trường thử riêng | Dùng dữ liệu thử và môi trường Android tách biệt để chứng minh một chương từ app đến kết quả lưu được, sau đó ba chương | Cần owner chấp thuận thay đổi phạm vi kiểm chứng và chuẩn tương đương; có thể cần build/cấu hình riêng. Không tự kế thừa acceptance code207 và không được bỏ kiểm identity/budget/preservation |
| C — thu hẹp bản giao | Giao phần Translation đã được xác minh; hoãn Editorial và ghi rõ chưa hoàn thành | Cần owner đổi phạm vi release; code208 chưa device-QA nên không tự coi là bản ổn định. Không xóa hoặc viết lại bằng chứng thất bại |

### Giới hạn thực thi đề xuất, chưa phải live authorization

1. A chỉ có một đợt chẩn đoán/sửa, tối đa 60 phút active work hoặc hai chu kỳ patch–targeted test, điều kiện nào đến trước. Đây là ngưỡng quyết định phương án, không phải cam kết sửa xong trong một giờ. Không build APK khi chưa có thay đổi Android cần kiểm chứng.
2. Điều kiện đạt: đúng ranh giới mở process/arguments/console trên PS5.1; dữ liệu giả, child giả và không có khả năng gọi provider/device; quan sát được prompt thử và terminal result; kiểm cả đường lỗi giữ được nguyên nhân. Không tự động nhập approval thật, không dùng key thật. Nếu vẫn chỉ có PASS của helper, coi là chưa đạt.
3. Mỗi chu kỳ phải tạo hiểu biết mới: tái hiện được lỗi, loại trừ một giả thuyết bằng bằng chứng, hoặc sửa được lỗi đã tái hiện. Nếu không đạt trong giới hạn, dừng hướng A và trình B/C; không sinh thêm packet/review để đổi tên cùng blocker. Lỗi local vẫn là FAILED_REPAIRING, không giả thành BLOCKED_EXTERNAL.
4. Khi offline đạt, giữ đúng điều kiện one-use hiện tại cho event live mới. Nếu dispatch không rõ, không retry. Đánh giá RAW, persisted evidence và Before/After trước; tách bước RECONCILE/L1 theo phạm vi được phép. Mỗi mốc phải có đầu ra sản phẩm, không dùng số lượng test/commit/tài liệu làm phần trăm hoàn thành.
5. Luna chỉ làm việc nhỏ có đầu ra cụ thể, tối đa một lượt cho mỗi thay đổi đáng review; không gọi agent lặp lại để duyệt cùng bytes. Giữ một báo cáo này và snapshot, không tạo release track hay checklist mới.

### Câu hỏi chốt hướng

1. ĐÃ CHỐT: owner ưu tiên hoàn thiện L1–L3 để có bản biên tập cuối; không cần bàn giao riêng output từng lượt. Translation-only không đáp ứng yêu cầu. Dữ liệu trung gian cần cho runtime/phục hồi/kiểm chứng vẫn giữ nội bộ; không xóa evidence cũ.
2. Nếu hướng A hết giới hạn, có chấp nhận môi trường Android/dữ liệu thử riêng theo B? Acceptance mới phải chứng minh tương đương điều gì với app mục tiêu?
3. Ba chương đại diện nào là bộ nghiệm thu cố định, và trần chi phí tổng cho pilot là bao nhiêu? Ngân sách một RAW event hiện tại không phải ngân sách cho toàn bộ L1–L3.

Mặc định đề xuất A có giới hạn rồi B nếu cần; đây là khuyến nghị, chưa thay đổi canonical scope hoặc quyền live. Không thể hứa hoàn tất dứt điểm trước khi có bằng chứng chạy sản phẩm, nhưng có thể chấm dứt việc tiếp tục cùng phương pháp mà không có tiêu chí dừng.

Tiêu chí sản phẩm theo câu trả lời của owner: một chương đi hết L1–L3 và tạo bản đã biên tập được lưu, mở lại và xuất ra sau kiểm tra cuối; sau đó xác minh trên bộ ba chương đại diện theo kế hoạch. Output L1/L2 không bắt buộc thành file giao riêng. Phương án C không được chọn; A/B chỉ là phương tiện đạt cùng kết quả cuối.

## Vòng chẩn đoán entry owner-window 1/2 — 2026-10-01

- Bộ đếm: active ≈ 15/60 phút; 1/2 vòng (1 reproducer + 1 test mới; không sửa entrypoint thêm).
- Giả thuyết: switch `-LibraryOnly` của các thư viện dot-source ghi đè `$LibraryOnly` trong cùng scope entrypoint, nên `if ($LibraryOnly) { return }` thoát im lặng trước audit.
- Reproducer: Windows PowerShell 5.1 sạch, console thật có input (`Start-Process` + `WriteConsoleInput`), đường dẫn có khoảng trắng, đúng argument của run guide, PrivateRoot tạm, chỉ gõ literal sai. Không key/ADB/provider.
- Kết quả:
  - Bytes staged `CC01C33F…8E23` (stdin redirect và console thật): exit 0, stdout/stderr rỗng, không audit, không prompt — chính là triệu chứng pre-prompt exit và là false exit 0. Giả thuyết được xác nhận.
  - Bytes working-tree `68DF8061…70BA`: tới prompt (audit tạo trước prompt), nhận input trong console thật, trả JSON `P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP`, exit 1.
  - Outer failure (thiếu `-JavaPath`): stderr giữ `P5E_A43_INTEGRATION_CONTRACT_MISSING_STOP`, exit 1, không audit.
  - Redirected stdin + prompt: treo ở `Read-Host` (không phải console); `-NonInteractive`: typed `UNKNOWN_STOP/PSInvalidOperationException`, exit 1, audit ghi (mất message gốc; chấp nhận, không sửa vì đổi mã typed buộc re-pin file parent).
- Kết luận: tested bytes = 68DF (đã có trong working tree); staged CC01 là bytes cũ lỗi, không được commit/dùng. Owner phải chạy 68DF, không dùng launcher/hash cũ.
- Test mới: `scripts/test-p5e-a43-entry-console.ps1` (2 case PASS, report `evidence/p5e-a43-parent-integration-20260930/P5E_A43_ENTRY_CONSOLE_QA.json`).
- Không chứng minh: RAW/L1/P5 exit; đường owner thật vẫn cần một lần chạy có thẩm quyền.
