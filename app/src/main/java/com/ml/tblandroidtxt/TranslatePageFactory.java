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


class TranslatePageFactory {
    private final MainActivity a;

    TranslatePageFactory(MainActivity activity) {
        this.a = activity;
    }


    View buildTranslatePage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);

        // Keep the primary workflow above the fold; detailed configuration remains in Settings.

        LinearLayout lang = a.rowContainer();
        lang.addView(a.fieldBlock("SOURCE LANGUAGE", a.sourceField = a.input("Japanese", "Japanese")), new LinearLayout.LayoutParams(0, -2, 1));
        lang.addView(a.space(12, 1));
        lang.addView(a.fieldBlock("TARGET LANGUAGE", a.targetField = a.input("Vietnamese", "Vietnamese")), new LinearLayout.LayoutParams(0, -2, 1));
        a.bindEstimateInput(a.sourceField);
        a.bindEstimateInput(a.targetField);
        root.addView(lang);
        lang.setVisibility(View.GONE);
        LinearLayout profileTools = a.rowContainer();
        profileTools.addView(a.secondaryButton("Language profile", v -> a.showLanguageProfilePicker()), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        profileTools.addView(a.space(8, 1));
        profileTools.addView(a.secondaryButton("Prompt preview", v -> a.showPromptPreview()), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        root.addView(profileTools, a.marginLP(-1, -2, 0, 0, 0, 12));
        profileTools.setVisibility(View.GONE);

        LinearLayout uploadCard = a.card(22, a.CARD, a.BLUE);
        uploadCard.setOrientation(LinearLayout.VERTICAL);
        uploadCard.setGravity(Gravity.CENTER);
        uploadCard.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        TextView cloud = a.text("📄", 42, a.BLUE, false);
        cloud.setGravity(Gravity.CENTER);
        TextView h = a.text("Input TXT", 21, a.TEXT, true);
        h.setGravity(Gravity.CENTER);
        TextView sub = a.text("Chọn 1 hoặc nhiều file .txt qua Android file picker", 13, a.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        cloud.setVisibility(View.GONE);
        h.setVisibility(View.GONE);
        sub.setVisibility(View.GONE);
        Button browse = a.primaryButton("Browse TXT(s)", v -> a.chooseInput());
        a.inputFileLabel = a.text(a.currentInputLabel(), 13, a.MUTED, false);
        a.inputFileLabel.setGravity(Gravity.CENTER);
        uploadCard.addView(cloud);
        uploadCard.addView(h);
        uploadCard.addView(sub);
        uploadCard.addView(a.space(1, 4));
        uploadCard.addView(browse, new LinearLayout.LayoutParams(a.dp(180), a.dp(44)));
        uploadCard.addView(a.inputFileLabel);
        a.estimatePanel = a.card(12, a.FIELD, a.BORDER);
        a.estimatePanel.setOrientation(LinearLayout.VERTICAL);
        a.estimatePanel.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        a.estimateText = a.text("0 chunks\nEstimated tokens unavailable\nEstimated cost unavailable", 14, a.TEXT, true);
        a.estimateText.setGravity(Gravity.CENTER);
        a.estimatePanel.addView(a.estimateText);
        a.pricingRetryButton = a.secondaryButton("Refresh model pricing", v -> a.refreshModelCatalog());
        a.estimatePanel.addView(a.pricingRetryButton, a.marginLP(-1, a.dp(42), 0, 8, 0, 0));
        a.pricingRetryButton.setVisibility(View.GONE);
        a.estimatePanel.setVisibility(View.VISIBLE);
        uploadCard.addView(a.estimatePanel, a.marginLP(-1, -2, 0, 10, 0, 0));
        root.addView(uploadCard, a.marginLP(-1, -2, 0, 8, 0, 14));

        // Core readiness must remain visible on Translate; do not hide it behind Settings.
        root.addView(translationSupportCard(), a.marginLP(-1, -2, 0, 0, 0, 10));
        root.addView(outputCard(), a.marginLP(-1, -2, 0, 0, 0, 10));

        LinearLayout actionGrid = a.card(18, a.CARD, a.BORDER);
        actionGrid.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        actionGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout actionTop = a.rowContainer();
        LinearLayout actionBottom = a.rowContainer();
        a.startButton = a.primaryButton("▶ Start", v -> a.startTranslation());
        a.pauseButton = a.secondaryButton("⏸ Pause", v -> a.sendSvc(TranslatorService.ACTION_PAUSE));
        a.resumeButton = a.secondaryButton("↻ Resume", v -> a.sendSvc(TranslatorService.ACTION_RESUME));
        a.cancelButton = a.dangerButton("✕ Cancel", v -> a.sendSvc(TranslatorService.ACTION_CANCEL));
        actionTop.addView(a.startButton, new LinearLayout.LayoutParams(0, a.dp(48), 1));
        actionTop.addView(a.space(8, 1));
        actionTop.addView(a.pauseButton, new LinearLayout.LayoutParams(0, a.dp(48), 1));
        actionBottom.addView(a.resumeButton, new LinearLayout.LayoutParams(0, a.dp(48), 1));
        actionBottom.addView(a.space(8, 1));
        actionBottom.addView(a.cancelButton, new LinearLayout.LayoutParams(0, a.dp(48), 1));
        actionGrid.addView(actionTop);
        actionGrid.addView(actionBottom, a.marginLP(-1, -2, 0, 8, 0, 0));
        LinearLayout quickRow = a.rowContainer();
        quickRow.addView(a.secondaryButton("👁 Prompt preview", v -> a.showPromptPreview()), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        quickRow.addView(a.space(8, 1));
        a.retryButton = a.secondaryButton("↻ Retry failed chunks", v -> a.retryFailedChunks());
        quickRow.addView(a.retryButton, new LinearLayout.LayoutParams(0, a.dp(44), 1));
        actionGrid.addView(quickRow, a.marginLP(-1, -2, 0, 8, 0, 0));
        root.addView(actionGrid, a.marginLP(-1, -2, 0, 0, 0, 8));

        a.translateMeta = a.text("OpenRouter · Model chưa nạp · Japanese → Vietnamese", 12, a.MUTED, false);
        a.translateMeta.setGravity(Gravity.CENTER);
        root.addView(a.translateMeta, a.marginLP(-1, -2, 0, 2, 0, 2));

        a.statusBanner = a.text("", 15, a.TEXT, true);
        a.statusBanner.setVisibility(View.GONE);
        a.statusBanner.setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(12));
        root.addView(a.statusBanner, a.marginLP(-1, -2, 0, 16, 0, 12));

        a.progress = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        a.progress.setMax(100);
        root.addView(a.progress, a.marginLP(-1, a.dp(12), 0, 8, 0, 8));

        root.addView(buildTrackingCard(), a.marginLP(-1, -2, 0, 8, 0, 16));

        a.resultCard = a.card(16, a.CARD, a.GREEN);
        a.resultCard.setOrientation(LinearLayout.HORIZONTAL);
        a.resultCard.setGravity(Gravity.CENTER_VERTICAL);
        a.resultCard.setPadding(a.dp(16), a.dp(14), a.dp(16), a.dp(14));
        TextView doc = a.text("📄", 30, a.TEXT, false);
        a.resultCard.addView(doc, new LinearLayout.LayoutParams(a.dp(48), a.dp(48)));
        LinearLayout rt = new LinearLayout(a);
        rt.setOrientation(LinearLayout.VERTICAL);
        a.resultTitle = a.text("Translation result", 16, a.TEXT, true);
        a.resultMeta = a.text("Chưa có output", 12, a.MUTED, false);
        rt.addView(a.resultTitle); rt.addView(a.resultMeta);
        a.resultCard.addView(rt, new LinearLayout.LayoutParams(0, -2, 1));
        a.resultCard.addView(a.secondaryButton("Open output", v -> a.toast("Output đã được lưu vào file anh chọn")), new LinearLayout.LayoutParams(a.dp(140), a.dp(42)));
        a.resultCard.setVisibility(View.GONE);
        root.addView(a.resultCard, a.marginLP(-1, -2, 0, 8, 0, 16));

        root.addView(activityLogCard(), a.marginLP(-1, -2, 0, 0, 0, 16));

        a.updateMetaLine();
        a.updateInputEstimate();
        a.updateActionButtons();
        a.restoreRuntimeStateToUi();
        return scroll;
    }

    View outputCard() {
        LinearLayout card = a.sectionCard("⇩", "Nơi lưu bản dịch");
        a.outputFileLabel = a.text(a.outputUri == null ? "File riêng: chưa chọn" : "File riêng: " + FileUtil.displayName(a, a.outputUri), 12, a.MUTED, false);
        a.outputFolderLabel = a.text(a.outputTreeUri == null ? "Thư mục: chưa chọn" : "Thư mục: " + FileUtil.treeName(a.outputTreeUri), 12, a.MUTED, false);
        LinearLayout buttons = a.rowContainer();
        buttons.addView(a.secondaryButton("Chọn file output", v -> a.chooseOutput()), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        buttons.addView(a.space(8, 1));
        a.outputFolderButton = a.secondaryButton("Chọn thư mục", v -> a.chooseOutputFolder());
        buttons.addView(a.outputFolderButton, new LinearLayout.LayoutParams(0, a.dp(44), 1));
        card.addView(buttons);
        card.addView(a.outputFileLabel, a.marginLP(-1, -2, 0, 8, 0, 0));
        card.addView(a.outputFolderLabel, a.marginLP(-1, -2, 0, 4, 0, 0));
        return card;
    }

    View translationSupportCard() {
        TranslationConfigState state = a.translationConfigState();
        LinearLayout section = a.sectionCard("◇", state.fromActiveJob
                ? "Cấu hình đang dùng • Job #" + state.jobId : "Cấu hình bản dịch");
        section.addView(supportRow(state.glossary, a.BLUE,
                v -> a.showTranslationConfig("glossary"),
                v -> { if (a.ensureConfigMutable()) a.switchTab("Glossaries"); },
                v -> a.clearGlossarySelection()));
        section.addView(supportRow(state.pronoun, a.CYAN,
                v -> a.showTranslationConfig("pronoun"),
                v -> a.switchTab("Pronouns"),
                v -> a.clearPronoun()), a.marginLP(-1, -2, 0, 8, 0, 0));
        section.addView(supportRow(state.instruction, a.AMBER,
                v -> a.showTranslationConfig("instruction"),
                v -> a.chooseYaml(),
                v -> a.clearInstruction()), a.marginLP(-1, -2, 0, 8, 0, 0));
        section.addView(a.text("Instruction YAML là tùy chọn. Không chọn sẽ hiển thị “Không sử dụng”. Cấu hình không thể đổi khi job đang chạy.", 12, a.MUTED, false),
                a.marginLP(-1, -2, 0, 8, 0, 0));
        return section;
    }

    View supportRow(TranslationConfigState.Item item, int accent,
                    View.OnClickListener viewAction, View.OnClickListener changeAction,
                    View.OnClickListener clearAction) {
        LinearLayout card = a.card(14, a.FIELD, item.status == TranslationConfigState.Status.INVALID ? a.RED : accent);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        card.addView(a.text(item.title, 15, a.TEXT, true));
        TextView current = a.text(item.displayText(), 12,
                item.status == TranslationConfigState.Status.INVALID ? a.RED
                        : item.status == TranslationConfigState.Status.WARNING ? a.AMBER : accent, true);
        current.setSingleLine(false);
        if ("Glossaries".equals(item.title)) a.translateGlossaryChip = current;
        else if ("Pronoun".equals(item.title)) a.translatePronounChip = current;
        else a.translateInstructionChip = current;
        card.addView(current, a.marginLP(-1, -2, 0, 4, 0, 7));
        LinearLayout actions = a.rowContainer();
        actions.addView(a.secondaryButton("Xem", viewAction), new LinearLayout.LayoutParams(0, a.dp(40), 1));
        actions.addView(a.space(6, 1));
        actions.addView(a.secondaryButton(item.isSelected() ? "Thay đổi" : "Chọn", changeAction), new LinearLayout.LayoutParams(0, a.dp(40), 1));
        actions.addView(a.space(6, 1));
        Button clear = a.dangerButton("Bỏ chọn", clearAction);
        clear.setEnabled(item.isSelected());
        clear.setAlpha(item.isSelected() ? 1f : 0.45f);
        actions.addView(clear, new LinearLayout.LayoutParams(0, a.dp(40), 1));
        card.addView(actions);
        return card;
    }

    View quickOptionsCard() {
        LinearLayout card = a.sectionCard("✓", "Tùy chọn bản dịch");
        AppSettings settings = SettingsStore.load(a);
        a.refineBox = a.check("Làm mượt bản dịch", settings.refineAfter, "Chạy thêm bước biên tập sau khi dịch thô.");
        a.bilingualBox = a.check("Xuất song ngữ", settings.bilingualOutput, "Giữ cả nội dung gốc và bản dịch để đối chiếu.");
        card.addView(a.refineBox);
        card.addView(a.bilingualBox);
        return card;
    }



    View workflowDashboard() {
        LinearLayout card = a.card(18, a.CARD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(14), a.dp(14), a.dp(14), a.dp(14));

        LinearLayout head = a.rowContainer();
        TextView title = a.text("Workflow", 17, a.TEXT, true);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = a.text((TranslatorService.isActive() || a.translationActive) ? "RUNNING" : "READY", 11, (TranslatorService.isActive() || a.translationActive) ? a.GREEN : a.MUTED, true);
        status.setGravity(Gravity.CENTER);
        status.setPadding(a.dp(10), a.dp(5), a.dp(10), a.dp(5));
        a.tint(status, a.FIELD, (TranslatorService.isActive() || a.translationActive) ? a.GREEN : a.BORDER, 1, 14);
        head.addView(status, new LinearLayout.LayoutParams(-2, a.dp(32)));
        card.addView(head);

        LinearLayout steps = a.rowContainer();
        steps.addView(stepBox("1", "TXT", a.inputUris.isEmpty() ? "chưa chọn" : (a.inputUris.size() == 1 ? "1 file" : a.inputUris.size() + " files"), a.inputUris.isEmpty() ? a.MUTED : a.GREEN), new LinearLayout.LayoutParams(0, -2, 1));
        steps.addView(a.space(6, 1));
        steps.addView(stepBox("2", "Output", a.outputTreeUri != null ? "folder" : (a.outputUri != null ? "file" : "chưa chọn"), (a.outputTreeUri != null || a.outputUri != null) ? a.GREEN : a.MUTED), new LinearLayout.LayoutParams(0, -2, 1));
        steps.addView(a.space(6, 1));
        GlossaryStore.Glossary g = GlossaryStore.selected(a);
        steps.addView(stepBox("3", "Locks", g == null ? "optional" : g.count() + " terms", g == null ? a.MUTED : a.GREEN), new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(steps, a.marginLP(-1, -2, 0, 10, 0, 10));

        TextView hint = a.text("Gợi ý: chọn TXT + output trước, kiểm Prompt preview nếu có glossary/pronoun, rồi Start. UI 2.9 giữ navigation cũ để tránh mất state dịch.", 12, a.MUTED, false);
        hint.setSingleLine(false);
        card.addView(hint);
        return card;
    }

    LinearLayout stepBox(String no, String label, String value, int accent) {
        LinearLayout box = a.card(12, a.FIELD, accent == a.MUTED ? a.BORDER : accent);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(8), a.dp(9), a.dp(8), a.dp(9));
        TextView n = a.text(no, 13, accent, true);
        n.setGravity(Gravity.CENTER);
        TextView l = a.text(label, 11, a.TEXT, true);
        l.setGravity(Gravity.CENTER);
        TextView v = a.text(value, 10, a.MUTED, false);
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(false);
        box.addView(n);
        box.addView(l);
        box.addView(v);
        return box;
    }


    View buildTrackingCard() {
        a.trackingCard = a.card(18, a.CARD, a.BORDER);
        a.trackingCard.setOrientation(LinearLayout.VERTICAL);
        a.trackingCard.setPadding(a.dp(16), a.dp(16), a.dp(16), a.dp(16));
        a.trackingCard.setVisibility(View.VISIBLE);
        a.trackingTitle = a.text("Running", 18, a.TEXT, true);
        a.trackingFile = a.text("—", 13, a.MUTED, false);
        a.trackingCard.addView(a.trackingTitle);
        a.trackingCard.addView(a.trackingFile, a.marginLP(-1, -2, 0, 2, 0, 12));

        LinearLayout row1 = a.rowContainer();
        a.metricChunks = a.text("0", 16, a.BLUE, true);
        a.metricCompleted = a.text("0/0 chunks", 15, a.BLUE, true);
        a.metricElapsed = a.text("0s", 15, a.BLUE, true);
        a.metricRemaining = a.text("Calculating…", 14, a.BLUE, true);
        row1.addView(statBox("PROGRESS", a.metricCompleted), new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(a.space(6, 1));
        row1.addView(statBox("ELAPSED", a.metricElapsed), new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(a.space(6, 1));
        row1.addView(statBox("EST. REMAINING", a.metricRemaining), new LinearLayout.LayoutParams(0, -2, 1));
        a.trackingCard.addView(row1);

        LinearLayout health = a.rowContainer();
        a.metricFailed = a.text("0 failed", 16, a.BLUE, true);
        a.metricFallbacks = a.text("0 fallbacks", 16, a.BLUE, true);
        health.addView(statBox("FAILED", a.metricFailed), new LinearLayout.LayoutParams(0, -2, 1));
        health.addView(a.space(6, 1));
        health.addView(statBox("FALLBACK", a.metricFallbacks), new LinearLayout.LayoutParams(0, -2, 1));
        a.trackingCard.addView(health, a.marginLP(-1, -2, 0, 6, 0, 0));

        LinearLayout money = a.rowContainer();
        a.metricCost = a.text("$0.000 spent", 17, Color.rgb(172, 96, 0), true);
        a.metricTokens = a.text("0 tokens used", 17, a.BLUE, true);
        money.addView(wideStat("TOKENS", a.metricTokens, Color.rgb(206, 225, 255)), new LinearLayout.LayoutParams(0, -2, 1));
        money.addView(a.space(8, 1));
        money.addView(wideStat("COST", a.metricCost, Color.rgb(255, 238, 176)), new LinearLayout.LayoutParams(0, -2, 1));
        a.trackingCard.addView(money, a.marginLP(-1, -2, 0, 6, 0, 0));

        LinearLayout rules = a.rowContainer();
        a.metricCurrentChunk = a.text("—", 15, a.CYAN, true);
        a.metricGlossaryLocks = a.text("—", 14, a.CYAN, true);
        a.metricPronounLocks = a.text("—", 14, a.CYAN, true);
        rules.addView(statBox("CURRENT CHUNK", a.metricCurrentChunk), new LinearLayout.LayoutParams(0, -2, 1));
        rules.addView(a.space(6, 1));
        rules.addView(statBox("GLOSSARY", a.metricGlossaryLocks), new LinearLayout.LayoutParams(0, -2, 1));
        rules.addView(a.space(6, 1));
        rules.addView(statBox("PRONOUN", a.metricPronounLocks), new LinearLayout.LayoutParams(0, -2, 1));
        a.trackingCard.addView(rules, a.marginLP(-1, -2, 0, 8, 0, 0));
        a.lockUsageMeta = a.text("Rule usage appears when a chunk request is prepared.", 11, a.MUTED, false);
        a.trackingCard.addView(a.lockUsageMeta, a.marginLP(-1, -2, 0, 0, 0, 12));

        LinearLayout previewCard = a.card(14, a.FIELD, a.BORDER);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(a.dp(12), a.dp(12), a.dp(8), a.dp(10));
        previewCard.addView(a.text("LAST TRANSLATION PREVIEW", 11, a.CYAN, true));
        a.previewMeta = a.text("Waiting for the first accepted chunk.", 11, a.MUTED, false);
        previewCard.addView(a.previewMeta, a.marginLP(-1, -2, 0, 2, 0, 8));
        a.previewText = a.text("The latest accepted translation will appear here.", 13, a.TEXT, false);
        a.previewText.setSingleLine(false);
        a.previewText.setTextIsSelectable(true);
        a.previewText.setPadding(0, 0, a.dp(8), 0);
        ScrollView previewScroll = new ScrollView(a);
        previewScroll.setFillViewport(false);
        previewScroll.setVerticalScrollBarEnabled(true);
        previewScroll.setScrollbarFadingEnabled(false);
        previewScroll.setNestedScrollingEnabled(false);
        previewScroll.addView(a.previewText, new ScrollView.LayoutParams(-1, -2));
        previewCard.addView(previewScroll, new LinearLayout.LayoutParams(-1, a.dp(150)));
        a.trackingCard.addView(previewCard);
        return a.trackingCard;
    }


    LinearLayout statBox(String label, TextView value) {
        LinearLayout box = a.card(10, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(a.dp(4), a.dp(8), a.dp(4), a.dp(8));
        box.addView(value);
        TextView l = a.text(label, 9, a.MUTED, true);
        l.setGravity(Gravity.CENTER);
        box.addView(l);
        return box;
    }


    LinearLayout wideStat(String label, TextView value, int fill) {
        LinearLayout box = a.card(10, fill, fill);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(a.dp(8), a.dp(8), a.dp(8), a.dp(8));
        box.addView(value);
        TextView l = a.text(label, 9, Color.rgb(50, 50, 50), true);
        l.setGravity(Gravity.CENTER);
        box.addView(l);
        return box;
    }


    View activityLogCard() {
        LinearLayout logCard = a.sectionCard("▣", "Activity Log");
        LinearLayout actions = a.rowContainer();
        Button clear = a.secondaryButton("Clear Log", v -> {
            LogStore.clear(a);
            if (a.logView != null) a.logView.setText("");
            a.toast("Đã xóa log");
        });
        actions.addView(clear, new LinearLayout.LayoutParams(0, a.dp(42), 1));
        actions.addView(a.space(8, 1));
        actions.addView(a.secondaryButton("Export Log", v -> a.exportLog()), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        logCard.addView(actions, a.marginLP(-1, -2, 0, 0, 0, 8));

        a.logView = a.text(a.tail(LogStore.read(a), 12000), 12, a.TEXT, false);
        a.logView.setTypeface(Typeface.MONOSPACE);
        a.logView.setTextIsSelectable(false);
        a.logView.setSingleLine(false);
        a.logView.setPadding(0, 0, a.dp(8), 0);
        a.logScroll = new ScrollView(a);
        a.logScroll.setFillViewport(false);
        a.logScroll.setVerticalScrollBarEnabled(true);
        a.logScroll.setScrollbarFadingEnabled(false);
        a.logScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        a.logScroll.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        a.logScroll.setNestedScrollingEnabled(false);
        a.logScroll.addView(a.logView, new ScrollView.LayoutParams(-1, -2));

        LinearLayout inner = a.card(16, a.FIELD, a.BORDER);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(a.dp(12), a.dp(12), a.dp(6), a.dp(12));
        TextView hint = a.text("Kéo trong khung này để xem log cũ/mới. Thanh cuộn luôn hiện ở mép phải.", 11, a.MUTED, false);
        inner.addView(hint, a.marginLP(-1, -2, 0, 0, 0, 6));
        inner.addView(a.logScroll, new LinearLayout.LayoutParams(-1, a.dp(210)));
        logCard.addView(inner, new LinearLayout.LayoutParams(-1, -2));
        a.logScroll.post(() -> a.logScroll.fullScroll(View.FOCUS_DOWN));
        return logCard;
    }
}
