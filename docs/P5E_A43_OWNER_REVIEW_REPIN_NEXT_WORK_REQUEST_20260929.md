# Yêu cầu làm việc tiếp theo — chốt owner decision cho đúng một A4.3 event

## 1. Trạng thái đầu vào

`OFFLINE_PACKET_REVIEWABLE_REPIN_PASS / OWNER_REVIEW_PENDING / NOT_AUTHORIZED / NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`

Work package offline đã sửa xong lỗi contract, collector, binding, capture,
decision atomicity và expected-value isolation. Lần đồng bộ cuối phát hiện thêm
một lỗi tài liệu: manifest vẫn yêu cầu làm lại archive/review dù hai gate đó đã
PASS. Dòng này đã được sửa, manifest/command đã repin và clean-archive của
commit `a68d6ceb` đã PASS.

Work package tiếp theo chỉ được làm một việc: kiểm tra packet exact-byte và đưa
ra một quyết định owner rõ ràng `APPROVE_ONE_FRESH_EVENT` hoặc `STOP`. Không
được quay lại sửa parser/fixture/runtime nếu không có một bằng chứng RED mới,
cụ thể và tái hiện được offline.

## 2. Packet phải review

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Runtime candidate commit: `a68d6ceb`.
- Serial: `15e84958`.
- Manifest:
  `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md`.
- Manifest SHA-256:
  `9F0928876963EBE00187A384008D10569C0667C18C54588C20A9BE0E9D30E200`.
- Command:
  `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt`.
- Command SHA-256:
  `A179433558798687452E061AEAE8C0D20E4D8C96AA2F6C9ADC05B0A573B656B1`.
- Helper SHA-256:
  `4DAD6E30928DD0D78396BDDFA57AFA1053E75481524D2EA05659227869C57444`.
- Runtime guard SHA-256:
  `C31217CDBD40F22DB9A74AFB529B1ECC33F725F485ECD462992073235F59ADD8`.
- Exporter SHA-256:
  `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99`.
- SQLite bridge SHA-256:
  `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`.
- Toolchain SHA-256:
  `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8`.
- Certificate SHA-256:
  `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.

Mọi manifest, command, decision và event cũ đều
`CLOSED_CONSUMED_NON_REUSABLE`. Không có fallback sang hash cũ.

## 3. Vì sao trước đây bị lặp

1. Nhiều typed stop khác nhau bị gom thành một cảm giác “chưa sẵn sàng”, nên
   mỗi lần dừng lại sinh thêm packet mà không khóa rõ lỗi nào đã giải quyết.
2. Hash phụ thuộc theo chuỗi manifest -> command -> provenance -> canonical
   docs. Sửa một byte ở upstream nhưng không repin theo đúng thứ tự làm tài
   liệu mâu thuẫn và kéo review quay lại.
3. Current-state và historical-state nằm cùng tài liệu. Một dòng next-action
   cũ có thể bị đọc như trạng thái hiện hành dù bằng chứng mới đã PASS.
4. Owner gate từng được lặp lại sau khi account đã `MATCH`. Không được xin lại
   key, endpoint hoặc fingerprint; chỉ cần expected digest process-only tại
   đúng thời điểm mở event.
5. Một số stop là fail-closed đúng thiết kế, nhưng đã bị xử lý như lời mời tự
   retry. Event/decision đã dùng phải đóng vĩnh viễn.

Quy tắc chống lặp từ work package này:

- Chỉ một bảng pin hiện hành trong phần đầu canonical docs.
- Freeze dependency theo thứ tự runtime -> manifest -> command -> archive QA
  -> Luna review -> provenance -> canonical state.
- Không sửa manifest/command sau khi archive QA và independent review đã PASS.
- Không tạo packet mới nếu chưa có RED evidence mới.
- Mỗi event chỉ có một terminal outcome; stop nghĩa là đóng event, không retry.
- P6 chỉ được đánh giá sau một kết quả RAW terminal hợp lệ và readback PASS.

## 4. Các lỗi lịch sử đã được cover

| Lỗi đã gặp | Cách packet hiện tại xử lý | Nếu tái diễn |
| --- | --- | --- |
| Expected process value thiếu | Gate trước event; không mở event/ADB | Dừng `EXPECTED_PROCESS_VALUE_MISSING`, không xin/đọc giá trị từ agent |
| Package/signature layout không nhận diện | Parser AOSP wrapper và signer split đã có fixture | Chỉ sửa khi có redacted layout fixture mới |
| Sai class/method identity | Exact instrumentation identity contract | Dừng trước account runner; không đoán tên |
| Account `MISMATCH` do route | Route đã sửa và event sau đó `MATCH` | Không chạy lại account check; kiểm mapping ngoài event |
| ADB/toolchain không launch | Absolute resolver, timeout/capture và tool pins | Typed stop; không đổi PATH hoặc retry thủ công |
| Package version mismatch | Version contract dùng exact artifact metadata | Dừng trước export; không bỏ version gate |
| WAL/SHM exit 1 | Được phân loại `ABSENT` hợp lệ | Không coi là ADB failure |
| Android không có `sqlite3` | Binary export + host SQLite readback | Không quay lại live `sqlite3` |
| Binding tuple mismatch | Host contract/matrix `262/262` | Dừng và giữ export; chỉ chẩn đoán offline |
| Outer capture code 125/pm path | Tách helper exit, drain status và typed collector stop | Giữ cả hai bằng chứng; không suy diễn USB/package |
| Decision/receipt race hoặc crash | Atomic one-use reservation, crash vẫn consumed | Không rollback/xóa marker để retry |
| Expected digest rò sang phase khác | Phase-specific environment isolation | Dừng trước helper nếu boundary sai |
| Stale manifest next action | Đã sửa và repin exact bytes | Canonical consistency check phải fail nếu tái xuất hiện |

## 5. QA bắt buộc trước khi owner quyết định

Các bước sau phải chạy offline và phải cùng chỉ về packet ở mục 2:

1. Tính lại SHA-256 manifest, command, helper, guard, exporter, bridge và
   toolchain; so exact, không chấp nhận prefix.
2. Parse PowerShell 5.1 cho toàn bộ runtime/QA dependency.
3. Chạy main runtime suite: yêu cầu `21/21 PASS`.
4. Chạy atomicity/environment suite: yêu cầu `28/28 PASS`, `highFailures=0`.
5. Chạy binding suite: yêu cầu `262/262 PASS`, regression `175/175`.
6. Chạy DB host-readback suite: yêu cầu `56/56 PASS`.
7. Tái dựng từ exact Git archive của `a68d6ceb`, không workspace fallback và
   không cached result.
8. Chạy secret/endpoint/native-output scan trên packet/evidence.
9. Kiểm tra closed events/consumed decisions vẫn không reusable.
10. Luna phản biện độc lập; chỉ `0 BLOCKER / 0 HIGH` mới được trình owner.
11. Chạy `git diff --check` và xác nhận không stage owner dirt ngoài phạm vi.

Evidence archive-clean hiện tại:
`docs/P5E_A43_OWNER_REVIEW_REPIN_ARCHIVE_CLEAN_20260929.json`, SHA-256
`54BC70042C3239E058CA701E915054788BEEDCD7791A2B9A9BB65872DB5B7B28`.

## 6. Owner cần quyết định gì

Owner không cần cung cấp lại API key, endpoint, account fingerprint hoặc
credential reference. Owner chỉ cần chọn một trong hai kết quả sau:

### A. STOP

Ghi `STOP_NO_EVENT`. Không tạo receipt, marker, event hoặc command launcher.
Giữ `P6_NOT_READY`.

### B. APPROVE_ONE_FRESH_EVENT

Quyết định phải bind đủ hash ở mục 2 và chỉ cho phép:

1. đúng một fresh A4.3 event trên serial `15e84958`;
2. read-only Before/After collection đã pin;
3. account comparison memory-only bằng expected digest process-only;
4. đúng một fresh runtime authorization;
5. tối đa một RAW/GLOSSARY provider call;
6. đúng DB writes trong allowlist của manifest;
7. budget tối đa `USD 0.05`;
8. không retry, repair-in-event, fallback, RECONCILE, cleanup, restore,
   redispatch, build/install hoặc tự mở P6.

Expected digest phải được owner nạp vào `Process` của chính PowerShell host sẽ
chạy command. Agent không đọc, in, log, persist, suy diễn hoặc yêu cầu owner gửi
giá trị đó.

## 7. Trình tự nếu owner chọn APPROVE_ONE_FRESH_EVENT

Không bỏ bước và không tự động chuyển tiếp sau typed stop.

1. Xác nhận decision bind exact full hashes, serial, scope và budget.
2. Xác nhận decision ID mới; không reuse ID/receipt/event cũ.
3. Xác nhận expected digest có mặt và đúng shape trong Process; chỉ trả
   present/valid, không trả value/length/prefix/suffix.
4. Kiểm hash packet lần cuối trước reservation.
5. Tạo atomic decision reservation một lần.
6. Tạo receipt binding một lần; flush và giữ consumed ngay cả khi bước sau lỗi.
7. Tạo event directory mới và `EVENT_PLAN.json`; không rename/reuse path cũ.
8. Chạy Before collector read-only.
9. Nếu Before có typed stop: đóng event, ghi redacted result, kết thúc; không
   chạy Dispatch/After và không retry.
10. Nếu Before PASS: chạy account comparison memory-only.
11. Nếu account không `MATCH`: đóng event, không gọi provider/DB.
12. Nếu `MATCH`: tạo fresh authorization đã bind và gọi tối đa một
    RAW/GLOSSARY request trong budget.
13. Ghi đúng allowlisted DB outcome theo terminal provider result; không
    fallback hoặc RECONCILE.
14. Chạy After collector read-only đúng cùng event.
15. Verify exact report/receipt bytes, hashes, tuple binding, counters và
    no-redispatch.
16. Phát hành một terminal result duy nhất: `PASS`, `MISMATCH`, typed stop,
    provider failure hoặc verification failure.
17. Xóa expected digest khỏi Process sau khi terminal và không ghi value vào
    evidence.
18. Đồng bộ canonical docs một lần. Không sửa frozen event evidence.

## 8. Điều kiện đánh giá P5/P6

- `P5_EXIT_NOT_CLAIMED` và `P6_NOT_READY` nếu event chưa chạy, dừng trước RAW,
  RAW/provider thất bại, readback/verify thất bại hoặc evidence không đủ.
- Chỉ được đề xuất đánh giá P5 exit khi đúng một RAW terminal success, durable
  allowlisted write, After readback và verifier đều PASS.
- P6 vẫn cần một work request riêng; event A4.3 thành công không tự mở P6.

## 9. Acceptance của work package tiếp theo

Work package được coi là hoàn tất khi đạt đúng một trong hai trạng thái:

- `OWNER_STOP_RECORDED / EVENT_NOT_OPENED / P6_NOT_READY`; hoặc
- `OWNER_APPROVAL_EXACT_PACKET_RECORDED / ONE_FRESH_EVENT_AUTHORIZED / EVENT_NOT_YET_OPENED / P6_NOT_READY`.

Không được kết thúc bằng một packet sửa chữa offline mới nếu không có finding
BLOCKER/HIGH hoặc RED fixture mới. Finding MEDIUM/LOW chỉ được sửa khi trực tiếp
ảnh hưởng safety/correctness của đúng event này; nếu không, ghi nhận và tiếp
tục owner decision.
