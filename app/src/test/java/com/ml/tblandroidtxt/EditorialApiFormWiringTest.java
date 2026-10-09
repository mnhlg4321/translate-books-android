package com.ml.tblandroidtxt;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Structural guard only (the behaviour is exercised on a device by EditorialApiBienTapUiInstrumentedTest): every action that
 * rebuilds the combo screen keeps the typed values first, and the screen is filled from them.
 */
public final class EditorialApiFormWiringTest {
    private static String source(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/ml/tblandroidtxt/" + name + ".java")), StandardCharsets.UTF_8);
    }

    @Test public void everySourceChoiceCapturesTheFormBeforeItRebuildsTheScreen() throws Exception {
        String controller = source("EditorialApiUiController");
        for (String method : new String[] {"void pickFile(boolean raw) {", "void pickRecent(boolean raw) {", "void chooseGlossary() {", "void choosePronoun() {"}) {
            int at = controller.indexOf(method);
            assertTrue(method, at > 0);
            assertTrue(method, controller.substring(at + method.length(), at + method.length() + 40).trim().startsWith("captureForm();"));
        }
        assertTrue(controller.contains("form = new Form(name, mode, model, capText);"));
    }

    @Test public void theComboScreenIsFilledFromTheKeptFormAndOffersSaveWithoutSending() throws Exception {
        String factory = source("EditorialApiPageFactory");
        assertTrue(factory.contains("form != null ? form.name : combo.name"));
        assertTrue(factory.contains("form != null ? form.model : settings.model"));
        assertTrue(factory.contains("form != null ? form.cap : settings.maxUsdPerChapter.toPlainString()"));
        assertFalse("the Kỹ mode is no longer offered", factory.contains("\"Kỹ\""));
        assertTrue(factory.contains("c.saveOnly("));
    }
}
