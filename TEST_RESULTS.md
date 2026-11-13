# TEST_RESULTS — GitTeller 1.0.2 — 2026-11-13

Среда: JDK 17.0.20, Gradle 8.9, Android SDK platform 35, build-tools 35.0.0.
ANDROID_HOME=/tmp/android-sdk. Физический Samsung A52 недоступен.

## Сборка

Команда: `gradle --no-daemon :app:assembleDebug`
Exit code: 0
Результат: BUILD SUCCESSFUL in 11s
APK: `app/build/outputs/apk/debug/app-debug.apk`
SHA-256: `361383a97c28833beefa46de9970a58e6433ade95cabb99fd17815f456c0706b`
Размер: 35349 байт

`aapt dump badging` (build-tools 35.0.0), не README:

- package name `app.gitteller.mobile`
- versionCode 16
- versionName 1.0.2
- compileSdk 35, minSdk 26, targetSdk 35
- label GitTeller
- uses-permission: только `android.permission.INTERNET`
- debuggable: да (это debug APK)
- icon: пусто
- в classes.dex строка WebView не найдена
- строки `ghp_`, `github_pat_`, `UNO-android-AI-Agent` в dex не найдены

## Gradle unit-test task

Команда: `gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug`
Exit code: 1
Задача `:app:testDebugUnitTest` FAILED.
Причина: процесс `Gradle Test Executor 1` не смог подключиться к серверу Gradle
(`Unexpected type tag 71` на loopback, затем `Connection refused`).
Это сбой исполнителя тестов в этой среде, не падение assert.
Полный лог: `evidence/gt_gradle.log`.
Повторно именно эту Gradle-задачу после удаления эксперимента с classpath не гонял как «зелёную»:
считать `:app:testDebugUnitTest` пройденной нельзя.

## Прямой запуск тех же class-файлов

Классы взяты из `compileDebugJavaWithJavac` и `compileDebugUnitTestJavaWithJavac` той же сборки.
Команда: `java -cp <test>:<main>:junit-4.13.2:hamcrest-core-1.3:json-20240303 org.junit.runner.JUnitCore ...`
Exit code: 0
Результат: `OK (22 tests)`
Лог: `evidence/junit_direct.log`

Это не замена отчёта Gradle Test Executor. Это доказательство, что скомпилированные тесты
проходят на JDK 17 с настоящей библиотекой org.json.

Покрыто тестами:

- первый снимок, сдвиг/появление/исчезновение веток
- неполный список не объявляет исчезновение
- одинаковые SHA не объявляются прогрессом
- неполное сравнение не говорит «изменений нет»
- пустой снимок не доказывает удаление
- смена repo не считается diff
- смена статуса issue видна и не называется PASS
- предупреждения ограничивают вывод
- пустые issues не выдумывают работу
- CommitExplainer: fix/docs/feat/test/пустое — не проверка
- HTTP 401, 403, 403+remaining 0, 429, 404, 500
- JSON-архив выкидывает token/body, отвергает плохой SHA, чужой repo и отсутствие веток

## Сеть (JVM, не телефон)

`GithubClient.load` без токена:

- `octocat/Hello-World`: repo совпал, веток 3 (complete=true), коммитов 3, issues 51, pulls 100, warnings 2, токена в JSON нет. Объяснение первого коммита содержит «не проверена».
- `octocat/Spoon-Knife`: repo совпал, веток 3, коммитов 3, issues 6, pulls 100, warnings 2.
- `octocat/definitely-missing-gitteller-xyz`: текст 404 «Старые данные сохранены».
- следующий живой вызов сразу после этого: `Лимит запросов GitHub исчерпан (429/403). Повторите позже. Старые данные сохранены.` Реальный 429/403, не подмена.

Приватный репозиторий не проверялся: тестовый PAT в эту среду не передавался и в чат его класть нельзя.

## Устройство

Физический Samsung A52 недоступен. **NOT_DEVICE_VERIFIED.**

Эмулятор Android 14 (system-image `google_apis` x86_64, API 34) запускался без окна, экран 1080x2400, dpi 420. Факты:

- `ro.build.version.release=14`, `ro.build.version.sdk=34`.
- `sys.boot_completed` в опросе 200 секунд оставался пустым. В логе эмулятора позже строка `Boot completed in 250078 ms`.
- `adb install -r -t app-debug.apk` → `Success`. Пакет `app.gitteller.mobile` виден в `pm list packages`.
- `dumpsys package` показывает launcher `app.gitteller.mobile/app.unocontrol.MainActivity`.
- `am start -n app.gitteller.mobile/app.unocontrol.MainActivity` вернулся без ошибки «does not exist».
- Скриншот `evidence/emulator-anr-dialog.png` снят `adb exec-out screencap` с этого процесса. На нём системный диалог «GitTeller isn't responding», не главный экран приложения. Это не макет и не доказательство вёрстки.
- В `/data/anr` следов GitTeller нет: ANR-файлы относятся к телефонии, GMS, SystemUI. Гостевая система была перегружена (предупреждение QEMU `hanging thread CPU0`, загрузка ~250 с).
- Процесс эмулятора после `adb root` умер. Повторный прогон UI не делался.
- Синхронизация GitHub, смена проекта, 401 и импорт **из APK на Android не выполнялись**.
