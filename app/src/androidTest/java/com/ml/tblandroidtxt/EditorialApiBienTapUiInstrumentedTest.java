package com.ml.tblandroidtxt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * Real view traversal of the Biên tập tab: buttons, radio buttons, text fields and a dialog list are found in the rendered
 * tree and driven through their own listeners, the way a finger would. What it does NOT traverse is the system file picker and
 * the system "save as" screen: those two results are delivered to {@code onActivityResult} directly, and
 * {@link EditorialApiBienTapFlowInstrumentedTest} stays a controller-level test (it sets combo fields and calls the
 * controller). Nothing here reaches a provider.
 *
 * <p>The second half is a two-phase test for reopening after the process is gone: phase {@code seed} creates a combo and a
 * stored result, the shell force-stops the app, phase {@code verify} opens the same combo from the list and reads the stored
 * result with a provider that fails the test if it is ever called, phase {@code cleanup} deletes it. Without the
 * {@code bientap_phase} argument those three tests are skipped.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialApiBienTapUiInstrumentedTest {
    private static final String RAW = "第一行の原文です。\n勇者は静かに言った。";
    private static final String DRAFT = "Dòng một của bản nháp.\nDũng giả nói khẽ.";
    private static final String EDITED = "Dòng một của bản nháp.\nDũng giả khẽ nói.";
    private static final String REOPEN_NAME = "bientap-reopen-synthetic";

    private Context context;
    private File dir;
    private String glossaryId = "";
    private final List<Long> comboIds = new ArrayList<>();

    @Before public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        dir = new File(context.getFilesDir(), "bientap-ui-" + UUID.randomUUID());
        assertTrue(dir.mkdirs());
        Files.write(new File(dir, "raw.txt").toPath(), RAW.getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "draft.txt").toPath(), DRAFT.getBytes(StandardCharsets.UTF_8));
    }

    @After public void tearDown() {
        EditorialApiUiController.providerOverride = null;
        if (!glossaryId.isEmpty()) {
            List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
            final String id = glossaryId;
            all.removeIf(g -> g.id.equals(id));
            GlossaryStore.saveAll(context, all);
        }
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
            for (long id : comboIds) store.deleteCombo(id);
            for (EditorialApiCombo combo : store.listCombos()) {
                if (combo.name.equals("tên gõ dở") || combo.name.equals("ui-result-combo")) store.deleteCombo(combo.id);
            }
        }
    }

    // ---- view helpers ----

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

    private static List<EditText> fields(MainActivity activity) {
        List<EditText> found = new ArrayList<>();
        for (View view : views(activity)) if (view instanceof EditText) found.add((EditText) view);
        return found;
    }

    /** A real click: the button's own listener runs on the UI thread. */
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

    private Intent fileResult(String name) { return new Intent().setData(Uri.fromFile(new File(dir, name))); }

    private void openTab(ActivityScenario<MainActivity> scenario) {
        onUi(scenario, activity -> {
            activity.switchTab("Editorial");
            activity.editorialApi().showList();
            return null;
        });
    }

    // ---- unsaved form survives choosing sources; save without dispatch; reopen the same combo ----

    @Test public void typedFormValuesSurviveChoosingAGlossaryAndSavingKeepsThemForTheSameCombo() throws Exception {
        GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
        glossary.name = "glossary-bientap-ui";
        GlossaryStore.Term term = new GlossaryStore.Term();
        term.source = "勇者";
        term.target = "Dũng giả";
        glossary.terms.add(term);
        List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
        all.add(0, glossary);
        GlossaryStore.saveAll(context, all);
        glossaryId = glossary.id;
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.editsAndPasses(EDITED);
        EditorialApiUiController.providerOverride = provider;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            click(scenario, "+ Tổ hợp mới", 0);
            // the file picker is the system's; its two results are delivered the way the activity receives them
            onUi(scenario, activity -> activity.editorialApi().onActivityResult(EditorialApiUiController.REQ_RAW, Activity.RESULT_OK, fileResult("raw.txt")));
            onUi(scenario, activity -> activity.editorialApi().onActivityResult(EditorialApiUiController.REQ_DRAFT, Activity.RESULT_OK, fileResult("draft.txt")));

            // type into the fields and pick "Kỹ" with real views
            onUi(scenario, activity -> {
                List<EditText> fields = fields(activity);
                assertEquals(3, fields.size());
                fields.get(0).setText("tên gõ dở");
                fields.get(1).setText("vendor/model-gõ-dở");
                fields.get(2).setText("0.25");
                for (View view : views(activity)) {
                    if (view instanceof RadioButton && ((RadioButton) view).getText().toString().equals("Kỹ")) ((RadioButton) view).setChecked(true);
                }
                return null;
            });

            // choose the glossary through the dialog list; this rebuilds the screen
            click(scenario, "Đổi", 0);
            waitFor("glossary dialog", () -> onUi(scenario, activity -> activity.editorialApi().lastDialog != null
                    && activity.editorialApi().lastDialog.isShowing()));
            onUi(scenario, activity -> {
                android.app.AlertDialog dialog = activity.editorialApi().lastDialog;
                int index = -1;
                for (int i = 0; i < dialog.getListView().getCount(); i++) {
                    if (String.valueOf(dialog.getListView().getItemAtPosition(i)).startsWith("glossary-bientap-ui")) index = i;
                }
                assertTrue("glossary item present", index >= 0);
                assertTrue(dialog.getListView().performItemClick(null, index, dialog.getListView().getItemIdAtPosition(index)));
                return null;
            });

            String after = onUi(scenario, activity -> {
                List<EditText> fields = fields(activity);
                StringBuilder values = new StringBuilder();
                for (EditText field : fields) values.append(field.getText()).append('|');
                for (View view : views(activity)) {
                    if (view instanceof RadioButton && ((RadioButton) view).isChecked()) values.append(((RadioButton) view).getText());
                }
                return values + "\n" + allText(activity);
            });
            assertTrue(after, after.startsWith("tên gõ dở|vendor/model-gõ-dở|0.25|Kỹ\n"));
            assertTrue(after, after.contains("glossary-bientap-ui"));
            assertTrue(after, after.contains("raw.txt") && after.contains("draft.txt"));

            // save without sending anything, back to the list
            click(scenario, "Lưu", 0);
            String list = onUi(scenario, MainActivity -> allText(MainActivity));
            assertTrue(list, list.contains("tên gõ dở"));
            assertEquals("saving never reaches the provider", 0, provider.requests.size());
            EditorialApiCombo stored;
            try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
                stored = store.listCombos().get(0);
                comboIds.add(stored.id);
            }
            assertEquals("tên gõ dở", stored.name);
            assertEquals(glossaryId, stored.glossaryId);
            assertEquals(EditorialApiContract.Mode.THOROUGH, stored.settings().mode);
            assertEquals("vendor/model-gõ-dở", stored.settings().model);
            assertEquals("0.25", stored.settings().maxUsdPerChapter.toPlainString());

            // reopen the same combo from the list: the values come back from the database, not from memory
            click(scenario, "Sửa", 0);
            String reopened = onUi(scenario, activity -> {
                List<EditText> fields = fields(activity);
                return fields.get(0).getText() + "|" + fields.get(1).getText() + "|" + fields.get(2).getText();
            });
            assertEquals("tên gõ dở|vendor/model-gõ-dở|0.25", reopened);
            assertEquals(stored.id, onUi(scenario, activity -> activity.editorialApi().combo.id).longValue());
        }
    }

    @Test public void aFinishedRunIsOpenedFromTheListWithItsStoredResult() throws Exception {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.editsAndPasses(EDITED);
        EditorialApiUiController.providerOverride = provider;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            EditorialApiUiController controller = onUi(scenario, activity -> {
                EditorialApiUiController c = activity.editorialApi();
                c.newCombo();
                c.combo.rawUri = Uri.fromFile(new File(dir, "raw.txt")).toString();
                c.combo.rawName = "raw.txt";
                c.combo.draftUri = Uri.fromFile(new File(dir, "draft.txt")).toString();
                c.combo.draftName = "draft.txt";
                c.saveAndContinue("ui-result-combo", EditorialApiContract.Mode.QUICK, "fake-model", "1.00");
                return c;
            });
            waitFor("confirmation", () -> controller.screen == EditorialApiUiController.Screen.CONFIRM);
            comboIds.add(controller.combo.id);
            click(scenario, "Chạy", 0);
            waitFor("result", () -> controller.screen == EditorialApiUiController.Screen.RESULT && !controller.running());
            int sent = provider.requests.size();
            click(scenario, "Về danh sách", 0);
            click(scenario, "Xem kết quả", 0);
            String shown = onUi(scenario, activity -> allText(activity));
            assertTrue(shown, shown.contains("Dũng giả khẽ nói."));
            assertEquals("opening a stored result sends nothing", sent, provider.requests.size());
        }
    }

    // ---- two phases around a process death ----

    private static String phase() {
        return InstrumentationRegistry.getArguments().getString("bientap_phase", "");
    }

    @Test public void reopenPhaseSeed() throws Exception {
        Assume.assumeTrue("seed".equals(phase()));
        EditorialApiUiController.providerOverride = FakeEditorialApiProvider.editsAndPasses(EDITED);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            EditorialApiUiController controller = onUi(scenario, activity -> {
                EditorialApiUiController c = activity.editorialApi();
                c.newCombo();
                c.combo.rawUri = Uri.fromFile(new File(dir, "raw.txt")).toString();
                c.combo.rawName = "raw.txt";
                c.combo.draftUri = Uri.fromFile(new File(dir, "draft.txt")).toString();
                c.combo.draftName = "draft.txt";
                c.saveAndContinue(REOPEN_NAME, EditorialApiContract.Mode.QUICK, "fake-model", "1.00");
                return c;
            });
            waitFor("confirmation", () -> controller.screen == EditorialApiUiController.Screen.CONFIRM);
            click(scenario, "Chạy", 0);
            waitFor("result", () -> controller.screen == EditorialApiUiController.Screen.RESULT && !controller.running());
        }
        // the combo and its run stay on purpose: the shell force-stops the app and runs the verify phase
        comboIds.clear();
    }

    @Test public void reopenPhaseVerify() throws Exception {
        Assume.assumeTrue("verify".equals(phase()));
        FakeEditorialApiProvider mustNotBeCalled = new FakeEditorialApiProvider((request, index) -> {
            throw new AssertionError("reopening must never call the provider");
        });
        EditorialApiUiController.providerOverride = mustNotBeCalled;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openTab(scenario);
            String list = onUi(scenario, activity -> allText(activity));
            assertTrue(list, list.contains(REOPEN_NAME));
            assertTrue(list, list.contains("Xong"));
            click(scenario, "Xem kết quả", 0);
            String shown = onUi(scenario, activity -> allText(activity));
            assertTrue(shown, shown.contains("Đã biên tập xong"));
            assertTrue(shown, shown.contains("Dũng giả khẽ nói."));
            assertFalse(shown, shown.contains("gián đoạn"));
            assertEquals(0, mustNotBeCalled.requests.size());
        }
    }

    @Test public void reopenPhaseCleanup() {
        Assume.assumeTrue("cleanup".equals(phase()));
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
            for (EditorialApiCombo combo : store.listCombos()) if (REOPEN_NAME.equals(combo.name)) store.deleteCombo(combo.id);
        }
    }
}
