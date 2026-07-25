package com.ml.tblandroidtxt.macrobenchmark;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.RemoteException;

import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;
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

    private PowerManager.WakeLock benchmarkWakeLock;

    @Before
    public void keepBenchmarkRunnerAlive() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            throw new IllegalStateException("PowerManager is unavailable on the benchmark device.");
        }
        benchmarkWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TranslateBooks:ReleaseMacrobenchmark"
        );
        benchmarkWakeLock.setReferenceCounted(false);
        benchmarkWakeLock.acquire(15 * 60 * 1_000L);
        wakeScreen();
        startBenchmarkHost();
    }

    @After
    public void releaseBenchmarkWakeLock() {
        if (benchmarkWakeLock != null && benchmarkWakeLock.isHeld()) {
            benchmarkWakeLock.release();
        }
    }

    @Test
    public void coldStartup() {
        benchmarkRule.measureRepeated(
                TARGET_PACKAGE,
                Collections.singletonList(new StartupTimingMetric()),
                CompilationMode.DEFAULT,
                StartupMode.COLD,
                5,
                scope -> {
                    wakeScreen();
                    return Unit.INSTANCE;
                },
                scope -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN)
                            .setClassName(TARGET_PACKAGE, TARGET_ACTIVITY)
                            .addCategory(Intent.CATEGORY_LAUNCHER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    scope.startActivityAndWait(intent);
                    startBenchmarkHost();
                    return Unit.INSTANCE;
                }
        );
    }

    private static void wakeScreen() {
        try {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wakeUp();
        }
        catch (RemoteException error) {
            throw new IllegalStateException("Unable to wake the physical benchmark device.", error);
        }
    }

    private static void startBenchmarkHost() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, BenchmarkHostActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
