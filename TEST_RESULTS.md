# Проверки 1.0.10-dev

Каталог: `/workspace/gitteller`. Подпись APK совпадает с debug-сертификатом 1.0.6–1.0.9 (`SHA-256` `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef`). `applicationId` `app.gitteller.mobile`, `versionCode` 25, `versionName` 1.0.10-dev. `minSdk` 26, `targetSdk` 35. Только `INTERNET`. `allowBackup=false`. `usesCleartextTraffic=false`. Сборка debuggable.

База 1.0.9 не переписывалась. APK `0e5e8579927a53ca69ed8b8d497ca9188cd8c7213983e8a04b6b872a134675a8`. Архив исходников `d7ff692a6a67f24cec1f0da3a96b932d4b4a4e9bc1cb77bfdc8b249f9132d3f4`.

## Видео A52 1.0.9

Ролик `A52_screen_recording_1.0.9_compressed.mp4`, 3:38, просмотрен целиком по кадрам каждые 2 секунды (00:00–03:36) и по полным кадрам дома, изменений, направления и повторного дома. Это запись 1.0.9, не этой сборки.

| Нажатие | Ожидал узнать | Увидел на 1.0.9 | Почему непонятно |
|---|---|---|---|
| Дом `ivvan2006/UNO` | Что реально изменилось | Два абзаца про сдвиг `fix/gitteller-human-checks` и `fix/android-summary-wording` и длинную оговорку | Нет файлов, сравнения, автора и отдельного вывода |
| Разделы → Изменения | Конкретное событие | «Не подтверждено: Не подтверждено», задача №6 только как обновление записи | Повтор шаблона, `updated_at` выглядит как правка без содержания |
| Направление `fix/...` | Идёт ли работа и кто | «Предположение по названию», сравнение «ещё не получено», история с `fix:` | Название ветки читается как доказательство ремонта |
| `research/uno-train` | Над чем работа | Указатель не сдвинулся, в истории коммиты про документы | Нет связи «направление → факт → человек» |
| Меню → подключение, смена на `gitteller` | Какой проект открыт | Поле токена пустое, тост что пустое поле не стирает доступ, затем первая проверка без базы | Активный репозиторий на доме не подписан как выбранный проект |
| Дом после синхронизации `gitteller` | Есть ли проблемы | «Базы нет», проверки не получены, отсутствие не успех | Верно по осторожности, но нет разделения «ошибка / задача / неизвестно» |
| Направление `main` | Что сделано | Нечего сравнивать, Initial commit / README | Общая фраза «в истории появилась запись» |
| Настройки, список проектов, назад на UNO | Вернуться и понять UNO | Снова те же указатели и та же лента с двойным «Не подтверждено» | Логика экранов не отвечает на жалобу |

## Аудит 1.0.9.1 до патча

Сохранённый stdout зонда на неисправленной модели 1.0.9: `evidence/logs/repro-1.0.9-RED-before-1091.txt`. В нём PASS=4 FAIL=4. Зонд при любом FAIL вызывает `System.exit(2)`. Четыре красных пункта: известный сдвиг ветки не попал в ленту при неполном списке; новое имя спрятано с главной при неполном текущем списке; любой сдвиг SHA назван новыми записями; имя, которого нет в двух полных списках, не дало события. Патч `03_SOURCE_DIFF.patch` применён после этого, не вместо текущих исходников. Сам zip патча Android-сборкой не является.

## Сборка

```
ANDROID_HOME=/tmp/android-sdk /opt/gradle-8.9/bin/gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

- Прогон 1: **exit 1**. Test Executor оборвался (`Unexpected type tag 71`, затем `Connection refused`). Лог: `evidence/logs/gradle-1.0.10-try1.log`. Это не результат тестов продукта.
- Прогон 2: **exit 0**, `BUILD SUCCESSFUL`. В логе снова есть отброшенное `Unexpected type tag 71`, сборка из-за него не упала. Лог: `evidence/logs/gradle-1.0.10-try2.log`.

APK: `app/build/outputs/apk/debug/app-debug.apk`  
Копии: `evidence/apk/GitTeller-1.0.10-dev-debug.apk`, `/workspace/artifacts/GitTeller-1.0.10-dev.apk`  
SHA-256 APK: `fabb14dbacff9a3f9162ea9e198c100ef4bd10365e0cc91c3b30b583b5fd51ec`

`apksigner verify --print-certs` подтвердил сертификат выше. Установка поверх на устройство: **NOT_RUN**.

## Gradle unit tests

Тот же успешный прогон. XML: `evidence/unit-xml-1.0.10/`.

**88 тестов, 0 failures, 0 errors, 0 skipped.**

Summary 16, Truth 23, EventModel 18, GithubStatus 12, DetailFormat 5, Snapshot 4, HumanReport 4, CommitExplainer 3, TokenVaultRules 3.

Новые или усиленные проверки: четыре случая 1.0.9.1; `updated_at` содержит «неизвестно» и не содержит «исправлен»; `unknownOnce` не удваивает «Не подтверждено»; ответ владельца на сдвиг ветки без SHA и с фразой, что файлы не запрашивались; обычная задача не дефект, «Crash»/«bug» — только по названию; автор `octocat` не превращается в Grok/Codex и не называется работающим сейчас; первая проверка по-прежнему «Базы»/«первая» и «нет достоверного сравнения».

Это не Keystore и не телефон.

## Локальные двойники

```
sh tests-local/run.sh
```

**exit 0.** `46 model checks`, `8 GithubAuditSmoke`, `14 independent audit regressions`, `IndependentProbe109` CHECKS PASS=8 FAIL=0, `PARSE OK 22`. Лог: `evidence/logs/tests-local-1.0.10.log`.

## Матрица: видео → код → тест → A52

| Проблема | Что сделано | Код | Тест | На A52 ещё проверить |
|---|---|---|---|---|
| Дом про указатели без вывода | Факт сдвига и «файлы ещё не запрашивались» | `ChangeSet` `branch_moved`, `HumanReport.ownerChanged` | `HumanReportTest.ownerAnswersStayTiedToEvidence` | Карточка «Что изменилось» на UNO |
| «Не подтверждено» дважды | Префикс снят, экран пишет «Что не проверено» один раз | `ChangeSet.event`, `HumanReport.unknownOnce`, `SectionActivity.changes` | `unknownOnce` в том же тесте | Лента изменений |
| Задача №6 по `updated_at` | «содержание неизвестно», не «исправлено» | `ChangeSet` `issue_touch` | `EventModelTest.updatedAtWithoutBodyIsAnEditNotAFix` | Событие задачи №6 |
| `fix/` выглядит как ремонт | Подпись: точка сдвинулась или нет, название — предположение | `HumanReport.branchPointerLine` | тот же тест владельца | Открыть `fix/...` |
| Все задачи кажутся ошибками | Дефект по названию, блокировка, обычная задача, проверки отдельно | `problemKind`, `ownerProblems`, экран «Проблемы» | `problemKind` в тесте владельца | Экран «Проблемы» |
| Неясно, кто работал | Только автор GitHub или «не установлен» | `ownerWho`, `slimCommits` поле `author` | тест владельца не содержит grok/codex | Карточка «Кто выполнял» |
| Неясно, какой репозиторий | «Проект: owner/repo» и кнопка «Сменить проект» | `MainActivity`, шапка `SectionActivity` | нет UI-теста | Смена UNO ↔ gitteller |
| Частичный список прятал сдвиг ветки | Совпавшее имя сравнивается всегда | `ChangeSet.diffBranches` | `matchedBranchChangeAppearsEvenWithPartialLists`, зонд 8/8 | Живой неполный список |
| Новое имя пряталось с главной | «впервые обнаружено», если прошлый список полный | `HumanReport.whatChanged` | `partialCurrentWithProvenNewNameStillShowsHomeChange` | Не гонять специально |
| Любой SHA = новые записи | Откат не называется новыми записями | `Summary.plain` | `SummaryTest`, зонд | Не видно без двух снимков |
| Пропавшая ветка без события | «не обнаружено», удаление не подтверждено | `branch_absent` | `branchAbsenceInCompleteSnapshotsIsNotCalledDeletion` | Только если оба списка полные |

## UI и сеть

Эмулятор Android 14 (`gt14`) в этом прогоне **загрузился** (`sys.boot_completed=1`). `adb install -r -t` APK 1.0.10: **Success**. Сразу после старта снят один читаемый кадр главного экрана, тёмная тема: `evidence/ui-110/home-dark.png` и `/workspace/artifacts/ui-110-home-dark.png`.

На кадре уже был снимок `octocat/Hello-World`, время загрузки на экране `Nov 14, 2026 2:21 AM`. Это данные, которые лежали на эмуляторе до этого сеанса, не новая сверка, запущенная здесь. Видно: «Проект: octocat/Hello-World», «Нет достоверного сравнения», «Часть данных недоступна», первая проверка без базы, обычная задача «GitHub Flow» названа неподтверждённой ошибкой, пустые проверки не названы успехом.

Дальше System UI показал «isn't responding». Кадр светлой темы вышел чёрным и удалён. Разделы через `am start` не открылись: `SectionActivity` не экспортирована. Отдельных кадров «Изменения», «Проблемы», «Направления» и Back нет. Приватный репозиторий и PAT не использовались. Samsung A52: **NOT_DEVICE_VERIFIED**.
