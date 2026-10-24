package app.gap.clarity;
import java.time.*;

public final class BudgetEngineTest {
    static int count;static final ZoneId Z=ZoneId.of("Asia/Shanghai");
    static long start=ZonedDateTime.of(2026,10,20,10,0,0,0,Z).toInstant().toEpochMilli();
    static void eq(long expected,long actual,String name){if(expected!=actual)throw new AssertionError(name+": expected "+expected+", actual "+actual);count++;}
    static BudgetEngine fresh(){BudgetEngine e=new BudgetEngine();e.tick(start,0,1,false,Z);return e;}
    public static void main(String[] args){
        BudgetEngine e=fresh();eq(600000,e.available(),"initial batch");eq(3600000,e.dailyRemaining(),"initial daily");
        e.tick(start+180000,180000,1,true,Z);eq(420000,e.available(),"3 minutes used");
        e.tick(start+3780000,3780000,1,false,Z);eq(420000,e.available(),"away does not spend or refill");
        e.tick(start+4200000,4200000,1,true,Z);eq(0,e.available(),"batch exhausted");eq(7200000,e.cooling(start+4200000,4200000,1),"2h cooldown");
        e.tick(start+4300000,4300000,1,false,Z);eq(7100000,e.cooling(start+4300000,4300000,1),"cooldown advances while away");
        e.tick(start+11400000,11400000,1,false,Z);eq(600000,e.available(),"restore exactly one batch");
        e.tick(start+30000000,30000000,1,false,Z);eq(600000,e.available(),"no stockpile");
        e=fresh();long t=0;for(int i=0;i<6;i++){t+=600000;e.tick(start+t,t,1,true,Z);if(i<5){t+=7200000;e.tick(start+t,t,1,false,Z);}}
        eq(0,e.dailyRemaining(),"six batches reach daily limit");t+=7200000;e.tick(start+t,t,1,false,Z);eq(0,e.available(),"no seventh grant");
        e=fresh();e.tick(start+600000,600000,1,true,Z);long wall=e.coolWall; e.tick(start+610000,10000,2,false,Z);eq(wall-(start+610000),e.cooling(start+610000,10000,2),"reboot deadline survives");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.tick(start+86400000,600001,1,false,Z);eq(600000,e.available(),"midnight clears cooldown");eq(0,e.used,"midnight clears daily use");
        e=fresh();e.tick(start+10000,10000,1,true,Z);e.tick(start-86400000,20000,1,false,Z);eq(10000,e.used,"backdate does not reset");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.tick(start+3000000,610000,1,false,Z);eq(7190000,e.cooling(start+3000000,610000,1),"same boot cooldown uses monotonic time");
        e=fresh();e.tick(start+180000,180000,1,true,Z);e.changeRules(3600000,1200000,7200000);eq(420000,e.available(),"rule edit does not refill");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.changeRules(3600000,600000,60000);eq(60000,e.cooling(start+600000,600000,1),"gap edit applies to active cooldown");
        long beforeMidnight=ZonedDateTime.of(2026,10,20,23,59,59,0,Z).toInstant().toEpochMilli();
        e=new BudgetEngine();e.tick(beforeMidnight,0,1,false,Z);e.tick(beforeMidnight+2000,2000,1,true,Z);eq(1000,e.used,"split charge at midnight");
        e=fresh();e.used=3599000;e.remaining=600000;e.tick(start+2000,2000,1,true,Z);eq(0,e.dailyRemaining(),"partial final grant cannot overspend");
        e=fresh();e.tick(start+1500000,1500000,1,true,Z);eq(6300000,e.cooling(start+1500000,1500000,1),"cooldown starts at exhaustion not late callback");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.tick(start+1200000,1200000,1,false,Z);
        e.changeRules(3600000,600000,1800000);eq(1200000,e.cooling(start+1200000,1200000,1),"waited 10 min: new 30 min gap leaves 20");
        eq(600000,e.used,"gap edit preserves daily consumption");
        e.tick(start+1320000,1320000,1,false,Z);e.changeRules(3600000,600000,3600000);
        eq(2880000,e.cooling(start+1320000,1320000,1),"repeated edit keeps original start: waited 12 min leaves 48");
        e.changeRules(3600000,600000,600000);eq(0,e.cooling(start+1320000,1320000,1),"wait beyond new gap clears immediately");
        eq(600000,e.available(),"completed new gap restores exactly one batch");eq(3000000,e.dailyRemaining(),"restoration does not reset daily balance");
        e.tick(start+1440000,1440000,1,true,Z);e.changeRules(3600000,600000,600000);
        eq(480000,e.available(),"editing again after restoration cannot refill current batch");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.tick(start+2400000,2400000,1,false,Z);e.changeRules(3600000,600000,1800000);
        eq(600000,e.available(),"wait exactly new gap restores immediately");
        e=fresh();e.used=3000000;e.tick(start+600000,600000,1,true,Z);e.tick(start+1200000,1200000,1,false,Z);e.changeRules(3600000,600000,300000);
        eq(0,e.available(),"shorter gap cannot bypass exhausted daily limit");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.used=3300000;e.tick(start+1200000,1200000,1,false,Z);e.changeRules(3600000,600000,300000);
        eq(300000,e.available(),"immediate restoration is capped by daily remainder");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.tick(start+1200000,10000,2,false,Z);e.changeRules(3600000,600000,1800000);
        eq(1200000,e.cooling(start+1200000,10000,2),"after reboot edit preserves elapsed cooldown");
        BudgetEngine restored=new BudgetEngine();restored.day=e.day;restored.used=e.used;restored.remaining=e.remaining;restored.cooldownLength=e.cooldownLength;restored.coolWall=e.coolWall;restored.coolMono=e.coolMono;restored.coolBoot=e.coolBoot;
        restored.tick(start+1260000,70000,2,false,Z);eq(1140000,restored.cooling(start+1260000,70000,2),"edited deadline survives state reload");
        e=fresh();e.tick(start+180000,180000,1,true,Z);e.changeRules(3600000,600000,1800000);eq(420000,e.available(),"gap edit outside cooldown does not refill");
        e=fresh();e.tick(start+600000,600000,1,true,Z);e.finishCooldown();eq(600000,e.available(),"legacy migration releases one batch");eq(600000,e.used,"legacy migration preserves daily use");
        e.tick(start+660000,660000,1,true,Z);e.finishCooldown();eq(540000,e.available(),"repeated migration is idempotent");
        e=fresh();e.used=3000000;e.tick(start+600000,600000,1,true,Z);e.finishCooldown();eq(0,e.available(),"migration cannot bypass daily cap");
        System.out.println("PASS: "+count+" meaningful budget assertions");
    }
}
