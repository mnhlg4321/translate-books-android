package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * The host test EditorialPairAddressKeepServiceTest over real SQLite: an ADDRESS point that the pronoun rows cannot justify is
 * kept as the DRAFT line, the valid repairs of the same chunk are applied and stored, and a new store over the same database file
 * returns the same candidate, gate and export without any provider call. Synthetic characters only.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialPairAddressKeepSqliteInstrumentedTest {
    private static final String PRONOUN = "from,speaker,target,self,call,scope,note\nリナ,Aki,Rina,mình,Rina,*,close friends\n森先生,Aki,Mori,em,cô,*,teacher\n";
    private static final String GLOSSARY = "竜王\tLong Vương\tname\t\n";
    private static final String RAW = "「リナ……お願い、それを覚えさせて」\n竜王が静かに現れた。\n「それ、ちゃんと覚えたいな」\n\n";
    private static final String DRAFT = "「Rina…… làm ơn, hãy cho tớ học cái đó nhé」\nRồng vương đã lặng lẽ xuất hiện trước mặt.\n「Tớ muốn học thật kỹ cái đó」\n\n";
    private static final String EXPECTED = "「Rina…… làm ơn, hãy cho tớ học cái đó nhé」\nLong Vương đã lặng lẽ xuất hiện trước mặt.\n「Mình muốn học thật kỹ cái đó」";
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private Context context;
    private String databaseName;
    private final List<TranslationRepository> open = new ArrayList<>();

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-pair-keep-" + UUID.randomUUID() + ".db";
    }

    @After public void tearDown() {
        for (TranslationRepository r : open) r.close();
        context.deleteDatabase(databaseName);
    }

    private SqliteEditorialPairRunStore store() {
        TranslationRepository repository = new TranslationRepository(context, databaseName);
        open.add(repository);
        return new SqliteEditorialPairRunStore(repository);
    }

    private static long combo(SqliteEditorialPairRunStore pairs) {
        SQLiteDatabase db = pairs.databaseForTest();
        ContentValues v = new ContentValues();
        v.put("name", "ghép thử");
        v.put("created_at", 1L);
        v.put("updated_at", 1L);
        v.put("source_kind", "JOB");
        v.put("job_id", 5L);
        return db.insertOrThrow("editorial_combos", null, v);
    }

    private static JSONObject point(PairItem item, int id) throws Exception {
        JSONArray points = new JSONObject(item.gateJson).getJSONArray("fixPoints");
        for (int i = 0; i < points.length(); i++) if (points.getJSONObject(i).getInt("id") == id) return points.getJSONObject(i);
        throw new AssertionError("no point " + id);
    }

    @Test public void aKeptAddressPointDoesNotBlockTheValidRepairsAndSurvivesClosingTheDatabase() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> new EditorialApiFlow.StepResponse(
                EditorialPairTestData.targetedAnswer(request, index, (i, d) -> {
                    if (d.contains("Rina")) return d.replace("tớ", "em");
                    if (d.contains("Rồng vương")) return d.replace("Rồng vương", "Long Vương");
                    return d.replace("Tớ", "Mình");
                }), "stop", 1500, 400, new BigDecimal("0.0001"), true, "fake-model", "fake-route", ""));
        long runId;
        try (SqliteEditorialPairRunStore store = store()) {
            long comboId = combo(store);
            EditorialPairRunService service = new EditorialPairRunService(store, fake, PRICING, 60_000L);
            PairRun run = service.prepare(comboId, new EditorialPairSource(EditorialPairModels.SOURCE_JOB, "7", "truyen.txt", "", List.of(RAW), List.of(DRAFT), List.of()),
                    GLOSSARY, PRONOUN, null, "model-x", "Vietnamese", new BigDecimal("0.05"), "C");
            runId = run.id;
            PairRun done = service.execute(runId, null);
            assertEquals(RunState.FINAL_ELIGIBLE, done.state);
            assertEquals(1, fake.requests.size());
        }
        // a new connection over the same file: nothing is sent, everything is read back
        try (SqliteEditorialPairRunStore store = store()) {
            PairRun stored = store.getRun(runId);
            assertNotNull(stored);
            assertEquals(RunState.FINAL_ELIGIBLE, stored.state);
            PairItem item = store.items(runId).get(0);
            assertEquals(PairState.ACCEPTED, item.state);
            assertFalse(item.gateJson.contains("TARGETED_LINE_REJECTED"));
            assertEquals("KEPT", point(item, 1).getString("status"));
            assertEquals("ADDRESS_FORM_NOT_IN_APPLICABLE_ROW", point(item, 1).getString("reason"));
            assertEquals("FIXED", point(item, 2).getString("status"));
            assertEquals("FIXED", point(item, 3).getString("status"));
            assertEquals(EXPECTED, item.candidateText.trim());
            EditorialPairRunService reopened = new EditorialPairRunService(store,
                    new FakeEditorialApiProvider((r, i) -> { throw new AssertionError("no call on reopen"); }), PRICING, 1L);
            assertEquals(EXPECTED, reopened.exportPlan(runId).text.trim());
        }
        assertEquals(1, fake.requests.size());
    }
}
