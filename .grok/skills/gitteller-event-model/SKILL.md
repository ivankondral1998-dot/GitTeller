---
name: gitteller-event-model
description: "Model GitHub events and reports correctly for GitTeller. Use when implementing or reviewing issue/PR/branch/commit change detection, HumanReport, SnapshotCodec, or any logic that turns GitHub data into user-facing facts."
type: workflow
lifecycle: active
---

# GitTeller Event Model — Accurate GitHub Facts

Never invent conclusions. Every user-facing statement must be tied to a real field from a real GitHub response (or explicitly marked unknown/partial).

## Core Rules (non-negotiable)

1. **No `closed == merged`**. Closing and merging are separate facts. `merged_at` / `merged` is an independent event. Confirm merge via the PR detail endpoint when needed.
2. **No heuristics from names**. Branch name `fix/...`, commit message `[skip ci]`, `fix:`, author, or "progress" language never prove a fix, successful test, or intent.
3. **Completeness is explicit**. Missing item from a partial page ≠ deleted. Track per-source completeness flags (`LIVE`, `CACHED`, `IMPORTED`, `PARTIAL`, `NOT_REQUESTED`). Absence from incomplete snapshot ≠ "no change".
4. **Typed events**. Prefer explicit event types over "state changed":
   - new open issue/PR
   - closed / reopened
   - title or description updated (`updated_at` only → "record updated" if body not loaded)
   - merged (separate)
   - branch SHA changed (then classify via compare)
5. **Branch history**. SHA change alone is not "progress". For changed priority branches call `GET /repos/{owner}/{repo}/compare/{oldSHA}...{newSHA}` and report:
   - ahead + new commits → "new commits appeared"
   - behind → "history rolled back"
   - diverged → "history changed, not ordinary advance"
   - identical / insufficient data → "unknown"
   New branch not present in previous full snapshot → "first seen between snapshots", not "just created".

## Evidence Linking

Every explanation stores:
- event type
- stable ID (`owner/repo#number` or full SHA)
- source timestamp
- GitHub URL (only `https://github.com/{owner}/{repo}/...`)
- concrete files/checks when available
- completeness + origin flag

If data is missing: say "not checked" / "unknown", never guess.

## Snapshot & Export

In `SnapshotCodec` whitelist only needed fields (`merged`, `merged_at`, `updated_at`, completeness flags). Never export tokens, Authorization, patches, or unknown fields. Imported snapshot is never treated as fresh confirmed GitHub data.

## Implementation Checklist

- [ ] Event types or equivalent discrimination present
- [ ] `merged_at` handled separately from closed
- [ ] Compare used for meaningful branch statements
- [ ] Completeness flags propagated to UI
- [ ] Tests cover: reopen, description update, partial pagination, missing baseline, the original 4 RED cases
- [ ] HumanReport / ChangeSet / UiMode never claim more than the evidence supports

See project task docs (latest `00_READ_ME_GROK.md` and versioned TASK files) for full acceptance criteria.
