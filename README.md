# GitTeller 1.0.10-dev

Нативное Android-приложение. Не коммерческий релиз и не приёмка на устройстве.

`applicationId` `app.gitteller.mobile`, `versionCode` 25, `versionName` 1.0.10-dev. Подпись debug совпадает с 1.0.6–1.0.9, поэтому обновление идёт поверх, без удаления. Если система откажется, не очищайте приложение: на телефоне может быть токен.

Главный экран отвечает на четыре вопроса: что изменилось, где работа, какие проблемы, кто выполнял. Сдвиг указателя ветки не называется прогрессом и не называется списком файлов, пока сравнение не открыто. Обычная задача не называется ошибкой. Автор GitHub не называется работающим сейчас агентом.

```bash
ANDROID_HOME=/tmp/android-sdk /opt/gradle-8.9/bin/gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
sh tests-local/run.sh
```

Первый Gradle Test Executor в этой среде часто падает с `Unexpected type tag 71`. Успешен только прогон с `BUILD SUCCESSFUL`. См. `TEST_RESULTS.md`. Прямой JUnitCore не подменяет Gradle. Samsung A52 этой сборкой не проверялся.
