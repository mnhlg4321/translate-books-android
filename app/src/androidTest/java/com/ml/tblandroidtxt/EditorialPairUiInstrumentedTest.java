package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The chunk-pair flow of the Biên tập tab through real views: choosing a translation job, explicitly dropping the glossary and
 * pronoun lists, saving, reading the preview, running with a scripted provider, reading the result and exporting. As in the
 * whole-chapter UI test, the system "save as" result is delivered to {@code onActivityResult} directly, and nothing here reaches
 * a real provider.
 *
 * <p>The two-phase tests ({@code bientap_phase}=seed/verify/cleanup) reopen a stored pair run after the shell force-stops the
 * app; without that argument they are skipped.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialPairUiInstrumentedTest {
    private static final String REOPEN_NAME = "cap-reopen-synthetic";

    private Context context;
    private long jobId;
    private final List<Long> comboIds = new ArrayList<>();
    private final File exportFile = new File(ApplicationProvider.<Context>getApplicationContext().getFilesDir(), "pair-export-" + UUID.randomUUID() + ".txt");

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        jobId = "verify".equals(phase()) || "cleanup".equals(phase()) ? 0 : seedJob(3);
    }

    @After public void tearDown() {
        EditorialApiUiController.providerOverride = null;
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
            for (long id : comboIds) store.deleteCombo(id);
        }
        if (jobId > 0 && !"seed".equals(phase())) deleteJob(jobId);
        //noinspection ResultOfMethodCallIgnored
        exportFile.delete();
    }

    private long seedJob(int rows) {
        TranslationRepository repository = new TranslationRepository(context);
        try {
            List<Chunk> chunks = new ArrayList<>();
            int offset = 0;
            for (int i = 1; i <= rows; i++) {
                String raw = EditorialPairTestData.rawRow(i);
                chunks.add(new Chunk(i - 1, offset, offset + raw.length(), "", raw, "", ""));
                offset += raw.length();
            }
            long id = repository.createJob("content://synthetic/raw", "content://synthetic/out", "cap-synthetic.txt", new AppSettings(), chunks);
            for (int i = 1; i <= rows; i++) repository.markChunkDone(id, i - 1, EditorialPairTestData.draftRow(i));
            return id;
        } finally {
            repository.close();
        }
    }

    private void deleteJob(long id) {
        TranslationRepository repository = new TranslationRepository(context);
        try { repository.deleteJob(id); } finally { repository.close(); }
    }

    private static FakeEditorialApiProvider noPointProvider() {
        return new FakeEditorialApiProvider((request, index) -> {
            throw new AssertionError("a chunk without fix points must not call the provider");
        });
    }

    // ---- view helpers

    private static void collect(View view, List<View> out) {
        if (view.getVisibility() == View.VISIBLE) out.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), out);
        }
    }

    private static List<View> views(MainActivity activity) {
        List<View> out = new ArrayList<>();
        View page = activity.pageCache.get("Editorial");
        assertNotNull(page);
        collect(page, out);
        return out;
    }

    private static List<TextView> texts(MainActivity activity, String exact) {
        List<TextView> found = new ArrayList<>();
        for (View view : views(activity)) {
            if (view instanceof TextView && !(view instanceof EditText) && ((TextView) view).getText().toString().equals(exact)) found.add((TextView) view);
        }
        return found;
    }

    private static String allText(MainActivity activity) {
        StringBuilder out = new StringBuilder();
        for (View view : views(activity)) if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
        return out.toString();
    }

    private static void click(ActivityScenario<MainActivity> scenario, String label, int index) {
        scenario.onActivity(activity -> {
            List<TextView> matches = texts(activity, label);
            assertTrue("no visible control labelled '" + label + "' in:\n" + allText(activity), matches.size() > index);
            assertTrue(label, matches.get(index).performClick());
        });
    }

    private static <T> T onUi(ActivityScenario<MainActivity> scenario, java.util.function.Function<MainActivity, T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        scenario.onActivity(activity -> result.set(action.apply(activity)));
        return result.get();
    }

    private static void waitFor(String what, Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 20_000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.call()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("timed out waiting for " + what);
    }

    private static void openTab(ActivityScenario<MainActivity> scenario) {
        onUi(scenario, activity -> {
            activity.switchTab("Editorial");
            activity.editorialApi().showList();
            return null;
        });
    }

    private EditorialApiUiController configureJobCombo(ActivityScenario<MainActivity> scenario, String name) {
        return onUi(scenario, activity -> {
            EditorialApiUiController c = activity.editorialApi();
            c.newCombo();
            c.combo.sourceKind = EditorialPairModels.SOURCE_JOB;
            c.combo.jobId = jobId;
            c.combo.rawName = "cap-synthetic.txt";
            c.combo.draftName = "bản dịch của job " + jobId;
            c.combo.glossaryId = "";
            c.combo.pronounId = "";
            c.saveAndContinue(name, EditorialApiContract.Mode.QUICK, "fake-model", "1.00");
            return c;
        });
    }

    // ---- the flow

    @Test public void aJobComboIsSavedPreviewedRunAndReadBackWithoutTouchingTheJob() throws Exception {
        FakeEditorialApiProvider provider = noPointProvider();
        EditorialApiUiController.providerOverride = provider;
        String jobBefore = jobFingerprint();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            EditorialApiUiController controller = configureJobCombo(scenario, "cap-ui-combo");
            waitFor("pair preview", () -> controller.screen == EditorialApiUiController.Screen.PAIR_CONFIRM && controller.pair.preview != null);
            comboIds.add(controller.combo.id);
            assertTrue("no pair is missing and nothing blocks the run", controller.pair.preview.runnable());
            assertEquals(3, controller.pair.preview.rows.size());
            assertEquals("a preview sends nothing", 0, provider.requests.size());

            click(scenario, "Chạy", 0);
            waitFor("result", () -> controller.screen == EditorialApiUiController.Screen.PAIR_RESULT && !controller.pair.running());
            assertEquals("the synthetic job has no deterministic fix points", 0, provider.requests.size());
            String shown = onUi(scenario, activity -> allText(activity));
            assertTrue(shown, shown.contains("Đã ghép đủ các đoạn"));
            // structural result only: the screen must not say the merge is a proof of good translation
            assertFalse(shown, shown.toLowerCase(java.util.Locale.ROOT).contains("đã đạt"));

            // export is delivered the way the system "save as" screen delivers it
            onUi(scenario, activity -> activity.editorialApi().onActivityResult(EditorialPairUiController.REQ_EXPORT, android.app.Activity.RESULT_OK,
                    new Intent().setData(Uri.fromFile(exportFile))));
            waitFor("export file", () -> exportFile.exists() && exportFile.length() > 0);

            click(scenario, "Về danh sách", 0);
            String list = onUi(scenario, activity -> allText(activity));
            assertTrue(list, list.contains("cap-ui-combo"));
            int sent = provider.requests.size();
            click(scenario, "Xem kết quả", 0);
            assertEquals("opening a stored pair result sends nothing", sent, provider.requests.size());
        }
        assertEquals("the translation job is read, never written", jobBefore, jobFingerprint());
    }

    private String jobFingerprint() {
        TranslationRepository repository = new TranslationRepository(context);
        try {
            StringBuilder sb = new StringBuilder();
            TranslationRepository.Job job = repository.getJob(jobId);
            sb.append(job.status).append('|').append(job.outputUri).append('\n');
            for (TranslationRepository.ChunkRow r : repository.getChunkRows(jobId)) {
                sb.append(r.index).append('|').append(r.status).append('|').append(r.translated).append('|').append(r.responseHash).append('\n');
            }
            return sb.toString();
        } finally {
            repository.close();
        }
    }

    @Test public void droppingTheGlossaryAndPronounIsAnExplicitSavedChoice() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            onUi(scenario, activity -> {
                EditorialApiUiController c = activity.editorialApi();
                c.newCombo();
                c.combo.sourceKind = EditorialPairModels.SOURCE_JOB;
                c.combo.jobId = jobId;
                c.combo.rawName = "cap-synthetic.txt";
                c.combo.glossaryId = "g-gone";
                c.combo.pronounId = "p-gone";
                c.refresh();
                return null;
            });
            click(scenario, "Bỏ", 0);
            click(scenario, "Bỏ", 0);
            click(scenario, "Lưu", 0);
            EditorialApiCombo stored;
            try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
                stored = store.listCombos().get(0);
                comboIds.add(stored.id);
            }
            assertEquals("", stored.glossaryId);
            assertEquals("", stored.pronounId);
            assertEquals(EditorialPairModels.SOURCE_JOB, stored.sourceKind);
            assertEquals(jobId, stored.jobId);
        }
    }

    @Test public void twoFilesAreCutIntoChunksAndTheFormSaysSo() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            onUi(scenario, activity -> {
                EditorialApiUiController c = activity.editorialApi();
                c.newCombo();
                c.refresh();
                return null;
            });
            String shown = onUi(scenario, activity -> allText(activity));
            assertTrue(shown, shown.contains("Ứng dụng tự căn dòng RAW với DRAFT"));
        }
    }

    // ---- two phases around a process death

    private static String phase() {
        return InstrumentationRegistry.getArguments().getString("bientap_phase", "");
    }

    @Test public void pairReopenPhaseSeed() throws Exception {
        Assume.assumeTrue("seed".equals(phase()));
        EditorialApiUiController.providerOverride = noPointProvider();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            EditorialApiUiController controller = configureJobCombo(scenario, REOPEN_NAME);
            waitFor("pair preview", () -> controller.screen == EditorialApiUiController.Screen.PAIR_CONFIRM && controller.pair.preview != null);
            click(scenario, "Chạy", 0);
            waitFor("result", () -> controller.screen == EditorialApiUiController.Screen.PAIR_RESULT && !controller.pair.running());
        }
        // the combo, its run and the job stay on purpose: the shell force-stops the app and runs the verify phase
        comboIds.clear();
    }

    @Test public void pairReopenPhaseVerify() throws Exception {
        Assume.assumeTrue("verify".equals(phase()));
        FakeEditorialApiProvider mustNotBeCalled = new FakeEditorialApiProvider((request, index) -> {
            throw new AssertionError("reopening must never call the provider");
        });
        EditorialApiUiController.providerOverride = mustNotBeCalled;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            String list = onUi(scenario, activity -> allText(activity));
            assertTrue(list, list.contains(REOPEN_NAME));
            click(scenario, "Xem kết quả", 0);
            String shown = onUi(scenario, activity -> allText(activity));
            assertTrue(shown, shown.contains("Đã ghép đủ các đoạn"));
            assertFalse(shown, shown.contains("gián đoạn"));
            assertEquals(0, mustNotBeCalled.requests.size());
        }
    }

    @Test public void pairReopenPhaseCleanup() {
        Assume.assumeTrue("cleanup".equals(phase()));
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
            for (EditorialApiCombo combo : store.listCombos()) if (combo.name.equals(REOPEN_NAME)) store.deleteCombo(combo.id);
        }
        TranslationRepository repository = new TranslationRepository(context);
        try {
            for (TranslationRepository.JobSummary s : repository.getJobSummaries(100)) if ("cap-synthetic.txt".equals(s.fileName)) repository.deleteJob(s.id);
        } finally {
            repository.close();
        }
    }
}
