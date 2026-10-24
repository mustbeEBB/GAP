package app.gap.clarity;

import android.accessibilityservice.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import java.util.*;

/** System-bound accessibility service; no screen text or network access. */
public final class GuardService extends AccessibilityService {
    public static volatile boolean connected=false;
    public static volatile String problem="";
    private static GuardService instance;
    static void refreshNow(){if(instance!=null)instance.handler.post(()->{if(instance!=null&&!instance.stopping){instance.notifiedMinute=-1;instance.inspect();}});}
    private final Handler handler=new Handler(Looper.getMainLooper());
    private StateStore store; private long lastBlockLaunch=-5000;
    private long notifiedMinute=-1; private String lastNotice="";
    private String foreground=""; private boolean stopping=false;
    private final Runnable pulse=new Runnable(){@Override public void run(){if(stopping)return;inspect();if(((PowerManager)getSystemService(POWER_SERVICE)).isInteractive())handler.postDelayed(this,!store.paused()&&store.target().equals(foreground)?250:1500);}};
    private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){handler.removeCallbacks(pulse);if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){store.step(false);foreground="";hide();}else handler.post(pulse);}};
    @Override protected void onServiceConnected(){
        super.onServiceConnected();store=StateStore.get(this);
        connected=true;problem="";stopping=false;instance=this;
        IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screenReceiver,f);
        NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel=new NotificationChannel("guard",getString(R.string.copy_109),NotificationManager.IMPORTANCE_LOW);channel.setDescription(getString(R.string.copy_110));channel.setShowBadge(false);n.createNotificationChannel(channel);
        handler.post(pulse);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(store==null||stopping)return;
        // Package-name hints only; never inspect event text or view content.
        if(e.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && e.getPackageName()!=null){
            String hint=e.getPackageName().toString();if(!hint.equals(getPackageName()))foreground=hint;
        }
        inspect();
    }
    private String focusedPackage(){
        // Only application package names are inspected, never screen text.
        try{
            List<AccessibilityWindowInfo> windows=getWindows();
            AccessibilityWindowInfo candidate=null;
            for(AccessibilityWindowInfo w:windows){
                if(w.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;
                if(candidate==null||w.isFocused()||(!candidate.isFocused()&&w.isActive()))candidate=w;
            }
            if(candidate!=null){AccessibilityNodeInfo root=candidate.getRoot();if(root!=null){CharSequence pkg=root.getPackageName();String name=pkg==null?"":pkg.toString();root.recycle();if(!name.isEmpty())return name;}}
        }catch(Exception ignored){}
        return foreground;
    }
    private void inspect(){
        if(store==null)return;
        PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(store.paused()||!store.consent()||!p.isInteractive()||keyguard.isKeyguardLocked()){
            store.step(false);hide();notifyState();return;
        }
        foreground=focusedPackage();boolean target=store.target().equals(foreground);
        // step bills only the PREVIOUS active interval, then switches to this interval.
        store.step(target && !BlockActivity.present());
        if(target && store.blocked()){store.step(false);show();}
        else if(!(BlockActivity.present() && foreground.equals(getPackageName()) && store.blocked())){hide();}
        notifyState();
    }
    private void show(){
        if(BlockActivity.present())return;
        long now=SystemClock.elapsedRealtime();
        if(now-lastBlockLaunch<1500)return;
        lastBlockLaunch=now;
        try{
            android.media.AudioManager audio=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PAUSE));
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PAUSE));
        }catch(RuntimeException ignored){}
        try{
            startActivity(new Intent(this,BlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NO_ANIMATION));
            problem="";
        }catch(RuntimeException ex){problem=getString(R.string.copy_111)+ex.getClass().getSimpleName();performGlobalAction(GLOBAL_ACTION_HOME);}
    }
    private void hide(){BlockActivity.close();}
    private void notifyState(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        String text=store.paused()?getString(R.string.copy_112):store.engine.dailyRemaining()==0?getString(R.string.copy_113):store.cooling()>0?getString(R.string.copy_114)+StateStore.duration(store.cooling()):getString(R.string.copy_115)+StateStore.duration(store.engine.available())+getString(R.string.copy_116)+StateStore.duration(store.engine.dailyRemaining());
        long minute=SystemClock.elapsedRealtime()/60000;String mode=store.paused()?"paused":store.engine.dailyRemaining()==0?"daily":store.cooling()>0?"cool":"ready";
        if(minute==notifiedMinute && mode.equals(lastNotice))return;notifiedMinute=minute;lastNotice=mode;
        PendingIntent click=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"guard").setSmallIcon(app.gap.clarity.R.drawable.ic_launcher).setContentTitle(store.paused()?getString(R.string.copy_117):getString(R.string.copy_118)).setContentText(text).setContentIntent(click).setOngoing(true).setOnlyAlertOnce(true).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(10,n);
    }
    private void shutdown(){stopping=true;connected=false;if(instance==this)instance=null;handler.removeCallbacksAndMessages(null);if(store!=null)store.step(false);hide();try{unregisterReceiver(screenReceiver);}catch(Exception ignored){}((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(10);}
    @Override public void onInterrupt(){if(store!=null)store.step(false);hide();}
    @Override public boolean onUnbind(Intent intent){shutdown();return super.onUnbind(intent);}
    @Override public void onDestroy(){shutdown();super.onDestroy();}
}
