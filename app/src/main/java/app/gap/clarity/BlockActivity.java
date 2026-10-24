package app.gap.clarity;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.*;
import android.view.*;

/** Same blocking/audio policy as0.1.5; shared approved artwork with live countdown. */
public final class BlockActivity extends Activity {
    static BlockActivity current;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private StateStore store;private RestArtwork artwork;
    private AudioManager audio;private AudioFocusRequest focus;
    private boolean ownsFocus=false,resumed=false;
    private boolean exiting=false,exitToHome=false;
    private BlockCue cue;
    private boolean cuePresented;
    private final Runnable tick=new Runnable(){public void run(){
        if(!resumed||exiting)return;store.refresh();if(store.paused()||!store.blocked()){finish();return;}
        boolean daily=store.engine.dailyRemaining()==0;
        artwork.setTimer(daily?getString(R.string.rest_tomorrow):StateStore.duration(store.cooling()),daily);handler.postDelayed(this,500);
    }};
    static boolean present(){return current!=null&&!current.isFinishing();}
    static void close(){if(present())current.finish();}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);overridePendingTransition(0,0);current=this;store=StateStore.get(this);store.step(false);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->goHome());
        if(store.paused()||!store.consent()||!store.blocked()){finish();return;}
        cuePresented=state!=null&&state.getBoolean("cuePresented",false);
        if(!cuePresented)cue=new BlockCue(this);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener(change->{if(change<0){ownsFocus=false;}},handler).build();
        artwork=RestScreen.install(this,getString(R.string.rest_home_button),()->goHome());
        boolean daily=store.engine.dailyRemaining()==0;artwork.setTimer(daily?getString(R.string.rest_tomorrow):StateStore.duration(store.cooling()),daily);
    }
    private void stopBackgroundPlayback(){
        if(audio==null||isFinishing()||!resumed||store.paused())return;
        try{if(!ownsFocus)ownsFocus=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}catch(RuntimeException ignored){}
        try{audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PAUSE));audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PAUSE));}catch(RuntimeException ignored){}
    }
    @Override protected void onResume(){super.onResume();resumed=true;if(store==null||artwork==null)return;store.step(false);RestScreen.immersive(this);handler.post(tick);handler.postDelayed(()->stopBackgroundPlayback(),100);handler.postDelayed(()->stopBackgroundPlayback(),450);
        if(cue!=null&&!cuePresented)handler.postDelayed(()->{if(resumed&&!exiting){cuePresented=true;cue.play();}},180);
    }
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus){RestScreen.immersive(this);stopBackgroundPlayback();}}
    @Override protected void onPause(){resumed=false;handler.removeCallbacksAndMessages(null);if(cue!=null){cue.close();cue=null;cuePresented=true;}releaseFocus();super.onPause();}
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("cuePresented",cuePresented);super.onSaveInstanceState(state);}
    private void releaseFocus(){if(audio!=null&&focus!=null&&ownsFocus){audio.abandonAudioFocusRequest(focus);ownsFocus=false;}}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(cue!=null)cue.close();releaseFocus();if(current==this)current=null;super.onDestroy();}
    @Override public void finish(){
        if(exiting)return;exiting=true;if(cue!=null){cue.close();cue=null;}
        RestScreen.dismiss(this,()->{
            if(exitToHome)startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            super.finish();overridePendingTransition(0,0);
        });
    }
    private void goHome(){exitToHome=true;finish();}
    @Override public void onBackPressed(){goHome();}
}
