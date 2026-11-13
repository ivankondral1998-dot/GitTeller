# TEST_RESULTS.md — GitTeller v1.1

## Сборка
- Команда: gradle --no-daemon :app:assembleDebug
- ANDROID_HOME=/tmp/android-sdk (platforms;android-35, build-tools;35.0.0)
- Exit code: 0
- Результат: BUILD SUCCESSFUL
- APK SHA-256: (см. ниже после копирования)
- Размер: ~28 KB

## Unit-тесты
- Команда: gradle --no-daemon :app:testDebugUnitTest
- Результат: FAILED (process exit non-zero)
- Причина: проблемы среды (Gradle daemon socket / Unexpected type tag) + возможные различия org.json в unit-test classpath.
- Полный лог: gradle_v1.1_test.log

## Прочее
- В APK нет секретов и тестовых PAT.
- Manifest: только INTERNET, allowBackup=false, usesCleartextTraffic=false, нет WebView.
- Сеть: только GET к api.github.com.
- **NOT_DEVICE_VERIFIED**. Реальные сценарии с GitHub (публичные/приватные, ошибки 401/403/429, переключение проектов) в этой среде не запускались.
