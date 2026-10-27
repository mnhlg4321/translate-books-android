# v4.24 P1b — Đưa vòng chạy dịch vào `translation-core` (gói việc cho Codex)

**Quyết định sản phẩm được phục vụ:** phương án A cho bản PC (owner duyệt 2026-10-27). Sau P0b/P1a, lõi đã có prompt, chunk, validator, chi phí và lưu trữ. Còn lại phần **điều khiển một lượt dịch**:
- dịch từng chunk, thử lại và tách chunk;
- trần chi phí (F6), ngân sách lượt thử theo chunk gốc;
- dừng / tiếp tục / hủy, đồng hồ (F5);
- tiếp tục job dở, dịch lại chunk lỗi, ghi checkpoint và output từng phần.

Phần này nằm trong `TranslatorService` (1.233 dòng, một `android.app.Service`) và `TranslationEngine` (539 dòng). P1b chuyển nó vào lõi. Nhờ vậy app PC (P2) chạy đúng logic dịch, dừng, tiếp tục và trần tiền như điện thoại, không phải viết lại.

**Không đổi hành vi trên điện thoại:**
- cùng thứ tự gọi provider, cùng số lần thử;
- cùng trạng thái DB, cùng nội dung output;
- cùng dữ liệu phát cho giao diện (broadcast và `RuntimeStateStore`).

**Không sửa tệp `Editorial*`.** **Không gọi provider** nếu chưa có quyền chi riêng.

**Nơi làm:** nhánh `feature/v4.24`, worktree `D:\P5E-builds\wt-v4.24`, từ `8f35c0fa`. Không tag, push hay merge vào nhánh khác.

## 1. Hiện trạng (đo trên `8f35c0fa`)

**`TranslationEngine`:**
- Phần cốt lõi đã gần thuần Java: `translateWithRetry`, `refineWithRetry`, `assembleOutput`, backoff, `isTruncation`, `isReasoningBound`, …, cùng các kiểu lồng `ProviderClient`, `CancelChecker`, `UsageSink`, `EventSink` và hai exception.
- `Context` chỉ dùng để:
  - ghi vết qua `DebugTraceStore` (6 chỗ);
  - đọc/ghi tệp qua `FileUtil` + `Uri`: `readInputUris(Intent)`, `loadExternalConfig(Intent, …)`, `prepareInput(Uri, …)`, `updatePartialOutput(…, Uri, …)`.
- `PreparedInput` có trường `Uri`.

**`TranslatorService`:**
- **Vòng chạy** (dòng 467–1140): `continueExistingJob`, `resumeJobById`, `retryFailedChunksForJob`, `retryFailedRows`, `translateJob`, các hàm trạng thái/tạm dừng/đồng hồ (`setState`, `waitIfPaused`, `markPausedAt`, `closePause`, `elapsedMs`, `remainingMs`, …), `enforceCostLimit`, `commitAccepted`, `previousBefore`, `runtimeEventSink`, `addUsage`, `rootStableKey`.
- **Trạng thái của lượt** là ~35 trường: `paused`, `cancelled`, `runCost`, `rootAttemptBudget`, `rootByChunk`, mốc thời gian phiên, `metric*`, `acceptedPreview`, `jobState`, `activeJobId`, `chapterOutcome`, `setIndex`.
- **Phần gắn Android:**
  - `onStartCommand` và đọc `Intent`;
  - thông báo foreground: `promote`, `buildNotification`, `updateNotification`;
  - `broadcast()`: tính số liệu, gọi `RuntimeStateStore.save`/`saveSession`, rồi `sendBroadcast(Intent)`;
  - output qua SAF: `deferredOutput`, `materializeOutput`, `validateOutputAccess`;
  - `FileUtil` + `Uri.parse(job.outputUri)`;
  - `LogStore.append(this, …)`;
  - `Log`.
- **Ai dùng từ bên ngoài:**
  - hằng `ACTION_*` / `EXTRA_*` (giao diện, và `EditorialPairForegroundService` dùng `ACTION_PROGRESS`);
  - `isActive()` (50 chỗ);
  - `TranslateTestSeam` (test trên máy, dùng provider giả).

## 2. Thiết kế

### 2.1 Lõi

**`TranslationEngineCore`** (lõi): mọi thứ trong `TranslationEngine` không cần Android, gồm các kiểu lồng. App giữ lớp tên cũ `TranslationEngine extends TranslationEngineCore`. Lớp này giữ:
- các constructor nhận `Context`;
- `readInputUris(Intent)`, `loadExternalConfig(Intent, …)`, `prepareInput(Uri, …)`, `PreparedInput`;
- bản `updatePartialOutput(…, Uri, …)`.

Đây là mẫu đã dùng ở P1a, nên `TranslationEngine.ProviderClient` và các tên cũ vẫn biên dịch.

**`TranslationRunner`** (lõi, mới):
- Chứa **toàn bộ trạng thái của lượt** và **các hàm vòng chạy** liệt kê ở mục 1. **Thân hàm chép nguyên**, chỉ thay theo bảng ở mục 2.3.
- API cho bên ngoài:
  - `pause()`, `resume()`, `cancel()`, `isPaused()`, `isCancelled()`;
  - `resetSession()`;
  - `continueExistingJob(...)`, `resumeJobById(...)`, `retryFailedChunksForJob(...)`, `translateJob(...)`;
  - `snapshot(status, progress, log)` (mục 2.2).

**Cổng** (lõi). Mỗi cổng chỉ gồm đúng các thao tác đang dùng:

| Cổng | Thay cho | Thao tác |
|---|---|---|
| `DocumentIO` | `FileUtil` + `Uri` cho output | `readText(ref)`, `displayName(ref)`, `writeTextVerified(ref, text)`, `writeText(ref, text)`. `ref` là chuỗi URI như đang lưu trong DB (`job.outputUri`) |
| `RunListener` | `broadcast()` + thông báo | `onSnapshot(RunSnapshot s)` |
| `RunStatePersister` | `RuntimeStateStore.save` / `saveSession` | hai hàm, cùng tham số |
| `RuntimeLog` | `LogStore.append(this, …)`, `Log.*` | `append(String)`, `error(String tag, String msg, Throwable)` |
| `TraceSink` | `DebugTraceStore.request/response/error` | ba hàm, cùng tham số |
| `Clock` | `System.currentTimeMillis()` trong vòng chạy | `now()` (để test JVM điều khiển thời gian) |

**Các hiện thực trong app:** `AndroidDocumentIO`, `ServiceRunListener`, `AndroidRunStatePersister`, `AndroidRuntimeLog`, `AndroidTraceSink`, `SystemClock`. Mỗi lớp **chỉ gọi lại đúng code cũ**.

### 2.2 `RunSnapshot`: tách "tính số liệu" khỏi "gửi Intent"

- `broadcast(status, progress, log)` hiện tính số liệu rồi gửi `Intent`. Tách thành:
  - **lõi:** `RunSnapshot snapshot(status, progress, log)` chạy y hệt phần đầu (cập nhật `metricLastProgress`/`metricLastLog`, `updateSessionMarkers`, `elapsedMs`, `remainingMs`), gọi `RunStatePersister`, rồi trả về một đối tượng bất biến chứa **mọi giá trị** mà Intent đang mang;
  - **app:** `ServiceRunListener.onSnapshot` dựng `Intent(ACTION_PROGRESS)` với **cùng khóa, cùng kiểu, cùng giá trị** như hiện nay, rồi gọi `sendBroadcast`. Nếu `promote`/`updateNotification` đang được gọi kèm thì gọi đúng chỗ đó.
- **Hằng `ACTION_*` / `EXTRA_*` ở lại `TranslatorService`**, giữ nguyên giá trị (Editorial và giao diện dùng chúng).
- **Bảng đối chiếu:** viết `RunSnapshot` → danh sách `(khóa EXTRA, kiểu, trường)` thành một bảng trong mã. Test JVM khẳng định bảng có đủ mọi `EXTRA_*` mà `broadcast()` cũ đặt vào Intent: lấy danh sách khóa ở mốc B0 bằng cách quét mã nguồn `broadcast()` cũ.

### 2.3 Phép thay được phép trong thân hàm chép sang

Chỉ được thay những chỗ sau. Mọi chỗ khác giữ từng ký tự:

| Cũ | Mới |
|---|---|
| `broadcast(a,b,c)` | `publish(a,b,c)`: gọi `listener.onSnapshot(snapshot(a,b,c))` |
| `updateNotification(...)` / `promote(...)` | `listener` (thêm hàm `onNotification(text,done,total)` vào `RunListener` nếu cần) |
| `Uri x` / `Uri.parse(s)` khi chỉ dùng làm tham chiếu output | `String x` / `s` |
| `FileUtil.writeTextVerified(this, uri, t)` và các hàm `FileUtil` khác | `documents.writeTextVerified(ref, t)` |
| `LogStore.append(this/TranslatorService.this, m)` | `log.append(m)` |
| `Log.e/w/i(...)` | `log.error(...)` |
| `RuntimeStateStore.save*(this, …)` | `persister.save*(…)` |
| `DebugTraceStore.*(appContext, …)` | `trace.*(…)` |
| `System.currentTimeMillis()` trong vòng chạy | `clock.now()` |
| `repo` | `repo` (kiểu `TranslationRepositoryCore`) |
| `engine` | `engine` (kiểu `TranslationEngineCore`) |

**Hàm ở lại `TranslatorService`:** `onCreate`, `onDestroy`, `onTimeout`, `onStartCommand`, `ensureRuntime`, `acquireRun`, `releaseRun`, `finishCancelled`, `runNewJobs`, `runPreparedJobs`, `runSinglePreparedJob`, `runSingleJob`, `runLiveBenchmark`, `deferredOutput`, `materializeOutput`, `validateOutputAccess`, `inputStillMatches`, thông báo.
- Các hàm này gọi sang runner.
- Hàm nào còn cần một trạng thái của lượt thì đọc qua getter của runner. **Trạng thái không được nằm ở cả hai nơi.**

### 2.4 Bổ sung sau phản biện (mục này thắng nếu khác các mục 2.1–2.3)

**Phép thay cần thêm vào bảng 2.3.** Đây là những chỗ duy nhất được phép không chép nguyên:

| Cũ | Mới |
|---|---|
| Chuỗi kết thúc `releaseRun(); stopForeground(…); …nm.cancel(NOTIFY_ID)…; stopSelf();` (trong `resumeJobById`, `retryFailedChunksForJob`) | `listener.onRunEnded()`. App chạy lại đúng chuỗi cũ, đúng thứ tự |
| `finishCancelled(m)` | `listener.onCancelledEnd(m)`. App giữ thân `finishCancelled` cũ |
| `SettingsStore.withCurrentSecrets(this, x)` | `secrets.withCurrentSecrets(x)` (cổng mới `SecretsResolver`) |
| `TranslationRepository.Job` / `ChunkRow` / … | `TranslationRepositoryCore.Job` / `ChunkRow` / … |
| `TranslationEngine.isReasoningBound` / `isRootAttemptLimit` / `shouldSplitAfterFailure` / `DeliveryUnknownException` / … | `TranslationEngineCore.*` |
| `DebugTraceStore.error(this, …)` trong `logFailure` | `trace.error(…)` |
| `Log.e(tag, msg, t)` / `Log.getStackTraceString(t)` | `log.error(tag, msg, t)`. Bản Android gọi đúng `Log.e` và `Log.getStackTraceString`, nên chữ trong log không đổi |
| `LogStore.append(this, log)` trong `broadcast` | `log.append(log)` |

**Đường gọi: lõi không bao giờ gọi ngược vào app.**
- **Chuyển sang runner:**
  - `setState`, `logFailure`, `resetMetrics`, `setExpectedMetrics`, `enforceCostLimit`;
  - phần thuần của `applyEffectiveCostLimit(settings, Double cap)`. Service đọc `cap` từ Intent rồi truyền vào;
  - `CostLimitReachedException` (đang là lớp lồng private của service).
- **`updatePartialOutput` (~9 chỗ gọi) chuyển sang runner.** Phần chọn nơi ghi (SAF tree, `TranslateTestSeam.outputDir()`, `FileUtil.createOutputInTree`, tức `materializeOutput`/`deferredOutput`) đi qua cổng mới `DocumentIO.resolveOutput(jobId, ref)`, hiện thực ở app. Runner vẫn giữ các lời gọi repo `getJob` / `updateJobOutputUri`.
- **`inputStillMatches`** chuyển sang runner, đọc tệp qua `DocumentIO.readText`.
- **Runner có hàm `public`** để service gọi từ luồng chính và từ `onStartCommand`, thay cho việc service sửa trực tiếp các trường:
  - `publish(status, progress, log)`, `setState(next, phase)`;
  - `pause()` / `resume()`: làm đủ các bước phụ như hiện nay: `markPausedAt`/`closePause`, `repoPauseLast`, `setState`, `broadcast`;
  - `cancel()`;
  - setter/getter cho `benchmarkRun`, `activeJobId`, `metricPhase`, `chapterOutcome`, `setIndex`.
- **Runner có thêm lối vào:**
  - `retryChunk(jobId, chunkIdx)` cho `ACTION_RETRY_CHUNK`, gồm cả `repo.applyConfigRevision`;
  - `runCostSupplier()`, để `ensureRuntime` gắn `engine.setSessionCostSupplier(runner::sessionCost)`.
- **`TranslateTestSeam` ở lại app.** Các `V423*InstrumentedTest` phải biên dịch không cần sửa.

**`broadcast` / `RunSnapshot`:**
- `RunSnapshot` mang **cả** `log` gốc (có thể `null`, đưa vào `EXTRA_LOG`) **lẫn** `metricLastLog` (đưa vào `RuntimeStateStore`).
- Cùng mang `chapterOutcome` (total/done/failed), `setIndex`, `runCost` và các mốc phiên.
- Chốt khóa Intent: so **mọi khóa truyền vào `putExtra` trong `broadcast()` ở B0**, khoảng 40 khóa. Không so "mọi `EXTRA_*`", vì `EXTRA_EFFECTIVE_COST_LIMIT_USD` là khóa đầu vào.

**Engine:**
- `metricsRepository` đổi kiểu sang `TranslationRepositoryCore`.
- `TraceSink` mặc định là no-op, như khi constructor test truyền `appContext = null`.
- `System.currentTimeMillis()` ở `TranslationEngine.java:252, 407, 507` đi qua `Clock`.
- `sleepBackoff` (dòng 486–495, `Thread.sleep` thật) đi qua cổng mới `Sleeper`. Bản Android gọi `Thread.sleep`; test dùng bản không ngủ.
- `engine.updatePartialOutput(TranslationRepository, …)` (dòng 462) không có nơi gọi. Giữ nguyên ở lớp con app, **không xóa** trong gói này (không đổi hành vi); ghi vào báo cáo để dọn sau.

**Mức truy cập:** `RunCostMeter`, `AcceptedTranslationPreview`, `RootChunkAttemptBudget` giữ nguyên mức truy cập. P2 dùng cùng package nên không cần đổi.

## 3. Thứ tự làm (mỗi bước một commit, hồi quy xanh sau mỗi bước)

**B0 — Mốc**, trên `8f35c0fa`:
- tập khóa `EXTRA_*` mà `broadcast()` đặt vào Intent;
- danh sách hàm và trường của `TranslatorService` và `TranslationEngine`;
- thân của từng hàm sẽ chuyển, lưu vào thư mục tạm ngoài Git để so ở mục 4.

**B1 — Engine.** Tách `TranslationEngineCore` và cổng `DocumentIO` / `TraceSink`. Test vàng 12/12 phải giữ.

**B2 — Runner.** Thêm các cổng, `RunSnapshot`, `TranslationRunner`, rồi chuyển vòng chạy theo mục 2. `TranslatorService` chỉ còn phần Android.

**B3 — Test kịch bản trên JVM.** Dùng `TranslationRunner`, `TranslationRepositoryCore` trên sqlite-jdbc (fixture lược đồ của P1a), `ProviderClient` giả theo kịch bản, `Clock` giả và `DocumentIO` ghi vào thư mục tạm. Mỗi kịch bản khẳng định:
- trạng thái chunk trong DB;
- số lần gọi provider;
- nội dung output;
- dãy `RunSnapshot` (trạng thái và phần trăm).

Các kịch bản:
1. 3 chunk đều thành công → job `COMPLETED`, output ghép đúng thứ tự.
2. Chunk 2 lỗi mạng 1 lần rồi thành công → retry đúng như `RetryPolicy`.
3. Chunk lỗi hẳn → `FAILED`; gọi `retryFailedChunksForJob` → thành công.
4. Phản hồi bị cắt (truncation) → tăng giới hạn theo `nextLimitAfterTruncation`.
5. Reasoning-bound (F6) → thử lại một lần với effort hạ một bậc. Không vượt `RootChunkAttemptBudget(6)`.
6. Trần chi phí đạt giữa chừng → dừng đúng như `enforceCostLimit`, trạng thái `paused`. Output từng phần chỉ có các chunk đã xong khi `savePartialOutput` bật; khẳng định đúng như vậy.
7. `pause()` giữa chunk rồi `resume()` → đồng hồ không tính thời gian dừng (F5). `cancel()` → gọi `listener.onCancelledEnd`.
   - Runner chạy trên luồng riêng.
   - `pause()`/`resume()` được kích từ bên trong provider giả (chốt ở chunk N).
   - `Clock` giả do test đẩy tiến.
   - `waitIfPaused` vẫn dùng `Thread.sleep(300)` như cũ.
8. Tiếp tục job dở: bỏ qua chunk đã xong, dịch tiếp đúng chunk; `previousBefore` đúng.
9. Từ chối dòng nguồn còn sót (F2 `RETAINED_SOURCE_LINE`) → xử lý như hiện nay.

**Kỳ vọng phải suy ra từ mã ở B0 và các test F1–F6 hiện có, không từ mã đã chuyển.** Ghi lý do từng kỳ vọng ngay trong test.

**Thêm cho mọi kịch bản:**
- Job tạo bằng `repo.createJob(...)` rồi gọi `runner.translateJob`.
- Các snapshot đầu tiên mà service tự phát (ví dụ "Đã chia…") không có trong test. Ghi rõ điều này.
- Thiết lập dùng `Sleeper` không ngủ.

## 4. Chốt chặn

- **`git diff 8f35c0fa..HEAD -- '*Editorial*'` rỗng.**
- **So thân hàm** bằng script:
  - lấy thân hàm bằng bộ khớp ngoặc biết bỏ qua chuỗi, ký tự và chú thích;
  - tách thành token, gộp khoảng trắng;
  - áp các phép thay ở mục 2.3 và 2.4 (dạng regex) lên token cũ;
  - so hai dãy token.

  Kết quả phải **giống hệt**. Script in token khác đầu tiên kèm ngữ cảnh, và in số lần mỗi phép thay được áp. Đính kèm output.
- **Khóa Intent:** tập khóa `EXTRA_*` do `ServiceRunListener` đặt vào Intent bằng tập ở mốc B0.
- **Lõi:** không `import android`; không tham chiếu `Editorial*` (trừ `EditorialPreferences`); `javap` không có `JSONObject.put(String,float)`.
- **Trạng thái:** không trường nào của lượt (danh sách ở mục 1) còn khai báo trong `TranslatorService`. Service đọc và ghi chúng qua getter/setter của runner (mục 2.4).

## 5. Kiểm, build, điện thoại

1. **Hồi quy** trên `git archive` sạch:
   ```
   gradlew --offline -q testDebugUnitTest :translation-core:test compileDebugAndroidTestJavaWithJavac lintDebug
   ```
   0 FAIL; test vàng 12/12; lint 0 lỗi. Các test `V423*InstrumentedTest` phải **biên dịch** được. **Không chạy** chúng trên điện thoại thật, vì chúng ghi job giả vào DB thật của owner.
2. **Build** `4.24-dev` bằng `scripts/build-and-save.ps1`.
3. **Điện thoại `15e84958`:**
   - sao lưu bằng `cmd /c`;
   - ghi số đếm trước khi cài (như P1a);
   - **dừng chờ owner gõ "duyệt cài" trong chính phiên làm việc**;
   - cài bằng `install -r`;
   - sau khi cài: số đếm giống hệt; màn Dịch hiện ước tính; mở được job cũ; 0 lỗi `AndroidRuntime`.
4. **Lượt dịch thật để kiểm vòng chạy:** **chỉ khi owner cấp quyền chi riêng.** Đề xuất trần ≤ $0,03:
   - một tệp `QA_` ngắn (2–3 chunk), effort `medium`;
   - bấm Dừng rồi Tiếp tục một lần;
   - đối chiếu số lần gọi, chi phí, đồng hồ và output với cùng tệp chạy trên dev.2 nếu có.

   Không có quyền chi thì ghi `NOT_MEASURED` và dừng ở bước 3.

## 6. Không được làm

- Không sửa tệp `Editorial*` hay `editorial-engine/`.
- Không đổi logic thử lại, ngân sách, trần chi phí, thứ tự trạng thái, prompt hay giá trị mặc định.
- Không đổi giá trị `ACTION_*` / `EXTRA_*` hay khóa `RuntimeStateStore`.
- Không chạy instrumented test trên điện thoại thật.
- Không gọi provider khi chưa có quyền chi. Không tag, push hay merge vào nhánh khác. Không làm phần app PC.

## 7. Báo cáo

- Các commit B0–B3.
- Kết quả từng chốt chặn, kèm output của script so thân hàm.
- Danh sách hàm ở lại app và lý do.
- Kết quả 9 kịch bản.
- Hồi quy: số test app và lõi, lint.
- Bản build, SHA-256, bản sao lưu; số đếm trước và sau khi cài.
- Lượt dịch thật: kết quả, hoặc `NOT_MEASURED`.
