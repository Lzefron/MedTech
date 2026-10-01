# MedTrack 1.2.2 — Play Console package

This release uses **`com.lzefron.medtrack`**, matching the app created in Google Play Console. Version code is **5**. The previous `com.medtrack.app` package was unavailable for this publisher.

- **MedTrack-v1.2.2.aab** — upload this file under Internal testing → Create new release → App bundles in Play Console.
- **MedTrack-v1.2.2.apk** — install directly on an Android phone.
- **SHA256SUMS.txt** — checksums for both packages.

Scanning and record storage remain offline. No API setup, account or hosting is required. Review every scanned result against the original report before saving.

## Existing installations

Android treats the new package as a separate app. Version 1.2.2 cannot automatically access records from 1.2.1 or earlier (`com.medtrack.app`). Keep the old app installed if it contains important records, and export its PDF summary for reference. Do not uninstall it or clear its storage to install this new app. Future updates using `com.lzefron.medtrack` and the same signing key can preserve the new app's records.

This GitHub release provides the signed files; it does not submit or publish the app in Google Play automatically. Complete the store listing, testing and declarations in Play Console, including a public privacy-policy URL.

## Verification

The build and JVM test checks passed, and all 13 Android emulator tests passed under the new QA package. Coverage includes profile creation/selection, offline report reading and saving, retained photos/text, database migration and PDF sharing. Android lint reported 0 errors and 27 existing warnings.
