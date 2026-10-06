# Báo cáo thực thi Editorial API V1 — N1–N4 (offline)

Ngày chốt: 2026-10-06 (+07:00). Phạm vi tiếp tục chuỗi Claude đã hoàn tất N1–N3; N4 được kiểm chứng trên cùng HEAD và push riêng. Không gọi provider, không đọc khóa/account, không đụng pilot. Dừng trước N5 vì D-N4 chưa được duyệt.

## Quyết định và commit

- Câu duyệt D-N1, D-N2, D-N3 đã được ghi trong mục **Quyết định** của `docs/EDITORIAL_API_V1_WORK_REQUEST_N1_N4_20261005.md` tại commit `511960ee`; D-N4 vẫn chưa duyệt.
- N1 engine: `b2fb1a6d`.
- N2 app/store/provider/service/export/runner: `afbd34ce`.
- N3 tab Biên tập và presenter/UI tests: `195406bb`.
- Sửa tiếp theo đã có trên remote: `e3f8a784`, `18571c01` (giá model không biết; QUICK là E một lượt).
- N4: báo cáo, trạng thái và bằng chứng được commit/push tại `72b6178a`.

## Bằng chứng phải giữ

### N1–N3 và regression

N1–N3 được kiểm lại từ worktree tạm sạch `D:\P5E-builds\wt-api-n4-20261005`, source `18571c0138436a699bdcc67851b2afb42b60d60b`:

| Hạng mục | Kết quả |
|---|---:|
| `:editorial-engine:test` | 484/484, 0 fail |
| `:app:testDebugUnitTest` | 386/386, 0 fail |
| Python `scripts/p6` (`unittest discover`) | 71/71, 0 fail |
| `:app:assembleDebugAndroidTest --offline` | PASS |
| SAFE4 cũ trong các test trên | PASS |

`py_compile` cho Python `scripts/p6` PASS. Lần gọi engine test đầu tiên gặp JVM 8 do shell; chạy lại với JDK 21 mà wrapper yêu cầu đã PASS, không có thay đổi mã.

### N4 build và archive

Production đã được tạo bằng `scripts/build-and-save.ps1`; AndroidTest bằng `scripts/build-and-save-android-test.ps1` với `--offline`, cùng source archive:

| Payload | Giá trị |
|---|---|
| Version/code | `4.18-api.3` / `240` |
| Source | `18571c0138436a699bdcc67851b2afb42b60d60b` |
| APK SHA-256 | `9F66F0C9D69F19728DD7EBB77138B946CDAF6672081F468A7B52638E5E4957C8` |
| Source ZIP SHA-256 | `971373AF209C5FF137EA3F52BCB6008C734F06A012C80E5F1EC01EEFA2227A14` |
| AndroidTest SHA-256 | `20CBCF3C69AE4E81C26B31FB73BB0C6B94FDC1282AB2F5AE38F6444D812A9381` |
| Certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |
| Production archive | `D:\P5E-builds\wt-api-n4-20261005\artifacts\builds\v4.18-api.3\build-20261005-230303` |
| Production backup | `D:\P5E-builds\wt-api-n4-20261005\backup\builds\v4.18-api.3\build-20261005-230303` |
| AndroidTest archive | `D:\P5E-builds\wt-api-n4-20261005\artifacts\test-builds\v4.18-api.3\api-n4-18571c01-20261005-03` |
| AndroidTest backup | `D:\P5E-builds\wt-api-n4-20261005\backup\test-builds\v4.18-api.3\api-n4-18571c01-20261005-03` |

APK/source ZIP bytes match between artifact and backup. The archive contains the exact tracked source ZIP; the temporary worktree was clean when it was made.

### N4 emulator và fake provider

Chỉ dùng `emulator-5554`; pilot không được truy cập. Production preflight qua install guard khớp version/code/certificate; AndroidTest package readback có đúng SHA `20CBCF3C69AE4E81C26B31FB73BB0C6B94FDC1282AB2F5AE38F6444D812A9381`.

| Kiểm tra | Kết quả |
|---|---:|
| `EditorialApiBienTapFlowInstrumentedTest` | 3/3 PASS: đủ 4 nguồn với QUICK; thiếu Glossary/Pronoun với THOROUGH; export/readback SHA |
| Force-stop app rồi chạy lại hai combo | 2/2 PASS |
| `EditorialApiStoreInstrumentedTest` | 4/4 PASS: DB v26, reopen, migration, cascade |
| Actual provider calls / spend | 0 / USD 0 |

Ảnh chỉ chứa UI và văn bản mẫu/placeholder, giữ ngoài Git:

- `D:\P5E-private\editorial-api-n4-20261006\screenshots\editorial-tab-synthetic.png` — SHA-256 `DFF35377356E74CC0ADD88675A26E2FA2D4F1673BA30973A3A929B930D78F1FE`.
- `D:\P5E-private\editorial-api-n4-20261006\screenshots\editorial-combo-empty-synthetic.png` — SHA-256 `1921BE5D2B2B09CBDA17D4383B9606E63DE54411717D4D6DD9974DC923742BA3`.

### Fixture runner dry-run

Runner API V1 chạy `fx-a01` với `API_V1_QUICK`, fake provider cục bộ, group `API-N4-QUICK-20261005`. Evidence private: `D:\P5E-private\p6-runs\4a0e6d67-0f2c-4c52-9f8e-2c0d9f4f7a61`.

- `STRUCTURAL_VALID=1/1`, state `FINAL_OK`, `providerKind=FAKE_OFFLINE`.
- `actualProviderCalls=0`, fake calls 1; ledger 2 rows, pending UNKNOWN 0, settled USD 0.004 trong cap 0.50.
- Metadata ghi `contractRevision=EDITORIAL_API_V1.1`, source commit và APK code 240.
- `SEMANTIC_EVAL=FAIL` là kết quả đúng của scorer vì `fx-a01` là fixture có lỗi gieo; không dùng nó để tuyên bố semantic PASS và không sửa fixture/đáp án.

## Lệch so với yêu cầu

- N1–N3 đã được Claude hoàn tất và push trước khi tiếp quản; không tạo lại commit hay reset branch. N4 dùng archive đã tạo từ đúng HEAD sau khi kiểm tra hash, test output và parity hai nơi.
- AndroidTest archive ghi `installed=false` theo thiết kế archive-only; package đã có trên emulator và được xác minh bằng hash/readback trước instrumentation. Không có cài đặt pilot.
- Ảnh và response/final text fixture giữ ngoài Git.

## Kết luận và bước tiếp theo

N1–N4 offline đạt các gate kỹ thuật của work request. Không có provider call hay chi phí API. N5 chưa chạy; bước duy nhất tiếp theo là owner quyết định D-N4 (ngân sách/cap và phạm vi N5) trước mọi provider/device run.

## Independent review — 2026-10-06 (supersedes the readiness conclusion above)

Reviewed HEAD `5bea4c93f1eeb506e30e82debaf06b499b33707b`, matching the local upstream ref. Changes since APK source `18571c01` are documentation only. The existing owner changes in `.idea` and P5E documents were preserved. This review changes four runtime/test files locally plus handoff documentation; no commit, push, build, device operation or provider call was performed.

### Claims and verified evidence

| Claim | Evidence checked | Review conclusion |
|---|---|---|
| Production/test archives and exact source retained twice | Recomputed all APK/ZIP hashes in the four paths above; values match the report | Verified payload parity; not a fresh installed-byte check |
| Useful new user UI exists | N3 source and both retained synthetic screenshots; `EditorialApiUiController` selects Library glossary/pronoun and stores combo settings | Verified implementation. RAW/DRAFT use SAF or recent Translation jobs, not an in-app Library browser |
| UI 3/3 proves complete user journey | `EditorialApiBienTapFlowInstrumentedTest.runCombo` sets controller fields directly, calls `saveAndContinue/startRun`, and injects a file-URI export callback | Proves rendered screens plus controller/store/export callback integration, not selector/button/SAF-grant traversal |
| Force-stop/reopen 2/2 proves persisted combo/result reopening | Test creates a new combo and reads a fresh store; no phase reopens the same retained combo ID after process loss | Not established by these tests. Preserve the historical reported run counts, narrow the conclusion |
| Fake fixture is structural 1/1, no actual calls | Stored metadata: `FAKE_OFFLINE`, code240/source18571c01, QUICK, one fake call, `finish=stop` | Metadata verified; semantic quality NOT_MEASURED. Frozen scorer FAIL remains evidence of seeded defects, not model quality |
| USD 0.004 in dry-run ledger is actual spending | Metadata identifies a fake provider | Simulated charge only; review and N4 actual provider spend USD 0 |

### Small fixes completed locally

1. **Cancellation accounting.** `EditorialApiRunService.execute` discarded the returned response when cancellation raced with completion, losing the call, tokens and charge. Added `EditorialApiFlow.cancel(StepResponse)` to record returned usage without accepting output. Cancel still returns original DRAFT and never dispatches a later step. Strengthened the existing app regression to assert one call, the returned USD 0.004, tokens, CANCELLED record and persisted charge. This closes the lost-returned-response case; unknown transport charges remain an open issue below.
2. **Incomplete check falsely clean.** Parser normalization can turn `{}` into PASS, or drop an issue with an empty quote. Flow previously returned FINAL_OK. C and C2 now retain the candidate with FINAL_NOTES/checkUnavailable when findings were lost, or an empty result lacks a trustworthy clean verdict. No semantic content is filled in, no retry is added, optional fixes and extra keys remain accepted. Regression covers empty object, ISSUES with no rows, and lost empty-quote issue, in both C and C2.

Validation used a new `git archive 5bea4c93` extraction at `D:\P5E-builds\review-api-5bea4c93-20261006\source`, with only the four reviewed source/test patches overlaid and local SDK configuration. This is **baseline plus patch**, not a PASS claim for unmodified HEAD. Each new assertion failed before its implementation fix (`before-cancel.log`, `before-check.log`); final targeted API regression passed engine **63/63**, app **44/44**, failures 0 (`after-review.log`, JUnit XML in that extraction). No APK task or broad regression rerun. Existing code240 does not contain these fixes.

### Remaining findings and same-package repair ownership

Reproducible local patch: `D:\P5E-builds\review-api-5bea4c93-20261006\runtime-review.patch`, SHA-256 `FE1C2109493CAA36F03D9D452EFC3137C436CDE4B37D2E2DC6930F56901CDEB3`. All four tested file hashes match the current workspace files. This patch is not committed and not included in code240.

| Finding / cause | Reproduction and smallest repair | Owner / PASS and stopping point |
|---|---|---|
| C/C2 do not receive glossary/pronoun: `CheckPromptBuilder.build` takes only RAW/DRAFT/EDITED/flags/language; Flow discards reference inputs at this boundary | Synthetic selected term and scoped pronoun must appear in both check requests, with correct counts; pass the same filtered authoritative sources used by E, exclude E NOTES. Test no-reference and filtered-out cases | Engine owner: CheckPromptBuilder/Flow/prompt tests. PASS = captured E/C/C2 inputs preserve intended source visibility and scope; no live needed |
| Provider exceptions become `StepResponse.failure` with costKnown=true and zero cost; flow can retry despite unknown dispatch outcome. RunService cap uses accumulated known amounts | Simulate timeout after dispatch versus definite pre-dispatch failure. Preserve unknown charge and prevent automatic further calls/retry for unknown outcome; retain bounded retry only when external outcome permits it | App/provider owner: provider, service, accounting tests. PASS = no second dispatch after UNKNOWN, truthful known/estimated/unknown distinction |
| Fixture `LedgerProvider.call` settles `response.cost().min(worst)`, hiding actual charges exceeding reservation | Fake response above reservation; record exact actual charge/evidence and stop further dispatch when bounds fail, never clamp expense | Runner/accounting owner: fixture runner and bounded ledger tests; do not modify existing ledgers |
| Combo form edits are view-local until Continue: `EditorialApiPageFactory.comboScreen`; file/reference choices call refresh and rebuild from unchanged combo | Type name/model/cap, choose Kỹ, then select/change source or profile. Add a real UI regression for preserved values; retain form draft before rebuild. Verify Save without dispatch and reopen the same combo/result after process kill | UI owner: PageFactory/UiController/androidTest. Verify selector buttons, permission persistence and SAF export rather than injecting callbacks. Device work needs applicable explicit authority; no device operation in this review |
| N0 acceptance document remains L1–L3-only; plan header still says pending; N5 estimates say 10 runs while specified matrix implies 12 per arm | Reconcile accepted D-N1..D-N3 in existing documents. Eight base fixtures plus two repetitions each of a04/a11 = 12 per arm, 25 target opportunities per arm; freeze exact run list, retries/caps, actual pricing and non-clean a02 annotation before approval | Coordinator: current plan/acceptance/proposal only. Do not reinterpret fake quality as PASS or lower criteria to clear a finding |

The source-form loss is established by the view-local values and refresh path; no device reproduction has been performed in this review. Reference omission and accounting findings are source-confirmed; semantic impact remains NOT_MEASURED until controlled evaluation. General retry/accounting changes and prompt/UI changes are the next bounded repair group, not included in the two small fixes above.

**Next action:** close these remaining offline gaps in this existing N1–N4 package, then present corrected evidence and an exact D-N4 proposal. N5 is not ready merely because a budget could be approved. Keep SAFE4 frozen, G2 cancelled and historical evidence intact. Three accepted API_V1 FINAL chapters: **0/3 established**; P7 unmet. No live authorization is created here.

## Offline follow-up of the independent review — 2026-10-06 (local, uncommitted)

Scope: close the offline findings above inside the existing N1–N4 package. No provider call, no pilot, no device operation, no build, no commit/push. Source basis: HEAD `5bea4c93` plus 23 changed/new files under `editorial-engine`, `app/src`, `scripts/p6` (SHA-256 manifest `D:\P5E-builds\followup-api-20261006\overlay-manifest.txt`; exact archive `source-5bea4c93.zip`, SHA-256 `F71B7E6F97408A7944BB5436017FCA25DCA97B67A6A52761F30E7D685DCD4862`).

### What was done

| Item | Change | Evidence before → after |
|---|---|---|
| Two review patches (cancel keeps returned usage; incomplete check is not FINAL_OK) | Kept unchanged and re-verified | HEAD main sources with the new tests: `incompleteCheckCannotBecomeACleanFinalOrTriggerMoreCalls` and `cancelDuringACallEndsTheRunWithTheDraft` FAIL (`before-two-patches-HEAD-main.log`); with the patches: engine 485 / app 386 PASS (`baseline-two-patches.log`) |
| C/C2 saw no glossary/pronoun | `CheckPromptBuilder` takes the same RAW-filtered glossary entries and pronoun rows as E (one shared `appendReferences`), adds an "AUTHORITATIVE REFERENCE" block only when something applies, records counts in `ApiPrompt`; E's NOTES are still never passed. Flow passes them to CHECK and RECHECK | `checkAndRecheckSeeTheSameFilteredReferenceAsTheEditButNeverItsNotes` FAIL with the old call (`before-check-references.log`), PASS after (`after-check-references.log`); also no-reference and filtered-out cases |
| Provider failure marked known-zero and retried | `StepResponse.unknownOutcome` (cost unknown) for anything that may have been sent (timeouts, resets, 5xx, 408, unreadable body, cancel); only proven pre-dispatch failures (bad settings, no route, 4xx except 408) stay known-zero. Flow never repeats a step whose outcome is unknown: EDIT → RETRY_REQUIRED with the draft, CHECK/RECHECK → edit kept, FINAL_NOTES, check unavailable. Run service reserves the worst case of every unpriced/unknown call when testing the cap | New flow, service and provider-classification tests (all FAIL by construction on the old behaviour: no `unknownOutcome` existed) |
| Call billed above its own worst case | Service stops dispatching (`COST_BOUND_EXCEEDED`), keeps the real charge in the run. Fixture ledger wrapper (`EditorialApiLedgerProvider`, shared by JVM tests and runner) no longer clamps: the reservation stays pending (group stops on UNKNOWN), exact amounts go to `cost-overrun.txt` and metadata `costOverrunCalls`; `verify_fixture_run.py` rejects either | `EditorialApiLedgerProviderTest` (4), service test, Python overrun test |
| Combo form lost on selection | Typed name/mode/model/cap are kept by the controller before any picker/dialog rebuilds the screen and are what the screen shows next; new "Lưu" saves without reading sources or sending anything | `EditorialApiFormWiringTest` (structural guard only) + device test below |
| UI evidence overstated | `EditorialApiBienTapFlowInstrumentedTest` is now documented as controller-level. New `EditorialApiBienTapUiInstrumentedTest` finds and clicks the real buttons/radio/dialog list, checks form retention, Save, reopen of the same combo ID from the store and "Xem kết quả"; two-phase `seed → force-stop → verify → cleanup` (argument `bientap_phase`) reopens a stored result with a provider that fails if called | Compiles; **not run on a device** (see below) |
| Old tests broken by schema v26 | 10 `getVersion()` constants in 4 old androidTests updated 25 → 26 (they failed with "expected 25 but was 26" on the code240 run) | Compile only; device rerun pending |
| Acceptance / matrix | `EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` §1A (API_V1 criteria, L1–L3 artifacts historical only, 0/3 chapters, no live authority); plan §6: 12 runs per arm (8 base + 2 repeats of a04 + 2 of a11), 24 runs, 25 targets per arm (11 + 2×1 + 2×6), a02 not treated as clean (G1 adjudication found `E_L245_MEANING`), cost ≈ USD 0.19 (A) + 0.34 (B) ≈ 0.53, cap USD 1.00. Gate thresholds unchanged | Documents |

### Verification

Clean `git archive 5bea4c93` + the overlay (`clean-overlay`, logs `after-clean-overlay-final.log`): `:editorial-engine:test` **492/492**, `:app:testDebugUnitTest` **397/397**, `scripts/p6` Python **72/72**, `:app:compileDebugAndroidTestJavaWithJavac` PASS, 0 failures. Actual provider calls / spend: 0 / USD 0.

### Not proven (still missing)

- Device behaviour of any of the new code: no APK contains it (a wrapper build records and archives a commit; none was made). `EditorialApiBienTapUiInstrumentedTest` (all 5 tests), the 10 updated old androidTests, and the process-death phase have not run.
- Real system file picker and save-as traversal: the UI test delivers those two results to `onActivityResult`.
- The 11 instrumented failures on code240 that are not v26 constants (REPAIR_L1 ledger, v24 schema expectation, VOL5 files, historical bindings) were not re-baselined against the previous APK.
- Accuracy of the worst-case reserve for unpriced answers is a conservative rule, not a measured one. Semantic quality remains NOT_MEASURED; three accepted FINAL chapters: **0/3**.

### Exact D-N4 proposal to present after the device evidence (not requested yet, no authority created)

Runs: arms `API_V1_QUICK` and `API_V1_THOROUGH`, each `fx-a02, a03, a04, a05, a07, a08, a11, a12` + `a04` ×2 repeats + `a11` ×2 repeats (24 runs); model `openai/gpt-5.6-luna`, same sources; new ledger group, cap USD 1.00, chapter cap USD 0.10; at most one technical retry per step and never after an UNKNOWN outcome; Z3 stop rule; pricing basis still to be frozen by the owner (plan estimate ~0.13/1.25 USD per M, runner reservation basis 0.25/1.20); response retention outside Git; "new errors" adjudicated independently after the run.
