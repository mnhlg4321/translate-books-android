# v4.23 — bản dev `4.23-dev.10` (code 285): nhóm polish S, A, G, B+C+H, F, E, D1, D2, I

Ngày 2026-10-19, nhánh `feature/v4.23`. Yêu cầu: `docs/V423_POLISH_WORK_REQUEST.md`. Emulator-5554, provider giả, USD 0. Điện thoại `15e84958` không bị chạm (vẫn `4.23-dev.8`).

**Về số bản:** nhóm này lần đầu build thành `4.23-dev.9` (code 284, APK `BD34FA28…626A`, hồi quy sạch đạt). Lượt QA đầu phát hiện thiếu một khe test (đọc tệp `file://`) nên phải đổi mã sản phẩm một dòng và build lại thành `4.23-dev.10`. **dev.9 không dùng; bản giao là dev.10.**

## 1. Bản dev
| Mục | Giá trị |
|---|---|
| Bản | `4.23-dev.10` / code 285, event `build-20261019-212611`, source `b3a7a7ba` |
| APK | `D:\App Translate Books\artifacts\builds\v4.23-dev.10\build-20261019-212611\TranslateBooks-v4.23-dev.10-code285.apk`, SHA-256 `F2EFB6950E886B3C897775B667CF875B31BB56B0D15E40C27625E855D16D773E`, chứng chỉ `47F31389…EF3C155` |
| Source ZIP | `05E40048…3B10` |
| APK test cuối | `v423-dev10-test8-20261019`, `54E82B6F…7D38`, commit `1032812a` (chỉ đổi mã androidTest sau `b3a7a7ba`) |

## 2. Đã làm (theo thứ tự S → A → G → B+C+H → F → E → D1 → D2 → I)
- **S:** `JobStatus.classify()` (bảng một chỗ, không phân biệt hoa thường) và `sqlIn(...)`; mọi danh sách trạng thái SQL/Java dùng chung (`TranslationRepository` ×3, `LibraryRawStore` ×2, bộ lọc/biểu tượng ở `JobsPageFactory`, `JobPicker`).
- **A:** `InputIdentity.match(stored, text)` nhận hash của văn bản thô hoặc đã chuẩn hóa, dùng cả ở `continueExistingJob` và `resumeJobById`, ghi log nhánh khớp (`input_identity`). Sửa dữ liệu một lần `InputConflictRepair` (nền, cờ trong prefs): chỉ job `INPUT_CONFLICT` có `prepared_batch_id`, tệp còn đọc được và hash đã chuẩn hóa khớp thì về `paused`.
- **G:** `setNestedScrollingEnabled(true)` cho preview, Activity Log và Runtime log tail. Không cần helper dự phòng.
- **B+C+H:** nút **"Bỏ job này"** (xác nhận; trạng thái `dismissed`, không xóa dữ liệu); `recoverInterruptedChunks` chỉ chạm job đang chạy/tiếp tục được, không chạm job đã bỏ/xong/hủy và không chạm lại job đã `NEEDS_REVIEW`; ẩn job dở khi có job mới hơn cùng `input_hash` đã xong (không ghi gì); thẻ đọc ở nền; "và n job dở khác · Xem tất cả"; job có chunk không rõ đã gửi: "Xem trong Jobs" là nút chính, "Tiếp tục dịch" tắt kèm lý do; Current job = job đang chạy hoặc job dở mới nhất, không bao giờ job đã xong, số liệu đọc từ chính job; làm mới cache khi done/error/paused/cancelled; nhấn giữ một dòng: "Bỏ" / "Xóa" (cảnh báo mất lịch sử token/chi phí và liên kết DRAFT của Biên tập).
- **F:** chế độ Tổ hợp ẩn hai hàng Glossaries và Pronoun, thay bằng một dòng; đổi chế độ ẩn/hiện ngay; "Không sử dụng" chỉ còn một lần.
- **E (tối thiểu):** hàng "N mục" + "Chọn" cho RAW/Glossaries/Pronouns/Tổ hợp; thanh thông báo nhập dùng chung ("Đã thêm n · bỏ qua m (trùng/lỗi)" + "Xem chi tiết"); xóa Glossary và Pronoun trên thẻ đi qua hộp xác nhận và thanh hoàn tác 10 giây như RAW; tiêu đề lỗi không còn dấu ✅.
- **D1:** mỗi dòng job có "token vào (cached) · ra · USD …" từ `chunk_metrics`; thẻ "Token và chi phí" Hôm nay / 7 ngày (số chương xong, token, USD; lượt chưa rõ giá ghi riêng; retry tính đúng một lần mỗi lượt gọi).
- **D2:** ảnh chụp `translate_sessions/<id>.json` ghi thêm `resumedJobs`; tab Jobs gom theo lượt tổ hợp (tên, thời gian, "x/y chương xong · z lỗi · n chưa chạy", token, USD, thời lượng), bấm mở danh sách chương có "Mở kết quả", "tiếp tục từ lượt …"; job không có lượt: một dòng, ghi "(lượt cũ)". Chỉ đọc, không đổi schema.
- **I:** test mới và `V423ResumeInstrumentedTest` tự xóa job/tệp của mình; kiểm tra sau QA: 0 job `v423x-…` và 0 RAW thử còn lại (xem mục 6).
- Một dòng mã sản phẩm cho khe test: `FileUtil.validateReadable` chấp nhận URI `file://` chỉ khi `TranslateTestSeam` đang bật (bản chạy thường không có seam).

## 3. Test mới
Host (25): `JobStatusTest` 3, `InputIdentityTest` 5, `JobPickerTest` 6, `UsageTotalsTest` 5, `RunGroupsTest` 5, `TranslationConfigStateTest` +1. Thiết bị `V423PolishInstrumentedTest` (9): a CRLF resume cả hai đường (nút Resume và "Tiếp tục job cũ" từ Start), b sửa một lần chỉ khôi phục xung đột do xuống dòng và để nguyên tệp đã đổi thật, c recovery không chạm job đã bỏ/xong/hủy, d job xong mới hơn ẩn job dở (không ghi gì), e "Bỏ job này" ẩn hẳn kể cả sau recovery, f tab Jobs (tổng, thẻ lượt tổ hợp, Current không phải job xong), g chế độ Tổ hợp ẩn/hiện hàng, h vuốt tay trong Activity Log, i "N mục", thanh thông báo, xóa Pronoun có hoàn tác.
Test h có đối chứng: với nested scrolling **tắt** (hành vi cũ) cùng thao tác vuốt không làm log cuộn (`scrollY 12775 → 12775`); với nested **bật** log cuộn (`12775 → 12439`). Vậy bản sửa G có hiệu lực trên Android emulator; trên OPPO vẫn cần owner thử.

## 4. Hồi quy archive sạch (`b3a7a7ba`)
Engine 742 / 1 SKIP; app debug, release, benchmark 595 mỗi biến thể / 5 SKIP; 0 FAIL; lint 0 lỗi / 64 cảnh báo; AndroidTest compile PASS. Editorial: `git diff 5d81061f -- editorial-engine 'app/.../Editorial*'` rỗng.

## 5. QA thiết bị (emulator-5554)
214 phát hiện / **198 PASS / 16 SKIP / 0 FAIL** / 0 INCOMPLETE (frozen 117/116/1, chunk 27/21/6, product 15/15/0, v4.22 13/6/7, v4.23 27/25/2, restart 15/15/0). Nhóm v4.23 chạy lại bằng APK test cuối. Các lượt lỗi trước đó, đã sửa: ba test polish đỏ ở lượt đầu (xem `earlier-attempts/`): hai test dùng tệp `file://` bị chặn bởi khe test và dùng lại tên tệp nên bộ nhớ đệm kế hoạch trả kế hoạch cũ (đã dùng tên tệp riêng mỗi lần), một test vuốt tiêm sự kiện sai cửa sổ (nay gửi qua activity); test màn hình resume cũ không chờ thẻ vì thẻ nay đọc ở nền.
Dữ liệu prefs emulator trước/sau: chỉ `runtime_state.xml` đổi (trạng thái chạy); `settings`, `glossary_store`, `pronoun_store`, `translate_source`… giữ nguyên hash (`appearance.xml` chỉ khác dấu BOM của dòng đầu tệp liệt kê).

## 6. Dữ liệu thử còn lại trên emulator (mục I)
Test mới và `V423Resume` không để lại gì (0 job `v423x-…`, 0 RAW). Nhưng có **66 job** `fresh.txt` / `fake.txt` / `fake-provider-3-chunks.txt` trạng thái `running` do các lớp test cũ (nhóm product/frozen) tạo ra từ trước và không bao giờ dọn; chúng hiện ở "Recent jobs" của emulator. Không thuộc phạm vi đợt này; đề xuất thêm dọn vào các lớp đó.

## 7. Ảnh (`media/`, đã xem từng ảnh)
`43` thẻ job dở có "Bỏ job này" · `44a` tab Jobs: Token và chi phí + Current job (job dở, không phải job xong) · `44` thẻ lượt tổ hợp "Tổ hợp thử v423 340 — 1/3 chương xong · 1 lỗi · 1 chưa chạy · USD 0.0150" · `45` lượt mở ra: chương đã xong có "Mở kết quả", chương lỗi · `46` màn Dịch chế độ Tổ hợp gọn (một dòng thay hai hàng, Instruction YAML còn) · `47` Activity Log đã cuộn · `48` Library Pronouns: "2 mục", thanh thông báo "Đã thêm 2 · bỏ qua 1 (trùng/lỗi)" và thanh hoàn tác · `48a`, `48b` "N mục" ở RAW và Glossaries.
Ảnh thứ 7 trong yêu cầu ("preview đang cuộn") thay bằng ảnh `47` (Activity Log): khung preview chỉ hiện khi có job chạy; cùng cơ chế cuộn.

## 8. Giới hạn và việc chưa làm
- "Xem tất cả" ở thẻ job dở chỉ mở tab Jobs (chưa có bộ lọc "Đang dở"); ô tìm cho Glossaries/Pronouns chưa làm (yêu cầu ghi "để lần sau").
- Gom lượt tổ hợp cần ảnh chụp lượt chạy; ảnh cũ bị dọn thì job về dạng một dòng. `resumedJobs` chỉ ghi từ bản này trở đi.
- Job đã bỏ vẫn nằm trong "Recent jobs" (ghi "Đã bỏ") để giữ lịch sử.
- Dừng bằng nút Stop đặt `cancelled` nên job rời thẻ job dở (như đã nêu ở mục rủi ro của yêu cầu).
- Trên điện thoại thật mọi mục là NOT_MEASURED tới khi owner duyệt cài và thử G1–G6.

## 9. Phiếu thử cho owner (G1–G6)
| # | Owner làm | Đạt khi |
|---|---|---|
| G1 | Bấm "Bỏ job này" trên thẻ job dở (xác nhận "Bỏ job") | thẻ biến mất, không quay lại sau khi đóng và mở lại app |
| G2 | Mở tab Jobs | Current job không hiện job đã xong; thấy "Token và chi phí" Hôm nay/7 ngày; một lượt tổ hợp gom thành một thẻ, bấm mở ra danh sách chương |
| G3 | Màn Dịch, chọn nguồn Tổ hợp | không còn hai hàng Glossaries/Pronoun, chỉ còn một dòng "theo từng chương" |
| G4 | Vuốt bằng tay trong Activity Log (và preview khi đang dịch) | khung cuộn được bằng tay |
| G5 | Library: nhập/xóa/hoàn tác ở RAW, Glossaries, Pronouns | cùng kiểu thông báo ("Đã thêm n · bỏ qua m"), có "N mục" |
| G6 | Resume một tệp TXT tạo trên Windows (xuống dòng CRLF) | chạy tiếp được, không báo "Input file changed" |
