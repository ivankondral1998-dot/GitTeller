# Проверки 1.0.5

Среда: JDK 17, Gradle 8.9, AGP 8.7.3, compileSdk 35. Эмулятор `gt14`, Android 14, x86_64, 1080×2400, гостевая RAM 768. Хост 4 ГБ без swap. qemu убит OOM при anon-rss около 3.8 ГБ.

## Команды

- `sh tests-local/run.sh` — exit 0. `PASS: 27 model checks`. `PARSE OK: 16`. Это не Android и не Keystore.
- `gradle --no-daemon :app:testDebugUnitTest` — exit 0, `BUILD SUCCESSFUL`. XML: 31 тест, 0 failures, 0 errors (Summary 12, GithubStatus 6, SnapshotCodec 4, CommitExplainer 3, TokenVaultRules 3, HumanReport 3). В том же логе есть `Unexpected type tag 71`. Это шум сокета, не падение тестов: XML записан.
- `gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest` — APK собраны. Первый общий запуск упал на компиляции `HumanReportTest` (необработанный JSONException). Тест исправлен, APK пересобирать не пришлось: основной код уже был в dex. Повторный unit-test прошёл.
- Инструментальные тесты Keystore на 1.0.5: **NOT_RUN**. На 1.0.4 они проходили на таком же эмуляторе; этот APK заново не гонялся, потому что процесс умер после снимка главного экрана.

## Эмулятор

- Установка debug APK: Success.
- `am start` компонента `app.gitteller.mobile/app.unocontrol.MainActivity`: принят.
- Снимок `evidence/emulator-home-1.0.5.png` (92964 байт) показывает главный экран GitTeller, не заставку и не ANR: «Подключите проект», кнопка «Обновить с GitHub», репозиторий `octocat/Hello-World`, без SHA и без списка веток.
- Следующая загрузка эмулятора дошла до `am start`, но `uiautomator` вернул `null root node`, а серия `input tap` не изменила ни одного пикселя кадра. Меню и разделы поэтому не сняты. Считается только первый кадр главного экрана.
