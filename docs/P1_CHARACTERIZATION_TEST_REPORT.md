# P1 — Characterization test report

Ngày chạy: `2026-09-03` (+07:00)

Trạng thái handoff: `P1_CHARACTERIZATION_COMPLETE_WITH_RUNTIME_GAPS / P2_REFERENCE_PACK_PENDING`.

Workspace: `D:\App Translate Books\App Translate Books-translation-profile`

Branch/HEAD baseline: `feature/v4.18` / `921af9256e1b1fe4ab9ac113affa98eec7a1e339`

APK/build metadata: không tạo APK mới; baseline vẫn `4.17-dev.1` / code169.

## Working-tree baseline được bảo toàn

Trước P1, working tree đã có các thay đổi P0/user-owned sau và được giữ nguyên, không reset, clean hoặc stage: `BUILD_STATE.md`, `GIT_WORKFLOW.md`, `README.md`, `TRANSLATION_PROFILE_RECOVERY_V4_17.md`, `WORKSPACE_SNAPSHOT.md`, `EDITORIAL_RECOVERY_V4_18.md`, `docs/EDITORIAL_PACK_V1_4_1_3_INTEGRATION.md`, `docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md` và `release_checklists/v4.18-editorial-v5-safe-4-1-3.md`. P1 chỉ bổ sung test fixture, test source, script và báo cáo/state update trong workspace continuation.

## Authority và fixture

Ba authority file đã được đọc từ `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE\CHATGPT` và hash/length khớp kế hoạch:

| ZIP entry | Authority | Bytes | SHA-256 |
|---|---|---:|---|
| `project.txt` | `PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt` | 9485 | `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD` |
| `prompt.txt` | `PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | 8852 | `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F` |
| `workflow.txt` | `WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | 34917 | `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730` |

Command:

```powershell
.\scripts\create-editorial-p1-fixture.ps1
```

Result:

- Test-only ZIP: `app/src/androidTest/assets/editorial-p1/v5-safe-4.1.3-full.zip`.
- Manifest mirror: `editorial-engine/src/test/resources/editorial-p1/editorial-pack.json`.
- ZIP có đúng 4 root entries: `editorial-pack.json`, `project.txt`, `prompt.txt`, `workflow.txt`; không nested path/file phụ.
- Manifest: 8285 bytes, SHA-256 `3e88503e312db8da351ca574820c98216ab6fd3fa233e35aedb0db379e50013a`.
- Canonical pack hash trong manifest: `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d`.
- ZIP SHA-256: `b9c65dbeb9d4c4ed46b67d5ec28ff6252cc2bdc4b63bc902904612987ec58987`.
- In-memory byte comparison: cả 3 authority payload `byte_identical=True`; BOM `False`.

## Lệnh và kết quả

| Lệnh | Kết quả |
|---|---|
| `.\gradlew.bat :editorial-engine:test --no-daemon` với `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` (Java 21) | PASS, `123 tests completed`, `BUILD SUCCESSFUL`. Bao gồm manifest, integrity, compatibility và P1 future/unknown-cap tests. |
| `.\gradlew.bat :app:testDebugUnitTest --no-daemon` với JDK 21 + Android SDK | PASS, `210 tests completed`, `BUILD SUCCESSFUL`. Bao gồm P1 false-block tests. |
| `.\gradlew.bat :app:compileDebugAndroidTestJavaWithJavac --no-daemon` với JDK 21 + Android SDK | PASS, `BUILD SUCCESSFUL`; chỉ compile AndroidTest. |
| `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon` | Runtime task đã tạo debug/test APK nhưng Gradle installer dừng trước test vì `INSTALL_FAILED_VERSION_DOWNGRADE` (generated app code48 < device target code169). Không downgrade/uninstall bằng Gradle. |
| `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon` | PASS, `BUILD SUCCESSFUL`; test-only APK tạo ra tại `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. |
| `adb install -r` baseline `TranslateBooks-v4.17-dev.1-code169.apk` + `adb install -r` test APK | PASS; baseline giữ `versionName=4.17-dev.1`, `versionCode=169`; test APK cài riêng. Baseline SHA-256 `3C3AAEF1...2B276D1`; test APK SHA-256 `1A35DE4F...CFBAC19`, 1,097,892 bytes. |
| `adb shell am instrument -w -r -e class com.ml.tblandroidtxt.EditorialP1PackImportCharacterizationInstrumentedTest ...` | Đã execute `7` test trên `CPH2691`/API 35; `5` PASS, `2` failure do GAP-012 (valid 4.1.3 và synthetic 4.1.4 bị `STAGING/TRUNCATED_STREAM`). |
| `adb shell am instrument -w -r com.ml.tblandroidtxt.test/...` | Đã execute full `100` instrumented tests; `91` PASS, `9` failure. `7` failure là baseline schema tests còn assert v17 trong app database `VER=18`; `2` failure là P1 GAP-012. Real API test giữ opt-in nên không gọi provider/API thật. |
| `D:\Ebooks\...\TESTS\test_full_release.ps1` | PASS, `PASS=306 FAIL=0 OLD_WORDS=5308 NEW_WORDS=7050 EXACT_RETAINED=223/311`; đây là static qualification ngoài app. |
| `adb devices -l` | PASS: allowed device `15e84958` (`CPH2691`, status `device`) is connected. |

## Test coverage đã tạo/đối chiếu

| Nhóm | Bằng chứng |
|---|---|
| Manifest/canonical declarations | `EditorialP1ManifestCharacterizationTest`: identity 4.1.3, contract/schema, minimum engine, exact file metadata, 4 input roles, pronoun/pair policy, phase graph, evidence/gate/release descriptors, G1–G24; đồng thời ghi nhận các execution fields còn thiếu. |
| Compatibility matrix | `EditorialP1CompatibilityCharacterizationTest`: supported → `DATA_COMPATIBLE`; known adapter seam → `ADAPTER_REQUIRED`; machine mismatch → `ENGINE_UPGRADE_REQUIRED`; dishonest declaration → `INVALID`; synthetic 4.1.4 content/identity-only change → `DATA_COMPATIBLE`; unknown required cap trusted → `ENGINE_UPGRADE_REQUIRED` + missing capability. |
| Import/integrity/storage | Existing `EditorialPackImportServiceInstrumentedTest` covers valid/repeat/one-pass, missing/extra/path/traversal, truncation, duplicate, symlink, wrong hash, declared length, storage collision, orphan staging and DB failure. New `EditorialP1PackImportCharacterizationInstrumentedTest` adds exact authority fixture, 4.1.4 side-by-side, trusted unknown cap, size/compression/truncated ZIP, immutable readback and recovery assertions. |
| False-block | `EditorialP1FalseBlockCharacterizationTest`: hidden phase asset, optional Pair Context, explicit Pronoun status, zero counters/edit, current generic status vocabulary, no model self-certification. |

## Android runtime result và trạng thái hoàn thành

Android instrumented tests đã execute trên device `15e84958` (`CPH2691`, API 35) bằng baseline APK immutable code169 và test APK riêng. P1 class chạy `7` test, `5` PASS và `2` failure có cùng nguyên nhân GAP-012: fixture ZIP hợp lệ host-validated bị importer trả `STAGING/TRUNCATED_STREAM`; synthetic 4.1.4 dùng cùng ZIP path cũng không thể chứng minh side-by-side vì bị chặn trước snapshot. Full suite chạy `100` test, `91` PASS và `9` failure; bảy failure schema v17/v18 là regression/test-baseline mismatch có sẵn ngoài P1, không được sửa trong P1.

Đây là bằng chứng runtime đủ để đóng P1 characterization với gap được ghi nhận, không phải bằng chứng 4.1.3 đã runnable/certified. GAP-012 được thêm vào gap matrix; không sửa production source, database schema, UI hoặc execution protocol.

Không có provider/API call thật (real API test không opt-in). Có build/install test-only APK theo quyền đã cấp; không đổi version/versionCode/build metadata trong source và không tạo v4.18 release APK. `git diff --check` phải được chạy ở cuối phiên trước khi bàn giao.

## Phân loại bảy schema-test failure v17/v18

Các failure dưới đây không phải gap 4.1.3. Chúng là test-baseline hygiene: `TranslationRepository` hiện mở database mới hoặc migrate database cũ lên `VER=18`, trong khi một số assertion còn giữ mốc v17.

| Test | Phân loại | Cách xử lý P2.8 |
|---|---|---|
| `EditorialLineagePersistenceInstrumentedTest.freshSchemaIsV17WithLineageTablesIndexesAndTriggers` | Assertion schema hiện tại bị stale | Đổi chỉ expected version hiện tại thành `18`; giữ nguyên table/index/trigger checks. |
| `EditorialLineagePersistenceInstrumentedTest.v15ToV17PreservesRowsAndCreatesNoIdentityBackfill` | Migration test, starting schema `15` đúng; assertion sau khi mở repository bị stale vì migration tiếp tục tới `18` | Giữ `old.setVersion(15)` và dữ liệu migration; đổi post-open version assertion thành `18`. |
| `EditorialPackCompatibilityEvaluationInstrumentedTest.freshSchemaIsV17WithImmutableV15EvaluationHistory` | Assertion schema hiện tại bị stale | Đổi chỉ expected version hiện tại thành `18`; giữ v15 table/evaluation checks. |
| `EditorialPackCompatibilityEvaluationInstrumentedTest.migrationV14ToV17PreservesRowsAndDoesNotBackfillProvenance` | Migration test, starting schema `14` đúng; post-open helper luôn hoàn tất tới `18` | Giữ `old.setVersion(14)`/legacy row assertions; đổi post-open version assertion thành `18`. |
| `EditorialPackCompatibilityEvaluationInstrumentedTest.migrationChainV13ToV14ToV15ToV17AndReopenIsIdempotent` | Migration test, starting schema `13` đúng; cả hai post-open assertions đang kỳ vọng intermediate v17 | Giữ starting version `13` và chain checks; đổi hai current-database assertions thành `18`. |
| `EditorialV17IdentityStoreInstrumentedTest.v16ToV17IsAdditiveAndDoesNotBackfillIdentityRows` | Migration test, starting schema `16` đúng; post-open helper tiếp tục migrate v18 | Giữ `old.setVersion(16)` và no-backfill assertions; đổi post-open version assertion thành `18`. |
| `EditorialV17IdentityStoreInstrumentedTest.freshV17SchemaHasFiveStoresAndNoIdentityBackfill` | Assertion schema hiện tại bị stale | Đổi chỉ expected version hiện tại thành `18`; giữ five-store/no-backfill checks. |

Không thay số `17` hàng loạt và không sửa production database schema. Sau P2.8, full suite phải còn riêng hai failure GAP-012 của P1.
