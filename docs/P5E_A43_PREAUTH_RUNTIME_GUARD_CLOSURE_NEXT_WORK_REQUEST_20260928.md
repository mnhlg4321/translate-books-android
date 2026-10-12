# Yêu cầu làm việc tiếp theo — đóng runtime guards trước owner authorization

## Trạng thái đầu vào

`OFFLINE_PM_PATH_CLASSIFICATION_AND_OUTER_CAPTURE_REPAIR_PASS / LIVE_PACKET_NOT_READY / A43_OUTER_PROCESS_TREE_AND_AUTHORITY_GATE_CLOSURE_REQUIRED / 0_BLOCKER / 1_HIGH / 2_MEDIUM / 2_LOW / CLOSED_EVENTS_NON_REUSABLE / NOT_AUTHORIZED / NOT_DISPATCHED / P6_NOT_READY`

Packet offline hiện tại đã sửa đúng lỗi PM-path classification và tách process
exit khỏi stream-drain status. Packet đó reviewable nhưng chưa đủ điều kiện xin
một live event mới vì Luna xác nhận một HIGH và hai MEDIUM liên quan trực tiếp
đến khả năng tái diễn lỗi vận hành:

1. **HIGH:** Windows PowerShell 5.1/.NET Framework 4.8 không có overload
   `Process.Kill(bool)`; fallback hiện tại chỉ chứng minh kill parent, không
   chứng minh ADB/instrumentation/provider descendant đã dừng;
2. **MEDIUM:** exporter/toolchain còn untracked và SQLite bridge đang modified,
   nên commit packet không tái dựng được exact runtime bundle;
3. **MEDIUM:** command chưa machine-enforce owner-decision receipt và chưa
   kiểm presence/shape của expected Process value trước event creation/ADB.

LOW về capture không có byte cap độc lập được đóng cùng work package vì nằm
trên cùng đường code. LOW về severity metadata chỉ cần được kiểm độc lập trong
QA/Luna review, không cần tạo runtime feature riêng.

## Mục tiêu duy nhất

Sửa offline command/supervisor contract để:

- một decision mới được bind bằng máy vào đúng packet, serial và phạm vi trước
  khi tạo event hoặc gọi helper;
- helper process tree bị đóng có kiểm chứng khi timeout, capture overflow hoặc
  drain failure;
- stdout/stderr outer capture có byte cap độc lập, không giữ raw output quá
  giới hạn và không che process exit/typed helper outcome;
- exact exporter/toolchain/bridge bytes có thể tái dựng từ một commit hoặc
  immutable source archive đã hash-bind.

Sau khi GREEN, refreeze đúng một packet owner-review mới. Không xin hoặc giả
lập owner decision trong work package này và không chạy live.

## Phân tích nguyên nhân vòng lặp

1. Các lần trước thường đóng lỗi parser cụ thể nhưng để control plane dựa vào
   quy ước tài liệu; state `pending/received/consumed` dễ bị lệch giữa file và
   phiên làm việc.
2. Event single-use bị tiêu thụ trước khi chẩn đoán có đủ độ phân giải, nên mỗi
   lỗi nhỏ lại cần repair → repin → owner decision → event mới.
3. Outer wrapper từng biến helper exit đã biết thành `125`, khiến lỗi thật và
   lỗi quan sát bị nhập làm một.
4. Synthetic PASS chứng minh logic đã test, nhưng không tự chứng minh cleanup
   toàn bộ descendant hoặc ràng buộc owner decision tại runtime.
5. Tài liệu lịch sử quá nhiều làm `Next action` dễ bị đọc nhầm. Trạng thái hiện
   hành phải chỉ nằm ở banner đầu và có đúng một next action.

Work package này chỉ đóng ba lỗ hổng control-plane nêu trên. Không mở thêm
parser, DB, model, prompt, Android source, build hoặc release scope.

## Phạm vi bất biến

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Baseline packet commit: `a30a4d1653c3a5a1e0d39fac4b07ae000d588edf`.
- Closed event gần nhất:
  `raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc`.
- Closed event, quyết định và các packet cũ không được rename, edit, retry,
  redispatch hoặc dùng làm fallback.
- Production/test APK, source ZIP, BUILD_INFO, certificate, serial, RAW,
  GLOSSARY, hidden DRAFT/PRONOUN, route/model, budget và DB allowlist không đổi.
- Không ADB, device, provider, credential, DB write, build, install, RAW,
  RECONCILE hoặc P6.
- Không xóa hoặc ghi đè exporter/toolchain/bridge hiện có; chỉ preserve exact
  bytes vào Git hoặc immutable archive, không stage owner changes khác.

## Các bước nhỏ nhất

### A. Freeze và tái hiện ba gap

1. Hash-check manifest `C08C3F6D…C11ADC3`, command
   `C641F6A0…2BA4CD5`, helper `959F2BBD…080A8F0`, exporter
   `D8783B31…EA41D06` và bridge `4598BFDF…BEA2111`.
2. Xác nhận current HEAD là commit con chứa packet; ghi rõ baseline trước
   packet nếu manifest dùng giá trị parent để tránh drift diễn giải.
3. Trích đúng hàm outer process/capture vào harness byte-equivalent; không chạy
   command review-only.
4. RED-1: synthetic helper tạo child và grandchild giữ stdout/stderr handle;
   parent timeout hoặc exit nhưng descendant vẫn còn sống sau fallback hiện tại.
5. RED-2: chứng minh command hiện tại có thể đi tới tạo event directory khi
   command hash đúng nhưng không có owner-decision receipt/token.
6. RED-3: chứng minh expected Process value missing/malformed vẫn có thể đi tới
   event creation/Before collector nếu không có pre-event gate.
7. RED-4: synthetic helper phát output vượt giới hạn; chứng minh
   `ReadToEndAsync` hiện tại không có byte cap độc lập.
8. Xác nhận trên chính Windows PowerShell 5.1 host rằng
   `[Diagnostics.Process].GetMethods()` không có `Kill(Boolean)`; không coi
   câu gọi `$process.Kill($true)` là tree termination đã được hỗ trợ.
9. Lưu chỉ PID synthetic, enum/status, byte count và boolean; không lưu raw
   payload của fixture nếu chứa sentinel nhạy cảm.

### B. Machine-enforce owner decision binding

1. Định nghĩa schema receipt không chứa secret, tối thiểu gồm:
   `decisionId`, packet identifier, serial, manifest/command/helper/exporter/
   bridge hashes, one-event limit, one-provider-call limit, scope
   RAW/GLOSSARY, no-retry/no-fallback/no-redispatch, issued-at và expiry.
2. Receipt phải là file riêng do owner kiểm soát ngoài repo; command chỉ nhận
   canonical path và expected SHA-256 qua `Process` variables. Không nhận JSON
   hoặc token qua argv.
3. Trước khi tạo event directory, command phải kiểm: biến Process có mặt, hash
   exact, schema/required fields, packet/serial/scope exact, thời gian hợp lệ,
   regular file, canonical path và no-reparse ancestors.
4. Thiếu/sai/expired/malformed/mismatched receipt phải stop trước event/helper/
   ADB với typed code riêng; không sửa hoặc tự sinh receipt.
5. Trước event creation, kiểm biến
   `P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT` chỉ theo presence và exact
   lower-case 64-hex shape. Không đọc, in, persist, hash lại, derive hoặc
   truyền giá trị qua argv. Missing/malformed phải dừng với event/ADB count `0`.
6. `decisionId` và receipt hash phải được bind vào `EVENT_PLAN.json` khi
   PrepareEvent thành công.
7. Trước tạo event, scan allowlisted event-plan metadata dưới evidence root để
   từ chối `decisionId` hoặc receipt hash đã dùng. Scan fail/unknown phải
   fail-closed; không đọc instrumentation/provider/credential files.
8. Một pre-event failure không được đánh dấu decision consumed. Decision chỉ
   consumed khi event directory và plan bind thành công.
9. Event đã bind decision luôn tiêu thụ decision dù dừng ở Before; không có
   retry/redispatch hoặc event thứ hai.
10. Clear decision variables khỏi child processes không cần chúng; expected
    account digest vẫn process-only và giữ contract hiện hành.
11. Không dùng riêng thao tác scan-then-create vì hai launcher có thể cùng
    quan sát trạng thái unused. Event directory hoặc decision reservation phải
    có tên deterministic từ `decisionId`/receipt hash và được tạo nguyên tử
    bằng create-new semantics. Chỉ một launcher có thể thắng; launcher còn lại
    phải stop trước helper/ADB. Không dùng check-then-create tách rời.
12. Hoàn tất toàn bộ pin/path/receipt/expected-value precheck trước atomic
    reservation. Atomic reservation thành công và event-plan binding là cùng
    một transaction logic: nếu event directory đã được tạo thì decision được
    coi là consumed kể cả plan/Before sau đó fail; nếu chưa tạo được directory
    thì decision chưa consumed. Không xóa reservation/event để tái sử dụng.
13. Owner chỉ cần gửi đúng một câu approval đã được soạn sẵn sau khi packet
    cuối được review. Agent tạo canonical receipt từ chính quyết định đó và
    công bố path/hash để owner kiểm tra; owner không phải tự viết JSON, không
    cung cấp key, endpoint hoặc digest cho agent. Runtime chỉ chấp nhận exact
    receipt bytes/hash đã được owner phê duyệt.

### C. Deterministic process-tree containment

1. Không dùng `$process.Kill($true)` làm bằng chứng tree-kill trên .NET
   Framework 4.8. Dùng một cơ chế Windows có lifecycle rõ ràng, ưu tiên Job Object với
   `KILL_ON_JOB_CLOSE`, hoặc cơ chế tương đương được test trên Windows
   PowerShell 5.1.
2. Gán helper process vào containment ngay sau start và trước khi cho phép
   đường code có thể spawn descendant. Nếu assign thất bại, typed stop; không
   tiếp tục ở chế độ parent-only.
3. Timeout, capture overflow, drain failure và exception phải đóng containment,
   chờ bounded, rồi xác minh parent/child/grandchild synthetic đều exit.
4. Không dùng `taskkill` bằng chuỗi lệnh dựng từ input. Nếu cần fallback, path
   và PID phải typed, bounded và có fixture riêng; failure vẫn fail-closed.
5. Preserve riêng `processExitCode`, containment status, stdout/stderr drain
   status, timeout, overflow và cleanup verification. Không thay process exit
   bằng wrapper code chung.
6. Nếu tree termination hoặc post-verification không chứng minh được, emit
   terminal `EXTERNAL_PROCESS_STATE_UNKNOWN`; After/Verify không được suy luận
   external action đã dừng và không được dispatch thêm.
7. Không infer helper success khi process tree chưa được xác minh đã kết thúc.

### D. Bounded outer capture

1. Thay `ReadToEndAsync` không giới hạn bằng stream pump có byte cap độc lập
   cho stdout và stderr.
2. Chọn cap từ output contract hiện hữu và ghi hằng số rõ trong manifest; cap
   phải đủ cho safe helper metadata nhưng nhỏ hơn mức có thể gây memory spike.
3. Khi vượt cap: ngừng giữ thêm bytes, đánh dấu `OUTPUT_TOO_LARGE`, đóng process
   tree, giữ process exit nếu có và không in/persist raw prefix.
4. UTF-8 split boundary, CRLF/LF, empty output, simultaneous stdout/stderr,
   delayed close, invalid bytes, exactly-at-cap và cap+1 phải có fixture.
5. Chỉ emit allowlisted status/byte-count fields. Secret sentinels trong output
   không được xuất hiện ở console, receipt, QA hoặc exception.

### E. Durable exact dependency provenance

1. Xác nhận trạng thái hiện tại: `scripts/p5e-db-binary-export.ps1` và
   `scripts/p5e-raw-toolchain.ps1` là untracked; `docs/P5E_SQLITE_BRIDGE.py`
   khác tracked blob dù current hashes khớp packet.
2. Preserve exact pinned bytes bằng một trong hai cách: commit chỉ đúng các
   dependency cần thiết, hoặc tạo immutable tracked source archive có manifest
   path/length/SHA-256 cho từng file.
3. Không stage `.idea`, owner worktree, artifact directory hoặc file unrelated.
4. Chứng minh clean checkout hoặc archive extraction tái tạo đúng ba hash
   dependency và parse được trên PowerShell/Python tương ứng.
5. Record riêng implementation baseline, dependency provenance commit/archive
   và final packet commit; không gọi parent baseline là current HEAD.
6. Nếu byte dependency không đổi, giữ hash; nếu đổi một byte, recompute toàn bộ
   downstream manifest/helper/command/QA/provenance.

### F. QA và mutation matrix

1. Owner receipt positive fixture và negative cases: missing, wrong hash,
   expired, future-issued, wrong serial, wrong packet hash, wrong scope,
   eventLimit >1, providerCallLimit >1, retry/fallback true, reparse path,
   reused decisionId và malformed JSON.
2. Race fixture phải khởi chạy đồng thời tối thiểu hai synthetic contenders
   dùng cùng receipt và chứng minh đúng một atomic reservation/event creation,
   contender còn lại stop trước helper/ADB; không chấp nhận test tuần tự giả
   lập race.
3. Process-tree fixtures: normal exit, parent nonzero, timeout, parent exit với
   child giữ pipe, grandchild giữ pipe, assign failure, cleanup timeout và
   already-exited race.
4. Capture fixtures: stdout-only, stderr-only, both, empty, exact cap, cap+1,
   invalid UTF-8, delayed close and secret sentinel.
5. Giữ PM classifier matrix, helper self-test, binding `262/262`, regression
   `175/175`, DB readback `56/56`, PowerShell 5.1 parse, path/reparse guard,
   secret scan và `git diff --check`.
6. Chứng minh counters ADB/device/provider/credential/DB-write/build/install/
   RAW/redispatch đều `0`.
7. QA report phải tính severity từ assertion failures và policy mapping; không
   chỉ chép số `0 BLOCKER / 0 HIGH` vào metadata.

### G. Phản biện độc lập

Yêu cầu Luna kiểm exact final bytes và phản biện tối thiểu:

- receipt giả hoặc stale có thể mở event hay không;
- cùng decision có thể tạo hai event khi race hay không;
- event directory có thể được tạo trước decision gate hay không;
- descendant hoặc pipe handle có thể sống sau terminal stop hay không;
- PowerShell 5.1 host có thực sự dùng được cơ chế tree termination đã chọn;
- cap có thể bị vượt qua bằng encoding/chunk boundary hay không;
- wrapper có còn che process exit hoặc helper typed outcome hay không;
- raw output/secret có thể lọt vào log/exception hay không;
- clean checkout/archive có tái tạo exact exporter/toolchain/bridge bytes;
- closed event, old command hoặc old decision có đường fallback hay không.

Chỉ được kết luận PASS khi `0 BLOCKER / 0 HIGH`; mọi MEDIUM phải được owner
packet nêu rõ. Nếu còn MEDIUM trên decision binding, process containment hoặc
capture cap, chưa tạo owner authorization request.

### H. Freeze và handoff

1. Freeze theo thứ tự: receipt schema/validator → helper/containment → manifest
   → command → QA → provenance → Luna review → owner request.
2. Hash lại toàn bộ downstream file khi một byte upstream đổi.
3. Đồng bộ canonical plan, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, checklist
   và proposal đúng một lần, với đúng một `Current Next action`.
4. Chỉ khi QA và Luna đạt điều kiện trên mới tạo owner authorization request
   cho đúng một event mới; không tự coi request là approval.
5. Không mở P6. P6 chỉ được đánh giá sau một A4.3 event có RAW acceptance và
   P5 exit evidence hoàn chỉnh.

## Tiêu chí hoàn thành

- Bốn RED fixtures tái hiện được gap hiện tại và chuyển GREEN.
- Owner decision receipt được machine-enforce trước event creation.
- Expected Process value presence/shape được kiểm trước event/ADB với zero
  disclosure.
- Decision reuse/race bị từ chối bằng fixture.
- Parent/child/grandchild synthetic đều được xác minh kết thúc ở mọi terminal
  path.
- Outer capture có byte cap độc lập và không lưu raw output.
- Clean checkout hoặc immutable archive tái tạo exact runtime dependencies.
- Regression đầy đủ PASS, live counters bằng `0`, Luna `0 BLOCKER / 0 HIGH`.
- Có một packet owner-review mới hoặc một typed offline stop duy nhất; không có
  live execution, retry hoặc P6.
