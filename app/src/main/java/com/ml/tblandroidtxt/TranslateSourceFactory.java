package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translate screen, step 1: the source is either TXT files (as before) or a saved chapter set. With a set the owner ticks chapters;
 * each ticked chapter is translated with the Glossary and Pronoun saved in the set, never with the Active ones.
 * The ticked chapters become the run's inputs ({@code tblraw://} addresses), so planning, estimate and Start use the usual path.
 */
final class TranslateSourceFactory {
    static final String MODE_FILES = "files", MODE_SET = "set";
    private static final String PREF = "translate_source";

    private final MainActivity a;
    private boolean loaded;
    private String mode = MODE_FILES;
    private String setId = "";
    private String tickInitFor = "";
    private final LinkedHashSet<String> ticked = new LinkedHashSet<>();
    private final List<Row> rows = new ArrayList<>();
    private final ArrayList<Uri> filesUris = new ArrayList<>();
    private final Map<String, TextView> estimateViews = new HashMap<>();
    private String setName = "";

    private LinearLayout box, chapterBox;
    private Button filesButton, setButton;

    /** One chapter of the open set with what it points at right now. */
    private static final class Row {
        ChapterSetStore.Row stored;
        LibraryRawStore.Item raw;
        GlossaryStore.Glossary glossary;
        PronounStore.Profile pronoun;
        boolean translated;
        boolean usable() { return raw != null; }
    }

    TranslateSourceFactory(MainActivity activity) { a = activity; }

    boolean isSetMode() { load(); return MODE_SET.equals(mode); }

    private void load() {
        if (loaded) return;
        loaded = true;
        SharedPreferences p = a.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE);
        mode = p.getString("mode", MODE_FILES);
        setId = p.getString("set_id", "");
        tickInitFor = p.getString("tick_init_for", "");
        for (String id : p.getString("ticked", "").split(",")) if (!id.isEmpty()) ticked.add(id);
    }

    private void save() {
        a.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE).edit().putString("mode", mode).putString("set_id", setId)
                .putString("tick_init_for", tickInitFor).putString("ticked", String.join(",", ticked)).apply();
    }

    /** The card shown above the TXT picker. */
    View build() {
        load();
        box = a.card(18, a.CARD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        box.addView(a.text("Nguồn", 15, a.TEXT, true));
        LinearLayout modeRow = a.rowContainer();
        filesButton = a.secondaryButton("Tệp TXT", v -> setMode(MODE_FILES));
        setButton = a.secondaryButton("Bộ chương", v -> setMode(MODE_SET));
        filesButton.setContentDescription("source-files");
        setButton.setContentDescription("source-set");
        modeRow.addView(filesButton, new LinearLayout.LayoutParams(0, a.dp(44), 1));
        modeRow.addView(a.space(8, 1));
        modeRow.addView(setButton, new LinearLayout.LayoutParams(0, a.dp(44), 1));
        box.addView(modeRow, a.marginLP(-1, -2, 0, 6, 0, 0));
        chapterBox = new LinearLayout(a);
        chapterBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(chapterBox, a.marginLP(-1, -2, 0, 8, 0, 0));
        if (isSetMode()) refresh(); else render();
        return box;
    }

    void setMode(String next) {
        load();
        if (next.equals(mode)) return;
        if (MODE_SET.equals(next)) {
            filesUris.clear();
            for (Uri u : a.inputUris) if (!RawUri.isRaw(u.toString())) filesUris.add(u);
            mode = MODE_SET;
            save();
            refresh();
        } else {
            mode = MODE_FILES;
            a.chapterBindings = null;
            a.inputUris.clear();
            a.inputUris.addAll(filesUris);
            a.inputUri = a.inputUris.isEmpty() ? null : a.inputUris.get(0);
            UiSessionStore.saveInputUris(a, a.inputUris);
            save();
            render();
            a.setLabel(a.inputFileLabel, a.currentInputLabel());
            a.updateInputEstimate();
        }
    }

    /** Reloads the open set from the database (the Library may have changed) and re-applies the ticked chapters. */
    void refresh() {
        load();
        rows.clear();
        setName = "";
        ChapterSetStore.SetData data = setId.isEmpty() ? null : ChapterSetStore.get(a, setId);
        if (data != null) {
            setName = data.name;
            Set<String> completed = LibraryRawStore.completedHashes(a);
            for (ChapterSetStore.Row r : data.rows) {
                Row x = new Row();
                x.stored = r;
                x.raw = LibraryRawStore.find(a, r.rawId);
                x.glossary = r.glossaryId.isEmpty() ? null : GlossaryStore.find(a, r.glossaryId);
                x.pronoun = r.pronounId.isEmpty() ? null : PronounStore.find(a, r.pronounId);
                x.translated = x.raw != null && completed.contains(x.raw.inputHash);
                rows.add(x);
            }
        } else {
            setId = "";
        }
        if (!tickInitFor.equals(setId)) {
            ticked.clear();
            for (Row x : rows) if (x.usable() && !x.translated) ticked.add(x.raw.id);
            tickInitFor = setId;
        } else {
            Set<String> usable = new HashSet<>();
            for (Row x : rows) if (x.usable()) usable.add(x.raw.id);
            ticked.retainAll(usable);
        }
        save();
        applySelection();
        render();
    }

    /** Bindings for the ticked chapters, in set order, read from the stores now. */
    List<ChapterBinding> freshBindings() {
        ArrayList<ChapterBinding> out = new ArrayList<>();
        for (Row x : rows) {
            if (!x.usable() || !ticked.contains(x.raw.id)) continue;
            out.add(ChapterBinding.of(x.raw.id, x.raw.originalName, x.stored.chapterNo, x.glossary, x.pronoun));
        }
        return out;
    }

    String currentSetId() { return setId; }

    String currentSetName() { return setName; }

    private void applySelection() {
        List<ChapterBinding> bindings = freshBindings();
        a.chapterBindings = bindings;
        a.inputUris.clear();
        for (ChapterBinding b : bindings) a.inputUris.add(Uri.parse(b.rawUri()));
        a.inputUri = a.inputUris.isEmpty() ? null : a.inputUris.get(0);
        UiSessionStore.saveInputUris(a, a.inputUris);
        a.setLabel(a.inputFileLabel, bindings.isEmpty() ? "Bộ chương: chưa tick chương nào" : "📚 " + setName + " · " + bindings.size() + " chương");
        a.updateInputEstimate();
    }

    private void render() {
        if (box == null) return;
        boolean set = MODE_SET.equals(mode);
        a.tint(filesButton, set ? a.FIELD : a.BLUE, set ? a.BORDER : a.BLUE, 1, 12);
        a.tint(setButton, set ? a.BLUE : a.FIELD, set ? a.BLUE : a.BORDER, 1, 12);
        if (a.filesSourceCard != null) a.filesSourceCard.setVisibility(set ? View.GONE : View.VISIBLE);
        chapterBox.setVisibility(set ? View.VISIBLE : View.GONE);
        chapterBox.removeAllViews();
        estimateViews.clear();
        if (!set) return;
        List<ChapterSetStore.SetInfo> sets = ChapterSetStore.listSets(a);
        String pickerText = sets.isEmpty() ? "Chưa có bộ chương — tạo ở Library > RAW"
                : setName.isEmpty() ? "Chọn bộ chương…  ▾" : "Bộ chương: " + setName + "  ▾";
        Button picker = a.secondaryButton(pickerText, v -> pickSet(sets));
        picker.setAllCaps(false);
        picker.setEnabled(!sets.isEmpty());
        picker.setContentDescription("set-picker");
        chapterBox.addView(picker, new LinearLayout.LayoutParams(-1, a.dp(46)));
        if (rows.isEmpty()) return;
        int translatedCount = 0;
        for (Row x : rows) if (x.translated) translatedCount++;
        TextView hint = a.text("Tick các chương cần dịch" + (translatedCount > 0 ? " (" + translatedCount + " chương đã có bản dịch xong, mặc định không tick)" : "") + ". Mỗi chương dùng đúng Glossary/Pronoun ghi trong bộ, không dùng bản Active.", 12, a.MUTED, false);
        hint.setSingleLine(false);
        chapterBox.addView(hint, a.marginLP(-1, -2, 0, 8, 0, 4));
        for (Row x : rows) chapterBox.addView(rowView(x), a.marginLP(-1, -2, 0, 4, 0, 0));
        TextView total = a.text(ticked.size() + "/" + rows.size() + " chương được tick", 13, a.GREEN, true);
        total.setContentDescription("chapter-total");
        chapterBox.addView(total, a.marginLP(-1, -2, 0, 8, 0, 0));
        chapterBox.addView(a.secondaryButton("Prompt preview (chọn chương)", v -> previewChapter()), a.marginLP(-1, a.dp(44), 0, 8, 0, 0));
        PreparedBatch current = a.currentPreparedBatch();
        if (current != null) updateEstimates(current);
    }

    private View rowView(Row x) {
        LinearLayout row = a.card(12, a.FIELD, x.usable() ? a.BORDER : a.RED);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(a.dp(10), a.dp(6), a.dp(10), a.dp(8));
        String number = x.stored.chapterNo >= 0 ? "Chương " + x.stored.chapterNo : "Chương ?";
        String name = x.usable() ? x.raw.originalName : "(RAW đã bị xóa)";
        CheckBox check = new CheckBox(a);
        check.setText(number + " · " + name);
        check.setTextColor(a.TEXT);
        check.setTextSize(14);
        check.setEnabled(x.usable());
        check.setChecked(x.usable() && ticked.contains(x.raw.id));
        check.setContentDescription("chapter:" + name);
        check.setOnClickListener(v -> {
            if (check.isChecked()) ticked.add(x.raw.id); else ticked.remove(x.raw.id);
            save();
            applySelection();
            render();
        });
        row.addView(check);
        String g = x.stored.glossaryId.isEmpty() ? "Không dùng" : x.glossary == null ? "(đã bị xóa)" : x.glossary.name;
        String p = x.stored.pronounId.isEmpty() ? "Không dùng" : x.pronoun == null ? "(đã bị xóa)" : x.pronoun.name;
        TextView meta = a.text("Glossary: " + g + "\nPronoun: " + p, 12, (g.startsWith("(") || p.startsWith("(")) ? a.AMBER : a.MUTED, false);
        meta.setSingleLine(false);
        row.addView(meta, a.marginLP(-1, -2, 32, 0, 0, 0));
        if (x.translated) row.addView(a.text("✓ đã có bản dịch xong", 12, a.GREEN, true), a.marginLP(-1, -2, 32, 2, 0, 0));
        if (x.usable()) {
            TextView est = a.text("", 12, a.MUTED, false);
            estimateViews.put(x.raw.id, est);
            row.addView(est, a.marginLP(-1, -2, 32, 2, 0, 0));
        }
        return row;
    }

    /** Opens a saved set as the source (the picker dialog calls this). */
    void selectSet(String id) {
        load();
        mode = MODE_SET;
        setId = id;
        refresh();
    }

    private void pickSet(List<ChapterSetStore.SetInfo> sets) {
        String[] names = new String[sets.size()];
        for (int i = 0; i < names.length; i++) names[i] = sets.get(i).name + " (" + sets.get(i).chapters + " chương)";
        new AlertDialog.Builder(a).setTitle("Chọn bộ chương").setItems(names, (d, which) -> {
            selectSet(sets.get(which).id);
        }).setNegativeButton("Hủy", null).show();
    }

    /** Fills each chapter row with its own chunk count and cost range once the plan is ready. */
    void updateEstimates(PreparedBatch batch) {
        if (!isSetMode() || batch == null || !batch.ready() || batch.bindings == null) return;
        AppSettings s = a.collectSettings();
        for (int i = 0; i < batch.inputs.size() && i < batch.bindings.size(); i++) {
            TextView view = estimateViews.get(batch.bindings.get(i).rawId);
            if (view == null) continue;
            CostEstimator.Estimate e = CostEstimator.estimatePreparedChunksLightweight(batch.inputs.get(i).chunks, PreparationCoordinator.settingsFor(s, batch.bindings, i));
            CostEstimator.applyPricing(e, s);
            view.setText(batch.inputs.get(i).chunks.size() + " chunk" + (e.pricingAvailable ? " · ước tính tối đa " + CostEstimator.money(e.costHigh) : " · chưa có giá"));
        }
    }

    /** Prompt preview with the chosen chapter's own Glossary and Pronoun. */
    void previewChapter() {
        List<ChapterBinding> bindings = freshBindings();
        if (bindings.isEmpty()) { a.toast("Chưa tick chương nào"); return; }
        String[] names = new String[bindings.size()];
        for (int i = 0; i < names.length; i++) names[i] = (bindings.get(i).chapterNo >= 0 ? "Chương " + bindings.get(i).chapterNo + " · " : "") + bindings.get(i).rawName;
        new AlertDialog.Builder(a).setTitle("Xem prompt của chương nào?").setItems(names, (d, which) -> {
            ChapterBinding b = bindings.get(which);
            PromptPreviewDialog.showForInput(a, Uri.parse(b.rawUri()), b.applyTo(a.collectSettings()), a.TEXT, a.MUTED, a.FIELD, a.BORDER, a::appendLog, a::toast);
        }).setNegativeButton("Hủy", null).show();
    }

    /** Problems that block Start in set mode, or null. */
    String readinessError() {
        List<ChapterBinding> bindings = freshBindings();
        if (setId.isEmpty()) return "Bộ chương: chưa chọn bộ";
        if (bindings.isEmpty()) return "Bộ chương: chưa tick chương nào";
        for (ChapterBinding b : bindings) {
            PromptContextBuilder.ParseReport r = PromptContextBuilder.validate(b.glossaryText, b.pronounText);
            if (!r.malformedGlossaryRows.isEmpty()) return "Chương " + b.chapterNo + ": Glossary có " + r.malformedGlossaryRows.size() + " dòng lỗi";
            if (!r.malformedPronounRows.isEmpty()) return "Chương " + b.chapterNo + ": Pronoun có " + r.malformedPronounRows.size() + " dòng lỗi";
        }
        return null;
    }

    /** Lock report for the pre-start warnings: the first chapter with a problem, else an empty report. */
    PromptContextBuilder.ParseReport locksReport() {
        PromptContextBuilder.ParseReport last = PromptContextBuilder.validate("", "");
        for (ChapterBinding b : freshBindings()) {
            PromptContextBuilder.ParseReport r = PromptContextBuilder.validate(b.glossaryText, b.pronounText);
            if (!r.conflicts.isEmpty() || !r.malformedGlossaryRows.isEmpty() || !r.malformedPronounRows.isEmpty()) return r;
            last = r;
        }
        return last;
    }

    /** "Locks" step text of the Translate screen. */
    String locksSummary() {
        int g = 0;
        for (Row x : rows) if (x.usable() && ticked.contains(x.raw.id) && x.glossary != null) g++;
        return ticked.size() + " chương · " + g + " glossary";
    }

}
