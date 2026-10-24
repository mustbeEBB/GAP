package app.gap.clarity;

import android.app.Activity;
import android.os.Build;
import android.view.*;
import android.widget.*;

final class RestScreen {
    private static final int MOTION_MS=280;
    private static final android.view.animation.Interpolator EASE=new android.view.animation.PathInterpolator(.22f,.72f,.18f,1);
    private static final class Surface extends FrameLayout {
        RestArtwork art;TextView button;boolean exiting;Runnable exitCompletion;
        Surface(Activity a){super(a);setBackgroundColor(0xFFE5E4DB);}
        void enter(){
            if(!MotionHost.enabled())return;
            art.setAlpha(0);art.setScaleX(1.035f);art.setScaleY(1.035f);button.setAlpha(0);
            art.animate().alpha(1).scaleX(1).scaleY(1).setDuration(MOTION_MS).setInterpolator(EASE).start();
            button.animate().alpha(1).setDuration(MOTION_MS).setInterpolator(EASE).start();
        }
        void dismiss(Runnable done){
            if(exiting)return;exiting=true;exitCompletion=done;
            art.animate().cancel();button.animate().cancel();button.setEnabled(false);
            // Keep the photograph covering the window until the desktop takes over.
            art.setAlpha(1);button.setAlpha(1);
            if(!MotionHost.enabled()){complete();return;}
            art.animate().scaleX(1.035f).scaleY(1.035f).setDuration(MOTION_MS).setInterpolator(EASE).withEndAction(this::complete).start();
        }
        void complete(){Runnable done=exitCompletion;exitCompletion=null;if(done!=null)done.run();}
        @Override protected void onDetachedFromWindow(){art.animate().cancel();button.animate().cancel();complete();super.onDetachedFromWindow();}
    }
    static void dismiss(Activity a,Runnable done){
        ViewGroup content=a.findViewById(android.R.id.content);
        if(content!=null&&content.getChildCount()>0&&content.getChildAt(0) instanceof Surface)((Surface)content.getChildAt(0)).dismiss(done);else done.run();
    }
    static RestArtwork install(Activity a,String action,Runnable click){
        a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFFE5E4DB));
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        WindowManager.LayoutParams attrs=a.getWindow().getAttributes();
        if(Build.VERSION.SDK_INT>=30)attrs.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        else if(Build.VERSION.SDK_INT>=28)attrs.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        a.getWindow().setAttributes(attrs);if(Build.VERSION.SDK_INT>=30)a.getWindow().setDecorFitsSystemWindows(false);
        Ui u=new Ui(a);Surface root=new Surface(a);RestArtwork art=new RestArtwork(a);root.art=art;root.addView(art,new FrameLayout.LayoutParams(-1,-1));
        TextView button=u.text(action,14,android.graphics.Color.BLACK,true);button.setGravity(Gravity.CENTER);button.setMinHeight(u.dp(48));button.setPadding(u.dp(24),u.dp(12),u.dp(24),u.dp(12));
        android.graphics.drawable.GradientDrawable surface=u.shape(0xEBFFFFFF,24);surface.setStroke(u.dp(1),0x22000000);button.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22000000),surface,null));button.setOnClickListener(v->click.run());u.accessibleButton(button,null);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);bp.bottomMargin=u.dp(20);root.addView(button,bp);
        root.setOnApplyWindowInsetsListener((v,insets)->{int safe=0;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets cut=insets.getInsets(WindowInsets.Type.displayCutout());safe=cut.bottom;}bp.bottomMargin=Math.max(u.dp(20),safe+u.dp(8));button.setLayoutParams(bp);return insets;});
        root.button=button;a.setContentView(root);root.requestApplyInsets();immersive(a);
        // The surface stays fully opaque; only its artwork and control animate above it.
        root.enter();
        return art;
    }
    static void immersive(Activity a){
        if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=a.getWindow().getInsetsController();if(c!=null){c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);c.hide(WindowInsets.Type.systemBars());}}
        else a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
