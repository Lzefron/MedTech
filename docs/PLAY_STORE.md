# Publishing MedTrack on Google Play

## File to upload

Upload `MedTrack-v1.2.1.aab` from the [latest GitHub Release](https://github.com/Lzefron/MedTech/releases/latest). Do not upload the APK for a new Play Store application. The APK is provided for direct installation outside Google Play.

| Property | Value |
| --- | --- |
| Application ID / package name | `com.medtrack.app` |
| Version code | `4` |
| Version name | `1.2.1` |
| Minimum Android version | Android 8.0 / API 26 |
| Target SDK | API 36 |

The AAB is signed with the MedTrack upload key. Enroll in Play App Signing during the first release; Google then manages the app-signing key while this upload key verifies future bundles.

## Play Console steps

1. Create the app in Play Console and use the package name above.
2. Complete the main store listing, content rating, target audience, ads declaration, app access, data safety, and the Health apps declaration.
3. Add a publicly accessible privacy-policy URL. The repository includes [`PRIVACY.md`](../PRIVACY.md) as the prepared policy text, but a private GitHub file is not a public policy URL.
4. Go to **Test and release → Testing → Internal testing** first, choose **Create new release**, enable Play App Signing, and upload `MedTrack-v1.2.1.aab`.
5. Add release notes, save, review the release, and invite testers. After verifying the Play-delivered build, promote it through the required testing track and then to production.

Google requires every published app to complete the Health apps declaration. MedTrack provides health-record management features, so declare those features accurately; do not select “no health features.” Version 1.2.1 performs report reading and storage on the device, removes cloud upload controls, and has no internet permission. Complete Data safety answers according to this shipped version and any services you add; deliberate user exports remain available. Publish the updated privacy policy.

New personal Play developer accounts may have to meet Google's closed-testing requirement before production access is available. Play Console will show the requirement for the account.

## Future updates

- Keep the package name `com.medtrack.app` unchanged.
- Increase `versionCode` for every uploaded bundle (2, 3, 4, and so on).
- Update `versionName` for the user-visible release number.
- Sign every bundle with the same private upload key.
- Keep `release-secrets/medtrack-upload.jks` and `release-secrets/keystore.properties` backed up securely. Neither file belongs in GitHub, email, chat, or a Release.
- Run `./Build-Play-Release.ps1`, test the signed APK, and upload the generated AAB.

Official references: [prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348), [Play App Signing](https://developer.android.com/studio/publish/app-signing), and [Health apps declaration](https://support.google.com/googleplay/android-developer/answer/14738291).
