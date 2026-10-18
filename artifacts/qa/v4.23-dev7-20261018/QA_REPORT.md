# v4.23 — bản dev `4.23-dev.7` (code 282): thẻ "Job đang dở" không bị job cũ che

Ngày 2026-10-18, nhánh `feature/v4.23`. Provider giả trên emulator, USD 0 cho QA. Điện thoại `15e84958` đang chạy `4.23-dev.6`; dev.7 chưa cài.

## 1. Lỗi và nguyên nhân
Thử dev.6 trên điện thoại thật (RAW lỗi tự tạo, 4 chunk, cắt mạng sau chunk 0): Retry/"Tiếp tục job cũ" dịch nốt đúng job (F1, F3 đạt) nhưng thẻ "Job đang dở" hiện job cũ `0023-023 苦い後味.txt` (id 2, `NEEDS_REVIEW`, 1 chunk "không rõ đã gửi"). Job này bị cập nhật `updated_at` mỗi lần mở app nên luôn đứng đầu `getIncompleteJobs`, che job thử có thể tiếp tục; nút "Tiếp tục dịch" của nó lại bị tắt, nên trông như Resume không phản hồi.

## 2. Sửa
`ResumePrompt.pickCardJob`: thẻ hiện job mới nhất không có chunk "không rõ đã gửi"; chỉ khi không còn job nào khác mới hiện job không rõ đã gửi. `MainActivity.refreshPendingJobCard` lấy 20 job dở và chọn bằng hàm này. Hộp hỏi khi Start không đổi (đã lọc theo hash của tệp).

## 3. Bản dev
| Mục | Giá trị |
|---|---|
| Bản | `4.23-dev.7` / code 282, event `build-20261018-224505`, source `a5facba8` |
| APK | `D:\App Translate Books\artifacts\builds\v4.23-dev.7\build-20261018-224505\TranslateBooks-v4.23-dev.7-code282.apk`, SHA-256 `C088BDCED5C5A37338341EE6CF7F3750E6C53037F6056DBCDFB218CB3A8CA59C`, chứng chỉ `47F31389…EF3C155` |
| Source ZIP | `0C75025F…D7332` |
| APK test | `v423-dev7-test-20261018`, `FBFA4E57…83198` |

## 4. Hồi quy archive sạch (`a5facba8`)
Engine 742 / 1 SKIP; app debug, release, benchmark 570 mỗi biến thể / 5 SKIP; 0 FAIL; lint 0 lỗi / 65 cảnh báo; AndroidTest compile PASS. Test host mới trong `ResumePromptTest` (7): job cũ không rõ đã gửi đứng đầu không che job có thể tiếp tục; chỉ còn job không rõ đã gửi thì hiện nó; danh sách rỗng trả null. Editorial: `git diff 5d81061f -- editorial-engine 'app/.../Editorial*'` rỗng.

## 5. QA thiết bị (emulator-5554)
204 phát hiện / **188 PASS / 16 SKIP / 0 FAIL** / 0 INCOMPLETE (frozen 117/116/1, chunk 27/21/6, product 15/15/0, v4.22 13/6/7, v4.23 17/15/2, restart 15/15/0), giống dev.6. `V423ResumeInstrumentedTest` 7/7; test màn hình nay cấy một job cũ có chunk "không rõ đã gửi" mới cập nhật nhất và khẳng định thẻ vẫn hiện job có thể tiếp tục, không hiện job cũ.

## 6. Ảnh (`media/`, đã xem)
`38-pending-job-card` (thẻ hiện `011_v423x-screens.txt` — 2/19 chunk xong · 1 lỗi · 16 chưa dịch, dù job cũ đã cấy), `39-start-question`, `40-state-after-retry`.

## 7. Giới hạn
- Thẻ vẫn chỉ hiện một job; job dở cũ còn ở tab Jobs. Job cũ "không rõ đã gửi" không tự biến mất: cần xem ở tab Jobs.
- Chỉ trên emulator; dev.7 trên điện thoại là NOT_MEASURED tới khi owner duyệt cài và thử.
