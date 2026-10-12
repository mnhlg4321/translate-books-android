# P5E post-MATCH Luna review — A4.3 dispatch readiness

Date: 2026-09-24  
Scope: bounded, read-only provenance review after the corrected account event. No
device, provider, collector, readback, environment, secret, or RAW action was
performed by this review.

## Result

`A4.3_NOT_READY_FOR_DISPATCH`. The account predicate is proven by the corrected
event, but that result does not issue A4.3 or authorize RAW/GLOSSARY egress.
The existing RAW command and proposal are historical/fixed-scope material whose
own status remains `NOT_ISSUED / NOT_READY_FOR_DISPATCH`.

## Evidence checked

* `docs/P5E_ACCOUNT_IDENTITY_ROUTE_CORRECTED_EVENT_RESULT_20260924.json` records
  `CLOSED_ACCOUNT_MATCH`, one account launch, seven read-only preflight calls,
  zero provider calls/DB writes/RAW dispatches, and disposition
  `ACCOUNT_MATCH_PROVEN_ONLY_A4_3_REVIEW_REQUIRED_NO_RAW_OR_P6`.
* The event receipt hashes are retained in that result: command
  `C00F41164A3AC8AA50D5A025729D72606703E445088EA435FE8B6820296BCE81`,
  preflight `C0604F6AAA3E9BBF6BC978C97FDE667402C6F572CB368A4B4A8248023D4779C7`,
  and account runner `529B082D6255AEFE1323CAAFB6B29200BB4256F81A29DA0C7C597EDA549702C9`.
* `docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md` is hash
  `DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501` and
  declares `FIXED_SCOPE_PACKET / OWNER_DECISION_REQUIRED / NOT_ISSUED /
  NOT_READY_FOR_DISPATCH`.
* `docs/P5E_RAW_AUTHORIZATION_COMMAND.txt` is hash
  `47044AB73C0B76A00E3E40A85D6E893B0F94C015F6332036484EA5ABB5FA55AB`.
  `docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md` is hash
  `BE21E5341376E246F96397896912BC2161234F78AC78E4973C9ED86605557606`.

## Concrete blockers

1. **Helper revision selection is stale for the current checkout.** The RAW
   command points at the nested archival helper and pins
   `364A6AA2C52A90E7AD20F28EC6C46A0EAD1BA39E8909727BEA7396287896FFE7`.
   That hash does match the file at the old nested path; this is an obsolete
   revision-selection/path problem, not a failed hash check. The current
   tracked helper used by the corrected account work is
   `scripts/p5e-raw-live-supervisor.ps1`, hash
   `B491DD4D26444ACA1234A1A03D8A932A2525FB7CD112F6B6F1520A735C631897`.
   The two files differ materially in runner/component, account transport, and
   parser/supervisor code. The old command therefore cannot be treated as the
   current qualified helper selection; it must be refrozen with the intended
   source and a new command/manifest provenance chain.

2. **The test artifact identity is not proven for an A4.3 event.** The RAW
   command pins the old A4.2 AndroidTest APK
   `57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA`, while
   the corrected account MATCH event used the separate replacement account APK
   `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`.
   The account receipt proves only the account-test package. It does not prove
   that the RAW-pinned test package is currently installed or that its package,
   certificate, and version match. A fresh, read-only A4.3 preflight must prove
   the exact chosen RAW artifact, or an owner-approved replacement must produce
   a new artifact/command/manifest pin. No reinstall is authorized by this
   review.

3. **Collector/readback scope is still unapproved.** The proposal explicitly
   marks read-only package/certificate/SQLite/WAL-aware collection and the
   RAW/GLOSSARY dispatch `NOT_APPROVED`; the manifest requires owner approval
   before constructing runtime authorization. Account MATCH does not grant
   before/after collection, database readback, provider egress, or acceptance
   evidence. The collector must be separately reviewed and bound to the same
   event, artifact set, and helper hash before any dispatch decision.

4. **Budget and expiry have no fresh A4.3 owner decision.** The packet proposes
   input/output/total caps `100000/4096/104096`, maximum cost `USD0.05`, and
   fresh execution/auth/host windows `120000/180000/240000 ms`; it says these
   values are not approved until dispatch-time issuance. The authorization
   validity is a fresh 180000 ms window and cannot be copied, refreshed, or
   reused. No issued timestamp, expiry, consumed authorization, or known cost
   exists for this post-MATCH review, so dispatch cannot be inferred from the
   account receipt.

## Recommendation

Keep RAW, provider, readback, P5, and P6 closed. Prepare one new owner-decision
packet only after refreezing the helper path/hash and resolving the exact RAW
test-artifact identity. That packet must separately approve the collector/
readback and RAW/GLOSSARY egress scope, the one-use authorization identity and
budget, and the fresh expiry rules. Do not rerun the account check solely to
address these A4.3 gates; the existing MATCH receipt is sufficient for the
account predicate.

## Review of `P5E_POST_MATCH_A43_WORK_REQUEST_20260924.md`

The work request is directionally correct and preserves the no-device/no-provider
scope. Two edits are required before treating it as an actionable handoff:

* Its artifact step assumes that the `058BE8…158E` account APK, BUILD_INFO, and
  source ZIP are available for offline inspection, but the workspace inventory
  does not establish those files or a complete immutable path/hash bundle. The
  request must name the retained private evidence paths (or mark the parity gate
  pending) and must not infer source/archive parity from the account receipt or
  one Java source hash.
* “Prefer using the test APK already installed” is not an offline provenance
  decision. An installed package may be considered only after a future,
  explicitly approved read-only A4.3 preflight proves package/version/
  certificate/hash identity. The offline packet should select an immutable APK
  and matching source archive first; it must not make installed state the
  artifact authority.

The request should also say explicitly that no separate account-only event is
needed: reuse the existing corrected MATCH receipt, while any account check
embedded in a future RAW method remains a separately described live-scope gate.

### Final correction after request revision

The work request now resolves both actionable defects: step 4 marks parity
`PARITY_PENDING` and names the exact private pullback APK path plus expected
source-ZIP hash, and step 5 requires a verified immutable APK/source bundle
before selection while reserving installed-package identity for a future
approved read-only A4.3 preflight. The nested helper statement already records
that `364A…6FFE7` is the correct hash of the old nested file; it is obsolete
revision selection, not a hash mismatch. Final verdict: request is accurate
and actionable within its offline scope, with A4.3 still pending the stated
parity, helper refreeze, collector/readback approval, and fresh budget/expiry
owner decision. No further account event is required.
