package app.gap.clarity;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

/** Opaque navigation surfaces: Settings-like push/parallax and its exact reverse. */
final class MotionHost extends FrameLayout {
    private View current;
    private ValueAnimator animation;
    private Runnable completion;
    private boolean pending;
    private int generation;
    MotionHost(Context c){super(c);setClipChildren(true);setBackgroundColor(0xFFF3F4F7);}
    static boolean enabled(){return ValueAnimator.areAnimatorsEnabled();}
    void settle(){
        generation++;pending=false;
        if(animation!=null){animation.removeAllListeners();animation.cancel();animation=null;}
        if(completion!=null){Runnable done=completion;completion=null;done.run();}
    }
    void show(View next,int direction){
        settle();final View old=current;current=next;
        if(old==null||direction==0||!enabled()){
            removeAllViews();addView(next,new LayoutParams(-1,-1));return;
        }
        // Pop reveals the destination behind the outgoing page; push does the opposite.
        if(direction<0)addView(next,0,new LayoutParams(-1,-1));else addView(next,new LayoutParams(-1,-1));
        old.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        old.setLayerType(View.LAYER_TYPE_HARDWARE,null);next.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        final float sign=getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?-1:1;
        completion=()->{
            removeView(old);old.setTranslationX(0);old.setAlpha(1);old.setLayerType(View.LAYER_TYPE_NONE,null);
            old.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            next.setVisibility(VISIBLE);next.setTranslationX(0);next.setAlpha(1);next.setElevation(0);next.setLayerType(View.LAYER_TYPE_NONE,null);
        };
        pending=true;next.setVisibility(INVISIBLE);final int token=generation;
        next.post(()->{
            if(token!=generation)return;
            pending=false;next.setVisibility(VISIBLE);
            final float width=getWidth();
            if(width<=0){settle();return;}
            animation=ValueAnimator.ofFloat(0,1);animation.setDuration(360);
            animation.setInterpolator(new PathInterpolator(.22f,.72f,.18f,1));
            animation.addUpdateListener(a->{
                float t=(float)a.getAnimatedValue();
                if(direction>0){next.setTranslationX(sign*width*(1-t));old.setTranslationX(-sign*width*.28f*t);old.setAlpha(1-.08f*t);}
                else{old.setTranslationX(sign*width*t);next.setTranslationX(-sign*width*.28f*(1-t));next.setAlpha(.92f+.08f*t);}
            });
            animation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){settle();}});
            animation.start();
        });
    }
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){
        // Prevent double navigation or taps reaching a partially exposed outgoing surface.
        if(pending||animation!=null)return true;
        return super.dispatchTouchEvent(e);
    }
    @Override protected void onDetachedFromWindow(){settle();super.onDetachedFromWindow();}
}
