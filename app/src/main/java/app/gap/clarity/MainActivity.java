package app.gap.clarity;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private Ui u; private StateStore store; private LinearLayout page; private String screen="home";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView status,time,label,daily,caption; private boolean resumed=false;
    private AlertDialog pauseDialog; private PauseGate pauseGate;
    private Ui.Meter availableMeter,dailyMeter;
    private MotionHost motionHost;
    private final NavigationState navigation=new NavigationState();
    private final Map<String,Integer> scrollPositions=new HashMap<>();
    private ScrollView activeScroll;
    private EditText[] ruleFields;
    private String[] ruleDraft;
    private android.window.OnBackInvokedCallback systemBack;
    private boolean backRegistered;
    private final Runnable update=new Runnable(){public void run(){if(!resumed)return; if(screen.equals("home"))updateHome();handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);store=StateStore.get(this);u=new Ui(this);
        if(Build.VERSION.SDK_INT>=33)systemBack=()->onBackPressed();
        if(store.prefs.getBoolean("legacyGapNotice",false)){Toast.makeText(this,getString(R.string.copy_001),Toast.LENGTH_LONG).show();store.prefs.edit().putBoolean("legacyGapNotice",false).commit();}
        if(!store.prefs.getBoolean("introDone",false) || store.prefs.getBoolean("showIntro",false))showIntro(0);else showHome();
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(update);handler.post(update);if(screen.equals("permissions"))showPermissions();}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(update);if(motionHost!=null)motionHost.settle();if(pauseGate!=null)pauseGate.cancel();if(pauseDialog!=null)pauseDialog.dismiss();super.onPause();}
    @Override public void onBackPressed(){if(screen.equals("intro")&&!store.prefs.getBoolean("introDone",false)){finish();return;}if(!screen.equals("home"))goBack();else super.onBackPressed();}
    private void goBack(){
        String parent=navigation.parent();
        if(parent.equals("rules"))showRules();
        else if(parent.equals("permissions"))showPermissions();
        else if(parent.startsWith("intro:"))showIntro(Integer.parseInt(parent.substring(6)));
        else showHome();
    }
    private void shell(String name,String title,String subtitle) {
        shell(name,title,subtitle,name);
    }
    private void shell(String name,String title,String subtitle,String key) {
        String previous=navigation.current();
        if(activeScroll!=null)scrollPositions.put(previous,activeScroll.getScrollY());
        if(previous.equals("rules")&&key.equals("sandbox")&&ruleFields!=null){ruleDraft=new String[3];for(int i=0;i<3;i++)ruleDraft[i]=ruleFields[i].getText().toString();}
        int direction=navigation.visit(key);
        if(key.equals("home")){ruleDraft=null;ruleFields=null;}
        View focus=getCurrentFocus();if(direction!=0&&focus!=null)((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);
        screen=name; u=new Ui(this);u.window(this); ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(u.bg);
        if(Build.VERSION.SDK_INT>=33){
            if(!name.equals("home")&&!backRegistered){getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,systemBack);backRegistered=true;}
            else if(name.equals("home")&&backRegistered){getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBack);backRegistered=false;}
        }
        activeScroll=scroll;
        page=u.column();page.setPadding(u.dp(22),u.dp(16),u.dp(22),u.dp(28));page.setFocusableInTouchMode(true);
        scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        LinearLayout frame=u.column();frame.setBackgroundColor(u.bg);frame.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        frame.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        if(motionHost==null){motionHost=new MotionHost(this);setContentView(motionHost);}
        motionHost.show(frame,direction);frame.requestApplyInsets();page.requestFocus();
        final int saved=scrollPositions.containsKey(key)?scrollPositions.get(key):0;
        scroll.post(()->scroll.scrollTo(0,saved));
        if(!name.equals("home")&&!name.equals("intro")){TextView back=u.text(getString(R.string.copy_002),16,u.accent,true);back.setMinHeight(u.dp(48));back.setGravity(Gravity.CENTER_VERTICAL);back.setBackground(u.pressSurface(u.bg,12));back.setOnClickListener(v->goBack());page.addView(back);}
        if(title!=null){u.gap(page,12);page.addView(u.text(title,28,u.ink,true));u.gap(page,8);}
        if(subtitle!=null){page.addView(u.text(subtitle,14,u.sub,false));u.gap(page,24);}
    }
    private void showHome(){
        shell("home",null,null);availableMeter=null;dailyMeter=null;
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=u.text(getString(R.string.app_name),Ui.STYLE==1?54:36,u.ink,true);brand.setTypeface(android.graphics.Typeface.create("sans-serif-black",0));brand.setLetterSpacing(-.06f);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));header.addView(u.link(getString(R.string.home_help),()->showIntro(0)));page.addView(header);
        u.gap(page,Ui.STYLE==1?18:14);
        status=u.text(getString(R.string.copy_003),13,u.sub,true);status.setMinHeight(u.dp(48));status.setGravity(Gravity.CENTER_VERTICAL);status.setPadding(u.dp(15),u.dp(13),u.dp(15),u.dp(13));status.setOnClickListener(v->showPermissions());u.accessibleButton(status,null);page.addView(status,new LinearLayout.LayoutParams(-1,-2));
        if(Ui.STYLE==0)homeClarity();else if(Ui.STYLE==1)homeSpace();else homePulse();
        updateHome();
    }
    private void homeClarity(){
        u.gap(page,22);LinearLayout hero=u.card(page);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setMinimumHeight(u.dp(36));label=u.text(getString(R.string.home_portion_title),14,u.sub,true);top.addView(label,new LinearLayout.LayoutParams(0,-2,1));hero.addView(top);
        u.gap(hero,13);time=u.number("05:00",76);time.setGravity(Gravity.CENTER);hero.addView(time,new LinearLayout.LayoutParams(-1,-2));u.gap(hero,10);
        caption=u.text(getString(R.string.copy_004),13,u.sub,false);caption.setGravity(Gravity.CENTER);hero.addView(caption);u.gap(hero,22);
        availableMeter=new Ui.Meter(u,false);hero.addView(availableMeter,new LinearLayout.LayoutParams(-1,u.dp(5)));u.gap(hero,23);u.hairline(hero);u.gap(hero,20);
        hero.addView(u.text(getString(R.string.home_daily_title),12,u.sub,true));u.gap(hero,7);daily=u.number(getString(R.string.copy_005),23);hero.addView(daily,new LinearLayout.LayoutParams(-1,-2));u.gap(hero,12);dailyMeter=new Ui.Meter(u,false);hero.addView(dailyMeter,new LinearLayout.LayoutParams(-1,u.dp(4)));
        u.gap(page,26);u.section(page,getString(R.string.home_section));
        u.row(page,getString(R.string.protect_target),store.targetName()+getString(R.string.copy_006),()->showApps());
        u.row(page,getString(R.string.rules_entry),rulesSummary(),()->showRules());
        u.row(page,getString(R.string.permissions_entry),getString(R.string.copy_007),()->showPermissions());
    }
    private void homeSpace(){
        u.gap(page,28);page.addView(u.text(getString(R.string.copy_008),22,u.ink,true));u.gap(page,30);
        label=u.text(getString(R.string.home_portion_title),13,u.sub,true);page.addView(label);u.gap(page,8);time=u.number("05:00",98);page.addView(time,new LinearLayout.LayoutParams(-1,-2));u.gap(page,14);
        caption=u.text(getString(R.string.copy_004),13,u.sub,false);page.addView(caption);u.gap(page,24);availableMeter=new Ui.Meter(u,false);page.addView(availableMeter,new LinearLayout.LayoutParams(-1,u.dp(7)));
        u.gap(page,30);u.hairline(page);u.gap(page,20);page.addView(u.text(getString(R.string.home_daily_title),12,u.sub,true));u.gap(page,7);daily=u.number(getString(R.string.copy_005),29);page.addView(daily,new LinearLayout.LayoutParams(-1,-2));u.gap(page,10);
        page.addView(u.text(getString(R.string.copy_009)+store.engine.batchLimit/60000+getString(R.string.copy_010)+store.engine.cooldownLength/60000+getString(R.string.copy_011),12,u.sub,false));
        u.gap(page,27);LinearLayout actions=new LinearLayout(this);page.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        u.tile(actions,getString(R.string.protect_target),store.targetName(),()->showApps(),false);u.tile(actions,getString(R.string.rules_entry),getString(R.string.copy_012),()->showRules(),true);u.gap(page,12);
        u.row(page,getString(R.string.permissions_entry),getString(R.string.copy_007),()->showPermissions());
    }
    private void homePulse(){
        u.gap(page,22);LinearLayout hero=u.card(page);
        LinearLayout upper=new LinearLayout(this);upper.setGravity(Gravity.CENTER_VERTICAL);label=u.text(getString(R.string.home_portion_title),13,u.sub,true);upper.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView target=u.text(store.targetName(),12,u.accent,true);upper.addView(target);hero.addView(upper);u.gap(hero,20);
        time=u.number("05:00",82);time.setGravity(Gravity.CENTER);hero.addView(time,new LinearLayout.LayoutParams(-1,-2));u.gap(hero,24);
        availableMeter=new Ui.Meter(u,true);hero.addView(availableMeter,new LinearLayout.LayoutParams(-1,u.dp(26)));u.gap(hero,17);
        caption=u.text(getString(R.string.copy_004),12,u.sub,false);caption.setGravity(Gravity.CENTER);hero.addView(caption);u.gap(hero,20);u.hairline(hero);u.gap(hero,16);
        TextView rhythm=u.text(getString(R.string.copy_013)+store.engine.batchLimit/60000+" min    /    GAP "+store.engine.cooldownLength/60000+" min",12,u.sub,true);rhythm.setGravity(Gravity.CENTER);hero.addView(rhythm);
        u.gap(page,14);LinearLayout balance=u.card(page);balance.addView(u.text(getString(R.string.home_daily_title),12,u.sub,true));u.gap(balance,8);daily=u.number(getString(R.string.copy_005),26);balance.addView(daily,new LinearLayout.LayoutParams(-1,-2));u.gap(balance,13);dailyMeter=new Ui.Meter(u,false);balance.addView(dailyMeter,new LinearLayout.LayoutParams(-1,u.dp(4)));
        u.gap(page,24);LinearLayout actions=new LinearLayout(this);page.addView(actions,new LinearLayout.LayoutParams(-1,-2));u.tile(actions,getString(R.string.rules_entry),getString(R.string.copy_014),()->showRules(),false);u.tile(actions,getString(R.string.protect_target),store.targetName(),()->showApps(),true);u.gap(page,12);
        u.row(page,getString(R.string.permissions_entry),getString(R.string.copy_007),()->showPermissions());
    }
    private String rulesSummary(){return store.engine.batchLimit/60000+getString(R.string.copy_015)+store.engine.cooldownLength/60000+getString(R.string.copy_016);}
    private void updateHome(){
        store.refresh();long cool=store.cooling();boolean connected=GuardService.connected;boolean installed=isInstalled(store.target());
        String message;int state;
        if(store.paused()){message=getString(R.string.copy_017);state=2;}
        else if(!store.consent()){message=getString(R.string.status_consent_missing);state=1;}
        else if(!connected){message=getString(R.string.status_disconnected);state=1;}
        else if(!installed){message=getString(R.string.copy_018);state=3;}
        else if(!GuardService.problem.isEmpty()){message=getString(R.string.copy_019);state=3;}
        else{message=getString(R.string.status_connected);state=0;}
        status.setText(message);u.status(status,state);
        if(store.paused()){label.setText(getString(R.string.protection_title));time.setText(getString(R.string.paused_title));time.setTextSize(44);caption.setText(getString(R.string.copy_020));}
        else if(store.engine.dailyRemaining()==0){label.setText(getString(R.string.copy_021));time.setText(getString(R.string.rest_tomorrow));time.setTextSize(42);caption.setText(getString(R.string.copy_022));}
        else if(cool>0){label.setText(getString(R.string.home_cooldown_title));time.setText(StateStore.duration(cool));time.setTextSize(Ui.STYLE==1?94:Ui.STYLE==2?78:72);caption.setText(getString(R.string.home_cooldown_hint));}
        else{label.setText(getString(R.string.home_portion_title));time.setText(StateStore.duration(store.engine.available()));time.setTextSize(Ui.STYLE==1?98:Ui.STYLE==2?82:76);caption.setText(getString(R.string.copy_023)+store.targetName()+getString(R.string.copy_024));}
        daily.setText(Ui.minutes(store.engine.dailyRemaining())+" / "+store.engine.dailyLimit/60000+getString(R.string.copy_011));
        if(availableMeter!=null)availableMeter.setFraction(cool>0?1.0-(double)cool/store.engine.cooldownLength:(double)store.engine.available()/store.engine.batchLimit);
        if(dailyMeter!=null)dailyMeter.setFraction((double)store.engine.dailyRemaining()/store.engine.dailyLimit);
    }
    private boolean isInstalled(String pkg){try{getPackageManager().getApplicationInfo(pkg,0);return true;}catch(Exception e){return false;}}
    private void showIntro(int index){
        String[] titles={getString(R.string.brand_slogan),getString(R.string.intro_title_2),getString(R.string.intro_title_3),getString(R.string.intro_title_4)};
        String[] bodies={getString(R.string.copy_025), getString(R.string.copy_026), getString(R.string.copy_027), getString(R.string.copy_028)};
        shell("intro",null,null,"intro:"+index);u.gap(page,24);page.addView(u.text(getString(R.string.copy_029)+(index+1)+" / 4",12,u.sub,true));u.gap(page,20);LinearLayout steps=new LinearLayout(this);for(int n=0;n<4;n++){View dot=new View(this);dot.setBackground(u.shape(n<=index?u.accent:u.line,3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,u.dp(4),1);if(n<3)lp.rightMargin=u.dp(6);steps.addView(dot,lp);}page.addView(steps);u.gap(page,20);
        page.addView(new Ui.Orbit(u),new LinearLayout.LayoutParams(-1,u.dp(130)));u.gap(page,25);
        page.addView(u.text(titles[index],30,u.ink,true));u.gap(page,22);page.addView(u.text(bodies[index],17,u.sub,false));u.gap(page,24);
        if(index==3){
            CheckBox every=new CheckBox(this);every.setText(getString(R.string.copy_030));every.setTextColor(u.ink);every.setChecked(store.prefs.getBoolean("showIntro",false));page.addView(every);u.gap(page,15);
            page.addView(u.button(getString(R.string.copy_031),true,()->{store.prefs.edit().putBoolean("introDone",true).putBoolean("showIntro",every.isChecked()).apply();showHome();}));
            u.gap(page,14);page.addView(u.text(getString(R.string.copy_032),13,u.sub,false));
        }else page.addView(u.button(getString(R.string.copy_033),true,()->showIntro(index+1)));
        if(index>0){u.gap(page,10);page.addView(u.button(getString(R.string.copy_034),false,()->showIntro(index-1)));}
    }
    private void showPermissions(){
        shell("permissions",getString(R.string.permissions_title),getString(R.string.copy_035));
        LinearLayout block=u.card(page);block.addView(u.text(GuardService.connected?getString(R.string.copy_036):getString(R.string.copy_037),18,u.ink,true));u.gap(block,12);
        block.addView(u.text(getString(R.string.copy_038),15,u.sub,false));u.gap(block,18);
        block.addView(u.button(GuardService.connected?getString(R.string.copy_039):getString(R.string.copy_040),true,()->new AlertDialog.Builder(this).setTitle(getString(R.string.copy_041)).setMessage(getString(R.string.copy_042))
            .setNegativeButton(getString(R.string.copy_043),null).setPositiveButton(getString(R.string.copy_044),(d,w)->{store.prefs.edit().putBoolean("consent",true).commit();open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}).show()));
        u.gap(page,14);u.row(page,getString(R.string.copy_045),getString(R.string.copy_046),()->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        u.row(page,getString(R.string.copy_047),getString(R.string.copy_048),()->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);});
        u.row(page,getString(R.string.copy_049),getString(R.string.copy_050),()->showPermissions());
        LinearLayout details=u.card(page);details.addView(u.text(getString(R.string.copy_051),16,u.ink,true));u.gap(details,8);details.addView(u.text("Android "+Build.VERSION.RELEASE+" · "+Build.MANUFACTURER+" "+Build.MODEL+getString(R.string.copy_052)+store.targetName()+getString(R.string.copy_053)+(GuardService.connected?getString(R.string.copy_054):getString(R.string.copy_055))+"\n"+(GuardService.problem.isEmpty()?getString(R.string.copy_056):GuardService.problem)+getString(R.string.copy_057),14,u.sub,false));
        u.gap(page,26);
        TextView control=u.text(store.paused()?getString(R.string.copy_058):getString(R.string.copy_059),14,u.sub,false);
        control.setMinHeight(u.dp(48));control.setGravity(Gravity.CENTER_VERTICAL);control.setFocusable(true);
        control.setContentDescription(store.paused()?getString(R.string.copy_060):getString(R.string.copy_061));
        control.setOnClickListener(v->{if(store.paused()){store.setPaused(false);GuardService.refreshNow();Toast.makeText(this,GuardService.connected?getString(R.string.copy_062):getString(R.string.copy_063),Toast.LENGTH_LONG).show();showHome();}else{pauseGate=new PauseGate();pauseGate.start(SystemClock.elapsedRealtime());showPauseStage();}});
        page.addView(control);page.addView(u.text(store.paused()?getString(R.string.copy_064):getString(R.string.copy_065),12,u.sub,false));
    }
    private void showPauseStage(){
        final PauseGate gate=pauseGate;final int stage=gate.stage();if(stage<1||stage>3)return;
        String[] headings={getString(R.string.copy_066),getString(R.string.copy_067),getString(R.string.copy_068)};
        String[] messages={getString(R.string.copy_069), getString(R.string.copy_070), getString(R.string.copy_071)};
        final String action=stage==3?getString(R.string.copy_072):getString(R.string.copy_073);
        final Handler waitHandler=new Handler(Looper.getMainLooper());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(headings[stage-1]).setMessage(messages[stage-1])
            .setNegativeButton(getString(R.string.copy_074),(d,w)->gate.cancel()).setPositiveButton(action,null).create();
        pauseDialog=dialog;dialog.setCanceledOnTouchOutside(false);dialog.setOnCancelListener(d->gate.cancel());
        dialog.setOnDismissListener(d->waitHandler.removeCallbacksAndMessages(null));
        dialog.setOnShowListener(d->{
            android.widget.Button next=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Runnable countdown=new Runnable(){public void run(){if(!dialog.isShowing())return;long left=gate.waitMillis(SystemClock.elapsedRealtime());next.setEnabled(left==0);next.setText(left==0?action:action+"（"+((left+999)/1000)+getString(R.string.copy_075));if(left>0)waitHandler.postDelayed(this,150);}};
            next.setOnClickListener(v->{long now=SystemClock.elapsedRealtime();if(gate.waitMillis(now)>0||gate.stage()!=stage)return;boolean completed=gate.confirm(now);dialog.dismiss();if(completed){store.setPaused(true);BlockActivity.close();GuardService.refreshNow();Toast.makeText(this,getString(R.string.copy_076),Toast.LENGTH_LONG).show();showHome();}else showPauseStage();});
            countdown.run();
        });dialog.show();
    }
    private void open(Intent i){try{startActivity(i);}catch(Exception e){Toast.makeText(this,getString(R.string.copy_077),Toast.LENGTH_LONG).show();}}
    private void showApps(){
        shell("apps",getString(R.string.copy_078),getString(R.string.copy_079));
        EditText search=new EditText(this);search.setSingleLine();search.setTextColor(u.ink);search.setHintTextColor(u.sub);search.setHint(getString(R.string.copy_080));search.setTextSize(16);search.setMinHeight(u.dp(54));search.setPadding(u.dp(14),u.dp(12),u.dp(14),u.dp(12));search.setBackground(u.shape(u.soft,12));page.addView(search);u.gap(page,16);
        LinearLayout list=u.column();page.addView(list);list.addView(u.text(getString(R.string.copy_081),14,u.sub,false));
        new Thread(()->{
            List<ResolveInfo> found=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
            Map<String,String> apps=new TreeMap<>();for(ResolveInfo r:found){String p=r.activityInfo.packageName;if(!p.equals(getPackageName())&&!p.equals("com.android.settings")&&!p.contains("launcher"))apps.put(p,r.loadLabel(getPackageManager()).toString());}
            runOnUiThread(()->{if(!screen.equals("apps"))return;Runnable draw=()->{list.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);int count=0;for(Map.Entry<String,String> a:apps.entrySet())if((a.getValue()+a.getKey()).toLowerCase(Locale.ROOT).contains(query)){
                count++;u.row(list,(a.getKey().equals(store.target())?"✓  ":"")+a.getValue(),a.getKey(),()->new AlertDialog.Builder(this).setTitle(getString(R.string.copy_082)+a.getValue()+"？").setMessage(getString(R.string.copy_083)).setNegativeButton(getString(R.string.copy_084),null).setPositiveButton(getString(R.string.copy_085),(d,w)->{store.step(false);store.prefs.edit().putString("target",a.getKey()).putString("targetName",a.getValue()).commit();showHome();}).show());}
                if(count==0)list.addView(u.text(getString(R.string.copy_086),14,u.sub,false));};
                search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){draw.run();}public void afterTextChanged(android.text.Editable e){}});draw.run();});
        }).start();
    }
    private EditText number(LinearLayout c,String title,long value){
        c.addView(u.text(title,13,u.sub,true));u.gap(c,8);EditText e=new EditText(this);e.setTextColor(u.ink);e.setInputType(2);e.setSingleLine(true);e.setSelectAllOnFocus(true);e.setText(Long.toString(value));e.setTextSize(24);e.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));e.setPadding(u.dp(14),u.dp(12),u.dp(14),u.dp(12));e.setMinHeight(u.dp(54));e.setBackground(u.shape(u.soft,12));e.setContentDescription(title);c.addView(e,new LinearLayout.LayoutParams(-1,-2));u.gap(c,18);return e;
    }
    private void showRules(){
        store.refresh();shell("rules",getString(R.string.rules_title),getString(R.string.copy_087));
        LinearLayout c=u.card(page);EditText d=number(c,getString(R.string.rule_daily_label),store.engine.dailyLimit/60000),b=number(c,getString(R.string.rule_portion_label),store.engine.batchLimit/60000),cool=number(c,getString(R.string.rule_gap_label),store.engine.cooldownLength/60000);
        ruleFields=new EditText[]{d,b,cool};if(ruleDraft!=null)for(int i=0;i<3;i++)ruleFields[i].setText(ruleDraft[i]);
        c.addView(u.text(getString(R.string.copy_088),13,u.sub,false));u.gap(c,18);
        c.addView(u.button(getString(R.string.rules_save),true,()->{try{long dv=Long.parseLong(d.getText().toString()),bv=Long.parseLong(b.getText().toString()),cv=Long.parseLong(cool.getText().toString());if(dv<1||dv>1440||bv<1||bv>dv||cv<1||cv>1440)throw new Exception();store.refresh();store.engine.changeRules(dv*60000,bv*60000,cv*60000);store.save();GuardService.refreshNow();Toast.makeText(this,getString(R.string.copy_089),Toast.LENGTH_SHORT).show();showHome();}catch(Exception e){Toast.makeText(this,getString(R.string.copy_090),Toast.LENGTH_LONG).show();}}));
        u.gap(page,20);u.row(page,getString(R.string.copy_091),getString(R.string.copy_092),()->showSandbox());
        CheckBox check=new CheckBox(this);check.setText(getString(R.string.copy_093));check.setTextColor(u.ink);check.setChecked(store.prefs.getBoolean("showIntro",false));check.setOnCheckedChangeListener((v,on)->store.prefs.edit().putBoolean("showIntro",on).apply());page.addView(check);
    }
    private void showSandbox(){
        shell("sandbox",getString(R.string.copy_094),getString(R.string.copy_095));
        BudgetEngine e=new BudgetEngine();e.dailyLimit=60000;e.batchLimit=10000;e.remaining=10000;e.cooldownLength=20000;
        long[] elapsed={0};long base=System.currentTimeMillis();e.tick(base,0,1,false,java.time.ZoneId.systemDefault());
        LinearLayout c=u.card(page);TextView result=u.text("",20,u.ink,true);c.addView(result);
        Runnable draw=()->result.setText(getString(R.string.copy_096)+StateStore.duration(e.available())+getString(R.string.copy_097)+StateStore.duration(e.dailyRemaining())+getString(R.string.copy_098)+StateStore.duration(e.cooling(base+elapsed[0],elapsed[0],1)));
        java.util.function.BiConsumer<Long,Boolean> step=(ms,using)->{elapsed[0]+=ms;e.tick(base+elapsed[0],elapsed[0],1,using,java.time.ZoneId.systemDefault());draw.run();};draw.run();u.gap(page,20);
        page.addView(u.button(getString(R.string.copy_099),true,()->step.accept(5000L,true)));u.gap(page,12);page.addView(u.button(getString(R.string.copy_100),false,()->step.accept(10000L,false)));u.gap(page,12);
        page.addView(u.button(getString(R.string.copy_101),false,()->showSandbox()));
    }
}
