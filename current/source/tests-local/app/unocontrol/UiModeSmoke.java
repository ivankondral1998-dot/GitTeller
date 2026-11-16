package app.unocontrol;

public final class UiModeSmoke {
    private static void ok(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        ok(UiMode.NOVICE.equals(UiMode.normalize(null)), "default mode");
        ok(UiMode.NOVICE.equals(UiMode.normalize("invalid")), "unknown mode");
        ok(UiMode.DEVELOPER.equals(UiMode.normalize(UiMode.DEVELOPER)), "developer selection");
        ok(UiMode.reason("changed",true,false,true).contains("два достоверных"), "no baseline");
        ok(UiMode.reason("changed",true,true,false).contains("неполных"), "partial evidence");
        ok(UiMode.reason("changed",true,true,true).contains("не проверка"), "no false PASS");
        ok(UiMode.reason("who",true,true,true).contains("не определяют исполнителя"), "author is not agent");
        ok(UiMode.reason("problems",true,true,true).contains("не обязательно"), "issue is not defect");
        ok(UiMode.reason("where",true,true,true).contains("не того, что агент"), "branch is not live agent");
        System.out.println("UiModeSmoke: 9 PASS");
    }
}
