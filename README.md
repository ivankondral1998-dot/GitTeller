# GitTeller

Native Android app (`app.gitteller.mobile`) that explains what actually changed in a GitHub project. Read-only. Two modes, one data model.

This repository is the versioned archive plus the current source.

## Layout

| Path | What |
|---|---|
| `versions/v1.0` … `versions/v1.0.13.1` | APK, source zip, audit, handoff and screenshots for that version |
| `current/source` | Live 1.0.13.1-dev tree (versionCode 29) |
| `handoffs` | Early Grok handoff packs that are not tied to one version folder |
| `screenshots` | Extra device shots |
| `INDEX.md` | Size and SHA-256 of every archived file |

Versions are ordered from first to last. Overlay installs keep the same package and the original debug certificate `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef`.

Current: **1.0.13.1-dev**, versionCode **29**. A52 verification is `OWNER_PENDING`.
