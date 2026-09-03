# P3B — Capability evidence map

Ngày bắt đầu: `2026-09-03` (+07:00)

Baseline: branch `feature/v4.18`, starting HEAD `b5589bf5f2b3b60942841950c5877e6f8281f7ff`, app baseline `4.17-dev.1`/code169. P3B chỉ xây deterministic contract/control; không chạy chương thật, không provider/API, không execution/certification.

## Entry-gate caller inventory

| Thành phần | Production caller / vai trò hiện tại | Quyết định P3B |
|---|---|---|
| `EditorialEngineProfileResolver` | `EditorialPackImportService` là trusted compatibility path; `EditorialPackImportPageFactory` tạo resolver cho UI import; `BundledEditorialTrustedClosedRunFactsResolver` kiểm tra profile khi đóng run context. | Giữ resolver làm trust boundary; không cho legacy evaluator bypass caller production. Profile mới chỉ dùng để compatibility, không mở execution. |
| `EditorialCompatibilityEvaluator` | Legacy evaluator được `EditorialPackImportService` giữ làm fallback/test seam; engine unit/P1 characterization gọi trực tiếp. | Giữ để tương thích CODE169; production path ưu tiên trusted resolver. Không dùng declaration/model PASS làm evidence. |
| `TrustedEditorialEngineProfileCatalog` | Chỉ được `BundledEditorialEngineContractProfileRegistry` dùng làm compile-time allow-list. | Giữ bootstrap anchor nguyên trạng; thêm anchor profile P3B riêng sau khi profile evidence đã có source commit thật. |
| `BundledEditorialEngineContractProfileRegistry` | `EditorialPackImportService`, `EditorialPackImportPageFactory`, `BundledEditorialTrustedClosedRunFactsResolver`. | Giữ load-only/immutable/deterministic; bootstrap profile vẫn load được và non-executable. |
| `EditorialSafe4Workflow` | `EditorialAssetManifest`, `EditorialImportPlanner`, `EditorialPageFactory`, `EditorialRepository`, `MainActivity` dùng roles/gates/chapter read-only legacy. | Không dùng enum legacy làm run disposition; deterministic P3B types nằm trong `editorial-engine`, không đổi DB/UI/binding. |

## Capability map trước implementation

Quy tắc: chỉ ghi `Implemented` sau khi owner, positive contract test, negative test, fingerprint và source commit tồn tại. `evidenceFingerprint` là SHA-256 của descriptor deterministic do code-owned catalog tạo; không lấy từ manifest declaration, external static qualification hoặc model output.

| Capability | Production owner | Contract test | Negative test | Evidence | Trạng thái trước P3B |
|---|---|---|---|---|---|
| `pack.integrity.sha256.v1` | `EditorialPackIntegrityValidator` | Existing P1/P2 exact-byte/import tests | Existing wrong hash/length/BOM/path matrix | P2/P3A receipts + source commit `b5589bf5f2b3b60942841950c5877e6f8281f7ff` | Implemented |
| `source.preflight.safe4-full.v1` | `EditorialSourcePreflight` | P01–P09 deterministic matrix | Missing/unreadable/schema-invalid inputs | Pending | Missing |
| `bundle.phase-visibility.safe4-full.v1` | `EditorialPhaseContextProjector` | Full-bundle/projection matrix | Hidden asset, forbidden role, identity mutation | Pending | Missing |
| `status.glossary-pronoun.safe4-full.v1` | `EditorialSourceStatusResolver` | Explicit status matrix | Ambiguous/non-authoritative/implicit NONE | Pending | Missing |
| `ledger.exhaustive.safe4-full.v1` | `EditorialLedgerValidator` | Exact population/ledger coverage | Duplicate/unaccounted/modeled PASS | Pending | Missing |
| `preserve.draft.safe4-full.v1` | `EditorialStopDecision` + `EditorialReceiptValidator` | Preserve boundary test | Canon/propagation/input substitution | Pending | Missing |
| `stop.typed.safe4-full.v1` | `EditorialStopDecision` | Typed stop/recovery matrix | Wrong class/generic BLOCKED/repair loop | Pending | Missing |
| `diff.change-coverage.v1` | `EditorialDiffValidator` | App-computed diff mapping | Model-declared span/hash mismatch | Pending | Missing |
| `qa.l3-two-adversarial.v1` | `EditorialQaValidator` | Two-pass deterministic QA test | One pass/duplicate identity/model PASS | Pending | Missing |
| `release.safe4-full.v1` | `EditorialReleaseValidator` | Artifact/counter/gate acceptance | Missing artifact/nonzero release counter/auto state | Pending | Missing |
| `replay.safe4.g1-g24.v1` | `EditorialSafe4ReplayRunner` | G1–G24 executable fixture matrix | Wrong descriptor/decision/gate/equation | Pending | Missing |

## Review decision

The map has a concrete owner for every missing capability and keeps the existing integrity owner. The implementation order is: source preflight; bundle projection/status; typed stop/preserve; ledger/diff/receipt/QA/release validators; deterministic G1–G24 replay; then profile generation. No profile capability is promoted merely because the 4.1.3 manifest declares it.
