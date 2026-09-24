# MedTrack Privacy Policy

**Effective date: September 25, 2026**

MedTrack is a personal health-record organizer for Android. This policy describes the behavior of version 1.2.0.

## Data handled by the app

Users may enter profile details, laboratory results, medicine schedules, emergency information, notes, and report images. This information is stored locally in the app's private storage on the user's device.

## Data collection and sharing

Local report reading runs on the device. MedTrack does not include advertising, analytics or cloud synchronization. Without configuring and approving optional AI reading, report photos and health records are not uploaded.

If you configure a private AI gateway and choose **Send this photo**, the app sends the entire selected photo, including any patient information visible in it, to that gateway and Google Gemini for transcription. No other saved reports are sent. The destination is shown before upload. Use only a trusted gateway with billing-enabled Gemini services and the patient's permission. Google and the selected hosting provider have their own data-processing and retention policies. Google's [Gemini terms](https://ai.google.dev/gemini-api/terms) describe its paid-service handling; the app cannot delete copies retained by external providers.

The supplied gateway does not store images or log request contents. Its operator must configure the hosting platform accordingly. The response is shown as editable fields; saving remains a user action. Recognized text is saved locally as unverified transcription alongside the reviewed results and original photo.

## Permissions

The app may use a camera application or photo picker when the user chooses a report image. Internet permission supports optional AI reading through HTTPS. It requests notification permission when the user enables medicine reminders. Exported PDF files and files deliberately shared by the user leave app-private storage under the user's control.

## Retention and deletion

Records remain on the device until the user deletes the corresponding report or profile, clears the app's storage, or uninstalls the app. Android backup is disabled for this version. Uninstalling the app or clearing its storage permanently removes its local records.

## Security

Application data uses Android app-private storage. Gateway access tokens are encrypted with Android Keystore; provider API keys stay on the gateway. **Disconnect AI** removes the saved connection and token. Users should protect access to their device. The profile chooser is not an authentication mechanism; the app does not provide a separate PIN or biometric lock.

## Children

MedTrack is intended to be operated by adults managing their own or their family's records. It is not directed to children for independent use.

## Medical notice

MedTrack organizes user-provided information. It does not provide diagnosis, treatment, or emergency medical services. Users must review scanned values against the original document and consult a qualified clinician for medical decisions.

## Changes and contact

Material changes to data handling will be reflected in an updated policy and app release. Before Play Store publication, the publisher must add a monitored contact email and host this policy at a publicly accessible URL.
