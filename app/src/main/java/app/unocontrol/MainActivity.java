package app.unocontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Real Android views. No HTML, CSS or WebView. */
public final class MainActivity extends Activity {
    private static final String DEFAULT_REPO = "octocat/Hello-World";
    private static final int BG = Color.rgb(12,18,25);
    private static final int PANEL = Color.rgb(23,33,44);
    private static final int PANEL_2 = Color.rgb(30,43,56);
    private static final int TEXT = Color.rgb(238,242,247);
    private static final int MUTED = Color.rgb(158,172,188);
    private static final int ACCENT = Color.rgb(218,174,127);
    private static final int WARN = Color.rgb(239,198,128);
    private static final int STROKE = Color.rgb(47,63,80);
    private static final int REQUEST_IMPORT = 1;

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
        prefs = getSharedPreferences("uno_control_native_v1", MODE_PRIVATE);
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
        TextView state=txt(loading?"● ЗАГРУЗКА":"● "+(current==null?"НЕ ПРОВЕРЕНО":"ДАННЫЕ GITHUB"),12,
                loading?WARN:current==null?MUTED:ACCENT,true);
        hero.addView(state,lp(-1,-2));
        addLine(hero,summary.heading,22,TEXT,true,20);
        addLine(hero,summary.description,15,MUTED,false,12);
        if(current!=null) {
            addLine(hero,"Последняя сверка: "+formatTime(current.optLong("capturedAt")),12,MUTED,false,18);
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
        space(wrap,26);

        addLine(wrap,"На что обратить внимание",18,TEXT,true,0);
        space(wrap,13);
        LinearLayout attention=panel(wrap);
        addLine(attention,summary.attention,14,MUTED,false,0);

        if(current != null) {
            addLine(wrap,"Коротко об изменениях",18,TEXT,true,12);
            space(wrap,13);
            LinearLayout changes=panel(wrap);
            JSONArray cs=current.optJSONArray("commits");
            int count=cs==null?0:cs.length();
            if(count==0) addLine(changes,"Коммиты не загружены.",14,MUTED,false,0);
            else {
                addLine(changes,"Последние сообщения разработчиков",12,ACCENT,true,0);
                int n=Math.min(count,3);
                for(int i=0;i<n;i++){
                    JSONObject c=cs.optJSONObject(i);
                    String msg=c==null?"":c.optString("message", "Без описания");
                    if(msg.length()>125)msg=msg.substring(0,122)+"…";
                    addLine(changes,"•  "+msg,14,TEXT,false,i==0?12:10);
                }
                addLine(changes,"Это формулировки из коммитов, не проверенные результаты.",12,MUTED,false,17);
            }
            JSONArray warnings=current.optJSONArray("warnings");
            if(warnings!=null&&warnings.length()>0){
                space(wrap,8);
                LinearLayout warnCard=panel(wrap);
                addLine(warnCard,"Неполные данные",14,WARN,true,0);
                addLine(warnCard,warnings.length()+" ограничений при синхронизации. Подробности доступны в меню.",13,MUTED,false,8);
            }
        }
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
        pm.getMenu().add(0,6,5,"Импорт архива v1.2");
        pm.getMenu().add(0,7,6,"О приложении");
        pm.setOnMenuItemClickListener(item->{
            switch(item.getItemId()) {
                case 1: setupDialog();return true;
                case 2: listDialog("Ветки проекта", "branches", true);return true;
                case 3: listDialog("Последние коммиты", "commits", false);return true;
                case 4: detailsDialog();return true;
                case 5: warningsDialog();return true;
                case 6: importLegacy();return true;
                case 7: showInfo();return true;
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
                    if(!newToken.isEmpty())token=newToken;
                    if(!repo.equalsIgnoreCase(r)){
                        repo=r;
                        current=parse(prefs.getString(key(repo,"current"),""));
                        previous=parse(prefs.getString(key(repo,"previous"),""));
                        prefs.edit().putString("repo",repo).apply();
                    }
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
                    if(isFinishing()||!selected.equals(repo))return;
                    previous=current;
                    current=fresh;
                    prefs.edit().putString(key(repo,"previous"),previous==null?"":previous.toString())
                            .putString(key(repo,"current"),current.toString()).apply();
                    loading=false;draw();toast("Данные обновлены");
                });
            }catch(Exception e){
                final String message=GithubClient.safeMessage(e);
                runOnUiThread(()->{
                    if(isFinishing())return;
                    loading=false;draw();
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
                        .setMessage("Контрольный SHA:\n"+sha+"\n\nЭто запись GitHub, а не доказательство готовности.")
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
    private void showInfo(){
        new AlertDialog.Builder(this).setTitle("UNO Control 1.3")
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
        if(requestCode!=REQUEST_IMPORT || resultCode!=RESULT_OK || data==null || data.getData()==null)return;
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
            if(!"UNO_CONTROL_V1".equals(object.optString("format")) || object.optJSONObject("state")==null)
                throw new IllegalArgumentException("Не является архивом UNO Control v1.2.");
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
