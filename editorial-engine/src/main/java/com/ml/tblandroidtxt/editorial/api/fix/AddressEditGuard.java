package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.AddressChecklist;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Offline lock for an ADDRESS repair. The model only returns a corrected line; this class decides, without a model and
 * without knowing who speaks, whether that line is an address-only edit that the pronoun rows can justify.
 *
 * <p>It rejects only on proof, never on a guess that the answer is merely unusual, so a correct repair with no line-level
 * evidence (the speaker is clear only from the scene) is still applied:
 * <ul>
 * <li>scope: for an ADDRESS-only point, everything except address words, titles and row names must stay the same;</li>
 * <li>names: one name must not be swapped for another;</li>
 * <li>audience: a plural audience in RAW (先輩たち, みんな) has no row, so address words are kept;</li>
 * <li>direction: a row's {@code from} term followed by a case particle may be said about the person or to them, so it excludes the
 * rows of that person only when the other end of the row is not named in the RAW lines next to this one (no scene contact); a
 * vocative {@code from} term pins the rows to that term. A new address word must belong to a row that is still
 * applicable, and a word that already belongs to the pinned row must not be changed;</li>
 * <li>consistency: a line whose two or more address words already fit one row must still fit one row after the edit;</li>
 * <li>citation: an answer that changes address words names the one row it applies; only that row's forms may be added, none of them
 * removed, and the row must be one the RAW does not exclude.</li>
 * </ul>
 */
final class AddressEditGuard {
    /** Titles and honorific words that may move with an address word without being a pronoun. */
    static final Set<String> TITLES = Set.of("tiền", "bối", "hậu", "đàn", "senpai", "sempai", "sensei", "san", "kun", "chan", "sama");
    /** A {@code from} term followed by one of these Japanese characters is the subject, topic or object of the sentence, not a call. */
    private static final String THIRD_PERSON_FOLLOWERS = "がはをにのもとへでやっ";
    private static final Pattern PLURAL_AUDIENCE = Pattern.compile("(先輩|先生|さん|くん|君|ちゃん|様|殿)(たち|達|方)|みんな|皆さん|皆様|全員");
    private static final Set<String> PLURAL_MARKERS = Set.of("chúng", "bọn", "tụi");
    /** Pronouns that, after a plural marker, are first person ("chúng em"); the others are second person ("chúng mày"). */
    private static final Set<String> FIRST_PERSON = Set.of("ta", "tôi", "tớ", "tao", "tui", "mình", "em", "anh", "chị", "cháu", "con");
    private static final long MAX_DIFF_CELLS = 6_000_000L;

    /** Every rejection reason of this guard starts with this prefix. */
    static final String PREFIX = "ADDRESS_";

    private AddressEditGuard() { }

    /** Row-free checks only: the answer may draw its forms from any row the RAW does not exclude. */
    static String rejection(FixPoint point, String candidate) {
        return rejection(point, candidate, null);
    }

    /**
     * Checks an answer that cites a pronoun row ({@code [id] R<n> <line>}). An address change must name the row it applies; the added
     * forms must belong to that row, a form of that row must not be removed, and the row must not be ruled out by the RAW.
     *
     * @param citedRow 1-based row index, {@link TargetedFixParser.Answer#NOT_CITED} or {@link TargetedFixParser.Answer#INVALID_ROW}
     * @return a rejection reason, or null when the answer is not contradicted
     */
    static String rejection(FixPoint point, String candidate, int citedRow) {
        return rejection(point, candidate, Integer.valueOf(citedRow));
    }

    private static String rejection(FixPoint point, String candidate, Integer citedRow) {
        if (!point.types().contains(FixPoint.Type.ADDRESS) || point.addressRows().isEmpty() || point.insertion()) return null;
        boolean pure = point.types().size() == 1;
        List<String> beforeTokens = AddressChecklist.tokens(point.draftText());
        List<String> afterTokens = AddressChecklist.tokens(candidate);
        Lexicon lexicon = lexicon(point.addressRows());
        Set<String> allowed = new HashSet<>(lexicon.pronouns);
        allowed.addAll(lexicon.names);
        allowed.addAll(TITLES);
        Delta delta = diff(beforeTokens, afterTokens);
        if (delta == null) return "ADDRESS_LINE_TOO_LONG";
        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        for (String token : delta.removed) if (lexicon.pronouns.contains(token) || (pure && lexicon.names.contains(token))) removed.add(token);
        for (String token : delta.added) if (lexicon.pronouns.contains(token) || (pure && lexicon.names.contains(token))) added.add(token);

        if (pure && !skeleton(point.draftText(), allowed).equals(skeleton(candidate, allowed))) return "ADDRESS_SCOPE_EXCEEDED";
        // a plural phrase (chúng ta, bọn tôi, chúng mày) has no row: only a change inside that phrase is refused, a lone pronoun elsewhere is not
        String plural = pluralPhraseChange(beforeTokens, delta.removedAt(), lexicon);
        if (plural == null) plural = pluralPhraseChange(afterTokens, delta.addedAt(), lexicon);
        if (plural != null) return plural;
        if (removed.isEmpty() && added.isEmpty()) return null;
        AddressRow cited = null;
        if (citedRow != null) {
            if (citedRow == TargetedFixParser.Answer.NOT_CITED) return "ADDRESS_ROW_NOT_CITED";
            if (citedRow < 1 || citedRow > point.addressRows().size()) return "ADDRESS_ROW_CITATION_INVALID";
            cited = point.addressRows().get(citedRow - 1);
        }
        if (PLURAL_AUDIENCE.matcher(Normalizer.normalize(point.rawText(), Normalizer.Form.NFC)).find()) return "ADDRESS_AMBIGUOUS_AUDIENCE";
        if (pure) {
            List<String> removedNames = namesIn(lexicon, removed);
            List<String> addedNames = namesIn(lexicon, added);
            // a surname turned into the given name of the same person is a call repair; one person turned into another is not
            if (!removedNames.isEmpty() && !addedNames.isEmpty() && !lexicon.sameEntity(removedNames, addedNames)) return "ADDRESS_NAME_SWAPPED";
            // "Em Kuragane" must not become "Erika Kuragane": two name words of one person side by side make a full name the RAW does not carry
            if (combinesNames(afterTokens, delta.addedAt(), lexicon)) return "ADDRESS_NAME_COMBINED";
        }

        Evidence evidence = evidence(point);
        List<AddressRow> applicable = new ArrayList<>();
        for (AddressRow row : point.addressRows()) if (!evidence.excludes(row)) applicable.add(row);
        List<AddressRow> pinned = new ArrayList<>();
        for (AddressRow row : applicable) if (row.pair() && evidence.vocativeFrom.contains(row.from())) pinned.add(row);
        if (cited != null) {
            if (!applicable.contains(cited)) return "ADDRESS_CITED_ROW_EXCLUDED";
            // a vocative in RAW names the listener: a pair row for another listener contradicts it
            if (cited.pair() && !pinned.isEmpty() && !pinned.contains(cited)) return "ADDRESS_CITED_ROW_NOT_ADDRESSED";
            Set<String> citedForms = forms(cited);
            for (String token : added) if (!citedForms.contains(token)) return "ADDRESS_FORM_NOT_IN_CITED_ROW";
            for (String token : removed) if (citedForms.contains(token)) return "ADDRESS_VALID_FORM_CHANGED";
        }
        List<AddressRow> justifying = new ArrayList<>();
        for (AddressRow row : applicable) if (!row.pair() || pinned.isEmpty() || pinned.contains(row)) justifying.add(row);
        Set<String> justified = formsOf(justifying);
        for (String token : added) if (!justified.contains(token)) return "ADDRESS_FORM_NOT_IN_APPLICABLE_ROW";
        Set<String> pinnedForms = formsOf(pinned);
        for (String token : removed) if (pinnedForms.contains(token)) return "ADDRESS_VALID_FORM_CHANGED";

        Set<String> wordsBefore = addressWords(beforeTokens, lexicon);
        Set<String> wordsAfter = addressWords(afterTokens, lexicon);
        // one address word fits several rows by itself; only two or more words show that the line already follows one row
        if (wordsBefore.size() >= 2 && fitsOneRow(wordsBefore, applicable) && !fitsOneRow(wordsAfter, applicable)) return "ADDRESS_BREAKS_CONSISTENT_ROW";
        return null;
    }

    /** The tokens that differ, with their positions in the DRAFT line (removedAt) and in the candidate (addedAt). */
    private record Delta(List<String> removed, List<String> added, List<Integer> removedAt, List<Integer> addedAt) { }

    private static final class Evidence {
        final Set<String> vocativeFrom = new HashSet<>();
        final Set<String> thirdEntities = new HashSet<>();
        /** Entities that a {@code from} term names in the lines next to this one or as a call in it: they are in the scene. */
        final Set<String> onStage = new HashSet<>();

        /**
         * A name followed by a case particle may be said about the person or to them, so it does not exclude a row by itself.
         * A row stays excluded only when it involves such a person and its other end shows no sign of being in the scene.
         */
        boolean excludes(AddressRow row) {
            String speaker = key(row.speaker());
            String target = key(row.target());
            boolean speakerMentioned = thirdEntities.contains(speaker);
            boolean targetMentioned = thirdEntities.contains(target);
            if (speakerMentioned && targetMentioned) return true;
            if (targetMentioned) return !onStage.contains(speaker);
            if (speakerMentioned) return !onStage.contains(target);
            return false;
        }
    }

    /** Looks for each pair row's {@code from} term in RAW; the longest term wins where terms overlap (倉金さん over 倉金). */
    private static Evidence evidence(FixPoint point) {
        String raw = Normalizer.normalize(point.rawText(), Normalizer.Form.NFC);
        record Hit(AddressRow row, int start, int end, boolean third) { }
        List<Hit> hits = new ArrayList<>();
        for (AddressRow row : point.addressRows()) {
            if (!row.pair()) continue;
            String term = Normalizer.normalize(row.from(), Normalizer.Form.NFC);
            for (int at = raw.indexOf(term); at >= 0; at = raw.indexOf(term, at + 1)) {
                int end = at + term.length();
                boolean third = end < raw.length() && THIRD_PERSON_FOLLOWERS.indexOf(raw.charAt(end)) >= 0;
                hits.add(new Hit(row, at, end, third));
            }
        }
        Evidence out = new Evidence();
        Set<String> addressedEntities = new HashSet<>();
        List<Hit> kept = new ArrayList<>();
        for (Hit hit : hits) {
            boolean covered = false;
            for (Hit other : hits) {
                if (other != hit && other.start <= hit.start && other.end >= hit.end && other.end - other.start > hit.end - hit.start) covered = true;
            }
            if (!covered) kept.add(hit);
        }
        for (Hit hit : kept) {
            if (!hit.third) {
                out.vocativeFrom.add(hit.row.from());
                addressedEntities.add(key(hit.row.target()));
            }
        }
        for (Hit hit : kept) if (hit.third && !addressedEntities.contains(key(hit.row.target()))) out.thirdEntities.add(key(hit.row.target()));
        out.onStage.addAll(addressedEntities);
        for (String entity : point.sceneEntities()) out.onStage.add(key(entity));
        String context = Normalizer.normalize(point.rawContext(), Normalizer.Form.NFC);
        for (AddressRow row : point.addressRows()) {
            if (row.pair() && context.contains(Normalizer.normalize(row.from(), Normalizer.Form.NFC))) out.onStage.add(key(row.target()));
        }
        return out;
    }

    private static boolean fitsOneRow(Set<String> words, List<AddressRow> rows) {
        for (AddressRow row : rows) if (forms(row).containsAll(words)) return true;
        return false;
    }

    private static Set<String> addressWords(List<String> tokens, Lexicon lexicon) {
        Set<String> out = new HashSet<>();
        for (String token : tokens) if (lexicon.pronouns.contains(token)) out.add(token);
        return out;
    }

    /** Pronoun-like words (the shared lexicon plus every lower-case self/call word of a row) and names (capitalised row words). */
    private record Lexicon(Set<String> pronouns, Set<String> names, List<Set<String>> entities) {
        /** True when one entity (for instance a surname and a given name of the same person) owns every listed name. */
        boolean sameEntity(List<String> a, List<String> b) {
            for (Set<String> entity : entities) {
                if (entity.containsAll(a) && entity.containsAll(b)) return true;
            }
            return false;
        }
    }

    private static Lexicon lexicon(List<AddressRow> rows) {
        Set<String> pronouns = new HashSet<>(AddressChecklist.words());
        Set<String> names = new LinkedHashSet<>();
        for (AddressRow row : rows) {
            for (String form : new String[] {row.self(), row.call()}) {
                for (String word : caseWords(form)) {
                    String lower = word.toLowerCase(Locale.ROOT);
                    if (Character.isUpperCase(word.codePointAt(0))) names.add(lower);
                    else if (!TITLES.contains(lower)) pronouns.add(lower);
                }
            }
        }
        // every name of a speaker or target belongs to that entity, together with the names the rows use to call it
        java.util.Map<String, Set<String>> byEntity = new java.util.LinkedHashMap<>();
        for (AddressRow row : rows) {
            for (String entity : new String[] {row.speaker(), row.target()}) {
                if (entity.isEmpty() || entity.startsWith("*") || entity.startsWith("@")) continue;
                Set<String> group = byEntity.computeIfAbsent(key(entity), k -> new LinkedHashSet<>());
                for (String word : caseWords(entity)) group.add(word.toLowerCase(Locale.ROOT));
            }
            if (!row.target().isEmpty() && !row.target().startsWith("*") && !row.target().startsWith("@")) {
                Set<String> group = byEntity.get(key(row.target()));
                for (String word : caseWords(row.call())) if (Character.isUpperCase(word.codePointAt(0))) group.add(word.toLowerCase(Locale.ROOT));
            }
        }
        List<Set<String>> entities = new ArrayList<>();
        for (Set<String> group : byEntity.values()) {
            group.removeAll(pronouns);
            names.addAll(group);
            entities.add(group);
        }
        names.removeAll(pronouns);
        return new Lexicon(pronouns, names, entities);
    }

    private static Set<String> formsOf(List<AddressRow> rows) {
        Set<String> out = new HashSet<>();
        for (AddressRow row : rows) out.addAll(forms(row));
        return out;
    }

    private static Set<String> forms(AddressRow row) {
        Set<String> out = new HashSet<>(AddressChecklist.tokens(row.self()));
        out.addAll(AddressChecklist.tokens(row.call()));
        return out;
    }

    /** Letter runs with their case kept; no regex class, so it behaves the same on Android. */
    private static List<String> caseWords(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC);
        for (int i = 0; i < nfc.length(); ) {
            int cp = nfc.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isLetter(cp)) {
                word.appendCodePoint(cp);
            } else if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) out.add(word.toString());
        return out;
    }

    /** True when an added name word stands next to a different name word of the same entity. */
    private static boolean combinesNames(List<String> tokens, List<Integer> addedAt, Lexicon lexicon) {
        for (int at : addedAt) {
            String token = tokens.get(at);
            if (!lexicon.names.contains(token)) continue;
            for (int neighbour : new int[] {at - 1, at + 1}) {
                if (neighbour < 0 || neighbour >= tokens.size()) continue;
                String other = tokens.get(neighbour);
                if (!other.equals(token) && lexicon.names.contains(other) && lexicon.sameEntity(List.of(token), List.of(other))) return true;
            }
        }
        return false;
    }

    private static List<String> namesIn(Lexicon lexicon, List<String> values) {
        List<String> out = new ArrayList<>();
        for (String value : values) if (lexicon.names.contains(value)) out.add(value);
        return out;
    }

    /**
     * "chúng ta", "bọn tôi", "chúng mày": a marker followed by a pronoun is one plural phrase, and the pronoun rows give singular forms
     * only. Judged by the position of each changed token, not by its spelling, so a lone "tôi" in the same sentence stays repairable.
     * Third-person plurals ("chúng nó", "họ") hold no address word of the lexicon and are never touched by this rule.
     */
    private static String pluralPhraseChange(List<String> tokens, List<Integer> changedAt, Lexicon lexicon) {
        for (int at : changedAt) {
            String token = tokens.get(at);
            if (PLURAL_MARKERS.contains(token)) return "ADDRESS_PLURAL_PHRASE";
            if (at > 0 && PLURAL_MARKERS.contains(tokens.get(at - 1)) && lexicon.pronouns.contains(token)) {
                return FIRST_PERSON.contains(token) ? "ADDRESS_PLURAL_SELF" : "ADDRESS_PLURAL_ADDRESSEE";
            }
        }
        return null;
    }


    private static String key(String entity) { return entity.trim().toLowerCase(Locale.ROOT); }

    /** The text with every allowed word and all spaces removed: what must not change in an address-only edit. */
    private static String skeleton(String text, Set<String> allowed) {
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder out = new StringBuilder();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i <= nfc.length(); ) {
            int cp = i < nfc.length() ? nfc.codePointAt(i) : -1;
            if (cp >= 0 && Character.isLetter(cp)) {
                word.appendCodePoint(Character.toLowerCase(cp));
                i += Character.charCount(cp);
                continue;
            }
            if (word.length() > 0) {
                if (allowed.contains(word.toString())) {
                    int end = out.length();
                    while (end > 0 && (out.charAt(end - 1) == ' ' || out.charAt(end - 1) == '\t')) end--;
                    out.setLength(end);
                } else {
                    out.append(word);
                }
                word.setLength(0);
            }
            if (cp < 0) break;
            out.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return withoutBlanks(out);
    }

    /** Spaces carry no wording, so a pronoun dropped at the start of a quote must not look like an edit of the quote. */
    private static String withoutBlanks(StringBuilder text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) != ' ' && text.charAt(i) != '	') out.append(text.charAt(i));
        return out.toString();
    }

    /** Longest-common-subsequence difference of two token lists; null when the lines are too long to compare. */
    private static Delta diff(List<String> a, List<String> b) {
        if ((long) (a.size() + 1) * (b.size() + 1) > MAX_DIFF_CELLS) return null;
        int[][] lcs = new int[a.size() + 1][b.size() + 1];
        for (int i = a.size() - 1; i >= 0; i--) {
            for (int j = b.size() - 1; j >= 0; j--) {
                lcs[i][j] = a.get(i).equals(b.get(j)) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<Integer> removedAt = new ArrayList<>();
        List<Integer> addedAt = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < a.size() && j < b.size()) {
            if (a.get(i).equals(b.get(j))) { i++; j++; }
            else if (lcs[i + 1][j] >= lcs[i][j + 1]) { removedAt.add(i); removed.add(a.get(i++)); }
            else { addedAt.add(j); added.add(b.get(j++)); }
        }
        while (i < a.size()) { removedAt.add(i); removed.add(a.get(i++)); }
        while (j < b.size()) { addedAt.add(j); added.add(b.get(j++)); }
        return new Delta(removed, added, removedAt, addedAt);
    }
}
