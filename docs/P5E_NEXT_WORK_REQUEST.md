# Yêu cầu làm việc + provenance — đóng local evidence chain trước A4.3

Status: `AUTHORIZED_LOCAL_WORK_BY_CANONICAL_P5E_SCOPE / LIVE_ACTIONS_NOT_AUTHORIZED / P6_NOT_READY`

Baseline re-audit: branch `feature/v4.18-p5e-audit-20260914`, input HEAD
`8c24b7d2a0d4ddc39ff07cfd2220ce14843258d8`. Khi resume phải lấy actual HEAD và
status mới từ Git; không reset về hash này. Đọc trước
`docs/P5E_READINESS_REAUDIT_20260915.md` và
`docs/P5E_READINESS_REAUDIT_RESULT.json`.

## Mục tiêu

Đóng bốn blocker local H1–H4 trước khi gửi packet cho owner:

1. command fail-closed nếu helper khác hash đã duyệt;
2. verifier kiểm đúng REPORT_L1/receipt bytes do production serializer tạo;
3. có executable, reviewable acquisition/collector chain cho post-live durable
   state, kể cả recovery/unknown;
4. selected live path tạo hoặc cho phép lấy event-bound post-dispatch evidence.

Sau khi bốn blocker GREEN, freeze lại packet và chuẩn bị một owner request cụ
thể cho F1 account provenance, read-only collection và đúng một RAW/GLOSSARY
dispatch. Work request này không cấp quyền đọc credential/account thật, ADB,
device, instrumentation, provider, runtime authorization, DB live, build,
install, force-stop, uninstall, clear-data, tag, merge hoặc release.

## Đầu ra bắt buộc

- patch tối thiểu cho helper/command/verifier và test-only readback source nếu
  cần;
- source-derived artifact contract cho report và receipt, không dùng một shape
  chung tự dựng;
- live collection runbook executable và recovery path có typed outcomes;
- RED→GREEN tests độc lập, gồm golden bytes từ production serializer;
- hash/provenance table cuối cho manifest, command, helper, source, APK và
  collector;
- owner decision request ở trạng thái `PENDING`, không chứa secret;
- canonical plan, build state, snapshot, checklist và các P5E banner cùng một
  current status;
- commit có diff thật và worktree sạch.

## A. Resume và đóng băng phạm vi

1. Vào `D:\App Translate Books\App Translate Books-translation-profile`; không
   sửa checkout D1 ở thư mục cha.
2. Đọc theo startup: `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, actual
   branch/HEAD/status, rồi `GIT_WORKFLOW.md`; đọc canonical plan
   `EDITORIAL_RECOVERY_V4_18.md` và checklist v4.18.
3. Xác nhận branch là audit continuation hiện hành và input HEAD `8c24b7d2` là
   ancestor; nếu có commit mới, review diff rồi tiếp tục từ actual HEAD.
4. Nếu worktree có thay đổi của người khác, phân loại và giữ nguyên; không
   reset/clean/stash/delete.
5. Ghi baseline table: current HEAD; production source; AndroidTest source;
   production/test APK hashes; manifest/command/helper hashes; checklist gates.
6. Recompute manifest `DD58…4501`, command `1D9A…DA15E`, helper `6FAB…55A2`.
   Khác hash thì dừng dùng các nhãn cũ, đọc diff và tạo baseline mới.
7. Xác nhận Android production source, schema/migration, route/model, budget,
   pack/profile và source identities chưa đổi từ pin; không rebuild để chứng
   minh một fact có thể kiểm bằng Git/hash.
8. Giữ F2 transport là PASS nếu self-test hiện hành còn pass; không viết lại
   quoting/timeout supervisor khi không có failure mới.
9. Đặt current state ngay từ đầu:
   `F3_NOT_CLOSED / OWNER_PACKET_NOT_READY / A4.3_NOT_ISSUED /
   RAW_NOT_RUN / P6_NOT_READY`.
10. Không đánh dấu checklist 05–09; local host/test-only work chưa phải release
    regression/build/QA.

## B. Tạo RED tests cho H1–H4 trước khi sửa

11. Chạy `docs/P5E_READINESS_REAUDIT_PROBE.ps1`; lưu output vào unique temp
    directory ngoài repo nếu cần, không ghi đè historical evidence.
12. H1 RED: copy helper sang temp, đổi một byte không-secret; chứng minh command
    hiện hành vẫn không có runtime helper hash gate. Test không được chạy
    `-Dispatch`.
13. H1 control: exact current helper hash được nhận; missing file, symlink/path
    swap, wrong hash và changed-after-review phải bị từ chối trước environment
    fingerprint read và trước mọi ADB process creation.
14. H2 RED: enumerate helper parameter sets; chứng minh không có
    `CollectReadback`/equivalent executable mode.
15. H2 RED: gọi `VerifyOutcome` với missing `post-readback.json`; expected
    `ACCEPTANCE_NOT_PROVEN`, provider/device actions `0`.
16. H3 RED: tạo report và receipt bằng đúng production serializer hoặc một Java
    golden test gọi trực tiếp `EditorialP5PilotExecution`; đưa bytes qua host
    validator hiện hành và ghi typed mismatch.
17. H3 control: giữ fixture synthetic cũ để chứng minh vì sao nó tự pass; label
    rõ `HOST_SYNTHETIC_SHAPE`, không gọi nó là production artifact.
18. H4 RED: source/contract test kiểm selected live method có `dispatchRaw` nhưng
    không có post-dispatch status/file emitter.
19. Test timeout/exception path: method không tới dòng sau `dispatchRaw` vẫn phải
    có recovery acquisition plan; không dựa duy nhất vào in-method success
    emitter.
20. Ghi RED result với expected/actual, exact source hashes và counts
    `deviceActions=0`, `providerCalls=0`, `credentialReads=0`.

## C. Sửa H3 từ production serializer ra ngoài

21. Đọc `EditorialP5PilotExecution.reportBytes` và `receiptBytes`; lập hai field
    lists riêng, không dùng một required/allowed list chung.
22. REPORT_L1 bắt buộc kiểm ít nhất: schema, artifact type, binding,
    manifest, canonical pack/profile, evaluation, chapter, phase, bundle,
    predecessor, totals, gates, evidence refs, inventory và disposition.
23. Receipt bắt buộc kiểm ít nhất: receipt schema, artifact type, manifest/pack/
    profile/binding refs, phase, bundle, predecessor, totals, gates, evidence
    refs, canon/propagation flags, disposition, inventory và release count.
24. Dùng parser/validator production nếu module boundary cho phép. Nếu host
    PowerShell phải parse JSON, derive field map từ golden contract test và pin
    source commit/hash; không copy một subset tùy ý.
25. Kiểm strict UTF-8/no BOM và canonical JSON bytes theo implementation; không
    canonicalize lại rồi gọi bytes mới là stored bytes.
26. Hash/length tính trên exact persisted bytes. Parsed semantic checks và byte
    identity là hai evidence khác nhau.
27. Ràng buộc report manifest/binding/pack/profile/evaluation/chapter/phase/
    predecessor với request pin.
28. Ràng buộc receipt refs với cùng request pin và bundle/predecessor với report;
    không yêu cầu receipt có tên field của report.
29. Kiểm report/receipt pair atomic: cả hai tồn tại trên cùng exact attempt hoặc
    cả hai vắng khi status không COMMITTED; một bên tồn tại phải fail.
30. Khi status `RECOVERY_REQUIRED`, không ép parse missing final artifacts; trả
    typed non-acceptance và giữ lifecycle/cost unknown đúng dữ liệu.
31. Golden cases: valid production report+receipt; wrong manifest; wrong binding;
    wrong bundle; wrong predecessor; swapped report/receipt; one-byte mutation;
    extra/missing field; BOM; invalid UTF-8; noncanonical bytes nếu production
    contract cấm.
32. Regression cũ: timing boundary `issued`, `expires-1`, `expires`; event/path;
    wrong source hash; unknown cost; fingerprint redaction.

## D. Thiết kế acquisition chain trước khi đổi AndroidTest

33. Lập bảng mọi field verifier cần và nguồn thật của nó: package manager,
    installed APK/cert, DB metadata, exact rows, stored blobs, test status,
    event metadata và host clock.
34. Với mỗi field ghi: source owner, query/API, transform, redaction, output file,
    hash, timing và failure code. Field không có nguồn phải là `UNKNOWN`, không
    được nhập tay `true`.
35. Xác định các nguồn sẵn có trên pinned test APK. Không suy “test access” từ
    source; kiểm class/method thực sự nằm trong source archive của APK pin.
36. Đánh giá ba path theo thứ tự:
    a. same live method ghi redacted post-state;
    b. host read-only recovery collector đọc durable state sau method;
    c. test-only source delta nếu a/b không đủ.
37. Chọn a+b nếu có thể: in-method output cho normal completion, independent
    persisted readback cho timeout/process death/device reconnect.
38. Không dùng full database export làm report mặc định. Nếu consistent snapshot
    phải được kéo để query, giữ private outside Git, giới hạn quyền, hash toàn
    DB/WAL/SHM, chỉ xuất allowlisted derived fields vào evidence.
39. WAL rule: app/process state phải phù hợp runbook; capture DB cùng WAL/SHM
    hoặc dùng SQLite backup/read transaction. Copy riêng main DB khi WAL pending
    là typed failure.
40. Không đọc/pull settings JSON, credential, raw endpoint, full RAW/DRAFT/
    GLOSSARY/PRONOUN, prompt, request body hay model response.
41. Report/receipt stored blobs chỉ được export nếu policy packet cho phép; nếu
    không, validator phải chạy trong trusted test context và xuất hash/length +
    typed validation proof có source binding.
42. Định nghĩa event directory trước dispatch với unique event id, immutable
    metadata and planned filenames. Không ghép readback từ event khác.
43. Định nghĩa before observation, claim/consume timestamps, after observation
    và collected time. Authorization window là nửa kín
    `issued <= consume < expires`; readback có thể muộn hơn expires.
44. Phân biệt DB file hash thay đổi hợp lệ với data integrity. Sau RAW, không yêu
    cầu post DB hash bằng pre hash; kiểm allowed rows, immutables, integrity/FK.

## E. Test-only live evidence delta nếu cần

45. Nếu pinned APK không thể cung cấp field/source cần thiết, ghi decision record
    giải thích vì sao host-only không đủ trước khi sửa AndroidTest.
46. Chỉ sửa `androidTest`/test support tối thiểu. Không đổi production source,
    schema/migration, provider adapter, pack/profile, prompt, source data,
    route/model hoặc budgets.
47. Thêm redacted post-dispatch emitter cho cả terminal states: COMMITTED,
    RECOVERY_REQUIRED, rejected-before-claim và exception có durable row.
48. Emitter phải query durable store lại sau result; không serialize duy nhất
    object in-memory được trả từ dispatch.
49. Output bind event id, run/attempt/request/authorization, exact package/test
    identity, row counts, status, lifecycle, persisted metrics và artifact
    hashes/lengths/validation result.
50. Không phát fingerprint nếu policy process-only cấm log. Receipt có fingerprint
    thì chỉ xuất equality proof/hash policy cho phép, không error text chứa
    expected/actual digest.
51. Không phát source text, prompt, request body, API key, normalized endpoint hay
    raw provider response.
52. Emitter không tạo authorization mới, không gọi provider lần hai, không repair,
    retry, reconcile, cleanup hoặc mutate unrelated rows.
53. Đảm bảo status channel/file có bounded size và exact framing; thêm parser
    contract test để tránh lặp lỗi A4 emitter/parser.
54. Thêm recovery collector mode host chỉ sau khi input, output và permission
    boundary đã rõ. Nó phải có `-CollectReadback`/equivalent explicit parameter
    set; default/self-test không chạm device.
55. Collector nhận exact serial/event/output root và pin values; không chọn first
    connected device, latest folder hoặc wildcard evidence.
56. Collector chỉ chạy read-only commands được allowlist; log command class và
    exit code, không log secret-bearing argv/output.
57. USB unavailable, package mismatch, active writer, WAL inconsistency, missing
    row/blob, parser error hoặc timeout đều typed stop; không force-stop hoặc
    redispatch để lấy PASS.
58. Nếu test source đổi, production APK giữ nguyên; AndroidTest APK phải build
    qua project-approved test artifact flow, unique event, exact source ZIP,
    artifact/backup parity và increasing identity theo existing policy.
59. Không cài test APK trong local work package này. Chuẩn bị riêng proposal cho
    one replacement + post-install readback nếu repin cần thiết.

## F. Sửa H1 và freeze executable chain

60. Finalize helper/collector trước khi viết expected helper hash vào command.
61. Tính SHA-256 helper final; thêm constant/parameter expected helper hash vào
    command review packet.
62. Trước mọi access đến fingerprint env hoặc device, command phải: resolve exact
    regular file; reject missing/reparse/unexpected path theo policy; compute
    SHA-256; compare exact expected.
63. Chỉ sau helper match mới gọi child process. Không dot-source helper, không
    print full argv, không pass credential.
64. Negative tests: absent helper, wrong path, altered byte, wrong expected hash,
    path swap/reparse where testable; tất cả launch count `0`.
65. Positive test dùng harmless fake helper/process seam; launch count `1`, argv
    byte-exact, no device/provider.
66. Sau helper final, hash command final. Nếu command đổi tiếp, recompute; không
    ghi self-hash vào chính command.
67. Manifest chỉ đổi nếu owner scope/DB effects/readback permission hoặc artifact
    pin đổi. Nếu đổi, finalize manifest trước, rồi cập nhật command expected
    manifest hash.
68. Proposal/owner request phải bind full manifest, command, helper, collector,
    production APK, test APK và source commits. Một hash trong prose không thay
    runtime check cần thiết.

## G. QA vòng 1 — kỹ thuật

69. Chạy helper self-test một lần trên final source; ghi từng group count và
    device/provider/credential actions `0`.
70. Chạy re-audit probe; H1–H4 expected phải chuyển sang resolved assertions,
    không sửa expected để biến failure thành PASS.
71. Chạy golden producer→verifier dùng production serializer bytes. Synthetic
    fixture cũ chỉ là regression phụ.
72. Chạy toàn bộ negative matrix H1/H3/acquisition: mutated helper; missing/
    partial rows; orphan lifecycle; duplicate attempt; wrong event/run/path;
    WAL incomplete; wrong artifact identity; byte mutation; missing cost;
    timeout/unknown.
73. Chạy parser/emitter contract tests nếu AndroidTest emitter đổi. Phải bắt
    missing field, extra field, wrong boolean type, truncated/multiple frames và
    redaction violations.
74. Chạy targeted Java tests cho serializer/validator source bị ảnh hưởng. Không
    chạy full release regression nếu chỉ host docs/script; nếu AndroidTest đổi,
    compile và chạy local unit/contract suite phù hợp trước artifact build.
75. Chạy `git diff --check`, secret scan theo allowlist hiện có và production
    diff guard. Any credential-like hit phải review; không in matching content.
76. Kiểm artifact/backup parity nếu tạo AndroidTest artifact mới. Không dùng
    build output tạm hoặc artifact không có source ZIP/hash manifest.

## H. QA vòng 2 — phản biện độc lập

77. Trace một valid result từ production serializer đến stored bytes, collector,
    source hashes, verifier và final decision; không bỏ qua bước bằng boolean
    nhập sẵn.
78. Thay một field ở từng boundary và xác nhận fail ở boundary gần nhất.
79. Giả định instrumentation trả `OK (1 test)` nhưng DB là
    `RECOVERY_REQUIRED`; final phải `RAW_NOT_ACCEPTED`.
80. Giả định host timeout sau provider reach nhưng trước output; state phải
    `UNKNOWN/RECOVERY`, không relaunch.
81. Giả định device mất sau COMMITTED trước readback; acceptance vẫn
    `NOT_PROVEN`, authorization/attempt không được tái dùng.
82. Giả định helper đổi sau owner review; command phải fail trước fingerprint/
    ADB.
83. Giả định report/receipt cùng match nhau nhưng sai manifest hoặc bundle;
    verifier phải reject.
84. Giả định actual account fingerprint khác expected; capture không chứa cả hai
    digest và launch/provider count bằng 0.
85. Giả định expected fingerprint được lấy từ chính actual mismatch; provenance
    reviewer phải reject circular source.
86. Giả định cost absent; không thay bằng `0`, không gọi RAW accepted.
87. Giả định old code196/A3.2 evidence được đưa vào current acceptance; reject vì
    historical/reconstructed classification.
88. Giả định RAW accepted; vẫn trả `P6_READY=false` cho tới khi RECONCILE/L1 và
    P5 exit được đánh giá riêng.
89. Nếu vòng phản biện phát hiện lỗi có evidence, sửa đúng boundary rồi rerun
    checks bị ảnh hưởng. Khi hai vòng pass trên cùng final hashes, đóng local
    gate; không mở lại cùng câu hỏi nếu input không đổi.

## I. Provenance owner cần sau local GREEN

90. Tạo owner request riêng, status `PENDING`, không gọi tài liệu này là approval.
91. Ghi authorized operator/account-project label/credential reference không
    secret và thời điểm xác minh.
92. Expected fingerprint source phải độc lập với live actual: trusted prior
    record hoặc separately approved enrollment procedure có account/key mapping.
93. Thuật toán pin đúng source:
    `SHA256(UTF8(normalizeEndpoint(baseUrl) + "\n" + apiKey))`, lowercase hex.
94. Owner không gửi API key/raw endpoint qua chat hoặc Git. Fingerprint transport
    dùng process-only channel đã review.
95. Owner decision phải cho phép riêng: memory-only settings load/hash/compare;
    read-only package/DB/artifact collection; RAW/GLOSSARY egress; one primary
    call; exact allowed DB effects.
96. Giữ DRAFT/PRONOUN hidden; repair0, retry0, fallback off, RECONCILE off;
    input100000, output4096, total104096, cost USD0.05, execution120000 ms,
    auth180000 ms, host observation240000 ms.
97. Nếu AndroidTest pin đổi, owner request phải ghi test artifact/source mới và
    quyền one replacement. Approval cũ không tự áp dụng cho hash mới.
98. Stop conditions: any pin/account/route/schema/tuple mismatch, device
    unavailable, active competing writer, expired window, unknown previous
    outcome, collector unavailable hoặc redaction failure.
99. No-redispatch: timeout/nonzero/USB loss/missing postcheck không cho refresh ID
    hoặc chạy lại. Chỉ owner có thể quyết định một recovery scope mới sau khi
    đọc durable state.

### Mẫu provenance phải điền trước owner review

Không copy giá trị lịch sử vào cột `Current value` nếu chưa recompute trên final
bytes. Không lưu API key/raw endpoint.

| Nhóm | Current value ban đầu | Nguồn và phép kiểm bắt buộc |
|---|---|---|
| Git baseline | `PENDING_FINAL_HEAD` | actual branch/HEAD/status; source commits tách production, AndroidTest và host |
| Production artifact | code207 / `2CCBB8…800FD`, phải recheck | immutable artifact + backup parity + certificate/source ZIP hashes |
| AndroidTest artifact | `57EC99…FDEA` hoặc `PENDING_REPIN` | exact APK/source ZIP/cert/package/runner; không dùng pin cũ nếu test source đổi |
| Production serializer | `PENDING_FINAL_SOURCE_HASH` | `EditorialP5PilotExecution` source commit/hash + golden byte generator test |
| Approval manifest | `PENDING_FINAL_HASH` | fixed scope/DB effects/egress/readback permission; finalize trước command |
| Helper/collector | `PENDING_FINAL_HASHES` | exact paths, implementation IDs, modes, source hashes và negative tamper test |
| Command | `PENDING_FINAL_HASH` | pins manifest/helper/APKs; helper check xảy ra trước env/device access |
| Event | `NOT_CREATED` | unique id/directory; command/helper/collector hashes; no cross-event file reuse |
| Account expected | `NOT_PROVIDED` | independent owner record/enrollment + non-secret account/key mapping + verified time |
| Account actual | `NOT_COMPUTED` | owner-permitted device-only algorithm; equality result only under redaction policy |
| DB before/after | `NOT_OBSERVED` | consistent snapshot/read transaction; schema/integrity/FK/exact rows/immutables |
| Report/receipt | `NOT_OBSERVED` | exact persisted bytes or trusted in-device validation; separate production shapes; hash/length/pair binding |
| Lifecycle/cost | `NOT_OBSERVED` | exact attempt rows; unknown stays unknown; no provider-dashboard inference without evidence |
| Timing | `NOT_ISSUED` | host event clock + durable claim/consume/update/observation; half-open auth window |
| Authorization | `NOT_APPROVED / NOT_CREATED` | owner decision binds final hashes and scope; runtime object only after all prechecks |
| Readiness | `LOCAL_REPAIR_REQUIRED` | local GREEN → owner-ready; owner approval → dispatch-ready; durable RAW → P5 evaluation; never skip levels |

## J. Live runbook có điều kiện — chỉ dùng sau explicit owner decision

100. Recompute tất cả final hashes và verify actual installed pins bằng đúng
     read-only scope owner cho phép.
101. Tạo unique event directory; ghi planned command/helper/collector hashes và
     fresh issued/expires ngay trước execution.
102. Thực hiện memory-only account comparison; mismatch dừng trước authorization
     construction và provider.
103. Capture before-state consistent snapshot/derived evidence; exact lineage ID
     phải unused, zero conflicting rows.
104. Chạy selected live method đúng một lần trong cùng supervised process; khi
     tool yield, tiếp tục theo dõi cùng process.
105. Không chạy preflight instrumentation riêng, model remediation, install,
     cleanup, retry, repair hoặc RECONCILE trong event RAW.
106. Sau terminal/timeout, chạy đúng read-only collector một lần cho cùng event.
     Nếu device unavailable, record missing postcheck; không relaunch dispatch.
107. Verify persisted attempt/auth claim pair, lifecycle, report/receipt/metrics,
     immutable tuple, no reconciliation/history, no unrelated write/delete.
108. Final decision matrix:
     - pre-claim failure: `NOT_DISPATCHED`;
     - uncertain reach/timeout/missing readback: `ACCEPTANCE_NOT_PROVEN`;
     - durable recovery/invalid artifacts/cost unknown: `RAW_NOT_ACCEPTED`;
     - exact COMMITTED + all provenance: `RAW_ACCEPTED`.
109. Dù RAW_ACCEPTED, output `P6_READY=false`. Mở work package RECONCILE/L1 riêng
     theo canonical P5 exit; không gọi provider tiếp tự động.

## K. Đồng bộ và bàn giao

110. Cập nhật `EDITORIAL_RECOVERY_V4_18.md`, `BUILD_STATE.md`,
     `WORKSPACE_SNAPSHOT.md`, checklist và P5E proposal với một current status.
111. Historical reports giữ nguyên evidence nhưng thêm supersession banner khi
     câu current cũ có thể gây hiểu nhầm; không sửa kết quả lịch sử.
112. Snapshot ghi current version/branch, implementation baseline trước snapshot
     commit, build pins, completed/pending, known bugs, regression và đúng một
     next action.
113. Commit các nhóm độc lập: contract/tests; collector/emitter; packet/docs.
     Không tạo commit rỗng hoặc gộp release work không liên quan.
114. Trước handoff: `git status --short`, log, diff-check, hashes và result files
     phải khớp; worktree sạch.
115. Báo cáo rõ ba mức: local gate, owner-decision readiness, live/P6 readiness.
     Không dùng “hoàn tất” chung cho cả ba.

## Acceptance criteria của work package local

Work package chỉ GREEN khi tất cả điều sau đồng thời đúng:

- helper runtime hash được command kiểm trước mọi sensitive/external action;
- golden bytes do production serializer tạo được verifier chấp nhận, mutation
  tương ứng bị từ chối;
- có executable live/recovery collector với field-to-source map và typed stops;
- selected live path hoặc recovery acquisition tạo được post-state có thể kiểm
  mà không cần redispatch;
- final helper/command/collector/test artifact hashes đã freeze và được bind;
- QA kỹ thuật + phản biện pass trên cùng final hashes;
- current docs không còn gọi synthetic-only F3 là GREEN;
- owner provenance/permission vẫn PENDING và không bị giả lập;
- A4.3 chưa chạy, RAW chưa được chấp nhận, P6 vẫn chưa mở.
