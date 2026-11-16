---
name: gitteller-build-verify
description: "Build, test, and verify GitTeller Android APKs. Use when assembling debug APKs, running unit tests, checking signatures, computing SHA-256, or preparing a handoff package."
type: workflow
lifecycle: active
---

# GitTeller Build & Verify

Produce a real, installable-over-previous APK with evidence. Never claim DEVICE_VERIFIED without owner confirmation on A52.

## Version & Identity

- `applicationId` = `app.gitteller.mobile` (never change)
- Next version after 1.0.13.1-dev: bump both `versionName` and `versionCode` (current shipped code is 29 / `1.0.13.1-dev`).
- Same debug certificate required for overlay install. SHA-256 of cert: `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef`
- `app/build.gradle` must keep the original debug.keystore. Do **not** create or replace the keystore. If unavailable → report blocker, do not ship a differently signed APK as an update.

## Build Commands

```bash
# From project root (after extracting source)
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

- Capture full Gradle log + JUnit XML.
- Known flaky: `Unexpected type tag 71` in Gradle Test Executor. If it happens, note the first failure honestly and re-run; report both.
- Unit tests currently fail in this sandbox (Gradle daemon + org.json classpath). Mark `NOT_DEVICE_VERIFIED` / environment limitation; do not hide.

## APK Verification

After assemble:

```bash
aapt dump badging app/build/outputs/apk/debug/app-debug.apk
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

Confirm:
- package, versionCode, versionName
- permissions
- signer cert SHA-256 matches expected
- actual APK SHA-256 recorded

## Handoff Package

Must include:
- `GitTeller-1.0.XX-dev.apk` (same signature)
- `GitTeller-1.0.XX-dev-source.zip` with current sources, diff from previous, README/CHANGELOG/SECURITY/UPDATE, new tests, build instructions. No secrets.
- Build logs, test results, SHA-256 of both artifacts and cert
- Short matrix: REQUIREMENT → FILE/FUNCTION → TEST → RESULT → UNVERIFIED

If build fails: do not declare success. Save working parts, exact logs, and describe the blocker.

## Environment Notes

This sandbox often cannot complete unit tests. Prefer producing the APK + static checks (aapt, apksigner, SHA) and clear notes about what was not run. Owner verification on A52 remains `OWNER_PENDING`.
