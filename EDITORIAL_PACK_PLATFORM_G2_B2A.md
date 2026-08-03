# Editorial Pack Platform — G2-B2A

Status: `IMPLEMENTED / REVIEW STOP`

Scope is limited to a read-only management surface over the existing persistent registry. This increment does not import, certify, activate, bind, execute or delete anything.

## Read-only boundary

The page calls only `EditorialPackRegistry.list()`, `findByHash()` and `findByIdentity()` through `EditorialPackListPresenter`. It never opens SQLite directly and never reads immutable storage. `SqliteEditorialPackRegistry` remains the sole adapter that turns registry rows/storage integrity into a decorated manifest record. The page has no mutation callback and `EditorialPackUiModel.mutationActions()` is permanently empty.

The registry now exposes persisted metadata without changing canonical manifest bytes: storage state/key, blocked reason, import/validation timestamps, engine version, integrity state/reason and latest compatibility snapshot (including machine-contract fingerprint and missing capabilities). Missing immutable storage is represented as an explicit integrity state instead of silently disappearing from the list.

## UI surface

The existing Editorial tab contains an `Editorial Packs` section with `Xem các pack đã lưu`. It opens `EditorialPackManagementPageFactory`, which renders a scrollable grouped list and read-only detail dialog. The list is explicitly labeled `persistent imported packs`; built-in SAFE4 is not seeded or shown as a persisted pack.

Cards show display name, pack ID, version, shortened canonical hash, compatibility wording, storage state, contract/schema, stored timestamp and `READ ONLY`. Details show full hash, identity, minimum engine, machine-contract fingerprint, required/missing capabilities, all three declared file roles/path/length/hash, integrity, latest compatibility, timestamps, engine version and blocked reason. Prompt/chapter/API-key content is never rendered.

Exact wording is centralized in `EditorialPackUiMapper`:

- `STORED_READY_FOR_CERTIFICATION`: `Đã lưu • chờ chứng nhận`;
- `STORED_BLOCKED`: `Đã lưu • bị khóa`;
- `DATA_COMPATIBLE`: `Tương thích contract • chưa chứng nhận`;
- `ADAPTER_REQUIRED`: `Cần adapter • không thể kích hoạt`;
- `ENGINE_UPGRADE_REQUIRED`: `Cần nâng engine/APK • không thể kích hoạt`;
- `INVALID`/`BLOCKED`: `Không hợp lệ`/`Bị khóa`.

No state is presented as runnable or “READY”. Sorting is locale-independent: pack ID ascending, numeric version descending within a pack ID, then canonical hash ascending. Hash remains the final identity; no latest/active concept exists.

## Verification

- Pure engine tests: 19/19 pass.
- Full app JVM tests: 155/155 pass, including 7 G2-B2A presenter/mapper tests.
- Android instrumentation source compilation: pass; page tests cover empty/list/error text, recreation, scrollable construction, prohibited action labels and unchanged pack row count.
- Lint: pass with the existing 53 warnings and 0 errors.
- Connected/visual UI QA: not run because `adb` is unavailable; no device pass is claimed.

## Explicit non-goals

Picker/import, `EditorialPackImportService` calls, certification, Golden Replay, activation, project binding, L1/L2/L3, model/API, release and capability changes remain blocked. Canonical SAFE4 code86 hashes remain unchanged. Candidate identities `DBE214...` and `3B2FCC...` are not read from D:, not imported and not displayed unless a future isolated test explicitly places a record in the registry.
