# Báo cáo kiểm định Translate Books v4.6

Ngày: 2026-07-16  
Thiết bị: OnePlus CPH2691, Android 15  
Phiên bản: `4.6` (`versionCode 45`)

## Phạm vi

v4.6 chỉ khôi phục thông tin thiết yếu trên màn Translate. Không viết lại core translation và không thay đổi state machine, SAF validation, `start_session_id`, verified output writing hoặc recovery lõi của v4.5.

## Kết quả trực tiếp trên thiết bị

### Prepared

- TXT đã chọn dùng prepared batch bền vững có sẵn; renderer không đọc/chunk file lần hai.
- Hiển thị đúng:
  - `11 chunks`
  - `Estimated tokens ≈ 59,456`
  - `Estimated cost ≈ $0.171`
- Không còn `exact chunks` hoặc `Ready to validate and start`.

### Running

Fixture fake-provider tạo job 3 chunk và hoàn thành chunk đầu tiên với provider usage giả lập.

- `1/3 chunks`
- `Elapsed: 1m 5s` tại snapshot
- `Est. remaining: 2m 0s`
- `Failed: 0`
- `Fallback: 0`
- `140 used`
- `$0.0100 current`

Force-stop rồi cold-start vẫn phục hồi đúng state và toàn bộ metric của fixture. Dashboard không có ticker định kỳ; phép đo 5 giây giữa hai progress event ghi nhận 0 frame redraw, nên không tạo tải nền chỉ để tăng đồng hồ.

### Completed

Fixture fake-provider hoàn tất đủ 3 chunk, mỗi response có usage và cost provider giả lập:

- `3/3 chunks`
- `Failed: 0`
- `Fallback: 0`
- `420 actual`
- `$0.030 actual`
- `Elapsed: 3m 15s`

Estimate `600 tokens / $0.05` của fixture đã bị thay bằng actual. Khi người dùng chọn TXT mới lúc service không chạy, snapshot hiển thị cũ được xóa nhưng checkpoint/job DB vẫn giữ nguyên; Start mới cũng reset metric trước khi validation. Force-stop/cold-start và activity recreation do xoay màn hình vẫn phục hồi actual token/cost từ runtime snapshot v4.6 của session hiện tại.

Không có request API thật và không phát sinh chi phí API.

## Regression và build

| Kiểm định | Kết quả |
|---|---:|
| Clean Gradle build + APK + AndroidTest APK + lint | Thành công, 72 tác vụ |
| Unit test JVM | 95/95 đạt |
| Instrumentation Android 15 | 6/6 đạt |
| Regression lõi v4.5 trong instrumentation | 4/4 đạt |
| Fixture dashboard fake-provider v4.6 | 2/2 đạt |
| Lint errors | 0 |
| Lint warnings | 44 |
| Fatal/ANR/SecurityException/SQLiteException của tiến trình app | 0 |

Regression lõi bao gồm fresh Start session, delivery-unknown, response recovery trước commit và fake-provider ordered multi-chunk.

## Main-thread và tab switching

- `PreparationCoordinator` tiếp tục chunk và estimate trên worker; `PreparedBatch.estimate` được cache trong SQLite và tái sử dụng.
- Render Prepared chỉ format object estimate hiện có.
- Tab switch không gọi `silentPersistCurrentUi`, không serialize settings/glossary/pronoun, không scan folder và không query job SQLite.
- Page view đã tạo được giữ attach và chuyển visibility, không dựng lại ở mỗi lần đổi tab.
- Vòng warm 40 lần qua Translate/Jobs/Library/Settings: P50 `23 ms`, P90 `42 ms`, P99 `85 ms`; PSS giảm khoảng `23 MB`, không crash.
- Vòng 40 lần chỉ Translate/Jobs: P50 `20 ms`, P90 `53 ms`; PSS tăng khoảng `2.2 MB`.

Không tuyên bố tab “không jank tuyệt đối”: bộ phân loại frame trên màn hình 90 Hz vẫn đánh dấu nhiều frame trễ deadline. Mục tiêu v4.6 đã loại bỏ công việc dữ liệu nặng và redraw định kỳ khỏi đường này; render/layout của các page lớn vẫn là baseline cần theo dõi riêng nếu có vòng tối ưu hiệu năng sau.

## File thay đổi

- `app/build.gradle`
- `app/src/main/java/com/ml/tblandroidtxt/AppBuildInfo.java`
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslatePageFactory.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslatorService.java`
- `app/src/main/java/com/ml/tblandroidtxt/RuntimeStateSnapshot.java`
- `app/src/main/java/com/ml/tblandroidtxt/RuntimeStateStore.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslationDashboardFormatter.java`
- `app/src/test/java/com/ml/tblandroidtxt/Hotfix441Test.java`
- `app/src/test/java/com/ml/tblandroidtxt/RuntimeStateSnapshotTest.java`
- `app/src/test/java/com/ml/tblandroidtxt/V46DashboardTest.java`
- `app/src/androidTest/java/com/ml/tblandroidtxt/V46DashboardInstrumentedTest.java`
- `CHANGELOG.md`
- `QA_REPORT_v4_6.md`

## Artifact

- APK: `app/build/outputs/apk/debug/TranslateBooks-v4.6-debug.apk`
- Kích thước: `2,014,212` byte
- SHA-256: `89863921FCBD9281120B18D5F4F536D98C0B452E40E4E9094ABB6CB0E8E67A5A`
- Prepared: `build/qa/v46/prepared-final.png`
- Running: `build/qa/v46/running-final.png`
- Completed: `build/qa/v46/completed-final.png`
