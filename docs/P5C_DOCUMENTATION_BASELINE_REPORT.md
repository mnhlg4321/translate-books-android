# P5C.0 — Documentation baseline consistency

Ngày kiểm tra: `2026-09-04` (+07:00)

## Decision

```text
P5_DOCUMENTATION_BASELINE_CONSISTENT
P5_DRY_RUN_ONLY
LIVE_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_RUNNABLE
```

P5C.0 chỉ sửa bằng chứng trạng thái và không mở provider access.

## Baseline

| Hạng mục | Giá trị đã xác nhận |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| Starting HEAD | `44e5a659f23da1314cd6294a87d1cc44d1a58686` |
| Device | `15e84958`, restored `4.17-dev.1` / code169 |
| Current validation APK | `4.17-dev.9` / code177 |
| APK SHA-256 | `8B4D4714287114013A43C181C232921ABC6D6BA43CFB34F5E87D6CABF34001FA` |
| Provider/API calls | `0` |

## P4 lineage correction

The active documentation now distinguishes the original P4 implementation
lineage from the post-closure correction. The following full hashes were
resolved with `git rev-parse` and recorded without replacing historical facts:

| Role | Commit |
|---|---|
| P4 source start | `270759e5589b2e9101c3e1a5a6b84cff12ec2fd3` |
| P4 implementation/test head before original docs closure | `76348b38174cdc6e25ce4ce19000b75984d44f75` |
| Original P4 docs closure | `8d676c336d8011ab1534d1d5528cb52171c11bbf` |
| P4 producer correction | `364faa42e7ed6bb08b75dda7fdc7335b8f931df7` |
| P4 correction docs closure | `16073c6285c5b31b13f25929ad7a774b5044d009` |

Current P5/P5C lineage is `788ce0c21b3e053bd49ba7c64008acd038b183cc`,
`cd9c91be3c62d922712685cbe59a9ea42f89d298`, `d0de39cf2ea22117303f299b522048a371372679`
and starting docs state `44e5a659f23da1314cd6294a87d1cc44d1a58686`.

## Artifact and count correction

`BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md` and the V4.18 checklist now identify
code177 as the current/latest validation artifact. Code176 remains explicitly
labeled historical P4 correction evidence in `docs/P4_VALIDATION_REPORT.md`;
it is not presented as the current artifact.

The device count is normalized everywhere to:

```text
112 total = 111 PASS + 1 approved real-API assumption skip; 0 failures
```

The historical counts remain traceable: P3B baseline `104`, P4 contribution
`+8`, host engine `178/178` (baseline `163`, P5 `+15`), app unit `210/210`,
external qualification `306/306`.

## Scope guard

- No provider, API, endpoint or account was contacted.
- No authority, canonical ZIP, profile resource, database schema, UI or build
  metadata was changed.
- No real chapter, request body, response body, API key or secret was added.
- `git diff --check` passed before the documentation commit.

The next P5C step is a failing exact-binding fake E2E contract over the
persisted P4 path. Live execution remains authorization-gated.
