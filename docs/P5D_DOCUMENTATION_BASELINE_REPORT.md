# P5D.0 — Documentation baseline consistency

Ngày kiểm tra: `2026-09-08` (+07:00)

## Gate

```text
P5D_DOCUMENTATION_BASELINE_CONSISTENT
NO_PROVIDER_CALL_IN_P5D.0-P5D.3
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

## Baseline

| Hạng mục | Giá trị |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| Starting HEAD | `e5733cf0563c12632988c2867288eee52a906b17` |
| Latest validation APK | `4.17-dev.16 / code184` |
| APK SHA-256 | `AA69A6EDD8A7B11D8488FC431B71097515FE6BD738710C970F6E4A8F54150797` |
| Device after P5C validation | `4.17-dev.1 / code169`; validation data removed by uninstall/reinstall |
| P4 source start, corrected | `270759e5589b2e9101c1e3a5a6b84cff12ec2fd3` |

## Corrections

- Corrected the P4 commit typo from `...9101c3e1...` to the resolvable commit
  `...9101c1e3...` in current state, P4 reports, P4 gap map, P5C historical
  documentation and the V4.18 checklist.
- Promoted code184 to the current/latest validation artifact in current state;
  code181, code177 and code176 remain explicitly historical artifacts.
- Kept the 2026-09-08 live attempt unchanged as a provider stop:
  `RETRY_PROVIDER_CALL_FAILED`, one RAW request, no response/usage, no
  RECONCILE, no report/receipt commit and external billing state unknown.
- Normalized current wording to distinguish `PASS`, approved `SKIP` and
  provider `FAIL/STOP`; historical reports retain their original result and
  date.

## Integrity and scope

- Canonical ZIP, Java control ZIP, profile v2 and all three authority hashes
  remain unchanged.
- No provider/API call was made during P5D.0. P5D.1-P5D.3 remain read-only or
  local hardening work; no retry authorization exists.
- No production source, database schema, UI, build metadata, authority or
  canonical pack was changed by this documentation gate.
- `git diff --check` passes before the documentation commit.

The next step is a bounded read-only OpenRouter Activity audit for the old
request. Input/output logging must not be enabled for that audit.
