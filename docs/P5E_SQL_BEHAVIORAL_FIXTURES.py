"""Build disposable SQLite fixtures from the project's current migration DDL."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
import sqlite3
from pathlib import Path


ATTEMPT = "7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e"
REQUEST = "ae328c3d771112ce73e9e9d6cba0bb951f930042fc6851a31a96f15f7b70ee06"
REQUEST_ENVELOPE = "5c25e1850c7f70081bd21d67effa2a3a642f410f025ab91cf8044b6ed1bd87f2"
BINDING = "845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf"
RUN = "8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc"
PACK = "497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d"
PROFILE = "beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21"
MANIFEST = "0353d751924d02ef0928bb6460c4ab894fee7c6324506e62b2972090e519c4da"
EVALUATION = "3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1"
REVISION = "1" * 64
SCOPE = "2" * 64
AUTHORIZATION = "9" * 64
RECONCILIATION = "8" * 64
HISTORY = "7" * 64

SOURCES = [
    ("RAW", 23814, "a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be", "VISIBLE"),
    ("GLOSSARY", 3249, "4bc3e2dd05542aa5ca6b7e5fcac43ed53e9af57060eb69c6fa71e9d0a2ea0314", "VISIBLE"),
    ("DRAFT", 26462, "64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5", "HIDDEN"),
    ("PRONOUN", 452, "4947ff9184995be5f850f2323fbe0a04c67302fb8d5afb63cf12202b44720686", "HIDDEN"),
]


def sql_literals(source: Path) -> list[str]:
    text = source.read_text(encoding="utf-8-sig")
    pattern = re.compile(r'"((?:CREATE|ALTER|INSERT OR IGNORE)[^"\n]*)"')
    return [match.group(1) for match in pattern.finditer(text)]


def ddl_hash(statements: list[str]) -> str:
    return hashlib.sha256("\n".join(statements).encode("utf-8")).hexdigest()


def make_schema(connection: sqlite3.Connection, statements: list[str]) -> None:
    connection.execute("PRAGMA foreign_keys=OFF")
    for statement in statements:
        connection.execute(statement)
    connection.execute("PRAGMA user_version=24")


def insert_common(connection: sqlite3.Connection) -> None:
    connection.execute(
        "INSERT INTO editorial_projects(series_name,volume_name,workflow_version,workflow_hash,output_tree_uri,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
        ("dummy", "dummy", "fixture", "a" * 64, "", 1, 1),
    )
    connection.execute(
        "INSERT INTO editorial_projects(series_name,volume_name,workflow_version,workflow_hash,output_tree_uri,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
        ("P5E fixture", "RAW", "fixture", "b" * 64, "", 1, 1),
    )
    connection.execute(
        "INSERT INTO editorial_packs(id,pack_id,version,canonical_pack_hash,contract_version,schema_version,minimum_engine_version,compatibility_class,state,storage_key,manifest_canonical_json,created_at,validated_at,engine_version_used,blocked_reason) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (1, "fixture-pack", "1", PACK, "fixture", "24", "fixture", "DATA_COMPATIBLE", "STORED_READY_FOR_CERTIFICATION", "fixture-pack", "{}", 1, 1, "fixture", ""),
    )
    connection.execute(
        "INSERT INTO editorial_pack_imports(import_id,state,pack_id,pack_version,canonical_pack_hash,staging_path,storage_key,storage_moved,pack_row_id,blocked_reason,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
        ("fixture-import", "STORED_READY_FOR_CERTIFICATION", "fixture-pack", "1", PACK, "fixture", "fixture-pack", 0, 1, "", 1, 1),
    )
    connection.execute(
        "INSERT INTO editorial_pack_compatibility_results(import_id,pack_row_id,canonical_pack_hash,engine_version_used,machine_contract_fingerprint,compatibility_class,required_class,blocked_reason,evaluated_at) VALUES (?,?,?,?,?,?,?,?,?)",
        ("fixture-import", 1, PACK, "fixture", "c" * 64, "DATA_COMPATIBLE", "DATA_COMPATIBLE", "", 1),
    )
    connection.execute(
        "INSERT INTO editorial_pack_compatibility_evaluations(evaluation_id,import_id,pack_row_id,compatibility_result_id,canonical_pack_hash,trusted_profile_id,trusted_profile_version,canonical_profile_hash,engine_version_used,machine_contract_fingerprint,evaluator_contract_version,adapter_set_fingerprint,capability_fingerprint,context_fingerprint,compatibility_outcome,reason_code,blocker_details,evaluated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (EVALUATION, "fixture-import", 1, 1, PACK, "fixture-profile", "1", PROFILE, "fixture", "d" * 64, "fixture", "e" * 64, "f" * 64, "0" * 64, "DATA_COMPATIBLE", "fixture", "", 1),
    )
    connection.execute(
        "INSERT INTO editorial_project_revisions(revision_identity,revision_canonical_version,project_semantic_key,project_definition_contract_version,semantic_project_type,scope_policy_fingerprint,workflow_policy_fingerprint,project_definition_fingerprint,project_definition_canonical,source_project_row_id,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
        (REVISION, "1", "fixture-project", "fixture", "BOOK", "1" * 64, "2" * 64, "3" * 64, "fixture", 2, 1),
    )
    connection.execute(
        "INSERT INTO editorial_input_scope_snapshots(scope_snapshot_identity,project_revision_identity,scope_canonical_version,canonical_scope_key,required_roles_canonical,required_roles_fingerprint,manifest_version,manifest_fingerprint,source_chapter_row_id,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
        (SCOPE, REVISION, "1", "001", "RAW,GLOSSARY,DRAFT,PRONOUN", "4" * 64, "1", MANIFEST, None, 1),
    )
    connection.execute(
        "INSERT INTO editorial_authoritative_run_declarations(declaration_identity,declaration_fingerprint,run_declaration_contract_version,attempt_request_selector,project_revision_identity,input_scope_snapshot_identity,compatibility_evaluation_id,run_kind,phase_identity,frozen_manifest_fingerprint,frozen_manifest_reference,node_kind,parent_record_identity,run_attempt_ordinal,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (RUN, "5" * 64, "fixture", "p5e-fresh-mercedes-vol5-20260911-01", REVISION, SCOPE, EVALUATION, "EDITORIAL_PILOT", "L1_SOURCE_PREFLIGHT", MANIFEST, "fixture", "ROOT", None, 0, 1),
    )
    connection.execute(
        "INSERT INTO editorial_p4_bindings(binding_identity,binding_fingerprint,binding_contract_version,attempt_request_selector,project_row_id,project_revision_identity,input_scope_snapshot_identity,run_declaration_identity,pack_id,pack_version,canonical_pack_hash,manifest_fingerprint,trusted_profile_id,trusted_profile_version,canonical_profile_hash,machine_contract_fingerprint,compatibility_evaluation_id,compatibility_outcome,evaluation_context_fingerprint,contract_version,schema_version,phase_graph_fingerprint,context_allow_list_fingerprint,source_mode,glossary_status,pronoun_status,pair_context_status,explicit_user_decision_provenance,input_manifest_fingerprint,run_attempt_ordinal,run_kind,phase_identity,execution_allowed,certification_state,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (BINDING, "6" * 64, "fixture", "p5e-fresh-mercedes-vol5-20260911-01", 2, REVISION, SCOPE, RUN, "fixture-pack", "1", PACK, MANIFEST, "fixture-profile", "1", PROFILE, "7" * 64, EVALUATION, "DATA_COMPATIBLE", "8" * 64, "fixture", "24", "9" * 64, "a" * 64, "NORMAL_FOUR_SOURCE", "AVAILABLE", "AVAILABLE", "NONE", "USER_CONFIRMED_NORMAL", MANIFEST, 0, "EDITORIAL_PILOT", "L1_SOURCE_PREFLIGHT", 0, "NOT_CERTIFIED", 1),
    )
    for ordinal, (role, length, digest, _visibility) in enumerate(SOURCES):
        connection.execute(
            "INSERT INTO editorial_p4_binding_inputs(binding_identity,role,ordinal,source_reference,byte_length,sha256,encoding,schema_status) VALUES (?,?,?,?,?,?,?,?)",
            (BINDING, role, ordinal, f"fixture://{role}", length, digest, "UTF-8", "VALID"),
        )


def insert_attempt(connection: sqlite3.Connection, status: str, report: bytes | None, receipt: bytes | None, metrics: str = "", reason: str = "") -> None:
    connection.execute(
        "INSERT INTO editorial_p5c_attempts(attempt_identity,request_identity,binding_identity,run_declaration_identity,chapter_key,phase,predecessor_identity,request_envelope_hash,provider,model,status,response_identity,report_bytes,receipt_bytes,metrics_json,recovery_reason_code,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (ATTEMPT, REQUEST, BINDING, RUN, "001", "L1_RAW_DISCOVERY", RUN, REQUEST_ENVELOPE, "openrouter", "openai/gpt-5.6-luna", status, "response-identity", report, receipt, metrics, reason, 1000, 1100),
    )


def insert_auth_and_lifecycle(connection: sqlite3.Connection) -> None:
    connection.execute(
        "INSERT INTO editorial_p5d_authorization_receipts(authorization_id_hash,exact_phase,attempt_identity,request_identity,binding_identity,run_declaration_identity,chapter_key,provider,model,endpoint_account_fingerprint,issued_at,expires_at,maximum_primary_calls,maximum_schema_repair_calls,maximum_network_retries,maximum_input_tokens,maximum_output_tokens,maximum_total_tokens,maximum_total_cost,maximum_execution_time_ms,consumed_at,consumption_result,consumed_attempt_identity,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (AUTHORIZATION, "L1_RAW_DISCOVERY", ATTEMPT, REQUEST, BINDING, RUN, "001", "openrouter", "openai/gpt-5.6-luna", "a" * 64, 900, 180900, 1, 0, 0, 100000, 4096, 104096, "0.05", 120000, 1100, "CONSUMED", ATTEMPT, 900),
    )
    connection.execute(
        "INSERT INTO editorial_p5d_network_lifecycle(attempt_identity,stage,request_body_bytes,response_body_bytes,http_status,response_content_type,exception_class,elapsed_ms,generation_id,provider_response_id,cancellation_source,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
        (ATTEMPT, "RESPONSE_BODY_COMPLETE", 100, 200, 200, "application/json", "", 1000, "fixture-generation", "fixture-response", "", 1150),
    )


def insert_reconciliation_history(connection: sqlite3.Connection) -> None:
    connection.execute(
        "INSERT INTO editorial_p5d_reconciliation(attempt_identity,classification,evidence_ref,endpoint_account_fingerprint,billing_state,decided_by,retry_eligible,duplicate_risk_acknowledged,new_authorization_id_hash,decided_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
        (ATTEMPT, "EXTERNAL_STATE_REMAINS_UNKNOWN", "fixture-evidence", "a" * 64, "UNKNOWN", "fixture", 0, 1, "", 1200),
    )
    connection.execute(
        "INSERT INTO editorial_p5d_reconciliation_history(decision_identity,attempt_identity,classification,evidence_ref,endpoint_account_fingerprint,billing_state,decided_by,retry_eligible,duplicate_risk_acknowledged,new_authorization_id_hash,decided_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
        (HISTORY, ATTEMPT, "EXTERNAL_STATE_REMAINS_UNKNOWN", "fixture-history", "a" * 64, "UNKNOWN", "fixture", 0, 1, "", 1201),
    )


def make_fixture(path: Path, statements: list[str], scenario: str, golden: dict[str, bytes] | None) -> dict[str, object]:
    if path.exists():
        path.unlink()
    connection = sqlite3.connect(path)
    try:
        make_schema(connection, statements)
        insert_common(connection)
        if scenario == "claimed-null":
            insert_attempt(connection, "CLAIMED", None, None, "", "")
        elif scenario == "recovery-null":
            insert_attempt(connection, "RECOVERY_REQUIRED", None, None, "", "RETRY_PROVIDER_CALL_TIMEOUT")
        elif scenario == "committed-golden":
            if golden is None:
                raise ValueError("golden pair required")
            insert_attempt(connection, "COMMITTED", golden["report"], golden["receipt"], '{"primaryCalls":1}', "")
            insert_auth_and_lifecycle(connection)
        elif scenario == "lineage-reconciliation":
            insert_attempt(connection, "RECOVERY_REQUIRED", None, None, "", "RECOVERY")
            insert_auth_and_lifecycle(connection)
            insert_reconciliation_history(connection)
        elif scenario == "partial-blob":
            insert_attempt(connection, "RECOVERY_REQUIRED", b"partial", None, "", "PARTIAL")
        elif scenario != "unused":
            raise ValueError(f"unknown scenario {scenario}")
        connection.commit()
        connection.execute("PRAGMA foreign_keys=ON")
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        foreign_keys = connection.execute("PRAGMA foreign_key_check").fetchall()
        if integrity != "ok" or foreign_keys:
            raise RuntimeError(f"fixture integrity failed: {scenario}: {integrity}: {foreign_keys}")
    finally:
        connection.close()
    return {"scenario": scenario, "path": str(path), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True)
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--golden-output", required=False)
    args = parser.parse_args()
    repo = Path(args.repo).resolve()
    output = Path(args.output_root).resolve()
    output.mkdir(parents=True, exist_ok=False)
    migration = repo / "app/src/main/java/com/ml/tblandroidtxt/EditorialMigrationSpec.java"
    statements = sql_literals(migration)
    golden: dict[str, bytes] | None = None
    if args.golden_output:
        exported = json.loads(Path(args.golden_output).read_text(encoding="utf-8"))
        golden = {
            "report": base64.b64decode(exported["reportBase64"]),
            "receipt": base64.b64decode(exported["receiptBase64"]),
        }
    scenarios = ["unused", "claimed-null", "recovery-null", "committed-golden", "lineage-reconciliation", "partial-blob"]
    fixtures = []
    for scenario in scenarios:
        fixtures.append(make_fixture(output / f"{scenario}.db", statements, scenario, golden))
    manifest = {
        "scope": "OFFLINE_SQLITE_DDL_FIXTURES_ONLY",
        "sqliteVersion": sqlite3.sqlite_version,
        "migrationSource": str(migration),
        "migrationSha256": hashlib.sha256(migration.read_bytes()).hexdigest(),
        "ddlStatementCount": len(statements),
        "ddlSha256": ddl_hash(statements),
        "goldenOutput": args.golden_output or None,
        "fixtures": fixtures,
        "deviceActions": 0,
        "providerCalls": 0,
        "credentialReads": 0,
    }
    (output / "FIXTURE_MANIFEST.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
