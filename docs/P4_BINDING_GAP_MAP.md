# P4 — Binding/Resume Gap Map

Ngày cập nhật: `2026-09-04` (+07:00)

Baseline vào P4: branch `feature/v4.18`, commit
`270759e5589b2e9101c3e1a5a6b84cff12ec2fd3`, app `4.17-dev.1/code169`.

Quy ước: kết luận gap chỉ dựa trên test hoặc bằng chứng source cụ thể.
Các gap dưới đây là gap CODE169 trước P4; cột kết quả ghi rõ P4 đã xử lý đến
đâu. P4 không mở execution, provider hoặc certification.

| ID | Yêu cầu 4.1.3/P4 | CODE169 hiện hỗ trợ | Test/bằng chứng | Gap | Đề xuất tối thiểu | Phase xử lý |
|---|---|---|---|---|---|---|
| GAP-P4-001 | Project mới phải bind đúng pack được người dùng chọn. | Không: `EditorialRepository.createProject()` hard-code legacy `EditorialSafe4Pack.VERSION/PACK_HASH`, không nhận imported 4.1.3. | `EditorialP4BindingCharacterizationInstrumentedTest.legacyRepositoryDoesNotAcceptImportedPackWithoutP4Service`; source `EditorialRepository`. | Legacy owner không biểu diễn pack/profile/evaluation imported. | Dùng `EditorialP4BindingTransactionService` làm owner setup; giữ legacy API read-only. | Đã xử lý P4 |
| GAP-P4-002 | Binding phải giữ immutable pack/profile/evaluation/contract/input identity. | Một phần: các store v17/v18 giữ các identity liên quan nhưng thiếu đầy đủ tuple, source reference, encoding và schema status. | `EditorialP4BindingCharacterizationInstrumentedTest.v18ScopeEntryStoreMustRetainP4SourceIdentityFacts`; engine `EditorialP4BindingCharacterizationTest`. | Schema v18 không đủ để readback tuple P4. | Migration additive v18→v19 với `editorial_p4_bindings` và `editorial_p4_binding_inputs`; không backfill legacy rows. | Đã xử lý P4 |
| GAP-P4-003 | Không có active/latest pack toàn cục; lựa chọn phải explicit và exact. | Không: management page read-only, chưa có project-scoped selection. | `EditorialPackManagementPageInstrumentedTest`; `EditorialP4BindingInstrumentedTest.canonicalAndSyntheticPackBindSideBySideAndResumeExactly`. | Chưa có selection policy nối vào project creation. | `EditorialPackSelectionPolicy` resolve exact `packId + version`, UI chỉ đưa candidate DATA_COMPATIBLE/VALID vào flow project mới. | Đã xử lý P4 |
| GAP-P4-004 | Pack 4.1.3/4.1.4 side-by-side, project cũ không auto-rebind. | Import/storage đã side-by-side; project binding chưa có. | `EditorialP4BindingInstrumentedTest.canonicalAndSyntheticPackBindSideBySideAndResumeExactly`; P1/P2 side-by-side tests. | Chưa có project-scoped frozen binding. | Mỗi project setup ghi binding riêng; import/re-import không sửa binding cũ. | Đã xử lý P4 |
| GAP-P4-005 | Resume sau activity recreation, database close/reopen, process death và app restart phải dùng exact identity. | Không có binding state để resume. | `EditorialPackManagementPageInstrumentedTest.detailRebuildAndActivityRecreationDoNotExposeMutationControls`; P4 close/reopen assertions; `EditorialP4ProcessDeathInstrumentedTest` A/B với host `am force-stop`. | Không có durable binding/resume owner trong CODE169. | Resume từ selector/project binding; không query latest, không auto-rebind. | Đã xử lý P4 |
| GAP-P4-006 | Source/pack/profile/evaluation/parent drift phải stale/fail-closed. | Không: legacy project path không có chain tuple để so sánh. | `EditorialP4BindingInstrumentedTest.sourceAndPackDriftAreStaleAndProjectCannotCreateExecutionChapter`; `EditorialP4ProcessDeathInstrumentedTest.b_resumeExactBindingAfterHostProcessStop`. | Chưa có stale decision trên P4 chain. | `EditorialP4BindingTransactionService.verifyResume()` kiểm tra app-computed source identity, storage, profile/evaluation và revision/scope/declaration chain. | Đã xử lý P4 |
| GAP-P4-007 | Setup persistence phải atomic, retry idempotent, collision fail-closed và không partial row. | Một phần: v17/v18 append-only owners có atomic primitives nhưng chưa có project+binding transaction. | `EditorialP4BindingInstrumentedTest.blockedPackCannotBeSelectedAndCollisionDoesNotCreatePartialBinding`; `persistenceFailureRollsBackEveryP4BindingRow`; existing lineage transaction tests. | Chưa có transaction bao trùm P4 setup. | Dùng một SQLite transaction qua existing DAOs và immutable P4 DAO; same selector same facts là `ALREADY_EXISTS`. | Đã xử lý P4 |
| GAP-P4-008 | Source mode/status phải explicit; Pronoun status không được suy diễn; Pair Context optional. | P3B có contract/validator nhưng CODE169 chưa pin vào project/run setup. | P3B source-status/preflight suite; P4 binding tuple stores source mode, glossary/pronoun/pair status and provenance; normal setup uses explicit `USER_CONFIRMED_NORMAL`. | Chưa có project-scoped persistence của source decision. | Pin decision fields in the immutable binding; reject invalid/ambiguous source facts before insert. | Đã xử lý P4 |
| GAP-P4-009 | P4 setup không được tự biến compatibility thành runnable/certified hoặc gọi provider. | Compatibility existed, but no project setup boundary. | All P4 device tests assert `executionAllowed=false`, `NOT_CERTIFIED`; repository rejects chapter creation for P4; full suite provider/API `0`. | Chưa có explicit execution lock on a project-bound tuple. | Hard-code P4 setup lock and preserve `NOT_CERTIFIED`; hand off execution to separately authorized P5. | Đã xử lý P4 |
| GAP-P4-010 | Blocked/invalid/missing-storage/unknown-capability candidates chỉ read-only, không selectable. | Import registry exposes states; no project selection policy. | `EditorialP4BindingInstrumentedTest.blockedPackCannotBeSelectedAndCollisionDoesNotCreatePartialBinding`; P2/P3B invalid/unknown-capability matrix. | Chưa có project-flow filter. | Policy filters storage marker, integrity, trusted profile, machine fingerprint, compatibility and required capabilities; management remains read-only. | Đã xử lý P4 |

## P4 kết luận

Các gap có failing characterization evidence đều có owner hiện tại phù hợp và
đã được xử lý bằng các owner hiện có hoặc additive P4 binding owner. Không có
gap P4 còn mở để mở execution. Các yêu cầu provider execution, certification,
L1/L2/L3 và project/run binding to a certified result vẫn là phạm vi P5/P6,
không được suy diễn từ trạng thái P4.

Giới hạn còn lại có chủ ý: UI P4 thu thập input setup text rõ ràng để tạo
metadata; nó không chứng nhận source và không mở nút Run/Start. Đây là giới
hạn pilot-setup, không phải tuyên bố 4.1.3 runnable.
