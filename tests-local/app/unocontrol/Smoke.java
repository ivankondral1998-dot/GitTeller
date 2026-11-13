package app.unocontrol;
import org.json.*;
public class Smoke {
  static int passed=0;
  static void check(boolean ok, String description){if(!ok)throw new AssertionError(description);passed++;}
  static JSONObject snapshot(boolean complete,String... pairs){
    JSONObject s=new JSONObject();JSONArray a=new JSONArray();
    for(int i=0;i<pairs.length;i+=2)a.put(new JSONObject().put("name",pairs[i]).put("sha",pairs[i+1]));
    return s.put("repo","demo/test").put("branches",a).put("branchesComplete",complete)
        .put("issuesComplete",true).put("pullsComplete",true).put("warnings",new JSONArray());
  }
  public static void main(String[] args){
    check(Summary.of(null,null).heading.contains("нет свежей"),"no data");
    check(Summary.of(snapshot(true,"main","1"),null).heading.contains("Первый"),"first snapshot");
    Summary s=Summary.of(snapshot(true,"main","2","new","3"), snapshot(true,"main","1","old","4"));
    check(s.advanced==1&&s.added==1&&s.missing==1,"moved/added/missing");
    check(Summary.of(snapshot(false,"main","2"),snapshot(true,"main","1","old","4")).missing==0,"incomplete must not infer deletion");
    check(Summary.of(snapshot(true,"main","1"),snapshot(true,"main","1")).heading.contains("не изменились"),"same SHA");
    check(Summary.of(snapshot(false,"main","1"),snapshot(true,"main","1")).heading.contains("неполное"),"incomplete no-change not claim");
    Summary partialOld=Summary.of(snapshot(true,"main","1","existing","2"),snapshot(false,"main","1"));
    check(partialOld.added==0 && partialOld.description.contains("могли существовать раньше"),"partial baseline not creation proof");
    JSONObject imported=snapshot(true,"main","1").put("restoredFromBackup",true);
    check(Summary.of(imported,snapshot(true,"main","2")).heading.contains("резервной"),"import is not GitHub evidence");
    check(Summary.of(snapshot(true,"main","2"),imported).advanced==0,"no live difference against imported data");
    check(Summary.of(snapshot(false,"main","1"),null).description.contains("неполный"),"first partial snapshot disclosed");
    check(CommitExplainer.explain("fix: something").contains("не проверена"),"fix no PASS");
    check(CommitExplainer.explain("docs: text").contains("документацию"),"docs recognized");
    check(CommitExplainer.explain("feat(safety): widget").contains("новой возможности"),"feat scope");
    check(CommitExplainer.explain("test: build").contains("не проверена"),"test not proof");
    check(CommitExplainer.explain("").contains("без описания"),"no description");
    check(HumanReport.homeTitle(null,null,Summary.of(null,null)).contains("Подключите"),"empty home is understandable");
    JSONObject old=snapshot(true,"main","1"),now=snapshot(true,"main","1");
    check(HumanReport.homeTitle(now,old,Summary.of(now,old)).contains("сдвигов"),"home avoids SHA headline");
    check(HumanReport.homeDescription(now,old,Summary.of(now,old)).contains("не означает"),"no-change does not mean completed");
    check(HumanReport.commitSummary("docs(agents): restore work log").contains("документы"),"docs maps to cautious plain label");
    check(HumanReport.commitSummary("fix: broken query").contains("сообщил"),"fix is developer claim");
    check(HumanReport.commitSummary("feat(ui): summary").contains("сообщил"),"feature is developer claim");
    check(HumanReport.branchKind("research/uno-train").equals("Исследования"),"branch category");
    check(HumanReport.branchTopic("research/uno-train").contains("не определена"),"topic is not inferred from a project name");
    check(!HumanReport.branchTopic("research/uno-train").contains("Обучение"),"no project-specific guess");
    check(HumanReport.branchTopic("main").contains("Основная"),"main branch clear");
    check(HumanReport.state("closed").contains("не проверен"),"closed is not proof of a test");
    check(HumanReport.countState(new JSONArray().put(new JSONObject().put("state","open")),"open")==1,"count open tasks");
    JSONObject closedTask=snapshot(true,"main","1");
    JSONObject openTask=snapshot(true,"main","1");
    closedTask.put("issues",new JSONArray().put(new JSONObject().put("number",7).put("state","closed")));
    openTask.put("issues",new JSONArray().put(new JSONObject().put("number",7).put("state","open")));
    Summary taskDiff=Summary.of(closedTask,openTask);
    check(taskDiff.issueChanges==1,"same branches can have changed issue status");
    check(HumanReport.homeTitle(closedTask,openTask,taskDiff).contains("статусы задач"),"home discloses changed issues");
    check(HumanReport.homeDescription(closedTask,openTask,taskDiff).contains("не доказывает"),"status change not a passing test");
    closedTask.put("issuesComplete",false);
    check(Summary.of(closedTask,openTask).issueChanges==0,"partial issue responses are not proof of status transition");
    check(HumanReport.homeTitle(closedTask,openTask,Summary.of(closedTask,openTask)).contains("недоступна"),"incomplete issues shown on home");
    System.out.println("PASS: "+passed+" model checks (test doubles for org.json; no Android runtime)");
  }
}
