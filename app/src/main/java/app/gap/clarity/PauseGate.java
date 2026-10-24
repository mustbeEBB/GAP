package app.gap.clarity;

/** Three deliberate confirmations, each with its own monotonic waiting period. */
public final class PauseGate {
    private int stage=0;
    private long readyAt=0;
    public void start(long now){stage=1;readyAt=now+3000;}
    public int stage(){return stage;}
    public long waitMillis(long now){return Math.max(0,readyAt-now);}
    public boolean confirm(long now){
        if(stage<1||stage>3||now<readyAt)return false;
        if(stage==3){stage=4;return true;}
        stage++;readyAt=now+3000;return false;
    }
    public void cancel(){stage=0;readyAt=0;}
}
