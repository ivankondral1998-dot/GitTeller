# AUDIT_RESULTS — GitTeller 1.0.4-dev

Дата среды: 2026-11-13. Это отчёт сборки, не сертификат коммерческой готовности.

## BASE

- Входной архив не менялся: `checkpoint/GitTeller_v1.0.3_AUDIT_PATCH_SOURCE_UNCHANGED.zip`
- SHA-256: `c98942799485422b1b4d3b018dbc75bddc239a5263b0dac340515fcb0f352954`
- UNO, ветка `main` и GitHub Actions не трогались.

## RESULT

- `applicationId` `app.gitteller.mobile`
- launcher `app.gitteller.mobile/app.unocontrol.MainActivity`
- `versionCode` 18, `versionName` 1.0.4-dev
- compileSdk/targetSdk 35, minSdk 26
- debug APK SHA-256: `d38e62b16ca47c0f62686e9cbdb5d56fb5a83269fcbc9ac13c78965bc5090cea`
- androidTest APK SHA-256: `0ae611b00de54d84c0c93ea2ec77865cd83de1dc2ea7f2877e1b5c2e8c9ae4ca`
- `aapt dump badging`: только `android.permission.INTERNET`, `allowBackup=false` в манифесте, debuggable, иконки нет
- В `classes.dex` основного APK нет строк `ghp_`, `github_pat_`, `synthetic-pat-not-real`
- WebView в исходниках нет (слово встречается только в комментарии и в тексте «без WebView»)

Отличия от 1.0.3-audit, `diff -rq` по дереву исходников:

- `app/src/main/java/app/unocontrol/TokenVault.java` — новый
- `app/src/test/java/app/unocontrol/TokenVaultRulesTest.java` — новый, JVM, без Keystore
- `app/src/androidTest/java/app/unocontrol/TokenVaultDeviceTest.java` — новый
- `app/src/androidTest/java/app/unocontrol/TokenVaultRestartTest.java` — новый
- `MainActivity.java` — токен больше не поле Activity; диалог доступа; отмена in-flight
- `GithubClient.java` — `cancel()`, User-Agent `GitTeller/1.0.4`, `safeMessage` режет bearer / `ghp_` / `github_pat_`
- `app/build.gradle` — version 18 / 1.0.4-dev, `AndroidJUnitRunner`, androidTest dependencies

## KEYSTORE

- `AndroidKeyStore`, alias `gitteller.pat.aes.v1`
- `AES/GCM/NoPadding`, ключ 256, `setRandomizedEncryptionRequired(true)`, tag 128 бит
- IV 12 байт берётся у `Cipher` при каждом `save`. Повторное использование IV кодом не задаётся
- AAD: байты строки `gitteller-pat-v1\n` + нормализованный `owner/repo` (нижний регистр)
- Файл: `noBackupFilesDir/token-vault-v1/<sha256(repo)>.bin`
- Формат: magic `GTV1` + IV + ciphertext/tag. Запись через `.tmp`, `fsync`, `rename`
- Пустой ввод бросает `VaultException` «Пустое поле не заменяет сохранённый доступ.» и не вызывает `Cipher`
- `delete` удаляет только файл этого репозитория
- Смена репозитория не копирует чужой токен и не переносит снимок другого проекта
- `KeyPermanentlyInvalidatedException` и `AEADBadTagException` дают короткие русские сообщения. Plaintext fallback нет
- `SharedPreferences gitteller_v1` хранит снимки, не PAT
- Экспорт `GITTELLER_V1` идёт через `SnapshotCodec.sanitizeSnapshot`. Ключей token/body/authorization в схеме нет
- Поле ввода парольное, при открытии пустое. После сохранения текст поля очищается. Java `String` из памяти этим не стирается; такая гарантия не заявляется
- Биометрия и StrongBox не включались
- Честный текст в «О приложении»: защита средствами Android, не неуязвимость

Секреты в этот отчёт не включались. Маркеры тестов — синтетические, не PAT.

## STATUS

| Проверка | Статус |
|---|---|
| Сборка debug + androidTest | PASS |
| `:app:testDebugUnitTest` последняя попытка, 28 тестов, 0 failures в XML | PASS. В том же логе есть нефатальный `Unexpected type tag 71`. Более ранний прогон (`evidence/gradle-testDebugUnitTest-EARLIER-FAIL.log`) — FAIL, Test Executor |
| Прямой `JUnitCore` | PASS, 28. Это не замена Gradle, но Gradle в этот раз тоже записал XML |
| `tests-local/run.sh` | PASS 15 model checks; PARSE OK 13. Это не Android |
| Keystore round-trip, раздельные репозитории, пустое сохранение, порча файла | PASS на эмуляторе Android 14 x86_64 |
| Перезапуск процесса: save, `force-stop`, load | PASS на том же эмуляторе |
| Внешний `run-as` grep по файлам | NOT_RUN. Команда оболочки не выполнила grep; в логе только имена каталогов. Проверка «маркера нет в файле» сделана внутри инструментального теста |
| Снимок главного экрана | NOT_RUN. `am start` принят, файл снимка 0 байт: хост убил qemu (OOM) |
| `pm clear` / потеря ключа на устройстве | NOT_RUN по той же причине |
| Поворот экрана на устройстве | NOT_RUN |
| Смена репозитория во время сети, на устройстве | NOT_RUN. В коде есть `syncEpoch` и `disconnect`, это не прогон |
| Приватный репозиторий с настоящим PAT | NOT_RUN. Токен владельца не запрашивался |
| Samsung A52 | NOT_DEVICE_VERIFIED |
| Коммерческая готовность | не заявляется |

## NEXT

Один недоказанный критерий: на живом Android (не убитом OOM эмуляторе) открыть главный экран после сохранения синтетического токена, снять кадр именно UI GitTeller, затем `pm clear` и убедиться, что процесс не падает, а потерянный ключ просит ввести токен заново.
