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

    @Test public void v20AddsOnlyDurableP5CAttemptOwner() {
        String sql = String.join("\n", EditorialMigrationSpec.from19To20()).toLowerCase();
        assertEquals(1, sql.split("create table if not exists", -1).length - 1);
        assertEquals(2, countIndexes(sql));
        assertEquals(2, sql.split("create trigger if not exists", -1).length - 1);
        assertTrue(sql.contains("create table if not exists editorial_p5c_attempts"));
        assertTrue(sql.contains("request_identity text not null"));
        assertTrue(sql.contains("request_envelope_hash text not null"));
        assertTrue(sql.contains("phase in ('l1_raw_discovery','l1_reconcile')"));
        assertTrue(sql.contains("status in ('claimed','recovery_required','committed')"));
        assertTrue(sql.contains("report_bytes blob"));
        assertTrue(sql.contains("receipt_bytes blob"));
        assertTrue(sql.contains("foreign key(binding_identity) references editorial_p4_bindings(binding_identity) on delete restrict"));
        assertTrue(sql.contains("foreign key(run_declaration_identity) references editorial_authoritative_run_declarations(declaration_identity) on delete restrict"));
        assertTrue(sql.contains("trg_editorial_p5c_attempts_identity_no_update"));
        assertTrue(sql.contains("trg_editorial_p5c_attempts_no_delete"));
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("editorial_projects ("));
    }

    @Test public void v21AddsOnlyRedactedP5DRecoveryOwners() {
        String sql = String.join("\n", EditorialMigrationSpec.from20To21()).toLowerCase();
        assertEquals(3, sql.split("create table if not exists", -1).length - 1);
        assertEquals(3, countIndexes(sql));
        assertEquals(4, sql.split("create trigger if not exists", -1).length - 1);
        assertTrue(sql.contains("create table if not exists editorial_p5d_network_lifecycle"));
        assertTrue(sql.contains("create table if not exists editorial_p5d_authorization_receipts"));
        assertTrue(sql.contains("create table if not exists editorial_p5d_reconciliation"));
        assertTrue(sql.contains("call_created"));
        assertTrue(sql.contains("response_body_complete"));
        assertTrue(sql.contains("external_state_remains_unknown"));
        assertTrue(sql.contains("authorization_id_hash"));
        assertTrue(sql.contains("duplicate_risk_acknowledged"));
        assertTrue(sql.contains("on delete restrict"));
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("request_body blob"));
        assertFalse(sql.contains("response_bytes"));
        assertFalse(sql.contains("response_content blob"));
        assertFalse(sql.contains("api_key"));
    }

    @Test public void v22AddsOnlyRedactedLifecycleFieldsRequiredForP5DReconciliation() {
        String sql = String.join("\n", EditorialMigrationSpec.from21To22()).toLowerCase();
        assertEquals(2, sql.split("alter table editorial_p5d_network_lifecycle", -1).length - 1);
        assertTrue(sql.contains("add column response_content_type text not null default ''"));
        assertTrue(sql.contains("add column cancellation_source text not null default ''"));
        assertFalse(sql.contains("request_body blob"));
        assertFalse(sql.contains("response_body"));
        assertFalse(sql.contains("response_content blob"));
        assertFalse(sql.contains("api_key"));
        assertFalse(sql.contains("drop table"));
    }

    @Test public void v23AddsAppendOnlyReconciliationHistoryWithoutReplacingThePrimaryDecision() {
        String sql = String.join("\n", EditorialMigrationSpec.from22To23()).toLowerCase();
        assertEquals(1, sql.split("create table if not exists", -1).length - 1);
        assertEquals(1, countIndexes(sql));
        assertEquals(2, sql.split("create trigger if not exists", -1).length - 1);
        assertTrue(sql.contains("editorial_p5d_reconciliation_history"));
        assertTrue(sql.contains("decision_identity"));
        assertTrue(sql.contains("on delete restrict"));
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("request_body"));
        assertFalse(sql.contains("response_body"));
        assertFalse(sql.contains("api_key"));
    }

    private static int countIndexes(String sql) {
        return sql.split("create index if not exists", -1).length - 1
                + sql.split("create unique index if not exists", -1).length - 1;
    }
}
