package app.gap.clarity;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Locked photograph plus a live native-text layer. No timer digits in the bitmap. */
final class RestArtwork extends View {
    private final Bitmap background;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private String timer="10:00";
    private boolean hoursFormat;
    private TimerSlots slots;
    private int layoutWidth,layoutHeight;
    RestArtwork(Context c){super(c);background=BitmapFactory.decodeResource(c.getResources(),R.drawable.gap_rest);setFocusable(true);setContentDescription(getContext().getString(R.string.copy_106));}
    void setTimer(String text,boolean daily){
        if(!daily&&text.length()==8)hoursFormat=true;
        if(!daily&&hoursFormat&&text.length()==5)text="00:"+text;
        if(timer.equals(text))return;timer=text;setContentDescription(daily?getContext().getString(R.string.copy_107):getContext().getString(R.string.copy_108)+text);invalidate();
    }
    @Override protected void onDraw(Canvas canvas){
        float w=getWidth(),h=getHeight();canvas.drawColor(0xFFE5E4DB);
        if(background!=null){float scale=Math.max(w/background.getWidth(),h/background.getHeight());float bw=background.getWidth()*scale,bh=background.getHeight()*scale;canvas.drawBitmap(background,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);}
        paint.setColor(Color.BLACK);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(Paint.Align.CENTER);
        if(timer.matches("[0-9]{2}:[0-9]{2}(:[0-9]{2})?")){
            if(slots==null||slots.centers.length!=timer.length()||layoutWidth!=getWidth()||layoutHeight!=getHeight()){
                paint.setTextSize(100);float digit=0;for(int i=0;i<10;i++)digit=Math.max(digit,paint.measureText(Integer.toString(i))/100);
                Paint.FontMetrics fm=paint.getFontMetrics();
                slots=new TimerSlots(w,h,digit,paint.measureText(":")/100,fm.ascent/100,fm.descent/100,timer.length());
                layoutWidth=getWidth();layoutHeight=getHeight();
            }
            paint.setTextSize(slots.size);
            for(int i=0;i<timer.length();i++)canvas.drawText(timer,i,i+1,slots.centers[i],slots.baseline,paint);
        }else{
            float size=h*.145f;paint.setTextSize(size);float measured=paint.measureText(timer);if(measured>w*.85f)paint.setTextSize(size*w*.85f/measured);
            Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText(timer,w/2,h*.15f-(fm.ascent+fm.descent)/2,paint);
        }
    }
}
