package app.gap.clarity;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;

/** Shared presentation primitives. No budget or protection decisions. */
final class Ui {
    static final int STYLE=0;
    static final String EDITION="清序";
    final Context c;
    final boolean dark=STYLE==2;
    final int bg=Color.parseColor("#F3F4F7"),card=Color.parseColor("#FFFFFF"),ink=Color.parseColor("#17181A"),sub=Color.parseColor("#696C73"),accent=Color.parseColor("#2168D5"),soft=Color.parseColor("#E7EFFC"),line=Color.parseColor("#E1E3E9");
    final int danger=Color.parseColor(dark?"#FFB4AB":"#B42318"),dangerBg=Color.parseColor(dark?"#482520":"#FFF0ED");
    Ui(Context c){this.c=c;}
    int dp(float x){return Math.round(x*c.getResources().getDisplayMetrics().density);}
    int radius(){return STYLE==0?24:STYLE==1?8:22;}
    void window(Activity a){
        a.getWindow().setStatusBarColor(bg);a.getWindow().setNavigationBarColor(bg);
        a.getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    GradientDrawable outlined(int color,int radius){GradientDrawable d=shape(color,radius);d.setStroke(dp(1),line);return d;}
    android.graphics.drawable.StateListDrawable pressSurface(int color,int radius){
        android.graphics.drawable.StateListDrawable d=new android.graphics.drawable.StateListDrawable();
        int pressed=android.graphics.Color.rgb((int)(Color.red(color)*.92f),(int)(Color.green(color)*.92f),(int)(Color.blue(color)*.92f));
        d.addState(new int[]{android.R.attr.state_pressed},shape(pressed,radius));
        d.addState(new int[]{android.R.attr.state_focused},shape(pressed,radius));
        d.addState(new int[]{},shape(color,radius));d.setExitFadeDuration(MotionHost.enabled()?130:0);return d;
    }
    LinearLayout column(){LinearLayout l=new LinearLayout(c);l.setOrientation(1);return l;}
    TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",0));t.setFontFeatureSettings("tnum");t.setLineSpacing(dp(3),1);return t;
    }
    TextView number(String s,int size){
        FitNumber t=new FitNumber(c);t.setText(s);t.setTextColor(ink);t.setTextSize(size);t.setIncludeFontPadding(false);t.setSingleLine();
        t.setTypeface(Typeface.create(STYLE==1?"sans-serif-black":"sans-serif",Typeface.BOLD));t.setFontFeatureSettings("tnum");t.setLetterSpacing(STYLE==1?-.055f:-.035f);return t;
    }
    void gap(LinearLayout p,int h){p.addView(new View(c),new LinearLayout.LayoutParams(1,dp(h)));}
    void hairline(LinearLayout p){View v=new View(c);v.setBackgroundColor(line);p.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    void section(LinearLayout p,String label){TextView t=text(label,12,sub,true);t.setLetterSpacing(.03f);p.addView(t);gap(p,12);}
    LinearLayout card(LinearLayout parent){LinearLayout l=column();l.setPadding(dp(22),dp(22),dp(22),dp(22));l.setBackground(STYLE==0?shape(card,radius()):outlined(card,radius()));parent.addView(l,new LinearLayout.LayoutParams(-1,-2));return l;}
    void accessibleButton(View v,String description){
        v.setFocusable(true);if(description!=null)v.setContentDescription(description);
        v.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo n){super.onInitializeAccessibilityNodeInfo(host,n);n.setClassName("android.widget.Button");}});
    }
    TextView button(String title,boolean primary,Runnable action){
        TextView b=text(title,16,primary?(dark?bg:Color.WHITE):ink,true);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(52));b.setPadding(dp(16),dp(14),dp(16),dp(14));
        b.setBackground(pressSurface(primary?accent:soft,STYLE==1?8:16));b.setOnClickListener(v->action.run());accessibleButton(b,null);b.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return b;
    }
    TextView link(String title,Runnable action){TextView t=text(title,14,STYLE==0?accent:sub,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setMinHeight(dp(48));t.setPadding(dp(8),dp(10),dp(8),dp(10));t.setBackground(pressSurface(bg,10));t.setOnClickListener(v->action.run());accessibleButton(t,null);return t;}
    void row(LinearLayout p,String title,String detail,Runnable action){
        LinearLayout l=new LinearLayout(c);l.setGravity(Gravity.CENTER_VERTICAL);l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setMinimumHeight(dp(76));
        l.setBackground(pressSurface(card,16));
        Icon icon=new Icon(this,title);l.addView(icon,new LinearLayout.LayoutParams(dp(32),dp(32)));View spacer=new View(c);l.addView(spacer,new LinearLayout.LayoutParams(dp(12),1));
        LinearLayout words=column();words.addView(text(title,16,ink,true));if(detail!=null){gap(words,5);words.addView(text(detail,12,sub,false));}l.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=text("›",23,sub,false);arrow.setPadding(dp(10),0,0,0);l.addView(arrow);l.setOnClickListener(v->action.run());accessibleButton(l,title+(detail==null?"":"，"+detail));
        p.addView(l,new LinearLayout.LayoutParams(-1,-2));gap(p,STYLE==0?3:10);
    }
    void tile(LinearLayout row,String title,String detail,Runnable action,boolean last){
        LinearLayout box=column();box.setPadding(dp(17),dp(18),dp(17),dp(18));box.setBackground(new RippleDrawable(ColorStateList.valueOf(dark?0x20FFFFFF:0x10000000),outlined(card,STYLE==1?8:20),null));
        LinearLayout head=new LinearLayout(c);head.setGravity(Gravity.CENTER_VERTICAL);head.addView(new Icon(this,title),new LinearLayout.LayoutParams(dp(28),dp(28)));TextView arrow=text("↗",19,sub,false);arrow.setGravity(Gravity.RIGHT);head.addView(arrow,new LinearLayout.LayoutParams(0,-2,1));box.addView(head);gap(box,16);box.addView(text(title,16,ink,true));gap(box,6);box.addView(text(detail,12,sub,false));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(!last)lp.rightMargin=dp(12);row.addView(box,lp);box.setOnClickListener(v->action.run());accessibleButton(box,title+"，"+detail);
    }
    void status(TextView view,int state){
        // state0 healthy,1 consent/service missing,2 paused,3 application/interception problem.
        if(Integer.valueOf(state).equals(view.getTag()))return;
        boolean error=state==1||state==3;int color=error?dangerBg:state==2?soft:STYLE==0?soft:card;
        GradientDrawable d=shape(color,STYLE==1?8:14);if(error)d.setStroke(dp(1),dark?0xFF9E544D:0xFFF0B3A9);else if(STYLE!=0)d.setStroke(dp(1),line);
        view.setTextColor(error?danger:state==0&&STYLE==0?accent:sub);view.setBackground(d);view.setTag(state);
    }
    void preview(LinearLayout page,Runnable action){
        LinearLayout box=new LinearLayout(c);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(12),dp(12),dp(16),dp(12));box.setBackground(new RippleDrawable(ColorStateList.valueOf(dark?0x22FFFFFF:0x10000000),outlined(card,STYLE==1?8:20),null));
        ImageView image=new ImageView(c);image.setImageResource(R.drawable.gap_photo);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(shape(soft,10));image.setClipToOutline(true);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);box.addView(image,new LinearLayout.LayoutParams(dp(56),dp(70)));
        LinearLayout words=column();words.setPadding(dp(14),0,0,0);words.addView(text(c.getString(R.string.brand_slogan),15,ink,true));gap(words,6);words.addView(text(c.getString(R.string.copy_102),12,sub,false));box.addView(words,new LinearLayout.LayoutParams(0,-2,1));box.addView(text("↗",21,sub,false));page.addView(box,new LinearLayout.LayoutParams(-1,-2));box.setOnClickListener(v->action.run());accessibleButton(box,c.getString(R.string.copy_103));
    }
    static String minutes(long millis){long s=(Math.max(0,millis)+999)/1000;return String.format(java.util.Locale.US,"%02d:%02d",s/60,s%60);}
    static final class FitNumber extends TextView {
        float desiredSp=60;boolean adjusting=false;
        FitNumber(Context c){super(c);}
        @Override public void setTextSize(float size){desiredSp=size;super.setTextSize(size);}
        @Override protected void onMeasure(int ws,int hs){
            int width=MeasureSpec.getSize(ws)-getPaddingLeft()-getPaddingRight();
            if(width>0){float px=desiredSp*getResources().getDisplayMetrics().scaledDensity;Paint p=new Paint(getPaint());p.setTextSize(px);float measured=p.measureText(getText().toString());if(measured>width)px*=width/measured;super.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Math.max(1,px));}
            super.onMeasure(ws,hs);
        }
    }
    static final class Meter extends View {
        final Ui u;final Paint p=new Paint(3);final boolean ticks;float fraction=1;
        Meter(Ui u,boolean ticks){super(u.c);this.u=u;this.ticks=ticks;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void setFraction(double f){fraction=(float)Math.max(0,Math.min(1,f));invalidate();}
        protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();if(ticks){int count=40;float step=w/count;for(int i=0;i<count;i++){p.setColor(i<count*fraction?u.accent:u.line);float top=i%5==0?0:h*.3f;c.drawRoundRect(i*step,top,i*step+step*.48f,h,1,1,p);}}else{p.setColor(u.soft);c.drawRoundRect(0,0,w,h,h/2,h/2,p);p.setColor(u.accent);c.drawRoundRect(0,0,w*fraction,h,h/2,h/2,p);}}
    }
    static final class Orbit extends View {
        final Ui u;final Paint p=new Paint(3);Orbit(Ui u){super(u.c);this.u=u;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.7f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(u.dp(2));p.setColor(u.line);c.drawCircle(x,y,r,p);p.setColor(u.accent);c.drawArc(x-r,y-r,x+r,y+r,-70,290,false,p);p.setStrokeWidth(u.dp(4));c.drawLine(x-r*.2f,y-r*.25f,x-r*.2f,y+r*.25f,p);c.drawLine(x+r*.2f,y-r*.25f,x+r*.2f,y+r*.25f,p);}
    }
    static final class Icon extends View {
        final Ui u;final String title;final Paint p=new Paint(3);Icon(Ui u,String t){super(u.c);this.u=u;title=t;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas canvas){canvas.save();canvas.scale(getWidth()/32f,getHeight()/32f);p.setColor(u.STYLE==0?0xFFF1F2F3:u.bg);p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(0,0,32,32,8,8,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(u.ink);
            if(title.equals(u.c.getString(R.string.protect_target))){canvas.drawRoundRect(10,6,22,26,3,3,p);canvas.drawLine(14,22,18,22,p);}else if((title.equals(u.c.getString(R.string.rules_entry))||title.equals(u.c.getString(R.string.copy_091)))){for(int i=0;i<3;i++){float y=10+i*6;canvas.drawLine(7,y,25,y,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(i==1?20:12,y,2.3f,p);p.setStyle(Paint.Style.STROKE);}}else{Path path=new Path();path.moveTo(16,5);path.lineTo(25,9);path.lineTo(24,19);path.quadTo(23,25,16,28);path.quadTo(9,25,8,19);path.lineTo(7,9);path.close();canvas.drawPath(path,p);canvas.drawLine(12,16,15,19,p);canvas.drawLine(15,19,21,12,p);}canvas.restore();}
    }
}
