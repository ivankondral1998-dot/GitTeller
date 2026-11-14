# Проверки 1.0.7-dev

Каталог: `/workspace/gitteller`. Подпись APK совпадает с debug-сертификатом 1.0.6 (`SHA-256` сертификата `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef`). `applicationId` `app.gitteller.mobile`, `versionCode` 22, `versionName` 1.0.7-dev.

## Сборка

```
ANDROID_HOME=/tmp/android-sdk gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`  
Копия: `evidence/apk/GitTeller-1.0.7-dev-debug.apk`  
SHA-256: `a2a9778ff5af812e8783554d9c4ae63ce9ca3aec581abf575fde97faa1e004d8`

## Gradle unit tests

Первый прогон вместе со сборкой:

```
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

**FAIL, exit 1.** Причина в логе `evidence/logs/gradle-test-1.0.7-FIRST-FAIL.log`: `Gradle Test Executor` / `Unexpected type tag 71` и `Connection refused`. Это сбой канала Gradle, не утверждение о тестах. XML того прогона не обновлялся.

Повтор только тестов:

```
gradle --no-daemon :app:testDebugUnitTest
```

**PASS, exit 0.** Лог: `evidence/logs/gradle-test-1.0.7-PASS.log`. XML в `app/build/test-results/testDebugUnitTest`: **67 тестов, 0 failures, 0 errors** (Summary 14, GithubStatus 12, Truth 23, DetailFormat 5, Snapshot 4, HumanReport 3, CommitExplainer 3, TokenVaultRules 3).

Прямой `org.junit.runner.JUnitCore` на тех же class-файлах: **OK (66 tests)** до добавления теста `continuationStaysInsideTheSameRepository`, затем Gradle-прогон уже содержит 67. JUnitCore не подменяет Gradle. Оба способа после исправления пагинации согласованы с успешным Gradle-прогоном.

## Локальные двойники

```
sh tests-local/run.sh
```

**exit 0.** `PASS: 32`, `PASS: 8` GithubAuditSmoke, `PARSE OK: 18`. Это не Android и не сеть.

## Сеть, публичный репозиторий, без токена

`GithubClient.load("octocat/Hello-World")` и `readCommit` первого коммита. Лог: `evidence/logs/api-smoke-hello-world.log`. **exit 0, `API_SMOKE_OK`.**

- Веток 3, `branchesComplete=true`.
- История помечена частичной.
- Задачи и предложения: `предел 2 страниц`, полнота `false`. Не сказано «ноль задач».
- В тексте коммита есть имя файла `README` и фраза, что список проверок GitHub не заменяет проверку на телефоне.
- Ссылка продолжения вида `/repositories/{id}/` принимается только для того же репозитория. Чужой хост и чужой id отклоняются (тест `continuationStaysInsideTheSameRepository`).

Приватный репозиторий: **NOT_RUN**. Токен владельца не использовался.

## Эмулятор Android 14

AVD `gt14`, образ `android-34`, экран 1080x2400. Установка debug APK прошла. Живой запрос к `octocat/Hello-World` без токена обновил главный экран.

Снято:

- `evidence/ui-107/home-try2.png` — пустой проект, кнопка «Обновить», без SHA.
- `evidence/ui-107/home-synced.png` — первая проверка, дата, покрытие, две открытые задачи, честная неполнота списка.
- `evidence/ui-107/branches.png` — направления, предположение по названию.
- `evidence/ui-107/sections.png` — отдельные разделы.
- `evidence/ui-107/menu.png` — меню.

Системный Back с экрана направлений вернул на главный: текст главного экрана снова в дампе UI. Остальные экраны (задачи, предложения, история, надёжность, настройки, тёмная тема, подробности коммита) в этом прогоне не сняты: эмулятор оборвал adb (`Broken pipe`, затем `no devices`). Кадры рабочего стола и пустые файлы в доказательство не входят.

**NOT_DEVICE_VERIFIED.** Samsung A52 не подключался. Это не приёмка на телефоне владельца.

## Чего эти проверки не доказывают

Keystore на устройстве, смена проекта во время запроса, 401 на живом токене, инструментальные тесты `androidTest`: **NOT_RUN** в этом прогоне.
