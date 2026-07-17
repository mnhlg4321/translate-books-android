# Báo cáo khôi phục lõi Translate Books v4.5

Ngày kiểm định: 2026-07-16  
Thiết bị: OnePlus CPH2691, Android 15  
Phiên bản: `4.5` (`versionCode 44`)

## Kết quả chính

- APK cuối build thành công, cài đè và cold-start thành công.
- Một lượt dịch thật qua OpenRouter / `openai/gpt-5.4-mini` hoàn tất `11/11` chunk, `0` chunk lỗi.
- Dịch vụ ghi nhận 11 request, 11 response và 11 lần validation; tổng thực tế `122,127` token, `$0.15012075`.
- Output chỉ được đánh dấu hoàn tất sau khi `writeTextVerified` ghi, đọc lại và kiểm tra hash nội dung.
- Sau force-stop/mở lại và sau reboot thiết bị, app phục hồi đúng `COMPLETED`, `11/11`, không hiện lại lỗi Fatal cũ.
- Quyền output TXT đơn đã qua preflight thật dù một quyền output folder cũ không còn hợp lệ; job đơn không bị chặn nhầm bởi folder.

## Nguyên nhân gốc và sửa chữa

1. **SAF chưa được xác minh đầy đủ.** Code cũ có thể coi quyền đã lưu là hợp lệ chỉ vì URI nằm trong danh sách persisted grants; không kiểm tra đủ cờ read/write và không thử mở/ghi thật. Folder cũng chỉ được parse ID. v4.5 kiểm tra đủ cờ, mở descriptor/stream, và tạo-xóa file probe trong tree.
2. **Start mới có thể tái sử dụng job cũ.** Ràng buộc duy nhất theo `(prepared_batch_id, ordinal)` khiến một lần Start mới nhận lại job cũ đã lỗi/đã dịch dở. v4.5 thêm `start_session_id`, chỉ chống trùng trong cùng một dispatch và luôn tạo job sạch cho lần Start khác.
3. **Trạng thái runtime bị rò từ bản trước.** Snapshot v4.4.1 có thể hiện `Fatal error`/số chunk cũ ngay khi v4.5 mở. Snapshot giờ gắn phiên bản app và job dùng state machine rõ ràng: `IDLE → VALIDATING → READY → RUNNING → PAUSED/COMPLETED/FAILED`.
4. **Tác vụ nặng chạy khi đổi tab.** Truy vấn SQLite, đọc identity URI, parse glossary/pronoun và scan output folder từng được kích hoạt trong luồng dựng tab. Các việc này đã chuyển sang worker hoặc chỉ chạy khi người dùng bấm Refresh; page và dữ liệu parse được cache.
5. **Màn Translate bị lẫn log kỹ thuật.** Activity log, fallback/thời gian phụ và sự kiện lock chi tiết đã rời khỏi màn chính; lỗi khởi tạo được ghi đầy đủ vào log chẩn đoán thay vì che bằng thông báo Fatal chung chung.

Không thể chỉ ra commit gây regression hoặc cung cấp commit hash: thư mục `.git` trong workspace rỗng và `git rev-parse` xác nhận đây không phải Git worktree. Không khởi tạo repository mới để tránh giả mạo lịch sử.

## Kiểm định tự động

| Cổng kiểm định | Kết quả |
|---|---:|
| Gradle build / APK / AndroidTest APK / lint | Thành công, 71 tác vụ |
| Unit test JVM | 91/91 đạt |
| Instrumentation trên thiết bị | 4/4 đạt |
| Lint errors | 0 |
| Lint warnings | 46 cảnh báo không chặn build |
| Fatal/ANR/SecurityException/SQLiteException trong log tiến trình sau cold start và reboot | 0 |

Instrumentation bao gồm: fake provider nhiều chunk theo đúng thứ tự, delivery-unknown không tự gửi lại, response bền vững qua mô phỏng chết tiến trình trước commit, và Start session mới không dùng lại output cũ.

## Kiểm định thiết bị thật

| Bài thử | Kết quả |
|---|---|
| Cài đè v4.5, giữ settings/credential | Đạt |
| SAF preflight cho input và output TXT đơn | Đạt |
| Dịch thật 11 chunk | Đạt: 11 completed, 0 failed |
| Ghi và đọc-kiểm-hash output trước khi complete | Đạt |
| Force-stop rồi cold-start | Đạt, phục hồi `COMPLETED 11/11` |
| Reboot thiết bị rồi cold-start | Đạt, `TotalTime 2012 ms`, phục hồi `COMPLETED 11/11` |
| Chuyển tab, vòng warm 40 lần | P50 9 ms, P90 17 ms, P95 19 ms, P99 20 ms; PSS +105 KB |
| Android frame classifier | 10/40 frame “janky” (25%); legacy jank 0/40 |

Không tuyên bố “không lag tuyệt đối”: frame percentile và bộ nhớ warm ổn định, nhưng bộ phân loại deadline mới của Android vẫn đánh dấu 25% frame trong kịch bản tap ADB. Đây là số liệu cần giữ làm baseline cho vòng tối ưu sau, không phải lý do tiếp tục thay UI trong bản khôi phục này.

## Chưa xác minh end-to-end

- Thu hồi quyền SAF giữa chừng rồi cấp lại bằng file picker.
- Batch nhiều TXT với output folder thật, vì thiết bị không còn grant folder hợp lệ và không tự ý mở picker/chọn thư mục thay người dùng.
- Lượt dịch thật có refinement; lượt đã chạy là translation-only để không phát sinh thêm chi phí API.
- Kill tiến trình giữa một request trả phí thật. Nhánh này được bao phủ bằng instrumentation delivery-unknown/response recovery, nhưng không cố tình làm gián đoạn job thật để tránh gửi lặp và tính phí kép.

## File chính đã thay đổi

- `app/build.gradle`
- `AppBuildInfo.java`, `FileUtil.java`, `MainActivity.java`, `TranslatorService.java`
- `TranslationRepository.java`, `TranslationJobState.java`
- `PreparationCoordinator.java`, `CostEstimator.java`
- `RuntimeStateSnapshot.java`, `RuntimeStateStore.java`
- `FilesPageFactory.java`, `JobsPageFactory.java`, `TranslatePageFactory.java`
- `GlossaryStore.java`, `PronounStore.java`
- `CoreRecovery45Test.java`, `Hotfix441Test.java`, `RuntimeStateSnapshotTest.java`, `Hotfix441InstrumentedTest.java`

## Artifact và hash

- APK: `app/build/outputs/apk/debug/TranslateBooks-v4.5-debug.apk`
- Kích thước: `2,012,416` byte
- SHA-256: `FB165E952447B1CF44618775D62BFD04376F7CF33BA2ED62593AFED769118188`
- Ảnh hoàn tất sau reboot: `build/qa/v45/v45-after-reboot.png`
- Ảnh hoàn tất của lượt dịch thật: `build/qa/v45/completed-app.png`
- Ảnh preflight SAF: `build/qa/v45/preflight2.png`
