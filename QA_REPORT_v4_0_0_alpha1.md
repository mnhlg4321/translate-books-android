# QA Report v4.0.0-alpha1

## UX scope

This alpha establishes the adaptive shell, settings information architecture and appearance system. It intentionally does not redesign Translate content yet; that is the scope of alpha2 after shell/state QA.

## Navigation

- Phone: bottom destinations for Translate, Jobs, Library and Settings.
- Tablet/large landscape (720dp+): navigation rail with the same destinations.
- Library keeps Files, Glossaries, Pronouns and Sample as compact secondary destinations.

## Appearance

Fifteen palettes are persisted locally and apply after Activity recreation. Palette accent colors do not replace semantic status colors. Invalid stored palette IDs fall back to Default.

## Settings

Settings are no longer one long form. Categories separate common defaults, translation behavior, provider credentials, prompt files, performance tuning, appearance, logging/privacy and release information.

## Manual QA

- Change every palette and recreate the Activity.
- Verify active jobs continue while changing appearance/navigation.
- Test 360dp portrait, landscape and 720dp+ tablet/emulator.
- Verify each Settings category saves only visible edits without clearing hidden fields.
- Verify Library secondary navigation still opens all previous pages.
- Verify status colors remain green/amber/red across palettes.
