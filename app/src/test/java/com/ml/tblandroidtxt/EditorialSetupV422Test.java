package com.ml.tblandroidtxt;

import org.junit.Test;
import static org.junit.Assert.*;

public class EditorialSetupV422Test {
    @Test public void newAndLegacyJsonDefaultToFourFiles() {
        assertFalse(new EditorialApiCombo.Settings().twoFiles);
        assertFalse(EditorialApiCombo.Settings.fromJson("{}").twoFiles);
    }
    @Test public void twoFileChoiceIsRememberedWithoutSchemaChange() {
        EditorialApiCombo.Settings s = new EditorialApiCombo.Settings(); s.twoFiles = true;
        assertTrue(EditorialApiCombo.Settings.fromJson(s.toJson()).twoFiles);
        s.twoFiles = false;
        assertFalse(EditorialApiCombo.Settings.fromJson(s.toJson()).twoFiles);
    }
    @Test public void fourFilesWarnForEitherMissingReferenceButTwoFilesDoNot() {
        EditorialApiCombo.Settings s = new EditorialApiCombo.Settings();
        assertTrue(EditorialApiCombo.missingReferences(s, "g", ""));
        assertTrue(EditorialApiCombo.missingReferences(s, "", "p"));
        assertFalse(EditorialApiCombo.missingReferences(s, "g", "p"));
        s.twoFiles = true;
        assertFalse(EditorialApiCombo.missingReferences(s, "", ""));
    }
}
