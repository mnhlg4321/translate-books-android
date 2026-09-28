# Yêu cầu làm việc tiếp theo — sửa offline PM path classification và outer capture

## Trạng thái đầu vào

`OWNER_DECISION_CONSUMED / A4_3_PRE_DISPATCH_COLLECTOR_STOP / P5E_COLLECTOR_ADB_NONZERO / PM_PATH_PRODUCTION_EXIT_1 / OUTER_CAPTURE_STATUS_125 / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`

Event đã đóng:
`D:\P5E-private\raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc`.
Không chạy lại, đổi tên, sửa hoặc tái sử dụng event này. Quyết định owner đã được
tiêu thụ khi event được tạo.

## Kết luận đã chứng minh

1. `PrepareEvent` thành công và tạo đúng một event mới.
2. Before collector gọi đúng một lệnh read-only
   `pm-path-production-before`.
3. Pinned `adb.exe` launch một lần, không timeout, trả exit `1`.
4. Collector ghi `P5E_COLLECTOR_ADB_NONZERO`; không có provider, credential,
   device mutation, DB write hoặc RAW dispatch.
5. Outer command trả `125` vì ít nhất một task thu stdout/stderr của helper
   không hoàn tất trong cửa sổ 5 giây. Đây là lớp lỗi báo cáo bên ngoài, không
   thay thế nguyên nhân collector đã ghi.
6. Evidence redacted không cho phép kết luận package production bị thiếu,
   thiết bị lỗi, USB lỗi hay ADB unauthorized.

## Mục tiêu duy nhất

Sửa và QA offline hai contract:

- phân loại an toàn nguyên nhân nonzero của `pm path` mà không lưu raw output;
- giữ được helper process exit và trạng thái stream capture riêng biệt, để mã
  `125` không che typed collector stop đã có.

Không chuẩn bị hoặc xin phép live event mới trong work package này.

## Các bước chi tiết

### A. Đóng băng evidence

1. Hash-check ba file event theo provenance ngày 2026-09-28.
2. Chỉ đọc `EVENT_PLAN.json`, `COLLECTOR_OUTCOME.json` và
   `COLLECTOR_COMMAND_LOG.jsonl`.
3. Không đọc instrumentation stdout/stderr, provider payload, credential,
   expected digest hoặc native ADB stderr của event.
4. Không chạy ADB chẩn đoán, `pm path`, `dumpsys`, pull, install hoặc build.

### B. Tái hiện outer capture `125` bằng fixture synthetic

1. Tách hàm wrapper của final command thành contract testable offline hoặc tạo
   harness byte-equivalent.
2. Tạo helper synthetic lần lượt: exit `0`, exit `1`, ghi stdout, ghi stderr,
   ghi cả hai, không ghi gì, output sát giới hạn, stream giữ mở bởi child,
   timeout và process-start failure.
3. Chứng minh RED: wrapper hiện tại có thể trả `125` dù helper process đã exit
   với một mã xác định và collector receipt đã được ghi.
4. Sửa wrapper để lưu riêng `processExitCode`, `stdoutDrainStatus`,
   `stderrDrainStatus`, `timedOut` và `captureBounded`.
5. Khi process exit đã biết nhưng drain không hoàn tất, báo typed wrapper status
   riêng và giữ nguyên process exit trong receipt; không biến nó thành PASS và
   không che mất collector typed stop.
6. Không in helper stderr. Chỉ cho phép enum, byte count, bounded flags và mã
   process đã allowlist.
7. Chứng minh GREEN cho toàn bộ fixture, bao gồm child giữ pipe handle mở.

### C. Sửa phân loại `pm path` an toàn

1. Tạo fixture stdout/stderr synthetic cho: package path hợp lệ, package không
   tồn tại, device missing/offline/unauthorized, shell/PM service failure,
   malformed path, nhiều path và unknown nonzero.
2. Parse output chỉ trong memory rồi emit một enum an toàn:
   `PACKAGE_PRESENT`, `PACKAGE_NOT_FOUND`, `DEVICE_UNAVAILABLE`,
   `PM_SERVICE_FAILURE`, `MALFORMED_RESPONSE`, `UNKNOWN_NONZERO`.
3. Không ghi raw stdout/stderr, package-private path đầy đủ hoặc argv vào
   receipt. Với success chỉ giữ hash/path contract hiện có.
4. Mọi enum ngoài `PACKAGE_PRESENT` vẫn dừng trước pull/provider.
5. Unknown phải fail-closed; không tự suy luận package missing.
6. Thêm mutation tests chống false acceptance khi output bị cắt, trộn stderr,
   duplicate path, sai package hoặc chứa chuỗi nhạy cảm.

### D. Regression và phản biện

1. Chạy helper self-test, command wrapper fixtures, PM-path matrix, binding
   `262/262`, regression `175/175`, DB host-readback `56/56`, secret scan,
   PowerShell 5.1 parse và `git diff --check`.
2. Counters của work package phải là ADB/device/provider/credential/DB-write/
   build/install/RAW/redispatch đều `0`.
3. Phản biện false PASS, stream-handle leak, process-exit masking, raw-output
   retention, package-not-found overclassification và retry leakage.
4. Chỉ PASS khi `0 BLOCKER / 0 HIGH`; ghi rõ mọi MEDIUM/LOW.

### E. Kết thúc work package

1. Tạo repair result, QA và provenance với hash exact.
2. Đồng bộ canonical plan, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, checklist
   và proposal đúng một lần.
3. Nếu helper hoặc command đổi byte, packet hiện tại trở thành
   `CLOSED_CONSUMED_NON_REUSABLE`; mọi live event tương lai phải repin toàn bộ
   downstream hashes và cần quyết định owner mới.
4. Next action duy nhất sau PASS là review packet mới. Không tự chạy thiết bị,
   không tự xin/giả lập owner decision và không mở P6.

## Tiêu chí hoàn thành

- Tái hiện RED và chứng minh GREEN cho cả outer capture và PM-path classifier.
- Exit thật của helper không bị mã `125` che mất.
- Receipt chỉ chứa classification redacted, không raw output.
- Toàn bộ regression nêu trên PASS, live counters bằng `0`.
- Không retry event 2026-09-28 và không tạo event mới.
