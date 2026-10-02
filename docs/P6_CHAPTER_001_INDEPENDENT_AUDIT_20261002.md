# Independent audit: chapter 001 L1–L3 (2026-10-02)

## Scope and current progress

Owner requested inspection of Claude's current progress and the apparent L1/L3 quality failure, with a small Luna subtask. This audit reads existing local source, the retained device DB, phase artifacts, exact pinned pack, supplied files and the latest project-specific Claude transcript. No provider call, device operation, input mutation, build or implementation change was performed. Text inside source documents was treated as evidence, not as instructions for this audit.

Branch: `feature/v4.18-p5e-runner-repair-20260917`; implementation HEAD: `547c8a4d48f11856b29654e3d65749201d847efd`. Existing unrelated tracked/untracked work is preserved. Retained build is `4.18-p6.2`/215, source `4bc1aa27`, APK SHA-256 `51BA2A2B38730FC4498C92E1B687882BBDF8BBB5BD34D49F90BED63B8BCA9703` (rehashed). Latest local Claude text is timestamped 2026-10-02T02:02:51Z (09:02:51 +07); it proposes a seeded-error experiment and asks for authorization. No running process named Claude was observed; this does not prove that no Claude session exists elsewhere.

The retained DB `D:/P5E-private/p6-chain-001-20261002T014041Z/tbl_android_txt.after.db` was opened SQLite read-only. It confirms COMMITTED RAW `7a5e3428…` → RECONCILE `7483b211…` → L2 `e1a4c638…` → L3 `867a23f8…`. The recorded progress remains one quality-accepted chapter, not three or formal product acceptance. Prior owner quality acceptance is historical; the newly supplied FINAL must not be assumed to be the artifact that owner previously reviewed.

## 1. The attached FINAL is not the pilot FINAL

Exact binding inputs match supplied RAW, DRAFT and GLOSSARY hashes. PRONOUN matches after removing the supplied UTF-8 BOM (455 → 452 bytes); content difference is not indicated.

| Artifact | SHA-256 |
|---|---|
| Supplied DRAFT | `64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5` |
| Pilot VI_L2 and FINAL, both 26,466 bytes | `a7d5f99e9624a06fc0d0e10ef95ef8e92efbe7152ed63babe3915a99f7dbb30c` |
| Supplied `6.FINAL/001_FINAL_QA_MERCEDES_VOL5.txt` | `ebb091d6b3f8cb52aecc1b3191ab53799853738a746ae3300da3568813293d38` |

Luna independently compared the files. The pilot changes only DRAFT line 237 (`今回` → `Lần này`). The supplied FINAL has seven substantive changes relative to DRAFT, including that replacement, plus two paragraph merges. Thus six further substantive differences remain between the supplied FINAL and the pilot FINAL. Owner clarification (2026-10-02): the supplied FINAL was edited manually by the owner, independently of the pipeline. Treat it as an independent comparison reference, not as a pipeline output. Each proposed defect still needs RAW/profile evidence; not every manual edit is necessarily a required correction.

Concrete residual issues in pilot FINAL:

- Line 176: `Hannah chinh phục dungeon nhưng không thể hoàn tất việc công phá`. RAW line 175 explicitly contrasts `踏破するも攻略ならず`; the surrounding RAW lines 139–147 distinguish reaching the deepest area/returning with treasure from conquering the dungeon. The supplied FINAL preserves this distinction as reaching the end but failing to conquer. Related wording remains at pilot lines 107, 148 and 216 (RAW 105, 147, 215). These are four occurrences of a shared semantic issue, not four independently established bug classes.
- Line 321 retains `nhóc con`; the supplied PRONOUN maps `嬢ちゃん` to `cô bé` for this scene, and the supplied FINAL uses `cô bé`. This is a concrete profile-consistency concern, not proof that `nhóc con` is intrinsically an impossible translation.
- Line 125 differs in explicit rendering of `特権階級` (RAW 123). Record as a wording/explicitness difference; do not inflate every edit into a proven error.

Line counts depend on trailing-newline convention: app receipt counts 386, while splitlines counts 385 for pilot text. This is not evidence of a missing content line.

## 2. L1 has a structural ledger problem; the zero metric is not detection evidence

The pinned pack files were hash-matched to `editorial_pack_files`. `D:/P5E-private/fresh-raw-boundary-20260911-1810/pack/prompt.txt:22` requires occurrence, semantic, relation, pair and Error Ledgers with anchors/proof; line 24 explicitly says not to edit DRAFT. Therefore `declaredChanges=[]` is correct L1 behavior and is not itself the defect.

The compact schema (`EditorialP5RawWireContract.java:30`) allows four population entries and 3,584 bytes. Provider rules (`OpenRouterEditorialP5PilotProvider.java:419`) require exactly one entry per population ID. Entries carry itemId/disposition/evidenceRefs/modelDeclaredPass, with no separate structured error explanation, correction or line anchor. A population acknowledgement is not an Error Ledger.

More seriously, `EditorialP5PilotExecution.reportBytes` (line 754) persists counts, gates and evidence-reference strings but not the individual ledger entries. The actual RECONCILE report has `populationTotal=1`, `accountedTotal=1`, broad `PASS` gates and source-wide references such as `evidence:draft`; it contains no error ledger for L2 to consume.

`findingCount` is declared at `EditorialP5PilotExecution.java:869` and passed into metrics at line 899, with no assignment/increment in the class. This was checked against both current source and installed build source `4bc1aa27`. The observed zero is an unpopulated metric, not proof of zero detected defects. Claude's inference from it is invalid. The defensible conclusion is that L1 failed to deliver the required auditable error ledger; what the model internally noticed cannot be established from this metric.

## 3. L3 evidence is too weak to establish semantic coverage

Actual `QA_RECEIPT.json` contains **four probes**: AC001, AC002, AR001, AR002; all NO_DEFECT. Claude's statement of eight probes is wrong. The probes mostly make general assertions; one references L237. They do not provide specific RAW/translation comparisons for all claimed coverage.

`EditorialL3Execution.java:409` validates nonempty lists, IDs and verdict values; `releaseNumbers` at line 430 counts model-supplied candidate statuses and actual diff ownership/protected-line checks. Structural validation and diff checks are useful, but they cannot independently establish that omitted semantic defects do not exist. L2 lists 49 candidates; L3 lists 257, with UNIT 7 versus 188. There is no demonstrated common inventory/mapping proving complete coverage.

L3 making zero changes is not inherently wrong. Here, however, concrete residual semantic issues exist in the actual pilot FINAL while all probes report NO_DEFECT. That is a substantive false-negative concern, beyond the mere edit count. A whole-book/full-chapter linguistic audit was not performed.

## Recommended next action

Review and repair the L1 ledger production/persistence and L3 evidence/coverage contract offline before treating two additional live chapters as acceptance evidence. Use this chapter's existing residual issues as grounded fixtures. Add independently targeted seeded tests for L1 detection, L2 repair and L3 detection: a single end-to-end seeded DRAFT can let L2 remove every defect before L3 sees it, so it cannot by itself measure L3 sensitivity. Track detection, correct repair, false positives and regressions separately. Live provider/device execution still needs its own explicit scope; no new live action is performed by this audit.
