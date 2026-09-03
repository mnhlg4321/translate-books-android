# P3B — Trusted Runtime Contract/Profile Validation Report

Ngày chốt: 2026-09-03 (+07:00)

## Kết luận

P3B đã hoàn tất phần deterministic contract, validator, replay và trusted
compatibility profile. Không có provider/API call, không chạy chương thật,
không kích hoạt Editorial execution và không certify pack.

Trạng thái chốt:

    P2_COMPLETE
    P3A_GAP012_COMPLETE
    IMPORT_ACCEPTANCE_PASS
    P3B_COMPLETE
    PACK_READY_FOR_CERTIFICATION
    EXECUTION_DISABLED
    NOT_CERTIFIED
    NOT_RUNNABLE

P3B chứng minh profile có thể đánh giá compatibility một cách fail-closed.
DATA_COMPATIBLE không được dùng như bằng chứng pack runnable hoặc certified.

## Baseline và entry gate

| Hạng mục | Bằng chứng |
|---|---|
| Workspace | D:\App Translate Books\App Translate Books-translation-profile |
| Branch | feature/v4.18 |
| P3B starting commit | b5589bf5f2b3b60942841950c5877e6f8281f7ff |
| Implementation baseline trước docs closure | a58090f6ece78f1aeb224c1bd249449a23c49ed5 |
| App baseline/device restore | 4.17-dev.1 / code169 |
| Canonical ZIP | 23,638 bytes; SHA-256 B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987 |
| Java control ZIP | 23,418 bytes; SHA-256 44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5 |
| Project authority | 9,485 bytes; SHA-256 1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD |
| Prompt authority | 8,852 bytes; SHA-256 D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F |
| Workflow authority | 34,917 bytes; SHA-256 5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730 |

The two ZIP hashes and all three authority hashes were re-checked. The
bootstrap profile remains loadable but non-executable; a missing/invalid
trusted profile remains fail-closed.

The caller inventory covered EditorialEngineProfileResolver,
EditorialCompatibilityEvaluator, TrustedEditorialEngineProfileCatalog,
BundledEditorialEngineContractProfileRegistry and EditorialSafe4Workflow.
The trusted resolver remains the compatibility boundary; no legacy direct
evaluator bypass was introduced.

## Implemented contract slices

The following production owners and deterministic tests were added or
extended:

- EditorialSafe4Contract: machine contract, input roles, phase graph,
  context allow-list and contract fingerprint.
- EditorialSourcePreflight: ordered EXACT_INVENTORY, BYTE_ACCESS,
  FORMAT_ENCODING, SOURCE_SCHEMA and BYTE_IDENTITY checks.
- EditorialPhaseContextProjector: full-bundle validation separated from
  phase projection; hidden assets are not missing assets.
- EditorialSourceStatusResolver: explicit AVAILABLE, NONE and
  LEGACY_REJECTED status with provenance.
- EditorialStopDecision: typed INPUT_REQUIRED, REPAIR_REQUIRED,
  RETRY_REQUIRED and CONTENT_BLOCKED decisions plus PRESERVE_DRAFT.
- EditorialReceiptValidator, EditorialLedgerValidator and
  EditorialDiffValidator: local schema, identity, evidence, population,
  equation and actual-diff checks.
- EditorialQaValidator and EditorialReleaseValidator: two-pass QA and
  release-artifact/gate checks.
- EditorialSafe4ReplayRunner: executable deterministic G1-G24 cases.
- EditorialEngineContractCapabilityEvidenceCatalog and
  EditorialSafe4TrustedProfileFactory: code-owned capability evidence and
  profile construction.
- EditorialEngineProfileResolver and
  BundledEditorialEngineContractProfileRegistry: trusted profile parsing,
  canonical-hash/evidence/machine-contract validation and unknown-capability
  fail-closed behavior.

No database schema, UI, project/run binding, provider adapter, execution
protocol or build version metadata was changed. GAP-012 importer code was not
changed in P3B.

## Trusted profile v2

| Field | Value |
|---|---|
| Profile ID/version | com.ml.tblandroidtxt.editorial.engine.safe4.full / 2.0.0 |
| Contract bounds | safe4.full.three-pass.v1 to safe4.full.three-pass.v1 |
| Receipt schema | safe4.full.receipt.v1 |
| sourceCommit | 3156835d1cc6b723d7932709224bf626dc7a1747 |
| Resource SHA-256 | 1b2db011d59f3e2ef4349aeb0daa9c54a19b7efd1e2ca6886bc29b56e4690d62 |
| Canonical profile hash | beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21 |
| Machine contract fingerprint | a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3 |
| implementedCapabilities | 11/11 exact capability IDs |
| explicitlyMissingCapabilities | none |
| Execution | disabled |
| Replacement/rebind | automatic replacement false; automatic project rebind false |

Each implemented capability has a production owner, positive contract test,
negative/adversarial test, deterministic evidence fingerprint and real
source commit. The profile was generated from the canonicalizer; its hash was
not edited manually and does not self-reference the profile commit.

Pack identity remains canonical manifest semantics plus exact authority
hashes. ZIP SHA-256 remains only the checksum of a transport artifact. The
canonical and Java-control ZIPs therefore resolve to the same logical pack
identity even though their transport hashes differ.

## Required focused evidence

| Area | Result |
|---|---|
| P01-P09 source preflight | 9/9 PASS |
| G1-G24 deterministic replay | 24/24 PASS |
| Typed stop/recovery and PRESERVE_DRAFT | PASS, including schema/truncation/input/content negatives |
| Bundle versus phase visibility | PASS, including hidden asset, optional Pair Context and identity invariants |
| Pronoun status matrix | PASS, including AVAILABLE/NONE/LEGACY_REJECTED, ambiguity and quarantine |
| Receipt/ledger/diff/QA/release validators | 7/7 receipt/coverage tests PASS; negative equations and model-PASS rejection PASS |
| Trusted profile acceptance | 3/3 engine tests PASS |
| Profile asset verification | PASS |
| Unknown required capability | ENGINE_UPGRADE_REQUIRED with missing capability ID; no crash or downgrade |
| Wrong machine/missing evidence/tampered profile hash | rejected fail-closed |

The five receipt artifact types REPORT_L1, VI_L2, CHANGE_MAP_L2, FINAL_QA
and QA_RECEIPT are all covered. Zero population and zero edit remain valid
values. Model-declared PASS, hash, population and diff are not authoritative.

## Host, Android and external validation

Host commands and results:

- :editorial-engine:test :app:verifyEditorialEngineProfileAsset
  :app:testDebugUnitTest --no-daemon — PASS; engine 161/161 and app unit
  210/210.
- Focused P3B trusted-profile acceptance — 3/3 PASS.
- External TESTS/test_full_release.ps1 — 306 PASS, 0 FAIL,
  OLD_WORDS=5308, NEW_WORDS=7050, EXACT_RETAINED=223/311.
- git diff --check — PASS at closure.

On allowed device 15e84958 (CPH2691, API 35), validation used the separately
archived 4.17-dev.3/code171 APK and the AndroidTest APK. The device was
restored and verified at 4.17-dev.1/code169 after testing.

| Instrumented scope | Result |
|---|---|
| EditorialPackImportServiceInstrumentedTest | 13/13 PASS |
| EditorialP1PackImportCharacterizationInstrumentedTest | 7/7 PASS |
| EditorialP2ReferencePackImportInstrumentedTest | 3/3 PASS |
| EditorialP3BTrustedProfileInstrumentedTest | 1/1 PASS |
| EditorialPackRuntimeWiringInstrumentedTest | 5/5 PASS |
| Full Android instrumentation | 104 total: 103 PASS, 1 approved real-API skip, 0 failures |

The full suite increased from 103 to 104 because the one P3B trusted-profile
instrumented test was added. The real API test stayed skipped by opt-in and
provider/API call count was 0.

Validation APK:

    artifacts/builds/v4.17-dev.3/build-20260903-193024/TranslateBooks-v4.17-dev.3-code171.apk
    SHA-256 034F33485352AC4C019C3363B4666C880CD036AF6CB1550DDADCB5ABF0923A33

AndroidTest APK SHA-256:

    A99CFD53D1E03E578FF6FA98AD684CA4EA5D97193BB524F7967DFB6DCE68E0BB

The initial full instrumentation run exposed four failures in legacy
bootstrap wiring fixtures after v2 was bundled. Test setup was isolated to a
bootstrap-only registry for those historical fixtures; no production wiring
was altered. The rerun passed.

## Compatibility acceptance

- Canonical 4.1.3 and synthetic 4.1.4, with the same contract/capability
  bounds, return DATA_COMPATIBLE through the trusted resolver.
- Unknown required capability returns ENGINE_UPGRADE_REQUIRED with the exact
  missing capability and does not silently downgrade.
- Wrong machine fingerprint, missing evidence and tampered canonical profile
  hash fail closed.
- Bootstrap v1 remains loadable and non-executable.
- Resolver choice is deterministic and does not use filename, display version
  or ZIP SHA as compatibility identity.
- Android trusted-profile test confirms import can reach
  STORED_READY_FOR_CERTIFICATION while EditorialSafe4Pack.executionEnabled()
  remains false and no CERTIFIED row is created.

## Production-change guard

The P3B production changes are limited to deterministic Safe4 contract,
validator, evidence-catalog, profile-factory, resolver and registry owners in
editorial-engine/src/main, plus app/build.gradle asset verification/bundling.
Test setup changes are confined to AndroidTest. There are no changes to
app/src/main, database schema, UI, provider/API wiring, project/run binding,
authority bytes, canonical ZIP, GAP-012 importer or build version metadata.
The original workspace D:\App Translate Books was not modified.

## Commit lineage

The P3B implementation was split into scoped commits:

    3c3f81f  test(editorial): freeze P3B capability evidence map
    a314b89  feat(editorial): add deterministic source preflight
    d551b49  feat(editorial): add phase visibility and source status
    08955b4  feat(editorial): add typed stop and preserve decisions
    fb0bca0  feat(editorial): validate receipts ledger and changes
    4f9fd73  test(editorial): execute safe4 full replay cases
    fae654d  feat(editorial): publish trusted contract evidence owners
    d74cedc  feat(editorial): add reproducible trusted profile generator
    ea71245  docs(editorial): snapshot P3B implementation progress
    750bd2f  feat(editorial): bind safe4 machine contract and profile resources
    3156835  feat(editorial): fail closed on unknown required capabilities
    388be8f  feat(editorial): publish qualified trusted profile
    585e4eb  test(editorial): verify P3B trusted profile acceptance
    cd1d2b0  test(editorial): isolate bootstrap wiring fixtures
    a58090f  test(editorial): cover all Safe4 receipt artifacts

The final documentation/state update is kept separate from these
implementation/test commits.

## Handoff

P3B does not open execution. The next work must be a separately approved
binding/resume or controlled-pilot phase. Any future execution work must add
failing contract tests for its own scope, retain the trusted resolver and keep
the profile state NOT_CERTIFIED / NOT_RUNNABLE until real certification
evidence exists.
