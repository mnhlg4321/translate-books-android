package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Opt-in (SC_ROOT = the q2-inputs folder with roles.json, SC_CORPUS_OUT = a private json file): for each chapter plans the real
 * chunks with the app's default settings, builds every chunk request with a fake provider and records only the ADDRESS CHECK
 * lines (labels and words, no book text). 0 USD. Used to see how often the checklist appears and how noisy it is.
 */
public final class EditorialAddressCheckCorpusTest {
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.0000002"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private static String read(File f) throws Exception {
        String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    @Test public void listAddressChecksOfEveryChapter() throws Exception {
        String root = System.getenv("SC_ROOT");
        Assume.assumeTrue("set SC_ROOT and SC_CORPUS_OUT", root != null && System.getenv("SC_CORPUS_OUT") != null);
        Map<String, Object> roles = EditorialCanonicalJson.parseObject(Files.readAllBytes(new File(root, "roles.json").toPath()));
        Map<String, Object> out = new LinkedHashMap<>();
        for (String chapter : new java.util.TreeSet<>(roles.keySet())) {
            @SuppressWarnings("unchecked") Map<String, Object> r = (Map<String, Object>) roles.get(chapter);
            File dir = new File(root, chapter);
            String glossaryName = (String) r.get("GLOSSARY");
            List<EditInputs.GlossaryEntry> entries = new ArrayList<>();
            for (GlossaryStore.Term term : GlossaryStore.parseTerms(glossaryName, read(new File(dir, glossaryName)))) {
                entries.add(new EditInputs.GlossaryEntry(term.source, term.target, term.category, term.note));
            }
            String glossaryText = EditorialApiSources.glossaryAsText(entries);
            String pronounText = read(new File(dir, (String) r.get("PRONOUN")));
            EditorialPairSourceLoader.References refs = new EditorialPairSourceLoader.References(glossaryText, glossaryName, pronounText, (String) r.get("PRONOUN"));
            EditorialPairSourceLoader.FilesLoad load = EditorialPairSourceLoader.planFiles((String) r.get("RAW") + " + " + r.get("DRAFT"),
                    read(new File(dir, (String) r.get("RAW"))), read(new File(dir, (String) r.get("DRAFT"))), refs, new AppSettings());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("verdict", load.plan.verdict);
            row.put("chunks", BigDecimal.valueOf(load.plan.chunks.size()));
            List<Object> checks = new ArrayList<>();
            if (!load.plan.blocked() && !load.source.rawRows.isEmpty()) {
                FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> FakeEditorialApiProvider.edited(EditorialPairTestData.draftPart(request)));
                EditorialPairRunService svc = new EditorialPairRunService(new InMemoryPairRunStore(), fake, PRICING, 60_000L);
                EditorialPairModels.PairRun run = svc.prepare(1, load.source, glossaryText, pronounText, null, "m", "Vietnamese", new BigDecimal("50"), EditorialPairModels.ARM_CHUNK);
                svc.execute(run.id, null);
                for (int i = 0; i < fake.requests.size(); i++) {
                    String system = fake.requests.get(i).prompt().system();
                    int a = system.indexOf("# ADDRESS CHECK");
                    if (a < 0) continue;
                    String section = system.substring(a, system.indexOf("OUTPUT CONTRACT"));
                    List<Object> lines = new ArrayList<>();
                    for (String line : section.split("\n")) if (line.startsWith("Row ")) lines.add(line);
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("chunk", BigDecimal.valueOf(i + 1));
                    c.put("lines", lines);
                    checks.add(c);
                }
            }
            row.put("checks", checks);
            out.put(chapter, row);
        }
        File dest = new File(System.getenv("SC_CORPUS_OUT"));
        if (dest.getParentFile() != null) dest.getParentFile().mkdirs();
        Files.write(dest.toPath(), EditorialCanonicalJson.canonicalize(out).getBytes(StandardCharsets.UTF_8));
        assertTrue(dest.exists());
    }
}
