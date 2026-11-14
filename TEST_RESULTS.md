# Проверки 1.0.8-dev

Каталог: `/workspace/gitteller`. Подпись APK совпадает с debug-сертификатом 1.0.7 и 1.0.6 (`SHA-256` сертификата `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef`). `applicationId` `app.gitteller.mobile`, `versionCode` 23, `versionName` 1.0.8-dev. `minSdk` 26, `targetSdk` 35. Только `INTERNET`. `allowBackup=false`. `usesCleartextTraffic=false`. Сборка debuggable.

APK SHA-256: `482c8777c8ceaa44090be627e46f600ce832f510a40db36ed64212f1b98236f7`  
Копия: `evidence/apk/GitTeller-1.0.8-dev-debug.apk` и `/workspace/artifacts/GitTeller-1.0.8-dev.apk`.  
Прежний 1.0.7 не перезаписан: `a2a9778ff5af812e8783554d9c4ae63ce9ca3aec581abf575fde97faa1e004d8`.

Это host-проверки. Они не равны приёмке на A52.

## Сборка и Gradle

Команда:

```
cd /workspace/gitteller
ANDROID_HOME=/tmp/android-sdk /opt/gradle-8.9/bin/gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

Первый прогон: **FAIL, exit 1**. Лог `evidence/logs/gradle-108-assemble-and-test-FIRST-FAIL.log`. Причина: `Unexpected type tag 71` и `Connection refused` у `Gradle Test Executor`. Сборка при этом дошла до `packageDebug`. Это не результат тестов.

Отдельный повтор `:app:testDebugUnitTest`: **FAIL, exit 1**, тот же обрыв сокета. Лог `evidence/logs/gradle-108-test-SECOND-FAIL.log`.

Дальше пять повторов того же теста. Первые два снова **FAIL** (`gradle-108-try1.log`, `gradle-108-try2.log`). Третий: **PASS, exit 0**, `BUILD SUCCESSFUL in 10s`. Лог `evidence/logs/gradle-108-test-PASS.log`.

XML этого успешного прогона скопирован в `evidence/unit-xml-1.0.8/`.

**78 tests, 0 failures, 0 errors, 0 skipped.**

| Класс | Тесты |
|---|---|
| TruthTest | 23 |
| SummaryTest | 14 |
| GithubStatusTest | 12 |
| EventModelTest | 11 |
| DetailFormatTest | 5 |
| SnapshotCodecTest | 4 |
| CommitExplainerTest | 3 |
| HumanReportTest | 3 |
| TokenVaultRulesTest | 3 |

`EventModelTest` покрывает четыре бывших RED-сценария, повторное открытие, `updated_at`, неполный список, первый снимок, тексты compare и отказ установки файла токена. Прямой JUnitCore в этот раз не запускался и не выдаётся за Gradle.

## Локальные двойники

```
sh tests-local/run.sh
```

**PASS, exit 0.** `PASS: 46 model checks`, `PASS: 8 GithubAuditSmoke checks`, `PARSE OK: 20`. Лог: `evidence/logs/tests-local-1.0.8.log`. Это `org.json`-двойник, не Android и не сеть.

## Исходный RED-зонд 1.0.8, без правок зонда

```
javac ... Summary.java ChangeSet.java SnapshotCodec.java GitTellerRedProbe.java
java app.unocontrol.GitTellerRedProbe
```

**exit 0.** Четыре строки `PASS`: новая задача, слияние при `closed → closed`, смена названия, `merged` после sanitize. Лог: `evidence/logs/red-probe-1.0.8.log`. На 1.0.7 тот же зонд давал exit 2 и 4 RED. Это не прогон Gradle и не телефон.

## Эмулятор и устройство

Эмулятор Android 14 (`gt14`, 1080×2400) дошёл до `sys.boot_completed=1`, затем оборвал графику: `Failed to find ColorBuffer` / `Failed to bind to texture`. `pm`/`am` ответили `Can't find service`, устройство ушло в `offline`. Чёрный кадр 15 КБ удалён и не считается экраном.

Экраны, системный Back, светлая и тёмная темы, крупный шрифт, живая синхронизация в приложении: **NOT_RUN**.

С телефона хост сделал только GET без токена к публичному `octocat/Hello-World` (не из APK): репозиторий открыт, три ветки, compare `master...octocat-patch-1` вернул `status=ahead`, `ahead_by=1`, `behind_by=0`, 1 коммит, 1 файл. Это проверка формы ответа GitHub, не прогон приложения. Лог: `evidence/logs/api-smoke-1.0.8.log`.

A52: **NOT_DEVICE_VERIFIED**. Приватный репозиторий: **NOT_RUN**.

