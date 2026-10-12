# P1 — Phần tái sử dụng và phạm vi tối thiểu P2/P3

## Tái sử dụng nguyên trạng

Các thành phần sau đã có owner rõ ràng và không cần fork/rewrite cho P2:

1. `EditorialPackManifest` với Pack Manifest v1 canonical semantics và đúng ba typed data roles.
2. `EditorialPackIntegrityValidator` cho canonical manifest, pack hash, byte length, SHA-256, BOM/UTF-8, missing/extra.
3. `EditorialPackImportService` cho ZIP one-pass, giới hạn an toàn, staging snapshot, atomic immutable move và import idempotency.
4. `EditorialPackStorageLayout` cho private staging/immutable roots, marker, no-follow path và recovery cleanup.
5. `SqliteEditorialPackRegistry` cho read-only registry/readback integrity metadata.
6. `EditorialPackCompatibilityEvaluator`/`EditorialEngineProfileResolver` làm compatibility boundary, với trusted resolver là đường production duy nhất.
7. `EditorialPackImportCoordinator` và read-only pack detail UI làm thin import surface; không coi đây là execution/certification UI.

“Nguyên trạng” không có nghĩa 4.1.3 đã runnable. Nó chỉ có nghĩa boundary đã chứng minh được việc parse, bảo toàn byte, lưu side-by-side và fail-closed.

## P2 tối thiểu

- Đóng gói canonical/reference 4.1.3 từ fixture đã kiểm chứng, giữ bốn root ZIP entry và byte identity.
- Chỉ tái sử dụng manifest v1 fields đã chứng minh; không ép `EditorialPackManifest` trả `DATA_COMPATIBLE` bằng cách bỏ required capabilities.
- Chạy lại toàn bộ negative matrix trên canonical reference pack khi có Android test runtime.
- Giữ pack ở `STORED_BLOCKED` nếu trusted profile chưa executable; không activate/certify.

## P3 tối thiểu, chỉ khi test contract tương ứng được chốt

- Qualified trusted engine profile cho contract/schema/capabilities/phase graph đã có evidence; không làm profile giả chỉ để mở khóa.
- Typed run/preflight outcome cho `INPUT_REQUIRED`, `REPAIR_REQUIRED`, `RETRY_REQUIRED`, `CONTENT_BLOCKED`, `PRESERVE_DRAFT` và recovery mapping; mở rộng owner workflow hiện có trước khi tạo class mới.
- Phase projection/full-bundle preflight phân biệt asset hidden với asset missing; lưu `PronounStatus` explicit và Pair Context optional.
- Receipt/evidence/derived-gate validator cho output schema, lineage, gate definitions và release artifact declarations.
- Chuẩn hóa compatibility caller về trusted resolver; chỉ thay legacy evaluator nếu caller inventory chứng minh còn đường production active.

## Không nằm trong P2/P3 từ bằng chứng P1

- Không copy APP/COMMON/TESTS vào runtime hoặc payload import.
- Không tạo pack format/importer/marketplace/auto-update/auto-rebind/RSC/V23/IPC/workflow engine tổng quát.
- Không thêm database migration/UI redesign/provider call/build/APK.
- Không chạy real chapter hoặc tự tuyên bố 4.1.3 ready trước khi trusted profile và execution contract được chứng minh.

## Trạng thái production code

P1 đã không sửa production source, database schema, UI, build metadata hay provider wiring. Các đề xuất trên là phạm vi P2/P3; chưa phải thay đổi đã thực hiện.
