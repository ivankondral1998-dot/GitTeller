# Проверки 1.0.9-dev

Каталог: `/workspace/gitteller`. Подпись APK совпадает с debug-сертификатом 1.0.6, 1.0.7 и 1.0.8 (`SHA-256` `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef`). `applicationId` `app.gitteller.mobile`, `versionCode` 24, `versionName` 1.0.9-dev. `minSdk` 26, `targetSdk` 35. Только `INTERNET`. `allowBackup=false`. `usesCleartextTraffic=false`. Сборка debuggable.

База — патч независимого аудита 1.0.8.1, проверенный заново, не принятый на слово. Оригинальный APK 1.0.8 патча не содержит. Его SHA-256 `482c8777c8ceaa44090be627e46f600ce832f510a40db36ed64212f1b98236f7`.

## Дефекты 1.0.8 до патча

Команда воспроизведения на исходниках 1.0.8 (заглушка `org.json`, не Android): **exit 2**.

- знакомое изменение задачи при неполном списке: `issueChanges=0`, событий 0
- ETag без признака одной страницы принят: `"page1"`
- после `rename` в `.bak` файл назначения уже не читается, шифртекст лежит только в `.bak`

Лог: `evidence/logs/repro-1.0.8-RED-before-109.txt`.

## Сборка

```
ANDROID_HOME=/tmp/android-sdk /opt/gradle-8.9/bin/gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

**PASS, exit 0**, один прогон, `BUILD SUCCESSFUL`. В логе есть отброшенное постороннее соединение `Unexpected type tag 71`, сборка из-за него не упала. Лог: `evidence/logs/gradle-1.0.9-try1.log`.

APK: `app/build/outputs/apk/debug/app-debug.apk`  
Копии: `evidence/apk/GitTeller-1.0.9-dev-debug.apk`, `/workspace/artifacts/GitTeller-1.0.9-dev.apk`  
SHA-256 APK: `0e5e8579927a53ca69ed8b8d497ca9188cd8c7213983e8a04b6b872a134675a8`

`apksigner verify --print-certs` подтвердил сертификат выше. Установка поверх на устройство: **NOT_RUN**.

## Gradle unit tests

Тот же прогон. XML: `evidence/unit-xml-1.0.9/` и `app/build/test-results/testDebugUnitTest`.

**83 теста, 0 failures, 0 errors.**

Summary 15, Truth 23, EventModel 15, GithubStatus 12, DetailFormat 5, Snapshot 4, HumanReport 3, CommitExplainer 3, TokenVaultRules 3.

Новые проверки этого прогона: знакомое изменение в неполном списке, ETag при `rel=next` не сохраняется, старый ETag без `*EtagSinglePage` не доверяется, повтор события не исчезает из ленты, `.bak` без файла назначения восстанавливается.

Это не Keystore и не телефон.

## Локальные двойники

```
sh tests-local/run.sh
```

**exit 0.** `46 model checks`, `8 GithubAuditSmoke`, `14 independent audit regressions`, `PARSE OK 22`. Лог: `evidence/logs/tests-local-1.0.9.log`.

## UI и сеть

Эмулятор и A52: **NOT_RUN**. Публичный compare для этой версии заново не гонялся. Форма ответа `ahead` / `ahead_by` была снята на 1.0.8 без токена и к формулировкам сравнения не привязана как прогон APK.
