package com.ml.tblandroidtxt;

import static org.junit.Assert.*;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;

public class EditorialProjectFolderInstrumentedTest {
    @Test public void releaseFolderPersistsOnlyOnTargetProject(){EditorialRepository repo=new EditorialRepository(ApplicationProvider.getApplicationContext());long first=-1,second=-1;try{EditorialRepository.Project a=new EditorialRepository.Project();a.seriesName="QA Folder A "+System.nanoTime();a.volumeName="V";a.workflowHash=HashUtil.sha256("w");first=repo.createProject(a);EditorialRepository.Project b=new EditorialRepository.Project();b.seriesName="QA Folder B "+System.nanoTime();b.volumeName="V";b.workflowHash=HashUtil.sha256("w");second=repo.createProject(b);repo.updateProjectOutputTree(first,"content://qa/tree/release");assertEquals("content://qa/tree/release",repo.getProject(first).outputTreeUri);assertTrue(repo.getProject(second).outputTreeUri.isEmpty());}finally{if(first>0)repo.deleteProject(first);if(second>0)repo.deleteProject(second);repo.close();}}
}
