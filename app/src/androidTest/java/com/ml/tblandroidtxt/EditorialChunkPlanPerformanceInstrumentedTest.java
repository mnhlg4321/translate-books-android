package com.ml.tblandroidtxt;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Plans one long chapter (the owner's 1,222-line settings chapter) on the device with the app's own file reader, Chunker and
 * Performance settings and records only counts and timings. Opt-in: the three files are staged by the harness under
 * {@code files/cs1/<name>/} (RAW.txt, DRAFT.txt, GLOSSARY.csv, original encodings) and {@code cs1_perf=YES} is passed.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialChunkPlanPerformanceInstrumentedTest {
    @Test public void aLongChapterIsPlannedInUnderOneSecond() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("opt-in", "YES".equalsIgnoreCase(args.getString("cs1_perf", "")));
        String name = args.getString("cs1_name", "");
        assertTrue(name.matches("[A-Za-z0-9._-]{1,40}"));
        Context context = ApplicationProvider.getApplicationContext();
        File dir = new File(context.getFilesDir(), "cs1/" + name);
        String raw = FileUtil.readText(context, Uri.fromFile(new File(dir, "RAW.txt")));
        String draft = FileUtil.readText(context, Uri.fromFile(new File(dir, "DRAFT.txt")));
        String glossary = FileUtil.readText(context, Uri.fromFile(new File(dir, "GLOSSARY.csv")));
        java.util.List<com.ml.tblandroidtxt.editorial.api.EditInputs.GlossaryEntry> entries = new java.util.ArrayList<>();
        for (GlossaryStore.Term term : GlossaryStore.parseTerms("GLOSSARY.csv", glossary)) {
            entries.add(new com.ml.tblandroidtxt.editorial.api.EditInputs.GlossaryEntry(term.source, term.target, term.category, term.note));
        }
        String glossaryText = EditorialApiSources.glossaryAsText(entries);
        AppSettings settings = SettingsStore.load(context).copy();
        EditorialPairSourceLoader.References refs = new EditorialPairSourceLoader.References(glossaryText, "g", "", "");
        long best = Long.MAX_VALUE;
        EditorialPairSourceLoader.FilesLoad load = null;
        for (int i = 0; i < 3; i++) {
            long start = System.nanoTime();
            load = EditorialPairSourceLoader.planFiles(name, raw, draft, refs, settings);
            best = Math.min(best, (System.nanoTime() - start) / 1_000_000L);
        }
        assertEquals("the chunks rebuild both files byte for byte", raw, String.join("", com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlanner.plan(
                raw, draft, EditorialPairSnapshot.glossaryFrom(glossaryText), glossaryText, "", EditorialPairSourceLoader.plannerSettings(settings)).rawRows()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", name);
        out.put("rawUnits", BigDecimal.valueOf(load.plan.rawUnits));
        out.put("draftUnits", BigDecimal.valueOf(load.plan.draftUnits));
        out.put("chunks", BigDecimal.valueOf(load.plan.chunks.size()));
        out.put("verdict", load.plan.verdict);
        out.put("bestOfThreeMillis", BigDecimal.valueOf(best));
        out.put("softUsed", BigDecimal.valueOf(load.plan.limits.soft()));
        out.put("hardUsed", BigDecimal.valueOf(load.plan.limits.hard()));
        File dest = new File(context.getExternalFilesDir(null), "cs1-perf-" + name + ".json");
        Files.write(dest.toPath(), EditorialCanonicalJson.canonicalize(out).getBytes(StandardCharsets.UTF_8));
        assertTrue("planning " + load.plan.rawUnits + " lines took " + best + " ms", best < 1000);
    }
}
