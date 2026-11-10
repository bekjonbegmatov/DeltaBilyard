package me.deltabilyard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class PhysicsTest {
 @Test void previewHasExactlyTheAuthoritativeOutcomeWithoutReplayFrames(){
  for(double[] input:new double[][]{{0,.98,0,0},{.013,.72,.4,-.5},{-.23,.5,-.5,.4}}){
   var balls=Physics.rack();var full=Physics.simulate(balls,input[0],input[1],input[2],input[3]);var preview=Physics.preview(balls,input[0],input[1],input[2],input[3]);
   assertTrue(preview.frames().isEmpty());assertFalse(full.frames().isEmpty());assertEquals(full.events(),preview.events());
   assertEquals(full.firstContact(),preview.firstContact());assertEquals(full.railAfterContact(),preview.railAfterContact());assertEquals(full.duration(),preview.duration());
   for(int i=0;i<balls.size();i++){var a=full.balls().get(i);var b=preview.balls().get(i);assertEquals(a.id,b.id);assertEquals(a.x,b.x);assertEquals(a.y,b.y);assertEquals(a.vx,b.vx);assertEquals(a.vy,b.vy);assertEquals(a.sx,b.sx);assertEquals(a.sy,b.sy);assertEquals(a.pocketed,b.pocketed);}
   assertEquals(Physics.W*.25,balls.get(0).x);assertEquals(Physics.H/2,balls.get(0).y);assertFalse(balls.get(0).pocketed);assertEquals(0,balls.get(0).vx);
  }
 }
 @Test void breakIsDeterministicAndStops(){var a=Physics.simulate(Physics.rack(),0,.85,0,0);var b=Physics.simulate(Physics.rack(),0,.85,0,0);assertEquals(a.frames().size(),b.frames().size()); for(int i=0;i<a.frames().size();i++)for(int j=0;j<16;j++)assertArrayEquals(a.frames().get(i).balls()[j],b.frames().get(i).balls()[j]);}
 @Test void cueHitsRackWithoutTunnelling(){var s=Physics.simulate(Physics.rack(),0,1,0,0);assertEquals(1,s.firstContact());assertTrue(s.events().stream().anyMatch(e->e.type().equals("collision")));assertTrue(s.duration()<24);assertTrue(s.balls().stream().allMatch(b->b.vx==0&&b.vy==0));}
 @Test void finalPositionsMatchForSameInput(){var a=Physics.simulate(Physics.rack(),.01,.7,.2,-.3);var b=Physics.simulate(Physics.rack(),.01,.7,.2,-.3);for(int i=0;i<16;i++){assertEquals(a.balls().get(i).x,b.balls().get(i).x);assertEquals(a.balls().get(i).y,b.balls().get(i).y);}}
 @Test void pocketCapturesBall(){var balls=Physics.rack();for(var b:balls)b.pocketed=true;var cue=balls.get(0);cue.pocketed=false;cue.x=.2;cue.y=.2;var s=Physics.simulate(balls,-Math.PI*3/4,.25,0,0);assertTrue(s.balls().get(0).pocketed);}
 @Test void bankReflects(){var balls=Physics.rack();for(var b:balls)if(b.id!=0)b.pocketed=true;var s=Physics.simulate(balls,-Math.PI/2,.25,0,0);assertTrue(s.events().stream().anyMatch(e->e.type().equals("rail")));}
 @Test void rejectsNonFiniteAndExcessPower(){assertThrows(IllegalArgumentException.class,()->Physics.simulate(Physics.rack(),Double.NaN,.5,0,0));assertThrows(IllegalArgumentException.class,()->Physics.simulate(Physics.rack(),0,1.1,0,0));assertThrows(IllegalArgumentException.class,()->Physics.simulate(Physics.rack(),0,.5,1,1));}
 @Test void cannotPlaceOutsideBedOrOverBall(){var b=Physics.rack();assertFalse(Physics.canPlace(b,0,0));assertFalse(Physics.canPlace(b,b.get(1).x,b.get(1).y));assertTrue(Physics.canPlace(b,.5,.5));}
 @Test void pocketCentresAreBehindCushionNoses(){assertTrue(Physics.POCKETS[0][0]<0);assertTrue(Physics.POCKETS[1][1]<0);assertTrue(Physics.POCKETS[5][0]>Physics.W);}
 @Test void sidePocketCapturesThroughOpenMouth(){var b=Physics.rack();for(var x:b)x.pocketed=x.id!=0;b.get(0).x=Physics.W/2;b.get(0).y=.25;var shot=Physics.simulate(b,-Math.PI/2,.3,0,0);assertTrue(shot.balls().get(0).pocketed);assertTrue(shot.events().stream().anyMatch(e->e.type().equals("pocket")&&e.pocket()==1));}
 @Test void missesOutsidePocketMouthMeetCushion(){var b=Physics.rack();for(var x:b)x.pocketed=x.id!=0;b.get(0).x=Physics.W/2+.17;b.get(0).y=.25;var shot=Physics.simulate(b,-Math.PI/2,.25,0,0);assertFalse(shot.balls().get(0).pocketed);assertTrue(shot.events().stream().anyMatch(e->e.type().equals("rail")));}
 @Test void normalPowerIsControllableAndMaximumStillBreaks(){assertTrue(Physics.speedForPower(.65)<2.7);assertTrue(Physics.speedForPower(.65)>2.3);assertTrue(Physics.speedForPower(.35)<1.1);assertTrue(Physics.speedForPower(1)<=4.8);for(double speed:new double[]{.7,1.5,3,4.8})assertEquals(speed,Physics.speedForPower(Physics.powerForSpeed(speed)),1e-9);}
 @Test void allSixPocketMouthsAdmitAimedBall(){for(int i=0;i<6;i++){var b=Physics.rack();for(var a:b)a.pocketed=a.id!=0;double[] p=Physics.POCKETS[i];var cue=b.get(0);cue.x=Math.max(.2,Math.min(Physics.W-.2,p[0]));cue.y=Math.max(.2,Math.min(Physics.H-.2,p[1]));var shot=Physics.simulate(b,Math.atan2(p[1]-cue.y,p[0]-cue.x),.4,0,0);final int pocket=i;assertTrue(shot.events().stream().anyMatch(e->e.type().equals("pocket")&&e.pocket()==pocket),"pocket "+i);}}
 @Test void ballsOutsideNarrowSideMouthRebound(){var b=Physics.rack();for(var a:b)a.pocketed=a.id!=0;b.get(0).x=Physics.W/2+.09;b.get(0).y=.20;var shot=Physics.simulate(b,-Math.PI/2,.35,0,0);assertTrue(shot.events().stream().anyMatch(e->e.type().equals("rail")));assertFalse(shot.balls().get(0).pocketed);}
}
