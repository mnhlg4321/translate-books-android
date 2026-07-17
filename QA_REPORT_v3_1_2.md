# QA Report v3.1.2

## Budget authorization

The user authorized a maximum live A/B budget of USD 0.50.

## Readiness result

The workspace contains provider/model defaults but no API key and no source TXT benchmark corpus. The app therefore correctly remains blocked before any paid request. No cost was incurred.

## Patch scope

- Live benchmark readiness validation.
- Safe, bounded context-length fallback with a changed request payload.
- Mandatory instruction and lock preservation during fallback.
- Pronoun conflict diagnostics.
- Regression tests and release metadata only.

## Remaining live gate

Select a representative TXT file in the app, configure a valid provider API key and keep the estimate at or below USD 0.50. Run Full and Balanced with identical model, temperature, chunks, configuration documents and max-output policy; then perform blind language-quality review before accepting measured savings.
