package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.DesktopHistory;
import com.ml.tblandroidtxt.DesktopPromptPreview;
import com.ml.tblandroidtxt.DesktopRawStore;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.DesktopSetEditor;
import com.ml.tblandroidtxt.RunSnapshot;
import com.ml.tblandroidtxt.TranslationRepositoryCore;
import com.ml.tblandroidtxt.VolumePlan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code DesktopLiveCheck --volume}: the whole-volume path of the real app (saved set, VolumePlan, startVolume with one shared cost cap),
 * headless. Without {@code --run} it only imports the QA_ files, builds the set and prints the estimate: no provider call. With {@code --run}
 * it stops before any call unless the key shape is right, the plan has no red row, every chapter has exactly the expected number of chunks
 * and the estimate is priced and within the cap. It pauses once after the first chunk of chapter 1 and resumes after N seconds.
 * Prints numbers and statuses only: never the key, never text.
 */
final class DesktopLiveVolume {
    private DesktopLiveVolume() { }

    static void run(String[] args) throws Exception {
        boolean run = false;
        Path rawDir = Paths.get("D:\\P5E-builds\\qa-desktop\\in\\vol\\raw");
        Path glossaryDir = Paths.get("D:\\P5E-builds\\qa-desktop\\in\\vol\\glossary");
        Path pronounDir = Paths.get("D:\\P5E-builds\\qa-desktop\\in\\vol\\pronoun");
        Path outDir = Paths.get("D:\\P5E-builds\\qa-desktop\\in\\out-volume");
        double cap = 0.05;
        int pauseSeconds = 10;
        int chunkTokens = 1700;
        int expectedChunks = 2;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--volume": break;
                case "--run": run = true; break;
                case "--raw-dir": rawDir = Paths.get(args[++i]); break;
                case "--glossary-dir": glossaryDir = Paths.get(args[++i]); break;
                case "--pronoun-dir": pronounDir = Paths.get(args[++i]); break;
                case "--output-dir": outDir = Paths.get(args[++i]); break;
                case "--cap": cap = Double.parseDouble(args[++i]); break;
                case "--pause-seconds": pauseSeconds = Integer.parseInt(args[++i]); break;
                case "--chunk-tokens": chunkTokens = Integer.parseInt(args[++i]); break;
                case "--chunks-per-chapter": expectedChunks = Integer.parseInt(args[++i]); break;
                default: System.out.println("unknown argument " + args[i]); System.exit(64);
            }
        }
        final int pauseMs = pauseSeconds * 1000;
        final AtomicBoolean pausedOnce = new AtomicBoolean(false);
        final RunSnapshot[] last = new RunSnapshot[1];
        final long[] pauseWallMs = new long[1];
        final DesktopRuntime[] holder = new DesktopRuntime[1];
        DesktopRuntime.UiEvents events = new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) {
                last[0] = s;
                DesktopRuntime rt = holder[0];
                // first chunk of the first chapter is done and that chapter has more: pause once
                if (rt != null && s.setIndex <= 1 && s.completed >= 1 && s.completed < s.totalChunks && pausedOnce.compareAndSet(false, true)) {
                    pauseWallMs[0] = System.currentTimeMillis();
                    rt.pause();
                    Thread t = new Thread(() -> {
                        try { Thread.sleep(pauseMs); } catch (InterruptedException ignored) { }
                        pauseWallMs[0] = System.currentTimeMillis() - pauseWallMs[0];
                        rt.resume();
                    }, "live-volume-resume");
                    t.setDaemon(true);
                    t.start();
                }
            }
            @Override public void onNotification(String text, int done, int total) { }
            @Override public void onRunEnded() { }
            @Override public void onCancelledEnd(String message) { }
        };
        DesktopRuntime rt = DesktopRuntime.open(DesktopRuntime.defaultDataDir(), events);
        holder[0] = rt;
        try {
            int length = rt.secrets().savedKeyLength();
            boolean shapeOk = rt.secrets().savedKeyHasOpenRouterShape();
            System.out.println("key: length=" + length + " openrouterShape=" + shapeOk);

            // the QA_ library (duplicates are skipped, so a second pass adds nothing)
            DesktopRuntime.FolderImport r1 = rt.importRawFolder(rawDir);
            DesktopRuntime.FolderImport r2 = rt.importGlossaryFolder(glossaryDir);
            DesktopRuntime.FolderImport r3 = rt.importPronounFolder(pronounDir);
            System.out.println("import: raw added=" + r1.added + " skipped=" + r1.skipped + " | glossary added=" + r2.added + " skipped=" + r2.skipped
                    + " | pronoun added=" + r3.added + " skipped=" + r3.skipped);

            Set<String> rawNames = new HashSet<>();
            try (java.util.stream.Stream<Path> s = Files.list(rawDir)) { s.forEach(p -> rawNames.add(p.getFileName().toString())); }
            List<String> rawIds = new ArrayList<>();
            for (DesktopRawStore.Item item : rt.raws().list()) if (rawNames.contains(item.originalName)) rawIds.add(item.id);
            Set<String> gNames = new HashSet<>(), pNames = new HashSet<>();
            try (java.util.stream.Stream<Path> s = Files.list(glossaryDir)) { s.forEach(p -> gNames.add(DesktopRawStore.stripExtension(p.getFileName().toString()))); }
            try (java.util.stream.Stream<Path> s = Files.list(pronounDir)) { s.forEach(p -> pNames.add(DesktopRawStore.stripExtension(p.getFileName().toString()))); }

            // a saved set, like the screen: reused when it already exists
            String setName = "QA_VOL_tap";
            String setId = "";
            for (DesktopRuntime.SetCard c : rt.setCards()) if (c.name.equals(setName)) setId = c.id;
            if (setId.isEmpty()) {
                DesktopSetEditor ed = rt.newSet(rawIds);
                List<String> gl = new ArrayList<>(), pr = new ArrayList<>();
                for (DesktopSetEditor.Choice c : ed.glossaryChoices()) if (gNames.contains(c.label)) gl.add(c.id);
                for (DesktopSetEditor.Choice c : ed.pronounChoices()) if (pNames.contains(c.label)) pr.add(c.id);
                ed.setGlossarySlot(gl);
                ed.setPronounSlot(pr);
                ed.setName(setName);
                setId = ed.save();
                if (setId == null) { System.out.println("STOP: the set cannot be saved: " + ed.blockReason()); System.exit(5); }
            }
            VolumePlan plan = rt.volumePlanFromSet(setId);
            System.out.println("set: id=" + setId + " chapters=" + plan.included() + " green=" + plan.green() + " yellow=" + plan.yellow() + " red=" + plan.red());

            AppSettings s = rt.loadSettings();
            s.model = "openai/gpt-5.6-luna";
            s.reasoningEffort = "medium";
            s.maxTokensPerChunk = chunkTokens;
            s.contextChars = 600;
            s.stopOnCostLimit = true;
            s.costLimitUsd = cap;
            rt.saveSettings(s);
            boolean chunksOk = true;
            AppSettings sc = rt.normalizedForEstimate(s);
            for (DesktopRawStore.Item item : rt.raws().list()) {
                if (!rawIds.contains(item.id)) continue;
                int n = DesktopPromptPreview.chunkCount(rt.raws().text(item.id), sc);
                System.out.println("chapter " + item.chapterNo + ": chars=" + item.chars + " chunks=" + n);
                if (n != expectedChunks) chunksOk = false;
            }
            CostEstimator.Estimate e = rt.estimateVolume(plan, s);
            System.out.println("estimate: chunks=" + e.chunks + " tokensHigh=" + e.totalTokensHigh + " pricingAvailable=" + e.pricingAvailable
                    + " costHigh=" + (Double.isNaN(e.costHigh) ? "NaN" : String.format(Locale.ROOT, "%.4f", e.costHigh)) + " cap=" + cap);
            if (!run) { System.out.println("dry run: no provider call was made"); return; }
            if (!shapeOk) { System.out.println("STOP: key shape invalid; no provider call was made"); System.exit(2); }
            if (plan.hasRed() || plan.included() == 0) { System.out.println("STOP: the plan has a red row; no provider call was made"); System.exit(3); }
            if (!chunksOk) { System.out.println("STOP: a chapter does not have exactly " + expectedChunks + " chunks; no provider call was made"); System.exit(3); }
            if (!e.pricingAvailable || Double.isNaN(e.costHigh) || e.costHigh > cap) {
                System.out.println("STOP: estimate is not within the cap (or has no price); no provider call was made");
                System.exit(3);
            }
            Files.createDirectories(outDir);
            long start = System.currentTimeMillis();
            if (!rt.startVolume(plan, s, cap, e, outDir)) { System.out.println("STOP: could not start"); System.exit(4); }
            Thread.sleep(500);
            while (rt.isRunning()) Thread.sleep(200);
            long wall = System.currentTimeMillis() - start;

            RunSnapshot f = last[0];
            System.out.println("run: status=" + (f == null ? "?" : f.status) + " pauseDone=" + pausedOnce.get());
            double sum = 0;
            int calls = 0;
            List<TranslationRepositoryCore.JobSummary> mine = new ArrayList<>();
            for (TranslationRepositoryCore.JobSummary j : rt.jobs()) if (rawNames.contains(j.fileName)) mine.add(j);
            java.util.Collections.reverse(mine);
            for (TranslationRepositoryCore.JobSummary j : mine) {
                double c = DesktopRuntime.jobCost(j);
                if (!Double.isNaN(c)) sum += c;
                calls += DesktopRuntime.jobCalls(j);
                System.out.println("chapter job #" + j.id + ": status=" + j.status + " chunks " + j.doneChunks + "/" + j.totalChunks + " failed=" + j.failedChunks
                        + " calls=" + DesktopRuntime.jobCalls(j) + " finish=" + rt.finishReasonCounts(j.id) + " costUsd=" + (Double.isNaN(c) ? "unknown" : String.format(Locale.ROOT, "%.6f", c)));
            }
            System.out.println("total: calls=" + calls + " dbCostUsd=" + String.format(Locale.ROOT, "%.6f", sum) + " withinCap=" + (sum <= cap));
            if (f != null) {
                System.out.println("tokens: sessionTokens=" + f.sessionTokens + " sessionCostUsd=" + String.format(Locale.ROOT, "%.6f", f.sessionCost));
                System.out.println("clock: elapsedMs=" + f.elapsedMs + " pausedTotalMs=" + f.pausedTotalMs + " wallMs=" + wall + " pauseWallMs=" + pauseWallMs[0]
                        + " elapsedExcludesPause=" + (f.elapsedMs <= wall - f.pausedTotalMs + 1000));
            }
            // offline checks
            DesktopHistory h = rt.history();
            System.out.println("history: jobsOfSet=" + h.jobIdsOfSet(setId).size() + " runEntries=" + h.page(-1, DesktopHistory.ALL, new HashSet<String>(), "", setId).entries.size());
            try (java.util.stream.Stream<Path> outs = Files.list(outDir)) {
                for (Path o : (Iterable<Path>) outs.sorted()::iterator) {
                    String text = new String(Files.readAllBytes(o), StandardCharsets.UTF_8);
                    int kana = 0;
                    for (int i = 0; i < text.length(); i++) { char ch = text.charAt(i); if ((ch >= 0x3040 && ch <= 0x30FF)) kana++; }
                    System.out.println("output " + o.getFileName() + ": bytes=" + Files.size(o) + " kanaChars=" + kana
                            + " rubyLike=" + (text.length() - text.replaceAll("[（(][ぁ-んァ-ン]+[）)]", "").length()));
                }
            }
        } finally {
            rt.close();
        }
    }
}
