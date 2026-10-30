# Translate Books 4.24.1 Release Notes

Status: patch release 4.24.1 (phone app only), built from branch `feature/v4.24.1` created from tag `v4.24.0`. The owner checked the fix on phone 15e84958 (dev build 4.24.1-dev.5, same code) and approved the release on 2026-10-30. Not merged to `main` and not pushed by the build session.

## 4.24.1 changes

- Library tab row (Thư viện): the four tabs RAW, Glossary, Pronoun, Tập are no longer clipped at the bottom (the 56 dp row left 46 dp for 48 dp tabs), are four equal tabs filling the row beside the menu, keep 8 dp of padding around the label (was 4 dp), and the chosen tab has a full 2 dp border.
- The menu button next to the tabs has the tabs' height, corner radius and border and a larger glyph (20 sp); its description is unchanged.
- When large text or a narrow screen makes the row scroll sideways, the chosen tab is scrolled into view.
- The PC app is not part of this release (it uses a JavaFX navigation tree); the PC package stays 4.24.0.

## 4.24.1 known limitations

- The page scrollbar (system overlay) is still drawn over the right border of the cards on some pages; not changed.
- The release APK is a debuggable build, same flavour as 4.24.0.
- Startup, run and PC measurements are those of 4.24.0 (the change is limited to the Library tab row); real-phone performance NOT_MEASURED.
- Editorial stays frozen; the msi stays unsigned; Tập still does not travel in the library ZIP.

---

# Translate Books 4.24.0 Release Notes

Status: release 4.24.0 (phone APK and PC package from one commit). Owner sign-off R5 recorded in `docs/V424_RELEASE_PLAN.md` section 8. Not merged to `main` and not pushed until R7.
Branch: feature/v4.24 (release build on the short-lived `feature/v4.24.0`, as v4.21.0 did).

## 4.24.0 changes

- Shared translation core: storage, the run loop and chapter-set planning moved into `translation-core` (plain Java, JDBC); the phone app and the new PC app use the same code. Golden outputs unchanged (14/14).
- New PC app (Windows, JavaFX, per-user `.msi`, jlink runtime): RAW, Glossary, Pronoun, Tập, Dịch, Công việc, Cài đặt, import/export of the phone library ZIP and settings profile, confirmation before any spend.
- Library, Tập (chapter sets) and per-chapter translation from v4.23, with the readable run tracking screen.
- Fix: phone Library ⋮ menu opened with zero height on the real phone; it is now anchored to the tapped button (source test blocks the old anchor).
- Installer ProductVersion rule: display `rc.N` = `4.24.(100+N)`, release `4.24.P` = `4.24.(200+P)`; so 4.24.0 installs as 4.24.200 over RC 4.24.101.

## 4.24.0 known limitations

- Biên tập (Editorial) stays frozen on both apps; the PC app has no Editorial.
- The `.msi` is not code-signed; SmartScreen will warn.
- The phone APK is a debuggable build, same flavour as the RC and v4.21.0.
- Tập (chapter sets) do not travel in the library ZIP between phone and PC.
- Pronoun on the PC is edited as text, not per cell.
- Schema upgrade v1-v29 on the PC is NOT_MEASURED; the PC only uses schema v30.
- Translation quality uses the phone's prompt v18; not re-measured on a real book translated by the PC.
- Emulator cold start 3.2-4.8 s (same range as v4.21.0 in the same emulator state); real-phone performance NOT_MEASURED.

---

# Translate Books 4.21.0 Release Notes

Status: release candidate preparation; owner approved tag/merge when every condition in docs/V421_E_RELEASE_WORK_REQUEST.md section4 passes.
Branch: feature/v4.21. Shipped version remains4.20.0/code270 until release closure.

## 4.21.0 changes

- Editorial restores DRAFT punctuation from the aligned RAW for quotation pairs
  `〝〟`, `「」`, `『』`, full-width colon `：` and ellipsis `……`.
- Restoration is deterministic: punctuation only, never letters or numbers.
  It does not call a model; chunks requiring only mark restoration consume no
  provider request. Model edits remain subject to the existing protection checks.
- Contract CP-IMPL-19: old-contract Editorial runs must be replaced by a new run;
  existing results remain viewable and exportable.

## 4.21.0 known limitations

- Only unambiguous one-to-one alignments with matching mark counts are restored.
  Nested or omitted quotes, `《・　・　・》` and RAW aligned across multiple DRAFT
  lines still need manual correction; ambiguous mappings keep the DRAFT.
- Restoration takes effect only when the chapter passes through Editorial;
  this does not change the translation-stage prompt or automatically process old results.
- Earlier pronoun/finder limitations remain. Performance and QA are measured
  on the emulator, not a physical device. No paid run is authorized for this release.

---

# Historical Translate Books 4.20.0 Release Notes

Status: tagged v4.20.0 after all E-bis release conditions passed; main integration/publication is the authorized final step.
Branch: feature/v4.20. Release APK4.20.0/code270, CP-IMPL-18, source261d3b8b; annotated tag atQA source2bc2384f (production inputs identical).

## 4.20.0 changes

- C1: an ADDRESS edit must cite its numbered pronoun row as `[id] R<n> <line>`.
  The app checks the change against that row; missing or incompatible citations
  keep the original DRAFT line.
- C2': one malformed point answer keeps its DRAFT line and rejection reason;
  two or more malformed point answers block the chunk. Anchoring errors still
  block; protected-symbol checks are unchanged.
- Chapter headings: a Han chapter number copied from RAW (for example `○一七`)
  is no longer treated as untranslated Japanese, so the heading is not sent to
  the model; the KANA rule text is now Vietnamese.
- Glossary lock: an answer for a non-ADDRESS point that drops a glossary term
  the DRAFT already uses correctly keeps the DRAFT line (`GLOSSARY_TARGET_REMOVED`);
  it never blocks the chunk.
- Contract CP-IMPL-18: old Editorial runs cannot continue under the new contract;
  create a new run. Existing results remain viewable and exportable.

## 4.20.0 known limitations

- Self-address errors not flagged by the finder can remain. Translation-stage
  pronoun selection is not fixed, nor are chapter 004 narration errors.
- The model sometimes omits the row number; such ADDRESS changes are discarded
  (5 times in chapter 017).
- Owner readings: chapter 014 accepted (8 first-person fixes, 0 wrong), three
  chunk-13 replay lines correct, chapter 017 16/17 correct; the wrong line was the
  heading case fixed above (verified by replaying the stored 014 and 017 answers).
- Device, performance and media evidence come from the emulator, not a physical device.

---

# Historical Translate Books 4.18.0 Release Notes

Status: `RELEASE_CANDIDATE` (not tagged, not merged to `main`)

Branch: `feature/v4.18-p5e-runner-repair-20260917`

## Summary

Version 4.18.0 releases the chunk-based Editorial (Bien tap) flow. The app aligns a
chapter's RAW and DRAFT, cuts the chapter into chunks, shows the plan with a verdict,
and, after confirmation, reviews each chunk and applies fixes only at the points it
flags. The earlier V5 three-pass path stays in the code as a frozen path and is not
part of this release.

## Scope

- Source selection and plan: RAW/DRAFT alignment, chunk plan, OK / WARN / BLOCK
  verdict shown before any provider request.
- Point-based fixes: address, glossary and kana points are flagged; a model proposal
  is accepted only when the deterministic guard passes, otherwise the DRAFT line is
  kept.
- Persistence: runs are stored in SQLite (database version 29) and survive restart and
  process death; the result is exported as TXT.
- Spend control: ledger and per-run cap; a plan must be confirmed first.
- Translation, Glossary, Pronoun and Settings are unchanged by this work.

## Known limitations

- Three chapters were measured and accepted by the owner. Their recorded misses stay:
  006 keeps 13 pronoun misses and the `cau` misses (CP-IMPL-9); 008 keeps 7 `cau`
  misses; 011 keeps 2:2 (correct fix blocked by the third-person rule), 12:2
  (uncertain) and 10:6 (missed). Details: `docs/EDITORIAL_CHUNK_PLAN.md` section 6.
- The call-name prompt (CP-IMPL-14) was measured on chapter 011 only. CP-IMPL-10
  to 13 were included in later live builds, but not every original case on 006/008
  was re-measured. CP-IMPL-15 is in the RC and device QA; its semantic effect is
  supported by stored-response replay, not a new live model evaluation.
- Performance numbers come from the emulator, not a physical device.
- The V5 three-pass gates in section C of the v4.18 checklist are out of this
  release's scope; they stay unchecked and are not waived.

## Verification

Independent review at `0d6502bc`: RC artifacts are valid, but QA corrections remain
open before D-R2; see `docs/EDITORIAL_CHUNK_PLAN.md` section 6. The archived RC
notes remain immutable. UI media exercises controller callbacks; it does not prove
system-picker interaction or semantic quality. Benchmark APKs were retained late
during review; original wrapper/archive-before-install compliance is not proven.

QA closure after the review: see `docs/EDITORIAL_R1_QA_ADDENDUM_20261012.md` (method-level PASS/SKIP/FAIL, glossary test fix, typed collector, strict archive events); the 4.18.0 package itself is unchanged.

- Frozen allowlist on `emulator-5554`: 117 discovered, 116 PASS, 1 opt-in UI seed SKIP, 0 failures (R1.1 and RC logs).
- Offline on a clean archive of `12824cee`: engine 706, app 472 per variant, lint
  0 errors / 57 warnings, `scripts/p6` 106, `scripts/chunk` 24.
- Device QA of the chunk scope: `artifacts/qa/v4.18-p7-device-12824cee/QA_REPORT.md`.
- Macrobenchmark and Perfetto: `artifacts/qa/v4.18-r1/r1.3-performance/`.
- Release-candidate device QA and the release payload: see the R1.5 entry in
  `docs/EDITORIAL_CHUNK_PLAN.md` section 6.
- Screenshots and video use synthetic data only; they contain no book text.

## Local artifact

The release-candidate APK, source ZIP and checksums are recorded in
`artifacts/releases/v4.18.0/<event>/` and `backup/v4.18.0/<event>/` by
`scripts/archive-release.ps1` (R1.5). Tag `v4.18.0` and the merge to `main` need the
owner's decision (D-R2).

---

# Translate Books 4.17 Development Artifact

Status: `D5_LOCAL_BUILD_QA_COMPLETE / V4.17_DEV_ARTIFACT_READY`

This recovery build continues the code113 translation workflow on the historical
recovery branch `feature/v4.17-translation-profile-compatibility`. It is a numbered
development artifact, not a public `v4.17` release or tag.

## Scope

- Restore chapter-by-chapter translation from the code113 source baseline.
- Glossary runtime uses `source,target,category,note`; a fifth `priority` column is
  tolerated for file review and ignored at runtime.
- Pronoun runtime uses
  `from,speaker,target,self,call,scope,note`, while legacy three-column input remains
  supported.
- Matched-only compact prompt locks preserve Glossary notes and scoped Pronoun
  rules through translation, refinement, settings snapshots, resume and retry.
- RSC, Editorial and IPC workflows are not activated by this recovery.

## Known limitations

- Multiline quoted CSV fields remain unsupported by the existing line-based parser.
- Malformed Pronoun P3 scopes fail closed.

## Local artifact

- Version: `4.17-dev.1` / Android versionCode `169`.
- Build event: `build-20260829-185818`.
- Build commit: `b0ab983bd4f9b017819af2bb0043bc57caad807a`.
- APK: `TranslateBooks-v4.17-dev.1-code169.apk`, 2,784,446 bytes, SHA-256
  `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`.
- Source ZIP: `project_source_build-20260829-185818.zip`, SHA-256
  `AA3A75CF4CFC4C527352F037D3AABEC9BBF24517138F58DB974794CEB67EAA6F`.
- Matching immutable payloads:
  `artifacts/builds/v4.17-dev.1/build-20260829-185818/` and
  `backup/builds/v4.17-dev.1/build-20260829-185818/`.
- Local QA: 205 JVM tests passed; Lint 0 errors/53 warnings; package and SDK
  manifest metadata verified statically.
- Device QA: not run because no device was connected; no provider/API request was
  sent.

This is a development artifact, not a public release or tag. Public tag/archive,
device QA, benchmark, Perfetto, screenshot and video evidence remain pending.

---

# Translate Books 4.15 Release Notes

Release date: 2026-07-29

Git tag: `v4.15`

## Summary

Version 4.15 fixes three workflow problems reported on the phone UI: selecting a
Glossary or Pronoun no longer sends the list back to the top, list imports can
select several files and create one independent profile per file, and saved
Provider, Prompt, and Performance settings are protected from being overwritten
during lazy screen construction, refresh, reset navigation, or app reopening.

## Highlights

- Preserve the exact Glossary and Pronoun list scroll position when `Use` changes
  the active profile.
- Keep editor navigation's intentional top-of-page behavior separate from
  list-level selection.
- Enable Android multi-select for list-level Glossary and Pronoun imports.
- De-duplicate returned URIs while preserving selection order.
- Persist every valid selected file as a separate profile with its own ID and
  source filename; activate the first valid imported profile.
- Report invalid siblings independently without discarding valid imports.
- Keep Pronoun replacement single-select and label intentional Glossary editor
  combination as `Merge files`.
- Hydrate lazy Settings sections from persisted values before any control can
  write defaults.
- Save Settings safely through normal lifecycle transitions without clearing
  Provider, Prompt, or Performance values.

## Pre-tag verification

- Exact archive-first APK: `4.15`/code62, SHA-256
  `7FDF60C934E75F4ACF47D77E14E248FBAC21F2EF5DDAD5E086AE586729163969`.
- Exact source ZIP SHA-256:
  `FEFEDF516FD85B3166C6BB36249C22C94909D4428844EAC5E8E811F06015F5EE`.
- Approved-logo guard and all 110 JVM tests passed.
- Android Lint completed with 0 errors and 53 warnings.
- Exact-code62 connected instrumentation completed with 15 passed, 1 explicitly
  opt-in paid real-API case skipped, and 0 failures/errors.
- Physical-device AndroidX Macrobenchmark passed all five cold starts at
  313.89/330.23/335.04 ms minimum/median/maximum and retained five real Perfetto
  traces.
- Five device screenshots, matching UI hierarchy dumps, and a documented
  25-frame MJPEG device sequence were retained in matching QA artifact/backup
  stores.

Final counts, hashes, artifact locations, benchmark measurements, and known
limitations are recorded in `QA_REPORT_v4_15.md`.

## Known limitations

- Android Lint retains 53 warnings and no errors.
- The paid real-API instrumentation case remains explicitly opt-in and was not
  run during release QA; no provider request or billing occurred.
- OnePlus Android 15 denies shell `screenrecord`. The retained AVI is explicitly
  a timed sequence of actual-device screenshots, not continuous MediaProjection
  capture.
- AndroidX reports `run-from-apk` compilation mode for the startup benchmark.

## Distribution

The source history and corrected annotated `v4.15` tag are published to the
private GitHub repository. The exact APK and immutable release evidence remain
outside Git by design in matching `artifacts/releases/v4.15/tag-v4.15/` and
`backup/v4.15/tag-v4.15/` payloads and must be distributed separately.
