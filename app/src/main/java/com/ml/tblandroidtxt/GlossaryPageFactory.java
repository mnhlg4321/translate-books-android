package com.ml.tblandroidtxt;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


class GlossaryPageFactory {
    static final String IMPORT_LABEL = "+ Nhập Glossary";
    static final String NEW_BLANK_LABEL = "+ Tạo Glossary trống";
    static final String HEALTH_CHECK_LABEL = "Kiểm tra dữ liệu";
    static final String USING_LABEL = "Đang dùng";
    static final String USE_LABEL = "Dùng";
    static final String EDIT_LABEL = "Sửa";
    static final String DELETE_LABEL = "Xóa";
    static final String BACK_LABEL = "← Quay lại";
    static final String USE_FOR_TRANSLATION_LABEL = "Dùng cho bản dịch";
    static final String MERGE_FILES_LABEL = "Gộp file";
    static final String SAVE_LABEL = "Lưu";
    static final String PREVIEW_LABEL = "Xem trước";
    static final String ADD_TERM_LABEL = "+ Thêm thuật ngữ";

    private final MainActivity a;

    GlossaryPageFactory(MainActivity activity) {
        this.a = activity;
    }


    View buildGlossariesPage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        if (GlossaryStore.isCorrupt(a)) { a.editingGlossary = null; root.addView(a.libraryLockedCard("Glossary")); }
        else if (a.editingGlossary == null) root.addView(glossaryListPage());
        else root.addView(glossaryEditorPage());
        return scroll;
    }


    View glossaryListPage() {
        LinearLayout panel = a.plainCard();
        TextView desc = a.text("Quản lý nhiều Glossary giống TBL PC: có thể chọn nhiều file cùng lúc, mỗi file tạo một Glossary riêng.", 13, a.MUTED, false);
        desc.setSingleLine(false);
        panel.addView(desc, a.marginLP(-1, -2, 0, 0, 0, 10));
        LinearLayout importActions = a.rowContainer();
        importActions.addView(a.primaryButton(IMPORT_LABEL, v -> a.chooseGlossary()), new LinearLayout.LayoutParams(0, -2, 1));
        importActions.addView(a.space(8, 1));
        importActions.addView(a.secondaryButton(NEW_BLANK_LABEL, v -> { if (!a.ensureConfigMutable()) return; a.editingGlossary = GlossaryStore.create(a, "New glossary"); if (a.editingGlossary == null) a.toast(MainActivity.LIBRARY_WRITE_REFUSED); a.invalidatePage("Glossaries"); a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(importActions);
        panel.addView(a.secondaryButton(HEALTH_CHECK_LABEL, v -> a.showGlossaryPronounHealth()), a.marginLP(-1, -2, 0, 8, 0, 0));

        GlossaryStore.Glossary activeGlossary = GlossaryStore.selected(a);
        TextView active = a.text(activeGlossary == null ? "Glossary đang dùng: —" : "Glossary đang dùng: " + activeGlossary.name + " • " + activeGlossary.count() + " mục", 13, activeGlossary == null ? a.MUTED : a.GREEN, true);
        active.setSingleLine(false);
        panel.addView(active, a.marginLP(-1, -2, 0, 10, 0, 12));

        List<GlossaryStore.Glossary> list = GlossaryStore.loadAll(a);
        LibrarySelectionController selection = a.bulkUi.controller(LibraryBulkUi.TAB_GLOSSARIES);
        if (selection.isActive()) {
            List<String> ids = new ArrayList<>();
            for (GlossaryStore.Glossary g : list) ids.add(g.id);
            panel.addView(a.bulkUi.selectionHeader(LibraryBulkUi.TAB_GLOSSARIES, ids), a.marginLP(-1, -2, 0, 0, 0, 10));
        } else if (!list.isEmpty()) {
            panel.addView(a.bulkUi.headerRow(LibraryBulkUi.TAB_GLOSSARIES, list.size()), a.marginLP(-1, -2, 0, 0, 0, 10));
        }
        if (list.isEmpty()) {
            TextView empty = a.text("Chưa có Glossary. Bấm Nhập Glossary để tạo profile mang đúng tên file, hoặc Tạo Glossary trống để nhập thủ công / ghép nhiều file.", 14, a.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setSingleLine(false);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, a.dp(110)));
        } else {
            String selectedId = GlossaryStore.getSelectedId(a);
            for (GlossaryStore.Glossary g : list) {
                boolean isActive = TextUtils.equals(g.id, selectedId);
                View card = selection.isActive()
                        ? a.bulkUi.selectableCard(LibraryBulkUi.TAB_GLOSSARIES, g.id, a.nonEmpty(g.name, "Glossary chưa đặt tên"), cardMeta(g) + (isActive ? " • Đang dùng" : ""), isActive ? a.GREEN : a.BORDER)
                        : glossaryItemCard(g, isActive);
                panel.addView(card, a.marginLP(-1, -2, 0, 0, 0, 10));
            }
        }
        return panel;
    }


    View glossaryItemCard(GlossaryStore.Glossary g, boolean active) {
        LinearLayout box = a.card(14, a.FIELD, active ? a.GREEN : a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));

        LinearLayout titleRow = a.rowContainer();
        TextView title = a.text(a.nonEmpty(g.name, "Glossary chưa đặt tên"), 15, active ? a.GREEN : a.TEXT, true);
        title.setSingleLine(false);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        if (active) titleRow.addView(a.chip(USING_LABEL, a.GREEN, true));
        box.addView(titleRow);

        String meta = cardMeta(g);
        TextView m = a.text(meta, 12, a.MUTED, false);
        m.setSingleLine(false);
        box.addView(m, a.marginLP(-1, -2, 0, 4, 0, 10));

        LinearLayout buttons = a.rowContainer();
        if (!active) {
            buttons.addView(a.tinyButton(USE_LABEL, a.BLUE, v -> a.selectGlossary(g)), new LinearLayout.LayoutParams(0, -2, 1));
            buttons.addView(a.space(6, 1));
        }
        buttons.addView(a.tinyButton(EDIT_LABEL, a.BLUE, v -> { if (!a.ensureConfigMutable()) return; a.editingGlossary = g; a.invalidatePage("Glossaries"); a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(a.space(6, 1));
        buttons.addView(a.tinyButton(DELETE_LABEL, a.RED, v -> a.bulkUi.deleteIds(LibraryBulkUi.TAB_GLOSSARIES, java.util.Collections.singletonList(g.id))), new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(buttons);
        a.bulkUi.attachLongPress(box, LibraryBulkUi.TAB_GLOSSARIES, g.id);
        return box;
    }


    void confirmDeleteGlossary(GlossaryStore.Glossary g) {
        if (!a.ensureConfigMutable()) return;
        if (g == null) return;
        new AlertDialog.Builder(a)
                .setTitle("Xóa Glossary?")
                .setMessage("Xóa Glossary: " + a.nonEmpty(g.name, "Glossary chưa đặt tên") + "\nKhông xóa file gốc trong máy.")
                .setPositiveButton(DELETE_LABEL, (d, w) -> {
                    boolean selected = g.id != null && g.id.equals(GlossaryStore.getSelectedId(a));
                    if (!GlossaryStore.delete(a, g.id)) { a.toast(MainActivity.LIBRARY_WRITE_REFUSED); return; }
                    if (selected) a.clearGlossarySelection();
                    a.toast("Đã xóa Glossary: " + a.nonEmpty(g.name, "Glossary chưa đặt tên"));
                    a.invalidatePage("Glossaries"); a.switchTab("Glossaries");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    /** Counts by category; languages only when someone filled them in. */
    static String cardMeta(GlossaryStore.Glossary g) {
        int characters = 0;
        if (g.terms != null) for (GlossaryStore.Term t : g.terms) if ("character".equalsIgnoreCase(t.category == null ? "" : t.category.trim())) characters++;
        String meta = g.count() + " mục · " + characters + " nhân vật · " + (g.count() - characters) + " thuật ngữ";
        boolean hasLanguages = !isBlankLanguage(g.sourceLang) || !isBlankLanguage(g.targetLang);
        return hasLanguages ? meta + " • " + label(g.sourceLang) + " → " + label(g.targetLang) : meta;
    }

    private static boolean isBlankLanguage(String value) { return value == null || value.trim().isEmpty() || "—".equals(value.trim()); }

    private static String label(String value) { return isBlankLanguage(value) ? "—" : value.trim(); }


    View glossaryEditorPage() {
        LinearLayout panel = a.sectionCard("", a.editingGlossary.name == null ? "Glossary" : a.editingGlossary.name);
        LinearLayout toolbar = new LinearLayout(a);
        toolbar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout toolbarTop = a.rowContainer();
        toolbarTop.addView(a.secondaryButton(BACK_LABEL, v -> { a.saveEditingGlossary(); a.editingGlossary = null; a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, -2, 1));
        toolbarTop.addView(a.space(8, 1));
        toolbarTop.addView(a.primaryButton(USE_FOR_TRANSLATION_LABEL, v -> { a.saveEditingGlossary(); a.selectGlossary(a.editingGlossary); }), new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(toolbarTop);
        toolbar.addView(a.dangerButton("Xóa Glossary", v -> confirmDeleteGlossary(a.editingGlossary)), a.marginLP(-1, -2, 0, 8, 0, 0));
        panel.addView(toolbar, a.marginLP(-1, -2, 0, 0, 0, 12));

        a.glossaryNameField = a.input("Tên", a.editingGlossary.name);
        a.glossarySourceField = a.input("Ngôn ngữ nguồn", a.editingGlossary.sourceLang);
        a.glossaryTargetField = a.input("Ngôn ngữ đích", a.editingGlossary.targetLang);
        panel.addView(a.fieldBlock("Tên", a.glossaryNameField));
        panel.addView(a.twoFields(a.fieldBlock("Ngôn ngữ nguồn", a.glossarySourceField), a.fieldBlock("Ngôn ngữ đích", a.glossaryTargetField)));

        LinearLayout tools = new LinearLayout(a);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.addView(a.primaryButton(MERGE_FILES_LABEL, v -> a.chooseGlossaryMulti()), new LinearLayout.LayoutParams(-1, -2));
        LinearLayout toolsBottom = a.rowContainer();
        toolsBottom.addView(a.secondaryButton(SAVE_LABEL, v -> { a.saveEditingGlossary(); a.toast("Đã lưu Glossary: " + a.editingGlossary.name); a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, -2, 1));
        toolsBottom.addView(a.space(8, 1));
        toolsBottom.addView(a.secondaryButton(PREVIEW_LABEL, v -> { a.saveEditingGlossary(); a.updateGlossaryPreview(); }), new LinearLayout.LayoutParams(0, -2, 1));
        toolsBottom.addView(a.space(8, 1));
        toolsBottom.addView(a.secondaryButton(HEALTH_CHECK_LABEL, v -> { a.saveEditingGlossary(); a.showGlossaryPronounHealth(); }), new LinearLayout.LayoutParams(0, -2, 1));
        tools.addView(toolsBottom, a.marginLP(-1, -2, 0, 8, 0, 0));
        panel.addView(tools, a.marginLP(-1, -2, 0, 0, 0, 12));

        LinearLayout add = a.card(14, a.FIELD, a.BORDER);
        add.setOrientation(LinearLayout.VERTICAL);
        add.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        add.addView(a.text("Thêm thuật ngữ", 15, a.TEXT, true));
        a.manualGlossarySource = a.input("Thuật ngữ nguồn", "");
        a.manualGlossaryTarget = a.input("Thuật ngữ đích", "");
        a.manualGlossaryCategory = a.input("Loại", "character");
        add.addView(a.twoFields(a.fieldBlock("Thuật ngữ nguồn", a.manualGlossarySource), a.fieldBlock("Thuật ngữ đích", a.manualGlossaryTarget)));
        add.addView(a.fieldBlock("Loại", a.manualGlossaryCategory));
        add.addView(a.secondaryButton(ADD_TERM_LABEL, v -> a.addManualTerm()), new LinearLayout.LayoutParams(-1, -2));
        panel.addView(add, a.marginLP(-1, -2, 0, 12, 0, 12));

        a.glossaryPreview = a.text("", 12, a.MUTED, false);
        a.glossaryPreview.setTextIsSelectable(true);
        LinearLayout previewCard = a.card(14, a.FIELD, a.BORDER);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        previewCard.addView(a.text("Xem trước Glossary • " + a.editingGlossary.count() + " mục", 15, a.TEXT, true));
        previewCard.addView(a.glossaryPreview);
        panel.addView(previewCard);
        a.updateGlossaryPreview();
        return panel;
    }
}
