# Yêu cầu làm việc tiếp theo — thực thi đúng một event A4.3 đã được owner phê duyệt

## 1. Trạng thái đầu vào bắt buộc

`OWNER_DECISION_RECEIVED / SINGLE_EVENT_AUTHORIZED_NOT_YET_CONSUMED / NOT_DISPATCHED / A4_3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`

Owner đã phê duyệt nguyên văn câu trong
`docs/P5E_A43_FINAL_EXECUTABLE_OWNER_AUTHORIZATION_REQUEST_20260926.md`.
Quyết định này chỉ có hiệu lực cho đúng một event mới trên serial
`15e84958`, với các byte đã đóng băng sau:

| Thành phần | SHA-256 bắt buộc |
|---|---|
| Manifest | `23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053` |
| Command | `C84355B912DCF3BB59D52004EC80E06CE8B37FC75B5ABE083ECCA95D64C89BA3` |
| Helper | `17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E` |
| Binary exporter | `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06` |
| SQLite bridge | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |

Không yêu cầu owner cung cấp thêm key, endpoint hoặc fingerprint. Expected
account digest phải tồn tại trong `Process` của đúng PowerShell host thực thi;
chỉ được kiểm tra `present + shape`, không đọc, in, ghi file hoặc chuyển cho agent.

## 2. Mục tiêu duy nhất

Thực thi đúng một lần file
`docs/P5E_A43_FINAL_EXECUTABLE_COMMAND_20260926.txt`, tạo đúng một event mới,
thu Before read-only, thực hiện memory-only account comparison, tạo fresh
authorization trong event nếu mọi gate đạt, phát tối đa một RAW/GLOSSARY call,
thu After trong `finally`, rồi chạy verifier. Kết thúc bằng một kết quả terminal
và provenance redacted.

Đây không phải work package sửa lỗi. Khi một gate dừng, giữ nguyên event và
chuyển sang chẩn đoán offline ở work package sau. Không sửa helper/command,
không chạy lại và không mở event thứ hai trong cùng yêu cầu này.

## 3. Baseline và provenance Git

1. Xác nhận branch thực tế là
   `feature/v4.18-p5e-runner-repair-20260917`.
2. Xác nhận packet commit hiện tại là
   `cbe820deea077ffd3ef920959bfbccb396f2455a`.
3. Diễn giải đúng giá trị `2b34266eb60e3439864c80aae5d30ae141cfe8aa`
   trong manifest/provenance đã đóng băng: đó là implementation baseline ngay
   trước packet commit, không phải HEAD hiện tại.
4. Không thay byte manifest, command, helper, exporter hoặc bridge để sửa cách
   diễn đạt trên. Thay byte sẽ làm quyết định owner mất hiệu lực.
5. Giữ nguyên toàn bộ thay đổi owner đang có; không reset, clean, checkout,
   stash, stage hoặc commit file không thuộc kết quả event.

## 4. Các bước thực hiện nhỏ nhất

### A. Kiểm tra bất biến trước khi mở event

1. Đọc canonical plan, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md` và checklist.
2. Ghi nhận quyết định owner là `RECEIVED_NOT_CONSUMED`; không hỏi lại owner.
3. Tính lại SHA-256 của năm thành phần trong bảng trên từ file hiện hành.
4. So sánh từng hash theo exact bytes; sai một giá trị thì dừng trước ADB và
   ghi `AUTHORIZED_PACKET_PIN_MISMATCH`.
5. Kiểm tra các path là file thường, path chuẩn, không có leaf/ancestor reparse.
6. Kiểm tra resolver, `local.properties`, APK/source/BUILD_INFO pair và
   certificate theo các pin trong manifest; không build hoặc install.
7. Trong đúng PowerShell host sẽ chạy command, đặt
   `P5E_A43_COMMAND_SHA256` bằng exact command hash ở trên.
8. Trong cùng Process, kiểm tra expected digest chỉ theo `present + shape`.
   Không xuất giá trị ra console, log, file, child process hoặc tài liệu.
9. Nếu expected digest không còn trong Process, dừng trước event với
   `EXPECTED_PROCESS_VALUE_MISSING`; không lấy key từ app, không đọc key và
   không thay expected bằng giá trị tự suy ra.
10. Không chạy probe ADB thủ công ngoài chuỗi lệnh đã pin.

### B. Mở và chạy đúng một event

1. Chỉ sau khi A.1–A.10 đạt, chạy đúng file command đã pin bằng Windows
   PowerShell 5.1 theo nội dung của file; không chép lại hoặc sửa lệnh.
2. Cho command tự tạo một event directory mới. Không nhập đường dẫn event cũ.
3. `PrepareEvent` phải chạy một lần và bind manifest/helper/exporter/bridge,
   serial, artifact, certificate, scope, budget và deadlines.
4. Before collector chỉ dùng các lệnh read-only được allowlist và export binary
   database theo contract host-readback.
5. WAL/SHM exit `1` chỉ được hiểu là `ABSENT` ở đúng presence probes; các exit
   khác phải đi qua typed contract, không suy đoán.
6. Binding tuple phải được lấy theo contract đã sửa và so sánh đủ trường; không
   dùng heuristic, basename hoặc fallback query.
7. Account comparison chỉ memory-only. `MISMATCH`, thiếu identity hoặc capture
   không đầy đủ đều là terminal stop trước provider.
8. Khi toàn bộ Before gates đạt, tạo đúng một fresh authorization thuộc event.
9. Dispatch tối đa một primary RAW/GLOSSARY call, budget tối đa `USD 0.05`,
   không network retry, schema-repair call hoặc fallback model.
10. Không để DRAFT/PRONOUN đi vào payload hay evidence.
11. Chỉ các atomic attempt, authorization receipt, lifecycle và committed-result
    writes đã allowlist mới được phép.
12. After collector phải chạy trong `finally` dù dispatch success, failure,
    timeout hoặc trạng thái chưa biết.
13. Chạy `VerifyOutcome` một lần sau After; không biến lỗi verify thành lý do
    redispatch.

### C. Xử lý kết quả terminal

1. **Before typed stop:** RAW chưa dispatch; đóng event; không retry.
2. **Account mismatch/unknown:** provider phải là `0`; đóng event; không tự sửa
   route/key và không chạy account event khác.
3. **Dispatch timeout/exception/unknown:** coi external outcome là chưa biết;
   vẫn thu After; tuyệt đối không redispatch.
4. **After hoặc verifier fail:** không công nhận A4.3/P5 exit; giữ evidence;
   không cleanup/restore/reconcile.
5. **Provider từ chối hoặc output không hợp lệ:** giữ một-call result; không
   fallback hay schema-repair call.
6. **Success:** chỉ công nhận `A4_3_ACCEPTED` khi fresh authorization, đúng một
   provider call, allowlisted writes, receipt/artifact exact bytes, Before/After
   readback và verifier đều PASS.
7. Dù success, không tự mở P6. P5 exit phải được đánh giá riêng từ evidence của
   chính event này.

### D. Evidence và báo cáo bắt buộc

1. Ghi event id/path mới và SHA-256 của `EVENT_PLAN.json`, collector outcomes,
   command log, dispatch/After/verifier receipts tồn tại.
2. Báo số launch/read-only commands, provider calls, credential reads, device
   writes, DB writes, RAW dispatches, retries và redispatches.
3. Báo rõ event dừng ở bước nào và typed code chính xác.
4. Chỉ đọc evidence redacted được contract cho phép. Không đọc hoặc chép
   instrumentation stdout/stderr, credential, endpoint, expected digest hay
   provider payload thô.
5. Đối chiếu Before/After database theo exact row/field matrix trong manifest;
   không đánh dấu PASS bằng suy luận từ exit code chung.
6. Tạo một result JSON và một provenance JSON cho event, rồi QA JSON parse,
   exact hashes, counters, redaction và `git diff --check`.
7. Nhờ Luna phản biện kết quả cuối theo exact evidence, tập trung vào khả năng
   false PASS, missing After, hidden retry/redispatch và write ngoài allowlist.
8. Chỉ sau QA và Luna review mới đồng bộ canonical plan, `BUILD_STATE.md`,
   `WORKSPACE_SNAPSHOT.md`, checklist và proposal.

## 5. Quy tắc chống vòng lặp

- Quyết định owner hiện tại đã nhận; không tạo thêm owner-request trước lần chạy.
- Một decision → một event → một terminal result. Event bắt đầu là decision bị
  consume, kể cả khi dừng trước provider.
- Không “thử lại để lấy thêm log”. Chẩn đoán sau stop phải offline từ evidence
  redacted đã có.
- Không tạo manifest/command/branch/checklist mới nếu chưa có một lỗi thực tế
  buộc thay byte hoặc owner thay scope.
- Không sửa nhiều lớp cùng lúc. Một typed stop chỉ mở một work package có test
  tái hiện tối thiểu, RED→GREEN, regression và phản biện.
- Không dùng số lượng test PASS để suy ra live success. Chỉ receipt của event
  mới quyết định A4.3/P5.
- P6 luôn là quyết định sau khi A4.3 và P5 exit đã được chứng minh; không gộp
  vào event này.

## 6. Tiêu chí hoàn thành

Work request này hoàn thành khi có đúng một trong hai dạng kết quả:

1. `TERMINAL_TYPED_STOP`: event được đóng, decision consumed, không retry,
   phạm vi chưa đạt được nêu chính xác; hoặc
2. `A4_3_ACCEPTED`: toàn bộ acceptance matrix và provenance của một event đạt,
   sau đó tạo đánh giá riêng về P5 exit; P6 vẫn chưa tự động mở.

Không được kết thúc bằng trạng thái mơ hồ như `continue investigation`,
`owner review pending` hoặc `retry recommended`.
