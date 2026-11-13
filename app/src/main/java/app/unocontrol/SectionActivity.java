package app.unocontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
    private String repo, page;
    private TokenVault vault;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("gitteller_v1",MODE_PRIVATE);
        repo=prefs.getString("repo","octocat/Hello-World");
        current=parse(prefs.getString(key(repo,"current"),""));
        previous=parse(prefs.getString(key(repo,"previous"),""));
        vault=new TokenVault(this);
        page=getIntent().getStringExtra("page");
        if(page==null)page="index";
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
        LinearLayout l=col();l.setPadding(dp(17),dp(18),dp(17),dp(18));
        l.setBackground(background(panel));LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(13);
        body.addView(l,p);return l;
    }
    private void note(String title,String description) {
        LinearLayout card=card();line(card,title,17,true,ink);gap(card,10);line(card,description,15,false,muted);
    }
    private void action(String title,String subtitle,Runnable run) {
        LinearLayout b=card();line(b,title+"   ›",17,true,ink);gap(b,7);line(b,subtitle,14,false,muted);
        b.setContentDescription(title+". "+subtitle);b.setClickable(true);b.setFocusable(true);b.setOnClickListener(v->run.run());
    }
    private void go(String name){Intent i=new Intent(this,SectionActivity.class);i.putExtra("page",name);startActivity(i);}
    private void goItem(String name,int idx){Intent i=new Intent(this,SectionActivity.class);i.putExtra("page",name);i.putExtra("idx",idx);startActivity(i);}
    private String title() {
        switch(page){
            case "overview":return "О проекте простыми словами";
            case "branches":return "Направления разработки";
            case "commits":return "История основной ветки";
            case "tasks":return "Задачи и предложения";
            case "accuracy":return "Насколько точны данные";
            case "access":return "GitHub и доступ";
            case "data":return "Резервные копии";
            case "about":return "О GitTeller";
            case "branch-detail":return "Подробности направления";
            case "issue-detail":return "Задача GitHub";
            case "pull-detail":return "Предложение изменений";
            case "commit-detail":return "Подробности изменения";
            case "task-detail":return "Подробности задачи";
            default:return "Разделы проекта";
        }
    }
    private void render(){
        LinearLayout root=col();root.setBackgroundColor(bg);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18),dp(19),dp(18),dp(18));
        TextView back=text("‹",34,false,ink);back.setGravity(Gravity.CENTER);
        back.setContentDescription("Назад");back.setClickable(true);back.setFocusable(true);
        back.setOnClickListener(v->finish());header.addView(back,lp(48,48));
        TextView h=text(title(),20,true,ink);h.setPadding(dp(10),0,0,0);
        header.addView(h,new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(header,lp(-1,-2));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        body=col();body.setPadding(dp(20),dp(14),dp(20),dp(30));scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));setContentView(root);
        switch(page) {
            case "overview":overview();break;
            case "branches":branches();break;
            case "commits":commits();break;
            case "tasks":tasks();break;
            case "accuracy":accuracy();break;
            case "access":access();break;
            case "data":data();break;
            case "about":about();break;
            case "branch-detail":branchDetail();break;
            case "commit-detail":commitDetail();break;
            case "issue-detail":case "pull-detail":taskDetail();break;
            default:index();
        }
    }
    private void index(){
        line(body,"Выберите, что хотите узнать",21,true,ink);gap(body,16);
        action("Состояние и выводы","Что можно установить по загруженным сведениям",()->go("overview"));
        action("Направления разработки","Ветки GitHub с объяснением назначения",()->go("branches"));
        action("История изменений","Коммиты только основной ветки, без выдуманных результатов",()->go("commits"));
        action("Задачи и предложения","Что обсуждают, что открыто и что закрыто",()->go("tasks"));
        action("Точность данных","Чего приложение не может знать",()->go("accuracy"));
    }
    private void overview(){
        if(current==null){note("Нет данных","Вернитесь на главный экран и нажмите «Обновить с GitHub».");return;}
        Summary s=Summary.of(current,previous);
        note("Что произошло с последней проверки",HumanReport.homeDescription(current,previous,s));
        JSONArray a=current.optJSONArray("commits");
        if(a!=null&&a.length()>0){
            JSONObject c=a.optJSONObject(0);
            if(c!=null)action("Одна из последних записей в основной ветке",HumanReport.commitSummary(c.optString("message"))+". Это сообщение автора, не результат испытаний.",()->goItem("commit-detail",0));
        }
        JSONArray issues=current.optJSONArray("issues"),prs=current.optJSONArray("pulls");
        note("Что осталось открытым в GitHub",HumanReport.countState(issues,"open")+" задач, "+HumanReport.countState(prs,"open")+
                " предложений изменений. Отсутствие открытых записей не подтверждает готовность программы.");
        note("Что нужно проверить", HumanReport.NO_CHECK + ". GitTeller не запускает сборку, тесты и телефон. " + HumanReport.NOT_ENOUGH + ": в снимке нет отчётов испытаний.");
        if (s != null) note("На что смотреть дальше", s.attention);
        action("Посмотреть задачи","Обсуждения и предложения в GitHub",()->go("tasks"));
        action("Проверить источник","Дата загрузки, полнота данных и ограничения",()->go("accuracy"));
    }
    private void branches(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        JSONArray list=current.optJSONArray("branches");
        note("Что такое направление разработки?", "Разработчики могут параллельно работать над разными частями проекта. Название ветки лишь подсказывает тему, а не доказывает готовность функции.");
        if(list==null||list.length()==0){note("Список пуст","GitHub не вернул ветки.");return;}
        note("Найдено направлений: "+list.length(), current.optBoolean("branchesComplete",false)?
                "Список веток получен целиком по данным последнего запроса.":"Список может быть неполным.");
        for(int i=0;i<list.length();i++){
            JSONObject x=list.optJSONObject(i);if(x==null)continue;final int idx=i;
            String name=x.optString("name");
            action(HumanReport.branchKind(name)+" · "+(i+1),HumanReport.branchTopic(name)+". Откройте для подробностей.",()->goItem("branch-detail",idx));
        }
    }
    private void branchDetail(){
        JSONObject x=at("branches");if(x==null){note("Запись не найдена","Вернитесь к списку.");return;}
        String name=x.optString("name"),sha=x.optString("sha");
        note(HumanReport.branchKind(name),"Это отдельная линия работы в GitHub. Само существование ветки не означает, что её код готов или проверен.");
        note("Название в GitHub",name);
        note("Контрольная версия",sha+"\n\nSHA позволяет однозначно найти состояние этой ветки в GitHub. Для обычного чтения он не нужен.");
        note("Проверка", "По одному названию и SHA нельзя узнать, прошли ли тесты и можно ли использовать результат.");
    }
    private void commits(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        String branch=current.optString("defaultBranch","main");
        note("Какие изменения здесь показаны", "Это только последние загруженные записи ветки «"+branch+"». В других ветках изменения могут быть новее. GitTeller пока не строит общую хронологию всех веток.");
        JSONArray a=current.optJSONArray("commits");if(a==null||a.length()==0){note("Нет записей","Список коммитов не загрузился.");return;}
        for(int i=0;i<a.length();i++){
            JSONObject x=a.optJSONObject(i);if(x==null)continue;final int idx=i;
            action(HumanReport.commitSummary(x.optString("message")),"Запись №"+(i+1)+" из истории «"+branch+"». Результат не проверен.",()->goItem("commit-detail",idx));
        }
    }
    private void commitDetail(){
        JSONObject x=at("commits");if(x==null){note("Запись не найдена","Вернитесь к списку.");return;}
        note(HumanReport.commitSummary(x.optString("message")),"Это классификация по заголовку, а не анализ исходного кода.");
        note("Что написал автор",x.optString("message","Без описания"));
        note("Когда запись сохранена",x.optString("date","Дата недоступна"));
        note("Техническая версия",x.optString("sha","SHA недоступен"));
        note("Что осталось неизвестным", HumanReport.NOT_ENOUGH + ". Список изменённых файлов и результат испытаний не загружены. " + HumanReport.NO_CHECK + ".");
    }
    private void tasks(){
        if(current==null){note("Нет данных","Сначала обновите проект.");return;}
        JSONArray issues=current.optJSONArray("issues"),pulls=current.optJSONArray("pulls");
        note("Что означают эти записи", "Задачи — вопросы или работа, которую обсуждают. Запрос изменений — предложение внести код в проект. Закрытие не равно успешному испытанию.");
        note("Задачи",HumanReport.countState(issues,"open")+" открыто; "+HumanReport.countState(issues,"closed")+" закрыто в загруженной выборке.");
        if(issues!=null)for(int i=0;i<issues.length();i++){
            JSONObject x=issues.optJSONObject(i);if(x==null)continue;final int idx=i;
            action("Задача №"+x.optInt("number")+" · "+("open".equals(x.optString("state"))?"Открыта":"Закрыта"),
                    HumanReport.NOT_ENOUGH+": текст задачи не загружен. Закрытие не доказывает выполнение.",()->goItem("issue-detail",idx));
        }
        note("Предложения изменений",HumanReport.countState(pulls,"open")+" открыто; "+HumanReport.countState(pulls,"closed")+" закрыто в загруженной выборке.");
        if(pulls!=null)for(int i=0;i<pulls.length();i++){
            JSONObject x=pulls.optJSONObject(i);if(x==null)continue;final int idx=i;
            action("Предложение №"+x.optInt("number")+" · "+("open".equals(x.optString("state"))?"Открыто":"Закрыто"),
                    "Закрыто не значит объединено. Слитие и тесты в этот снимок не входят.",()->goItem("pull-detail",idx));
        }
    }
    private void taskDetail(){
        boolean pr="pull-detail".equals(page);
        JSONObject x=at(pr?"pulls":"issues");if(x==null){note("Запись не найдена","Вернитесь к списку.");return;}
        note((pr?"Предложение изменений":"Задача")+" №"+x.optInt("number"),HumanReport.state(x.optString("state")));
        note("Исходный заголовок",x.optString("title","Без названия"));
        note("Что это значит", pr
                ? HumanReport.NOT_ENOUGH + ". Нет данных, объединено ли предложение, какие файлы изменены и прошли ли проверки. Закрытие само по себе не означает merge. " + HumanReport.NO_CHECK + "."
                : HumanReport.NOT_ENOUGH + ". Текст задачи и метки не загружены. По заголовку и статусу нельзя установить, выполнена ли работа. " + HumanReport.NO_CHECK + ".");
    }
    private JSONObject at(String field){
        if(current==null)return null;JSONArray list=current.optJSONArray(field);if(list==null)return null;
        return list.optJSONObject(getIntent().getIntExtra("idx",-1));
    }
    private void accuracy(){
        if(current==null){note("Нет данных","Ещё не было проверки GitHub.");return;}
        note("Источник",(current.optBoolean("restoredFromBackup",false)?"Импортированный JSON, не свежий запрос GitHub.":"GitHub API, только чтение.")+"\n\nРепозиторий: "+repo);
        note("Когда получены сведения",DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(current.optLong("capturedAt",0))));
        note("Полнота списка веток",current.optBoolean("branchesComplete",false)?"Ветки загружены полностью по данным запроса.":"Не подтверждена: часть веток может отсутствовать.");
        note("Ограничение истории", "Загружаются последние записи только основной ветки. GitTeller не анализирует все коммиты других веток.");
        JSONArray warnings=current.optJSONArray("warnings");
        if(warnings==null||warnings.length()==0)note("Ошибки загрузки","Во время последнего получения снимка ошибок не зарегистрировано. Это не означает, что вся разработка проверена.");
        else for(int i=0;i<warnings.length();i++)note("Частично не загружено",warnings.optString(i));
        note("Чего GitTeller пока не знает", HumanReport.NO_CHECK + ". Не присваивает PASS. " + HumanReport.NOT_ENOUGH + " для текста задач, файлов коммита и проверок GitHub.");
    }
    private void access(){
        note("Безопасное подключение","Для публичного репозитория токен не нужен. Для приватного создайте токен GitHub только на чтение. Он сохраняется через Android Keystore отдельно для каждого проекта.");
        LinearLayout form=card();
        line(form,"Репозиторий GitHub",14,true,ink);gap(form,8);
        EditText repoField=new EditText(this);repoField.setSingleLine(true);repoField.setText(repo);
        repoField.setTextColor(ink);form.addView(repoField,lp(-1,-2));gap(form,15);
        String status;
        try{status=vault.hasToken(repo)?"Доступ сохранён на устройстве.":"Токен для текущего проекта не найден.";}
        catch(Exception e){status="Статус хранилища недоступен.";}
        line(form,status,14,false,muted);gap(form,12);
        line(form,"Токен (оставьте пустым, чтобы сохранить существующий)",13,false,muted);
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
        LinearLayout.LayoutParams params=lp(-1,53);params.topMargin=dp(9);box.addView(b,params);b.setOnClickListener(v->action.run());
    }
    private void data(){
        note("Резервные копии","В архив попадают только сведения GitHub. Защищённый токен не экспортируется. Импортированный файл нельзя считать достоверной текущей проверкой GitHub.");
        action("Сохранить копию проекта","Создать JSON без токена",()->requestData("export"));
        action("Восстановить из JSON","Импорт заменяет данные выбранного репозитория только после подтверждения",()->requestData("import"));
    }
    private void requestData(String action){setResult(RESULT_OK,new Intent().putExtra("action",action));finish();}
    private void about(){
        note("GitTeller 1.0.5", "Объясняет историю GitHub человеку без разработки. Это не коммерческий релиз и не проверка готовности программы.");
        note("Безопасность", "Чтение GitHub только через HTTPS. Приватные токены сохраняются в Android Keystore и не включаются в JSON-архив.");
        note("Ограничения", "Не проверяет фактическую работу кода на телефоне. Без отдельного источника подтверждения готовность проекта остаётся неизвестной.");
    }
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
}
