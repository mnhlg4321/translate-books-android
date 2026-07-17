package com.ml.tblandroidtxt;
import org.junit.Test;
import static org.junit.Assert.*;
public class UiAppearanceTest {
 @Test public void offersManyNamedPalettesAndSafeFallback(){assertTrue(UiAppearance.all().size()>=12);assertEquals("default",UiAppearance.find("missing").id);assertNotEquals(UiAppearance.find("blue").accent,UiAppearance.find("gold").accent);}
 @Test public void palettesKeepReadableTextContrast(){for(UiAppearance.Palette p:UiAppearance.all()){assertTrue(luma(p.text)>luma(p.bg));assertTrue(luma(p.muted)>luma(p.bg));}}
 private double luma(int c){return 0.2126*((c>>16)&255)+0.7152*((c>>8)&255)+0.0722*(c&255);}
}
