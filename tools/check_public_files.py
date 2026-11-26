"""Reject credential, patient-data and binary-package files in the Git index.

Only filenames are printed. This complements Gitleaks; it cannot determine
whether a screenshot contains real patient details. Review images manually.
"""
from pathlib import PurePosixPath
import fnmatch
import subprocess
import sys

BLOCKED_NAMES = {"keystore.properties", "id_rsa", "id_ed25519", "id_ecdsa"}
BLOCKED_DIRECTORIES = {
    "release-secrets", "private-vision", "patient-data", "private-reports",
    "audit-results", "node_modules", ".gradle", "gradle-cache", ".git",
}
BLOCKED_PATTERNS = (
    "*.jks", "*.keystore", "*.jceks", "*.p12", "*.pfx", "*.pem", "*.key",
    "*.dpapi.json", ".env", ".env.*", "*.db", "*.db-*", "*.sqlite",
    "*.sqlite-*", "*.sqlite3", "*.sqlite3-*", "*.pdf", "*.apk", "*.aab",
    "*.log", "*service-account*.json", "*service_account*.json",
    "*serviceaccount*.json", "credentials.json", "google-services.json",
)


def blocked(path):
    parts = PurePosixPath(path.replace("\\", "/")).parts
    names = [part.lower() for part in parts]
    if not names:
        return False
    return (names[-1] in BLOCKED_NAMES
            or bool(set(names[:-1]) & BLOCKED_DIRECTORIES)
            or any(fnmatch.fnmatchcase(names[-1], pattern)
                   for pattern in BLOCKED_PATTERNS))


def main():
    result = subprocess.run(
        ["git", "ls-files", "--cached", "-z"], check=True, capture_output=True)
    paths = result.stdout.decode("utf-8", errors="surrogateescape").split("\0")
    rejected = sorted(path for path in paths if path and blocked(path))
    if rejected:
        print("Remove these files from the Git index; keep local copies private:")
        for path in rejected:
            print("  " + path)
        return 1
    print("Tracked-file safety check passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
