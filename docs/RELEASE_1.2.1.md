# MedTrack 1.2.1 — Offline scanning, no API setup

This update removes the AI connection dialog, gateway settings and cloud-reading actions from report scanning. Take or choose a photo, review the fields filled by on-device OCR, correct values or add missing tests, and save locally. No API key, account, hosting or paid service is required.

The Android release no longer requests internet permission. Photos, recognized text and saved records remain on the device unless you deliberately export/share them. Recognition can still miss handwriting, unclear text or unusual table layouts; compare results with the original report before saving.

## Downloads

- **MedTrack-v1.2.1.apk** — install this on an Android phone.
- **MedTrack-v1.2.1.aab** — upload this to Google Play Console (version code **4**).
- **SHA256SUMS.txt** — checksums for both files.

Install the APK as an update over the existing signed release to keep local records. Do not uninstall or clear app storage. The package remains `com.medtrack.app` and uses the existing upload signing key.

The repository is private, so GitHub downloads require an account with access. This release does not publish the app to Google Play automatically. Complete Play Console requirements and provide a public privacy-policy URL before production publication.

## Verification

43 JVM tests and 5 focused Android emulator tests passed. The scan test checks that API controls are absent, internet permission is absent, OCR fills editable fields, and corrected results save with the detected date. Local-storage tests cover retained photos/text and profile isolation. Android lint reports 0 errors and 27 existing warnings. The updated app was installed and launched on the review emulator.
