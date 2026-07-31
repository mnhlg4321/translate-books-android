package com.ml.tblandroidtxt;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static String repeat(char value,int count){StringBuilder out=new StringBuilder(count);for(int i=0;i<count;i++)out.append(value);return out.toString();}
}
