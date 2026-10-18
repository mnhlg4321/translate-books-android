package com.ml.tblandroidtxt;

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

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * v4.22 R (F3) on the device with real SQLite and the real result screen, synthetic characters only: a refused ADDRESS answer is
 * shown under "Đề xuất bị chặn — chờ bạn quyết", the button applies exactly that line into the export, the journal records it,
 * it survives a new connection and cannot be applied twice; a pronoun cell with "/" forms lets the same answer through without
 * a proposal. Process-kill phases run with {@code -e r_restart seed|verify} (force-stop in between). With
 * {@code -e v422_media run} the screen is held before and after applying so the shell can take pictures. No provider is reached.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialRefusedProposalInstrumentedTest {
    private static final String TAG = "V422MEDIA";
    private static final String PRONOUN = "from,speaker,target,self,call,scope,note\nリナ,Aki,Rina,mình,Rina,*,close friends\n森先生,Aki,Mori,em,cô,*,teacher\n";
    private static final String PRONOUN_SLASH = "from,speaker,target,self,call,scope,note\nリナ,Aki,Rina,mình/em,Rina,*,close friends\n森先生,Aki,Mori,em,cô,*,teacher\n";
    private static final String GLOSSARY = "竜王\tLong Vương\tname\t\n";
    private static final String RAW = "「リナ……お願い、それを覚えさせて」\n竜王が静かに現れた。\n「それ、ちゃんと覚えたいな」\n\n";
    private static final String DRAFT = "「Rina…… làm ơn, hãy cho tớ học cái đó nhé」\nRồng vương đã lặng lẽ xuất hiện trước mặt.\n「Tớ muốn học thật kỹ cái đó」\n\n";
    private static final String PROPOSED = "「Rina…… làm ơn, hãy cho em học cái đó nhé」";
    private static final String APPLIED = PROPOSED + "\nLong Vương đã lặng lẽ xuất hiện trước mặt.\n「Mình muốn học thật kỹ cái đó」";
    private static final String MARKER = "r-restart-ids.txt";
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private Context context;
    private final List<Long> comboIds = new ArrayList<>();
    private final List<Long> runIds = new ArrayList<>();

    @Before public void setUp() { context = ApplicationProvider.getApplicationContext(); }

    @After public void tearDown() {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(context)) { for (long id : runIds) store.deleteRun(id); }
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(context)) { for (long id : comboIds) store.deleteCombo(id); }
    }

    private static FakeEditorialApiProvider provider() {
        return new FakeEditorialApiProvider((request, index) -> new EditorialApiFlow.StepResponse(
                EditorialPairTestData.targetedAnswer(request, index, (i, d) -> {
                    if (d.contains("Rina")) return "R1 " + d.replace("tớ", "em");
                    if (d.contains("Rồng vương")) return d.replace("Rồng vương", "Long Vương");
                    return "R1 " + d.replace("Tớ", "Mình");
                }), "stop", 1500, 400, new BigDecimal("0.0001"), true, "fake-model", "fake-route", ""));
    }

    /** Creates a combo and a finished run in the app database; returns {comboId, runId}. */
    private long[] finishedRun(String pronoun, List<Long> combos, List<Long> runs) throws Exception {
        long comboId;
        try (SqliteEditorialApiStore api = new SqliteEditorialApiStore(context)) {
            EditorialApiCombo combo = new EditorialApiCombo();
            combo.name = "r-proposal-synthetic";
            combo.sourceKind = EditorialPairModels.SOURCE_JOB;
            comboId = api.insertCombo(combo);
        }
        combos.add(comboId);
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(context)) {
            EditorialPairRunService service = new EditorialPairRunService(store, provider(), PRICING, 60_000L);
            PairRun prepared = service.prepare(comboId, new EditorialPairSource(EditorialPairModels.SOURCE_JOB, "7", "synthetic.txt", "", List.of(RAW), List.of(DRAFT), List.of()),
                    GLOSSARY, pronoun, null, "fake-model", "Vietnamese", new BigDecimal("0.05"), "C");
            runs.add(prepared.id);
            PairRun done = service.execute(prepared.id, null);
            assertEquals(RunState.FINAL_ELIGIBLE, done.state);
            return new long[]{comboId, prepared.id};
        }
    }

    private static JSONObject point(PairItem item, int id) throws Exception {
        JSONArray points = new JSONObject(item.gateJson).getJSONArray("fixPoints");
        for (int i = 0; i < points.length(); i++) if (points.getJSONObject(i).getInt("id") == id) return points.getJSONObject(i);
        throw new AssertionError("no point " + id);
    }

    private PairItem onlyItem(long runId) {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(context)) { return store.items(runId).get(0); }
    }

    private String exportText(long runId) {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(context)) {
            return new EditorialPairRunService(store, new FakeEditorialApiProvider((r, i) -> { throw new AssertionError("no call"); }), PRICING, 1L).exportPlan(runId).text.trim();
        }
    }

    @Test public void theResultScreenShowsTheRefusedProposalAndItsButtonAppliesExactlyThatLine() throws Exception {
        long[] ids = finishedRun(PRONOUN, comboIds, runIds);
        long comboId = ids[0], runId = ids[1];
        assertEquals("KEPT", point(onlyItem(runId), 1).getString("status"));
        assertEquals(APPLIED.replace(PROPOSED, "「Rina…… làm ơn, hãy cho tớ học cái đó nhé」"), exportText(runId));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> { activity.switchTab("Editorial"); activity.editorialApi().pair.openResult(comboId); });
            String before = text(scenario);
            assertTrue(before, before.contains("Đề xuất bị chặn — chờ bạn quyết"));
            assertTrue(before, before.contains(PROPOSED));
            media("12-refused-proposal-waiting");

            click(scenario, "Áp dụng đề xuất");
            String after = text(scenario);
            assertFalse("the button is gone once applied:\n" + after, after.contains("Áp dụng đề xuất"));
            media("13-refused-proposal-applied");
        }

        assertEquals(APPLIED, exportText(runId));
        JSONObject owner = point(onlyItem(runId), 1);
        assertEquals("OWNER_APPLIED", owner.getString("status"));
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(context)) {
            assertTrue(store.journal(runId).stream().anyMatch(e -> e.endsWith("PROPOSAL_APPLIED id=1")));
            String again = new EditorialPairRunService(store, new FakeEditorialApiProvider((r, i) -> { throw new AssertionError("no call"); }), PRICING, 1L)
                    .applyProposal(runId, onlyItem(runId).pairId, 1);
            assertEquals("Không có đề xuất để áp dụng.", again);
        }
        assertEquals(APPLIED, exportText(runId));
    }

    @Test public void aSlashCellLetsTheSameAnswerThroughWithoutAProposal() throws Exception {
        long runId = finishedRun(PRONOUN_SLASH, comboIds, runIds)[1];
        PairItem item = onlyItem(runId);
        assertEquals("FIXED", point(item, 1).getString("status"));
        assertFalse(point(item, 1).has("proposed"));
        assertEquals(APPLIED, exportText(runId));
    }

    /** Phase 1: run and apply, record the ids; the shell then force-stops the app. */
    @Test public void restartSeed() throws Exception {
        Assume.assumeTrue("seed".equals(InstrumentationRegistry.getArguments().getString("r_restart", "")));
        List<Long> keepCombos = new ArrayList<>(), keepRuns = new ArrayList<>();
        long[] ids = finishedRun(PRONOUN, keepCombos, keepRuns);
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(context)) {
            assertEquals(null, new EditorialPairRunService(store, provider(), PRICING, 1L).applyProposal(ids[1], store.items(ids[1]).get(0).pairId, 1));
        }
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), MARKER))) {
            out.write((ids[0] + "," + ids[1]).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Phase 2 after force-stop: the applied line is still in the export and the proposal is closed; then everything is removed. */
    @Test public void restartVerify() throws Exception {
        Assume.assumeTrue("verify".equals(InstrumentationRegistry.getArguments().getString("r_restart", "")));
        File marker = new File(context.getFilesDir(), MARKER);
        assertTrue(marker.isFile());
        String[] parts;
        try (FileInputStream in = new FileInputStream(marker)) {
            byte[] b = new byte[(int) marker.length()];
            assertEquals(b.length, in.read(b));
            parts = new String(b, StandardCharsets.UTF_8).split(",");
        }
        comboIds.add(Long.parseLong(parts[0]));
        long runId = Long.parseLong(parts[1]);
        runIds.add(runId);
        assertEquals(APPLIED, exportText(runId));
        assertEquals("OWNER_APPLIED", point(onlyItem(runId), 1).getString("status"));
        assertTrue(marker.delete());
    }

    private static void media(String stage) throws InterruptedException {
        if (!"run".equals(InstrumentationRegistry.getArguments().getString("v422_media", ""))) return;
        Thread.sleep(700L);
        Log.i(TAG, "stage=" + stage);
        Thread.sleep(5_000L);
    }

    private static void collect(View view, List<View> out) {
        if (view == null) return;
        if (view.getVisibility() == View.VISIBLE) out.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), out);
        }
    }

    private static String text(ActivityScenario<MainActivity> scenario) {
        AtomicReference<String> result = new AtomicReference<>();
        scenario.onActivity(activity -> {
            List<View> views = new ArrayList<>();
            collect(activity.pageCache.get("Editorial"), views);
            StringBuilder out = new StringBuilder();
            for (View view : views) if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
            result.set(out.toString());
        });
        return result.get();
    }

    private static void click(ActivityScenario<MainActivity> scenario, String label) {
        scenario.onActivity(activity -> {
            List<View> views = new ArrayList<>();
            collect(activity.pageCache.get("Editorial"), views);
            TextView match = null;
            for (View view : views) {
                if (view instanceof TextView && !(view instanceof EditText) && ((TextView) view).getText().toString().equals(label)) { match = (TextView) view; break; }
            }
            assertTrue("no visible control labelled '" + label + "'", match != null);
            assertTrue(label, match.performClick());
        });
    }
}
