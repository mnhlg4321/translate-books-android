package com.ml.tblandroidtxt;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class EditorialL1ContextBuilderTest {
    private static EditorialRepository.AssetSnapshot asset(EditorialWorkflowV5.AssetRole role,String text){return new EditorialRepository.AssetSnapshot(role,"memory://"+role,role+".txt",text);}
    @Test public void createsIsolatedL1PackageWithRawMapBeforeDraft() {
        EditorialL1ContextBuilder.Context context=EditorialL1ContextBuilder.build(7,Arrays.asList(asset(EditorialWorkflowV5.AssetRole.RAW,"raw first\nraw last"),asset(EditorialWorkflowV5.AssetRole.DRAFT,"draft"),asset(EditorialWorkflowV5.AssetRole.GLOSSARY,"terms"),asset(EditorialWorkflowV5.AssetRole.PRONOUN,"rules")));
        assertTrue(context.prompt.user.indexOf("RAW MAP")<context.prompt.user.indexOf("DRAFT:")); assertTrue(context.manifestJson.contains("RAW")); assertTrue(context.rawMapJson.contains("p000001"));
    }
    @Test public void refusesForbiddenPriorRunFiles() {
        try { EditorialL1ContextBuilder.build(7,Arrays.asList(asset(EditorialWorkflowV5.AssetRole.RAW,"raw"),asset(EditorialWorkflowV5.AssetRole.DRAFT,"draft"),asset(EditorialWorkflowV5.AssetRole.GLOSSARY,"g"),asset(EditorialWorkflowV5.AssetRole.PRONOUN,"p"),asset(EditorialWorkflowV5.AssetRole.REPORT_L1,"old"))); fail(); } catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("Forbidden"));}
    }
    @Test public void blocksRatherThanTruncatesLongChapter() {
        StringBuilder longRaw=new StringBuilder();while(longRaw.length()<=EditorialL1ContextBuilder.MAX_INLINE_SOURCE_CHARS)longRaw.append('x');
        try { EditorialL1ContextBuilder.build(7,Arrays.asList(asset(EditorialWorkflowV5.AssetRole.RAW,longRaw.toString()),asset(EditorialWorkflowV5.AssetRole.DRAFT,"draft"),asset(EditorialWorkflowV5.AssetRole.GLOSSARY,"g"),asset(EditorialWorkflowV5.AssetRole.PRONOUN,"p"))); fail(); } catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("segmentation"));}
    }
}
