# P5E owner local input guide — metadata only

This guide is for the owner’s next response. Do not send an API key, endpoint,
fingerprint, settings value, screenshot, command transcript or credential in
chat, Git, an issue or shared evidence.

## Minimal response

Reply with one of these statements and the nonsecret metadata below:

- `Tôi còn key gốc ở nơi lưu riêng.`
- `Key chỉ còn trong app.`

Then provide, if known:

```text
recordAvailability=YES | NO | NOT_VERIFIED
recordAuthority=<owner-controlled authority label>
recordReference=<opaque reference; no secret>
accountOrProjectMapping=<nonsecret account/project label>
verificationTime=<timestamp and timezone, or NOT_VERIFIED>
recordPredatesActualRead=YES | NO | NOT_VERIFIED
endpointScopeMapping=YES | NO | NOT_VERIFIED
```

Do not invent a reference or use the key name itself as a digest. If the
record is absent or the account/endpoint mapping is unclear, the result is
`EXPECTED_PROVENANCE_UNAVAILABLE_STOP` and the device branch stays closed.
The local host repair can still remain complete.

## What counts as an independent record

The record must be controlled outside the device actual-read event, have an
authority and opaque reference, and map unambiguously to the same account and
endpoint scope. It may contain a previously verified 64-hex digest, or the
original key plus independently recorded endpoint configuration for the owner
to derive locally. The agent does not read either form.

If the owner derives a digest locally, use the Android source semantics exactly:
normalize the endpoint with the Java `normalizeEndpoint` behavior, then hash
the UTF-8 bytes of `normalizedEndpoint + one LF character + exact API-key
bytes`. Do not trim or rewrite the key, change endpoint settings, or tune the
input to obtain a match. Keep the digest and source credential in the owner’s
local controlled process only.

## Conditional process-only setup

This is not an instruction to launch now. Only after the repair QA is accepted
and the owner record is independently verified may the owner load the expected
digest into the exact PowerShell Process that will create the child runner.
The value must not be placed in User/Machine environment, a file, clipboard,
command line or transcript. A new PowerShell window does not inherit a value
from a different existing window.

Use a local placeholder in any shared example; never paste the real value here:

```powershell
$env:P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT = '<owner-local-64-hex-digest>'
```

The host runner must be started from that same process or a child of it, with
the new repair pins. It will pass the expected only through its in-memory
stdin path to the remote shell and will reject any leak or incomplete
instrumentation result. Do not run the command merely because a value can be
supplied; owner metadata and the separate account-check gate are still
required. Do not reinstall the APK or run RAW as part of this setup.

After a permitted follow-on, the receipt may contain only typed status, counts,
serial, launch/timeout/exit metadata and source hashes. It must not contain
the expected value, actual value, key, endpoint or digest.
