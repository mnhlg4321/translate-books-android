# P3A — GAP-012 importer hardening report

Ngày chạy: `2026-09-03` (+07:00)

Trạng thái: `P2_COMPLETE / P3A_GAP012_COMPLETE / IMPORT_ACCEPTANCE_PASS / NOT_RUNNABLE / NOT_CERTIFIED`.

## Baseline và phạm vi

- Workspace: `D:\App Translate Books\App Translate Books-translation-profile`
- Branch: `feature/v4.18`
- Starting commit: `1090661140a4f970c4bd033e6314fc28f6b5ab04`
- Source baseline: `921af9256e1b1fe4ab9ac113affa98eec7a1e339`
- App baseline: `4.17-dev.1` / code169, device `15e84958` (`CPH2691`, API 35).
- Canonical ZIP SHA-256: `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`.
- Java control ZIP SHA-256: `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`.

P3A chỉ thay đổi một owner production: `app/src/main/java/com/ml/tblandroidtxt/EditorialPackImportService.java`. Ba AndroidTest class liên quan được mở rộng để chuyển characterization thành acceptance. Không thay authority, manifest, database schema, UI, build metadata trong source, trusted profile, execution/certification, provider/API hoặc public importer API.

## Bằng chứng trước khi sửa

P1 và P2 đã giữ test đỏ trước production change:

- P1 class: `7` test, `5` PASS và đúng `2` GAP-012 failure.
- P2 targeted class: `3` test, nhưng canonical assertion khi đó chủ động ghi nhận known failure; Java control và negative matrix pass.
- Existing focused importer class: `13/13` PASS.
- Full suite sau P2.8: `103` test, `101` PASS và đúng `2` GAP-012 failure.

Instrumentation cục bộ không lưu authority text đã xác định ranh giới tiêu thụ:

- Canonical source ZIP: `23,638` bytes; EOCD bắt đầu tại offset `23,616`.
- Khi `ZipInputStream.getNextEntry()` trả `null`, underlying counting stream mới tiêu thụ `23,487` bytes.
- EOCD có trong source nhưng chưa nằm trong tail của `ZipStructureProbe`.
- Không có lỗi manifest, hash, path hoặc entry-count; kết quả sai là `STAGING/TRUNCATED_STREAM`.

Điều này chứng minh canonical ZIP hợp lệ bị false-block bởi thời điểm probe, không phải do canonical pack sai định dạng.

## Sửa tối thiểu

Sau khi hoàn tất vòng lặp local entries, importer gọi `ZipStructureProbe.drainToEnd()` rồi mới kiểm tra `hasCompleteEndOfCentralDirectory()`.

- Drain dùng đúng stream hiện tại, không reopen SAF URI.
- Chỉ dùng buffer cố định `8192` bytes và bỏ dữ liệu sau khi probe đã giữ bounded tail `65557` bytes; không snapshot ZIP lần hai.
- Không thay đổi limits: `MAX_ENTRY_COUNT=4`, `MAX_FILE_BYTES=4 MiB`, `MAX_TOTAL_BYTES=12 MiB`, compression ratio `100`.
- Không commit pack trước khi transport validation hoàn tất.
- `IOException` trong drain đi qua catch hiện tại và vẫn trả `TRUNCATED_STREAM`.
- EOCD/central-directory validation vẫn bắt buộc; empty, malformed và truncated ZIP tiếp tục fail-closed.
- Không hard-code theo filename, ZIP SHA, writer hoặc version.

## Acceptance matrix

| Trường hợp | Kết quả sau sửa | Bằng chứng |
|---|---|---|
| Canonical 4.1.3 import | `STORED_READY_FOR_CERTIFICATION`, `NONE`, `DATA_COMPATIBLE`, ready | P1 `7/7`, P2 `3/3` |
| Java control import | Pass, exact immutable readback | P2 `3/3` |
| Canonical re-import | Idempotent, không duplicate row/identity | P2 `3/3` |
| Canonical rồi Java control | Cùng logical identity, không collision row sai | P2 `3/3` |
| Synthetic 4.1.4 | Side-by-side; không overwrite, auto-activate hoặc auto-rebind | P1 `7/7` |
| Unknown required capability | `ENGINE_UPGRADE_REQUIRED`, không crash/downgrade/execute | P1/P2 regression |
| ZIP truncated/malformed | Typed `TRUNCATED_STREAM`/integrity/path error; không marker, row hoặc partial storage | Focused `13/13` và P2 negative matrix |
| IOException giữa lúc drain | `TRUNCATED_STREAM`; staging/registry/immutable state được dọn fail-closed | Focused `13/13` |

Mọi positive readback giữ nguyên bytes của `project.txt`, `prompt.txt` và `workflow.txt`; không có execution/certification record.

## Lệnh và kết quả

Production APK validation được tạo bằng script bắt buộc, chỉ để chạy code đã sửa trên device:

```powershell
.\scripts\build-and-save.ps1 -Series '4.17-dev' -Notes 'P3A GAP-012 importer hardening device validation' -JavaHome 'C:\Program Files\Android\Android Studio\jbr' -MinimumVersionCode 169 -Install
```

Kết quả là artifact tạm kiểm thử `4.17-dev.2`/code170, APK SHA-256 `6F89A7A2DBD2DBC5A93D5CBD7C17D44E2C2FB928774BFEA3729EF54C44A42D52`, được lưu tại `artifacts/builds/v4.17-dev.2/build-20260903-183325` và mirror `backup/builds/v4.17-dev.2/build-20260903-183325`. Không sửa `app/build.gradle`, versionName, versionCode hay build metadata nguồn; sau kiểm thử device đã được khôi phục về APK immutable code169.

AndroidTest APK được build/cài riêng:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon
adb -s 15e84958 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
```

Test APK cuối: `1,061,212` bytes, SHA-256 `4FF6FDFEA4D5AEC9D628A5E267281756FFF2D02F28B02854028F677340F5479F`.

Các lệnh instrumentation dùng `com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner`:

```powershell
adb -s 15e84958 shell am instrument -w -r -e class com.ml.tblandroidtxt.EditorialPackImportServiceInstrumentedTest com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner
adb -s 15e84958 shell am instrument -w -r -e class com.ml.tblandroidtxt.EditorialP1PackImportCharacterizationInstrumentedTest com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner
adb -s 15e84958 shell am instrument -w -r -e class com.ml.tblandroidtxt.EditorialP2ReferencePackImportInstrumentedTest com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner
adb -s 15e84958 shell am instrument -w -r com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner
```

| Nhóm | Kết quả |
|---|---|
| Focused importer | `13/13 PASS` |
| P1 characterization | `7/7 PASS` |
| P2 reference-pack acceptance | `3/3 PASS` |
| Full instrumented suite | `103/103 PASS`; real API test skipped bởi opt-in assumption |
| `:editorial-engine:test --no-daemon` | `125/125 PASS` |
| `:app:testDebugUnitTest --no-daemon` | `210/210 PASS` |
| External `TESTS/test_full_release.ps1` | `PASS=306 FAIL=0 OLD_WORDS=5308 NEW_WORDS=7050 EXACT_RETAINED=223/311` |
| Provider/API calls | `0`; real API test skipped |

Tổng instrumented test không đổi: trước và sau sửa đều `103`; các test hiện có được mở rộng assertion/fixture, không xóa hai regression GAP-012.

## Guardrails và handoff

Negative security matrix vẫn pass: entry count/size/total size/compression ratio, missing/extra/duplicate/path traversal/nested/symlink, hash/length/manifest, truncated central directory/EOCD và drain IOException đều fail-closed; không có immutable marker, valid registry row, execution hoặc certification state cho pack lỗi.

Final guard trước commit: `git diff --check` PASS; diff dưới `app/src/main` chỉ có `EditorialPackImportService.java`; không có diff trong `editorial-engine/src/main`, database schema, UI hoặc build metadata. Canonical/control ZIP được re-hash đúng các giá trị baseline ở trên; device `15e84958` báo lại `versionCode=169`, `versionName=4.17-dev.1`.

`P2B_IMPORT_ACCEPTANCE_PASS` được ghi nhận cùng P2A. Việc importer hoạt động không chứng minh runtime contract đã đủ; trạng thái vẫn `NOT_RUNNABLE / NOT_CERTIFIED`.

Handoff duy nhất tiếp theo là `P3B — trusted runtime contract/profile`, xử lý từng gap còn lại bằng failing contract test. Không mở execution, auto-activate hoặc auto-rebind trong P3A.
