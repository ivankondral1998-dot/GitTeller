# CHANGELOG

## 1.0.4-dev (versionCode 18)

- Токен GitHub хранится по репозиторию в Android Keystore (AES-GCM). На диске только `GTV1` + IV + ciphertext. Каталог `noBackupFilesDir`, `allowBackup=false`.
- Пустое поле не стирает сохранённый доступ. Удаление — отдельная кнопка и только выбранный `owner/repo`.
- Смена проекта не переносит токен и обрывает незавершённый запрос (`syncEpoch`, `disconnect`). Ответ опоздавшего запроса не пишется в другой проект.
- Ошибки Keystore и повреждённый файл не откатываются в открытый SharedPreferences.
- `GithubClient` не кладёт токен в URL. `safeMessage` маскирует bearer, `ghp_` и `github_pat_`.
- Инструментальные тесты на синтетических маркерах. Настоящий PAT не использовался.

## 1.0.3-audit

Смотрите checkpoint ZIP. APK 1.0.3 не выпускался. Логика сводки и архива из того патча сохранена.
