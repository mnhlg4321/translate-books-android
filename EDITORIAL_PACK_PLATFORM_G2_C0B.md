# Editorial Pack Platform — G2-C0B Bundled Trusted Engine Profile Registry

Status: `PASS / REVIEW STOP`

Phạm vi này chỉ đóng gói một trusted engine contract profile production trong
`:editorial-engine` và cung cấp registry read-only, immutable, fail-closed.
G2-C0C, runtime compatibility/importer wiring, selector, SQLite, certification,
activation, project binding và execution không được triển khai.

## Baseline và commit

- Branch đầu/cuối: `feature/v4.16`.
- HEAD đầu: `c31b9e29157c33b2d323f992e0be267c3607a709`.
- Commit implementation của nhóm: `fb2bd262dc996d20fbef051414727525410e9e1a` (`feat(editorial): bundle trusted engine profile registry`), `6c6fdb2c3ab15e972716d005b1588cee8b51fdec` (`build(editorial): copy trusted profile into APK assets`) và `285819c329c922d1256eb27bc13ee5ba75ec5fd1` (`fix(editorial): pin capability evidence provenance`). Đây là implementation baseline trước documentation handoff commit, theo quy ước snapshot không tự tham chiếu commit đang chứa chính snapshot.
- Baseline đã xác minh trước khi sửa: Gradle `9.3.0`, AGP `8.7.3`, JBR/JDK `21`, Java source/target `17`, compile/target SDK `35`, SQLite v14, latest accepted APK `4.16-dev.30`/code92.
- Ba file user-owned `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml` không bị sửa, stage, stash, commit, xóa hoặc ghi đè.

## File thay đổi

- `.gitattributes` — giữ profile JSON ở EOL LF để raw-resource anchor tái lập trên Windows.
- `app/build.gradle` — build-only copy/verify từ engine-owned resource vào generated APK asset; không tạo JSON source thứ hai và không thêm runtime call site.
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialEngineContractProfileRegistry.java` — API chỉ đọc.
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/BundledEditorialEngineContractProfileRegistry.java` — loader cố định và registry immutable.
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/TrustedEditorialEngineProfileCatalog.java` — compile-time allow-list/trust anchors độc lập.
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialEngineContractProfileRegistryException.java` — mã lỗi fail-closed, không lộ nội dung resource.
- `editorial-engine/src/main/resources/editorial/engine-profile/v1/profile.json` — production resource duy nhất.
- `editorial-engine/src/test/java/com/ml/tblandroidtxt/editorial/pack/BundledEditorialEngineContractProfileRegistryTest.java` — 17 test cases registry/resource boundary.

## Production profile và trust anchor

Profile resource path cố định:

`editorial/engine-profile/v1/profile.json`

- Profile ID: `com.ml.tblandroidtxt.editorial.engine.bootstrap`
- Profile version: `1.0.0`
- Engine version: `4.16-dev.30`
- Contract bounds: `null`/`null`; schema, input roles, phase graph, context allow-list, evidence-schema fingerprints, gate fingerprints, release fingerprints và adapters đều rỗng. Đây là no-executable-contract profile, không phải wildcard support.
- Full canonical profile hash: `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`
- Full machine contract fingerprint: `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`
- Full raw resource SHA-256: `deb0e89a4084a88c137c71ba2aa7a7170f84d9979395ef58866c73529ce601eb`
- `buildSourceCommit`: `8fbc8d1ed004c1524e001b7bbd0a2b321584331b`.
- Capability evidence: `pack.integrity.sha256.v1`, source commit `8fbc8d1ed004c1524e001b7bbd0a2b321584331b`, evidence fingerprint `9846036d707a0df8d68d5c106938052c34a103d288ae51871b05599c10a23d1f`, evidence class `production-build`.
- Evidence fingerprint method: SHA-256 của UTF-8 manifest `EDITORIAL_ENGINE_CAPABILITY_EVIDENCE_V1`, `capabilityId=pack.integrity.sha256.v1`, implementation Git blob `46621fa9d0d8bd2e7e332e101392b117e80ce346`, unit-test Git blob `1a4909e88470bbf9d14cb4a6701dcdb18511b1b4` và `BUILD_INFO.json` SHA-256 `962da813527c71d7090b4fb397e0852a60495569b0c3685542aff165105c9220`.
- Production capability được công nhận duy nhất: `pack.integrity.sha256.v1`.
- Chín capability còn thiếu và được khai báo explicit: `lineage.exact-parent.v1`, `ledger.exhaustive.safe4.v1`, `gate.derived.safe4.v1`, `context.pronoun-pair.safe4.v1`, `barrier.l1-raw-first.v1`, `diff.change-coverage.v1`, `qa.l3-two-adversarial.v1`, `release.safe4.v1`, `replay.safe4.g1-g10.v1`.

`TrustedEditorialEngineProfileCatalog` compile cùng engine và chứa resource
path, profile identity/version, raw resource SHA-256, canonical profile hash và
machine fingerprint. Loader không tin `canonicalProfileHash` tự khai báo trong
JSON: nó đọc đúng resource allow-list, chạy strict parser/validator C0A, tính
lại hash/fingerprint, đối chiếu các anchor độc lập và yêu cầu production
capability evidence catalog xác nhận capability. Không có classpath scan,
fallback hoặc chọn profile “latest”.

`app/build.gradle` chỉ đồng bộ đúng JSON này vào generated asset path
`editorial/engine-profile/v1/profile.json` và verify byte parity trước build.
Generated APK asset đã được tạo trong regression với cùng raw SHA-256
`deb0e89a4084a88c137c71ba2aa7a7170f84d9979395ef58866c73529ce601eb`; C0B
không gọi asset từ runtime, để dành wiring cho C0C.

Registry trả về collection/object immutable, ordering deterministic theo
identity/version/hash, và chỉ có `list`, `findByCanonicalHash` và
`findByIdentity`. Không có add/import/update/replace/delete/certify/activate,
default/project binding, SQLite/filesystem/SAF/network/ZIP hoặc code execution
API.

## Fail-closed evidence

Focused registry suite: `17/17` pass, `0` fail, `0` error, `0` skip. Các nhánh
đã kiểm tra gồm:

- resource thiếu;
- resource tamper/raw SHA mismatch;
- canonical hash anchor mismatch;
- machine fingerprint anchor mismatch;
- BOM/UTF-8-invalid boundary và unknown field/schema invalid;
- capability implemented nhưng thiếu evidence;
- duplicate identity/version và duplicate canonical hash trong catalog;
- catalog/resource identity không đồng nhất;
- resource ngoài allow-list không được yêu cầu/nạp;
- registry lookup hash/identity, deterministic ordering và collection immutability;
- test fixture namespace không xuất hiện trong production registry;
- public registry không có mutation/certify/activate/default/binding/execution API.

## Regression và build boundary

JDK preflight chuẩn hóa: `scripts/test-java-toolchain-preflight.ps1` pass
`4/4` — JDK 21 được chấp nhận; Java 8, thiếu `JAVA_HOME` và đường dẫn không hợp
lệ bị từ chối; không sửa environment hệ thống và không lộ đường dẫn cá nhân.

Tất cả Gradle regression dùng `gradlew.bat` với JBR 21:

```text
.\gradlew.bat --version
Gradle 9.3.0; Launcher JVM 21.0.10; Daemon JVM Android Studio JBR 21

.\gradlew.bat :editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon
PASS: editorial-engine 50/50; app JVM 161/161; instrumentation compilation PASS; 0 failures/errors/skips.

.\gradlew.bat :app:lintDebug --no-daemon
PASS: 0 errors, 53 existing warnings.

git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' diff --check
PASS.
```

Không chạy `scripts/build-and-save.ps1`, không build/cài APK và không tạo release
archive. Latest accepted APK vẫn là code92:

- APK SHA-256: `07BC98B22019832AFD37D0307E691957FBDC47D1C0E929949535D69AFB472801`
- Source ZIP SHA-256: `BD6BCDC832DC5C3D9AFFA3E1C597A12D090F560F6B8D6319D0D65F7FB22C1126`
- Artifact/backup parity của build code92 vẫn đạt; không có payload mới.

`EditorialSafe4Pack.executionEnabled()` vẫn `false`; canonical SAFE4 hashes,
database v14, importer/SAF flow, QA blocked row và runtime message
`UNSUPPORTED_CONTRACT_SCHEMA: No trusted contract descriptor is installed`
không bị thay đổi. Không có app/runtime importer call site mới.

## Worktree và quyết định

Sau documentation handoff/commit, worktree được xác minh chỉ còn ba thay đổi user-owned
`.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml`, tất cả unstaged;
không có thay đổi app, database, importer, SAFE4 hoặc candidate DBE214/3B2FCC.

Kết luận G2-C0B: `PASS / REVIEW STOP`.

Bước tiếp theo đề xuất: review và phê duyệt riêng G2-C0C. G2-C0C chưa được
triển khai trong nhóm này.
