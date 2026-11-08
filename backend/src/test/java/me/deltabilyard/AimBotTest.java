package me.deltabilyard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class AimBotTest {
 List<Physics.Ball> openTable(){var b=Physics.rack();for(var x:b)x.pocketed=true;b.get(0).pocketed=false;b.get(0).x=Physics.W/2;b.get(0).y=.7;b.get(1).pocketed=false;b.get(1).x=Physics.W/2;b.get(1).y=.35;b.get(9).pocketed=false;b.get(9).x=.4;b.get(9).y=.8;return b;}
 @Test void eachLevelTargetsOnlyItsGroupAndHasFinitePower(){for(String level:List.of("beginner","medium","pro")){var plan=AimBot.choose(openTable(),1,false,level,123);assertEquals(1,plan.target());assertTrue(plan.power()>=.02&&plan.power()<=1);assertTrue(Double.isFinite(plan.angle()));}}
 @Test void proPotsEasyBallAndMediumIsLessAccurate(){var balls=openTable();var pro=AimBot.choose(balls,1,false,"pro",123);var medium=AimBot.choose(balls,1,false,"medium",123);var beginner=AimBot.choose(balls,1,false,"beginner",123);assertTrue(Math.abs(pro.angle()+Math.PI/2)<Math.abs(beginner.angle()+Math.PI/2));assertNotEquals(pro.angle(),medium.angle());var shot=Physics.simulate(balls,pro.angle(),pro.power(),0,0);assertEquals(1,shot.firstContact());assertTrue(shot.balls().get(1).pocketed);}
 @Test void finalEightIsCalledAndNoEarlyEightIsTargeted(){var b=openTable();b.get(8).pocketed=false;b.get(8).x=1;b.get(8).y=.3;assertNotEquals(8,AimBot.choose(b,1,false,"pro",1).target());b.get(1).pocketed=true;var plan=AimBot.choose(b,1,false,"pro",1);assertEquals(8,plan.target());assertTrue(plan.pocket()>=0&&plan.pocket()<6);}
 @Test void ballInHandFindsLegalPlacement(){var b=openTable();b.get(0).pocketed=true;double[] p=AimBot.placement(b,1,false);assertTrue(Physics.canPlace(b,p[0],p[1]));}
 @Test void breakAlwaysTargetsApex(){for(String level:List.of("beginner","medium","pro")){var plan=AimBot.choose(Physics.rack(),0,true,level,3);assertEquals(1,plan.target());var result=Physics.simulate(Physics.rack(),plan.angle(),plan.power(),0,0);assertEquals(1,result.firstContact());}}
}
