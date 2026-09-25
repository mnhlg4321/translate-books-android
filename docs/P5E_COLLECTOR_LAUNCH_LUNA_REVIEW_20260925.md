# Luna review — RAW collector launch repair

Date: 2026-09-25  
Baseline: `148a6a52da7fec230c7e1c1e8bacae758c806816`  
Scope: offline critique of `P5E_COLLECTOR_LAUNCH_NEXT_WORK_REQUEST_20260925.md`

## Verdict

`DIAGNOSTIC_PASS_REPAIR_NOT_GREEN`.

The synthetic probe is useful and correctly bounded: a missing executable produces `FAILED_BEFORE_LAUNCH/launch0`, while an absolute PowerShell executable produces `launch1/exit0`. This validates the supervisor classification mechanism only. It does not prove the historical cause of the closed event, and it is not a runtime repair. The old event remains terminal; no retry, device access, provider call, credential read, or redispatch is authorized by this review.

The leading source finding remains credible: RAW passes bare `adb`/`apksigner` into `ProcessStartInfo`, and Dispatch separately hardcodes `FilePath 'adb'`. The old receipt has no native launch error, so the review must not upgrade that finding into historical cause proof or infer package/device state.

## Required repair gates

The repair plan is complete only when one side-effect-free toolchain contract is used by Before, Dispatch, and After. It must resolve and validate absolute regular files for ADB, the signer, and Java where required; reject missing, ambiguous, or reparse paths before expected/device access; and pass the same resolved paths through every phase. The plan must explicitly settle whether Windows `apksigner.bat` is launched through `cmd.exe` or via a Java-plus-`apksigner.jar` contract, with quoting tested under Windows PowerShell 5.1. File existence alone is insufficient.

The fake integration must cover the whole Prepare → Before → at-most-one Dispatch → After-finally → Verify chain. It must include absolute paths containing spaces, missing ADB/signer/Java, reparse paths, native process-start failure, nonzero exit, timeout, stdin/capture handling, redaction, and environment inheritance. It must prove that an expected account value is not inherited by tools that do not need it, and that a prelaunch failure cannot trigger redispatch. The tests must use fake tools only; no ADB, signer, Java, device, or provider command may run.

Launch diagnostics should persist only an allowlisted error class/native code or typed reason. Exception text, argv, PATH/environment dumps, and raw output remain forbidden. Any schema change requires synchronized validator, fixture, dependency-pin, and hash/provenance updates while preserving the old receipt and event hashes.

## Release boundary

Until those gates pass, the state is `LOCAL_HOST_REPAIR_REQUIRED / OLD_EVENT_CLOSED / RAW_NOT_DISPATCHED / P6_NOT_READY`. A future live attempt requires a new packet and a new owner decision; the prior event decision and hashes cannot be reused. Even after offline repair is green, it establishes tool-launch readiness only and does not establish live RAW acceptance or P5/P6 readiness.

