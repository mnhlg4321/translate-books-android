package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditGuards;
import com.ml.tblandroidtxt.editorial.api.EditResponseParser;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Maps a flow outcome onto the stored run and back; the JSON columns hold only app-authored fields and the model's quotes. */
final class EditorialApiRunCodec {
    private EditorialApiRunCodec() { }

    static void applyProgress(EditorialApiRun run, List<EditorialApiFlow.StepRecord> steps, String editedSoFar) {
        run.stepsJson = stepsJson(steps);
        run.calls = countCalls(steps);
        long in = 0;
        long out = 0;
        BigDecimal usd = BigDecimal.ZERO;
        boolean known = true;
        for (EditorialApiFlow.StepRecord step : steps) {
            in += step.inputTokens();
            out += step.outputTokens();
            usd = usd.add(step.cost());
            known &= step.costKnown();
        }
        run.inputTokens = in;
        run.outputTokens = out;
        run.usd = usd;
        run.costKnown = known;
        run.editedText = editedSoFar == null ? "" : editedSoFar;
    }

    static void applyOutcome(EditorialApiRun run, EditorialApiFlow.Outcome outcome) {
        applyProgress(run, outcome.steps(), outcome.editedText());
        run.state = outcome.state();
        run.finalText = outcome.finalText();
        run.wrongPairEvidence = outcome.wrongPairEvidence();
        run.notesJson = notesJson(outcome.notes(), outcome.notesDropped());
        run.issuesJson = issuesJson(outcome.issues());
        run.guardsJson = guardsJson(outcome);
    }

    /** Steps that really reached the provider; a step skipped by the app (cost cap) is not a call. */
    private static int countCalls(List<EditorialApiFlow.StepRecord> steps) {
        int n = 0;
        for (EditorialApiFlow.StepRecord step : steps) if (!step.result().equals("COST_CAP")) n++;
        return n;
    }

    static String stepsJson(List<EditorialApiFlow.StepRecord> steps) {
        JSONArray array = new JSONArray();
        try {
            for (EditorialApiFlow.StepRecord s : steps) {
                array.put(new JSONObject().put("step", s.step().name()).put("attempt", s.attempt())
                        .put("finish", s.finishReason()).put("in", s.inputTokens()).put("out", s.outputTokens())
                        .put("usd", s.cost().toPlainString()).put("costKnown", s.costKnown()).put("model", s.model())
                        .put("route", s.route()).put("result", s.result()));
            }
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
        return array.toString();
    }

    private static String notesJson(List<EditResponseParser.Note> notes, int dropped) {
        JSONArray array = new JSONArray();
        try {
            for (EditResponseParser.Note n : notes) {
                array.put(new JSONObject().put("quote", n.quote()).put("kind", n.kind()).put("reason", n.reason()));
            }
            if (dropped > 0) array.put(new JSONObject().put("dropped", dropped));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
        return array.toString();
    }

    private static String issuesJson(List<EditorialApiFlow.IssueRecord> issues) {
        JSONArray array = new JSONArray();
        try {
            for (EditorialApiFlow.IssueRecord r : issues) {
                array.put(new JSONObject().put("step", r.step().name()).put("status", r.status().name()).put("reason", r.reason())
                        .put("kind", r.issue().kind().name()).put("editedQuote", r.issue().editedQuote())
                        .put("rawQuote", r.issue().rawQuote()).put("fix", r.issue().fix()));
            }
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
        return array.toString();
    }

    private static String guardsJson(EditorialApiFlow.Outcome outcome) {
        try {
            JSONObject o = new JSONObject();
            JSONArray flags = new JSONArray();
            EditGuards.Report report = outcome.guards();
            if (report != null) {
                for (EditGuards.Flag f : report.flags()) flags.put(new JSONObject().put("code", f.code().name()).put("detail", f.detail()));
                o.put("structureDelta", report.structureDelta()).put("rewriteRatio", report.rewriteRatio())
                        .put("changedSegments", report.changedSegments());
            }
            o.put("flags", flags).put("checkUnavailable", outcome.checkUnavailable()).put("checkCounters", outcome.checkCountersTotal());
            return o.toString();
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Needs-review issues of a stored run, for the result screen. */
    static List<String[]> reviewItems(EditorialApiRun run) {
        List<String[]> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(run.issuesJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                if ("NEEDS_REVIEW".equals(o.optString("status"))) {
                    out.add(new String[] {o.optString("kind"), o.optString("editedQuote"), o.optString("rawQuote"), o.optString("fix"),
                            o.optString("reason")});
                }
            }
        } catch (Exception ignored) {
            // an unreadable column shows as no items
        }
        return out;
    }

    static List<String> guardFlags(EditorialApiRun run) {
        List<String> out = new ArrayList<>();
        try {
            JSONArray array = new JSONObject(run.guardsJson).optJSONArray("flags");
            if (array != null) for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                out.add(o.optString("code") + ": " + o.optString("detail"));
            }
        } catch (Exception ignored) {
            // an unreadable column shows as no flags
        }
        return out;
    }

    static List<String[]> notes(EditorialApiRun run) {
        List<String[]> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(run.notesJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                if (o.has("quote")) out.add(new String[] {o.optString("quote"), o.optString("kind"), o.optString("reason")});
            }
        } catch (Exception ignored) {
            // an unreadable column shows as no notes
        }
        return out;
    }
}
