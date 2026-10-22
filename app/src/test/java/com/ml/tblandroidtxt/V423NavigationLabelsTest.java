package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** v4.23 section 10: the translation and library action labels stay localized while technical terms remain English. */
public class V423NavigationLabelsTest {
    @Test public void translateLabelsUseVietnameseActionsAndEnglishTechnicalTerms() {
        assertEquals("Ngôn ngữ nguồn", TranslatePageFactory.SOURCE_LANGUAGE_LABEL);
        assertEquals("Ngôn ngữ đích", TranslatePageFactory.TARGET_LANGUAGE_LABEL);
        assertEquals("Xem prompt", TranslatePageFactory.PROMPT_PREVIEW_LABEL);
        assertEquals("Cập nhật giá model", TranslatePageFactory.MODEL_PRICING_LABEL);
        assertEquals("Chi tiết kỹ thuật", TranslatePageFactory.TECHNICAL_DETAILS_LABEL);
        assertEquals("Log hoạt động", TranslatePageFactory.ACTIVITY_LOG_LABEL);
        assertEquals("Xóa log", TranslatePageFactory.CLEAR_LOG_LABEL);
        assertEquals("Xuất log", TranslatePageFactory.EXPORT_LOG_LABEL);
    }

    @Test public void glossaryActionsAreVietnamese() {
        assertEquals("+ Nhập Glossary", GlossaryPageFactory.IMPORT_LABEL);
        assertEquals("+ Tạo Glossary trống", GlossaryPageFactory.NEW_BLANK_LABEL);
        assertEquals("Kiểm tra dữ liệu", GlossaryPageFactory.HEALTH_CHECK_LABEL);
        assertEquals("Đang dùng", GlossaryPageFactory.USING_LABEL);
        assertEquals("Dùng", GlossaryPageFactory.USE_LABEL);
        assertEquals("Sửa", GlossaryPageFactory.EDIT_LABEL);
        assertEquals("Xóa", GlossaryPageFactory.DELETE_LABEL);
        assertEquals("Lưu", GlossaryPageFactory.SAVE_LABEL);
        assertEquals("Xem trước", GlossaryPageFactory.PREVIEW_LABEL);
        assertEquals("+ Thêm thuật ngữ", GlossaryPageFactory.ADD_TERM_LABEL);
    }

    @Test public void pronounActionsAreVietnamese() {
        assertEquals("+ Nhập Pronoun", PronounPageFactory.IMPORT_LABEL);
        assertEquals("Kiểm tra dữ liệu", PronounPageFactory.HEALTH_CHECK_LABEL);
        assertEquals("Đang dùng", PronounPageFactory.USING_LABEL);
        assertEquals("Dùng", PronounPageFactory.USE_LABEL);
        assertEquals("Sửa", PronounPageFactory.EDIT_LABEL);
        assertEquals("Xóa", PronounPageFactory.DELETE_LABEL);
        assertEquals("Đổi tên", PronounPageFactory.RENAME_LABEL);
        assertEquals("Thay file", PronounPageFactory.REPLACE_FILE_LABEL);
    }
}
