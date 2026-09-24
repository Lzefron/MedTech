# MedTrack 1.2.0

- Scans fill editable result names, numbers/written results, units and ranges. Add missing tests on the same scan screen.
- Preserve partial rows, improve table grouping, support side-by-side test panels and more printed units. Scanned values keep their printed units.
- Read more date formats and prefer collection/test dates over report-printing dates. Missing or conflicting dates stay visible for correction.
- Make review/save requirements visible, show useful validation errors, confirm successful saving, and preserve the original photo plus unverified recognized text in the saved report.
- Optional Gemini gateway integration with photo-specific consent, encrypted gateway credentials, multiple server-side key/model routes, project-aware cooldowns and manual fallback. AI fills gaps without silently replacing edits.
- Preserve existing records with the database version 3 migration. Android version code 3, target API 36.

Install **MedTrack-v1.2.0.apk** on your phone; upload **MedTrack-v1.2.0.aab** to Play Console. Signed 1.0/1.1 installations can update without uninstalling. SHA256SUMS.txt contains verified checksums.

**AI setup is optional and not active by default.** The gateway must be hosted and configured with your billing-enabled Gemini keys; see vision-server/README.md. No keys or live endpoint are bundled. Automated API tests use simulated responses; live model accuracy has not been validated against your report. Every extraction, especially handwriting, requires checking against the photo.
