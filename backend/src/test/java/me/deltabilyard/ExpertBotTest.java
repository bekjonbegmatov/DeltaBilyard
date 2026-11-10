package me.deltabilyard;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpertBotTest {
 private static List<Physics.Ball> position(String rules,double[]... points){
  var balls=Physics.rack(rules);for(var ball:balls)ball.pocketed=true;
  for(var point:points){var ball=balls.get((int)point[0]);ball.x=point[1];ball.y=point[2];ball.pocketed=false;}
  return balls;
 }
 private static Physics.Shot shoot(List<Physics.Ball> balls,AimBot.Plan plan){return Physics.simulate(balls,plan.angle(),plan.power(),plan.spinX(),plan.spinY());}
 private static Rules.Result result(String rules,List<Physics.Ball> balls,AimBot.Plan plan,Physics.Shot shot,int group){return Rules.evaluate(rules,balls,shot,0,group,false,plan.pocket(),0,8);}

 @Test void solvesThinCutThatProMissesUsingVerifiedSpin(){
  var balls=position("eight",new double[]{0,1.141326905,.992489192},new double[]{1,2.286365944,.710022451},new double[]{8,1.182946242,.295241627},new double[]{9,1.052671616,.631116505});
  var pro=AimBot.choose(balls,1,false,"pro",123,"eight");
  assertFalse(result("eight",balls,pro,shoot(balls,pro),1).continues());
  var expert=AimBot.choose(balls,1,false,"expert",123,"eight");var shot=shoot(balls,expert);
  assertTrue(result("eight",balls,expert,shot,1).continues());
  assertEquals(1,shot.firstContact());assertTrue(shot.balls().get(1).pocketed);assertFalse(shot.balls().get(0).pocketed);
  assertNotEquals(0,expert.spinY());
 }

 @Test void canKickOffCushionAndStillPotLegalBall(){
  var balls=position("eight",new double[]{0,2.292721097,.793537479},new double[]{1,.383947683,.384330038},new double[]{8,.586564138,.226786374},new double[]{9,1.249071739,.554617231});
  var expert=AimBot.choose(balls,1,false,"expert",7,"eight");var shot=shoot(balls,expert);
  double contact=shot.events().stream().filter(e->e.type().equals("collision")).mapToDouble(Physics.Event::t).findFirst().orElseThrow();
  assertTrue(shot.events().stream().anyMatch(e->e.type().equals("rail")&&e.ball()==0&&e.t()<contact));
  assertEquals(1,shot.firstContact());assertTrue(result("eight",balls,expert,shot,1).continues());
  var pro=AimBot.choose(balls,1,false,"pro",123,"eight");assertTrue(result("eight",balls,pro,shoot(balls,pro),1).foul());
 }

 @Test void findsLegalBankWhenDirectProShotMisses(){
  var balls=position("eight",new double[]{0,1.937471643,1.136173519},new double[]{1,2.120242484,1.162755751},new double[]{8,1.253105118,.425245842},new double[]{9,.441479981,.255882930});
  var expert=AimBot.choose(balls,1,false,"expert",7,"eight");var shot=shoot(balls,expert);
  double pot=shot.events().stream().filter(e->e.type().equals("pocket")&&e.ball()==1).mapToDouble(Physics.Event::t).findFirst().orElseThrow();
  assertTrue(shot.events().stream().anyMatch(e->e.type().equals("rail")&&e.ball()==1&&e.t()<pot));
  assertEquals(1,shot.firstContact());assertTrue(result("eight",balls,expert,shot,1).continues());assertFalse(shot.balls().get(8).pocketed);
  var pro=AimBot.choose(balls,1,false,"pro",123,"eight");assertFalse(result("eight",balls,pro,shoot(balls,pro),1).continues());
 }

 @Test void legalNineCombinationWinsWithoutTouchingNineFirst(){
  var balls=position("nine",new double[]{0,1.27,1},new double[]{1,1.27,.7},new double[]{9,1.27,.25});
  var plan=AimBot.choose(balls,0,false,"expert",42,"nine");var shot=shoot(balls,plan);
  assertEquals(1,shot.firstContact());assertTrue(shot.balls().get(9).pocketed);assertEquals(0,result("nine",balls,plan,shot,0).winner());
 }

 @Test void callsFinalEightButDoesNotTargetItEarly(){
  var balls=position("eight",new double[]{0,1.27,.9},new double[]{8,1.27,.3},new double[]{9,.4,.8});
  var plan=AimBot.choose(balls,1,false,"expert",42,"eight");var shot=shoot(balls,plan);
  assertEquals(8,plan.target());assertEquals(0,result("eight",balls,plan,shot,1).winner());
  assertTrue(shot.events().stream().anyMatch(e->e.type().equals("pocket")&&e.ball()==8&&e.pocket()==plan.pocket()));
  balls.get(1).pocketed=false;balls.get(1).x=.8;balls.get(1).y=.5;
  var early=AimBot.choose(balls,1,false,"expert",42,"eight");var earlyShot=shoot(balls,early);
  assertEquals(1,early.target());assertNotEquals(1,result("eight",balls,early,earlyShot,1).winner());assertFalse(earlyShot.balls().get(8).pocketed);
 }

 @Test void boundedSearchIsDeterministicAndDoesNotMutatePosition(){
  for(String rules:List.of("eight","nine","free")){
   var balls=ExpertBenchmark.position(rules,20261110);var before=balls.stream().map(Physics.Ball::copy).toList();int group=rules.equals("eight")?1:0;
   var a=ExpertBot.search(balls,group,false,rules);var b=ExpertBot.search(balls,group,false,rules);
   assertEquals(a,b);assertTrue(a.simulations()>1&&a.simulations()<=ExpertBot.MAX_PREVIEWS);
   assertEquals(a.plan(),AimBot.choose(balls,group,false,"expert",999999,rules));
   var shot=shoot(balls,a.plan());assertFalse(result(rules,balls,a.plan(),shot,group).foul());
   for(int i=0;i<balls.size();i++){assertEquals(before.get(i).x,balls.get(i).x);assertEquals(before.get(i).y,balls.get(i).y);assertEquals(before.get(i).pocketed,balls.get(i).pocketed);assertEquals(0,balls.get(i).vx);}
  }
 }

 @Test void breakUsesOnlySixTrialsAndRespectsEveryRuleSet(){
  for(String rules:List.of("eight","nine","free")){
   var balls=Physics.rack(rules);var search=ExpertBot.search(balls,0,true,rules);var plan=search.plan();var shot=shoot(balls,plan);
   assertEquals(6,search.simulations());assertEquals(1,shot.firstContact());
   assertFalse(Rules.evaluate(rules,balls,shot,0,0,true,plan.pocket(),0,8).foul());
  }
 }

 @Test void ballInHandCreatesLegalPottingPosition(){
  var balls=position("eight",new double[]{1,1.27,.3},new double[]{8,2,.7},new double[]{9,.4,.8});
  var place=AimBot.placement(balls,1,false,"eight","expert");assertTrue(Physics.canPlace(balls,place[0],place[1]));
  assertTrue(balls.get(0).pocketed);balls.get(0).x=place[0];balls.get(0).y=place[1];balls.get(0).pocketed=false;
  var plan=AimBot.choose(balls,1,false,"expert",1,"eight");assertTrue(result("eight",balls,plan,shoot(balls,plan),1).continues());
 }
}
