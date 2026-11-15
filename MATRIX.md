# 1.0.13.1-dev — требование, код, тест, результат

| Требование | Где | Тест | Результат | Не проверено |
| --- | --- | --- | --- | --- |
| Отдельное открытие ссылки, не только текст | `SectionActivity.technical` | строки в APK; сценарий A52 | код и DEX | тап на A52 OWNER_PENDING |
| URL только выбранного репозитория, без query/fragment/чужого хоста | `GithubLinks.validatedRepoUrl` | `GithubLinksSmoke` 21 PASS | host PASS | живой браузер NOT_RUN |
| `%2e%2e` и `..` не открывают другой путь | `GithubLinks` | smoke, в том числе encoded traversal | host PASS | — |
| Прежняя кнопка копирования технических сведений | `SectionActivity.technical` | чтение кода | кнопка осталась | — |
| Тот же пакет, сертификат, TokenVault, снимки | `app/build.gradle`, Keystore не трогался | apksigner, versionCode 29 | сертификат совпал | установка поверх OWNER_PENDING |
| Gradle unit tests | `:app:testDebugUnitTest` | штатный Gradle | FAIL: type tag 71, XML нет | — |
| Те же 110 тестов запасным JUnit | `app/src/test` | JUnitCore + пустой Activity | OK 110 | это не Gradle и не A52 |

# 1.0.13-dev — требование, код, тест, результат


| Требование | Где | Тест | Результат | Не проверено |
| --- | --- | --- | --- | --- |
| Поиск по сохранённым данным, без запроса на каждый символ | `ProjectSearch`, кнопка на главном, `SectionActivity.search` | `ProjectSearchTest`, `CoverageSearchSmoke` | host PASS | клавиатура и вёрстка на A52 |
| Кириллица, `fix/`, `#6`, префикс SHA, дубликаты, нет поля, >100 | `ProjectSearch.find` | `ProjectSearchTest` | host PASS | визуальный перенос длинных имён |
| Пустой запрос и «нет совпадений» не говорят «на GitHub нет» | `ProjectSearch.emptyHint/noMatch` | те же | host PASS | — |
| Открытие записи по ключу, а не чужой строки после смены снимка | `SectionActivity.at` | поиск отдаёт `key`; экран при чужом ключе возвращает «не найдена» | логика в коде, отдельного Android-теста нет | тап на A52 |
| Правая панель вместо `PopupMenu`, Back не выходит из приложения | `MainActivity.openDrawer`, `DrawerMotion` | `DrawerMotionTest` | host PASS для двойного открытия и Back | анимация, размытие, свайп, поворот, тема, шрифт: NOT_RUN |
| Размытие фона на API 31+, без него — затемнение | `MainActivity.setBlur` | нет прибора | код есть | A52 и эмулятор NOT_RUN |
| Почему данные неполные, один классификатор | `Coverage`, экран `incomplete`, блок «Что известно» | `CoverageTest` | host PASS | чтение экрана глазами |
| 401/403/429/таймаут не смешиваются; голый 403 не доказанная нехватка прав | `Coverage.http` | `CoverageTest.httpCodesAreNotMerged` | host PASS | живой ответ GitHub NOT_RUN |
| Полный список, 12 коммитов, предел страниц, обрыв, stale, 304, импорт, старая схема, checks не PASS | `Coverage` | `CoverageTest`, `CoverageSearchSmoke` | host PASS | — |
| Совместимость 1.0.12: пакет, токен, снимки, режим | versionCode 28, Keystore и prefs не мигрируют разрушающе | сборка и сертификат APK | подпись совпала | установка поверх на A52: OWNER_PENDING |
| Gradle и локальные тесты | `testDebugUnitTest`, `tests-local/run.sh` | см. `TEST_RESULTS.md` | try1 compile/type tag; try2 1 падение текста; try3 обрыв исполнителя; try4 110/110 exit 0 | эмулятор NOT_RUN |
