# GitHub: только чтение

Разрешение Android: `INTERNET`. Запись в репозиторий не выполняется. Токен с правом записи не нужен и не запрашивается.

Все адреса только `https://api.github.com`, метод `GET`, заголовок `Authorization: Bearer` если токен сохранён. Продолжение страницы принимается лишь для того же `owner/repo` или для `repositories/{id}` этого же ответа.

| Когда | Адрес | Зачем |
|---|---|---|
| Обновление | `/repos/{owner}/{repo}` | Имя, ветка по умолчанию, id |
| Обновление | `/repos/{owner}/{repo}/branches?per_page=100` | Указатели направлений, не больше 5 страниц |
| Обновление | `/repos/{owner}/{repo}/commits?sha={default}&per_page=12` | Последние записи основной ветки, не вся история |
| Обновление | `/repos/{owner}/{repo}/issues?state=all&per_page=100` | Задачи, не больше 2 страниц |
| Обновление | `/repos/{owner}/{repo}/pulls?state=all&per_page=100` | Предложения, не больше 2 страниц |
| Открыли задачу | `/repos/{owner}/{repo}/issues/{n}` | Текст и метки |
| Открыли предложение | `/repos/{owner}/{repo}/pulls/{n}` и `/files` | Слияние, описание, файлы |
| Открыли коммит | `/repos/{owner}/{repo}/commits/{sha}` | Файлы этой записи |
| Открыли направление | `/repos/{owner}/{repo}/commits?sha={branch}&per_page=8` | Короткая история только этой ветки |
| Проверки | `/commits/{sha}/check-runs` | Если доступно. Обычный PAT часто не может |
| Запасной путь | `/commits/{sha}/status` | Статусы коммита, право Commit statuses: read |
| Запасной путь | `/actions/runs?head_sha={sha}` | Actions, право Actions: read, только этот SHA |

Если страница оборвана, в снимке стоит неполнота. `skipped` и `neutral` не считаются успехом. Нет ответа — «Результаты автоматических проверок получить не удалось».
