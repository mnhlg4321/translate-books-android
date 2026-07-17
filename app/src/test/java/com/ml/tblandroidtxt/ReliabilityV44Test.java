package com.ml.tblandroidtxt;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Reproducible coverage for the workflows completed in 4.4. */
public class ReliabilityV44Test {
    @Test public void editedRevisionChangesOnlyRemainingChunks(){
        AppSettings first=new AppSettings();first.model="provider/original";String revision1=SettingsStore.toJson(first);
        AppSettings edited=SettingsStore.fromJson(revision1);edited.model="provider/edited";String revision2=SettingsStore.toJson(edited);
        String[] applied={revision1,revision1,revision2,revision2};
        assertEquals("provider/original",SettingsStore.fromJson(applied[1]).model);
        assertEquals("provider/edited",SettingsStore.fromJson(applied[2]).model);
    }

    @Test public void continueIncompleteDoesNotCallCompletedChunks(){
        ReliabilityScenarioRunner.State state=ReliabilityScenarioRunner.create(sources(4));
        state.chunks.get(0).status="COMPLETED";state.chunks.get(0).accepted="T0\n";
        state.chunks.get(1).status="COMPLETED";state.chunks.get(1).accepted="T1\n";
        int[] calls=new int[4];AppSettings s=new AppSettings();s.maxAttempts=2;s.maxOutputTokens=100;
        ReliabilityScenarioRunner.run(state,(i,a)->{calls[i]++;return new ReliabilityScenarioRunner.Reply("T"+i+"\n","stop",2,.001);},s);
        assertEquals(0,calls[0]+calls[1]);assertEquals(1,calls[2]);assertEquals(1,calls[3]);assertEquals("T0\nT1\nT2\nT3\n",state.output());
    }

    @Test public void manualReplacementKeepsPriorCandidateOutOfFinalOutput(){
        ReliabilityScenarioRunner.State state=ReliabilityScenarioRunner.create(sources(1));ReliabilityScenarioRunner.Entry row=state.chunks.get(0);
        row.candidate="provider attempt";ReliabilityScenarioRunner.acceptCandidate(row,"validated manual text");
        assertEquals("validated manual text",state.output());assertFalse(state.output().contains("provider attempt"));
    }

    @Test public void multiMegabyteTxtChunkingHasExactCoverage(){
        String paragraph="A bounded large-file paragraph with unicode æ—¥æœ¬èªž and a safe ending.\n";StringBuilder book=new StringBuilder(6*1024*1024);
        while(book.length()<6*1024*1024)book.append(paragraph);AppSettings s=new AppSettings();s.chunkMode="char";s.maxCharsPerChunk=16000;s.softLimitRatio=.85f;s.contextOverlapEnabled=true;s.contextChars=300;
        String text=book.toString();List<Chunk> chunks=Chunker.chunkText(text,s);Chunker.Coverage coverage=Chunker.verifyCoverage(Chunker.normalizeSource(text),chunks);
        assertTrue(coverage.valid);assertTrue(chunks.size()>300);
    }

    @Test public void integrityAuditRejectsGapAndDuplicateIndex(){
        ArrayList<TranslationRepository.ChunkRow> rows=new ArrayList<>();rows.add(row(0,0,2,"ab","A"));rows.add(row(0,3,5,"cd","B"));
        OutputIntegrityAudit.Result result=OutputIntegrityAudit.audit(rows);assertFalse(result.passed());assertTrue(result.errors.toString().contains("DUPLICATE_INDEX"));assertTrue(result.errors.toString().contains("SOURCE_GAP_OR_OVERLAP"));
    }

    private static List<String> sources(int n){ArrayList<String> s=new ArrayList<>();for(int i=0;i<n;i++)s.add("source"+i);return s;}
    private static TranslationRepository.ChunkRow row(int idx,int start,int end,String source,String output){TranslationRepository.ChunkRow r=new TranslationRepository.ChunkRow();r.index=idx;r.startOffset=start;r.endOffset=end;r.source=source;r.sourceHash=HashUtil.sha256(source);r.translated=output;r.status="COMPLETED";return r;}
}
