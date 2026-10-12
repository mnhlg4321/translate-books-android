package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.ReferenceProjector;

/** The pronoun-row facts the ADDRESS edit lock needs: who says it to whom, with which self/call words. Carries no book text. */
public record AddressRow(String from, String speaker, String target, String self, String call) {
    public AddressRow {
        from = clean(from);
        speaker = clean(speaker);
        target = clean(target);
        self = clean(self);
        call = clean(call);
    }

    public static AddressRow of(ReferenceProjector.Row row) {
        return new AddressRow(row.from(), row.speaker(), row.target(), row.self(), row.call());
    }

    /** A pair row names both ends and gives a call word; a row without a call word only says how one speaker refers to themself. */
    boolean pair() { return !call.isEmpty() && !from.isEmpty(); }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
