package com.ml.tblandroidtxt;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class ReliabilityV43IntegrationTest {
    private AppSettings settings(){AppSettings s=new AppSettings();s.maxAttempts=3;s.maxOutputTokens=100;s.stopOnCostLimit=false;return s;}
    private List<String> sources(int n){ArrayList<String>x=new ArrayList<>();for(int i=0;i<n;i++)x.add("日本語"+i);return x;}
    @Test public void scenarioA_tenChunksCompleteInOrder(){ReliabilityScenarioRunner.State st=ReliabilityScenarioRunner.create(sources(10));ReliabilityScenarioRunner.run(st,(i,a)->new ReliabilityScenarioRunner.Reply("T"+i+"\n","stop",2,.01),settings());assertEquals("COMPLETED",st.chunks.get(9).status);assertEquals("T0\nT1\nT2\nT3\nT4\nT5\nT6\nT7\nT8\nT9\n",st.output());}
    @Test public void scenarioB_onlyEmptyMiddleChunkRetries(){ReliabilityScenarioRunner.State st=ReliabilityScenarioRunner.create(sources(10));int[]calls=new int[10];ReliabilityScenarioRunner.run(st,(i,a)->{calls[i]++;return new ReliabilityScenarioRunner.Reply(i==4&&a==1?"":"T"+i+"\n","stop",2,.01);},settings());for(int i=0;i<10;i++)assertEquals(i==4?2:1,calls[i]);assertEquals(1,count(st.output(),"T4\n"));}
    @Test public void scenarioC_processDeathResumesFirstIncompleteWithoutRebillingCompleted(){ReliabilityScenarioRunner.State st=ReliabilityScenarioRunner.create(sources(6));for(int i=0;i<3;i++){st.chunks.get(i).status="COMPLETED";st.chunks.get(i).accepted="T"+i+"\n";}st.chunks.get(3).status="REQUESTING";st.chunks.get(3).attempts=1;ReliabilityScenarioRunner.recoverAfterProcessDeath(st);int[]calls=new int[6];ReliabilityScenarioRunner.run(st,(i,a)->{calls[i]++;return new ReliabilityScenarioRunner.Reply("T"+i+"\n","stop",2,.01);},settings());assertEquals(0,calls[0]+calls[1]+calls[2]);assertEquals(1,calls[3]);assertEquals("COMPLETED",st.chunks.get(5).status);}
    @Test public void scenarioD_lengthIsNotAccepted(){ReliabilityScenarioRunner.State st=ReliabilityScenarioRunner.create(sources(1));ReliabilityScenarioRunner.run(st,(i,a)->new ReliabilityScenarioRunner.Reply("partial","length",100,.01),settings());assertEquals("RETRYABLE_ERROR",st.chunks.get(0).status);assertTrue(st.chunks.get(0).accepted.isEmpty());}
    @Test public void scenarioE_changedInputIsBlocked(){assertEquals("INPUT_HASH_MISMATCH",ResumeGuard.verifyInput(HashUtil.sha256("old"),"new"));}
    @Test public void scenarioF_budgetRetainsCurrentThenPausesBeforeNext(){AppSettings s=settings();s.stopOnCostLimit=true;s.costLimitUsd=.05;ReliabilityScenarioRunner.State st=ReliabilityScenarioRunner.create(sources(3));ReliabilityScenarioRunner.run(st,(i,a)->new ReliabilityScenarioRunner.Reply("T"+i+"\n","stop",2,.06),s);assertEquals("COMPLETED",st.chunks.get(0).status);assertTrue(st.paused);assertEquals("PENDING",st.chunks.get(1).status);}
    @Test public void scenarioG_onlyAcceptedCandidateEntersOutput(){ReliabilityScenarioRunner.State st=ReliabilityScenarioRunner.create(sources(1));ReliabilityScenarioRunner.Entry e=st.chunks.get(0);e.candidate="first\n";ReliabilityScenarioRunner.acceptCandidate(e,"second\n");assertEquals("second\n",st.output());assertFalse(st.output().contains("first"));}
    @Test public void failedIntegrityCannotBecomeCompleted(){List<TranslationRepository.ChunkRow>rows=new ArrayList<>();TranslationRepository.ChunkRow r=new TranslationRepository.ChunkRow();r.index=0;r.source="a";r.translated="";r.status="COMPLETED";r.startOffset=0;r.endOffset=1;r.sourceHash=HashUtil.sha256("a");rows.add(r);assertEquals("FAILED_INTEGRITY_CHECK",OutputIntegrityAudit.audit(rows).status());}
    private int count(String s,String part){int n=0,p=0;while((p=s.indexOf(part,p))>=0){n++;p+=part.length();}return n;}
}
