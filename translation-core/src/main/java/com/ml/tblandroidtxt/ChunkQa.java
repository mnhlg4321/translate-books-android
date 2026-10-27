package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Local structural QA. Warnings are review signals, not semantic judgments. */
public final class ChunkQa {
    public static Metrics inspect(String source, String output, String previousOutput) {
        Metrics m = new Metrics(); String s=safe(source), o=safe(output);
        m.sourceChars=s.length(); m.outputChars=o.length(); m.sourceJapanese=countJapanese(s); m.untranslatedJapanese=countJapanese(o);
        m.ratio=s.isEmpty()?0:o.length()/(double)s.length(); m.sourceParagraphs=paragraphs(s);m.outputParagraphs=paragraphs(o);
        m.dialogueBalance=balance(o,'「','」')+balance(o,'『','』'); m.repeatedLinePercent=repeatedLinePercent(o);
        m.longestRepeatedSequence=longestRepeatedLineRun(o);m.sourceHash=HashUtil.sha256(s);m.outputHash=HashUtil.sha256(o);
        if (!s.isEmpty() && m.ratio < .18) m.warnings.add("UNEXPECTEDLY_SHORT");
        if (!s.isEmpty() && m.ratio > 5.0) m.warnings.add("UNEXPECTEDLY_LONG");
        if (m.sourceParagraphs >= 3 && (m.outputParagraphs < m.sourceParagraphs/3 || m.outputParagraphs > m.sourceParagraphs*3)) m.warnings.add("PARAGRAPH_COUNT_SHIFT");
        if (m.outputChars > 40 && m.untranslatedJapanese > Math.max(20, m.outputChars/3)) m.warnings.add("HIGH_RETAINED_JAPANESE");
        if (m.repeatedLinePercent > .45 || m.longestRepeatedSequence >= 3) m.warnings.add("REPEATED_BLOCK");
        if (Math.abs(m.dialogueBalance) > 1) m.warnings.add("DIALOGUE_UNBALANCED");
        if (!o.isBlank() && normalize(o).equals(normalize(previousOutput))) m.warnings.add("DUPLICATES_PREVIOUS_CHUNK");
        return m;
    }
    public static class Metrics {
        public int sourceChars,sourceJapanese,outputChars,sourceParagraphs,outputParagraphs,dialogueBalance,untranslatedJapanese,longestRepeatedSequence;
        public double ratio,repeatedLinePercent; public String sourceHash,outputHash; public final List<String>warnings=new ArrayList<>();
    }
    private static int countJapanese(String s){int n=0;for(int i=0;i<s.length();i++){char c=s.charAt(i);if((c>='\u3040'&&c<='\u30ff')||(c>='\u4e00'&&c<='\u9fff'))n++;}return n;}
    private static int paragraphs(String s){String t=s.trim();return t.isEmpty()?0:t.split("(?:\\r?\\n){2,}").length;}
    private static int balance(String s,char a,char b){int n=0;for(int i=0;i<s.length();i++){if(s.charAt(i)==a)n++;if(s.charAt(i)==b)n--;}return n;}
    private static double repeatedLinePercent(String s){String[]x=s.split("\\R");if(x.length==0)return 0;HashSet<String>seen=new HashSet<>();int repeated=0;for(String v:x){v=v.trim();if(!v.isEmpty()&&!seen.add(v))repeated++;}return repeated/(double)x.length;}
    private static int longestRepeatedLineRun(String s){String[]x=s.split("\\R");int best=0,run=0;String prior=null;for(String v:x){v=v.trim();if(!v.isEmpty()&&v.equals(prior))run++;else run=1;prior=v;best=Math.max(best,run);}return best;}
    private static String normalize(String s){return safe(s).replaceAll("\\s+"," ").trim();} private static String safe(String s){return s==null?"":s;}
    private ChunkQa(){}
}
