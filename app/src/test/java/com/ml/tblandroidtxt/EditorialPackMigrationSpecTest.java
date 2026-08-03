package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.*;

public class EditorialPackMigrationSpecTest {
    @Test public void v14IsAdditiveAndCreatesRequiredImmutableRegistry() {
        String sql = String.join("\n", EditorialMigrationSpec.from13To14()).toLowerCase();
        assertTrue(sql.contains("create table if not exists editorial_packs"));
        assertTrue(sql.contains("create table if not exists editorial_pack_files"));
        assertTrue(sql.contains("create table if not exists editorial_pack_imports"));
        assertTrue(sql.contains("create table if not exists editorial_pack_compatibility_results"));
        assertTrue(sql.contains("unique(pack_id,version)"));
        assertTrue(sql.contains("canonical_pack_hash text not null unique"));
        assertTrue(sql.contains("on delete restrict"));
        assertTrue(sql.contains("trg_editorial_packs_no_update"));
        assertTrue(sql.contains("trg_editorial_pack_files_no_delete"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("delete from"));
    }

    @Test public void stateAndCompatibilityChecksAreFailClosed() {
        String sql = String.join("\n", EditorialMigrationSpec.from13To14());
        assertTrue(sql.contains("STORED_BLOCKED"));
        assertTrue(sql.contains("STORED_READY_FOR_CERTIFICATION"));
        assertTrue(sql.contains("ENGINE_UPGRADE_REQUIRED"));
        assertTrue(sql.contains("FOREIGN KEY(pack_row_id) REFERENCES editorial_packs(id) ON DELETE RESTRICT"));
    }
}
