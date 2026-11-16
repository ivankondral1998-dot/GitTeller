package app.unocontrol;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import java.util.concurrent.RejectedExecutionException;

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
    private static final int REQUEST_SECTION = 3;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private JSONObject current, previous;
    private String repo = DEFAULT_REPO;
    private TokenVault vault;
    private int syncEpoch;
    private volatile GithubClient inflight;
    private boolean loading;
    private String lastError;
    private JSONArray feed = new JSONArray();
    private LinearLayout content;
    private Button refresh;
    private FrameLayout homeRoot;
    private ScrollView homeScroll;
    private int savedScroll;
    private boolean reopenDrawer;
    private final DrawerMotion drawer = new DrawerMotion();
    private View drawerLayer;
    private float dragX;
    private final Ads ads = new Ads();
    private FrameLayout bannerSlot;
    private boolean bannerReady;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        prefs = getSharedPreferences("gitteller_v1", MODE_PRIVATE);
        vault = new TokenVault(this);
        repo = prefs.getString("repo", DEFAULT_REPO);
        current = parse(prefs.getString(key(repo, "current"), ""));
        previous = parse(prefs.getString(key(repo, "previous"), ""));
        lastError = prefs.getString(key(repo, "lastError"), "");
        if (lastError != null && lastError.isEmpty()) lastError = null;
        feed = parseArray(prefs.getString(key(repo, "feed"), ""));
        if (savedInstanceState != null) {
            savedScroll = savedInstanceState.getInt("scroll", 0);
            reopenDrawer = savedInstanceState.getBoolean("drawer", false);
        }
        draw();
        ads.preload(this);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (homeScroll != null) savedScroll = homeScroll.getScrollY();
        out.putInt("scroll", savedScroll);
        out.putBoolean("drawer", drawer.open);
    }

    @Override public void onBackPressed() {
        if (drawer.consumeBack()) {
            closeDrawer(motionMs() > 0);
            return;
        }
        super.onBackPressed();
    }

    private static String key(String repo, String suffix) { return repo.toLowerCase(java.util.Locale.ROOT) + ":" + suffix; }

    private static JSONObject parse(String s) {
        try { return s == null || s.isEmpty() ? null : new JSONObject(s); }
        catch (Exception ignored) { return null; }
    }

    private static JSONArray parseArray(String s) {
        try { return s == null || s.isEmpty() ? new JSONArray() : new JSONArray(s); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private void rememberFeed(JSONObject older, JSONObject newer) {
        try {
            JSONArray prior = parseArray(prefs.getString(key(repo, "feed"), ""));
            JSONArray news = ChangeSet.between(older, newer);
            feed = EventFeed.merge(prior, news, newer.optLong("capturedAt", System.currentTimeMillis()));
        } catch (Exception ignored) {
            // A broken local feed must not discard the fresh snapshot.
        }
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

    private boolean developerMode() {
        return UiMode.DEVELOPER.equals(UiMode.normalize(prefs.getString(UiMode.PREF, UiMode.NOVICE)));
    }

    private void modePicker(LinearLayout parent) {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setContentDescription("Переключатель режима GitTeller");
        String selected = developerMode() ? UiMode.DEVELOPER : UiMode.NOVICE;
        String[] modes = {UiMode.NOVICE, UiMode.DEVELOPER};
        String[] captions = {"Режим чайника", "Режим разработчика"};
        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            final boolean active = selected.equals(mode);
            TextView option = txt(captions[i], 13, active ? BG : TEXT, active);
            option.setGravity(Gravity.CENTER);
            option.setBackground(shape(active ? ACCENT : PANEL, 12, active ? 0 : STROKE));
            option.setContentDescription(captions[i] + (active ? ", выбран" : ", выбрать"));
            option.setClickable(true);
            option.setFocusable(true);
            option.setOnClickListener(v -> {
                if (mode.equals(selected)) return;
                if (!prefs.edit().putString(UiMode.PREF, mode).commit()) {
                    toast("Не удалось сохранить выбранный режим");
                    return;
                }
                draw();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i == 0) params.rightMargin = dp(8);
            controls.addView(option, params);
        }
        parent.addView(controls, lp(-1,-2));
    }

    private void question(LinearLayout parent, String title, String body, Runnable open, String reason) {
        LinearLayout card = panel(parent);
        addLine(card, title, 15, ACCENT, true, 0);
        addLine(card, body, 16, TEXT, false, 6);
        TextView more = txt("Подробнее ›", 14, MUTED, true);
        more.setContentDescription("Подробнее: " + title);
        more.setPadding(0, dp(10), 0, dp(10));
        more.setClickable(true);
        more.setFocusable(true);
        more.setOnClickListener(v -> open.run());
        space(card, 12);
        card.addView(more, lp(-1,-2));
        if (reason != null) {
            TextView why = txt("Почему так?", 14, ACCENT, false);
            why.setClickable(true);
            why.setFocusable(true);
            why.setContentDescription("Объяснить основание вывода: " + title);
            why.setPadding(0, dp(9), 0, dp(9));
            TextView explanation = txt(reason, 14, MUTED, false);
            explanation.setVisibility(View.GONE);
            why.setOnClickListener(v -> {
                boolean show = explanation.getVisibility() != View.VISIBLE;
                explanation.setVisibility(show ? View.VISIBLE : View.GONE);
                why.setText(show ? "Скрыть объяснение" : "Почему так?");
            });
            space(card, 10);
            card.addView(why, lp(-1,-2));
            space(card, 8);
            card.addView(explanation, lp(-1,-2));
        }
    }

    private void developerCard(LinearLayout parent, String title, String body, Runnable open) {
        question(parent, title, body, open, null);
    }

    private static int count(JSONObject snapshot, String field) {
        if (snapshot == null) return 0;
        JSONArray list = snapshot.optJSONArray(field);
        return list == null ? 0 : list.length();
    }

    private String collectionCoverage(String field) {
        return Coverage.one(current, field).shown(developerMode());
    }

    private void developerDashboard(LinearLayout parent) {
        if (current == null) {
            LinearLayout panel = panel(parent);
            addLine(panel, "Нет сохранённого снимка", 16, TEXT, true, 0);
            addLine(panel, "Нажмите «Обновить», чтобы прочитать GitHub.", 14, MUTED, false, 7);
            return;
        }
        LinearLayout evidence = panel(parent);
        addLine(evidence, "Снимок GitHub", 16, TEXT, true, 0);
        addLine(evidence, "Источник: " + (SnapshotCodec.isImported(current) ? "IMPORTED (не живые данные)"
                : current.optString("origin", "не определён")), 14, MUTED, false, 8);
        addLine(evidence, "Дата сохранения: " + formatTime(current.optLong("capturedAt", 0)), 14, MUTED, false, 6);
        addLine(evidence, "История коммитов — выборочная. Тесты не считаются успешными без отдельного ответа GitHub.", 13, MUTED, false, 9);
        developerCard(parent, "Ветки: " + count(current, "branches"),
                collectionCoverage("branches"),
                () -> openPage("branches"));
        developerCard(parent, "Изменения: " + feed.length(),
                "Лента сравнений сохранённых снимков. Не результат испытаний кода.",
                () -> openPage("changes"));
        developerCard(parent, "Задачи: " + count(current, "issues"),
                collectionCoverage("issues"),
                () -> openPage("tasks"));
        developerCard(parent, "Pull Requests: " + count(current, "pulls"),
                collectionCoverage("pulls"),
                () -> openPage("pulls"));
        developerCard(parent, "Проверки и диагностика", "Охват запросов, ограничения API и неизвестные результаты тестов",
                () -> openPage("accuracy"));
        developerCard(parent, "Коммиты: " + count(current, "commits"),
                Coverage.one(current, "commits").shown(true),
                () -> openPage("commits"));
    }

    private void draw() {
        applyPalette();
        boolean keepDrawer = drawer.open || reopenDrawer;
        if (homeScroll != null) savedScroll = homeScroll.getScrollY();
        if (drawerLayer != null && homeRoot != null) homeRoot.removeView(drawerLayer);
        drawer.markClosed();
        drawerLayer = null;
        ensureShell();
        homeRoot.setBackgroundColor(BG);
        if (homeScroll != null && homeScroll.getParent() == homeRoot) homeRoot.removeView(homeScroll);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout wrap=vertical();
        wrap.setPadding(dp(20),dp(22),dp(20),dp(34));
        scroll.addView(wrap);
        FrameLayout.LayoutParams scrollBox = new FrameLayout.LayoutParams(-1,-1);
        scrollBox.bottomMargin = bannerReady ? dp(56) : 0;
        homeRoot.addView(scroll, 0, scrollBox);
        if (bannerSlot.getParent() != homeRoot) {
            homeRoot.addView(bannerSlot, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        }
        homeScroll = scroll;
        content=wrap;
        final int restore = savedScroll;
        homeScroll.post(() -> homeScroll.scrollTo(0, restore));

        LinearLayout head=new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=vertical();
        brand.addView(txt("GitTeller",17,TEXT,true));
        addLine(brand,developerMode() ? "Анализ GitHub" : "Что сделали, без чтения кода",12,MUTED,false,5);
        head.addView(brand,new LinearLayout.LayoutParams(0,-2,1f));
        TextView search=txt("⌕",22,TEXT,false);
        search.setGravity(Gravity.CENTER);
        search.setContentDescription("Поиск по сохранённым данным проекта");
        search.setBackground(shape(PANEL,14,STROKE));
        search.setClickable(true);
        search.setFocusable(true);
        LinearLayout.LayoutParams searchBox=lp(48,48);
        searchBox.rightMargin=dp(8);
        head.addView(search,searchBox);
        search.setOnClickListener(v -> openPage("search"));
        TextView menu=txt("⋮",27,TEXT,false);
        menu.setGravity(Gravity.CENTER);menu.setContentDescription("Открыть панель разделов");
        menu.setBackground(shape(PANEL,14,STROKE));
        menu.setClickable(true);
        menu.setFocusable(true);
        head.addView(menu,lp(48,48));
        menu.setOnClickListener(v -> toggleDrawer());
        wrap.addView(head,lp(-1,-2));
        space(wrap,18);
        addLine(wrap,developerMode() ? "Панель разработчика" : "Что происходит с проектом",22,TEXT,true,0);
        String shortRepo=repo.length()>48?repo.substring(0,45)+"…":repo;
        addLine(wrap,"Проект: "+shortRepo,18,TEXT,true,6);
        space(wrap,8);

        Summary summary=Summary.of(current,previous);
        String step = HumanReport.nextStep(current,previous,summary,lastError);
        if (loading) {
            LinearLayout hero=panel(wrap);
            addLine(hero,"Идёт чтение GitHub",20,TEXT,true,0);
            addLine(hero,"Подождите. Старые сведения проекта пока не меняются.",16,TEXT,false,10);
        } else if (current == null) {
            LinearLayout start=panel(wrap);
            addLine(start,"Здесь видно, что изменилось в проекте. Код читать не нужно. Приложение ничего в проекте не меняет.",15,TEXT,false,0);
            TextView guide=txt("Как пользоваться",15,ACCENT,true);
            guide.setMinHeight(dp(48));
            guide.setGravity(Gravity.CENTER_VERTICAL);
            guide.setContentDescription("Как пользоваться");
            guide.setOnClickListener(v -> openPage("guide"));
            space(start,8);
            start.addView(guide,lp(-1,-2));
        } else if (developerMode()) {
            developerDashboard(wrap);
            recentStrip(wrap);
        } else {
            boolean has = true;
            boolean comparable = previous != null && !SnapshotCodec.isImported(current)
                    && !SnapshotCodec.isImported(previous)
                    && current.optString("repo").equalsIgnoreCase(previous.optString("repo"));
            boolean complete = comparable && current.optBoolean("branchesComplete",false)
                    && previous.optBoolean("branchesComplete",false)
                    && current.optBoolean("issuesComplete",false) && previous.optBoolean("issuesComplete",false)
                    && current.optBoolean("pullsComplete",false) && previous.optBoolean("pullsComplete",false);
            question(wrap,"Что изменилось", HumanReport.ownerChanged(current,previous,summary),
                    ()->openPage("changes"), UiMode.reason("changed",has,comparable,complete));
            question(wrap,"Где работа", HumanReport.ownerWhere(current,previous),
                    ()->openPage("branches"), UiMode.reason("where",has,comparable,complete));
            question(wrap,"Какие проблемы", HumanReport.ownerProblems(current),
                    ()->openPage("problems"), UiMode.reason("problems",has,comparable,complete));
            question(wrap,"Кто выполнял", HumanReport.ownerWho(current),
                    ()->openPage("commits"), UiMode.reason("who",has,comparable,complete));
            recentStrip(wrap);
        }
        if (!loading && current != null) {
            addLine(wrap,HumanReport.ownerStatus(current,previous,summary),14,MUTED,false,0);
            space(wrap,8);
        }
        coverageStrip(wrap);
        if (lastError != null && !loading) {
            LinearLayout warn=panel(wrap);
            addLine(warn,"Последняя попытка не удалась. "+lastError,14,WARN,false,0);
        }
        if (!loading && current != null && (current.optBoolean("issuesStale", false) || current.optBoolean("pullsStale", false)
                || current.optBoolean("branchesStale", false) || current.optBoolean("commitsStale", false)
                || SnapshotCodec.isImported(current))) {
            LinearLayout stale=panel(wrap);
            addLine(stale,"Часть сведений с прошлой удачной загрузки или из файла.",14,WARN,false,0);
            TextView why=txt("Почему данные неполные?",14,ACCENT,true);
            why.setMinHeight(dp(48));
            why.setGravity(Gravity.CENTER_VERTICAL);
            why.setContentDescription("Почему данные неполные");
            why.setOnClickListener(v -> openPage("incomplete"));
            space(stale,6);
            stale.addView(why,lp(-1,-2));
        }

        refresh=new Button(this);
        refresh.setText(loading?"Обновление…":"Обновить");
        refresh.setAllCaps(false);
        refresh.setTextSize(16);
        refresh.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        refresh.setTextColor(BG);
        refresh.setBackground(shape(ACCENT,15,0));
        refresh.setEnabled(!loading);
        refresh.setContentDescription("Обновить данные с GitHub");
        wrap.addView(refresh,lp(-1,56));
        refresh.setOnClickListener(v->sync());
        space(wrap,8);
        if (current == null && !loading) {
            Button connect=new Button(this);
            connect.setText("Подключить проект");
            connect.setAllCaps(false);
            connect.setTextSize(15);
            connect.setTextColor(TEXT);
            connect.setBackground(shape(PANEL,15,STROKE));
            connect.setContentDescription("Подключить проект или токен");
            wrap.addView(connect,lp(-1,48));
            connect.setOnClickListener(v->openPage("access"));
            space(wrap,8);
        }
        if (!loading && !"Повторить загрузку".equals(step)) {
            Button next=new Button(this);
            next.setText(step);
            next.setAllCaps(false);
            next.setTextSize(15);
            next.setTextColor(TEXT);
            next.setBackground(shape(PANEL_2,15,STROKE));
            wrap.addView(next,lp(-1,48));
            next.setOnClickListener(v->{
                if ("Посмотреть задачу".equals(step)) openPage("tasks");
                else if ("Проверить доступ".equals(step)) openPage("access");
                else if ("Открыть изменения".equals(step)) openPage("changes");
                else if ("Открыть направления".equals(step)) openPage("branches");
                else sync();
            });
            space(wrap,8);
        }
        TextView switchProject=txt("Сменить проект",14,ACCENT,false);
        switchProject.setMinHeight(dp(48));
        switchProject.setGravity(Gravity.CENTER_VERTICAL);
        switchProject.setContentDescription("Сменить репозиторий GitHub");
        switchProject.setOnClickListener(v->openPage("projects"));
        wrap.addView(switchProject,lp(-1,-2));
        TextView connectLink=txt("Подключить проект или токен",14,MUTED,false);
        connectLink.setMinHeight(dp(48));
        connectLink.setGravity(Gravity.CENTER_VERTICAL);
        connectLink.setContentDescription("Подключить проект или токен");
        connectLink.setOnClickListener(v->openPage("access"));
        if (current != null) wrap.addView(connectLink,lp(-1,-2));
        space(wrap,8);
        TextView footer=txt("Чтение GitHub. Это не проверка готовности программы.",12,MUTED,false);
        footer.setGravity(Gravity.CENTER);
        wrap.addView(footer,lp(-1,-2));
        reopenDrawer = false;
        if (keepDrawer) openDrawer(false);
    }

    private void recentStrip(LinearLayout parent) {
        java.util.List<HomeFeed.Row> rows = HomeFeed.latest(current, System.currentTimeMillis(), 5);
        if (rows.isEmpty()) return;
        LinearLayout card = panel(parent);
        addLine(card, "Свежее", 16, TEXT, true, 0);
        addLine(card, "Из уже загруженного списка. Это не вся история и не проверка, что код работает.", 12, MUTED, false, 4);
        for (HomeFeed.Row row : rows) {
            TextView line = txt(row.title, 15, TEXT, false);
            line.setMinHeight(dp(44));
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setContentDescription(row.title);
            final String page = row.page;
            line.setClickable(true);
            line.setFocusable(true);
            line.setOnClickListener(v -> openPage(page));
            space(card, 8);
            card.addView(line, lp(-1, -2));
            if (row.meta != null && !row.meta.isEmpty()) {
                addLine(card, row.meta, 12, MUTED, false, 0);
            }
        }
    }

    private void coverageStrip(LinearLayout parent) {
        LinearLayout card = panel(parent);
        addLine(card, "Что известно", 16, TEXT, true, 0);
        addLine(card, Coverage.compact(current, developerMode()), 14, MUTED, false, 8);
        if (current != null && developerMode()) {
            addLine(card, "Обновлено вручную: " + formatTime(current.optLong("capturedAt", 0))
                    + ". Само не запускается.", 13, MUTED, false, 8);
        } else if (current != null) {
            addLine(card, "Обновлено вручную: " + formatTime(current.optLong("capturedAt", 0)) + ".", 13, MUTED, false, 8);
        }
        TextView link = txt("Почему данные неполные?", 15, ACCENT, true);
        link.setMinHeight(dp(48));
        link.setGravity(Gravity.CENTER_VERTICAL);
        link.setContentDescription("Почему данные неполные");
        link.setOnClickListener(v -> openPage("incomplete"));
        space(card, 4);
        card.addView(link, lp(-1, -2));
    }

    private static String formatTime(long millis){
        if(millis<=0) return "нет даты";
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(millis));
    }

    private void ensureShell() {
        if (homeRoot != null && bannerSlot != null) return;
        homeRoot = new FrameLayout(this);
        bannerSlot = new FrameLayout(this);
        bannerSlot.setVisibility(android.view.View.GONE);
        setContentView(homeRoot);
        ads.attachBanner(this, bannerSlot);
        bannerReady = true;
    }

    private void openPage(String page) {
        Intent intent = new Intent(this, SectionActivity.class);
        intent.putExtra("page", page);
        startActivityForResult(intent, REQUEST_SECTION);
    }

    private void toggleDrawer() {
        if (drawer.open || drawer.busy) closeDrawer(motionMs() > 0);
        else openDrawer(motionMs() > 0);
    }

    private void openDrawer(boolean animate) {
        if (homeRoot == null || !drawer.requestOpen()) return;
        if (drawerLayer != null) homeRoot.removeView(drawerLayer);
        int screen = getResources().getDisplayMetrics().widthPixels;
        int panelWidth = Math.min(dp(320), Math.max(dp(220), screen - dp(56)));
        FrameLayout layer = new FrameLayout(this);
        layer.setClickable(true);
        layer.setFocusable(true);
        View scrim = new View(this);
        scrim.setBackgroundColor(0x99000000);
        scrim.setAlpha(animate ? 0f : 1f);
        scrim.setContentDescription("Закрыть панель");
        scrim.setOnClickListener(v -> closeDrawer(motionMs() > 0));
        layer.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout panel = vertical();
        panel.setClickable(true);
        panel.setBackground(drawerShape());
        int bar = systemBar("status_bar_height");
        int nav = systemBar("navigation_bar_height");
        panel.setPadding(dp(16), dp(12) + bar, dp(16), dp(16) + nav);
        TextView close = txt("Закрыть", 15, ACCENT, true);
        close.setMinHeight(dp(48));
        close.setGravity(Gravity.CENTER_VERTICAL);
        close.setContentDescription("Закрыть панель");
        close.setOnClickListener(v -> closeDrawer(motionMs() > 0));
        panel.addView(close, lp(-1, 48));
        addLine(panel, "GitTeller", 13, MUTED, false, 0);
        space(panel, 8);
        modePicker(panel);
        space(panel, 8);
        ScrollView list = new ScrollView(this);
        LinearLayout items = vertical();
        list.addView(items);
        panel.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        drawerItem(items, "Поиск", () -> { closeDrawer(false); openPage("search"); });
        drawerItem(items, "Как пользоваться", () -> { closeDrawer(false); openPage("guide"); });
        drawerItem(items, "Разделы проекта", () -> { closeDrawer(false); openPage("index"); });
        drawerItem(items, "Обновить", () -> { closeDrawer(false); sync(); });
        drawerItem(items, "Сменить проект", () -> { closeDrawer(false); openPage("projects"); });
        drawerItem(items, "Подключение GitHub", () -> { closeDrawer(false); openPage("access"); });
        drawerItem(items, "Почему данные неполные?", () -> { closeDrawer(false); openPage("incomplete"); });
        drawerItem(items, "Настройки", () -> { closeDrawer(false); openPage("settings"); });
        drawerItem(items, "О приложении", () -> { closeDrawer(false); openPage("about"); });
        View grip = new View(this);
        grip.setContentDescription("Смахните вправо, чтобы закрыть панель");
        grip.setOnTouchListener((v, event) -> dragDrawer(panel, event));
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.addView(grip, new LinearLayout.LayoutParams(dp(28), -1));
        shell.addView(panel, new LinearLayout.LayoutParams(0, -1, 1f));
        shell.setBackground(drawerShape());
        panel.setBackgroundColor(0);
        FrameLayout.LayoutParams panelBox = new FrameLayout.LayoutParams(panelWidth, -1, Gravity.END);
        layer.addView(shell, panelBox);
        homeRoot.addView(layer, new FrameLayout.LayoutParams(-1, -1));
        drawerLayer = layer;
        if (!animate) {
            setBlur(18f);
            drawer.markOpen();
            return;
        }
        shell.setTranslationX(panelWidth);
        shell.animate().translationX(0f).setDuration(motionMs()).setInterpolator(new DecelerateInterpolator()).start();
        scrim.animate().alpha(1f).setDuration(motionMs()).start();
        ValueAnimator blur = ValueAnimator.ofFloat(0f, 18f);
        blur.setDuration(motionMs());
        blur.addUpdateListener(a -> setBlur((Float) a.getAnimatedValue()));
        blur.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (!drawer.closing && drawerLayer != null) drawer.markOpen();
            }
        });
        blur.start();
    }

    private boolean dragDrawer(View panel, MotionEvent event) {
        View shell = (View) panel.getParent();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            dragX = event.getRawX();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            shell.setTranslationX(Math.max(0f, event.getRawX() - dragX));
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (shell.getTranslationX() > shell.getWidth() * 0.33f) closeDrawer(motionMs() > 0);
            else shell.animate().translationX(0f).setDuration(motionMs()).start();
            return true;
        }
        return true;
    }

    private void drawerItem(LinearLayout parent, String label, Runnable action) {
        TextView item = txt(label, 16, TEXT, false);
        item.setMinHeight(dp(48));
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(8), 0, dp(8), 0);
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(label);
        item.setOnClickListener(v -> action.run());
        parent.addView(item, lp(-1, -2));
    }

    private void closeDrawer(boolean animate) {
        if (drawerLayer == null) {
            drawer.markClosed();
            setBlur(0f);
            return;
        }
        if (!drawer.closing && !drawer.requestClose()) return;
        View layer = drawerLayer;
        if (!animate) {
            setBlur(0f);
            if (homeRoot != null) homeRoot.removeView(layer);
            if (drawerLayer == layer) drawerLayer = null;
            drawer.markClosed();
            return;
        }
        layer.animate().alpha(0f).setDuration(motionMs()).withEndAction(() -> {
            setBlur(0f);
            if (homeRoot != null) homeRoot.removeView(layer);
            if (drawerLayer == layer) drawerLayer = null;
            drawer.markClosed();
        }).start();
    }

    private GradientDrawable drawerShape() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(PANEL);
        float r = dp(18);
        d.setCornerRadii(new float[]{r, r, 0f, 0f, 0f, 0f, r, r});
        return d;
    }

    private void setBlur(float radius) {
        if (homeScroll == null || Build.VERSION.SDK_INT < 31) return;
        if (radius < 0.8f) homeScroll.setRenderEffect(null);
        else homeScroll.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
    }

    private long motionMs() {
        try {
            float scale = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            if (scale == 0f) return 0L;
        } catch (Exception ignored) { /* keep the normal motion */ }
        return 240L;
    }

    private int systemBar(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs == null) return;
        String selected = prefs.getString("repo", DEFAULT_REPO);
        if (!selected.equals(repo)) {
            abandonSync();
            repo = selected;
            current = parse(prefs.getString(key(repo,"current"),""));
            previous = parse(prefs.getString(key(repo,"previous"),""));
            feed = parseArray(prefs.getString(key(repo,"feed"),""));
            lastError = prefs.getString(key(repo,"lastError"), "");
            if (lastError != null && lastError.isEmpty()) lastError = null;
            draw();
        }
    }

    private String accessStatus() {
        try {
            if (vault != null && vault.hasToken(repo))
                return "Доступ: на устройстве есть запись. Расшифровка ещё не доказана, секрет не показывается.";
            return "Доступ: токен не сохранён. Публичный проект читается без него.";
        } catch (RuntimeException e) {
            return "Доступ: хранилище Android недоступно. Введите токен заново.";
        }
    }

    private void abandonSync() {
        syncEpoch++;
        GithubClient client = inflight;
        if (client != null) client.cancel();
        loading = false;
    }

    private void sync(){
        if(loading)return;
        if (ads.showInterstitialIfDue(this, prefs, this::syncNow)) return;
        syncNow();
    }

    private void syncNow(){
        if(loading || isFinishing() || isDestroyed()) return;
        final int epoch=syncEpoch;
        final String selected=repo;
        final String access;
        try {
            String loaded=vault.load(selected);
            access=loaded==null?"":loaded;
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("Доступ не прочитан")
                    .setMessage((e.getMessage()==null?"Хранилище Android недоступно.":e.getMessage())
                            +"\n\nСохранённые сведения проекта не изменены.")
                    .setPositiveButton("Закрыть",(d,w)->{}).show();
            return;
        }
        loading=true;draw();
        try {
        worker.execute(()->{
            GithubClient client=new GithubClient(access);
            inflight=client;
            try{
                JSONObject fresh=client.load(selected, current).snapshot;
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    if(epoch!=syncEpoch || !selected.equals(repo)) return;
                    previous=current;
                    current=fresh;
                    lastError=null;
                    rememberFeed(previous, current);
                    prefs.edit().putString(key(repo,"previous"),previous==null?"":previous.toString())
                            .putString(key(repo,"current"),current.toString())
                            .putString(key(repo,"feed"), feed.toString())
                            .remove(key(repo,"lastError")).commit();
                    loading=false;draw();toast("Данные обновлены");
                });
            }catch(Exception e){
                final String message=GithubClient.safeMessage(e);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    if(epoch!=syncEpoch || !selected.equals(repo)) return;
                    loading=false;
                    lastError=message;
                    prefs.edit().putString(key(repo,"lastError"), message).apply();
                    draw();
                    new AlertDialog.Builder(this).setTitle("Сверка не выполнена")
                            .setMessage(message+"\n\nПредыдущие данные не изменены.")
                            .setPositiveButton("Закрыть",(d,w)->{}).show();
                });
            } finally {
                if(inflight==client) inflight=null;
            }
        });
        } catch (RejectedExecutionException ignored) {
            loading = false;
        }
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
            fresh.put("origin","IMPORTED");
            if(old!=null){old=SnapshotCodec.sanitizeSnapshot(old,restoredRepo);old.put("restoredFromBackup",true);old.put("origin","IMPORTED");}
        }catch(Exception e){throw new IllegalArgumentException("Некорректный архив: "+GithubClient.safeMessage(e));}
        final JSONObject finalFresh=fresh, finalOld=old;
        new AlertDialog.Builder(this).setTitle("Восстановить из архива?")
                .setMessage("Архив "+restoredRepo+" заменит сохранённые данные этого репозитория. Текущие данные другого проекта не затронет.")
                .setNegativeButton("Отмена",(d,w)->{})
                .setPositiveButton("Восстановить",(d,w)->{
                    if(loading){toast("Дождитесь завершения синхронизации");return;}
                    if(!prefs.edit().putString(key(restoredRepo,"current"),finalFresh.toString())
                            .putString(key(restoredRepo,"previous"),finalOld==null?"":finalOld.toString())
                            .remove(key(restoredRepo,"feed"))
                            .putString("repo",restoredRepo).commit()){
                        toast("Не удалось сохранить архив");return;
                    }
                    repo=restoredRepo;current=finalFresh;previous=finalOld;feed=new JSONArray();draw();toast("Данные восстановлены из архива");
                }).show();
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
        if(requestCode==REQUEST_SECTION) {
            if(resultCode==RESULT_OK && data!=null) {
                String action=data.getStringExtra("action");
                if("import".equals(action)) {importLegacy();return;}
                if("export".equals(action)) {exportBackup();return;}
                if("sync".equals(action)) {sync();return;}
                if("changed".equals(action)) {
                    abandonSync();
                    String selected=prefs.getString("repo",DEFAULT_REPO);
                    repo=selected;
                    current=parse(prefs.getString(key(repo,"current"),""));
                    previous=parse(prefs.getString(key(repo,"previous"),""));
                    feed=parseArray(prefs.getString(key(repo,"feed"),""));
                    lastError=prefs.getString(key(repo,"lastError"), "");
                    if (lastError != null && lastError.isEmpty()) lastError = null;
                    draw();return;
                }
            }
            return;
        }
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
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){super.onDestroy();abandonSync();ads.destroy();worker.shutdownNow();}
}
