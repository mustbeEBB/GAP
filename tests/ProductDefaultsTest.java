package app.gap.clarity;

import java.time.ZoneId;

public final class ProductDefaultsTest {
    static int checks=0;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        BudgetEngine e=new BudgetEngine();e.dailyLimit=ProductRules.DAILY;e.batchLimit=ProductRules.PORTION;e.cooldownLength=ProductRules.GAP;e.remaining=e.batchLimit;
        long wall=java.time.Instant.parse("2026-10-24T02:00:00Z").toEpochMilli(),mono=1000;ZoneId zone=ZoneId.of("Asia/Shanghai");
        e.tick(wall,mono,1,false,zone);check(e.available()==300000,"fresh portion is5 minutes");check(e.dailyRemaining()==3600000,"daily limit is60 minutes");
        wall+=120000;mono+=120000;e.tick(wall,mono,1,true,zone);check(e.available()==180000,"2 used leaves3");
        wall+=600000;mono+=600000;e.tick(wall,mono,1,false,zone);check(e.available()==180000,"leaving does not consume or refill");
        wall+=180000;mono+=180000;e.tick(wall,mono,1,true,zone);check(e.cooling(wall,mono,1)==600000,"exhaustion starts10-minute GAP");
        wall+=120000;mono+=120000;e.tick(wall,mono,1,false,zone);e.changeRules(ProductRules.DAILY,ProductRules.PORTION,180000);check(e.cooling(wall,mono,1)==60000,"editing GAP preserves2 minutes already waited");
        wall+=60000;mono+=60000;e.tick(wall,mono,1,false,zone);check(e.available()==300000,"completed GAP grants exactly one5-minute portion");check(e.dailyRemaining()==3300000,"first5-minute use remains deducted");
        System.out.println("PASS: "+checks+" product-default integration assertions");
    }
}
