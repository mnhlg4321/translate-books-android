# P5E expected-value loader review — 2026-09-23

## Decision

The owner has confirmed that the original key is retained outside the app. The
nonsecret provenance record remains accepted for review, and the new local
loader has passed its offline contract QA. No real key, fingerprint or device
setting was read in this work package; the public source-pinned endpoint is
embedded in the loader and was not read from the owner, app, device or provider.

```text
HOST_RUNNER_REPAIR_OFFLINE_PASS
EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW
EXPECTED_VALUE_LOADER_OFFLINE_PASS
EXPECTED_VALUE_PROCESS_LOAD_PENDING
ACCOUNT_CHECK_NOT_EXECUTED
A4.3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY
```

`EXPECTED_VALUE_LOADER_OFFLINE_PASS` qualifies the small local tool only. It
does not mean a value has been loaded, does not launch the account runner, and
does not authorize a device, provider, database, A4.3, RAW, P5 exit or P6
action.

## Provenance retained

The accepted owner metadata is unchanged:

```text
originalKeyAvailability=RETAINED_OUTSIDE_APP
recordAuthority=OpenRouter Default Workspace / API Keys
recordReference=OpenRouter dashboard / Default Workspace / API Keys / xzx
accountOrProjectMapping=OpenRouter Default Workspace / App Translate Books
verificationTime=2026-07-13 Asia/Ho_Chi_Minh (date only; hour not retained)
recordPredatesActualRead=YES
endpointScopeMapping=YES
```

`xzx` identifies the owner record only. It is neither a key nor a digest.

## Source-derived contract

The current account-only Android test computes the actual value as:

```text
SHA-256(UTF-8(normalizeEndpoint(baseUrl) + LF + apiKey))
```

This comes from
`app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EAccountCheckOnlyInstrumentedTest.java`.
`AppSettings.normalizeEndpoint` trims Java ASCII control/space characters,
removes one trailing slash, and appends `/chat/completions` only after a final
`/v1`. The API key is concatenated verbatim: it is not trimmed or hashed by
itself.

The account test also requires `openrouter`, exactly `openai/gpt-5.6-luna`,
and the normalized current default endpoint
`https://openrouter.ai/api/v1/chat/completions`. The loader fixes that endpoint
in its source rather than taking an endpoint command-line argument or reading
the app. A later `MISMATCH` must stop for review; it must never trigger a
settings change to force a match.

The historical P5C/P5D formula using an endpoint plus a hash of the key is not
the current P5E account-check formula and must not be used.

## Offline QA and adversarial review

`scripts/test-p5e-load-expected-digest.ps1` completed 26/26 synthetic,
offline assertions. It loaded the helper as a library, never invoked the
interactive path, and recorded zero credential inputs, expected-value loads,
ADB invocations, device actions, provider calls, database actions and RAW
dispatches.

| Item | SHA-256 | Result |
| --- | --- | --- |
| Local loader | `28A6B0AC8669FC56D19C5C1C256E0F4EA1C0E8D693CC319A4FE8603FC165C078` | PASS |
| Loader QA | `16B6ADF1694E541AEAD5197CDD1A060AF165D7A24A7BB9831836F87F924297C5` | 26/26 PASS |
| QA result | `C380B478BF84CB98C60C499E761DBB3BC5385A2B7BADE26BA8BF9CD6217A577C` | PASS |

The tests use synthetic values only. They cover the exact current endpoint,
`/v1` expansion, one-slash-only behavior, Java trim behavior including a
nonbreaking-space edge case, key trailing-space and malformed-UTF-16 fallback
boundaries, lower-case 64-hex Process storage, stale-value clearing after a
failed load, and source scans that reject persistent environment scopes,
command-line key input, child/network launch, clipboard, transcript and direct
value output.

An independent source review also checked the Android implementation and
identified the route/model precondition and the historical formula trap above.

## Owner-local step

Use a private Windows PowerShell window that you control. Do not use a shell
opened by Codex, and do not put the original key, digest or a screenshot into
chat, Git, a file, clipboard, command line or transcript.

1. In that same PowerShell window, enter:

   ```powershell
   Set-Location -LiteralPath 'D:\App Translate Books'
   (Get-FileHash -LiteralPath '.\scripts\p5e-load-expected-digest.ps1' -Algorithm SHA256).Hash
   ```

   Confirm the printed hash is the loader hash in the table above. If scripts
   are blocked, run `Set-ExecutionPolicy -Scope Process -ExecutionPolicy
   Bypass` in this same window only, then continue.

2. Enter this exact command in the same window:

   ```powershell
   .\scripts\p5e-load-expected-digest.ps1
   ```

3. At the hidden prompt, enter the original key exactly once and press Enter.
   Use password-manager auto-type only when it does not use the operating-system
   clipboard. The prompt is not a command-line argument and is not written by
   the helper. The helper fixes the source-derived endpoint internally; do not
   add an endpoint or key argument.

4. Accept only this nonsecret success signal:

   ```text
   P5E_EXPECTED_VALUE_PROCESS_LOAD=PASS
   ```

   It means a 64-hex digest exists only in that PowerShell process. It does
   not reveal the digest and does not run an account check.

5. Keep that PowerShell window open and do not run another P5E command. Reply
   only `đã nạp` when the signal appears; do not include a transcript,
   screenshot, digest, endpoint or key. A separate bounded request is still
   required before one account-check launch can be considered.

6. If you want to pause or the helper reports a stop, clear the process value
   and close the window:

   ```powershell
   .\scripts\p5e-load-expected-digest.ps1 -Clear
   ```

   Do not create or rotate a key, change app settings, install anything, or
   start the account runner to work around a stop.

## Remaining decision

The sole live boundary remains the owner-local Process load. Only after the
owner confirms its typed success without revealing a value may a separate,
one-launch account-check request be reviewed. `MATCH` would establish equality
with this independent record only; it would not prove provider validity,
preservation, A4.3, RAW/P5 exit or P6 readiness.
