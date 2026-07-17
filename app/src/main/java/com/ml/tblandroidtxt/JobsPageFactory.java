package com.ml.tblandroidtxt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JobsPageFactory {
    private static String statusFilter="ALL";
    public interface LogSink { void append(String msg); }
    public interface ToastSink { void show(String msg); }
    public interface JobActionSink { void run(long jobId); }

    private final Activity a;
    private final int BG, PANEL, CARD, FIELD, BORDER, TEXT, MUTED, BLUE, GREEN, CYAN, RED, AMBER;
    private final Runnable onRefresh, onResume, onRetry, onExportLog, onClearLog;
    private final JobActionSink onResumeJob, onRetryJob, onExportJobLog;
    private final LogSink log;
    private final ToastSink toast;

    public JobsPageFactory(Activity a,
                           int BG, int PANEL, int CARD, int FIELD, int BORDER, int TEXT, int MUTED, int BLUE, int GREEN, int CYAN, int RED, int AMBER,
                           Runnable onRefresh, Runnable onResume, Runnable onRetry, Runnable onExportLog, Runnable onClearLog,
                           JobActionSink onResumeJob, JobActionSink onRetryJob, JobActionSink onExportJobLog,
                           LogSink log, ToastSink toast) {
        this.a = a;
        this.BG = BG; this.PANEL = PANEL; this.CARD = CARD; this.FIELD = FIELD; this.BORDER = BORDER;
        this.TEXT = TEXT; this.MUTED = MUTED; this.BLUE = BLUE; this.GREEN = GREEN; this.CYAN = CYAN; this.RED = RED; this.AMBER = AMBER;
        this.onRefresh = onRefresh; this.onResume = onResume; this.onRetry = onRetry; this.onExportLog = onExportLog; this.onClearLog = onClearLog;
        this.onResumeJob = onResumeJob; this.onRetryJob = onRetryJob; this.onExportJobLog = onExportJobLog;
        this.log = log; this.toast = toast;
    }

    public View build() {
        ScrollView scroll = scroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);

        List<TranslationRepository.JobSummary> all = a instanceof MainActivity
                ? ((MainActivity)a).jobSummariesSnapshot() : java.util.Collections.emptyList();
        List<TranslationRepository.JobSummary> list = new ArrayList<>();
        for(TranslationRepository.JobSummary j:all)if(matchesFilter(j))list.add(j);

        LinearLayout jobs = sectionCard("🧾", "Job Manager / Checkpoints");
        TextView desc = text(AppBuildInfo.RELEASE_LABEL + ": đóng băng tính năng, giữ core 2.7/SAF 2.8/glossary 2.8.5/UI 2.9; ưu tiên resume/retry/export log để QA release.", 13, MUTED, false);
        desc.setSingleLine(false);
        jobs.addView(desc, marginLP(-1, -2, 0, 0, 0, 12));

        jobs.addView(summaryDashboard(list), marginLP(-1, -2, 0, 0, 0, 12));

        LinearLayout actions = rowContainer();
        actions.addView(primaryButton("↻ Refresh", v -> onRefresh.run()), new LinearLayout.LayoutParams(0, dp(46), 1));
        actions.addView(space(8, 1));
        actions.addView(secondaryButton("Resume last", v -> onResume.run()), new LinearLayout.LayoutParams(0, dp(46), 1));
        jobs.addView(actions, marginLP(-1, -2, 0, 0, 0, 8));

        jobs.addView(secondaryButton("Filters: "+statusFilter,v->showFilters()),marginLP(-1,dp(42),0,0,0,8));

        LinearLayout actions2 = rowContainer();
        actions2.addView(secondaryButton("Retry last failed", v -> onRetry.run()), new LinearLayout.LayoutParams(0, dp(46), 1));
        actions2.addView(space(8, 1));
        actions2.addView(secondaryButton("Export all logs", v -> onExportLog.run()), new LinearLayout.LayoutParams(0, dp(46), 1));
        actions2.addView(space(8, 1));
        actions2.addView(dangerButton("Clear logs", v -> onClearLog.run()), new LinearLayout.LayoutParams(0, dp(46), 1));
        jobs.addView(actions2, marginLP(-1, -2, 0, 0, 0, 12));

        if (list.isEmpty()) {
            TextView empty = text("Chưa có checkpoint/job nào. Sau khi chạy dịch, job sẽ hiện ở đây để resume/retry/export.", 14, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setSingleLine(false);
            jobs.addView(empty, new LinearLayout.LayoutParams(-1, dp(110)));
        } else {
            for (TranslationRepository.JobSummary j : list) jobs.addView(jobSummaryCard(j), marginLP(-1, -2, 0, 0, 0, 10));
        }
        root.addView(jobs);

        LinearLayout logBox = sectionCard("🪵", "Runtime log tail");
        String logs = a instanceof MainActivity ? ((MainActivity)a).runtimeLogSnapshot() : "";
        TextView logText = text(logs.isEmpty() ? "No runtime log yet." : tail(logs, 12000), 11, TEXT, false);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextIsSelectable(true);
        logText.setSingleLine(false);
        ScrollView logScroll = new ScrollView(a);
        logScroll.setFillViewport(false);
        logScroll.addView(logText, new ScrollView.LayoutParams(-1, -2));
        LinearLayout logInner = card(14, FIELD, BORDER);
        logInner.setOrientation(LinearLayout.VERTICAL);
        logInner.setPadding(dp(12), dp(12), dp(12), dp(12));
        logInner.addView(logScroll, new LinearLayout.LayoutParams(-1, dp(240)));
        logBox.addView(logInner);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        root.addView(logBox);
        return scroll;
    }

    public void openJobDetails(long jobId) { showJobDetails(jobId); }

    public void showDeveloperTools() {
        ScrollView scroll = scroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);
        LinearLayout logs = sectionCard("≡", "Runtime diagnostics");
        String raw = a instanceof MainActivity ? ((MainActivity)a).runtimeLogSnapshot() : "";
        TextView logText = text(raw.isEmpty() ? "No runtime log yet." : tail(raw, 6000), 11, TEXT, false);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextIsSelectable(true);
        logText.setSingleLine(false);
        logs.addView(logText);
        LinearLayout logActions = rowContainer();
        logActions.addView(secondaryButton("Export", v -> onExportLog.run()), new LinearLayout.LayoutParams(0, dp(42), 1));
        logActions.addView(space(8, 1));
        logActions.addView(dangerButton("Clear", v -> onClearLog.run()), new LinearLayout.LayoutParams(0, dp(42), 1));
        logs.addView(logActions, marginLP(-1, -2, 0, 10, 0, 0));
        root.addView(logs);

        List<TranslationRepository.JobSummary> jobs = a instanceof MainActivity
                ? ((MainActivity)a).jobSummariesSnapshot() : java.util.Collections.emptyList();
        LinearLayout tools = sectionCard("{}", "Job developer tools");
        int shown = 0;
        for (TranslationRepository.JobSummary job : jobs) {
            LinearLayout card = card(12, FIELD, BORDER);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(10), dp(10), dp(10));
            card.addView(text("#" + job.id + " · " + nonEmpty(job.fileName, "Unknown file"), 13, TEXT, true));
            LinearLayout first = rowContainer();
            first.addView(tinyButton("Prompt dump", CYAN, v -> showPromptPreviewForJob(job.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
            first.addView(space(6,1));
            first.addView(tinyButton("Chunk inspector", CYAN, v -> showChunkInspector(job.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
            card.addView(first, marginLP(-1,-2,0,8,0,0));
            LinearLayout second = rowContainer();
            second.addView(tinyButton("Dry A/B", BLUE, v -> showDryBenchmark(job.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
            second.addView(space(6,1));
            second.addView(tinyButton("Live A/B", AMBER, v -> confirmLiveBenchmark(job.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
            second.addView(space(6,1));
            second.addView(tinyButton("Blind review", GREEN, v -> showBenchmarkReview(job.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
            card.addView(second, marginLP(-1,-2,0,8,0,0));
            LinearLayout third = rowContainer();
            third.addView(tinyButton("Export job log", GREEN, v -> onExportJobLog.run(job.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
            third.addView(space(6,1));
            third.addView(tinyButton("Apply current config", AMBER, v -> applyCurrentConfig(job.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
            card.addView(third, marginLP(-1,-2,0,8,0,0));
            tools.addView(card, marginLP(-1,-2,0,0,0,8));
            if (++shown == 8) break;
        }
        if (shown == 0) tools.addView(text("No jobs available.", 13, MUTED, false));
        root.addView(tools);
        new AlertDialog.Builder(a).setTitle("Developer tools").setView(scroll).setPositiveButton("Close", null).show();
    }

    private View summaryDashboard(List<TranslationRepository.JobSummary> list) {
        int running = 0, done = 0, error = 0, paused = 0, cancelled = 0, failedChunks = 0, total = list == null ? 0 : list.size();
        if (list != null) {
            for (TranslationRepository.JobSummary j : list) {
                String st = j.status == null ? "" : j.status;
                if ("done".equals(st)) done++;
                else if ("error".equals(st)) error++;
                else if ("paused".equals(st)) paused++;
                else if ("cancelled".equals(st)) cancelled++;
                else running++;
                failedChunks += Math.max(0, j.failedChunks);
            }
        }
        LinearLayout box = card(14, FIELD, BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView t = text("Jobs: " + total + " • running " + running + " • paused " + paused + " • error " + error + " • done " + done + " • cancelled " + cancelled + " • failed chunks " + failedChunks, 12, TEXT, true);
        t.setSingleLine(false);
        box.addView(t);
        return box;
    }

    private View jobSummaryCard(TranslationRepository.JobSummary j) {
        LinearLayout box = card(14, FIELD, BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        int color = jobStatusColor(j.status);
        TextView title = text(jobStatusIcon(j.status) + " #" + j.id + " • " + nonEmpty(j.fileName, "Unknown file"), 14, color, true);
        title.setSingleLine(false);
        box.addView(title);

        int pct = j.totalChunks <= 0 ? 0 : Math.round(j.doneChunks * 100f / j.totalChunks);
        String meta = nonEmpty(j.sourceLanguage, "?") + " → " + nonEmpty(j.targetLanguage, "?")
                + " • " + nonEmpty(j.status, "unknown")
                + " • " + pct + "%"
                + " • done " + j.doneChunks + "/" + j.totalChunks
                + " • failed " + j.failedChunks
                + " • pending " + j.pendingChunks
                + "\ncreated: " + dateTime(j.createdAt)
                + " • updated: " + dateTime(j.updatedAt)
                + "\noutput: " + previewUri(j.outputUri, 100);
        TextView m = text(meta, 12, MUTED, false);
        m.setSingleLine(false);
        box.addView(m, marginLP(-1, -2, 0, 4, 0, 8));

        ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(pct);
        box.addView(bar, new LinearLayout.LayoutParams(-1, dp(8)));

        LinearLayout buttons = rowContainer();
        Button resume = tinyButton("Resume", BLUE, v -> resumeSelectedJob(j));
        Button retry = tinyButton("Retry", AMBER, v -> retrySelectedJob(j));
        buttons.addView(resume, new LinearLayout.LayoutParams(0, dp(40), 1));
        buttons.addView(space(6, 1));
        buttons.addView(retry, new LinearLayout.LayoutParams(0, dp(40), 1));
        buttons.addView(space(6, 1));
        buttons.addView(tinyButton("Details", CYAN, v -> showJobDetails(j.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
        box.addView(buttons, marginLP(-1, -2, 0, 10, 0, 0));

        LinearLayout buttons2 = rowContainer();
        buttons2.addView(tinyButton("Prompt", CYAN, v -> showPromptPreviewForJob(j.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
        buttons2.addView(space(6, 1));
        buttons2.addView(tinyButton("Export", GREEN, v -> onExportJobLog.run(j.id)), new LinearLayout.LayoutParams(0, dp(40), 1));
        buttons2.addView(space(6, 1));
        buttons2.addView(tinyButton("Delete", RED, v -> confirmDeleteJob(j)), new LinearLayout.LayoutParams(0, dp(40), 1));
        box.addView(buttons2, marginLP(-1, -2, 0, 8, 0, 0));
        box.addView(tinyButton("Dry A/B benchmark (no API cost)", BLUE, v -> showDryBenchmark(j.id)), marginLP(-1, dp(40), 0, 8, 0, 0));
        LinearLayout benchmarkButtons=rowContainer();
        benchmarkButtons.addView(tinyButton("Live A/B", AMBER, v -> confirmLiveBenchmark(j.id)),new LinearLayout.LayoutParams(0,dp(40),1));
        benchmarkButtons.addView(space(6,1));
        benchmarkButtons.addView(tinyButton("Blind review", GREEN, v -> showBenchmarkReview(j.id)),new LinearLayout.LayoutParams(0,dp(40),1));
        box.addView(benchmarkButtons,marginLP(-1,-2,0,0,0,0));
        box.addView(tinyButton("Chunk Inspector",CYAN,v->showChunkInspector(j.id)),marginLP(-1,dp(40),0,8,0,0));
        LinearLayout reliability=rowContainer();
        reliability.addView(tinyButton("Continue incomplete",BLUE,v->continueIncomplete(j.id)),new LinearLayout.LayoutParams(0,dp(40),1));
        reliability.addView(space(6,1));
        reliability.addView(tinyButton("Apply current config to remaining",AMBER,v->applyCurrentConfig(j.id)),new LinearLayout.LayoutParams(0,dp(40),1));
        box.addView(reliability,marginLP(-1,-2,0,8,0,0));

        if ("done".equals(j.status)) resume.setEnabled(false);
        if (j.failedChunks <= 0) retry.setEnabled(false);
        return box;
    }

    private void resumeSelectedJob(TranslationRepository.JobSummary j) {
        if ("done".equals(j.status)) { toast.show("Job #" + j.id + " đã hoàn tất"); return; }
        new AlertDialog.Builder(a)
                .setTitle("Resume job #" + j.id + "?")
                .setMessage("App sẽ chạy tiếp các chunk chưa done của file:\n" + nonEmpty(j.fileName, "unknown") + "\n\nDone " + j.doneChunks + "/" + j.totalChunks + ", failed " + j.failedChunks + ", pending " + j.pendingChunks)
                .setPositiveButton("Resume", (d, w) -> onResumeJob.run(j.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void retrySelectedJob(TranslationRepository.JobSummary j) {
        if (j.failedChunks <= 0) { toast.show("Job #" + j.id + " không có chunk lỗi"); return; }
        new AlertDialog.Builder(a)
                .setTitle("Retry failed chunks #" + j.id + "?")
                .setMessage("Chỉ dịch lại " + j.failedChunks + " chunk lỗi của file:\n" + nonEmpty(j.fileName, "unknown") + "\n\nCác chunk đã done sẽ được giữ nguyên.")
                .setPositiveButton("Retry", (d, w) -> onRetryJob.run(j.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJobDetails(long jobId) {
        try {
            JobStore jobStore = new JobStore(a);
            TranslationRepository.JobSummary summary = jobStore.summary(jobId);
            TranslationRepository.Job job = jobStore.get(jobId);
            List<TranslationRepository.ChunkRow> rows = jobStore.chunks(jobId);
            StringBuilder sb = new StringBuilder();
            sb.append("Job #").append(jobId).append("\n");
            if (summary != null) {
                sb.append("File: ").append(nonEmpty(summary.fileName, "unknown")).append("\n");
                sb.append("Status: ").append(nonEmpty(summary.status, "unknown")).append("\n");
                sb.append("Language: ").append(nonEmpty(summary.sourceLanguage, "?")).append(" → ").append(nonEmpty(summary.targetLanguage, "?")).append("\n");
                sb.append("Created: ").append(dateTime(summary.createdAt)).append("\n");
                sb.append("Updated: ").append(dateTime(summary.updatedAt)).append("\n");
                sb.append("Chunks: total=").append(summary.totalChunks)
                        .append(", done=").append(summary.doneChunks)
                        .append(", failed=").append(summary.failedChunks)
                        .append(", pending=").append(summary.pendingChunks).append("\n");
                sb.append("Input URI: ").append(nonEmpty(summary.inputUri, "")).append("\n");
                sb.append("Output URI: ").append(nonEmpty(summary.outputUri, "")).append("\n");
            } else if (job != null) {
                sb.append("File: ").append(nonEmpty(job.fileName, "unknown")).append("\n");
                sb.append("Status: ").append(nonEmpty(job.status, "unknown")).append("\n");
                sb.append("Input URI: ").append(nonEmpty(job.inputUri, "")).append("\n");
                sb.append("Output URI: ").append(nonEmpty(job.outputUri, "")).append("\n");
            }
            sb.append("\n");
            TranslationRepository.MetricsSummary metrics = jobStore.metrics(jobId);
            if (metrics != null && metrics.attempts > 0) {
                sb.append("ESTIMATED BEFORE RUN\n")
                        .append("Input: ").append(metrics.estimatedInputTokens).append(" | Estimated cost: ").append(CostEstimator.money(metrics.estimatedCostBefore)).append("\n\n")
                        .append("ACTUAL AFTER RUN\n")
                        .append("Usage source: PROVIDER rows ").append(metrics.providerUsageRows).append(" | ESTIMATED/UNKNOWN rows ").append(metrics.estimatedUsageRows).append("\n")
                        .append("Input: ").append(metrics.actualInputTokens).append(" | Cached input: ").append(metrics.cachedInputTokens).append(" | Uncached input: ").append(metrics.uncachedInputTokens).append("\n")
                        .append("Output: ").append(metrics.outputTokens).append(" | Retry overhead (provider/estimated): ").append(metrics.retryTokens).append("\n")
                        .append("Total actual cost: ").append(CostEstimator.money(metrics.totalCost)).append(" | Unknown pricing/usage rows: ").append(metrics.unknownCostRows).append("\n\n")
                        .append("PROMPT BREAKDOWN\n")
                        .append("Raw: ").append(metrics.rawTokens).append(" | Instruction: ").append(metrics.instructionTokens).append("\n")
                        .append("Glossary: ").append(metrics.glossaryTokens).append(" | Pronoun: ").append(metrics.pronounTokens).append("\n")
                        .append("Context: ").append(metrics.contextTokens).append("\n")
                        .append("Estimated input saved vs full: ").append(metrics.estimatedSavedInputTokens()).append(" tokens\n")
                        .append("Attempts: ").append(metrics.attempts).append(" (success ").append(metrics.successes).append(")\n\n");
            }
            TranslationRepository.TokenCalibration calibration=jobStore.calibration(jobId);
            AppSettings snapshot=job==null?new AppSettings():SettingsStore.fromJson(job.settingsJson);
            sb.append("Provider capabilities: ").append(ProviderCapabilities.forSettings(snapshot).summary()).append("\n");
            if(calibration.samples>0)sb.append("Tokenizer calibration: actual/estimated = ").append(String.format(Locale.US,"%.3f",calibration.ratio())).append(" from ").append(calibration.samples).append(" provider samples\n");
            else sb.append("Tokenizer calibration: chưa có provider usage samples\n");
            ModelCatalog.ModelInfo price=ModelCatalog.findModelInfo(snapshot.provider,snapshot.model);
            sb.append("Pricing source: ").append(price.pricingSource).append(" • ").append(price.priceSuffix()).append("\n\n");
            int shown = 0;
            for (TranslationRepository.ChunkRow r : rows) {
                if (shown >= 160) { sb.append("...\n"); break; }
                sb.append("#").append(r.index + 1).append(" • ").append(nonEmpty(r.status, "unknown"));
                if (r.error != null && !r.error.trim().isEmpty()) sb.append(" • ").append(r.error.replace('\n', ' '));
                sb.append("\nSRC: ").append(preview(r.source, 220).replace('\n', ' '));
                if (r.translated != null && !r.translated.trim().isEmpty()) sb.append("\nOUT: ").append(preview(r.translated, 220).replace('\n', ' '));
                sb.append("\n\n");
                shown++;
            }
            showTextDialog("Job details", sb.toString());
        } catch (Exception e) {
            toast.show("Không đọc được checkpoint");
            log.append("Job details lỗi: " + e.getMessage());
        }
    }

    private void showPromptPreviewForJob(long jobId) {
        try {
            JobStore jobStore = new JobStore(a);
            TranslationRepository.Job job = jobStore.get(jobId);
            if (job == null) { toast.show("Không thấy job"); return; }
            AppSettings s = SettingsStore.fromJson(job.settingsJson);
            List<TranslationRepository.ChunkRow> rows = jobStore.chunks(jobId);
            ArrayList<Chunk> chunks = new ArrayList<>();
            for (TranslationRepository.ChunkRow r : rows) chunks.add(new Chunk(r.index, "", r.source, ""));
            if (chunks.isEmpty()) { toast.show("Job không có chunk"); return; }
            askChunkAndShowPrompt(chunks, s, "Job #" + jobId + " • " + nonEmpty(job.fileName, "unknown"));
        } catch (Exception e) {
            toast.show("Không preview được job");
            log.append("Prompt preview job lỗi: " + e.getMessage());
        }
    }

    private void showChunkInspector(long jobId){
        try{JobStore store=new JobStore(a);List<TranslationRepository.ChunkRow>rows=store.chunks(jobId);if(rows.isEmpty()){toast.show("No chunks");return;}String[]labels=new String[rows.size()];for(int i=0;i<rows.size();i++){TranslationRepository.ChunkRow r=rows.get(i);labels[i]="#"+(r.index+1)+" • "+nonEmpty(r.status,"unknown")+" • attempt "+r.attempt;}
            new AlertDialog.Builder(a).setTitle("Chunk Inspector • Job #"+jobId).setItems(labels,(d,which)->showChunk(jobId,rows.get(which),store)).setNegativeButton("Close",null).show();
        }catch(Exception e){toast.show("Cannot open chunk inspector");}
    }

    private void showChunk(long jobId,TranslationRepository.ChunkRow r,JobStore store){
        StringBuilder b=new StringBuilder();b.append("Stable ID: ").append(nonEmpty(r.stableId,"unknown")).append("\nRange: ").append(r.startOffset).append("..").append(r.endOffset).append("\nStatus: ").append(r.status).append("\nAttempt: ").append(r.attempt).append("\nFinish reason: ").append(nonEmpty(r.finishReason,"unknown")).append("\nSource hash: ").append(nonEmpty(r.sourceHash,"unknown")).append("\nRequest hash: ").append(nonEmpty(r.requestHash,"unknown")).append("\nResponse hash: ").append(nonEmpty(r.responseHash,"unknown")).append("\nResult source: ").append(nonEmpty(r.resultSource,"unknown")).append("\nWarnings: ").append(nonEmpty(r.validationWarnings,"none")).append("\nError: ").append(nonEmpty(r.error,"none")).append("\n\nSOURCE\n").append(nonEmpty(r.source,"(empty)")).append("\n\nTRANSLATION\n").append(nonEmpty(r.translated,"(none)")).append("\n\nATTEMPTS\n");
        for(TranslationRepository.AttemptRow x:store.attempts(jobId,r.index))b.append("#").append(x.attempt).append(" ").append(x.status).append(" • ").append(x.provider).append("/").append(x.model).append(" • finish=").append(x.finishReason).append(" • tokens=").append(x.inputTokens).append('/').append(x.outputTokens).append(" cached=").append(x.cachedTokens).append(" • cost=").append(CostEstimator.money(x.actualCost)).append(" ").append(x.usageSource).append(" • ").append(x.validationResult).append("\n").append(nonEmpty(x.responsePreview,"(no preview)")).append("\n");
        AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle("Chunk #"+(r.index+1)).setMessage(b.toString()).setPositiveButton("Manual replace",(d,w)->showManualReplacement(jobId,r,store));
        if("DELIVERY_UNKNOWN".equalsIgnoreCase(r.status))dialog.setNegativeButton("Retry anyway",(d,w)->new AlertDialog.Builder(a).setTitle("Possible duplicate charge").setMessage("The previous request may have reached the provider. Retrying can create a second paid request. Continue only after reviewing the attempt timeline.").setPositiveButton("Retry anyway",(confirm,which)->retryOneChunk(jobId,r.index,true)).setNegativeButton("Cancel",null).show());
        else dialog.setNeutralButton("Retry edited",(d,w)->showEditedRetry(jobId,r.index,store)).setNegativeButton("Retry/continue",(d,w)->retryOneChunk(jobId,r.index));
        dialog.show();
    }

    private void showManualReplacement(long jobId,TranslationRepository.ChunkRow row,JobStore store){EditText edit=new EditText(a);edit.setText(row.translated);edit.setMinLines(8);edit.setGravity(Gravity.TOP);edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);new AlertDialog.Builder(a).setTitle("Validated manual replacement").setMessage("Original model attempts remain in history. Manual text has no provider token usage.").setView(edit).setPositiveButton("Validate & accept",(d,w)->{try{store.manualReplace(jobId,row,edit.getText().toString());toast.show("Manual replacement accepted and output rebuilt");}catch(Exception e){toast.show(e.getMessage());}}).setNegativeButton("Cancel",null).show();}
    private void copyText(String label,String value){ClipboardManager cb=(ClipboardManager)a.getSystemService(Activity.CLIPBOARD_SERVICE);if(cb!=null)cb.setPrimaryClip(ClipData.newPlainText(label,value==null?"":value));toast.show("Copied "+label);}
    private void retryOneChunk(long jobId,int idx){retryOneChunk(jobId,idx,false);}
    private void retryOneChunk(long jobId,int idx,boolean confirmUnknown){Intent i=new Intent(a,TranslatorService.class);i.setAction(TranslatorService.ACTION_RETRY_CHUNK);i.putExtra("jobId",jobId);i.putExtra("chunkIdx",idx);i.putExtra("confirmDeliveryUnknown",confirmUnknown);a.startForegroundService(i);toast.show("Retry queued for chunk "+(idx+1));}

    private void showEditedRetry(long jobId,int idx,JobStore store){TranslationRepository.Job job=store.get(jobId);if(job==null)return;AppSettings s=SettingsStore.fromJson(job.settingsJson);LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),dp(6),dp(18),0);EditText model=input("Exact model ID",s.model);EditText max=number("Maximum output tokens",String.valueOf(s.maxOutputTokens));EditText attempts=number("Attempts",String.valueOf(s.maxAttempts));form.addView(model,new LinearLayout.LayoutParams(-1,dp(56)));form.addView(max,new LinearLayout.LayoutParams(-1,dp(56)));form.addView(attempts,new LinearLayout.LayoutParams(-1,dp(56)));new AlertDialog.Builder(a).setTitle("Retry chunk "+(idx+1)+" with edited settings").setMessage("A configuration revision is recorded from this chunk. Completed chunks remain unchanged.").setView(form).setPositiveButton("Retry",(d,w)->{s.model=model.getText().toString().trim();s.maxOutputTokens=intVal(max,s.maxOutputTokens);s.maxAttempts=intVal(attempts,s.maxAttempts);Intent i=new Intent(a,TranslatorService.class);i.setAction(TranslatorService.ACTION_RETRY_CHUNK);i.putExtra("jobId",jobId);i.putExtra("chunkIdx",idx);i.putExtra("settings",SettingsStore.toJson(AppValidator.normalize(s)));a.startForegroundService(i);toast.show("Edited retry queued; revision applies from chunk "+(idx+1));}).setNegativeButton("Cancel",null).show();}

    private void continueIncomplete(long jobId){JobStore store=new JobStore(a);int idx=store.firstRunnableChunk(jobId);if(idx<0){toast.show("All chunks are complete");return;}retryOneChunk(jobId,idx);}
    private void applyCurrentConfig(long jobId){JobStore store=new JobStore(a);int idx=store.firstRunnableChunk(jobId);if(idx<0){toast.show("No remaining chunks");return;}AppSettings current=SettingsStore.load(a);new AlertDialog.Builder(a).setTitle("Apply current configuration?").setMessage("Revision starts at chunk "+(idx+1)+". Completed chunks and their attempt history are not changed.\n\nModel: "+current.model).setPositiveButton("Apply",(d,w)->{int revision=store.applyRevision(jobId,idx,current);toast.show("Configuration revision "+revision+" saved for remaining chunks");}).setNegativeButton("Cancel",null).show();}
    private boolean matchesFilter(TranslationRepository.JobSummary j){if("ALL".equals(statusFilter))return true;String s=nonEmpty(j.status,"").toUpperCase(Locale.US);if("INCOMPLETE".equals(statusFilter))return !"DONE".equals(s)&&!"INTEGRITY_PASSED".equals(s);if("FAILED".equals(statusFilter))return j.failedChunks>0||"ERROR".equals(s)||"NEEDS_REVIEW".equals(s);return statusFilter.equals(s);}
    private void showFilters(){String[] values={"ALL","INCOMPLETE","FAILED","PAUSED","DONE"};int selected=0;for(int i=0;i<values.length;i++)if(values[i].equals(statusFilter))selected=i;new AlertDialog.Builder(a).setTitle("Job Manager filter").setSingleChoiceItems(values,selected,(d,w)->{statusFilter=values[w];d.dismiss();onRefresh.run();}).setNegativeButton("Cancel",null).show();}

    private void showDryBenchmark(long jobId) {
        try {
            JobStore store = new JobStore(a);
            TranslationRepository.Job job = store.get(jobId);
            if (job == null) { toast.show("Không thấy job"); return; }
            AppSettings settings = SettingsStore.fromJson(job.settingsJson);
            ArrayList<Chunk> chunks = new ArrayList<>();
            for (TranslationRepository.ChunkRow row : store.chunks(jobId)) chunks.add(new Chunk(row.index, "", row.source, ""));
            BenchmarkReport report = BenchmarkReport.dryRun(chunks, settings);
            showTextDialog("Dry A/B benchmark • Job #" + jobId, report.asText()
                    + "\nQuality is NOT evaluated by this dry run. Paired live/manual review is still required.");
        } catch (Exception e) {
            toast.show("Không tạo được benchmark");
            log.append("Dry benchmark lỗi: " + e.getMessage());
        }
    }

    private void confirmLiveBenchmark(long jobId){
        try{
            JobStore store=new JobStore(a);TranslationRepository.Job job=store.get(jobId);if(job==null){toast.show("Không thấy job");return;}
            AppSettings s=SettingsStore.fromJson(job.settingsJson);List<TranslationRepository.ChunkRow> all=store.chunks(jobId);
            List<TranslationRepository.ChunkRow> samples=LiveBenchmarkRunner.selectSamples(all);ModelCatalog.ModelInfo pricing=ModelCatalog.findModelInfo(s.provider,s.model);
            if(s.apiKey==null||s.apiKey.trim().isEmpty()){toast.show("Job snapshot không có API key. Hãy tạo job mới sau khi lưu key.");return;}
            if(pricing==null||!pricing.hasPricing()){toast.show("Chưa có dữ liệu giá; không thể bảo vệ budget benchmark.");return;}
            double worst=LiveBenchmarkRunner.estimateWorst(samples,s,pricing,s.maxOutputTokens);double budget=0.50;
            if(worst>budget){toast.show("Worst-case benchmark vượt budget $0.50; hãy giảm MAX_OUTPUT_TOKENS.");return;}
            String message="This sends "+samples.size()+" stored source chunks to "+s.provider+" using "+s.model+".\n\n"
                    +"Requests: "+(samples.size()*2)+" (Full + Balanced)\nWorst-case estimate: "+CostEstimator.money(worst)+"\nHard budget: "+CostEstimator.money(budget)
                    +"\n\nOutputs are stored locally with blind labels X/Y. Translation job output is not changed.";
            new AlertDialog.Builder(a).setTitle("Run paid live A/B benchmark?").setMessage(message)
                    .setPositiveButton("Run",(d,w)->{Intent i=new Intent(a,TranslatorService.class);i.setAction(TranslatorService.ACTION_BENCHMARK_JOB);i.putExtra("jobId",jobId);i.putExtra("budgetUsd",budget);a.startForegroundService(i);toast.show("Benchmark started in service");})
                    .setNegativeButton("Cancel",null).show();
        }catch(Exception e){toast.show("Không chuẩn bị được live benchmark");log.append("Live benchmark readiness lỗi: "+e.getMessage());}
    }

    private void showBenchmarkReview(long jobId){
        JobStore store=new JobStore(a);TranslationRepository.BenchmarkSummary summary=store.latestBenchmark(jobId);
        if(summary==null){toast.show("Job chưa có live benchmark");return;}
        List<TranslationRepository.BenchmarkResultRow> rows=store.benchmarkResults(summary.id);StringBuilder blind=new StringBuilder();
        blind.append("Benchmark #").append(summary.id).append(" • ").append(summary.status).append("\nModel: ").append(summary.model)
                .append("\nCost: ").append(CostEstimator.money(summary.spentUsd)).append(" / ").append(CostEstimator.money(summary.budgetUsd)).append("\n\n");
        for(TranslationRepository.BenchmarkResultRow r:rows){blind.append("CHUNK ").append(r.chunkIndex+1).append(" • OUTPUT ").append(r.blindLabel)
                .append("\nTokens in/out: ").append(r.inputTokens).append('/').append(r.outputTokens).append(" • cached ").append(r.cachedTokens)
                .append(" • ").append(r.durationMs).append("ms • ").append(CostEstimator.money(r.costUsd)).append(" [").append(r.costStatus).append("]")
                .append("\nUsage: ").append(r.usageSource).append("\nQuality warnings: ").append(nonEmpty(r.qualityIssues,"none"))
                .append("\n\n").append(r.output).append("\n\n----------------\n\n");}
        new AlertDialog.Builder(a).setTitle("Blind A/B review").setMessage("Review X/Y before revealing the pipeline mapping.")
                .setPositiveButton("Open outputs",(d,w)->showTextDialog("Blind A/B outputs",blind.toString()))
                .setNeutralButton("Reveal mapping",(d,w)->showBenchmarkMapping(summary.id,rows)).setNegativeButton("Close",null).show();
    }

    private void showBenchmarkMapping(long sessionId,List<TranslationRepository.BenchmarkResultRow> rows){StringBuilder b=new StringBuilder("Benchmark #"+sessionId+" mapping\n\n");for(TranslationRepository.BenchmarkResultRow r:rows)b.append("Chunk ").append(r.chunkIndex+1).append(" • ").append(r.blindLabel).append(" = ").append(r.pipeline).append('\n');showTextDialog("A/B mapping",b.toString());}

    private void askChunkAndShowPrompt(List<Chunk> chunks, AppSettings s, String label) {
        final EditText e = number("Chunk index", "1");
        int pad = dp(20);
        FrameLayout box = new FrameLayout(a);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(e, new FrameLayout.LayoutParams(-1, dp(54)));
        new AlertDialog.Builder(a)
                .setTitle("Prompt preview • chọn chunk 1-" + chunks.size())
                .setView(box)
                .setPositiveButton("Preview", (d, w) -> {
                    int idx = intVal(e, 1) - 1;
                    if (idx < 0) idx = 0;
                    if (idx >= chunks.size()) idx = chunks.size() - 1;
                    showPromptForChunk(chunks.get(idx), idx + 1, chunks.size(), s, label);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPromptForChunk(Chunk chunk, int oneBased, int total, AppSettings s, String label) {
        try {
            PromptPair p = PromptBuilder.translationPrompt(chunk, "", s);
            String matched = PromptContextBuilder.preview(s.glossaryText, s.pronounText, chunk.mainContent, s);
            String body = "[SOURCE]\n" + label + "\n\n[CHUNK]\n" + oneBased + "/" + total
                    + "\n\n[MATCHED GLOSSARY / PRONOUN RULES]\n" + matched
                    + "\n\n[SYSTEM]\n" + p.system + "\n\n[USER]\n" + p.user;
            showTextDialog("Prompt preview • chunk " + oneBased + "/" + total, body);
            log.append("Prompt preview: " + label + " • chunk " + oneBased + "/" + total);
        } catch (Exception e) {
            toast.show("Không tạo được prompt preview");
            log.append("Prompt preview chunk lỗi: " + e.getMessage());
        }
    }

    private void confirmDeleteJob(TranslationRepository.JobSummary j) {
        if (j == null) return;
        if (TranslatorService.isActive() && "running".equals(j.status)) {
            toast.show("Không xóa job đang chạy. Hãy Cancel/Pause trước.");
            return;
        }
        new AlertDialog.Builder(a)
                .setTitle("Delete checkpoint #" + j.id + "?")
                .setMessage("Xóa job khỏi SQLite. Không xóa file output đã xuất.\n\nFile: " + nonEmpty(j.fileName, "unknown") + "\nStatus: " + nonEmpty(j.status, "unknown"))
                .setPositiveButton("Delete", (d, w) -> {
                    try {
                        new JobStore(a).delete(j.id);
                        toast.show("Đã xóa checkpoint #" + j.id);
                        log.append("Đã xóa checkpoint/job #" + j.id);
                        onRefresh.run();
                    } catch (Exception e) { toast.show("Xóa checkpoint lỗi"); log.append("Xóa checkpoint lỗi: " + e.getMessage()); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTextDialog(String title, String body) {
        TextView tv = text(body == null ? "" : body, 11, TEXT, false);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        tv.setSingleLine(false);
        ScrollView sv = new ScrollView(a);
        sv.setPadding(dp(10), dp(10), dp(10), dp(10));
        sv.addView(tv);
        new AlertDialog.Builder(a).setTitle(title).setView(sv).setPositiveButton("OK", null).show();
    }

    private int jobStatusColor(String status) {
        if ("done".equals(status)) return GREEN;
        if ("error".equals(status)) return RED;
        if ("paused".equals(status)) return AMBER;
        if ("cancelled".equals(status)) return MUTED;
        return BLUE;
    }

    private String jobStatusIcon(String status) {
        if ("done".equals(status)) return "✅";
        if ("error".equals(status)) return "⚠";
        if ("paused".equals(status)) return "⏸";
        if ("cancelled".equals(status)) return "✕";
        return "▶";
    }

    private String dateTime(long t) { return t <= 0 ? "—" : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(t)); }
    private String nonEmpty(String s, String def) { return s == null || s.trim().isEmpty() ? def : s.trim(); }
    private String preview(String s, int max) { if (s == null || s.trim().isEmpty()) return "Trống"; return s.length() <= max ? s : s.substring(0, max) + "\n..."; }
    private String previewUri(String s, int max) { if (s == null || s.trim().isEmpty()) return "—"; return s.length() <= max ? s : "..." + s.substring(Math.max(0, s.length() - max)); }
    private String tail(String s, int max) { if (s == null) return ""; return s.length() <= max ? s : s.substring(s.length() - max); }
    private int intVal(EditText e, int def) { try { return Integer.parseInt(e.getText().toString().trim()); } catch (Exception ex) { return def; } }

    private ScrollView scroll() { ScrollView s = new ScrollView(a); s.setFillViewport(false); return s; }
    private LinearLayout pageRoot() { LinearLayout r = new LinearLayout(a); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(0, dp(10), 0, dp(24)); return r; }
    private LinearLayout rowContainer() { LinearLayout r = new LinearLayout(a); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    private View space(int w, int h) { View v = new View(a); v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), dp(h))); return v; }

    private LinearLayout sectionCard(String icon, String title) {
        LinearLayout c = card(18, PANEL, BORDER);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout head = rowContainer();
        head.addView(text(icon, 20, BLUE, false));
        head.addView(space(10, 1));
        head.addView(text(title, 18, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(head, marginLP(-1, -2, 0, 0, 0, 14));
        c.setLayoutParams(marginLP(-1, -2, 0, 0, 0, 14));
        return c;
    }

    private LinearLayout card(int radius, int fill, int stroke) {
        LinearLayout v = new LinearLayout(a);
        tint(v, fill, stroke, 1, radius);
        return v;
    }

    private Button tinyButton(String s, int color, View.OnClickListener l) {
        Button b = button(s, FIELD, TEXT, l);
        b.setTextColor(color);
        b.setTextSize(10);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        return b;
    }

    private Button primaryButton(String s, View.OnClickListener l) { return button(s, BLUE, TEXT, l); }
    private Button secondaryButton(String s, View.OnClickListener l) { return button(s, FIELD, TEXT, l); }
    private Button dangerButton(String s, View.OnClickListener l) { return button(s, RED, Color.WHITE, l); }

    private Button button(String s, int fill, int txt, View.OnClickListener l) {
        Button b = new Button(a);
        b.setText(s);
        b.setTextColor(txt);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        tint(b, fill, fill == FIELD ? BORDER : fill, 1, 12);
        return b;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(a);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(true);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private EditText input(String hint, String value) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(14);
        e.setSingleLine(false);
        e.setMinLines(1);
        e.setMaxLines(3);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        e.setPadding(dp(12), 0, dp(12), 0);
        tint(e, FIELD, BORDER, 1, 12);
        return e;
    }

    private EditText number(String hint, String value) { EditText e = input(hint, value); e.setInputType(InputType.TYPE_CLASS_NUMBER); return e; }

    private void tint(View v, int fill, int stroke, int strokeWidth, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(strokeWidth), stroke);
        v.setBackground(g);
    }

    private LinearLayout.LayoutParams marginLP(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private int dp(int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
