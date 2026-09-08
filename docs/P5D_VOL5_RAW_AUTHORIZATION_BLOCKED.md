# P5D — VOL5 RAW authorization gate result

Date: 2026-09-09 (+07:00)

## Scope

The user authorized a new, independent RAW attempt for VOL5/chapter `001`:

- egress: `YES`
- provider: `OpenRouter`
- model: `openai/gpt-5.6-luna`
- maximum calls: `1` primary plus `1` schema-only repair
- automatic network retries: `0`
- total cost cap: `$0.10`
- execution window: `5 minutes`
- RECONCILE: not authorized

The attempt is not a retry of the consumed VOL4 attempt. It uses a new
persisted P4 binding and a new single-use RAW authorization.

## Binding and source evidence

The test-only device setup passed and read back the exact binding:

| Fact | Redacted value |
|---|---|
| selector | `p5d-raw-mercedes-vol5-001` |
| project row | `1` |
| binding identity | `2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520` |
| run declaration | `7d804fa125728561c32ac4fa52df44d80f84d7cf8e9207bc69594f155ae072f0` |
| canonical pack hash | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` |
| canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| input manifest fingerprint | `0353d751924d02ef0928bb6460c4ab894fee7c6324506e62b2972090e519c4da` |

The app-owned normalized source identities were:

| Role | Bytes | SHA-256 |
|---|---:|---|
| RAW | 23,814 | `A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE` |
| DRAFT | 26,462 | `64ADECD8CECCBB13446EF14C494CA9BB1987117C428C5758E7442270EC7F62B5` |
| GLOSSARY | 3,249 | `4BC3E2DD05542AA5CA6B7E5FCAC43ED53E9AF57060EB69C6FA71E9D0A2EA0314` |
| PRONOUN | 452 | `4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686` |

The original PRONOUN file had a UTF-8 BOM (455 bytes); the existing app-owned
source normalization removed only that BOM before binding, yielding the 452
byte identity above. No source text is stored in this report.

## Gate result

Validation APK `4.17-dev.20` / code `188` was installed on device `15e84958`.
The explicit RAW test stopped before constructing or consuming an authorization:

```text
LIVE_AUTHORIZATION_INCOMPLETE:
OpenRouter API key is absent after validation restore
providerCalls=0
RECONCILE calls=0
```

No provider request, attempt claim, request body, response, report or receipt
was created. The failure is credential-state incompleteness, not a source,
binding, pack, profile or preflight failure. The validation package retains the
VOL5 binding and local test inputs for a later retry after the user saves the
OpenRouter key again.

## Next action

Save the OpenRouter API key again in the validation app's Settings (the prior
uninstall/reinstall used to restore code169 removed app-private settings), then
rerun the same explicit RAW test. It must still use the VOL5 selector above,
one new single-use RAW authorization, zero network retries and no RECONCILE.

Status remains:

```text
VOL5_BINDING_READY
LIVE_AUTHORIZATION_INCOMPLETE
PROVIDER_CALLS_0
RECONCILE_NOT_AUTHORIZED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```
