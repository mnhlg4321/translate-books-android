# P6 R6 — Z1–Z4 execution (2026-10-05)

Scope: `docs/P6_R6_DECISIVE_FIX_PLAN_20261005.md` (owner approved D-Z3). Z1–Z4 are offline: **0 provider calls**; the G1 ledger
`G1-20261003-ecbf8c55` is unchanged (11 settled calls, USD 0.09358090 / 1.00, 0 pending).

## Z1 — classification of every rejection code (commit `652bc3b1`, completed in `1cfb6e5e`)

- `docs/P6_R6_VALIDATION_RULE_CLASSIFICATION.md` is generated from `editorial-engine/src/test/resources/rejection-classification.csv`:
  every code literal of the L1/L2/L3/final-read parsers, the change-map reconstructor, the receipt/ledger/diff validators and the legacy
  P5 wire contract (423 literals): SEMANTIC 132, APP 169, PROTOCOL 30, MIXED 20, BOOKKEEPING 11, NOT_A_CODE 61.
  `EditorialRejectionClassificationTest` fails when a literal is missing from the table or a BOOKKEEPING code is still thrown.
- BOOKKEEPING rows are normalized with a `kind:path` note (REPORT_L1 `normalizations`, L2/L3 `wireWarnings`, final-read evidence) instead of a
  refusal: unknown keys; unused anchor fields (`draft.after` of LINES, `start`/`end` of MISSING — the W4 refusal); absent MAY keys and arrays;
  note text (control characters, length); unusable `evidenceRefs`/`occurrenceUnits`; whole speaker records; protected-span id/range/reason;
  reason and stop class of a CONTINUE/PRESERVE_DRAFT disposition; final-read caps. `contractRevision` is `L1_LEDGER_V10`.

## Z2 — mutation suite, RAW anchors, strict L2/L3 schemas (commit `1cfb6e5e`)

- `EditorialWireMutationEngine` + `EditorialWireMutationSuiteTest` (synthetic: L1 RAW/RECONCILE, L2 edit, L3 reconcile, final read) and
  `:editorial-engine:mutateP6WireResponses` (offline, saved live responses). Report `D:\P5E-private\z2-mutation-report-20261005.json`:
  the three saved live RECONCILE baselines (T3, U6, W4) with their RAW responses x 63 variants each (48 bookkeeping variants must parse,
  15 semantic variants must be refused with a SEMANTIC/PROTOCOL/MIXED code): **0 defects**. (Of the saved live responses, 6 of 7 are used as
  baselines; the seventh, the very old RAW with `L1173`, is the expected-reject replay case.)
- The matrix found one more bookkeeping row: the model's RAW unit number is a hint. The unit carrying `rawQuote` is the anchor
  (`rawAnchorDerivedFromQuote`; ambiguous => `L1_RAW_QUOTE_AMBIGUOUS`; none => `L1_RAW_QUOTE_NOT_IN_ANCHOR`), also for L3 probes. The saved W4
  RECONCILE response (E-L321 quoted from RAW line 323) now parses, with its three `after` fields ignored.
- `EditorialStrictSchema` generates strict `json_schema` for the ledger wires of L2 (discovery, edit, final read) and L3 (re-audit, reconcile,
  final read) from `EditorialFieldSpec`; the L2/L3 providers use it (legacy wires keep `json_object`). Golden wires pass schema and parser.
- Replay-all `D:\P5E-private\p6-z2-replay-all-manifest-20261005.json` (SHA-256 `FA73C598B75DFC3FBF4094C2A82DE4BA209D76B01ACAFC864205741878FFA3C1`)
  result `…p6-z2-replay-all-20261005.json` (SHA-256 `68CFA5D362FBA76AFE7E8D6CA17F808F621EAEF46CA19A00A337B739873A17E5`): PASS — old RAW expected
  reject; T3, U6 (`speakerRecordsDropped=2`) and W4 RECONCILE responses PASS.

## Z3 — run rule (commit `808dd9e9`)

`run_group.ps1` (live): a typed refusal of the production engine is a measurement. `scripts/p6/group_policy.py` classifies each fixture
VALID / REFUSED (REPAIR_/CONTENT_/INPUT_ stop with its `CODE:path`) / INFRASTRUCTURE (RETRY_/STOP_/budget/UNKNOWN cost/harness crash/missing
output) and stops the group only on infrastructure, the group cap (spend precheck) or three consecutive refusals with the same code.
`-StopOnFirstRefusal` keeps the old rule. The instrumented runner writes `measuredRefusal` and does not fail on a typed refusal;
`verify_fixture_run.py --measure-refusals` verifies prompts, ledger and stops of the executed fixtures. Python 49/49.

## Z4 — wrapper builds and emulator checks

- Source `808dd9e95e289162b826756d2b304b1bbd09ec11`, clean worktree `D:\P5E-builds\wt-p6-z4-20261005` (removed afterwards).
- Production `4.18-p6.23` / code 236, event `build-20261005-190002`, APK SHA-256 `B13A59A162C6EC1D7305B571DF50AEFCE8FADFA0EB5D81B316F5DE34052754F6`,
  source ZIP SHA-256 `C561FB701046B1C6E75F9A81D98C284B86F07B8528ECD8CD13715B9CDD97E207`; 5-file payload in `artifacts/builds` and `backup/builds`.
- AndroidTest event `p6-z4-808dd9e9-20261005-01`, APK SHA-256 `DB313687AC2A9CC847CDDF4EE85975C2D9FDF396F6C7EA7905B0A1288715238F`; 8-file payload in
  `artifacts/test-builds` and `backup/test-builds`.
- Installed on `emulator-5554` only (the AVD was restarted for the session); the installed base APK hash equals the archive. Preflight class 5/5,
  coordinator class 12/12, fake CHAIN `2dc1e688-…` `STRUCTURAL_VALID 14/14` (0 actual calls, 1,680 entries, 0 pending), negative gate `1428e513-…`
  `0/1` as expected (0 calls). The G1 device ledger was untouched (8,228 bytes, SHA-256 `e82b0e88…dfdf77`).
