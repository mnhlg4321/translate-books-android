package com.ml.tblandroidtxt;
import org.junit.Test;import static org.junit.Assert.*;
public class EditorialL1SceneContractTest {
 @Test public void acceptsClosedSceneWithNoIssues(){String json="{\"sceneId\":\"scene-001\",\"rawStart\":\"p000001\",\"rawEnd\":\"p000002\",\"coverage\":\"ALIGNED\",\"status\":\"CLOSED\",\"issues\":[]}";assertNull(EditorialL1SceneContract.validate(json));}
 @Test public void blocksOpenOrEvidenceFreeIssue(){assertNotNull(EditorialL1SceneContract.validate("{\"sceneId\":\"s\",\"rawStart\":\"a\",\"rawEnd\":\"b\",\"coverage\":\"ALIGNED\",\"status\":\"OPEN\",\"issues\":[]}"));}
}
