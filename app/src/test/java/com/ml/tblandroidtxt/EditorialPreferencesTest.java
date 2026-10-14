package com.ml.tblandroidtxt;
import org.junit.Test;
import java.util.*;
import java.math.BigDecimal;
import com.ml.tblandroidtxt.editorial.api.fix.*;
import static org.junit.Assert.*;
public class EditorialPreferencesTest {
    @Test public void allDisabledServiceNeverCallsProviderAndDoesNotRestoreMarks() throws Exception {
        EditorialPreferences p=new EditorialPreferences();p.address=false;p.glossary=false;p.kana=false;p.punctuation=false;
        EditorialPairSource source=EditorialPairRunServiceTest.source(1);
        EditorialApiProvider provider=new EditorialApiProvider(){
            public com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.StepResponse call(com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.Request r,String m,int out,long timeout){throw new AssertionError("Disabled checks must not call provider");}
            public void cancel(){}
        };
        InMemoryPairRunStore store=new InMemoryPairRunStore();EditorialPairRunService service=new EditorialPairRunService(store,provider,null,1000L);
        EditorialPairModels.PairRun run=service.prepare(1,source,"","",p.freeze(null),"fake-model","Vietnamese",new BigDecimal("0.5"),EditorialPairModels.ARM_CHUNK);
        service.execute(run.id,null);assertEquals(0,store.getRun(run.id).calls);
        assertEquals(source.draftRows.get(0),store.items(run.id).get(0).candidateText);
        assertEquals(0,EditorialPairPreview.of(source,List.of(),"",null,p).fixPointCount);
    }
    @Test public void defaultsAndSettingsExportRoundTripPreserveExistingBehavior() {
        AppSettings settings=new AppSettings();assertTrue(settings.editorial.address&&settings.editorial.glossary&&settings.editorial.kana&&settings.editorial.punctuation);
        settings.editorial.model="fake-model";settings.editorial.reasoning="high";settings.editorial.cap=new BigDecimal("0.25");settings.editorial.address=false;
        AppSettings loaded=SettingsStore.fromJson(SettingsStore.toJson(settings));assertEquals(settings.editorial.toJson(),loaded.editorial.toJson());
        AppSettings copied=settings.copy();copied.editorial.address=true;assertFalse(settings.editorial.address);
    }
    @Test public void frozenChoicesIgnoreSubsequentSettingsAndDoNotLeakIntoPronounCueFields() {
        EditorialPreferences p=new EditorialPreferences();p.glossary=false;String frozen=p.freeze(null);p.glossary=true;
        assertFalse(EditorialPreferences.frozen(frozen).glossary);assertEquals(Set.of("from","speaker","target"),EditorialPairSnapshot.cues(frozen));
        assertTrue(EditorialPreferences.frozen("from,speaker,target").glossary);
    }
    @Test public void allDisabledMeansNoModelPointsOrRawPunctuationRepairs() {
        FixPoint point=new FixPoint(1,Set.of(FixPoint.Type.ADDRESS,FixPoint.Type.GLOSSARY),0,"Tôi đi.","こんにちは。",List.of("ADDRESS: a","GLOSSARY: b"));
        FixPointFinder.Result found=new FixPointFinder.Result(List.of(point),0,1,1);
        EditorialPreferences p=new EditorialPreferences();p.address=false;p.glossary=false;p.kana=false;p.punctuation=false;
        assertTrue(p.filter(found).points().isEmpty());assertTrue(p.filter(found).punctuation().isEmpty());
        p.glossary=true;FixPoint retained=p.filter(found).points().get(0);assertEquals(Set.of(FixPoint.Type.GLOSSARY),retained.types());assertEquals(List.of("GLOSSARY: b"),retained.rules());
    }
    @Test public void globalModelAndCapMayBeOverriddenByComboWithoutSchemaChange() {
        AppSettings app=new AppSettings();app.editorial.model="editorial-model";app.editorial.cap=new BigDecimal("0.5");EditorialApiCombo combo=new EditorialApiCombo();
        assertEquals("editorial-model",EditorialApiUiController.effectiveModel(combo,app));assertEquals(app.editorial.cap,EditorialApiUiController.effectiveCap(combo,app));
        EditorialApiCombo.Settings own=new EditorialApiCombo.Settings();own.model="combo-model";own.capOverride=true;own.maxUsdPerChapter=new BigDecimal("0.2");combo.settingsJson=own.toJson();
        assertEquals("combo-model",EditorialApiUiController.effectiveModel(combo,app));assertEquals(own.maxUsdPerChapter,EditorialApiUiController.effectiveCap(combo,app));
    }
}
