package com.ml.tblandroidtxt;
import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import com.ml.tblandroidtxt.EditorialPairModels.*;
import com.ml.tblandroidtxt.editorial.api.PairStates.*;
import static org.junit.Assert.*;

/** Real service + SQLite + fake transport; opt-in seed/verify is followed by an external force-stop. */
@RunWith(AndroidJUnit4.class)
public class EditorialForegroundInstrumentedTest {
    private Context context;
    private ActivityScenario<MainActivity> activity;
    private final List<Long> runs=new ArrayList<>(),combos=new ArrayList<>();
    private static final String PREF="v422_foreground_test";
    @Before public void setUp(){context=ApplicationProvider.getApplicationContext();activity=ActivityScenario.launch(MainActivity.class);}
    @After public void cleanup() throws Exception {
        for(long id:runs){EditorialPairForegroundService.cancelRun(id);awaitStopped(id);try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){store.deleteRun(id);}}
        for(long id:combos)try(SqliteEditorialApiStore store=new SqliteEditorialApiStore(context)){store.deleteCombo(id);}
        EditorialApiUiController.providerOverride=null;if(activity!=null)activity.close();
    }
    private long prepare(int count) throws Exception {
        EditorialApiCombo combo=new EditorialApiCombo();combo.name="v422-foreground-synthetic-"+UUID.randomUUID();
        try(SqliteEditorialApiStore store=new SqliteEditorialApiStore(context)){store.insertCombo(combo);}combos.add(combo.id);
        EditorialPairSource base=EditorialPairTestData.source(count);List<String> draft=new ArrayList<>();for(String row:base.draftRows)draft.add(row.replace("dòng 3:","dòng 3: かな"));
        EditorialPairSource source=new EditorialPairSource(base.kind,base.ref,base.label,base.chapterId,base.rawRows,draft,base.lineageIssues);
        try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){
            PairRun run=new EditorialPairRunService(store,new FakeEditorialApiProvider((r,i)->{throw new AssertionError("Preparation must not call provider");}),null,1000L)
                    .prepare(combo.id,source,"","",new EditorialPreferences().freeze(null),"fake-model","Vietnamese",new BigDecimal("2"),EditorialPairModels.ARM_CHUNK);
            runs.add(run.id);return run.id;
        }
    }
    private FakeEditorialApiProvider keep(){return new FakeEditorialApiProvider((r,i)->FakeEditorialApiProvider.text(EditorialPairTestData.targetedAnswer(r,i,(n,d)->d),"stop"));}
    private void awaitStopped(long id) throws Exception {
        long until=System.currentTimeMillis()+30000;while(EditorialPairForegroundService.isActive(id)&&System.currentTimeMillis()<until)Thread.sleep(25);
        assertFalse("foreground worker did not stop",EditorialPairForegroundService.isActive(id));
    }
    @Test public void serviceCompletesAndDuplicateStartDoesNotResend() throws Exception {
        long id=prepare(3);FakeEditorialApiProvider provider=keep();EditorialApiUiController.providerOverride=provider;
        EditorialPairForegroundService.start(context,id);EditorialPairForegroundService.start(context,id);awaitStopped(id);
        assertEquals(3,provider.requests.size());try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){assertEquals(RunState.FINAL_ELIGIBLE,store.getRun(id).state);assertEquals(3,EditorialJobPresentation.done(store.items(id)));}
    }
    @Test public void cancelAfterCurrentChunkAndResumeOnlyPendingChunks() throws Exception {
        long id=prepare(3);FakeEditorialApiProvider first=keep();first.beforeAnswer=()->EditorialPairForegroundService.cancelRun(id);EditorialApiUiController.providerOverride=first;
        EditorialPairForegroundService.start(context,id);awaitStopped(id);assertEquals(1,first.requests.size());
        try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){assertTrue(EditorialJobPresentation.canResume(store.getRun(id),store.items(id),false));}
        FakeEditorialApiProvider resumed=keep();EditorialApiUiController.providerOverride=resumed;EditorialPairForegroundService.start(context,id);awaitStopped(id);assertEquals(2,resumed.requests.size());
    }
    private static void collectText(android.view.View view,StringBuilder out){
        if(view instanceof android.widget.TextView)out.append(((android.widget.TextView)view).getText()).append('\n');
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)collectText(group.getChildAt(i),out);}
    }
    @Test public void jobsTabShowsTheEditorialRunCard() throws Exception {
        long id=prepare(3);String name;
        try(SqliteEditorialApiStore store=new SqliteEditorialApiStore(context)){name=store.listCombos().stream().filter(c->c.id==combos.get(0)).findFirst().get().name;}
        StringBuilder text=new StringBuilder();
        activity.onActivity(a->{a.invalidatePage("Jobs");a.switchTab("Jobs");collectText(a.pageCache.get("Jobs"),text);});
        assertTrue("the Jobs tab lists the Editorial run of "+name+" (run "+id+"):\n"+text,text.toString().contains(name)&&text.toString().contains("Mở kết quả"));
    }
    private String phase(){return InstrumentationRegistry.getArguments().getString("v422_phase","");}
    @Test public void processReopenSeed() throws Exception {
        Assume.assumeTrue("seed".equals(phase()));long id=prepare(3);CountDownLatch secondSent=new CountDownLatch(1);
        FakeEditorialApiProvider provider=new FakeEditorialApiProvider((r,i)->{
            if(i==1){secondSent.countDown();try{Thread.sleep(120000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
            return FakeEditorialApiProvider.text(EditorialPairTestData.targetedAnswer(r,i,(n,d)->d),"stop");
        });EditorialApiUiController.providerOverride=provider;
        context.getSharedPreferences(PREF,0).edit().putLong("run",id).putLong("combo",combos.get(0)).commit();
        EditorialPairForegroundService.start(context,id);assertTrue(secondSent.await(15,TimeUnit.SECONDS));
        try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){assertEquals(PairState.ACCEPTED,store.items(id).get(0).state);assertEquals(PairState.E_SENT,store.items(id).get(1).state);assertEquals(PairState.IMPORTED,store.items(id).get(2).state);}
        runs.clear();combos.clear(); // Keep only this synthetic fixture for the shell force-stop and verify phase.
    }
    @Test public void processReopenVerify() throws Exception {
        Assume.assumeTrue("verify".equals(phase()));long id=context.getSharedPreferences(PREF,0).getLong("run",0);assertTrue(id>0);
        try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){
            EditorialPairRunService reader=new EditorialPairRunService(store,new FakeEditorialApiProvider((r,i)->{throw new AssertionError("Reopening must not call provider");}),null,1000L);
            reader.recover(id);assertEquals(PairState.ACCEPTED,store.items(id).get(0).state);assertEquals(PairState.UNKNOWN,store.items(id).get(1).state);
            assertTrue(EditorialJobPresentation.canResume(store.getRun(id),store.items(id),false));
        }
        FakeEditorialApiProvider resumed=keep();EditorialApiUiController.providerOverride=resumed;EditorialPairForegroundService.start(context,id);awaitStopped(id);assertEquals(1,resumed.requests.size());
        try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){assertEquals(PairState.UNKNOWN,store.items(id).get(1).state);assertEquals(PairState.ACCEPTED,store.items(id).get(2).state);}
    }
    @Test public void processReopenCleanup(){
        Assume.assumeTrue("cleanup".equals(phase()));long id=context.getSharedPreferences(PREF,0).getLong("run",0),combo=context.getSharedPreferences(PREF,0).getLong("combo",0);
        try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(context)){if(id>0)store.deleteRun(id);}
        try(SqliteEditorialApiStore store=new SqliteEditorialApiStore(context)){if(combo>0)store.deleteCombo(combo);}
        context.getSharedPreferences(PREF,0).edit().clear().commit();
    }
}
