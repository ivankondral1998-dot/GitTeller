# GitTeller 1.0.18-dev

Нативное Android-приложение. Читает GitHub только через HTTPS GET и объясняет, что реально изменилось в проекте. Не коммерческий релиз и не приёмка на устройстве.

| | |
|---|---|
| Пакет | `app.gitteller.mobile` |
| Версия | `1.0.18-dev`, versionCode **34** |
| Сертификат | тот же debug-ключ, SHA-256 `67ec79901c9ff9f59d9dc1387efd7b759cfd716953bd2d0385076e68bd26b6ef` |
| Устройство | Samsung Galaxy A52, Android 14 — `OWNER_PENDING` |

## Рабочая среда

Клонировать и собирать из корня репозитория. Исходники — это `main`, не архив.

Нужно: JDK 17 или 21, Android SDK 35, Build Tools 34.0.0. Gradle берётся из wrapper.

```bash
git clone https://github.com/ivankondral1998-dot/GitTeller.git
cd GitTeller
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
sh tests-local/run.sh
```

`local.properties` не коммитится. Новый debug-ключ не создавать: `app/build.gradle` требует `~/.android/debug.keystore`. Если его нет, сборка останавливается специально.

Готовый APK: `app/build/outputs/apk/debug/app-debug.apk`. Уже собранные версии лежат в `releases/`.

## Что где

| Путь | Назначение |
|---|---|
| `app/` | код, манифест, тесты |
| `tests-local/` | host-проверки без Android runtime |
| `releases/` | APK и скриншоты по версиям |
| `evidence/` | логи и прошлые APK из аудитов |
| `CHANGELOG.md`, `UPDATE.md`, `SECURITY.md` | история и правила обновления |

История `main` — коммит на каждую версию от v1.0 до 1.0.13.1-dev.

GitHub Actions для этого репозитория не включены: сборка локальная, чтобы не плодить чужие прогоны.
