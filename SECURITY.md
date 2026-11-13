# SECURITY.md — GitTeller v1.1

- Токен только в RAM процесса. Не сохраняется в SharedPreferences, не логируется, не экспортируется, не попадает в APK.
- Только HTTPS GET к api.github.com. Никаких POST/PATCH/DELETE.
- Резервная копия — JSON без токена.
- Только разрешение INTERNET.
- Нет аналитики, WebView, телеметрии.
- Репозиторий по умолчанию — публичный octocat/Hello-World.
