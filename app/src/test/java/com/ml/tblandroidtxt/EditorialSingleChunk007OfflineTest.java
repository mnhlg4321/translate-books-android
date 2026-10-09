package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairText;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Opt-in (SC_RAW, SC_DRAFT, SC_GLOSSARY, SC_PRONOUN name the owner's four original files of one chapter; SC_OUT is a private
 * folder): prepares the real chunk run offline, selects one chunk with the single-chunk guards, sends it to a fake provider
 * through the one-call wrapper, and checks the request against a recorded baseline prompt. Writes the selector values and the
 * new prompt to SC_OUT; nothing from the books goes into the repository. 0 USD, no network, no device.
 */
public final class EditorialSingleChunk007OfflineTest {
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private static String read(String env) throws Exception {
        byte[] b = Files.readAllBytes(new File(System.getenv(env)).toPath());
        String s = new String(b, StandardCharsets.UTF_8);
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    private static String name(String env) { return new File(System.getenv(env)).getName(); }

    @Test public void chunkFiveIsSelectedAlonePromptDiffersFromTheBaselineOnlyInTheCore() throws Exception {
        Assume.assumeTrue("set SC_RAW/SC_DRAFT/SC_GLOSSARY/SC_PRONOUN/SC_OUT", System.getenv("SC_RAW") != null && System.getenv("SC_OUT") != null);
        int ordinal = Integer.parseInt(System.getenv().getOrDefault("SC_ORDINAL", "5"));
        AppSettings settings = new AppSettings();
        List<EditInputs.GlossaryEntry> entries = new ArrayList<>();
        for (GlossaryStore.Term term : GlossaryStore.parseTerms(name("SC_GLOSSARY"), read("SC_GLOSSARY"))) {
            entries.add(new EditInputs.GlossaryEntry(term.source, term.target, term.category, term.note));
        }
        String glossaryText = EditorialApiSources.glossaryAsText(entries);
        String pronounText = read("SC_PRONOUN");
        EditorialPairSourceLoader.References refs = new EditorialPairSourceLoader.References(glossaryText, name("SC_GLOSSARY"), pronounText, name("SC_PRONOUN"));
        EditorialPairSourceLoader.FilesLoad load = EditorialPairSourceLoader.planFiles(name("SC_RAW") + " + " + name("SC_DRAFT"), read("SC_RAW"), read("SC_DRAFT"), refs, settings);
        assertEquals("OK", load.plan.verdict);
        if (System.getenv("SC_BASELINE_PLAN") != null) {
            String baseline = new String(Files.readAllBytes(new File(System.getenv("SC_BASELINE_PLAN")).toPath()), StandardCharsets.UTF_8);
            assertEquals("the plan cut on the host must be the plan the device cut", baseline, load.plan.toJson());
        }

        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> FakeEditorialApiProvider.edited(EditorialPairTestData.draftPart(request)));
        EditorialSingleChunkProbe.OneCallProvider capped = new EditorialSingleChunkProbe.OneCallProvider(fake);
        InMemoryPairRunStore store = new InMemoryPairRunStore();
        EditorialPairRunService svc = new EditorialPairRunService(store, capped, PRICING, 60_000L);
        PairRun run = svc.prepare(1, load.source, glossaryText, pronounText, null, "openai/gpt-5.6-luna", "Vietnamese", new BigDecimal("0.01"), EditorialPairModels.ARM_CHUNK);

        PairMap map = EditorialPairSnapshot.of(run).map;
        PairMap.Entry entry = null;
        for (PairMap.Entry e : map.entries()) if (e.displayOrdinal() == ordinal) entry = e;
        EditorialSingleChunkProbe.Selector selector = new EditorialSingleChunkProbe.Selector(ordinal, entry.pairId(), run.mapHash,
                map.rawParagraphStart(entry), map.rawParagraphEnd(entry), PairText.sha256(map.rawText(entry)), PairText.sha256(map.draftText(entry)));
        String pairId = EditorialSingleChunkProbe.resolve(run, selector);
        svc.executeOnly(run.id, pairId, null);

        assertEquals("only the selected chunk reaches the provider", 1, fake.requests.size());
        assertEquals(1, capped.dispatched());
        assertEquals(load.plan.chunks.size(), svc.items(run.id).size());
        for (PairItem i : svc.items(run.id)) {
            assertEquals(i.pairId.equals(pairId) ? PairState.ACCEPTED : PairState.IMPORTED, i.state);
        }
        assertEquals(1, store.allReservations(run.id).size());

        String system = fake.requests.get(0).prompt().system();
        String user = fake.requests.get(0).prompt().user();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ordinal", BigDecimal.valueOf(ordinal));
        out.put("pairId", pairId);
        out.put("mapHash", run.mapHash);
        out.put("firstParagraph", BigDecimal.valueOf(selector.firstParagraph()));
        out.put("lastParagraph", BigDecimal.valueOf(selector.lastParagraph()));
        out.put("rawRangeSha256", selector.rawRangeSha256());
        out.put("draftRangeSha256", selector.draftRangeSha256());
        out.put("chunks", BigDecimal.valueOf(load.plan.chunks.size()));
        out.put("contractRevision", run.contractRevision);
        out.put("maxOutputTokens", BigDecimal.valueOf(fake.maxOutputTokens.get(0)));
        out.put("estimatedInputTokens", BigDecimal.valueOf(fake.requests.get(0).prompt().estimatedInputTokens()));
        out.put("systemSha256", PairText.sha256(system));
        out.put("userSha256", PairText.sha256(user));

        String baselinePath = System.getenv("SC_BASELINE_PROMPT");
        if (baselinePath != null) {
            String baseline = new String(Files.readAllBytes(new File(baselinePath).toPath()), StandardCharsets.UTF_8);
            int u = baseline.indexOf("\nUSER\n");
            String baseSystem = baseline.substring("SYSTEM\n".length(), u);
            String baseUser = baseline.substring(u + "\nUSER\n".length());
            assertEquals("user message (RAW, DRAFT, reference-only context) is identical", baseUser, user);
            int g = baseSystem.indexOf("# GLOSSARY");
            int c = system.indexOf("# GLOSSARY");
            assertTrue(g > 0 && c > 0);
            assertEquals("glossary, pronoun rows, partial-scope notes and the output contract are identical", baseSystem.substring(g), system.substring(c));
            int baseContract = baseSystem.indexOf("CHUNK CONTRACT");
            int newContract = system.indexOf("CHUNK CONTRACT");
            assertTrue(baseContract > 0 && newContract > 0);
            String baseTail = baseSystem.substring(baseContract, g);
            String newTail = system.substring(newContract, c);
            String oldLayout = "Keep the line breaks and any marker line (such as a line with only a symbol) of the DRAFT part where they are.";
            String newLayout = "Preserve scene markers and the DRAFT layout unless a RAW-supported content correction requires changing the layout.";
            assertTrue(baseTail.contains(oldLayout) && newTail.contains(newLayout));
            assertEquals("the chunk contract differs by the layout sentence only", baseTail.replace(oldLayout, newLayout), newTail);
            out.put("promptComparison", "user identical; references and output contract identical; differs only in the core and one layout sentence");
        }
        File dir = new File(System.getenv("SC_OUT"));
        dir.mkdirs();
        Files.write(new File(dir, "selector-chunk" + ordinal + ".json").toPath(), EditorialCanonicalJson.canonicalize(out).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "prompt-cp-impl-4-chunk" + ordinal + ".txt").toPath(), ("SYSTEM\n" + system + "\nUSER\n" + user).getBytes(StandardCharsets.UTF_8));
    }
}
