package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** R0 findings kept as regression checks on the packaging script: the icon is passed to jpackage and the README tells to close the app before installing. */
public class PackagingScriptTest {
    private static String script() throws Exception {
        return new String(Files.readAllBytes(Paths.get("..", "scripts", "build-desktop.ps1")), StandardCharsets.UTF_8);
    }

    @Test public void jpackageGetsTheLogoIcon() throws Exception {
        String s = script();
        assertTrue(s.contains("--icon (Join-Path $repo 'desktop\\packaging\\translate_books.ico')"));
        assertTrue(Files.exists(Paths.get("packaging", "translate_books.ico")));
    }

    @Test public void theInstallerVersionCanBeSetSoAReleaseCandidateStaysAboveTheDevelopmentInstalls() throws Exception {
        String s = script();
        assertTrue(s.contains("[string]$MsiVersion"));
        assertTrue(s.contains("Get-DesktopMsiVersion -Version $version"));
        assertTrue(s.contains("Installer version rule"));
        assertTrue(Files.exists(Paths.get("..", "scripts", "desktop-msi-version.ps1")));
    }

    @Test public void aReleasePackageKeepsTheExactVersionNameAndRefusesToBeBuiltTwice() throws Exception {
        String s = script();
        assertTrue(s.contains("[string]$ExactReleaseVersion"));
        assertTrue(s.contains("$version = $ExactReleaseVersion"));
        assertTrue(s.contains("immutable release"));
    }

    @Test public void theReadmeTellsToCloseTheAppBeforeInstalling() throws Exception {
        String s = script();
        assertTrue(s.contains("Close Translate Books (every window) before installing a newer .msi"));
        assertTrue(s.contains("error 1603"));
    }
}
