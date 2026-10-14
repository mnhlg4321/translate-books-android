package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

/**
 * v4.22 screens on synthetic rows only (no book text) with a scripted provider, for screenshots: four-file setup (default), the
 * two-file tick, the missing-reference warning, the plan, the progress card while a call is held, the Jobs tab card, the readable
 * result and the Editorial settings page. Runs only with the instrumentation argument {@code v422_media=run}. Every stage holds
 * the screen and logs {@code V422MEDIA stage=...} so the shell can take a picture. Nothing reaches a provider.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialV422MediaWalkthroughInstrumentedTest {
    private static final String TAG = "V422MEDIA";
    private static final long HOLD_MILLIS = 5_000L;

    private Context context;
    private long jobId;
    private String glossaryId = "";
    private final List<Long> comboIds = new ArrayList<>();
    private final List<Long> runIds = new ArrayList<>();

    @Before public void setUp() {
        Assume.assumeTrue("run".equals(InstrumentationRegistry.getArguments().getString("v422_media", "")));
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("editorial_setup", 0).edit().remove("twoFiles").commit();
        TranslationRepository repository = new TranslationRepository(context);
        try {
            List<Chunk> chunks = new ArrayList<>();
            int offset = 0;
            for (int i = 1; i <= 3; i++) {
                String raw = EditorialPairTestData.rawRow(i);
                chunks.add(new Chunk(i - 1, offset, offset + raw.length(), "", raw, "", ""));
                offset += raw.length();
            }
            jobId = repository.createJob("content://synthetic/raw", "content://synthetic/out", "synthetic-v422.txt", new AppSettings(), chunks);
            for (int i = 1; i <= 3; i++) repository.markChunkDone(jobId, i - 1, EditorialPairTestData.draftRow(i));
        } finally {
            repository.close();
        }
        GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
        glossary.name = "synthetic-glossary-v422";
        GlossaryStore.Term hanako = new GlossaryStore.Term();
        hanako.source = "花子";
        hanako.target = "Hanako";
        glossary.terms.add(hanako);
        List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
        all.add(0, glossary);
        GlossaryTestSupport.saveAndFlush(context, all);
        glossaryId = glossary.id;
    }

    @After public void tearDown() throws Exception {
        EditorialApiUiController.providerOverride = null;
        if (context == null) return;
        for (long id : runIds) {
            EditorialPairForegroundService.cancelRun(id);
            long until = System.currentTimeMillis() + 30_000L;
            while (EditorialPairForegroundService.isActive(id) && System.currentTimeMillis() < until) Thread.sleep(25);
            try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(context)) { store.deleteRun(id); }
        }
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
            for (long id : comboIds) store.deleteCombo(id);
        }
        if (jobId > 0) {
            TranslationRepository repository = new TranslationRepository(context);
            try { repository.deleteJob(jobId); } finally { repository.close(); }
        }
        if (!glossaryId.isEmpty()) GlossaryTestSupport.removeAndFlush(context, java.util.Collections.singletonList(glossaryId));
        context.getSharedPreferences("editorial_setup", 0).edit().remove("twoFiles").commit();
    }

    private static void collect(View view, List<View> out) {
        if (view.getVisibility() == View.VISIBLE) out.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), out);
        }
    }

    private static String allText(MainActivity activity, String page) {
        List<View> views = new ArrayList<>();
        collect(activity.pageCache.get(page), views);
        StringBuilder out = new StringBuilder();
        for (View view : views) if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
        return out.toString();
    }

    private static void click(ActivityScenario<MainActivity> scenario, String page, String label) {
        scenario.onActivity(activity -> {
            List<View> views = new ArrayList<>();
            collect(activity.pageCache.get(page), views);
            TextView match = null;
            for (View view : views) {
                if (view instanceof TextView && !(view instanceof EditText) && ((TextView) view).getText().toString().equals(label)) { match = (TextView) view; break; }
            }
            assertTrue("no visible control labelled '" + label + "' in:\n" + allText(activity, page), match != null);
            assertTrue(label, match.performClick());
        });
    }

    private static <T> T onUi(ActivityScenario<MainActivity> scenario, java.util.function.Function<MainActivity, T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        scenario.onActivity(activity -> result.set(action.apply(activity)));
        return result.get();
    }

    private static void waitFor(String what, Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 45_000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.call()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("timed out waiting for " + what);
    }

    private static void stage(String name) throws InterruptedException {
        Thread.sleep(700L); // let the screen settle before the shell takes the picture
        Log.i(TAG, "stage=" + name);
        Thread.sleep(HOLD_MILLIS);
    }

    @Test public void synthetic_walkthrough_of_the_v422_screens() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        FakeEditorialApiProvider provider = new FakeEditorialApiProvider((request, index) -> {
            if (index == 0) {
                try { release.await(90, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            // the first chunk gets one real edit (the glossary name is added); the others keep their lines
            String answer = EditorialPairTestData.targetedAnswer(request, index, (pointId, draft) -> index == 0 ? draft.replaceFirst("Hàng", "Hanako: Hàng") : draft);
            return FakeEditorialApiProvider.text(answer, "stop");
        });
        EditorialApiUiController.providerOverride = provider;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onUi(scenario, activity -> { activity.switchTab("Editorial"); activity.editorialApi().showList(); activity.editorialApi().newCombo(); return null; });
            stage("01-setup-four-files-default");

            EditorialApiUiController controller = onUi(scenario, activity -> activity.editorialApi());
            onUi(scenario, activity -> { controller.useTwoFiles(true); return null; });
            stage("02-setup-two-files-ticked");
            onUi(scenario, activity -> { controller.useTwoFiles(false); return null; });

            onUi(scenario, activity -> {
                controller.combo.sourceKind = EditorialPairModels.SOURCE_JOB;
                controller.combo.jobId = jobId;
                controller.combo.rawName = "synthetic-v422.txt";
                controller.combo.draftName = "bản dịch của job " + jobId;
                controller.combo.glossaryId = glossaryId;
                controller.combo.pronounId = "";
                controller.refresh();
                return null;
            });
            stage("03-setup-job-source-glossary-only");

            onUi(scenario, activity -> { controller.saveAndContinue("v422-walkthrough", EditorialApiContract.Mode.QUICK, "fake-model", "1.00"); return null; });
            waitFor("warning dialog", () -> controller.lastDialog != null && controller.lastDialog.isShowing());
            stage("04-warning-missing-pronoun");

            onUi(scenario, activity -> { controller.lastDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); return null; });
            waitFor("plan", () -> controller.screen == EditorialApiUiController.Screen.PAIR_CONFIRM && controller.pair.preview != null);
            comboIds.add(controller.combo.id);
            stage("05-plan-verdict");

            click(scenario, "Editorial", "Chạy");
            waitFor("progress card", () -> controller.screen == EditorialApiUiController.Screen.PAIR_RUN && controller.pair.runId > 0
                    && EditorialPairForegroundService.isActive(controller.pair.runId));
            runIds.add(controller.pair.runId);
            stage("06-progress-card-on-the-editorial-screen");

            onUi(scenario, activity -> { activity.switchTab("Jobs"); return null; });
            stage("07-jobs-tab-card");
            onUi(scenario, activity -> { activity.switchTab("Editorial"); return null; });

            release.countDown();
            waitFor("result", () -> controller.screen == EditorialApiUiController.Screen.PAIR_RESULT && !controller.pair.running());
            stage("08-result-summary");
            stage("09-result-changed-lines"); // the shell scrolls the page before this picture

            onUi(scenario, activity -> { activity.switchTab("Settings"); activity.switchSettingsCategory("Biên tập"); return null; });
            stage("10-settings-editorial");
        }
    }
}
