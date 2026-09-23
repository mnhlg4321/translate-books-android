# P5E — yêu cầu công việc và provenance kế tiếp

> **Loại công việc:** QA local loader và một bước owner-local Process-only.
> **Không phải:** lệnh chạy thiết bị, kiểm tra tài khoản, A4.3, RAW, P5 exit hay P6.

## Quyết định hiện tại

Tại pre-snapshot baseline `7a64b71acb1dafa32cdfe3d99c13e353589b19ab`, phần sửa host runner ở
`70fe4b1b9820997bd345da2c3cdd689a63b9378e` đã được kiểm lại. Owner đã trả lời
đủ metadata provenance và còn giữ key gốc ngoài app. Loader source-derived đã
PASS 26/26 với input giả, nhưng chưa nạp expected value vào host `Process`.

| Gate | Trạng thái |
| --- | --- |
| Host runner / parser / transport fake-process | `HOST_RUNNER_REPAIR_OFFLINE_PASS` |
| Provenance expected độc lập | `EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW` |
| Local expected-value loader | `EXPECTED_VALUE_LOADER_OFFLINE_PASS / EXPECTED_VALUE_PROCESS_LOAD_PENDING` |
| Account check trên thiết bị | `ACCOUNT_CHECK_NOT_EXECUTED` |
| A4.3, RAW và P5 exit | `A4.3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED` |
| P6 | `P6_NOT_READY` |

Vì vậy, **chỉ bước owner-local Process-only có thể mở tiếp**. Loader QA không tự
launch account check; chưa đủ điều kiện chuyển P5 exit hoặc P6.

## Metadata owner đã nhận — decision typed, không có secret

```text
originalKeyAvailability=RETAINED_OUTSIDE_APP
recordAuthority=OpenRouter Default Workspace / API Keys
recordReference=OpenRouter dashboard / Default Workspace / API Keys / xzx
accountOrProjectMapping=OpenRouter Default Workspace / App Translate Books
verificationTime=2026-07-13 Asia/Ho_Chi_Minh (date only; hour not retained)
recordPredatesActualRead=YES
endpointScopeMapping=YES
```

Decision: `EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW`. Đây là owner metadata
assertion, không phải provider/endpoint verification. `xzx` là nhãn record,
không phải expected digest; agent không đọc, lưu hoặc nhận digest. Remaining
gate là `EXPECTED_VALUE_PROCESS_LOAD_PENDING`, rồi mới xem xét một follow-on
account check riêng với tối đa một launch. Không có ADB/device/provider/DB/RAW
action trong package này. Chi tiết hash và thao tác owner-local duy nhất nằm ở
`docs/P5E_EXPECTED_VALUE_LOADER_REVIEW_20260923.md`.

## Vì sao công việc bị lặp và kéo dài

1. Các tài liệu current từng giữ nhãn của audit cũ là
   `ACCOUNT_RUNNER_REPAIR_REQUIRED` sau khi repair đã PASS. Điều đó khiến
   cùng một sửa chữa bị xem như việc chưa làm.
2. Một work package repair từng được ghi như một release branch/checklist
   riêng. Nó làm lệch quy tắc một release chỉ có một canonical plan và một
   release checklist.
3. Bốn gate khác nhau bị trộn thành một câu “sẵn sàng”: sửa runner local,
   provenance expected, account check, rồi A4.3/RAW/P6. PASS ở gate trước
   không chứng minh gate sau.
4. Audit trước repair đã có bốn false-green parser cases. Các lượt sau phải
   sửa và QA đúng lỗi đó, nhưng các tài liệu cũ vẫn tiếp tục được dùng như
   lệnh thực thi.
5. “Process-only” trước đây được diễn đạt quá rộng. Sự thật cần giữ là: raw
   credential không được chia sẻ; digest không có trong host `adb` argv hay
   child environment. Sau stdin tới Android shell, digest vẫn được cấp cho
   instrumentation như một extra tạm thời. Nếu mức lộ digest này không được
   owner chấp nhận, phải dừng và thiết kế lại trước mọi device run.

Cách chấm dứt vòng lặp là dùng một bảng trạng thái hiện hành duy nhất, chỉ
mở đúng một gate còn thiếu, và đóng ngay khi input owner không đổi. Không tạo
branch, checklist, build, reinstall hoặc audit mới để “thử lại” một metadata
chưa thay đổi.

## Provenance đã kiểm

| Thành phần | SHA-256 / commit | Vai trò |
| --- | --- | --- |
| Repair implementation | `70fe4b1b9820997bd345da2c3cdd689a63b9378e` | baseline sửa runner/helper và QA |
| Snapshot HEAD | `c6173e317a733e13080f041fdc152fe5fe08f4e8` | snapshot sau repair |
| Account runner | `96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65` | full component, stdin, parser fail-closed |
| RAW helper, future-only | `5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34` | component and bounded capture; không chạy RAW |
| Offline QA | `D8940D498AB9DABBBFED4A0A31013448622E266D30ACBC2AEFF7C9E96FEF82D7` | 37/37 fake-process assertions |
| Account test source | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` | test-only account comparison boundary |
| Account test APK, historical replacement | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` | đã replacement đúng một lần; không reinstall |
| Repair result | `D72483FC31409424CB95EB0575D839CCC239B8BC8A84FAE8BEDCAC398C2773AC` | immutable baseline sau metadata owner, trước loader |
| Expected-value loader | `28A6B0AC8669FC56D19C5C1C256E0F4EA1C0E8D693CC319A4FE8603FC165C078` | fixed-endpoint, hidden-prompt Process-only loader |
| Loader QA | `16B6ADF1694E541AEAD5197CDD1A060AF165D7A24A7BB9831836F87F924297C5` | 26/26 synthetic offline PASS |

QA được chạy lại từ source hiện tại bằng
`scripts/test-p5e-account-runner-repair.ps1` với output tạm ngoài repository:
`37/37 PASS`, `fakeAdbCalls=0`, `deviceActions=0`, `providerCalls=0`,
`dbWrites=0`, `rawDispatches=0`. Đây là bằng chứng local; nó không thay thế
một device run.

## Input owner còn cần gửi

Không cần gửi thêm metadata. Owner đã xác nhận record độc lập và còn giữ key
gốc ngoài app; decision `EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW` vẫn giữ
nguyên. Không gửi API key, endpoint, fingerprint, digest, ảnh màn hình hoặc
transcript vào chat, Git hay evidence chung.

## Yêu cầu công việc hiện tại: P5E owner-local expected-value load

Đây là một bước chủ sở hữu tự làm, không phải event thiết bị. Thực hiện đúng
một lần trong window PowerShell riêng do owner kiểm soát.

1. Mở `docs/P5E_EXPECTED_VALUE_LOADER_REVIEW_20260923.md`. Xác nhận loader
   hash `28A6B0AC8669FC56D19C5C1C256E0F4EA1C0E8D693CC319A4FE8603FC165C078`
   và QA hash `16B6ADF1694E541AEAD5197CDD1A060AF165D7A24A7BB9831836F87F924297C5`.
   Hash lệch là `PIN_DRIFT_STOP`; không sửa file, build hay chạy device.
2. Mở một Windows PowerShell window riêng, chuyển tới
   `D:\App Translate Books`, rồi chạy trực tiếp
   `.\scripts\p5e-load-expected-digest.ps1`. Không dùng `powershell -File`
   ở process khác, không truyền endpoint/key qua command line và không dùng
   clipboard/transcript.
3. Ở hidden prompt, owner nhập exact original key một lần. Chỉ dùng auto-type
   của password manager khi nó không dùng operating-system clipboard. Script tự
   dùng exact OpenRouter endpoint được source P5E yêu cầu, chuẩn hoá theo Java
   semantics, thêm đúng một LF và không trim key.
4. Chấp nhận duy nhất signal
   `P5E_EXPECTED_VALUE_PROCESS_LOAD=PASS`. Không gửi digest hoặc ảnh màn hình;
   chỉ phản hồi `đã nạp` và giữ nguyên window PowerShell đó mở.
5. Không launch account runner, ADB, provider, database, A4.3, RAW hoặc P6.
   Sau signal, tạo **một request follow-on riêng** cho account check. Scope
   `OWNER_ACCOUNT_CHECK_SCOPE_RECEIVED` hiện có không tự launch event.
6. Nếu helper stop hoặc owner muốn pause, chạy
   `.\scripts\p5e-load-expected-digest.ps1 -Clear` trong chính window đó rồi
   đóng window. Không tạo/rotate key, đổi settings hoặc retry device để ép
   `MATCH`.

QA local đã kiểm 26/26 vector giả, Java trim/one-slash/key-whitespace
boundaries, Process-only write/clear và các guard chống persistent environment,
argument, child/network, clipboard, transcript hay output digest. Phản biện độc
lập đã loại công thức P5C/P5D cũ và xác nhận P5E account test còn phụ thuộc
provider/model/endpoint hiện có trên app; loader không và không được kiểm tra
thay runtime state đó.

## Follow-on bị hoãn: một account check duy nhất

Danh sách này **không được thực hiện trong request hiện tại**. Nó chỉ là tiêu
chí cho request sau khi metadata được accept và owner cho phép device event.

1. Kiểm lại runner/helper/APK/certificate/serial pin trước khi đọc expected.
2. Owner dùng đúng process PowerShell đã nhận
   `P5E_EXPECTED_VALUE_PROCESS_LOAD=PASS` từ
   `scripts/p5e-load-expected-digest.ps1`. Loader đã tạo digest theo đúng Java
   semantics từ endpoint P5E cố định, một LF và exact key bytes; agent không
   nhận raw values.
3. Expected chỉ có trong process scope của process tạm. Không dùng User/Machine
   environment, file, clipboard, Git, chat, host command line hay transcript.
4. Trước launch, ghi nhận owner/operator đã xác nhận giới hạn đã nêu trong scope
   account check hiện có: Android instrumentation nhận **digest** như extra tạm
   thời sau stdin; raw key từ record owner không được truyền qua host tới thiết bị.
5. Xác nhận account test APK historical replacement còn đúng pin; không install,
   replace, uninstall, clear data, downgrade hay build lại APK.
6. Chạy một preflight device read-only duy nhất. Nếu device/serial không sẵn
   sàng, ghi typed stop một lần và không reconnect loop.
7. Chạy đúng một account runner launch. Không retry, redispatch, refresh key,
   đổi serial hoặc tạo authorization.
8. Chỉ nhận output có exact class/method, đúng một result `MATCH|MISMATCH`,
   `OK (1 test)`, đúng một terminal `INSTRUMENTATION_CODE: -1`, exit zero và
   không có failure marker/redaction violation.
9. Timeout, non-zero, output thiếu/trùng, identity sai hoặc redaction violation
   là `ACCOUNT_CHECK_NOT_PROVEN_STOP`; không retry.
10. `MATCH` chỉ xác nhận equality với record độc lập. Nó không cấp A4.3, RAW,
    P5 exit hay P6.
11. `MISMATCH` dừng để owner xem lại provenance/mapping. Không sửa settings,
    không đổi key và không chạy lại để ép match.
12. Xuất receipt chỉ có typed status, counts, source hashes và time metadata;
    không có expected, actual, endpoint hoặc credential.

## Ma trận sự cố đã biết và guard tương ứng

| Sự cố / rủi ro | Guard bắt buộc |
| --- | --- |
| `DEVICE_NOT_FOUND` trước đây | stop một lần; không reconnect/reinstall loop |
| code196 data loss hoặc downgrade | không production install, clear, uninstall, downgrade, connected suite |
| certificate-case / APK pin drift | hash/certificate gate trước event; lệch thì dừng |
| bốn parser false-green | exact component, identity, terminal success, một result và exit zero |
| timeout capture | bounded capture; timeout là not-proven, không retry |
| test APK bị nhầm với RAW artifact | account APK historical không chứng nhận A4.3/RAW |
| expected suy từ device actual | cấm; record phải độc lập và predate actual-read |
| phát tán secret trong docs/log | metadata-only, scan trước commit, receipt typed-only |
| status cũ lấn status mới | canonical header, BUILD_STATE, snapshot và active release checklist dùng cùng table |

## Điều kiện hoàn thành

Gói **owner-provenance review** hoàn thành khi có một decision typed từ metadata
hoặc một stop typed vì metadata chưa đủ. Cả hai đều là kết thúc hợp lệ của gói;
không tạo thêm vòng audit cho cùng input. Chỉ decision accepted mở quyền soạn
request device kế tiếp. Không trạng thái nào trong tài liệu này cho phép A4.3,
RAW, P5 exit, P6, build, reinstall, provider call, database action hoặc đọc
credential.
