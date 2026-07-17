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


class FilesPageFactory {
    private final MainActivity a;

    FilesPageFactory(MainActivity activity) {
        this.a = activity;
    }


    View buildFilesPage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        LinearLayout files = a.sectionCard("📁", "Files");
        a.filesSummary = a.text(a.fileSummaryText(), 14, a.TEXT, false);
        a.filesSummary.setSingleLine(false);
        files.addView(a.filesSummary, a.marginLP(-1, -2, 0, 0, 0, 12));

        a.outputFileLabel = a.text(a.outputUri == null ? "Single output TXT: chưa chọn" : "Single output TXT: " + FileUtil.displayName(a, a.outputUri) + " (" + FileUtil.accessBadge(a, a.outputUri, true) + ")", 13, a.MUTED, false);
        a.outputFolderLabel = a.text(a.outputTreeUri == null ? "Output folder: chưa chọn" : "Output folder: " + FileUtil.treeName(a.outputTreeUri) + " (" + FileUtil.accessBadge(a, a.outputTreeUri, true) + ")", 13, a.MUTED, false);
        files.addView(fileActionCard("Create / choose output TXT", "Dịch 1 file có thể dùng output TXT riêng.", a.outputFileLabel, v -> a.chooseOutput()));
        files.addView(fileActionCard("Choose output folder", "Batch nhiều TXT sẽ tự tạo file trong folder này. Vuốt xuống hoặc bấm Refresh để scan lại file mới.", a.outputFolderLabel, v -> a.chooseOutputFolder()));
        LinearLayout fileActions = a.rowContainer();
        fileActions.addView(a.primaryButton("↻ Refresh files", v -> a.refreshFilesPage()), new LinearLayout.LayoutParams(0, a.dp(48), 1));
        fileActions.addView(a.space(8, 1));
        fileActions.addView(a.secondaryButton("Resume last checkpoint", v -> a.resumeCheckpoint()), new LinearLayout.LayoutParams(0, a.dp(48), 1));
        files.addView(fileActions, a.marginLP(-1, -2, 0, 4, 0, 10));
        files.addView(a.text("Naming Convention nằm trong Settings. Với batch, app tạo từng output theo tên input + target language.", 12, a.MUTED, false));
        files.addView(outputFolderFilesCard(), a.marginLP(-1, -2, 0, 12, 0, 0));

        LinearLayout history = a.card(14, a.FIELD, a.BORDER);
        history.setOrientation(LinearLayout.VERTICAL);
        history.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        history.addView(a.text("Recent translated files", 15, a.TEXT, true));
        List<TranslationRepository.Job> jobs = java.util.Collections.emptyList();
        if (jobs.isEmpty()) history.addView(a.text("Chưa có job nào.", 13, a.MUTED, false));
        else for (TranslationRepository.Job j : jobs) {
            TextView row = a.text(("done".equals(j.status) ? "✅ " : "• ") + j.fileName + "\n" + j.status, 12, "done".equals(j.status) ? a.GREEN : a.MUTED, false);
            row.setSingleLine(false);
            history.addView(row, a.marginLP(-1, -2, 0, 6, 0, 4));
        }
        files.addView(history, a.marginLP(-1, -2, 0, 14, 0, 0));
        root.addView(files);
        return scroll;
    }


    View outputFolderFilesCard() {
        LinearLayout box = a.card(14, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        box.addView(a.text("Output folder scan", 15, a.TEXT, true));
        TextView help = a.text("Vuốt từ trên xuống trong tab Files hoặc bấm Refresh files để quét lại folder đã chọn.", 12, a.MUTED, false);
        help.setSingleLine(false);
        box.addView(help, a.marginLP(-1, -2, 0, 4, 0, 8));
        a.outputFolderFilesList = new LinearLayout(a);
        a.outputFolderFilesList.setOrientation(LinearLayout.VERTICAL);
        a.outputFolderFilesList.addView(a.text("Bấm Refresh files để quét folder. App không tự quét khi chuyển tab.", 13, a.MUTED, false));
        box.addView(a.outputFolderFilesList);
        return box;
    }


    void populateOutputFolderFiles(LinearLayout list) {
        if (list == null) return;
        list.removeAllViews();
        if (a.outputTreeUri == null) {
            TextView empty = a.text("Chưa chọn output folder nên app không thể scan file mới trong hệ thống.", 13, a.MUTED, false);
            empty.setSingleLine(false);
            list.addView(empty);
            return;
        }
        String permissionProblem = FileUtil.validateTreeWritable(a, a.outputTreeUri, "Output folder");
        if (permissionProblem != null) {
            TextView empty = a.text(permissionProblem + "\nBấm Choose output folder để cấp lại quyền.", 13, a.RED, false);
            empty.setSingleLine(false);
            list.addView(empty);
            return;
        }
        List<FileUtil.TreeEntry> files = FileUtil.listFilesInTree(a, a.outputTreeUri, 50);
        if (files.isEmpty()) {
            list.addView(a.text("Không thấy file .txt nào trong output folder hiện tại.", 13, a.MUTED, false));
            return;
        }
        TextView count = a.text(files.size() + " file .txt gần nhất trong output folder", 12, a.GREEN, true);
        list.addView(count, a.marginLP(-1, -2, 0, 0, 0, 6));
        for (FileUtil.TreeEntry f : files) {
            TextView row = a.text("📄 " + f.name + "\n" + f.detail(), 12, a.TEXT, false);
            row.setSingleLine(false);
            row.setOnClickListener(v -> {
                Intent open = new Intent(Intent.ACTION_VIEW);
                open.setDataAndType(f.uri, "text/plain");
                open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { a.startActivity(open); } catch (Exception e) { a.toast("Không có app mở TXT"); }
            });
            list.addView(row, a.marginLP(-1, -2, 0, 5, 0, 5));
        }
    }

    void renderOutputFolderFiles(LinearLayout list, String permissionProblem, List<FileUtil.TreeEntry> files) {
        if (list == null) return;
        list.removeAllViews();
        if (permissionProblem != null) {
            TextView error = a.text(permissionProblem + "\nChoose the output folder again to restore access.", 13, a.RED, false);
            error.setSingleLine(false);
            list.addView(error);
            return;
        }
        if (files == null || files.isEmpty()) {
            list.addView(a.text("No TXT files found in the selected output folder.", 13, a.MUTED, false));
            return;
        }
        list.addView(a.text(files.size() + " recent TXT file(s)", 12, a.GREEN, true));
        for (FileUtil.TreeEntry f : files) {
            TextView row = a.text("📄 " + a.preview(f.name, 80) + "\n" + f.detail(), 12, a.TEXT, false);
            row.setSingleLine(false);
            row.setOnClickListener(v -> {
                Intent open = new Intent(Intent.ACTION_VIEW);
                open.setDataAndType(f.uri, "text/plain");
                open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { a.startActivity(open); } catch (Exception e) { a.toast("No app can open this TXT file"); }
            });
            list.addView(row, a.marginLP(-1, -2, 0, 5, 0, 5));
        }
    }


    View fileActionCard(String title, String desc, TextView label, View.OnClickListener listener) {
        LinearLayout box = a.card(14, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        box.addView(a.primaryButton(title, listener), new LinearLayout.LayoutParams(-1, a.dp(48)));
        TextView d = a.text(desc, 12, a.MUTED, false);
        d.setSingleLine(false);
        box.addView(d, a.marginLP(-1, -2, 0, 8, 0, 4));
        label.setSingleLine(false);
        box.addView(label);
        LinearLayout.LayoutParams lp = a.marginLP(-1, -2, 0, 8, 0, 8);
        box.setLayoutParams(lp);
        return box;
    }
}
