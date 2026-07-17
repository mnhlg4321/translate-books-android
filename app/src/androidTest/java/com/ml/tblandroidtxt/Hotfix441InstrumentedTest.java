package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Hotfix441InstrumentedTest {
    private AppSettings settings(){AppSettings s=new AppSettings();s.apiKey="fake-key";s.provider="fake";s.model="fake/model";s.maxAttempts=1;s.refineAfter=false;return AppValidator.normalize(s);}
    private static Chunk chunk(int index,String source){return new Chunk(index,index*source.length(),(index+1)*source.length(),"",source,"","");}
    private static OpenAICompatibleClient.ChatResult response(String text){OpenAICompatibleClient.ChatResult r=new OpenAICompatibleClient.ChatResult();r.content="<TRANSLATION>"+text+"</TRANSLATION>";r.promptTokens=120;r.completionTokens=20;r.totalTokens=140;r.usageReported=true;r.providerResponseId="fake-response";r.finishReason="stop";return r;}

    @Test public void fakeProviderCompletesOrderedMultiChunkPlan() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();TranslationRepository repo=new TranslationRepository(context);AppSettings s=settings();
        List<Chunk> chunks=Arrays.asList(chunk(0,"第一の長い原文です。"),chunk(1,"第二の長い原文です。"));
        long job=repo.createJob("content://fake/input","content://fake/output","fake.txt",s,chunks,"input-hash","settings-hash");AtomicInteger calls=new AtomicInteger();
        TranslationEngine engine=new TranslationEngine(context,repo,(settings,prompt,limit,id,observer)->{observer.onRequestBodyStarted();observer.onRequestBodySent(100);observer.onResponseHeaders(200);return response("Bản dịch kết quả số "+(calls.incrementAndGet()));});
        String previous="";for(Chunk c:chunks){String out=engine.translateWithRetry(job,c,previous,s,()->false,(r,x)->{},m->{});repo.commitValidatedChunk(job,c.index,out,"","FAKE_PROVIDER");previous=out;}
        String assembled=repo.assembleOutput(job,false);assertTrue(assembled.indexOf("Bản dịch kết quả số 1")<assembled.indexOf("Bản dịch kết quả số 2"));assertEquals(2,calls.get());
        for(Chunk c:chunks){List<TranslationRepository.AttemptRow> attempts=repo.getAttemptRows(job,c.index);assertEquals(1,attempts.size());assertEquals("ACCEPTED",attempts.get(0).status);assertEquals("fake-response",attempts.get(0).providerResponseId);}
        repo.close();
    }

    @Test public void sentWithoutDurableResponsePausesForReview() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();TranslationRepository repo=new TranslationRepository(context);AppSettings s=settings();Chunk c=chunk(0,"送信状態を検査する十分な原文です。");long job=repo.createJob("content://fake/unknown","content://fake/output","unknown.txt",s,Arrays.asList(c),"unknown-input","unknown-settings");
        TranslationEngine engine=new TranslationEngine(context,repo,(settings,prompt,limit,id,observer)->{observer.onRequestBodyStarted();observer.onRequestBodySent(100);throw new java.io.IOException("simulated process/network loss");});
        try{engine.translateWithRetry(job,c,"",s,()->false,(r,x)->{},m->{});fail("delivery should be unknown");}catch(TranslationEngine.DeliveryUnknownException expected){assertTrue(expected.getMessage().contains("automatic retry"));}
        assertEquals("DELIVERY_UNKNOWN",repo.getChunkRows(job).get(0).status);assertEquals("NEEDS_REVIEW",repo.getJob(job).status);repo.close();
    }

    @Test public void responseSurvivesDeathBeforeCommitForRevalidation() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();TranslationRepository repo=new TranslationRepository(context);AppSettings s=settings();Chunk c=chunk(0,"応答保存を検査する十分な原文です。");long job=repo.createJob("content://fake/recovery","content://fake/output","recovery.txt",s,Arrays.asList(c),"recovery-input","recovery-settings");
        TranslationEngine engine=new TranslationEngine(context,repo,(settings,prompt,limit,id,observer)->{observer.onRequestBodyStarted();observer.onRequestBodySent(80);observer.onResponseHeaders(200);return response("Bản dịch đã được lưu bền vững");});
        assertFalse(engine.translateWithRetry(job,c,"",s,()->false,(r,x)->{},m->{}).isEmpty());assertEquals("VALIDATING",repo.getChunkRows(job).get(0).status);repo.close();
        TranslationRepository reopened=new TranslationRepository(context);reopened.recoverInterruptedState();TranslationRepository.ChunkRow row=reopened.getChunkRows(job).get(0);assertEquals("RESPONSE_RECEIVED",row.status);assertTrue(row.persistedResponse.contains("Bản dịch đã được lưu bền vững"));reopened.close();
    }

    @Test public void freshStartSessionNeverResumesAnOlderPreparedJob() {
        Context context=ApplicationProvider.getApplicationContext();TranslationRepository repo=new TranslationRepository(context);AppSettings s=settings();
        Chunk c=chunk(0,"fresh start source text");String batch="batch-"+UUID.randomUUID();String firstSession=UUID.randomUUID().toString(),secondSession=UUID.randomUUID().toString();
        long first=repo.createJobFromPrepared(batch,firstSession,0,"content://fake/fresh","content://fake/out1","fresh.txt",s,Arrays.asList(c),"input","settings");
        repo.commitValidatedChunk(first,0,"old accepted output","","FAKE_PROVIDER");
        long duplicateDispatch=repo.createJobFromPrepared(batch,firstSession,0,"content://fake/fresh","content://fake/out1","fresh.txt",s,Arrays.asList(c),"input","settings");
        long fresh=repo.createJobFromPrepared(batch,secondSession,0,"content://fake/fresh","content://fake/out2","fresh.txt",s,Arrays.asList(c),"input","settings");
        assertEquals(first,duplicateDispatch);assertNotEquals(first,fresh);assertEquals("PENDING",repo.getChunkRows(fresh).get(0).status);assertEquals("",repo.getChunkRows(fresh).get(0).translated);repo.close();
    }
}
