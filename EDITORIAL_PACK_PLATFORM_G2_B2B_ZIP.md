# Editorial Pack Platform — G2-B2B-ZIP

Status: `IMPLEMENTED / REVIEW STOP`

This increment adds only runtime ZIP selection and a thin UI bridge to the
existing G2-B1 headless importer. It does not certify, activate, bind, execute,
run Golden Replay, import folders, or read files from the external `D:`
candidate location.

## Boundary and data flow

```text
Editorial tab
  -> EditorialPackImportPageFactory (instructions/progress/result wording)
  -> EditorialPackSafBridge (ACTION_OPEN_DOCUMENT, one read-only URI)
  -> MainActivity thin request/result dispatch
  -> EditorialPackImportCoordinator (worker executor, one-shot stream)
  -> EditorialPackImportService.importZip(InputStream)
  -> ZIP limits + path/symlink checks + staging snapshot
  -> G2-A integrity/compatibility engine
  -> v14 immutable storage + read-only registry
```

The UI never parses ZIP entries, reads prompt bytes, computes hashes, queries
SQLite or opens immutable storage. `EditorialPackImportService.importZip` is a
headless boundary that consumes the selected stream once, closes it, bounds
entries/file/total bytes and compression ratio, then delegates the existing
G2-B1 staging, integrity, compatibility, atomic move and registry transaction.
The optional `EditorialPackImportProgressListener` reports importer state
transitions without allowing a UI callback to affect import correctness.

The current installed app has no trusted runtime contract profile. The UI uses
an empty profile with the current engine version; therefore a pack cannot be
guessed compatible and will remain `STORED_BLOCKED` until a future trusted
contract/adapter registry supplies explicit facts. No SAFE4 capability is
marked implemented and `EditorialSafe4Pack.executionEnabled()` is unchanged.

## SAF contract

- `EditorialPackSafBridge.REQUEST_CODE` is dedicated to the ZIP picker.
- `ACTION_OPEN_DOCUMENT`, `CATEGORY_OPENABLE`, `EXTRA_ALLOW_MULTIPLE=false`.
- `application/zip` is preferred; the intent includes `application/octet-stream`
  as provider fallback and retries that MIME only if no ZIP provider exists.
- Only `FLAG_GRANT_READ_URI_PERMISSION` is set; no write or persistable URI
  permission is requested and the URI is never stored as a runtime source.
- `ClipData` is rejected so a provider cannot silently return multiple files.
- Cancel produces no importer call, staging directory, import row or registry
  mutation.

## UI states and wording

The coordinator emits `PICKER_OPEN`, `SNAPSHOTTING`, integrity checking,
compatibility checking and immutable storing progress. Terminal states are:

- `STORED_READY_FOR_CERTIFICATION`: `Đã nhập và lưu bất biến • chờ chứng nhận`,
  with pack identity, canonical hash, `DATA_COMPATIBLE` and
  `Pack chưa được chứng nhận và chưa thể chạy biên tập`.
- `STORED_BLOCKED`: `Pack đã được lưu nhưng đang bị khóa`, with compatibility
  class, blocked reason and missing capabilities when supplied by the engine.
- invalid stream/ZIP/manifest/security failures: `Không thể nhập pack` plus the
  typed importer error and concrete reason.

The result action only opens the refreshed read-only management detail for the
returned canonical hash. No pack-level mutation action is exposed.

## Security and recovery

ZIP directory entries, traversal/absolute/alternate-separator paths, duplicate
normalized paths, detectable Unix symlinks, entry-count overflow, file/total
size overflow, compression-ratio overflow, malformed/truncated streams,
strict UTF-8/BOM/manifest/hash/length/schema failures and unknown extra files
remain fail-closed through the G2-B1 service. No Java/Dex/native code is loaded
from ZIP. Atomic filesystem/SQLite recovery remains exclusively the G2-B1
orphan recovery protocol; the UI never deletes staging or storage.

## Verification evidence

- `:editorial-engine:test`: 19 tests passed.
- `:app:testDebugUnitTest`: 160 tests passed, 0 failures/errors/skips.
- `:app:compileDebugAndroidTestJavaWithJavac`: passed (connected device not
  available, so no instrumentation execution or visual QA is claimed).
- `:app:lintDebug`: passed with 0 errors and the existing warning baseline.
- `git diff --check`: passed.

Focused JVM coverage includes picker cancel, double-click serialization,
worker-thread execution, exact-hash result refresh, stream close on success and
failure, fail-closed wording and blocked compatibility classes. Instrumentation
sources cover SAF flags/single selection, Editorial-tab action placement,
cancel row invariance, ZIP valid/traversal/extra/truncated fixtures and the
existing v13→v14/legacy-row persistence regression.

## Explicitly blocked after G2-B2B-ZIP

Folder/tree import, certification, Golden Replay, activation, project binding,
L1/L2/L3, model/API calls, release flow, dynamic code loading and additional
implemented capabilities remain blocked. Canonical SAFE4 code86 hashes are not
modified. Candidate identities `DBE214...` and `3B2FCC...` are not read from
`D:` and cannot enter the registry without a user-selected ZIP and a valid
manifest.
