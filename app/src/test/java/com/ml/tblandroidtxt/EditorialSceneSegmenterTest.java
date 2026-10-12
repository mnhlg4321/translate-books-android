package com.ml.tblandroidtxt;
import org.junit.Test;
import static org.junit.Assert.*;
public class EditorialSceneSegmenterTest {
 @Test public void retainsEveryRawAnchorOnceAcrossLongChapter(){String raw="a\n\n"+"b".repeat(300)+"\n◇◇◇\n"+"c".repeat(300)+"\n";EditorialSceneSegmenter.Plan p=EditorialSceneSegmenter.split(raw,300);StringBuilder rebuilt=new StringBuilder();for(EditorialSceneSegmenter.Scene s:p.scenes)rebuilt.append(s.raw);assertEquals(raw,rebuilt.toString());assertEquals("p000001",p.scenes.get(0).startAnchor);assertEquals("p000005",p.scenes.get(p.scenes.size()-1).endAnchor);}
 @Test public void ledgerCoversAllScenes(){EditorialSceneSegmenter.Plan p=EditorialSceneSegmenter.split("one\ntwo\nthree\n",256);assertTrue(p.ledgerJson().contains("PENDING_AUDIT"));assertEquals(1,p.scenes.size());}
}
