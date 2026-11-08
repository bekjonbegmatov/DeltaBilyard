package me.deltabilyard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class RuleSetsTest {
 Physics.Shot shot(List<Physics.Ball> before,int first,boolean rail,int... pots){
  var balls=before.stream().map(Physics.Ball::copy).toList();var events=new ArrayList<Physics.Event>();
  for(int id:pots){balls.get(id).pocketed=true;events.add(new Physics.Event(1,"pocket",id,-1,2,1));}
  return new Physics.Shot(balls,List.of(),events,first,rail,2);
 }
 Rules.Result evaluate(String kind,List<Physics.Ball> before,Physics.Shot shot,boolean breaking,int points,int target){return Rules.evaluate(kind,before,shot,0,0,breaking,-1,points,target);}
 @Test void nineRackIsDiamondWithOneAtApexAndNineInCenter(){
  var balls=Physics.rack("nine");assertEquals(10,balls.size());for(int id=0;id<=9;id++)assertEquals(id,balls.get(id).id);
  assertEquals(Physics.W*.72,balls.get(1).x,1e-9);assertEquals(Physics.H/2,balls.get(1).y,1e-9);
  assertEquals(Physics.W*.72+2*Physics.R*Math.sqrt(3)*1.005,balls.get(9).x,1e-9);assertEquals(Physics.H/2,balls.get(9).y,1e-9);
  var rows=balls.stream().filter(b->b.id>0).collect(java.util.stream.Collectors.groupingBy(b->b.x,TreeMap::new,java.util.stream.Collectors.counting()));assertEquals(List.of(1L,2L,3L,2L,1L),new ArrayList<>(rows.values()));
 }
 @Test void nineRequiresLowestButLegalCombinationWins(){
  var balls=Physics.rack("nine");balls.get(1).pocketed=true;
  var wrong=evaluate("nine",balls,shot(balls,3,true,9),false,0,8);assertTrue(wrong.foul());assertEquals(-1,wrong.winner());assertTrue(wrong.message().contains("№2"));
  var combo=evaluate("nine",balls,shot(balls,2,true,9),false,0,8);assertFalse(combo.foul());assertEquals(0,combo.winner());assertEquals(0,combo.assignedGroup());
 }
 @Test void nineScratchDoesNotWinAndOtherLegalPotContinues(){
  var balls=Physics.rack("nine");var foul=evaluate("nine",balls,shot(balls,1,true,9,0),false,0,8);assertEquals(-1,foul.winner());assertTrue(foul.foul());assertTrue(foul.message().contains("возвращается"));
  assertTrue(evaluate("nine",balls,shot(balls,1,true,4),false,0,8).continues());assertFalse(evaluate("nine",balls,shot(balls,1,true),false,0,8).continues());
 }
 @Test void nineBreakNeedsPotOrFourDifferentRails(){
  var balls=Physics.rack("nine");assertTrue(evaluate("nine",balls,shot(balls,1,true),true,0,8).foul());assertEquals(0,evaluate("nine",balls,shot(balls,1,true,9),true,0,8).winner());
  var events=List.of(new Physics.Event(1,"rail",1,-1,-1,1),new Physics.Event(1,"rail",2,-1,-1,1),new Physics.Event(1,"rail",3,-1,-1,1),new Physics.Event(1,"rail",4,-1,-1,1));
  assertFalse(evaluate("nine",balls,new Physics.Shot(balls,List.of(),events,1,true,2),true,0,8).foul());
 }
 @Test void freeTreatsEightAsOrdinaryBallAndScoresOnlyLegalPots(){
  var balls=Physics.rack("free");var ordinary=evaluate("free",balls,shot(balls,8,true,8),false,0,8);assertFalse(ordinary.foul());assertTrue(ordinary.continues());assertEquals(-1,ordinary.winner());
  assertEquals(0,evaluate("free",balls,shot(balls,8,true,8),false,7,8).winner());var scratch=evaluate("free",balls,shot(balls,8,true,8,0),false,7,8);assertTrue(scratch.foul());assertEquals(-1,scratch.winner());assertFalse(scratch.continues());
 }
 @Test void freePracticeRequiresFifteenAndSharedFoulsStillApply(){
  var balls=Physics.rack("free");assertEquals(-1,evaluate("free",balls,shot(balls,1,true,1),false,7,15).winner());assertEquals(0,evaluate("free",balls,shot(balls,1,true,1),false,14,15).winner());
  assertTrue(evaluate("free",balls,shot(balls,-1,true),false,0,8).foul());assertTrue(evaluate("free",balls,shot(balls,1,false),false,0,8).foul());
 }
 @Test void respotPreservesNumberAndAvoidsOccupiedFootSpotIncludingCue(){
  var balls=Physics.rack("nine");balls.get(9).pocketed=true;balls.get(0).x=Physics.W*.72;balls.get(0).y=Physics.H/2;Physics.respot(balls,9);var nine=balls.get(9);
  assertFalse(nine.pocketed);assertEquals(9,nine.id);assertTrue(nine.x>Physics.W*.72);assertEquals(Physics.H/2,nine.y,1e-9);for(var ball:balls)if(ball.id!=9&&!ball.pocketed)assertTrue(Math.hypot(ball.x-nine.x,ball.y-nine.y)>=Physics.R*2);
 }
 @Test void nineAndFreeBotsTargetTheirActualLegalBalls(){
  var nine=Physics.rack("nine");for(var ball:nine)ball.pocketed=ball.id!=0&&ball.id!=4&&ball.id!=9;nine.get(0).x=1.27;nine.get(0).y=.7;nine.get(4).x=1.27;nine.get(4).y=.35;
  for(String level:List.of("beginner","medium","pro")){var plan=AimBot.choose(nine,0,false,level,17,"nine");assertEquals(4,plan.target());var placement=AimBot.placement(nine,0,false,"nine");assertTrue(Physics.canPlace(nine,placement[0],placement[1]));}
  var free=Physics.rack("free");for(var ball:free)ball.pocketed=ball.id!=0&&ball.id!=8;free.get(0).x=1.27;free.get(0).y=.7;free.get(8).x=1.27;free.get(8).y=.35;
  var plan=AimBot.choose(free,0,false,"pro",17,"free");assertEquals(8,plan.target());var outcome=Physics.simulate(free,plan.angle(),plan.power(),0,0);assertTrue(outcome.balls().get(8).pocketed);
 }
}
