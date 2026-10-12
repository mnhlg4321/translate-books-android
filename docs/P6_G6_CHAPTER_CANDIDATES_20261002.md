# P6 G6 — candidate chapters for the two remaining representative finals (2026-10-02)

Read-only listing from `D:\Ebooks\MERCEDES\VOL 5` (RAW `02_RAW_MERCEDES_VOL5`, DRAFT `05_DRAFT_MERCEDES_VOL5`, GLOSSARY `03_…`, PRONOUN `04_…`). Nothing was imported, bound or sent anywhere. Characters are Python `len()` of the decoded text; "「" is the count of Japanese opening quotes in the RAW; pronoun/glossary are data rows (header excluded). Chapter 001 for reference: RAW 8,217, DRAFT 20,013, 34 quotes (4.1 per 1,000), 3 pronoun rows, 36 glossary rows.

| Ch | RAW | DRAFT | 「 (per 1,000) | pronoun rows | glossary rows | note |
|---|---|---|---|---|---|---|
| 007 | 8,499 | 20,675 | 53 (6.2) | 8 | 50 | most dialogue in absolute terms, 8 pronoun rows, 2nd-largest glossary |
| 009 | 8,379 | 20,815 | 33 (3.9) | 9 | 25 | most pronoun rows |
| 004 | 7,622 | 19,169 | 44 (5.8) | 3 | 27 | dense dialogue, few pronoun rows |
| 018 | 3,396 | 9,197 | 41 (12.1) | 4 | 12 | densest dialogue but short; looks like an afterword |
| 010 | 9,876 | **25,536** | 50 (5.1) | 7 | 22 | longest DRAFT, ~28 % longer than 001 |
| 014 | **10,042** | 24,586 | 6 (0.6) | 7 | 34 | longest RAW, almost no dialogue |
| 013 | 4,323 | 11,999 | 12 (2.8) | 3 | 21 | shortest full chapter |
| 015/016/017 | 854 / 186 / 698 | 2,392 / – / – | – | – | – | fragments (016 and 017 have no DRAFT) |

Proposal (owner decides):
- **Dialogue/pronoun-dense:** `007` (highest absolute dialogue, 8 pronoun rows, large glossary); `009` if pronoun handling matters more than dialogue volume.
- **Long, near the limit:** `010` (longest DRAFT, so the largest L2/L3 inputs and outputs); `014` if the longest RAW matters more. Both fit the D3 caps (input ≤ 200,000 byte per call; chapter 001 used 21k–40k input tokens per call).
- **Short:** chapter 001 (8.2k RAW) is mid-sized for this volume; if it should not count as the "short" category, `013` is the shortest complete chapter.

Dependency to settle before any live run: the pilot's P4 project/binding for a chapter is created once and is immutable. The Editorial tab offers no file import for a P4-bound project, so each new chapter needs a project/binding setup (the create-project dialog takes pasted text; the fresh-pilot instrumented runner is the other route) and then its own L1 RAW + RECONCILE, which is another two provider calls per chapter on top of the L2/L3 chain (about USD 0.01 for L1 plus roughly USD 0.04 for the chain, by the chapter 001 measurements).
