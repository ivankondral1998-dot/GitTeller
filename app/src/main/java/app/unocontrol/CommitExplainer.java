package app.unocontrol;

import java.util.Locale;

/** Conservative, source-attributed explanation of a commit subject; no claims of verification. */
final class CommitExplainer {
    private CommitExplainer() {}
    static String explain(String message) {
        String text = message == null ? "" : message.replace('\n', ' ').trim();
        if (text.isEmpty()) return "Добавлен коммит без описания. Проверка результата неизвестна.";
        String low = text.toLowerCase(Locale.ROOT);
        String intro;
        if (low.matches("^(fix|bugfix|hotfix)(\\([^)]*\\))?!?:.*"))
            intro = "Разработчик сообщил об исправлении. ";
        else if (low.matches("^(feat|feature)(\\([^)]*\\))?!?:.*"))
            intro = "Разработчик сообщил о новой возможности. ";
        else if (low.matches("^(test|tests)(\\([^)]*\\))?!?:.*"))
            intro = "Автор сообщил об изменении тестов. ";
        else if (low.matches("^(docs|doc)(\\([^)]*\\))?!?:.*"))
            intro = "Автор сообщил об обновлении документации. ";
        else intro = "В истории появился коммит. ";
        if (text.length()>140) text=text.substring(0,137)+"…";
        return intro+"Текст коммита: «"+text+"». Работа функции не проверена.";
    }
}
