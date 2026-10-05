package com.ml.tblandroidtxt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The Biên tập flow on the device with a fake provider and synthetic text: create a combo, confirm, run, read the result,
 * reopen after the process-level state is gone, and export TXT whose SHA-256 equals the final text. No network.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialApiBienTapFlowInstrumentedTest {
    private static final String RAW = "第一行の原文です。\n\n勇者は静かに言った。\n三人が待っていた。";
    private static final String DRAFT = "Dòng một của bản nháp.\n\nDũng giả nói khẽ.\nBa người đang đợi.";
    private static final String EDITED = "Dòng một của bản nháp.\n\nDũng giả khẽ nói.\nBa người đang đợi.";

    private Context context;
    private File dir;
    private final List<String> glossaryIds = new ArrayList<>();
    private final List<String> pronounIds = new ArrayList<>();
    private final List<Long> comboIds = new ArrayList<>();

    @Before public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        dir = new File(context.getCacheDir(), "bientap-" + UUID.randomUUID());
        assertTrue(dir.mkdirs());
        write("raw.txt", RAW);
        write("draft.txt", DRAFT);
    }

    @After public void tearDown() {
        EditorialApiUiController.providerOverride = null;
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) {
            for (long id : comboIds) store.deleteCombo(id);
        }
        List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
        all.removeIf(g -> glossaryIds.contains(g.id));
        GlossaryStore.saveAll(context, all);
        for (String id : pronounIds) PronounStore.delete(context, id);
    }

    private File write(String name, String text) throws Exception {
        File file = new File(dir, name);
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private void waitFor(String what, java.util.concurrent.Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 20_000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.call()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("timed out waiting for " + what);
    }

    private static void collect(View view, List<String> out) {
        if (view instanceof TextView && view.getVisibility() == View.VISIBLE) out.add(((TextView) view).getText().toString());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), out);
        }
    }

    private static String screenText(Activity activity, MainActivity main) {
        List<String> texts = new ArrayList<>();
        View page = main.pageCache.get("Editorial");
        assertNotNull(page);
        collect(page, texts);
        return String.join("\n", texts);
    }

    private static <T> T onUi(ActivityScenario<MainActivity> scenario, java.util.function.Function<MainActivity, T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        scenario.onActivity(activity -> result.set(action.apply(activity)));
        return result.get();
    }

    private void runCombo(ActivityScenario<MainActivity> scenario, boolean withReferences, EditorialApiContract.Mode mode) throws Exception {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.editsAndPasses(EDITED);
        EditorialApiUiController.providerOverride = provider;
        String glossaryId = "";
        String pronounId = "";
        if (withReferences) {
            GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
            glossary.name = "glossary-bientap-test";
            GlossaryStore.Term term = new GlossaryStore.Term();
            term.source = "勇者";
            term.target = "Dũng giả";
            glossary.terms.add(term);
            List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
            all.add(0, glossary);
            GlossaryStore.saveAll(context, all);
            glossaryIds.add(glossary.id);
            glossaryId = glossary.id;
            PronounStore.Profile profile = PronounStore.importProfile(context, "pronoun-bientap-test",
                    "", "from,speaker,target,self,call,scope,note\n勇者,勇者,仲間,tôi,các bạn,all,\n");
            pronounIds.add(profile.id);
            pronounId = profile.id;
        }
        final String glossary = glossaryId;
        final String pronoun = pronounId;
        EditorialApiUiController controller = onUi(scenario, activity -> {
            activity.switchTab("Editorial");
            EditorialApiUiController c = activity.editorialApi();
            c.newCombo();
            c.combo.rawUri = Uri.fromFile(new File(dir, "raw.txt")).toString();
            c.combo.rawName = "raw001.txt";
            c.combo.draftUri = Uri.fromFile(new File(dir, "draft.txt")).toString();
            c.combo.draftName = "draft001.txt";
            c.combo.glossaryId = glossary;
            c.combo.pronounId = pronoun;
            c.saveAndContinue("", mode, "fake-model", "0.10");
            return c;
        });
        waitFor("confirmation", () -> controller.screen == EditorialApiUiController.Screen.CONFIRM);
        assertTrue(controller.confirmation.canRun());
        String confirmText = onUi(scenario, activity -> screenText(activity, activity));
        assertTrue(confirmText, confirmText.contains("raw001.txt"));
        assertTrue(confirmText, confirmText.contains("draft001.txt"));
        assertTrue(confirmText, confirmText.contains(withReferences ? "glossary-bientap-test" : "Không dùng Glossary"));
        assertTrue(confirmText, confirmText.contains(withReferences ? "pronoun-bientap-test" : "Không dùng Pronoun"));
        assertEquals(withReferences, confirmText.contains("— 1 mục"));
        assertEquals(!withReferences, confirmText.contains("⚠"));
        long comboId = controller.combo.id;
        comboIds.add(comboId);
        assertEquals("raw001+draft001" + (withReferences ? "+glossary-bientap-test+pronoun-bientap-test" : ""), controller.combo.name);

        onUi(scenario, activity -> { activity.editorialApi().startRun(); return null; });
        waitFor("result", () -> controller.screen == EditorialApiUiController.Screen.RESULT && !controller.running());
        assertEquals(mode == EditorialApiContract.Mode.QUICK ? 2 : 2, provider.requests.size());

        String resultText = onUi(scenario, activity -> screenText(activity, activity));
        assertTrue(resultText, resultText.contains("Đã biên tập xong"));
        assertTrue(resultText, resultText.contains("Dũng giả khẽ nói."));

        // the stored run is the final text, and a fresh store (as after a restart) reads the same row without any provider call
        int callsBefore = provider.requests.size();
        EditorialApiRun stored;
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) { stored = store.latestRun(comboId); }
        assertNotNull(stored);
        assertEquals(EditorialApiContract.RunState.FINAL_OK, stored.state);
        assertEquals(EDITED, stored.finalText);
        assertEquals(callsBefore, provider.requests.size());

        // export through the same entry the file picker uses; the file read back must hash like the final text
        File out = new File(dir, "out-" + comboId + ".txt");
        Intent data = new Intent().setData(Uri.fromFile(out));
        onUi(scenario, activity -> activity.editorialApi().onActivityResult(EditorialApiUiController.REQ_EXPORT, Activity.RESULT_OK, data));
        waitFor("exported file", () -> out.isFile() && out.length() > 0);
        waitFor("export readback", () -> HashUtil.sha256(new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8)).equals(HashUtil.sha256(EDITED)));
        assertEquals(HashUtil.sha256(EDITED), HashUtil.sha256(new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8)));

        String all = onUi(scenario, activity -> screenText(activity, activity));
        for (String forbidden : new String[] {"pack", "binding", "SAFE4_BLOCKED", "cấp phép"}) {
            assertFalse(forbidden + " in: " + all, all.toLowerCase().contains(forbidden.toLowerCase()));
        }
    }

    @Test public void comboWithAllFourSourcesRunsQuickAndExports() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runCombo(scenario, true, EditorialApiContract.Mode.QUICK);
        }
    }

    @Test public void comboWithoutGlossaryAndPronounRunsThoroughAndExports() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runCombo(scenario, false, EditorialApiContract.Mode.THOROUGH);
        }
    }

    @Test public void theListAndTheCollapsedDeveloperSectionUseOnlyThePlannedWords() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            String list = onUi(scenario, activity -> {
                activity.switchTab("Editorial");
                activity.editorialApi().showList();
                return screenText(activity, activity);
            });
            assertTrue(list, list.contains("Biên tập"));
            assertTrue(list, list.contains("+ Tổ hợp mới"));
            assertTrue(list, list.contains(EditorialApiPresenter.LEGACY_SECTION_TITLE));
            // collapsed: nothing of the legacy page is visible
            assertFalse(list, list.contains("Tạo project SAFE4"));
            for (String forbidden : new String[] {"binding", "SAFE4_BLOCKED", "cấp phép"}) {
                assertFalse(forbidden + " in: " + list, list.contains(forbidden));
            }
            String expanded = onUi(scenario, activity -> {
                activity.editorialApi().toggleLegacy();
                return screenText(activity, activity);
            });
            assertTrue(expanded, expanded.contains("Tạo project SAFE4"));
            onUi(scenario, activity -> { activity.editorialApi().toggleLegacy(); return null; });
        }
    }
}
