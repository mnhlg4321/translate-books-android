# P5E — kiểm tra hành vi collector và điều kiện chuyển pha, 2026-09-16

> **Historical RED input, superseded for current readiness.** The bounded repair
> at implementation HEAD `77f060ad4aba61852c2c92f22326c2ed02180e20` resolved the
> three SQLite defects and executed the production golden JVM→host bridge. Use
> `docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json` for the current local gate;
> A4.3, RAW and P6 remain closed. The RED findings below are retained unchanged.

**Kết luận: LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED. Chưa đủ điều kiện A4.3 hoặc P6; owner approval không sửa được lỗi collector.** Tiếp tục P5E với phạm vi sửa/kiểm thử local trong `P5E_NEXT_WORK_REQUEST.md`. Đây là yêu cầu tiếp tục phase hiện tại, không mở phase/release mới.

## Baseline và phạm vi

- Worktree `D:\App Translate Books\App Translate Books-translation-profile`, branch `feature/v4.18-p5e-audit-20260914`, actual input HEAD `31a09d02323cd317a9089411901849e6d51b8f66`, clean lúc bắt đầu.
- Đọc startup BUILD_STATE → snapshot → Git/status, workflow/canonical và bộ P5E hiện hành; đối chiếu diff `35c52600..31a09d02`, command, helper, schema DAO/migration, serializer, golden test và readiness probe. Quét điều hướng tài liệu; không khẳng định đã kiểm ngữ nghĩa từng dòng mọi tài liệu lịch sử hay chạy regression toàn ứng dụng.
- Luna được user chỉ định: rà tài liệu, viết review và yêu cầu công việc; primary đối chiếu code, tái hiện SQLite và review đầu ra Luna. Đây là hai góc kiểm tra, không phải hai lần chứng minh runtime độc lập.
- Không credential/account read, ADB/device/instrumentation/provider/runtime authorization/DB live/APK build/install trong audit.

## Những phần đã tiến bộ — giữ, không làm lại

| Phần | Evidence và giới hạn |
|---|---|
| H1 | Command đã pin helper SHA và kiểm file/hash trước invocation; helper có gate tương ứng. Không mở lại lỗi “không có hash guard” của audit cũ. |
| H2/H4 | Có PrepareEvent/CollectReadback Before/After và recovery path trong source; không còn đúng khi nói “không có collector”. Nhưng có function không chứng minh nó chạy đúng. |
| H3 | Contract riêng report/receipt khớp tên field serializer; golden Java test source có thật. Chưa có execution và chưa có bytes từ Java đi xuyên host validator. |
| F2 | SelfTest chạy lại exit0, required arguments8, process outcomes4, outcome fixtures8, command-log/redaction PASS, provider/device0. |
| Phase | RAW chưa chạy; chưa có predecessor; RECONCILE/L1/P5 exit chưa đạt; P6 vẫn khóa. |

## Ba lỗi có tái hiện bằng SQLite thật trong RAM

Probe `P5E_SQL_BOUNDARY_PROBE_20260916.py` trích nguyên query của `Get-P5EConsistentDatabaseReadback`, thay chỉ các biến identity bằng dữ liệu giả; tạo tables từ DDL trong `EditorialMigrationSpec.java`. Không đọc DB pilot. SQLite host 3.53.1 dùng cho tái hiện cú pháp/NULL; không suy version SQLite trên device từ đây.

| ID | Lỗi và evidence | Hệ quả |
|---|---|---|
| S1 | `SELECT 'SCHEMA' ... (SELECT user_version)` → `no such column: user_version`. Control `SELECT user_version FROM pragma_user_version` trả24. | Before collector không thể hoàn tất query hiện hành. |
| S2 | LINEAGE query xuất tag +11 giá trị =12 cột; parser yêu cầu17, slice1..16. | Kể cả sửa S1, parser vẫn từ chối output của chính query. |
| S3 | Một row RECOVERY_REQUIRED được tạo với report/receipt NULL đúng DDL; concat `length(report_bytes)` làm toàn SELECT ATTEMPT thành NULL. | sqlite CLI có thể xuất dòng trống rồi bị filter loại; recovery row tồn tại nhưng evidence không giữ được status. Không được suy “không có claim” từ sự vắng dòng này. |

Kết quả lưu `P5E_SQL_BOUNDARY_RESULT_20260916.json`; cả ba `confirmedDefects=true`. Đây là ba lớp lỗi độc lập: sửa một lỗi chưa đóng collector. INPUT query là tag+6 fields, parser7 đúng; không đưa nghi ngờ ban đầu này thành finding.

## Vì sao tiếp tục lặp

1. Readiness probe dùng regex tìm tên function/parameter và sự tồn tại golden test, rồi suy `allFourBlockersResolved/readyForOwnerDecision=true`. Nó không execute query→parser→collector happy path.
2. Bằng chứng negative “không launch được ADB” chỉ chứng minh fail-closed ở đầu vào, không chứng minh thu được state hợp lệ. SelfTest PASS và collector hỏng có thể đồng thời đúng.
3. Golden test chưa chạy lại được gọi “optional”. Một test source chưa chạy không đáp ứng acceptance yêu cầu bytes từ production serializer. Test Java hiện kiểm contract trong Java, chưa export bytes sang PowerShell.
4. Request trước của chính assistant có mâu thuẫn: cấm build chung nhưng các bước sau yêu cầu test/build nếu cần; 115 bước trộn sửa local, quyền live và bàn giao. Điều này tạo cớ trì hoãn kiểm thử cần thiết. Request mới tách host/JVM tests khỏi APK build/install; không thêm approval giả cho test offline thông thường.
5. README còn nói helper không có gate/collector, trái với code mới; state/proposal lại nói GREEN. Lịch sử hash/các banner lặp làm mất một next action rõ ràng.

Không giải quyết vòng lặp bằng thêm checklist dài hơn: giữ đúng ba lỗi đã tái hiện, kiểm xuyên boundary còn thiếu, rồi freeze một lần. Chỉ mở lại gate đã đạt khi có diff hoặc phản chứng mới.

## Những điểm cần kiểm trong lần sửa, chưa gọi là lỗi đã tái hiện

- Golden Java output → host canonicalization/identity validation phải có test thực, với fixture identity tách live pins; không sửa JSON production bytes để ép match.
- `transactionSemanticsPassed=true` hiện được gán sau kiểm source hashes. Hash source là provenance, không phải test execution; phải link kết quả transaction test đã chạy phù hợp hoặc ghi UNKNOWN.
- Khả dụng `run-as`, sqlite3, apksigner và phiên bản shell là prerequisite chưa quan sát trên device. Test offline adapters trước; sau quyền read-only mới kiểm khả dụng thực. Không suy host sqlite3 có nghĩa Android cũng có.
- Command dùng powershell.exe. Kiểm đúng runtime đó, không chỉ chạy helper trong pwsh khác version rồi gọi runbook PASS.
- Read-only snapshot/WAL, no-unrelated-writes và no-deletes cần chứng cứ đủ phạm vi, không suy từ row counts bằng nhau. Không ép equality toàn DB trước/sau RAW vì writes được phép.

## Provenance snapshot

| File tại input HEAD | SHA-256 |
|---|---|
| Approval manifest | DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501 |
| Command | 30B50BFEF809225B33901AC130CC5EE83CC8AD76A3D958D987EF19C8FCA50D10 |
| Helper/collector | 4D68F4BB0B0D31FA6D443439967746C1C83CCB4514EE6D7AF830CF90DBE0EC76 |
| Artifact contract | FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF |
| Historical local result | ECD61953C8E9C4E4539EC5B2B5865EDBC08D686E37F4C4743D67084887D8E725 |

Không sửa các bytes này trong audit. Result cũ giữ nguyên để truy vết; nhãn GREEN của nó bị supersede bởi bằng chứng ngày16/9. Fingerprint vẫn NOT_PROVIDED/NOT_APPROVED, không tự sinh hoặc lấy actual mismatch làm expected.

## QA và phản biện đầu ra

QA cuối: Python AST parse PASS; chạy lại probe và so JSON với result đã lưu
(trừ metadata baselineHead) khớp hoàn toàn, ba defects tái hiện. SHA-256 của
cả năm input trong bảng trên khớp; diff `app`, `editorial-engine`, `scripts`
bằng 0; `git diff --check` PASS. Probe chạy bằng Python bundled tại
`C:\Users\ADMIN\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe`
với đối số `docs/P5E_SQL_BOUNDARY_PROBE_20260916.py`. Không coi kết quả này
là full collector execution: query SQL thật được execute, parser width được
đọc từ source; full PowerShell collector behavioral test là việc tiếp theo.

Review request đã sửa mâu thuẫn `NO_BUILD`/JVM, recovery fixture vi phạm NOT
NULL, và giả định 17 columns luôn đúng. Request cuối 48 bước có field/source
provenance, refreeze order, risk mapping lịch sử và exit criteria hữu hạn.

- **“Đã fail-closed nên đủ review?”** Fail-closed ngăn call sai; không chứng minh happy path có thể hoàn thành. S1/S2 chặn cả dữ liệu hợp lệ.
- **“Chỉ cần owner decision?”** Sai: SQL/parser là dependency local. Owner có thể chuẩn bị provenance song song nhưng không được gọi packet execution-ready.
- **“Cần rebuild Android?”** Chưa có bằng chứng cần; collector host và JVM golden có thể kiểm offline. Chỉ repin APK khi thật sự đổi source được đóng vào APK.
- **“Recovery output rỗng = chưa gọi provider?”** Sai: S3 cho thấy rỗng do serializer SQL. UNKNOWN phải giữ nguyên; không retry/refresh ID.
- **“Lại mở H1–H4 từ đầu?”** Không. Giữ H1/F2 và H3 field mapping đã đạt; sửa S1–S3 và kiểm các boundary chưa được chạy.
- **“RAW accepted mở P6?”** Không; còn RECONCILE/L1 và P5 exit theo canonical. Ba-chương QA của P6 không phải điều kiện tự đặt trước RAW.

Audit/plan bàn giao có thể hoàn tất trong khi readiness sản phẩm vẫn FAIL. Không gọi SQL probe tìm được lỗi là test acceptance GREEN. Next action duy nhất: thực hiện yêu cầu local đã sửa trong `P5E_NEXT_WORK_REQUEST.md`, đạt behavioral QA rồi mới freeze packet gửi owner.
