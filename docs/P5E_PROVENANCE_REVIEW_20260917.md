# P5E provenance review — 2026-09-23 metadata continuation

## Decision

The host-runner repair is locally qualified. The owner supplied all required
nonsecret provenance metadata, and that metadata is accepted for review. The
account-check branch is still not ready to run because the expected value has
not been read or loaded into the exact host PowerShell `Process`. This is not a
device event and does not authorize A4.3, RAW, P5 exit, or P6.

```text
HOST_RUNNER_REPAIR_OFFLINE_PASS
EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW / EXPECTED_VALUE_PROCESS_LOAD_PENDING
ACCOUNT_CHECK_NOT_EXECUTED
A4.3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY
```

This review is a provenance/QA record, not a runtime authorization. It does
not read a credential, endpoint, expected fingerprint, device setting, or
database; it does not use ADB or call a provider.

## Scope and source classification

The checkout examined is `D:\App Translate Books`, branch
`feature/v4.18-p5e-runner-repair-20260917`, at documentation baseline
`1d730bd2344cba146a3af91afafa7539f9929aa8`. The implementation baseline
remains `70fe4b1b9820997bd345da2c3cdd689a63b9378e`; the prior snapshot
baseline was `c6173e317a733e13080f041fdc152fe5fe08f4e8`.

| Classification | Evidence | Meaning in this review |
| --- | --- | --- |
| Current | `docs/P5E_ACCOUNT_RUNNER_REPAIR_RESULT_20260917.json` | Host repair PASS; owner provenance metadata accepted for review, expected value not read. |
| Current | `docs/P5E_ACCOUNT_RUNNER_REPAIR_QA_20260917.json` | 37/37 source/fake-process assertions pass; zero ADB/device/provider/DB/RAW actions. |
| Current | `docs/P5E_OWNER_LOCAL_INPUT_GUIDE_20260917.md` | Required owner metadata and local-only handling rules. |
| Current | `BUILD_STATE.md` and `WORKSPACE_SNAPSHOT.md` | State/P6 decision and the current intended next action. |
| Historical defect evidence | `docs/P5E_ACCOUNT_RUNNER_AUDIT_RESULT_20260917.json` | Four false acceptances in the pre-repair runner; do not use its old hash or acceptance rule. |
| Historical device evidence | `docs/P5E_ACCOUNT_CHECK_LOCAL_RESULT_20260916.json` | One CheckOnly and one test-package replacement completed; no account check or `MATCH`/`MISMATCH`. |
| Historical release evidence | A4.2/RAW documents, checklist and prior P5E reports | Retained context only; none authorizes the next account event or P6. |

The repaired current pins were independently re-hashed during this review:

| Item | SHA-256 |
| --- | --- |
| Account runner | `96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65` |
| RAW helper (future-only) | `5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34` |
| Account test installer | `21AADE819DB83464E96C0BB6AC28CB42FB26AB916D5905AD13CED37C15FC786B` |
| Account-only test source | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` |
| Repair QA | `D8940D498AB9DABBBFED4A0A31013448622E266D30ACBC2AEFF7C9E96FEF82D7` |
| Repair result after metadata receipt | `D72483FC31409424CB95EB0575D839CCC239B8BC8A84FAE8BEDCAC398C2773AC` |

## QA and adversarial review

The current repair QA exercises the repaired source and five synthetic local
processes. It rejects the four defects found by the preceding audit:
`match_then_failure`, truncated output after a match, wrong test identity and
a token suffix. It also rejects duplicate/missing results, terminal failure,
non-zero exit, timeout, malformed component, malformed expected value and a
digest-leak shape. Its PASS does not substitute for a device run.

The historical account result is intentionally narrower: CheckOnly passed and
one replacement of `com.ml.tblandroidtxt.test` succeeded with pull-back hash
and certificate matching. Its account check has `adbLaunches=0` and produced
no result token. The field `replacementTestArtifact.deviceOperations=0` is
ambiguous because the same file records `liveSequence.replacement.installAttempts=1`;
the latter is the authoritative count for that historical replacement. Neither
field proves account equality, provider validity, data preservation, A4.3, or
P6 readiness.

## Provenance condition

An expected fingerprint may be admitted only if an owner-controlled record
exists independently of the device actual-read event and maps to the same
account/project and endpoint scope. The record may retain the original key or a previously verified digest. Neither
raw owner value is sent to Codex, chat, Git, shared evidence, host ADB argv,
child environment, clipboard, file, User environment or Machine environment.
A later device event supplies only the digest as a temporary Android
instrumentation extra after stdin; the raw key from the owner record is never
transferred through the host to the device.

The owner supplied this metadata in the current continuation; it contains no
raw key, endpoint, fingerprint or digest:

```text
originalKeyAvailability=RETAINED_OUTSIDE_APP
recordAuthority=OpenRouter Default Workspace / API Keys
recordReference=OpenRouter dashboard / Default Workspace / API Keys / xzx
accountOrProjectMapping=OpenRouter Default Workspace / App Translate Books
verificationTime=2026-07-13 Asia/Ho_Chi_Minh (date only; hour not retained)
recordPredatesActualRead=YES
endpointScopeMapping=YES
```

The metadata decision is
`EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW`. The record authority, reference,
account/project mapping, predate assertion and endpoint-scope assertion are
recorded as owner metadata only; no provider or endpoint was read to validate
them. The reference `xzx` is an opaque record label, not an expected digest and
was not used as one. Date-only verification time is retained with its stated
precision; it is not converted into an invented hour.

This decision does not read or store the expected value. Before a separately
bounded account follow-on can be considered, the owner must locally create or
retrieve the expected digest and place it only in the exact host PowerShell
`Process`. The value must not be sent to Codex, chat, Git, shared evidence,
host command line, file, User/Machine environment, clipboard or transcript.
Do not derive an expected value from the device actual, alter app settings to
make a digest match, create a new key, or retry an account check to resolve
uncertainty.

## Exact next work request

1. Keep the repaired runner/helper/APK pins fixed and do not reinstall the
   test package. The prior replacement is complete.
2. The seven owner metadata fields are now recorded and the typed decision is
   `EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW`; no secret value was received.
3. Keep the expected value unread by the agent. The next conditional step is
   owner-local Process-only loading into the exact host PowerShell process;
   loading it does not launch ADB or the account runner.
4. After that load is independently confirmed without revealing the value,
   create one separately bounded account-check follow-on. It re-hashes the
   repaired files, uses the exact Process-only stdin transport, permits one
   account-runner launch and records only typed result/counts. It does not
   reinstall, issue A4.3, dispatch RAW or open P6.
5. Treat `MATCH` only as equality of the configured account/endpoint digest
   against the independent record. Treat `MISMATCH`, timeout, non-zero exit,
   incomplete terminal status, duplicate result or redaction failure as a
   stop with no retry or redispatch.
6. Assess A4.3, RAW/P5 exit and P6 only in later, separately authorized work
   packages after their own required evidence exists.

## Documentation corrections in this change

1. The canonical plan, `BUILD_STATE.md`, workspace snapshot and active v4.18
   release checklist now use `HOST_RUNNER_REPAIR_OFFLINE_PASS` with the same
   accepted-for-review/process-load-pending boundary. They no longer instruct
   a repeat runner repair or treat the owner record as a digest.
2. The erroneous second file under `release_checklists/` was moved to
   `docs/P5E_ACCOUNT_RUNNER_REPAIR_WORKLOG_20260917.md` and marked historical.
   The canonical v4.18 release checklist remains the only release checklist.
3. The snapshot names the root repair checkout and separates it from the nested
   parent audit worktree. It also records the pre-existing user-owned dirt
   instead of claiming a globally clean worktree.
4. Old runner/helper pins remain historical evidence only. Any future account
   event uses the repaired current pins listed above.
5. The owner guide now distinguishes host-argv protection from the temporary
   Android instrumentation extra, so it does not promise a transport property
   the implementation cannot provide.
## Incidents retained as active constraints

| Prior issue | Required guard for the next stage |
| --- | --- |
| code196 data loss / connected installer downgrade | Never install, downgrade, clear, uninstall, reset or run a connected suite against production. |
| A3.2 route/model mismatch and preservation gap | A mismatch does not change settings; account equality does not prove preservation. |
| Earlier `DEVICE_NOT_FOUND` | If availability fails, stop once; do not reconnect-loop or reinstall. |
| Historical false-green parser | Require exact component, exact test identity, one terminal success and one exact result token. |
| Timeout/external-state uncertainty | Stop with typed not-proven result; do not retry, redispatch or infer zero billing/calls. |
| Test/RAW artifact confusion | The account test APK is not the RAW/A4 artifact; its replacement cannot certify RAW. |

## Result

The project may proceed to the owner-local Process-only expected-value stage.
It may not proceed to a device account check, A4.3, RAW/P5 exit or P6. The
current authority files were synchronized in this metadata continuation. No
local repair, additional hash, test-package replacement or repeated QA is
needed while the source pins and metadata remain unchanged.
