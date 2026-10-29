package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.DesktopPromptPreview;
import com.ml.tblandroidtxt.DesktopRawStore;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.GlossaryStoreCore;
import com.ml.tblandroidtxt.PronounStoreCore;
import com.ml.tblandroidtxt.RunSnapshot;
import com.ml.tblandroidtxt.TranslationRepositoryCore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * One real chapter from the owner's library, headless, with the owner's own saved settings (only the cost cap is set here). Dry by default.
 * Prints numbers only: never text, never the key. It refuses to call the provider unless the key shape, the Glossary and Pronoun of the
 * chapter (entries > 0, first chunk matches > 0) and a priced estimate within the cap all check out.
 *
 * <pre>
 *   --list                         names of the RAW / Glossary / Pronoun in the library and the stored model settings
 *   --raw NAME --glossary NAME --pronoun NAME --output-dir DIR --cap USD [--run]
 * </pre>
 */
final class DesktopRealChapter {
    private DesktopRealChapter() { }

    static void run(String[] args) throws Exception {
        String rawName = "", glossaryName = "", pronounName = "";
        Path outDir = Paths.get("D:\\P5E-builds\\r0-real-output");
        double cap = 0.03;
        boolean run = false, list = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--real-chapter": break;
                case "--list": list = true; break;
                case "--run": run = true; break;
                case "--raw": rawName = args[++i]; break;
                case "--glossary": glossaryName = args[++i]; break;
                case "--pronoun": pronounName = args[++i]; break;
                case "--output-dir": outDir = Paths.get(args[++i]); break;
                case "--cap": cap = Double.parseDouble(args[++i]); break;
                default: System.out.println("unknown argument " + args[i]); System.exit(64);
            }
        }
        final RunSnapshot[] last = new RunSnapshot[1];
        DesktopRuntime.UiEvents events = new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) { last[0] = s; }
            @Override public void onNotification(String text, int done, int total) { }
            @Override public void onRunEnded() { }
            @Override public void onCancelledEnd(String message) { }
        };
        try (DesktopRuntime rt = DesktopRuntime.open(DesktopRuntime.defaultDataDir(), events)) {
            AppSettings s = rt.loadSettings();
            System.out.println("settings: model=" + s.model + " effort=" + s.reasoningEffort + " preset=" + s.optimizationPreset + " maxTokensPerChunk=" + s.maxTokensPerChunk
                    + " contextChars=" + s.contextChars + " maxOutputTokens=" + s.maxOutputTokens + " bilingual=" + s.bilingualOutput);
            if (list) {
                for (DesktopRawStore.Item i : rt.raws().list()) System.out.println("raw: " + i.originalName + " chars=" + i.chars);
                for (GlossaryStoreCore.Glossary g : rt.glossaries()) System.out.println("glossary: " + g.name + " entries=" + g.count() + (g.id.equals(rt.selectedGlossaryId()) ? " [selected]" : ""));
                for (PronounStoreCore.Profile p : rt.pronouns()) System.out.println("pronoun: " + p.name + " rules=" + p.count() + (p.id.equals(rt.selectedPronounId()) ? " [selected]" : ""));
                return;
            }
            boolean shapeOk = rt.secrets().savedKeyHasOpenRouterShape();
            System.out.println("key: length=" + rt.secrets().savedKeyLength() + " openrouterShape=" + shapeOk);
            DesktopRawStore.Item item = null;
            for (DesktopRawStore.Item i : rt.raws().list()) if (i.originalName.equals(rawName)) item = i;
            GlossaryStoreCore.Glossary g = null;
            for (GlossaryStoreCore.Glossary x : rt.glossaries()) if (x.name.equals(glossaryName)) g = x;
            PronounStoreCore.Profile p = null;
            for (PronounStoreCore.Profile x : rt.pronouns()) if (x.name.equals(pronounName)) p = x;
            if (item == null || g == null || p == null) { System.out.println("STOP: raw/glossary/pronoun name not found; no provider call was made"); System.exit(5); }
            rt.selectGlossary(g.id);
            rt.selectPronoun(p.id);
            System.out.println("chapter " + item.chapterNo + ": chars=" + item.chars + " glossary entries=" + g.count() + " pronoun rules=" + p.count());

            Files.createDirectories(outDir);
            Path src = Files.createDirectories(outDir.resolve("_source")).resolve(item.originalName);
            Files.write(src, rt.raws().text(item.id).getBytes(StandardCharsets.UTF_8));
            DesktopPromptPreview.Result pr = rt.previewPrompt(src, s, 0, "");
            System.out.println("prompt chunk 1/" + pr.chunkCount + ": matchedGlossary=" + pr.glossaryLines.size() + " matchedPronoun=" + pr.pronounLines.size() + " inputTokens=" + pr.inputTokens);
            CostEstimator.Estimate e = rt.estimate(src, s);
            System.out.println("estimate: chunks=" + e.chunks + " tokensHigh=" + e.totalTokensHigh + " pricingAvailable=" + e.pricingAvailable
                    + " costHigh=" + (Double.isNaN(e.costHigh) ? "NaN" : String.format(Locale.ROOT, "%.4f", e.costHigh)) + " cap=" + cap);
            if (!run) { System.out.println("dry run: no provider call was made"); return; }
            if (!shapeOk) { System.out.println("STOP: key shape invalid; no provider call was made"); System.exit(2); }
            if (g.count() == 0 || p.count() == 0 || pr.glossaryLines.isEmpty() || pr.pronounLines.isEmpty()) {
                System.out.println("STOP: empty Glossary/Pronoun or first chunk matches nothing; no provider call was made"); System.exit(6);
            }
            if (!e.pricingAvailable || Double.isNaN(e.costHigh) || e.costHigh > cap) {
                System.out.println("STOP: estimate is not within the cap (or has no price); no provider call was made"); System.exit(3);
            }
            Path out = outDir.resolve(s.buildOutputName(item.originalName));
            long start = System.currentTimeMillis();
            if (!rt.startNew(src, out, s, cap, e)) { System.out.println("STOP: could not start"); System.exit(4); }
            Thread.sleep(500);
            while (rt.isRunning()) Thread.sleep(200);
            long wall = System.currentTimeMillis() - start;
            RunSnapshot f = last[0];
            System.out.println("run: status=" + (f == null ? "?" : f.status) + " wallMs=" + wall);
            TranslationRepositoryCore.JobSummary job = null;
            for (TranslationRepositoryCore.JobSummary j : rt.jobs()) if (j.fileName.equals(item.originalName)) { job = j; break; }
            if (job != null) {
                double c = DesktopRuntime.jobCost(job);
                System.out.println("job #" + job.id + ": status=" + job.status + " chunks " + job.doneChunks + "/" + job.totalChunks + " failed=" + job.failedChunks
                        + " calls=" + DesktopRuntime.jobCalls(job) + " finish=" + rt.finishReasonCounts(job.id) + " costUsd=" + (Double.isNaN(c) ? "unknown" : String.format(Locale.ROOT, "%.6f", c))
                        + " withinCap=" + (!Double.isNaN(c) && c <= cap));
            }
            if (f != null) System.out.println("tokens: sessionTokens=" + f.sessionTokens + " sessionCostUsd=" + String.format(Locale.ROOT, "%.6f", f.sessionCost));
            reportOutput(rt, item, g, out);
        } finally {
            Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir")).resolve("tbl-real-chapter.marker"));
        }
    }

    private static int count(String text, String part) {
        if (part.isEmpty()) return 0;
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }

    /** Numbers only: output size, kana left, and per Glossary term that occurs in the raw text: source left / exact target / translated another way. */
    private static void reportOutput(DesktopRuntime rt, DesktopRawStore.Item item, GlossaryStoreCore.Glossary g, Path out) throws Exception {
        System.out.println("output exists=" + Files.exists(out) + " bytes=" + (Files.exists(out) ? Files.size(out) : 0));
        if (!Files.exists(out)) return;
        String text = new String(Files.readAllBytes(out), StandardCharsets.UTF_8);
        String raw = rt.raws().text(item.id);
        int kana = 0;
        for (int i = 0; i < text.length(); i++) { char ch = text.charAt(i); if (ch >= 0x3040 && ch <= 0x30FF) kana++; }
        System.out.println("kanaChars=" + kana + " rubyLike=" + (text.length() - text.replaceAll("[（(][ぁ-んァ-ン]+[）)]", "").length()));
        int terms = 0, exactAll = 0, leftAll = 0, otherAll = 0, usedTerms = 0;
        List<GlossaryStoreCore.Term> all = g.terms;
        for (GlossaryStoreCore.Term t : all) {
            int inRaw = count(raw, t.source);
            if (inRaw == 0) continue;
            terms++;
            int left = count(text, t.source), exact = count(text, t.target);
            int other = Math.max(0, inRaw - left - exact);
            exactAll += Math.min(exact, inRaw); leftAll += left; otherAll += other;
            if (exact > 0) usedTerms++;
            System.out.println("  term " + terms + " (" + t.category + "): inRaw=" + inRaw + " sourceLeft=" + left + " targetExact=" + exact + " translatedOtherWay=" + other);
        }
        System.out.println("glossary terms present in this chapter=" + terms + " followed(at least once)=" + usedTerms + " | occurrences: exactTarget=" + exactAll + " sourceLeft=" + leftAll + " otherWay=" + otherAll);
    }
}
