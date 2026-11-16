# GitTeller — правила для продолжения работы

Это рабочий репозиторий. `main` — собираемый проект. `archive` — старый снимок папок `versions/`.

Перед правками прочитай навыки в `.grok/skills/`:

- `gitteller-core-invariants` — что нельзя менять
- `gitteller-event-model` — как описывать события GitHub
- `gitteller-android-ux` — экраны и кнопки
- `gitteller-build-verify` — как собирать и что проверять

Кратко:

- Только HTTPS GET. TokenVault не трогать без отдельного задания.
- События типизированные. `closed` не значит `merged`.
- Ссылки GitHub открываются только на выбранный проект, без query и чужих хостов.
- Сборка: `./gradlew :app:assembleDebug`. Новый debug-ключ не создавать.
- Устройство: Samsung Galaxy A52 — проверка только после реального запуска. Не писать `DEVICE_VERIFIED`.
- GitHub Actions этого репозитория не запускать.

Текущая версия: `1.0.15-dev`, versionCode 31.
