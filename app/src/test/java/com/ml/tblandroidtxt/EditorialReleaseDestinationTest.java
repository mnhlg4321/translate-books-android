package com.ml.tblandroidtxt;

import org.junit.Test;
import static org.junit.Assert.*;

public class EditorialReleaseDestinationTest {
    @Test public void archiveNameKeepsZipAndRemovesFilesystemCharacters(){assertEquals("QA_C001_RELEASE.zip",FileUtil.sanitizeArchiveName("QA:C001/RELEASE"));assertEquals("ready.zip",FileUtil.sanitizeArchiveName("ready.zip"));}
    @Test public void emptyAndLongArchiveNamesStayBounded(){assertEquals("editorial-release.zip",FileUtil.sanitizeArchiveName("  "));String value=FileUtil.sanitizeArchiveName("x".repeat(220));assertTrue(value.endsWith(".zip"));assertTrue(value.length()<=180);}
}
