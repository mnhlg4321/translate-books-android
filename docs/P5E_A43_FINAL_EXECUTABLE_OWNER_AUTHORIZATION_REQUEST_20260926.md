# P5E A4.3 — final executable owner authorization request

## Decision state

`FINAL_EXECUTABLE_PACKET_READY / OWNER_DECISION_PENDING / NOT_DISPATCHED / P6_NOT_READY`

Work package: `P5E_A43_FINAL_EXECUTABLE_PACKET_REPIN`

This is the only current owner authorization request. It asks for one fresh
decision for one possible future A4.3 event on serial `15e84958`. It does not
execute the command, open an event, retry or redispatch any event, issue RAW,
claim P5 exit or open P6. Every earlier event, decision, manifest and command
remains closed/consumed and non-reusable.

No additional key, endpoint or fingerprint is requested from the owner. The
future command's expected-value boundary remains process-only and owner-
controlled; the agent has not read or stored that value.

## Final packet pins

| Input | Exact SHA-256 |
|---|---|
| New approval manifest `docs/P5E_A43_FINAL_EXECUTABLE_APPROVAL_MANIFEST_20260926.md` | `23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053` |
| New executable command `docs/P5E_A43_FINAL_EXECUTABLE_COMMAND_20260926.txt` | `C84355B912DCF3BB59D52004EC80E06CE8B37FC75B5ABE083ECCA95D64C89BA3` |
| Required helper `scripts/p5e-raw-live-supervisor.ps1` | `17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E` |
| Binary exporter `scripts/p5e-db-binary-export.ps1` | `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06` |
| SQLite bridge `docs/P5E_SQLITE_BRIDGE.py` | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| Toolchain resolver `scripts/p5e-raw-toolchain.ps1` | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |
| `local.properties` | `71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2` |
| Production APK, code207 | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` |
| Production source ZIP | `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348` |
| Production `BUILD_INFO.json` | `DB20DA0CF708410AAAB65E5AF69ADF89A769ED62240B577A89CA4E2007FB7F06` |
| AndroidTest APK | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` |
| AndroidTest source ZIP | `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F` |
| AndroidTest `BUILD_INFO.json` | `772F32E23AEF3537FEF00DACE8E4B9B994448BFEE2071150CF0E0540DDCCC5A9` |
| Production/test certificate identity | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |
| Final packet QA script | `BACC1F7676DF394822BAAAD135826CA1B38CD55EC222368EE661D7E50AE46BDE` |
| Final packet QA report | `F967E73CB8FC6939E6ED8C9D13584150A65AF013F8D1BF0B43EC21B0852A76BF` |
| Luna exact-byte review | `05B11BB5C174D4DB24C9E5E7472396AFFCEC8AFB83A7D75B9F34E2E3601C24D7` |

Serial: `15e84958`.

The consumed references deliberately are not inputs to this request:

- Manifest `669C54049920C49344D2FB55533EFA9FA9F87E933A6A18DE5FA7215F1146D147`.
- Command `A2EF2BA90F07D3F4D2517E7F1541EA752615F6BCF61E304E579E301A8D5D3D08`.

## Offline evidence and boundaries

- Final packet QA: `14/14 PASS`, `0` failures.
- Captured-export/binding QA: `262/262 PASS`; regression matrix: `175/175 PASS`.
- DB host-readback QA: `56/56 PASS`.
- PrepareEvent simulation, dependency/hash order, command binding,
  path/reparse guards, expected-value isolation, redaction/secret scan,
  timeout/no-retry/no-redispatch, consumed-event rejection, PowerShell 5.1
  parse and `git diff --check`: PASS.
- Luna: `PASS / 0 BLOCKER / 0 HIGH / 1 MEDIUM / 2 LOW`.
- Live counters for this work package: ADB `0`; device reads/writes `0`;
  provider `0`; credential `0`; database writes `0`; build/install `0`;
  RAW dispatch `0`; redispatch `0`.
- DRAFT and PRONOUN remain hidden. Only the already-bound RAW/GLOSSARY scope
  is visible to any future approved call.

## Copyable owner approval sentence

Approve exactly one fresh A4.3 event on serial `15e84958` under manifest SHA-256 `23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053`, command SHA-256 `C84355B912DCF3BB59D52004EC80E06CE8B37FC75B5ABE083ECCA95D64C89BA3`, helper SHA-256 `17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E`, exporter SHA-256 `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06` and SQLite bridge SHA-256 `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`; allow only the pinned read-only Before/After collection, memory-only account comparison, one fresh authorization and the exact allowlisted RAW/GLOSSARY scope, with no retry, repair, fallback, RECONCILE, cleanup, restore, redispatch, build/install or P6 action; do not reuse any closed event, decision, manifest or command, and do not require any additional key, endpoint or fingerprint from the agent.

Until that sentence is explicitly approved, the command is not to be run.
