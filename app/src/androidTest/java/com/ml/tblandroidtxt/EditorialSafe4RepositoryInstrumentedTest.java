package com.ml.tblandroidtxt;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EditorialSafe4RepositoryInstrumentedTest {
    @Test public void newChapterBindsSafe4AndAcceptsPronounNoneWhileExecutionIsBlocked() {
        EditorialRepository repo = new EditorialRepository(ApplicationProvider.getApplicationContext());
        long projectId = -1L;
        try {
            EditorialRepository.Project project = new EditorialRepository.Project();
            project.seriesName = "QA SAFE4 " + System.nanoTime();
            project.volumeName = "V1";
            projectId = repo.createProject(project);
            long chapterId = repo.createChapter(projectId, "001", "001", Arrays.asList(
                    new EditorialRepository.AssetSnapshot(EditorialSafe4Workflow.AssetRole.RAW, "memory://raw", "raw.txt", "原文"),
                    new EditorialRepository.AssetSnapshot(EditorialSafe4Workflow.AssetRole.DRAFT, "memory://draft", "draft.txt", "Bản nháp"),
                    new EditorialRepository.AssetSnapshot(EditorialSafe4Workflow.AssetRole.GLOSSARY, "memory://glossary", "glossary.txt", "term=value")));

            assertEquals(EditorialSafe4Pack.VERSION, repo.getProject(projectId).workflowVersion);
            assertEquals(EditorialSafe4Pack.PACK_HASH, repo.getProject(projectId).workflowHash);
            assertEquals(EditorialSafe4Workflow.ChapterState.SAFE4_BLOCKED, repo.getChapter(chapterId).state);
            assertEquals(3, repo.chapterAssets(chapterId).size());
            assertNull(repo.projectReference(projectId, EditorialSafe4Workflow.AssetRole.PRONOUN));
        } finally {
            if (projectId > 0) repo.deleteProject(projectId);
            repo.close();
        }
    }
}
