package q1;

import com.ml.tblandroidtxt.editorial.api.AddressChecklist;
import com.ml.tblandroidtxt.editorial.api.ReferenceProjector;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Offline only. Never imported by app/engine runtime; produces candidates, never edits text. */
public final class Q1SpeakerSidecar {
    public record Label(String speaker, String listener) {}
    public record Check(String status, String row, List<String> unexpected) {
        public boolean candidate() { return status.equals("CANDIDATE"); }
    }
    private static final Pattern WIRE = Pattern.compile("\\[([1-9][0-9]*)] (C[1-9][0-9]*|\\?|@NARRATOR|@GROUP) > (C[1-9][0-9]*|\\?|@GROUP)");
    private Q1SpeakerSidecar() {}

    /** Ordered chapter-wide roster from valid seven-column rows, no aliases invented from notes. */
    public static Map<String,String> roster(List<ReferenceProjector.Row> rows) {
        Map<String,String> out = new LinkedHashMap<>();
        for (var r : rows) if (r.p3()) for (String s : List.of(r.speaker(), r.target()))
            if (!s.isBlank() && !s.equals("*") && !s.equals("-") && !s.equals("@NARRATOR") && !s.equals("@AUDIENCE")
                    && !out.containsValue(s)) out.put("C" + (out.size()+1), s);
        return Collections.unmodifiableMap(out);
    }
    /** Rejects the whole response on malformed, duplicate, missing or unknown IDs; no partial guesses. */
    public static Map<Integer,Label> parse(String response, Set<Integer> requested, Map<String,String> roster) {
        Map<Integer,Label> out = new TreeMap<>();
        for (String line : response.replace("\r\n","\n").split("\n",-1)) {
            if (line.isBlank()) continue;
            Matcher m = WIRE.matcher(line);
            if (!m.matches()) throw new IllegalArgumentException("Q1_WIRE_FORMAT");
            int n;
            try { n = Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { throw new IllegalArgumentException("Q1_LINE_RANGE"); }
            if (!requested.contains(n) || out.containsKey(n)) throw new IllegalArgumentException("Q1_LINE_ID");
            out.put(n, new Label(resolve(m.group(2),roster),resolve(m.group(3),roster)));
        }
        if (!out.keySet().equals(requested)) throw new IllegalArgumentException("Q1_MISSING_LINE");
        return Collections.unmodifiableMap(out);
    }
    private static String resolve(String id, Map<String,String> roster) {
        if (id.equals("?") || id.equals("@NARRATOR") || id.equals("@GROUP")) return id;
        if (!roster.containsKey(id)) throw new IllegalArgumentException("Q1_CHARACTER_ID");
        return roster.get(id);
    }
    private static String norm(String s) { return Normalizer.normalize(s.trim(),Normalizer.Form.NFC).toLowerCase(Locale.ROOT); }
    private static Check skip(String status) { return new Check(status,"",List.of()); }

    public static Check compare(String draft, int paragraph, Label label, List<ReferenceProjector.Row> rows) {
        if (label == null || label.speaker().equals("?") || label.listener().equals("?")) return skip("UNKNOWN");
        if (label.speaker().equals("@NARRATOR")) return skip("NARRATION");
        if (label.speaker().equals("@GROUP") || label.listener().equals("@GROUP")) return skip("GROUP_UNRESOLVED");
        String quoted = quotation(draft);
        if (quoted == null) return skip("QUOTATION_UNRESOLVED");
        List<ReferenceProjector.Row> exact = new ArrayList<>(), fallback = new ArrayList<>();
        for (var row : rows) {
            if (!row.p3() || !row.covers(paragraph) || !row.speaker().equals(label.speaker())) continue;
            if (row.target().equals(label.listener())) exact.add(row);
            else if (row.target().equals("*")) fallback.add(row);
        }
        List<ReferenceProjector.Row> chosen = exact.isEmpty() ? fallback : exact;
        if (chosen.isEmpty()) return skip("NO_SCOPED_ROW");
        // The design asks for one row. Same pair with conflicting forms abstains; source cues are not used to guess.
        Set<String> forms = new HashSet<>();
        for (var row : chosen) forms.add(norm(row.self())+"\u0000"+norm(row.call()));
        if (forms.size()!=1) return skip("ROW_CONFLICT");
        var row = chosen.get(0);
        if (row.self().isBlank() || row.call().isBlank()) return skip("INCOMPLETE_ROW");
        Set<String> vocabulary = new LinkedHashSet<>(AddressChecklist.words());
        Set<String> names = new HashSet<>();
        for (var r : rows) for (String entity : List.of(r.speaker(),r.target())) names.addAll(AddressChecklist.tokens(entity));
        // Table-supplied atomic forms catch words absent from the historical lexicon, without a new vocabulary list.
        for (var r : rows) for (String form : List.of(r.self(),r.call())) {
            var tokens = AddressChecklist.tokens(form);
            if (tokens.size()==1 && !names.contains(tokens.get(0))) vocabulary.add(tokens.get(0));
        }
        Set<String> allowed = new HashSet<>(AddressChecklist.tokens(row.self()+" "+row.call()));
        Set<String> unexpected = new TreeSet<>();
        for (String token : AddressChecklist.tokens(quoted)) if (vocabulary.contains(token) && !allowed.contains(token)) unexpected.add(token);
        return new Check(unexpected.isEmpty()?"CLEAN":"CANDIDATE", row.key(),List.copyOf(unexpected));
    }
    /** One balanced outer quotation; embedded quoted content and surrounding narration are excluded. */
    public static String quotation(String draft) {
        String pairs = "「」『』“”\"\"";
        int start=-1, end=-1; Deque<Character> closes=new ArrayDeque<>();StringBuilder outer=new StringBuilder();
        for (int i=0;i<draft.length();i++) {
            char c=draft.charAt(i);
            int at=pairs.indexOf(c);
            if (at<0) {if(start>=0&&end<0&&closes.size()==1)outer.append(c);continue;}
            if (start<0) {
                if (at%2!=0) return null;
                start=i;closes.push(pairs.charAt(at+1));
            } else if (end<0) {
                if (c==closes.peek()) {closes.pop();if(closes.isEmpty())end=i;else if(closes.size()==1)outer.append(' ');}
                else if(at%2==0){closes.push(pairs.charAt(at+1));if(closes.size()==2)outer.append(' ');}
                else return null;
            } else return null;
        }
        return start>=0 && end>start ? outer.toString() : null;
    }
}
