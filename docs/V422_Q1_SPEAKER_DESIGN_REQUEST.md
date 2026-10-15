# v4.22 Q1 — Yêu cầu làm việc: thiết kế phát hiện lỗi xưng hô theo người nói (offline)

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (mục 5, khối v4.22, Q1). Nhánh: `feature/v4.22` (chỉ tài liệu và mã thử nghiệm tắt mặc định; không ảnh hưởng hành vi 4.22.0). Owner duyệt bắt đầu ở dạng thiết kế + test offline 2026-10-14; lượt đo trả phí xin riêng.

## 1. Vấn đề (đã chứng minh, lặp lại lần thứ năm)

App không biết dòng thoại nào do ai nói với ai, nên:

- Chương 013 Kumo tập 7 (`D:\P5E-private\v422-q0\finder-points.txt`): finder tạo 19 điểm; 15 điểm xưng hô giả (lời Potimas bị so với hàng Hyuban và ngược lại), 2 glossary giả; lỗi thật dòng 151 (Ariel → Shiro dùng "tôi/cô", hàng ghi `chị`) có điểm, model đề xuất đúng nhưng guard chặn vì cột `call` của hàng thiếu `em` (đính chính 2026-10-15; xử lý ở R của `docs/V422_WORK_REQUEST.md`); lỗi thật dòng 346 (Potimas → Ariel dùng "lão", từ của hàng Ariel → Potimas) không có điểm. Bản cuối chỉ đổi 1 dòng, không cần thiết.
- JAKUAKU bước B: 50/122 lỗi tự xưng là lấy `self` của nhân vật khác; finder không phát hiện vì từ sai thuộc một hàng khác.
- Các hướng vá bằng luật chữ đã đo và loại (cổng cue trong dòng RAW, dò ngôi ba, mở rộng từ vựng, thay thế tất định).

## 2. Hướng thiết kế cần đánh giá

Tách việc theo nguyên tắc "model làm nội dung, app làm ghi sổ":

1. **Model ghi người nói/người nghe** cho mỗi dòng thoại của một đoạn, chỉ bằng tham chiếu đơn giản: chọn từ danh sách nhân vật lấy từ file pronoun (cột `speaker`/`target`), hoặc `?` khi không chắc. Không viết lại câu.
2. **App đối chiếu tất định**: với dòng có người nói X và người nghe Y, lấy hàng pronoun X → Y (đúng phạm vi); từ tự xưng/gọi trong dòng trái với `self`/`call` của hàng đó thì thành điểm, kèm đúng một hàng. Dòng có `?` không tạo điểm.
3. **Sửa**: model chỉ được hỏi trên những điểm đó (đã biết hàng), hoặc app thay thế tất định khi chỉ có một cách đổi; câu hỏi mở cho đánh giá.

## 3. Việc cần làm (offline, USD 0)

1. **Tài liệu thiết kế** (mục mới trong file này): định dạng câu trả lời ghi người nói (dòng `[n] X > Y` hoặc tương tự, đơn giản nhất cho parser), cách app dựng danh sách nhân vật, xử lý lời kể (`@NARRATOR`), nhóm (`@GROUP`), người nghe nhiều, phạm vi `scope`, và chỗ gắn vào luồng hiện tại (`FixPointFinder`, `TargetedFixPrompt`, `AddressEditGuard`, `EditorialPairRunService`) — bước mới chạy trước hay thay bước tìm điểm ADDRESS. So sánh chi phí: thêm một lời gọi mỗi đoạn hay gộp vào lời gọi sửa.
2. **Ước lượng giá trị bằng nhãn tham chiếu**: với chương 013 Kumo và chương 014 JAKUAKU, lập nhãn người nói/người nghe cho các dòng thoại (đọc RAW và dòng kề; ghi rõ là nhãn tham chiếu do phiên làm việc lập, không phải đo model của app; lưu ngoài Git). Chạy bộ đối chiếu tất định với nhãn đó: số điểm thật / giả so với finder hiện tại; số lỗi đã biết bắt được (013: dòng 151 và 346; 014: các ca bước B).
3. **Mã thử nghiệm tắt mặc định** (nếu cần cho bước 2): parser câu trả lời người nói, bộ đối chiếu tất định, test tổng hợp (nhân vật giả). Không nối vào luồng chạy của app, không đổi prompt hiện hành.
4. **Đề xuất lượt đo trả phí**: giả thuyết (model ghi đúng người nói ≥ một ngưỡng; số điểm giả giảm; lỗi thật được bắt), chương (013 Kumo và một chương JAKUAKU chưa dùng để chỉnh), số lời gọi tối đa, trần USD, cách chấm (so nhãn model với nhãn tham chiếu; owner đọc các điểm mới). Dừng chờ owner duyệt.

## 4. Tiêu chí

- Tài liệu có quyết định thiết kế rõ, các phương án đã cân nhắc và lý do chọn; số liệu bước 2 cho thấy hướng này loại được phần lớn điểm giả và bắt được các lỗi thật đã biết, hoặc chỉ ra vì sao không.
- Nếu số liệu bước 2 không cho thấy cải thiện rõ: dừng, báo owner, không đề xuất lượt trả phí.

## 5. Không làm

Không gọi provider, không đổi hành vi app 4.22.0, không đưa FINAL của owner vào prompt, không thêm bộ kiểm/cổng đo ngoài phần cần cho bước 2.

## 6. Q1 offline design and measured result — 2026-10-14

Product decision served: whether speaker attribution is sufficient to justify a separately approved paid measurement of fewer false ADDRESS points and better coverage of actual self-address errors. This is an offline sidecar at baseline `9113523a40f808a9be4bdbc7d0e078ab337d1b95`, not a runtime change or a release gate. No provider/device/APK/commit operation was performed. Task1 files and canonical state/checklist are outside this task's write scope.

### 6.1 Contract and deterministic responsibilities

Choose two stages: attribute speakers first, then generate ADDRESS candidates from the attributed row. Preserve other finder types. Do not simply add labels after the old ADDRESS finder: that cannot discover a wrong form that the old finder missed because it is valid for another speaker.

Build a stable chapter roster from the valid seven-column pronoun rows, in CSV order, taking distinct `speaker` and `target` values. Assign opaque `C1`, `C2`, etc.; keep the original entity strings behind those IDs. Do not invent aliases by parsing prose notes. Exclude blank, `*`, `-`, `@NARRATOR`, `@AUDIENCE` from ordinary entity IDs. A specific table entity such as a named group or secondary speaker retains its own opaque ID; `@GROUP` means unresolved collective identity and never selects a row. Legacy three-column rows cannot establish a speaker pair.

The proposed attribution request contains the roster, numbered RAW lines, neighbouring RAW context, and the row notes needed to interpret the scene. Attribution is based on RAW; DRAFT word choice must not determine the speaker. The response contains only one assignment per requested line, for example (invented IDs):

```text
[12] C1 > C2
[13] ? > C2
[14] @NARRATOR > ?
[15] C1 > @GROUP
```

Use `?` for either unresolved role; never force alternating turns. `@NARRATOR` marks narration rather than a speaking character. Several listeners, a change of listener within a line, simultaneous speakers, and unidentified groups require abstention unless the CSV explicitly identifies that exact entity. A future span-based representation could handle these, but is not implemented here. The private reference labels cover every RAW line containing `「` or `『`, including embedded quotations; unresolved cases remain explicit. They do not claim to cover unquoted dialogue or every quote convention in other books.

Parser: exact numeric line IDs and roster IDs; reject duplicate, missing, unsolicited, out-of-range or overflowing line IDs, unknown characters, fences and extra prose. No partial response acceptance. The current sidecar returns an error to its caller; there is no provider or retry mechanism. A future attribution failure must leave DRAFT unchanged for the affected chunk; it must not silently fall back to broad ADDRESS edits.

Comparator: use the existing `ReferenceProjector` scope parser, chapter prefix and paragraph coordinates. Disable lexical cue filtering for the selected speaker's table lookup: an identified speaker need not name themselves in their own line. Select scoped exact speaker/listener rows before a scoped `target=*` default. Different self/call pairs for the same pair are a conflict, not a reason to guess from a short RAW cue. No row, incomplete self/call, unknown roles, or unresolved group produces a candidate. Equivalent forms in duplicate rows may share a representative row; notes are retained, not interpreted as executable scope or aliases.

For an eligible, one-to-one aligned line, extract its single outer DRAFT quotation, excluding surrounding narration and embedded quoted content. Multiple outer quotations or malformed nesting abstain. Compare tokens with the selected row's self/call token union. Candidate vocabulary is the existing `AddressChecklist.words()` plus atomic self/call forms present in the CSV, excluding character-name tokens. This is sufficient to expose a wrong form valid for another row without inventing a general vocabulary expansion. Each candidate retains the selected row and unexpected tokens. It is a candidate only: there is no replacement or generated corrected text.

Limits measured or pinned in synthetic tests: token union cannot distinguish a self form used as a call or vice versa; common nouns, plurals, kinship descriptions and third-person mentions still yield false candidates. Token membership is not semantic role identification. Notes may allow forms not encoded in self/call. This sidecar therefore does not support automatic substitution, even when one row is selected.

### 6.2 Future integration and cost choice (design only)

`EditorialPairRunService` would run attribution after the existing chunk plan and before ADDRESS detection, retaining line IDs and the selected row with the existing run. `FixPointFinder` would replace only its broad ADDRESS loop with these candidates; glossary, KANA, missing-line and punctuation work would continue independently. `TargetedFixPrompt` would receive one selected row per ADDRESS point and require its citation. `AddressEditGuard` must still preserve punctuation, names, plural forms and other existing content protections, and validate the proposed edit against that row. A single row does not grant permission to rewrite a sentence. Attribution results would need the existing source/plan binding to prevent reuse after inputs change; no new receipt system is proposed.

A separate attribution call per chunk makes row selection deterministic before asking for fixes, and can discover lines absent from the old finder. A combined attribution-and-fix call saves that extra call but asks the model to select all possible errors itself, prevents prior deterministic candidate selection, and expands the response/validation problem. Choose separate attribution for a possible future measurement; do not implement either in 4.22 runtime. With the measured default cuts here, attribution alone would add 11 calls for Kumo and 16 for JAKUAKU, before any necessary fix calls. These are measured chunk counts, not measured provider cost or a proposed paid budget. Full chapters, context, roster size, output/reasoning allowance and current pricing would need a separate bounded estimate if the stop condition below is resolved. No FINAL, private labels, historical owner judgement, or reference substitutions belong in a runtime prompt.

### 6.3 Inputs, provenance and comparison method

Inputs are exactly the RAW/DRAFT/glossary/pronoun files under `D:\P5E-private\v422-q0\inputs` for Kumo013 and `D:\P5E-private\q2-inputs\014` for JAKUAKU014. The tool never reads the FINAL file present beside the Kumo inputs. B reference indices/occurrences come from `D:\P5E-private\product-plan-A\stepB-self-cases.json`; source indexing was checked against `D:\P5E-private\v419-pronoun\line-facts.jsonl`. B indices are zero-based nonblank DRAFT units, converted to one-based units, not physical file lines.

Reference speaker/listener assignments and candidate judgements are session inferences from RAW, neighbouring narration, DRAFT and the supplied pronoun rows, not owner gold, model predictions or an independent accuracy test. The same session wrote the prototype and labels; this is an optimistic offline feasibility measurement. No historical owner acceptance or B label was overwritten. Literal name/pronoun call alternatives and a plural self case are counted as uncertain rather than declared wrong. Some historically owner-approved unchanged B lines differ from the old FINAL; reference matching must not reverse those owner judgements.

The actual baseline `FixPointFinder` is compiled from a clean `git archive` of the pinned engine main-source subtree and invoked on each real engine chunk, not reimplemented in a script. Planning uses app defaults: token mode, configured hard 450, balanced multiplier 1.5 -> effective hard 675 / soft 540, max output 4096, context 400, output factor 1.3, default series ratio. The offline token measure uses the exact arithmetic of `Chunker.approxTokens`. The sidecar uses the same plan/beads and abstains on non-one-to-one alignment. No Gradle invocation or shared build directory was used, so Task1's Gradle use is independent.

Historical Q0 is a different cut/configuration: its saved log has 4 chunks, 19 point lines, 18 ADDRESS-bearing lines and 3 GLOSSARY-bearing lines (types overlap). Those are direct counts from the existing artifact, not 19 independent ADDRESS errors. The request's shorthand count is retained above. The fresh defaults comparison below has 11 chunks / 17 total points / 16 ADDRESS lines on Kumo; do not pass it off as an exact recreation of the owner's Q0 settings. A preliminary whole-chapter run was rejected as the decision comparison because global row conflicts suppressed baseline ADDRESS; its evidence is retained as superseded.

### 6.4 Measured counts

Counts below are distinct DRAFT candidate lines, with semantic classes inferred in this session. Uncertain is separate from false; none is a measured app-model result.

| Chapter / method | ADDRESS candidate lines | Session true | Session false | Uncertain |
|---|---:|---:|---:|---:|
| Kumo013 existing finder | 16 | 1 | 15 | 0 |
| Kumo013 Q1 with reference assignments | 3 | 1 | 2 | 0 |
| JAKUAKU014 existing finder | 58 | 12 | 30 | 16 |
| JAKUAKU014 Q1 with reference assignments | 46 | 18 | 5 | 23 |

Kumo: 220 RAW / 220 DRAFT units; 48 quotation-bearing RAW lines, 40 have both roles assigned and 8 have an unknown role. Comparator outcomes: 3 candidates, 23 clean, 5 unresolved group, 1 incomplete row, 8 no scoped row, 8 unknown. Inferred false points fall 15 -> 2 (86.7%). The two residual false points concern third-person references, not the selected listener. The known physical DRAFT line 151 (unit 76) is detected. Physical line 346 (unit 174) is not: the applicable directed row in the supplied file is scoped to another paragraph. A wrong label or broader vocabulary cannot safely repair this missing scoped data. Known-error coverage remains 1/2 for both methods.

JAKUAKU: 273 RAW / 273 DRAFT units; 156 quotation-bearing RAW lines, 143 have both roles assigned and 13 have an unknown role. Comparator outcomes: 46 candidates, 54 clean, 6 unresolved group, 37 row conflict, 13 unknown. Inferred false points fall 30 -> 5 (83.3%); 23 candidate lines still require owner judgement, so 18/46 must not be reported as an independently measured precision score. The five inferred false candidates are relationship/plural wording, third-person references and ordinary nouns. Direct speaker labels do not solve token roles.

Known B reference coverage: 21 distinct lines / 34 recorded self substitutions. Existing finder flags 13 lines containing 24 reference substitutions; Q1 flags 18 lines containing 30. Q1 abstains at units 18 and 31 because the selected pair has conflicting call rows (3 recorded substitutions), and unit 132 is clean for the session-inferred speaker (1 recorded substitution); it is not silently credited as a repaired error. Consequently Q1 touches lines containing 30/34 reference substitutions, not 30 proven corrected occurrences. An ADDRESS point on a line does not establish that every substitution there is correct. The B table in private evidence retains each line's old-finder flag and precise Q1 outcome, including the historically accepted plural case. Nothing is changed in DRAFT.

### 6.5 Decision and handoff

There is a substantial local reduction in inferred false points with reference assignments and six additional session-inferred self-error candidate lines in JAKUAKU. However, Q1's criterion of catching both known Kumo errors is not met even with reference attribution; unresolved row data and token semantics remain. **Do not propose a paid measurement or a USD budget in this handoff.** First review the private row-scope finding and uncertain candidates; do not widen scope, reinterpret historical gold, or invest in another measurement apparatus. Model attribution accuracy, new-chapter generalisation, correction quality, actual spend and app integration are NOT_MEASURED. No holdout chapter has been consumed or labelled here.

Exact repository files in this task: this document (append-only design); new `scripts/q1/Q1SpeakerSidecar.java`, `scripts/q1/Q1SpeakerSyntheticTest.java`, `scripts/q1/Q1SpeakerMeasurement.java`, `scripts/q1/run-q1-offline.ps1`. These sources are outside app/engine source sets and cannot run in the app. No existing tests, production sources, Task1 files or state/checklist were edited. Leave all changes uncommitted for the main agent's combined scan/review.

Verification command, from `D:\P5E-builds\wt-v4.22`:

```powershell
.\scripts\q1\run-q1-offline.ps1
```

Final evidence: `D:\P5E-private\v422-q1\offline-20261014-155817-609\`. Contains the clean baseline engine source ZIP, exact sidecar source copy, compiled offline classes, compile log, **36 synthetic assertions PASS / 0 FAIL**, both summaries, both session assessments, input text hashes, per-line comparison TSVs, expanded reference labels, judgement-source TSVs, known-B table, two private RAW/context/DRAFT reading pages, and SHA256SUMS. Checks cover strict parsing, missing/duplicate/unknown IDs, overflow, one unknown role, narration/group abstention, wrong-speaker valid forms, scope bounds/chapter mismatch, row conflicts, wildcard precedence, nested quotation exclusion, common-case Unicode/case handling, third-party names, CSV-derived atomic forms and the explicit swapped-role limitation. The original Q0 evidence is untouched. Private source labels/judgements live at `D:\P5E-private\v422-q1\reference-{Kumo013,JAKUAKU014}.tsv` and `adjudication-{Kumo013,JAKUAKU014}.tsv`; their exact copies are retained in the final evidence directory. Earlier offline directories are diagnostic attempts, not additional model measurements. This is an engine-subtree archive with a Q1 overlay, not an APK or whole-release archive.

Next action for the main agent: review this isolated Q1 diff and private scope/uncertainty findings, scan the combined changes, and commit together if appropriate. No request for phone/provider/build/tag/merge/push authority is made.

Parallel Task1 handoff note: the parent reports U3 at `239c3299` plus test fix `9c467ffc`, contract `CP-IMPL-20`, with ADDRESS unchanged and new filters defaulting to all true. These Q1 numbers remain attributed to the archived finder at **`9113523a`**, not to a new Task1 regression or that later contract. Final evidence checksum verification: 628 entries matched; the four current Q1 source/script files matched their tested overlay copies. Diff whitespace check passed; a narrow scan of the five Q1 files found zero secret-pattern hits and zero complete input-book lines of at least 40 characters. This scan is supplemental; the parent's combined review/scan is still pending.
