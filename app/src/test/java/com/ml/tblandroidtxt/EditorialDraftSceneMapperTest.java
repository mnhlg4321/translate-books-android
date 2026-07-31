package com.ml.tblandroidtxt;
import org.junit.Test;import java.util.List;import static org.junit.Assert.*;
public class EditorialDraftSceneMapperTest {
 @Test public void alignsOnlyMatchingStructuralScenes(){List<EditorialDraftSceneMapper.Pair> p=EditorialDraftSceneMapper.map("raw one\n◇◇◇\nraw two","draft one\n◇◇◇\ndraft two");assertEquals(2,p.size());assertEquals(EditorialDraftSceneMapper.Status.ALIGNED,p.get(0).status);}
 @Test public void refusesToPretendPlainChapterIsAligned(){List<EditorialDraftSceneMapper.Pair> p=EditorialDraftSceneMapper.map("raw text","draft text");assertEquals(EditorialDraftSceneMapper.Status.AMBIGUOUS,p.get(0).status);}
 @Test public void marksDifferentSeparatorCountsMisaligned(){List<EditorialDraftSceneMapper.Pair> p=EditorialDraftSceneMapper.map("a\n◇◇◇\nb","a");assertEquals(EditorialDraftSceneMapper.Status.MISALIGNED,p.get(1).status);}
}
