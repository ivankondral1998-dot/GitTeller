# Проверки 1.0.6

## Что прошло

- `sh tests-local/run.sh`: exit 0, `PASS: 32`, `PARSE OK: 17`. Это заглушка `org.json`, не Android.
- `gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest`: exit 0. APK versionCode 21.
- Прямой `org.junit.runner.JUnitCore` (не задача Gradle): OK (36 tests). В том числе `DetailFormatTest`.

## Что не прошло как Gradle-тест

- `gradle --no-daemon :app:testDebugUnitTest` на этих исходниках: **FAIL**, exit 1. Два прогона. Причина в логе: `Gradle Test Executor` / `Connection refused` и `Connection reset by peer`. XML в `test-results` остался от 31 старого теста и **не содержит** `DetailFormatTest`. Это не PASS.
- Инструментальные тесты Keystore на 1.0.6: **NOT_RUN**.
- Эмулятор Android 14, сборка 1.0.6: кадр главного экрана `evidence/emulator-home-1.0.6.png` (пустой `octocat/Hello-World`, одна кнопка, без SHA). Кадр меню `evidence/emulator-menu-1.0.6.png`: пять пунктов, без списка веток. Кадр `evidence/emulator-sections-1.0.6.png`: полноэкранный «Разделы проекта» со стрелкой назад и пятью карточками.
- Системный Back после этого кадра экран не сменил. Светлая тема, ветки, история, задачи и точность по отдельности не сняты. Чёрные кадры в доказательство не входят.
