# GitTeller 1.0.5

Нативное Android-приложение. Объясняет снимок GitHub по-русски и не выдаёт коммит за готовую функцию. Это не коммерческий релиз.

Сборка: JDK 17, Gradle 8.9, SDK 35.

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

Текущие результаты: `AUDIT_RESULTS.md`, `TEST_RESULTS.md`, `REMAINING.md`.
Старые `AUDIT_VERIFIED.md` и `TESTS_EXECUTED.md`, если остались в дереве, относятся к предыдущим версиям.
