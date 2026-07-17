package com.ml.tblandroidtxt;

import org.junit.Test;
import static org.junit.Assert.*;

public class Patch312Test {
    @Test public void contextFallbackChangesPayloadBudgetButKeepsMandatoryData() {
        AppSettings s=new AppSettings(); s.contextChars=600; s.maxOutputTokens=4096; s.maxTokensPerChunk=450;
        s.translationInstructions="Mandatory instruction"; s.glossaryText="Hero => Anh hùng [global]";
        AppSettings f=TranslationEngine.contextFallbackSettings(s);
        assertEquals(0,f.contextChars); assertTrue(f.maxOutputTokens < s.maxOutputTokens);
        assertEquals(s.translationInstructions,f.translationInstructions); assertEquals(s.glossaryText,f.glossaryText);
    }
    @Test public void readinessBlocksMissingCredentialAndBudgetOverflow() {
        AppSettings s=new AppSettings(); s.apiKey=""; s.model="anthropic/claude-sonnet-4.6";
        BenchmarkReadiness.Result missing=BenchmarkReadiness.check("source text",s,0.5);
        assertTrue(missing.blockers.contains("API_KEY_MISSING"));
        BenchmarkReadiness.Result noSource=BenchmarkReadiness.check("",s,0.5);
        assertTrue(noSource.blockers.contains("SOURCE_MISSING"));
    }
    @Test public void pronounConflictsAreReported() {
        AppSettings s=new AppSettings();
        PromptContextBuilder.MatchReport report=PromptContextBuilder.match("","Alice -> Bob: chị/em\nAlice -> Bob: tôi/bạn","Alice met Bob",s);
        assertTrue(report.warnings.toString().contains("Pronoun conflict"));
    }
}
