# v4.23 — Yêu cầu làm việc: 4 lỗi phía app trong luồng dịch (F1–F4)

**Trạng thái:** ĐÃ PHẢN BIỆN.
- 2026-10-22: phản biện F1–F4, đọc mã tại `7adcfa78`, tìm ra 12 lỗi; đã sửa tất cả.
- 2026-10-23: thêm F5 (đồng hồ) và F6 (reasoning chạy không đáy). Phản biện lần hai tìm ra 9 lỗi; đã sửa tất cả.

**Mốc:**
- `7adcfa78` trên `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`.
- Điện thoại `15e84958` chạy `4.23-dev.22` / code 300.

**Quyết định sản phẩm phục vụ:** bớt lượt gọi lại vô ích, không chấp nhận chunk còn dòng tiếng Nhật chưa dịch, giảm chi phí đầu vào, và không lưu API key trong các bảng SQLite (`jobs`, `config_revisions`, `prepared_batches`).
- Key vẫn nằm trong SharedPreferences `settings` (`SettingsStore.save`, `SettingsStore:16`), nên vẫn có trong bản sao lưu.
- Phần SharedPreferences nằm ngoài phạm vi đợt này.

**Nguồn bằng chứng:**
- Bản sao lưu điện thoại ngày 2026-10-22 21:37 (`v4.23-dev.22-code300`), nằm ngoài Git. Phân tích chỉ in số đếm; bản giải nén tạm đã xóa.
- Dữ liệu: 330 job, 1.254 chunk đã xong, 1.365 lượt gọi `openai/gpt-5.6-luna` qua openrouter.
- Thiết lập: `reasoningEffort=medium`, `maxOutputTokens=4096`, `maxTokensPerChunk=1700`, preset `balanced`, prompt `14.lightnovel_general_qc_translation_only_v14.yaml`.

**Ranh giới:**
- Không sửa `editorial-engine/` hay tệp `Editorial*`. Thay đổi trong `OpenAICompatibleClient` không được đổi request của Biên tập (`V5ChatEditorialApiProvider`, `OpenRouterEditorialApiProvider`).
- Không đổi schema. Không merge, tag hay push.
- Không gọi provider khi chưa có quyền chi tiền riêng của owner (mục 6).
- Không đưa văn bản sách, API key hay dữ liệu cá nhân vào Git, test hay log commit. Fixture phải là văn bản tổng hợp.

## 1. F1 — Giới hạn output không tính phần reasoning

**Triệu chứng:**
- 53 lượt `finish_reason=length` (52 lượt bị từ chối vì `FINISH_REASON_LENGTH`), chủ yếu ở **chunk ngắn**: trung vị nguồn chỉ 524 ký tự.
- Ví dụ trong DB: job 321 chunk 3 có `raw_tokens=431`, `max_output_tokens=786`, phần trả lời nhìn thấy chỉ **4 ký tự**. Job 314 chunk 3: 444 → 807, cũng 4 ký tự. Job 323 chunk 5: 333 → 629, 352 ký tự.

**Nguyên nhân đã chứng minh:**
- Giới hạn gửi đi tính bằng `PromptPlan.adaptiveMaxOutputTokens` (`PromptPlan:145–151`) = `min(maxOutputTokens, rawTokens × 1.6 + 96)` với preset balanced. Giá trị này được dùng ở `TranslationEngine:184`.
- Nhánh dịch gửi kèm `reasoning_effort` (`TranslationEngine:45–49`, `ReasoningEffort.forTranslation`; điện thoại đang đặt `medium`). Body gửi `max_tokens` (`OpenAICompatibleClient:474,482–484`).
- Với model reasoning, `max_tokens` là trần cho **cả** phần reasoning lẫn câu trả lời.
  - Bằng chứng: chunk 321/3 có `completion_tokens` ≈ `max_tokens` (786), mà phần trả lời nhìn thấy chỉ 4 ký tự.
- Vì vậy chunk ngắn chỉ được vài trăm token cho cả phần nghĩ lẫn câu trả lời. Phần nghĩ dùng gần hết, câu trả lời bị cắt.
- Lượt thử lại nhân đôi giới hạn (`nextLimitAfterTruncation`, sàn 8192, `TranslationEngine:80–85,257`) nên cuối cùng vẫn qua, nhưng mỗi lần cắt cụt tốn thêm một lượt gọi.

**Sửa nhỏ nhất:**
- Tách giới hạn phần trả lời nhìn thấy với giới hạn gửi đi:
  - `visible` = giá trị adaptive hiện nay, vẫn bị chặn bởi `maxOutputTokens` của owner;
  - `requestCap = visible + reasoningReserve(effort)`.
- **Mức dự trữ:**
  - nếu `ReasoningEffort.forTranslation(s)` khác rỗng: theo mức, `low` 1024, `medium` 2048, `high` 8192 (theo F6);
  - nếu rỗng nhưng `ReasoningEffort.looksLikeReasoningModel(model)`: 2048, vì "off" chỉ bỏ tham số, còn model gpt-5 vẫn reasoning theo mặc định của provider;
  - các trường hợp còn lại: 0.
  - Đây là trần, không phải chi phí: chỉ token thật dùng mới tính tiền.
- **Vị trí sửa:**
  - chỉ sửa `TranslationEngine:184,318`, cùng một helper trong `PromptPlan` hoặc `ReasoningEffort`;
  - **không** sửa `buildChatRequestBody` của `OpenAICompatibleClient`, nên request Biên tập không đổi.
- **Hai chỗ phải nhận `requestCap` và giữ nhất quán:**
  - `max_tokens` của request dịch;
  - `ResponseValidator.validate(..., maxOutputTokens)`, vì `OUTPUT_TOKEN_LIMIT_REACHED` và `LIKELY_TRUNCATED_END` so với giá trị này.
- **Lượt thử lại sau cắt cụt:** `boostedLimit = nextLimitAfterTruncation(requestCap)`, dùng nguyên giá trị này, không cộng dự trữ lần nữa.
- **Ghi số liệu:** `ChunkMetric.maxOutputTokens` (`:418`) ghi `requestCap`; ghi `visible` vào sự kiện `chunk_request`.

**Không sửa:**
- `contextFallbackSettings` (`:277–281`): `@Deprecated`, chỉ `Patch312Test` dùng;
- `LiveBenchmarkRunner`: không gửi `reasoning_effort` (`:38`), ngoài phạm vi;
- `CostEstimator:62,94`: dùng `s.maxOutputTokens` chứ không dùng giới hạn adaptive, giữ nguyên.

**Test (Java thuần):**
- `raw=333`, medium → visible 629, requestCap 2677;
- `raw=333`, high → requestCap 8821;
- `raw=333`, off, model không reasoning → 629;
- `raw=333`, off, model `gpt-5*` → 2677;
- trần `maxOutputTokens` vẫn chặn visible;
- preset full;
- validator nhận `maxOutputTokens` bằng `requestCap`;
- `boostedLimit` sau cắt cụt;
- body request Biên tập không đổi (so sánh trước/sau).

**Ảnh hưởng:** mọi lần dịch có reasoning. Không đổi prompt, chunking hay dữ liệu lưu bền.

## 2. F2 — Chunk còn câu tiếng Nhật chưa dịch vẫn được chấp nhận

**Triệu chứng:**
- 13 trên 1.254 chunk đã chấp nhận có ít nhất một dòng gần như toàn tiếng Nhật (≥ 6 ký tự kana/kanji và ≥ 40% dòng).
- Job 93 chunk 2: **54/58 dòng chưa dịch**.
- Job 303 chunk 7: 2 lời thoại còn nguyên tiếng Nhật, 1 lời thoại dịch dở.
- Rải rác: tiêu đề `あとがき` để nguyên, thán từ `あ`, `ああああ` để nguyên.

**Nguyên nhân đã chứng minh:**
- `ResponseValidator` (`:19–45`) chỉ từ chối `SOURCE_ECHO` khi **toàn bộ** output trùng nguồn.
- Kiểm tra duy nhất về chữ Nhật còn sót là `ChunkQa` (`:18`), và nó:
  - chỉ là **cảnh báo**, không từ chối;
  - tính trên cả chunk với ngưỡng `> max(20, outputChars/3)`, nên vài dòng sót trong 60–100 dòng không bao giờ chạm ngưỡng;
  - được gọi sau khi đã chấp nhận (`TranslatorService:1011`).

**Sửa:**
- Thêm lỗi `RETAINED_SOURCE_LINE` vào `ResponseValidator`. Đây là lỗi kiểm định có thể thử lại, đi theo `retryOnValidationFailure` và `maxAttempts` hiện có. Hết lượt thử thì là lỗi riêng chương, như các lỗi kiểm định khác.
- **Chỉ bật qua overload mới** `validate(..., Collection<String> keptTargets, boolean checkRetainedSource)`:
  - bật ở `TranslationEngine:213,344` và `TranslatorService:558` (khôi phục response đã nhận);
  - `JobStore:34` (`manualReplace`, bản sửa tay của owner) và `ReliabilityScenarioRunner:14,19` giữ overload cũ, không bị quy tắc chặn.
- Chạy kiểm tra trong khối `if (!truncated)`.
- **Nguồn `keptTargets`:**
  - thêm `List<String> glossaryTargets` vào `PromptContextBuilder.ContextBlock` và `PromptPlan`; hiện `ContextBlock` chỉ có văn bản đã định dạng (`PromptContextBuilder:638`);
  - đường khôi phục dựng lại bằng `PromptPlan.forTranslation(new Chunk(r.index, "", r.source, ""), "", s)`.
- **Cách tính một dòng:**
  - Ký tự Nhật gồm `U+3040–30FF`, `U+4E00–9FFF` và `U+3005`, trừ `・` (U+30FB) và `ー` (U+30FC).
  - Trước khi đếm, loại bỏ:
    - (a) đoạn `《…》` đứng ngay sau chữ Hán (ruby);
    - (b) **nguyên chuỗi** của từng `keptTargets` có chứa chữ Nhật (không loại từng ký tự).
  - Dòng "còn nguồn" khi sau đó còn ≥ 6 ký tự Nhật **và** chúng chiếm ≥ 40% số ký tự không phải khoảng trắng của dòng. Đây là ngưỡng đã dùng để đếm triệu chứng.
- Từ chối khi có ≥ 1 dòng như vậy. Thông báo gồm số dòng, số thứ tự dòng đầu tiên và tỷ lệ. **Không** in nội dung dòng.
- **Không bắt được** (giới hạn đã biết): tiêu đề ngắn như `あとがき`, thán từ ngắn như `ああああ` (dưới 6 ký tự). Prompt v15 xử lý phần này.
- `ChunkQa` giữ nguyên làm cảnh báo; không trùng mã lỗi.

**Tái hiện offline:**
- Fixture tổng hợp gồm 60 dòng tiếng Việt và 2 dòng thoại tiếng Nhật. Bản cũ chấp nhận, bản mới từ chối.
- Ngoài Git: chạy script trên bản sao DB điện thoại, đếm số chunk đã chấp nhận mà quy tắc mới sẽ từ chối, ở **cả hai** ngưỡng 40% và 30%. Kỳ vọng khoảng 13 ở ngưỡng 40%. Báo cả số dương tính giả quan sát được. Chỉ in số đếm.
- `V423OfflineReplayTest` (`acceptedAfterRepair >= 7`, dùng tệp riêng trên D:) có thể đổi kết quả. Nếu đổi, ghi rõ lý do trong báo cáo, không hạ ngưỡng của test.

**Test:**
- dòng chưa dịch → từ chối;
- dòng có ruby `漢字《かな》` đã dịch → không từ chối;
- lock giữ kanji → không từ chối;
- dòng tiếng Việt có một tên katakana ngắn → không từ chối;
- `manualReplace` → không bị chặn;
- đường khôi phục → từ chối giống nhánh dịch.

## 3. F3 — Cache prompt chỉ trúng khoảng 5%

**Triệu chứng:**
- Tỷ lệ `cached_input_tokens / actual_input_tokens` theo ngày từ 2% đến 7%, trung bình 7,1%.
- Phần instruction chiếm khoảng 2.200 token mỗi lượt gọi (ước tính thật). Phần này lặp nguyên văn ở đầu system prompt của mọi chunk.
- Giá ước tính từ dữ liệu: đầu vào ~$0,24 / 1M token, đầu ra ~$1,22 / 1M; instruction chiếm khoảng 14% tổng chi phí. Nếu cache trúng, phần này giảm mạnh.

**Đã biết từ mã:**
- System = câu base (cố định theo cặp ngôn ngữ) + `# GLOBAL INSTRUCTION` + locks + output contract (`PromptPlan:43–53`).
- Tiền tố trước phần locks giống nhau giữa các chunk, nên **theo lý thuyết** phải cache được.
- Response có trường `provider` (`OpenAICompatibleClient:505`, `responseProvider`), nhưng nhánh dịch **không lưu** trường này.

**Giả thuyết (chưa chứng minh):**
- H1: OpenRouter phân các lượt gọi sang nhiều provider khác nhau, nên cache của provider này không dùng được cho provider kia.
- H2: tiền tố không thật sự giống nhau giữa các chunk.
- H3: tham số request làm hỏng cache.

**Việc làm:**
1. **Kiểm H2 offline:** test dựng `PromptPlan.translation` cho 3 chunk tổng hợp liên tiếp cùng thiết lập. Khẳng định system prompt của các chunk có chung tiền tố ≥ 1.024 token ước tính, và tiền tố đó kết thúc trước phần locks. Nếu sai thì sửa thứ tự cho tiền tố cố định đứng trước, rồi chạy lại test.
2. **Ghi chẩn đoán**, không đổi schema:
   - thêm `responseProvider` vào sự kiện `chunk_response` (`TranslationEngine:208`). Sự kiện này đã có job, `inputTokens` và `cachedTokens`;
   - không sửa client dùng chung;
   - không làm hàm tổng hợp hay giao diện; số liệu tính offline từ log kéo về.
   - Lưu ý: `chunk_attempts.provider` và `ChunkMetric.provider` là provider **trong cài đặt**, không phải provider đã trả lời.
   - H2 gần như chắc chắn PASS vì `base` và `instruction` không đổi trong một job; test ở bước 1 chỉ để xác nhận.
3. **Chưa đổi định tuyến.** Chỉ đề xuất sửa (ví dụ ghim provider qua `provider.order`, hoặc tham số cache của OpenRouter) sau khi có số liệu thật ở mục 6. Không tự gửi tham số mới khi chưa đo.

**Test:** test tiền tố; test sự kiện log có đủ trường và không chứa key/header.

## 4. F4 — API key nằm trong cơ sở dữ liệu

**Triệu chứng:**
- Snapshot thiết lập của mỗi job (`config_revisions.settings_json`) có trường `apiKey`; đã thấy trong bản sao lưu điện thoại.
- Key còn có ở SharedPreferences `settings`. Phần đó nằm ngoài phạm vi, nên bản sao lưu vẫn sẽ chứa key sau đợt này.

**Nguyên nhân:**
- `SettingsStore.toJson` ghi `apiKey` (`SettingsStore:122`).
- Các chỗ lưu bền dùng hàm này:
  - `TranslationRepository:335` (`jobs.settings`);
  - `:342` (`config_revisions`);
  - `:435` (revision mới, đồng thời cập nhật `jobs.settings` ở `:439`);
  - `PreparationCoordinator:86,101,107,135` (`prepared_batches.settings_json`).
- `JobStore:105` in nguyên `settingsJson` khi parse lỗi. Sau khi dọn thì nội dung in ra đã sạch.
- Các chỗ đọc snapshot rồi **gọi provider**:
  - `TranslatorService:224` (retry chunk, extra `settings`), `:296`, `:360` (`batch.settingsJson`), `:441`, `:548`, `:636`;
  - `LiveBenchmarkRunner:16`.
- `JobsPageFactory:412` từ chối snapshot không có key ("Job snapshot không có API key"). Không sửa thì benchmark sẽ bị chặn.

**Sửa:**
1. Thêm `SettingsStore.toPersistedJson(s)`: như `toJson` nhưng **không có** `apiKey`. Đây cũng là chỗ duy nhất để loại các trường bí mật khác nếu có; rà `AppSettings` để liệt kê đủ.
2. Mọi chỗ **ghi xuống DB hay tệp** dùng `toPersistedJson`.
   - Intent nội bộ gửi cho service của chính app (`MainActivity:2666`, `JobsPageFactory:383`) giữ nguyên trong đợt này. Ghi rõ lý do trong báo cáo.
3. **Bơm key hiện tại khi đọc:**
   - thêm `SettingsStore.withCurrentSecrets(context, snapshot)`: nếu snapshot không có key thì lấy `apiKey` hiện tại từ `SettingsStore.load(context)`;
   - gọi hàm này ngay sau mọi `fromJson` phía service: `TranslatorService:224,296,360,441,548,636`, `LiveBenchmarkRunner:16`;
   - sửa guard `JobsPageFactory:412` dùng key hiện tại;
   - các chỗ chỉ hiển thị (`MainActivity:3443`, `JobStore:37,90,167`) không cần.
   - **Bước này phải nằm cùng commit với bước dọn (4), hoặc trước nó.** Nếu dọn trước, resume sẽ lỗi "API key is empty".
4. **Dọn dữ liệu cũ một lần:**
   - chạy **đồng bộ** khi mở app, trước bất kỳ lượt chạy service nào; cờ SharedPreferences chỉ được đặt **sau** khi transaction commit;
   - đọc từng hàng `jobs.settings`, `config_revisions.settings_json`, `prepared_batches.settings_json`; bỏ `apiKey` khỏi JSON; ghi lại;
   - chạy trong transaction; hàng có JSON không parse được thì bỏ qua và đếm;
   - **không đổi** `settings_hash`: `HashUtil.settingsHash` đã được xác nhận là không tính key;
   - báo số hàng đã dọn, số hàng bỏ qua ở mỗi bảng.

**Test:**
- JSON lưu bền không có `apiKey`;
- resume, retry, retry đã sửa, đọc `batch.settingsJson` và benchmark dùng key hiện tại;
- guard `JobsPageFactory:412` chấp nhận snapshot không có key khi Cài đặt có key;
- dọn dữ liệu trên DB fixture tổng hợp: hàng có key thì sạch, hàng không key giữ nguyên, chạy lần hai không đổi gì;
- `settings_hash` không đổi.

**Ghi cho owner:** các bản sao lưu cũ ngoài Git vẫn chứa key. Việc thay key mới ở provider là quyết định của owner.

## 4b. F5 — Đồng hồ "Đã chạy" vẫn tăng sau khi dịch xong (bổ sung 2026-10-23)

**Triệu chứng:**
- Job 332 (chương lẻ `001_KUMO_JP_VOL13`) chạy thật 2 phút 45 s: `elapsedMs` = 165.058, `updatedAt − sessionStartMs` = 165.258.
- Sau khi xong, màn Dịch hiện "Đã chạy 18 giờ 8 phút", và vẫn tăng mỗi lần mở lại.

**Nguyên nhân đã chứng minh:**
- `MainActivity:3915` đặt `timedRunElapsedBaseMs = now − sessionStartMs` mỗi khi có `sessionStartMs`, ở **mọi** trạng thái.
- Hàm này chạy lại mỗi lần `onStart` (`:617`) và mỗi lần chuyển sang tab Dịch (`:884`), qua `restoreRuntimeStateToUi` → `updateTrackingFromIntent`.
- `RuntimeStateStore:129` phát lại `sessionStartMs` đã lưu.
- Vì vậy lượt đã xong (hay đã tạm dừng, đã lỗi) bị tính lại theo giờ thật.
- Chế độ tổ hợp dùng chung dòng này (suy luận; trên máy chưa có lượt tổ hợp nào để đối chứng).

**Sửa:**
- Service ghi mốc kết thúc (`sessionEndMs`) khi lượt dừng ở COMPLETED, FAILED, CANCELLED; ghi `pausedAt` cộng tổng thời gian tạm dừng khi PAUSED. Các mốc này lưu vào `RuntimeStateStore` và được phát lại.
- **Nơi ghi:**
  - thêm `sessionEndMs`, `pausedAtMs`, `pausedTotalMs` vào `RuntimeStateStore.saveSession` (`:47`) và `toIntent` (`:129`);
  - service ghi các mốc này ở nhánh broadcast `done` / `error` / `cancelled` / `paused`.
  - `pausedTotal` chỉ áp cho tạm dừng rồi tiếp tục trong cùng tiến trình (`TranslatorService:141–159`). Hết thời gian chờ hay lỗi thì service dừng hẳn, và "Tiếp tục" gọi `resetSession`.
- Tách logic thành hàm thuần `RunPanelFormatter.runElapsedMs(state, nowWall, sessionStart, sessionEnd, storedElapsed, pausedTotal)`:
  - RUNNING: `now − start − pausedTotal`;
  - PAUSED: `pausedAt − start − pausedTotal`;
  - trạng thái kết thúc: `end − start − pausedTotal`;
  - không có `start`: dùng `storedElapsed`.
- Gọi hàm này ở `MainActivity:3915`.

**Test (Java thuần):**
- COMPLETED, FAILED, CANCELLED đứng yên khi phát lại sau 18 giờ;
- PAUSED đứng yên;
- RUNNING vẫn tăng;
- trường hợp tổ hợp;
- `sessionStart = 0`;
- trạng thái đang dừng (`stopping`) không tính là đã kết thúc.

**Họ lỗi cần rà:**
- `TranslatorService.elapsedMs()` (`:994`, `now − jobStartMs`) sau khi job xong;
- thời gian chết khi service bị dừng rồi chạy lại (`resetSession`, `:93`).

## 4c. F6 — Reasoning "high" chạy không đáy (bổ sung 2026-10-23)

**Bằng chứng** (bản sao dữ liệu điện thoại `20261023-180815-readonly-diagnosis`, ngoài Git; chỉ số đếm). Cùng một chương, cùng model `gpt-5.6-luna`:

| | Job 331, `high` | Job 332, `medium` |
|---|---|---|
| Kết quả | owner ngắt sau ~15 phút | xong |
| Lượt gọi | 13 | 7 |
| Output token | 104.939 | 15.089 |
| Chi phí | $0,1334 | $0,0282 |
| Chunk xong | 1/7 | 7/7 |

- Ở job 331 có 6/13 lượt mà phần reasoning chiếm **100% trần** (`reasoning = output`), nên không còn chữ trả lời.

**Nguyên nhân đã chứng minh:**
1. **Trần output không chừa chỗ cho reasoning:** đây là F1 (`PromptPlan:145`). `ReasoningEffort.forTranslation` (`:33–38`), qua `TranslationEngine.chatForTranslation` (`:45–49`), gửi nguyên "high", không kèm dự trữ.
2. **Gọi lại gấp đôi trần** (`TranslationEngine:80–84,257`, sàn 8.192, trần 16.384). Model nghĩ tới hết trần mới, nên gọi lại chỉ làm tăng tiền.
3. **Vòng chia nhỏ không giới hạn** (`TranslatorService:806–818`). Bị cắt thì chia đôi chunk rồi gọi lại `translateJob`. Mỗi chunk con có lại 3 lượt với cùng mức reasoning, nên lại bị cắt.
   - Chuỗi thực tế: 2.898 → 2.142 → 1.605 → 1.132 ký tự.
   - Chỉ dừng khi không chia được nữa: không có giới hạn về độ sâu, số lượt gọi hay tiền.
4. **Không có chặn chi phí:**
   - kiểm tra trần chi phí trong engine chỉ chạy khi `attempt > 1`, `stopOnCostLimit` bật và `costLimitUsd > 0` (`TranslationEngine:178`);
   - `enforceCostLimit` (`TranslatorService:730,780`) chỉ chạy giữa các chunk, và cũng chỉ khi `stopOnCostLimit` bật;
   - trên máy cả hai điều kiện đều tắt;
   - `maxPaidRetries` và `maxRetryCostUsd` không được nối vào luồng dịch; chỉ `ReliabilityScenarioRunner` dùng chúng, qua `BudgetPolicy`.

**Sửa:**
1. **Dự trữ reasoning theo mức** (mở rộng F1): `low` 1.024, `medium` 2.048, `high` 8.192.
2. **Không chia nhỏ khi bị cắt do reasoning.**
   - **Nhận biết:** engine gắn cờ `reasoningBound` vào `RejectedResponseException` khi `finish=length` **và** `reasoningTokens ≥ 0,9 × completionTokens`. Nếu provider không báo `reasoningTokens`, coi là reasoning-bound khi phần trả lời nhìn thấy dưới 32 ký tự. `result.reasoningTokens` có sẵn ở `OpenAICompatibleClient:138` và còn trong phạm vi khối catch của engine.
   - Service đọc cờ này **trước** `splitForContextOverflow` (`TranslatorService:806`). Khi có cờ thì không chia nhỏ.
   - **Gọi lại một lần:**
     - dùng `attemptSettings = s.copy()` với effort hạ một bậc (`high` → `medium` → `low`), và `requestCap` tính lại theo mức mới;
     - không dùng `nextLimitAfterTruncation`;
     - nếu effort đã là `low`, `auto` hay `off` thì báo lỗi riêng chương ngay;
     - vẫn bị cắt thì báo lỗi riêng chương với lý do "reasoning dùng hết giới hạn".
3. **Giới hạn tổng lượt gọi cho mỗi chunk gốc**, tính cả các chunk con: tối đa 6. Vượt thì báo lỗi riêng chương, không chia tiếp.
   - Chunk gốc: đi ngược `parent_stable_id` tới hàng không có cha. Các hàng SUPERSEDED (idx âm) vẫn được giữ nên đi ngược được.
   - Bộ đếm là `Map<rootStableId, Integer>` của lượt chạy, truyền qua lời gọi đệ quy `translateJob` (`:817`). Không lưu DB; "Dịch lại chunk lỗi" bắt đầu đếm lại từ 0.
4. **Kiểm trần chi phí trước mọi lượt gọi**, kể cả lượt đầu (bỏ điều kiện `attempt > 1`).
   - Thêm trần **mặc định an toàn** cho mỗi lượt chạy: gấp 3 lần chi phí dự kiến của kế hoạch dịch, tối thiểu $0,10.
   - UI tính **trần hiệu lực** lúc owner xác nhận, hiện số này trong hộp "Sẵn sàng dịch", và gửi cho service qua `EXTRA_EFFECTIVE_COST_LIMIT_USD`. Service và engine chỉ dùng giá trị này, để số hiện ra và số thực thi là một.
   - **Ý nghĩa của 0:** `costLimitUsd = 0` nghĩa là dùng trần mặc định an toàn. Muốn bỏ trần thì phải bật công tắc "Không giới hạn" trong Cài đặt và xác nhận (gói giao diện U5.3 dùng đúng quy ước này).
   - `maxPaidRetries` và `maxRetryCostUsd` chưa có tác dụng: ẩn khỏi Cài đặt, hoặc ghi "chưa có tác dụng".
5. **Ghi log** mỗi lượt: số token reasoning, lý do dừng, lượt thứ mấy của chunk gốc. Ghi số liệu, không ghi văn bản.

**Tái hiện offline:**
- Dùng provider giả (đã có trong test) trả `finish=length` với `reasoning = output` cho mọi lượt.
- Bản cũ: số lượt gọi tăng theo số lần chia.
- Bản mới: 1 lượt ở mức gốc + 1 lượt ở mức thấp hơn, rồi lỗi riêng chương; không chia nhỏ; tổng lượt ≤ 6.

**Test:**
- dự trữ theo mức;
- không chia khi reasoning-bound;
- hạ mức reasoning ở lượt gọi lại;
- trần 6 lượt cho mỗi chunk gốc;
- trần chi phí chặn ngay lượt đầu;
- trần mặc định được tính đúng và hiện trong hộp xác nhận;
- chunk bị cắt vì câu trả lời dài (reasoning thấp) vẫn được chia như cũ.

## 5. Thứ tự, build và thử trên điện thoại

**Thứ tự:** F4 → F1 + F6 → F5 → F2 → F3.
- F4 trước vì ảnh hưởng lưu bền.
- F1 và F6 làm cùng nhau vì cùng sửa trần output và đường gọi lại.
- F3 chỉ ghi chẩn đoán.

Mỗi bước: viết test trước, sửa, chạy toàn bộ test `app`, rồi commit.

**Điểm dừng 1:**
1. hồi quy bằng archive sạch: unit test cả 3 biến thể, lint, biên dịch `androidTest`;
2. build `4.23-dev.23` (code > 300) qua `scripts/build-and-save.ps1 -Series 4.23-dev`;
3. sao lưu điện thoại theo mục 3 của `docs/V423_WORKSPACE_NAV_WORK_REQUEST.md`;
4. **dừng chờ owner "duyệt cài"**, chỉ cài bằng `install -r`.

**Thử trên điện thoại** (không gọi provider):
- app mở được, các trang Dịch, Công việc, Thư viện và job cũ hiện đủ;
- **trình tự:** cài → mở app (bước dọn chạy) → sao lưu lần nữa → đếm số hàng còn chứa `"apiKey"` trong ba bảng. Kỳ vọng 0. Chỉ in số đếm;
- SharedPreferences `settings` vẫn còn key: đây là điều đúng như dự kiến, chỉ xác nhận ô key không trống. Không in, không chụp giá trị;
- logcat không có FATAL.

Ghi các mục cần gọi provider là `NOT_MEASURED`, rồi **dừng** và báo cáo.

## 6. Lượt live đề xuất (chỉ chạy khi owner cấp quyền chi tiền riêng)

**Giả thuyết cần kiểm:**
- F1: chunk ngắn có reasoning không còn bị `length`;
- F6: ở mức `high`, chương xong hoặc dừng có kiểm soát trong trần chi phí, không còn vòng chia nhỏ. Thử thêm 1 chương ngắn ở `high`, ngân sách tối đa **$0,10** riêng;
- F3: tỷ lệ cache theo provider (H1).

**Vì sao không giải được offline:** hành vi `max_tokens` + reasoning và định tuyến/cache của OpenRouter chỉ thấy trên lượt gọi thật.

**Phạm vi:**
- một tập `QA_` gồm 1 chương ngắn, khoảng 3–4 chunk, chunk cuối ngắn hơn 500 ký tự;
- thiết lập hiện tại (`gpt-5.6-luna`, `medium`);
- ngân sách tối đa **$0,10**: đặt `costLimitUsd=0.10` cùng `stopOnCostLimit=true`, rồi khôi phục thiết lập cũ sau lượt thử.

**Tổng quyền chi xin owner: $0,20** (F1/F3 $0,10 + F6 $0,10).

**PASS khi:**
- không có `FINISH_REASON_LENGTH` ở lần thử đầu;
- F6: chương ở mức `high` hoàn tất, hoặc dừng với lý do rõ ràng, trong ≤ $0,10, và không có lần chia nhỏ nào do reasoning;
- có số liệu `responseProvider` và cache của từng lượt.

Kết quả cache dùng để quyết định có ghim provider hay không. Ghim provider là gói sau.

## 7. Báo cáo

Báo cáo gồm:
- commit;
- bản, mã, SHA-256 APK;
- đường dẫn sao lưu trước và sau khi cài;
- số hàng đã dọn ở từng bảng;
- số chunk mà F2 sẽ từ chối khi replay (cùng số dương tính giả);
- kết quả test tiền tố của F3;
- PASS/FAIL/NOT_MEASURED từng mục.

Cập nhật `WORKSPACE_SNAPSHOT.md` và mục 10 của `PRODUCT_DIRECTION_PLAN.md`.
