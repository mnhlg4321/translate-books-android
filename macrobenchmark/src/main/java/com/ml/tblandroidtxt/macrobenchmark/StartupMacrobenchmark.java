package com.ml.tblandroidtxt.macrobenchmark;

import android.content.Intent;

import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import kotlin.Unit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class StartupMacrobenchmark {
    private static final String TARGET_PACKAGE = "com.ml.tblandroidtxt";
    private static final String TARGET_ACTIVITY = "com.ml.tblandroidtxt.MainActivity";

    @Rule
    public final MacrobenchmarkRule benchmarkRule = new MacrobenchmarkRule();

    @Test
    public void coldStartup() {
        benchmarkRule.measureRepeated(
                TARGET_PACKAGE,
                Collections.singletonList(new StartupTimingMetric()),
                CompilationMode.DEFAULT,
                StartupMode.COLD,
                5,
                scope -> Unit.INSTANCE,
                scope -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN)
                            .setClassName(TARGET_PACKAGE, TARGET_ACTIVITY)
                            .addCategory(Intent.CATEGORY_LAUNCHER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    scope.startActivityAndWait(intent);
                    return Unit.INSTANCE;
                }
        );
    }
}
