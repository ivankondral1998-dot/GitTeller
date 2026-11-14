# GitTeller 1.0.12-dev

Нативное Android-приложение. Не коммерческий релиз и не приёмка на устройстве.

`applicationId` — `app.gitteller.mobile`, `versionCode` — 27, `versionName` — `1.0.12-dev`. Этот APK подписан исходным debug-ключом 1.0.6–1.0.11 (`67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef`). См. [UPDATE.md](UPDATE.md).

На главном два режима: «Режим чайника» и «Режим разработчика». Оба читают один снимок. Режим чайника отвечает на четыре вопроса и объясняет правило вывода. Режим разработчика показывает покрытие списков. Это не проверка готовности.

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
sh tests-local/run.sh
```

Для сборки нужны JDK 17 или 21, Gradle 8.9 и Android SDK 35 с Build Tools 34.0.0. Укажите свой SDK в `ANDROID_HOME` или `local.properties`. Результаты проверки 1.0.11-dev находятся в отдельном архиве проверок.
