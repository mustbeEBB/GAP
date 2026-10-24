package app.gap.clarity;

import android.app.Activity;
import android.os.*;

/** Isolated visual preview. Never reads/writes budgets or requests audio focus. */
public final class ArtPreviewActivity extends Activity {
    private RestArtwork artwork;private long started;private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){long left=Math.max(0,600000-(SystemClock.elapsedRealtime()-started));artwork.setTimer(left==0?getString(R.string.copy_104):Ui.minutes(left),false);handler.postDelayed(this,500);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);started=state==null?SystemClock.elapsedRealtime():state.getLong("started",SystemClock.elapsedRealtime());artwork=RestScreen.install(this,getString(R.string.copy_105),()->finish());}
    @Override protected void onResume(){super.onResume();RestScreen.immersive(this);handler.post(tick);}
    @Override protected void onPause(){handler.removeCallbacksAndMessages(null);super.onPause();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)RestScreen.immersive(this);}
    @Override protected void onSaveInstanceState(Bundle state){state.putLong("started",started);super.onSaveInstanceState(state);}
}
