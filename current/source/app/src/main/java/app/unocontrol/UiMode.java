package app.unocontrol;

/** Presentation only. Both modes read the same GitHub snapshots and use the same evidence rules. */
final class UiMode {
    static final String PREF = "display_mode";
    static final String NOVICE = "novice";
    static final String DEVELOPER = "developer";

    private UiMode() {}

    static String normalize(String candidate) {
        return DEVELOPER.equals(candidate) ? DEVELOPER : NOVICE;
    }

    /** Explains the evidence rule, never inventing a successful check or current agent activity. */
    static String reason(String kind, boolean hasSnapshot, boolean comparable, boolean complete) {
        if (!hasSnapshot) return "GitHub ещё не предоставил данные этого проекта. Объяснять пока нечего.";
        switch (kind) {
            case "changed":
                if (!comparable) return "Чтобы установить изменения, нужны два достоверных снимка одного проекта. Импортированный файл не заменяет живую проверку.";
                return complete
                        ? "Сопоставлены два снимка GitHub: указатели веток и доступные состояния задач и PR. Это не проверка работоспособности кода."
                        : "Сопоставлены только доступные записи двух снимков. Из-за неполных списков нельзя делать вывод о состоянии всего проекта.";
            case "where":
                return "GitHub показывает названия веток и их указатели. Это свидетельство записанных изменений, но не того, что агент работает прямо сейчас.";
            case "problems":
                return "Открытая задача не обязательно является ошибкой. Для подтверждения дефекта нужны отдельные результаты проверок или воспроизведения.";
            case "who":
                return "GitHub может показать автора коммита и автора PR. Эти сведения сами по себе не определяют исполнителя или активную сессию ИИ-агента.";
            default:
                return "Формулировка основана на доступных данных GitHub. Неизвестное не считается успешной проверкой.";
        }
    }
}
