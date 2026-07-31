package com.ml.tblandroidtxt;

import static org.junit.Assert.*;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EditorialReleaseInstrumentedTest {
    @Test public void bundleRedactsInputsAndReleaseIsRecorded() throws Exception {
        EditorialRepository repo=new EditorialRepository(ApplicationProvider.getApplicationContext());long projectId=-1;
        try {
            EditorialRepository.Project p=new EditorialRepository.Project();p.seriesName="QA Release "+System.nanoTime();p.volumeName="V1";p.workflowHash=HashUtil.sha256("workflow");projectId=repo.createProject(p);
            long chapterId=repo.createChapter(projectId,"C1","Chapter",Arrays.asList(asset(EditorialWorkflowV5.AssetRole.RAW,"RAW_SECRET"),asset(EditorialWorkflowV5.AssetRole.DRAFT,"DRAFT_SECRET"),asset(EditorialWorkflowV5.AssetRole.GLOSSARY,"GLOSSARY_SECRET"),asset(EditorialWorkflowV5.AssetRole.PRONOUN,"PRONOUN_SECRET")));
            long run=repo.createRun(chapterId,"L3","provider","model",HashUtil.sha256("PROMPT_SECRET"),p.workflowHash,"{\"secret\":\"INPUT_SECRET\"}");String finalText="FINAL_PUBLIC",g="{\"coverage\":\"CLOSED\",\"fidelity\":\"CLOSED\",\"referenceVoice\":\"CLOSED\",\"continuityStructure\":\"CLOSED\",\"change\":\"CLOSED\"}",json="{\"schema\":\"editorial-v5-evidence-1\",\"run\":\"L3\",\"chapterId\":\""+chapterId+"\",\"output\":\""+finalText+"\",\"outputComplete\":true,\"changeSet\":[],\"crossSceneVoiceAudit\":\"CLOSED\",\"finalReadThrough\":\"CLOSED\",\"gates\":"+g+"}";
            repo.saveEvidence(run,"FINAL_QA_JSON",json);repo.saveEvidence(run,"FINAL_QA_TEXT",finalText);for(EditorialWorkflowV5.Gate gate:EditorialWorkflowV5.Gate.values())repo.saveGate(run,gate,EditorialWorkflowV5.GateStatus.CLOSED,g);repo.updateRunState(run,"CLOSED");advance(repo,chapterId);
            EditorialReleaseBundle.Result bundle=EditorialReleaseBundle.build(repo,chapterId,123L);Map<String,String> files=unzip(bundle.zip);assertEquals(finalText,files.get("FINAL_QA.txt"));String evidence=files.get("EVIDENCE_REDACTED.json");assertTrue(evidence.contains("hash-length-status-only"));for(String secret:new String[]{"RAW_SECRET","DRAFT_SECRET","GLOSSARY_SECRET","PRONOUN_SECRET","INPUT_SECRET","PROMPT_SECRET"})assertFalse(evidence.contains(secret));assertTrue(files.get("SHA256SUMS.txt").contains(HashUtil.sha256(finalText)));
            repo.recordRelease(chapterId,run,bundle.manifest,bundle.sha256);assertEquals(EditorialWorkflowV5.ChapterState.RELEASED,repo.getChapter(chapterId).state);assertEquals(bundle.sha256,repo.evidencePayload(run,"RELEASE_BUNDLE_SHA256"));
        } finally {if(projectId>0)repo.deleteProject(projectId);repo.close();}
    }
    @Test public void releaseRejectsOpenGate() throws Exception {EditorialRepository repo=new EditorialRepository(ApplicationProvider.getApplicationContext());long projectId=-1;try{EditorialRepository.Project p=new EditorialRepository.Project();p.seriesName="QA Gate "+System.nanoTime();p.volumeName="V1";p.workflowHash=HashUtil.sha256("w");projectId=repo.createProject(p);long chapterId=repo.createChapter(projectId,"C2","",Arrays.asList(asset(EditorialWorkflowV5.AssetRole.RAW,"R"),asset(EditorialWorkflowV5.AssetRole.DRAFT,"D"),asset(EditorialWorkflowV5.AssetRole.GLOSSARY,"G"),asset(EditorialWorkflowV5.AssetRole.PRONOUN,"P")));long run=repo.createRun(chapterId,"L3","p","m","h",p.workflowHash,"{}");String json="{\"schema\":\"editorial-v5-evidence-1\",\"run\":\"L3\",\"chapterId\":\""+chapterId+"\",\"output\":\"F\",\"outputComplete\":true,\"changeSet\":[],\"crossSceneVoiceAudit\":\"CLOSED\",\"finalReadThrough\":\"CLOSED\",\"gates\":{\"coverage\":\"CLOSED\",\"fidelity\":\"CLOSED\",\"referenceVoice\":\"CLOSED\",\"continuityStructure\":\"CLOSED\",\"change\":\"CLOSED\"}}";repo.saveEvidence(run,"FINAL_QA_JSON",json);repo.saveEvidence(run,"FINAL_QA_TEXT","F");for(EditorialWorkflowV5.Gate gate:EditorialWorkflowV5.Gate.values())repo.saveGate(run,gate,gate==EditorialWorkflowV5.Gate.CHANGE?EditorialWorkflowV5.GateStatus.OPEN:EditorialWorkflowV5.GateStatus.CLOSED,"{}");repo.updateRunState(run,"CLOSED");advance(repo,chapterId);try{EditorialReleaseBundle.build(repo,chapterId,1L);fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("gates"));}assertEquals(EditorialWorkflowV5.ChapterState.RELEASE_READY,repo.getChapter(chapterId).state);}finally{if(projectId>0)repo.deleteProject(projectId);repo.close();}}
    private static EditorialRepository.AssetSnapshot asset(EditorialWorkflowV5.AssetRole role,String text){return new EditorialRepository.AssetSnapshot(role,"memory://"+role,role+".txt",text);}
    private static void advance(EditorialRepository r,long id){EditorialWorkflowV5.ChapterState[] s={EditorialWorkflowV5.ChapterState.L1_READY,EditorialWorkflowV5.ChapterState.L1_RUNNING,EditorialWorkflowV5.ChapterState.L1_CLOSED,EditorialWorkflowV5.ChapterState.L2_READY,EditorialWorkflowV5.ChapterState.L2_RAW_MAPPING,EditorialWorkflowV5.ChapterState.L2_RUNNING,EditorialWorkflowV5.ChapterState.L2_CLOSED,EditorialWorkflowV5.ChapterState.L3_READY,EditorialWorkflowV5.ChapterState.L3_INDEPENDENT_RUNNING,EditorialWorkflowV5.ChapterState.L3_REPORT_REVIEW,EditorialWorkflowV5.ChapterState.L3_RUNNING,EditorialWorkflowV5.ChapterState.RELEASE_READY};for(int i=0;i<s.length-1;i++)r.transitionChapter(id,s[i],s[i+1]);}
    private static Map<String,String> unzip(byte[] bytes)throws Exception{Map<String,String> out=new HashMap<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;byte[] buffer=new byte[1024];while((e=zip.getNextEntry())!=null){ByteArrayOutputStream value=new ByteArrayOutputStream();int n;while((n=zip.read(buffer))>=0)value.write(buffer,0,n);out.put(e.getName(),value.toString(StandardCharsets.UTF_8.name()));}}return out;}
}
