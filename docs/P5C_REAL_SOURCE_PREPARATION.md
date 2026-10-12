# P5C real source preparation — 2026-09-07

Status: SOURCE_PREPARATION_INCOMPLETE; live provider calls: 0.

User supplied `D:\Ebooks\MERCEDES\VOL 4` and delegated remaining setup choices. Data egress is approved in principle, total pilot cost is capped at USD 0.10 and execution time at 5 minutes. Final live-call confirmation after concrete preflight is still required by P5C section 11.

Selected candidate chapter: 001. Original files remain unchanged; no book text is included here.

| Role | Relative source | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| RAW | `6.ALL/001_RAW_1_4.txt` | 7685 | FE4CE02301E9EE5FD457A7A744C982B23EE3C95100CFDC5CBDF908B3979F40AF |
| DRAFT | `5. DRAFT/001_DRAFT_MERCEDES_VOL4.txt` | 9075 | 2B0213214190E0DED1DD1AADE70D7959C23BED294E278E8DFC962BCBC73C042C |
| GLOSSARY | `6.ALL/001_CHAPTER_GLOSSARY_FINAL_MERCEDES_VOL4.csv` | 1652 | 459FBF1037BC52FA756843BFF7364759D0460388A38F51D16662D8751ACC5E8C |
| PRONOUN | `6.ALL/001_PRONOUN.csv` | 175 | 1F71CDCBC8760022E171FB8303E58A312D1913EE157CF783350E62100D9829FB |

Glossary has five header columns; pronoun has seven. Both CSVs begin with UTF-8 BOM. `EditorialSourcePreflight.strictUtf8WithoutBom` rejects those exact bytes. This is a source/code inspection finding, not an executed preflight PASS. Attempted jshell execution could not run because the bundled JBR has no jshell executable. Semantic chapter assessment has not started.

Read-only device inspection on `15e84958`: package directory contains cache/code_cache only; shared_prefs does not exist. No persisted pilot binding or account configuration was obtained. No provider credential environment variable was found by name-only inspection. Secrets were not printed.

Additional production limitation: `EditorialP5CExactBindingExecution.boundedOutputTokens` caps requested output at 256; the provider remains injected. Existing fake acceptance does not establish real chapter readiness.

Next work: establish the intended existing provider/account through its secret configuration mechanism; resolve CSV BOM handling with explicit derived-source provenance or a justified tested reader policy; create and reload a real P4 binding; finish provider integration and realistic output budget validation; then produce exact-binding fake evidence and a priced preflight for final live confirmation. Do not fabricate IDs or treat the earlier synthetic test binding as the user's persisted binding.
