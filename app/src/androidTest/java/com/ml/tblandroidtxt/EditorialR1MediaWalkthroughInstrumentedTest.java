package com.ml.tblandroidtxt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

/**
 * Release walkthrough for screenshots and video: the Biên tập chunk flow on synthetic rows only (no book text): choose a source,
 * read the plan, run with a scripted provider, read the result by point, export, reopen. It only runs with the instrumentation
 * argument {@code r1_media=run}; every stage holds the screen for a few seconds and logs {@code R1MEDIA stage=...} so the shell
 * can take a screenshot. Nothing here reaches a provider, and the shell script that drives it records the screen.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialR1MediaWalkthroughInstrumentedTest {
    private static final String TAG = "R1MEDIA";
    private static final long HOLD_MILLIS = 4_000L;

    private Context context;
    private long jobId;
    private String glossaryId = "";
    private final List<Long> comboIds = new ArrayList<>();
    private final File exportFile = new File(ApplicationProvider.<Context>getApplicationContext().getFilesDir(), "r1-media-export-" + UUID.randomUUID() + ".txt");

    @Before public void setUp() {
        Assume.assumeTrue("run".equals(InstrumentationRegistry.getArguments().getString("r1_media", "")));
        context = ApplicationProvider.getApplicationContext();
        TranslationRepository repository = new TranslationRepository(context);
        try {
            List<Chunk> chunks = new ArrayList<>();
            int offset = 0;
            for (int i = 1; i <= 3; i++) {
                String raw = EditorialPairTestData.rawRow(i);
                chunks.add(new Chunk(i - 1, offset, offset + raw.length(), "", raw, "", ""));
                offset += raw.length();
            }
            jobId = repository.createJob("content://synthetic/raw", "content://synthetic/out", "synthetic-walkthrough.txt", new AppSettings(), chunks);
            for (int i = 1; i <= 3; i++) repository.markChunkDone(jobId, i - 1, EditorialPairTestData.draftRow(i));
        } finally {
            repository.close();
        }
        GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
        glossary.name = "synthetic-glossary";
        GlossaryStore.Term hanako = new GlossaryStore.Term();
        hanako.source = "花子";
        hanako.target = "Hanako";
        glossary.terms.add(hanako);
        GlossaryStore.Term taro = new GlossaryStore.Term();
        taro.source = "太郎";
        taro.target = "Taro";
        glossary.terms.add(taro);
        List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
        all.add(0, glossary);
        GlossaryStore.saveAll(context, all);
        glossaryId = glossary.id;
    }

    @After public void tearDown() {
        EditorialApiUiController.providerOverride = null;
        if (context == null) return;
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
            for (long id : comboIds) store.deleteCombo(id);
        }
        if (jobId > 0) {
            TranslationRepository repository = new TranslationRepository(context);
            try { repository.deleteJob(jobId); } finally { repository.close(); }
        }
        if (!glossaryId.isEmpty()) {
            List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
            final String id = glossaryId;
            all.removeIf(g -> g.id.equals(id));
            GlossaryStore.saveAll(context, all);
        }
        //noinspection ResultOfMethodCallIgnored
        exportFile.delete();
    }

    private static void collect(View view, List<View> out) {
        if (view.getVisibility() == View.VISIBLE) out.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), out);
        }
    }

    private static String allText(MainActivity activity) {
        List<View> views = new ArrayList<>();
        View page = activity.pageCache.get("Editorial");
        collect(page, views);
        StringBuilder out = new StringBuilder();
        for (View view : views) if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
        return out.toString();
    }

    private static void click(ActivityScenario<MainActivity> scenario, String label) {
        scenario.onActivity(activity -> {
            List<View> views = new ArrayList<>();
            collect(activity.pageCache.get("Editorial"), views);
            TextView match = null;
            for (View view : views) {
                if (view instanceof TextView && !(view instanceof EditText) && ((TextView) view).getText().toString().equals(label)) { match = (TextView) view; break; }
            }
            assertTrue("no visible control labelled '" + label + "' in:\n" + allText(activity), match != null);
            assertTrue(label, match.performClick());
        });
    }

    private static <T> T onUi(ActivityScenario<MainActivity> scenario, java.util.function.Function<MainActivity, T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        scenario.onActivity(activity -> result.set(action.apply(activity)));
        return result.get();
    }

    private static void waitFor(String what, Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.call()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("timed out waiting for " + what);
    }

    private static void stage(String name) throws InterruptedException {
        Thread.sleep(600L); // let the screen settle before the shell takes the picture
        Log.i(TAG, "stage=" + name);
        Thread.sleep(HOLD_MILLIS);
    }

    @Test public void synthetic_walkthrough_of_the_chunk_flow() throws Exception {
        FakeEditorialApiProvider provider = new FakeEditorialApiProvider((request, index) -> EditorialApiFlow_answer(request, index));
        EditorialApiUiController.providerOverride = provider;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onUi(scenario, activity -> { activity.switchTab("Editorial"); activity.editorialApi().showList(); return null; });
            stage("01-list");

            EditorialApiUiController controller = onUi(scenario, activity -> {
                EditorialApiUiController c = activity.editorialApi();
                c.newCombo();
                c.combo.sourceKind = EditorialPairModels.SOURCE_JOB;
                c.combo.jobId = jobId;
                c.combo.rawName = "synthetic-walkthrough.txt";
                c.combo.draftName = "bản dịch của job " + jobId;
                c.combo.glossaryId = glossaryId;
                c.combo.pronounId = "";
                c.refresh();
                return c;
            });
            stage("02-choose-source");

            onUi(scenario, activity -> { controller.saveAndContinue("synthetic-walkthrough", EditorialApiContract.Mode.QUICK, "fake-model", "1.00"); return null; });
            waitFor("plan", () -> controller.screen == EditorialApiUiController.Screen.PAIR_CONFIRM && controller.pair.preview != null);
            comboIds.add(controller.combo.id);
            stage("03-plan-verdict");

            click(scenario, "Chạy");
            waitFor("result", () -> controller.screen == EditorialApiUiController.Screen.PAIR_RESULT && !controller.pair.running());
            stage("04-result-by-point");

            onUi(scenario, activity -> activity.editorialApi().onActivityResult(EditorialPairUiController.REQ_EXPORT, Activity.RESULT_OK,
                    new Intent().setData(Uri.fromFile(exportFile))));
            waitFor("export file", () -> exportFile.exists() && exportFile.length() > 0);
            stage("05-exported-txt");

            click(scenario, "Về danh sách");
            stage("06-list-with-result");
        }
    }

    /** Edits each flagged glossary point of a synthetic row; nothing leaves the process. */
    private static com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.StepResponse EditorialApiFlow_answer(
            com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.Request request, int index) {
        String answer = EditorialPairTestData.targetedAnswer(request, index, (call, draft) -> draft);
        return FakeEditorialApiProvider.text(answer, "stop");
    }
}
