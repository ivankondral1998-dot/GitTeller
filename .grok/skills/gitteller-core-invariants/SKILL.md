---
name: gitteller-core-invariants
description: "Enforce GitTeller project invariants. Use at the start of any GitTeller coding task, when reviewing architecture, security, token handling, or before declaring a version done."
type: knowledge
lifecycle: active
---

# GitTeller Core Invariants

You are the engineering AI. The owner is the architect. Follow the rules; do not invent new product direction.

## Identity & Compatibility

- Package: `app.gitteller.mobile`
- Overlay install must preserve: token (Android Keystore + `token-vault-v1`), SharedPreferences (`gitteller_v1`, `display_mode`), local snapshots, repositories, import/export.
- Never `pm clear`, never delete owner data, never change signature for an update.
- GitHub is **READ ONLY**. HTTPS GET only on allowed endpoints. No writes to foreign repos. No Actions triggers. No exotic analytics or ad SDKs.

## Security

- Token stays in Android Keystore + AES-GCM, bound to repo.
- Use atomic replacement (`AtomicFile` or equivalent). Write failure must leave the previous readable token.
- Temporary files only ciphertext in `noBackupFilesDir`.
- `hasToken` ≠ successful decrypt.
- Never put real PAT in tests or source.
- Snapshot export: whitelist only. No tokens, Authorization, patches, unknown fields.
- Imported file is never "fresh confirmed GitHub".

## Architecture

- Native Android Views (Java). No WebView, no Compose.
- One data model, two modes: `novice` / `developer`.
- Key classes: `GithubClient`, `SnapshotCodec`, `HumanReport`, `TokenVault`, `MainActivity`, `SectionActivity`.
- Rate-limit aware: limited requests on home sync, ETag where useful, Retry-After, user retry button, cancel. 403 is not always bad token. Checks may be unavailable for fine-grained PAT — show Commit Statuses/Actions instead.
- Do not auto-update on launch until owner decides.

## Completeness & Honesty

- Partial data is partial. Say so.
- Do not claim DEVICE_VERIFIED or OWNER_VERIFIED without actual owner confirmation on A52.
- Prefer working product + clear evidence over infinite audit or rewrite from scratch.
- Base work on the latest provided source zip (do not restart from older checkpoints).

## When Starting a Task

1. Confirm current version, SHA of source, and exact next versionCode/Name.
2. Read latest `00_READ_ME_GROK.md` / TASK file if present.
3. Preserve rollback point.
4. Apply the specialized skills (`gitteller-event-model`, `gitteller-android-ux`, `gitteller-build-verify`) as the work requires.

These invariants override convenience. If a rule blocks progress, report the blocker precisely.
