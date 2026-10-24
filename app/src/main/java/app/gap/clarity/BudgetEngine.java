package app.gap.clarity;

import java.time.*;

/** Pure budget state machine. All durations are milliseconds. No Android dependency. */
public final class BudgetEngine {
    public long dailyLimit=3600000L, batchLimit=600000L, cooldownLength=7200000L;
    public long used=0, remaining=600000L, coolWall=0, coolMono=0;
    public int coolBoot=-1;
    public String day="";
    private long previousWall=-1, previousMono=-1;
    private int previousBoot=-1;

    public long dailyRemaining() { return Math.max(0,dailyLimit-used); }
    public long available() { return Math.max(0, Math.min(remaining,dailyRemaining())); }
    public long cooling(long wall,long mono,int boot) {
        if(coolWall==0) return 0;
        return Math.max(0, coolBoot==boot ? coolMono-mono : coolWall-wall);
    }
    public boolean blocked(long wall,long mono,int boot) {
        return dailyRemaining()==0 || cooling(wall,mono,boot)>0 || available()==0;
    }
    public void finishCooldown() {
        if(coolWall==0)return;
        coolWall=0;coolMono=0;coolBoot=-1;
        remaining=Math.min(batchLimit,dailyRemaining());
    }
    public void tick(long wall,long mono,int boot,boolean wasUsing,ZoneId zone) {
        LocalDate today=Instant.ofEpochMilli(wall).atZone(zone).toLocalDate();
        long delta=previousMono>=0 && previousBoot==boot ? Math.max(0,mono-previousMono):0;
        boolean reset=day.isEmpty() || today.toString().compareTo(day)>0;
        if(reset) {
            day=today.toString(); used=0; remaining=batchLimit; coolWall=0; coolMono=0; coolBoot=-1;
            long midnight=today.atStartOfDay(zone).toInstant().toEpochMilli();
            delta=Math.min(delta, Math.max(0,wall-midnight));
        }
        // A backward date change never grants fresh quota.
        if(coolWall!=0 && cooling(wall,mono,boot)==0) {
            finishCooldown();
            // Do not bill the blocked interval that just ended.
            delta=0;
        }
        if(wasUsing && coolWall==0 && available()>0 && delta>0) {
            long cost=Math.min(delta,available());
            used+=cost; remaining-=cost;
            if(remaining<=0 || dailyRemaining()==0) {
                long overshoot=delta-cost;
                coolWall=wall-overshoot+cooldownLength;
                coolMono=mono-overshoot+cooldownLength;
                coolBoot=boot;
            }
        }
        previousWall=wall; previousMono=mono; previousBoot=boot;
    }
    public void changeRules(long daily,long batch,long cooldown) {
        if(daily<=0 || batch<=0 || batch>daily || cooldown<=0) throw new IllegalArgumentException("规则范围不合法");
        // Call tick with current clocks before editing (StateStore.refresh does this).
        // Both persisted deadlines refer to the original start plus the old gap.
        // Moving them by only the gap difference preserves the time already waited,
        // including across repeated edits and the wall-clock fallback after a reboot.
        if(coolWall!=0){
            long difference=cooldown-cooldownLength;
            coolWall+=difference;coolMono+=difference;
        }
        dailyLimit=daily; batchLimit=batch; cooldownLength=cooldown;
        remaining=Math.min(remaining,batch);
        if(coolWall!=0 && cooling(previousWall,previousMono,previousBoot)==0){
            // A shorter gap may complete this cooldown immediately, but never
            // grant beyond today's balance or accumulate multiple batches.
            finishCooldown();
        }
    }
}
