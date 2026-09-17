# P5E — yêu cầu làm việc + provenance tiếp theo

Ngày: `2026-09-17`
Baseline evidence đầu vào: `c222b13cafbb0c58a868072b44ae6df17905d089`; resume HEAD mới có audit, không checkout/reset về baseline.
Branch: `feature/v4.18-p5e-audit-20260914`

## 1. Quyết định tại điểm bàn giao

Kết quả hiện tại là:

`ACCOUNT_TEST_CHECKONLY_PASS / TEST_PACKAGE_REPLACEMENT_PASS / ACCOUNT_CHECK_NOT_EXECUTED_EXPECTED_PROCESS_VALUE_MISSING / A4.3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY`.

Chưa đủ điều kiện chuyển P5/P6 hoặc phát hành A4.3. Audit phản biện đã bổ sung trạng thái `ACCOUNT_RUNNER_REPAIR_REQUIRED / EXPECTED_SOURCE_PENDING`: ngoài expected fingerprint độc lập chưa có, runner hiện còn lỗi contract cần sửa và kiểm thử offline trước mọi device run. Chỉ đủ điều kiện mở một work package hẹp để sửa/QA host runner, xác minh tính khả thi của expected fingerprint độc lập và kế thừa process-only. Work package này không được cài lại APK, không chạy RAW, không đọc credential/endpoint/fingerprint thực tế từ máy, không gọi provider, không ghi DB và không retry.

Account check chỉ được mở khi host repair/transport QA đạt và owner có expected độc lập trong đúng host process. Scope account check đã được owner duyệt; không xin lại cùng quyền đó. Nếu thiếu record/mapping, giữ `EXPECTED_PROVENANCE_UNAVAILABLE_STOP` cho nhánh device; vẫn hoàn tất sửa/QA local độc lập. Tuyệt đối không lấy actual vừa đọc trên thiết bị làm expected.

## 2. Phân tích nguyên nhân vòng lặp overthinking

Vòng lặp phát sinh do nhiều lớp tài liệu cùng chứa “next step”, trong đó lịch sử và current bị đọc ngang hàng; các nhãn local PASS/GREEN cũ từng tồn tại cạnh behavioral RED. Ngoài ra, account boundary, A4.3 authorization, RAW egress, readback/reconcile và P6 bị suy diễn như một chuỗi liên tục, khiến mỗi thiếu một input lại tạo thêm proposal hoặc retry thay vì đóng một stop condition.

Nguyên nhân trực tiếp của trạng thái hiện tại gồm hai nhóm độc lập: CheckOnly/replacement đã có bằng chứng nhưng account runner chưa launch vì expected channel không có giá trị; và review source phát hiện runner chưa bảo đảm instrumentation component đầy đủ, parser chưa chứng minh terminal success, expected đang được truyền trong ADB argv nên không đạt transport memory-only nghiêm ngặt. Đây không phải lỗi provider, DB, RAW hay thiết bị. Lịch sử trước đó gồm `DEVICE_NOT_FOUND`, installer certificate-case bug và các lỗi SQL/collector là dữ liệu regression; không được dùng để mở lại gate đã đóng hoặc biện minh cho reinstall.

Quy tắc chống lặp: fail local thì sửa trong cùng scope và chạy lại đúng test liên quan; chỉ dừng bước phụ thuộc. Không thử lại live account/RAW. Chốt một bộ bằng chứng RED→GREEN và phản biện, không mở thêm vòng cho input không đổi. MATCH chỉ chứng minh equality cùng route predicate; không chứng minh provider account validity hoặc mở P6. MISMATCH có thể do route/load exception, không chắc là sai key.

## 3. Provenance đầu vào đã xác nhận

Các hash dưới đây được tính lại từ file local không chứa secret tại HEAD nêu trên:

| Thành phần | SHA-256 | Vai trò |
|---|---|---|
| `scripts/p5e-account-check.ps1` | `0722A243C92724D59AFB7CF4B6DE674024F9BAE4FD76A712DF0210AF25B036F3` | Host runner; hash gate trước expected read/ADB |
| `scripts/p5e-raw-live-supervisor.ps1` | `364A6AA2C52A90E7AD20F28EC6C46A0EAD1BA39E8909727BEA7396287896FFE7` | Helper; import `-LibraryOnly` |
| `scripts/p5e-install-account-check-test.ps1` | `21AADE819DB83464E96C0BB6AC28CB42FB26AB916D5905AD13CED37C15FC786B` | Installer guard; đã sửa ở `afc34b87` |
| `EditorialP5EAccountCheckOnlyInstrumentedTest.java` | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` | Test-only memory boundary |
| account-check local result | `3FBE39142BA2DAA16AF6F5301271525E71FE1871CF6C14F0DF2A816346AA53EF` | CheckOnly/replacement/account-not-executed evidence |
| installer QA | `041760CD298DA06928D35D41321CF497D7DB49C25A0608D35609370884138519` | Offline QA và certificate normalization |
| replacement AndroidTest APK | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` | Đã replacement đúng một lần; không dùng để reinstall |
| runner audit probe result | `AA2E6FE3CBFB7084CCA15C5D3AA4F20A59BA9525714123D1ABAC3B863350173F` | Source-only classifier QA; 4 false acceptances |

Provenance lịch sử cần giữ nguyên: source boundary commit `9e5ffb7819bfb91dcb8ed9e25c901ab10aa48390`; installer correction commit `afc34b87158ccdd14ee109d3242fbb3c1be9ca7d`; current result commit `c222b13cafbb0c58a868072b44ae6df17905d089`. Hash thay đổi chỉ được refreeze sau khi source thực sự thay đổi; không sửa tài liệu để làm hash “xanh”.

## 4. Kế hoạch thực hiện chi tiết

### A. Khóa trạng thái và không tái diễn hành động cũ

1. Đọc `BUILD_STATE.md` rồi `WORKSPACE_SNAPSHOT.md`.
2. Xác nhận HEAD, branch và diff, sau đó đọc `GIT_WORKFLOW.md`/`DEVELOPMENT_WORKFLOW.md` trước sửa; không reset, clean, stash hoặc đụng `.idea`/evidence của agent khác.
3. Đối chiếu current banner với `P5E_OWNER_PROVENANCE_INPUT_PACKET_20260916.md`, `P5E_ACCOUNT_CHECK_LOCAL_RESULT_20260916.json` và `P5E_ACCOUNT_TEST_INSTALLER_QA_20260917.json`.
4. Ghi nhận bất biến: `installAttempts=1` cho replacement trước đó, `CheckOnly installAttempts=0`, production package untouched, `providerCalls=0`, `dbWrites=0`, `rawDispatches=0`.
5. Không gọi `p5e-install-account-check-test.ps1` nữa; không uninstall, clear-data, downgrade, reinstall hoặc pull APK lần nữa.
6. Không gọi RAW supervisor, A4.3, reconciliation, provider, endpoint, database hoặc production instrumentation.

### B. Kiểm tra feasibility của expected provenance

7. Xác định owner-controlled record độc lập bằng mã tham chiếu opaque, nhãn account/project và thời điểm xác minh; không đưa giá trị fingerprint, API key, endpoint hay settings vào chat/Git/log.
8. Kiểm tra record đó có nguồn gốc trước lần đọc actual của account runner, có người/authority xác nhận, và mapping rõ tới đúng account/endpoint scope hay không.
9. Record có thể chứa digest đã xác minh, hoặc key gốc và cấu hình endpoint độc lập để owner tự tạo digest local. Không yêu cầu người dùng tự biết sẵn digest; tên key như `xzx` không phải digest. Agent không đọc key thật. Nếu cần tool tạo digest, viết/QA bằng dữ liệu giả trước, bám đúng normalizeEndpoint Java, UTF-8, LF một ký tự và key exact bytes; không tự trim key hoặc đổi settings để khớp.
10. Nếu record không tồn tại, không độc lập hoặc mapping mơ hồ: ghi `EXPECTED_PROVENANCE_UNAVAILABLE_STOP` cho nhánh device, tiếp tục C và D.1 offline. Không yêu cầu owner cung cấp lại cùng input ở mỗi lượt, không dựng expected từ actual.
11. Chỉ sau transport repair/QA và record hợp lệ mới nạp giá trị thật vào **Process** environment của đúng host. Không dùng User/Machine, file, clipboard, command line hoặc transcript để chuyển secret. Nếu không có record, phương án xác minh khác cần quyết định thay scope; không tự đổi key trong app.
12. Host runner phải chạy trong chính PowerShell owner đã nạp biến hoặc là child được tạo từ đó. PowerShell mới không truyền env ngược vào Codex đang chạy; không yêu cầu “đặt biến ở cửa sổ khác rồi Codex sẽ thấy”. Chỉ kiểm presence/shape, không in value; dọn biến trong finally và kết thúc process riêng, không hứa zeroize được immutable string.
13. Tạo một process-only inheritance probe offline bằng giá trị giả có cùng shape; assert parent Process → child Process nhận được, User/Machine vẫn không được dùng, và probe không gọi ADB.
14. Xóa giá trị giả ngay sau probe; không dùng probe để suy ra account result.
15. Nếu không chứng minh được kế thừa process-only đến đúng host child hoặc phát hiện value đi vào command/log/file: `PROCESS_ONLY_INHERITANCE_STOP`, không launch account check.

### C. Review tính đúng của account-only runner trước launch

16. Recompute tám hash ở mục 3 ngay trước review; nếu lệch pin thì dừng `PIN_DRIFT_STOP`, không tự sửa pin trong cùng event.
17. Đọc runner để xác nhận hash-check script/helper/APK xảy ra trước khi đọc expected và trước khi tạo ADB process; ghi nhận current defect nếu expected được đặt vào argv.
18. Xác nhận helper được import bằng `-LibraryOnly`, private module không ghi đè tham số runner và không thể dispatch.
19. Xác nhận serial cố định là `15e84958`; mọi serial khác là `ACCOUNT_CHECK_SERIAL_MISMATCH_STOP`.
20. Sửa command component thành cú pháp đầy đủ `com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner` (hoặc component tương đương đã được aapt/manifest pin), rồi thêm test command builder để thiếu package hoặc sai runner bị reject trước ADB.
21. Xác nhận Java test chỉ load settings trong memory, normalize endpoint, hash `endpoint + newline + apiKey`, so sánh case-insensitive và gửi status `MATCH`/`MISMATCH`.
22. Xác nhận Java test không repository, DB, provider, RAW, authorization, response healing, retry hoặc redispatch; exception chỉ biến thành `MISMATCH`.
23. Sửa transport phù hợp scope: tránh expected trong host ADB argv/environment không cần thiết, transcript, file/log; khảo sát stdin của một shell process có giám sát, dùng fake endpoint để chứng minh exact bytes/quoting và không log. Input environment chỉ chứng minh điểm nhập, không chứng minh Android args/process metadata vô hình. Nếu không đáp ứng được policy memory-only đã duyệt, ghi cụ thể vị trí lộ metadata, phương án và tradeoff để owner quyết định đúng thay đổi scope; không xin lại quyền account check chung hoặc âm thầm nới scope.
24. Sửa parser để bắt buộc đúng một terminal success từ instrumentation (test finished/zero failures theo schema đã pin), đúng một `p5e.account.result`, exit code 0 và không có error/failure marker; chỉ regex `MATCH|MISMATCH` hoặc chỉ exit 0 là không đủ.
25. Xác nhận stdout/stderr được redact expected và mọi digest 64-hex; redaction failure là stop, không diễn giải thành mismatch.
26. Xác nhận exit non-zero, timeout, thiếu result, nhiều result, terminal failure hoặc launch failure đều là `NOT_PROVEN`, không phải `MISMATCH` và không được retry.
27. QA độc lập phải cố tình kiểm tra fake process: missing expected, malformed expected, nonzero process, timeout, missing result, duplicate result, terminal failure, malformed component và digest leakage; phải reject ở boundary gần nhất.
28. Nếu review phát hiện runner có thêm side effect hoặc không tách được account-only path, dừng `ACCOUNT_RUNNER_REVIEW_STOP`; sửa source là bắt buộc trong work package repair trước khi có event.

### D.1. Bằng chứng RED đã tái hiện và phạm vi sửa

29. Giữ nguyên audit result `docs/P5E_ACCOUNT_RUNNER_AUDIT_RESULT_20260917.json`: source-only, không process launch, không environment read, không device. Kết quả có `classifierUnexpectedAcceptances=4` cho `match_then_failure`, `truncated_after_match`, `wrong_test_identity` và `token_suffix`.
30. Sửa identity ở cả account runner và helper dùng cho RAW tương lai; helper hiện cũng có `$script:P5ERunner='androidx.test.runner.AndroidJUnitRunner'` nên không được coi account fix là đã lan truyền sang RAW. Mọi propagation phải có source diff, pin mới và QA riêng; không được chạy RAW trong request này.
31. Thêm test chứng minh parser không chấp nhận `MATCH` nếu instrumentation failure/terminal marker xuất hiện sau đó, nếu class/method sai, hoặc nếu result có suffix; test phải kiểm exact identity và terminal lifecycle.
32. Đánh giá rủi ro timeout: sau kill, `GetAwaiter().GetResult()` có thể vẫn chờ task nếu stream đóng không hoàn tất. Tạo fake process offline để chứng minh bounded completion; nếu không chứng minh được, dừng `ACCOUNT_RUNNER_TIMEOUT_BOUNDARY_STOP`.

### D. Một lần account check có kiểm soát

33. Nhánh này là điều kiện sau, không tự chạy trong host-only repair: chỉ khi toàn bộ local QA/phản biện E đạt, transport đúng scope, expected hợp lệ và quyền account check đã có vẫn áp dụng, ghi provenance receipt không chứa value. Không tạo RAW authorization.
34. Xác nhận không có competing process/writer và cửa sổ hành động còn hiệu lực; không boot/reinstall để đạt điều kiện.
35. Launch đúng một host runner với expected ở Process environment inherited; chỉ thực hiện sau khi transport repair đã chứng minh expected không đi vào argv.
36. Ghi `launchCount`, timeout, exit code, terminal status và result token; không ghi actual/expected/endpoint/key.
37. Nếu result là `MATCH` hoặc `MISMATCH` đúng một lần cùng terminal success, lưu typed outcome và counts; không chạy lại để xác nhận.
38. Nếu result thiếu, duplicate, terminal failure, timeout, non-zero, redaction fail hoặc process chết: `ACCOUNT_CHECK_NOT_PROVEN_STOP`; không đổi ID, refresh expected, retry hay redispatch.
39. `MATCH` chỉ cập nhật `ACCOUNT_CHECK_COMPLETED_MATCH`; nó không issue A4.3, không authorize RAW, không claim P5 exit và không làm P6 ready.
40. `MISMATCH` là `ACCOUNT_CHECK_COMPLETED_MISMATCH`; dừng mọi bước sau và yêu cầu owner xử lý provenance/mapping riêng.

### E. QA và phản biện trước khi xuất tài liệu

41. Vòng QA 1 đối chiếu component đầy đủ, terminal instrumentation success, result với runner output, process launch count và hash pins; xác nhận đúng một lần và không có secret shape trong evidence.
42. Vòng QA 2 dùng evidence đã đóng, thử đổi expected giả, serial, APK hash, helper hash, process inheritance, argv inspection, malformed component, terminal failure, timeout và result duplicate; mỗi biến thể phải stop và không side effect.
43. Phản biện logic: kiểm tra rằng local `MATCH` không bị mô tả thành provider account validity; replacement PASS không bị mô tả thành account check PASS; `DEVICE_NOT_FOUND` lịch sử không bị nâng thành current blocker; expected thiếu và runner defect được báo cáo tách biệt.
44. Phản biện provenance: kiểm tra expected không lấy từ actual, không dùng historical APK pin thay replacement pin, và hash table khớp file thực tế.
45. Phản biện tiến trình: kiểm tra không có reinstall, second ADB account launch, RAW, DB/provider call, retry hoặc redispatch.
46. Chạy `git diff --check`, secret scan trên tài liệu/result không chứa secret và kiểm tra JSON schema; không build APK.
47. Chỉ cập nhật current result/snapshot khi có evidence mới; giữ nguyên toàn bộ historical failure, không sửa lịch sử thành PASS.
48. Xuất báo cáo với năm trạng thái tách biệt: host runner repair, expected provenance, account boundary, A4.3/RAW/P5 và P6. Không dùng một nhãn “hoàn tất” chung.

## 5. Ma trận stop bắt buộc

| Phát hiện | Hành động |
|---|---|
| Không có record expected độc lập | `EXPECTED_PROVENANCE_UNAVAILABLE_STOP`; không launch |
| Record không mapping đúng account/endpoint scope | Dừng owner review; không dựng expected |
| Expected đi qua User/Machine/file/command line/log | `PROCESS_ONLY_CHANNEL_STOP`; không launch |
| Component chỉ là runner class, thiếu package | `ACCOUNT_RUNNER_REPAIR_REQUIRED`; không ADB |
| MATCH xuất hiện trước terminal failure hoặc sai identity vẫn được nhận | `ACCOUNT_RUNNER_REPAIR_REQUIRED`; không ADB |
| Timeout kill không chứng minh bounded completion | `ACCOUNT_RUNNER_TIMEOUT_BOUNDARY_STOP` |
| Hash/script/APK/certificate/package lệch | `PIN_DRIFT_STOP`; không reinstall |
| Runner có DB/provider/RAW side effect | `ACCOUNT_RUNNER_REVIEW_STOP` |
| Timeout/non-zero/missing/duplicate result | `ACCOUNT_CHECK_NOT_PROVEN_STOP`; không retry |
| Redaction leak hoặc digest xuất hiện | `ACCOUNT_CHECK_REDACTION_FAILURE`; giữ evidence an toàn |
| `MATCH` | Chỉ account equality; P5/P6 vẫn chưa sẵn sàng |
| `MISMATCH` | Dừng; owner xử lý provenance, không retry |

## 6. Tiêu chí hoàn thành và bàn giao

Deliverable bắt buộc của work package trước mắt là host repair + test nguồn thật + phản biện + pin mới + hướng dẫn owner nhập local đã QA giả. Có thể hoàn tất deliverable này dù owner chưa có expected. Nhánh account-device là conditional follow-on, không nằm trong PASS local. Nếu local đã đạt nhưng thiếu record, ghi `BLOCKED_EXTERNAL_EXPECTED_PROVENANCE`, không tuyên bố account failed và không tạo thêm vòng audit/build. Baseline hiện tại vẫn `ACCOUNT_RUNNER_REPAIR_REQUIRED / EXPECTED_SOURCE_PENDING`.

Sau PASS, packet tiếp theo phải ghi rõ result account, nguồn provenance bằng mã tham chiếu, hash runner/helper/APK, launch count, provider/DB/RAW counts và các gate vẫn đóng. `A4.3_NOT_ISSUED`, `RAW_NOT_RUN`, `P5_EXIT_NOT_CLAIMED`, `P6_NOT_READY` phải được giữ nguyên cho tới khi có authorization và work package riêng.

Tài liệu này là request bounded và provenance review; không phải authorization cho device operation, RAW, provider, database, release, tag, merge hay P6.

## 7. Input tối thiểu cần owner trả lời

Owner chỉ cần xác nhận bằng metadata, không gửi secret: **còn giữ bản ghi gốc độc lập của credential/account đã dùng để xác minh expected fingerprint hay không**, bản ghi đó do authority nào quản lý, mã tham chiếu là gì, và mapping account/project nào. Nếu câu trả lời là không hoặc không thể chứng minh mapping, giữ `EXPECTED_SOURCE_PENDING` và dừng; không yêu cầu API key, endpoint, actual fingerprint hay ảnh chụp màn hình.

Để trả lời ban đầu, chỉ cần: “Tôi còn key gốc ở nơi lưu riêng” hoặc “Key chỉ còn trong app”. Không bắt owner tự viết hash hoặc hiểu enrollment record. Metadata mapping được điền tiếp từ record có thật; chưa xác minh thì ghi NOT_VERIFIED, không tự lấy ngày tạo key làm ngày xác minh.

## 8. Các lỗi cũ phải được bao phủ nhưng không chạy lại vô cớ

| Lỗi/incident | Ràng buộc của request |
|---|---|
| code196 mất data; connected installer hạ version | Không connectedAndroidTest, install production, clear, uninstall, reset/downgrade. Không tuyên bố data gốc đã phục hồi; giữ RECONSTRUCTED_ONLY. |
| A3.2 model/route mismatch, preservation gap | MISMATCH không tự sửa model/key/settings. Giữ gap lịch sử; package PASS không thay DB preservation proof. |
| DEVICE_NOT_FOUND trước đây | Chỉ là lịch sử. Nếu device không có ở event sau: dừng; không reinstall/auto reconnect-loop. |
| Certificate khác hoa/thường | Fixture phải gọi chính hàm so certificate đã sửa, không tự so một biểu thức khác rồi gọi PASS. Installer không chạy lại trong request này. |
| SQL schema24/LINEAGE17/INPUT7/NULL/golden | Đã đóng bằng behavioral evidence; chỉ chạy lại test liên quan nếu sửa helper tác động code đó. |
| RAW truncate/timeout/unknown, approval consumed | Không repair/retry/fallback/reconcile hoặc refresh ID. Readback cùng event theo scope sau này; không suy billing=0 từ failure. |
| Runner/parser false-green mới | Command token thật và classifier source thật phải có test positive/negative, terminal + identity + exact token. |
| Hash của helper sửa ảnh hưởng command | Refreeze helper → command reference → command hash → packet/current docs một lần; giữ result cũ theo baseline. |
| Test APK account pin khác RAW/A4 pin | Không dùng installed 058BE… thay pin RAW 57EC…; account-only selection không chứng nhận toàn APK cho RAW. Mọi chuyển artifact tương lai phải kiểm riêng trong scope sau. |

Sau account MATCH, bước sau là đánh giá packet A4.3 hiện hành, scope readback và một RAW/GLOSSARY call theo manifest; không tự mở P6. DRAFT/PRONOUN vẫn ẩn, primary=1, retry/repair=0; giữ caps và window trong manifest trừ khi owner đổi scope. Chỉ đánh giá P5 exit/P6 sau outcome, report/receipt, DB/readback, predecessor và đầy đủ tiêu chí canonical. Request này không cấp quyền các bước đó.

## 9. QA/phản biện của chính request này

Parent đã sửa bản nháp: bỏ điều kiện “chỉ cần expected”, không bắt có digest sẵn
khi owner còn record key gốc, cho phép sửa offline dù input owner thiếu, sửa
thứ tự startup và số bước, giữ quyền account đã nhận và tách local deliverable
khỏi device follow-on. Luna review độc lập probe/audit xác nhận bốn false
accept; timeout/transport là source risk, chưa là runtime reproduction.
QA cuối kiểm pin/probe, reference, diff guard, JSON và checklist chưa đánh dấu.
