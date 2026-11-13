package app.unocontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Real Android views. No HTML, CSS or WebView. */
public final class MainActivity extends Activity {
    private static final String DEFAULT_REPO = "octocat/Hello-World";
    private int BG = Color.rgb(12,18,25);
    private int PANEL = Color.rgb(23,33,44);
    private int PANEL_2 = Color.rgb(30,43,56);
    private int TEXT = Color.rgb(238,242,247);
    private int MUTED = Color.rgb(158,172,188);
    private int ACCENT = Color.rgb(218,174,127);
    private int WARN = Color.rgb(239,198,128);
    private int STROKE = Color.rgb(47,63,80);
    private static final int REQUEST_IMPORT = 1;
    private static final int REQUEST_EXPORT = 2;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private JSONObject current, previous;
    private String repo = DEFAULT_REPO;
    private String token = ""; // session only; NEVER saved
    private boolean loading;
    private LinearLayout content;
    private Button refresh;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        prefs = getSharedPreferences("gitteller_v1", MODE_PRIVATE);
        repo = prefs.getString("repo", DEFAULT_REPO);
        current = parse(prefs.getString(key(repo, "current"), ""));
        previous = parse(prefs.getString(key(repo, "previous"), ""));
        draw();
    }

    private static String key(String repo, String suffix) { return repo.toLowerCase(java.util.Locale.ROOT) + ":" + suffix; }

    private static JSONObject parse(String s) {
        try { return s == null || s.isEmpty() ? null : new JSONObject(s); }
        catch (Exception ignored) { return null; }
    }

    private void applyPalette() {
        int night = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean dark = night != Configuration.UI_MODE_NIGHT_NO;
        if (dark) {
            BG = Color.rgb(12,18,25);
            PANEL = Color.rgb(23,33,44);
            PANEL_2 = Color.rgb(30,43,56);
            TEXT = Color.rgb(238,242,247);
            MUTED = Color.rgb(158,172,188);
            ACCENT = Color.rgb(218,174,127);
            WARN = Color.rgb(239,198,128);
            STROKE = Color.rgb(47,63,80);
            getWindow().getDecorView().setSystemUiVisibility(0);
        } else {
            BG = Color.rgb(246,243,236);
            PANEL = Color.rgb(255,255,255);
            PANEL_2 = Color.rgb(236,230,220);
            TEXT = Color.rgb(28,32,38);
            MUTED = Color.rgb(90,98,108);
            ACCENT = Color.rgb(140,96,48);
            WARN = Color.rgb(140,84,20);
            STROKE = Color.rgb(214,206,194);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius, int line) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));
        if (line!=0) d.setStroke(dp(1), line);
        return d;
    }
    private LinearLayout vertical() {
        LinearLayout ll=new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        return ll;
    }
    private LinearLayout.LayoutParams lp(int w,int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }
    private TextView txt(String text,int sp,int color,boolean bold) {
        TextView v=new TextView(this);
        v.setText(text);v.setTextSize(sp);v.setTextColor(color);
        v.setFontFeatureSettings("kern");
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setIncludeFontPadding(false);
        return v;
    }
    private void space(LinearLayout p,int size){ p.addView(new View(this),lp(1,size)); }
    private void addLine(LinearLayout p,String text,int sp,int color,boolean bold,int top){
        if (top>0)space(p,top);
        p.addView(txt(text,sp,color,bold),lp(-1,-2));
    }
    private LinearLayout panel(LinearLayout parent){
        LinearLayout card=vertical();
        card.setPadding(dp(18),dp(18),dp(18),dp(18));
        card.setBackground(shape(PANEL,18,STROKE));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(12);
        parent.addView(card,p);
        return card;
    }

    private void draw() {
        applyPalette();
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout wrap=vertical();
        wrap.setPadding(dp(20),dp(22),dp(20),dp(34));
        scroll.addView(wrap);
        root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        content=wrap;

        LinearLayout head=new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=vertical();
        brand.addView(txt("GitTeller",17,TEXT,true));
        addLine(brand,"GitHub простыми словами",12,MUTED,false,5);
        head.addView(brand,new LinearLayout.LayoutParams(0,-2,1f));
        TextView menu=txt("⋮",27,TEXT,false);
        menu.setGravity(Gravity.CENTER);menu.setContentDescription("Дополнительные разделы и настройки");
        menu.setBackground(shape(PANEL,14,STROKE));
        head.addView(menu,lp(44,44));
        menu.setOnClickListener(v -> showMenu(menu));
        wrap.addView(head,lp(-1,-2));
        space(wrap,34);

        addLine(wrap,"Состояние проекта",29,TEXT,true,0);
        String shortRepo=repo.length()>43?repo.substring(0,40)+"…":repo;
        addLine(wrap,shortRepo,13,MUTED,false,11);
        space(wrap,25);

        Summary summary=Summary.of(current,previous);
        LinearLayout hero=panel(wrap);
        TextView state=txt(loading?"● ЗАГРУЗКА":"● "+(current==null?"НЕ ПРОВЕРЕНО":
                current.optBoolean("restoredFromBackup",false)?"СНИМОК ИЗ ФАЙЛА":"ДАННЫЕ GITHUB"),12,
                loading?WARN:current==null?MUTED:ACCENT,true);
        hero.addView(state,lp(-1,-2));
        addLine(hero,summary.heading,20,TEXT,true,16);
        addLine(hero,summary.plain,16,TEXT,false,10);
        addLine(hero,summary.description,13,MUTED,false,10);
        if(current!=null) {
            long seen=current.optLong("capturedAt",0);
            addLine(hero,"Последняя сверка: "+formatTime(seen),12,MUTED,false,18);
            if (current.optBoolean("restoredFromBackup",false))
                addLine(hero,"Данные восстановлены из файла, не сверены заново с GitHub.",12,WARN,false,10);
            if (seen<=0 || seen>System.currentTimeMillis()+60_000L || System.currentTimeMillis()-seen>6L*60*60*1000)
                addLine(hero,"⚠ Сохранённые сведения могут быть устаревшими. Обновите данные.",12,WARN,false,10);
        }
        space(wrap,6);

        refresh=new Button(this);
        refresh.setText(loading?"Обновление…":"↻   Обновить с GitHub");
        refresh.setAllCaps(false);
        refresh.setTextSize(15);
        refresh.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        refresh.setTextColor(BG);
        refresh.setBackground(shape(ACCENT,15,0));
        refresh.setEnabled(!loading);
        wrap.addView(refresh,lp(-1,54));
        refresh.setOnClickListener(v->sync());
        space(wrap,14);
        Button details=new Button(this);
        details.setText("Подробнее о проекте");
        details.setAllCaps(false);
        details.setTextSize(15);
        details.setTextColor(TEXT);
        details.setBackground(shape(PANEL_2,15,STROKE));
        wrap.addView(details,lp(-1,52));
        details.setOnClickListener(v->showReport());
        if (current != null) {
            JSONArray commits = current.optJSONArray("commits");
            if (commits != null && commits.length() > 0 && commits.optJSONObject(0) != null) {
                space(wrap, 8);
                LinearLayout claim = panel(wrap);
                addLine(claim, "Последнее заявление разработчика", 12, ACCENT, true, 0);
                addLine(claim, CommitExplainer.explain(commits.optJSONObject(0).optString("message")), 14, TEXT, false, 8);
                addLine(claim, "Это пересказ заголовка коммита, не проверка работы программы.", 12, MUTED, false, 8);
            }
        }
        space(wrap,18);

        addLine(wrap,"Требует внимания",16,TEXT,true,0);
        addLine(wrap,summary.attention,14,MUTED,false,11);
        space(wrap,10);
        TextView footer=txt("Чтение GitHub · без изменений в репозитории",12,MUTED,false);
        footer.setGravity(Gravity.CENTER);
        wrap.addView(footer,lp(-1,-2));
    }

    private static String formatTime(long millis){
        if(millis<=0) return "нет даты";
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(millis));
    }

    private void showMenu(View anchor) {
        PopupMenu pm=new PopupMenu(this,anchor);
        pm.getMenu().add(0,1,0,"Репозиторий и доступ");
        pm.getMenu().add(0,2,1,"Ветки проекта");
        pm.getMenu().add(0,3,2,"Последние коммиты");
        pm.getMenu().add(0,4,3,"Задачи и запросы изменений");
        pm.getMenu().add(0,5,4,"Ограничения сверки");
        pm.getMenu().add(0,6,5,"Импорт JSON-архива");
        pm.getMenu().add(0,7,6,"О приложении");
        pm.getMenu().add(0,8,7,"Экспорт данных JSON");
        pm.getMenu().add(0,9,8,"Подробный отчёт");
        pm.setOnMenuItemClickListener(item->{
            switch(item.getItemId()) {
                case 1: setupDialog();return true;
                case 2: listDialog("Ветки проекта", "branches", true);return true;
                case 3: listDialog("Последние коммиты", "commits", false);return true;
                case 4: detailsDialog();return true;
                case 5: warningsDialog();return true;
                case 6: importLegacy();return true;
                case 7: showInfo();return true;
                case 8: exportBackup();return true;
                case 9: showReport();return true;
            }return false;
        });
        pm.show();
    }

    private void setupDialog(){
        LinearLayout form=vertical();form.setPadding(dp(20),dp(8),dp(20),dp(2));
        addLine(form,"GitHub-адрес, например owner/repo",13,MUTED,false,0);
        EditText repoField=new EditText(this);
        repoField.setSingleLine(true);repoField.setText(repo);
        repoField.setSelectAllOnFocus(true);
        repoField.setTextColor(TEXT);repoField.setHintTextColor(MUTED);
        form.addView(repoField,lp(-1,-2));
        addLine(form,"Токен для приватного репозитория (не сохраняется)",13,MUTED,false,18);
        EditText tokenField=new EditText(this);
        tokenField.setHint(token.isEmpty()?"Необязательно для публичного проекта":"Токен уже введён до закрытия приложения");
        tokenField.setTextColor(TEXT);tokenField.setHintTextColor(MUTED);
        tokenField.setSingleLine(true);
        tokenField.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(tokenField,lp(-1,-2));
        new AlertDialog.Builder(this).setTitle("Подключение к GitHub")
                .setView(form)
                .setNegativeButton("Отмена",(d,w)->{})
                .setNeutralButton("Сбросить токен",(d,w)->{token="";toast("Токен удалён из памяти приложения");})
                .setPositiveButton("Сохранить",(d,w)->{
                    String r=repoField.getText().toString().trim();
                    if(!r.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}")||r.contains("..")) {
                        toast("Нужен адрес вида owner/repo");return;
                    }
                    String newToken=tokenField.getText().toString().trim();
                    if(!repo.equalsIgnoreCase(r)){
                        // Do not silently reuse a credential when changing the repository.
                        token = "";
                        repo=r;
                        current=parse(prefs.getString(key(repo,"current"),""));
                        previous=parse(prefs.getString(key(repo,"previous"),""));
                        prefs.edit().putString("repo",repo).apply();
                    }
                    if(!newToken.isEmpty())token=newToken;
                    draw();toast("Настройки сохранены. Нажмите «Обновить».");
                }).show();
    }

    private void sync(){
        if(loading)return;
        loading=true;draw();
        final String selected=repo;
        final String access=token;
        worker.execute(()->{
            try{
                JSONObject fresh=new GithubClient(access).load(selected).snapshot;
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    if(!selected.equals(repo)) {loading=false;draw();return;}
                    previous=current;
                    current=fresh;
                    prefs.edit().putString(key(repo,"previous"),previous==null?"":previous.toString())
                            .putString(key(repo,"current"),current.toString()).apply();
                    loading=false;draw();toast("Данные обновлены");
                });
            }catch(Exception e){
                final String message=GithubClient.safeMessage(e);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    loading=false;draw();
                    if(!selected.equals(repo))return;
                    new AlertDialog.Builder(this).setTitle("Сверка не выполнена")
                            .setMessage(message+"\n\nПредыдущие данные не изменены.")
                            .setPositiveButton("Закрыть",(d,w)->{}).show();
                });
            }
        });
    }

    private void listDialog(String title,String name,boolean branches){
        if(current==null){toast("Сначала обновите данные GitHub");return;}
        JSONArray data=current.optJSONArray(name);
        if(data==null || data.length()==0){toast("Данных в разделе пока нет");return;}
        int n=Math.min(data.length(),120);
        String[] items=new String[n];
        for(int i=0;i<n;i++) {
            JSONObject x=data.optJSONObject(i);
            if(x==null){items[i]="Недоступная запись";continue;}
            String namePart=branches?x.optString("name"):x.optString("message");
            String hash=x.optString("sha","");
            if(namePart.length()>145)namePart=namePart.substring(0,142)+"…";
            items[i]=namePart+"\n"+(hash.length()>10?hash.substring(0,10):hash);
        }
        new AlertDialog.Builder(this).setTitle(title+(data.length()>120?" · первые 120":""))
                .setItems(items,(d,which)->{
                    JSONObject x=data.optJSONObject(which);
                    if(x==null)return;
                    String sha=x.optString("sha","");
                    String label=branches?x.optString("name"):x.optString("message");
                    new AlertDialog.Builder(this).setTitle(label)
                        .setMessage((branches ? "Ветка GitHub.\n\n" : CommitExplainer.explain(x.optString("message")) + "\n\n") +
                                "Контрольный SHA:\n"+sha)
                        .setPositiveButton("Копировать SHA",(dd,w)->copy(sha))
                        .setNegativeButton("Назад",(dd,w)->listDialog(title,name,branches)).show();
                })
                .setNegativeButton("Закрыть",(d,w)->{}).show();
    }
    private void detailsDialog(){
        if(current==null){toast("Сначала обновите данные GitHub");return;}
        JSONArray issues=current.optJSONArray("issues"),pulls=current.optJSONArray("pulls");
        StringBuilder b=new StringBuilder();
        b.append("Задачи: ").append(issues==null?"нет данных":issues.length()).append(" записей.\n");
        b.append("Запросы изменений: ").append(pulls==null?"нет данных":pulls.length()).append(" записей.\n\n");
        b.append("Закрытые задачи и объединённые изменения не означают, что тесты пройдены.\n");
        if(issues!=null)for(int i=0;i<Math.min(issues.length(),8);i++){
            JSONObject x=issues.optJSONObject(i);
            if(x!=null)b.append("\n#").append(x.optInt("number")).append(" ")
                    .append(x.optString("title")).append(" [").append(x.optString("state")).append("]");
        }
        new AlertDialog.Builder(this).setTitle("Задачи и изменения").setMessage(b.toString())
                .setPositiveButton("Закрыть",(d,w)->{}).show();
    }
    private void warningsDialog(){
        if(current==null){toast("Новых данных нет");return;}
        JSONArray warnings=current.optJSONArray("warnings");
        StringBuilder msg=new StringBuilder("GitHub API · только чтение.\n");
        msg.append("Ветки: ").append(current.optBoolean("branchesComplete")?"список загружен целиком":"список может быть неполным");
        if(warnings==null || warnings.length()==0)msg.append("\n\nОшибок загрузки не записано.");
        else for(int i=0;i<warnings.length();i++)msg.append("\n\n• ").append(warnings.optString(i));
        msg.append("\n\nСинхронизация не проверяет тесты, сборки или качество кода.");
        new AlertDialog.Builder(this).setTitle("Точность данных").setMessage(msg.toString())
                .setPositiveButton("Закрыть",(d,w)->{}).show();
    }
    private void showReport(){
        Summary sum=Summary.of(current,previous);
        StringBuilder report=new StringBuilder(sum.heading).append("\n\n")
                .append(sum.description).append("\n\n")
                .append("На что обратить внимание: ").append(sum.attention).append("\n");
        if(current!=null){
            report.append("\nИсточник: ")
                    .append(current.optBoolean("restoredFromBackup",false)?"импортированный JSON, сеть не проверена":"GitHub API")
                    .append(", ").append(repo)
                    .append("\nСверка: ").append(formatTime(current.optLong("capturedAt")));
            JSONArray commits=current.optJSONArray("commits");
            if(commits!=null&&commits.length()>0){
                report.append("\n\nПоследние заявления разработчиков:");
                for(int i=0;i<Math.min(6,commits.length());i++){
                    JSONObject c=commits.optJSONObject(i);
                    if(c!=null)report.append("\n• ").append(CommitExplainer.explain(c.optString("message")));
                }
            }
            JSONArray branches=current.optJSONArray("branches");
            if(branches!=null)report.append("\n\nПолучено веток: ").append(branches.length());
            if(!current.optBoolean("branchesComplete",false))report.append(" (список неполный)");
            if(previous!=null){
                appendStateChanges(report,"задач",previous.optJSONArray("issues"),current.optJSONArray("issues"));
                appendStateChanges(report,"запросов изменений",previous.optJSONArray("pulls"),current.optJSONArray("pulls"));
                java.util.Map<String,String> before=branchHeads(previous.optJSONArray("branches"));
                java.util.Map<String,String> after=branchHeads(branches);
                report.append("\n\nИзменения SHA относительно прошлого снимка:");
                int shown=0;
                for(java.util.Map.Entry<String,String> entry:after.entrySet()){
                    String old=before.get(entry.getKey());
                    if(old==null || !old.equals(entry.getValue())){
                        report.append("\n• ").append(entry.getKey())
                                .append(old==null?" (новая ветка)":" (SHA изменён)");
                        if(++shown>=20){report.append("\n… остальные изменения скрыты");break;}
                    }
                }
                boolean complete=current.optBoolean("branchesComplete",false) && previous.optBoolean("branchesComplete",false);
                if(complete)for(String name:before.keySet())if(!after.containsKey(name)&&shown<20){
                    report.append("\n• ").append(name).append(" (больше не обнаружена)");shown++;
                }
                if(!complete)report.append("\nВнимание: не все списки полны, исчезновение веток не оценивается.");
                if(shown==0)report.append("\nИзменений не обнаружено в доступной выборке.");
            }
        }
        report.append("\n\nЗаявление разработчика и наличие коммита не доказывают, что функция работает.");
        new AlertDialog.Builder(this).setTitle("Подробный отчёт")
                .setMessage(report.toString()).setPositiveButton("Закрыть",(d,w)->{}).show();
    }

    private static void appendStateChanges(StringBuilder report,String what,JSONArray old,JSONArray now){
        if(old==null || now==null)return;
        java.util.Map<Integer,String> states=new java.util.HashMap<>();
        for(int i=0;i<old.length();i++){
            JSONObject v=old.optJSONObject(i);
            if(v!=null)states.put(v.optInt("number"),v.optString("state"));
        }
        int changed=0;
        for(int i=0;i<now.length();i++){
            JSONObject v=now.optJSONObject(i);
            if(v!=null){String prior=states.get(v.optInt("number"));
                if(prior!=null&&!prior.equals(v.optString("state")))changed++;}
        }
        if(changed>0)report.append("\nСтатус изменён у ").append(changed).append(" ").append(what)
                .append(" (из доступной выборки; не доказательство работы ПО).");
    }

    private static java.util.Map<String,String> branchHeads(JSONArray list){
        java.util.Map<String,String> m=new java.util.TreeMap<>();
        if(list==null)return m;
        for(int i=0;i<list.length();i++){
            JSONObject b=list.optJSONObject(i);
            if(b!=null)m.put(b.optString("name"),b.optString("sha"));
        }
        return m;
    }

    private void exportBackup(){
        if(current==null){toast("Сначала получите данные GitHub");return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,"gitteller-"+repo.replace('/','-')+".json");
        startActivityForResult(i,REQUEST_EXPORT);
    }

    private void writeBackup(Uri uri){
        try{
            JSONObject archive=new JSONObject();
            archive.put("format","GITTELLER_V1");
            archive.put("repo",repo);
            archive.put("current",current==null?JSONObject.NULL:SnapshotCodec.sanitizeSnapshot(current,repo));
            archive.put("previous",previous==null?JSONObject.NULL:SnapshotCodec.sanitizeSnapshot(previous,repo));
            byte[] bytes=archive.toString().getBytes(StandardCharsets.UTF_8);
            try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){
                if(out==null)throw new IllegalStateException("Не удаётся создать файл.");
                out.write(bytes);
            }
            toast("Архив сохранён без токена");
        }catch(Exception e){new AlertDialog.Builder(this).setTitle("Экспорт не выполнен")
                .setMessage(GithubClient.safeMessage(e)).setPositiveButton("Закрыть",(d,w)->{}).show();}
    }

    private void restoreBackup(JSONObject archive){
        String restoredRepo=archive.optString("repo");
        if(!restoredRepo.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}")||restoredRepo.contains(".."))
            throw new IllegalArgumentException("Недопустимое имя репозитория в архиве.");
        JSONObject fresh=archive.optJSONObject("current");
        JSONObject old=archive.optJSONObject("previous");
        if(fresh==null)throw new IllegalArgumentException("В архиве нет текущего снимка.");
        try{
            fresh=SnapshotCodec.sanitizeSnapshot(fresh,restoredRepo);
            // Imported snapshots are user-provided evidence, not authenticated live data.
            fresh.put("restoredFromBackup",true);
            if(old!=null){old=SnapshotCodec.sanitizeSnapshot(old,restoredRepo);old.put("restoredFromBackup",true);}
        }catch(Exception e){throw new IllegalArgumentException("Некорректный архив: "+GithubClient.safeMessage(e));}
        final JSONObject finalFresh=fresh, finalOld=old;
        new AlertDialog.Builder(this).setTitle("Восстановить из архива?")
                .setMessage("Архив "+restoredRepo+" заменит сохранённые данные этого репозитория. Текущие данные другого проекта не затронет.")
                .setNegativeButton("Отмена",(d,w)->{})
                .setPositiveButton("Восстановить",(d,w)->{
                    if(loading){toast("Дождитесь завершения синхронизации");return;}
                    if(!prefs.edit().putString(key(restoredRepo,"current"),finalFresh.toString())
                            .putString(key(restoredRepo,"previous"),finalOld==null?"":finalOld.toString())
                            .putString("repo",restoredRepo).commit()){
                        toast("Не удалось сохранить архив");return;
                    }
                    repo=restoredRepo;token="";current=finalFresh;previous=finalOld;draw();toast("Данные восстановлены из архива");
                }).show();
    }

    private void showInfo(){
        new AlertDialog.Builder(this).setTitle("GitTeller 1.0.2")
                .setMessage("Нативное Android-приложение без WebView.\n\n"+
                        "Читает открытые данные GitHub бесплатно, для приватных нужен read-only токен. " +
                        "Ключ остаётся в памяти и очищается при закрытии процесса.\n\n"+
                        "Ничего не меняет в GitHub. Не проверяет фактическую готовность ПО.")
                .setPositiveButton("Закрыть",(d,w)->{}).show();
    }
    private void importLegacy(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});
        startActivityForResult(i,REQUEST_IMPORT);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null)return;
        if(requestCode==REQUEST_EXPORT){writeBackup(data.getData());return;}
        if(requestCode!=REQUEST_IMPORT)return;
        Uri uri=data.getData();
        try(InputStream in=getContentResolver().openInputStream(uri)){
            if(in==null)throw new IllegalStateException("Не удаётся открыть файл.");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1){
                if(bytes.size()+n>5_000_000)throw new IllegalStateException("Архив слишком большой (5 МБ).");
                bytes.write(buffer,0,n);
            }
            String raw=bytes.toString(StandardCharsets.UTF_8.name());
            JSONObject object=new JSONObject(raw);
            if("GITTELLER_V1".equals(object.optString("format"))){restoreBackup(object);return;}
            if(!"UNO_CONTROL_V1".equals(object.optString("format")) || object.optJSONObject("state")==null)
                throw new IllegalArgumentException("Неизвестный формат резервной копии.");
            // Isolated archive. Do not replace a fresh GitHub snapshot with legacy claims.
            getSharedPreferences("uno_legacy_archive",MODE_PRIVATE).edit().putString("imported",raw).apply();
            JSONObject legacy=object.optJSONObject("state");
            int tasks=legacy.optJSONArray("tasks")==null?0:legacy.optJSONArray("tasks").length();
            new AlertDialog.Builder(this).setTitle("Архив сохранён отдельно")
                    .setMessage("Импортированы данные v1.2, включая "+tasks+" локальных задач. " +
                            "Текущая сверка GitHub не заменена архивом. Редактирование старого архива пока не реализовано.")
                    .setPositiveButton("Закрыть",(d,w)->{}).show();
        }catch(Exception e){
            new AlertDialog.Builder(this).setTitle("Импорт не выполнен")
                    .setMessage(GithubClient.safeMessage(e))
                    .setPositiveButton("Закрыть",(d,w)->{}).show();
        }
    }
    private void copy(String s){
        ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("SHA",s));
        toast("SHA скопирован");
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();token="";}
}
