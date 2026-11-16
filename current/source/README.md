# GitTeller 1.0.13.1-dev

Нативное Android-приложение. Не коммерческий релиз и не приёмка на устройстве.

`applicationId` — `app.gitteller.mobile`, `versionCode` — 29, `versionName` — `1.0.13.1-dev`. Подпись — исходный debug-ключ 1.0.6–1.0.13. Контрольная SHA-256 сертификата `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef` сверяется по готовому APK. См. [UPDATE.md](UPDATE.md).

На главном: два режима, поиск по сохранённым данным, блок «Что известно» и правая панель. Экран «Почему данные неполные?» объясняет только то, что записано в снимке. Ссылка GitHub на карточке события и задачи/PR открывается отдельной кнопкой и только на выбранный проект, без query и чужих хостов.

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
sh tests-local/run.sh
```

Для сборки нужны JDK 17 или 21, Gradle 8.9 и Android SDK 35 с Build Tools 34.0.0. Укажите свой SDK в `ANDROID_HOME` или `local.properties`. Результаты проверки 1.0.11-dev находятся в отдельном архиве проверок.
