# Yêu cầu làm việc kế tiếp — tiếp tục P5E, đóng lỗ hổng trước RAW

Đọc audit `docs/P5E_AUDIT_20260914.md`. Tiếp tục `EDITORIAL_RECOVERY_V4_18.md`, phase P5/P5E. **Chưa chuyển P6 và chưa chạy file A4.3 hiện tại.** Mục tiêu của lượt tới là làm cho đường chuẩn bị RAW thực sự chạy được và kiểm chứng được, rồi trình đúng quyết định còn thiếu. Không viết thêm một proposal tương đương chỉ đổi ngày/tên A4.x.

## A. Phạm vi được giao khi sử dụng yêu cầu này

Được thực hiện sửa tài liệu, command/host helper và kiểm chứng offline cho các F1–F3 đã có bằng chứng. Không đổi production source, schema, migration, pack/profile, prompt, model/route, budget, input identities hoặc dữ liệu pilot. Không đọc credential, instrumentation, cài/rebuild APK, tạo runtime authorization, provider call hay RECONCILE từ đoạn này.

Phần E dưới đây là runbook có điều kiện để review, không phải quyền live mặc định. Quyền cần thiết sẽ được hỏi một lần khi có packet cuối đủ cụ thể, kèm phần account operation chính xác. Việc user yêu cầu nghiên cứu/viết plan không thay cho quyết định gửi sách.

## B. Baseline và bàn giao — làm từng bước

1. Vào `D:\App Translate Books\App Translate Books-translation-profile`. Không dùng checkout D1 ở thư mục cha.
2. Đọc BUILD_STATE rồi WORKSPACE_SNAPSHOT theo chỉ thị startup của owner; đọc canonical plan, GIT_WORKFLOW và DEVELOPMENT_WORKFLOW trước khi sửa.
3. Ghi `git rev-parse HEAD`, `git status --short --branch`; xác nhận audit baseline là hậu duệ của `f8fe433ef454772a1b55dea496a2c4bfd679766f`. Dừng mutation nếu có diff không rõ chủ sở hữu; vẫn có thể đọc để phân loại.
4. Resume branch audit/repair đã được giao. Không tự mở release/version/checklist khác. Nếu chỉ thị owner phiên mới yêu cầu branch mới, ghi rõ đây là branch công việc từ exact HEAD hiện tại, không thay baseline về main cũ.
5. Dùng checklist release `release_checklists/v4.18-editorial-v5-safe-4-1-3.md`, đối chiếu TEMPLATE; không đánh dấu bước 05–09 release chỉ vì host repair PASS.
6. Kiểm lại manifest/command A4.3 hash theo audit; giữ baseline hash trước sửa. Không chạy `.txt`, dot-source hay Invoke-Expression.
7. Kiểm production/test APK và source ZIP hai bản bằng hash; đọc metadata event. Không biến `installed=false` tại build time thành lỗi trạng thái thiết bị.
8. Kiểm source diff từ d51b7f3c tới HEAD theo đường production/test/script. Nếu Android source khác, dừng dependent dispatch planning để xác định artifact có còn tương ứng; không rebuild tự động.
9. Chốt bảng current chỉ gồm: worktree/branch/actual HEAD; code207/test57EC99; P5E; A4.2 PASS; RAW chưa chạy; F1–F3. Mọi CP6/IPC/code196 là lịch sử, không là gate mới.

Đầu ra B: một baseline table có evidence, không có action thiết bị.

## C. Sửa F2 và làm rõ F3 ở host

10. Đọc toàn bộ command và live method được chọn; lập danh sách required arguments từ code, không từ proposal.
11. Reproduce dấu pipe với dữ liệu giả qua các lớp thực tế: PowerShell → process argv → ADB nối remote command → shell → argv tương đương am instrument. Stub phải không có khả năng gọi adb/provider thật.
12. Ghi RED: cancellationStopAuthority bị tách hoặc không còn byte-exact. Parser PowerShell PASS không được dùng thay RED này.
13. Sửa quoting tối thiểu tại host. Dùng một cơ chế truyền argument rõ ràng, bảo vệ cả lớp Windows và lớp shell Android. Không chỉ đổi cách đặt quote ngoài PowerShell.
14. So sánh argument cuối với đúng hằng `AUTHORIZATION_STOP_AUTHORITY`, bao gồm mọi pipe. Phải đến harness dưới dạng một giá trị duy nhất; không dùng base64 nếu callee không decode.
15. Fixture GREEN: exact class/method, serial, key-value pairs, required đủ, không duplicate/extra opt-in, đúng hash/number, stop-authority nguyên bytes. Negative: mất quote, đổi một pipe, thiếu fingerprint, sai hash, duplicate arg, class khác, stale expiry.
16. Kiểm fake process cho 4 outcome: thành công, nonzero, timeout, failure trước launch. Supervisor chỉ dispatch tối đa một lần; timeout không kích hoạt refresh/retry. Ghi kết quả numeric rõ, không suy luận từ text `True`.
17. Dùng cửa sổ host 240000 ms, auth validity 180000 ms, execution 120000 ms đúng đơn vị. Test offline không cần đợi 240 giây: dùng fake clock/process, vẫn kiểm production constants. Khi chạy bằng công cụ, yield/poll cùng process để cập nhật tiến độ; không relaunch vì host tool yield.
18. Nếu sửa launcher dùng Start-Process background, dùng chế độ hidden phù hợp; kiểm combination tham số, không để NoNewWindow xung đột WindowStyle. Không mở terminal lạ.
19. Chuẩn bị verifier outcome live độc lập với exit code: test đúng method, đúng số test, terminal; sau đó durable status/identity/report/receipt/metrics. Không yêu cầu schema 67 field của A4.2 từ method live không phát schema đó.
20. Chuẩn bị readback SQL/allowlist từ schema và store thực tế: trước claim không row mới; sau claim exact attempt và receipt atomic; lifecycle đúng attempt; reconciliation/history=0; source/binding/run/settings bất biến. Không thực thi SQL/device ở bước host.
21. Fixture outcome: OK nhưng RECOVERY_REQUIRED; OK nhưng thiếu post-readback; COMMITTED thiếu receipt; unknown cost; duplicate attempt; lifecycle không có attempt; unrelated write. Tất cả phải từ chối RAW acceptance.
22. Fixture hợp lệ: đúng một COMMITTED RAW, valid report/receipt, calls1/repair0/retry0, known cost trong cap, valid schema/finish/token/deadline, allowed DB diff và integrity/FK. Chỉ fixture này được RAW_ACCEPTED; vẫn P6_NOT_READY.

Đầu ra C: command/helper patch và evidence RED→GREEN host, không sửa APK. Local failure sửa trong cùng phase; không mở review-stop mới cho mỗi typo.

## D. Giải quyết F1 mà không giả lập quyền hoặc tài khoản

23. Tìm entry point account verification trong đúng source của test57EC99; xác định input/output, side effects, redaction và cách gọi. Không chạy nó trong lượt offline này.
24. Phân biệt hai việc: lấy expected fingerprint từ nguồn owner tin cậy; và live method tự tính actual để so sánh. Không lấy actual rồi tự khẳng định nó là account được owner phê duyệt.
25. Nếu đã có expected từ owner: ghi provenance dạng không-secret và cách truyền process-only; không yêu cầu owner gửi API key vào chat. Chứng minh normalization/UTF-8/newline giống source; kiểm lowercase vì live assert so sánh chuỗi chính xác trong khi host chấp nhận cả A–F.
26. Nếu không có expected hoặc không có đường lấy hợp lệ trên APK pin: ghi kết luận cụ thể, không lặp “pending verification”. Đưa đúng một phương án tối thiểu có thể review: input owner cần cung cấp, hoặc test-only verifier cần bổ sung, side effects, artifact/pins/permission nào phải thay đổi.
27. Không chạy harness cũ, reflection, settings pull, shell cat, logcat, sửa preferences hay bỏ assert để tạo fingerprint. Không chế giá trị 64 hex hoặc hash settings-file.
28. Nếu phải sửa AndroidTest: đề xuất diff tối thiểu trước; chưa build/install từ request offline này. Chứng minh vì sao host-only không giải quyết được, tests/redaction, wrapper build test-only và hai bản archive theo policy. Không đổi production code207. Artifact mới đồng nghĩa pin57EC99 và packet cũ không còn quyền dùng tự động; chỉ tái qualify phần chịu ảnh hưởng theo quyết định cụ thể.
29. Hoàn tất mọi công việc host độc lập với F1 trước khi hỏi owner. Nếu F1 không giải quyết được trong boundary hiện tại, kết thúc bằng thiếu đầu vào/permission nào và phương án đã chuẩn bị; không viết rằng “chỉ cần approve rồi chắc chắn chạy”.

Đầu ra D: chứng minh account path khả thi hoặc một quyết định owner cụ thể. Không đọc credential thật trong quá trình chuẩn bị.

## E. QA, phản biện rồi mới trình quyết định

30. QA lần một: kiểm C/D, artifact/evidence hashes, source-required arguments, redaction ở cả success/error/timeout, absence of provider/device action và diff scope. Chỉ chạy targeted suites chịu ảnh hưởng; không full regression/build vì sửa prose.
31. Phản biện lần hai với các câu hỏi: fingerprint expected đến từ đâu; pipe còn nguyên không; failure có log fingerprint/secret không; host exit có che non-COMMITTED không; timeout có retry không; DB mutation nào cho phép; branch/test-source/proposal-HEAD có bị lẫn; permission thực sự bao gồm thao tác account nào?
32. Sửa những lỗi mới có evidence rồi rerun kiểm tra bị ảnh hưởng. Khi cả hai vòng đạt và không còn thay đổi, dừng phân tích; không yêu cầu một vòng review thứ ba với cùng input.
33. Chỉ khi đường account khả thi, finalize packet đúng một lần: cập nhật actual baseline/branch và host command, giữ source/artifact pins nếu không đổi. Manifest không tự chứa hash; tính manifest hash → nhúng vào command → tính command hash → cập nhật proposal/references.
34. Cấm sửa silently file đã owner duyệt. Mọi byte thay đổi làm cần review bản hash mới. Không quay ID chỉ vì ngày cũ; verify unused theo pre-dispatch evidence tương lai.
35. Trình một yêu cầu quyết định gồm: exact pins + diff, RAW/GLOSSARY egress, account operation/source expected, tối đa1 primary/0repair/0retry, budgets, allowed DB writes, outcome matrix, no-redispatch. Nêu rõ chưa phê duyệt thì không dispatch. Không hỏi lại quyền sửa local đã giao.

## F. Runbook có điều kiện — chỉ sau phê duyệt packet cuối

36. Kiểm tra quyết định owner khớp manifest/command hash cuối và bao gồm account check. Không suy quyền từ A4.2 hoặc từ request nghiên cứu này.
37. Read-only xác nhận đúng serial15e84958, production code207/APK/certificate, test APK/runner exact, app không có writer cạnh tranh, schema24/integrity/FK, fresh tuple và unused lineage. Mismatch: dừng trước auth; không force-stop/install/restore để ép match.
38. Kiểm DB/settings snapshot bằng phương thức WAL-aware được phép, chỉ hash/count metadata. Không pull/log settings content hoặc raw book. Pre-live DB pin là gate; post-live dùng allowed mutation matrix.
39. Thực hiện đúng account verification đã owner cho phép; route đúng, credential nonempty, expected có provenance, actual match. Chỉ fingerprint được vào process argument; credential/endpoint không được ghi ra file/log. Nếu cách thực hiện cần đổi scope, dừng trước auth.
40. Tạo issued/expires mới ngay trước invocation, kiểm cửa sổ hợp lệ. Khởi tạo một private evidence directory unique và capture hữu hạn. Không echo argument list.
41. Dispatch đúng một method `EditorialP5EFreshRawLiveInstrumentedTest#authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn`. Không thêm preflight instrumentation riêng; method đã tự preflight trước authorization.
42. Theo dõi cùng process. Khi timeout/nonzero/mất USB, giữ evidence và durable state, trạng thái external call UNKNOWN nếu chưa chứng minh. Kill host observer không chứng minh server đã hủy. Không refresh expiry, không redispatch hoặc gọi model/provider lần hai.
43. Post-readback có chọn lọc theo schema đã review: exact attempt/receipt, lifecycle, report/receipt/metrics, immutable rows, schema/integrity/FK và package/settings. Không export raw response hoặc toàn DB vào báo cáo.
44. Phân loại: chưa claim có evidence→zero-call stop; sau claim fail→RECOVERY_REQUIRED hoặc observed unresolved; thiếu post-check→ACCEPTANCE_NOT_PROVEN; valid COMMITTED→RAW_ACCEPTED. Không gán giá0 khi cost unknown; không nhận test OK làm RAW PASS.
45. RAW accepted vẫn dừng ở RAW. Đánh giá scope RECONCILE/L1 tiếp theo từ predecessor thật; không dùng một RAW receipt để mở P6. Bất kỳ live failure nào giữ nguyên sự thật lịch sử và không tự retry.

## G. Tiêu chí kết thúc và các P phía sau

46. Kết thúc lượt host khi F2/F3 đã có evidence kiểm chứng, F1 có đường khả thi hoặc quyết định owner chính xác, packet không tự cấp quyền, snapshot có một next action. Không yêu cầu APK mới chỉ để đánh dấu hoàn tất host.
47. Cập nhật canonical current/state/snapshot/checklist theo kết quả; snapshot có version, branch, implementation/source và HEAD baseline tách biệt, build, phase, completed/pending/bugs/regression, đúng một next action. Commit tài liệu có diff thật; verify actual HEAD/status sau commit.
48. Chỉ đề nghị vào P6 sau khi P5/L1 exit có evidence thật, RAW predecessor và phần L1 còn lại được nghiệm thu, không có false-block không giải thích được hoặc recovery chưa chứng minh. Không nâng kết quả A4.2 thành L1 PASS.
49. Khi P6 được mở: triển khai theo thứ tự L2 atomic result/change-map → actual diff/declared change correspondence → L3 atomic final/receipt → five derived release numbers/no-regression → process-death/stale predecessor → ba chương đại diện. Không thực hiện P6 trong request này.
50. P7 sau P6: focused + full preserved Translation/Editorial regression/lint; build đánh số tăng so với code đã cấp cao nhất qua wrapper; hai bản immutable; device QA; benchmark/performance evidence thật và release gates trước tag/merge. Không dùng ngưỡng >169 cũ để bỏ qua code207 hoặc code lớn hơn đã tồn tại.

Lời bàn giao phải nói rõ: việc gì đã làm, evidence nào mới, việc gì chưa làm, đúng một bước kế tiếp. Không viết “toàn bộ dự án PASS”, “release hoàn tất”, “P6 ready” khi chỉ host QA đã đạt.
