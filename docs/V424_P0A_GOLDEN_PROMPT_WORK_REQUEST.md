# v4.24 P0a — Test "prompt vàng" trước khi tách lõi (gói việc cho Codex)

**Quyết định sản phẩm được phục vụ:** owner duyệt phương án A ngày 2026-10-27 (`docs/PC_APP_RESEARCH.md`): tách lõi dịch Java dùng chung, rồi làm app PC bằng JavaFX.

Gói này dựng lưới an toàn: ghi lại chính xác app hiện tại tạo ra gì từ một bộ đầu vào cố định. Mỗi bước tách lõi sau đó (P0b, P1a, P1b) phải chứng minh được là không làm đổi bản dịch trên điện thoại.

**Không đổi hành vi app.** Không sửa tệp nào trong `app/src/main/`. Chỉ được thêm hoặc sửa:
- test và fixture trong `app/src/test/`;
- một dòng trong `.gitattributes`;
- khối `testOptions` trong `app/build.gradle`, chỉ để chuyển cờ cập nhật tệp vàng vào JVM của test.

## 1. Nhánh và nơi làm

- Tạo nhánh `feature/v4.24` từ đầu nhánh `feature/v4.23` hiện tại, và worktree `D:\P5E-builds\wt-v4.24`.
- Tạo `release_checklists/v4.24-pc-shared-core.md` từ `release_checklists/TEMPLATE.md`. Ghi phạm vi v4.24: lõi chung và app PC, theo các giai đoạn ở mục 4 của `docs/PC_APP_RESEARCH.md`.
- Các commit sửa của v4.23 sẽ được merge sang khi v4.23 chốt. Gói này không làm việc đó.
- Không tag, không merge, không push. Không đụng worktree `wt-v4.23` hay workspace `D:\App Translate Books`.

## 2. Việc cần làm

### 2.1 Bộ đầu vào mẫu

Đặt trong `app/src/test/resources/fixtures/golden-prompt/`. **Chỉ dùng văn bản tự viết.** Không chép câu nào từ sách, từ bản dịch của owner, từ dữ liệu điện thoại hay từ `D:\Ebooks\`.

- **`raw_ja.txt`:** 3.000–4.000 ký tự tiếng Nhật tự viết. Phải có:
  - lời kể;
  - thoại 「」 ở đầu dòng;
  - trích dẫn 「」 giữa câu kể;
  - ruby 《かな》;
  - thán từ kéo dài ー/ッ;
  - dòng trống giữa các đoạn;
  - một tiêu đề kiểu あとがき;
  - ít nhất 3 tên nhân vật.

  Văn bản phải đủ dài để chia ra ít nhất 3 chunk trong mọi tổ hợp.
- **`glossary.csv`:** 4 cột `source,target,category,note`, 6–10 dòng. Có một dòng `target` trống và một mục không xuất hiện trong RAW.
- **`pronoun.csv`:** 7 cột `from,speaker,target,self,call,scope,note`. Có `@NARRATOR`, một `@group`, một `*`, và ô trống để thành `(omit)`.
- **`pronoun_legacy3.csv`:** định dạng 3 cột cũ.
- **`instruction.yaml`:** instruction ngắn tự viết. Đọc qua `YamlInstructionParser.parse` như ở `TranslationEngine.java:193`, rồi đưa vào `translationInstructions`.
- **`prev_translation.txt`:** 1.500 ký tự tiếng Việt tự viết, dài hơn mọi giới hạn ngữ cảnh. Nó đóng vai bản dịch trước, như `tail(translated, 1500)` ở `TranslatorService.java:829`.

Glossary và Pronoun đưa thẳng vào `glossaryText` / `pronounText` dưới dạng chuỗi thô. `PromptContextBuilder` tự phân tích, không qua store.

**Tổ hợp cài đặt** nằm trong `cases.json`. Mỗi tổ hợp **ghi đủ mọi trường dưới đây**, không dựa vào giá trị mặc định của `new AppSettings()` (mặc định là `maxTokensPerChunk` 450, `contextChars` 400, `contextOverlapEnabled` false).

**Các trường cần ghi:**
- `provider`, `model`, `sourceLanguage`, `targetLanguage`;
- `reasoning`, `temperature`;
- `optimizationPreset`, `chunkMode`, `maxTokensPerChunk`, `maxCharsPerChunk`, `softLimitRatio`;
- `maxOutputTokens`, `contextChars`, `contextOverlapEnabled`;
- `glossaryInjectLimit`, `pronounInjectLimit`, `refineAfter`.

| Tên | Khác với `balanced_default` |
|---|---|
| `balanced_default` | provider `openrouter`, `openai/gpt-5.6-luna`, Japanese → Vietnamese, reasoning `medium`, temperature 0.3, preset `balanced`, chunk theo token 1700, `maxOutputTokens` 4096, `contextChars` 600, `contextOverlapEnabled` **true** |
| `no_overlap` | `contextOverlapEnabled` false |
| `economy_char` | preset `economy`, `chunkMode` `char`, `maxCharsPerChunk` 1200 |
| `quality` | preset `quality` |
| `full_no_context` | preset `full`, `contextChars` 0. Preset này đi qua `PromptBuilder` cũ, không có khối OUTPUT CONTRACT |
| `reasoning_high` | reasoning `high` |
| `reasoning_off` | reasoning `off`, nên request không có trường effort |
| `tight_inject_limits` | `glossaryInjectLimit` 2, `pronounInjectLimit` 2 |
| `no_glossary_no_pronoun` | không Glossary, không Pronoun |
| `legacy_pronoun` | dùng `pronoun_legacy3.csv` |
| `refine_on_estimate` | `refineAfter` true. Chỉ ảnh hưởng `estimate.json` |

`bilingualOutput` không dùng trong prompt dịch, nên ghi "không bao phủ" trong README của fixture.

Không cần API key. Nếu code bắt buộc phải có, dùng `TEST_KEY_NOT_SECRET`. Chuỗi này không được có trong tệp vàng.

### 2.2 Đi qua đúng đường của app khi dịch

Mỗi tổ hợp chạy theo thứ tự sau, giống `TranslationEngine`:

1. `AppSettings s = AppValidator.normalize(settings)` (`TranslationEngine.java:237`).
2. `Chunker.chunkText(raw, s)`.
3. Với mỗi chunk `i`, gọi `PromptPlan.forTranslation(chunk, previous, s)` (`TranslationEngine.java:245`). **Không** dùng `PromptPlan.translation`.
   - `previous` là chuỗi rỗng với chunk 0, và là `prev_translation.txt` với chunk `i > 0`.
4. Gọi `OpenAICompatibleClient.buildChatRequestBody(s, plan.prompt, plan.requestMaxOutputTokens(s), null, false, ReasoningEffort.forTranslation(s))`. Đây là đường mà `chatWithUsage` dùng: không `response_format`, không khối `provider`.
5. Gọi cả `CostEstimator.estimatePreparedChunks` (màn hình dùng) và `CostEstimator.estimatePreparedChunksLightweight` (kiểm trước khi chạy, `PreparationCoordinator.java:160`).

**Tệp vàng** ghi trong `expected/<tên>/`:
- **`chunks.json`:**
  - số chunk;
  - với mỗi chunk: chỉ số, `startOffset`/`endOffset`, `paragraphStart`/`paragraphEnd`, độ dài và SHA-256 (byte UTF-8) của nội dung, `contextBefore` và `contextAfter`.
- **`chunk-<i>.system.txt` và `chunk-<i>.user.txt`:** nguyên văn prompt, UTF-8, xuống dòng `\n`.
- **`chunk-<i>.plan.json`:**
  - các số đếm token: raw, system, instruction, glossary, pronoun, `totalInputTokens`;
  - `visibleMaxOutputTokens`, `requestMaxOutputTokens`;
  - mức effort được gửi.
- **`chunk-<i>.request.json`:** thân request.
  - Ghi chuẩn hóa: khóa sắp xếp, thụt lề cố định.
  - Mọi số thực (`temperature`, …) ghi thành chuỗi 6 chữ số thập phân. JVM và Android biểu diễn float khác nhau.
  - Không bao giờ ghi header hay key.
- **`estimate.json`** và **`estimate-lightweight.json`**. Ghi chú: estimator tự dùng đuôi chunk trước làm "bản dịch trước" (`CostEstimator.java:38`), khác với bước 3.

### 2.3 Test

Lớp `GoldenPromptSnapshotTest`, đặt trong package `com.ml.tblandroidtxt` vì `buildChatRequestBody` là package-private:
- Mỗi tổ hợp là một test.
- Prompt và `chunks.json` so **từng byte**. Các tệp JSON so theo nội dung sau khi chuẩn hóa.
- **Khi lệch:** báo tên tổ hợp, tên tệp, dòng và cột đầu tiên khác, kèm tối đa 80 ký tự quanh đó. Không in cả prompt.
- **Cập nhật tệp vàng chỉ khi có cờ:**
  - cờ là system property `golden.update=true`;
  - chuyển cờ vào JVM của test bằng `testOptions.unitTests.all { systemProperty 'golden.update', System.getProperty('golden.update', 'false') }` trong `app/build.gradle`;
  - mặc định test chỉ đọc. Không có đường nào khác để ghi đè.
- **Đọc/ghi tệp:** luôn bằng byte UTF-8 tường minh, không dùng charset mặc định.
- **Trạng thái tĩnh của `ModelCatalog`** (`dynamicById`, cache tra cứu): test không được làm đổi nó. Kết quả phải giống nhau khi chạy riêng lớp test và khi chạy cùng cả bộ.
- **Chạy offline:** không mạng, không điện thoại, không `Context`. `ObservabilityLog` tự bỏ qua khi không có `Context`. Nếu bước nào vẫn cần `Context`, dừng và báo cáo: đó là thông tin cho P0b, không phải lý do sửa mã app.
- **Quét tệp vàng** (một test riêng): không có `TEST_KEY_NOT_SECRET`, không khớp regex `\bsk-[A-Za-z0-9]{8,}` hay `\bBearer\s+\S+`.

**`.gitattributes`:** thêm dòng `app/src/test/resources/fixtures/golden-prompt/** -text`, để Git trên Windows không đổi xuống dòng của fixture và tệp vàng.

**Kiểm chạy tay một lần** (ghi vào báo cáo, không thành test): SHA-256 của các tệp fixture không trùng tệp nào trong `D:\Ebooks\`.

### 2.4 Chứng minh test bắt được thay đổi

Trên một bản sao tạm, không commit, lần lượt sửa một ký tự ở mỗi chỗ sau rồi chạy test:
1. chuỗi OUTPUT CONTRACT ở `PromptPlan.java:64` → phải FAIL mọi tổ hợp trừ `full_no_context`;
2. phần định dạng đầu ra của `PromptBuilder` → phải FAIL `full_no_context`;
3. định dạng một dòng lock Glossary trong `PromptContextBuilder` → phải FAIL các tổ hợp có Glossary;
4. ngưỡng chia chunk trong `Chunker` → phải FAIL `chunks.json`.

Ghi kết quả 4 lần thử vào báo cáo, rồi xóa bản sao.

## 3. Kiểm

- Hồi quy trên bản `git archive` sạch của commit cuối, có `local.properties` trỏ tới SDK:
  ```
  JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
  gradlew --offline -q testDebugUnitTest compileDebugAndroidTestJavaWithJavac lintDebug
  ```
  - Mốc trước gói: 730 test, 5 SKIP, 0 FAIL.
  - Sau gói: 730 cộng số test mới, 0 FAIL; lint 0 lỗi.
- Chạy test vàng hai lần trên bản archive sạch. Kết quả phải giống nhau, và phải giống khi chạy riêng lớp test. Nếu thứ tự không ổn định, sửa ở phía test (sắp xếp, chuẩn hóa), không sửa mã app.
- Không build APK, không cài điện thoại: không có gì đổi trên máy.

## 4. Báo cáo

- Nhánh, worktree, các commit.
- Danh sách tệp fixture và tệp vàng, kèm tổng số và kích thước.
- Kết quả hồi quy: số test, SKIP, FAIL, lỗi lint.
- Kết quả 4 lần thử ở mục 2.4.
- Các chỗ có đụng tới lớp gắn Android (ví dụ `ModelCatalog`) và cách test xử lý.
- Xác nhận:
  - không sửa `app/src/main/`;
  - không sửa tệp `Editorial*`;
  - không có văn bản sách, instruction của owner hay API key trong Git;
  - không gọi provider.
