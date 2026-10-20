package com.ml.tblandroidtxt;
import org.junit.Test;
import static org.junit.Assert.*;
public class UiAppearanceTest {
 @Test public void offersManyNamedPalettesAndSafeFallback(){assertTrue(UiAppearance.all().size()>=12);assertEquals("default",UiAppearance.find("missing").id);assertNotEquals(UiAppearance.find("blue").accent,UiAppearance.find("gold").accent);}
 @Test public void palettesKeepReadableTextContrast(){for(UiAppearance.Palette p:UiAppearance.all()){assertTrue(ColorMath.ratio(p.text,p.bg)>=7);assertTrue(ColorMath.ratio(p.muted,p.bg)>=4.5);}}
 private double luma(int c){return 0.2126*((c>>16)&255)+0.7152*((c>>8)&255)+0.0722*(c&255);}
}
