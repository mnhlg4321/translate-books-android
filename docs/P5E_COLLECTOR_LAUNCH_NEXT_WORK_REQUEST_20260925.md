# Yêu cầu làm việc + provenance — sửa toolchain launch RAW, 2026-09-25

Baseline 148a6a52da7fec230c7e1c1e8bacae758c806816, branch feature/v4.18-p5e-runner-repair-20260917, D:\App Translate Books.
Status: LOCAL_HOST_REPAIR_REQUIRED / OLD_EVENT_CLOSED / RAW_NOT_DISPATCHED / P6_NOT_READY. Chưa đủ trình chạy live tiếp. Owner không cần cung cấp key/digest hoặc duyệt thêm việc sửa offline.

## Kết luận và phạm vi bằng chứng

Event raw-live-20260925-005619067-59f53846763e4f09b0fb948551b3b98a STOP trước launch ở pm-path-production-before. Một command record là một lần thử, không phải một ADB process đã chạy. Launch0/exitnull không chứng minh package thiếu hoặc thiết bị lỗi. Account MATCH cũ vẫn giữ nguyên.

Lỗi cấu trúc đã thấy trong source: RAW command truyền tên trần adb/apksigner cho Before/After; helper Dispatch còn hardcode FilePath 'adb'. Account command khác ở chỗ resolve SDK → executable path trước launch. Host hiện tại không resolve adb/apksigner qua Get-Command, nhưng SDK/platform-tools/adb.exe tồn tại theo local.properties. Đây là bằng chứng môi trường hiện tại, không hồi dựng PATH trong cửa sổ owner lúc event cũ.

Probe synthetic qua chính Invoke-P5EReadOnlyProcess tái hiện executable thiếu → FAILED_BEFORE_LAUNCH/launch0 và executable tuyệt đối → launch1/exit0. Không gọi ADB thật. Đây là chẩn đoán cơ chế, chưa phải repair GREEN và chưa chứng minh nguyên nhân lịch sử duy nhất. Receipt cũ không có native error; không đọc stdout/stderr để đoán.

Vì sao QA nhiều mà vẫn fail: QA collector SQL/verifier và fake supervisor chưa chứng minh đường resolve toolchain của RAW trên Windows PowerShell thực tế. Account path đã được sửa nhưng RAW còn entrypoint khác. Không giải quyết bằng việc thêm adb vào PATH thủ công rồi chạy lại.

## Yêu cầu thực hiện, cùng branch, không chỉ soạn thêm plan

1. Đọc canonical/state/snapshot, xác minh HEAD/status và ba hash evidence event cũ; không sửa/xóa/rename/reopen event.
2. Bảo toàn receipt MATCH và các fixture parser đã PASS; không chạy account-only lại, không xin provenance/key mới.
3. Rà toàn bộ entrypoint Before/Dispatch/After và local certificate verification. Lập đúng một contract toolchain cho adb, signer và Java nếu signer cần; không chỉ sửa command đầu tiên.
4. Implement resolver offline dùng SDK cấu hình/đường dẫn được chỉ định rõ, kiểm absolute regular file, ancestor reparse, missing/ambiguous selection. Có thể tái dùng logic account runner nhưng không dot-source entrypoint có side effects. Không mặc định “bản build-tools mới nhất” nếu không chứng minh tương thích.
5. Xác định cách launch signer trên Windows: apksigner.bat có phụ thuộc cmd/Java. Chọn cơ chế có kiểm chứng và đúng quoting (hoặc Java executable + apksigner.jar), không giả định file tồn tại là chạy được. Không execute signer/ADB thật trong QA này.
6. Resolve/validate toolchain trước tạo live event, trước expected/device access; in chỉ typed reason, không dump PATH/environment. Missing tool phải có typed STOP trước bất kỳ ADB launch nào.
7. Truyền cùng absolute adb path xuyên Before → Dispatch → After. Thêm tham số vào Dispatch nếu cần; bỏ hardcode 'adb' ở FilePath. Command phải truyền resolved signer đúng hai collector; không sửa global PATH để che lỗi.
8. Bổ sung chẩn đoán launch redacted: error class và native numeric code/allowlisted reason nếu có. Không ghi Exception.Message, argv, env hoặc raw output. Giữ đọc được receipt cũ; update schema/validator/test đồng bộ nếu thay shape.
9. Kiểm expected không bị child tool không cần secret kế thừa. Chỉ đường dispatch được phép nhận expected theo contract; fixture synthetic chứng minh redaction và env boundary, không đọc expected thật.
10. RED→GREEN fixture phải dùng Windows PowerShell5.1 và đúng collector/command launch path: PATH không có SDK nhưng file hợp lệ; absolute path chứa khoảng trắng; missing adb/signer/Java; reparse; process start native error; nonzero; timeout; stdin/capture/redaction. Dùng fake tools, không adb version/devices/shell thật.
11. Kiểm toàn chuỗi fake Prepare → Before → Dispatch tối đa1 → After trong finally → Verify, gồm failure trước launch và sau possible dispatch. Nếu chỉ có static regex/SQL fixture thì chưa đủ GREEN. Không để lần chạy thật kế tiếp mới phát hiện Dispatch/signer vẫn dùng PATH.
12. Khi test fail, sửa ngay cùng phase/branch rồi chạy lại đúng test; không phát hành một packet “review pending” thay cho repair. Nhờ Luna phản biện missing dependency, quoting, env leakage và no-redispatch.
13. Sau PASS, cập nhật helper/command dependency pins rồi hash cuối; manifest chỉ đổi nếu contract thực sự thay đổi. Lưu provenance về SHA cũ→mới và evidence test. Giữ budget/project/request/source/artifact pins và production/app source nguyên trạng nếu không có nguyên nhân buộc đổi.
14. Đồng bộ canonical/state/snapshot/checklist/proposal: repair offline PASS khác live acceptance; account MATCH đóng, event cũ terminal, RAW chưa chạy. Chỉ một Next action.
15. Bàn giao một packet sửa hoàn chỉnh để owner duyệt một event mới. Quyết định cũ gắn event/hash cũ đã dùng, không tự tái sử dụng. Không xin owner duyệt lại việc chẩn đoán offline và không thử device để kiểm resolver.

## Điều kiện thoát repair

Cả Before/Dispatch/After dùng toolchain đã xác định, fake command integration PASS và các counterexample bị chặn đúng; hash/provenance đồng bộ; Luna review không còn lỗi cụ thể. Không claim sửa dứt điểm mọi lỗi thiết bị từ probe executable đơn giản.

Phạm vi cấm trong công việc này: ADB/device/provider/live DB/account/credential/build/install/settings write/RAW/retry/redispatch. Không restore DB, rotate authorizationId, thay key, nới budget hoặc bỏ certificate gate.

Sau repair: một quyết định mới trên packet mới mới có thể cho live. Dù RAW sau này COMMITTED, vẫn đối chiếu tiêu chí P5 trước P6. Nếu live STOP mới, chỉ mở đúng lỗi có evidence; không reset toàn kế hoạch.

## QA của lần lập yêu cầu

Đọc sâu control docs, code launch và typed receipts liên quan; không tuyên bố đọc mọi dòng tài liệu lịch sử. Synthetic launch probe PASS_DIAGNOSTIC_NOT_REPAIR; không runtime source thay đổi. Hash evidence event cũ phải giữ nguyên. Companion provenance: P5E_COLLECTOR_LAUNCH_REVIEW_PROVENANCE_20260925.json.