package com.ml.tblandroidtxt;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class PromptPreviewDialog {
    public interface LogSink { void append(String msg); }
    public interface ToastSink { void show(String msg); }

    public static void showForInput(Activity a, Uri inputUri, AppSettings settings,
                                    int textColor, int mutedColor, int fieldColor, int borderColor,
                                    LogSink log, ToastSink toast) {
        try {
            String text = FileUtil.readText(a, inputUri);
            List<Chunk> chunks = Chunker.chunkText(text, settings);
            if (chunks.isEmpty()) { toast.show("File trống"); return; }
            String label = FileUtil.displayName(a, inputUri);
            askChunkAndShowPrompt(a, chunks, settings, label, textColor, mutedColor, fieldColor, borderColor, log, toast);
        } catch (Exception e) {
            toast.show("Không tạo được prompt preview");
            log.append("Prompt preview lỗi: " + e.getMessage());
        }
    }

    private static void askChunkAndShowPrompt(Activity a, List<Chunk> chunks, AppSettings s, String label,
                                              int textColor, int mutedColor, int fieldColor, int borderColor,
                                              LogSink log, ToastSink toast) {
        final EditText e = input(a, "Chunk index", "1", textColor, mutedColor, fieldColor, borderColor);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        int pad = dp(a, 20);
        FrameLayout box = new FrameLayout(a);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(e, new FrameLayout.LayoutParams(-1, dp(a, 54)));
        new AlertDialog.Builder(a)
                .setTitle("Prompt preview • chọn chunk 1-" + chunks.size())
                .setView(box)
                .setPositiveButton("Preview", (d, w) -> {
                    int idx = intVal(e, 1) - 1;
                    if (idx < 0) idx = 0;
                    if (idx >= chunks.size()) idx = chunks.size() - 1;
                    showPromptForChunk(a, chunks.get(idx), idx + 1, chunks.size(), s, label, textColor, log, toast);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showPromptForChunk(Activity a, Chunk chunk, int oneBased, int total, AppSettings s, String label,
                                           int textColor, LogSink log, ToastSink toast) {
        try {
            PromptPair p = PromptBuilder.translationPrompt(chunk, "", s);
            String matched = PromptContextBuilder.preview(s.glossaryText, s.pronounText, chunk.mainContent,
                    chunk.contextBefore + "\n" + chunk.mainContent,
                    chunk.paragraphStart, chunk.paragraphEnd, s);
            String body = "[SOURCE]\n" + label + "\n\n[CHUNK]\n" + oneBased + "/" + total
                    + "\n\n[MATCHED GLOSSARY / PRONOUN RULES]\n" + matched
                    + "\n\n[SYSTEM]\n" + p.system + "\n\n[USER]\n" + p.user;
            showTextDialog(a, "Prompt preview • chunk " + oneBased + "/" + total, body, textColor);
            log.append("Prompt preview: " + label + " • chunk " + oneBased + "/" + total);
        } catch (Exception e) {
            toast.show("Không tạo được prompt preview");
            log.append("Prompt preview chunk lỗi: " + e.getMessage());
        }
    }

    private static void showTextDialog(Activity a, String title, String body, int textColor) {
        TextView tv = new TextView(a);
        tv.setText(body == null ? "" : body);
        tv.setTextSize(11);
        tv.setTextColor(textColor);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        tv.setSingleLine(false);
        ScrollView sv = new ScrollView(a);
        sv.setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10));
        sv.addView(tv);
        new AlertDialog.Builder(a).setTitle(title).setView(sv).setPositiveButton("OK", null).show();
    }

    private static EditText input(Activity a, String hint, String value, int textColor, int mutedColor, int fieldColor, int borderColor) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(textColor);
        e.setHintTextColor(mutedColor);
        e.setTextSize(14);
        e.setSingleLine(false);
        e.setMinLines(1);
        e.setMaxLines(3);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        e.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(fieldColor);
        g.setCornerRadius(dp(a, 12));
        g.setStroke(dp(a, 1), borderColor);
        e.setBackground(g);
        return e;
    }

    private static int intVal(EditText e, int def) { try { return Integer.parseInt(e.getText().toString().trim()); } catch (Exception ex) { return def; } }
    private static int dp(Activity a, int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
