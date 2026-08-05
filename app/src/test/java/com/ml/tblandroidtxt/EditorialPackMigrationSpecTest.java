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

    @Test public void v15AddsOnlyImmutableCompatibilityEvaluationHistory() {
        String sql = String.join("\n", EditorialMigrationSpec.from14To15()).toLowerCase();
        assertTrue(sql.contains("create table if not exists editorial_pack_compatibility_evaluations"));
        assertTrue(sql.contains("evaluation_id text not null unique"));
        assertTrue(sql.contains("trusted_profile_id text not null"));
        assertTrue(sql.contains("trusted_profile_version text not null"));
        assertTrue(sql.contains("canonical_profile_hash text not null"));
        assertTrue(sql.contains("evaluator_contract_version text not null"));
        assertTrue(sql.contains("context_fingerprint text not null"));
        assertTrue(sql.contains("reason_code text not null"));
        assertTrue(sql.contains("blocker_details text not null default ''"));
        assertTrue(sql.contains("foreign key(import_id) references editorial_pack_imports(import_id) on delete restrict"));
        assertTrue(sql.contains("foreign key(compatibility_result_id) references editorial_pack_compatibility_results(id) on delete restrict"));
        assertTrue(sql.contains("idx_editorial_pack_compatibility_evaluations_pack"));
        assertTrue(sql.contains("trg_editorial_pack_compatibility_evaluations_no_update"));
        assertTrue(sql.contains("trg_editorial_pack_compatibility_evaluations_no_delete"));
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("insert or replace"));
        assertFalse(sql.contains("create table if not exists editorial_packs"));
    }

    @Test public void v16AddsOnlyAppendOnlyLineageTablesAndProtection() {
        String sql = String.join("\n", EditorialMigrationSpec.from15To16()).toLowerCase();
        assertTrue(sql.contains("create table if not exists editorial_lineage_records"));
        assertTrue(sql.contains("create table if not exists editorial_lineage_input_entries"));
        assertTrue(sql.contains("record_identity text primary key not null"));
        assertTrue(sql.contains("record_fingerprint text not null unique"));
        assertTrue(sql.contains("node_kind text not null check(node_kind in ('root','child'))"));
        assertTrue(sql.contains("primary key(record_identity,role,ordinal)"));
        assertTrue(sql.contains("foreign key(parent_record_identity) references editorial_lineage_records(record_identity) on delete restrict"));
        assertTrue(sql.contains("trg_editorial_lineage_records_no_update"));
        assertTrue(sql.contains("trg_editorial_lineage_records_no_delete"));
        assertTrue(sql.contains("trg_editorial_lineage_input_entries_no_update"));
        assertTrue(sql.contains("trg_editorial_lineage_input_entries_no_delete"));
        assertTrue(sql.contains("idx_editorial_lineage_records_pack"));
        assertTrue(sql.contains("idx_editorial_lineage_records_run"));
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("insert or replace"));
        assertFalse(sql.contains("editorial_packs"));
        assertFalse(sql.contains("editorial_pack_compatibility_evaluations"));
    }

    @Test public void v17AddsExactlyFiveAuthoritativeIdentityTablesAdditively() {
        String sql = String.join("\n", EditorialMigrationSpec.from16To17()).toLowerCase();
        assertEquals(5, sql.split("create table if not exists", -1).length - 1);
        assertTrue(sql.contains("create table if not exists editorial_project_revisions"));
        assertTrue(sql.contains("create table if not exists editorial_input_scope_snapshots"));
        assertTrue(sql.contains("create table if not exists editorial_input_scope_snapshot_entries"));
        assertTrue(sql.contains("create table if not exists editorial_closed_run_contexts"));
        assertTrue(sql.contains("create table if not exists editorial_run_lineage_bindings"));
        assertTrue(sql.contains("on delete restrict"));
        assertTrue(sql.contains("trg_editorial_project_revisions_no_update"));
        assertTrue(sql.contains("trg_editorial_input_scope_snapshots_no_delete"));
        assertTrue(sql.contains("trg_editorial_input_scope_snapshot_entries_no_update"));
        assertTrue(sql.contains("trg_editorial_closed_run_contexts_no_delete"));
        assertTrue(sql.contains("trg_editorial_run_lineage_bindings_no_update"));
        assertTrue(sql.contains("unique(closed_run_identity)"));
        assertTrue(sql.contains("unique(lineage_record_identity)"));
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("insert or replace"));
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("editorial_projects ("));
        assertFalse(sql.contains("editorial_chapters ("));
        assertFalse(sql.contains("editorial_runs ("));
    }
}
