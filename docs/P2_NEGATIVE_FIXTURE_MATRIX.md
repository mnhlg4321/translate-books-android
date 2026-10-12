# P2 — Negative fixture matrix

Ngày: `2026-09-03` (+07:00).

Mỗi case dưới đây được tạo độc lập từ canonical/control payload và chỉ thay đổi nguyên nhân được nêu. Các fixture đều test-only; không fixture nào được đưa vào `app/src/main` hoặc production APK.

| ID | Single cause | Fixture construction | Expected result | Evidence |
|---|---|---|---|---|
| NEG-001 | One-byte authority drift | Flip one byte trong `prompt.txt`, giữ nguyên manifest | `INTEGRITY_INVALID` | `EditorialP2ReferencePackImportInstrumentedTest.independentNegativeFixturesRemainTypedAndFailClosed` |
| NEG-002 | Wrong byte length | Tăng `fileRoles[prompt].byteLength`, recompute canonical hash | `INTEGRITY_INVALID` | P2 Android test; manifest representation cũng được kiểm tra trong P2 host test |
| NEG-003 | Wrong per-file hash | Đổi riêng SHA của `prompt.txt`, recompute canonical hash | `INTEGRITY_INVALID` | P2 Android test |
| NEG-004 | Wrong canonical pack hash | Đặt `canonicalPackHash` về zero hash, không sửa payload | `INTEGRITY_INVALID` | P2 host/Android tests |
| NEG-005 | Missing manifest | ZIP chỉ còn ba authority entry | `INTEGRITY_INVALID` | P2 Android test |
| NEG-006 | Missing authority entry | Bỏ `workflow.txt` | `INTEGRITY_INVALID` | P2 Android test |
| NEG-007 | Extra entry | Thêm `extra.txt` thành entry thứ năm | `ENTRY_COUNT_LIMIT` | P2 Android test |
| NEG-008 | Nested path | Dùng `nested/project.txt` | `INVALID_ENTRY_PATH` | P2 Android test |
| NEG-009 | Path traversal | Dùng `../editorial-pack.json` | `INVALID_ENTRY_PATH` | P2 Android test |
| NEG-010 | Duplicate normalized path | Dùng đồng thời `prompt.txt` và `PROMPT.TXT` | `DUPLICATE_NORMALIZED_PATH` | P2 Android test |
| NEG-011 | Duplicate role | Đổi role của `prompt.txt` thành `PROJECT_INSTRUCTION` | `INTEGRITY_INVALID` | P2 host manifest code `DUPLICATE_FILE_ROLE`; P2 Android importer test |
| NEG-012 | Symlink | `EditorialPackImportEntry` đánh dấu manifest là symlink | `SYMLINK_FORBIDDEN` | P2 Android test |
| NEG-013 | UTF-8 BOM | Thêm BOM vào manifest | `INTEGRITY_INVALID` | P2 host code `BOM_FORBIDDEN`; P2 Android importer test |
| NEG-014 | Wrong contract | Đổi `contractVersion`, recompute canonical hash | `STORED_BLOCKED / BLOCKED` | P2 Android test; compatibility seam hiện hành được giữ nguyên |
| NEG-015 | Unknown required capability | Thêm `future.required.capability.v1` | `STORED_BLOCKED / ENGINE_UPGRADE_REQUIRED`, capability được nêu | P2 Android test dùng trusted resolver |
| NEG-016 | Truncated ZIP | Cắt byte cuối của control ZIP | `TRUNCATED_STREAM` | P2 Android test |
| NEG-017 | Entry size limit | Tạo entry manifest lớn hơn `MAX_FILE_BYTES` | `ENTRY_SIZE_LIMIT` | P2 Android test |
| NEG-018 | Compression ratio limit | Tạo payload lặp `A` 20,000 bytes | `COMPRESSION_RATIO_LIMIT` | P2 Android test |

Các negative case không tự chứng nhận pack runnable. Riêng NEG-016 trên canonical ZIP vẫn phải giữ `TRUNCATED_STREAM`; GAP-012 là lỗi xử lý transport của importer, không phải lý do thay đổi authority hoặc canonical identity.
