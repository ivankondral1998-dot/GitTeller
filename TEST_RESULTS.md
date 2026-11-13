# TEST_RESULTS.md

## Сборка
- Команда: `gradle --no-daemon :app:assembleDebug`
- ANDROID_HOME=/tmp/android-sdk (platforms;android-35, build-tools;35.0.0)
- Exit code: 0
- Результат: BUILD SUCCESSFUL in 14s
- APK: app/build/outputs/apk/debug/app-debug.apk
- SHA-256: 0d3175e3b77bbb3a32a4eab64b60738919705a685a26e2f35bc84cd99a769aed
- Размер: ~28 KB

## Unit-тесты
- Команда: `gradle --no-daemon :app:testDebugUnitTest`
- Exit code: non-zero (BUILD FAILED)
- Результат: 4 tests completed, 4 failed
- Причина: NullPointerException в SummaryTest.snapshot() — org.json.JSONObject.put() в чистом JVM возвращает не this (в отличие от Android runtime). Тесты не адаптированы под unit-test classpath.
- Полный лог: см. gradle_test.log в пакете.

## Другие проверки
- В APK и исходниках нет тестовых PAT или секретов (токен только в RAM, не сохраняется).
- AndroidManifest: только INTERNET, нет WebView, нет analytics, allowBackup=false, usesCleartextTraffic=false.
- Сеть: только GET к api.github.com по HTTPS.
- Устройство: эмулятор и физический Samsung A52 недоступны в этой среде. **NOT_DEVICE_VERIFIED**.

Известные ограничения: unit-тесты не проходят в текущей конфигурации; интерфейс не полностью приведён к финальному P0-описанию; нет скриншотов с устройства.
