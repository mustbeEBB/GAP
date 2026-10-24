package app.gap.clarity;
public final class PauseGateTest {
    private static int count;
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);count++;}
    public static void main(String[] args){
        PauseGate g=new PauseGate();
        check(!g.confirm(10000),"no confirmation before entering");
        g.start(1000);check(g.stage()==1,"first stage");
        check(!g.confirm(3999)&&g.stage()==1,"cannot bypass first wait");
        check(!g.confirm(4000)&&g.stage()==2,"first confirmation alone does not pause");
        check(!g.confirm(4000)&&g.stage()==2,"double tap cannot skip second wait");
        check(!g.confirm(7000)&&g.stage()==3,"second confirmation alone does not pause");
        check(!g.confirm(9999)&&g.stage()==3,"third stage requires its own wait");
        check(g.confirm(10000)&&g.stage()==4,"exactly three deliberate confirmations complete");
        check(!g.confirm(20000),"cannot commit twice");
        g.start(30000);g.confirm(33000);g.cancel();check(!g.confirm(36000)&&g.stage()==0,"cancel resets progress");
        g.start(40000);check(g.stage()==1&&g.waitMillis(40000)==3000,"reopening starts over");
        System.out.println("PASS: "+count+" pause-confirmation assertions");
    }
}
