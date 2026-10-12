"""Read source only; execute extracted collector SQL against disposable in-memory SQLite."""
import hashlib
import json
import re
import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
helper_path = root / 'scripts/p5e-raw-live-supervisor.ps1'
source = helper_path.read_text(encoding='utf-8-sig')
query_source = source.split('function New-P5EConsistentDatabaseReadbackQuery {', 1)[1].split(
    '\nfunction ConvertFrom-P5EConsistentDatabaseReadbackOutput', 1)[0]
parser_source = source.split('function ConvertFrom-P5EConsistentDatabaseReadbackOutput {', 1)[1].split(
    '\nfunction Get-P5EConsistentDatabaseReadback', 1)[0]
query = re.search(r'\$query = @"\n(.*?)\n"@', query_source, re.S)[1]
query = query.replace('$attemptLiteral', "'" + 'a' * 64 + "'")
query = query.replace('$bindingLiteral', "'" + 'b' * 64 + "'")
query = query.replace('$script:P5EProjectRowId', '2')
schema_source = (root / 'app/src/main/java/com/ml/tblandroidtxt/EditorialMigrationSpec.java').read_text(encoding='utf-8-sig')
ddl = re.findall(r'"(CREATE TABLE IF NOT EXISTS editorial_[^"\n]+)"', schema_source)
db = sqlite3.connect(':memory:')
for statement in ddl:
    db.execute(statement)
db.execute('PRAGMA user_version=24')

def statement_for(tag):
    return re.search(r"SELECT '" + tag + r"'.*?;", query, re.S)[0]

schema_error = None
try:
    db.execute(statement_for('SCHEMA')).fetchall()
except sqlite3.Error as error:
    schema_error = str(error)
lineage = db.execute(statement_for('LINEAGE')).fetchone()[0]
expected_columns = int(re.search(r"'LINEAGE' \{ if \(\$columns.Count -ne (\d+)", parser_source)[1])
actual_columns = len(lineage.split('\t'))
db.execute('INSERT INTO editorial_p5c_attempts (attempt_identity,request_identity,binding_identity,run_declaration_identity,chapter_key,phase,predecessor_identity,request_envelope_hash,provider,model,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)',
           ('a'*64, 'c'*64, 'b'*64, 'd'*64, '001', 'L1_RAW_DISCOVERY', 'e'*64, 'f'*64, 'fake', 'fake', 'RECOVERY_REQUIRED', 1, 2))
attempt = db.execute(statement_for('ATTEMPT')).fetchone()[0]
result = {
    'scope': 'SOURCE_SQL_IN_MEMORY_ONLY',
    'helperSha256': hashlib.sha256(helper_path.read_bytes()).hexdigest(),
    'sqliteVersion': sqlite3.sqlite_version,
    'schemaStatementError': schema_error,
    'schemaPositiveControl': db.execute('SELECT user_version FROM pragma_user_version').fetchone()[0],
    'lineageActualColumns': actual_columns,
    'lineageParserExpectedColumns': expected_columns,
    'recoveryAttemptExists': db.execute('SELECT count(*) FROM editorial_p5c_attempts').fetchone()[0],
    'recoveryAttemptSerializedRow': attempt,
    'historicalRedFindings': [schema_error == 'no such column: user_version', actual_columns != expected_columns, attempt is None],
    'resolvedAssertions': {
        'schemaQueryPassed': schema_error is None and db.execute('SELECT user_version FROM pragma_user_version').fetchone()[0] == 24,
        'lineageColumnsMatchParser': actual_columns == expected_columns == 17,
        'nullableAttemptRowPreserved': attempt is not None,
    },
    'deviceActions': 0, 'providerCalls': 0, 'credentialReads': 0,
    'readyForOwnerDecision': False, 'p6Ready': False
}
print(json.dumps(result, indent=2))
assert all(result['resolvedAssertions'].values()), 'SQL boundary repair did not resolve every assertion'
assert not any(result['historicalRedFindings']), 'Historical RED finding still reproduces after repair'
