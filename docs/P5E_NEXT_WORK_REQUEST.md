# Yêu cầu làm việc + provenance — tiếp tục P5E sau host preparation

**Baseline bàn giao để đọc:** `9beafef8e59825714e47cdfe594dc287b6c5a0ce`, branch `feature/v4.18-p5e-audit-20260914`. Khi resume lấy actual HEAD mới từ Git và snapshot; không reset về hash này. Canonical plan vẫn là `EDITORIAL_RECOVERY_V4_18.md`, checklist release vẫn `release_checklists/v4.18-editorial-v5-safe-4-1-3.md`.

**Kết luận:** chưa chuyển P6, chưa dispatch A4.3. Đọc `docs/P5E_PROVENANCE_REVIEW_20260915.md` và result JSON trước khi làm. F2 đã sửa; F3 có bốn false-accept mới, thiếu readback producer được chứng minh và failure redaction cần bổ sung. Đây là sửa local trong P5E, không phải một A4.x/release/checklist mới.

## Phạm vi và đầu ra cần hoàn thành

Khi owner giao thực hiện request này: sửa host helper/collector/tests và tài liệu liên quan đến các lỗi đã chứng minh; chuẩn bị provenance F1. Chưa được phép credential/account check thật, ADB/device, instrumentation, provider, runtime authorization, DB readback thật, APK build/install, migration hoặc thay đổi production/route/model/budget/pack/profile/source. Phần live cuối là runbook có điều kiện, không phải authorization.

Đầu ra: patch local tối thiểu; RED→GREEN 4 mutation và failure redaction; một đường tạo readback có source mapping và fixture xuyên producer→verifier; packet/provenance không-secret đủ review; snapshot một next action. Không chờ owner để sửa phần host độc lập.

## A. Baseline và giữ phần đã đạt

1. Vào đúng worktree con `D:\App Translate Books\App Translate Books-translation-profile`. Đọc BUILD_STATE, snapshot theo startup owner; rồi canonical plan, Git/development workflow. Không sửa checkout D1 ở thư mục cha.
2. Ghi actual branch/HEAD/status; xác nhận ancestor 9beafef8 và đọc diff mới. Nếu có diff của người khác, phân loại và giữ nguyên; không reset/clean. Resume branch hiện tại theo scope đã bàn giao, không tạo release track mới.
3. Kiểm manifest DD58…4501, command 1D9A…DA15E, helper BEEF…6799 và host report C54…E3C bằng full SHA trong provenance table của audit. Nếu khác, đọc diff để xác định baseline; không chạy probe pin cũ rồi bỏ guard để ép pass.
4. Kiểm code207/test57EC99/source d51b7f3c và parity production5/test8 khi byte artifact thay đổi hoặc trước owner packet cuối. Không rebuild/cài lại để xác nhận trạng thái build-time `installed=false`.
5. Giữ F2 đóng theo evidence; source Android phải không đổi. Không làm lại A4.2, model remediation, IPC/AVD/CP6 hay full JVM suite cho sửa prose. Checklist 05–09 release vẫn chưa được đánh dấu từ host-only PASS.

## B. Sửa F3 theo evidence mới

6. Đọc `P5E_PROVENANCE_REVIEW_PROBE.ps1`; tái hiện đúng helper baseline trên dữ liệu synthetic, không chạy Dispatch hoặc đọc biến fingerprint thật. Giữ hai control đúng và ghi bốn false accept là RED, không viết 6/6 PASS.
7. Vẽ bảng thời gian theo source: issued ≤ claim/consume < expires; attempt created≤updated; observation không được trước dữ liệu nó chứng thực. Post-readback sau expiry có thể hợp lệ; không chặn vì authorization đã hết hạn sau khi call/commit hợp lệ kết thúc.
8. Sửa check consume upper bound theo `EditorialP5PilotAuthorization` (`now>=expires` invalid). Fixtures: issued-1, issued, expires-1, expires, expires+1. Bổ sung observedAt trước run/before updated và observation muộn hợp lệ. Không dùng giờ hiện tại của audit để loại evidence lịch sử hợp lệ.
9. Truy nguồn manifestFingerprint đúng từ request/binding/model artifact serializer. Kiểm report và receipt cùng identity với manifest cần pin; không chỉ đúng dạng 64 hex hoặc chỉ bằng nhau. Fixture: một bên khác, cả hai giống nhau nhưng sai expected, đúng expected.
10. Ràng buộc metadata/readback với đúng event thật và file đã mở: canonical path trong evidence directory đã chọn, source input hash, collector implementation identity, run identity/timestamps. Thay metadata.evidenceDirectory bằng chuỗi khác phải fail. Không tạo hash tự tham chiếu hoặc một hệ thống ký/chứng thực mới.
11. Không cho fixture với đúng mọi booleans thay provenance. Khi chưa có evidence producer, verifier phải trả ACCEPTANCE_NOT_PROVEN; không đổi field UNKNOWN thành true để hợp schema.
12. Kiểm validator không phụ thuộc vào duy nhất `OK (1 test)`/exit0: missing postcheck, RECOVERY_REQUIRED, timeout, expired consume, wrong event, mismatched manifest đều không được RAW_ACCEPTED. P6 luôn false trong RAW-only verifier.

## C. Làm producer/readback cụ thể, không chỉ schema nhận JSON

13. Tìm đường read-only có sẵn trên đúng baseline đã pin và các host tools hiện có. Kết quả phải là file/function/entry point, input, output, side effects và command plan có thể review; không ghi chung “lấy DB readback sau đó”. Không chạy trên device trong lượt host.
14. Nếu chưa có collector, implement tối thiểu ở host đọc snapshot/metadata được cung cấp qua quy trình đã cho phép; dùng DB/report/receipt giả hoặc disposable fixture offline. Không mở DB pilot thật và không yêu cầu owner tự điền booleans.
15. Lập bảng field→nguồn→biến đổi→gate theo mẫu ở phần provenance bên dưới; bao phủ mọi required field của p5e.raw.readback.v1. Không tự thêm field giả để đáp ứng verifier. Field không thể chứng minh phải UNKNOWN và gate tương ứng chưa đạt.
16. Xác định rõ phạm vi dữ liệu cần thu: package APK/cert; schema/integrity/FK; exact immutable tuple; exact attempt/authorization/lifecycle; actual stored report/receipt bytes để validator tính hash/size; settings chỉ hash/change proof theo quyền. Không đưa toàn DB, source text, prompt hoặc raw response vào Git/báo cáo.
17. WAL/consistent snapshot: lựa chọn phương thức đang được runbook cho phép, không copy riêng DB khi WAL chưa xử lý. Trước dispatch phải chuẩn bị được collector; không chờ live chạy xong mới phát hiện thiếu phương thức readback.
18. Bằng chứng atomicity phải tách nguồn: code/transaction tests chứng minh semantics; before/after event kiểm row-pair/no partial state; bytes validators kiểm integrity. Không tự suy atomicClaim=true từ một ảnh chụp cuối đơn lẻ.
19. Tạo fixture xuyên producer→verifier từ DB/schema và serialized artifact thực tế (synthetic content). Điều kiện GREEN: producer tạo file được verifier chấp nhận, và file có provenance input hash/collector/run đúng. Không gọi `New-P5EValidReadbackFixture` làm producer thật.
20. Negative xuyên boundary: thiếu row, orphan lifecycle, duplicate attempt, sai event, schema drift, WAL/incomplete snapshot, missing report/receipt, modified immutable tuple, wrong source hash, UNKNOWN cost, invalid validator output. Trả lỗi typed, không retry/provider/fix DB.
21. Nếu pin APK không có phương thức thu tối thiểu cần thiết: hoàn tất mọi phần host có thể làm, ghi đúng missing entry point và đề xuất test-only delta. Không tự build/install, không dùng harness lịch sử, reflection, shell pull settings hoặc bypass pin. Chỉ tái qualification phần chịu ảnh hưởng nếu owner cấp scope riêng; không mặc định reset toàn A4.

## D. Account provenance và redaction

22. Đọc phép tính từ source: `SHA256(UTF8(normalizeEndpoint(baseUrl) + "\n" + apiKey))`; lowercase hex. Phân biệt expected do nguồn owner tin cậy với actual do app tính. Fingerprint không phải account ID và không chứng minh billing/ownership nếu thiếu mapping.
23. Với fixture fake, kiểm route normalization đúng source: trim endpoint, bỏ một slash cuối, /v1→/chat/completions; không tự trim credential ngoài source. Không dùng raw endpoint/credential thật để debug.
24. Sửa capture khi String assertion mismatch mang expected/actual fingerprint: redacted failure metadata còn đủ chẩn đoán nhưng không giữ digest bị policy cấm log. Test fake assertion/ComparisonFailure, exception wrapper, success, error và timeout. Không tuyên bố đây là credential leak đã quan sát.
25. Làm rõ nơi được phép tồn tại fingerprint: process argument; durable receipt/readback nếu manifest/schema yêu cầu và owner scope cho phép. Logs/chat không chứa fingerprint nếu packet cấm. Không đặt hai yêu cầu mâu thuẫn “không serialize fingerprint ở đâu cả” và “readback phải có endpointAccountFingerprint”.
26. Chuẩn bị bảng owner input dưới đây ở trạng thái NOT_PROVIDED/NOT_APPROVED. Nếu owner đã có expected đáng tin, dùng provenance xác minh nguồn tạo ra và mapping account/key; không xin API key trong chat và không đọc password manager/credential store bằng agent.
27. Nếu owner chưa có expected: không lặp lại yêu cầu bất khả thi “cung cấp provenance” mà không có cách tạo. Đưa đúng phương án tối thiểu: source/entry point enrollment, owner attestation cho account/key mapping, phép tính device-only, đường chuyển digest process-only và scope cần cho phép. Nếu phải thay AndroidTest, đề xuất đó riêng với lý do kỹ thuật; hiện tại chưa được chạy.
28. Không lấy actual của live assertion thất bại làm expected mới rồi gọi lại. Không chạy thử một RAW call để học account. Không tự chấp thuận provenance từ chỉ một chuỗi 64 hex; không giả người dùng đã xác nhận.

## E. QA + phản biện, rồi chốt packet một lần

29. QA vòng một: 2 control và 4 mutation cũ đúng expected sau sửa; fixture mới producer→verifier đúng; no-secret failure; no Android/source/budget drift. Chạy suite host F2/F3 hiện có một lần sau final helper patch để kiểm regression, không sửa lại F2 khi đang đạt.
30. Phản biện vòng hai: nguồn từng bool/hash là gì; fixture có độc lập với verifier không; expired consume lọt không; report/receipt đúng manifest của ai; event có bị tráo không; capture có ghi digest assertion không; device collector có thật trên pin không; owner input có tự tạo trust vòng tròn không?
31. Với lỗi mới có evidence, sửa local và rerun các checks bị ảnh hưởng trên cùng branch. Khi hai vòng đạt, không mở thêm vòng cùng input. Chưa đạt phải ghi FAILED_REPAIRING, không đẩy toàn bộ trách nhiệm sang owner.
32. Freeze command/helper cuối và source mapping; update proposal/hash references nhất quán. Manifest chỉ đổi nếu scope/operation thay đổi, và phải giữ bản cũ qua Git. Thứ tự: finalize manifest→hash manifest→command/helper references→hash command/helper→human proposal/provenance. Không tự chứa hash của chính file.
33. Current owner decision phải bind đủ manifest, command và helper hiện hành; manifest DD58 pin cũ không tự duyệt mọi helper tương lai. Không yêu cầu approval trên packet đang thay đổi hoặc claim latest commit bằng hash tự tham chiếu.
34. Chỉ sau khi local readiness đạt, trình một yêu cầu owner cụ thể với source expected, permission account check, RAW/GLOSSARY egress, đúng route/caps, allowed DB effects và no-redispatch. Account approval đơn lẻ không tự cấp quyền gửi sách; user audit request không phải approval.

## F. Runbook live có điều kiện — không thực hiện từ request host này

35. Chỉ vào cửa sổ live sau explicit owner approval khớp packet cuối, provenance F1 đầy đủ và local QA đạt. Current read-only device pins phải được kiểm trong phạm vi owner cho phép: serial15e84958, production code207/cert, test exact, schema24, immutable tuple, unused ID và không writer cạnh tranh. Mismatch dừng; không force-stop/uninstall/clear/downgrade/install để ép PASS.
36. Account operation đúng quyền, actual=trusted expected; fresh issued/expires ngay trước một dispatch. Chỉ RAW/GLOSSARY visible; DRAFT/PRONOUN hidden. Caps: primary1, repair0, retry0, input100000/output4096/total104096, cost0.05, execution120000ms/auth180000ms/host240000ms. Không thay cap theo số token/giá phỏng đoán.
37. Chạy đúng selected live method một lần, không preflight instrumentation riêng hoặc model remediation. Theo dõi cùng process khi tool yield, không relaunch. Timeout/USB loss/nonzero giữ UNKNOWN nếu chưa có evidence; kill observer không chứng minh provider đã hủy. Không refresh expiry hoặc redispatch.
38. Dùng collector đã chuẩn bị lấy evidence sau run đúng event, kiểm allowed DB effects thay equality hash toàn DB. Chỉ exact attempt+receipt claim atomic, allowlisted lifecycle/validated RAW artifacts; reconciliation/history=0; immutable source/binding/run/settings không đổi. Missing post-check→ACCEPTANCE_NOT_PROVEN, không biến delayed readback thành immediate preservation.
39. RAW_ACCEPTED chỉ khi actual durable COMMITTED + validated bytes/metrics/provenance đúng. Unknown cost không bằng0; test OK không đủ. RAW accepted vẫn không mở P6; đánh giá RECONCILE/L1 scope/predecessor riêng. Giữ code196 RECONSTRUCTED_ONLY và A3.2 historical gaps đúng sự thật.
40. Cập nhật state/snapshot/checklist với observed results, actual HEAD và một next action; commit có diff thật. Không tag/merge/release. P6 chỉ sau P5 exit; ba chương L1-L3 thuộc exit P6; P7 mới regression/build/QA/archive theo wrapper và versionCode tăng đúng policy.

## Provenance bắt buộc của đầu ra kỹ thuật

| Nhóm | Nguồn có thể kiểm chứng | Quy tắc |
|---|---|---|
| Code/artifact | Git ref + APK/source ZIP hashes + event manifest | Tách production source, test source, host source, proposal và actual HEAD |
| Invocation | exact command/helper hashes + selected method + event metadata | Không chỉ ghi hash manifest; không dùng metadata event khác |
| Account expected | owner-supplied trusted record/procedure + account/key mapping | Không chép digest từ actual mismatch, settings-file hash hoặc fixture |
| Account actual | selected device-only calculation after approval | Chỉ match proof/metadata được phép; không log credential/endpoint |
| Package | installed read-only readback + cert/APK hash | Host artifact tồn tại không tự chứng minh installed APK |
| DB tuple/counts | consistent snapshot identity + exact query/collector | Global và lineage counts đúng ownership; không sửa query bằng cách sửa DB |
| Report/receipt | stored bytes → real validator → hash/size/schema/identity | `validationPassed=true` phải là kết quả validator, không giá trị nhập tay |
| Atomicity | source transaction tests + observed row consistency | Ghi rõ loại evidence; snapshot cuối không chứng minh toàn bộ lịch sử |
| Cost/lifecycle | exact persisted metrics/metadata of attempt | Unknown giữ unknown, không fallback0; không lưu raw model response |
| Timing/event | host event và durable timestamps/source hashes | Consume trong auth window; readback phải có chronology hợp lệ, không buộc trước expiry |

## Mẫu owner provenance — nội dung cần cung cấp, không phải authorization đã ký

Giữ template này NOT_PROVIDED cho tới khi nhận dữ liệu thật; không lưu actual fingerprint vào Git nếu policy process-only. Owner không gửi API key/credential/raw endpoint qua chat.

| Trường | Hiện trạng / nội dung cần có |
|---|---|
| Decision | NOT_APPROVED |
| Owner/authorized operator | NOT_PROVIDED — ai chịu trách nhiệm xác nhận account và phạm vi |
| Account/project label | NOT_PROVIDED — nhãn không-secret đủ phân biệt tài khoản được phép |
| Credential reference | NOT_PROVIDED — nhãn/version tham chiếu, không giá trị credential |
| Expected source type | NOT_PROVIDED — trusted prior record hoặc separately approved enrollment |
| Source reference + created/verified time | NOT_PROVIDED — đường dẫn/ref không-secret, thời điểm và cách kiểm chứng |
| Algorithm/version | Source-defined SHA256/UTF8/normalizeEndpoint/newline, pin exact implementation |
| Account mapping attestation | NOT_PROVIDED — căn cứ owner biết credential reference thuộc account/project được chọn |
| Fingerprint transport | NOT_PROVIDED — owner-controlled process environment, lowercase64hex; không chat/log/Git |
| Credential rotation since verification | UNKNOWN — nếu đổi key/endpoint phải reverify, không dùng provenance cũ |
| Approved account operation | NOT_APPROVED — exact device-only load/normalize/hash/compare, side effects và redaction |
| Approved data egress | NOT_APPROVED — RAW/GLOSSARY only; one primary, no retry/repair/RECONCILE |
| Approved artifact/code/command/helper refs | PENDING_FINAL_LOCAL_QA — điền full hashes cuối, không tự copy pin đã thay đổi |
| Validity and stop conditions | PENDING_DECISION — expiry, mismatch, unavailable device, unknown outcome và no-redispatch |

Không yêu cầu owner mua quyền/công cụ mới nếu nguồn sẵn có đáp ứng. Không tạo owner receipt phức tạp cho công việc sửa host thông thường. Mẫu trên chỉ giải quyết trust và permission của account/live boundary đã tồn tại.

## Tiêu chí bàn giao để tránh lặp

Bàn giao local: producer có phương thức cụ thể, các RED mới đã GREEN, old suite không regression, provenance template không giả dữ kiện, packet cuối có hash, snapshot một action. Nếu thiếu owner input, nói chính xác thiếu trường nào và đưa phương án đã chuẩn bị; không viết lại plan giống hệt. Nếu local vẫn fail, tiếp tục sửa local, không bảo owner “approve trước rồi sẽ tính”.
