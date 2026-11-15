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
