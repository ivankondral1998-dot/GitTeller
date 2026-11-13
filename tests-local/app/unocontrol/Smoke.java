package app.unocontrol;
import org.json.*;
public class Smoke {
  static int passed=0;
  static void check(boolean ok, String description){if(!ok)throw new AssertionError(description);passed++;}
  static JSONObject snapshot(boolean complete,String... pairs){
    JSONObject s=new JSONObject();JSONArray a=new JSONArray();
    for(int i=0;i<pairs.length;i+=2)a.put(new JSONObject().put("name",pairs[i]).put("sha",pairs[i+1]));
    return s.put("repo","demo/test").put("branches",a).put("branchesComplete",complete).put("warnings",new JSONArray());
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
    System.out.println("PASS: "+passed+" model checks (test doubles for org.json; no Android runtime)");
  }
}
