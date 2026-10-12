# P2B — Import acceptance report

Ngày chạy: `2026-09-03` (+07:00)

Trạng thái: `P2_COMPLETE / P3A_GAP012_COMPLETE / IMPORT_ACCEPTANCE_PASS / NOT_RUNNABLE / NOT_CERTIFIED`.

## Identity và artifact

- Canonical ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-canonical.zip`, SHA-256 `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`.
- Java control ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-java-control.zip`, SHA-256 `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`.
- Canonical manifest SHA-256: `3e88503e312db8da351ca574820c98216ab6fd3fa233e35aedb0db379e50013a`.
- Canonical pack hash: `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d`.
- Cả hai ZIP có đúng bốn root entry, cùng manifest semantics, ba authority bytes và cùng canonical identity; chỉ transport SHA/writer khác nhau.

Authority payload được readback byte-identical:

| Entry | Bytes | SHA-256 |
|---|---:|---|
| `project.txt` | 9,485 | `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD` |
| `prompt.txt` | 8,852 | `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F` |
| `workflow.txt` | 34,917 | `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730` |

## P2B acceptance

`EditorialP2ReferencePackImportInstrumentedTest` đạt `3/3 PASS` trên device `15e84958` (`CPH2691`, API 35):

1. Canonical ZIP import thành công với `STORED_READY_FOR_CERTIFICATION`, error `NONE`, `DATA_COMPATIBLE`, marker ready; immutable readback giữ nguyên cả ba authority bytes.
2. Re-import canonical idempotent; import canonical rồi Java control dùng cùng logical identity và không tạo duplicate/identity row sai. Java control readback cũng byte-identical.
3. Negative matrix giữ fail-closed: invalid integrity/path/structure/transport không ghi pack row hợp lệ, immutable marker hoặc execution/certification state.

P1 regression class đạt `7/7 PASS`, bao gồm synthetic 4.1.4 side-by-side: identity mới được lưu cạnh 4.1.3, không overwrite, không auto-activate và không auto-rebind project/run hiện tại. Unknown required capability vẫn trả `ENGINE_UPGRADE_REQUIRED`, không crash hoặc silent downgrade.

## Runtime verification

Production APK patched được tạo bằng `scripts/build-and-save.ps1` và lưu immutable tại `artifacts/builds/v4.17-dev.2/build-20260903-183325` cùng backup mirror; SHA-256 APK `6F89A7A2DBD2DBC5A93D5CBD7C17D44E2C2FB928774BFEA3729EF54C44A42D52`. Đây là build validation `4.17-dev.2`/code170 để chạy production fix trên device; source build metadata không đổi và device đã khôi phục về baseline `4.17-dev.1`/code169.

| Kiểm tra | Kết quả |
|---|---|
| `EditorialPackImportServiceInstrumentedTest` | `13/13 PASS` |
| `EditorialP1PackImportCharacterizationInstrumentedTest` | `7/7 PASS` |
| `EditorialP2ReferencePackImportInstrumentedTest` | `3/3 PASS` |
| Full instrumented suite | `103/103 PASS`; real API skipped theo opt-in |
| Engine unit suite | `125/125 PASS` |
| App unit suite | `210/210 PASS` |
| Static qualification | `306 PASS / 0 FAIL` |
| Provider/API calls | `0` |

Tổng full instrumented suite giữ nguyên `103` test trước/sau P3A; không xóa hai test GAP-012 mà chuyển acceptance từ expected failure sang positive import/readback assertion.

## Quyết định

P2B đạt: canonical 4.1.3 import/readback/re-import pass; canonical và Java control cùng identity; synthetic 4.1.4 side-by-side; security negatives không suy giảm. `GAP-012` đã resolved trong importer và giữ regression coverage.

P2B không phải chứng nhận runnable. Trusted engine profile, runtime contract, execution, certification, auto-activation và auto-rebind vẫn chưa được triển khai; do đó phải giữ `NOT_RUNNABLE / NOT_CERTIFIED`.
