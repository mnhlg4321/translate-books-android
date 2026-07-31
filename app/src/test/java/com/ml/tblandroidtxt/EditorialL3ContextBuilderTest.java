package com.ml.tblandroidtxt;import org.junit.Test;import static org.junit.Assert.*;
public class EditorialL3ContextBuilderTest {
 private static final String E="{\"chapterId\":\"7\",\"scenes\":[{\"id\":\"s1\",\"rawStart\":\"p1\",\"rawEnd\":\"p2\",\"coverage\":\"ALIGNED\",\"fidelity\":\"CLOSED\",\"voice\":\"CLOSED\",\"status\":\"CLOSED\"}],\"issues\":[],\"crossSceneVoiceAudit\":\"CLOSED\",\"status\":\"CLOSED\"}";
 @Test public void independentContextCannotSeeReport(){PromptPair p=EditorialL3ContextBuilder.independent(7,"RAW","VI","G","P");assertTrue(p.user.contains("RAW"));assertTrue(p.user.contains("VI"));assertFalse(p.user.contains("REPORT_L1"));}
 @Test public void reportUnlockRequiresClosedIndependentEvidence(){PromptPair p=EditorialL3ContextBuilder.reportReview(7,"RAW","VI","G","P",E,"REPORT_SECRET");assertTrue(p.user.contains("REPORT_SECRET"));try{EditorialL3ContextBuilder.reportReview(7,"RAW","VI","G","P",E.replace("\"status\":\"CLOSED\"}","\"status\":\"OPEN\"}"),"REPORT_SECRET");fail();}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("independent"));}}
}
