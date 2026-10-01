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

### M0 sau khi sửa USB (lần 4) — 2026-10-01 02:21: ĐẠT

- Evidence: `D:\P5E-private\m0-observation-20261001-022149935\` (`M0_OBSERVATION.json` SHA-256 bắt đầu `7f667559`). Chỉ đọc; không key/provider/ghi; 11 lệnh, không bị ngắt.
- Một lượt đầy đủ: máy có mặt; production `4.17-p5e.11`/code207; APK production `2CCBB844…` và test `058BE851…` khớp pin; WAL/SHM vắng; DB 18,952,192 B `8D084050…` khớp pin mới (cùng hash ở 01:56, 02:18, 02:21).
- Readback host (SQL của collector, `--immutable`, bản sao): giống hệt lượt 01:56 — schema 24, binding và bốn input đúng pin, lineage/global toàn 0, integrity ok, không vi phạm foreign key.
- Còn lại cho owner: mỗi thay đổi trên máy (mở app, cài, chạm DB) có thể đổi hash file; chạy run guide khi trạng thái này còn đúng. Không có approval/quyền nào được tạo bởi M0.

## Event A4.3 owner-run — 2026-10-01 09:25 (candidate D8E4D0FC…)

- Cửa vào: cửa sổ PowerShell nhìn thấy của owner chạy đúng run guide; prompt hiện, approval được nhận, reservation/owner root/receipt tạo, key prompt 1 lần, child chạy 1 lần và thoát 1 (`CHILD_EXIT_NONZERO`), diagnostic và outer log (`.p5e-a43-outer-p5e-a43-owner-1599345c….log`, SHA-256 bắt đầu `1db3fd1384f2623c`) được ghi, exit 1 giữ đúng. Cửa vào hoạt động; đây là lần đầu đường owner thật có terminal result.
- Event `raw-live-a43-preauth-33253efa…` (DecisionId `p5e-a43-owner-1599345c…`, audit theo DecisionId): Before captured, một launch instrumentation, After captured, readback. `COLLECTOR_OUTCOME`: `EXTERNAL_CALL_STATE_UNKNOWN`, detail `P5E_POST_DISPATCH_DURABLE_STATE_INCOMPLETE`; `OUTCOME_VERIFICATION`: lỗi `INSTRUMENTATION_TEST_COUNT_OR_FAILURE_INVALID` (`NOT_OK_1_TEST`), `providerCalls=0` là bộ đếm collector chỉ-đọc, không phải bộ đếm của app. Decision, receipt và event đã dùng; không dùng lại.
- Bằng chứng cục bộ: hash DB After = Before = `8D084050…`; readback After: lineage/global toàn 0, integrity ok; cả quá trình khoảng 20 giây; stderr instrumentation rỗng.
- Nguyên nhân (đã xác nhận bằng source hiện tại và bằng source ZIP `5029E2AE…` của APK test `058BE851…`): `EditorialP5EFreshRawLiveInstrumentedTest` có `EXPECTED_DB_SHA256 = 3563f44b…`, yêu cầu tham số `p5e_expected_db_sha256` (dòng 259) và hash DB thật (dòng 289) bằng hằng số đó, trước khi đọc settings/key hoặc mở repository. Sau re-pin phía host, tham số là `8D084050…` nên `AssertionError`. Output instrumentation thô không được giữ (host chỉ lưu marker an toàn), nên bước thất bại chính xác là suy luận từ source, không phải quan sát trực tiếp.
- Lỗi quy trình của tôi: re-pin chỉ tìm hash cũ trong `scripts` và `docs`, không tìm trong `app/src/androidTest` và APK test, dù APK test được pin bằng hash. M0 không bắt được vì M0 chỉ so hash trên máy với pin host.
- Phân loại: chính thức `EXTERNAL_CALL_STATE_UNKNOWN`. Source cho thấy assertion nằm trước key/mạng và DB không có attempt row, nên rất có khả năng không có provider call, nhưng chưa kiểm phía nhà cung cấp; không suy ra `$0`. Đóng bằng cách owner xem Activity của OpenRouter cho 09:25 giờ địa phương (02:25 UTC) hoặc cấp riêng quyền đọc metadata (M2b).
- Hướng sửa trình owner: lấy kỳ vọng hash DB từ tham số launch do helper pin (so với hash DB thật) thay cho hằng số, build lại APK test bằng `scripts/build-and-save-android-test.ps1`, re-pin APK test/source ZIP/BUILD_INFO/source commit theo chuỗi helper → manifest → command → entry, kiểm lại offline, rồi owner duyệt riêng việc thay test package và một event mới. Không retry event cũ.

### Sửa lỗi APK test hard-code hash DB — chuẩn bị offline (2026-10-01, owner đồng ý hướng khuyến nghị)

- Source (`2f12a785`): `EditorialP5EFreshRawLiveInstrumentedTest` bỏ hằng `EXPECTED_DB_SHA256`; kỳ vọng hash DB lấy từ tham số `p5e_expected_db_sha256` (helper pin) và so với hash file DB thật (cả test preflight và test live). Biên dịch `:app:compileDebugAndroidTestJavaWithJavac` đạt.
- Build: `scripts/build-and-save-android-test.ps1` dừng khi Gradle ghi `Note:` javac ra stderr vì Windows PowerShell 5.1 biến native stderr thành lỗi dừng (lỗi tái hiện được ở lần build đầu); đã sửa cục bộ trong script (`35470f6d`). Build chạy trong worktree sạch tạm (nhánh tạm `tmp/p5e-a43-testbuild-20261001`, đã xóa cùng worktree), vì script đòi worktree sạch và nhánh không phải main.
- Kết quả: AndroidTest APK `D2B0E590C30DA5DAE73C5C28EA98BC9C6CB37DC4CE362C991EA46812D301B42F` (1,155,720 B), source ZIP `EEE56868…`, BUILD_INFO `D4CEBF68…`, source commit `35470f6d…`, certificate khớp pin; không còn chuỗi hash cũ trong dex; payload giống hệt ở `artifacts/` và `backup/test-builds/v4.17-p5e.11/p5e-a43-db-arg-20261001-01`. Chưa cài, không thao tác thiết bị, không provider.
- Cascade pin (commit `c496cab4`): helper `23801AD5…` (APK test, source commit, hash nguồn test `CD890651…`, đường dẫn APK) → child contract `70CBC0B2…` → manifest `45690909…` → command `F7EB3AF6…` → entry (candidate) `29029B46…`; run guide cập nhật.
- Kiểm lại: helper self-test, source-contract của helper trên nguồn test mới, entry boundary 6, integration 21/21, console 5/5, child-invocation contract, decision-atomicity 28/28, binding-tuple 262/262, DB host-readback 56/56, preauth guard closure 21/21, production-version, collector probe; integration/boundary/console chạy lại PASS từ extract `git archive`. Hash command/payload test đã đối chiếu với file trên đĩa. Report: `*_REPIN2_20261001.json`.
- Không áp dụng nữa: các test pin gói lịch sử 09-28 (`test-p5e-a43-final-executable-packet-repin`, `post-repair-packet-repin`) khẳng định hash gói cũ và `p5e-account-check-device-command` pin APK test cũ; không chạy lại và không dùng làm gate hiện hành.
- Còn lại (cần owner duyệt): thay test package trên máy (APK cũ `058BE851…` đang cài) bằng APK mới qua `scripts/p5e-install-account-check-test.ps1` (`-CheckOnly` rồi `-ExecuteOneReplacement`); chạy lại M0; event mới với decision mới. Event đầu vẫn `EXTERNAL_CALL_STATE_UNKNOWN`.

### Thay test package trên máy và M0 sau thay — 2026-10-01 09:52 (owner đã duyệt)

- `scripts/p5e-install-account-check-test.ps1`: `-CheckOnly` PASS (`test-install-check-20261001-095223`, 0 thao tác production/RAW). Sau đó `-ExecuteOneReplacement` chạy một lần (serial `15e84958`, pin hash APK `D2B0E590…`, chứng chỉ `47F31389…`). Script không in kết quả cuối vì bước kiểm tra sau cài (`adb pull`) ghi tiến trình ra stderr và Windows PowerShell 5.1 coi là lỗi dừng (cùng lớp lỗi với script build; chưa sửa vì không cần chạy lại). Không chạy lại lần thay thứ hai; `ACCOUNT_TEST_INSTALL_RESULT.json` không được ghi, chỉ có `installed-test-package.apk` (1,155,720 B) trong `D:\P5E-private\test-install-exec-20261001-095233`.
- Xác minh độc lập chỉ-đọc (`D:\P5E-private\m0-observation-20261001-025250216`, `M0_OBSERVATION.json` SHA-256 bắt đầu `3d50c617`): APK test đang cài `D2B0E590…` khớp pin mới; production APK `2CCBB844…` khớp, `4.17-p5e.11`/code207; WAL/SHM vắng; DB 18,952,192 B `8D084050…` nguyên vẹn (cùng hash như trước khi thay). Cờ `testApkMatchesPin=false` trong file là so với pin cũ trong script quan sát, không phải kết quả.
- Thay đổi trên máy: đúng một lần thay test package; app production và DB không bị đụng; không provider, không key.

### Lần chạy owner thứ hai — 2026-10-01 10:04 (candidate 29029B46…): dừng ở prompt approval, chưa dùng gì

- Kết quả trong cửa sổ owner: `Stage=APPROVAL`, `TypedCode=P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP`, `OuterExitCode=1`, `ReservationCreated/OwnerRootCreated/ReceiptCreated=false`, `KeyPromptCount=0`, `ChildAttempts=0`, audit theo DecisionId `p5e-a43-owner-f543a7a3…` và outer log đã ghi. Kiểm thư mục private: chỉ thêm audit và outer log; không reservation, owner root hay event mới. Không dùng decision, không key, không child, không provider call.
- Nguyên nhân: literal nhận được không đúng. Ảnh chụp cho thấy các dòng lệnh phía sau dòng khởi chạy xuất hiện ngay sau prompt, nên có khả năng khối lệnh được dán một lần và các dòng sau bị prompt ẩn đọc thay cho câu trả lời; cũng có thể là gõ sai. Không xác định được. Thử nghiệm console thật với khối cũ (input chèn bằng `WriteConsoleInput`) không tái hiện việc nuốt dòng nên không chứng minh được cơ chế.
- Thay đổi: khối lệnh trong run guide được gói thành một lệnh duy nhất (`& { ... }`) để không còn dòng nào đứng sau lệnh khởi chạy. Thử trong console thật (synthetic): prompt chờ, một literal sai cho kết quả typed `P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP` và outer log được ghi bởi chính lệnh đó. Không chạy lại suite khác (entry, pin không đổi).

## Event A4.3 lần 3 — 2026-10-01 10:10 (candidate 29029B46…): request tới nhà cung cấp trả HTTP 404

- Cửa sổ owner: approval nhận, key prompt 1 lần, child 1 lần exit 1 (`CHILD_EXIT_NONZERO`), audit `p5e-a43-owner-b2c8d3fa…` và outer log (SHA-256 bắt đầu `70e51aadeb490915`) ghi đúng, `OUTER_EXIT=1`. Đường owner thật có đầy đủ terminal result.
- Event `raw-live-a43-preauth-df7ee78f…`: Before và After captured; instrumentation `OK (1 test)`; verifier `errorCount=0`; `COLLECTOR_OUTCOME` `RECOVERY_REQUIRED` / `P5E_DURABLE_RECOVERY_REQUIRED`; `post-readback`: `attemptStatus=RECOVERY_REQUIRED`, `recoveryReasonCode=RETRY_PROVIDER_HTTP_ERROR`, `acceptance=NOT_PROVEN`.
- DB After (host readback, integrity ok, foreign key sạch; hash file đổi `8D084050…` → `fa71cb75…` do app ghi): attempt 1, authorization receipt 1, lifecycle 1 = `CALL_FAILED`, request body 89,133 B, HTTP 404, `application/json` 628 B, `ApiHttpException`, 956 ms, có generation id; không response identity, không report, không receipt. Tức một request đã được gửi và được trả lời bằng lỗi; không có completion.
- Không có: thân lỗi 404 (chỉ lưu độ dài), usage/cost. Nguyên nhân 404 chưa biết. Giả thuyết chưa kiểm: request compact-wire gửi `temperature`, `max_tokens`, `stream=false`, `response_format` json_schema strict, `reasoning_effort` cùng `provider` = `require_parameters=true`, `allow_fallbacks=false`, `only=[openai]`, `data_collection=deny` (`EditorialP5EFreshRawRoutingPolicy`, `OpenAICompatibleClient`); OpenRouter có thể trả 404 khi không endpoint nào đáp ứng tất cả tham số hoặc chính sách dữ liệu. Các lần HTTP 200 tháng 09 dùng route và hình dạng request cũ.
- Chi phí: chưa kiểm; lỗi 404 thường không tạo completion. Không suy ra `$0` cho tới khi có metadata hoặc owner đối chiếu Activity/credits.
- Đã dùng: decision key, receipt, event và reservation của candidate `29029B46…` (`.p5e-a43-pre-reservation-reservations\29029b46….reservation`). Event mới cần candidate hash mới (entry bytes đổi) và decision mới; không retry event này.
- Việc theo dõi (chưa làm): lưu thân lỗi nhà cung cấp đã redacted và giới hạn kích thước trong lifecycle khi gặp HTTP error để lần sau không phải chẩn đoán mù.

### Nguyên nhân HTTP 404 — metadata công khai OpenRouter (owner đã cho phép đọc, 2026-10-01)

- Đọc: `GET https://openrouter.ai/api/v1/models/openai/gpt-5.6-luna/endpoints` (công khai, không key, không gọi model, không chi phí); không lưu vào repo.
- Kết quả: 7 endpoint (OpenAI, OpenAI flex, OpenAI fast, Azure ×3, Amazon Bedrock). Tham số được hỗ trợ ở các endpoint OpenAI: `include_reasoning`, `max_tokens`, `reasoning`, `reasoning_effort`, `response_format`, `seed`, `structured_outputs`, `tool_choice`, `tools`. **Không endpoint nào liệt kê `temperature`.**
- Request RAW của `OpenAICompatibleClient.buildChatRequestBody` luôn gửi `model`, `temperature` (0.3 từ settings), `max_tokens`, `stream=false`, `response_format` (json_schema strict), `provider` = `{require_parameters:true, allow_fallbacks:false, only:[openai], data_collection:deny}`, `reasoning_effort`, `messages`. Với `require_parameters=true`, nhà cung cấp loại các endpoint không hỗ trợ đủ tham số; vì `temperature` không được hỗ trợ ở đâu nên không còn endpoint nào, nhiều khả năng dẫn tới 404. Các lần HTTP 200 tháng 09 dùng đường cũ `buildChatRequestBody(..., false, "")` không có `require_parameters` nên `temperature` bị bỏ qua âm thầm.
- Mức chứng minh: bằng chứng mạnh nhưng gián tiếp; thân lỗi không được lưu và chưa xác nhận chính chi tiết `data_collection=deny`. Không đọc metadata generation (cần key; chưa được cho phép) nên chi phí vẫn chưa kiểm.
- Hướng sửa trình owner: không gửi `temperature` khi `providerPreferences` đòi `require_parameters` (route P5E), kèm unit test; giữ route fingerprint. Việc này đổi body request nên đổi hash body/envelope/identity đã pin và cần APK production mới, cài giữ dữ liệu, preflight trên máy để tính lại hash, re-pin và candidate mới. Phương án `require_parameters=false` không ưu tiên (nhà cung cấp bỏ tham số âm thầm và đổi route fingerprint).

### Sửa `temperature` — đã commit, chưa build (2026-10-01, owner đã duyệt hướng sửa)

- Thay đổi (`16b5c97e`): `OpenAICompatibleClient.buildChatRequestBody` bỏ `temperature` khi `provider.require_parameters=true` và `reasoning_effort` không rỗng (đúng route RAW của P5E); các route khác vẫn gửi `temperature`. Phạm vi hẹp để đường P5C/P5D cũ không đổi hành vi.
- Test: `EditorialP5COutputBudgetTest` cập nhật và thêm `temperatureIsOnlyDroppedForRequireParametersWithReasoning`; `:app:testDebugUnitTest` 250/250 (249 + 1), 0 lỗi.
- Mức ảnh hưởng: `git diff 995d3b6c HEAD -- app/src/main` chỉ gồm thay đổi này (8 dòng thêm, 1 xóa). Build mới từ HEAD sẽ khác APK code207 đang cài đúng ở thay đổi này (cộng version/BUILD_INFO).
- Đổi body request nên đổi hash body/envelope/request identity/attempt identity đã pin trong helper; route fingerprint không đổi. Hằng số production trong test APK (version code, hash APK) cũng phải đổi khi có APK production mới.
- Việc kế tiếp (mỗi bước cần duyệt): build production `scripts/build-and-save.ps1 -Series 4.18-p5e` (số bản dựa trên `artifacts/builds` của checkout chính nên dự kiến `4.18-p5e.1`, versionCode 209); cập nhật pin; build lại APK test; cài giữ dữ liệu; preflight trên máy; candidate/decision mới.

### Chuỗi sửa `temperature` — đã thực hiện đến trước preflight (owner duyệt build và các bước sau, 2026-10-01)

- Build production: `scripts/build-and-save.ps1 -Series 4.18-p5e -MinimumVersionCode 209` trong worktree sạch tạm (nhánh tạm đã xóa): `4.18-p5e.1`, versionCode 209, sự kiện `build-20261001-102120`, APK SHA-256 `4F3DAF9C77DAD104A57CC54536A237D09B941610600A60A5876BD608C168D41C`, source ZIP `CA39E984…`, BUILD_INFO `561C5211…`; test đơn vị và lint đạt; payload giống hệt ở `artifacts/builds/v4.18-p5e.1/…` và `backup/builds/v4.18-p5e.1/…`.
- Test APK build lại từ commit `ed54d2b6` (hằng production đổi sang 209): `8F3329631DA2B35C66412B480CCD0C93F1CF6C52BE7D1B6FD21FBE3FE1C43566`, source ZIP `49BEDB48…`, BUILD_INFO `02E43140…`; payload ở `artifacts/` và `backup/test-builds/v4.18-p5e.1/p5e-a43-prod209-20261001-01`.
- Máy: bản sao DB chỉ-đọc trước khi cài (`pre-install-20261001-102354`, hash `FA71CB75…`); `install-validated.ps1 -CheckOnly` rồi một lần cài `4.18-p5e.1` (Success, verification PASS, chữ ký `abebea4b`); `p5e-install-account-check-test.ps1` `-CheckOnly` rồi một lần thay test package (script dừng sau cài ở bước kiểm tra `adb pull` vì stderr, như lần trước); đọc lại chỉ-đọc (`post-install-20261001-102500`): production `4F3DAF9C…` / `4.18-p5e.1` / 209, test `8F332963…`, DB vẫn `FA71CB75…` (dữ liệu giữ nguyên).
- Pin: helper (production, test APK, source commit `ed54d2b6…`, hash nguồn test), child contract, manifest, command, entry, test `test-p5e-production-version-contract` (đường BUILD_INFO mới); helper self-test, integration 21/21 và source-contract PASS. Các pin attempt/request/envelope/body trong helper vẫn là của body cũ và chỉ tính lại được sau preflight.
- Chặn mới: `Assert-P5ECollectorZeroBefore` (host) và `assertLineageZero` (test preflight/live) đều đòi attempt/receipt/lifecycle bằng 0. DB trên máy (`FA71CB75…`) còn dòng attempt/receipt/lifecycle của event HTTP 404, còn pin DB `8D084050…` chính là ảnh DB trước event. Cần khôi phục ảnh trước event lên máy (ghi vào dữ liệu pilot; owner chưa duyệt bước này) rồi mới chạy được preflight và event kế tiếp. Ảnh sau event được giữ ở `pre-install-…`, `post-install-…` và thư mục event `raw-live-a43-preauth-df7ee78f…`.

### Khôi phục DB zero-state, preflight chính xác, pin cuối — 2026-10-01 (owner đã duyệt)

- Khôi phục DB: ảnh trước event `8D084050…` (18,952,192 B, từ `m0-observation-20261001-025250216`, hash kiểm trước khi dùng) được ghi qua `exec-in` vào tệp tạm trong thư mục databases của app (app không chạy), kiểm hash và kích thước trên máy, rồi `mv` thay DB (cùng chủ sở hữu/quyền `rw-rw----`; `-journal` 0 byte giữ nguyên). Ảnh sau event `FA71CB75…` được giữ ở `pre-install-20261001-102354`, `post-install-20261001-102500` và thư mục event `raw-live-a43-preauth-df7ee78f…`. Đọc lại (`restore-verify-20261001-102829`): hash `8D084050…`; readback host: schema 24, lineage/global toàn 0, integrity ok, foreign key sạch.
- Preflight chính xác trên máy (`exact-preflight-20261001-102915`): `OK (1 test)`, `providerCalls=0`, `attemptCreated=false`, lineage `UNUSED`, DB không đổi, `reasoningEffort=minimal`, route/`requireParameters=true`/`only=[openai]`/`dataCollection=deny`, body 88,570 B. `attemptIdentity`, `requestIdentity`, `requestEnvelopeHash` và `routeFingerprint` không đổi; chỉ `canonicalHttpRequestBodySha256` đổi thành `a9d54a4c4c85d4aa367f23ffa1d7c2846575f55d23d8a10bfdf8ecbf0f970641` (đúng tác động của việc bỏ `temperature`).
- Pin cuối (commit `8fdb6de3`): helper `83A92AB9…`, child contract `D19FF16F…`, manifest `7F1243D8…`, command `290026A4…`, entry (candidate) `9A4F4666D1DC1EC32653360B7C6A580C9A0F7667BC18C927283C123F0D1A1B5C`; run guide cập nhật.
- Kiểm: helper self-test, entry boundary 6, integration 21/21, console 5/5, child-invocation contract, decision-atomicity 28/28, binding-tuple 262/262, DB host-readback 56/56, preauth guard closure 21/21, production-version, collector probe; integration/boundary/console chạy lại PASS từ extract `git archive`. M0 cuối (`m0-observation-20261001-033151849`): máy, production `4.18-p5e.1`/209 và AndroidTest khớp pin, DB `8D084050…`, WAL/SHM vắng.
- Đã dùng trước đó (không dùng lại): decision của các event 09:25 và 10:10 và reservation của các candidate `D8E4D0FC…` (không dùng), `29029B46…`. Candidate `9A4F4666…` chưa có reservation.
