# SECURITY — GitTeller 1.0.2

- Сеть: только HTTPS GET на `https://api.github.com/repos/...`. Редиректы выключены. Чужой хост отклоняется.
- Методов POST/PATCH/DELETE в клиенте нет.
- Токен только в поле `MainActivity.token`. В SharedPreferences не пишется. `onDestroy` обнуляет его. Смена репозитория обнуляет токен, пока пользователь не введёт новый.
- Экспорт и импорт проходят через `SnapshotCodec.sanitizeSnapshot`. В снимок копируются только repo, даты, ветки (name+sha), commits (sha, message, date), issues/pulls (number, title, state), warnings. Поля token, authorization, body не копируются. Это проверено тестом `tokenFieldsAreDropped`.
- `allowBackup=false`, `usesCleartextTraffic=false`.
- Разрешение манифеста: только INTERNET.
- Ошибки 401/403/404/429 не затирают сохранённый снимок: `sync()` пишет prefs только после успешного `load`.
- User-Agent: `GitTeller/1.0.2`.
- Приватный PAT в APK и в исходниках этого пакета отсутствует.
- Keystore для токена не используется: токен живёт только в памяти процесса. Это ограничение, не «защищённое хранение».
