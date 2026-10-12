# P5E owner input — metadata provenance only; expected value remains local

> Current signature-layout disposition — 2026-09-24: the prior event remains CLOSED_STOP at PRODUCTION_PACKAGE / UNSUPPORTED_LAYOUT and its receipts are unchanged. The source-derived AOSP wrapper adapter is locally qualified by synthetic preflight 189/189 and command 28/28 on Windows PowerShell 5.1. No device compatibility or account result is claimed; A4.3/RAW/P5 exit/P6 remain closed.
> Sole next action: owner reviews [the new event packet](P5E_SIGNATURE_LAYOUT_ADAPTER_EVENT_PACKET_20260924.md) and, only if desired, gives a separate decision. Do not execute or reuse the closed event command; older pending-event text is historical.


> Current post-STOP local disposition — 2026-09-24: `HOST_SIGNATURE_LAYOUT_ADAPTER_OFFLINE_PASS / PREFLIGHT_QA_189_OF_189 / COMMAND_QA_28_OF_28 / PATH_GUARD_QA_12_OF_12 / OWNER_EVENT_DECISION_PENDING / ACCOUNT_CHECK_NOT_EXECUTED / A4.3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`.
> The old event is terminal at `PRODUCTION_PACKAGE` and must not be reused. The new packet contains the exact source pins and one-time command; its evidence directory is unused and uncreated.
> Sole next action: owner reviews the exact packet and decides whether to authorize one new account-only event. This guide does not authorize device work. Earlier current/next-action text below is historical context; release 05–09 remain incomplete.


> Current P5E state: `HOST_RUNNER_REPAIR_OFFLINE_PASS /
> EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW /
> EXPECTED_VALUE_LOADER_OFFLINE_PASS / EXPECTED_VALUE_PROCESS_LIFETIME_UNKNOWN /
> OWNER_EVENT_DECISION_PENDING / ACCOUNT_CHECK_NOT_EXECUTED /
> A4.3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY`.
> The qualified loader does not launch an account check.

Dùng mẫu này cho phản hồi owner tiếp theo. Không gửi API key, endpoint,
fingerprint, digest, settings value, ảnh màn hình hoặc command transcript vào
chat, Git, issue hay evidence dùng chung.

## Mẫu trả lời đơn giản

Sao chép rồi điền những phần biết chắc. Nếu chưa biết, dùng `NOT_VERIFIED`.

```text
originalKeyAvailability=RETAINED_OUTSIDE_APP | APP_ONLY | NOT_VERIFIED
recordAuthority=<nhãn nơi quản lý record, ví dụ password manager hoặc workspace record>
recordReference=<mã/nhãn opaque; không phải key name, không phải digest>
accountOrProjectMapping=<nhãn account/project không secret>
verificationTime=<timestamp with timezone, date-only with timezone, or NOT_VERIFIED>
recordPredatesActualRead=YES | NO | NOT_VERIFIED
endpointScopeMapping=YES | NO | NOT_VERIFIED
```

Ví dụ `recordAuthority` có thể là nhãn password manager hoặc workspace; nó
không phải API key. `recordReference` chỉ là mã để owner tự tìm lại record, ví
dụ một ID nội bộ. Không dùng tên key hay chuỗi 64-hex làm reference.

## Metadata đã nhận trong continuation này

Owner đã xác nhận `RETAINED_OUTSIDE_APP`, authority là `OpenRouter Default
Workspace / API Keys`, reference là nhãn record
`OpenRouter dashboard / Default Workspace / API Keys / xzx`, mapping là
`OpenRouter Default Workspace / App Translate Books`, ngày xác minh
`2026-07-13` theo `Asia/Ho_Chi_Minh` nhưng không lưu giờ, cùng với
`recordPredatesActualRead=YES` và `endpointScopeMapping=YES`.

Decision provenance được ghi là `EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW`; local
loader được ghi riêng là `EXPECTED_VALUE_LOADER_OFFLINE_PASS`. Reference `xzx`
chỉ là nhãn record, không phải expected digest. Agent chưa đọc hoặc lưu expected
value; account check vẫn chưa chạy.

## Khi nào metadata đủ

Record phải tồn tại độc lập trước event đọc actual từ thiết bị, có authority và
reference, và mapping rõ tới cùng account/project và endpoint scope. Metadata
owner hiện đã đáp ứng điều kiện review này ở mức owner assertion; không có
provider/endpoint read để biến nó thành xác minh runtime.

`NO` hoặc `NOT_VERIFIED` ở hai trường mapping/predates là một stop hợp lệ:
`EXPECTED_PROVENANCE_UNAVAILABLE_STOP`. Không tạo key mới, không lấy expected
từ actual, và không chạy lại account check để giải quyết thiếu metadata.

## Ranh giới kỹ thuật cho follow-on sau này

Đây không phải hướng dẫn launch. Chỉ sau khi metadata được accept, scope hiện có
`OWNER_ACCOUNT_CHECK_SCOPE_RECEIVED` được đối chiếu và owner/operator xác nhận
ranh giới transport, owner mới tự tạo expected digest trong một process tạm cục
bộ theo Java semantics: endpoint đã normalize, một LF, rồi exact API-key bytes.
Agent không đọc key, endpoint hoặc digest.

Bản ghi cũ chỉ xác nhận owner đã nạp trong một event trước; Process lifetime hiện
tại không biết. Nếu packet mới được owner chấp thuận và cửa sổ hiện tại không
còn value, owner có thể dùng `scripts/p5e-load-expected-digest.ps1` theo hướng
dẫn đã QA trong `docs/P5E_EXPECTED_VALUE_LOADER_REVIEW_20260923.md`: script chỉ
hỏi key ở hidden prompt, khóa endpoint P5E hiện hành trong source và chỉ ghi
digest lower-case vào Process scope. Đây là phục hồi input local, không phải
account retry. Không gửi digest cho agent và không launch ADB trong bước nạp.
Loader QA hoặc provenance metadata không tự mở account check.

Chạy script trong một PowerShell window riêng do owner kiểm soát và giữ chính
window đó mở sau signal `P5E_EXPECTED_VALUE_PROCESS_LOAD=PASS`. Không chạy nó
qua `powershell -File` ở process khác vì value sẽ không còn ở host follow-on.
Không đưa endpoint/key qua command line. `-Clear` xóa value khỏi Process khi
owner muốn dừng hoặc đóng window.

Raw key từ record owner không được truyền qua host tới thiết bị. Test Android
chỉ đọc key đã lưu trong app ở memory để tạo actual fingerprint; host/agent không
đọc hoặc chuyển key đó. Digest không có trong host `adb` argv, child environment,
file, clipboard, Git, chat hoặc transcript. Sau stdin tới Android shell, digest
vẫn được chuyển vào instrumentation như một extra tạm thời. Đây là giới hạn thực tế
được xác nhận trong scope account check hiện có; nếu owner/operator không chấp nhận,
dừng để thiết kế lại transport.

Receipt sau một event được phép chỉ có typed status, counts, serial, timeout,
exit metadata và source hashes. Nó không có expected, actual, endpoint, digest
hoặc credential.
