# P3B — Capability evidence map

Ngày chốt: `2026-09-03` (+07:00).

Baseline: workspace `D:\App Translate Books\App Translate Books-translation-profile`, branch `feature/v4.18`, P3B starting HEAD `b5589bf5f2b3b60942841950c5877e6f8281f7ff`, app baseline `4.17-dev.1`/code169. P3B chỉ xây deterministic contract/control và trusted compatibility profile; không chạy chương thật, không provider/API, không Editorial execution/certification.

## Entry-gate caller inventory

| Thành phần | Production caller / vai trò hiện tại | Quyết định P3B |
|---|---|---|
| `EditorialEngineProfileResolver` | `EditorialPackImportService` là trusted compatibility path; `EditorialPackImportPageFactory` tạo resolver cho UI import; `BundledEditorialTrustedClosedRunFactsResolver` kiểm tra profile khi đóng run context. | Giữ resolver làm trust boundary; không cho legacy evaluator bypass caller production. Profile v2 chỉ dùng để đánh giá compatibility. |
| `EditorialCompatibilityEvaluator` | Legacy evaluator được `EditorialPackImportService` giữ làm fallback/test seam; engine unit/P1 characterization gọi trực tiếp. | Giữ để tương thích CODE169; trusted production path phải đi qua resolver/normalizer. |
| `TrustedEditorialEngineProfileCatalog` | Chỉ `BundledEditorialEngineContractProfileRegistry` dùng làm compile-time allow-list. | Giữ bootstrap anchor nguyên trạng; thêm anchor v2 độc lập. |
| `BundledEditorialEngineContractProfileRegistry` | `EditorialPackImportService`, `EditorialPackImportPageFactory`, `BundledEditorialTrustedClosedRunFactsResolver`. | Load-only, immutable, deterministic; bootstrap v1 vẫn load được, Safe4 v2 có shape contract nhưng không mở execution. |
| `EditorialSafe4Workflow` | `EditorialAssetManifest`, `EditorialImportPlanner`, `EditorialPageFactory`, `EditorialRepository`, `MainActivity` dùng roles/gates/chapter read-only legacy. | Không dùng enum legacy làm run disposition; P3B types/validators nằm trong `editorial-engine`, không đổi DB/UI/binding. |

## Final evidence rule

Một capability chỉ được công bố `implemented` khi catalog code-owned cung cấp production owner, positive contract test, negative/adversarial test và fingerprint; profile v2 còn pin `sourceCommit` thật `3156835d1cc6b723d7932709224bf626dc7a1747`. Fingerprint là SHA-256 của descriptor deterministic, không lấy từ manifest declaration, external qualification hoặc model output.

## Capability map sau P3B

| Capability | Production owner | Contract test | Negative test | Evidence fingerprint | Source commit | Trạng thái |
|---|---|---|---|---|---|---|
| `pack.integrity.sha256.v1` | `EditorialPackIntegrityValidator` (P3A owner) | `EditorialP2ReferencePackImportInstrumentedTest` | `EditorialPackIntegrityValidatorTest` + P2 negative matrix | `a448c80dd20582cfb1e0b0d0590be11a8e16c926446db14b6b41be5050948696` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `source.preflight.safe4-full.v1` | `EditorialSourcePreflight` | `EditorialSourcePreflightTest.P01-P09` | `EditorialSourcePreflightTest.P05-P07` | `9da27922ea96f60fb978fea22a2178a21d6ec4c15ceb2913aa55a244bd67cf82` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `bundle.phase-visibility.safe4-full.v1` | `EditorialPhaseContextProjector` | `EditorialPhaseContextProjectorTest` | `EditorialPhaseContextProjectorTest.fullBundleRejectsDuplicateRoleAndSourceId` | `6f9bf170f58a0514e76a9e4ba2f128fc5322f96cd365057dd6f768c3e5cf78b5` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `status.glossary-pronoun.safe4-full.v1` | `EditorialSourceStatusResolver` | `EditorialPhaseContextProjectorTest.noneStatusIsRetainedWhenPronounIsHidden` | `EditorialPhaseContextProjectorTest.fullBundleRejectsDuplicateRoleAndSourceId` | `bd154028aa8015f0ef2ce888ed2d2d4b4e4c782ce6bafb1f9e2982d0dfff401b` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `ledger.exhaustive.safe4-full.v1` | `EditorialLedgerValidator` | `EditorialReceiptAndCoverageValidatorTest.ledgerIsExhaustiveAndDoesNotTrustModelPass` | Same test: duplicate/unaccounted/model `PASS` | `112aa6a1e328e5a8f209cf586f5dd86427e0aa8f0dfdf85cfbe0c8c765128146` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `preserve.draft.safe4-full.v1` | `EditorialStopDecision` + `EditorialReceiptValidator` | `EditorialReceiptAndCoverageValidatorTest.preserveDraftCannotBecomeCanonOrPropagation` | Same test: no canon/propagation and no input substitution | `eba222c50395c337109e5cac5c5fcae05855dc0ca8186de4368a859035d551a3` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `stop.typed.safe4-full.v1` | `EditorialStopDecision` | `EditorialStopDecisionTest.inputMissingIsTypedAndHasRecovery` | `EditorialStopDecisionTest.schemaFailureIsRepairNotContentConflict` | `984e2d2496c1401a9c2ddaa7527be1128bc7dd5db0ffbc0a2757c8e274037737` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `diff.change-coverage.v1` | `EditorialDiffValidator` | `EditorialReceiptAndCoverageValidatorTest.actualDiffMustMatchDeclaredErrorMapping` | Same test: model-declared span/hash mismatch | `cee7cc0107edeab0a113ea761efb5bcd2a52482ffd6ca7e87e46e6d6b798ac04` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `qa.l3-two-adversarial.v1` | `EditorialQaValidator` | `EditorialReceiptAndCoverageValidatorTest.qaRequiresTwoIndependentPassesAndReleaseIsFailClosed` | Same test: one pass/duplicate identity/invalid pass | `91ca161da682162d8fea3abed523fc85d4c9d4bb6567c2b063d3525c42f0a1d4` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `release.safe4-full.v1` | `EditorialReleaseValidator` | `EditorialReceiptAndCoverageValidatorTest.qaRequiresTwoIndependentPassesAndReleaseIsFailClosed` | Same test: missing artifact/nonzero counter/auto state | `570b7527222ea949e004b80e058b929a00fce6aae6183bc1ca65a5f73f64364c` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |
| `replay.safe4.g1-g24.v1` | `EditorialSafe4ReplayRunner` | `EditorialSafe4ReplayRunnerTest.G1ThroughG24ExecuteWithDeterministicExpectedDecisions` | `EditorialSafe4ReplayRunnerTest.replayCaseRequiresDecisionReasonGateAndEquation` | `fe6b1d21cb1d457ad196f8d17ed32ad4a56f07b09d0e196a3a0b7174a6e93e93` | `3156835d1cc6b723d7932709224bf626dc7a1747` | Implemented |

Profile v2 declares exactly these 11 capabilities in `implementedCapabilities` and has `explicitlyMissingCapabilities=[]`. The profile itself is anchored independently by:

- ID/version: `com.ml.tblandroidtxt.editorial.engine.safe4.full` / `2.0.0`.
- Resource SHA-256: `1b2db011d59f3e2ef4349aeb0daa9c54a19b7efd1e2ca6886bc29b56e4690d62`.
- Canonical profile hash: `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21`.
- Machine contract fingerprint: `a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3`.

`EditorialSafe4Pack.executionEnabled()` remains `false`; all evidence above is deterministic contract/validation evidence, not proof of real chapter execution or certification.
