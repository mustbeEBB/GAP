package app.gap.clarity;

/** Fixed clock geometry, independent of the current digit shapes. */
final class TimerSlots {
    final float size,baseline;
    final float[] centers;
    TimerSlots(float width,float height,float digitAtUnit,float colonAtUnit,float ascentAtUnit,float descentAtUnit,int length){
        int colons=length==8?2:1;
        float units=(length-colons)*digitAtUnit+colons*colonAtUnit;
        size=Math.min(height*.145f,width*.85f/units);
        baseline=height*.15f-(ascentAtUnit+descentAtUnit)*size/2;
        centers=new float[length];float x=(width-units*size)/2;
        for(int i=0;i<length;i++){
            boolean colon=length==8?(i==2||i==5):i==2;
            float advance=(colon?colonAtUnit:digitAtUnit)*size;
            centers[i]=x+advance/2;x+=advance;
        }
    }
}
