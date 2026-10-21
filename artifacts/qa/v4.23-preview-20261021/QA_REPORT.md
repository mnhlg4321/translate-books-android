# v4.23 — dev.20 preview/lifecycle và cổng cài điện thoại (2026-10-21, +07)

Quyết định sản phẩm: khi dịch một tập, owner luôn đọc được chunk được chấp nhận gần nhất, biết nó thuộc chương nào và không mất trạng thái khi đổi màn. Tiếp tục thiết kế màn tiến trình bằng skill UI/UX đã tải. Không mở release/branch/checklist mới.

## Baseline và claim → evidence → kết luận

| Claim | Evidence kiểm độc lập | Kết luận |
|---|---|---|
| HEAD báo cáo `419bd52c` | HEAD/worktree tại lúc tiếp nhận khớp; chỉ media dev.14 untracked có sẵn | VERIFIED; checkout v4.18 bẩn giữ nguyên |
| Dev.18/code296 | BUILD_INFO: nguồn `a4243e1f`; APK SHA `3B7BD4B5A421306EDAA65C07AA6B81D25D579788CA1FBBDD3FCC6CA34DF30F51`; 4/4 checksum hợp lệ, mirrors giống byte; ZIP = git archive `a4243e1f` (3001 entries) | VERIFIED; HEAD tài liệu khác nguồn APK |
| Hồi quy dev.18 | XML tại `D:/P5E-builds/v423-clean-archive-a4243e1f`: engine 742 tổng = 741 PASS/1 SKIP; mỗi biến thể app 667 tổng = 662 PASS/5 SKIP; lint 58 Warning/0 Error | VERIFIED số liệu lưu; không chạy lại baseline |
| Dev.18 cài điện thoại và QA 3.1–3.6 PASS | Hồ sơ phiên trước + 6 PNG hợp lệ ở `D:/P5E-builds/phone-qa-v423-dev18-20261021` | Không kiểm lại trực tiếp điện thoại; chưa chứng minh job đang chạy/TalkBack |
| Dev.20 cài điện thoại `15e84958` | Không có service dịch đang chạy; backup đầy đủ ngoài Git; cài duy nhất qua `scripts/install-validated.ps1` với `-r` sau owner approval; package đọc lại code298/SHA đúng; tar dữ liệu trước/sau cùng SHA, 1380 entries | VERIFIED; không uninstall, không provider/API |
| A2 safe subcheck | Từ Dịch ở trạng thái dừng, chạm chip trạng thái mở đúng Công việc; ảnh `D:/P5E-builds/phone-qa-v423-dev20-20261021/a2-static-chip-jobs.png` PNG hợp lệ | PASS cho route tĩnh; active-translation variant `NOT_MEASURED` |
| A4 safe boundary | Không bấm Start/Dịch và không tạo tệp lẻ mới vì quy tắc cấm provider/spend | `NOT_MEASURED`; không có provider call |
| A5 so với dev.16 | Source dev.16 `c4715931` còn `buildPrimaryNavigation/tabBar`; source dev.20 không còn hai symbol này; drawer phone hiện đủ 6 nhãn kiểm tra; ảnh `D:/P5E-builds/phone-qa-v423-dev20-20261021/a5-drawer.png` PNG hợp lệ | PASS về cấu trúc hiển thị; không cài lại dev.16 |
| Ảnh owner cho thấy không có preview dù 2 chương xong | Ảnh hiển thị chương 3/23, chunk1/2, placeholder; mã xóa preview mỗi chương | Nguyên nhân phù hợp và tái hiện offline; không suy ra chưa lưu/mất bản dịch |
| Provider/model/prompt/schema của event trong ảnh | Chỉ thấy nhãn openrouter, ngôn ngữ và các số tổng trên UI; không thu response/ledger hay dữ liệu pilot | UNKNOWN; không gán config hiện tại cho event, không xác nhận USD0.0042 là ledger đã đối soát |

## Lỗi, nguyên nhân và bản sửa

1. **Preview bị xóa khi chuyển chương — presentation/runtime state.** Baseline `TranslatorService.java:466–480`; caller `runSinglePreparedJob:460`, `continueExistingJob:452`. `AcceptedTranslationPreview` giữ text + file + chunk + tổng của chính chunk đó qua `beginFile`; chỉ `beginRun` sau khi giành quyền chạy mới xóa. Mọi nhánh accept/retry/checkpoint dùng cùng helper. Bổ sung identity vào snapshot/store/broadcast; UI không lấy mẫu số của chương mới để gắn vào preview chương cũ. Không đổi parser/prompt/validator/output.
2. **Mở Dịch muộn sau recreate không khôi phục preview — reconstruction UI.** Baseline `MainActivity.java:835–859`; `onStart:608` chỉ khôi phục khi đã có trackingCard. Nay khôi phục khi mở Dịch, sau khi view đã được dựng. Ca thiết bị đi Jobs → recreate → Dịch kiểm text và tên chunk nguồn, có ảnh tổng hợp.
3. **Callback sau hủy Activity làm app văng — lifecycle.** Dev.19 log `instrumentation-final.txt`: `RejectedExecutionException`, `MainActivity.refreshPendingJobCard:364`; callback trễ sau `onDestroy` dùng executor đã shutdown. Sửa `submitActivityWork` cho cả hai executor, chặn callback render sau hủy. Ca mới giữ Activity đã hủy rồi gọi các đường refresh/dismiss/delete với danh sách rỗng; ca service liên tiếp kiểm thêm đường thật. Dev.19 không phải candidate cuối.
4. **Fixture thành công cũ bị thiếu dòng — bộ kiểm.** `V423ChapterRunInstrumentedTest.response` chỉ trả một câu cho RAW nhiều dòng; validator báo OUTPUT_MISSING_LINES, retry validation hợp lệ bị bộ kiểm truncation coi là gửi lặp. Chuyển riêng response thành công sang helper phủ số đoạn; giữ các ca ngoặc hỏng, cắt, lỗi xác thực/context. Không hạ validator hoặc acceptance. Lượt đầu giữ nguyên, đã dừng có chủ đích sau chẩn đoán fixture; lượt hai giữ nguyên crash thật. Không diễn giải các FAIL này thành chất lượng model.

Ownership: coordinator sửa/tích hợp/test; Luna đọc độc lập chẩn đoán và diff, không sửa tệp. Coordinator kiểm lại bằng mã, XML, hash, test thiết bị và ảnh. Không dùng/giả nhận Sonnet.

## Nguồn và kiểm chứng bản cuối

- Nhánh `feature/v4.23`; implementation/QA/APK source **`98a2e15e8487c4141a3ecabd711830c98acb9232`**. Hai commit giữa `06b542ce` và bản cuối chỉ sửa androidTest; crash fix mới làm phát sinh build dev.20.
- **4.23-dev.20/code298**, event `build-20261021-205039`; APK SHA-256 **`46C608AC8E90DBCE25409AB9EA5F21CF57B7C5A5025810E50C0354210BFA6991`**; ZIP SHA **`08F6801AC018A08B53CA861E1EBADC4FD58A8EE56C57437D74CBD13C0F98AD31`**. Payload cùng nội dung trong `artifacts/builds/v4.23-dev.20/build-20261021-205039/` và `backup/builds/...`.
- Clean Git archive của đúng source; chỉ bổ sung local.properties và junction archive ngoài nguồn theo môi trường. Wrapper bắt buộc chạy clean/test/lint/build, không build APK trực tiếp. Debug: **672 tổng / 667 PASS / 5 SKIP / 0 FAIL**; release và benchmark cùng số trên đúng source. Lint **0 Error / 58 Warning**. Engine không đổi: dùng kết quả 741 PASS/1 SKIP đã xác minh ở source `eadfc2bb`, không gọi nó là lượt chạy mới trên HEAD.
- AndroidTest wrapper event `v423-preview-dev20-test-20261021`: cùng commit và ZIP nguồn với APK, productionDiffCount=0, mirrors hợp lệ. Chỉ cài **máy ảo mới `tbl-v423-preview-qa`, `emulator-5580`**, không dùng AVD pilot. App được đặt lại dữ liệu tổng hợp của chính QA AVD trước lượt cuối.
- **13/13 PASS, 0 SKIP/FAIL/incomplete**: 3 ca preview/lifecycle; 9 ca chapter service (chuyển 3 chương, context/auth/validation stop/continue, resume, truncation/split, quote repair); 1 ca kích thước panel. Service/provider giả, SQLite và output thật của fixture. Assertion preview quan sát broadcast từ service trong lượt ba chương, không chỉ bơm text vào UI.
- Ảnh `D:/P5E-builds/v423-preview-qa-20261021/preview-dev20-recreated.png`: coordinator đã xem; chương hiện tại 002/chunk1/2, preview ghi đúng 001/chunk5/5; nội dung tổng hợp, không phải sách owner. Chữ preview 16sp, metadata/toggle14sp, giãn dòng1.4, thông báo tiếng Việt; giữ cuộn/chọn chữ/thu gọn.
- Logs/XML/checksum/ảnh/JSON đầy đủ: **`D:/P5E-builds/v423-preview-qa-20261021/`**. `instrumentation.txt` là lượt fixture FAIL bị dừng; `instrumentation-final.txt` là crash dev.19; `instrumentation-dev20.txt` và `device-summary-dev20.json` là lượt cuối PASS. Log install guard lần đầu từ chối token không khớp được giữ; cài lại đúng token máy ảo đọc được qua preflight, không bypass guard.
- Phone QA dev.20: backup trước cài `D:/App Translate Books/App Translate Books-device-backups/v4.23-dev.18-code296/20261021-211152/`; QA files và ảnh `D:/P5E-builds/phone-qa-v423-dev20-20261021/`; base trước SHA `3B7BD4B5A421306EDAA65C07AA6B81D25D579788CA1FBBDD3FCC6CA34DF30F51`; private-data tar SHA `168E77F8E489CCEDA5239F89409C6FBAEDC705A7C034891F2595686346580788`; post-install tar SHA identical; `postinstall-top.png` bắt đầu `89 50 4E 47`; A2 static ảnh SHA `79084B61170C3F6FCB0033F6D07A3138A05D7527230BEBEAEBD0B217255D59A1`; A5 drawer ảnh SHA `234A0A71AA225F5D9C2FB45FC8876EC55C0C747254166FA98DE47FCFE8A434ED`.

## Giới hạn và hướng đi

- STRUCTURAL_VALID: chỉ các fixture offline ở trên; app save/reopen/output đã đi qua các ca tổng hợp. **Event sách trong ảnh, SEMANTIC_EVAL mới, provider thật và TalkBack: NOT_MEASURED**. Dev.20 phone install/data-preservation/UI-open gate đã VERIFIED; A2 active-translation và A4 single-file run là `NOT_MEASURED` vì không được bấm Start/Dịch và provider bị cấm. A2 static chip route và A5 structural comparison PASS. Không dùng synthetic FINAL để kết luận chất lượng dịch.
- Không thay kiến trúc Editorial, schema DB, prompt, model, giá, hoặc sổ chi; không cần model làm thêm ghi sổ. Ba chương FINAL 006/008/011 owner đã nhận và giới hạn từng chương giữ nguyên; P7 current chunk scope PASS lịch sử, không đo/nâng nhãn lại.
- Thiết kế tiếp theo được cụ thể hóa trong `docs/V423_REFINED_UI_DESIGN.md` mục8; ưu tiên bảng tiến trình phẳng và phân biệt chương đang chạy với nguồn preview. Chưa triển khai toàn bộ R0a–R5; phương án drawer A mới nhất thay cho nav dưới trong bản thiết kế cũ.
- 0 translation/editorial provider calls, USD0 trong cổng cài/QA này. Không uninstall, không schema/editorial change, không push/merge/tag. Media untracked cũ giữ nguyên.
- **Next action:** chờ owner cấp spend authority riêng nếu muốn đo A2 active/A4 single-file; không tự gọi provider và không cài lại dev.16.

Câu giao việc: “Duyệt cài 4.23-dev.20/code298 sau khi lượt dịch hiện tại kết thúc; sao lưu đầy đủ trước cài, giữ dữ liệu, xác nhận preview và điều hướng; không tự gọi provider.” Đã hoàn tất cổng cài; A2 active/A4 single-file vẫn `NOT_MEASURED`, A2 static route và A5 structural comparison đã kiểm an toàn.
