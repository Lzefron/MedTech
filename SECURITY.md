# Repository security

MedTrack source and GitHub Release downloads are public. The Android 1.2.2
release reads and stores records on the device and has no internet permission.
The experimental vision server is not connected to that release and must not
be treated as a production service for public customers.

## Keep private material outside GitHub

Do not commit or attach API keys, gateway tokens, passwords, signing keystores,
`keystore.properties`, encrypted credential caches, patient databases, report
photos, exported PDFs or logs containing patient details. Use fictional data
in tests and screenshots. Review screenshots for names, identifiers and QR
codes before sharing them. `.gitignore` helps with accidental staging; it
cannot protect a file already committed or deliberately force-added.

Signed APK/AAB files belong in Releases after inspection. Their public signing
certificate is expected to be visible; the private signing key must never be
included. Retain an independent secure backup of that key. Do not generate a
replacement key merely because a local checkout is restored from GitHub.

## Checks for contributors

Run `python tools/check_public_files.py` to check staged/tracked filenames.
Run `gitleaks git . --config .gitleaks.toml --redact --log-opts=--all` to scan
history. The Repository safety workflow runs both checks on pushes and pull
requests. The scanner includes Google's AQ. auth-key format in addition to
default secret rules. These checks reduce accidental exposure; they are not
a full application penetration test or an image-based patient-data detector.

A failed workflow does not remove a committed secret. Revoke or rotate exposed
credentials first, then coordinate removal from files, history, release assets
and any copies. Deleting the latest copy or making the repository private does
not invalidate a credential that someone already copied. Restrict direct pushes
and require Repository safety in branch protection/rulesets when configured.

## Reporting a vulnerability

Use GitHub private vulnerability reporting if available. Otherwise ask the
maintainer for a private reporting channel without posting exploit details,
credentials or patient data publicly. Reproduce issues using fictional data.
