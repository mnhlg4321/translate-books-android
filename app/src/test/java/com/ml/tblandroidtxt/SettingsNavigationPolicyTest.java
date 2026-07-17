package com.ml.tblandroidtxt;
import org.junit.Test;
import static org.junit.Assert.*;
public class SettingsNavigationPolicyTest {
 @Test public void sameCategoryDoesNotTriggerRebuild(){assertFalse(SettingsNavigationPolicy.shouldSwitch("Provider","Provider"));assertFalse(SettingsNavigationPolicy.shouldSwitch("Provider",null));}
 @Test public void aDifferentValidCategorySwitches(){assertTrue(SettingsNavigationPolicy.shouldSwitch("General","Appearance"));}
 @Test public void narrowOrLargeTextStacksFieldPairs(){assertTrue(SettingsNavigationPolicy.shouldStackFields(359,1f));assertTrue(SettingsNavigationPolicy.shouldStackFields(411,1.3f));assertFalse(SettingsNavigationPolicy.shouldStackFields(411,1f));}
}
