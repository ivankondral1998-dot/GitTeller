# GitTeller 1.0.9-dev

Нативное Android-приложение. Не коммерческий релиз и не приёмка на устройстве.

`applicationId` `app.gitteller.mobile`, `versionCode` 24, `versionName` 1.0.9-dev. Подпись debug совпадает с 1.0.6, 1.0.7 и 1.0.8, поэтому обновление идёт поверх, без удаления. Если система откажется, не очищайте приложение: на телефоне может быть токен.

```bash
ANDROID_HOME=/tmp/android-sdk gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
sh tests-local/run.sh
```

Первый Gradle Test Executor в этой среде часто падает с `Unexpected type tag 71`. Успешен только прогон с `BUILD SUCCESSFUL`. См. `TEST_RESULTS.md`. Прямой JUnitCore не подменяет Gradle. A52 не проверялся.

