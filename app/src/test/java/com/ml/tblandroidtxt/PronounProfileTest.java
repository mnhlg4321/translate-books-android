package com.ml.tblandroidtxt;
import org.junit.Test;
import static org.junit.Assert.*;
public class PronounProfileTest {
 @Test public void profileCountsRecognizedRules(){PronounStore.Profile p=new PronounStore.Profile();p.text="from,to,pronoun\nAlice,Bob,chị/em\nBob,Alice,em/chị";assertEquals(2,p.count());}
 @Test public void activeProfileSnapshotFieldsCopy(){AppSettings s=new AppSettings();s.selectedPronounId="p1";s.selectedPronounName="Volume 8";s.pronounText="Alice -> Bob: chị/em";AppSettings copy=s.copy();assertEquals("p1",copy.selectedPronounId);assertEquals(s.pronounText,copy.pronounText);}
}
