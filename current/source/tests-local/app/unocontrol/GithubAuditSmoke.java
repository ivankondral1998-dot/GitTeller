package app.unocontrol;
import org.json.JSONArray;
import org.json.JSONObject;
/** Source-model regression tests. Not Android runtime or HTTP integration tests. */
public final class GithubAuditSmoke {
  private static int passed;
  static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);passed++;}
  public static void main(String[] args) {
    JSONArray all = new JSONArray()
      .put(new JSONObject().put("conclusion", "success"))
      .put(new JSONObject().put("conclusion", "skipped"))
      .put(new JSONObject().put("conclusion", "neutral"))
      .put(new JSONObject().put("conclusion", "failure"))
      .put(new JSONObject().put("conclusion", ""));
    String msg=GithubClient.formatChecks(new JSONObject().put("check_runs",all).put("total_count", 7));
    check(msg.contains("1 с результатом success"),"success counted only as success");
    check(msg.contains("2 пропущенных или нейтральных"),"skip and neutral are not success");
    check(msg.contains("1 с ошибкой"),"failed check disclosed");
    check(msg.contains("1 без итога"),"pending check disclosed");
    check(msg.contains("Список обрезан"),"check run pagination marked incomplete");
    JSONObject commit=new JSONObject().put("files",new JSONArray()
      .put(new JSONObject().put("filename","app/Main.java").put("status","modified")));
    check(GithubClient.formatCommit(commit,"нет проверок",true).text.contains("список неполный"),
      "commit file pagination must be disclosed");
    check(!GithubClient.formatCommit(commit,"нет проверок",false).text.contains("список неполный"),
      "complete commit files not misreported as cut");
    check(GithubClient.formatChecks(new JSONObject().put("check_runs",new JSONArray()))
       .contains("Проверок GitHub в ответе нет"),"empty runs not called successful");
    System.out.println("PASS: "+passed+" GithubAuditSmoke checks (model doubles, no network/Android)");
  }
}
