# Yêu cầu làm việc + provenance — một event A4.3 RAW sau offline PASS

Trạng thái: READY_FOR_OWNER_DECISION / NOT_AUTHORIZED_BY_THIS_DOCUMENT / NOT_EXECUTED.
Baseline: 6e405c1ceed3056232fb6829c2fe3ed8c8708aa3, branch feature/v4.18-p5e-runner-repair-20260917, workspace D:\App Translate Books.

## Đang chờ owner quyết định điều gì?

Owner quyết định cho phép thử RAW thật, gồm bốn tác động: đọc package/DB/WAL trước và sau; so account trong bộ nhớ ngay trong live method; cấp runtime authorization mới và gửi tối đa một yêu cầu chứa RAW/GLOSSARY tới provider; ghi đúng attempt/authorization receipt/lifecycle/recovery hoặc kết quả COMMITTED được manifest cho phép. Có thể phát sinh chi phí provider theo budget đã chốt. Không phải chờ key mới, provenance mới hoặc sửa parser nữa.

Đủ điều kiện trình quyết định A4.3. Chưa đủ để chạy khi chưa được duyệt, và chưa đủ chuyển P6. Account MATCH, parity và offline QA đã đóng; không mở lại khi byte không đổi và không có counterexample mới.

## Provenance đã đối chiếu

| File | SHA-256 |
|---|---|
| docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md | 412790E2E55A8289FF170D3EE93B683553468ADE252EF5C565D833599E5F5EA3 |
| docs/P5E_RAW_AUTHORIZATION_COMMAND.txt | 51A71D47BBFC91DE19658FEEA120CF419ECD0F2FFD77562D74B83C73F9A98AB5 |
| scripts/p5e-raw-live-supervisor.ps1 | CB9C07312E025DA94D8DB4840B7600CE9A3613DB75CC0E0E4F28EB059E00901F |
| docs/P5E_A43_OFFLINE_QA_20260924.json | D124711219DFC1FD205CDB9205C394E2DA4CB3569827CA6C9D3B0A592EEC7050 |

Provenance probe 5C327F8265567734A744F5A43E770A76C5C52B51B6D4378E45CE2E4B1C0E5A3D; host-only PrepareEvent plan 9A87EAEB728994A281143DF068ABDBCB5D8F373B40E2EF56798A8DADCABFCAD6. Đã đối chiếu bytes của các file theo path trong QA; không chạy lại chúng. PrepareEvent QA cũ không được dùng làm event live.

Production APK 2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD/code207; selected test APK 058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8. Chi tiết source/BUILD_INFO/backup trong QA và manifest; giữ riêng raw source contract d51b7f3c với test bundle source9e5ffb78. Không dùng pin cũ 57EC99 hoặc nested helper cũ.

## Mẫu quyết định owner — chỉ có hiệu lực khi owner chủ động xác nhận

> Tôi đồng ý thực hiện đúng một event A4.3 L1_RAW_DISCOVERY theo manifest SHA-256 412790E2E55A8289FF170D3EE93B683553468ADE252EF5C565D833599E5F5EA3, command 51A71D47BBFC91DE19658FEEA120CF419ECD0F2FFD77562D74B83C73F9A98AB5 và helper CB9C07312E025DA94D8DB4840B7600CE9A3613DB75CC0E0E4F28EB059E00901F. Cho phép serial15e84958: collector read-only trước/sau và recovery cùng event; account memory-only trong live method; fresh runtime authorization; tối đa một RAW/GLOSSARY call và đúng DB writes allowlisted trong manifest. Giữ budget tối đa USD0.05, input100000/output4096/total104096 tokens, execution120000ms, authorization180000ms, host observation240000ms. Không fallback, schema repair, retry, RECONCILE, cleanup, restore hoặc redispatch; không build/install/thay credential hay settings. Dừng sau phân loại RAW; không tự mở P6.

Mẫu trên là đề xuất, không phải quyền đã nhận. Quyền account event cũ đã tiêu thụ; không đủ để gửi provider hoặc ghi DB. Sau khi owner xác nhận đúng phạm vi này, không hỏi lại từng substep đã duyệt.

## Yêu cầu thực hiện theo thứ tự

1. Đọc canonical → BUILD_STATE → snapshot; kiểm branch/HEAD/status. Giữ thay đổi người dùng, không tạo release/branch/checklist khác.
2. Kiểm hash bảng trên và dependency pins theo manifest/command. Nếu đúng, dùng QA đã lưu; không rerun account hoặc lặp toàn bộ QA. Nếu sai, dừng trước device, chỉ xác định file thay đổi; không sửa hash để bỏ gate.
3. Xác nhận có quyết định owner khớp mẫu/phạm vi. Nếu thiếu, chỉ yêu cầu quyết định này; không xin thêm thông tin key hoặc viết thêm packet.
4. Owner chuẩn bị serial15e84958 và đúng PowerShell chứa expected value Process. Nếu cửa sổ cũ đã đóng, owner nạp lại bằng loader đã kiểm pin trong cùng cửa sổ, không gửi key/digest vào chat. Agent không đọc value. Không thực hiện account-only event mới; live method tự kiểm lại account trong phạm vi duyệt.
5. Chuẩn bị launcher dạng file .ps1 bằng bản sao nguyên byte của command .txt trong một thư mục private mới; kiểm lại SHA-25651A71…8AB5 trước chạy. Không dùng Invoke-Expression/scriptblock vì command cần MyInvocation.MyCommand.Path cho self-hash. Không chuyển encoding hoặc sửa newline. Bản sao chỉ là dạng chạy của cùng command, không phải revision mới.
6. Trong chính owner PowerShell, đặt Process P5E_A43_COMMAND_SHA256 thành hash command đầy đủ ở bảng; đây là hash công khai của file, khác expected account digest. Gọi Windows PowerShell5.1 -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File với bản sao .ps1 đã kiểm hash đúng một lần. Process con kế thừa expected value; không chuyển secret vào argv, transcript hoặc file. Không chạy command từ cửa sổ agent thiếu expected rồi liên tục thử lại.
7. Để command tự tạo đúng một raw-live-* event mới và kiểm self/dependency/artifact/backup pins. Không tái dùng event account, event QA, event RAW đã có; không chạy một PrepareEvent khác thủ công.
8. PrepareEvent → Before collector: kiểm installed identity, package/certificate, DB/WAL/schema/integrity/FK, tuple và unused authorization. MATCH cũ không thay readback này. Bất kỳ STOP trước dispatch thì không gọi provider; tình trạng chưa claim/unused chỉ kết luận khi evidence chứng minh.
9. Chỉ sau các gate PASS, tạo issued/expires tại thời điểm dispatch và gọi selected live method tối đa một lần. Không refresh expiry, đổi authorization ID, tăng budget hoặc thay route để vượt STOP. RAW/GLOSSARY được phép egress; DRAFT/PRONOUN vẫn ẩn.
10. Cho command thực hiện After/recovery trong finally cùng event sau possible dispatch kể cả exception/nonzero/timeout. Không kill/relaunch command chỉ vì đang im lặng. Nếu USB mất, thiếu readback vẫn là NOT_PROVEN/UNKNOWN, không retry dispatch.
11. VerifyOutcome dựa trên durable DB và report/receipt bytes/hash/length/pair, đúng allowed row set, atomic claim, lifecycle, token/cost/time. Không yêu cầu DB hash trước/sau bằng nhau. Không ghi usage/cost thiếu thành0.
12. COMMITTED và verifier accepted mới đủ công nhận RAW predecessor. Instrumentation OK/exit0 đơn lẻ chưa đủ. Output/schema/semantic lỗi, cost unknown/overcap hoặc readback thiếu thì không accept RAW, không repair/RECONCILE/cleanup/restore.
13. Đóng event, lưu receipt/hashes redacted, giữ nguyên toàn bộ key và dữ liệu ngoài write scope. Báo actual launch/provider counts từ evidence; thiếu bằng chứng thì ghi UNKNOWN, không suy ra0. Không chia sẻ stdout/stderr, raw prompt/response hoặc settings.
14. Cập nhật canonical/state/snapshot/checklist một lần với kết quả terminal; snapshot đúng một Next action. RAW accepted vẫn cần đánh giá riêng tiêu chí P5 trước P6; không tự tick release05–09.

## Nếu lỗi xảy ra

| Nhóm lỗi | Hành động |
|---|---|
| File/hash/path/artifact sai | STOP trước live; không nới pin/reinstall |
| Device/account/route/fresh tuple/expiry sai | Không dispatch; giữ evidence, không chạy account riêng để thử |
| Provider/network/timeout | Giữ durable attempt/recovery, không fallback/retry; có thể đã phát sinh call/cost |
| Result không hợp lệ hoặc cost không rõ | RAW_NOT_ACCEPTED; không tự tăng budget hoặc sửa model output |
| Post-readback/verifier thiếu | ACCEPTANCE_NOT_PROVEN/UNKNOWN; không redispatch |
| RAW accepted | Đóng RAW; đánh giá P5 tiếp theo theo canonical, P6 chưa tự mở |

## Vì sao không cần thêm một vòng nghiên cứu?

Các lỗi layout/lifecycle, route mismatch và artifact/host pin đã có repair/evidence riêng. Vòng lặp trước đến từ fixture thiếu thực tế và nhiều chỉ dẫn cũ còn mang nhãn current. Bước hiện tại là một quyết định về tác động live; không còn pending owner về kỹ thuật account. Không tạo thêm plan hoặc QA khi các pin vẫn đúng. Nếu có lỗi mới, chỉ xử lý đúng counterexample và giữ ranh giới không redispatch.

QA tài liệu: hash packet/probe/PrepareEvent khớp; JSON hợp lệ; phạm vi tách account lịch sử khỏi live quyền mới; không ghi secret; quyết định gắn hash cuối; các đường STOP/success không mở P6. Audit tập trung tài liệu điều khiển và chuỗi evidence liên quan, không tuyên bố đọc từng dòng mọi lịch sử hoặc chứng minh provider readiness từ synthetic QA.