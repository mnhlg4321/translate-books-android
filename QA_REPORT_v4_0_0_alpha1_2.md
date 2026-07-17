# QA Report v4.0.0-alpha1.2

## Root cause

Glossaries used a dedicated multi-item store with an active ID, while Pronoun was kept directly as one URI/text value in AppSettings. The screens therefore could not offer the same selection model.

## New workflow

Pronouns now has a library list matching Glossaries: import multiple profiles, select one Active profile, inspect/edit/replace it, or delete it without deleting the original device file. Translate and new jobs receive the active profile text.

## Migration

On first launch, an existing single pronoun file/text is imported into PronounStore and selected. Migration is one-time and does not remove the legacy settings snapshot until the active profile has been synchronized.
