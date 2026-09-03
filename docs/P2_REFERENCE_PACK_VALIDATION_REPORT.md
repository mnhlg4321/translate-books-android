# P2 — Reference pack validation report

Ngày chạy: `2026-09-03` (+07:00)

Trạng thái hiện tại: `P2_REFERENCE_PACK_FROZEN / RUNTIME_IMPORT_BLOCKED_BY_GAP-012 / NOT_RUNNABLE / NOT_CERTIFIED`.

P2 được tách thành hai phần do `GAP-012`:

- `P2A — Reference pack frozen`: canonical bytes, manifest, identity, transport controls và negative matrix.
- `P2B — Import acceptance`: chỉ mở sau P3 sửa `EditorialPackImportService`; khi đó mới xác nhận canonical import, immutable readback và side-by-side 4.1.3/4.1.4.

P2 chưa chứng nhận pack runnable, chưa kích hoạt Editorial execution và chưa ghi `P2_COMPLETE`.

## Entry gate

| Gate | Evidence |
|---|---|
| Workspace/branch/baseline | `D:\App Translate Books\App Translate Books-translation-profile`; `feature/v4.18`; source baseline `921af9256e1b1fe4ab9ac113affa98eec7a1e339`; app baseline `4.17-dev.1`/code169. |
| P0/P1 checkpoint | `aef7da1` — `chore(editorial): checkpoint P0 P1 characterization evidence`; checkpoint bao gồm các thay đổi P0/P1 đã ghi nhận, không có production source. |
| Authority qualification | `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE\TESTS\test_full_release.ps1`: `PASS=306 FAIL=0 OLD_WORDS=5308 NEW_WORDS=7050 EXACT_RETAINED=223/311`. |
| Git safety | `git diff --check` pass; không có diff trong `app/src/main` hoặc `editorial-engine/src/main`; không sửa database schema/UI/build metadata; không stage production file. |
| Schema-test hygiene | Bảy assertion current schema `17→18` đã sửa trong đúng ba AndroidTest class; seed/migration starting versions `13/14/15/16` giữ nguyên. |
| P2 split | `GAP-012` được phân loại thuộc importer transport; P2A và P2B được ghi riêng. |

## Locked authority bytes

| ZIP entry | Authority source | Bytes | SHA-256 |
|---|---|---:|---|
| `project.txt` | `PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt` | 9,485 | `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD` |
| `prompt.txt` | `PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | 8,852 | `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F` |
| `workflow.txt` | `WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | 34,917 | `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730` |

Generator từ chối authority có BOM, sai byte length hoặc sai SHA trước khi tạo output. Không newline normalization, không thêm/xóa newline cuối file và không sửa authority source.

## Frozen outputs

Generator: `scripts/create-editorial-p2-reference-pack.ps1`, với Java control writer test-only `scripts/EditorialP2JavaZipWriter.java`.

| Artifact | Path | Bytes/SHA-256 |
|---|---|---|
| Canonical manifest | `editorial-engine/src/test/resources/editorial-p2/editorial-pack.json` | 8,285 / `3e88503e312db8da351ca574820c98216ab6fd3fa233e35aedb0db379e50013a` |
| Canonical ZIP | `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-canonical.zip` | `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987` |
| Java data-descriptor control ZIP | `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-java-control.zip` | `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5` |
| Checksum receipt | `docs/P2_REFERENCE_PACK_CHECKSUM_RECEIPT.json` | `4EE030EBB4CA89C7C9C5DA9F4F99B594F52C7ADF3C54E0CE56CE70D198811E11` |

Cả hai ZIP có đúng bốn root entry theo đúng thứ tự:

```text
editorial-pack.json
project.txt
prompt.txt
workflow.txt
```

Không có folder entry, nested path, file phụ, APP/COMMON/TESTS, script, executable hoặc symlink. Canonical ZIP được tạo bằng .NET `ZipArchive` với timestamp `2026-09-03T00:00:00+07:00`, order cố định và `CompressionLevel.Optimal`; control ZIP dùng Java `ZipOutputStream` cùng payload/timestamp nhưng data descriptor.

## Identity versus transport

```text
Pack identity
  = canonical manifest semantics
  + exact SHA-256 của project/prompt/workflow

Transport artifact identity
  = SHA-256 của file ZIP cụ thể
```

Canonical identity bị khóa bởi:

- `packId = com.ml.tblandroidtxt.editorial.safe4.full`
- `version = 4.1.3`
- `manifestFormat = com.ml.tblandroidtxt.editorial-pack`
- `manifestVersion = 1`
- `contractVersion = safe4.full.three-pass.v1`
- `schemaVersion = safe4.full.receipt.v1`
- manifest SHA-256 `3e88503e...e50013a`
- canonical pack hash `497786e1...d05642d`
- ba authority hashes ở bảng trên

Canonical ZIP SHA-256 `b9c65d...58987` và Java control ZIP SHA-256 `44f994...609a5` khác nhau, nhưng manifest bytes, authority bytes, canonical pack hash và machine contract fingerprint giống nhau. Vì vậy compatibility không được dựa trên filename, display version, ZIP SHA hoặc ZIP writer.

## Manifest characterization

`EditorialP2ReferencePackTest.canonicalManifestFreezesAllPackManifestV1Declarations` kiểm tra host-side:

- contract/schema/minimum engine và `DATA_COMPATIBLE` declaration;
- required capabilities, bốn input roles `RAW/DRAFT/GLOSSARY/PRONOUN`;
- pronoun policy explicit và Pair Context optional;
- phase graph bảy phase, context allow-list, evidence schemas, gate definitions;
- 24 golden replay descriptors `G1–G24`;
- release artifact boundary có `EXECUTABLE_CODE`/`REMOTE_SCRIPT` forbidden;
- migration policy không automatic upgrade/rebind;
- canonical JSON, canonical pack hash và exact manifest hash.

Manifest chỉ là declaration/identity. Không có field execution/certification để biến pack thành runnable; P2 giữ nguyên điều này.

## Negative and compatibility evidence

Negative fixture đầy đủ được ghi tại `docs/P2_NEGATIVE_FIXTURE_MATRIX.md`. Host test pass cho manifest canonicality/negative codes; AndroidTest source tạo từng ZIP/entry mutation độc lập.

Compatibility outcomes đã được characterization từ P1 và giữ làm regression:

| Input | Expected |
|---|---|
| Contract/schema/capabilities được profile test hỗ trợ | `DATA_COMPATIBLE` |
| Adapter seam đã biết nhưng chưa cài | `ADAPTER_REQUIRED` |
| Required capability chưa hỗ trợ | `ENGINE_UPGRADE_REQUIRED` + capability thiếu; trusted resolver không crash/downgrade |
| Manifest/hash/path không hợp lệ | `INVALID`/typed integrity error; không store pack hợp lệ giả |
| Pack hợp lệ nhưng trusted profile non-executable | Lưu fail-closed, `STORED_BLOCKED`, không certify |

`GAP-012` không làm canonical pack bị đổi. Current importer vẫn là owner của failure: ZIP canonical hợp lệ bị `STAGING/TRUNCATED_STREAM` vì `ZipStructureProbe` kiểm tra EOCD trên stream trước khi central directory được drain; Java control đi qua importer.

## Tests and runtime status

| Command/test | Result |
|---|---|
| `scripts/create-editorial-p2-reference-pack.ps1` | PASS; authority preflight, canonical manifest, canonical ZIP, Java control ZIP, readback và package boundary. |
| Generator chạy hai thư mục tạm độc lập | PASS; manifest `3e88503e...e50013`, canonical ZIP `b9c65db...58987`, control ZIP `44f99423...609a5` tái tạo byte-identical. |
| Generator chạy lại trên output đã frozen | PASS fail-closed; từ chối overwrite. |
| `:editorial-engine:test --no-daemon` | PASS; `125/125`, gồm P2 manifest negatives. |
| `:app:testDebugUnitTest --no-daemon` | PASS; `210/210`; P2 không sửa app unit production behavior. |
| `:app:compileDebugAndroidTestJavaWithJavac --no-daemon` | PASS; gồm P2 Android characterization và P2.8 schema hygiene. |
| `:app:assembleDebugAndroidTest --no-daemon` | PASS; test-only APK `1,104,521` bytes, SHA-256 `8596B2B9CA89354C0045E33C50271946E792D5E441A236D9B8023F7A87F49998`; không tạo release APK/version mới. |
| Android P1 runtime trước P2 | Device `15e84958`/API 35: P1 class `7` tests = `5` pass + `2` GAP-012; full suite `100` = `91` pass + `7` schema-baseline + `2` GAP-012. |
| Android P2 targeted sau P2.8 | PASS; `EditorialP2ReferencePackImportInstrumentedTest`: `3/3`. Canonical ZIP giữ expected `STAGING/TRUNCATED_STREAM`; Java control import/readback/re-import pass; negative matrix pass fail-closed. |
| Android P1 regression sau P2.8 | `7` tests = `5` PASS + đúng `2` GAP-012 failures: `valid413ImportIsIdempotentAndImmutableReadbackIsExact` và `synthetic414IsSideBySideAndDoesNotRebindOrActivateCurrentProject`. |
| Android full instrumented suite sau P2.8 | `103` tests = `101` PASS + đúng `2` GAP-012 failures; real API test skipped by its explicit opt-in assumption, không gọi provider/API thật. Không còn bảy schema v17/v18 failures. |

P2 Android test acceptance đã được thực thi trên `15e84958`. Lần chạy đầu phát hiện test setup dùng chung registry DB giữa các negative fixture, gây `IDENTITY_COLLISION` ngoài nguyên nhân đang kiểm tra; setup đã được sửa trong AndroidTest để mỗi fixture dùng DB độc lập, sau đó targeted P2 đạt `3/3`. Đây là test-only correction, không phải production fix.

## P2A/P2B decision and P3 handoff

P2A đã đạt exit gate: reference bytes/identity host-validated, negative matrix pass, Android targeted P2 `3/3`, và full suite chỉ còn đúng hai failure GAP-012. Trạng thái đóng băng là `P2_REFERENCE_PACK_FROZEN / RUNTIME_IMPORT_BLOCKED_BY_GAP-012 / NOT_RUNNABLE / NOT_CERTIFIED`. Không đánh dấu `P2_COMPLETE` vì P2B vẫn chưa đạt.

P2B chưa bắt đầu. Chỉ sau P3 mới kiểm tra canonical import thành công, immutable readback byte-identical, idempotent re-import, side-by-side 4.1.3/4.1.4, không auto-activate/auto-rebind và security matrix không suy giảm.

Handoff production duy nhất sang P3:

```text
GAP-012 — sửa EditorialPackImportService để đọc/drain và
xác nhận central directory/EOCD của ZIP hợp lệ mà vẫn giữ
one-pass, bounded memory, limits và fail-closed behavior.
```

Không gộp trusted profile, UI, preflight, execution activation, database migration hoặc project/run binding vào GAP-012.
