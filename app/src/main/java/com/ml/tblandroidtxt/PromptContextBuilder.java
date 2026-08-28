package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.Normalizer;
import java.util.Comparator;

public class PromptContextBuilder {
    private static final class ParseCache {String glossary,pronoun;ParseReport report;}
    private static final ThreadLocal<ParseCache> PARSE_CACHE=ThreadLocal.withInitial(ParseCache::new);
    public static class ContextBlock {
        public String glossary = "";
        public String pronouns = "";
        public int glossaryCount = 0;
        public int pronounCount = 0;
        public String asPromptText() {
            StringBuilder sb = new StringBuilder();
            if (!glossary.trim().isEmpty()) sb.append("# GLOSSARY - LOCK TERMS\n\n").append(glossary.trim()).append("\n\n");
            if (!pronouns.trim().isEmpty()) sb.append("# CHARACTER / PRONOUN RULES\n\n").append(pronouns.trim()).append("\n\n");
            return sb.toString();
        }
    }

    public static class TermEntry {
        public String source = "";
        public String target = "";
        public String category = "term";
        public int lineNumber = -1;
        public String aliases = "";
        public boolean global = false;
        public String note = "";
        public TermEntry() {}
        public TermEntry(String s, String t, String c) { this(s, t, c, ""); }
        public TermEntry(String s, String t, String c, String n) {
            source = safe(s).trim();
            target = safe(t).trim();
            category = safe(c).trim().isEmpty() ? "term" : safe(c).trim();
            note = safe(n).trim();
        }
    }

    public static class PronounRule {
        public String text = "";
        public String key = "";
        public String sourceName = "";
        public String targetName = "";
        public String details = "";
        public int lineNumber = -1;
    }

    private static class RankedPronounRule {
        PronounRule rule;
        int score;
        boolean dialogueParticipant;
        String line;
    }

    public static class ParseReport {
        public ArrayList<TermEntry> glossaryTerms = new ArrayList<>();
        public ArrayList<PronounRule> explicitPronouns = new ArrayList<>();
        public ArrayList<String> malformedGlossaryRows = new ArrayList<>();
        public ArrayList<String> malformedPronounRows = new ArrayList<>();
        public ArrayList<String> duplicateTerms = new ArrayList<>();
        public ArrayList<String> conflicts = new ArrayList<>();
        public int pronounNotesInGlossary = 0;
        public int normalGlossaryTerms = 0;
        public boolean explicitPronounFileUsed = false;

        public String summaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("Glossary terms: ").append(glossaryTerms.size()).append('\n');
            sb.append("Pronoun rules: ").append(explicitPronouns.size()).append('\n');
            sb.append("Pronoun notes inside glossary: ").append(pronounNotesInGlossary).append('\n');
            sb.append("Explicit pronoun file priority: ").append(explicitPronounFileUsed ? "ON" : "OFF").append('\n');
            if (!duplicateTerms.isEmpty()) appendLimited(sb, "\nDuplicate same source/target", duplicateTerms, 20);
            if (!conflicts.isEmpty()) appendLimited(sb, "\nConflicts: same source has different targets", conflicts, 30);
            if (!malformedGlossaryRows.isEmpty()) appendLimited(sb, "\nMalformed glossary rows", malformedGlossaryRows, 20);
            if (!malformedPronounRows.isEmpty()) appendLimited(sb, "\nMalformed pronoun rows", malformedPronounRows, 20);
            if (duplicateTerms.isEmpty() && conflicts.isEmpty() && malformedGlossaryRows.isEmpty() && malformedPronounRows.isEmpty()) {
                sb.append("\nNo obvious glossary/pronoun format problems detected.\n");
            }
            return sb.toString().trim();
        }
    }

    public static class MatchReport {
        public ContextBlock block = new ContextBlock();
        public ArrayList<String> matchedGlossaryLines = new ArrayList<>();
        public ArrayList<String> matchedPronounLines = new ArrayList<>();
        public ArrayList<String> warnings = new ArrayList<>();
        public int totalGlossaryTerms = 0;
        public int totalPronounRules = 0;
        public int estimatedInjectedChars = 0;

        public String asPreviewText() {
            StringBuilder sb = new StringBuilder();
            sb.append("Matched glossary: ").append(block.glossaryCount).append('/').append(totalGlossaryTerms).append('\n');
            sb.append("Matched pronoun rules: ").append(block.pronounCount).append('/').append(totalPronounRules).append('\n');
            sb.append("Injected chars: ~").append(estimatedInjectedChars).append('\n');
            if (!warnings.isEmpty()) appendLimited(sb, "\nWarnings", warnings, 12);
            String body = block.asPromptText();
            if (body.trim().isEmpty()) sb.append("\nNo relevant glossary/pronoun rules injected for this chunk.\n");
            else sb.append("\n").append(body.trim()).append('\n');
            return sb.toString().trim();
        }
    }

    public static ContextBlock build(String glossaryText, String pronounText, String chunkText, AppSettings s) {
        return match(glossaryText, pronounText, chunkText, chunkText, s).block;
    }

    public static ContextBlock buildWithRuleContext(String glossaryText, String pronounText, String chunkText, String ruleContext, AppSettings s) {
        return match(glossaryText, pronounText, chunkText, ruleContext, s).block;
    }

    public static MatchReport match(String glossaryText, String pronounText, String chunkText, AppSettings s) {
        return match(glossaryText, pronounText, chunkText, chunkText, s);
    }

    public static MatchReport match(String glossaryText, String pronounText, String chunkText, String ruleContext, AppSettings s) {
        MatchReport report = new MatchReport();
        String chunk = safe(chunkText);
        int gLimit = Math.max(0, s == null ? 80 : s.glossaryInjectLimit);
        int pLimit = Math.max(0, s == null ? 40 : s.pronounInjectLimit);

        ParseReport parsed = validateCached(glossaryText, pronounText);
        ArrayList<TermEntry> normalTerms = new ArrayList<>();
        ArrayList<PronounRule> glossaryPronouns = new ArrayList<>();
        for (TermEntry t : parsed.glossaryTerms) {
            if (isPronounNote(t.category)) {
                PronounRule r = new PronounRule();
                r.text = formatPronounFromTerm(t);
                r.key = safe(t.source) + " " + safe(t.target) + " " + safe(t.category);
                r.lineNumber = t.lineNumber;
                glossaryPronouns.add(r);

                TermEntry nameLock = new TermEntry(t.source, t.target, "character");
                nameLock.lineNumber = t.lineNumber;
                normalTerms.add(nameLock);
            } else {
                normalTerms.add(t);
            }
        }

        ArrayList<PronounRule> pronounRules = parsed.explicitPronouns.isEmpty() ? glossaryPronouns : parsed.explicitPronouns;
        report.totalGlossaryTerms = normalTerms.size();
        report.totalPronounRules = pronounRules.size();
        if (!parsed.conflicts.isEmpty()) report.warnings.add("Glossary conflict detected: " + parsed.conflicts.size() + " source term(s) have multiple targets.");
        if (!parsed.malformedGlossaryRows.isEmpty()) report.warnings.add("Glossary malformed rows: " + parsed.malformedGlossaryRows.size());
        if (!parsed.malformedPronounRows.isEmpty()) report.warnings.add("Pronoun malformed rows: " + parsed.malformedPronounRows.size());
        if (!parsed.explicitPronouns.isEmpty() && !glossaryPronouns.isEmpty()) report.warnings.add("Explicit pronoun file is used, so pronoun notes in glossary are kept only as character/name locks.");
        int pronounConflicts = countPronounConflicts(pronounRules);
        if (pronounConflicts > 0) report.warnings.add("Pronoun conflict detected: " + pronounConflicts + " speaker/listener pair(s) have multiple rules.");

        StringBuilder g = new StringBuilder();
        int gc = 0;
        LinkedHashSet<String> seenTerms = new LinkedHashSet<>();
        normalTerms.sort(Comparator.comparingInt((TermEntry t) -> termRank(chunk, t)).reversed().thenComparingInt(t -> t.lineNumber));
        for (TermEntry t : normalTerms) {
            if (gc >= gLimit) break;
            if (safe(t.source).trim().isEmpty() || safe(t.target).trim().isEmpty()) continue;
            if (!t.global && !termMatches(chunk, t)) continue;
            String k = (t.source + "\u0000" + t.target).toLowerCase(Locale.ROOT);
            if (!seenTerms.add(k)) continue;
            String line = formatTermLine(t);
            g.append(line).append('\n');
            report.matchedGlossaryLines.add(line);
            gc++;
        }

        StringBuilder p = new StringBuilder();
        int pc = 0;
        LinkedHashSet<String> seenRules = new LinkedHashSet<>();
        for (RankedPronounRule selected : rankPronounRules(chunk, safe(ruleContext), pronounRules)) {
            if (pc >= pLimit) break;
            PronounRule r = selected.rule;
            if (safe(r.text).trim().isEmpty()) continue;
            String line = selected.line;
            if (!seenRules.add(line.toLowerCase(Locale.ROOT))) continue;
            p.append(line).append('\n');
            report.matchedPronounLines.add(line);
            pc++;
        }

        report.block.glossary = g.toString();
        report.block.pronouns = p.toString();
        report.block.glossaryCount = gc;
        report.block.pronounCount = pc;
        report.estimatedInjectedChars = report.block.asPromptText().length();
        return report;
    }

    private static ParseReport validateCached(String glossaryText,String pronounText){String glossary=safe(glossaryText),pronoun=safe(pronounText);ParseCache cache=PARSE_CACHE.get();if(cache.report!=null&&cache.glossary==glossaryText&&cache.pronoun==pronounText)return cache.report;if(cache.report!=null&&cache.glossary!=null&&cache.pronoun!=null&&cache.glossary.equals(glossary)&&cache.pronoun.equals(pronoun))return cache.report;cache.glossary=glossary;cache.pronoun=pronoun;cache.report=validate(glossary,pronoun);return cache.report;}

    public static String preview(String glossaryText, String pronounText, String chunkText, AppSettings s) {
        return match(glossaryText, pronounText, chunkText, s).asPreviewText();
    }

    public static ParseReport validate(String glossaryText, String pronounText) {
        ParseReport report = new ParseReport();
        ParseResult terms = parseTermsDetailed(glossaryText);
        report.glossaryTerms.addAll(terms.terms);
        report.malformedGlossaryRows.addAll(terms.malformedRows);
        for (TermEntry t : report.glossaryTerms) {
            if (isPronounNote(t.category)) report.pronounNotesInGlossary++;
            else report.normalGlossaryTerms++;
        }
        PronounParseResult rules = parsePronounRulesDetailed(pronounText);
        report.explicitPronouns.addAll(rules.rules);
        report.malformedPronounRows.addAll(rules.malformedRows);
        report.explicitPronounFileUsed = !report.explicitPronouns.isEmpty();
        analyzeDuplicatesAndConflicts(report);
        return report;
    }

    public static String validateText(String glossaryText, String pronounText) {
        return validate(glossaryText, pronounText).summaryText();
    }

    private static class ParseResult {
        ArrayList<TermEntry> terms = new ArrayList<>();
        ArrayList<String> malformedRows = new ArrayList<>();
    }

    private static class PronounParseResult {
        ArrayList<PronounRule> rules = new ArrayList<>();
        ArrayList<String> malformedRows = new ArrayList<>();
    }

    private static ParseResult parseTermsDetailed(String text) {
        ParseResult out = new ParseResult();
        String raw = safe(text).trim();
        if (raw.isEmpty()) return out;
        if (raw.startsWith("{") || raw.startsWith("[")) {
            try {
                JSONArray arr;
                if (raw.startsWith("{")) {
                    JSONObject obj = new JSONObject(raw);
                    arr = obj.optJSONArray("terms");
                    if (arr == null) arr = obj.optJSONArray("glossary");
                    if (arr == null) arr = new JSONArray();
                } else arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) { out.malformedRows.add("json[" + i + "]: not an object"); continue; }
                    TermEntry t = new TermEntry();
                    t.source = first(o.optString("source"), o.optString("src"), o.optString("ja"), o.optString("term"));
                    t.target = first(o.optString("target"), o.optString("tgt"), o.optString("vi"), o.optString("translation"));
                    t.category = first(o.optString("category"), o.optString("type"), "term");
                    t.note = clean(o.optString("note", ""));
                    Object aliasObj = o.opt("aliases");
                    if (aliasObj instanceof JSONArray) {
                        JSONArray aa = (JSONArray) aliasObj; StringBuilder ab = new StringBuilder();
                        for (int j = 0; j < aa.length(); j++) { if (ab.length() > 0) ab.append('|'); ab.append(aa.optString(j)); }
                        t.aliases = ab.toString();
                    } else t.aliases = o.optString("aliases", o.optString("alias", ""));
                    t.global = o.optBoolean("global", false) || isGlobalCategory(t.category);
                    t.lineNumber = i + 1;
                    if (!t.source.trim().isEmpty() && !t.target.trim().isEmpty()) out.terms.add(t);
                    else out.malformedRows.add("json[" + i + "]: missing source/target");
                }
                return out;
            } catch (Exception e) {
                out.malformedRows.add("JSON parse failed: " + e.getMessage());
            }
        }
        String[] lines = raw.split("\\r?\\n");
        boolean aliasesColumn = false;
        boolean promptTextHeader = false;
        for (String candidate : lines) {
            String header = safe(candidate).trim();
            if (header.isEmpty() || header.startsWith("#")) continue;
            String headerLower = header.toLowerCase(Locale.ROOT);
            if (headerLower.contains("source") && headerLower.contains("target")) {
                aliasesColumn = headerLower.contains("aliases");
                promptTextHeader = headerLower.contains("category") && !headerLower.contains("note") && !aliasesColumn;
                break;
            }
        }
        boolean csvQuoteOpen = false;
        for (int i = 0; i < lines.length; i++) {
            String l = safe(lines[i]).trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            if (csvQuoteOpen || l.contains(",")) {
                boolean quoteOpenAfter = csvQuoteState(l, csvQuoteOpen);
                if (csvQuoteOpen || quoteOpenAfter) {
                    out.malformedRows.add("line " + (i + 1) + ": multiline CSV fields are not supported");
                    csvQuoteOpen = quoteOpenAfter;
                    continue;
                }
                csvQuoteOpen = false;
            }
            String lower = l.toLowerCase(Locale.ROOT);
            if ((lower.contains("source") && lower.contains("target")) || (lower.startsWith("term,") && lower.contains("translation"))) continue;

            TermEntry t = parsePromptTermLine(l, promptTextHeader);
            if (t == null) {
                String[] cols = splitLine(l);
                if (cols.length >= 2) {
                    t = new TermEntry(clean(cols[0]), clean(cols[1]), cols.length >= 3 ? clean(cols[2]) : "term");
                    if (cols.length >= 4) {
                        if (aliasesColumn) t.aliases = clean(cols[3]);
                        else t.note = clean(cols[3]);
                    }
                    t.global = isGlobalCategory(t.category);
                }
            }
            if (t != null && !t.source.isEmpty() && !t.target.isEmpty()) {
                t.lineNumber = i + 1;
                out.terms.add(t);
            } else if (looksLikeDataLine(l)) {
                out.malformedRows.add("line " + (i + 1) + ": " + preview(l));
            }
        }
        return out;
    }

    private static TermEntry parsePromptTermLine(String l, boolean promptTextHeader) {
        if (!l.contains("=>")) return null;
        String[] a = l.split("=>", 2);
        if (a.length != 2) return null;
        String src = clean(a[0]);
        String rhs = a[1].trim();
        String aliases = "";
        String note = "";
        int aliasSep = rhs.indexOf(" | ");
        if (aliasSep >= 0) {
            String suffix = rhs.substring(aliasSep + 3).trim();
            if (promptTextHeader) note = suffix;
            else aliases = suffix;
            rhs = rhs.substring(0, aliasSep).trim();
        }
        String cat = "term";
        int b = rhs.lastIndexOf('[');
        int e = rhs.endsWith("]") ? rhs.length() - 1 : -1;
        String tgt;
        if (b >= 0 && e > b) {
            tgt = clean(rhs.substring(0, b));
            cat = clean(rhs.substring(b + 1, e));
        } else tgt = clean(rhs);
        if (src.isEmpty() || tgt.isEmpty()) return null;
        TermEntry term = new TermEntry(src, tgt, cat, note);
        term.aliases = aliases; term.global = isGlobalCategory(cat);
        return term;
    }

    private static PronounParseResult parsePronounRulesDetailed(String text) {
        PronounParseResult out = new PronounParseResult();
        String raw = safe(text).trim();
        if (raw.isEmpty()) return out;
        if (raw.startsWith("{") || raw.startsWith("[")) {
            try {
                JSONArray arr;
                if (raw.startsWith("{")) {
                    JSONObject obj = new JSONObject(raw);
                    arr = obj.optJSONArray("pronouns");
                    if (arr == null) arr = obj.optJSONArray("rules");
                    if (arr == null) arr = new JSONArray();
                } else arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) { out.malformedRows.add("json[" + i + "]: not an object"); continue; }
                    String line = first(o.optString("rule"), o.optString("text"));
                    if (line.isEmpty()) {
                        String from = first(o.optString("from"), o.optString("speaker"));
                        String to = first(o.optString("to"), o.optString("listener"));
                        String pair = first(o.optString("pair"), o.optString("pronoun"), o.optString("xung_ho"));
                        if (!from.isEmpty() || !to.isEmpty() || !pair.isEmpty()) line = from + " → " + to + ": " + pair;
                    }
                    if (!line.trim().isEmpty()) {
                        PronounRule r = new PronounRule();
                        r.sourceName = first(o.optString("from"), o.optString("speaker"));
                        r.targetName = first(o.optString("to"), o.optString("listener"));
                        r.details = first(o.optString("pair"), o.optString("pronoun"), o.optString("xung_ho"));
                        r.text = line.trim(); r.key = line.trim(); r.lineNumber = i + 1;
                        if (r.sourceName.isEmpty()) populateStructuredRule(r);
                        out.rules.add(r);
                    } else out.malformedRows.add("json[" + i + "]: missing rule/from/to/pair");
                }
                return out;
            } catch (Exception e) {
                out.malformedRows.add("JSON parse failed: " + e.getMessage());
            }
        }
        String[] lines = raw.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String l = safe(lines[i]).trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            String lower = l.toLowerCase(Locale.ROOT);
            if (lower.contains("from") && lower.contains("to") && (lower.contains("pronoun") || lower.contains("xưng") || lower.contains("pair"))) continue;
            String textLine = l;
            String[] cols = splitLine(l);
            String sourceName = "", targetName = "", details = "";
            if (cols.length >= 3 && !l.contains("→") && !l.contains("=>")) {
                sourceName = clean(cols[0]); targetName = clean(cols[1]); details = clean(cols[2]);
                textLine = sourceName + " → " + targetName + ": " + details;
            }
            if (textLine.trim().isEmpty() || looksLikeBrokenRule(textLine)) out.malformedRows.add("line " + (i + 1) + ": " + preview(l));
            else {
                PronounRule r = new PronounRule(); r.text = textLine.trim(); r.key = textLine.trim(); r.lineNumber = i + 1;
                r.sourceName = sourceName; r.targetName = targetName; r.details = details;
                if (r.sourceName.isEmpty()) populateStructuredRule(r);
                out.rules.add(r);
            }
        }
        return out;
    }

    private static void analyzeDuplicatesAndConflicts(ParseReport report) {
        HashSet<String> seenPairs = new HashSet<>();
        LinkedHashMap<String, LinkedHashSet<String>> bySource = new LinkedHashMap<>();
        for (TermEntry t : report.glossaryTerms) {
            String src = safe(t.source).trim();
            String tgt = safe(t.target).trim();
            if (src.isEmpty() || tgt.isEmpty()) continue;
            String pair = src.toLowerCase(Locale.ROOT) + "\u0000" + tgt.toLowerCase(Locale.ROOT);
            if (!seenPairs.add(pair)) report.duplicateTerms.add(src + " => " + tgt);
            String key = src.toLowerCase(Locale.ROOT);
            LinkedHashSet<String> targets = bySource.get(key);
            if (targets == null) { targets = new LinkedHashSet<>(); bySource.put(key, targets); }
            targets.add(tgt);
        }
        for (Map.Entry<String, LinkedHashSet<String>> e : bySource.entrySet()) {
            if (e.getValue().size() <= 1) continue;
            report.conflicts.add(e.getKey() + " => " + join(e.getValue(), " / "));
        }
    }

    public static boolean isPronounNote(String category) {
        String c = safe(category).toLowerCase(Locale.ROOT);
        return c.contains("pronoun") || c.contains("xưng") || c.contains("xung") || c.contains("cách gọi") || c.contains("cach goi")
                || c.contains("→") || c.contains("->")
                || (c.contains("/") && (c.contains("tôi") || c.contains("ta") || c.contains("em") || c.contains("chị") || c.contains("anh") || c.contains("cô") || c.contains("ngươi")));
    }

    private static String formatPronounFromTerm(TermEntry t) {
        String c = safe(t.category).trim();
        if (c.contains("→") || c.contains("->") || c.contains(":")) return safe(t.target).trim() + ": " + c;
        return safe(t.target).trim() + " / " + safe(t.source).trim() + ": " + c;
    }

    private static String formatTermLine(TermEntry t) {
        String line = t.source.trim() + " => " + t.target.trim();
        if (!safe(t.category).trim().isEmpty()) line += " [" + t.category.trim() + "]";
        String note = normalizePromptNote(t.note);
        if (!note.isEmpty()) line += " | " + note;
        return line;
    }

    private static String addMatchedAliases(String chunk, List<TermEntry> terms) {
        StringBuilder expanded = new StringBuilder(safe(chunk));
        if (terms == null || terms.isEmpty()) return expanded.toString();
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        for (TermEntry t : terms) {
            String src = safe(t.source).trim();
            String tgt = safe(t.target).trim();
            if (!src.isEmpty() && chunk.contains(src) && !tgt.isEmpty()) aliases.add(tgt);
            if (!tgt.isEmpty() && chunk.contains(tgt) && !src.isEmpty()) aliases.add(src);
        }
        for (String a : aliases) expanded.append(' ').append(a);
        return expanded.toString();
    }

    private static ArrayList<RankedPronounRule> rankPronounRules(String chunk, String ruleContext, List<PronounRule> rules) {
        ArrayList<RankedPronounRule> ranked = new ArrayList<>();
        String main = safe(chunk);
        String contextOnly = removeFirst(safe(ruleContext), main);
        for (PronounRule r : rules) {
            if (safe(r.text).trim().isEmpty()) continue;
            if (safe(r.sourceName).isEmpty() && safe(r.targetName).isEmpty()) populateStructuredRule(r);
            if (isGlobalRule(r.text)) {
                RankedPronounRule rr = new RankedPronounRule(); rr.rule = r; rr.score = 10_000;
                rr.dialogueParticipant = true; rr.line = normalizeRuleLine(r.text); ranked.add(rr); continue;
            }
            LinkedHashSet<String> cues = characterCues(r);
            int sourceOccurrences = countSingleCueOccurrences(main, r.sourceName);
            int targetOccurrences = countSingleCueOccurrences(main, r.targetName);
            int occurrences = sourceOccurrences + targetOccurrences;
            boolean alias = containsDetailAlias(main, r);
            boolean relationship = normalizeForMatch(r.details).startsWith("relationship");
            if (relationship) {
                if ((sourceOccurrences == 0 || targetOccurrences == 0) && !alias) continue;
            } else if (occurrences == 0 && !alias) continue;
            boolean dialogue = isDialogueParticipant(main, cues);
            int contextOccurrences = countCueOccurrences(contextOnly, cues);
            RankedPronounRule rr = new RankedPronounRule(); rr.rule = r; rr.dialogueParticipant = dialogue;
            rr.score = occurrences * 100 + (dialogue ? 500 : 0) + (alias ? 220 : 0) + Math.min(30, contextOccurrences * 5);
            ranked.add(rr);
        }
        ranked.sort(Comparator.comparingInt((RankedPronounRule r) -> r.score).reversed().thenComparingInt(r -> r.rule.lineNumber));
        int fullCount = 0;
        for (RankedPronounRule rr : ranked) {
            boolean full = rr.dialogueParticipant || fullCount == 0;
            if (full && fullCount < 4) fullCount++; else full = false;
            rr.line = full ? normalizeRuleLine(rr.rule.text) : compactRuleLine(rr.rule);
        }
        return ranked;
    }

    private static LinkedHashSet<String> characterCues(PronounRule r) {
        LinkedHashSet<String> cues = new LinkedHashSet<>();
        addCue(cues, r.sourceName); addCue(cues, r.targetName);
        if (cues.isEmpty()) cues.addAll(namesFromRule(r.key));
        return cues;
    }

    private static void addCue(Set<String> cues, String value) {
        String v = safe(value).trim(); if (v.length() >= 2) cues.add(v);
    }

    private static int countCueOccurrences(String text, Set<String> cues) {
        String hay = normalizeForMatch(text); int count = 0;
        for (String cue : cues) {
            String needle = normalizeForMatch(cue); if (needle.isEmpty()) continue;
            int at = hay.indexOf(needle);
            while (at >= 0) {
                if (isCjkText(needle) || (boundary(hay, at - 1) && boundary(hay, at + needle.length()))) count++;
                at = hay.indexOf(needle, at + Math.max(1, needle.length()));
            }
        }
        return count;
    }

    private static int countSingleCueOccurrences(String text, String cue) {
        LinkedHashSet<String> one = new LinkedHashSet<>(); addCue(one, cue);
        return countCueOccurrences(text, one);
    }

    private static boolean containsDetailAlias(String text, PronounRule r) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}ー]{2,}").matcher(safe(r.details));
        while (m.find()) if (containsNormalized(text, m.group())) return true;
        return false;
    }

    private static boolean isDialogueParticipant(String text, Set<String> cues) {
        String source = safe(text), normalized = normalizeForMatch(source);
        for (String cue : cues) {
            String needle = normalizeForMatch(cue); int at = normalized.indexOf(needle);
            while (at >= 0) {
                int safeAt = Math.min(at, source.length());
                int from = Math.max(0, safeAt - 140), to = Math.min(source.length(), safeAt + cue.length() + 140);
                if (source.substring(from, to).matches("(?s).*[「」『』“”\\\"].*")) return true;
                at = normalized.indexOf(needle, at + Math.max(1, needle.length()));
            }
        }
        return false;
    }

    private static String compactRuleLine(PronounRule r) {
        String name = safe(r.sourceName).trim(), target = safe(r.targetName).trim(), details = safe(r.details).trim();
        if (name.isEmpty() && target.isEmpty()) return normalizeRuleLine(r.text);
        ArrayList<String> keep = new ArrayList<>();
        for (String raw : details.split(";")) {
            String part = raw.trim(), n = normalizeForMatch(part);
            if (part.isEmpty()) continue;
            if (n.contains("nam") || n.contains("nữ") || n.contains("nu") || n.contains("lời kể") || n.contains("loi ke")
                    || n.contains("tự xưng") || n.contains("tu xung") || n.contains("gọi") || n.contains("goi")
                    || n.contains("đại từ") || n.contains("dai tu")) keep.add(part);
            if (keep.size() >= 3) break;
        }
        if (keep.isEmpty() && !details.isEmpty()) keep.add(details.split(";")[0].trim());
        return name + (target.isEmpty() ? "" : " => " + target) + (keep.isEmpty() ? "" : " [" + join(keep, "; ") + "]");
    }

    private static void populateStructuredRule(PronounRule r) {
        String text = safe(r.text).trim(); int arrow = text.indexOf("→"), arrowLen = 1;
        if (arrow < 0) { arrow = text.indexOf("->"); arrowLen = 2; }
        if (arrow < 0) { arrow = text.indexOf("=>"); arrowLen = 2; }
        int colon = text.indexOf(':', Math.max(0, arrow + arrowLen));
        if (arrow > 0) {
            r.sourceName = text.substring(0, arrow).trim();
            r.targetName = colon > arrow ? text.substring(arrow + arrowLen, colon).trim() : text.substring(arrow + arrowLen).trim();
            r.details = colon > arrow ? text.substring(colon + 1).trim() : "";
        }
    }

    private static String removeFirst(String value, String part) {
        if (part.isEmpty()) return value;
        int at = value.indexOf(part); return at < 0 ? value : value.substring(0, at) + value.substring(at + part.length());
    }

    private static Set<String> namesFromRule(String s) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        String cleaned = safe(s).replace("→", " ").replace("->", " ").replace(":", " ").replace("=", " ").replace("/", " ");
        for (String part : cleaned.split("[\\s,;|]+")) {
            String t = part.trim();
            if (t.length() < 2) continue;
            String l = t.toLowerCase(Locale.ROOT);
            if (isStopRuleToken(l)) continue;
            set.add(t);
        }
        return set;
    }

    private static boolean isStopRuleToken(String l) {
        return l.equals("pronoun") || l.equals("xưng") || l.equals("hô") || l.equals("xung") || l.equals("ho")
                || l.equals("voi") || l.equals("với") || l.equals("to") || l.equals("from") || l.equals("speaker") || l.equals("listener")
                || l.equals("toi") || l.equals("tôi") || l.equals("ta") || l.equals("tớ") || l.equals("mình") || l.equals("em")
                || l.equals("chị") || l.equals("anh") || l.equals("cô") || l.equals("ngươi") || l.equals("ông") || l.equals("bà")
                || l.equals("con") || l.equals("cháu") || l.equals("cậu") || l.equals("bạn") || l.equals("tên") || l.equals("gọi");
    }

    private static boolean termMatches(String chunk, TermEntry t) {
        if (containsNormalized(chunk, t.source) || containsNormalized(chunk, t.target)) return true;
        for (String alias : safe(t.aliases).split("[|;/]")) if (containsNormalized(chunk, alias)) return true;
        return false;
    }

    private static int termRank(String chunk, TermEntry t) {
        if (t.global) return 1000;
        if (containsNormalized(chunk, t.source)) return 900;
        for (String alias : safe(t.aliases).split("[|;/]")) if (containsNormalized(chunk, alias)) return 800;
        if (containsNormalized(chunk, t.target)) return 700;
        return 0;
    }

    private static boolean containsNormalized(String haystack, String needle) {
        String n = normalizeForMatch(needle); if (n.isEmpty()) return false;
        String h = normalizeForMatch(haystack); int at = h.indexOf(n);
        while (at >= 0) {
            if (isCjkText(n) || (boundary(h, at - 1) && boundary(h, at + n.length()))) return true;
            at = h.indexOf(n, at + 1);
        }
        return false;
    }

    private static String normalizeForMatch(String s) {
        return Normalizer.normalize(safe(s), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim();
    }
    private static boolean boundary(String s, int i) { return i < 0 || i >= s.length() || !Character.isLetterOrDigit(s.charAt(i)); }
    private static boolean isCjkText(String s) { for (int i=0;i<s.length();i++) { char c=s.charAt(i); if ((c>='\u3040'&&c<='\u30ff')||(c>='\u3400'&&c<='\u9fff')) return true; } return false; }
    private static boolean isGlobalCategory(String c) { String n=normalizeForMatch(c); return n.contains("global") || n.contains("mandatory") || n.contains("always"); }
    private static boolean isGlobalRule(String r) { String n=normalizeForMatch(r); return n.startsWith("global:") || n.startsWith("mandatory:") || n.startsWith("always:") || n.startsWith("*:");
    }

    private static int countPronounConflicts(List<PronounRule> rules) {
        LinkedHashMap<String, LinkedHashSet<String>> byPair = new LinkedHashMap<>();
        for (PronounRule rule : rules) {
            String n = normalizeForMatch(rule.text).replace("->", "→");
            int arrow = n.indexOf('→'), colon = n.indexOf(':');
            if (arrow <= 0 || colon <= arrow) continue;
            String pair = n.substring(0, colon).replaceAll("\\s+", "");
            String value = n.substring(colon + 1).trim();
            byPair.computeIfAbsent(pair, k -> new LinkedHashSet<>()).add(value);
        }
        int conflicts=0; for (LinkedHashSet<String> values : byPair.values()) if (values.size()>1) conflicts++;
        return conflicts;
    }

    private static boolean looksLikeDataLine(String l) {
        return l.contains(",") || l.contains("\t") || l.contains("=>") || l.contains("->") || l.contains("→");
    }

    private static boolean looksLikeBrokenRule(String l) {
        String x = safe(l).trim();
        return x.equals(",") || x.equals("=>") || x.equals("->") || x.equals("→") || x.length() < 2;
    }

    private static String normalizeRuleLine(String s) { return safe(s).replaceAll("\\s+", " ").trim(); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String first(String... vals) { for (String v : vals) if (v != null && !v.trim().isEmpty()) return v.trim(); return ""; }
    private static String clean(String s) { return safe(s).replace("\uFEFF", "").trim(); }
    private static String normalizePromptNote(String s) { return safe(s).replace('\r', ' ').replace('\n', ' ').trim(); }

    private static String[] splitLine(String l) {
        if (l.contains("\t")) return cleanArr(l.split("\t", -1));
        if (l.contains(",")) return cleanArr(splitCsv(l));
        if (l.contains("=>")) return cleanArr(l.split("=>", -1));
        if (l.contains("->")) return cleanArr(l.split("->", -1));
        if (l.contains("→")) return cleanArr(l.split("→", -1));
        return new String[]{l};
    }

    private static String[] splitCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (q && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else q = !q;
            } else if (ch == ',' && !q) { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String[] cleanArr(String[] arr) {
        ArrayList<String> out = new ArrayList<>();
        for (String s : arr) out.add(clean(s));
        return out.toArray(new String[0]);
    }

    private static boolean csvQuoteState(String line, boolean quoted) {
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch != '"') continue;
            if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { i++; continue; }
            quoted = !quoted;
        }
        return quoted;
    }

    private static void appendLimited(StringBuilder sb, String title, List<String> items, int limit) {
        sb.append(title).append(" (showing ").append(Math.min(limit, items.size())).append('/').append(items.size()).append("):\n");
        int n = 0;
        for (String it : items) {
            if (n++ >= limit) { sb.append("...\n"); break; }
            sb.append("- ").append(it).append('\n');
        }
    }

    private static String join(Iterable<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String x : list) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(x);
        }
        return sb.toString();
    }

    private static String preview(String s) {
        String one = safe(s).replace('\n', ' ').replace('\r', ' ').trim();
        return one.length() <= 160 ? one : one.substring(0, 160) + "...";
    }
}
