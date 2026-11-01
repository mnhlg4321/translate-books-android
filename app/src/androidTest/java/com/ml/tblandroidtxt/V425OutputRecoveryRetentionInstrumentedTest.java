package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Exercises the recovery-copy deletion path on-device with local QA_ files only. */
@RunWith(AndroidJUnit4.class)
public class V425OutputRecoveryRetentionInstrumentedTest {
    @Test public void tenCopiesForOneOutputArePrunedToThree() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String outputKey = HashUtil.sha256("QA_V425_OUTPUT_" + System.nanoTime());
        long now = System.currentTimeMillis();
        File[] copies = new File[10];
        File outputDir = new File(c.getCacheDir(), "v425-output-recovery-fake-provider");
        outputDir.mkdirs();
        TranslateTestSeam.setOutputDir(outputDir);
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            throw new AssertionError("retention test must not call a provider");
        });
        try {
            for (int i = 0; i < copies.length; i++) {
                copies[i] = OutputRecoveryCopies.namedCopy(c.getFilesDir(), outputKey, now - i * 1000L);
                Files.write(copies[i].toPath(), "QA_ recovery fixture".getBytes(StandardCharsets.UTF_8));
            }

            int deleted = FileUtil.pruneOutputRecoveryCopies(c);

            assertEquals("seven older copies removed", 7, deleted);
            int remaining = 0;
            for (File copy : copies) if (copy.exists()) remaining++;
            assertEquals("the three newest copies remain", 3, remaining);
            assertTrue(copies[0].exists());
            assertTrue(copies[1].exists());
            assertTrue(copies[2].exists());
        } finally {
            TranslateTestSeam.clear();
            for (File copy : copies) if (copy != null && copy.exists()) copy.delete();
        }
    }
}
