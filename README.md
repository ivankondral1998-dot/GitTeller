# GitTeller 1.0.4-dev

Текущая сборка этого дерева: `versionName 1.0.4-dev`, `versionCode 18`, debug APK собран. Итог проверок — в `AUDIT_RESULTS.md`, `TEST_RESULTS.md`, `REMAINING.md`. Это не заявление о коммерческой готовности и не проверка Samsung A52.

Неизменённый вход: `checkpoint/GitTeller_v1.0.3_AUDIT_PATCH_SOURCE_UNCHANGED.zip`, SHA-256 `c98942799485422b1b4d3b018dbc75bddc239a5263b0dac340515fcb0f352954`.

Ниже сохранён текст про 1.0.3-audit. Он описывает тот checkpoint, не текущий APK.

# GitTeller 1.0.3-audit — исходники после независимого аудита

Продолжение Android-кода Grok 1.0.2, не новый проект. Версия 1.0.3-audit существует **только как исходники**. APK 1.0.3-audit **не собран**. APK 1.0.2 остаётся исходным артефактом Grok, его SHA указан в `AUDIT_VERIFIED.md`.

## Сборка на компьютере с Android SDK

Требования из 1.0.2: JDK 17+, Gradle 8.9, Android SDK 35, build-tools 35.0.0, AGP 8.7.3.

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

Сначала проверить тесты и затем собрать APK. Сохранить полные логи, SHA-256 и результат реальной установки на Android. В случае ошибки нельзя писать PASS.

Локальная частичная проверка без Android SDK:

```bash
sh tests-local/run.sh
```

Читайте `AUDIT_VERIFIED.md` и `TESTS_EXECUTED.md`. Полный оригинальный пакет сохранён отдельно у владельца.
