package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EditorialV18MigrationSpecTest {
    @Test public void v18AddsExactlyTwoTablesFiveIndexesAndFourImmutableTriggers() {
        String sql = String.join("\n", EditorialMigrationSpec.from17To18()).toLowerCase();
        assertEquals(2, sql.split("create table if not exists", -1).length - 1);
        assertEquals(5, countIndexes(sql));
        assertEquals(4, sql.split("create trigger if not exists", -1).length - 1);
        assertTrue(sql.contains("create table if not exists editorial_authoritative_run_declarations"));
        assertTrue(sql.contains("create table if not exists editorial_run_closure_events"));
        assertTrue(sql.contains("authoritative_run_identity text not null unique"));
        assertTrue(sql.contains("foreign key(authoritative_run_identity) references editorial_authoritative_run_declarations(declaration_identity) on delete restrict"));
        assertTrue(sql.contains("foreign key(parent_record_identity) references editorial_lineage_records(record_identity) on delete restrict"));
        assertTrue(sql.contains("closure_eligibility text not null check(closure_eligibility='eligible')"));
        assertTrue(sql.contains("trg_authoritative_run_declarations_no_update"));
        assertTrue(sql.contains("trg_authoritative_run_declarations_no_delete"));
        assertTrue(sql.contains("trg_run_closure_events_no_update"));
        assertTrue(sql.contains("trg_run_closure_events_no_delete"));
    }

    private static int countIndexes(String sql) {
        return sql.split("create index if not exists", -1).length - 1
                + sql.split("create unique index if not exists", -1).length - 1;
    }

    @Test public void v18IsAdditiveZeroBackfillAndNeverReferencesLegacyAuthority() {
        String sql = String.join("\n", EditorialMigrationSpec.from17To18()).toLowerCase();
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("insert or replace"));
        assertFalse(sql.contains("editorial_runs"));
        assertFalse(sql.contains("source_run_row_id"));
        assertFalse(sql.contains("source_run_selector_authority"));
        assertTrue(sql.contains("on delete restrict"));
    }
}
