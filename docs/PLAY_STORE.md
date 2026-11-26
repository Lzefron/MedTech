# Publishing MedTrack on Google Play

## File to upload

Upload `MedTrack-v1.2.3.aab` from the [latest GitHub Release](https://github.com/Lzefron/MedTech/releases/latest). Do not upload the APK for a new Play Store application. The APK is provided for direct installation outside Google Play.

| Property | Value |
| --- | --- |
| Application ID / package name | `com.lzefron.medtrack` |
| Version code | `6` |
| Version name | `1.2.3` |
| Minimum Android version | Android 8.0 / API 26 |
| Target SDK | API 36 |

The AAB is signed with the MedTrack upload key. Enroll in Play App Signing during the first release; Google then manages the app-signing key while this upload key verifies future bundles.

Version 1.2.2 changes the package from `com.medtrack.app` to `com.lzefron.medtrack` to match the publisher's new Play Console app. Future updates, including 1.2.3, use the same package name. On phones, this package installs separately from earlier builds and does not automatically migrate their private records; retain the old app and export any needed PDF summaries.

## Play Console steps

1. Create the app in Play Console and use the package name above.
2. Complete the main store listing, content rating, target audience, ads declaration, app access, data safety, and the Health apps declaration.
3. Paste the [public privacy policy](https://lzefron.github.io/MedTech/) URL into Play Console. Version 1.2.3 links to this policy from the profile screen.
4. Go to **Test and release → Testing → Internal testing** first, choose **Create new release**, confirm Play App Signing, and upload `MedTrack-v1.2.3.aab`. For an existing closed test, edit its draft and attach that bundle.
5. Add release notes, save, review the release, and invite testers. After verifying the Play-delivered build, promote it through the required testing track and then to production.

Google requires every published app to complete the Health apps declaration. MedTrack provides health-record management features, so declare those features accurately; do not select “no health features.” Version 1.2.2 performs report reading and storage on the device, removes cloud upload controls, and has no internet permission. Complete Data safety answers according to this shipped version and any services you add; deliberate user exports remain available. Publish the updated privacy policy.

New personal Play developer accounts may have to meet Google's closed-testing requirement before production access is available. Play Console will show the requirement for the account.

## Future updates

- Keep the package name `com.lzefron.medtrack` unchanged.
- Increase `versionCode` for every uploaded bundle (2, 3, 4, and so on).
- Update `versionName` for the user-visible release number.
- Sign every bundle with the same private upload key.
- Keep `release-secrets/medtrack-upload.jks` and `release-secrets/keystore.properties` backed up securely. Neither file belongs in GitHub, email, chat, or a Release.
- Run `./Build-Play-Release.ps1`, test the signed APK, and upload the generated AAB.

Official references: [prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348), [Play App Signing](https://developer.android.com/studio/publish/app-signing), and [Health apps declaration](https://support.google.com/googleplay/android-developer/answer/14738291).
