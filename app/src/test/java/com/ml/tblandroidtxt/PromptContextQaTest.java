package com.ml.tblandroidtxt;

import org.junit.Test;
import static org.junit.Assert.*;

public class PromptContextQaTest {
    private AppSettings settings() { AppSettings s=new AppSettings(); s.glossaryInjectLimit=3; s.pronounInjectLimit=3; return s; }
    @Test public void glossarySupportsDirectAliasGlobalNearNameAndDedup() {
        String glossary="Hero => Anh hùng [term] | Yuusha\nAnn => An [character]\nAnna => Anna [character]\nWorld => Thế giới [global]\nUnused => Bỏ [term]\nHero => Anh hùng [term]";
        PromptContextBuilder.ContextBlock b=PromptContextBuilder.build(glossary,"","Yuusha met Anna",settings());
        assertTrue(b.glossary.contains("Hero")); assertTrue(b.glossary.contains("Anna")); assertTrue(b.glossary.contains("World"));
        assertFalse(b.glossary.contains("Ann =>")); assertFalse(b.glossary.contains("Unused"));
        assertEquals(3,b.glossaryCount);
    }
    @Test public void pronounOmitsContextOnlyCharacterAndKeepsGlobalWithoutNearNameCollision() {
        String rules="Alice -> Bob: chị / em\nAl -> Bo: tôi / bạn\nGLOBAL: Preserve established speaker relation";
        PromptContextBuilder.ContextBlock b=PromptContextBuilder.buildWithRuleContext("",rules,"彼女は言った。","Alice entered.\n彼女は言った。",settings());
        assertFalse(b.pronouns.contains("Alice")); assertTrue(b.pronouns.contains("GLOBAL")); assertFalse(b.pronouns.contains("Al ->"));
    }
    @Test public void pronounKeepsMainCharacterFullAndCompactsSecondaryCharacter() {
        String rules="Alice,Bob,character; nữ; lời kể dùng cô/Alice; tự xưng tôi; ghi chú dài không cần cho vai phụ\n"
                + "Carol,Caroline,character; nữ; lời kể dùng cô/Carol; tự xưng mình; ghi chú dài không cần cho vai phụ";
        String chunk="Alice Alice Alice bước vào. Carol đứng ở cuối phòng.";
        PromptContextBuilder.ContextBlock b=PromptContextBuilder.build("",rules,chunk,settings());
        assertEquals(2,b.pronounCount);
        assertTrue(b.pronouns.contains("ghi chú dài không cần cho vai phụ"));
        assertTrue(b.pronouns.contains("Carol => Caroline ["));
        assertFalse(b.pronouns.contains("Carol → Caroline: character"));
    }
    @Test public void promptBreakdownExposesRequestedFiveParts() {
        AppSettings s=settings(); s.translationInstructions="Translate naturally";
        s.glossaryText="Alice => Alice [character]";
        s.pronounText="Alice,Bob,character; nữ; lời kể dùng cô/Alice";
        CostEstimator.Estimate e=CostEstimator.estimate("Alice said hello.",s);
        assertTrue(e.breakdownSource>0); assertTrue(e.breakdownInstruction>0);
        assertTrue(e.breakdownGlossary>0); assertTrue(e.breakdownPronoun>0);
        assertTrue(e.breakdownContext>=0);
    }
    @Test public void beforeAndAfterContextBothSurviveBudget() {
        AppSettings s=settings(); s.contextChars=40; s.optimizationPreset="balanced";
        PromptPlan p=PromptPlan.translation(new Chunk(0,"1234567890-before-tail","main","after-head-1234567890"),"",s);
        assertTrue(p.surroundingContext.contains("Before:")); assertTrue(p.surroundingContext.contains("After:"));
    }

    @Test public void promptPlanReportsCountsFromTheExactInjectedLocks() {
        AppSettings s=settings();
        s.glossaryText="Alice => An [character]\nWorld => Thế giới [global]\nUnused => Bỏ [term]";
        s.pronounText="Alice -> Bob: chị / em\nGLOBAL: Preserve established speaker relation";
        Chunk chunk=new Chunk(0,"Bob appeared earlier.","Alice entered the World.","");

        PromptPlan p=PromptPlan.translation(chunk,"",s);
        PromptContextBuilder.ContextBlock exact=PromptContextBuilder.buildWithRuleContext(
                s.glossaryText,s.pronounText,chunk.mainContent,chunk.contextBefore+"\n"+chunk.mainContent,s);

        assertEquals(exact.glossaryCount,p.glossaryLockCount);
        assertEquals(exact.pronounCount,p.pronounLockCount);
        assertTrue(p.prompt.system.contains(p.glossary.trim()));
        assertTrue(p.prompt.system.contains(p.pronoun.trim()));
    }

    @Test public void emptyPromptLocksReportZeroInsteadOfUnknown() {
        PromptPlan p=PromptPlan.translation(new Chunk(0,"","No matching rules.",""),"",settings());
        assertEquals(0,p.glossaryLockCount);
        assertEquals(0,p.pronounLockCount);
    }
}
