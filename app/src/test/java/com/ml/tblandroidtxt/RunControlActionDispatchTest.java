package com.ml.tblandroidtxt;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** Keeps the UI dispatcher exhaustive when RunControlModel.Action grows. */
public class RunControlActionDispatchTest {
    private static File mainSource() {
        File file = new File("src/main/java/com/ml/tblandroidtxt/MainActivity.java");
        return file.isFile() ? file : new File("app/src/main/java/com/ml/tblandroidtxt/MainActivity.java");
    }

    @Test public void everyActionHasAnExplicitRunDispatcherBranch() throws Exception {
        String source = new String(Files.readAllBytes(mainSource().toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n");
        int start = source.indexOf("private void dispatchRunAction(");
        int end = source.indexOf("private void showRunOverflow(", start);
        assertTrue("run dispatcher found", start >= 0 && end > start);
        String dispatcher = source.substring(start, end);
        for (RunControlModel.Action action : RunControlModel.Action.values()) {
            assertTrue("missing dispatcher branch for " + action.name(),
                    dispatcher.contains("case " + action.name() + ":"));
        }
        assertTrue("secondary actions use the exhaustive dispatcher",
                source.contains("private void runSecondaryAction(RunControlModel.Action action) {\n        dispatchRunAction(action);"));
    }
}
