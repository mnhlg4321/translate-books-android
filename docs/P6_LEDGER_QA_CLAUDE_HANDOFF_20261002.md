# Bàn giao cho Claude điều phối sửa L1–L3

## Yêu cầu thực hiện

Tiếp tục v4.18 trên branch hiện có. Đọc `EDITORIAL_RECOVERY_V4_18.md` trước, sau đó `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`; xác minh branch/HEAD/status thật. Kế hoạch sửa và phản biện nằm tại **mục 11 canonical plan**, thứ tự R0→R7. Tài liệu này chỉ là chỉ dẫn bàn giao, không phải release plan thứ hai. Đọc audit `docs/P6_CHAPTER_001_INDEPENDENT_AUDIT_20261002.md` và acceptance hiện có trước thay đổi.

Mục tiêu: sửa toàn bộ lỗi đã xác minh về ledger, persistence, coverage, protected spans, final-read và kiểm ngữ nghĩa; chứng minh riêng năng lực L1/L2/L3, sau đó tái nghiệm thu sản phẩm. Không dừng ở việc tăng giới hạn finding, làm tests fake xanh hoặc chạy thêm hai chương.

## Baseline bàn giao

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- HEAD khi lập kế hoạch: `547c8a4d48f11856b29654e3d65749201d847efd`; không reset về hash này nếu HEAD đã tiến lên, phải kiểm diff và công việc đang có.
- Phase: P6, sửa chất lượng/evidence trước dùng G6 làm nghiệm thu; P7 chưa hoàn tất.
- Pilot/build lưu lại: `4.18-p6.2`/215 từ `4bc1aa27`; source có hai UX fix chưa build/cài. Không coi build bằng HEAD.
- Đã làm: read-only audit code, pinned pack, DB và file chương 001; Luna đối chiếu file và phản biện thiết kế test; đã viết mục 11 canonical plan. Chưa sửa code, chưa chạy test implementation mới hay provider/device trong phiên lập kế hoạch.
- Tests/build cũ: engine 255/255, app 312/312, lint/compile và emulator QA như snapshot; đây là historical evidence, không phải kết quả cho bản sửa sắp làm. Kiểm tra whitespace tài liệu PASS.
- Snapshot/audit/plan có thay đổi chưa commit. Các `.idea`, P5E docs và untracked evidence khác phải giữ nguyên, không stage hàng loạt.

## Sự thật phải giữ khi điều phối

1. Bản `D:/Ebooks/MERCEDES/VOL 5/6.FINAL/001_FINAL_QA_MERCEDES_VOL5.txt` là **owner sửa tay độc lập**, không phải output app. Nó là reference để tìm/chấm lỗi, không đưa vào runtime prompt và không bắt app chép đúng từng chữ.
2. Pilot FINAL `a7d5f99e…` chỉ sửa dòng 237. Bản manual `ebb091d6…` có thêm sáu khác biệt nội dung. RAW/profile mới quyết định khác biệt nào là lỗi; nhóm `踏破`/`攻略` là đối chứng nghĩa quan trọng.
3. Sửa kết luận cũ của Claude: `findingCount` chưa được gán nên số 0 không đo được phát hiện lỗi; receipt L3 có **4** probe. Không suy ra L1 không đọc hoặc chỉ hai phase làm việc từ edit counts.
4. L1 không sửa DRAFT là đúng contract. Sai ở ledger thiếu thông tin, REPORT_L1 bỏ entry khi lưu và không cung cấp đủ dữ liệu cho L2.
5. `EditorialChapterFinalCoordinator` đang truyền protected set rỗng; `EditorialL3Execution` ghi finalReadOrder cố định. Phải test đường runtime và final-read đúng hash sau QA edit, không chỉ sửa lời báo cáo.

## Cách giao việc cho sub-agent

Claude giữ contract/integration/acceptance. Mỗi subtask nêu file ownership, đầu vào, phạm vi, expected output và test. Giao Luna việc nhỏ như xác minh một nhóm fixture/anchor, round-trip ledger, rà probe hoặc kiểm bảng claim→evidence. Không giao cùng file cho hai writer, không để sub-agent tự đổi schema/route/budget hoặc chứng nhận chất lượng toàn chain.

Sau mỗi nhóm, reviewer đối chiếu kết quả bằng code/test/artifact thực. Tranh luận thiết kế phải kết thúc bằng một quyết định có case kiểm chứng, không sinh thêm tầng gate hoặc tài liệu thay cho implementation. Local failure sửa ngay cùng nhóm/branch, không biến thành yêu cầu owner xử lý.

## Các cửa nghiệm thu không được bỏ qua

- L1 report giữ nguyên toàn bộ ledger/proof qua serialize, DB, restart, L2; fixture >4 lỗi không mất dữ liệu. `findingCount` và counts khác có định nghĩa, không đếm candidate như lỗi.
- L2 giải quyết mọi L1 finding, cho phép bác false positive có proof; sửa đúng và không làm sai clean controls. Không được “PROCESSED” bằng lời khai mà thiếu outcome.
- L3 được kiểm riêng với lỗi cài trực tiếp vào VI_L2, kể cả lỗi không nằm trong L1. Sau edit phải đọc lại bản app vừa dựng trước commit.
- Coverage dựa RAW inventory, anchor mapping và trạng thái giải quyết; không ép L2/L3 có số candidate bằng nhau. Hỗ trợ thiếu/thừa câu, dòng bị gộp/tách, nhiều occurrence và ngữ cảnh xuyên chunk.
- Bản manual/gold không lọt vào prompt. Test fake chứng minh code, semantic evaluation với provider thật mới đo model. Giữ toàn bộ lượt thử, chấm từng loại lỗi, false positive, sửa đúng và regression.
- Báo cáo cũ vẫn xem được nhưng không đạt contract mới. Không hot-swap prompt/schema trong chain, không sửa artifact cũ để “đạt”.

## Phản biện để tránh sửa nửa chừng

- Tăng MAX_FINDINGS không sửa được report làm mất ledger.
- Một lần gieo lỗi end-to-end không đo được L3 nếu L2 đã sửa sạch.
- Số 0 do app đếm từ trạng thái model không chứng minh không có lỗi nghĩa.
- Quote đúng/anchor hợp lệ chưa đủ chứng minh lập luận đúng; cần chấm nội dung độc lập.
- Model mạnh hơn không thay cho contract/persistence đúng; chỉ thử như biến đo riêng sau khi sửa cấu trúc.
- Không yêu cầu edit cho đủ chỉ tiêu; bản sạch phải được giữ nguyên về nghĩa.
- Không dùng USD 0.05 cũ làm trần mặc định cho toàn bộ bộ thử mới có L1, chunk và verification.

## Quyền và bước bắt đầu

Yêu cầu hiện tại là lập kế hoạch để bàn giao, không phải chứng nhận đã sửa hay quyền live mới. Khi owner giao tiếp tục triển khai, thực hiện các nhóm offline đã nằm trong phạm vi được phép, không xin lại cho công việc thường lệ. Provider, cài/migrate pilot, project/binding mới và ngân sách bổ sung cần nằm trong quyền explicit còn hiệu lực; chuẩn bị xong code/tests/build và bảng call/cost cụ thể trước hỏi phần quyền còn thiếu. Không sử dụng lại quyền event cũ đã tiêu thụ.

**Next action duy nhất:** hoàn tất R0: bảng issue có chứng cứ và bộ fixture dựa RAW/profile, gồm lỗi thật, clean controls và trường hợp chưa đủ căn cứ; sau đó chốt contract R1 để các nhóm tiếp tục.
