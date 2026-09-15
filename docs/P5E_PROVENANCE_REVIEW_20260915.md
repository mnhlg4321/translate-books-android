# P5E — tái kiểm tra readiness và provenance, 2026-09-15

> **Historical findings superseded for current readiness:** the local repair at
> probe HEAD `35c52600` resolves H1–H4 with an exact helper hash gate, a
> source-derived separate report/receipt contract, an executable same-event
> Before/After collector and a typed recovery path. The timing/event/path and
> redaction evidence below remains valid in its tested scope. Current evidence
> is `P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json`.

**Chưa được chuyển P6; chưa dispatch A4.3.** F2 giữ bằng chứng RED→GREEN.
F3 local evidence chain is now GREEN; the synthetic producer remains a
regression-only test and the live collector has not been run on a device. F1
trusted expected fingerprint and the exact account/readback/egress permission
remain `PENDING`. Đây là tiếp tục P5E, không tạo phase/release mới.

## Current local resolution — H1–H4 closed, owner decision pending

The current branch is `feature/v4.18-p5e-audit-20260914`; input baseline
`8c24b7d2` is an ancestor and the offline probe ran at
`35c52600d59cb3cd068a5c566dc9f7e43bed50a5`. The unchanged manifest is
`DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501`.
The final command SHA-256 is
`30B50BFEF809225B33901AC130CC5EE83CC8AD76A3D958D987EF19C8FCA50D10`; the
helper/collector SHA-256 is
`4D68F4BB0B0D31FA6D443439967746C1C83CCB4514EE6D7AF830CF90DBE0EC76`.
The current host-preparation document SHA-256 is
`CF8DBC069457A998BD5BDC7C84B500B9566CB598ABF0AAD0F053D766E046C711`.
The current local result SHA-256 is
`ECD61953C8E9C4E4539EC5B2B5865EDBC08D686E37F4C4743D67084887D8E725`.

H1 command/helper hash gates and tamper controls are PASS with negative launch
count `0`. H2/H4 have explicit `CollectReadback` Before/After modes, same-event
path binding, WAL-aware/read-only source mapping and typed
`NO_CLAIM_OBSERVED`/`EXTERNAL_CALL_STATE_UNKNOWN`/`RECOVERY_REQUIRED`/
`COLLECTOR_TYPED_STOP` outcomes; the collector records bounded allowlisted
operation classes and numeric exit codes in `COLLECTOR_COMMAND_LOG.jsonl`
without argv or captured output. H3 uses the separate contract
`docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json` (SHA-256
`FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`) pinned
to serializer SHA-256
`1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C`.
The 11 synthetic byte/shape mutation cases all reject; the production golden
serializer test source is present but was not executed because this request
forbids build. No device, provider or credential operation occurred.

The owner template remains `PENDING / NOT_APPROVED / NOT_PROVIDED`; this local
closure does not approve A4.3, RAW, account verification or P6. The one next
action is owner review of the final hash-bound packet and a separate exact
account/readback/RAW permission decision.

## Historical provenance of the superseded review

| Dữ kiện | Nguồn trực tiếp | Kết quả/giới hạn |
|---|---|---|
| HEAD được user bàn giao | `git rev-parse HEAD` | `9beafef8e59825714e47cdfe594dc287b6c5a0ce`; mốc bàn giao trước provenance review |
| HEAD lúc resume repair | `git rev-parse HEAD` | `31a262a806372dc804a0650c4d65f8012d4f78bb`; branch đúng, clean trước local mutation |
| Branch | `git status --short --branch` | `feature/v4.18-p5e-audit-20260914`; tiếp tục cùng phạm vi, không tạo release/checklist khác |
| Host implementation | helper working tree + `Get-FileHash` | local repair dựa trên `c2c79a19842f551fc752a53328024aab8ddb529d`; helper hiện hành `6FABAE1F53973942561EDA52002F052F775570A931EAB65188103E7DEE9955A2` |
| Audit trước | Git log | `0f52d36e516560bb33d294303c70fa1753cb64f9` |
| A4.3 ban đầu | Git log | `f8fe433ef454772a1b55dea496a2c4bfd679766f`; proposal preparation `c1e3ec6eec62e38a1e2f4fdb0d0f151d5efdcfae` |
| Test source/archive | Source pin và diff app/editorial-engine | `d51b7f3c16bdc482513b9904db07b97daed592d1`; Android source không đổi tới HEAD bàn giao |
| Manifest | Hash tính lại từ file | `DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501` |
| Command hiện hành | Hash tính lại từ file | `1D9A67693C4C4F64AF182300CDEB963A9FD9D44361EF77E91371F88C92EDA15E` |
| Helper hiện hành | Hash tính lại từ file | `6FABAE1F53973942561EDA52002F052F775570A931EAB65188103E7DEE9955A2`; `BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799` là input RED cũ |
| Probe wrapper | Hash tính lại từ file | `EA38C04399436AC5FA9748877FEBD346CAD21D0F4C371CA29E5A28D3CA72AA53`; gọi helper bằng child process, không dot-source |
| Báo cáo host hiện hành | Hash tính lại từ file | `728E1F3986A1C7E921CD9DD5B3199BD8F1B36E8699EF9050C4306D38DABF5703` |
| Mutation/provenance probe hiện hành | `P5E_PROVENANCE_REVIEW_PROBE.ps1` và `P5E_PROVENANCE_REVIEW_RESULT.json` | synthetic producer→verifier; không chạy Dispatch, VerifyOutcome entry point, SelfTest, process supervisor, ADB hoặc provider |
| Quyền trong tin nhắn user hiện tại | Yêu cầu audit/plan/provenance | Không phải quyết định cho phép đọc credential hoặc gửi sách |

Đã kiểm kê/đọc máy 158 MD/TXT tracked, 27.796 dòng tại baseline. Đọc sâu các tài liệu điều khiển hiện hành, bộ P5E, thay đổi từ audit trước, command/helper, selected live method và nguồn liên quan đến expiry/identity. Không tuyên bố semantic review từng dòng mọi tài liệu lịch sử hoặc toàn bộ Android regression. Những chứng cứ lịch sử A4.2, code196, IPC/CP6 vẫn giữ cách phân loại trong audit trước; không đọc lại toàn bộ raw lịch sử khi không có thay đổi liên quan.

## Đã đóng gì, còn gì

| Mục | Trạng thái đúng |
|---|---|
| F2 pipe quoting | Sửa ở host; giữ bằng chứng RED→GREEN cũ. Không đưa lỗi pipe cũ trở lại thành blocker khi chưa có regression mới |
| F3 consumer và fixtures cũ | Đã có; PASS cũ vẫn đúng trong phạm vi fixture đã chạy |
| F3 khả năng chứng minh dữ liệu thật | Historical review closed only an offline synthetic boundary; the current local chain additionally has an executable same-event collector and typed recovery path, but no live device run or RAW acceptance |
| F1 actual fingerprint computation | Có trong test pin; settings→normalize endpoint→SHA-256→compare, trước authorization |
| F1 expected fingerprint provenance | Chưa có từ owner; phép tính actual không tự chứng minh đúng tài khoản được owner chọn |
| P5/P5E exit, P6 | Chưa đạt. Không có RAW/L1 acceptance mới |

### R1 — chấp nhận readback sai thời gian, identity và event

Probe dùng đúng helper hash BEEF…6799, nạp AST declarations và gọi verifier như hàm trên dữ liệu giả. Không chạy các top-level dispatch/default branches. Kết quả:

| Case | Mong đợi | Thực tế |
|---|---|---|
| valid_control | accept | RAW_ACCEPTED, đúng |
| missing_receipt_control | reject | RAW_NOT_ACCEPTED, đúng |
| observed_before_run: observedAtMillis=0 | reject | RAW_ACCEPTED, sai |
| consumed_after_expiry: consumed=issued+180001, validity=180000 | reject | RAW_ACCEPTED, sai |
| artifact_manifest_mismatch: receipt manifest khác report | reject | RAW_ACCEPTED, sai |
| wrong_evidence_directory: metadata thuộc event khác | reject | RAW_ACCEPTED, sai |

Không gọi đây là “6/6 PASS”: chỉ hai control có kết quả đúng; bốn mutation phát hiện false acceptance. P6 vẫn false trong mọi case, nhưng điều đó không làm RAW acceptance sai trở thành chấp nhận được.

Nguồn lỗi: `Test-P5EReadback` chỉ kiểm observedAt>=0; consumed>=issued mà không kiểm consumed<expires; manifestFingerprint chỉ kiểm 64 hex mà không ràng buộc report/receipt với identity đúng; metadata.evidenceDirectory thuộc required shape nhưng không đối chiếu với event/file đã mở. Engine authorization có `nowMillis >= expiresAtMillis` là invalid, nên expiry boundary đã có căn cứ nguồn, không phải gate mới tự đặt.

Đây là lỗi verifier host trên dữ liệu giả, **không phải bằng chứng production đã commit dữ liệu sai**, cũng không chứng minh từng readback hợp lệ phải hoàn tất trước auth expiry. Post-readback có thể muộn hơn expiry; claim/consume phải thuộc cửa sổ được phép. Phải phân biệt hai thời điểm khi sửa.

### R2 — có schema nhận readback, chưa có producer được chứng minh

Search `p5e.raw.readback.v1`, `PostReadbackPath`, `post-readback.json` trong scripts/docs/app cho thấy consumer, template comment và generator **fixture** trong helper. Chưa tìm được collector thực từ package/DB/report/receipt đến schema này. Đây là kết luận giới hạn trong phạm vi đã search, không khẳng định không tồn tại công cụ ngoài repo.

Một JSON ghi `atomicClaim=true`, `validationPassed=true`, `allowedDiff=true` chỉ là khai báo nếu không truy ngược được nguồn. Dữ liệu sau transaction cũng không tự chứng minh mọi chi tiết atomicity trong quá khứ. Cần tách: quan sát before/after của event thật; validator chạy trên bytes thật; invariant transaction được chứng minh bằng source/test liên quan. Không bắt owner tự viết JSON giống fixture và không copy booleans từ fixture vào evidence thật.

Producer là dependency thực trước live: nếu chỉ thiết kế sau khi live đã chạy, sẽ tái diễn A3.2 thiếu post-check và A4 emitter/parser gap. Ưu tiên host-only collector từ readback được phép hoặc đường read-only sẵn có; chỉ đề nghị AndroidTest mới khi chứng minh không có đường tương thích với pin hiện tại.

### R3 — tuyên bố không log fingerprint quá mạnh

Test pin dùng `assertEquals(expectedEndpointAccountFingerprint, endpointAccountFingerprint)` ở dòng 297. Khi mismatch, assertion String có thể đưa expected/actual vào failure text. Host redactor chỉ nhận api-key/bearer/endpoint/URL patterns; probe assertion giả với hai fingerprint vẫn giữ nguyên cả chuỗi và `redactionViolation=false`.

Không phát hiện credential thật, không chứng minh đã có leak thật. Fingerprint cũng không phải API key. Tuy vậy, nếu packet yêu cầu fingerprint không vào logs thì regex hiện tại chưa bảo đảm điều đó ở failure path. Sửa host capture để loại giá trị fingerprint ở assertion bằng input fake và bật fail-closed; không đọc credential để tạo test. Giữ phân biệt fingerprint ở metadata/receipt nào được policy cho phép và fingerprint không được phép log, tránh vừa cấm toàn bộ vừa yêu cầu lưu cùng field trong readback.

### R4 — provenance của expected cần một thao tác thực tế

Expected là hash endpoint chuẩn hóa + newline + đúng credential; không phải account ID trên dashboard. Owner phải xác nhận mapping giữa credential đó và account/project/billing được phép. Fingerprint chứng minh chuỗi endpoint/key khớp expected, không tự xác thực legal owner hay trạng thái billing.

Có hai đường: (A) owner đã có giá trị/trusted attestation do quy trình có nguồn tạo ra; hoặc (B) chưa có, cần owner cho phép một enrollment/verification tối thiểu được mô tả chính xác. Đường B chưa được giải quyết bằng lời “hãy cung cấp provenance”; không được giả expected=actual rồi tự phê duyệt. Không cần gửi API key vào chat. Mẫu provenance và giới hạn quyền nằm trong request kế tiếp.

## Vì sao vẫn lặp

Chuỗi mới vẫn lặp kiểu cũ: viết consumer + tự tạo fixture đạt → gọi chung là host complete → chuyển dependency cho owner → chưa chứng minh producer/event linkage → audit sau phát hiện boundary còn trống. 1.495 dòng helper không phải thước đo completeness. Vấn đề nằm ở bằng chứng end-to-end và tiêu chí kết thúc, không phải cần thêm một lượng tài liệu tùy ý.

Audit trước của chính tôi cũng có phần cần cải thiện: request 50 bước đòi readback/schema-derived nhưng chưa buộc bàn giao producer executable và bảng field→source→transformation→evidence. Điều đó để lại chỗ cho “verifier có fixture” được hiểu là đã hoàn tất F3. Request mới bổ sung acceptance cụ thể cho khoảng trống này, giữ F2 đã đóng và không yêu cầu rerun mọi lịch sử.

Tài liệu còn drift: runbook/reconciliation vẫn có banner audit 14/9 trong khi canonical/state ghi host complete 15/9; snapshot gọi helper là working-tree change dù helper đã commit c2c79a19. Audit này sửa điều hướng/current và baseline, không thay bằng chứng đã pin C54…E3C hoặc tự đánh dấu release 05–09.

## Historical Resolution record — prior local F3 fixture repair

Local repair đã hoàn tất trên cùng branch, không đổi production source, AndroidTest source, schema, migration, pack/profile, prompt, model/route, budget, input identity hoặc artifact pin. Helper mới có collector entry point `Invoke-P5ESyntheticReadbackCollector`; nó chỉ đọc một disposable event directory gồm `collector-input.json`, `before-snapshot.json`, `after-snapshot.json`, `transaction-evidence.json`, `report.bin` và `receipt.bin`, rồi ghi `post-readback.json`. Collector này không gọi ADB, instrumentation, provider, database thật hoặc credential; vì vậy đây là bằng chứng producer→verifier offline, chưa phải bằng chứng collector live trên device.

| Kiểm tra | Evidence và kết quả |
|---|---|
| Bốn mutation bị lọt trước đây | `observed_before_run`, `consumed_after_expiry`, `artifact_manifest_mismatch`, `wrong_evidence_directory` đều `RAW_NOT_ACCEPTED`; `valid_control` và `late_observation_control` đều `RAW_ACCEPTED`. Consume dùng nửa kín `issued <= consumed < expires`; readback có thể quan sát sau expiry nếu chronology hợp lệ. |
| Authorization boundary | Năm fixture độc lập `issued-1`, `issued`, `expires-1`, `expires`, `expires+1` cho kết quả lần lượt `reject`, `accept`, `accept`, `reject`, `reject`; các timestamp phụ được điều chỉnh để kiểm đúng boundary thay vì tạo lỗi chronology phụ. |
| Manifest/artifact identity | Report và receipt được đọc từ bytes thật trong fixture, validate schema/identity/hash/length; cả hai phải mang manifest fingerprint expected từ source `EditorialP5PilotRequest.manifestFingerprint()`/`canonicalJson()`: `0353d751924d02ef0928bb6460c4ab894fee7c6324506e62b2972090e519c4da`. Khác nhau, hoặc cùng nhau nhưng sai expected, đều bị từ chối. Đây là pack manifest identity, không phải account fingerprint. |
| Event/file provenance | Metadata, event id, run identity, canonical evidence directory, metadata path, post-readback path, sáu input path, source hashes và collector implementation hash phải trỏ đúng event đã mở; đổi evidence directory bị từ chối. Collector identity là `p5e.raw.host-readback-collector.v1`, source mapping là `p5e.raw.readback.source-map.v1`, helper hash hiện hành là `6FABAE1F53973942561EDA52002F052F775570A931EAB65188103E7DEE9955A2`. |
| Timing/atomicity | `issued <= claim/consume < expires`, `before <= claim`, `attempt.created <= attempt.updated <= observed <= collected`; observation muộn hợp lệ không bị loại chỉ vì auth đã hết hạn. Transaction evidence được tách khỏi before/after row-pair và artifact-byte validator; không suy `atomicClaim` từ snapshot cuối đơn lẻ. |
| Producer negative boundary | Producer trả typed stop cho missing report/receipt, wrong event, schema drift, WAL-incomplete snapshot và invalid validator output. Verifier cũng từ chối missing row/orphan lifecycle/duplicate attempt/wrong event/schema drift/WAL-incomplete snapshot/missing artifact/modified tuple/wrong source hash/unknown cost/invalid bytes; không retry, provider call hoặc sửa DB. |
| Fingerprint failure redaction | Self-test/probe fake success, error, timeout, JUnit `ComparisonFailure` và wrapper không để hai digest synthetic xuất hiện trong capture; `P5E_FINGERPRINT_FAILURE_REDACTION=PASS`, `redactionViolation=false`, `syntheticFingerprintStillPresent=false`. Đây không phải quan sát credential leak thật. |

### Field → source → transformation → gate

| Field group | Producer source | Transformation | Verifier gate |
|---|---|---|---|
| Package/artifact | `collector-input.json` (`production`, `test`) | copy exact package/version/code/APK/certificate/source commit | frozen production/test pins, cert and package identity |
| Fresh tuple/sources | `collector-input.json` (`freshTuple`) | preserve app-owned project, selector, chapter, binding/run/evaluation, pack/profile and four source roles/hashes/bytes | exact tuple, source projection, immutable identity |
| DB/schema/integrity | `before-snapshot.json`, `after-snapshot.json` | map consistent snapshot SHA/schema/integrity/FK and immutable tuple | `WAL_AWARE_CONSISTENT`, schema24, integrity `ok`, FK 0, source hashes/path binding |
| Lineage/allowed diff | before/after snapshot lineage plus transaction evidence | compare zero-before/allowlisted-after counts; do not edit DB | exact attempt/receipt/lifecycle/report pair, reconciliation/history 0, no unrelated write/delete |
| Attempt/auth/lifecycle | `collector-input.json` templates | bind event/run/attempt and replace artifact byte hash/length with validator output | one exact attempt, consumed receipt, `COMMITTED`, caps/timing/lifecycle identity |
| Report/receipt | `report.bin`, `receipt.bin` | UTF-8 no-BOM JSON parse, schema/identity checks, SHA-256 and byte length from actual bytes | valid stored bytes, matching manifest/tuple, validator result not input boolean |
| Atomicity/provenance | `transaction-evidence.json` plus event metadata and all input files | hash source files and keep transaction/row-pair evidence separate | transaction schema/flags, exact event/path/hash, chronology and collector identity |

The tracked result is `docs/P5E_PROVENANCE_REVIEW_RESULT.json`, SHA-256
`BCB2DE2BD98C8191EB32CBE8298089ADFB42A8DADF33231A2733B4C272B72D01`. It
records the two accepted controls, five exact authorization-boundary fixtures,
all rejected mutation/producer/verifier fixtures, source mapping,
`deviceActions=0`, `providerCalls=0` and `p6Ready=false`.

## Readiness

- F2 đã đóng theo evidence RED→GREEN cũ; F3 local evidence chain hiện đã đóng với helper/command hash gate, source-derived artifact contract và executable same-event collector. Không dùng fixture shape-only làm producer live.
- Host-only chưa chứng minh một lần thu thập trên pinned APK/device. Nếu runbook live được mở, phải dùng collector hiện hành ở đúng event và owner-permitted read-only source; không tự build/install hoặc dùng harness lịch sử.
- Với local evidence đã đạt, chỉ khi owner input/permission đúng và current device pins đạt trong cửa sổ đã được cho phép mới đánh giá one-shot RAW; tài liệu này không cấp quyền live.
- F1 vẫn thiếu expected fingerprint có provenance trusted và mapping account/key. Actual do app tự tính không được nâng thành expected; owner không cần gửi API key qua chat.
- RAW accepted chưa phải P5 exit. RECONCILE/phần L1 còn lại cần predecessor và scope tương ứng; P6 chỉ mở sau P5 exit. Ba chương L1–L3 là exit P6, không đặt thành prerequisite vòng tròn cho việc vào P6.

## QA và phản biện trước xuất

1. Có bằng chứng mới hay chỉ một vòng review cùng input? Có: 4 mutation false accept và assertion-redaction probe; lưu script tái hiện pin chính xác.
2. Có phủ nhận F2/F3 PASS cũ? Không; giới hạn đúng tập fixtures cũ. Không rerun F2 để trì hoãn sửa F3.
3. Có đòi cryptographic attestation platform mới? Không; bảng provenance, hashes/event binding, trusted collector và source-derived checks là đủ. Không thêm service/session-ledger/framework.
4. Có biến owner input thành blocker cho toàn bộ local work? Không; sửa R1–R3 trước, chuẩn bị lựa chọn F1 song song.
5. Có ép no-secret bằng cách bỏ toàn bộ failure evidence? Không; redacted type/status/field/hash allowlist vẫn phải đủ phân loại, fake canaries kiểm success/mismatch/timeout.
6. Có gọi probe thành QA release? Không; result mới chỉ chứng minh local F3 producer→verifier offline. Audit/request hoàn tất không có nghĩa host live/device/RAW/P5/P6 đạt.
7. Có viết nguyên nhân xóa code196 từ suy đoán? Không; giữ historical loss, không khẳng định actor thiếu evidence, không restore để tạo PASS.

Không credential/account check thật, ADB, instrumentation, provider, runtime authorization, DB readback, build/install/tag/merge trong audit này. Các số đếm trong fixture là dữ liệu giả.

### Kiểm tra bản xuất

- Artifact/backup được kiểm lại toàn payload: production 5/5, test 8/8, mismatch=0.
- Manifest remains `DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501`; current command is `30B50BFEF809225B33901AC130CC5EE83CC8AD76A3D958D987EF19C8FCA50D10`; helper/collector is `4D68F4BB0B0D31FA6D443439967746C1C83CCB4514EE6D7AF830CF90DBE0EC76`; production artifact contract is `FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`.
- Request giữ đúng 40 bước đánh số liên tục; template owner giữ NOT_PROVIDED/NOT_APPROVED, không giả quyết định.
- Snapshot phải ghi actual HEAD sau commit và tách implementation/source baseline khỏi snapshot commit.
- Checklist release 05–09 vẫn có đủ 5 mục chưa hoàn tất; diff app/editorial-engine/scripts rỗng; git diff --check đạt.
- Probe script/result được lưu trong docs để tái hiện; input synthetic của lần kiểm tra được tạo trong disposable output root ngoài repo `D:\P5E-provenance-offline-20260915-01`. Đây không phải source dữ liệu thật.
- QA bản xuất đạt các điều kiện local trên; **F3 local evidence-chain GREEN, F1/account và live execution vẫn pending**, không có tuyên bố release/session phát triển hoàn tất 14 bước.
