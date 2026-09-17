# P5E — audit luồng thực thi và nguyên nhân lặp, 2026-09-17

Baseline đã kiểm: `c222b13cafbb0c58a868072b44ae6df17905d089`, branch
`feature/v4.18-p5e-audit-20260914`, worktree sạch lúc bắt đầu.

## Kết luận hiện hành

`TEST_PACKAGE_REPLACEMENT_PASS / ACCOUNT_RUNNER_REPAIR_REQUIRED / EXPECTED_SOURCE_PENDING / ACCOUNT_CHECK_NOT_EXECUTED / A4.3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY`.

Đã đủ bằng chứng để tiếp tục sửa host runner trong P5E. Chưa đủ chạy account
check nguyên trạng, A4.3 hoặc chuyển P6. Không cài lại test package. Request
tiếp nối duy nhất: `P5E_NEXT_WORK_REQUEST_20260917.md`. Đây là audit + kế hoạch,
không phải một lần sửa runtime, chạy thiết bị hay cấp quyền mới.

## Phạm vi và provenance

Kiểm kê hash 163 Markdown/TXT tracked trong `P5E_DOCUMENT_INVENTORY_20260917.json`.
Đọc sâu authority/workflow, canonical plan, BUILD_STATE, snapshot, checklist,
packet/proposal/manifest/command, các báo cáo P5E và code account runner, Java
verifier, SettingsStore, endpoint normalization, helper transport/supervisor.
Không tuyên bố đã đọc ngữ nghĩa mọi dòng lịch sử hoặc QA mọi module sản phẩm.

Đã đọc đúng hai JSON kết quả cài ngoài Git và hash APK pull-back đã tồn tại;
không đọc credential, environment fingerprint, DB hoặc gọi ADB. Kết quả:

| Evidence | SHA-256 kiểm lại |
|---|---|
| Account local result | `3FBE39142BA2DAA16AF6F5301271525E71FE1871CF6C14F0DF2A816346AA53EF` |
| Installer offline QA | `041760CD298DA06928D35D41321CF497D7DB49C25A0608D35609370884138519` |
| CheckOnly private JSON | `31F22C8561CC403B7F11A437E2312B9CF784DC98DAAC5B4E8D52DF72146FD809` |
| Replacement private JSON | `C6D4B73514135413BE72A6C08D859758FD3B644BF978FFB3A5E424D2DD46FD76` |
| Installed APK pull-back | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` |
| Account runner đầu vào | `0722A243C92724D59AFB7CF4B6DE674024F9BAE4FD76A712DF0210AF25B036F3` |
| RAW helper đầu vào | `364A6AA2C52A90E7AD20F28EC6C46A0EAD1BA39E8909727BEA7396287896FFE7` |
| Probe result mới | `AA2E6FE3CBFB7084CCA15C5D3AA4F20A59BA9525714123D1ABAC3B863350173F` |

Hash khớp chứng minh liên kết file, không tự chứng minh tất cả assertion trong
file đúng. Private installer JSON ghi code207 nhưng không chứa full production
APK/DB before-after: không nâng thành chứng nhận bảo toàn toàn bộ dữ liệu.
`replacementTestArtifact.deviceOperations=0` trong result cũ là trường mơ hồ
khi `installed=true`; số install hiện hành phải lấy từ `liveSequence` (=1).
Installer QA là offline snapshot, `devicePreflightStillRequired=true` của nó
không ghi đè evidence cài sau đó. Giữ nguyên bytes evidence cũ.

## Lỗi/giới hạn có bằng chứng

1. **P1 — sai component gọi instrumentation.** Account runner gán
   `$testRunner = 'androidx.test.runner.AndroidJUnitRunner'` rồi dùng làm token
   cuối. Cần `com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner`.
   RAW helper cũng có cùng class-only token tại `$script:P5ERunner`.
   [Android Developers](https://developer.android.com/studio/test/command-line)
   quy định dạng `<test_package_name>/<runner_class>`. Đây là source-contract
   defect; chưa chạy Android để tái hiện failure và không suy ngược A4.2 cũ fail.
2. **P1 — false acceptance đã tái hiện.** Probe thực thi đúng đoạn classifier
   lấy từ runner, không sao chép implementation. Với adb exit 0, runner chấp
   nhận cả bốn trường hợp: MATCH rồi FAILURES/terminal 0; MATCH thiếu terminal;
   MATCH của class/method khác; MATCH_EXTRA. Thiếu ràng buộc test identity,
   kết thúc thành công và exact token. Hai control duplicate/missing bị từ
   chối đúng. Fixture hợp lệ là control cho classifier cũ, không phải chứng
   nhận đủ contract mới. Lệnh probe trả nonzero theo thiết kế vì phát hiện RED.
3. **P1 — mô tả kênh fingerprint chưa khớp code.** Expected đọc từ environment
   Process nhưng tiếp đó được đưa vào `ProcessStartInfo` argv cho adb và
   instrumentation extras. Redaction stdout không xóa argv/process metadata;
   child cũng kế thừa environment nếu không loại bỏ. Không được gọi toàn
   transport là “không có trong command text”. Chưa có bằng chứng secret thật
   đã lộ: account check chưa chạy. Cần chọn/kiểm chứng transport phù hợp scope
   đã duyệt trước khi nạp giá trị thật; không âm thầm nới scope để làm PASS.
4. **P2 — timeout chưa bảo đảm giới hạn toàn bộ.** Sau Kill thất bại hoặc child
   giữ pipe, `ReadToEndAsync().GetAwaiter().GetResult()` vẫn có thể đợi vô hạn.
   Đây là risk từ source, chưa tái hiện runtime. Cần fake-process test có hạn
   thời gian và sửa deadline/drain nếu cần, không thử bằng device.
5. **Giới hạn ý nghĩa kết quả.** MATCH chỉ là endpoint/key digest bằng expected
   và route predicate khớp. Không chứng minh key còn hiệu lực, số dư, billing
   account hoặc quyền provider. Java catch Throwable và route mismatch đều
   trả MISMATCH; không kết luận chắc chắn “sai account” từ MISMATCH.
6. **Tài liệu điều hướng mâu thuẫn.** Runbook cài và reconciliation vẫn có
   banner Current yêu cầu sửa SQL đã hoàn tất. Canonical plan/checklist có
   nhiều “exact next action” ở các checkpoint cũ. Đây là nguồn mở lại việc cũ.

## Vì sao nhiều vòng mà ít tiến triển

- Đã có tiến triển thật: SQL/serializer sửa xong; CheckOnly/replacement đã
  chạy. Hai việc này phải đóng, không tái cài hoặc audit lại khi input không đổi.
- QA trước kiểm từng mảnh hoặc regex/self-test mô phỏng, chưa kiểm cả command
  component và kết thúc instrumentation. Chính lượt trước tuyên bố installer
  QA + phản biện PASS nhưng vẫn bỏ sót certificate uppercase/lowercase, sau đó
  phải sửa tại `afc34b87`. Tôi rút lại suy rộng PASS đó thành live-ready.
- “Cung cấp trusted expected” chưa trở thành thao tác owner làm được: chưa rõ
  còn key gốc độc lập hay chỉ còn key trong app; environment ở cửa sổ PowerShell
  khác không truyền ngược vào process Codex đang chạy.
- Owner/account permission và input provenance bị trộn với bug code. Xin thêm
  approval không sửa được command/parser; thêm hash/tài liệu không tạo expected.
- History và current cùng dùng từ Current/next action, làm mỗi lượt mở lại
  một checklist cũ. Chỉ canonical current override và request mới điều phối.

## QA và phản biện trước xuất

- Probe `P5E_ACCOUNT_RUNNER_AUDIT_PROBE_20260917.ps1`: classifier source thật,
  4 false accepts; no runner launch, environment read, ADB, provider, credential.
- Kiểm lại hashes evidence, pull-back APK và Git baseline; giữ evidence lịch sử.
- Không coi JSON đếm providerCalls=0 gán hằng là telemetry provider độc lập.
- Không coi fixture PASS là live PASS; không lấy actual làm expected.
- Không coi account MATCH hoặc RAW đơn lẻ là P5 exit/P6; cần đủ checklist
  canonical, outcome/readback, predecessor và quyết định chuyển phase.
- Review cuối phải kiểm request có điều kiện dừng cụ thể, không reinstall,
  không chạy lại H1–H4/SQL/golden nếu không đụng code liên quan.

## Quy tắc thoát vòng lặp

Một lần repair có mục tiêu: component + classifier + transport/deadline.
Một bộ fixture dùng chính code sửa, một vòng phản biện, rồi chốt hash một lần.
Nếu chỉ còn thiếu nguồn expected: ghi BLOCKED_EXTERNAL, nêu chính xác input,
không tạo thêm build/commit/review cho trạng thái không thay đổi. Nếu owner
không còn nguồn key độc lập, yêu cầu quyết định phương án xác minh khác;
không giả tạo MATCH. Không có yêu cầu phê duyệt lại account check đã được duyệt.
