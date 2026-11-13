# TEST_RESULTS — 1.0.4-dev

Среда: Linux sandbox, JDK 17, Gradle 8.9, AGP 8.7.3, `ANDROID_HOME=/tmp/android-sdk`, platform android-35, build-tools 35.0.0. Эмулятор: AVD `gt14`, образ Android 14, `x86_64`, 1080x2400 dpi 420, `-no-window -gpu swiftshader_indirect -accel on -memory 1024`. Хост: MemTotal около 4 ГБ, swap нет. `/dev/kvm` есть.

Маркеры инструментальных тестов: `synthetic-pat-not-real-001` и `synthetic-pat-not-real-002`. Это не токены GitHub.

## Gradle

Команда, последний прогон модульных тестов:

```text
gradle --no-daemon :app:testDebugUnitTest --console=plain
```

- exit code 0
- лог: `evidence/gradle-testDebugUnitTest-1.0.4.log`
- XML: `app/build/test-results/testDebugUnitTest/`
- SummaryTest 12, GithubStatusTest 6, SnapshotCodecTest 4, CommitExplainerTest 3, TokenVaultRulesTest 3
- сумма 28, failures 0, errors 0, skipped 0
- В логе до успешного task есть `Unexpected type tag 71` на сокете демона. На этот конкретный прогон task всё же записал XML и Gradle завершился 0
- Предыдущий прогон той же задачи падал: `evidence/gradle-testDebugUnitTest-EARLIER-FAIL.log`, `BUILD FAILED`, Test Executor `Connection refused`. Тот FAIL не отменяется задним числом; актуальный результат — XML от 2026-11-13 21:27:51 UTC

Сборка:

```text
gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```

- exit code 0, лог `evidence/gradle-assemble-1.0.4.log`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256 `d38e62b16ca47c0f62686e9cbdb5d56fb5a83269fcbc9ac13c78965bc5090cea`

## JUnitCore

Отдельный процесс, не Gradle Test Executor:

```text
java -cp <unit-test-classes>:<app-classes>:junit-4.13.2:hamcrest-core-1.3:json-20240303:android.jar org.junit.runner.JUnitCore ...
```

- exit code 0, `OK (28 tests)`, лог `evidence/junitcore-1.0.4.log`
- `TokenVaultRulesTest` проверяет нормализацию и отказ от `..`. Шифрование он не доказывает: на JVM нет AndroidKeyStore

## tests-local

```text
sh tests-local/run.sh
```

- exit code 0
- `PASS: 15 model checks` на заглушках `org.json`
- `PARSE OK: 13` — только синтаксис
- лог `evidence/tests-local-1.0.4.log`

## Android 14 emulator

Установка обоих APK: `Success`.

```text
adb shell am instrument -w -e class app.unocontrol.TokenVaultDeviceTest app.gitteller.mobile.test/androidx.test.runner.AndroidJUnitRunner
```

- `OK (4 tests)`, Time 6.525, exit 0
- тесты: round-trip без plaintext в `noBackup` и `shared_prefs`; два репозитория и локальное удаление; пустой save не стирает; порча последнего байта даёт сообщение с «поврежд» и без маркера

Перезапуск, два отдельных запуска instrumentation:

```text
am instrument ... TokenVaultRestartTest#saveForRestart
am force-stop app.gitteller.mobile
# pidof пустой, в логе process_gone
am instrument ... TokenVaultRestartTest#loadAfterRestart
```

- save: `OK (1 test)`, Time 0.263
- load после остановки процесса: `OK (1 test)`, Time 0.132
- Расшифровка после `force-stop` совпала с маркером. Затем тест удалил запись

Полный текст сессии: `evidence/emulator-android14-instrument.txt`. `getprop ro.build.version.release` = 14.

Внешний просмотр файла через `run-as` в этой сессии не состоялся: в логе `=== SCAN ===` напечатаны имена каталогов data dir, без `PLAINTEXT_ABSENT` / `PLAINTEXT_HIT`. Причина — сломанное экранирование `adb shell`. Статус внешнего grep: NOT_RUN. Утверждение «в файле нет маркера» опирается на assert внутри `saveForRestart` и `loadAfterRestart`, не на `adb`.

UI:

```text
adb shell am start -n app.gitteller.mobile/app.unocontrol.MainActivity
```

- Activity Manager ответил `Starting: Intent { cmp=app.gitteller.mobile/app.unocontrol.MainActivity }`
- `screencap` записал 0 байт, дальше `adb: device offline`
- `dmesg`: `Out of memory: Killed process ... (qemu-system-x86)`, anon-rss около 2.8–3.8 ГБ. Фрагмент: `evidence/host-oom-kills.txt`
- Кадра главного экрана нет. Это не PASS и не доказанный ANR приложения
- `pm clear` не выполнялся

Повторный подъём эмулятора ради снимка не делался: тот же OOM уже убил qemu трижды, хост 4 ГБ.

## Не запускалось

- Samsung A52: NOT_DEVICE_VERIFIED
- Настоящий PAT и приватный репозиторий: NOT_RUN
- Поворот, `pm clear`, визуальная проверка «токен не показан на главном экране»: NOT_RUN
