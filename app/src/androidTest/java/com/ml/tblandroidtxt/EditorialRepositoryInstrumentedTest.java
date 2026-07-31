package com.ml.tblandroidtxt;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

/** Uses unique data and always cleans it up so it does not alter user projects. */
public class EditorialRepositoryInstrumentedTest {
    @Test public void persistsProjectChapterAssetsRunSceneGateAndEvidence() {
        EditorialRepository repo = new EditorialRepository(ApplicationProvider.getApplicationContext());
        long projectId = -1;
        try {
            EditorialRepository.Project project = new EditorialRepository.Project();
            project.seriesName = "QA Editorial " + System.nanoTime(); project.volumeName = "V1"; project.workflowHash = HashUtil.sha256("v5");
            projectId = repo.createProject(project);
            EditorialRepository.AssetSnapshot raw = new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.RAW, "content://qa/raw", "raw.txt", "原文\n◇◇◇\n終わり");
            EditorialRepository.AssetSnapshot draft = new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.DRAFT, "content://qa/draft", "draft.txt", "Bản nháp");
            EditorialRepository.AssetSnapshot glossary = new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.GLOSSARY, "content://qa/glossary", "glossary.txt", "A,B");
            EditorialRepository.AssetSnapshot pronoun = new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.PRONOUN, "content://qa/pronoun", "pronoun.txt", "A,B,anh/em");
            long chapterId = repo.createChapter(projectId, "C01", "QA chapter", Arrays.asList(raw, draft, glossary, pronoun));
            assertEquals(EditorialWorkflowV5.ChapterState.L1_READY, repo.getChapter(chapterId).state);
            assertTrue(repo.assetsMatchSnapshot(chapterId, Arrays.asList(raw, draft, glossary, pronoun)));
            long runId = repo.createRun(chapterId, "L1", "fake", "fake/model", HashUtil.sha256("prompt"), project.workflowHash, "{\"assets\":4}");
            repo.saveScene(runId, "s1", "p000001", "p000003", "CLOSED", "{\"scene\":1}");
            repo.saveGate(runId, EditorialWorkflowV5.Gate.COVERAGE, EditorialWorkflowV5.GateStatus.CLOSED, "{\"ok\":true}");
            repo.saveEvidence(runId, "REPORT_L1", "{\"report\":true}");
            assertEquals(1, repo.evidenceCount(runId));
        } finally {
            if (projectId > 0) repo.deleteProject(projectId);
            repo.close();
        }
    }

    @Test public void segmentedL1CheckpointsEveryStructurallyMappedScene() throws Exception {
        EditorialRepository repo=new EditorialRepository(ApplicationProvider.getApplicationContext());long projectId=-1;
        try {EditorialRepository.Project project=new EditorialRepository.Project();project.seriesName="QA Segmented "+System.nanoTime();project.volumeName="V1";project.workflowHash=HashUtil.sha256("v5");projectId=repo.createProject(project);StringBuilder raw=new StringBuilder(),draft=new StringBuilder();for(int i=0;i<30;i++){raw.append(repeat('原',500));draft.append(repeat('v',500));if(i<29){raw.append("\n◇◇◇\n");draft.append("\n◇◇◇\n");}}
            long chapterId=repo.createChapter(projectId,"LONG","Long chapter",Arrays.asList(new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.RAW,"memory://raw","raw.txt",raw.toString()),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.DRAFT,"memory://draft","draft.txt",draft.toString()),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.GLOSSARY,"memory://g","g.txt","term"),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.PRONOUN,"memory://p","p.txt","voice")));
            EditorialL1Runner runner=new EditorialL1Runner(repo,(settings,prompt,requestId)->{Matcher scene=Pattern.compile("SCENE: (scene-\\d+)").matcher(prompt.user),anchors=Pattern.compile("EXPECTED RAW ANCHORS: (p\\d+) -> (p\\d+)").matcher(prompt.user);assertTrue(scene.find());assertTrue(anchors.find());OpenAICompatibleClient.ChatResult response=new OpenAICompatibleClient.ChatResult();response.content="{\"sceneId\":\""+scene.group(1)+"\",\"rawStart\":\""+anchors.group(1)+"\",\"rawEnd\":\""+anchors.group(2)+"\",\"coverage\":\"ALIGNED\",\"status\":\"CLOSED\",\"issues\":[]}";response.totalTokens=10;return response;});EditorialL1Runner.Result result=runner.run(chapterId,new AppSettings());assertTrue(result.report.startsWith("REPORT_L1"));assertEquals(EditorialWorkflowV5.ChapterState.L1_CLOSED,repo.getChapter(chapterId).state);assertTrue(repo.evidenceCount(result.runId)>=34);
        }finally{if(projectId>0)repo.deleteProject(projectId);repo.close();}
    }
    @Test public void retryReusesClosedSceneAndCallsOnlyFailedScene() throws Exception {
        EditorialRepository repo=new EditorialRepository(ApplicationProvider.getApplicationContext());long projectId=-1;try{EditorialRepository.Project project=new EditorialRepository.Project();project.seriesName="QA Retry "+System.nanoTime();project.volumeName="V1";project.workflowHash=HashUtil.sha256("v5");projectId=repo.createProject(project);String raw=repeat('原',7000)+"\n◇◇◇\n"+repeat('終',7000),draft=repeat('a',7000)+"\n◇◇◇\n"+repeat('b',7000);long chapterId=repo.createChapter(projectId,"RETRY","Retry chapter",Arrays.asList(new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.RAW,"memory://raw","raw.txt",raw),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.DRAFT,"memory://draft","draft.txt",draft),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.GLOSSARY,"memory://g","g.txt","term"),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.PRONOUN,"memory://p","p.txt","voice")));AtomicInteger calls=new AtomicInteger();EditorialL1Runner runner=new EditorialL1Runner(repo,(settings,prompt,requestId)->{int call=calls.incrementAndGet();Matcher scene=Pattern.compile("SCENE: (scene-\\d+)").matcher(prompt.user),anchors=Pattern.compile("EXPECTED RAW ANCHORS: (p\\d+) -> (p\\d+)").matcher(prompt.user);assertTrue(scene.find());assertTrue(anchors.find());OpenAICompatibleClient.ChatResult response=new OpenAICompatibleClient.ChatResult();if("scene-002".equals(scene.group(1))&&call==2){response.content="invalid";return response;}response.content="{\"sceneId\":\""+scene.group(1)+"\",\"rawStart\":\""+anchors.group(1)+"\",\"rawEnd\":\""+anchors.group(2)+"\",\"coverage\":\"ALIGNED\",\"status\":\"CLOSED\",\"issues\":[]}";return response;});try{runner.run(chapterId,new AppSettings());fail();}catch(Exception expected){assertEquals(EditorialWorkflowV5.ChapterState.FAILED,repo.getChapter(chapterId).state);}runner.retryFailedScene(chapterId,new AppSettings());assertEquals(3,calls.get());assertEquals(EditorialWorkflowV5.ChapterState.L1_CLOSED,repo.getChapter(chapterId).state);
        }finally{if(projectId>0)repo.deleteProject(projectId);repo.close();}
    }
    @Test public void l2ClosesRawLedgerBeforeOpeningDraftAndReport() throws Exception {
        EditorialRepository repo=new EditorialRepository(ApplicationProvider.getApplicationContext());long projectId=-1;
        try{EditorialRepository.Project project=new EditorialRepository.Project();project.seriesName="QA L2 "+System.nanoTime();project.volumeName="V1";project.workflowHash=HashUtil.sha256("v5");projectId=repo.createProject(project);long chapterId=repo.createChapter(projectId,"L2","L2",Arrays.asList(new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.RAW,"m://r","r.txt","RAW_SECRET"),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.DRAFT,"m://d","d.txt","DRAFT_SECRET"),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.GLOSSARY,"m://g","g.txt","G"),new EditorialRepository.AssetSnapshot(EditorialWorkflowV5.AssetRole.PRONOUN,"m://p","p.txt","P")));long l1=repo.createRun(chapterId,"L1","fake","fake",HashUtil.sha256("p"),project.workflowHash,"{}");String report="{\"schema\":\"editorial-v5-evidence-1\",\"run\":\"L1\",\"chapterId\":\""+chapterId+"\",\"scenes\":[{\"id\":\"s1\",\"rawStart\":\"p1\",\"rawEnd\":\"p1\",\"coverage\":\"ALIGNED\",\"status\":\"CLOSED\"}],\"issues\":[],\"gates\":{\"coverage\":\"CLOSED\",\"fidelity\":\"CLOSED\",\"referenceVoice\":\"CLOSED\",\"continuityStructure\":\"CLOSED\"}}";repo.saveEvidence(l1,"REPORT_L1_JSON",report);repo.transitionChapter(chapterId,EditorialWorkflowV5.ChapterState.L1_READY,EditorialWorkflowV5.ChapterState.L1_RUNNING);repo.transitionChapter(chapterId,EditorialWorkflowV5.ChapterState.L1_RUNNING,EditorialWorkflowV5.ChapterState.L1_CLOSED);AtomicInteger calls=new AtomicInteger();EditorialL2Runner runner=new EditorialL2Runner(repo,(settings,prompt,id)->{OpenAICompatibleClient.ChatResult response=new OpenAICompatibleClient.ChatResult();if(calls.getAndIncrement()==0){assertTrue(prompt.user.contains("RAW_SECRET"));assertFalse(prompt.user.contains("DRAFT_SECRET"));assertFalse(prompt.user.contains("REPORT_L1"));response.content="{\"chapterId\":\""+chapterId+"\",\"scenes\":[{\"id\":\"s1\",\"rawStart\":\"p1\",\"rawEnd\":\"p1\",\"pov\":\"A\",\"cast\":\"A\",\"event\":\"E\",\"risk\":\"R\",\"status\":\"CLOSED\"}]}";}else{assertTrue(prompt.user.contains("DRAFT_SECRET"));assertTrue(prompt.user.contains("REPORT_L1"));response.content="{\"schema\":\"editorial-v5-evidence-1\",\"run\":\"L2\",\"chapterId\":\""+chapterId+"\",\"output\":\"VI\",\"outputComplete\":true,\"globalChanges\":[],\"gates\":{\"coverage\":\"CLOSED\",\"fidelity\":\"CLOSED\",\"referenceVoice\":\"CLOSED\",\"continuityStructure\":\"CLOSED\",\"change\":\"CLOSED\"}}";}return response;});EditorialL2Runner.Result result=runner.run(chapterId,new AppSettings());assertEquals(2,calls.get());assertEquals("VI",result.output);assertEquals(EditorialWorkflowV5.ChapterState.L2_CLOSED,repo.getChapter(chapterId).state);assertFalse(repo.evidencePayload(result.runId,"L2_RAW_LEDGER").isEmpty());
        }finally{if(projectId>0)repo.deleteProject(projectId);repo.close();}
    }
    private static String repeat(char value,int count){StringBuilder out=new StringBuilder(count);for(int i=0;i<count;i++)out.append(value);return out.toString();}
}
