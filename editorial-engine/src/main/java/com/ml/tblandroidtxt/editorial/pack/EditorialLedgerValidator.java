package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Computes exhaustive population/ledger coverage from app-owned values. */
public final class EditorialLedgerValidator {
    public record Entry(String itemId, String disposition, List<String> evidenceRefs,
                        boolean modelDeclaredPass) {
        public Entry {
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(disposition, "disposition");
            evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
        }
    }

    public record Request(List<String> populationIds, List<Entry> entries) {
        public Request {
            populationIds = List.copyOf(populationIds == null ? List.of() : populationIds);
            entries = List.copyOf(entries == null ? List.of() : entries);
        }
    }

    public record Issue(String code, String itemId) {
        public Issue {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(itemId, "itemId");
        }
    }

    public record Result(boolean valid, int populationTotal, int accountedTotal,
                         List<Issue> issues) {
        public Result {
            issues = List.copyOf(issues == null ? List.of() : issues);
        }
    }

    public Result validate(Request request) {
        if (request == null) return new Result(false, 0, 0, List.of(new Issue("LEDGER_REQUEST_MISSING", "")));
        List<Issue> issues = new ArrayList<>();
        Set<String> population = new HashSet<>();
        for (String itemId : request.populationIds()) {
            if (itemId == null || itemId.isBlank() || !population.add(itemId)) {
                issues.add(new Issue("LEDGER_POPULATION_DUPLICATE_OR_BLANK", itemId == null ? "" : itemId));
            }
        }
        Set<String> accounted = new HashSet<>();
        for (Entry entry : request.entries()) {
            if (entry == null || entry.itemId().isBlank()) {
                issues.add(new Issue("LEDGER_ENTRY_BLANK", ""));
                continue;
            }
            if (!population.contains(entry.itemId())) issues.add(new Issue("LEDGER_UNKNOWN_ITEM", entry.itemId()));
            if (!accounted.add(entry.itemId())) issues.add(new Issue("LEDGER_DUPLICATE_ITEM", entry.itemId()));
            if ("PASS".equals(entry.disposition())) {
                issues.add(new Issue("LEDGER_MODEL_PASS_NOT_A_DECISION", entry.itemId()));
            }
            if (!Set.of("PROCESSED", "PRESERVE_DRAFT", "NOT_EVALUATED").contains(entry.disposition())) {
                issues.add(new Issue("LEDGER_DISPOSITION_INVALID", entry.itemId()));
            }
            if (entry.evidenceRefs().stream().anyMatch(value -> value == null || value.isBlank())) {
                issues.add(new Issue("LEDGER_EVIDENCE_REF_INVALID", entry.itemId()));
            }
            if ("PROCESSED".equals(entry.disposition()) && entry.evidenceRefs().isEmpty()) {
                issues.add(new Issue("LEDGER_PROCESSED_EVIDENCE_MISSING", entry.itemId()));
            }
        }
        for (String itemId : population) {
            if (!accounted.contains(itemId)) issues.add(new Issue("LEDGER_UNACCOUNTED_ITEM", itemId));
        }
        return new Result(issues.isEmpty(), population.size(), accounted.size(), issues);
    }
}
