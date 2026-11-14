package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/** Independent regression probes for audit 1.0.8 -> 1.0.8.1; no Android runtime. */
public final class AuditRegression1081 {
  static int total;
  static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); total++; }
  static JSONObject snap(boolean allIssues, JSONObject issue) {
    JSONArray issues = new JSONArray();
    if (issue != null) issues.put(issue);
    return new JSONObject().put("repo","demo/app")
      .put("branches",new JSONArray().put(new JSONObject().put("name","main").put("sha","0123456789abcdef0123456789abcdef01234567")))
      .put("branchesComplete",false)
      .put("issuesComplete",allIssues).put("pullsComplete",true)
      .put("issues",issues).put("pulls",new JSONArray());
  }
  static JSONObject row(String title,String state) {return new JSONObject().put("number",7).put("title",title).put("state",state);}
  public static void main(String[] args) throws Exception {
    JSONObject old = snap(false,row("Fix crash","open"));
    JSONObject now = snap(false,row("Fix crash","closed"));
    check(ChangeSet.issueChanges(old,now)==1,"known item must be detected even with incomplete enumeration");
    JSONArray diff = ChangeSet.between(old,now);
    check(diff.length()==1 && diff.optJSONObject(0).optString("kind").equals("issue_state"),"partial list exposes exact matched change");
    check(HumanReport.whatChanged(now,old,Summary.of(now,old)).contains("закрыта"),"home does not hide known change behind incomplete branch list");
    check(ChangeSet.between(snap(false,null),snap(false,row("unseen","open"))).length()==0,"unseen partial item is not declared new");

    JSONObject fullOld = snap(true,null).put("branchesComplete",true);
    JSONObject fullNow = snap(true,row("New task","open")).put("branchesComplete",true);
    String line = Summary.of(fullNow,fullOld).description;
    check(line.contains("Изменились записи") && !line.contains("Статус изменился"),"creation is not described as status transition");
    check(HumanReport.homeTitle(fullNow,fullOld,Summary.of(fullNow,fullOld)).contains("записи"),"home title explains record changes");

    JSONArray prior = new JSONArray().put(new JSONObject().put("kind","branch_moved").put("id","branch:main")
       .put("event","Указатель изменился").put("at",100L));
    JSONArray recent = new JSONArray().put(new JSONObject().put("kind","branch_moved").put("id","branch:main")
       .put("event","Указатель изменился"));
    JSONArray merged = EventFeed.merge(prior,recent,200L);
    check(merged.length()==2 && merged.optJSONObject(0).optLong("at", 0)==200L,
       "legitimate recurring change must not disappear from history");

    Method method=GithubClient.class.getDeclaredMethod("etagIfSafe",JSONObject.class,String.class,String.class,String.class);
    method.setAccessible(true);
    JSONObject legacy = fullNow.put("issuesEtag","\"etag-of-first-page\"");
    check(method.invoke(null, legacy, "issues", "issuesComplete", "issuesEtag") == null,
      "legacy ETag without proven single-page coverage must not be trusted");
    legacy.put("issuesEtagSinglePage",true);
    check("\"etag-of-first-page\"".equals(method.invoke(null,legacy,"issues","issuesComplete","issuesEtag")),
      "single-page ETag can be used safely");

    File dir=Files.createTempDirectory("gt-atomic-regression").toFile();
    try {
      File dest=new File(dir,"token.bin"),bak=new File(dir,"token.bin.bak"),tmp=new File(dir,"token.bin.tmp");
      Files.write(bak.toPath(),new byte[]{7,1,2});
      check(AtomicCipherFile.hasRecord(dest),"legacy backup is recognized");
      check(AtomicCipherFile.recover(dest).isFile() && !bak.exists(),"recover interrupted old rename");
      check(Files.readAllBytes(dest.toPath())[0]==7,"recovered old encrypted content unchanged");
      Files.write(tmp.toPath(),new byte[]{9,8,7});
      AtomicCipherFile.replace(tmp,dest);
      check(Files.readAllBytes(dest.toPath())[0]==9 && !tmp.exists(),"new encrypted content installed atomically");
      AtomicCipherFile.delete(dest);
      check(!AtomicCipherFile.hasRecord(dest),"delete removes encrypted record");
    } finally {
      for (File file: dir.listFiles()) file.delete();
      dir.delete();
    }
    System.out.println("PASS: "+total+" independent audit regressions (host only, synthetic data)");
  }
}
