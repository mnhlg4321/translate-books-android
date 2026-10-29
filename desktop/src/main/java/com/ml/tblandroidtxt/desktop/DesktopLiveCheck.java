package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.RunSnapshot;
import com.ml.tblandroidtxt.TranslationRepositoryCore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Headless check of the real desktop path (same DesktopRuntime, TranslationRunner and DPAPI key as the window). It prints
 * numbers and statuses only: never the key and never text.
 *
 * <pre>
 *   --check-key            only test the saved key's shape (length and pattern) and exit
 *   --volume [--run] ...   the whole-volume path (see DesktopLiveVolume): dry by default, one capped run with --run
 *   --run                  estimate, then (only when the key shape and the estimate pass) one real run with one pause/resume
 *   --input FILE  --output-dir DIR  --cap USD  --pause-seconds N   overrides (defaults below)
 * </pre>
 */
public final class DesktopLiveCheck {
    private DesktopLiveCheck() {}

    public static void main(String[] args) throws Exception {
        for (String arg : args) if (arg.equals("--volume")) { DesktopLiveVolume.run(args); return; }
        boolean run = false;
        boolean checkKey = false;
        Path input = Paths.get("D:\\P5E-builds\\qa-desktop\\in\\QA_raw.txt");
        Path outDir = Paths.get("D:\\P5E-builds\\qa-desktop\\in\\out");
        double cap = 0.03;
        int pauseSeconds = 10;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--run": run = true; break;
                case "--check-key": checkKey = true; break;
                case "--input": input = Paths.get(args[++i]); break;
                case "--output-dir": outDir = Paths.get(args[++i]); break;
                case "--cap": cap = Double.parseDouble(args[++i]); break;
                case "--pause-seconds": pauseSeconds = Integer.parseInt(args[++i]); break;
                default: System.out.println("unknown argument " + args[i]); System.exit(64);
            }
        }
        Path dataDir = DesktopRuntime.defaultDataDir();
        final int pauseMs = pauseSeconds * 1000;
        final AtomicBoolean pausedOnce = new AtomicBoolean(false);
        final RunSnapshot[] last = new RunSnapshot[1];
        final long[] pauseWallMs = new long[1];
        final DesktopRuntime[] holder = new DesktopRuntime[1];
        DesktopRuntime.UiEvents events = new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) {
                last[0] = s;
                DesktopRuntime rt = holder[0];
                if (rt != null && s.completed >= 1 && s.completed < s.totalChunks && pausedOnce.compareAndSet(false, true)) {
                    pauseWallMs[0] = System.currentTimeMillis();
                    rt.pause();
                    Thread t = new Thread(() -> {
                        try { Thread.sleep(pauseMs); } catch (InterruptedException ignored) { }
                        pauseWallMs[0] = System.currentTimeMillis() - pauseWallMs[0];
                        rt.resume();
                    }, "live-check-resume");
                    t.setDaemon(true);
                    t.start();
                }
            }
            @Override public void onNotification(String text, int done, int total) { }
            @Override public void onRunEnded() { }
            @Override public void onCancelledEnd(String message) { }
        };
        DesktopRuntime rt = DesktopRuntime.open(dataDir, events);
        holder[0] = rt;
        try {
            int length = rt.secrets().savedKeyLength();
            boolean shapeOk = rt.secrets().savedKeyHasOpenRouterShape();
            System.out.println("key: length=" + length + " openrouterShape=" + shapeOk);
            if (checkKey) return;
            if (!shapeOk) {
                System.out.println("STOP: key still invalid (length " + length + "); no provider call was made");
                System.exit(2);
            }
            AppSettings s = rt.loadSettings();
            s.model = "openai/gpt-5.6-luna";
            s.reasoningEffort = "medium";
            s.maxTokensPerChunk = 1700;
            s.contextChars = 600;
            s.stopOnCostLimit = true;
            s.costLimitUsd = cap;
            CostEstimator.Estimate e = rt.estimate(input, s);
            System.out.println("estimate: chunks=" + e.chunks + " tokensHigh=" + e.totalTokensHigh + " pricingAvailable=" + e.pricingAvailable
                    + " costHigh=" + (Double.isNaN(e.costHigh) ? "NaN" : String.format(java.util.Locale.ROOT, "%.4f", e.costHigh)) + " cap=" + cap);
            if (!run) return;
            if (!e.pricingAvailable || Double.isNaN(e.costHigh) || e.costHigh > cap) {
                System.out.println("STOP: estimate is not within the cap (or has no price); no provider call was made");
                System.exit(3);
            }
            Files.createDirectories(outDir);
            Path out = outDir.resolve(s.buildOutputName(input.getFileName().toString()));
            long start = System.currentTimeMillis();
            if (!rt.startNew(input, out, s, cap, e)) {
                System.out.println("STOP: a run is already active");
                System.exit(4);
            }
            Thread.sleep(500);
            while (rt.isRunning()) Thread.sleep(200);
            long wall = System.currentTimeMillis() - start;
            List<TranslationRepositoryCore.JobSummary> jobs = rt.jobs();
            TranslationRepositoryCore.JobSummary job = jobs.isEmpty() ? null : jobs.get(0);
            RunSnapshot f = last[0];
            System.out.println("result: status=" + (f == null ? "?" : f.status) + " jobStatus=" + (job == null ? "?" : job.status));
            if (job != null) {
                System.out.println("chunks: total=" + job.totalChunks + " done=" + job.doneChunks + " failed=" + job.failedChunks + " statuses=" + rt.chunkStatusCounts(job.id));
                System.out.println("calls: " + DesktopRuntime.jobCalls(job) + " finishReasons=" + rt.finishReasonCounts(job.id));
                double cost = DesktopRuntime.jobCost(job);
                System.out.println("providerCostUsd: " + (Double.isNaN(cost) ? "unknown" : String.format(java.util.Locale.ROOT, "%.6f", cost)));
            }
            if (f != null) {
                System.out.println("tokens: " + f.totalTokens + " sessionTokens=" + f.sessionTokens + " sessionCostUsd=" + String.format(java.util.Locale.ROOT, "%.6f", f.sessionCost));
                System.out.println("clock: elapsedMs=" + f.elapsedMs + " pausedTotalMs=" + f.pausedTotalMs + " wallMs=" + wall + " pauseWallMs=" + pauseWallMs[0]
                        + " elapsedExcludesPause=" + (f.elapsedMs <= wall - f.pausedTotalMs + 1000));
            }
            System.out.println("pauseDone=" + pausedOnce.get() + " outputExists=" + Files.exists(out) + " outputBytes=" + (Files.exists(out) ? Files.size(out) : 0));
        } finally {
            rt.close();
        }
    }
}
