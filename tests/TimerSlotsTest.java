package app.gap.clarity;

public final class TimerSlotsTest {
    static int count;
    static void near(float expected,float actual){count++;if(Math.abs(expected-actual)>.002f)throw new AssertionError(expected+" != "+actual);}
    static void check(boolean value){count++;if(!value)throw new AssertionError();}
    public static void main(String[] args){
        TimerSlots s=new TimerSlots(1216,2640,.64f,.28f,-.94f,.24f,5);
        String[] times={"00:54","00:36","00:32","00:21","00:20","00:19","10:00","09:59","00:01","00:00"};
        // All glyphs have fixed independent centers, including the colon; no text-width input.
        for(String time:times){TimerSlots next=new TimerSlots(1216,2640,.64f,.28f,-.94f,.24f,time.length());near(s.size,next.size);near(s.baseline,next.baseline);for(int i=0;i<5;i++)near(s.centers[i],next.centers[i]);}
        near(s.size*.64f,s.centers[1]-s.centers[0]);
        near(s.size*(.64f+.28f)/2,s.centers[2]-s.centers[1]);
        near(1216,s.centers[0]+s.centers[4]);
        near(2640*.15f,s.baseline+(-.94f+.24f)*s.size/2);
        check(s.centers[0]-s.size*.64f/2>=1216*.075f-.002f);
        TimerSlots hours=new TimerSlots(1216,2640,.64f,.28f,-.94f,.24f,8);
        check(hours.size<s.size);near(1216,hours.centers[0]+hours.centers[7]);
        near(hours.size*.28f,hours.centers[3]-hours.centers[2]-(hours.size*(.64f-.28f)/2));
        TimerSlots small=new TimerSlots(360,780,.64f,.28f,-.94f,.24f,5);
        check(small.centers[4]+small.size*.64f/2<=360*.925f+.002f);
        System.out.println("PASS: "+count+" fixed-clock geometry assertions");
    }
}
