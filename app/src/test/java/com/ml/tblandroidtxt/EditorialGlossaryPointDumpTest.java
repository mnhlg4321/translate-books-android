package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint;

import org.junit.Assume;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Opt-in (GDUMP_DIRS = chapter folders separated by ';', GDUMP_OUT = a private TSV): runs the real chunk plan and the real
 * finder with the app defaults over each folder (RAW, DRAFT, Glossary, Pronoun found by file name) and writes one row per
 * GLOSSARY point: folder, chunk, global DRAFT unit, glossary sources named by the rule, SHA-256 of the DRAFT line. It carries no
 * book text. Used to count glossary points before and after a finder change (v4.22 step G). No provider.
 */
public final class EditorialGlossaryPointDumpTest {
    private static final Pattern SOURCE = Pattern.compile("nguồn '([^']*)'");

    @Test public void dumpGlossaryPointsWhenConfigured() throws Exception {
        String dirs = System.getenv("GDUMP_DIRS");
        String out = System.getenv("GDUMP_OUT");
        Assume.assumeTrue("set GDUMP_DIRS and GDUMP_OUT", dirs != null && !dirs.isBlank() && out != null && !out.isBlank());
        StringBuilder tsv = new StringBuilder();
        int total = 0;
        for (String d : dirs.split(";")) {
            if (d.isBlank()) continue;
            Path dir = Path.of(d.trim());
            assertTrue("missing folder " + dir, Files.isDirectory(dir));
            Path raw = pick(dir, "raw");
            Path draft = pick(dir, "draft");
            Path glossaryPath = pick(dir, "glossary");
            Path pronounPath = pick(dir, "pronoun");
            List<EditInputs.GlossaryEntry> glossary = GlossaryStore.parseTerms(glossaryPath.getFileName().toString(), read(glossaryPath)).stream()
                    .map(t -> new EditInputs.GlossaryEntry(t.source, t.target, t.category, t.note)).toList();
            String glossaryText = EditorialApiSources.glossaryAsText(glossary);
            String pronoun = read(pronounPath);
            EditorialPairSourceLoader.References refs = new EditorialPairSourceLoader.References(
                    glossaryText, glossaryPath.getFileName().toString(), pronoun, pronounPath.getFileName().toString());
            EditorialPairSourceLoader.FilesLoad load = EditorialPairSourceLoader.planFiles(dir.getFileName().toString(), read(raw), read(draft), refs, settings());
            assertNotNull("plan missing " + dir, load.plan);
            assertFalse("blocked plan " + dir, load.plan.blocked());
            EditorialPairPreview preview = EditorialPairPreview.of(load.source, glossary, pronoun, EditorialPairSnapshot.cues("from,speaker,target"));
            assertTrue("preview blocked " + dir, preview.runnable());
            ChunkPlan plan = load.plan;
            for (int e = 0; e < preview.rows.size(); e++) {
                int offset = plan.chunks.get(e).draftFrom();
                for (FixPoint p : preview.rows.get(e).fixPoints) {
                    if (!p.types().contains(FixPoint.Type.GLOSSARY)) continue;
                    List<String> sources = new ArrayList<>();
                    for (String rule : p.rules()) {
                        Matcher m = SOURCE.matcher(rule);
                        while (m.find()) sources.add(m.group(1));
                    }
                    tsv.append(dir.getFileName()).append('\t').append(e + 1).append('\t').append(offset + p.draftLineIndex()).append('\t')
                            .append(String.join("|", sources)).append('\t').append(sha(p.draftText())).append('\n');
                    total++;
                }
            }
        }
        Files.write(Path.of(out), tsv.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("GDUMP glossaryPoints=" + total + " out=" + out);
    }

    private static Path pick(Path dir, String role) throws Exception {
        List<Path> hits = new ArrayList<>();
        try (var files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String n = p.getFileName().toString();
                String u = n.toUpperCase(Locale.ROOT);
                if (!Files.isRegularFile(p) || u.contains("FINAL")) continue;
                boolean isDraft = u.endsWith("_DRAFT.TXT") || u.endsWith("_TRANSLATED.TXT");
                boolean ok;
                switch (role) {
                    case "draft": ok = isDraft; break;
                    case "raw": ok = !isDraft && u.contains("RAW") && u.endsWith(".TXT"); break;
                    case "glossary": ok = u.contains("GLOSSARY") && u.endsWith(".CSV"); break;
                    default: ok = u.contains("PRONOUN") && u.endsWith(".CSV");
                }
                if (ok) hits.add(p);
            }
        }
        assertTrue("expected exactly one " + role + " file in " + dir + " but found " + hits.size(), hits.size() == 1);
        return hits.get(0);
    }

    private static String sha(String text) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format(Locale.ROOT, "%02x", b));
        return sb.toString();
    }

    private static String read(Path path) throws Exception {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    private static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.provider = "openrouter";
        s.model = "openai/gpt-5.6-luna";
        s.optimizationPreset = "balanced";
        s.chunkMode = "token";
        s.maxTokensPerChunk = 450;
        s.softLimitRatio = 0.8f;
        s.maxOutputTokens = 4096;
        s.contextChars = 400;
        return s;
    }
}
