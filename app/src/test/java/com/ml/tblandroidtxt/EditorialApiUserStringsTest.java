package com.ml.tblandroidtxt;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Biên tập flow speaks about files, glossaries and results: none of the engineering vocabulary of the earlier editorial
 * path may appear in a string the person can see. The one allowed place is the collapsed developer section title.
 */
public final class EditorialApiUserStringsTest {
    private static final String[] FILES = {"EditorialApiPresenter", "EditorialApiPageFactory", "EditorialApiUiController"};
    private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
    private static final Pattern[] FORBIDDEN = {
            Pattern.compile("\\bpack\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("binding", Pattern.CASE_INSENSITIVE),
            Pattern.compile("SAFE4", Pattern.CASE_INSENSITIVE),
            Pattern.compile("cấp phép", Pattern.CASE_INSENSITIVE),
            Pattern.compile("lineage", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ledger", Pattern.CASE_INSENSITIVE),
            Pattern.compile("certif", Pattern.CASE_INSENSITIVE),
    };

    private static String source(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/ml/tblandroidtxt/" + name + ".java")), StandardCharsets.UTF_8);
    }

    private static List<String> literals(String source) {
        List<String> out = new ArrayList<>();
        Matcher m = LITERAL.matcher(source);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Test public void noEngineeringTermReachesAStringOfTheBienTapFlow() throws Exception {
        int checked = 0;
        for (String file : FILES) {
            for (String literal : literals(source(file))) {
                if (literal.equals(EditorialApiPresenter.LEGACY_SECTION_TITLE)) continue;
                checked++;
                for (Pattern pattern : FORBIDDEN) {
                    assertTrue(file + ": \"" + literal + "\" contains " + pattern.pattern(), !pattern.matcher(literal).find());
                }
            }
        }
        assertTrue("the scan must have looked at real strings", checked > 150);
    }

    @Test public void theLegacyTitleIsUsedOnlyAsTheCollapsedSectionButtonLabel() throws Exception {
        String legacy = EditorialApiPresenter.LEGACY_SECTION_TITLE;
        assertEquals("Công cụ dev — SAFE4 legacy", legacy);
        String factory = source("EditorialApiPageFactory");
        assertTrue(factory.contains("EditorialApiPresenter.LEGACY_SECTION_TITLE"));
        assertTrue(factory.contains("if (c.legacyOpen)"));
        // the legacy page is embedded only inside the open branch
        int open = factory.indexOf("if (c.legacyOpen)");
        int embed = factory.indexOf("new EditorialPageFactory(a).populate(inner)");
        assertTrue(open > 0 && embed > open);
        for (String file : new String[] {"EditorialApiPresenter", "EditorialApiUiController"}) {
            String text = source(file).toLowerCase(Locale.ROOT);
            assertEquals(file, text.contains("safe4_blocked"), false);
        }
    }

    @Test public void theMainTabIsLabelledBienTapAndNotWithAnEngineeringName() throws Exception {
        String main = source("MainActivity");
        assertTrue(main.contains("String tabLabel(String name)"));
        assertTrue(main.contains("EditorialApiPresenter.TAB_TITLE"));
        assertTrue(main.contains("View buildEditorialPage() { return new EditorialApiPageFactory(this, editorialApi()).build(); }"));
    }
}
