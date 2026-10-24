package app.gap.clarity;

public final class NavigationStateTest {
    static int count;
    static void equal(Object expected,Object actual){count++;if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
    public static void main(String[] args){
        NavigationState n=new NavigationState();
        equal(0,n.visit("home"));equal(1,n.visit("rules"));equal(1,n.visit("sandbox"));
        equal("rules",n.parent());equal(-1,n.visit("rules"));equal("home",n.parent());
        equal(0,n.visit("rules"));equal("home",n.parent());equal(-1,n.visit("home"));equal("",n.parent());
        equal(1,n.visit("permissions"));equal(0,n.visit("permissions"));equal("home",n.parent());
        equal(-1,n.visit("home"));equal(1,n.visit("intro:0"));equal(1,n.visit("intro:1"));equal(-1,n.visit("intro:0"));equal(-1,n.visit("home"));equal("",n.parent());
        n=new NavigationState();equal(0,n.visit("intro:0"));equal(1,n.visit("intro:1"));equal(-1,n.visit("home"));equal("",n.parent());
        System.out.println("PASS: "+count+" navigation assertions");
    }
}
