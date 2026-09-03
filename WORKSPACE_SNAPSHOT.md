# Workspace Snapshot

- Updated: `2026-09-03` (+07:00).
- Current phase/status: `P2_REFERENCE_PACK FROZEN HOST VALIDATED / ANDROID REVALIDATION PENDING DEVICE`.
- Current version/build: verified baseline `4.17-dev.1` / code169; no v4.18 build.
- Current branch/worktree: `feature/v4.18` / `D:\App Translate Books\App Translate Books-translation-profile`.
- Current commit baseline: `921af9256e1b1fe4ab9ac113affa98eec7a1e339`, the clean device-verified v4.17 HEAD immediately before P0 documentation; not self-referential.
- P0/P1 checkpoint: `aef7da1` (`chore(editorial): checkpoint P0 P1 characterization evidence`).
- Current build: `TranslateBooks-v4.17-dev.1-code169.apk`, SHA-256 `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`; artifact/backup parity preserved.
- Active authority: `EDITORIAL_RECOVERY_V4_18.md`.
- Editorial source: `V5-SAFE.4.1.3-FULL`; exact Project/Prompt/Workflow hashes are recorded in the plan and Pack v1 integration contract.
- Completed: repaired the moved v4.17 Git worktree; verified clean CODE169 baseline; created `feature/v4.18`; reran external static qualification `306 PASS / 0 FAIL`; audited existing Pack Manifest v1/import/storage/compatibility code; completed canonical plan, integration contract, implementation map, checklist and README direction; generated the test-only 4.1.3 ZIP; added P1 manifest/compatibility/import/storage and false-block characterization; checkpointed P0/P1 at `aef7da1`; added P2 canonical manifest, reproducible generator, Java data-descriptor control, checksum receipt, host manifest negatives and Android negative/transport characterization source. Host generator and engine tests pass; AndroidTest Java compile passes; prior Android P1 class ran `7` tests (`5` pass, `2` GAP-012 failures) and full suite ran `100` tests (`91` pass, `9` failures).
- Design decision: reuse the existing four-root-entry Pack Manifest v1. Imported runtime payload is `editorial-pack.json` plus exact Project/Prompt/Workflow files. External APP/COMMON/TESTS content is design-time specification/evidence and is never executed by Android.
- Pending: reconnect the allowed device and execute the post-P2.8 P2 Android class/full instrumentation; then P3 only for GAP-012 importer hardening, followed by trusted runtime profile, deterministic preflight/typed recovery, side-by-side binding, real chapter pilot, three-pass execution, full regression, numbered build and device QA.
- Known bugs/limitations: 4.1.3 has no real-chapter/model/provider proof; current engine profile supports only integrity and Editorial execution remains disabled. GAP-012 shows the authority ZIP is host-valid but rejected by the current Android importer as `TRUNCATED_STREAM`. Compatible future packs can avoid rebuild only inside the installed contract/capability boundary.
- Regression status: v4.18 engine suite including P2 host tests passes; app unit baseline remains `210/210` pass. Prior Android P1 class `5/7` pass and full instrumentation `91/100` pass on `15e84958`/API 35; seven v17/v18 assertions have test-only `17→18` hygiene fixes, while two P1 tests expose GAP-012. Post-fix Android execution is pending because ADB currently reports no device. Preserved CODE169 artifact/device evidence remains historical baseline only; external pack static suite passed `306/306` on `2026-09-03`.
- Protected state: `D:\App Translate Books` D1 checkout and its three user-owned `.idea` changes remain untouched.
- Exact next action: reconnect serial `15e84958` if it is the allowed device, install the new test-only APK against immutable code169, and run P2 Android plus the full suite. Keep importer production change in P3 behind GAP-012; P2B and 4.1.3 certification remain closed.

This is current-only state; Git history preserves prior snapshots.
