---
name: gitteller-android-ux
description: "Implement native Android UX for GitTeller. Use when adding or changing screens, navigation, search, drawer, menus, Back behavior, themes, or any UI that the owner sees on the A52."
type: workflow
lifecycle: active
---

# GitTeller Android UX — Native, Obvious, Correct

Native Android Views only (no WebView, no Compose). One data model, two display modes (`novice` / `developer`).

## Non-negotiable UX Rules

1. **Search is always visible**. Permanent search affordance (icon + accessible name) on the home screen of **both** modes. Also reachable from the drawer. Not buried only in a menu.
2. **Back works as Android expects**. System Back, drawer scrim tap, swipe, and double-tap must behave correctly. Content behind scrim is not clickable. Preserve scroll position where reasonable.
3. **Coverage is visible**. Home screen of both modes shows a compact "What is known / Data coverage" block with a clear link to the full "Why is data incomplete?" screen. Do not hide incompleteness behind "all good".
4. **"Why incomplete?" is reachable**. From home (both modes), from the drawer, and contextually next to incomplete messages or cards. One clear place that lists branches, issues, PRs, commits, checks, snapshot source with status, count/time, actual reason, and safe next action.
5. **Modes differ only in language**. Novice: plain, non-alarming ("History shown selectively. This is not an error"). Developer: exact statuses, source type, HTTP/pagination reason, coverage bounds. Never invent an HTTP code that was not stored.
6. **Update is explicit**. "Обновить" button is the only way to refresh. No silent auto-update on launch (owner decision still pending). Show last-update time.
7. **Deep links and navigation**. Support deep link to branch/issue/PR/commit. Drawer, theme switch, rotation, and project switch must not lose state incorrectly.
8. **GitHub links are actions, not only text**. On an event or issue/PR card, offer "Открыть в GitHub" and "Копировать только ссылку" beside "Скопировать технические сведения". Open only a canonical `https://github.com/{selectedRepo}/issues|pull/{n}` or `/tree/{branch}`. Reject query, fragment, user-info, port, `%` encoding, and `..` segments. Never send the browser a GitHub search URL such as `?q=author:`. A browser search-filter error is not proof of an Android Intent bug.

## Implementation Notes

- `MainActivity.showMenu()` / PopupMenu is current; `SectionActivity` for sections.
- Keep `display_mode` in SharedPreferences (`gitteller_v1`).
- Search only over **locally loaded / saved data** of the active project (sections, branches, issue/PR number+title, commit SHA/message/author, local change feed, diagnostic records). Do not claim to search "all of GitHub".
- No new system popups that fight Android Back. Prefer standard patterns.

## Checklist Before Claiming Done

- [ ] Search button visible on home both modes + drawer
- [ ] "Why incomplete?" screen reachable without hunting
- [ ] Compact coverage block on home
- [ ] Back / drawer / scrim tested (at least mentally or via available tools)
- [ ] Novice vs developer text differs correctly
- [ ] No false "all good" when sources were not requested

Owner device is Samsung Galaxy A52 (Android 14). Mark `OWNER_PENDING` or `NOT_DEVICE_VERIFIED` until the owner confirms.
