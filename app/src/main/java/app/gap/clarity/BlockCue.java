package app.gap.clarity;

import android.content.Context;
import android.media.*;
import android.os.*;

/** A single local notification cue per presentation; never changes system volume or focus. */
final class BlockCue {
    private SoundPool pool;
    private final AudioManager audio;
    private boolean loaded,requested,played,closed;
    private int sample;
    BlockCue(Context c){
        audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        try{
            pool=new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            pool.setOnLoadCompleteListener((p,id,status)->{if(closed)return;loaded=status==0;if(loaded&&requested)play();});
            sample=pool.load(c,R.raw.gap_notice,1);
        }catch(RuntimeException e){close();}
    }
    void play(){
        if(closed||played)return;requested=true;if(!loaded)return;
        played=true;
        // Notification audio respects DND; additionally respect silent/vibrate ringer modes.
        if(audio==null||audio.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL)return;
        try{pool.play(sample,.7f,.7f,1,0,1);}catch(RuntimeException ignored){}
    }
    void close(){closed=true;requested=false;if(pool!=null){pool.release();pool=null;}}
}
