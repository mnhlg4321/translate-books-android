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

> Record lịch sử của vòng 1; giới hạn kết luận và next action đã được hiệu chỉnh trong mục QA ngay dưới. Reproducer CC01 không chứng minh nguyên nhân latest owner attempt đã pin 68DF; không thực hiện chỉ dẫn owner-run dưới đây trước offline closure. Tiêu chí hiện hành yêu cầu ba final thành công, không tính conflict stop thay một final.

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

## QA và phản biện bàn giao Claude — 2026-10-01

Phạm vi: primary kiểm tra source/diff/Git tree/report; Luna chỉ audit test console mới, bốn findings, không thực thi. Baseline quan sát `a4051e00c648c659589f1cccde4ae2a475a1bf17`, branch `feature/v4.18-p5e-runner-repair-20260917`; remote lúc kiểm tra khớp HEAD này. Không sửa runtime, không chạy lại launcher/suite/build, không đọc private payload. Phần này làm rõ giới hạn kết luận vòng 1 ở trên, không sửa evidence bất biến.

### Kết luận nguyên nhân không tiến triển

1. **Mã đã test chưa thành bộ mã có thể bàn giao từ Git.** `git ls-tree HEAD` có `scripts/test-p5e-a43-entry-console.ps1`, nhưng thiếu entrypoint, parent launcher, `test-p5e-a43-pre-reservation-integration.ps1` và `test-p5e-a43-entry-boundary.ps1`. Các file này có ở local index/working tree. Vì vậy clone đúng HEAD cũng chưa chạy được chuỗi QA. Bàn giao tài liệu trước đây chưa phải bàn giao đủ implementation; không thể yêu cầu Claude tái lập từ GitHub bằng chỉ một lệnh test.
2. **Kết luận nguyên nhân vượt quá bằng chứng.** BUILD_STATE ghi hash 68DF đã khớp trước latest owner-window failure, còn vòng 1 tái hiện triệu chứng trên CC01. Đó là hai phiên bản khác nhau. Diff chứng minh bản cũ dùng `$LibraryOnly` sau dot-source, bản working bảo toàn intent/arguments; đây là defect cụ thể của bản cũ. Nhưng chưa có chain evidence chứng minh process thất bại gốc chạy CC01. Không được viết “68DF chưa từng được owner chạy” hoặc “đã xác định root cause của lần lỗi gốc”. Chỉ có thể nói lần owner attempt được ghi nhận đã pin 68DF, execution/output thực tế không đủ rõ.
3. **Test mới có tiến bộ nhưng chưa phủ cả tiêu chí được đề ra.** Test có console input injection và typed terminal rejection; nó chỉ thử WRONG_LITERAL và thiếu JavaPath. Test không có CC01 variant, không ghi nhận prompt được hiển thị cho owner, không chạy approval→synthetic child success. `-WindowStyle Hidden` có thể vẫn có console; không được kết luận test vô giá trị, cũng không được gọi đó là bằng chứng cho đúng cửa sổ owner-visible.
4. **Thông tin lỗi bị mất qua nhiều lớp.** Test mới ghi JSON tổng hợp hai case rồi xóa temporary output/audit, không lưu observed per-case facts. Entry pin checks/dot-source/contract construction nằm trước parent diagnostic. Vì vậy có trường hợp wrapper/test báo PASS mà người xử lý lỗi sau không có dữ kiện để xác định process dừng ở đâu. Không cần log secret: chỉ cần path/hash, invocation shape không nhạy cảm, stage, exit/typed cause và bounded redacted error.
5. **Handoff và state dẫn tới lặp lại hoặc nhảy bước.** HANDOFF cũ ghi 0/2 vòng trong khi round record ghi 1/2; canonical nói chẩn đoán còn snapshot chuyển thẳng tới owner run guide. Mỗi agent có thể chọn một next action khác. Cập nhật đồng bộ cùng một kết luận và giữ bộ đếm đã dùng; không khởi động lại diagnosis vì đổi agent.
6. **Tiến độ tập trung vào harness, còn deliverable sản phẩm chưa có.** P5E.9/P5.4 vẫn thiếu accepted RAW và persisted/reopened L1 report/receipt. L1 request enum chỉ có RAW/RECONCILE; L2/L3 có contracts/validators nhưng UI vẫn locked. Build code208, số test và số tài liệu không đóng những khoảng thiếu này. Đây là phân tích điểm nghẽn từ code/evidence, không phải số liệu đo token hoặc thời gian toàn dự án.

### Fact / chưa chứng minh

| Kết luận | Mức chứng minh |
|---|---|
| Working entrypoint hash = 68DF8061…70BA, trùng report console | Đã hash/readback trong lượt QA này |
| Latest console report ghi PASS/2 cases | Report hiện có; không rerun trong lượt review |
| CC01 có lỗi intent bị library scope ghi đè | Diff/source phù hợp giải thích; reproduction CC01 chỉ được ghi trong narrative, test đã commit không chứa case đó |
| Chính CC01 gây latest owner-window failure | Chưa chứng minh; mâu thuẫn với hash 68DF được ghi trước launch |
| Bản 68DF đã qua valid approval→child trong owner console | Chưa chứng minh bằng test console này |
| Không thể đi live vì một lỗi code mới đã được chứng minh | Chưa kết luận; phần thiếu là độ phủ/bằng chứng, không tự phát minh thêm blocker runtime |
| Clone HEAD có đủ source để tái lập | Không; thiếu các file runtime/test nêu trên |

### Hướng giải quyết có giới hạn để Claude thực hiện

**Một nhóm việc còn lại: đóng khả năng tái lập của entry boundary offline.** Không viết lại plan hoặc launcher framework.

1. Đối chiếu HEAD/index/working bytes và dependency pins. Chuẩn bị commit đúng working 68DF cùng parent/dependencies/test/report cần thiết, đọc cả diff của expected-digest loader nếu nó là dependency. Không stage toàn workspace; không đưa private data hay `.idea` vào. Mục tiêu là một commit chứa đủ source để checkout độc lập, không chỉ file test trỏ vào mã local chưa commit.
2. Dùng phần ngân sách chẩn đoán còn lại để bổ sung đúng case còn thiếu ở process boundary: prompt thử và successful synthetic child trên cùng entry/control flow; thay external action tại biên để không có quyền tới provider/device/secret. Kiểm cả failure trước parent audit bằng outer capture. Dữ liệu approval giả chỉ hợp lệ trong fixture đã tách khỏi live; tuyệt đối không bơm literal APPROVE thật vào candidate live để “test”. Tái dùng integration fixture hiện có, không tạo thêm supervisor.
3. Persist per-case hash/arguments shape/observed stage/exit/typed cause và action counts từ spies vào report hiện có hoặc event QA duy nhất, không hard-code counters thành bằng chứng. Không xóa nguồn diagnostic duy nhất trước khi lưu bản đã lọc. Hạn chế trường log; không ghi environment/secret/payload hàng loạt.
4. Xác minh source trong commit chứa đủ references và fixture chạy từ checkout độc lập. Một clone chạy được cần môi trường/toolchain được mô tả; không đồng nghĩa private/live evidence phải được upload.
5. Chỉ khi offline closure đạt mới bàn giao scope live review được. RAW accepted vẫn phải nối RECONCILE có quyền phù hợp, atomic REPORT_L1/receipt và reopen trước P5 exit. Không dùng lịch sử mất native cause làm yêu cầu vô hạn: có thể giữ historical cause UNRESOLVED nếu đường hiện tại đã được chứng minh và các gate còn lại đạt.

Budget không reset: đã ghi 1/2 vòng và khoảng 15/60 phút trước lượt QA này; cộng thời gian active review/diagnosis thực tế, không tuyên bố còn nguyên 45 phút nếu chưa đo. Lượt này chỉ QA nguồn/tài liệu, không có vòng patch–runtime-test mới. Claude xác nhận elapsed còn lại khi bắt đầu; tối đa một vòng còn lại, hết 60 phút thì dừng ngay. Nếu không đóng được phần thiếu trong giới hạn, trình phương án B đã có trong canonical §6, không mở vòng launcher thứ ba.

### Phản biện trước xuất

- “68DF tới prompt rồi, chạy owner ngay?” — kết quả mới đáng giữ; nhưng chỉ rejection/outer-error, thiếu success synthetic child ở boundary và bộ commit đầy đủ. Hoàn thiện đúng phần thiếu một lần, không rerun tất cả suites.
- “Staged CC01 sai nên mọi lỗi gốc đã rõ?” — không; hash của latest attempt được ghi là 68DF. Phải phân biệt defect phiên bản cũ với nguyên nhân event cụ thể.
- “Ẩn cửa sổ nghĩa là không có console?” — sai; console injection có thể hoạt động. Điều chưa chứng minh là tương đương với đường owner-visible và full orchestration.
- “Phải thêm log toàn bộ mới điều tra được?” — không; bounded redacted process/exit/stage facts đủ cho bước này, không cần key/provider output.
- “Cứ tiếp tục review cho chắc?” — không. Đây là review bounded; không review lại cùng bytes. Chỉ một targeted closure còn lại, hoặc đổi phương pháp đúng hạn.

QA tại lần xuất: năm file entry/parent/test parse không lỗi bằng parser của shell hiện tại; không nhận là PS5.1 execution PASS. Console JSON đọc được, hash candidate khớp; Git tree đối chiếu được các file chưa commit; source guard từ chối WRONG_LITERAL trước reservation/key/child. Luna rà độc lập và primary đối chiếu các findings với source. Giữ console report gốc, staged runtime bytes và các private events nguyên trạng.

## Vòng chẩn đoán entry owner-window 2/2 — 2026-10-01

- Bộ đếm: ≈30/60 phút (≈15 trước QA + ≈15 active từ 08:21 theo đồng hồ máy); 2/2 vòng. Đây là vòng cuối của phương pháp launcher; không mở vòng thứ ba.
- Giả thuyết cần loại trừ: nguyên nhân latest owner attempt (hash 68DF đã khớp trước launch, audit/reservation/owner-root vắng) không phải CC01. Dữ kiện mới: `D:\P5E-private\.p5e-a43-audit` đang chứa `68df8061….json` (tạo 2026-09-30 18:11:37) cùng hai audit đặt tên theo DecisionId (18:16, 18:40); cả ba là `APPROVAL / P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP`, exit 1, không reservation/key/child. Audit mặc định của entry chỉ đặt tên theo hash candidate.
- Reproducer (root tổng hợp, không đụng private thật): seed `.p5e-a43-audit\<hash>.json` rồi chạy 68DF `-Execute` đúng argument guide. Kết quả: stderr `P5E_A43_AUDIT_ALREADY_EXISTS_STOP`, exit 1, không prompt, không audit/reservation/owner-root mới, file cũ nguyên vẹn. Đây là một failure mode đã tái hiện. Review sau đó chỉ ra bản ghi lần owner thất bại ghi audit path theo DecisionId, tức không dùng tên hash mặc định, nên va chạm này không được coi là nguyên nhân đã chứng minh của lần đó.
- Patch: không sửa entrypoint (68DF giữ nguyên). Guide chạy owner thêm `-AuditPath` theo DecisionId và ghi kết quả ngoài cùng vào log CreateNew `.p5e-a43-outer-<DecisionId>.log`.
- Targeted test mới: `scripts/test-p5e-a43-entry-console.ps1` mở rộng lên 6 case trong console thật, mọi input là synthetic: (1) prompt + literal sai; (2) thiếu JavaPath giữ stderr/exit; (3) approval→key→synthetic child thành công (`CHILD_EXIT_ZERO`, exit 0, counters từ spy, live=0) bằng integration fixture với Read-Host thật; (4) bytes CC01 giữ lại: exit 0 im lặng; (5) audit hash có sẵn → `P5E_A43_AUDIT_ALREADY_EXISTS_STOP`, exit 1; (6) cùng trạng thái nhưng có `-AuditPath` riêng → tới prompt, typed stop, file cũ nguyên. Report: `evidence/p5e-a43-parent-integration-20260930/P5E_A43_ENTRY_CONSOLE_QA_03.json`. Integration test thêm `-ConsoleProbe`; chạy lại 21/21 vì bytes đổi (`..._QA_20260930_04.json`).
- Giới hạn: console là hidden-window thật, không phải cửa sổ owner nhìn thấy; case 3 chạy qua integration fixture, không qua `-Execute` của entry (entry live sẽ chạy command thật); không bằng chứng cho RAW/L1/provider/P5 exit.
- Kết luận: đường mở process/nhận input được tái lập offline ở cả success (prompt → terminal result → synthetic child qua integration fixture) và failure (cause + exit giữ nguyên). Nguyên nhân lần owner-window lịch sử vẫn UNRESOLVED (không có stderr được lưu). Không chạy owner trước khi owner xác nhận scope live.

## Review chuyển offline → P5 — 2026-10-01

| Loại | Nội dung |
|---|---|
| Fact | HEAD `88dab3a8` = origin; entry `68DF8061…`; QA_03 PASS 6 case (exit khớp), console hidden thật, input synthetic; command `.txt` copy thành `.ps1` parse sạch trên PowerShell 5.1; helper pin DB `3563F44B…`; hash DB đo lần cuối trên máy (09-26) `2CC23078…`, 17,784,832 B, bản sao host còn trong event 09-26 |
| Giả thuyết | Va chạm audit hash là nguyên nhân lần owner-window gần nhất: yếu đi, vì bản ghi lần đó ghi audit path theo DecisionId. Giữ UNRESOLVED |
| Chưa kiểm | Entry `-Execute` live với command thật; đường PrivateRoot thật; log CreateNew vào `D:\P5E-private`; `-LocalPropertiesPath`/`-BuildToolsVersion` thật trong test lưu; trạng thái DB/APK hiện tại trên máy |
| Khoảng thiếu cụ thể | Before dừng cứng `P5E_COLLECTOR_PRELIVE_DATABASE_HASH_MISMATCH` nếu hash DB khác pin. Cổng này chưa từng được chạm ở event live (09-26 dừng sớm ở tuple, 09-28 dừng ở `pm path`). Cần một quan sát chỉ-đọc (M0) trước khi owner duyệt event |
| Đánh giá | Đủ để chuẩn bị event RAW giới hạn theo scope trong run guide; không thêm gate mới ngoài cổng đã có trong helper. Success qua integration fixture vẫn dùng `Invoke-P5EPhaseChildInvocation` thật; phần chưa phủ là nội dung command thật chạy như `.ps1` |

## M0 quan sát chỉ-đọc — 2026-10-01 (owner đã cho phép)

- Đã chạy: `adb version`, `adb devices -l`, `adb -s 15e84958 get-state`. Daemon adb chưa chạy và được khởi động (host, không chạm máy).
- Kết quả: `adb devices` trống; `get-state` exit 1 `device '15e84958' not found`. Windows không liệt kê thiết bị Android/USB nào. Không có `pm path`, pull APK hay export DB vì không có máy; không có provider call, key hay ghi thiết bị.
- Kết luận: M0 CHƯA quan sát được (máy không kết nối). Hash DB/APK so với pins vẫn chưa biết; khoảng thiếu pin DB `3563F44B…` vs `2CC23078…` (09-26) chưa đóng. Có thể cùng nhóm nguyên nhân với event 09-28 dừng ở `pm path` (chưa chứng minh).
- Evidence riêng: `D:\P5E-private\m0-observation-20261001-014636633\M0_OBSERVATION.json` (SHA-256 bắt đầu `4b5c6b24a133f3b8`) và `M0_COMMAND_LOG.json`; không đổi/xóa.

### M0 chạy lại sau khi cắm máy — kết quả

- Lần 1 (`m0-observation-20261001-015546420`) bị hỏng do USB: sau khi `adb pull` APK production dừng ở khoảng 2% (1,572,864 B), mọi lệnh sau báo `device '15e84958' not found` (transport_id đổi 1 → 3). File APK dở và DB rỗng trong thư mục đó không có giá trị; không đọc thành mismatch.
- Lần 2 (`m0-observation-20261001-015631487`, thứ tự đã đổi: đọc nhỏ trước, truyền lớn sau) hoàn tất 11 lệnh chỉ-đọc, không key/provider/ghi:
  - Production `4.17-p5e.11`/code207, APK SHA-256 `2CCBB844…` khớp pin; test package có mặt, APK `058BE851…` khớp pin. WAL/SHM vắng (exit 1 = ABSENT hợp lệ).
  - DB chính 18,952,192 B, SHA-256 `8D084050974E0681BF05AE46D799DB8741BB2593FFEFE92B2D12B5920FDFE685`: **khác pin `3563F44B…` và khác lần đo 09-26 `2CC23078…`**.
  - Readback host (cùng SQL của collector, bridge `--immutable`, trên bản sao): schema 24; binding đúng selector/binding/run/pack/manifest fingerprint/profile/evaluation đã pin; bốn input RAW/GLOSSARY/DRAFT/PRONOUN đúng byte và hash đã pin; `LINEAGE` và `GLOBAL` toàn số 0; `integrity_check` ok; không vi phạm foreign key.
- Kết luận: nội dung DB khớp tuple tươi, không có lineage; chỉ hash file lệch pin. Before sẽ dừng cứng `P5E_COLLECTOR_PRELIVE_DATABASE_HASH_MISMATCH`, không gọi provider. Hash file đã đổi ba lần (09-13, 09-26, 10-01) trong khi nội dung tuple không đổi, nên pin hash file dễ vỡ. Chưa biết vì sao file đổi và có ổn định khi máy để yên hay không; chưa đo.
- Còn lại: bản sao chỉ-đọc DB hiện tại và hai APK đã nằm trong thư mục evidence (hash bắt đầu `3bd872a8` cho `M0_OBSERVATION.json`). Đây chưa phải bằng chứng P5E.8 trong checklist.

### Re-pin hash file DB — 2026-10-01 (owner đã đồng ý)

- Thay đổi (commit `68f0e8da`): pin DB của helper `3563F44B…` → `8D084050…`; cập nhật theo thứ tự child contract, manifest (thêm dòng pin DB), command, entry. Hash mới: helper `F0A567A2…`, child contract `70BB550C…`, manifest `B3A79783…`, command `6E87E8F5…`, entry (candidate) `D8E4D0FC…`. Run guide cập nhật candidate/manifest/scope.
- Kiểm: entry-boundary 6, integration 21/21, console 5/5 (report `*_REPIN_20261001.json`), child-invocation contract, decision-atomicity 28/28, binding-tuple 262/262, DB host-readback 56/56, preauth guard closure 21/21, production-version, collector launch probe; integration/boundary/console chạy lại PASS từ extract `git archive HEAD`.
- Không chạy được/không còn áp dụng: `test-p5e-child-invocation-launcher-static` (cần launcher riêng cũ), `test-p5e-a43-pm-path-capture-repair` (gọi `pwsh` chưa cài trên máy này; không liên quan pin). Case CC01 của console test bị gỡ vì bytes cũ dừng ở pin gate của chính nó; kết quả cũ giữ ở `P5E_A43_ENTRY_CONSOLE_QA_03.json`.
- Giới hạn: hash file DB có thể đổi lần nữa nếu máy/app được dùng; M0 chạy lại ngay trước khi owner duyệt event.

### M0 sau re-pin (lần 3) — 2026-10-01 02:18

- Evidence: `D:\P5E-private\m0-observation-20261001-021833029\` (`M0_OBSERVATION.json` SHA-256 bắt đầu `b2531f63`). Chỉ đọc; không key/provider/ghi.
- Đạt: máy có mặt; production `4.17-p5e.11`/code207; WAL/SHM vắng; DB 18,952,192 B SHA-256 `8D084050…` khớp pin mới và trùng bản export 01:56 (ổn định ít nhất 22 phút khi chỉ đọc). Cờ `dbMatchesPin=false` trong file là so với pin cũ trong script quan sát, không phải kết quả.
- Không đạt: hash APK lần này không xác nhận được. `adb pull` APK production dừng ở 2% (2,228,224 B); đọc lại bằng `exec-out cat` bị cụt ở 849,408 B với exit 0 (nên cần kiểm độ dài/hash, không tin exit 0); transport_id đổi 3 → 5 → 7. Hai APK đã khớp pin trong lượt đầy đủ lúc 01:56; APK không đổi nếu không cài.
- Rủi ro: collector đọc APK bằng `adb pull` (`P5E_COLLECTOR_PACKAGE_PULL_FAILED` nếu lỗi). Đọc APK bị ngắt 3/4 lần quan sát. Windows đang bật USB selective suspend (AC và DC). Một lần ngắt ở Before dừng event trước provider nhưng tiêu hao decision. Lần event 09-28 dừng ở `pm path` có thể cùng nhóm nguyên nhân (chưa chứng minh).
- Chưa làm: thay đổi cài đặt nguồn Windows (cài đặt hệ thống, thuộc owner); không thêm probe ổn định ngoài phạm vi M0.
