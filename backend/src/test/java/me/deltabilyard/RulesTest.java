package me.deltabilyard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class RulesTest {
 Physics.Shot shot(List<Physics.Ball> b,int first,boolean rail,int... pots){var copy=b.stream().map(Physics.Ball::copy).toList();var events=new ArrayList<Physics.Event>();for(int id:pots){copy.get(id).pocketed=true;events.add(new Physics.Event(1,"pocket",id,-1,2,1));}return new Physics.Shot(copy,List.of(),events,first,rail,2);}
 @Test void legalPotAssignsGroupAndKeepsTurn(){var b=Physics.rack();var r=Rules.evaluate(b,shot(b,1,true,1),0,0,false,-1);assertFalse(r.foul());assertTrue(r.continues());assertEquals(1,r.assignedGroup());}
 @Test void breakDoesNotAssignGroup(){var b=Physics.rack();var r=Rules.evaluate(b,shot(b,1,true,1),0,0,true,-1);assertEquals(0,r.assignedGroup());assertTrue(r.continues());}
 @Test void scratchAndWrongContactAreFouls(){var b=Physics.rack();assertTrue(Rules.evaluate(b,shot(b,1,true,0),0,1,false,-1).foul());assertTrue(Rules.evaluate(b,shot(b,9,true),0,1,false,-1).foul());}
 @Test void noRailIsFoul(){var b=Physics.rack();assertTrue(Rules.evaluate(b,shot(b,1,false),0,1,false,-1).foul());}
 @Test void earlyEightLoses(){var b=Physics.rack();assertEquals(1,Rules.evaluate(b,shot(b,8,true,8),0,1,false,2).winner());}
 @Test void calledEightWinsAndWrongPocketLoses(){var b=Physics.rack();for(var x:b)if(x.id>=1&&x.id<=7)x.pocketed=true;assertEquals(0,Rules.evaluate(b,shot(b,8,true,8),0,1,false,2).winner());assertEquals(1,Rules.evaluate(b,shot(b,8,true,8),0,1,false,3).winner());}
 @Test void eightOnBreakReracks(){var b=Physics.rack();assertTrue(Rules.evaluate(b,shot(b,1,true,8),0,0,true,-1).rerack());}
 @Test void teamTurnsAlternateSeries(){var r=new Games.Room();r.mode="team";var g=new Games(null,null,null);g.advance(r);assertEquals(1,r.turn);g.advance(r);assertEquals(2,r.turn);g.advance(r);assertEquals(3,r.turn);g.advance(r);assertEquals(0,r.turn);}
 @Test void practiceRetainsSinglePlayer(){var r=new Games.Room();r.mode="practice";new Games(null,null,null).advance(r);assertEquals(0,r.turn);}
}
