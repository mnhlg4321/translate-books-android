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
        profileTools.addView(a.secondaryButton("Language profile", v -> a.showLanguageProfilePicker()), new LinearLayout.LayoutParams(0, -2, 1));
        profileTools.addView(a.space(8, 1));
        profileTools.addView(a.secondaryButton("Prompt preview", v -> a.showPromptPreview()), new LinearLayout.LayoutParams(0, -2, 1));
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
        a.filesSourceCard = uploadCard;
        root.addView(a.sourceUi.build(), a.marginLP(-1, -2, 0, 8, 0, 0));
        root.addView(uploadCard, a.marginLP(-1, -2, 0, 8, 0, 14));
        if (a.sourceUi.isSetMode()) uploadCard.setVisibility(View.GONE);

        // Core readiness must remain visible on Translate; do not hide it behind Settings.
        a.translateSupportCard = translationSupportCard();
        a.translateOutputCard = outputCard();
        root.addView(a.translateSupportCard, a.marginLP(-1, -2, 0, 0, 0, 10));
        root.addView(a.translateOutputCard, a.marginLP(-1, -2, 0, 0, 0, 10));

        LinearLayout actionGrid = a.card(18, a.CARD, a.BORDER);
        actionGrid.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        actionGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout actionTop = a.rowContainer();
        LinearLayout actionBottom = a.rowContainer();
        a.startButton = a.primaryButton("▶ Start", v -> a.startTranslation());
        a.pauseButton = a.secondaryButton("⏸ Pause", v -> a.sendSvc(TranslatorService.ACTION_PAUSE));
        a.resumeButton = a.secondaryButton("↻ Resume", v -> a.sendSvc(TranslatorService.ACTION_RESUME));
        a.cancelButton = a.dangerButton("✕ Cancel", v -> a.sendSvc(TranslatorService.ACTION_CANCEL));
        actionTop.addView(a.startButton, new LinearLayout.LayoutParams(0, -2, 1));
        actionTop.addView(a.space(8, 1));
        actionTop.addView(a.pauseButton, new LinearLayout.LayoutParams(0, -2, 1));
        actionBottom.addView(a.resumeButton, new LinearLayout.LayoutParams(0, -2, 1));
        actionBottom.addView(a.space(8, 1));
        actionBottom.addView(a.cancelButton, new LinearLayout.LayoutParams(0, -2, 1));
        actionGrid.addView(actionTop);
        actionGrid.addView(actionBottom, a.marginLP(-1, -2, 0, 8, 0, 0));
        LinearLayout quickRow = a.rowContainer();
        quickRow.addView(a.secondaryButton("👁 Prompt preview", v -> a.showPromptPreview()), new LinearLayout.LayoutParams(0, -2, 1));
        quickRow.addView(a.space(8, 1));
        a.retryButton = a.secondaryButton("↻ Retry failed chunks", v -> a.retryFailedChunks());
        quickRow.addView(a.retryButton, new LinearLayout.LayoutParams(0, -2, 1));
        actionGrid.addView(quickRow, a.marginLP(-1, -2, 0, 8, 0, 0));
        a.pendingJobCard = a.card(18, a.CARD, a.AMBER);
        a.pendingJobCard.setOrientation(LinearLayout.VERTICAL);
        a.pendingJobCard.setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(12));
        a.pendingJobCard.setVisibility(View.GONE);
        root.addView(a.pendingJobCard, a.marginLP(-1, -2, 0, 0, 0, 8));
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
        buttons.addView(a.secondaryButton("Chọn file output", v -> a.chooseOutput()), new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(a.space(8, 1));
        a.outputFolderButton = a.secondaryButton("Chọn thư mục", v -> a.chooseOutputFolder());
        buttons.addView(a.outputFolderButton, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(buttons);
        card.addView(a.outputFileLabel, a.marginLP(-1, -2, 0, 8, 0, 0));
        card.addView(a.outputFolderLabel, a.marginLP(-1, -2, 0, 4, 0, 0));
        return card;
    }

    View translationSupportCard() {
        TranslationConfigState state = a.translationConfigState();
        LinearLayout section = a.sectionCard("◇", state.fromActiveJob
                ? "Cấu hình đang dùng • Job #" + state.jobId : "Cấu hình bản dịch");
        a.translateGlossaryRow = supportRow(state.glossary, a.BLUE,
                v -> a.showTranslationConfig("glossary"),
                v -> { if (a.ensureConfigMutable()) a.switchTab("Glossaries"); },
                v -> a.clearGlossarySelection());
        section.addView(a.translateGlossaryRow);
        a.translatePronounRow = supportRow(state.pronoun, a.CYAN,
                v -> a.showTranslationConfig("pronoun"),
                v -> a.switchTab("Pronouns"),
                v -> a.clearPronoun());
        section.addView(a.translatePronounRow, a.marginLP(-1, -2, 0, 8, 0, 0));
        a.translateComboNote = a.text("Glossary/Pronoun: theo từng chương của tổ hợp (xem danh sách chương ở trên).", 13, a.MUTED, false);
        a.translateComboNote.setSingleLine(false);
        section.addView(a.translateComboNote, a.marginLP(-1, -2, 0, 8, 0, 0));
        a.sourceUi.applyConfigRows();
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
        actions.addView(a.secondaryButton("Xem", viewAction), new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(a.space(6, 1));
        actions.addView(a.secondaryButton(item.isSelected() ? "Thay đổi" : "Chọn", changeAction), new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(a.space(6, 1));
        Button clear = a.dangerButton("Bỏ chọn", clearAction);
        clear.setEnabled(item.isSelected());
        clear.setAlpha(item.isSelected() ? 1f : 0.45f);
        actions.addView(clear, new LinearLayout.LayoutParams(0, -2, 1));
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
        boolean setMode = a.sourceUi.isSetMode();
        steps.addView(stepBox("1", setMode ? "Tổ hợp" : "TXT", a.inputUris.isEmpty() ? "chưa chọn" : (a.inputUris.size() == 1 ? "1 file" : a.inputUris.size() + " files"), a.inputUris.isEmpty() ? a.MUTED : a.GREEN), new LinearLayout.LayoutParams(0, -2, 1));
        steps.addView(a.space(6, 1));
        steps.addView(stepBox("2", "Output", a.outputTreeUri != null ? "folder" : (a.outputUri != null ? "file" : "chưa chọn"), (a.outputTreeUri != null || a.outputUri != null) ? a.GREEN : a.MUTED), new LinearLayout.LayoutParams(0, -2, 1));
        steps.addView(a.space(6, 1));
        GlossaryStore.Glossary g = GlossaryStore.selected(a);
        steps.addView(stepBox("3", "Locks", setMode ? a.sourceUi.locksSummary() : g == null ? "optional" : g.count() + " terms", setMode ? a.GREEN : g == null ? a.MUTED : a.GREEN), new LinearLayout.LayoutParams(0, -2, 1));
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


    /**
     * The progress panel: two bars (the whole run, the chapter) and six cells of one fixed height whose text shrinks to fit, so the panel never
     * changes size while numbers change. The chapter-set bar and cell label appear only for a chapter-set run.
     */
    View buildTrackingCard() {
        a.trackingCard = a.card(18, a.CARD, a.BORDER);
        a.trackingCard.setOrientation(LinearLayout.VERTICAL);
        a.trackingCard.setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(12));
        a.trackingCard.setVisibility(View.VISIBLE);
        a.trackingTitle = a.text("Running", 17, a.TEXT, true);
        a.trackingTitle.setSingleLine(true);
        a.trackingTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        a.trackingCard.addView(a.trackingTitle);
        a.trackingFile = a.text("\u2014", 12, a.MUTED, false);
        a.trackingFile.setSingleLine(true);
        a.trackingFile.setEllipsize(android.text.TextUtils.TruncateAt.END);
        a.trackingCard.addView(a.trackingFile, a.marginLP(-1, -2, 0, 2, 0, 8));

        a.setBarRow = barRow("T\u1ed5 h\u1ee3p", a.setBar = bar(), a.setLine = a.text("", 12, a.TEXT, true));
        a.trackingCard.addView(a.setBarRow, a.marginLP(-1, -2, 0, 0, 0, 6));
        LinearLayout chapterRow = barRow("Ch\u01b0\u01a1ng", a.chapterBar = bar(), a.metricCompleted = a.text("", 12, a.TEXT, true));
        a.chapterRowLabel = (TextView) ((LinearLayout) chapterRow.getChildAt(0)).getChildAt(0);
        a.trackingCard.addView(chapterRow, a.marginLP(-1, -2, 0, 0, 0, 10));

        a.metricTokens = cellValue();
        a.metricCost = cellValue();
        a.metricElapsed = cellValue();
        a.metricRemaining = cellValue();
        a.metricFailed = cellValue();
        a.metricGlossaryLocks = cellValue();
        a.tokensCellLabel = cellLabel("T\u1ed5 h\u1ee3p");
        LinearLayout row1 = a.rowContainer();
        row1.addView(cell(a.metricTokens, a.tokensCellLabel), new LinearLayout.LayoutParams(0, a.dp(CELL_DP), 1));
        row1.addView(a.space(6, 1));
        row1.addView(cell(a.metricCost, cellLabel("Chi ph\u00ed")), new LinearLayout.LayoutParams(0, a.dp(CELL_DP), 1));
        row1.addView(a.space(6, 1));
        row1.addView(cell(a.metricElapsed, cellLabel("\u0110\u00e3 ch\u1ea1y")), new LinearLayout.LayoutParams(0, a.dp(CELL_DP), 1));
        a.trackingCard.addView(row1);
        LinearLayout row2 = a.rowContainer();
        row2.addView(cell(a.metricRemaining, cellLabel("C\u00f2n l\u1ea1i")), new LinearLayout.LayoutParams(0, a.dp(CELL_DP), 1));
        row2.addView(a.space(6, 1));
        row2.addView(cell(a.metricFailed, cellLabel("L\u1ed7i \u00b7 fb")), new LinearLayout.LayoutParams(0, a.dp(CELL_DP), 1));
        row2.addView(a.space(6, 1));
        row2.addView(cell(a.metricGlossaryLocks, cellLabel("Lu\u1eadt")), new LinearLayout.LayoutParams(0, a.dp(CELL_DP), 1));
        a.trackingCard.addView(row2, a.marginLP(-1, -2, 0, 6, 0, 0));

        // Fields other code still writes to; not shown.
        a.metricChunks = a.text("0", 1, a.BLUE, true);
        a.metricFallbacks = a.text("0", 1, a.BLUE, true);
        a.metricCurrentChunk = a.text("\u2014", 1, a.CYAN, true);
        a.metricPronounLocks = a.text("\u2014", 1, a.CYAN, true);
        a.lockUsageMeta = a.text("", 1, a.MUTED, false);

        a.estimateLine = a.text("", 11, a.MUTED, false);
        a.estimateLine.setSingleLine(true);
        a.estimateLine.setEllipsize(android.text.TextUtils.TruncateAt.END);
        a.trackingCard.addView(a.estimateLine, a.marginLP(-1, -2, 0, 8, 0, 4));

        LinearLayout previewCard = a.card(14, a.FIELD, a.BORDER);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(a.dp(12), a.dp(8), a.dp(8), a.dp(8));
        a.previewMeta = a.text("Waiting for the first accepted chunk.", 11, a.MUTED, false);
        a.previewText = a.text("The latest accepted translation will appear here.", 13, a.TEXT, false);
        a.previewText.setSingleLine(false);
        a.previewText.setTextIsSelectable(true);
        a.previewText.setPadding(0, 0, a.dp(8), 0);
        ScrollView previewScroll = new ScrollView(a);
        previewScroll.setFillViewport(false);
        previewScroll.setVerticalScrollBarEnabled(true);
        previewScroll.setScrollbarFadingEnabled(false);
        previewScroll.setNestedScrollingEnabled(true);
        previewScroll.addView(a.previewText, new ScrollView.LayoutParams(-1, -2));
        LinearLayout previewBody = new LinearLayout(a);
        previewBody.setOrientation(LinearLayout.VERTICAL);
        previewBody.addView(a.previewMeta, a.marginLP(-1, -2, 0, 4, 0, 6));
        previewBody.addView(previewScroll, new LinearLayout.LayoutParams(-1, a.dp(150)));
        previewBody.setVisibility(View.GONE);
        TextView previewToggle = a.text("\u25b8 B\u1ea3n d\u1ecbch g\u1ea7n nh\u1ea5t (thu g\u1ecdn)", 12, a.CYAN, true);
        previewToggle.setPadding(0, a.dp(8), 0, a.dp(8));
        previewToggle.setMinHeight(a.dp(44));
        previewToggle.setGravity(Gravity.CENTER_VERTICAL);
        previewToggle.setOnClickListener(v -> {
            boolean open = previewBody.getVisibility() != View.VISIBLE;
            previewBody.setVisibility(open ? View.VISIBLE : View.GONE);
            previewToggle.setText(open ? "\u25be B\u1ea3n d\u1ecbch g\u1ea7n nh\u1ea5t" : "\u25b8 B\u1ea3n d\u1ecbch g\u1ea7n nh\u1ea5t (thu g\u1ecdn)");
        });
        previewCard.addView(previewToggle);
        previewCard.addView(previewBody);
        a.trackingCard.addView(previewCard);
        return a.trackingCard;
    }

    /** Height of every cell: fixed, so a longer number shrinks its text instead of growing the panel. */
    static final int CELL_DP = 56;

    private ProgressBar bar() {
        ProgressBar p = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(1000);
        return p;
    }

    /** [label] [bar] and a text line under the bar. */
    private LinearLayout barRow(String label, ProgressBar bar, TextView line) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout top = a.rowContainer();
        TextView l = a.text(label, 11, a.MUTED, true);
        l.setMinWidth(Math.round(a.dp(52) * Math.max(1f, a.fontScale)));
        top.addView(l, new LinearLayout.LayoutParams(-2, -2));
        top.addView(bar, new LinearLayout.LayoutParams(0, a.dp(10), 1));
        row.addView(top);
        row.addView(line, a.marginLP(-1, -2, Math.round(52 * Math.max(1f, a.fontScale)), 2, 0, 0));
        line.setSingleLine(true);
        line.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return row;
    }

    private TextView cellValue() {
        TextView v = a.text("\u2014", 16, a.TEXT, true);
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setMaxLines(1);
        android.widget.TextView t = v;
        t.setAutoSizeTextTypeUniformWithConfiguration(10, 16, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        return v;
    }

    private TextView cellLabel(String text) {
        TextView l = a.text(text, 10, a.MUTED, true);
        l.setGravity(Gravity.CENTER);
        l.setSingleLine(true);
        return l;
    }

    private LinearLayout cell(TextView value, TextView label) {
        LinearLayout box = a.card(10, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(a.dp(4), a.dp(4), a.dp(4), a.dp(4));
        box.addView(value, new LinearLayout.LayoutParams(-1, 0, 1));
        box.addView(label, new LinearLayout.LayoutParams(-1, -2));
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
        actions.addView(clear, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(a.space(8, 1));
        actions.addView(a.secondaryButton("Export Log", v -> a.exportLog()), new LinearLayout.LayoutParams(0, -2, 1));
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
        a.logScroll.setNestedScrollingEnabled(true);
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
