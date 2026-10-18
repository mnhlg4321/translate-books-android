package com.ml.tblandroidtxt;

import java.io.File;

/**
 * Test-only hooks on the Translate side so device tests can run the whole service with a fake provider and a plain output
 * folder (the system picker cannot be driven from an instrumented test). Both hooks are null in every normal run.
 */
public final class TranslateTestSeam {
    private static volatile TranslationEngine.ProviderClient provider;
    private static volatile File outputDir;

    private TranslateTestSeam() {}

    /** Replaces the network call of the Translate service; null restores the real provider. */
    public static void setProvider(TranslationEngine.ProviderClient fake) { provider = fake; }

    /** Writes outputs into this folder instead of a picked SAF folder and skips the persisted-grant check; null restores normal behaviour. */
    public static void setOutputDir(File dir) { outputDir = dir; }

    public static void clear() { provider = null; outputDir = null; }

    static TranslationEngine.ProviderClient provider() { return provider; }

    static File outputDir() { return outputDir; }
}
