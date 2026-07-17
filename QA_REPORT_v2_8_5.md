# TBLAndroidTxt v2.8.5 Glossary / Pronoun Engine - QA Report

## Build target

| Item | Value |
|---|---|
| Base | v2.8.0-file-saf-stable |
| New versionCode | 26 |
| New versionName | `2.8.5-glossary-pronoun-engine` |
| Main goal | Validate glossary/pronoun data and preview only matched rules per chunk |

## What changed

| File | Change |
|---|---|
| `app/build.gradle` | Bumped version to 2.8.5 / versionCode 26 |
| `PromptContextBuilder.java` | Rebuilt as shared glossary/pronoun matching + validation engine |
| `GlossaryStore.java` | Glossary import now delegates parsing to the shared engine |
| `MainActivity.java` | Adds glossary/pronoun validation during import, before start, and via health check |
| `GlossaryPageFactory.java` | Adds `Health check` button on list page and `Health` button in editor |
| `TranslationEngine.java` | Logs matched glossary/pronoun lock counts per chunk before API call |
| `LogStore.java`, `JobStore.java` | Export bundles identify app version 2.8.5 |
| `JobsPageFactory.java` | Dashboard copy updated for 2.8.5 |
| `CHANGELOG.md` | Added v2.8.5 notes |

## Engine behavior

### Glossary handling

- Supports CSV/TSV/TXT arrow formats and JSON arrays/objects.
- Detects malformed rows instead of silently ignoring all suspicious content.
- Detects duplicate same source/target pairs.
- Detects same source with different targets.
- Keeps existing 3-column glossary compatibility.

### Pronoun handling

- Supports free-text rules, CSV-style `from,to,pair`, and JSON rules.
- Explicit Pronoun file has priority over pronoun notes inside glossary column 3.
- If explicit Pronoun file exists, glossary column-3 pronoun notes are not injected as pronoun rules, but the name/character lock remains.
- Prompt preview shows the matched pronoun rules for the selected chunk.

### Prompt injection

- Prompt still injects only relevant glossary/pronoun entries for the current chunk.
- Matching checks source and target aliases.
- Prompt preview now shows:
  - matched glossary count;
  - matched pronoun count;
  - total available rules;
  - injected character estimate;
  - warnings from validation.

## Fixes included

| Issue | Status |
|---|---|
| `PromptContextBuilder` had compile-risk brace structure from older patch | Fixed by rewriting the file cleanly |
| Duplicate `inputUrisJson()` declaration in `MainActivity` | Fixed |
| Glossary import used a separate parser from prompt injection | Unified through shared engine |
| Pronoun/glossary conflicts were invisible | Added health check and import/start warnings |

## Static QA performed

| Check | Result |
|---|---|
| Source zip extracted from v2.8.0 | Pass |
| Version bump in Gradle | Pass |
| Java brace/string/comment balance on all `.java` files | Pass |
| `PromptContextBuilder` syntax check with lightweight stubs | Pass |
| No duplicated `inputUrisJson()` signature | Pass |
| No DrawerLayout reintroduced | Pass |
| Job Manager actions retained | Pass |
| SAF/file handling code retained | Pass |

## Not verified in sandbox

APK build was not run here because the sandbox does not include Android SDK, Android Gradle Plugin runtime, or a Gradle wrapper. Android Studio/Gradle verification is still required.

## Manual QA checklist in Android Studio

1. Sync Gradle and build APK.
2. Confirm versionName is `2.8.5-glossary-pronoun-engine`.
3. Open Glossaries tab, press `Health check` with no glossary/pronoun.
4. Import a normal 3-column glossary CSV and confirm term count appears.
5. Import a glossary with duplicate same source/target and confirm duplicate warning appears.
6. Import a glossary where the same source has two different targets and confirm conflict warning appears.
7. Import a Pronoun file and confirm explicit pronoun priority is shown.
8. Open Prompt preview for a TXT chunk and confirm matched glossary/pronoun counts appear.
9. Start translation and confirm runtime log contains `Chunk N locks: glossary=X, pronoun=Y`.
10. Confirm translation output is unchanged for a file with no glossary/pronoun.
11. Confirm old v2.7.5 Jobs actions still work: Resume, Retry, Details, Prompt, Export, Delete.
12. Confirm old v2.8.0 file tests still pass: input URI, output folder, UTF-8 BOM, Shift-JIS fallback, output filename sanitize.

## Acceptance criteria

v2.8.5 can be accepted if:

- app builds successfully in Android Studio;
- old glossary CSV 3-column files still import correctly;
- explicit Pronoun file takes priority;
- prompt preview clearly shows matched glossary/pronoun rules;
- warnings appear for malformed/conflicting data but do not block translation unnecessarily;
- normal translation flow from v2.8.0 remains working.

## Deferred to v2.9.0

| Item | Reason |
|---|---|
| Larger UI polish | Should be done after core/file/glossary are stable |
| Full database-backed glossary editor | Current SharedPreferences glossary is enough for 3.0 path |
| Token-level exact estimator | Needs provider/model tokenizer support |
| Pronoun conflict graph UI | Useful later, but too heavy for this stabilizing build |
