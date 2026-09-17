# P5E owner input — metadata provenance only

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
verificationTime=<YYYY-MM-DDThh:mm:ss±hh:mm | NOT_VERIFIED>
recordPredatesActualRead=YES | NO | NOT_VERIFIED
endpointScopeMapping=YES | NO | NOT_VERIFIED
```

Ví dụ `recordAuthority` có thể là nhãn password manager hoặc workspace; nó
không phải API key. `recordReference` chỉ là mã để owner tự tìm lại record, ví
dụ một ID nội bộ. Không dùng tên key hay chuỗi 64-hex làm reference.

## Khi nào metadata đủ

Record phải tồn tại độc lập trước event đọc actual từ thiết bị, có authority và
reference, và mapping rõ tới cùng account/project và endpoint scope. Việc owner
còn key gốc ở nơi lưu riêng chỉ là dấu hiệu record có thể tồn tại; nó chưa tự
chứng minh mapping hoặc thời điểm xác minh.

`NO` hoặc `NOT_VERIFIED` ở hai trường mapping/predates là một stop hợp lệ:
`EXPECTED_PROVENANCE_UNAVAILABLE_STOP`. Không tạo key mới, không lấy expected
từ actual, và không chạy lại account check để giải quyết thiếu metadata.

## Ranh giới kỹ thuật cho follow-on sau này

Đây không phải hướng dẫn launch. Chỉ sau khi metadata được accept, scope hiện có
`OWNER_ACCOUNT_CHECK_SCOPE_RECEIVED` được đối chiếu và owner/operator xác nhận
ranh giới transport, owner mới tự tạo expected digest trong một process tạm cục
bộ theo Java semantics: endpoint đã normalize, một LF, rồi exact API-key bytes.
Agent không đọc key, endpoint hoặc digest.

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
