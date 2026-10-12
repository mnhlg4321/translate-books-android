# P5D — VOL5 RAW provider reconciliation

Audit completed: `2026-09-09` (+07:00), after the OpenRouter API key was
saved again in the validation package. This record supplements the earlier
credential gate in `P5D_VOL5_RAW_AUTHORIZATION_BLOCKED.md`; it does not rewrite
that historical result.

## Scope and local durable state

The exact persisted P4 binding was re-read for selector
`p5d-raw-mercedes-vol5-001`, chapter `001`, using validation APK
`4.17-dev.20 / code188` on device `15e84958`. The four source identities and
the canonical pack/profile identities matched the previously recorded values.
No source text is stored here.

The explicit single-use RAW authorization allowed egress to OpenRouter,
`openai/gpt-5.6-luna`, one primary call plus one schema-only repair, zero
automatic network retries, a `$0.10` cap and a five-minute window. RECONCILE
was not authorized.

Redacted app readback after the control session ended:

| Field | Value |
|---|---|
| Attempt identity | `157e3517b4b98535392db95f0c93285f08a0ac6b07341a82508ffc36aea9a5f0` |
| Request identity | `23549434e2dc61ca2aa027b77b6be500f691164c79a4269136273e4e9b566cde` |
| Binding identity | `2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520` |
| Phase | `L1_RAW_DISCOVERY` |
| Provider/model | `openrouter` / `openai/gpt-5.6-luna` |
| Local durable status | `RECOVERY_REQUIRED` |
| Local recovery reason | `RETRY_PROVIDER_CALL_FAILED_UNKNOWN` |
| Response identity | absent |
| REPORT_L1 bytes | `0` |
| Receipt bytes | `0` |
| RECONCILE calls | `0` |

The app-side instrumentation result stream was interrupted before its final
redacted result line. The durable row is authoritative for the local
fail-closed state; no local success, response or usage receipt is inferred.

The attempt row did not contain a metrics JSON payload. The opt-in test used
the existing provider constructor without a lifecycle recorder, so this
attempt is not evidence of live lifecycle-event persistence; the P5D lifecycle
owner remains covered only by its focused local regression tests. No attempt
was made to fill that missing evidence from provider-side content.

## Read-only OpenRouter evidence

The authenticated OpenRouter Logs view showed one matching generation at
`Sep 9, 06:42 AM` (+07:00 display), with the same model, provider and app
identity. Generation details showed:

| Field | Redacted metadata |
|---|---|
| Generation ID | `gen-1788910936-DHfTNOyDlU3f3PJOAvqb` |
| Model/provider | `GPT-5.6 Luna` / `OpenAI` |
| Input/output | `23,674` / `90` tokens |
| Provider HTTP | `200` |
| Streaming | `true` |
| Finish reason | `cancelled` |
| Displayed cost | `$0.00484` |
| Routing latency | `256 ms` |
| Provider latency | `593 ms` |
| Generation/total latency | `9.8 s` / `10.7 s` |
| Input/output logging | not enabled |

The request-size and timing match the app preflight for the VOL5 binding. No
prompt, chapter text, request body, response body, API key or session secret
was opened, copied or stored. The OpenRouter activity/Logs page is evidence
only; it is not used as a REPORT_L1 or receipt.

## Classification and decision

```text
EXTERNAL_CONFIRMED_CANCELLED
RETRY_ELIGIBLE: YES_AFTER_NEW_EXACT_PHASE_AUTHORIZATION
CURRENT_VOL5_RAW_PROVIDER_CALLS: 1
RECONCILE_AUTHORIZED: NO
```

The provider-confirmed cancellation closes the external-state question for
this attempt. The consumed authorization must not be reused. A further RAW
attempt requires a new exact-phase, single-use authorization that explicitly
acknowledges possible duplicate semantic work and additional billing. Until
then, the recovery gate remains closed and `providerCalls=0` for any new
dispatch. No RECONCILE authorization is issued by this record.

This is not a successful L1 pilot: there is no app-validated response, no
`REPORT_L1`, no receipt and no certification evidence. Editorial execution,
L2/L3 and certification remain disabled.

## Exit status

```text
P5D_EXTERNAL_AUDIT_COMPLETE
VOL5_EXTERNAL_CONFIRMED_CANCELLED
VOL5_RECOVERY_REQUIRED
NEW_RAW_RECOVERY_AUTHORIZATION_REQUIRED
RECONCILE_NOT_AUTHORIZED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```
