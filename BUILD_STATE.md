# Build State

## Current state — 2026-10-08 (Q2.4 dev gate stopped; holdout not authorized)

- Branch `feature/v4.18-p5e-runner-repair-20260917`; implementation baseline immediately before this state commit is `bdea3ad8`.
- Current build is `4.18-q2.1` / code `245`, source `45976adb`, installed only on `emulator-5554`; production and AndroidTest wrapper payloads are mirrored in `artifacts/` and `backup/`.
- Q2.1–Q2.3 are complete. Q2.4 dev 004–008 ran on both frozen arms; E-strong was structurally valid but failed the fixed quality selection gate, and V5-strong was invalid in every chapter because the model omitted `<FINAL>`.
- Q2 evidence is in `docs/EDITORIAL_API_V1_Q2_EXECUTION_20261008.md`; the price/build matrix is `docs/EDITORIAL_API_V1_Q2_AB_MANIFEST.json`.
- Current phase: EDITORIAL_API_V1 Q2.4 stopped before holdout; D-Q2 applied, U1 deferred.
- Completed tasks: engine/app offline tests, exact FULL CHATGPT pack hashes, V5 history/FINAL/truncation tests, cap policy, wrapper production/test archives, emulator gate, manifest, dev dispatch and scorer report.
- Pending tasks: owner decision on whether a contract/pack change is in scope; holdout 011/014/017 and `q2-outputs` were deliberately not run/created.
- Known bugs/limits: E-strong has 165 changed owner-unchanged lines versus the E-luna baseline 37 and two added kana/Han lines; V5-strong has five `V5_FINAL_MISSING` stops. No selected holdout quality result exists.
- Regression status: engine 566/566, app unit tasks PASS, AndroidTest compile and targeted emulator Editorial API tests PASS, Python verifier 14/14 PASS; Q2 ledger 10 settled logical reservations (20 entries / physical calls), USD 0.8375159, pending 0, UNKNOWN 0.
- Next action: owner decides whether to change the V5 input contract/pack or end Q2; no holdout dispatch until then.
## Superseded — independent API V1 review and N1–N4 checkpoint (historical, 2026-10-06)

The next two sections describe the review and checkpoint before the follow-up commits, N5 and `CP-OFFLINE-2`; keep them for history, do not read their next actions as current.

### Independent API V1 review — 2026-10-06

- Baseline HEAD/upstream ref `5bea4c93`; runtime APK source `18571c01`, code240. APK/test/source ZIP hashes verified in both archive roots; installed bytes not rechecked in this review.
- Two uncommitted local fixes: cancellation keeps returned call/token/cost evidence; incomplete C/C2 results retain candidate as FINAL_NOTES rather than falsely clean FINAL_OK. Isolated git-archive baseline plus these patches: API engine 63/63, app 44/44; new regressions failed before and passed after. No new build or device/provider operation.
- N4 readiness is narrowed: controller/render/callback tests do not prove real selector/SAF traversal or reopening the same combo after process death. Open source findings: missing glossary/pronoun in C/C2, unknown transport cost/retry handling, runner settlement clamps actual expense, view-local form values lost on refresh. N0 acceptance and N5 matrix/caps also need reconciliation.
- Actual calls/spend this review: 0 / USD 0. Three accepted API_V1 chapters 0/3 established; P7 unmet. Historical test/build results below remain evidence of their exact scope, not N5 readiness.
- Next action: close the remaining offline gaps in the existing N1–N4 package described in `docs/EDITORIAL_API_V1_N1_N4_EXECUTION_20261005.md`, then present corrected evidence and exact D-N4 proposal. No N5/provider/pilot action.

### Editorial API V1 N1–N4 checkpoint — 2026-10-06

- Current version: active v4.18. Current branch: feature/v4.18-p5e-runner-repair-20260917. The implementation baseline immediately before this documentation commit is 18571c0138436a699bdcc67851b2afb42b60d60b.
- Current build: 4.18-api.3/code240, source 18571c01; APK SHA-256 9F66F0C9D69F19728DD7EBB77138B946CDAF6672081F468A7B52638E5E4957C8; AndroidTest SHA-256 20CBCF3C69AE4E81C26B31FB73BB0C6B94FDC1282AB2F5AE38F6444D812A9381; mirrored production/test payloads are retained in the two archive roots.
- Current phase: EDITORIAL_API_V1 N1–N4 offline complete; stop before N5. N1–N3 were already implemented and pushed by Claude, and N4 is documented in docs/EDITORIAL_API_V1_N1_N4_EXECUTION_20261005.md.
- Completed tasks: engine 484/484, app 386/386, Python scripts/p6 71/71, AndroidTest compile PASS; emulator UI 3/3, force-stop/reopen 2/2, store/migration 4/4, fixture dry-run structural 1/1 with fake provider.
- Known bugs/limits: the dry-run scorer correctly reports semantic FAIL for seeded fx-a01; it is not a semantic acceptance and was not repaired. No model-quality result was measured because no provider was called.
- Regression status: all listed offline regressions PASS; actual provider calls and API spend are 0; pilot is untouched. Historical G1/G2 evidence and ledgers remain unchanged.
- Next action: owner decides D-N4 scope and budget before N5; no live dispatch, provider call or pilot operation is authorized by this package.

## Historical G2 approval preflight stop — 2026-10-05

Runtime/source baseline `270a27b9`; docs package `0d9b5507` and snapshot `84d10155` are pushed on the current branch. Owner approved the G2 proposal, but the first exact-predecessor preflight stopped before provider dispatch: `fx-a04` repeat-1 run `5d438ce3-1b6b-4fcd-8434-aa98deaa84a1` is not retained on the emulator, producing `P6_REUSED_L1_STATE_MISSING`. Fixture and prompt guards passed; actual provider calls and G2 spend are both 0. Do not substitute base `fx-a04`, whose report has a different hash and no finding. The preflight report is `docs/P6_R6_G2_PREFLIGHT_20261005.md`. G1 ledger remains 41 settled calls, USD 0.28859445 / 1.00, pending 0. Next action: owner decision on exact predecessor restoration or an explicit proposal change; no further G2 dispatch.

## Owner decision after U6 review — 2026-10-04

D-G1c replay gate now distinguishes valid-response PASS from hash-bound EXPECTED_REJECT for genuinely invalid model output; see wire simplification proposal section 12. U6 anchor mismatch is a correct rejection. Replay-all exit status/collection require offline repair before further live work; no immediate live permission is created. Build/pilot states below are unchanged.

## Current P6 item 12 checkpoint — 2026-10-04

- Current branch is `feature/v4.18-p5e-runner-repair-20260917`; implementation through `6cc32c25` contains the offline replay repair and bounded coverage reporting, and is pushed. The snapshot records this implementation baseline.
- No APK build, emulator/pilot operation or provider call occurred in this package. Existing U5 production/test artifacts and emulator evidence remain historical and unchanged.
- `EditorialWireReplayTool` now emits structured JSON schema 2 with hash-bound `PASS`/`EXPECTED_REJECT`, explicit legacy `UNSPECIFIED` mode and exit 0/2 semantics. `EditorialL1Ledger` keeps production fail-fast and adds bounded diagnostic collection for independent `findings[]` rows.
- Offline evidence: engine `391/391`; process replay `6/6`; anchor regression `6/6`. Missing/bad JSON, unexpected validation, hash/code mismatch and expected rejection with actual PASS all exit nonzero. Exact U6 response remains `REJECTED` with `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`; hash-bound EXPECTED_REJECT test exits 0 without accepting REPORT_L1.
- Report and replay evidence: `docs/P6_R6_REPLAY_REPAIR_20261004.md`, `D:\P5E-private\p6-item12-u7-replay-all-20261004.json`, and the exact response path recorded there. The second semantic finding is `UNRESOLVED_SUSPECTED_FALSE_POSITIVE`; no expected code was added.
- G1 ledger remains 9 settled calls / 18 entries, USD `0.07381520` spent / `1.00`, USD `0.92618480` remaining, pending 0. This package cost USD `0`; no G2 or pilot access.
- Next action: owner review of a concrete G1 proposal with hypothesis, source/prompt/schema revision, fixture, call/cost cap, stop conditions and scoring; no live dispatch from this offline package.

## P6 R6 wire v3 V1–V3 checkpoint — 2026-10-04

- V1/V2 PASS on implementation d1cc1452: engine 351/351, app 341/341, lint/AndroidTest compile PASS, Python 33/33. Offline wrapper 4.18-p6.16/code229, build-20261004-065614, APK SHA-256 98AAE03A23DD6D0573E2C5FFBCB46C6B18918D35D6A68B663143B0D5B6673141; AndroidTest event p6-unit-ref-d1cc1452-20261004-16 SHA-256 7A03D658CE7B83C59509791E6315976D943A67661D234B236F265E1777DF7AAF; source ZIP A7F543DBCC394C5295F61CF4FABF5FAE8750D827258D0E30430C3AFFFFEF2170. Mirrored payloads verified, installed emulator only. Preflight 5/5, coordinator 1/1, fake CHAIN 14/14 structural, typed negative L1_COVERAGE_GAP verifier/scorer PASS, 0 actual calls in V1–V2.
- Approved D-G1b V3 ran fx-a03 on same G1-20261003-ecbf8c55 ledger. RAW passed; RECONCILE rejected: P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID, phase=L1_RECONCILE, detail L1_TEXT_REQUIRED. Group stopped immediately. Two complete provider-reported calls: 50,650 input / 5,497 output tokens, USD 0.01925860; 0 retry/repair/UNKNOWN. G1 cumulative 4 calls/8 entries, USD 0.03222845/1.00, remaining 0.96777155, pending 0; prior bytes retained exactly. No G2/pilot access.
- Live partial STRUCTURAL_VALID 0/1; scorer semantic FAIL KNOWN_DEFECT_NOT_FIXED:T-S1 from fallback DRAFT, not model quality. No accepted REPORT_L1/final. Required text field name is unavailable in current safe diagnostic; response bodies were not retained. V1 d1cc1452 and V2 d9390733 pushed. Evidence: docs/P6_R6_UNIT_REF_EXECUTION_20261004.md; private run b4c0a9c2-118c-4d51-a172-955c6f95d999.
- Next action: diagnose L1_TEXT_REQUIRED offline and add a safe field-name diagnostic before any proposed further live G1 attempt.

## Current P6 G1-resume checkpoint — 2026-10-03

- W1–W2 đạt trên source commit `a178ff97`: production `4.18-p6.15`/code228 và AndroidTest event `p6-r6r7-a178ff97-20261003-15` được build bằng wrapper, archive hai nơi, cài trên `emulator-5554`; preflight `5/5`, coordinator `1/1`, fake CHAIN `STRUCTURAL_VALID 14/14`, 0 actual provider call.
- W2 negative gate đạt: `p6_fake_invalid_l1=YES` tạo `valid=false`, stage `L1`, `L1_COVERAGE_GAP`; verifier/scorer đọc được, 0 actual provider call.
- W3 group `G1-20261003-ecbf8c55` tiếp tục từ USD `0.0081852`, run `f7f71d89-9cc2-47fb-a8c7-4fd9e6d9bc2e` dừng tại `fx-a03` sau một call `L1_RAW_DISCOVERY`: `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`, chi tiết `L1_UNIT_UNKNOWN`, USD `0.00478465`, 0 UNKNOWN. Ledger cộng dồn USD `0.01296985` / trần `1.00`, còn USD `0.98703015`.
- Scorer live partial: `STRUCTURAL_VALID=0/1`; `SEMANTIC_EVAL=FAIL` vì final vẫn là DRAFT (`KNOWN_DEFECT_NOT_FIXED:T-S1`), không phải quality verdict. G2 chưa bắt đầu; không retry/repair call và không ghi API key/fingerprint.
- Report: `docs/P6_R6_G1_RESUME_EXECUTION_20261003.md`; private run: `D:\P5E-private\p6-runs\f7f71d89-9cc2-47fb-a8c7-4fd9e6d9bc2e\`.
- Next action: diagnose `L1_UNIT_UNKNOWN` offline and obtain owner authorization before any further live G1 call; do not start G2.

## P6 R6/R7 offline checkpoint — 2026-10-03

- P0–P3 offline execution is complete on `feature/v4.18-p5e-runner-repair-20260917`; available test/artifact gates pass, while C9's historical failure inventory remains open. P4 price and fixture/leak checks pass; no provider call was made. Source/report baseline before this P4 documentation commit is `fe082a6ab742c51451858d987712280f4e5a3a77`. The owner's pre-existing `.idea`, P5E files, evidence, artifacts and backups were preserved and not staged.
- Production `4.18-p6.11`/code224 was built with `scripts/build-and-save.ps1`, event `build-20261003-094552`, source `f1bf2268aa718232b155f8faf15a9450b8d2ddf5`; APK SHA-256 `4097F314313F19DBFBE5D7C38AB8A222D9296594F7542D4F7F5C6A78B9607397`, source ZIP SHA-256 `262C13728B515275A90C1242CD337D2B3AACF36252EC13A92BA13702D3718407`. App JVM `331/331`, lint PASS; payload parity exists under `artifacts/builds` and `backup/builds`. It is installed only on `emulator-5554`.
- AndroidTest archive `p6-r6r7-f1bf2268-20261003-11`, source `f1bf2268aa718232b155f8faf15a9450b8d2ddf5`, APK SHA-256 `A78E051B5D8AFD9291DC9FD55209DE1A8765074D6867CE3628CCD1603B46C5CA`, source ZIP SHA-256 `262C13728B515275A90C1242CD337D2B3AACF36252EC13A92BA13702D3718407`, mirrored in both test-build roots and installed only on `emulator-5554`. Safe offline R1–R4/v25 regression passed `18` selectors, `78/78` tests, `0` failures; logs are outside Git at `D:\P5E-private\p6-runs\regression-code224-20261003`.
- Fixture evidence: `fx-a01` CHAIN smoke committed `L3_FINAL_COMMITTED`, 8 fake calls, 0 actual calls, 16 spend-ledger entries. Full run `dcafc158-4f02-4fdc-b396-948a4d01f873`: `STRUCTURAL_VALID=14/14`, fake/no-edit `SEMANTIC_EVAL` FAIL 12/PASS 2 (not real translation-quality evidence), 224 spend entries, 0 pending UNKNOWN, 0 actual provider calls. P3 C8 is 191 units; 13 stale v24 assertions were updated across six disposable-database test classes. P3's available tests and artifacts pass, but full C9 inventory closure remains open because workspace evidence does not enumerate the requested 22 historical v24/seed failures.
- P4 price verified against OpenRouter’s official `openai/gpt-5.6-luna` page at 2026-10-03 03:29:53 UTC: USD 0.20/M uncached input, USD 0.25/M cache-write, USD 0.02/M cache-read, USD 1.20/M output. The pinned OpenAI route may auto-write cache for the long prompts, and the request does not disable automatic caching, so worst-case uses USD 0.25/M input. Totals: G1 0.9538944, G2 0.8752512, G3 0.596608, G4 0.298304, R6 2.7240576 (approved cap 3.25), R7 0.596608 (approved cap 0.75). `EditorialP6GroupSpendLedger` reservation rate matches this conservative upper bound.
- Rechecked fixtures after price lookup: `verify_fixtures.py` 14/14, errors 0; manifest SHA-256 `F60714B432ADC82CE14F9AC1FF461F5663E9DB9C1145BD8244E6A5DA70A1FC1E`; holdout lock SHA-256 `8872e28118242babb1f7333528d94fbd9ae7a462628a21e5e7816175a3714f21`. Full-run verifier: `STRUCTURAL_VALID=14/14`, identical duplicate output copies, 224 spend entries, 0 pending UNKNOWN, 0 actual provider calls. Fake/no-edit `SEMANTIC_EVAL` remains FAIL 12/PASS 2 and is not quality evidence.
- The owner supplied an endpoint/account fingerprint; its value is not stored in Git. Codex did not read the API key or compare that fingerprint against app Settings. No provider request, pilot-device access, or main-database operation occurred. P5/live G1 and release certification remain unstarted/unclaimed. See `docs/P6_R6_R7_OFFLINE_EXECUTION_20261003.md`.
- Current next action: wait for owner direction to continue into P5/live G1 after reviewing the P4 result and the open C9/fingerprint-match limitations.

## Previous planning handoff — 2026-10-01 (historical; superseded by the P6 checkpoint above)

- P6 R0–R7 (rolling): R5 is docs, a Python scorer and its tests: no build. Last archived build 4.18-p6.5/code218 (source 850458b0), installed on emulator-5554 only; the pilot still runs 4.18-p6.2/code215.
- P6 W4–W6 (2026-10-02): pilot `15e84958` now runs `4.18-p6.2`/code215 (APK SHA-256 `51BA2A2B38730FC4498C92E1B687882BBDF8BBB5BD34D49F90BED63B8BCA9703`, build `build-20261002-082255`); one live L2/L3 chain on chapter 001 committed (`L3_FINAL_COMMITTED`, release numbers 0, USD 0.04257). Device DB after the run `C1F40D14…` (v25, integrity ok). Source after the install adds two UX fixes (not built, not installed): 312 app unit tests. See `docs/P6_W4_W6_CHAPTER_001_EVIDENCE_20261002.md`.
- P6 W1–W3 (2026-10-02): numbered build `4.18-p6.2`/code215, event `build-20261002-082255`, commit `4bc1aa27`, APK SHA-256 `51BA2A2B38730FC4498C92E1B687882BBDF8BBB5BD34D49F90BED63B8BCA9703`, unit 311/311, engine 255/255, lint PASS, parity `artifacts/builds` ↔ `backup/builds`; AndroidTest `p6-w3-prod215-20261002-01` APK `41ED827E…6500` in `artifacts/test-builds` and `backup/test-builds`. Installed on the emulator only; the pilot still runs `4.18-p5e.5`/code213. Earlier `4.18-p6.1`/214 is superseded. See `docs/P6_W3_EMULATOR_QA_20261002.md`.
- G5 offline (2026-10-02, `a6da1a35`): chapter progress/inspect, FINAL viewer and verified TXT export on the P4 chapter card; app unit 298/298, lint PASS, androidTest compile PASS; not built into an APK, not run on a device. Installed build is still `4.18-p5e.5`/code213. L2_RAW_DISCOVERY is not wired (see `EDITORIAL_RECOVERY_V4_18.md` section 10).
- M4 attempt 2 (2026-10-02): numbered build `4.18-p5e.5`/code213, event `build-20261002-073259`, commit `b3f6e3de`, APK SHA-256 `88854E478642B722C1BB31436683E3B1727C229F0C0673383FCF2413DD5C5793`, unit 289/289, lint PASS, parity `artifacts/builds` ↔ `backup/builds`; AndroidTest `p5e-m4b-prod213-20261002-01` APK `26CB0563…` (byte-identical to the installed one). Installed on `15e84958`. One event: `RECONCILE_COMMITTED` (attempt `7483b211…`, REPORT_L1 `b33baf33…`, receipt `11754968…`, USD 0.00747405); DB `06C4C48E…`; P5 exit `P5_EXIT_PASS_UNDER_EXCEPTION_B`. See `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` section Event M4 lần 2.
- M4 attempt 1 (2026-10-02): numbered build `4.18-p5e.4`/code212, event `build-20261002-071732`, source `2232ee63`, APK SHA-256 `8B6A46838B05DF73F31D2E506D27A2EBB3EA713D4AF3BC87128F40E002D1A2C4`, unit 289/289, lint PASS, payload identical in `artifacts/builds` and `backup/builds`; AndroidTest `p5e-m4-prod212-20261002-01` APK `26CB0563CC30957ECC249290E3CB920F41430D796A60715EE1AD55F0F8EC85AF` in `artifacts/test-builds` and `backup/test-builds`. Both installed on `15e84958` and read back. One event: `RECONCILE_NOT_DISPATCHED`, `P5_TOKEN_BUDGET_EXCEEDED`, `providerCalls=0` (byte-vs-token gate, 107,231 bytes vs 100,000); device DB now v25 `bfce0b42…`. Gate fixed offline in `e2e1d3c5` (engine 247/247, app 289/289, lint PASS); not built into an APK. P5.4 and P5 exit remain unmet. See `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` section Event M4.
- Owner decision B 2026-10-01: event 7 RAW attempt `7a5e3428…` is the only RECONCILE predecessor for chapter 001 (narrow exception; formal verdict unchanged). M4 offline package complete: `4a603695` (dispatchReconcile, RECONCILE-only fresh adapter, app 268/268), `1f8e7334` (opt-in M4 instrumented test, compiles), `a64ae460` (lean host script, self-test 74/74). G3/G4 offline: `63b288fe` L2 OpenRouter adapter, `e8b21d26` L3 boundary + v25 store for L3_FINAL. Regression on that tree: engine 244/244, app unit 276/276, lintDebug PASS, androidTest compile PASS. No build, device, DB or provider action; M4 live needs owner scope approval.
- Takeover update 2026-10-01 (after event 7): event 7 supplementary verification committed `2e4b7c2d` (original `dd41e858` verifier reproduces the verdict byte-for-byte on a copy; 24 original files unchanged; formal verdict stays `RAW_NOT_ACCEPTED`). The committed artifact is phase `L1_RAW_DISCOVERY` (RAW predecessor), not REPORT_L1 after RECONCILE. Offline RECONCILE readiness `7763085e` (compact wire for both L1 phases, `executeReconcile` on the committed RAW; fresh A4.3 route still refuses RECONCILE). G3 L2 engine `f7a95482` (change-map reconstructor + L2_EDIT boundary; editorial-engine 232/232, fake providers only) and schema v25 one-row artifact store `b4bf2f15` (app unit 264/264, lint PASS, androidTest compiles; 8 store cases not yet run on a device). Installing a v25 APK migrates the pilot DB: build M4 from `7763085e` (v24) or back up first. Installed code211 contains none of these; no build, device or provider action in this update. Canonical plan section 9a lists groups G1–G6.
- Owner requires the completed edited text after L1–L3; intermediate files need not be delivered separately. Internal phase/receipt evidence remains required.
- Canonical plan has been rewritten in place: `EDITORIAL_RECOVERY_V4_18.md`. Its previous contents are preserved verbatim in `docs/EDITORIAL_RECOVERY_V4_18_HISTORY_20260930.md`. Follow the rewritten plan's section 10 for the sole next action; historical next-action banners below do not control execution.
- Actual branch/HEAD at latest QA: `feature/v4.18-p5e-runner-repair-20260917` / `a4051e00c648c659589f1cccde4ae2a475a1bf17`, local/remote matched; index/working tree differ. Latest review changed documentation only.
- Latest QA correction: console report records two 68DF rejection cases, not successful child completion. Original failed owner-window prelaunch already matched 68DF, so reproducing CC01 silent exit does not establish that event's root cause. HEAD lacks entrypoint/parent dependencies of its committed console test. See the 2026-10-01 Claude QA section in `docs/P5E_CONSOLIDATED_FAILURES_20260930.md`. Next action remains bounded offline reproducibility closure; carry forward 1/2 rounds and approximately 15/60 minutes before review, adding actual active diagnosis/QA time. No automatic live retry.
- Workspace cleanup 2026-10-01: 65,456 generated/cache/temp files totaling approximately 7,798.88 MiB were moved to `D:\App Translate Books-cleanup-quarantine-20261001`; two canonical offline Gradle caches remain in the workspace. Fifty-nine stale missing-worktree metadata entries were pruned; source, evidence, artifacts, backups and the active audit worktree were preserved.
- Build remains code208 development export, not installed; code207 pilot pins remain applicable. P5 is incomplete, P6/P7 are not complete. No new runtime authorization or QA PASS is created by a plan.
- Supporting acceptance: `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md`; current code-gap map: `docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md`. Snapshot contains the compact current handoff.
- Agent continuation entrypoint: `HANDOFF.md`; it consolidates current gates, commands, failed approaches and the bounded offline next action. It creates no new runtime evidence.

## Retained build and execution evidence — historical next actions superseded

> Owner-requested development export 2026-09-30: 4.18-dev.1/code208 built via build-and-save.ps1 in isolated source snapshot 240cdc814a324bca36543a8091af6bf6cdd07831; 249/249 app unit tests, lint PASS. APK SHA-256 DB3FE9056A3477FA18D4F9F44A18E1FCCA0F7195F95DD99C91F1BD60AA6FE465. artifacts/builds and backup/builds parity PASS. Delivery artifacts/deliveries/TranslateBooks-v4.18-dev.1-code208-20260930.zip verified 9/9 entries and backup hash parity. Not installed; existing production code207/A4.3 pins unchanged; no P5/P6 certification. Consolidated report: docs/P5E_CONSOLIDATED_FAILURES_20260930.md.

> Current status 2026-09-30: the fresh visible owner-window attempt exited before the approval prompt, before reservation/receipt/key/child, and produced no audit file. The result is `OWNER_WINDOW_PRE_PROMPT_EXIT / EVIDENCE_INCOMPLETE`; provider/database/RAW outcomes are unknown, not zero. Follow Current completion priorities in EDITORIAL_RECOVERY_V4_18.md; no live retry or speculative launcher rewrite. P5/P6 remain unclaimed.

> Latest owner-window evidence — 2026-09-30: candidate hash `68DF8061CECB22D64AA4B85245714AC3AEE1895EBC8846F1BF51FC13D2E170BA` and 13 dependency pins matched before launch. DecisionId `p5e-a43-owner-4abe5015e63643c0b5bf31b1c9576c3a` used audit path `D:\P5E-private\.p5e-a43-audit\p5e-a43-owner-4abe5015e63643c0b5bf31b1c9576c3a.json`; the PowerShell process exited before the owner prompt, and the audit, reservation marker and owner root are absent. No owner decision, key, receipt, child, stage code or dispatch evidence exists. Do not infer counters from absence.

> Entry-boundary repair 2026-09-30: candidate SHA-256 68DF8061CECB22D64AA4B85245714AC3AEE1895EBC8846F1BF51FC13D2E170BA supersedes CC01C33F for execution. Preserves all entry arguments across dot-source and uses .ps1 command copy; integration 21/21 PASS. No live action. See P5E_A43_ENTRY_BOUNDARY_RUN_GUIDE_20260930.md.

> Current audit — 2026-09-30: `OFFLINE_PRE_RESERVATION_REPAIR_PASS / EXECUTABLE_INTEGRATION_OFFLINE_PASS / OWNER_WINDOW_PRE_PROMPT_EXIT / A4_3_OWNER_DECISION_NOT_RECEIVED / EVIDENCE_INCOMPLETE / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`. The repair evidence and all old audits remain immutable; this owner-window attempt created no observable authority, receipt or event evidence and was not retried.

> Parent/pre-reservation repair QA — 2026-09-30: template SHA-256 `9183619AD9C3BA5E5C0AF2701968F1F2121894472A8FD136C0DC6B54898F9A8D`; exact-source PS5.1 fixture SHA-256 `2F320FBB617249A8E7202B71D68C366C23ECF9C15289C7AAEDCD6D2749668131`; final report SHA-256 `DBBBCAB4E545E8B1779A2E01A2D7A562F7FB6F27DA1B8014E8F7E455525A2565`; `26/26 PASS`, live actions `0`, candidate executed `false`, no authority/receipt/event created. Child regression `27/27 PASS`, static QA `16/16 PASS`, PS5.1 parse and diff check pass. A4.3/P5/P6 are not opened.

> Executable integration QA — 2026-09-30: candidate SHA-256 `CC01C33F056F86F4ACC4E3BD45AAAA2910CCEE9719AC5830EDAE99987A7C8E23`; integration fixture SHA-256 `4353DE8FC2F60B7BCCA9FF5EB13A817F8FB22E1475CE2D614371B0FD38B5C423`; report SHA-256 `DC54F9788BDAA2D5D0FB50BC65B07B69485CA9235107FECD9DB4C63B1DD3E457`, `21/21 PASS`, process probes `0/1`, candidate executed `false`, live actions `0`. The exact final review/provenance/delivery records are linked from the canonical plan. This is not A4.3/P5/P6 evidence.

> Current offline pins: manifest `FCDC4747D54075B0518A92D837FC74C67D7F3804F9BAA4EFFC3CB34C7764A01D`; command `510F2A9A93CD9B566F9BC153B2745750840B0CEE78768788CB98EB7817E0D0D4`; helper `998A5E45F13F61A5F2B53B46E1F6CF57D97EF1C3A6690FC0854530F57A53C7BF`; guard `5F78F59FCFE5FD7BB904C8D0B4815CA7C714E03999DB97CDCA2737E5F4C3DC3E`; exporter `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99`; bridge `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`; toolchain `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8`; certificate `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`; serial `15e84958`.

> Final child-invocation evidence: contract `2A1474494E2760D851DC9378AF8252968A65A4779E2E1786434AB70918E6D2EB`, synthetic QA `EE03540BAB9BDFD754595C5AEBE5276CBED3875104BC78BECEDEE822EE32F328`, static QA `ADE91A69C08793E409FE7B37E91B35448B1E41128B8366390F24FB4905A1D7F1`, candidate launcher `D:\P5E-private\P5E_A43_FINAL_OWNER_SINGLE_USE_LAUNCHER_20260930_CHILD_SAFE.ps1` hash `569F8437A0630D52E8F26D5631AF2D9CBC089947BFD337E7FEC510B831D98A41`; candidate was not run during that historical offline QA; the subsequent pre-reservation attempt is recorded above.

> New launcher attempt — 2026-09-30: `P5E_A43_LAUNCHER_PRE_RESERVATION_STOP`; launcher hash gate passed, but no candidate reservation marker, owner root, receipt, command copy, event or child result was observed. The catch path erased the originating preflight/approval stop, so the exact cause remains unresolved. This is a concrete offline failing case, not live evidence.

> Final QA for this repair: targeted synthetic `PrepareEvent` exit `0`, `EVENT_PLAN.json` SHA-256 `D2C9DC9538D31D95641ABAC4F47F14844A7BCF6F561E6EE4A19047000A24108E`, stderr `0` bytes, eleven owner-binding fields present, exact serial/dependency pins present, and expected/secret values absent. Historical main `21/21`, atomicity/environment `28/28`, binding `262/262`, regression `175/175`, DB `56/56` remain PASS evidence and were not rerun; PowerShell 5.1 static QA and `git diff --check` pass. The manual-window launcher terminal result is `docs/P5E_A43_LAUNCHER_CHILD_INVOCATION_EXCEPTION_20260929.json`: command-copy hash/parse/path checks pass, but `RemoteException` left child exit unknown and no event plan was observed. Provider/database/RAW outcomes were not reached and are not inferred from metadata. The earlier consumed live event made no ADB/provider/DB/RAW call.

> Closed historical A4.3 outcome — 2026-09-26: owner decision was received and consumed by exactly one event. State is `OWNER_DECISION_RECEIVED_AND_CONSUMED / A4_3_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / P5E_COLLECTOR_BINDING_TUPLE_MISMATCH / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`. Result: `docs/P5E_A43_EVENT_RESULT_20260926.json`. This does not authorize the current offline repair or a new event.

> Historical consumed-packet audit — 2026-09-26: all final packet hashes matched at execution and the command was invoked once. Manifest `669C54049920C49344D2FB55533EFA9FA9F87E933A6A18DE5FA7215F1146D147`, helper `8A0509743403B28F7C074BE37DD7D08D41A1B9C0E56AC70D8A43024803CDF434`, exporter `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06`, bridge `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`, command `A2EF2BA90F07D3F4D2517E7F1541EA752615F6BCF61E304E579E301A8D5D3D08`.

> Current DB readback prerequisite — 2026-09-26: `OFFLINE_DB_HOST_READBACK_REPAIR_PASS`, with `56/56 PASS` bound to `docs/P5E_DB_HOST_READBACK_REPAIR_QA_20260925.json`. It was consumed offline through a synthetic captured-export copy only; the closed event was not reused. A4.3, RAW, P5 exit and P6 remain not ready.

> The consumed live packet remains historical: `PrepareEvent` completed, `pm-path-production-before` exited `1` without timeout and the outer code `125` reflected incomplete helper stream drain. The offline repair keeps the collector typed stop and the helper process exit/drain states separate.

> Current Next action: Diagnose and behaviorally test offline the concrete visible PowerShell argument/entry boundary that exited before the owner prompt; do not retry live or perform approval, key, device, provider or DB work.

> Superseded DB diagnosis — 2026-09-25: WAL/SHM presence exit `1` meant `ABSENT` under the helper contract. The sole proven blocker was the launched, non-timeout `database-consistent-read-transaction` exit `1`; native cause remains unproven. The bounded offline repair named there is now PASS; no live event is authorized by that result.

> Current version-contract repair: collector Android versionName expected is corrected to 4.17-p5e.11, independently confirmed by immutable BUILD_INFO and offline inspection of the hash-matched pulled production APK. Release label v4.17-p5e.11 remains a label, not Android versionName. Companion QA is `PASS` (collector version 5/5); the new event below is terminal and no retry occurred.

> Historical toolchain owner handoff: docs/P5E_PACKAGE_VERSION_NEXT_WORK_REQUEST_20260925.md bound the version-corrected working-tree packet. The owner decision was received and consumed by one new event; no new account provenance is needed and prior events remain closed.

> Offline launch repair 2026-09-25: explicit toolchain resolver/contract is implemented for RAW Before, Dispatch and After. It pins SDK configuration, build-tools 35.0.0, absolute adb.exe and Java+apksigner.jar paths, with typed redacted launch diagnostics and child expected-value environment clearing. Windows PowerShell 5.1 targeted QA PASS; no runtime/device/provider/DB/credential action occurred.

> Historical owner decision/result — 2026-09-25: the owner authorized exactly one new A4.3 event on serial `15e84958` under the version-corrected hash-bound packet. Event `raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586` ran once and stopped before dispatch; the decision is consumed and no retry or redispatch is permitted.

> A4.3 live outcome — 2026-09-25: `A4_3_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`. The Before collector stopped with typed detail `P5E_COLLECTOR_ADB_NONZERO`; no provider or credential read, live method dispatch, or device mutation occurred.

> Current offline identity repair — 2026-09-24: `ACCOUNT_IDENTITY_LIFECYCLE_REPAIRED_OFFLINE` remains PASS. The repaired account-only runner now has a terminal typed `MATCH`; no further offline parser work is authorized without a new failing case.

> Current event disposition — 2026-09-25: event `D:\P5E-private\raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586` ran exactly once and is now `CLOSED_A43_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP`. Fourteen read-only Before commands each launched once with bounded capture; 11 exited `0` and 3 exited `1` (`database-wal-presence`, `database-shm-presence`, `database-consistent-read-transaction`); `readOnlyCommandCount=14`, provider calls `0`, credential reads `0`, device mutations `0`, redispatches `0`. No After collector or live method dispatch was reached. Preserve this event and do not retry or reuse its directory.

> Superseded P5E state before DB repair — `HOST_SIGNATURE_LAYOUT_ADAPTER_OFFLINE_PASS / PREFLIGHT_PASS_7_READ_ONLY_CALLS / ACCOUNT_CHECK_COMPLETED_MATCH / ACCOUNT_RUNNER_COMPLETED_MATCH / A43_OFFLINE_PACKET_TECHNICAL_GATES_PASS / PACKAGE_VERSION_REPAIR_OFFLINE_PASS / A43_OWNER_DECISION_RECEIVED_AND_CONSUMED / A43_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / P5E_COLLECTOR_ADB_NONZERO / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`. No After collector, live method or provider dispatch was reached; no RAW acceptance is claimed.

> Live evidence: event `D:\P5E-private\raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586`; `EVENT_PLAN.json` SHA-256 `4F43A09D4717827708ABF8932975CDF168C6014A3E57FDF1BC338110D0EFCB95`; `COLLECTOR_OUTCOME.json` SHA-256 `0DF2BA4435DAC7AAB38D7493E64FD1707F8C44B8A7C9BE1ED4ADA1A9DEFC35C1`; `COLLECTOR_COMMAND_LOG.jsonl` SHA-256 `E0FBF7EB667586D97D7CFD314A090AF914D92848A946DB0E4D6685EA1BC5BE40`. WAL/SHM presence returned the valid semantic result `ABSENT`; the typed blocker is the consistent-read command exit `1`, whose native reason is unavailable in the redacted receipt.

> The approved owner decision is consumed by this terminal one-event STOP. The version-contract repair is PASS, but any future event requires a new owner decision after offline diagnosis of the ADB nonzero gate; do not retry/reuse this event or open P6.

> Historical consumed-packet pins: manifest `6C33FFA3742340E9099B83A676538B5D32D1942276CC3C9643E98054CE6E4EA4`; command `D0F1462725CA51DD789480D4BBEC87FE1AC8AC2BBB5024B9101A74B67E14403B`; root helper `4BF9E361AA2CD535C819EAE1D6F315F8DC929B989FE30285EAA98C9A0E98B31C`; resolver `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8`; companion QA `893D1E4888D1905D50EAB6594AF6C29DF2EE79DA244EAA72C9943A2A2296D3E8`. The live decision was consumed by the terminal STOP; no authorization remains reusable.

> Offline repair gate: `PASS` for toolchain resolution, package-version producer/consumer contract, path/reparse guards, Java+JAR signer contract, bounded timeout/capture, legacy-plan dispatch rejection, redacted diagnostics, expected-value environment boundary and fake Prepare→Before→Dispatch→After→Verify orchestration. Live gate: terminal Before collector STOP on typed ADB nonzero; no RAW or P6 acceptance.

> Superseded Next action: implement and QA the offline DB binary-export plus host-readback contract; completed by the PASS packet above. No device retry, A4.3, RAW or P6 action.

> Historical MISMATCH and prior `NOT_PROVEN` events remain unchanged as provenance. The agent did not read the expected value or raw instrumentation; no secret or endpoint was recorded.


> Historical pre-event execution audit 2026-09-23 is retained below for provenance; it is superseded by the current event disposition above.


- Historical proposal gate: AUTHORIZED_LOCAL_WORK_BY_CANONICAL_P5E_SCOPE / F2_TRANSPORT_QUALIFIED / F3_LOCAL_BEHAVIORAL_GATE_GREEN / OWNER_ACCOUNT_CHECK_SCOPE_RECEIVED / ACCOUNT_TEST_INSTALLER_QUALIFIED_OFFLINE / ACCOUNT_TEST_CHECKONLY_PASS / TEST_PACKAGE_REPLACEMENT_PASS / HOST_RUNNER_REPAIR_OFFLINE_PASS / EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW / EXPECTED_VALUE_LOADER_OFFLINE_PASS / EXPECTED_VALUE_PROCESS_SHAPE_PASS_AT_EVENT_START / ACCOUNT_EVENT_SIGNATURE_LAYOUT_ADAPTER_01_CLOSED / PREFLIGHT_PASS / ACCOUNT_CHECK_NOT_PROVEN / ACCOUNT_RUNNER_IDENTITY_VALIDATION_STOP / F1_EXPECTED_ENDPOINT_ACCOUNT_FINGERPRINT_PENDING / A4_3_NOT_ISSUED / RAW_NOT_RUN / NO_RUNTIME_AUTHORIZATION_CREATED / PROVIDER_CALLS_ZERO / DB_WRITES_ZERO / RAW_DISPATCHES_ZERO / P5_EXIT_NOT_CLAIMED / P6_NOT_READY. The A4.2 authorization remains consumed; this one event is terminal and does not authorize a retry.

- Status: P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED / P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED / P5E_9B_A3_1_TECHNICAL_PASS / P5E_9B_A3_1R_DOCUMENTATION_AND_EVIDENCE_PASS / P5E_9B_A3_2_MODEL_MISMATCH_OBSERVED_HISTORICAL / P5E_9B_A3_2_PRESERVATION_NOT_PROVEN_HISTORICAL / P5E_9B_A4_MODEL_REMEDIATION_PASS_HISTORICAL / P5E_9B_A4_EXACT_PREFLIGHT_EVIDENCE_CHANNEL_FAILED_HISTORICAL / P5E_WORKFLOW_PRETAG_FAIL_CLOSED / P5E_9B_A4_1_HOST_CONTRACT_PASS / P5E_9B_A4_1_EMITTER_PARSER_CONTRACT_PASS / P5E_9B_A4_2_EXACT_PREFLIGHT_PASS / P5E_9B_A4_3_HOST_FIXTURES_PASS_HISTORICAL / F2_TRANSPORT_QUALIFIED / F3_LOCAL_BEHAVIORAL_GATE_GREEN / HOST_RUNNER_REPAIR_OFFLINE_PASS / EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW / EXPECTED_VALUE_LOADER_OFFLINE_PASS / EXPECTED_VALUE_PROCESS_SHAPE_PASS_AT_EVENT_START / ACCOUNT_CHECK_PREFLIGHT_DEVICE_STOP / F1_EXPECTED_ENDPOINT_ACCOUNT_FINGERPRINT_PENDING / RAW_AUTHORIZATION_REQUIRED / A4_3_NOT_ISSUED / RAW_NOT_RUN / NO_RUNTIME_AUTHORIZATION_CREATED / NO_LIVE_CALL_PERFORMED / PROVIDER_CALLS_ZERO / RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / P6_NOT_READY. Historical completed statuses remain in the evidence sections below and are not current live-readiness claims; the owner device STOP is current evidence and is not an account result.
- Active authority: V5-SAFE.4.1.3-FULL; canonical plan `EDITORIAL_RECOVERY_V4_18.md`.
- Workspace: D:\App Translate Books is the active repair checkout on `feature/v4.18-p5e-runner-repair-20260917`; nested `App Translate Books-translation-profile` remains the parent audit worktree at `db469415`. Root IDE/untracked material is user-owned and outside P5E.
- Current account-check source baseline: `9e5ffb7819bfb91dcb8ed9e25c901ab10aa48390`; host-installer repair commit `afc34b87158ccdd14ee109d3242fbb3c1be9ca7d`, SHA-256 `21AADE819DB83464E96C0BB6AC28CB42FB26AB916D5905AD13CED37C15FC786B`; repair branch `feature/v4.18-p5e-runner-repair-20260917` is derived from the audit branch. Production source and historical RAW command/test/artifact pins remain unchanged; the repaired helper has a new host-only pin recorded in `docs/P5E_ACCOUNT_RUNNER_REPAIR_RESULT_20260917.json`. The account-only test replacement is a separate test artifact built from the source baseline. Prior probes remain historical evidence; current local readiness is bound by `docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json`, `docs/P5E_ACCOUNT_CHECK_LOCAL_RESULT_20260916.json`, `docs/P5E_ACCOUNT_TEST_INSTALLER_QA_20260917.json` and the new repair QA.
- Historical implementation HEAD before the A3.2 result documentation commit: `9eaeaee322d38ddf66fe515f9726726400d4fe05`; the current implementation/test HEAD is recorded below. The result documentation commit is separate from this historical implementation baseline.
- Branch: `feature/v4.18-p5e-runner-repair-20260917` (host-only repair continuation; parent audit branch and release branch `feature/v4.18` are unchanged).
- Historical account-runner repair pins: account runner `96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65`, helper `5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34`, repair QA `D8940D498AB9DABBBFED4A0A31013448622E266D30ACBC2AEFF7C9E96FEF82D7`; source-only/fake-process QA is 37/37 PASS. The corrected expected-value loader is `1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D` with QA `933A7780700F3562106992BFF3F801B841AB09227C4A2664D192685541914E21`, 27/27 synthetic PASS. The current A4.3 root-runtime pin is listed above; these historical pins do not authorize account/device/RAW execution.
- Current post-STOP preflight/orchestration repair: source-derived wrapper adapter preflight `09A33DC74820A1AA18B3EE47AA96862DC0AFA03067D5B46B433B74E4EB1AFA22`, Windows PowerShell 5.1 QA `86A13ADBB27AABAC3D409DCBE65B47FDCDB8DF00CCD10DE992DB4B14ECC67E50` at `189/189`; one-attempt command `B79A6C7AD05DE30249DEBAA6C3E1E4FFBD0326754CFD3F14AFC0B69E265F1298`, synthetic orchestration QA `EB8A9BBA81F1F08419810B52C9BFBDACE4AE2669E0C52C1AA5B963A100597895` at `28/28`; path guard QA remains `12/12` at `7AAC21254FA13BAD3D760E099EB483E4AB29E947CB9474735F1F715DE3680080`. These host gates preceded the now-closed event; the current event result is `docs/P5E_ACCOUNT_EVENT_RESULT_20260924.json`.
- Pinned implementation/test source HEAD: `d51b7f3c16bdc482513b9904db07b97daed592d1`; host-repair start HEAD `0f52d36e516560bb33d294303c70fa1753cb64f9`; host-preparation commit `c2c79a19842f551fc752a53328024aab8ddb529d`; test correction commit `ea0d907a84a735b7ccb29237c1dd5add45defecf`; archive-policy documentation commit `2e08f3d392b044339a1df4cd883b6764d6b2a9d2`; build-tool corrections `1431b51156f0b9ba2d7080f10a751dcdc7335187` and `d51b7f3c16bdc482513b9904db07b97daed592d1`. Production source remains the frozen code207 candidate baseline and production was not rebuilt. The A4.2 result documentation is based on execution-start HEAD `005317cd83f107edbf275734cb2977b9929e88ce`; the documentation/result commit is separate.
- A4 pin table: the single canonical table is in `EDITORIAL_RECOVERY_V4_18.md` under “Canonical A4 current pin table”; this document references it rather than defining a second pin table. The A4.1 artifact was immutable and uninstalled in its host archive before A4.2; A4.2 replaced only the test package once and read back the exact bytes.
- Historical A3.1R commit baseline: actual resumed HEAD before A3.1R is `33cc68cf8195abf311d86604894226613303fc26`; implementation baseline is `a0009f04139431f0bee38d049f9b32e2b6b04c41`; A3.1 diagnostic test commit is `89eef75a4a62e5674d02b7e48eaaff012d9a7ae0`; A3.1R test-only commit is `9b59ce39b326d5e81e62861b150a618a5e80cddc`; documentation baseline before A3.1R is `fe265e7b579810942370ab4a1a203ca8070f001b`. This current entry keeps implementation/test baselines separate from the later documentation/result commit. Test-only correction `f2695c862a9b860e08fd01f932377ec5576d6ad1`, production lineage fix `ff6821a5de6f825c55e35f8570dfbf074b4e64b5` and prior test coverage `995d3b6c9678e93905b3802cf22eee0b091b1bb3` are ancestors. Candidate code204 is superseded as pre-correction evidence; code206 is historical installed evidence and code207 is the installed, frozen candidate. No production source, schema, migration, pack, profile, authority or final report/receipt schema changed in A3.1R.
- Last validation package built: production APK `4.17-p5e.11` / Android versionCode `207`, package `com.ml.tblandroidtxt`, event `build-20260911-201725`, APK SHA-256 `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`, source ZIP SHA-256 `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348`, build source snapshot `995d3b6c9678e93905b3802cf22eee0b091b1bb3`, production fix commit `ff6821a5de6f825c55e35f8570dfbf074b4e64b5`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; artifact and backup payloads are byte-identical. It was installed exactly once through the approved guard and read back successfully. The production package was last verified on device as `v4.17-p5e.11`/code207 with signature token `abebea4b`; the last verified DB was schema v24 with SHA-256 `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`, data `RECONSTRUCTED_ONLY`. The device later became unavailable during A3.2 post-run read-only verification, so this is a last-known fact rather than a current connectivity claim.
- The old LQ test APK at `D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk` with SHA-256 `9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2` is superseded and was not installed. The approved replacement is outside Git at `D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk`, package `com.ml.tblandroidtxt.test`, runner `androidx.test.runner.AndroidJUnitRunner`, SHA-256 `50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587`, size `1313798` bytes, test-source commit `f2695c862a9b860e08fd01f932377ec5576d6ad1`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; it was installed once as the test package only and read back with the exact hash. The A3R artifact was later installed once as the test package only under the separate A3.2 approval; its immediate post-install readback was exact, but the device became unavailable before complete post-run verification.
- Historical phase and prior device/readback narrative: A2 remains fail-closed and its approval is consumed. A3.1R, A3.2, the earlier device A4 result and the pre-event account state are historical evidence. The current event result is recorded in `docs/P5E_ACCOUNT_EVENT_RESULT_20260924.json`; it is `NOT_PROVEN` and does not create a live RAW acceptance, authorization or predecessor.
- Current data classification is `RECONSTRUCTED_ONLY` with a new fresh-pilot lineage appended; `RECONSTRUCTED_ONLY_FRESH_PILOT` is the private snapshot classification, not a recovery claim for code196.

## Historical pre-repair local behavioral gate — superseded for readiness

The final helper/query/parser path now passes the source-derived SQLite behavioral chain: schema `24`, LINEAGE `17` columns, INPUT `7` columns, CLAIMED/RECOVERY NULL rows preserved, COMMITTED golden bytes retained exactly, lineage/reconciliation counts mapped, malformed output rejected, and partial artifacts not accepted. The targeted `EditorialP5PilotExecutionBoundaryTest` ran on Android Studio JBR `21.0.10` and exported the production serializer bytes; the host validator accepted the valid pair and rejected identity/byte mutations. Helper self-test and SQL boundary probe pass. Evidence and final hashes are in `docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json`; action counts are device/provider/credential/ADB/instrumentation/build `0`.

This is local/offline evidence only. The account-only test-package installed-pin/readback is recorded separately. Owner provenance metadata is accepted for review, but F1 expected fingerprint value loading, owner account/readback/egress permission and any live dispatch remain pending. A4.3 is not issued, RAW is not run, P5 exit is not claimed and P6 remains not ready. Do not mark release checklist steps 05–09 from this gate.

The owner has since approved one memory-only account check with only
`MATCH`/`MISMATCH` output and no key/fingerprint/endpoint logging, provider,
DB write or RAW dispatch. The pinned test method is not account-only: after its
fingerprint comparison it proceeds into DB/preflight and `dispatchRaw`. No
safe entry point exists on that pinned RAW artifact, so a separate test-only
verifier/runner was added and wrapper-built. Replacement APK hash is
`058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`, source
ZIP hash `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F`,
event `p5e-account-check-20260916-01`, installed and read back exactly once.
The expected process-only fingerprint has not been read or loaded; account
check remains `NOT_EXECUTED_EXPECTED_PROCESS_VALUE_PENDING`, and the no-live/P6
gate is unchanged. The owner metadata decision is recorded in
`docs/P5E_PROVENANCE_REVIEW_20260917.md`; no device follow-on is automatic.

## Historical A4.1/A4.2 and 2026-09-15 local closure claims (superseded for readiness)

- A4.1 started from clean baseline `ade5c3ed7c8d948505f5b37864f4e0e9635aacb8` and completed on `fix/v4.18-p5e-9b-a4-1`. The current host source/archive commit is `d51b7f3c16bdc482513b9904db07b97daed592d1`; production implementation, schema, pack/profile, authority, wire contract and version metadata are unchanged.
- The host RED audit confirmed the prior defect: the manifest contains `reconciliationCreated=false`, the parser requires it, and the previous emitter mapping omitted it. The actual AndroidTest emitter now maps that field immediately after `attemptCreated`; the contract test reads the implementation mapping and checks parser required/allowed/strict-boolean contracts, rather than relying only on raw fixtures.
- Host QA: JDK `21.0.10`, Android SDK `android-35`, Gradle `9.3.0`; targeted parser/emitter-contract tests PASS, AndroidTest compilation PASS, production/schema/pack/profile/wire/version diff guard `0`, secret scan PASS and `git diff --check` PASS. No ADB, device, provider, authorization, attempt, reconciliation, DB or settings operation occurred.
- A4.2 host preparation event `D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833` revalidated the source-derived preflight arguments and offline parser contract. Its checksum-file SHA-256 is `CCE290D4ABC11E58623633C5E2208BFCD51C7CA9A44A8978DEB5ACBCC78D59E3`; its preparation document SHA-256 is `C6B1CD88C212EA51DA698DF3AA89335FD0C6B7334E53B2617CD0DCFF45945BD6`, and the appendix distinguishes preparation HEAD `90c40c4959004657d527b9a385449589347e10aa` from execution-start HEAD `005317cd83f107edbf275734cb2977b9929e88ce`.
- New immutable AndroidTest-only artifact: `D:\App Translate Books\App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\app-debug-androidTest.apk`, `1155224` bytes, SHA-256 `57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA`, certificate `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`, package `com.ml.tblandroidtxt.test`, target `com.ml.tblandroidtxt`, runner `androidx.test.runner.AndroidJUnitRunner`, source/archive commit `d51b7f3c16bdc482513b9904db07b97daed592d1`, source ZIP SHA-256 `382EC5D12FC786BC358316434692E49A73BD62C2F9DD9AC6BD1BC9274CE0B3FA`, and identical backup under `backup\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532`. It was the pinned artifact in the single A4.2 test-package replacement and post-install readback matched.
- The prior artifact `DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B` remains outside Git and is `SUPERSEDED_NOT_INSTALLED_INVALID_STATUS_MAPPING`; it must not be used or installed. No exact-preflight readiness is claimed from either artifact.
- A4.2 evidence event: `D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591`; manifest `A4-2-EVIDENCE-MANIFEST.md` SHA-256 `7FBBECD3E2A8868D42D34CB3F9F2F8BA8CDAC236B4C7161F6747CBB19EE2474D`; raw instrumentation SHA-256 `1FFE572DF3ADEBA6A8AB55061BD75F2F5EEBBC479F8BB0D1EA97DB3957D1577C`; host parser result `accepted=true`. Pre/post DB SHA-256, schema24, integrity, FK, settings hash, fresh tuple and all lineage/report/receipt counts matched; all relevant counts remained zero.
- The A4.2 single-use approval is consumed and not reusable. F2 transport remains qualified. Local H1–H4 are now closed offline: command-side and helper-side runtime hash gates, separate source-derived report/receipt contract, executable Before/After readback collector with typed UNKNOWN/RECOVERY stops, same-event no-redispatch runbook, and 11 artifact mutation rejects. The production-serializer golden test source is present but was not executed because this request forbids build; synthetic bytes are explicitly regression-only. F1 expected fingerprint provenance and the exact owner permission remain pending. Current statuses are `F3_LOCAL_EVIDENCE_CHAIN_GREEN`, `OWNER_PACKET_PENDING`, `A4_3_NOT_ISSUED`, `RAW_NOT_RUN`, `NO_RUNTIME_AUTHORIZATION_CREATED`, `NO_LIVE_CALL_PERFORMED`, `PROVIDER_CALLS_ZERO`, `RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED`, `EXECUTION_DISABLED`, `NOT_CERTIFIED`, `P6_NOT_READY`; P5/P5E exit is not claimed. The single next action is owner review of the final hash-bound packet; no dispatch before that decision.

## Historical 2026-09-15 P5E hash and evidence chain

- Manifest remains unchanged: `DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501`.
- Final host helper/collector: `scripts/p5e-raw-live-supervisor.ps1` SHA-256 `4D68F4BB0B0D31FA6D443439967746C1C83CCB4514EE6D7AF830CF90DBE0EC76`; implementation `p5e.raw.host-readback-collector.v1`, version `2`, with bounded `COLLECTOR_COMMAND_LOG.jsonl` operation-class/exit metadata and no argv/output.
- Final review-only command: `docs/P5E_RAW_AUTHORIZATION_COMMAND.txt` SHA-256 `30B50BFEF809225B33901AC130CC5EE83CC8AD76A3D958D987EF19C8FCA50D10`; parser-only check PASS; it is not executed.
- Separate production artifact contract: `docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json` SHA-256 `FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`; serializer source SHA-256 `1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C` is pinned in the contract and checked by the helper.
- Offline result: `docs/P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json` SHA-256 `ECD61953C8E9C4E4539EC5B2B5865EDBC08D686E37F4C4743D67084887D8E725`; H1/H2/H3/H4 resolved assertions, self-test/probe/collector typed-stop and mutation evidence; device/provider/credential actions `0`; `p6Ready=false`.
- Host-preparation report: `docs/P5E_RAW_HOST_PREPARATION_20260915.md` SHA-256 `4A0E678ED298F4FF879062C9FD5F81A383D235F1C27ED4EAE7CA15563DEB6797`; current collector log contract is `COLLECTOR_COMMAND_LOG.jsonl`, same-event and bounded.
- No production source, AndroidTest source/archive pin, schema/migration, route/model, budget, pack/profile or APK changed. Existing production code207 and AndroidTest 57EC99 artifact/backup parity remain facts; no build/install occurred.

The historical A4.3 packet is preserved as the fixed-scope predecessor. The
current local packet is hash-bound for owner review but remains pending:
docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md has SHA-256
DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501, and
docs/P5E_RAW_AUTHORIZATION_COMMAND.txt has SHA-256
30B50BFEF809225B33901AC130CC5EE83CC8AD76A3D958D987EF19C8FCA50D10.
The narrative is in docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md. The packet keeps
authorization ID P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01 and its
0aa82c5897e3df3ec8a7a1586736dbf184b316c66ec165e95e64e8e4832145eb hash; the
A4.2 zero-count evidence shows that ID is unused.

The current host supervisor/collector is
`scripts/p5e-raw-live-supervisor.ps1`, SHA-256
`4D68F4BB0B0D31FA6D443439967746C1C83CCB4514EE6D7AF830CF90DBE0EC76`.
The source-derived report/receipt contract is
`docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json`, SHA-256
`FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`.
Self-test, readiness probe, provenance probe and a typed collector-stop test
prove the local chain; the 11-case synthetic artifact matrix is regression
only. Full current evidence is in
`docs/P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json`; both the local result
and the readiness probe keep `p6Ready=false`. No live result is claimed.

The prior helper hash
`BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799` remains
the RED input for the provenance review, not the current helper pin.

The account fingerprint is intentionally unresolved. Owner approval must
separately permit the source-defined in-memory operation using
SettingsStore.load(target).copy(), the normalized endpoint, a newline and the
in-memory credential as the SHA-256 input. No credential value, settings
content or raw endpoint may enter the command, logs or evidence. A mismatch or
unverifiable account stops before runtime authorization construction and
provider dispatch. Fresh issued/expires values are generated only at the
owner-approved dispatch; the host observation window is 240000 ms, authorization
validity is 180000 ms and the execution deadline is 120000 ms. No
instrumentation preflight rerun is part of this proposal. The host repair does
not supply owner fingerprint provenance or grant dispatch permission.

## Historical active state — P5E A4 model remediation and preflight evidence blocker

- A4 input baseline was `07a0cf949b97b696306a6b259cae318a5db1d58b` on `feature/v4.18`; current test-only correction HEAD is `911fb355a2148feb8c7ec4b60a843c547596bf56`. Production code207 was not rebuilt or changed. The frozen production artifact/backup/source ZIP hashes remain `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`, `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`, and `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348`; certificate remains `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
- A4 event `D:\P5E-private\a4-model-preflight-device-20260913-200643711` verified the production package on device, replaced only `com.ml.tblandroidtxt.test` once with SHA `5D248FFD33F52AC649966C4135773708C7CA747CE34C28BB763B40EC1FE81467`, and ran model remediation once. The settings file was `PRESENT` before and after; only hashes are recorded (`C2FC2DC71F3687E09F6D3899A99F397C3B75AD05B402D176367C03A0280E8062` → `4A0AA4564B62D5F9852A108B1D7991DA21AEF46DCDA4ACBC485E6E45CD3214F9`). The method reported model write/readback success, other-settings unchanged and route match; no source, prompt, settings content or credential was logged.
- The single exact-preflight invocation reported one test `OK (1 test)`, terminal `INSTRUMENTATION_CODE: -1`, provider/model/endpoint/route true, valid conjunction, DB preservation true and provider calls 0. Host parsing rejected it because `p5e.preflight.v2.reconciliationCreated` was absent. This is `P5E_9B_A4_EXACT_PREFLIGHT_EVIDENCE_CHANNEL_FAILED`, not `FRESH_RAW_EXACT_PREFLIGHT_READY`; the one-shot device boundary forbids rerun.
- DB readback before/after remained SHA `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`, schema24, integrity `ok`, FK 0, exact fresh tuple/source identities, and zero attempts, authorization receipts, reconciliation, history, lifecycle, report and receipt bytes. Actual A4 mutation/call counts were one test-package replacement, one model settings commit, two direct one-test instrument invocations, zero production-package operations, zero authorization/attempt/reconciliation creation and zero provider/API calls.
- The historical test-only correction added the missing false field but its artifact `D:\P5E-private\a4-fresh-raw-model-preflight-statusfix-20260913-202218809\app-debug-androidTest.apk`, SHA `DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B`, is `SUPERSEDED_NOT_INSTALLED_INVALID_STATUS_MAPPING` because the actual emitter mapping was still absent. It must not be used or installed. The current A4.1 artifact and next approval are recorded in the current section above; live authorization, RAW, RECONCILE, execution, certification and P6 remain disabled.

## Historical active state — P5E.9B-A3.2C delayed read-only closure blocked

- A2 is closed as a typed route-precondition failure, not a pass: `P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED` and `P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED`. The A2 evidence SHA-256 is `42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A`.
- Pre-run read-only verification passed in private event `D:\P5E-private\fresh-raw-route-diagnostic-a3.2-run-20260912-012928513`: device `15e84958`, production code207/APK/certificate/signature, A2 test APK hash, DB SHA-256 `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`, schema v24, integrity `ok`, FK violations `0`, fresh tuple and source hashes matched, and all table/lineage counts were zero. The current data classification remains `RECONSTRUCTED_ONLY`; historical code196 preservation and pilot preservation failures remain unchanged.
- The A3.1 implementation baseline is `a0009f04139431f0bee38d049f9b32e2b6b04c41`; documentation baseline before this group is `6a35b2de1ebbb4dcdb6e47cde8d0a1d060781d5e`. P5E.9, A2, and the P5 exit gate remain incomplete.
- `PreTag` was rerun after the A3.1R documentation repair and failed closed at `Step 05 is not complete for gate PreTag`; the earlier `Step 09` result remains historical A3.1 evidence. No tag or release action was taken. No false-green condition was observed.
- The approved A3.2 run replaced only `com.ml.tblandroidtxt.test` once with A3R SHA-256 `F19051D849139CB66C4005342AF45DEE62F2A1C7C44D7F0316EC810F8F4DBD1E`; the production package was not installed or modified. The single diagnostic method loaded settings read-only and emitted exactly four instrumentation-status booleans. It returned `providerMatch=true`, `modelMatch=false`, `endpointMatch=true`, `routeMatch=false`, with a valid conjunction and `OK (1 test)`. The device became unavailable during the first post-run read-only sequence; therefore the preservation gate is not proven and the diagnostic result is observed evidence, not an acceptance gate. The private evidence manifest is `12898E7A7DFFDCBEEF4E2E0BF6794E88C90EDE4E6DAD15D0147C9710ABF137CB`.
- Private A3 artifact: `D:\P5E-private\fresh-raw-route-diagnostic-a3-20260912-002937-test-apk\app-debug-androidTest.apk`, package `com.ml.tblandroidtxt.test`, target `com.ml.tblandroidtxt`, runner `androidx.test.runner.AndroidJUnitRunner`, source commit `89eef75a4a62e5674d02b7e48eaaff012d9a7ae0`, SHA-256 `64A9976F43F04397DF0E593F21E7AE154CDED1ED6749294F3B1E5F9D2A77757A`, size `1326212` bytes, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; it is retained as `SUPERSEDED_NOT_INSTALLED` and was not deleted. New A3R private artifact: `D:\P5E-private\fresh-raw-route-diagnostic-a3r-20260912-005638832-test-apk\app-debug-androidTest.apk`, package `com.ml.tblandroidtxt.test`, target `com.ml.tblandroidtxt`, runner `androidx.test.runner.AndroidJUnitRunner`, source commit `9b59ce39b326d5e81e62861b150a618a5e80cddc`, SHA-256 `F19051D849139CB66C4005342AF45DEE62F2A1C7C44D7F0316EC810F8F4DBD1E`, size `1326403` bytes, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; it was installed once as the test package only under approved A3.2. Immediate post-install readback matched; complete post-run readback was blocked when the device became unavailable.
- Host-only A3.2 preparation is retained in private event `D:\P5E-private\fresh-raw-route-diagnostic-a3.2-prep-20260912-011838007` with A3R manifest `8/8` and parser fixtures `10/10`. The approved runtime evidence is in `D:\P5E-private\fresh-raw-route-diagnostic-a3.2-run-20260912-012928513`; its raw output and redacted parse are retained. The original parser's terminal-code assumption was corrected only in the private evidence parser; no production code changed.
- Current PreTag evidence is `FAIL` at `Step 05 is not complete for gate PreTag`; the earlier `Step 09` result is retained only as historical A3.1 evidence.
- P5E.9B-A3.2C performed one availability check and then stopped when the production process was active (`com.ml.tblandroidtxt`, PID `420`). The process was not force-stopped. Package, settings and database delayed readback were therefore not performed. Private stop evidence is in `D:\P5E-private\fresh-raw-route-diagnostic-a3.2c-20260912-070137359` with event manifest SHA-256 `2EB4EDEC9376B7FAE692A5050105C8E263C7B223B3B2B7C03FC992C43BC70615`.
- The A3.2 route output remains observed evidence only: provider `true`, model `false`, endpoint `true`, route `false`; it is not preservation or acceptance. `IMMEDIATE_POST_RUN_PRESERVATION_NOT_OBSERVED` remains in force, and no delayed-readback match is claimed. The current root-cause characterization is that the pre-run settings file was absent, `SettingsStore.load` therefore used `AppSettings` defaults, and the default model `anthropic/claude-sonnet-4.6` did not match the required fresh RAW model; this is consistent with A3.2 and is not historical A2 proof.
- The historical next action at that time was a separately authorized read-only window after both production and test processes were idle; no automatic retry or force-stop was authorized. A4 remediation was not selected and was not executed. No authorization was created or consumed, no provider call occurred, and P5E.9B/P5 exit/P6 remained incomplete. This historical action is superseded by the current A4.1 approval gate above.

## Source and authority identity

- P0/P1 checkpoint: aef7da1.
- P2/P3A incoming checkpoint: b5589bf5f2b3b60942841950c5877e6f8281f7ff.
- P4 starting checkpoint: 270759e5589b2e9101c1e3a5a6b84cff12ec2fd3.
- Canonical ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-canonical.zip`; 23,638 bytes; SHA-256 `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`.
- Java control ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-java-control.zip`; 23,418 bytes; SHA-256 `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`.
- Project authority: 9,485 bytes; SHA-256 `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD`.
- Prompt authority: 8,852 bytes; SHA-256 `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F`.
- Workflow authority: 34,917 bytes; SHA-256 `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730`.
- Canonical pack hash: `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d`.

## Trusted profile

- Profile: `com.ml.tblandroidtxt.editorial.engine.safe4.full / 2.0.0`.
- Contract: `safe4.full.three-pass.v1`.
- Receipt schema: `safe4.full.receipt.v1`.
- Profile resource SHA-256: `1b2db011d59f3e2ef4349aeb0daa9c54a19b7efd1e2ca6886bc29b56e4690d62`.
- Canonical profile hash: `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21`.
- Machine contract fingerprint: `a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3`.
- Implemented P3B capabilities: exact 11/11 with owner, positive/negative test, fingerprint and source commit.
- Explicitly missing P3B capabilities: none.
- Profile sourceCommit: `3156835d1cc6b723d7932709224bf626dc7a1747`.
- executionEnabled: false; automatic replacement and project rebind: false.

## Completed evidence

- P2A/P2B and P3A GAP-012 complete: canonical/control import, immutable byte readback, idempotent re-import, 4.1.4 side-by-side and security negatives pass.
- P3B complete: P01-P09 `9/9`, G1-G24 `24/24`, typed stop/recovery, PRESERVE_DRAFT, receipt/ledger/diff/QA/release validators and trusted profile acceptance.
- P5 dry-run boundary: `EditorialP5PilotExecutionBoundaryTest` `15/15`; bounded authorization, exact binding checks, preflight-before-provider, phase projection, typed truncation/repair/recovery, local ledger/diff/receipt validation, token budget, idempotency and atomic-store failure are covered with an injected fake provider. No real provider or chapter was used.
- P5C app-bound fake E2E: additive SQLite v20 `editorial_p5c_attempts`, exact persisted P4 selector/binding resolution, RAW predecessor readback, RECONCILE, redacted report/receipt commit, database reopen and idempotent replay are covered by `EditorialP5CExactBindingFakeE2EInstrumentedTest`.
- P4 characterization documented the legacy hard-coded project owner and the v18 tuple/source identity boundary. Additive v19 P4 tables were introduced only after that failing persistence evidence.
- P4 selection/binding: explicit exact pack selection, immutable tuple, app-computed source identities, atomic project/revision/scope/declaration/binding transaction, idempotent retry and collision rejection.
- P4 resume: side-by-side canonical 4.1.3/synthetic 4.1.4, exact DB close/reopen readback, activity recreation UI proof, two-invocation host process-stop proof and stale-chain fail-closed checks.
- P4 post-closure correction: `EditorialP4BindingTransactionService` and the P4 UI now write only the contract vocabulary `NORMAL_FOUR_SOURCE`/`ALTERNATE_EXPLICIT`; the prior `NORMAL`/`ALTERNATE` mismatch was caught by a new canonical-mode assertion and fixed in commit `364faa4`.
- P4 focused device evidence on 15e84958: importer `13/13`, P1 `7/7`, P2 `3/3`, P3B trusted profile `1/1`, runtime wiring `5/5`, P4 binding `4/4`, process-stop preparation/resume `1/1 + 1/1`, UI recreation `3/3`.
- Focused P5C device classes: `22/22 PASS`; this includes P1/P2/P3B/P4/P5C exact-binding fake coverage.
- Full device instrumentation: `117/117 PASS`, `0` failures; real API remained opt-in/skipped. Five tests were added by P5C relative to the previous `112` total. The first run also exposed seven stale v19 assertions; only test expectations were corrected to current additive schema v20 in `744349e` and `61ba760`.
- Host engine suite: `178/178 PASS` (baseline `163`; P5 added `15` tests).
- App unit suite: `211/211 PASS` per debug/release/benchmark variant; aggregate `:app:test` `633/633 PASS`.
- Previous P5C validation APK: `artifacts/builds/v4.17-dev.13/build-20260904-202447/TranslateBooks-v4.17-dev.13-code181.apk`, SHA-256 `807D2E0C28BF3F486845562FFEE05B039FBA6D09AFDA566618C6C892979CD0F2`; retained as historical evidence with matching backup archive.
- External qualification `TESTS/test_full_release.ps1`: `306 PASS / 0 FAIL`.
- Profile and canonical pack/authority verification: PASS. Fake provider calls: `2` in isolated app acceptance. The authorized live boundary dispatched one RAW request; the app received no usable response/usage receipt, while authenticated OpenRouter metadata records one matching generation as `cancelled` with displayed usage cost `$0.00366`. Code169 baseline APK SHA-256 is `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1` in both artifact and backup; the device is restored to that baseline. The uninstall/reinstall procedure removed validation-package data because `pm clear` is not accepted on this device.
- `git diff --check`: PASS at closure.

- VOL5 RAW gate evidence: `docs/P5D_VOL5_RAW_AUTHORIZATION_BLOCKED.md`; the earlier missing-key invocation remains historical and made `providerCalls=0`. After the key was saved, setup instrumentation again passed `1/1`; one exact RAW request for selector `p5d-raw-mercedes-vol5-001` was dispatched. Durable readback is `RECOVERY_REQUIRED`, with no response identity, report bytes or receipt bytes; OpenRouter confirms generation `gen-1788910936-DHfTNOyDlU3f3PJOAvqb` as `cancelled`, 23,674/90 tokens and displayed cost `$0.00484`. See `docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md`; no RECONCILE call occurred.

- Device setup preparation: canonical 4.1.3 was imported through the production pack picker on code183 and reported `DATA_COMPATIBLE`; the persistent setup test pinned the normalized app-import bytes and read back the exact P4 tuple for chapter `001`. Source files remain outside Git under the user-provided MERCEDES VOL 4 folder.
- Code184 focused device regression: setup `1/1`, P5C fake E2E `5/5`, P1 `7/7`, P2 `3/3`, P3B `1/1`, P4 binding `4/4`, process-death `2/2`; full instrumentation `118/118 PASS` with the opt-in real API test skipped. Host engine `179/179 PASS`; app unit `214/214 PASS` per debug/release/benchmark variant, aggregate `642/642 PASS`.
- Authorized live boundary: one OpenRouter RAW request, `providerCalls=1`, no automatic retry/repair, no usable app response/usage receipt, typed `RETRY_PROVIDER_CALL_FAILED`, and durable `RECOVERY_REQUIRED`; recovery inspection `1/1` confirmed zero report/receipt bytes. The authenticated provider audit found generation `gen-1788877749-P8b2hBo1TWuduENuKbQ3` with finish `cancelled`, `17,808/84` tokens and displayed cost `$0.00366`; a new retry is eligible only after new authorization and explicit duplicate/billing-risk acknowledgement.
- P5D.1/P5D.2 provider audit: authenticated OpenRouter Logs metadata matched the old app/model/time/request-size tuple and classified the external generation as `EXTERNAL_CONFIRMED_CANCELLED`; I/O logging remained disabled and P5D provider calls remain `0`.
- P5D.3/P5D.4: additive v21 lifecycle, authorization-receipt and reconciliation owners; typed provider-failure taxonomy; non-reclaimable `RECOVERY_REQUIRED` gate; and redacted timing/generation metadata passed focused tests. Code186 device XML reports `124` test methods, `0` failures, `0` errors and `4` approved skips; engine `180/180`; app aggregate `645/645`.
- P5D code189 local harness closure: `EditorialP5CExactBindingFakeE2EInstrumentedTest` `13/13 PASS`; immediate, 11-second delayed-with-legacy-cancel and 11-second delayed-without-legacy-cancel `1/1` each; schema/migration device classes `41/41`; full instrumentation `130 tests, 0 failures`; engine/app debug unit XML `396/396`; external qualification `306/306`; no provider call. Detailed evidence: `docs/P5D_LOCAL_HTTP_HARNESS_REPORT.md`.
- P5D code189 DB readback: schema v22; historical VOL5 attempt remains `RECOVERY_REQUIRED`, prior authorization is consumed, response/report/receipt are absent, lifecycle and local reconciliation rows are absent for the historical attempt. RAW/DRAFT/GLOSSARY bytes match the binding; PRONOUN is 455 bytes/hash `63E79EEBCBFE6BEDCEA088640339EB7D05AEDA75C28FD4A4E17B282B8ED1A49C` raw with BOM and 452 bytes/hash `4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686` after the existing BOM removal, so the semantic source identity matches.
- P5D controlled diagnostic preflight rerun: code189 `EditorialP5CExactBindingFakeE2EInstrumentedTest` `13/13 PASS`, live recovery inspection `1/1 PASS`, and engine `EditorialP5PilotExecutionBoundaryTest` `16/16 PASS`; provider calls `0`, pilot DB unchanged. See `docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`.
- The approved authorization `P5D-VOL5-RAW-DIAGNOSTIC-20260909-01` was consumed once for the exact VOL5/chapter001 binding. One OpenRouter RAW primary call returned HTTP `200` with complete transport but ended at `2,048` output tokens (`finish=length`); app metrics were `20,327/2,048/22,375` tokens, reported cost `$0.0075392`, latency `20,590 ms`, schema/receipt invalid. Durable readback is `RECOVERY_REQUIRED` with lifecycle `RESPONSE_BODY_COMPLETE`, no response identity and zero report/receipt bytes. Full redacted evidence: `docs/P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md`.
- Output-budget alignment and recovery-history hardening are complete without a provider call: the prior `4,096 → 2,048` clamp was reproduced test-first, then removed so authorization/request/HTTP use the exact cap; v23 adds append-only reconciliation history without replacing the immutable primary row. Code191 validation focused device evidence is recorded in `docs/P5D_RAW_ACCEPTANCE_PREFLIGHT.md`.
- Code191 focused validation: VOL5 v23 recovery readback `1/1`, fake E2E `14/14`, schema/migration `41/41`, importer/P1/P2 `23/23`, P3B/P4 `5/5`; host engine `181/181`, app all unit variants `657/657`, external qualification `306/306`; provider calls for alignment/preflight `0`.
- RAW acceptance attempt evidence: authorization `P5D-VOL5-RAW-ACCEPTANCE-20260909-01` was consumed for exactly one dispatch. The final focused test APK (`CE5E29CABA15D08D3C6C2A032B961171C11428E28CC41A302792CA6D904CDCC4`) ran on `15e84958`; the process did not reach terminal result by the five-minute cap. Redacted readback retained lifecycle `RESPONSE_HEADERS_RECEIVED`, HTTP `200`, request bytes `85,068`, generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8`, and zero report/receipt bytes. Owner recovery closure changed `CLAIMED` to `RECOVERY_REQUIRED / RETRY_PROVIDER_CALL_TIMEOUT`; provider usage/cost and external completion remain unknown. See `docs/P5D_RAW_ACCEPTANCE_ATTEMPT_REPORT.md`.

- P5D deadline/body-read/recovery closure: production fix `d39bca7dcd16a64d6a97006d71e17f994653c065` gives the RAW adapter a scoped monotonic deadline, removes the legacy `+30s` grace from bounded pilot calls, keeps RAW cancellation separate from legacy global cancellation, bounds one-pass response-body buffering and persists redacted progress bytes. Schema v24 adds only `response_body_bytes`; stale claims recover fail-closed without redispatch. Code196 on device `15e84958` passed the focused class's prior `17/17`, short stalled-body `1/1` (`2.069s`) and five-minute stalled-body `1/1` (`301.501s`) with server request-count `1`; the five-minute run was not host force-stopped. Isolated P1-P5C/migration regression was `92/92`, VOL5 readback `1/1`, engine `183/183`, app unit `222/222` per debug/release/benchmark variant, external qualification `306/306`, and high-confidence secret scan found no credential. This validation made `0` provider/API calls. Details: `docs/P5D_LOCAL_HTTP_HARNESS_REPORT.md` and `docs/P5D_VALIDATION_REPORT.md`.
- P5E local contract gate: code191 metadata is reconciled as `EXTERNAL_CONFIRMED_CANCELLED`; the full legacy response shape is `49,665` bytes and the compact wire worst case is `2,785` bytes under a `3,584` byte ceiling, with the effective output cap retained at `4,096`. A separate `safe4.raw.discovery.wire.v1` schema, strict parser, app-owned RAW materializer, empty-change guard, reasoning accounting, evidence coverage and replay binding are implemented. Engine tests are `200/200 PASS`; app unit variants are `228/228 PASS` each (`684/684` aggregate); Android test compilation passes. No new provider call has been made.
- P5E focused regression on pre-upgrade code199 with the clean test APK passed manually via `adb shell am instrument` (no connected-test installer): raw-only fake boundary, raw-then-reconcile compatibility replay, compact-wire recovery evolution, immutable recovery-decision/history checks, reconstructed v24/VOL5 readback and persisted VOL5 setup (`7/7`). The raw-only fixture asserts exact RAW before/after and `declaredChanges=[]`; no provider call was made. Candidate code202 was then installed through the guarded path and its package/DB/source/pack readback matched the pre-upgrade invariants. Candidate-aligned test APK `63D3093CF68700A563CA979A9D15C3652FD8AB1DE60B219BDB35AE19442F76BC` from test-source commit `424278e44c042b882d1888f45d5c4b5b944e0dca` ran `EditorialP5EFreshPilotInstrumentedTest` directly through `adb shell am instrument` and passed `4/4`; fake predecessor/attempt rows were isolated. `:app:connectedDebugAndroidTest` remains fail-closed during Gradle configuration before any installer runs.
- P5E device preservation gate: the original code196 package data was lost when a connected instrumentation installer handled a version-downgrade attempt; no explicit uninstall/reset command was issued, but the package disappeared and no local DB backup was available. The v24/VOL5 data on device was rehydrated from the canonical pack and user-provided source files. Code197 → code198 and code198 → code199 `adb install -r`/reopen checks are `RECONSTRUCTED_ONLY`; they cannot be reported as code196 pilot-data preservation. An isolated no-provider identity probe reproduced the old evaluation-derived binding/run, but did not restore the missing attempt or reconciliation rows. `PILOT_DATA_PRESERVATION_GATE_FAILED`; no new authorization or provider dispatch followed. See `docs/P5E_INSTALL_AND_DATA_PRESERVATION_RUNBOOK.md` for the bounded installer evidence and no-backup decision.
- Fresh-pilot candidate rebaseline: the missing artifact payload pin was reproduced red against code201, then `scripts/install-validated.ps1` was changed to require exact APK SHA-256 and certificate SHA-256; `build-and-save.ps1 -Install` now passes the archive hash and requires certificate pin. Candidate code202 check-only and one guarded install passed with package/version/hash/certificate/device token; wrong hash, wrong certificate and missing certificate pin fail closed. No fallback or second install was attempted.
- Fresh-pilot local verification: the approved setup created selector `p5e-fresh-mercedes-vol5-20260911-01`, binding `845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf` and run declaration `8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc`, distinct from the old selector/binding. Source bytes and pinned pack/profile hashes were read back; current DB has schema v24, `2` projects, `2` chapters, `8` assets, `2` P4 bindings, `2` revisions, `2` scopes, `2` run declarations and `0` P5C attempts / P5D authorization receipts / reconciliation rows. Fake compact RAW success/replay, fault STOP/no partial commit and parser negative cases passed in isolated storage only; provider calls and live authorizations remain `0`.

## Validation artifact

- Latest validation APK: `artifacts/builds/v4.17-p5e.11/build-20260911-201725/TranslateBooks-v4.17-p5e.11-code207.apk`.
- Backup mirror: `backup/builds/v4.17-p5e.11/build-20260911-201725/TranslateBooks-v4.17-p5e.11-code207.apk`; the full artifact/backup payloads are byte-identical.
- APK SHA-256: `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`.
- APK certificate SHA-256: `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`.
- Build event source snapshot: `995d3b6c9678e93905b3802cf22eee0b091b1bb3`; production lineage fix `ff6821a`; code207 is an archived validation candidate, not a release build, and has not been installed. Source ZIP SHA-256: `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348`.
- Current installed baseline remains code206: `v4.17-p5e.10`, APK SHA-256 `F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080`, device `15e84958`, signature token `abebea4b`, schema v24 and DB SHA-256 `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`; it was installed once under the earlier owner-approved guarded path.
- Historical QF candidate-aligned AndroidTest APK SHA-256: `68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A`; test-source commit `34a4ec2832d71a488a2531a0e69a85261e9c9b9b`; it was separately approved, installed only as `com.ml.tblandroidtxt.test`, and passed `1/1` plus `5/5`; this is historical code206 zero-call evidence and is superseded for readiness by the LQ production fix.
- Superseded LQ candidate-aligned AndroidTest APK: outside Git at `D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk`, SHA-256 `9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2`, build source snapshot `995d3b6c9678e93905b3802cf22eee0b091b1bb3`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; not installed and not approved.
- Code186 remains immutable historical evidence for the prior lifecycle baseline; it is not the current validation artifact.
- Code184 remains immutable historical evidence for the pre-P5D live attempt; it is not the current validation artifact.
- Code201 and code203 remain pre-patch/historical artifacts and are not valid evidence for the changed source. Code202 is the pre-install historical candidate and retains its prior package/hash/certificate/data readback; code204 remains pre-correction evidence and code205 is an intermediate dirty-source build, neither is an approval candidate. Code206 is the installed prior candidate for the evaluation-provenance correction under the explicit local-only approval. Fresh-pilot local/fake evidence and the QF device result are retained as prior evidence; no live RAW acceptance is claimed.
- P5E.9A-EVAL/QF/LQ are now separated by evidence: the fresh binding freezes `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`; the old code206 QF run remains historical, while the code207-aligned QF methods and LQ lineage class passed under direct zero-call instrumentation. Code207 device installation and post-install data readback passed, including the unchanged DB SHA and zero lineage counts. No live provider response, valid-authorization dispatch, REPORT_L1, receipt or certification is claimed; P5E.9B still requires a separately approved RAW authorization.
- This is a validation APK, not a V4.18 release build.
- Historical P4 correction validation remains immutable and is recorded in
  `docs/P4_VALIDATION_REPORT.md` as code176; it is not the current validation
  artifact.

## Allowed-change guard

P4 production changes are limited to the P4 binding/selection/service/DAO
owners, the additive v19 migration, read-only project projection/setup UI and
the engine P4 value types. P5C production changes are limited to the additive
v20 durable attempt owner, app-bound exact-binding coordinator, the existing
engine pilot boundary and the bounded OpenRouter adapter/request context. P5D
production changes are limited to the additive v21 lifecycle, authorization
receipt, reconciliation and typed provider-failure owners, v22 content-type and
cancellation-source fields, v23 append-only reconciliation history, v24
response-body progress bytes, scoped monotonic deadline/body-read handling and
exact output-budget/response acceptance propagation through existing pilot
owners. No global activation,
certification path, provider framework, UI activation or legacy workflow rewrite
was added. Test changes are AndroidTest/engine-test plus stale current-schema
assertions only.
No authority byte, canonical ZIP, trusted profile resource, build metadata or
legacy workflow was changed. The original workspace `D:\App Translate Books`
was not modified.

## Known limitations

- P4 creates setup metadata only. It does not run L1/L2/L3, call a provider,
  create a model request, certify a pack or open Run/Start.
- The setup UI collects explicit initial source text for the metadata fixture;
  this is not source certification and is not an execution UI.
- `DATA_COMPATIBLE`, selectable and `PILOT_SETUP_READY` do not mean runnable.
- Bootstrap profile v1 remains loadable and non-executable.
- Code189/code191 attempts and their authorizations are historical evidence. Code191 generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` is reconciled from authenticated metadata as `EXTERNAL_CONFIRMED_CANCELLED` with input `23,674`, aggregate output/reasoning `0/0`, generation duration `9,642 ms` and upstream usage `0.0047348`; there is no completion timestamp/body or unambiguous billing flag, so `$0` is not recorded.
- The historical diagnostic attempt returned complete HTTP `200` transport but stopped at `max_output_tokens`; the app typed `RETRY_OUTPUT_TRUNCATED` and committed no predecessor/report/receipt. No automatic retry is allowed; a new exact-phase authorization would be required for another call.
- P5C proves app-bound fake attempt persistence and idempotent RAW→RECONCILE replay, but does not prove a live response, real token/cost usage, live cancellation/process-death behavior, or a real chapter `REPORT_L1`/receipt commit. RECONCILE and final L1 acceptance remain unproven.
- Device is on candidate validation code207 after one guarded `scripts/install-validated.ps1` upgrade from code206; its v24/VOL5 DB remains reconstructed validation data, not the lost code196 pilot DB. The pre-upgrade WAL-aware snapshot and isolated data-level restore passed, the production guard CheckOnly passed, and post-upgrade package/data/source/pack/profile readback passed. The approved test package replacement used the exact test APK hash; direct focused instrumentation passed the lineage tests `13/13`, the QF single method `1/1`, the QF boundary class `5/5`, and the focused schema/chapter checks `1/1` each, without skip. Post-run read-only readback preserved the DB SHA, exact fresh tuple and zero lineage/report/receipt counts; provider calls remained `0`. No further uninstall/reset/database cleanup is permitted. The original code196 preservation claim is withdrawn; isolated harnesses remain separate from the current DB. Any future live authorization must use a separate exact preflight and approval.
- The selected acceptance cap is `4,096` requested/effective; `2,048` remains historical diagnostic evidence only. The exact acceptance authorization is consumed and cannot be reused. The acceptance attempt reached response headers but not a complete response within the five-minute cap; no provider usage/cost was available locally, so external billing remains unknown.
- The local harness initially exposed a device freezer interruption at `DELAY_STARTED`; bounded cleanup and the test-only foreground keepalive resolved the local test hang. The code196 five-minute stalled-body run completed under app control without host force-stop. The former `PROCESS_RESTART_RECOVERY_VERIFIED` label is superseded by `DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED`; the current evidence does not prove two independent app process invocations.
- The current PRONOUN transport file includes a UTF-8 BOM, but the app-owned semantic bytes match the immutable binding. Do not silently change the normalization rule or rebind the project.
- The original code196 pilot attempt/reconciliation rows are unavailable after the installer incident. Rehydrating source/pack data and reproducing an old deterministic identity in an isolated DB is not historical-row restoration; `PILOT_DATA_PRESERVED` therefore remains failed. No trusted code196 backup was found in the known artifact/backup roots; the private reconstructed snapshot and separate fresh-pilot snapshot are explicitly not historical recovery. Fresh selector/binding/run identities were created through the approved setup path; fresh snapshot manifest SHA-256 is `2BEA88D4B562DFFA0CEAE401E1BFA0ED50B353CCD014AA9840F15CE1FA7F35EF`, with source/pack hashes and readback invariants recorded outside Git. Fake predecessor rows remain isolated and current P5C/P5D counts are zero.
- The prior P5E.9A freeze recorded a stale runner expectation (`f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1`) against the official fresh binding evaluation (`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`). The source correction is committed at `049e72b5769f8b3fdcb6f50646d1f0ead3043940`; no SQL row was changed. The approved code206 install and post-install readback passed. QF corrected only the test-only SQL predicate through the required attempt-identity JOIN in commit `34a4ec2832d71a488a2531a0e69a85261e9c9b9b`; AndroidTest compile and the corrected test APK build passed, with artifact SHA-256 `68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A`. After separate owner approval the test package was installed only, the failed method passed `1/1`, and the full class passed `5/5`; production code206 and the DB hash remained unchanged and provider calls stayed at `0`. This is historical QF evidence and is superseded for readiness by the current production lineage-query defect and its new candidate. Do not create RAW authorization or request provider access from this local result.

## P5E.9A-LQ — pre-DV production lineage-query fix stop (historical)

The historical QF run reproduced the schema-v24 failure
`SQLiteException: no such column: binding_identity` before provider construction.
The new disposable schema-v24 AndroidTest fixture records the same PRAGMA and
legacy-predicate characterization and was compile-validated; it has not been
executed because code207 is not yet approved for device installation. The
current pilot DB was not used as a mutation fixture.

Production commit `ff6821a` adds one read-only `inspectLineage` helper and makes
`dispatchRaw` fail closed with `P5E_FRESH_RAW_LINEAGE_CHECK_FAILED` when any
lineage query/schema read fails. The helper reads all five counts before
calculating `UNUSED`/`ALREADY_USED`:

- attempts by `editorial_p5c_attempts.binding_identity`;
- authorization receipts by `binding_identity`;
- primary reconciliation through `r.attempt_identity = a.attempt_identity`;
- reconciliation history through the same attempt join;
- network lifecycle through the same attempt join.

The QF AndroidTest now calls this production helper rather than maintaining a
second reconciliation predicate. The isolated lineage class also covers empty
v24, each related evidence owner, unrelated binding, missing table/column, the
historical RED predicate, canonical chapter `001` versus `chapter001`, and
typed missing/wrong authorization with no mutation. No schema, migration,
canonical pack/profile/authority, wire/final schema, cap or route changed.

Local evidence for this group:

```text
branch=feature/v4.18
headBeforeDocumentation=995d3b6c9678e93905b3802cf22eee0b091b1bb3
productionFixCommit=ff6821a
testCoverageCommit=995d3b6c9678e93905b3802cf22eee0b091b1bb3
engine=200/200 PASS
appDebug=232/232 PASS
appRelease=232/232 PASS
appBenchmark=232/232 PASS
lintDebug=PASS
androidTestCompile=PASS
gitDiffCheck=PASS
secretScan=no matches
providerCalls=0
currentDbMutation=none
```

The new production candidate was archived without installation:

```text
version=v4.17-p5e.11
versionCode=207
event=build-20260911-201725
buildSourceSnapshot=995d3b6c9678e93905b3802cf22eee0b091b1bb3
productionFixCommit=ff6821a
apkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
sourceZipSha256=B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
artifactBackupByteEqual=true
artifact=D:\App Translate Books\App Translate Books-translation-profile\artifacts\builds\v4.17-p5e.11\build-20260911-201725
backup=D:\App Translate Books\App Translate Books-translation-profile\backup\builds\v4.17-p5e.11\build-20260911-201725
```

The superseded LQ test APK is not approved and must not be installed:

```text
testPackage=com.ml.tblandroidtxt.test
testSourceSnapshot=995d3b6c9678e93905b3802cf22eee0b091b1bb3
testApkSha256=9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk
status=SUPERSEDED_NOT_APPROVED_NOT_INSTALLED
```

QF2 corrected the AndroidTest version expectation from `206L` to `207L` and
renamed the schema-failure test to match its typed-check scope. The test-only
change was compiled and built without rebuilding production, touching the
device, reading or writing the current DB, or calling a provider. The new
candidate-aligned test APK is outside Git and remains uninstalled:

```text
testPackage=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
targetProductionVersion=v4.17-p5e.11
targetProductionVersionCode=207
testRunner=androidx.test.runner.AndroidJUnitRunner
testSourceCommit=f2695c862a9b860e08fd01f932377ec5576d6ad1
testApkSha256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testApkBytes=1313798
testApkLastWrite=2026-09-11 20:42:59 +07:00
testArtifactCaptured=2026-09-11 20:43:14 +07:00
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
testApkVersionCodeMetadata=not_present_in_androidTest_manifest
status=BUILT_NOT_INSTALLED_NOT_APPROVED
```

The installed device remains the prior owner-approved code206 artifact and
the current DB remains `RECONSTRUCTED_ONLY` with the pinned hash and fresh
tuple unchanged by this QF2 host-only work. No device operation or adb command
was performed. The current gate is:

```text
P5E_9A_LQ_TEST_VERSION_PIN_CORRECTED
CODE207_PRODUCTION_CANDIDATE_UNCHANGED
CODE207_ALIGNED_TEST_APK_BUILT
P5E_9A_LQ_DEVICE_HELPER_EXECUTION_REQUIRED
P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS: NOT_REACHED
NEW_TEST_ARTIFACT_OWNER_APPROVAL_REQUIRED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

## Next step recorded before approved device run (historical)

Code189/code191 and their consumed authorizations remain historical and must
not be reused. Code191 has the external P5E classification
`EXTERNAL_CONFIRMED_CANCELLED`; it is not a `$0` billing conclusion. The QF
test-only correction is complete, but the new artifact has not received owner
approval and has not been installed or executed. The historical next step at
that time was to
obtain a separate approval for this exact test APK before any test-package
installation and direct device zero-call rerun. It is not permission to
prepare P5E.9B authorization. Keep
`RAW_AUTHORIZATION_REQUIRED / NO_AUTHORIZATION_CREATED /
NO_LIVE_CALL_PERFORMED / RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED /
RECONCILE_AUTHORIZATION_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED`.

## P5E.9A-LQ-QF2 — code207 test-artifact alignment approval request (historical)

The following block is a request template, not an approval and not a live
authorization. It must be approved separately before device test-package
installation:

```text
P5E_9A_LQ_QF2_TEST_ARTIFACT_OWNER_APPROVAL_REQUEST
TARGET_DEVICE=15e84958
TARGET_PRODUCTION_PACKAGE=com.ml.tblandroidtxt
INSTALLED_PRODUCTION_VERSION=v4.17-p5e.10
INSTALLED_PRODUCTION_VERSION_CODE=206
INSTALLED_PRODUCTION_APK_SHA256=F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080
INSTALLED_PRODUCTION_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
INSTALLED_DEVICE_SIGNATURE_TOKEN=abebea4b
INSTALLED_SCHEMA_VERSION=24
CURRENT_DB_SHA256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
FROZEN_CANDIDATE_VERSION=v4.17-p5e.11
FROZEN_CANDIDATE_VERSION_CODE=207
FROZEN_CANDIDATE_APK_SHA256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
FROZEN_CANDIDATE_SOURCE_ZIP_SHA256=B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348
FROZEN_CANDIDATE_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
FRESH_SELECTOR=p5e-fresh-mercedes-vol5-20260911-01
FRESH_CHAPTER_KEY=001
FRESH_BINDING=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
FRESH_RUN_DECLARATION=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
FRESH_EVALUATION=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
TEST_PACKAGE=com.ml.tblandroidtxt.test
TEST_APK_PATH=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
TEST_APK_SHA256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
TEST_SOURCE_COMMIT=f2695c862a9b860e08fd01f932377ec5576d6ad1
TEST_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
TEST_RUNNER=androidx.test.runner.AndroidJUnitRunner
TARGET_PRODUCTION_APK_SHA256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
ALLOW_REPLACE_TEST_PACKAGE_ONLY=true
ALLOW_DIRECT_AM_INSTRUMENT_ONLY=true
ALLOW_SINGLE_METHOD_THEN_CLASS_ONLY=true
FORBID_PROVIDER_API=true
FORBID_AUTHORIZATION_ATTEMPT_RECONCILIATION=true
FORBID_RETRY_REPAIR=true
FORBID_PRODUCTION_INSTALL_UNINSTALL_CLEAR_RESET_DOWNGRADE=true
FORBID_CONNECTED_ANDROID_TEST=true
FORBID_CURRENT_DB_MUTATION=true
FORBID_RAW_AUTHORIZATION_CREATION_OR_CONSUMPTION=true
```

This file is current-only; Git history preserves prior state.

## P5E.9A-LQ-DV — code207 guarded install and zero-call device verification

The owner-approved device scope was executed after the QF2 host-only stop. The
production candidate was not rebuilt. One `scripts/install-validated.ps1`
upgrade changed the installed production package from code206 to code207, and
one exact-hash replacement changed only `com.ml.tblandroidtxt.test`. No
connected AndroidTest task, provider/API call, authorization, attempt,
reconciliation, retry, repair or RECONCILE was used.

```text
device=15e84958
productionPackage=com.ml.tblandroidtxt
installedProductionVersion=v4.17-p5e.11
installedProductionVersionCode=207
installedProductionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
installedProductionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
deviceSignatureToken=abebea4b
schemaVersion=24
dataClassification=RECONSTRUCTED_ONLY
productionUpgradeCount=1
testPackageReplacementCount=1
connectedAndroidTest=NOT_RUN
```

The pre-upgrade private snapshot and isolated restore evidence are outside Git:

```text
snapshotRoot=D:\P5E-private\p5e-9a-lq-dv-snapshot-20260911-210256
snapshotManifestSha256=BBE47271716785B1ED89F888748428C9A0437A47FF61804942FAF202BBE49C76
snapshotDatabaseSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
snapshotJournalSha256=E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855
snapshotWalPresent=false
snapshotShmPresent=false
snapshotRestoreRoot=D:\P5E-private\p5e-9a-lq-dv-restore-20260911-210256
snapshotRestoreStatus=PASS_DATA_LEVEL_ONLY
postInstallReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postinstall-20260911-210550
postRunReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postrun-verified2-20260911-211156
```

The pre-upgrade and post-run database SHA-256 are identical. Read-only SQLite
verification after the full isolated lineage class reported schema v24,
`integrity_check=ok`, foreign-key violations `0`, execution disabled,
`NOT_CERTIFIED`, and zero rows for attempts, authorization receipts,
reconciliation, reconciliation history, network lifecycle and non-empty
REPORT_L1/receipt blobs. The fresh project has one chapter row for canonical
key `001`; the database has two `001` rows globally because reconstructed data
contains another project, which is unchanged and outside the fresh binding.

```text
dbBeforeSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dbAfterSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
databaseHashUnchanged=true
freshSelector=p5e-fresh-mercedes-vol5-20260911-01
freshChapterKey=001
freshBinding=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
freshRunDeclaration=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
freshEvaluation=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
freshPackSha256=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
freshProfileSha256=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
freshProjectChapterRows=1
globalChapterKey001Rows=2
attempts=0
authorizationReceipts=0
reconciliation=0
reconciliationHistory=0
lifecycle=0
reportBytesNonEmpty=0
receiptBytesNonEmpty=0
partialCommit=false
automaticRedispatch=false
providerCalls=0
```

The direct device evidence is attributed to production code207 and the
candidate-aligned test APK from test-source commit
`f2695c862a9b860e08fd01f932377ec5576d6ad1`:

```text
testPackage=com.ml.tblandroidtxt.test
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
testApkSha256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testRunner=androidx.test.runner.AndroidJUnitRunner
isolatedLineageClass=13/13 PASS
schemaFailureMethod=1/1 PASS
chapterSemanticsMethod=1/1 PASS
qfSingleMethod=1/1 PASS
qfBoundaryClass=5/5 PASS
allTestsNoSkip=true
```

The current gate is now:

```text
P5E_9A_LQ_TEST_VERSION_PIN_CORRECTED
CODE207_PRODUCTION_CANDIDATE_UNCHANGED
CODE207_DEVICE_VERIFIED
P5E_9A_QF_ZERO_CALL_BOUNDARY_PASS
P5E_9A_LQ_PRODUCTION_LINEAGE_QUERY_FIX_PASS
P5E_9A_LQ_DEVICE_ZERO_CALL_PASS
P5E_9A_LQ_DEVICE_HELPER_EXECUTION_PASS
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

`P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS` is not claimed because the
approved zero-call scope deliberately did not dispatch a valid authorization.
`FRESH_RAW_EXACT_PREFLIGHT_READY` is consequently not used as a substitute for
that missing authorization-path evidence. The historical next single step at
that time was to prepare an exact P5E.9B RAW authorization block for separate
owner approval; it was superseded by the later A2 fail-closed route result and
the current A4.1 host-contract gate. It must not be created, consumed or
dispatched automatically.

## P5E.9B-A1 host-only fresh RAW live harness

The A1 work was host-only and zero-call. It did not use ADB, install either
APK, read an API key from a device, create or persist an authorization, create
an attempt, call a provider, run preflight on the device, or open RECONCILE.
The implementation baseline was:

~~~
branch=feature/v4.18
headBeforeDocumentation=a0009f04139431f0bee38d049f9b32e2b6b04c41
productionVersion=v4.17-p5e.11
productionVersionCode=207
productionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
productionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
deviceOperations=0
providerCalls=0
authorizationCreated=0
authorizationConsumed=0
~~~

The test-only commit a0009f04139431f0bee38d049f9b32e2b6b04c41 adds
EditorialP5EFreshRawLiveInstrumentedTest and exposes only the existing
test fixture access required by the harness. No file under app/src/main, no
schema or migration, no pack/profile/authority, no route, no wire contract and
no production version metadata changed. The harness has two distinct entry
points:

~~~
freshRawExactPreflightRunsOnlyWhenExplicitlyOptedIn
  first executable operation: p5e_fresh_raw_preflight == YES
  no opt-in: approved skip
  calls production preflightOnly only after exact argument checks
  never calls dispatchRaw or executeRaw

authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn
  first executable operation: p5e_fresh_raw_live == YES
  distinct live opt-in; not used in A1
  requires the complete authorization, manifest, request, route and account facts
  recomputes preflight and checks unused lineage before authorization construction
  dispatches only through the fresh RAW runner after all fail-closed checks
~~~

The live method has no live-field defaults and does not reuse P5D selectors,
attempts, recovery rows, retry, repair, response healing or RECONCILE. Source,
prompt, request body, response body, credential and unredacted endpoint data are
not placed in the redacted evidence manifest. The manifest records only hashes,
lengths, identities, route facts, visibility, counts and policy.

The final A1 test artifact is private and not installed:

~~~
testApkPath=D:\P5E-private\fresh-raw-live-harness-a1-20260911-233211-test-apk\app-debug-androidTest.apk
testPackage=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
testRunner=androidx.test.runner.AndroidJUnitRunner
testSourceCommit=a0009f04139431f0bee38d049f9b32e2b6b04c41
testApkSha256=697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testApkBytes=1323843
testBuildTimestamp=2026-09-11T23:32:11.8261069+07:00
installed=false
~~~

The pre-amend artifact SHA
4DA7EB0A5EB6162B6A2127648B9F26F7CFA21A352A6130B0CA426BBA5F9097FE is
superseded and is not approved. The prior QF2 device-QA artifact
50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587 remains
historical and is not the A1 harness artifact.

The frozen production candidate was revalidated without rebuilding it. Artifact
and backup APK SHA-256 are both
2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD; artifact
and backup source ZIP SHA-256 are both
B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348.
Matching SHA-256 establishes the required byte equality for these mirrored
files. AndroidTest compilation passed with
:app:compileDebugAndroidTestJavaWithJavac --no-daemon; the test APK build
passed with :app:assembleDebugAndroidTest --no-daemon --console=plain.
No connected test or production APK build was run.

Historical A1 template only; it is not an issued authorization and is
superseded by the current A4.3 owner-review packet. Its validity value below
is retained as historical evidence and is not a current dispatch value:

~~~
authorizationId=P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01
authorizationIdSha256=0aa82c5897e3df3ec8a7a1586736dbf184b316c66ec165e95e64e8e4832145eb
phase=L1_RAW_DISCOVERY
provider=openrouter
model=openai/gpt-5.6-luna
upstreamProvider=openai
maximumPrimarySemanticCalls=1
maximumSchemaRepairCalls=0
maximumNetworkRetries=0
maximumInputTokens=100000
maximumOutputTokens=4096
maximumTotalTokens=104096
maximumTotalCostUsd=0.05
maximumExecutionTimeMillis=120000
authorizationValidityWindowMillis=900000
allowChapterToProvider=true
allowFullModelResponseStorage=false
allowRequestBodyStorage=false
evidenceRedactionPolicy=HASH_ONLY
singleUse=true
cancellationStopAuthority=OWNER_CONTROLLED,RAW_ONLY,NO_SCHEMA_REPAIR,NO_AUTOMATIC_RETRY,NO_RECONCILE,NO_RESPONSE_HEALING,NO_FALLBACK,PRESERVE_DURABLE_RECOVERY_STATE
~~~

The routing fingerprint for the canonical route facts is
23149071716043a2a4dc7fb7af51073b4de838ba072919bb6fd750bc9e62948c.
The compact wire identity is safe4.raw.discovery.wire.v1; the A1 manifest
template records worstCaseWireBytes=2785, maximumWireBytes=3584,
outputTokenCap=4096 and contextSizeBytes=80317. The egress declaration is
RAW and GLOSSARY visible; DRAFT and PRONOUN hidden; pack authority required.
The source inventory is hash/length only:

~~~
RAW=23814:a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be
DRAFT=26462:64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5
GLOSSARY=3249:4bc3e2dd05542aa5ca6b7e5fcac43ed53e9af57060eb69c6fa71e9d0a2ea0314
PRONOUN_SEMANTIC=452:4947ff9184995be5f850f2323fbe0a04c67302fb8d5afb63cf12202b44720686
~~~

OpenRouter model capability/pricing was checked at
2026-09-11T23:30:58+07:00 using the official GPT-5.6 Luna model page. The
observed page listed JSON Schema structured outputs and pricing of $0.20/M
input and $1.20/M output. This is reference information only; the proposed
authorization retains the independently bounded $0.05 maximum cost.

Historical A1 state:

~~~
P5E_9A_LQ_DV_COMPLETE
P5E_9B_A1_LIVE_HARNESS_HOST_PASS
P5E_9B_AUTHORIZATION_TEMPLATE_PREPARED_NOT_ISSUED
P5E_9B_TEST_APK_BUILT_NOT_INSTALLED
P5E_9B_A2_ZERO_CALL_PREFLIGHT_OWNER_APPROVAL_REQUIRED
P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS: NOT_REACHED
FRESH_RAW_EXACT_PREFLIGHT_READY: NOT_REACHED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
~~~

Historical at that time: the only next step was owner approval for the A2
zero-call preflight below. That approval was not allowed to authorize the live
opt-in, and this historical step is superseded by the current A4.1 gate.

~~~
P5E_9B_A2_ZERO_CALL_PREFLIGHT_OWNER_APPROVAL_REQUEST
TARGET_DEVICE=15e84958
TARGET_PRODUCTION_PACKAGE=com.ml.tblandroidtxt
INSTALLED_PRODUCTION_VERSION=v4.17-p5e.11
INSTALLED_PRODUCTION_VERSION_CODE=207
INSTALLED_PRODUCTION_APK_SHA256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
INSTALLED_PRODUCTION_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
INSTALLED_DEVICE_SIGNATURE_TOKEN=abebea4b
CURRENT_DB_SHA256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
CURRENT_DB_SCHEMA=24
FRESH_SELECTOR=p5e-fresh-mercedes-vol5-20260911-01
FRESH_CHAPTER_KEY=001
FRESH_BINDING=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
FRESH_RUN_DECLARATION=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
FRESH_EVALUATION=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
FRESH_PACK_SHA256=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
FRESH_PROFILE_SHA256=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
TEST_APK_PATH=D:\P5E-private\fresh-raw-live-harness-a1-20260911-233211-test-apk\app-debug-androidTest.apk
TEST_APK_SHA256=697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A
TEST_APK_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
TEST_SOURCE_COMMIT=a0009f04139431f0bee38d049f9b32e2b6b04c41
TEST_PACKAGE=com.ml.tblandroidtxt.test
TEST_RUNNER=androidx.test.runner.AndroidJUnitRunner
ALLOW_REPLACE_TEST_PACKAGE_ONLY=true
ALLOW_DIRECT_AM_INSTRUMENT_ONLY=true
ALLOW_PREFLIGHT_METHOD_ONLY=true
PREFLIGHT_ARGUMENT=p5e_fresh_raw_preflight=YES
FORBID_LIVE_ARGUMENT=p5e_fresh_raw_live=YES
FORBID_AUTHORIZATION_CREATION_OR_CONSUMPTION=true
FORBID_ATTEMPT=true
FORBID_PROVIDER_API=true
FORBID_RETRY_REPAIR_RECONCILE=true
FORBID_CURRENT_DB_MUTATION=true
FORBID_CONNECTED_ANDROID_TEST=true
FORBID_PRODUCTION_PACKAGE_INSTALL=true
~~~

No exact preflight or live readiness is claimed until this separate approval
is granted and the device gate is run.

## P5E.9B-A2 zero-call preflight result (historical runtime evidence)

The owner approved one single-run A2 scope. The preflight gate passed for
device readiness, production code207 identity, the existing test package hash,
the target test artifact hash/certificate/package, and the pinned database
hash. The existing test package was replaced exactly once with adb install -r;
the production package was not installed or replaced.

The one direct instrumentation invocation was:

~~~
class=com.ml.tblandroidtxt.EditorialP5EFreshRawLiveInstrumentedTest
method=freshRawExactPreflightRunsOnlyWhenExplicitlyOptedIn
argument=p5e_fresh_raw_preflight=YES
liveArgument=absent
testsRun=1
failures=1
providerCalls=0
~~~

It stopped before production preflightOnly and before request construction.
The boundary failure was classified as:

~~~
P5E_FRESH_RAW_ROUTE_PRECONDITION_FAILED
assertion=current settings must select the fresh RAW route
implementationLine=EditorialP5EFreshRawLiveInstrumentedTest.java:149
observedPredicate=EditorialP5EFreshRawRoutingPolicy.matches(SettingsStore.load(target))
~~~

No settings value, API key, source content, prompt, request body or provider
response was logged. The failure identifies only that the current SettingsStore
does not satisfy the pinned fresh route predicate; it does not identify which
setting differs. No settings or route correction was attempted. The complete
redacted preflight manifest was not produced because the failure occurred before
preflightOnly.

Post-run read-only verification passed:

~~~
productionVersionCode=207
productionSignatureToken=abebea4b
testApkSha256=697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A
dbBeforeSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dbAfterSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
databaseHashUnchanged=true
schemaVersion=24
integrityCheck=ok
foreignKeyViolations=0
attempts=0
authorizationReceipts=0
reconciliation=0
reconciliationHistory=0
lifecycle=0
reportBytesNonEmpty=0
receiptBytesNonEmpty=0
partialCommit=false
automaticRedispatch=false
~~~

The read-only binding row still contains the exact fresh selector, binding,
run declaration, evaluation, pack/profile hashes and RAW/DRAFT/GLOSSARY/
PRONOUN byte/hash inventory pinned in the A2 approval. No fresh identity was
created or rebound.

Historical state after the failed single run:

~~~
P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED
P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED
P5E_9B_A1_LIVE_HARNESS_HOST_PASS
P5E_9B_AUTHORIZATION_TEMPLATE_PREPARED_NOT_ISSUED
P5E_9B_TEST_APK_BUILT_AND_INSTALLED_FOR_A2_ONLY
P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS: NOT_REACHED
FRESH_RAW_EXACT_PREFLIGHT_READY: NOT_REACHED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
~~~

The A2 approval is not reusable for a rerun. Stop here and obtain a new
owner-approved remediation scope for the route-precondition blocker before any
further device or settings action.

## P5E.9B-A3.1 host-only result

The A3.1 group used no ADB, device settings read, database access, provider,
authorization, attempt, reconciliation or instrumentation. The documentation
baseline was `6a35b2de1ebbb4dcdb6e47cde8d0a1d060781d5e`; the test-only
diagnostic commit is `89eef75a4a62e5674d02b7e48eaaff012d9a7ae0`, based on the
implementation baseline `a0009f04139431f0bee38d049f9b32e2b6b04c41`.

The existing PreTag verifier was run as:

~~~
scripts/verify-release-workflow.ps1 -ChecklistPath release_checklists/v4.18-editorial-v5-safe-4-1-3.md -Gate PreTag -ExpectedVersion 4.18
~~~

and failed closed with `Step 09 is not complete for gate PreTag`. The checklist
now keeps current P5/P5E closure unchecked, so no false-green PreTag result was
observed; no tag or release action was attempted.

The new diagnostic is test-only. Its first executable statement is the
explicit opt-in check `p5e_fresh_raw_route_diagnostic=YES`; only then does it
call `SettingsStore.load` once. It emits only four booleans:
`providerMatch`, `modelMatch`, `endpointMatch` and `routeMatch`, with
`routeMatch` required to equal their conjunction. It does not emit persisted
provider/model/endpoint values, API keys, source, prompt, request or response
content; it does not call `SettingsStore.save`, open the DB, construct a
provider/client/request or call production `preflightOnly`.

The offline synthetic cases are test-only source under `app/src/androidTest`.
They cover exact route, each component mismatch, provider/model case policy,
endpoint trailing-slash and surrounding-whitespace normalization, conjunction
invariance and output allowlisting. They were compiled, not run by
instrumentation in A3.1.

~~~
P5E_9B_A3_1_HOST_ONLY_PASS
P5E_9B_ROUTE_DIAGNOSTIC_ARTIFACT_BUILT_NOT_INSTALLED
P5E_9B_A3_2_DEVICE_DIAGNOSTIC_APPROVAL_REQUIRED
P5E_9B_A4_REMEDIATION_NOT_SELECTED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
~~~

The following A3.2 approval is a request only. It is not an authorization and
does not permit a live flag, provider call, current-DB mutation or reuse of A2.

~~~
P5E_9B_A3_2_ROUTE_DIAGNOSTIC_OWNER_APPROVAL_REQUEST
APPROVAL_STATUS=REQUIRED
APPROVAL_SCOPE_SINGLE_RUN=true
TARGET_DEVICE=15e84958
TARGET_PRODUCTION_PACKAGE=com.ml.tblandroidtxt
INSTALLED_PRODUCTION_VERSION=v4.17-p5e.11
INSTALLED_PRODUCTION_VERSION_CODE=207
INSTALLED_PRODUCTION_APK_SHA256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
INSTALLED_PRODUCTION_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
INSTALLED_DEVICE_SIGNATURE_TOKEN=abebea4b
CURRENT_DB_SHA256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
CURRENT_DB_SCHEMA=24
TEST_APK_PATH=D:\P5E-private\fresh-raw-route-diagnostic-a3-20260912-002937-test-apk\app-debug-androidTest.apk
TEST_APK_SHA256=64A9976F43F04397DF0E593F21E7AE154CDED1ED6749294F3B1E5F9D2A77757A
TEST_APK_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
TEST_SOURCE_COMMIT=89eef75a4a62e5674d02b7e48eaaff012d9a7ae0
TEST_PACKAGE=com.ml.tblandroidtxt.test
TEST_RUNNER=androidx.test.runner.AndroidJUnitRunner
DIAGNOSTIC_CLASS=com.ml.tblandroidtxt.EditorialP5EFreshRawRouteDiagnosticInstrumentedTest
DIAGNOSTIC_METHOD=persistedRawRouteDiagnosticRunsOnlyWhenExplicitlyOptedIn
DIAGNOSTIC_ARGUMENT_NAME=p5e_fresh_raw_route_diagnostic
DIAGNOSTIC_ARGUMENT_VALUE=YES
LIVE_ARGUMENT=ABSENT
ALLOW_REPLACE_EXISTING_TEST_PACKAGE_WITH_ADB_INSTALL_R_ONCE=true
ALLOW_EXACT_DIAGNOSTIC_METHOD_ONLY=true
ALLOW_SINGLE_INVOCATION=true
ALLOW_POST_RUN_READ_ONLY_VERIFICATION=true
PROVIDER_CALL_BUDGET=0
PRODUCTION_PACKAGE_OPERATIONS=0
FORBID_FULL_CLASS=true
FORBID_RERUN=true
FORBID_LIVE_ARGUMENT=true
FORBID_AUTHORIZATION_ATTEMPT_RECONCILIATION=true
FORBID_PROVIDER_API=true
FORBID_CURRENT_DB_MUTATION=true
FORBID_UNINSTALL_CLEAR_RESET_DOWNGRADE=true
FORBID_CONNECTED_ANDROID_TEST=true
~~~

## Offline follow-up of the independent review — 2026-10-06 (uncommitted)

- Source: HEAD `5bea4c93` plus 19 overlay files (SHA-256 manifest and logs in `D:\P5E-builds\followup-api-20261006`); no APK was built because a wrapper build needs a commit.
- Verified on the clean archive + overlay: engine 492/492, app unit 397/397, Python 72/72, androidTest compile PASS. Review patches: before FAIL / after PASS reproduced (`before-two-patches-HEAD-main.log` / `baseline-two-patches.log`). C/C2 reference test failed before the fix (`before-check-references.log`).
- Not proven: device runs of `EditorialApiBienTapUiInstrumentedTest` and the 10 updated old androidTests, SAF traversal, process-death reopen. Actual provider calls/spend 0 / USD 0.

- Device evidence 2026-10-06: build 4.18-api.4/code241 from commit 42b40fb1 (APK 8AABFFA6…E87DA) installed on emulator-5554; store 4/4, flow 3/3, UI 2/2, force-stop reopen 3/3, full suite 234/11 failures (11 historical). Details in the execution report.


