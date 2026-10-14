package q1;

import com.ml.tblandroidtxt.editorial.api.ReferenceProjector;
import java.util.*;

/** Synthetic names only; executable without Gradle/JUnit so parallel app work is unaffected. */
public final class Q1SpeakerSyntheticTest {
    private static int passed;
    private static void check(boolean ok) { if(!ok) throw new AssertionError("case "+(passed+1)); passed++; }
    private static void rejects(String s, Set<Integer> ids, Map<String,String> roster) {
        boolean rejected=false;try {Q1SpeakerSidecar.parse(s,ids,roster);} catch(IllegalArgumentException e){rejected=true;}check(rejected);
    }
    public static void main(String[] args) {
        String csv="from,speaker,target,self,call,scope,note\nA,Aki,Mori,tôi,em,p002-p004,x\nB,Ken,Mori,tao,mày,*,x\n";
        var rows=ReferenceProjector.project(new ReferenceProjector.Params("CH001",1,10,"","","",Set.of("none")),List.of(),csv).pronouns();
        var roster=Q1SpeakerSidecar.roster(rows);
        check(roster.equals(Map.of("C1","Aki","C2","Mori","C3","Ken")));
        var labels=Q1SpeakerSidecar.parse("[2] C1 > C2\n[3] ? > ?\n",Set.of(2,3),roster);
        check(labels.get(2).speaker().equals("Aki"));check(labels.get(3).speaker().equals("?"));
        rejects("[2] C9 > C2",Set.of(2),roster);
        rejects("[2] C1 > C2\n[2] C1 > C2",Set.of(2),roster);
        rejects("[3] C1 > C2",Set.of(2),roster);
        rejects("",Set.of(2),roster);
        rejects("```\n[2] C1 > C2\n```",Set.of(2),roster);
        rejects("[2] C1 > C2 prose",Set.of(2),roster);
        rejects("[02] C1 > C2",Set.of(2),roster);
        rejects("[99999999999999999999] C1 > C2",Set.of(2),roster);
        var label=labels.get(2);
        check(Q1SpeakerSidecar.compare("「Tôi đợi em」",2,label,rows).status().equals("CLEAN"));
        check(Q1SpeakerSidecar.compare("「Tao đợi em」",2,label,rows).candidate()); // wrong form valid in another row
        check(Q1SpeakerSidecar.compare("「Tao đợi em」",1,label,rows).status().equals("NO_SCOPED_ROW"));
        check(Q1SpeakerSidecar.compare("「Tao」",2,labels.get(3),rows).status().equals("UNKNOWN"));
        check(Q1SpeakerSidecar.compare("「Tao」",2,new Q1SpeakerSidecar.Label("Aki","?"),rows).status().equals("UNKNOWN"));
        check(Q1SpeakerSidecar.compare("「Tao」",2,new Q1SpeakerSidecar.Label("@NARRATOR","Mori"),rows).status().equals("NARRATION"));
        check(Q1SpeakerSidecar.compare("「Tao」",2,new Q1SpeakerSidecar.Label("Aki","@GROUP"),rows).status().equals("GROUP_UNRESOLVED"));
        check(Q1SpeakerSidecar.compare("Tao kể 「tôi đợi em」",2,label,rows).status().equals("CLEAN"));
        check(Q1SpeakerSidecar.quotation("「A『B』」").trim().equals("A"));check(Q1SpeakerSidecar.quotation("「A」「B」")==null);
        check(Q1SpeakerSidecar.quotation("「A")==null);check(Q1SpeakerSidecar.quotation("A")==null);
        check(Q1SpeakerSidecar.quotation("\"tôi\"").equals("tôi"));
        check(Q1SpeakerSidecar.compare("「Tôi nói 『tao』 với em」",2,label,rows).status().equals("CLEAN"));
        check(Q1SpeakerSidecar.compare("「Tôi gọi Ken, em nghe không」",2,label,rows).status().equals("CLEAN"));
        check(Q1SpeakerSidecar.compare("「TÔI đợi EM」",2,label,rows).status().equals("CLEAN"));
        var conflict=new ArrayList<>(rows);var r=rows.get(0);
        conflict.add(new ReferenceProjector.Row("x","A","Aki","Mori","ta","em","x",true,List.of(new int[]{2,4}),false,Set.of("GLOBAL")));
        check(Q1SpeakerSidecar.compare("「Tao」",2,label,conflict).status().equals("ROW_CONFLICT"));
        var bad=ReferenceProjector.project(new ReferenceProjector.Params("CH001",1,10,"","","",Set.of("none")),List.of(),"from,speaker,target,self,call,scope,note\nA,Aki,Mori,tôi,em,CH002:p002,x\nA,Aki,Mori,tôi,em,p004-p002,x");
        check(bad.pronouns().isEmpty());
        check(Q1SpeakerSidecar.compare("「Em đợi tôi」",2,label,rows).status().equals("CLEAN")); // known role-blind union limitation
        var extra=ReferenceProjector.project(new ReferenceProjector.Params("CH001",1,10,"","","",Set.of("none")),List.of(),
                "from,speaker,target,self,call,scope,note\nA,Aki,*,ta,ngươi,*,x\nA,Aki,Mori,tôi,em,p002,x\nB,Ken,Mori,ta,lão,*,x").pronouns();
        check(Q1SpeakerSidecar.compare("「Ta đợi ngươi」",3,label,extra).status().equals("CLEAN")); // wildcard only if no scoped exact row
        check(Q1SpeakerSidecar.compare("「Ta đợi ngươi」",2,label,extra).candidate()); // exact wins over default
        check(Q1SpeakerSidecar.compare("「Lão đợi tôi」",2,label,extra).candidate()); // table atomic call absent in historical vocabulary
        rejects("[2] C1 > @NARRATOR",Set.of(2),roster);
        check(Q1SpeakerSidecar.parse("[2] @NARRATOR > ?",Set.of(2),roster).get(2).speaker().equals("@NARRATOR"));
        check(Q1SpeakerSidecar.quotation("「A』")==null);
        System.out.println("Q1 synthetic PASS="+passed+" FAIL=0");
    }
}
