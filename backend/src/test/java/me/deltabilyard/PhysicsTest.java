package me.deltabilyard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class PhysicsTest {
 @Test void breakIsDeterministicAndStops(){var a=Physics.simulate(Physics.rack(),0,.85,0,0);var b=Physics.simulate(Physics.rack(),0,.85,0,0);assertEquals(a.frames().size(),b.frames().size()); for(int i=0;i<a.frames().size();i++)for(int j=0;j<16;j++)assertArrayEquals(a.frames().get(i).balls()[j],b.frames().get(i).balls()[j]);}
 @Test void cueHitsRackWithoutTunnelling(){var s=Physics.simulate(Physics.rack(),0,1,0,0);assertEquals(1,s.firstContact());assertTrue(s.events().stream().anyMatch(e->e.type().equals("collision")));assertTrue(s.duration()<24);assertTrue(s.balls().stream().allMatch(b->b.vx==0&&b.vy==0));}
 @Test void finalPositionsMatchForSameInput(){var a=Physics.simulate(Physics.rack(),.01,.7,.2,-.3);var b=Physics.simulate(Physics.rack(),.01,.7,.2,-.3);for(int i=0;i<16;i++){assertEquals(a.balls().get(i).x,b.balls().get(i).x);assertEquals(a.balls().get(i).y,b.balls().get(i).y);}}
 @Test void pocketCapturesBall(){var balls=Physics.rack();for(var b:balls)b.pocketed=true;var cue=balls.get(0);cue.pocketed=false;cue.x=.2;cue.y=.2;var s=Physics.simulate(balls,-Math.PI*3/4,.15,0,0);assertTrue(s.balls().get(0).pocketed);}
 @Test void bankReflects(){var balls=Physics.rack();for(var b:balls)if(b.id!=0)b.pocketed=true;var s=Physics.simulate(balls,-Math.PI/2,.25,0,0);assertTrue(s.events().stream().anyMatch(e->e.type().equals("rail")));}
 @Test void rejectsNonFiniteAndExcessPower(){assertThrows(IllegalArgumentException.class,()->Physics.simulate(Physics.rack(),Double.NaN,.5,0,0));assertThrows(IllegalArgumentException.class,()->Physics.simulate(Physics.rack(),0,1.1,0,0));assertThrows(IllegalArgumentException.class,()->Physics.simulate(Physics.rack(),0,.5,1,1));}
 @Test void cannotPlaceOutsideBedOrOverBall(){var b=Physics.rack();assertFalse(Physics.canPlace(b,0,0));assertFalse(Physics.canPlace(b,b.get(1).x,b.get(1).y));assertTrue(Physics.canPlace(b,.5,.5));}
}
