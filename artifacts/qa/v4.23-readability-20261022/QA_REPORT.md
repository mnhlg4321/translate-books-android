# v4.23 dev.21 — bảng theo dõi dễ hiểu, kiểm chứng offline

Quyết định sản phẩm: owner đọc được trạng thái/đơn vị/luật và có vùng xem chunk hữu dụng. Đây là tiếp tục J3/J4/R0b/R3, không đổi release, prompt, schema, model, provider, engine hoặc nội dung lưu bền. Không đo chất lượng model bằng fixture.

## Baseline và provenance

| Đối tượng | Bằng chứng / kết luận |
|---|---|
| Checkout được phép | `D:/P5E-builds/wt-v4.23`, `feature/v4.23`; baseline54ef672d40f6d20f2f6a5b4d0fcf1755f6b48b36. Checkout v4.18 và media dev.14 không đổi. |
| Bản điện thoại gần nhất đã kiểm | dev.20/code298, source98a2e15e8487c4141a3ecabd711830c98acb9232; retained postinstall-base.apk SHA46C608AC8E90DBCE25409AB9EA5F21CF57B7C5A5025810E50C0354210BFA6991 khớp artifact. A2 tĩnh mở Công việc và A5 drawer có evidence PNG/XML; không chứng minh A2 đang dịch/A4/provider. Không query hoặc mutate điện thoại ở lượt redesign. |
| Production APK mới | **4.23-dev.21/code299**, source4849de1968b6287e99f407837761d13028ad6cc9; eventbuild-20261021-223903. APK SHA2421F66B216492F234D8348EDEFFCAB5587CA79EC77E28F93BB410ED10877806. Source ZIP SHABCB49F630EB73E1CE5FE9E686BC05D012323A0C1F1EFD759D1E55495253428FC. |
| Source hiện tại đã kiểm | deb5846a0f4891320328be2a217ca6168f1b1125 trước commit tài liệu; `git diff --quiet 4849de19 HEAD -- app/src/main editorial-engine scripts` PASS. Các commit sau production chỉ test/snapshot. |
| Test APK dùng nghiệm thu ảnh cuối | eventv423-readability-dev21-test4-20261022, sourcedeb5846a; SHA A9022F28423C6E7FB140A2739DF8634413F71E87F2547A76444C1FB8E43E23A0, source ZIP29E80D52D7D5301327730A9D190C28D33A9E953BC12D3AD1A39A546C3142F1EC. Signer47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155. |
| Môi trường | Git archive sạch, local.properties/dependency có sẵn; Gradle9.3/AGP8.7.3/JDK21/SDK35 offline. Wrapper production/test bắt buộc. Chỉ AVD riêng tbl-v423-preview-qa, serialemulator-5580; không pilot5554 hoặc phone15e84958. |

APK/source ZIP/README/BUILD_INFO/SHA256SUMS ở cả `artifacts/builds/v4.23-dev.21/build-20261021-223903/` và `backup/builds/...`; test artifact có hai mirror tương ứng. `verification.json` chứng minh byte/SHA mọi file, manifest, source ZIP bằng `git archive` đúng ref. Không gán PASS của working tree bẩn cho HEAD.

## Claim → evidence → kết luận

| Claim | Evidence | Kết luận |
|---|---|---|
| Bản cũ dùng nhãn khó hiểu | Diff54ef672d→4849de19: RunPanelFormatter clock/tokens/failures/rules; top status dot, TranslatePageFactory statBox/preview150dp | PROVEN: vấn đề trình bày, không phải lỗi model. |
| Thời gian và số liệu có nghĩa | `RunPanelFormatter:78–113`, unit boundary clock/unknown rules; ảnh screenshots-clean/run-panel-paper-100-360x800.png | PASS: phút/s, token đầy đủ, lỗi theo chunk, Glossaries/Pronoun đủ tên; estimate có nhãn. |
| fb là số fallback hữu dụng | `TranslatorService:99,475,1047,1067`: chỉ khởi tạo/reset/persist/broadcast | FALSE: không có increment; bỏ khỏi UI, không giả số đo. |
| Chấm xanh giải thích trạng thái | Mã baseline chủ động compact thành dot; MainActivity:814–836,1148–1169 mới + test sáu trạng thái và route | PASS: luôn có chữ, min48dp, mô tả Mở Công việc; màu chỉ bổ trợ. |
| Preview tiết kiệm chỗ nhưng đọc được | TranslatePageFactory:323–410, TranslationPreviewScrollView:7–18, TranslationDashboardFormatter:41–48; test ngắn/dài/gap/scroll/selection/Ẩn–Xem + ảnh trước/sau cuộn khác nhau | PASS offline: bỏ header lặp, tự co ngắn, viewport dài160–480dp theo45% màn; cuộn trong vùng và cuộn trang đều được. |
| Không làm mất sửa preview xuyên chương | V423PreviewInstrumentedTest3ca, V423ChapterRunInstrumentedTest9ca, source engine/service/store không đổi | PASS tổng hợp; không chứng minh event sách thật. |
| Mọi giao diện đã hoàn thiện | Audit skill + Luna + kiểm lại source, docs/V423_REFINED_UI_DESIGN.md mục9 | FALSE: mới đóng nhóm đang chạy; nhãn tiếng Anh, nút42/44dp, log chính và UI2.9 còn ở màn khác, đã có hướng sửa trong gói cũ. |
| Ảnh owner là đúng dev.20/model cụ thể | Ảnh có Accepted chunk tiếng Anh trong khi formatter dev.20 tiếng Việt; không có event id/response/ledger tương ứng | UNKNOWN: không gán exact version/prompt/schema/model/provider event. |

## Chẩn đoán và phạm vi hồi quy

- **Trình bày:** formatter rút ngắn tên và MM:SS; status compact vì bề rộng; stat card nhiều tầng, heading/metadata/padding lặp và viewport150dp. Tái hiện offline bằng RuntimeStateSnapshot tổng hợp có trạng thái, token/cost/time/rule và preview; không cần model. Sửa nhỏ ở formatter/factory/header, không tính lại nghiệp vụ. Màn hẹp/chữ lớn wrap và chuyển một cột, trạng thái tách hàng; chữ không tự nhỏ.
- **Các họ tương tự:** rà token/k/tok, G/P, fb, đơn vị unknown, trạng thái cả6 loại, preview rỗng/ngắn/dài, qua chương/đổi trang/recreate/callback sau destroy. Required/nullable của schema không đổi: thiếu ngữ nghĩa không tự điền; không có lý do mở live/parser/Editorial ở nhóm này.
- **Tính nhất quán:** source preview luôn đi với chunk đã nhận, khác chương hiện tại. Preview vẫn chỉ là phần đuôi1200ký tự có sẵn, không tuyên bố hiển thị toàn chương/chunk. Số luật thuộc request hiện tại; tổng token/cost thuộc lượt, dòng chương có nhãn riêng. Nhãn `s` là đơn vị owner yêu cầu. Không bắt model tính/hash/diff/trạng thái; app giữ toàn bộ sổ này.
- **Biên tập:** không thay kiến trúc/change rows/guard/parser hoặc reference FINAL. Reuse formatter/layout là ứng viên theo từng màn; không lan helper rộng làm thay đổi Biên tập chưa kiểm.

## Kiểm thử, sửa trượt và bằng chứng giữ lại

- App source sạch4849de19: **673 total =668 PASS/5 SKIP/0 FAIL/0 ERROR mỗi biến thể debug/release/benchmark**; lint0Error/0Fatal/58Warning. Wrapper production PASS, compile androidTest PASS sau sửa import. XML aggregate ở verification.json; raw build-repaired.log/variants.log ngoài Git. Không chạy lại engine: không đổi engine, kết quả741PASS/1SKIP cũ chỉ được dùng với provenanceeadfc2bb, không phải lần đo mới.
- Core device: **19/19 PASS/0SKIP/0FAIL**, test APK sourced6733148d, production4849de19: readability2, preview/lifecycle3, chapter service9(fake provider/SQLite thật), appearance5. base-summary.json + device-base-360-paper.txt. Test-only bổ sung sau đó chỉ làm capture/outer-scroll; không đổi production. Không cộng các lần chạy lặp thành test độc lập.
- Matrix cuối trên app data sạch riêng từng case: **2ca×5cấu hình =10 PASS/0SKIP/0FAIL**, test APK sourcedeb5846a, logsdevice-clean-*.txt, clean-matrix-summary.json. 360×800 Paper100%/system1.0;320×800 Paper130%/system1.3;800×360 Light100%/system1.0;800×1180 Dark100%/system1.0;360×800 Dark130%/system2.0. Đã kiểm ảnh đại diện đủ5cấu hình, cả phần preview sau cuộn; không tuyên bố kiểm cả17palette.
- Hai compile failure ban đầu: width float→int sửa4849de19; thiếu static assertNotNull import sửa6733148d. Giữ build.log/build-test.log/test-compile-diagnosis.log, kết quả sửa PASS cùng nhánh; không đổi acceptance hoặc dựng cổng mới.
- Ảnh chụp đầu có cặp trùng: chờ main-thread idle chưa đảm bảo compositor vẽ frame cuộn. Sửa test chờ UiAutomation idle200ms; ảnh cuối10PNG hợp lệ,5cặp trước/sau khác SHA. Giữ ảnh/log cũ, không sửa bitmap. Outer scroll và inner scroll có assertion thật.
- **Test artifact3 INVALID_SOURCE_PROVENANCE, không cài/không dùng PASS:** gọi wrapper bằng absolute path trong cwd worktree khiến Gradle build ở cwd, wrapper copy APK cũ ở archive. Source ZIP đúng nhưng binary chưa có sửa capture. Sửa cách gọi bằng cwd clean archive; test4 hash mới, replay matrix/ảnh PASS. Wrapper hiện cần caller đứng đúng root; đây là giới hạn công cụ cần nhớ, không ngụy nhận artifact3 đã kiểm đúng source. Production event ban đầu chạy từ clean archive, và main/scripts sau4849de19 không đổi.
- Matrix trước app-data-clean có selection RAW tổng hợp đã bị ca service xóa, nên background preparation báo FileAccessException và collapsed source3tệp. Không coi lỗi fixture này là provider lỗi hoặc phát sinh dữ liệu owner. Matrix cuối clear **chỉ package trên AVD riêng** từng case, không còn lỗi đó; restored runtime tổng hợp không phải job thật (dòng nguồn chuẩn bị có thể ghi chưa sẵn sàng). Không bấm Start/Dịch.

## Giới hạn và nghiệm thu

STRUCTURAL_VALID/SEMANTIC_EVAL của provider event ảnh owner: **NOT_MEASURED**. Không có response/finish reason/token/cost/ledger của event để phân xử chất lượng. Fake provider hồi quy không thay thế live. Khả năng lưu/mở lại preview được kiểm bằng ca lifecycle/store tổng hợp; xuất bản dịch thật của lượt ảnh: NOT_MEASURED. A2 đang dịch thật, A4, TalkBack và UI mới trên phone NOT_MEASURED. Không tự đề xuất lượt live vì câu hỏi UI giải quyết offline được.

Tiến độ ba chương FINAL006/008/011 owner accepted và P7 current chunk scope giữ nguyên lịch sử; không có FINAL mới hoặc nghiệm thu P7 mới. Provider0calls/USD0. Không cài phone, không pilot, không push/merge/tag. Luna audit/review readonly đã được coordinator kiểm lại; không giả nhận dùng Sonnet.

Raw evidence `D:/P5E-builds/v423-readability-qa-20261021/`; ảnh bàn giao **screenshots-clean/**. Hai thư mục ảnh khác trong `EVIDENCE_MANIFEST.json` **không dùng để nghiệm thu** (bổ sung 2026-10-22 sau khi owner phát hiện ảnh trùng): `screenshots-final/` là lần chụp trước khi chờ compositor, 5 cặp `run-panel-*`/`preview-area-*` trùng SHA từng đôi; `screenshots-settled/` chụp trước matrix app-data sạch, nên còn lỗi fixture FileAccessException. Chỉ `screenshots-clean/` (10 ảnh, SHA khác nhau, magic `89 50 4E 47`) là bằng chứng. Bản lưu an toàn và manifest evidence: `backup/qa/v4.23-readability-20261022/`. Báo cáo/summary/verification không chứa credential hay văn bản sách. Số phiên bản/date event theo metadata thật, lượt tiếp tục và chốt báo cáo2026-10-22(+07).

**Next action:** owner nghiệm thu giao diện dev.21 từ ảnh và APK đã lưu; cài điện thoại chỉ sau duyệt riêng theo AGENTS.
