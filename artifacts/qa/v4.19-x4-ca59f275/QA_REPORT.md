# v4.19 X4 — offline QA, 2026-10-13

Owner chọn hướng (a): D-A áp dụng cho mọi self khi call là tên. Baseline `701470e5`; implementation `2ad2e623`, sửa assertion prompt cũ `ca59f275`. Production diff duy nhất: luật tên-call `TargetedFixPrompt.java`; finder/guard không đổi.

Archive `git archive ca59f275c1ba97c980848329eba3379b0b7b10ad`, ZIP SHA-256 `27469E09885EA27C47F76FD2CD294D8939D8EAFEDDC84C0892C736F0642C709F`; chạy trong `D:/P5E-builds/verify-v419-x4-final/source/`.

| Kiểm | Kết quả |
|---|---|
| `:editorial-engine:test` | 718 total, 717 PASS, 1 SKIP, 0 fail/error |
| `:app:test` debug/release/benchmark | Mỗi biến thể 473 total, 465 PASS, 8 SKIP, 0 fail/error |
| `py -3 -B -m unittest discover -s scripts/chunk -p test_*.py` | 58 PASS, 0 fail |
| `NameCallRuleDaContradictionTest` | 6 PASS: prompt đã lật; ba control và hai finder/guard unchanged |
| `suite --relabel` từ archive | 21 known / 137 clean, tổng 158; public indexes khớp byte |
| Replay stored 006 C6E, 008 C7A, 011 C7D | **158/158 unchanged**, 0 regressions, 0 improvements, exit 0; không dùng `--allow-da-choices` |
| Offline token estimate, prompt mới | 006: 15 chunks / 14 calls / 74 points / 73,220 input estimate / 57,344 output reserve / USD 0.087118 theo giá lịch sử giả định |

Relabel thêm 8 điểm (008 ×7 và 006 7:3), giữ nhãn gốc và lý do. 23 điểm D-A thực sự được gắn allowed complete-line hashes; năm điểm của danh sách 20 cũ thuộc ngôi ba/vocative/call đại từ vẫn là strict controls. Default replay không nới; chế độ chấm explicit cho lần đo mới chấp nhận cậu hoặc đúng call name ở đúng các spans, mọi chữ khác bất biến. Test có ca wrong pronoun, wrong name, đổi chữ/dấu câu và hoàn tác lời gọi đều bị coi là hồi quy.

Lỗi local đã sửa: archive đầu `2ad2e623` FAIL một test pipeline còn assert prompt ép mọi cậu → tên; cập nhật đúng hai assertion tên-call/keep, giữ assertions ambiguity/quotes/FINAL. Archive mới full PASS. Replay attempt đầu thiếu ANDROID_HOME trong shell con, fail trước test; đặt SDK env rồi rerun PASS. Không có bản sạch thay đổi/sửa sai cũ lọt lại.

Bằng chứng host: full-test.log, chunk-test.log, app-debug-final.log; XML engine/debug/release/benchmark; measurement-estimate.xml. Replay TSV chứa chữ sách ở `D:/P5E-private/v419-pronoun/X4-archive/replay-current/`, chỉ summary trong replay.log. Input pin/gói chi: `docs/V419_X4_006_MEASUREMENT_PACKAGE.md`. Artifact/backup mirror và SHA256SUMS được kiểm trước handoff.

Giới hạn: replay dùng câu trả lời cũ nên không đo compliance của model dưới prompt mới. Chưa có build APK/device/provider; USD 0, X5/X6 chưa chạy. Đề nghị trần USD 0.10 chỉ là gói chờ quyền chi, không phải quyền đã cấp.
