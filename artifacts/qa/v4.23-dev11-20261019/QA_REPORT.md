# v4.23 — bản dev `4.23-dev.11` (code 286): sửa đếm chương và Current job

Ngày 2026-10-19, nhánh `feature/v4.23`. Emulator-5554, provider giả, USD 0. Điện thoại `15e84958` đang chạy `4.23-dev.10`; dev.11 chưa cài (chờ owner xác nhận trong chat).

## 1. Lỗi và sửa
1. **"Hôm nay / 7 ngày: 0 chương xong" (thấy khi thử dev.10 trên máy thật).** `getUsageTotals` truyền mốc thời gian `since` qua `rawQuery(String[])`, nên SQLite bind nó là chữ. Phần token/USD so `created_at` với mốc đó và vẫn ra số, vì `created_at` là cột khai báo kiểu INTEGER: SQLite tự đổi tham số chữ sang số khi so với cột có kiểu INTEGER. Phần đếm chương so mốc với biểu thức `COALESCE(NULLIF(completed_at,0),updated_at)`, biểu thức không mang kiểu cột nên không có phép đổi kiểu: số so với chữ và luôn thua, kết quả luôn 0. Sửa: `CAST(? AS INTEGER)` ở cả hai truy vấn (`2fca9809`).
2. **Current job hiện job `NEEDS_REVIEW` cũ** (`0023-023`, chunk "không rõ đã gửi") dù có job khác tiếp tục được. `JobSummary` có thêm `unknownChunks` (truy vấn con đếm chunk `DELIVERY_UNKNOWN`); `JobPicker.current` ưu tiên job `unknownChunks == 0` giống `ResumePrompt.pickCardJob`, chỉ khi không còn job nào khác mới hiện job có request không rõ đã gửi. Một job `NEEDS_REVIEW` do lỗi vĩnh viễn nhưng không có chunk không rõ đã gửi vẫn được chọn bình thường.

## 2. Bản dev
| Mục | Giá trị |
|---|---|
| Bản | `4.23-dev.11` / code 286, event `build-20261019-222201`, source `344338c4` |
| APK | `D:\App Translate Books\artifacts\builds\v4.23-dev.11\build-20261019-222201\TranslateBooks-v4.23-dev.11-code286.apk`, SHA-256 `8ACA53D94572798E558C341D970B3C538A4CFBD670F08FE117ADABA605FF110A`, chứng chỉ `47F31389…EF3C155` |
| Source ZIP | `08E98D75…053D9` |
| APK test cuối | `v423-dev11-test2-20261019`, `60CA2969…0AA9B`, commit `87695bdf` (chỉ đổi mã androidTest sau `344338c4`) |

## 3. Test giữ lỗi không quay lại
- Host `JobPickerTest` (+3): Current ưu tiên job tiếp tục được hơn job `NEEDS_REVIEW` mới hơn có chunk không rõ đã gửi; chỉ còn job không rõ đã gửi thì hiện job mới nhất; job `NEEDS_REVIEW` không có chunk không rõ đã gửi vẫn chọn được.
- Thiết bị `V423PolishInstrumentedTest` (+2): `j_totalsCountChaptersFinishedToday` (job xong hôm nay tính theo `completed_at` hoặc chỉ `updated_at`, job đang dở không tính, cửa sổ ở tương lai đếm 0); `k_currentJobPrefersAJobThatCanContinue`. Test `f_…` khẳng định thẻ không còn ghi "Hôm nay: 0 chương xong" khi có job xong hôm nay.
- Ảnh `44a`: trên emulator thẻ ghi "Hôm nay: 1 chương xong" và Current job là job có thể tiếp tục.

## 4. Hồi quy archive sạch (`344338c4`)
Engine 742 / 1 SKIP; app debug, release, benchmark 598 mỗi biến thể / 5 SKIP; 0 FAIL; lint 0 lỗi / 64 cảnh báo; AndroidTest compile PASS. Editorial: `git diff 5d81061f -- editorial-engine 'app/.../Editorial*'` rỗng.

## 5. QA thiết bị (emulator-5554)
216 phát hiện / **200 PASS / 16 SKIP / 0 FAIL** / 0 INCOMPLETE (frozen 117/116/1, chunk 27/21/6, product 15/15/0, v4.22 13/6/7, v4.23 29/27/2, restart 15/15/0). `V423PolishInstrumentedTest` 11/11. Lượt chạy đầu có 1 FAIL (`k_…` nhìn nhầm các job `running` cũ do lớp test khác để lại trên emulator): đã sửa test chỉ xét hai job của nó (`earlier-polish-1-fail.txt`), mã sản phẩm không đổi. Dữ liệu prefs emulator trước/sau: chỉ `runtime_state.xml` đổi.

## 6. Giới hạn
- Chưa cài lên điện thoại; mọi hành vi trên máy thật là NOT_MEASURED cho tới khi owner xác nhận cài và thử.
- 66 job `running` cũ do các lớp test khác để lại trên emulator vẫn chưa được dọn.
- Hai tệp output thử trong thư mục của owner (`v423_g1_dismiss_translated.txt`, `v423_g6_crlf_translated.txt`) chưa xóa: chờ owner xác nhận trong chat.
