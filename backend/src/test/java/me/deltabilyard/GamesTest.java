package me.deltabilyard;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GamesTest {
 final ObjectMapper json=new ObjectMapper();
 Games game(){return new Games(mock(JdbcTemplate.class),json,mock(Auth.class));}
 Games.Room room(Games g){Games.Room r=new Games.Room();r.code="1234";r.phase="AIM";r.breaking=false;r.deadline=System.currentTimeMillis()+45000;r.seats.add(new Games.Seat(new Auth.User(1,"A",null,null)));r.seats.add(new Games.Seat(new Auth.User(2,"B",null,null)));g.rooms.put(r.code,r);return r;}
 JsonNode cmd(String action,int seq){return json.createObjectNode().put("action",action).put("seq",seq).put("id",java.util.UUID.randomUUID().toString());}
 @Test void outOfTurnAndStaleCommandsDoNotChangeState(){Games g=game();var r=room(g);assertThrows(IllegalArgumentException.class,()->g.command(r.code,2,cmd("shoot",0)));assertThrows(IllegalArgumentException.class,()->g.command(r.code,1,cmd("shoot",5)));assertEquals(0,r.seq);}
 @Test void timeoutAwardsBallInHand(){Games g=game();var r=room(g);r.deadline=0;g.tick();assertEquals(1,r.turn);assertTrue(r.ballInHand);assertTrue(r.balls.get(0).pocketed);}
 @Test void duplicateShotIsIdempotent(){Games g=game();var r=room(g);var n=(com.fasterxml.jackson.databind.node.ObjectNode)cmd("shoot",0);n.put("angle",0).put("power",.4);g.command(r.code,1,n);int seq=r.seq;g.command(r.code,1,n);assertEquals(seq,r.seq);assertEquals("SHOT",r.phase);}
 @Test void disconnectGetsOneGracePeriod(){Games g=game();var r=room(g);g.disconnected(r.code,1);assertTrue(r.disconnected.containsKey(1L));g.connected(r.code,1);assertTrue(r.disconnected.isEmpty());g.disconnected(r.code,1);g.tick();assertEquals("FINISHED",r.phase);assertEquals(1,r.winner);}
 @Test void validFinalEightCompletesMatchAndSurvivesCheckpoint()throws Exception {Games g=game();var r=room(g);r.groups=new int[]{1,2};for(var b:r.balls)b.pocketed=true;var cue=r.balls.get(0);cue.pocketed=false;cue.x=Physics.W/2;cue.y=.35;var eight=r.balls.get(8);eight.pocketed=false;eight.x=Physics.W/2;eight.y=.12;var n=(com.fasterxml.jackson.databind.node.ObjectNode)cmd("shoot",0);n.put("angle",-Math.PI/2).put("power",.2).put("pocket",1);g.command(r.code,1,n);assertEquals(0,r.winner);assertEquals(1,r.scores[0]);Games.Room recovered=json.readValue(json.writeValueAsString(r),Games.Room.class);assertEquals(r.shot.frames().size(),recovered.shot.frames().size());recovered.deadline=0;g.rooms.put(r.code,recovered);g.tick();assertEquals("FINISHED",recovered.phase);assertEquals(0,recovered.winner);}
 @Test void practiceHasNoTurnTimeout(){Games g=game();var r=room(g);r.mode="practice";r.deadline=0;g.tick();assertEquals("AIM",r.phase);assertFalse(r.ballInHand);}
}
