# Yêu cầu làm việc tiếp theo — sửa atomic decision one-use và cô lập expected digest

## Trạng thái đầu vào

`INDEPENDENT_REVIEW_STOP / 0_BLOCKER / 1_HIGH / 1_MEDIUM / CURRENT_PACKET_NOT_READY_FOR_OWNER_AUTHORIZATION / NOT_DISPATCHED / P6_NOT_READY`

Review độc lập đã tái hiện bằng fixture offline rằng cùng một `decisionId` có
thể tạo hai reservation nếu dùng hai receipt hash khác nhau trước khi
`EVENT_PLAN.json` được ghi. Review cũng xác nhận expected account digest đang
được truyền cho các helper phase không cần nó.

Nguồn finding:

- `docs/P5E_A43_FINAL_EXECUTABLE_INDEPENDENT_LUNA_REVIEW_20260928.md`
- SHA-256:
  `FACBDF6B54216C5D389804636DC177C66ACD01E2103453B5F57636DF045198F1`

Packet hiện tại không được dùng để xin owner authorization hoặc mở live event.
Work package này chỉ sửa hai finding trên bằng fixture synthetic và refreeze
packet nếu toàn bộ acceptance gate đạt.

## Mục tiêu duy nhất

Tạo một transaction fail-closed trong đó:

1. `decisionId` chỉ có thể thắng đúng một atomic reservation, bất kể receipt
   hash có giống hay khác;
2. receipt hash trở thành binding bất biến của decision đã thắng và không thể
   bind sang decision khác;
3. crash ở bất kỳ điểm nào sau reservation không làm decision hoặc receipt trở
   lại trạng thái reusable;
4. expected digest chỉ xuất hiện trong environment của đúng Dispatch/account
   phase; PrepareEvent, Before, After và Verify phải chứng minh không có biến;
5. QA cuối có thể chạy từ clean Git archive mà không dựa vào hai script QA
   untracked trong owner worktree.

Không mở rộng sang parser, Android source, APK, database schema, model, prompt,
route, provider hoặc P6.

## Baseline và các pin chỉ dùng để tái hiện

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Audit HEAD: `0730110a261edbab8f07f7db9e5831f567976fd3`.
- Runtime/dependency archive commit hiện tại:
  `6faebf0ad82ec6f0efba3ef42f50fec9e7bf3a74`.
- Manifest: `B093AB871072D78A9A935914AEC9FF56835606BB344F034AA5EE2F38EA83FF4F`.
- Command: `FA63C5D2498AA345C6AC426AD253B466D6BD3206D525BAE4E0E8E6158BA78F80`.
- Runtime guard: `F332954FB8AA2048EDF18630C5D1EF7B2039D57458C85CD6BB5CB07F59D96DBA`.
- Main QA script: `393196B7DC9C7BBA98A319DE1828E3CD6641A091946232C420B63556CF48B9EB`.

Các pin trên là input để RED-test và không còn đủ điều kiện live. Không sửa
ngược file frozen để giả giữ hash cũ; mọi byte runtime thay đổi phải sinh
packet và pin mới.

## Phạm vi cấm tuyệt đối

- Không ADB hoặc đọc/chạm thiết bị.
- Không provider, credential, endpoint hoặc account operation thật.
- Không DB write, build, install, RAW, GLOSSARY dispatch, retry, fallback,
  RECONCILE, restore, cleanup event hoặc redispatch.
- Không tạo owner receipt thật, không xin approval và không mở event live.
- Không reuse, rename, xóa hoặc sửa event/decision/packet đã đóng.
- Không tự mở hoặc đánh dấu P6 ready.
- Không stage/reset/clean các thay đổi owner không thuộc work package.

## Thiết kế bắt buộc

### 1. Chuẩn hóa identity trước reservation

1. Dùng validator hiện hành để parse receipt trước mọi reservation.
2. `decisionId` phải có canonical representation duy nhất. Không lowercase,
   trim hoặc Unicode-normalize âm thầm nếu schema hiện hành chưa quy định;
   reject input không canonical bằng typed stop.
3. Tạo `decisionKey = SHA-256(UTF-8 canonical decisionId)` và dùng full 64 hex
   cho tên marker. Không dùng prefix ngắn và không đưa raw decision ID vào path.
4. Receipt key là full SHA-256 của exact receipt bytes đã được owner bind.
5. Path root, marker root và event root phải qua regular-file/no-reparse guard
   trước mọi create operation.

### 2. Atomic decision reservation độc lập với receipt hash

1. Trước create, scan toàn bộ marker registry bounded và dưới path guard. Một
   decision marker hợp lệ đã chứa incoming receipt hash phải chặn receipt reuse
   ngay cả khi receipt marker/event/plan chưa từng được tạo. Bất kỳ marker
   partial, malformed hoặc unreadable nào cũng làm toàn registry stop
   `RESERVATION_LEDGER_UNKNOWN`; không tiếp tục vì không chứng minh được hash
   nào đã consumed.
2. Marker chính có dạng deterministic theo `decisionKey`, không chứa receipt
   hash trong tên: `.decisions/<decisionKey>.reservation`.
3. Tạo marker bằng `FileMode.CreateNew` hoặc primitive Windows tương đương.
   Scan rồi create tách rời không được coi là atomic.
4. Ghi ngay vào marker: schema, canonical decision ID hoặc digest không đảo
   ngược, exact receipt hash, packet ID, timestamp UTC và trạng thái
   `RESERVED_CONSUMED_FAIL_CLOSED`.
5. Flush file handle trước khi tiếp tục. Marker empty, partial, malformed hoặc
   unreadable vẫn có nghĩa decision đã consumed; không xóa để thử lại.
6. Nếu marker đã tồn tại, stop trước receipt marker, event directory, helper và
   ADB, bất kể incoming receipt hash giống hay khác.
7. `EVENT_PLAN.json` không phải nguồn duy nhất để xác định consumption. Marker
   decision tồn tại là bằng chứng đủ để từ chối reuse.

### 3. Receipt hash là binding bất biến và cũng one-use

1. Sau khi thắng decision marker, tạo `.receipts/<fullReceiptHash>.reservation`
   bằng `CreateNew` và bind về đúng `decisionKey`.
2. Nếu receipt marker đã tồn tại dưới decision khác, stop fail-closed. Decision
   marker vừa tạo vẫn giữ consumed; không rollback để dùng lại decision.
3. Nếu crash giữa decision marker và receipt marker, decision vẫn consumed.
   Incoming receipt hash cũng phải bị chặn qua registry-wide decision-marker
   scan ở lần sau, dù `.receipts/<hash>.reservation` chưa tồn tại.
4. Nếu crash giữa receipt marker và event directory, cả decision và receipt
   vẫn consumed.
5. Nếu event-directory create hoặc PrepareEvent thất bại, marker không được
   xóa, rename hoặc reset. Event thiếu `EVENT_PLAN.json` vẫn terminal consumed.
6. Event directory dùng identity deterministic từ `decisionKey`, không từ
   receipt-hash prefix. Create phải fail nếu path đã tồn tại.
7. Chỉ sau khi cả hai marker được flush mới được tạo event directory và gọi
   PrepareEvent synthetic/live path. Work package này chỉ gọi synthetic path.

Ghi rõ trong manifest rằng partial transaction có thể tiêu thụ một decision
mà chưa tạo được event. Đây là tradeoff fail-closed được chấp nhận; không thêm
rollback/recovery làm decision reusable.

### 4. Cô lập expected digest theo phase

1. Receipt và expected-value gate vẫn phải hoàn tất trước reservation/event/
   helper/ADB.
2. Không truyền receipt path/hash, command hash hoặc expected digest qua argv.
3. Tạo environment block riêng cho từng helper launch, bắt đầu từ allowlist
   hoặc explicit denylist đã test; không dựa vào inheritance mặc định.
4. PrepareEvent environment: expected digest `ABSENT`.
5. Before collector environment: expected digest `ABSENT`.
6. Dispatch/account environment: expected digest `PRESENT`, đúng shape, không
   log/in/persist/hash/derive; chỉ phase này được nhận giá trị.
7. After collector environment: expected digest `ABSENT`.
8. Verify environment: expected digest `ABSENT`.
9. Mọi error/timeout/overflow/exception path phải dispose environment/capture
   state và không đưa giá trị vào exception hoặc receipt.
10. Fixture child chỉ trả boolean `present` và validation class; tuyệt đối
    không echo length, prefix, suffix hoặc digest value.
11. Secret-sentinel scan phải phủ console, QA JSON, provenance, exception và
    temp fixture tree.

## RED fixtures bắt buộc trước khi sửa

Mỗi RED phải chạy trên byte hiện tại hoặc harness byte-equivalent và ghi typed
result, không chạm live command:

1. **RED sequential cross-hash:** cùng `decisionId`, receipt A rồi receipt B;
   chứng minh hiện tại cả hai reservation được chấp nhận khi chưa có plan.
2. **RED concurrent cross-hash:** hai process PowerShell 5.1 bắt đầu cùng lúc,
   cùng decision ID, receipt hash khác; chứng minh contract hiện tại không có
   uniqueness key độc lập với hash.
3. **RED same-hash:** cùng decision và cùng receipt chạy tuần tự và đồng thời;
   giữ bằng chứng control hiện tại chỉ chặn case này.
4. **RED crash window 1:** terminate synthetic worker sau decision marker và
   trước receipt marker/plan; chứng minh implementation cũ có thể reuse hoặc
   không có durable decision marker độc lập.
5. **RED crash window 2:** terminate sau receipt marker và trước event/plan.
6. **RED environment inheritance:** PrepareEvent/Before/After/Verify synthetic
   child quan sát expected variable đang present trong packet cũ.

Các fixture chỉ dùng temp root mới, PID synthetic, hash giả và sentinel giả.
Không dùng `D:\P5E-private` hoặc event thật.

## GREEN implementation theo từng bước

1. Viết/điều chỉnh pure functions cho canonical decision ID, `decisionKey`,
   receipt key và deterministic paths.
2. Viết atomic create function mở handle `CreateNew`, ghi bounded JSON/record,
   flush, close và trả typed outcome.
3. Mọi exception sau khi create thành công phải preserve marker. Không có
   catch/finally nào gọi delete marker hoặc event directory.
4. Tách decision reservation, receipt binding và event create thành state
   machine monotonic: `NONE -> DECISION_CONSUMED -> RECEIPT_CONSUMED ->
   EVENT_RESERVED -> PLAN_BOUND`. Không có transition lùi.
5. Khi thấy marker malformed/partial/unknown, emit typed terminal stop và coi
   registry state unknown; không reservation mới nào được phép cho đến một
   work package offline riêng. Không tự repair hoặc xóa marker trong runtime.
6. Thêm receipt-reuse guard cho different-decision/same-hash.
7. Sửa launcher để nhận phase enum bắt buộc và dựng child environment theo
   phase; unknown phase phải stop.
8. PrepareEvent/Before/After/Verify luôn remove expected variable khỏi child
   environment. Dispatch/account mới inject giá trị process-only.
9. Không thay Job Object, process-tree termination hoặc byte-cap code đã PASS,
   trừ phần tối thiểu cần truyền environment block. Nếu chạm các phần này phải
   rerun toàn bộ process-tree/capture matrix.
10. Cập nhật manifest/command/helper/runtime guard/QA/provenance theo dependency
    order và repin downstream sau khi implementation ổn định.

## Mutation, race và crash matrix bắt buộc

### A. Decision/receipt matrix

1. Same decision + same receipt, sequential: first wins, second typed stop.
2. Same decision + different receipt, sequential: first wins, second typed
   stop before receipt/event/helper.
3. Same decision + same receipt, concurrent: exactly one winner.
4. Same decision + different receipt, concurrent: exactly one winner.
5. Different decision + same receipt, sequential: receipt first wins; second
   decision becomes consumed and stops before event.
6. Different decision + same receipt, concurrent: exactly one event maximum;
   both decision markers may be consumed, receipt has exactly one binding.
7. Different decision + different receipt: independent reservations succeed
   only in isolated synthetic roots; no cross-binding.
8. Empty/malformed/overlong/noncanonical decision ID: stop before marker.
9. Receipt hash prefix collision: full 64-hex keys remain distinct.

### B. Crash-window matrix

Inject deterministic crash/fault after each boundary:

1. before decision-marker create;
2. after decision-marker create and flush;
3. after receipt-marker create and flush;
4. after event-directory create;
5. during/after plan write.

Expected outcome: only case 1 leaves the decision unused. Cases 2–5 must reject
every later reuse, even with a different receipt hash and even without
`EVENT_PLAN.json`. No fixture may clean marker to obtain GREEN.

### C. Expected-environment matrix

1. PrepareEvent absent.
2. Before absent.
3. Dispatch/account present exactly once.
4. After absent.
5. Verify absent.
6. Unknown phase stop.
7. Dispatch timeout/nonzero/capture overflow still does not expose value.
8. Sequential helper phases do not retain the Dispatch environment.
9. Concurrent synthetic launches do not cross-contaminate environments.

## QA dependency durability

Main QA currently invokes two worktree scripts that are not tracked:

- `scripts/test-p5e-binding-tuple-host-contract-repair.ps1`, length `34183`,
  SHA-256 `118DCA80D318EA71C07A171EEA0D5994EE59AFD306F5E79748F80969CB4A8697`;
- `scripts/test-p5e-db-host-readback-repair.ps1`, length `36733`, SHA-256
  `12B27AE04AA230F065BFE4CC20C1AB0560991494EDBC4B241C2233CF6B3B8FFC`.

Trước khi công bố QA cuối, chọn đúng một phương án và ghi rõ trong provenance:

1. **Preserve:** track exact hai script, pin path/length/hash và đưa cả hai vào
   Git archive reconstruction; hoặc
2. **Eliminate:** chuyển các assertion cần thiết vào QA script tracked hoặc
   fixture tracked, bỏ mọi runtime reference tới hai script, rồi chứng minh
   clean archive không cần chúng.

Không được báo binding/regression/DB PASS từ file output cũ nếu clean archive
không tự chạy được dependency tạo ra output đó.

## Full QA gate

1. Preserve toàn bộ legacy main packet assertions: `21/21 PASS`.
2. Chạy và ghi riêng mọi GREEN case mới ở decision, receipt, crash và expected
   environment matrices; tổng test mới không được gộp vào một assertion mơ hồ.
3. Binding suite: `262/262 PASS`.
4. Regression matrix: `175/175 PASS`.
5. DB host-readback: `56/56 PASS`.
6. Job Object/process-tree/capture byte-cap matrices: PASS nếu launcher hoặc
   environment construction chạm code path chung.
7. Windows PowerShell 5.1 parse: PASS cho mọi `.ps1` mới/đổi.
8. JSON parse/schema, path/reparse, secret/forbidden-pattern scan và
   `git diff --check`: PASS.
9. Counters ADB/device/provider/credential/DB-write/build/install/RAW/
   redispatch đều `0`.
10. QA severity phải được tính từ failed assertion; một failure ở atomicity,
    one-use hoặc environment isolation là HIGH và chặn packet.

## Archive-clean reconstruction gate

1. Commit exact runtime và QA dependencies của work package, không stage owner
   changes khác.
2. Xuất Git archive từ exact candidate commit vào temp root mới.
3. Chạy main QA và dependency suites từ archive, không tham chiếu workspace
   gốc, untracked file, absolute repo path hoặc cached result.
4. Từ archive phải tái tạo được manifest, command, helper, exporter, runtime
   guard, toolchain, bridge, main QA và hai dependency QA hoặc replacement đã
   chọn ở mục trên.
5. Hash path/length/bytes trong provenance phải khớp archive.
6. Xóa temp archive chỉ sau khi ghi bounded result; việc xóa fixture không tác
   động reservation/event thật.

## Phản biện độc lập trước khi refreeze

Reviewer phải cố phá tối thiểu các giả định sau:

1. Đổi receipt hash có vượt uniqueness của cùng decision ID không?
2. Đổi decision ID có tái dùng cùng receipt hash không?
3. Crash trước plan có làm marker bị bỏ qua không?
4. Marker partial/malformed có bị coi nhầm là unused không?
5. Hai contender cross-hash có cùng tạo event không?
6. Rollback/finally có xóa marker đã consumed không?
7. Expected digest có xuất hiện trong PrepareEvent/Before/After/Verify hoặc
   exception/capture/temp file không?
8. Clean archive có thật sự chạy 21/262/175/56 mà không dùng untracked file?
9. Closed event, old decision, old receipt hoặc old command có fallback path
   không?
10. P6 có thể bị mở từ bất kỳ PASS offline nào không?

Chỉ PASS nếu `0 BLOCKER / 0 HIGH`. Một MEDIUM còn lại trên atomicity,
receipt-binding, crash consumption, environment isolation hoặc archive
reconstruction cũng chặn owner packet.

## Thứ tự freeze bắt buộc

1. Runtime guard/state machine.
2. Phase-specific launcher environment.
3. Durable QA dependencies.
4. Main QA script và tất cả suites.
5. QA report.
6. Manifest.
7. Command.
8. Provenance.
9. Independent Luna review.
10. Canonical state synchronization và owner-review request chỉ trong work
    package sau khi tất cả mục trên PASS.

Không tự coi work request này là authorization. Không tạo owner receipt/live
command invocation trong work package sửa chữa.

## Tiêu chí hoàn thành

- RED fixtures tái hiện đủ cross-hash, same-hash, crash-window và environment
  inheritance của packet cũ.
- GREEN fixtures chứng minh một decision chỉ có một reservation bất kể receipt
  hash, một receipt chỉ bind một decision và crash không cho phép reuse.
- Expected digest chỉ present ở Dispatch/account; bốn phase còn lại absent.
- Legacy `21/21`, binding `262/262`, regression `175/175`, DB `56/56` và mọi
  test mới PASS từ clean Git archive.
- Hai QA dependency untracked đã được preserve hoặc loại bỏ có bằng chứng.
- Full pins/provenance tái dựng được; independent review `0 BLOCKER / 0 HIGH /
  0 MEDIUM` cho các control thuộc scope.
- Live counters đều `0`; không có owner authorization, event mới hoặc P6.

## Next action duy nhất

Thực hiện đúng work package offline này, bắt đầu bằng RED sequential
same-decision/different-receipt và kết thúc ở archive-clean independent review.
Không xin owner authorization và không chạy live trước khi packet mới được
repin và review độc lập đạt toàn bộ gate.
