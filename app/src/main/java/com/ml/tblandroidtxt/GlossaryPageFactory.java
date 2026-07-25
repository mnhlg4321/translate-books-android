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
    private final MainActivity a;

    GlossaryPageFactory(MainActivity activity) {
        this.a = activity;
    }


    View buildGlossariesPage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        if (a.editingGlossary == null) root.addView(glossaryListPage());
        else root.addView(glossaryEditorPage());
        return scroll;
    }


    View glossaryListPage() {
        LinearLayout panel = a.sectionCard("📖", "Glossaries");
        TextView desc = a.text("Quản lý nhiều glossary giống TBL PC: mỗi glossary có nút Use/Edit/Del riêng, dễ bấm trên màn hình dọc.", 13, a.MUTED, false);
        desc.setSingleLine(false);
        panel.addView(desc, a.marginLP(-1, -2, 0, 0, 0, 10));
        LinearLayout importActions = a.rowContainer();
        importActions.addView(a.primaryButton("+ Import glossary", v -> a.chooseGlossary()), new LinearLayout.LayoutParams(0, a.dp(48), 1));
        importActions.addView(a.space(8, 1));
        importActions.addView(a.secondaryButton("+ New blank", v -> { if (!a.ensureConfigMutable()) return; a.editingGlossary = GlossaryStore.create(a, "New glossary"); a.invalidatePage("Glossaries"); a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, a.dp(48), 1));
        panel.addView(importActions);
        panel.addView(a.secondaryButton("Health check", v -> a.showGlossaryPronounHealth()), a.marginLP(-1, a.dp(44), 0, 8, 0, 0));

        GlossaryStore.Glossary activeGlossary = GlossaryStore.selected(a);
        TextView active = a.text(activeGlossary == null ? "Active glossary: —" : "Active glossary: " + activeGlossary.name + " • " + activeGlossary.count() + " terms", 13, activeGlossary == null ? a.MUTED : a.GREEN, true);
        active.setSingleLine(false);
        panel.addView(active, a.marginLP(-1, -2, 0, 10, 0, 12));

        List<GlossaryStore.Glossary> list = GlossaryStore.loadAll(a);
        if (list.isEmpty()) {
            TextView empty = a.text("Chưa có glossary. Bấm Import glossary để tạo profile mang đúng tên file, hoặc New blank để nhập thủ công / ghép nhiều file.", 14, a.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setSingleLine(false);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, a.dp(110)));
        } else {
            String selectedId = GlossaryStore.getSelectedId(a);
            for (GlossaryStore.Glossary g : list) {
                panel.addView(glossaryItemCard(g, TextUtils.equals(g.id, selectedId)), a.marginLP(-1, -2, 0, 0, 0, 10));
            }
        }
        return panel;
    }


    View glossaryItemCard(GlossaryStore.Glossary g, boolean active) {
        LinearLayout box = a.card(14, a.FIELD, active ? a.GREEN : a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));

        LinearLayout titleRow = a.rowContainer();
        TextView title = a.text((active ? "✓ " : "📘 ") + a.nonEmpty(g.name, "Unnamed glossary"), 15, active ? a.GREEN : a.TEXT, true);
        title.setSingleLine(false);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        if (active) titleRow.addView(a.chip("ACTIVE", a.GREEN, true));
        box.addView(titleRow);

        String meta = a.nonEmpty(g.sourceLang, "—") + " → " + a.nonEmpty(g.targetLang, "—") + " • " + g.count() + " terms";
        TextView m = a.text(meta, 12, a.MUTED, false);
        m.setSingleLine(false);
        box.addView(m, a.marginLP(-1, -2, 0, 4, 0, 10));

        LinearLayout buttons = a.rowContainer();
        buttons.addView(a.tinyButton(active ? "Using" : "Use", active ? a.GREEN : a.BLUE, v -> a.selectGlossary(g)), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        buttons.addView(a.space(6, 1));
        buttons.addView(a.tinyButton("Edit", a.BLUE, v -> { if (!a.ensureConfigMutable()) return; a.editingGlossary = g; a.invalidatePage("Glossaries"); a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        buttons.addView(a.space(6, 1));
        buttons.addView(a.tinyButton("Del", a.RED, v -> confirmDeleteGlossary(g)), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        box.addView(buttons);
        return box;
    }


    void confirmDeleteGlossary(GlossaryStore.Glossary g) {
        if (!a.ensureConfigMutable()) return;
        if (g == null) return;
        new AlertDialog.Builder(a)
                .setTitle("Delete glossary?")
                .setMessage("Xóa glossary: " + a.nonEmpty(g.name, "Unnamed glossary") + "\nKhông xóa file gốc trong máy.")
                .setPositiveButton("Delete", (d, w) -> {
                    boolean selected = g.id != null && g.id.equals(GlossaryStore.getSelectedId(a));
                    GlossaryStore.delete(a, g.id);
                    if (selected) a.clearGlossarySelection();
                    a.toast("Đã xóa glossary: " + a.nonEmpty(g.name, "Unnamed"));
                    a.invalidatePage("Glossaries"); a.switchTab("Glossaries");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    LinearLayout glossaryRow(String name, String src, String tgt, String terms, String actions, boolean header, boolean active, GlossaryStore.Glossary g) {
        LinearLayout row = a.rowContainer();
        row.setPadding(a.dp(8), a.dp(6), a.dp(8), a.dp(6));
        int col = header ? a.MUTED : a.TEXT;
        TextView n = a.text(name, header ? 11 : 13, active ? a.GREEN : (header ? a.MUTED : a.BLUE), true);
        TextView s = a.text(src == null || src.isEmpty() ? "—" : src, header ? 11 : 13, col, header);
        TextView t = a.text(tgt == null || tgt.isEmpty() ? "—" : tgt, header ? 11 : 13, col, header);
        TextView c = a.text(terms, header ? 11 : 13, col, true);
        row.addView(n, new LinearLayout.LayoutParams(0, -2, 2.2f));
        row.addView(s, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(c, new LinearLayout.LayoutParams(0, -2, .75f));
        if (header) row.addView(a.text(actions, 11, a.MUTED, true), new LinearLayout.LayoutParams(0, -2, 1.8f));
        else {
            LinearLayout buttons = a.rowContainer();
            buttons.addView(a.tinyButton(active ? "✓" : "Use", active ? a.GREEN : a.BLUE, v -> a.selectGlossary(g)));
            buttons.addView(a.tinyButton("Edit", a.BLUE, v -> { if (!a.ensureConfigMutable()) return; a.editingGlossary = g; a.switchTab("Glossaries"); }));
            buttons.addView(a.tinyButton("Del", a.RED, v -> { if (!a.ensureConfigMutable()) return; GlossaryStore.delete(a, g.id); a.toast("Đã xóa glossary"); a.switchTab("Glossaries"); }));
            row.addView(buttons, new LinearLayout.LayoutParams(0, -2, 1.8f));
        }
        return row;
    }


    View glossaryEditorPage() {
        LinearLayout panel = a.sectionCard("📖", a.editingGlossary.name == null ? "Glossary" : a.editingGlossary.name);
        LinearLayout toolbar = new LinearLayout(a);
        toolbar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout toolbarTop = a.rowContainer();
        toolbarTop.addView(a.secondaryButton("← Back", v -> { a.saveEditingGlossary(); a.editingGlossary = null; a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        toolbarTop.addView(a.space(8, 1));
        toolbarTop.addView(a.primaryButton("Use for translation", v -> { a.saveEditingGlossary(); a.selectGlossary(a.editingGlossary); }), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        toolbar.addView(toolbarTop);
        toolbar.addView(a.dangerButton("Delete glossary", v -> confirmDeleteGlossary(a.editingGlossary)), a.marginLP(-1, a.dp(44), 0, 8, 0, 0));
        panel.addView(toolbar, a.marginLP(-1, -2, 0, 0, 0, 12));

        a.glossaryNameField = a.input("Name", a.editingGlossary.name);
        a.glossarySourceField = a.input("Source", a.editingGlossary.sourceLang);
        a.glossaryTargetField = a.input("Target", a.editingGlossary.targetLang);
        panel.addView(a.fieldBlock("NAME", a.glossaryNameField));
        panel.addView(a.twoFields(a.fieldBlock("SOURCE", a.glossarySourceField), a.fieldBlock("TARGET", a.glossaryTargetField)));

        LinearLayout tools = new LinearLayout(a);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.addView(a.primaryButton("Import files", v -> a.chooseGlossaryMulti()), new LinearLayout.LayoutParams(-1, a.dp(44)));
        LinearLayout toolsBottom = a.rowContainer();
        toolsBottom.addView(a.secondaryButton("Save", v -> { a.saveEditingGlossary(); a.toast("Đã lưu glossary: " + a.editingGlossary.name); a.switchTab("Glossaries"); }), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        toolsBottom.addView(a.space(8, 1));
        toolsBottom.addView(a.secondaryButton("Preview", v -> { a.saveEditingGlossary(); a.updateGlossaryPreview(); }), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        toolsBottom.addView(a.space(8, 1));
        toolsBottom.addView(a.secondaryButton("Health", v -> { a.saveEditingGlossary(); a.showGlossaryPronounHealth(); }), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        tools.addView(toolsBottom, a.marginLP(-1, -2, 0, 8, 0, 0));
        panel.addView(tools, a.marginLP(-1, -2, 0, 0, 0, 12));

        LinearLayout add = a.card(14, a.FIELD, a.BORDER);
        add.setOrientation(LinearLayout.VERTICAL);
        add.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        add.addView(a.text("Add a term", 15, a.TEXT, true));
        a.manualGlossarySource = a.input("Source term", "");
        a.manualGlossaryTarget = a.input("Target term", "");
        a.manualGlossaryCategory = a.input("Category", "character");
        add.addView(a.twoFields(a.fieldBlock("SOURCE", a.manualGlossarySource), a.fieldBlock("TARGET", a.manualGlossaryTarget)));
        add.addView(a.fieldBlock("CATEGORY", a.manualGlossaryCategory));
        add.addView(a.secondaryButton("+ Add term", v -> a.addManualTerm()), new LinearLayout.LayoutParams(-1, a.dp(44)));
        panel.addView(add, a.marginLP(-1, -2, 0, 12, 0, 12));

        a.glossaryPreview = a.text("", 12, a.MUTED, false);
        a.glossaryPreview.setTextIsSelectable(true);
        LinearLayout previewCard = a.card(14, a.FIELD, a.BORDER);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        previewCard.addView(a.text("Terms preview • " + a.editingGlossary.count() + " terms", 15, a.TEXT, true));
        previewCard.addView(a.glossaryPreview);
        panel.addView(previewCard);
        a.updateGlossaryPreview();
        return panel;
    }
}
