package com.ml.tblandroidtxt.editorial.api;

/**
 * EDITORIAL_API_V1 (docs/EDITORIAL_API_V1_PLAN_20261005.md): the model edits plain text between tags and an optional
 * second call checks it with a small JSON answer. Everything the model used to keep as a ledger (units, hashes, ids,
 * counts) is the app's business; an irregularity in a model answer becomes a counter, never a refusal code.
 */
public final class EditorialApiContract {
    public static final String CONTRACT_REVISION = "EDITORIAL_API_V1.2";

    /** QUICK = one edit call; THOROUGH = edit + check (+ one re-check after the app applied fixes). */
    public enum Mode { QUICK, THOROUGH }

    /** What a check issue is about (plan section 3). */
    public enum IssueKind {
        MEANING, OMISSION, ADDITION, NUMBER, NEGATION, SPEAKER, PRONOUN, GLOSSARY, REGRESSION, TECHNICAL
    }

    /** Persisted state of one run (plan section 4.4). */
    public enum RunState { RUNNING, RETRY_REQUIRED, WRONG_PAIR, FINAL_OK, FINAL_NOTES, CANCELLED }

    /** The calls of one run. */
    public enum Step { EDIT, CHECK, RECHECK }

    public static final int MAX_QUOTE_CHARS = 120;
    public static final int MAX_FIX_CHARS = 2_000;
    public static final int MAX_WRONG_PAIR_EVIDENCE_CHARS = 400;
    /** One technical retry per step, never more. */
    public static final int MAX_TECHNICAL_RETRIES_PER_STEP = 1;

    private EditorialApiContract() { }
}
