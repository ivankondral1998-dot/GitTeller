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
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/** Dedicated, native, full-screen project sections. No AlertDialog used for data lists. */
public final class SectionActivity extends Activity {
    private int bg, panel, ink, muted, stroke, accent;
    private LinearLayout body;
    private SharedPreferences prefs;
    private JSONObject current, previous;
    private String repo, page, seenCurrent;
    private TokenVault vault;
    private int detailEpoch;
    private int compareEpoch;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("gitteller_v1",MODE_PRIVATE);
        repo=prefs.getString("repo","octocat/Hello-World");
        seenCurrent=prefs.getString(key(repo,"current"),"");
        current=parse(seenCurrent);
        previous=parse(prefs.getString(key(repo,"previous"),""));
        vault=new TokenVault(this);
        page=getIntent().getStringExtra("page");
        if(page==null)page="index";
        if("access".equals(page) || "search".equals(page))
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        paint();render();
    }
    @Override protected void onResume() {
        super.onResume();
        if(prefs==null) return;
        String selected=prefs.getString("repo",repo);
        String raw=prefs.getString(key(selected,"current"),"");
        if(selected.equals(repo) && raw.equals(seenCurrent==null?"":seenCurrent)) return;
        repo=selected;
        seenCurrent=raw;
        current=parse(raw);
        previous=parse(prefs.getString(key(repo,"previous"),""));
        paint();render();
    }
    private static String key(String repo,String suffix) {return repo.toLowerCase(java.util.Locale.ROOT)+":"+suffix;}
    private static JSONObject parse(String s) {try{return s==null||s.isEmpty()?null:new JSONObject(s);}catch(Exception e){return null;}}
    private int dp(float v) {return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void paint() {
        boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_NO;
        bg=dark?Color.rgb(12,18,25):Color.rgb(246,243,236);
        panel=dark?Color.rgb(23,33,44):Color.WHITE;
        ink=dark?Color.rgb(238,242,247):Color.rgb(28,32,38);
        muted=dark?Color.rgb(164,178,194):Color.rgb(92,99,108);
        stroke=dark?Color.rgb(47,63,80):Color.rgb(214,206,194);
        accent=dark?Color.rgb(218,174,127):Color.rgb(133,88,42);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    private GradientDrawable background(int color) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(17));d.setStroke(dp(1),stroke);return d;
    }
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int width,int height){
        return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));
    }
    private TextView text(String v,int size,boolean strong,int color) {
        TextView t=new TextView(this);t.setText(v);t.setTextSize(size);t.setTextColor(color);
        t.setIncludeFontPadding(false);t.setLineSpacing(dp(2),1f);
        if(strong)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;
    }
    private void gap(LinearLayout l,int height){l.addView(new View(this),lp(1,height));}
    private TextView line(LinearLayout l,String v,int size,boolean strong,int color) {
        TextView t=text(v,size,strong,color);l.addView(t,lp(-1,-2));return t;
    }
    private LinearLayout card(){
        LinearLayout l=col();l.setPadding(dp(14),dp(12),dp(14),dp(12));
        l.setBackground(background(panel));LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(8);
        body.addView(l,p);return l;
    }
    private void note(String title,String description) {
        LinearLayout card=card();line(card,title,15,true,ink);gap(card,4);line(card,description,14,false,muted);
    }
    private void action(String title,String subtitle,Runnable run) {
        LinearLayout b=card();
        TextView titleView=line(b,title+"   ›",15,true,ink);
        titleView.setMaxLines(2);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        gap(b,4);
        TextView sub=line(b,subtitle,13,false,muted);
        sub.setMaxLines(3);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        b.setContentDescription(title+". "+subtitle);b.setClickable(true);b.setFocusable(true);b.setOnClickListener(v->run.run());
    }
    private void go(String name){Intent i=new Intent(this,SectionActivity.class);i.putExtra("page",name);startActivity(i);}
    private void goItem(String name,int idx){Intent i=new Intent(this,SectionActivity.class);i.putExtra("page",name);i.putExtra("idx",idx);startActivity(i);}
    private String title() {
        switch(page){
            case "changes":return "Изменения";
            case "problems":return "Проблемы";
            case "branches":return "Направления работы";
            case "commits":return "История изменений";
            case "tasks":return "Задачи";
            case "pulls":return "Предложения изменений";
            case "accuracy":return "Надёжность сведений";
            case "access":return "Подключение GitHub";
            case "settings":return "Настройки";
            case "why":return "Почему это не доказательство";
            case "incomplete":return "Почему данные неполные?";
            case "search":return "Поиск";
            case "projects":return "Сменить проект";
            case "about":return "О приложении";
            case "guide":return "Как пользоваться";
            case "token-help":return "Как получить токен";
            case "data":return "Настройки";
            case "branch-detail":return "Направление работы";
            case "issue-detail":return "Задача";
            case "pull-detail":return "Предложение изменений";
            case "commit-detail":return "Изменение";
            default:return "Разделы";
        }
    }
    private void render(){
        LinearLayout root=col();root.setBackgroundColor(bg);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12),dp(14),dp(12),dp(12));
        TextView back=text("‹",22,false,ink);back.setGravity(Gravity.CENTER);
        back.setContentDescription("Назад");back.setClickable(true);back.setFocusable(true);
        back.setOnClickListener(v->finish());header.addView(back,lp(48,48));
        TextView h=text(title(),18,true,ink);h.setPadding(dp(8),0,0,0);
        header.addView(h,new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(header,lp(-1,-2));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        body=col();body.setPadding(dp(16),dp(4),dp(16),dp(20));scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));setContentView(root);
        TextView project=line(body,repo,13,false,muted);project.setMaxLines(1);project.setEllipsize(TextUtils.TruncateAt.END);gap(body,8);
        switch(page) {
            case "overview":overview();break;
            case "changes":changes();break;
            case "problems":problems();break;
            case "branches":branches();break;
            case "commits":commits();break;
            case "tasks":tasks();break;
            case "pulls":pulls();break;
            case "accuracy":accuracy();break;
            case "access":access();break;
            case "settings":case "data":settings();break;
            case "about":about();break;
            case "guide":guide();break;
            case "token-help":tokenHelp();break;
            case "why":why();break;
            case "search":search();break;
            case "incomplete":incomplete();break;
            case "projects":projects();break;
            case "branch-detail":branchDetail();break;
            case "commit-detail":commitDetail();break;
            case "issue-detail":case "pull-detail":taskDetail();break;
            default:index();
        }
    }
    private void index(){
        line(body,"Разделы",22,true,ink);gap(body,8);
        line(body, UiMode.DEVELOPER.equals(UiMode.normalize(prefs.getString(UiMode.PREF,UiMode.NOVICE)))
                ? "Технические разделы используют те же снимки и ограничения, что и режим чайника. Системный Back возвращает назад."
                : "Простые объяснения без скрытия неизвестного. Технические подробности доступны по запросу. Системный Back возвращает назад.",14,false,muted);
        gap(body,14);
        action("Обзор","Что изменилось, где работа, какие проблемы, кто выполнял",()->go("overview"));
        action("Изменения","Каждое событие: что, где, откуда известно",()->go("changes"));
        action("Направления работы","Где сдвинулась точка работы",()->go("branches"));
        action("Проблемы","Проверки, похожие на дефект названия и обычные задачи — отдельно",()->go("problems"));
        action("История изменений","Записи выбранного направления, не весь проект сразу",()->go("commits"));
        action("Задачи","Открытые и закрытые обсуждения. Не каждая задача — ошибка",()->go("tasks"));
        action("Предложения изменений","Что предлагают внести и внесено ли это",()->go("pulls"));
        action("Надёжность сведений","Что загружено полностью, а что нет",()->go("accuracy"));
        action("Подключение GitHub","Репозиторий и доступ только на чтение",()->go("access"));
        action("Настройки","Копии данных и ограничения приложения",()->go("settings"));
        action("Поиск","По сохранённым данным этого проекта",()->go("search"));
        action("Почему данные неполные?","Причина по каждому списку, без ложного успеха",()->go("incomplete"));
    }
    private void overview(){
        if(current==null){note("Нет данных","Вернитесь назад и нажмите «Обновить».");return;}
        Summary s=Summary.of(current,previous);
        note("Что изменилось", HumanReport.ownerChanged(current,previous,s));
        note("Где работа", HumanReport.ownerWhere(current,previous));
        note("Какие проблемы", HumanReport.ownerProblems(current));
        note("Кто выполнял", HumanReport.ownerWho(current));
        action("Открыть изменения","Каждое событие отдельно",()->go("changes"));
        action("Открыть направления","Где именно сдвинулась точка работы",()->go("branches"));
        action("Открыть проблемы","Ошибки, задачи и неизвестное — отдельно",()->go("problems"));
    }
    private void branches(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        JSONArray list=current.optJSONArray("branches");
        note("Что это", "Направление — отдельная линия работы. Название только подсказывает тему. Оно не доказывает, что результат готов.");
        if(!current.optBoolean("branchesComplete",false))
            note("Покрытие", "Указатели веток проверены не полностью. Нельзя делать вывод о новых или исчезнувших направлениях.");
        else note("Покрытие", "Указатели веток проверены полностью. История каждого направления при этом изучена лишь частично.");
        if(list==null||list.length()==0){note("Список пуст","GitHub не вернул направления, либо они не загрузились.");return;}
        for(int i=0;i<list.length();i++){
            JSONObject x=list.optJSONObject(i);if(x==null)continue;final int idx=i;
            String name=x.optString("name");
            action(name.isEmpty()?"Без названия":name, HumanReport.branchPointerLine(name, current, previous), ()->goItem("branch-detail",idx));
        }
    }
    private void branchDetail(){
        JSONObject x=at("branches");if(x==null){note("Запись не найдена","Вернитесь к списку.");return;}
        String name=x.optString("name"),sha=x.optString("sha");
        note("Что произошло", "Открыто направление «"+name+"». "+HumanReport.branchAssumption(name));
        note("Что известно", branchDelta(name));
        note("Что остаётся неизвестным", "По названию и указателю нельзя узнать, прошли ли проверки и можно ли этим пользоваться. История ниже — только последние записи, не вся работа.");
        note("Источник", sourceLine());
        technical("Направление\n"+name+"\nУказатель\n"+sha);
        String prior = HumanReport.previousBranchSha(name, current, previous);
        if(prior != null && !prior.isEmpty() && !sha.isEmpty() && !prior.equals(sha)) loadCompare(prior, sha);
        loadBranchStory(name);
    }
    private String branchDelta(String name) {
        return HumanReport.branchPointerLine(name, current, previous);
    }
    private void loadCompare(String base, String head) {
        final int epoch = ++compareEpoch;
        LinearLayout slot = card();
        TextView status = line(slot, HumanReport.NOT_ENOUGH+". Сравнение указателей ещё не получено.", 15, false, muted);
        final String selected = repo;
        new Thread(() -> {
            String text;
            try {
                String token = vault.load(selected);
                GithubClient client = new GithubClient(token == null ? "" : token);
                text = "Что произошло: указатели направления различаются.\nОткуда известно: "+client.readCompare(selected, base, head)
                        +"\nЧто не проверено: тесты и готовность на Android.";
            } catch (Exception e) {
                text = HumanReport.NOT_ENOUGH+". "+GithubClient.safeMessage(e);
            }
            final String shown = text;
            runOnUiThread(() -> {
                if (isFinishing() || epoch != compareEpoch) return;
                status.setText(shown);
            });
        }, "gitteller-compare").start();
    }
    private void changes() {
        String filter = getIntent().getStringExtra("filter");
        if(filter==null) filter="all";
        line(body,"Что это",16,true,ink);gap(body,6);
        line(body,"Каждая строка — разница двух сохранённых проверок. Это не оценка готовности.",14,false,muted);
        gap(body,12);
        filterChip("Все","all",filter);
        filterChip("Новое","new",filter);
        filterChip("Проверки","checks",filter);
        filterChip("Задачи","tasks",filter);
        filterChip("Слияния","merges",filter);
        gap(body,8);
        JSONArray log = parseFeed();
        int shown=0;
        for(int i=0;i<log.length();i++){
            JSONObject event=log.optJSONObject(i);
            if(event==null || !matches(filter, event.optString("kind"))) continue;
            shown++;
            String happened=event.optString("event");
            if(happened.isEmpty()) happened="Запись без текста.";
            String bodyText="Что произошло: "+happened
                    +"\nГде: "+HumanReport.eventWhere(event)
                    +"\nОткуда известно: "+event.optString("origin","LIVE")+", "+HumanReport.humanDate(event.optLong("at",0))
                    +". "+event.optString("basis")
                    +"\nЧто это значит: "+HumanReport.eventMeaning(event.optString("kind"))
                    +"\n"+HumanReport.unknownOnce(event.optString("unknown"));
            String focus=getIntent().getStringExtra("eventId");
            if(focus!=null && !focus.isEmpty() && focus.equals(event.optString("id")))
                note("Открыто из поиска", bodyText);
            else note(happened, bodyText);
            String url=event.optString("url");
            if(url.startsWith("https://github.com/")) technical("Ссылка\n"+url+"\nИдентификатор\n"+event.optString("id"), url);
        }
        if(shown==0) note("Событий нет", "В этой ленте нет записей выбранного вида. Это не значит, что проект не менялся и что проблем нет.");
    }
    private void filterChip(String label, String id, String currentFilter) {
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(label+(id.equals(currentFilter)?" · сейчас":""));
        b.setTextColor(ink);
        b.setBackground(background(id.equals(currentFilter)?accent:panel));
        b.setContentDescription("Фильтр "+label);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(6);
        b.setMinHeight(dp(48));
        b.setPadding(dp(12),dp(8),dp(12),dp(8));
        body.addView(b,p);
        b.setOnClickListener(v->{
            Intent i=new Intent(this, SectionActivity.class);
            i.putExtra("page","changes");
            i.putExtra("filter", id);
            startActivity(i);
            finish();
        });
    }
    private static boolean matches(String filter, String kind) {
        if(filter==null || "all".equals(filter)) return true;
        if("new".equals(filter)) return kind.endsWith("_new") || "branch_seen".equals(kind);
        if("checks".equals(filter)) return kind.startsWith("check");
        if("tasks".equals(filter)) return kind.startsWith("issue");
        if("merges".equals(filter)) return "pull_merged".equals(kind);
        return true;
    }
    private JSONArray parseFeed() {
        try {
            String raw=prefs.getString(key(repo,"feed"),"");
            return raw==null || raw.isEmpty() ? new JSONArray() : new JSONArray(raw);
        } catch (Exception e) { return new JSONArray(); }
    }
    private void loadBranchStory(String branch) {
        final int epoch = ++detailEpoch;
        LinearLayout slot = card();
        TextView status = line(slot, "История этого направления ещё не запрошена. Это не вся история проекта.", 15, false, muted);
        final String selected = repo;
        new Thread(() -> {
            String text;
            try {
                String token = vault.load(selected);
                GithubClient client = new GithubClient(token == null ? "" : token);
                org.json.JSONArray commits = client.readBranchCommits(selected, branch);
                if (commits.length() == 0) text = HumanReport.NOT_ENOUGH + ". История этого направления пуста или не прочитана.";
                else {
                    StringBuilder b = new StringBuilder("Последние записи только этого направления. История изучена лишь частично.\n");
                    int n = Math.min(commits.length(), 5);
                    for (int i = 0; i < n; i++) {
                        org.json.JSONObject c = commits.optJSONObject(i);
                        if (c == null) continue;
                        b.append("\n").append(i + 1).append(". ").append(HumanReport.commitSummary(c.optString("message")));
                        b.append("\n").append(HumanReport.humanDate(c.optString("date")));
                        b.append("\nЗаявление автора: ").append(c.optString("message"));
                    }
                    b.append("\n\n").append(HumanReport.NO_CHECK);
                    text = b.toString();
                }
            } catch (Exception e) {
                text = HumanReport.NOT_ENOUGH + ". " + GithubClient.safeMessage(e);
            }
            final String shown = text;
            runOnUiThread(() -> {
                if (isFinishing() || epoch != detailEpoch) return;
                status.setText(shown);
            });
        }, "gitteller-branch").start();
    }
    private void commits(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        String branch=current.optString("defaultBranch","main");
        note("Что здесь", "Ниже — только последние записи основной ветки «"+branch+"». Это не последнее событие всего проекта. Другие направления открываются отдельно.");
        note("Покрытие", "История изучена лишь частично.");
        JSONArray a=current.optJSONArray("commits");
        if(a==null||a.length()==0) note("Нет записей","Список основной ветки не загрузился или пуст. Это не значит, что в других направлениях пусто.");
        else for(int i=0;i<a.length();i++){
            JSONObject x=a.optJSONObject(i);if(x==null)continue;final int idx=i;
            action(HumanReport.commitSummary(x.optString("message")),
                    HumanReport.humanDate(x.optString("date"))+". Заявление автора, не проверка.",
                    ()->goItem("commit-detail",idx));
        }
        gap(body,8);
        line(body,"Другие направления",16,true,ink);gap(body,8);
        action("Все направления","В том числе research и другие линии работы",()->go("branches"));
    }
    private void commitDetail(){
        JSONObject x=at("commits");if(x==null){note("Запись не найдена","Вернитесь к списку.");return;}
        String message=x.optString("message","Без описания");
        note("Что произошло", HumanReport.commitSummary(message)+".");
        note("Что заявил автор", message.isEmpty()?"Описания нет.":message);
        note("Что известно до отдельного запроса", "Запись есть в ветке «"+current.optString("defaultBranch","main")+"». Дата для человека: "+HumanReport.humanDate(x.optString("date"))+". Список файлов и проверки подгружаются ниже, если GitHub их отдаёт.");
        note("Что остаётся неизвестным", "Заголовок не доказывает, что изменение работает. Если ниже нет списка файлов или проверок — этих доказательств нет.");
        note("Источник", sourceLine());
        technical("Заголовок\n"+message+"\nКогда сохранено (как прислал GitHub)\n"+x.optString("date","нет")+"\nУказатель\n"+x.optString("sha","нет"));
        loadExtra("commit", 0, x.optString("sha"));
    }
    private void tasks(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        JSONArray issues=current.optJSONArray("issues");
        if(!current.optBoolean("issuesComplete", false))
            note("Покрытие", current.optBoolean("issuesStale", false)
                    ? "Свежий список задач не получен. Ниже прошлые сведения, если они были. Нельзя сказать, сколько задач всего."
                    : "Список задач неполный или не загрузился. Нельзя сказать, что открытых задач нет.");
        else note("Покрытие", "Список задач в пределах запроса загружен. Это не все возможные проблемы программы.");
        if(issues==null || issues.length()==0){
            note("Записей в этой выборке нет", current.optBoolean("issuesComplete", false)
                    ? "GitHub вернул пустой список. Это не доказательство, что программа без дефектов."
                    : "Пустой ответ при неполной загрузке не означает ноль задач.");
            return;
        }
        for(int i=0;i<issues.length();i++){
            JSONObject x=issues.optJSONObject(i);if(x==null)continue;final int idx=i;
            String title=x.optString("title","Без названия");
            String kind=HumanReport.problemKind(title, x.optString("labels"));
            String hint=HumanReport.state(x.optString("state"));
            if("open".equals(x.optString("state"))) {
                if("defect".equals(kind)) hint=hint+" По названию похоже на дефект, это не проверка.";
                else if("blocker".equals(kind)) hint=hint+" По названию похоже на блокировку, это не проверка.";
                else hint=hint+" Обычная задача, не подтверждённая ошибка.";
            }
            action(title, hint, ()->goItem("issue-detail",idx));
        }
    }
    private void pulls(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        JSONArray pulls=current.optJSONArray("pulls");
        if(!current.optBoolean("pullsComplete", false))
            note("Покрытие", current.optBoolean("pullsStale", false)
                    ? "Свежий список предложений не получен. Показаны прошлые сведения, если они были."
                    : "Список предложений неполный. Закрытие в видимой части не описывает весь проект.");
        else note("Покрытие", "Список предложений в пределах запроса загружен. Закрытие не означает внесение в проект.");
        if(pulls==null || pulls.length()==0){
            note("Записей в этой выборке нет", current.optBoolean("pullsComplete", false)
                    ? "GitHub вернул пустой список предложений."
                    : "Пустой ответ при неполной загрузке не означает, что предложений нет.");
            return;
        }
        for(int i=0;i<pulls.length();i++){
            JSONObject x=pulls.optJSONObject(i);if(x==null)continue;final int idx=i;
            action(x.optString("title","Без названия"), HumanReport.pullListLabel(x), ()->goItem("pull-detail",idx));
        }
    }
    private void taskDetail(){
        boolean pr="pull-detail".equals(page);
        JSONObject x=at(pr?"pulls":"issues");if(x==null){note("Запись не найдена","Вернитесь к списку.");return;}
        String title=x.optString("title","Без названия");
        note("Что произошло", (pr?"Предложение: «":"Задача: «")+title+"».");
        note("Что известно", pr ? HumanReport.pullListLabel(x) : HumanReport.state(x.optString("state")));
        note("Что остаётся неизвестным", pr
                ? "Пока нет отдельного ответа GitHub, нельзя утверждать, что предложение слито, какие файлы затронуты и прошли ли проверки."
                : "По заголовку нельзя установить, выполнена ли работа.");
        note("Источник", sourceLine());
        technical((pr?"Предложение №":"Задача №")+x.optInt("number")+"\nСостояние в GitHub: "+x.optString("state")
                +(pr?"\nОтметка внесения: "+(x.optBoolean("merged", false)?"есть":"нет"):"")
                +"\nЗаголовок\n"+title,
                GithubLinks.recordUrl(repo, pr ? "pull" : "issues", x.optInt("number")));
        loadExtra(pr ? "pull" : "issue", x.optInt("number"), null);
    }
    private void loadExtra(String kind, int number, String sha) {
        final int epoch = ++detailEpoch;
        LinearLayout slot = card();
        TextView status = line(slot, "Подробности ещё читаются из GitHub.", 15, false, muted);
        final String selected = repo;
        new Thread(() -> {
            String text;
            try {
                String token = vault.load(selected);
                GithubClient client = new GithubClient(token == null ? "" : token);
                GithubClient.Extra extra = "commit".equals(kind) ? client.readCommit(selected, sha)
                        : "pull".equals(kind) ? client.readPull(selected, number)
                        : client.readIssue(selected, number);
                text = "Подтверждения и пробелы\n\n" + extra.text;
            } catch (Exception e) {
                text = HumanReport.NOT_ENOUGH + ". " + GithubClient.safeMessage(e);
            }
            final String shown = text;
            runOnUiThread(() -> {
                if (isFinishing() || epoch != detailEpoch) return;
                status.setText(shown);
            });
        }, "gitteller-detail").start();
    }
    private JSONObject at(String field){
        JSONArray list=current==null?null:current.optJSONArray(field);
        if(list==null) return null;
        String key=getIntent().getStringExtra("key");
        if(key!=null && !key.isEmpty()){
            for(int i=0;i<list.length();i++){
                JSONObject row=list.optJSONObject(i);
                if(row==null) continue;
                if(key.equals(row.optString("name")) || key.equalsIgnoreCase(row.optString("sha"))
                        || key.equals(String.valueOf(row.optInt("number",-1)))) return row;
            }
            return null;
        }
        return list.optJSONObject(getIntent().getIntExtra("idx",-1));
    }
    private String sourceLine(){
        if(current==null) return "Источник ещё не получен.";
        String kind=SnapshotCodec.isImported(current)?"Импортированный файл, не свежий запрос GitHub."
                :GithubClient.isPartialSnapshot(current)?"Частичный ответ GitHub, не полная проверка."
                :"CACHED".equals(current.optString("origin"))?"GitHub подтвердил прежние списки. История при этом не проверена целиком."
                :"GitHub, только чтение.";
        return kind+" Проект: "+repo+". Загрузка: "+HumanReport.humanDate(current.optLong("capturedAt",0))+".";
    }
    private void technical(String value){technical(value, null);}
    private void technical(String value, String externalUrl){
        LinearLayout card=card();
        boolean developer=UiMode.DEVELOPER.equals(UiMode.normalize(prefs.getString(UiMode.PREF, UiMode.NOVICE)));
        line(card,developer ? "Технические сведения" : "Технические детали скрыты",13,true,muted);gap(card,8);
        if (!developer) {
            line(card,"Указатель и сырые поля не показаны, пока вы сами их не откроете.",14,false,muted);
            gap(card,10);
        }
        TextView hidden=line(card,value,13,false,muted);
        hidden.setVisibility(developer ? View.VISIBLE : View.GONE);
        if (!developer) button(card,"Показать технические сведения",()->{
            hidden.setVisibility(View.VISIBLE);
        });
        button(card,"Скопировать технические сведения",()->{
            ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard!=null) clipboard.setPrimaryClip(ClipData.newPlainText("GitTeller", value));
            toast("Скопировано");
        });
        String safeUrl=GithubLinks.validatedRepoUrl(repo, externalUrl);
        if(safeUrl!=null){
            button(card,"Открыть в GitHub",()->{
                try {
                    Intent view=new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(safeUrl));
                    startActivity(view);
                } catch (Exception e) { toast("Браузер для ссылки недоступен"); }
            });
            button(card,"Копировать только ссылку",()->{
                ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                if(clipboard!=null){
                    clipboard.setPrimaryClip(ClipData.newPlainText("GitHub URL", safeUrl));
                    toast("Ссылка скопирована");
                } else toast("Буфер обмена недоступен");
            });
        }
    }
    private void problems(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        note("Подтверждённые ошибки проверок",
                "В общем снимке их нет: автоматические проверки читаются только у конкретной записи. Пустое место здесь не означает, что проверки прошли.");
        action("Надёжность сведений","Что загружено и чего нет",()->go("accuracy"));
        boolean complete=current.optBoolean("issuesComplete", false);
        note("Список задач", complete
                ? "Список в пределах запроса загружен. Это не все возможные дефекты программы."
                : "Список неполный. Нельзя сказать, сколько проблем всего, и пустой ответ не означает отсутствие ошибок.");
        JSONArray issues=current.optJSONArray("issues");
        int defects=0, blockers=0, ordinary=0, shown=0;
        if(issues!=null) for(int i=0;i<issues.length();i++){
            JSONObject x=issues.optJSONObject(i);
            if(x==null || !"open".equals(x.optString("state"))) continue;
            shown++;
            String title=x.optString("title","Без названия");
            String kind=HumanReport.problemKind(title, x.optString("labels"));
            String hint;
            if("defect".equals(kind)){defects++; hint="По названию похоже на дефект. Это не проверка и не доказанная ошибка.";}
            else if("blocker".equals(kind)){blockers++; hint="По названию похоже на блокировку. Это не проверка.";}
            else {ordinary++; hint="Обычная открытая задача. Это не подтверждённая ошибка.";}
            final int idx=i;
            action(title, hint, ()->goItem("issue-detail", idx));
        }
        if(shown==0) note("Открытых задач в выборке нет", complete
                ? "Это не значит, что программа без дефектов."
                : "Неполная выборка не доказывает отсутствие задач.");
        else note("Как читать", "Похожих на дефект по названию: "+defects
                +". Похожих на блокировку: "+blockers
                +". Обычных открытых задач: "+ordinary
                +". Название и метка не являются подтверждённой ошибкой.");
        action("Все задачи","Включая закрытые. Закрытие не означает исправление.",()->go("tasks"));
        action("Предложения изменений","Закрытие не означает внесение в проект.",()->go("pulls"));
    }
    private void accuracy(){
        if(current==null){note("Нет данных","Ещё не было проверки GitHub.");return;}
        note("Источник", sourceLine());
        boolean developer=UiMode.DEVELOPER.equals(UiMode.normalize(prefs.getString(UiMode.PREF, UiMode.NOVICE)));
        for (Coverage.Row row : Coverage.explain(current)) {
            if ("snapshot".equals(row.area)) continue;
            note(row.title, row.shown(developer) + "\n" + row.meaning);
        }
        action("Почему данные неполные?","Статус, причина и следующее действие по каждому списку",()->go("incomplete"));
        JSONArray warnings=current.optJSONArray("warnings");
        if(warnings==null||warnings.length()==0) note("Замечания загрузки","Отдельных ошибок при последней загрузке не записано. Это не значит, что проект проверен.");
        else for(int i=0;i<warnings.length();i++) note("Записано при загрузке", warnings.optString(i));
    }
    private void access(){
        note("Безопасное подключение","Для публичного репозитория токен не нужен. Для приватного нужен токен GitHub только на чтение. Он хранится в Android Keystore отдельно для каждого проекта. Токен с правом записи не нужен и здесь не запрашивается.");
        button(card(),"Как получить токен","Что это и откуда его взять, без жаргона",()->go("token-help"));
        LinearLayout form=card();
        line(form,"Репозиторий GitHub",14,true,ink);gap(form,8);
        EditText repoField=new EditText(this);repoField.setSingleLine(true);repoField.setText(repo);
        repoField.setTextColor(ink);form.addView(repoField,lp(-1,-2));gap(form,15);
        String status;
        try{status=vault.hasToken(repo)?"На устройстве есть запись доступа. Она ещё не проверена расшифровкой, и секрет не показывается.":"Токен для текущего проекта не найден.";}
        catch(Exception e){status="Статус хранилища недоступен.";}
        line(form,status,14,false,muted);gap(form,12);
        line(form,"Токен. Пустое поле не удаляет сохранённый доступ.",13,false,muted);
        EditText tokenField=new EditText(this);tokenField.setSingleLine(true);tokenField.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenField.setTextColor(ink);tokenField.setHint("Вставьте токен вручную");tokenField.setHintTextColor(muted);
        form.addView(tokenField,lp(-1,-2));gap(form,14);
        button(form,"Сохранить подключение",()->{
            String r=repoField.getText().toString().trim();
            if(!r.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}")||r.contains("..")){toast("Адрес должен быть вида owner/repo");return;}
            String token=tokenField.getText().toString().trim();tokenField.setText("");
            try{
                boolean wrote=false;
                if(!token.isEmpty()){vault.save(r,token);wrote=true;}
                if(!prefs.edit().putString("repo",r).commit()){toast("Не удалось выбрать проект");return;}
                setResult(RESULT_OK,new Intent().putExtra("action","changed"));
                toast(wrote?"Доступ сохранён средствами Android":"Проект выбран. Пустое поле не стирает сохранённый доступ.");
                finish();
            }catch(Exception e){toast("Не удалось сохранить токен. Проверьте хранилище Android.");}
        });
        button(form,"Удалить токен этого проекта",()->{
            String r=repoField.getText().toString().trim();
            if(!r.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}")||r.contains("..")){toast("Неверный адрес");return;}
            new AlertDialog.Builder(this).setTitle("Удалить защищённый токен?")
            .setMessage("Будет удалён только токен проекта «"+r+"». Сведения проекта сохранятся.")
            .setNegativeButton("Отмена",(d,w)->{})
            .setPositiveButton("Удалить",(d,w)->{
                try{vault.delete(r);tokenField.setText("");toast("Токен удалён");setResult(RESULT_OK,new Intent().putExtra("action","changed"));finish();}
                catch(Exception e){toast("Не удалось удалить токен.");}
            }).show();
        });
    }
    private void button(LinearLayout box,String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(ink);b.setBackground(background(panel));
        LinearLayout.LayoutParams params=lp(-1,-2);params.topMargin=dp(9);
        b.setMinHeight(dp(48));
        b.setPadding(dp(12),dp(10),dp(12),dp(10));
        box.addView(b,params);b.setOnClickListener(v->action.run());
    }
    private void button(LinearLayout box,String label,String subtitle,Runnable action){
        if(subtitle!=null && !subtitle.isEmpty()){line(box,subtitle,14,false,muted);gap(box,6);}
        button(box,label,action);
    }
    private String sectionTime(String cacheFlag, String atKey) {
        String when = HumanReport.humanDate(current.optLong(atKey, 0));
        String cache = current.optBoolean(cacheFlag, false)
                ? " Первая страница не изменилась, показан прошлый список." : "";
        return " Время раздела: " + when + "." + cache;
    }
    private void settings(){
        note("GitTeller 1.0.18-dev", "Главный экран короче. Ответ нажимается целиком. Подробный режим в правой панели. Это не коммерческий релиз и не проверка готовности программы.");
        note("Реклама", "Реклама Яндекса. Баннер внизу. Большой ролик не чаще раза в час при «Обновить». Если реклама не загрузилась, обновление всё равно выполняется.");
        note("Режимы", "«Режим чайника» и «Режим разработчика» переключаются в правой панели. На главном по умолчанию простой вид: четыре ответа. Подробности не удалены, они в панели. Выбор сохраняется на устройстве. Права GitHub не меняются.");
        note("Оформление", "Светлая или тёмная тема берётся из настроек телефона. Отдельный переключатель не нужен.");
        note("Безопасность", "Чтение GitHub только через HTTPS. Токены в Android Keystore, отдельно на каждый проект, не попадают в резервную копию JSON и не пишутся в журнал.");
        note("Резервные копии", "В архив попадают только сведения GitHub. Защищённый токен не экспортируется. Импортированный файл нельзя считать свежей проверкой.");
        action("Сохранить копию проекта","Создать JSON без токена",()->requestData("export"));
        action("Восстановить из JSON","Заменяет данные выбранного репозитория только после подтверждения",()->requestData("import"));
        action("Поделиться кратким отчётом","Текст для себя. Токен и тела записей не входят", this::shareReport);
        action("Почему готовность нельзя доказать коммитом","Короткое объяснение",()->go("why"));
        note("Проекты на этом телефоне","Данные и токены не смешиваются. Выбор проекта не удаляет остальные.");
        projectChoices();
        note("Ограничения", "Приложение не запускает сборку и не проверяет работу на телефоне. Нехватка данных показывается как нехватка данных, а не как успех.");
    }
    private void about(){
        note("GitTeller 1.0.18-dev", "Нативное чтение GitHub. versionCode 34. Это не коммерческий релиз и не приёмка на телефоне.");
        note("Что умеет", "Два режима одних данных, поиск по уже загруженному, панель разделов и разбор, почему список неполный.");
        note("Чего не делает", "Не пишет в GitHub, не запускает тесты и не считает пустое место успехом.");
        action("Почему готовность нельзя доказать коммитом","Короткое объяснение",()->go("why"));
    }
    private void projects(){
        note("Проекты на этом телефоне","Список берётся из уже сохранённых снимков. Новый проект отсюда не придумывается.");
        projectChoices();
        action("Подключить другой адрес","Репозиторий и токен только на чтение",()->go("access"));
    }
    private void projectChoices(){
        boolean any=false;
        for (String stored : prefs.getAll().keySet()) {
            if (!stored.endsWith(":current")) continue;
            String name = stored.substring(0, stored.length() - ":current".length());
            if (name.isEmpty()) continue;
            any=true;
            action(name, name.equalsIgnoreCase(repo) ? "Сейчас открыт" : "Переключить на этот проект", () -> selectRepo(name));
        }
        if(!any) note("Список пуст","На устройстве ещё нет сохранённого снимка. Это не значит, что проектов нет на GitHub.");
    }
    private void selectRepo(String name){
        if (!prefs.edit().putString("repo", name).commit()) { toast("Не удалось выбрать проект"); return; }
        setResult(RESULT_OK, new Intent().putExtra("action", "changed"));
        finish();
    }
    private void incomplete(){
        boolean developer=UiMode.DEVELOPER.equals(UiMode.normalize(prefs.getString(UiMode.PREF, UiMode.NOVICE)));
        line(body, developer ? "Покрытие по полям снимка" : "Почему данные выглядят неполными", 18, true, ink);
        gap(body, 8);
        line(body, "Обновление не запускается само. Кнопка ниже только открывает действие, запрос уйдёт после «Обновить».", 14, false, muted);
        gap(body, 12);
        if(current==null){
            note("Снимка нет","Нажмите «Обновить» на главном экране. Пустой экран не означает пустой проект.");
            button(card(), "Обновить", () -> { setResult(RESULT_OK, new Intent().putExtra("action","sync")); finish(); });
            return;
        }
        note("Проект", repo + " · " + (SnapshotCodec.isImported(current) ? "IMPORTED" : current.optString("origin","не определён"))
                + " · " + HumanReport.humanDate(current.optLong("capturedAt",0)));
        for (Coverage.Row row : Coverage.explain(current)) {
            LinearLayout card=card();
            line(card, row.title, 17, true, ink); gap(card, 8);
            line(card, row.shown(developer), 15, false, ink); gap(card, 8);
            line(card, "Что известно: " + row.fact, 14, false, muted); gap(card, 6);
            line(card, "Что это значит: " + row.meaning, 14, false, muted);
            if (developer) {
                gap(card, 6);
                line(card, row.code + " · записей " + (row.count < 0 ? "нет массива" : String.valueOf(row.count))
                        + " · " + HumanReport.humanDate(row.at), 13, false, muted);
            }
            if ("refresh".equals(row.action))
                button(card, "Обновить", () -> { setResult(RESULT_OK, new Intent().putExtra("action","sync")); finish(); });
            else if ("access".equals(row.action))
                button(card, "Проверить доступ", () -> go("access"));
            else if ("commits".equals(row.action))
                button(card, "Открыть коммит", () -> go("commits"));
            else if (Coverage.SAMPLED_BY_DESIGN.equals(row.code))
                line(card, "Не требуется, это выборка.", 14, false, muted);
            else if (Coverage.NOT_VERIFIED.equals(row.code))
                line(card, "Отдельный запрос отсюда не докажет проверки. Откройте конкретную запись.", 14, false, muted);
            else line(card, "Дополнительный запрос для этого пункта не требуется.", 14, false, muted);
        }
    }
    private void search(){
        line(body, "Поиск по сохранённым данным", 18, true, ink); gap(body, 6);
        line(body, ProjectSearch.scope(current), 14, false, muted); gap(body, 10);
        LinearLayout bar=card();
        EditText field=new EditText(this);
        field.setSingleLine(true);
        field.setTextColor(ink);
        field.setHint("Ветка, #номер, SHA, текст");
        field.setHintTextColor(muted);
        String restored=getIntent().getStringExtra("query");
        if(restored!=null) field.setText(restored);
        field.setContentDescription("Строка поиска");
        bar.addView(field, lp(-1,-2));
        button(bar, "Очистить", () -> field.setText(""));
        LinearLayout results=col();
        body.addView(results, lp(-1,-2));
        Runnable apply=() -> {
            results.removeAllViews();
            String raw=field.getText()==null ? "" : field.getText().toString();
            getIntent().putExtra("query", raw);
            ProjectSearch.Report report=ProjectSearch.find(current, parseFeed(), raw, localRepos());
            if(report.emptyQuery){
                TextView hint=line(results, ProjectSearch.emptyHint(), 15, false, muted);
                hint.setPadding(0, dp(8), 0, 0);
                return;
            }
            if(report.hits.isEmpty()){
                line(results, ProjectSearch.noMatch(), 15, false, ink);
                return;
            }
            String group="";
            for (ProjectSearch.Hit hit : report.hits) {
                if(!hit.group.equals(group)){
                    group=hit.group;
                    gap(results, 8);
                    line(results, group, 13, true, muted);
                    gap(results, 6);
                }
                choice(results, hit.title, hit.detail.isEmpty() ? hit.group : hit.detail, () -> openHit(hit));
            }
            if(report.omitted>0){
                gap(results, 8);
                line(results, "Ещё "+report.omitted+" совпадений не показаны. Уточните запрос. Это не потерянные данные GitHub.", 14, false, muted);
            }
        };
        field.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { apply.run(); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        apply.run();
    }
    private void choice(LinearLayout host, String title, String subtitle, Runnable run) {
        LinearLayout b=col();
        b.setPadding(dp(17),dp(14),dp(17),dp(14));
        b.setBackground(background(panel));
        b.setMinimumHeight(dp(48));
        line(b,title+"   ›",17,true,ink);gap(b,7);line(b,subtitle,14,false,muted);
        b.setContentDescription(title+". "+subtitle);b.setClickable(true);b.setFocusable(true);b.setOnClickListener(v->run.run());
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(10);
        host.addView(b,p);
    }
    private java.util.List<String> localRepos(){
        java.util.ArrayList<String> out=new java.util.ArrayList<>();
        for (String stored : prefs.getAll().keySet()) {
            if (!stored.endsWith(":current")) continue;
            String name=stored.substring(0, stored.length()-":current".length());
            if(!name.isEmpty()) out.add(name);
        }
        return out;
    }
    private void openHit(ProjectSearch.Hit hit){
        if("switch-repo".equals(hit.page)){ selectRepo(hit.key); return; }
        Intent i=new Intent(this, SectionActivity.class);
        i.putExtra("page", hit.page);
        if(hit.index>=0) i.putExtra("idx", hit.index);
        if(hit.key!=null && !hit.key.isEmpty()) i.putExtra("key", hit.key);
        if(hit.marker!=null && !hit.marker.isEmpty()) i.putExtra("eventId", hit.marker);
        startActivity(i);
    }
    private void guide(){
        note("Что это","GitTeller смотрит за проектом на GitHub. GitHub — место, где лежит код и заметки людей или агентов, которые над ним работают. Приложение ничего туда не пишет.");
        note("Что смотреть на главном","Четыре ответа: что изменилось, где работа, какие проблемы, кто выполнял. Это сведения из GitHub, не проверка, что программа работает.");
        note("Когда нужен токен","Публичный проект читается без токена. Приватный — только с ключом на чтение. Ключ хранится на этом телефоне и никуда не отправляется.");
        action("Как получить токен","Пошагово, без жаргона",()->go("token-help"));
        note("Чего приложение не делает","Не пишет в репозиторий, не запускает тесты, не доказывает готовность кода и не считает пустой список успехом.");
    }
    private void tokenHelp(){
        note("Что такое токен","Это как пароль только на чтение. С ним приложение видит приватный проект. Без права записи оно не может ничего изменить.");
        note("Когда он не нужен","Если проект открыт всем (публичный), поле токена можно оставить пустым. Достаточно адреса вида owner/repo.");
        note("Как получить","1. Откройте github.com в браузере и войдите.\n2. Нажмите на аватар → Settings.\n3. Внизу слева: Developer settings.\n4. Personal access tokens → Fine-grained tokens.\n5. Generate new token. Доступ: только нужный репозиторий.\n6. Права: Contents = Read, Metadata = Read, Issues = Read, Pull requests = Read. Запись не включайте.\n7. Generate. Скопируйте токен сразу: GitHub показывает его один раз.\n8. Вставьте в поле на экране подключения и сохраните.");
        note("Важно","Никому не отправляйте токен, даже создателю приложения. Он остаётся только на этом телефоне, в защищённом хранилище Android.");
        button(card(),"Открыть настройки токенов GitHub","Точная страница fine-grained токенов", this::openTokenSettings);
    }
    private void openTokenSettings(){
        final String url = "https://github.com/settings/tokens?type=beta";
        if (!"https://github.com/settings/tokens?type=beta".equals(url)) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception e) { toast("Браузер недоступен"); }
    }
    private void why(){
        note("Событие", "Коммит, задача или сдвиг ветки говорят только о том, что запись появилась в GitHub.");
        note("Основание", "Основанием служит поле источника: state, merged_at, SHA или список файлов, если он загружен.");
        note("Не подтверждено", "Название ветки, префикс fix: и пометка skip ci не доказывают, что функция работает и что приёмка на телефоне пройдена.");
        note("Подробнее", "Технические поля и ссылка на github.com этого репозитория лежат внизу карточки. Их отсутствие означает «не проверено», а не «всё хорошо».");
    }
    private void shareReport(){
        Summary s=Summary.of(current, previous);
        String text="GitTeller\nПроект: "+repo+"\n"+HumanReport.ownerStatus(current, previous, s)
                +"\n\nЧто изменилось:\n"+HumanReport.ownerChanged(current, previous, s)
                +"\n\nГде работа:\n"+HumanReport.ownerWhere(current, previous)
                +"\n\nКакие проблемы:\n"+HumanReport.ownerProblems(current)
                +"\n\nКто выполнял:\n"+HumanReport.ownerWho(current)
                +"\n\nЭто не проверка работоспособности и не отчёт о готовности.";
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Краткий отчёт"));
    }
    private void requestData(String actionName){setResult(RESULT_OK,new Intent().putExtra("action",actionName));finish();}
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
}
