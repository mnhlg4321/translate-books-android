package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OutputRecoveryCopiesTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test public void pruneKeepsThreeRecentCopiesPerOutputAndExpiresCopiesAfterThirtyDays() throws Exception {
        File directory = temporaryFolder.newFolder("files");
        long now = 1_800_000_000_000L;
        String outputA = HashUtil.sha256("QA_output_A");
        String outputB = HashUtil.sha256("QA_output_B");
        for (int i = 0; i < 5; i++) create(OutputRecoveryCopies.namedCopy(directory, outputA, now - i * 1000L));
        create(OutputRecoveryCopies.namedCopy(directory, outputA, now - OutputRecoveryCopies.MAX_AGE_MS - 1000L));
        for (int i = 0; i < 4; i++) create(OutputRecoveryCopies.namedCopy(directory, outputB, now - i * 1000L));
        for (int i = 0; i < 4; i++) create(new File(directory, "output-recovery-" + (now - i * 1000L) + ".txt"));
        File expiredLegacy = new File(directory, "output-recovery-" + (now - OutputRecoveryCopies.MAX_AGE_MS - 1000L) + ".txt");
        create(expiredLegacy);
        File interruptedCopy = new File(directory, "output-recovery-" + outputA + "-" + now + "-partial.pending");
        create(interruptedCopy);

        int deleted = OutputRecoveryCopies.prune(directory, now);

        assertEquals(7, deleted);
        assertEquals(3, countPrefix(directory, "output-recovery-" + outputA + "-", ".txt"));
        assertEquals(3, countPrefix(directory, "output-recovery-" + outputB + "-", ".txt"));
        assertEquals(3, countLegacy(directory));
        assertFalse(expiredLegacy.exists());
        assertFalse(interruptedCopy.exists());
        assertEquals("three groups retain their own newest copies", 9, directory.listFiles().length);
    }

    private static void create(File file) throws Exception {
        Files.write(file.toPath(), "QA_ recovery fixture".getBytes(StandardCharsets.UTF_8));
    }

    private static int countPrefix(File directory, String prefix, String suffix) {
        int count = 0;
        for (File file : directory.listFiles()) if (file.getName().startsWith(prefix) && file.getName().endsWith(suffix)) count++;
        return count;
    }

    private static int countLegacy(File directory) {
        int count = 0;
        for (File file : directory.listFiles()) {
            String name = file.getName();
            if (name.startsWith("output-recovery-") && name.endsWith(".txt") && name.split("-").length == 3) count++;
        }
        return count;
    }
}
