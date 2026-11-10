package me.deltabilyard;

import java.util.*;

/** Reproducible comparison on the same scattered positions; not a human win-rate claim. */
public final class ExpertBenchmark {
 public static void main(String[] args) {
  int perRule=args.length==0?12:Integer.parseInt(args[0]);
  if(perRule<1||perRule>100)throw new IllegalArgumentException("Use 1..100 positions per rule");
  // Warm up both branches before measuring search latency.
  var warm=position("nine",17);
  for(int i=0;i<3;i++)for(String level:List.of("pro","expert"))AimBot.choose(warm,0,false,level,7,"nine");
  for(String level:List.of("pro","expert")){
   int fouls=0,scoring=0,wins=0,pots=0;var times=new ArrayList<Double>();
   for(String rules:List.of("eight","nine","free"))for(int i=0;i<perRule;i++){
    var balls=position(rules,20261110L+i);int group=rules.equals("eight")?1:0;
    long start=System.nanoTime();var plan=AimBot.choose(balls,group,false,level,20261110L+i,rules);
    times.add((System.nanoTime()-start)/1_000_000.0);
    var shot=Physics.simulate(balls,plan.angle(),plan.power(),plan.spinX(),plan.spinY());
    var result=Rules.evaluate(rules,balls,shot,0,group,false,plan.pocket(),0,8);
    if(result.foul())fouls++;
    if(result.winner()==0)wins++;
    if(!result.foul()){
     if(result.continues()||result.winner()==0)scoring++;
     for(int id:Rules.pocketedObjects(shot))if(!rules.equals("eight")||Rules.group(id)==group)pots++;
    }
   }
   Collections.sort(times);int n=times.size();
   System.out.printf(Locale.ROOT,"{\"level\":\"%s\",\"positions\":%d,\"scoringShots\":%d,\"legalPots\":%d,\"fouls\":%d,\"wins\":%d,\"searchMedianMs\":%.2f,\"searchP95Ms\":%.2f,\"searchMaxMs\":%.2f}%n",level,n,scoring,pots,fouls,wins,times.get(n/2),times.get(Math.min(n-1,(int)Math.ceil(n*.95)-1)),times.get(n-1));
  }
 }
 static List<Physics.Ball> position(String rules,long seed){
  var balls=Physics.rack(rules);var placed=new ArrayList<Physics.Ball>();var random=new Random(seed);
  for(var ball:balls){
   boolean valid=false;
   for(int attempts=0;attempts<10000&&!valid;attempts++){
    ball.x=.09+random.nextDouble()*(Physics.W-.18);ball.y=.09+random.nextDouble()*(Physics.H-.18);
    valid=placed.stream().noneMatch(b->Math.hypot(b.x-ball.x,b.y-ball.y)<Physics.R*2.2);
   }
   if(!valid)throw new IllegalStateException("Fixture could not be placed");
   placed.add(ball);
  }
  return balls;
 }
}
