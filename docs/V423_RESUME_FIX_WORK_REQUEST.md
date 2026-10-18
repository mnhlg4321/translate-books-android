# v4.23 — Yêu cầu làm việc: Retry chunk lỗi xong thì dịch tiếp được, không phải dịch lại từ đầu

Nhánh: `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23` (HEAD lúc chẩn đoán: `e8cd9c37`). Ranh giới "không động vào Biên tập" như `docs/V423_TRANSLATE_LIBRARY_WORK_REQUEST.md` mục 2.

**Quyết định sản phẩm phục vụ:** khi một chunk dịch lỗi rồi được retry thành công, owner chạy tiếp được các chunk còn lại của đúng job đó. Không mất tiền dịch lại phần đã xong.

## 1. Owner báo (điện thoại `15e84958`, `4.23-dev.5`, 2026-10-18)

Một chunk dịch lỗi. Owner retry thì chunk đó thành công. Owner bấm Resume tiếp thì app "không phản hồi gì", nên phải dịch lại toàn bộ. Owner nói lỗi này đã gặp từ trước, không riêng v4.23. Chưa biết lượt đó là TXT thường hay tổ hợp, và chưa biết owner bấm nút Resume nào.

## 2. Chẩn đoán (đọc mã tại `e8cd9c37`; đối chiếu `v4.21.0`: ba đoạn mã dưới đây giống hệt)

**Phân loại:** lỗi trạng thái job và luồng giao diện (reconstruction/persistence + UI). Không phải lỗi provider, prompt hay ngữ nghĩa.

| # | Sự thật trong mã | Chỗ | Mức chứng cứ |
|---|---|---|---|
| C1 | Khi một chunk lỗi, `translateJob` đánh dấu chunk `error` rồi ném lỗi. Các chunk sau vẫn ở `pending`, job chuyển `error` | `TranslatorService` ~700–715, ~314 | đã chứng minh trong mã |
| C2 | "Retry" (`retryFailedChunksForJob`) **chỉ** chạy lại các chunk lỗi, **không** dịch các chunk `pending` | `retryFailedRows` ~614–650: chỉ chạy dòng `isFailedStatus` | đã chứng minh |
| C3 | Retry xong mà không còn chunk lỗi thì gọi `repo.finishJob(job.id)`, dù vẫn còn chunk `pending`. Bước kiểm toàn vẹn đặt trạng thái job thành `FAILED_INTEGRITY_CHECK` (`INCOMPLETE_CHUNK:n`) | ~581; `TranslationRepository.finishJobWithAudit` ~559; `OutputIntegrityAudit` | đã chứng minh |
| C4 | Sau đó service phát `broadcast("done", 100, "Retry failed chunks completed")`. Giao diện hiện **100% / COMPLETED**, trong khi còn chunk chưa dịch | ~584 | đã chứng minh |
| C5 | Ở màn Dịch, nút "↻ Resume" **chỉ hiện khi service đang chạy** và chỉ dùng để bỏ tạm dừng trong bộ nhớ (`ACTION_RESUME`). Khi service không chạy, nút này phát `paused` với câu "dùng Resume last checkpoint". Việc chạy tiếp checkpoint nằm ở chỗ khác: "Resume last checkpoint" (tab Files), "Resume last" hoặc "Resume" trên thẻ job (tab Jobs) | `MainActivity.updateActionButtons` ~2056–2060; `TranslatorService` ~130–133; `FilesPageFactory:70`; `JobsMainPageFactory:37,87` | đã chứng minh |
| C6 | Bấm Start ở màn Dịch với cùng input **luôn tạo job mới** (`createJobFromPrepared`), không hỏi về job dở có cùng input | `TranslatorService` ~383 | đã chứng minh |
| C7 | Nếu owner dùng đúng "Resume last" / "Resume" ở tab Jobs, `resumeJobById` tìm được job (`FAILED_INTEGRITY_CHECK` nằm trong danh sách `getLastIncompleteJob`) và `translateJob` bỏ qua các chunk đã xong. Về lý thuyết đường này dịch tiếp được | ~450–500, ~661–671 | suy luận từ mã; chưa chạy thử |

**Giải thích nhiều khả năng nhất cho điều owner thấy** (giả thuyết, cần tái hiện):
- Sau Retry, app báo hoàn tất 100% (C4). Ở màn Dịch không có lối nào để chạy tiếp checkpoint: nút Resume bị ẩn hoặc chỉ là bỏ tạm dừng (C5).
- Thao tác tự nhiên của owner là bấm Start, và Start tạo job mới nên dịch lại toàn bộ (C6).
- Nếu owner đã bấm "↻ Resume" khi service vừa chạy xong, app chỉ hiện một dòng banner hoặc log. Đó là "không phản hồi".

**Còn chưa biết:** owner bấm nút nào; lượt đó là TXT hay tổ hợp; trạng thái job lúc bấm; chunk có bị chia nhỏ do vượt context hay không (`supersedeAndInsertSubchunks` đổi chỉ số chunk; cần kiểm resume sau khi chia). Bản sửa dưới đây không phụ thuộc vào câu trả lời. Log "Export Log" của owner chỉ dùng để xác nhận.

## 3. Bản sửa nhỏ nhất theo nguyên nhân

**R1 — Retry xong thì dịch tiếp phần còn lại (sửa C2–C4).**
- Trong `retryFailedChunksForJob`, sau khi retry các chunk lỗi thành công, nếu job còn chunk `pending`: **chạy tiếp ngay trong cùng lượt** bằng `translateJob(job.id, …, chunks, …)`. Hàm này đã bỏ qua chunk xong. Vẫn áp trần chi phí và các luật dừng hiện có.
- Chỉ gọi `finishJob` khi **mọi** chunk đã xong.
- Không phát `done 100` khi còn chunk chưa xong. Nếu dừng giữa chừng (trần chi phí, hủy, lỗi), job ở `paused` hoặc `error` với thông báo rõ "còn m chunk chưa dịch".
- `ACTION_RETRY_CHUNK` (retry một chunk) áp cùng quy tắc: không đánh dấu hoàn tất khi còn `pending`.
- Owner có thể chọn **tùy chọn B** thay cho chạy tiếp tự động: Retry chỉ sửa chunk lỗi, rồi để job ở `paused` và hiện nút "Tiếp tục dịch". Mặc định đề xuất là chạy tiếp tự động, vì lượt dịch ban đầu vốn sẽ dịch các chunk đó.

**R2 — Trạng thái đúng nghĩa.**
- Job chỉ thiếu chunk `pending` (lỗi kiểm toàn vẹn chỉ có `INCOMPLETE_CHUNK`) thì không gắn `FAILED_INTEGRITY_CHECK` mà để `paused`, vẫn chạy tiếp được.
- `FAILED_INTEGRITY_CHECK` dành cho lỗi thật: trùng hoặc thiếu chỉ số, hash nguồn sai, output trống.
- Thẻ Jobs và chip trạng thái hiện "Còn m chunk chưa dịch".

**R3 — Màn Dịch luôn có lối chạy tiếp (sửa C5–C6).**
- Khi service không chạy và có job dở (trạng thái `paused` / `error` / `NEEDS_REVIEW` / còn `pending`): màn Dịch hiện **thẻ "Job đang dở"** gồm tên tệp, "x/y chunk xong · z lỗi · m chưa dịch", và các nút:
  - **"Tiếp tục dịch"** (`ACTION_RESUME_JOB` với đúng job id);
  - "Retry chunk lỗi" (khi có);
  - "Xem trong Jobs".
- Bấm **Start** với input (hoặc chương trong tổ hợp) có job dở **cùng `input_hash`**: hỏi "Đã có job dở cho tệp này (x/y chunk). Tiếp tục job cũ / Dịch lại từ đầu / Hủy". Mặc định là Tiếp tục.
  - Với tổ hợp: áp cho từng chương; các chương có job dở được tiếp tục, không tạo job mới.
- Nút "↻ Resume" khi service không chạy: không phát `paused` (vì làm UI tưởng đang chạy). Thay vào đó mở thẻ "Job đang dở", hoặc toast "Không có job đang tạm dừng".

**R4 — Không còn thao tác im lặng.**
- Mọi nhánh không làm gì của service (`job đã hoàn tất`, `không có checkpoint`, `không có chunk lỗi`, `ACTION_RESUME` khi không chạy, `NEEDS_REVIEW` do không rõ đã gửi) phải hiện thông báo thấy được: toast + banner ở màn đang mở, kể cả khi owner đang ở tab Jobs hoặc Library.
- Ghi log có cấu trúc `resume_decision`: `jobId`, `action`, `branch`, số chunk `done/error/pending/unknown`. Không ghi key hay nội dung sách.

**Rà cả họ lỗi:**
- Tìm mọi chỗ gọi `finishJob` (hiện có `TranslatorService:491`, `:581`) và mọi chỗ phát `done` khi job còn chunk chưa xong.
- Rà nhánh chia chunk do vượt context: resume và retry sau khi chunk đã bị thay bằng các chunk con phải dùng đúng chỉ số. `findRow(rows, i)` theo chỉ số; test trường hợp này.

## 4. Test (tái hiện offline, provider giả qua `TranslateTestSeam`, USD 0)

**Host / service:**
- **T1** (tái hiện): job 5 chunk; provider giả ném lỗi xác thực ở chunk 2 → job `error`, chunk 3–5 `pending`. Gọi Retry.
  - Mong đợi (R1 mặc định): chunk 2–5 được dịch, mỗi chunk đúng một lần; chunk 1 không gọi lại; job `COMPLETED`; không có `done 100` trước khi xong.
  - Trên mã hiện tại test này phải **FAIL** (job `FAILED_INTEGRITY_CHECK`, chunk 3–5 không dịch, đã phát `done`). Giữ log FAIL làm bằng chứng tái hiện.
- **T2:** như T1 nhưng chạm trần chi phí ở chunk 4 → job `paused`, còn 4–5; "Tiếp tục dịch" chạy 4–5.
- **T3:** Start với cùng input khi có job dở → hộp hỏi. Chọn "Tiếp tục": không tạo job mới, chỉ dịch chunk còn lại. Chọn "Dịch lại": tạo job mới (hành vi cũ).
- **T4:** tổ hợp 3 chương, chương 2 dừng giữa chừng → Start lại tổ hợp: chương 1 không dịch lại, chương 2 tiếp tục đúng job, chương 3 chạy mới.
- **T5:** chunk vượt context bị chia thành chunk con rồi lỗi → Retry, rồi resume: đủ chunk, đúng thứ tự, bước kiểm toàn vẹn đạt.
- **T6:** `ACTION_RESUME` khi service không chạy → UI không chuyển sang trạng thái "đang chạy"; có thông báo.

**Thiết bị** (`emulator-5554`):
- T1 và T3 chạy đầu-cuối qua UI. Force-stop giữa chunk 3 rồi mở lại: thẻ "Job đang dở" hiện đúng số liệu, "Tiếp tục dịch" chạy nốt.
- Ảnh: thẻ "Job đang dở"; hộp hỏi khi Start; trạng thái sau Retry.

## 5. Thứ tự, điểm dừng, quyền

1. Viết T1 trước và giữ log FAIL trên mã hiện tại.
2. R1 → R2 → R3 → R4, kèm test.
3. Hồi quy trên archive sạch.
4. Build `4.23-dev.6` (code > mọi code đã dùng).
5. QA emulator đủ nhóm + test mới + ảnh.
6. **Dừng** và hỏi owner duyệt cài lên điện thoại; trước khi cài sao lưu `run-as` từ `4.23-dev.5`.

**Quyền:** không gọi provider; không chạy test trên điện thoại; không sửa `Editorial*`; không tag/merge; push sau khi quét.

**Phiếu thử cho owner sau dev.6:**

| # | Owner làm | Đạt khi |
|---|---|---|
| F1 | Dịch một tệp dài; khi có chunk lỗi bấm Retry | sau Retry app dịch tiếp đến hết, không báo 100% sớm |
| F2 | Thoát app giữa chừng rồi mở lại màn Dịch | thấy thẻ "Job đang dở" với số chunk đúng; "Tiếp tục dịch" chạy nốt |
| F3 | Bấm Start lại cùng tệp khi còn job dở | app hỏi Tiếp tục / Dịch lại |

Chi phí do owner quyết: thử với 1 chương ngắn. Mục owner không thử ghi NOT_MEASURED.
