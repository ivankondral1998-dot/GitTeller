# GitTeller 1.0.2

Нативное Android-приложение (Java, без WebView). Объясняет публичный или приватный GitHub простым языком и не пишет в репозиторий.

Это debug-сборка, не продукт для продажи. Ограничения: `REMAINING.md`. Проверки: `TEST_RESULTS.md`.

## Сборка

Нужны JDK 17, Android SDK platform 35, build-tools 35.0.0, Gradle 8.9, Android Gradle Plugin 8.7.3.

```
export ANDROID_HOME=/path/to/sdk
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Если Gradle Test Executor в среде не может открыть loopback, это надо писать как сбой задачи, а не как зелёные тесты. Классы можно прогнать отдельно через `org.junit.runner.JUnitCore` — это другой факт, его так и подписывать.

## Поведение

- Публичный репозиторий без токена. Приватный — read-only PAT только в памяти процесса.
- Главный экран: что изменилось, что требует внимания, кнопка обновления, «Подробнее».
- Ветки, SHA, задачи и запросы — в меню.
- Неполный список не считается удалением. Коммит не считается готовностью.
- Экспорт JSON без токена. Импорт проверяет схему и помечает данные как восстановленные из файла.
