package app.gap.clarity;

import android.content.*;
import android.os.SystemClock;
import android.provider.Settings;
import java.time.ZoneId;

public final class StateStore {
    private static StateStore instance;
    public final BudgetEngine engine=new BudgetEngine();
    public final android.content.SharedPreferences prefs;
    private final Context context;
    private boolean active=false;
    private StateStore(Context c) {
        context=c.getApplicationContext(); prefs=context.getSharedPreferences("pianke-state",0);
        engine.dailyLimit=prefs.getLong("daily",ProductRules.DAILY); engine.batchLimit=prefs.getLong("batch",ProductRules.PORTION);
        engine.cooldownLength=prefs.getLong("coolLength",ProductRules.GAP); engine.day=prefs.getString("day","");
        engine.used=prefs.getLong("used",0); engine.remaining=prefs.getLong("remaining",engine.batchLimit);
        engine.coolWall=prefs.getLong("coolWall",0); engine.coolMono=prefs.getLong("coolMono",0); engine.coolBoot=prefs.getInt("coolBoot",-1);
        if(prefs.getInt("gapDeadlineSchema",0)==0 && engine.coolWall!=0){
            // <=0.1.3 could overwrite the gap without moving its deadline, losing
            // the original start. Do not fabricate elapsed time from that record.
            engine.finishCooldown();
            prefs.edit().putBoolean("legacyGapNotice",true).commit();
        }
        step(false);
    }
    public static synchronized StateStore get(Context c) { if(instance==null) instance=new StateStore(c); return instance; }
    public int boot() { return Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,-1); }
    public long wall() { return System.currentTimeMillis(); }
    public long mono() { return SystemClock.elapsedRealtime(); }
    public void step(boolean next) {
        engine.tick(wall(),mono(),boot(),active,ZoneId.systemDefault()); active=next&&!paused(); save();
    }
    public void refresh() { step(active); }
    public boolean blocked() { return engine.blocked(wall(),mono(),boot()); }
    public long cooling() { return engine.cooling(wall(),mono(),boot()); }
    public String target() { return prefs.getString("target","com.ss.android.ugc.aweme"); }
    public String targetName() { return prefs.getString("targetName",context.getString(R.string.default_target_name)); }
    public boolean consent() { return prefs.getBoolean("consent",false); }
    public boolean paused() { return prefs.getBoolean("protectionPaused",false); }
    public void setPaused(boolean value) {
        // Settle the preceding interval once. Neither pause nor resume grants new quota.
        step(false);
        prefs.edit().putBoolean("protectionPaused",value).commit();
    }
    public void save() {
        prefs.edit().putInt("gapDeadlineSchema",1).putLong("daily",engine.dailyLimit).putLong("batch",engine.batchLimit).putLong("coolLength",engine.cooldownLength)
        .putString("day",engine.day).putLong("used",engine.used).putLong("remaining",engine.remaining)
        .putLong("coolWall",engine.coolWall).putLong("coolMono",engine.coolMono).putInt("coolBoot",engine.coolBoot).commit();
    }
    public static String duration(long millis) {
        long sec=(Math.max(0,millis)+999)/1000;
        if(sec>=3600) return String.format(java.util.Locale.US,"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60);
        return String.format(java.util.Locale.US,"%02d:%02d",sec/60,sec%60);
    }
}
