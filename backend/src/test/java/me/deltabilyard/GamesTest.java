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
 @Test void validFinalEightCompletesMatchAndSurvivesCheckpoint()throws Exception {Games g=game();var r=room(g);r.groups=new int[]{1,2};for(var b:r.balls)b.pocketed=true;var cue=r.balls.get(0);cue.pocketed=false;cue.x=Physics.W/2;cue.y=.35;var eight=r.balls.get(8);eight.pocketed=false;eight.x=Physics.W/2;eight.y=.12;var n=(com.fasterxml.jackson.databind.node.ObjectNode)cmd("shoot",0);n.put("angle",-Math.PI/2).put("power",Physics.powerForSpeed(1.2)).put("pocket",1);g.command(r.code,1,n);assertEquals(0,r.winner);assertEquals(0,r.roundWinner);assertEquals(-1,r.matchWinner);assertEquals(1,r.scores[0]);Games.Room recovered=json.readValue(json.writeValueAsString(r),Games.Room.class);assertEquals(r.shot.frames().size(),recovered.shot.frames().size());recovered.deadline=0;g.rooms.put(r.code,recovered);g.tick();assertEquals("FINISHED",recovered.phase);assertEquals(0,recovered.winner);assertEquals(0,recovered.matchWinner);assertTrue(recovered.message.contains("заказанной лузе"));}
 @Test void practiceHasNoTurnTimeout(){Games g=game();var r=room(g);r.mode="practice";r.deadline=0;g.tick();assertEquals("AIM",r.phase);assertFalse(r.ballInHand);}

 @Test void botRoomUsesVirtualReadySeatAndValidatesSettings(){Games g=game();when(g.auth.user(1)).thenReturn(new Auth.User(1,"A",null,null));var data=json.createObjectNode().put("mode","bot").put("botLevel","pro").put("ballStyle","black");var r=g.create(1,data);assertEquals("black",r.ballStyle);assertEquals(2,r.capacity());assertEquals(-1,r.seats.get(1).user.id());assertTrue(r.seats.get(1).ready);g.command(r.code,1,cmd("ready",0));assertEquals("AIM",r.phase);Games other=game();assertThrows(IllegalArgumentException.class,()->other.create(1,json.createObjectNode().put("ballStyle","arbitrary")));}
 @Test void botUsesAuthoritativeTurnAndPlacesCue(){Games g=game();var r=room(g);r.mode="bot";r.botLevel="pro";r.seats.set(1,new Games.Seat(new Auth.User(-1,"Bot",null,null)));r.turn=1;r.ballInHand=true;r.balls.get(0).pocketed=true;g.botTurn(r);assertEquals("SHOT",r.phase);assertEquals(2,r.seq);assertNotNull(r.shot);}
 @Test void botCannotStrikeDuringHumansTurn(){Games g=game();var r=room(g);r.mode="bot";assertThrows(IllegalArgumentException.class,()->g.botTurn(r));assertEquals(0,r.seq);}
 @Test void countsAreAttributedToShooterAndResetForNextRack(){Games g=game();var r=room(g);for(var b:r.balls)b.pocketed=true;var cue=r.balls.get(0);cue.pocketed=false;cue.x=Physics.W/2;cue.y=.7;var one=r.balls.get(1);one.pocketed=false;one.x=Physics.W/2;one.y=.35;r.groups=new int[]{1,2};var n=(com.fasterxml.jackson.databind.node.ObjectNode)cmd("shoot",0);n.put("angle",-Math.PI/2).put("power",Physics.powerForSpeed(1.2));g.command(r.code,1,n);assertEquals(1,r.potted.get(1L));g.command(r.code,1,n);assertEquals(1,r.potted.get(1L));assertFalse(r.potted.containsKey(2L));assertEquals(0,r.turn,"Legal own pot keeps the same shooter");assertEquals(1,r.lastShooterId);assertTrue(r.shotMessage.contains("серия продолжается"));assertEquals("",r.foulReason);assertEquals(r.shotAt+(long)(r.shot.duration()*1000)+4800,r.deadline);g.newRack(r);assertTrue(r.potted.isEmpty());}
 @Test void replayWindowIsBoundedForMultiplePots(){var events=java.util.stream.IntStream.range(1,6).mapToObj(i->new Physics.Event(i,"pocket",i,-1,1,1)).toList();var shot=new Physics.Shot(Physics.rack(),java.util.List.of(),events,1,true,7);assertEquals(7200,Games.replayMillis(shot));var miss=new Physics.Shot(Physics.rack(),java.util.List.of(),java.util.List.of(),1,true,7);assertEquals(400,Games.replayMillis(miss));}
 @Test void returnReminderIsOnceAndNotForLobbyOrIntentionalLeave(){Games g=game();g.bot=mock(Bot.class);var r=room(g);g.disconnected(r.code,1);verify(g.bot,times(1)).remindReturn(1,r.code);g.connected(r.code,1);g.disconnected(r.code,1);verify(g.bot,times(1)).remindReturn(1,r.code);var second=room(g);second.phase="LOBBY";g.disconnected(second.code,2);assertFalse(second.graceUsed.contains(2L));verify(g.bot,never()).remindReturn(2,second.code);g.command(second.code,1,cmd("leave",second.seq));g.disconnected(second.code,1);verify(g.bot,times(1)).remindReturn(1,r.code);}

 Games.Room lobby(Games g,String mode){when(g.auth.user(anyLong())).thenAnswer(call->new Auth.User(call.getArgument(0),"Player",null,null));return g.create(1,json.createObjectNode().put("mode",mode));}
 void action(Games g,Games.Room r,long id,String action,int slot){g.command(r.code,id,((com.fasterxml.jackson.databind.node.ObjectNode)cmd(action,r.seq)).put("slot",slot));}
 @Test void lobbySeatsStayStableAcrossGapsJoinsAndHostTransfer(){
  Games g=game();var r=lobby(g,"team");g.join(r.code,2);g.join(r.code,3);
  action(g,r,1,"seat",3);assertEquals(3,g.index(r,1));assertEquals(1,r.hostId);
  g.command(r.code,2,cmd("leave",r.seq));assertEquals(2,g.index(r,3));assertEquals(3,g.index(r,1));
  g.join(r.code,4);assertEquals(0,g.index(r,4));g.join(r.code,5);assertEquals(1,g.index(r,5));
  g.command(r.code,1,cmd("leave",r.seq));assertEquals(4,r.hostId);assertEquals(2,g.index(r,3));
 }
 @Test void occupiedHumanSeatRequiresTheirConsentAndResetsReadiness(){
  Games g=game();var r=lobby(g,"team");g.join(r.code,2);g.join(r.code,3);g.command(r.code,1,cmd("ready",r.seq));
  action(g,r,1,"seat",1);assertEquals(0,g.index(r,1));assertEquals(1,r.swapRequests.get(1L));
  var invalid=((com.fasterxml.jackson.databind.node.ObjectNode)cmd("acceptSwap",r.seq)).put("requester",1);
  assertThrows(IllegalArgumentException.class,()->g.command(r.code,3,invalid));
  g.command(r.code,2,((com.fasterxml.jackson.databind.node.ObjectNode)cmd("acceptSwap",r.seq)).put("requester",1));
  assertEquals(1,g.index(r,1));assertEquals(0,g.index(r,2));assertEquals(1,r.hostId);assertTrue(r.swapRequests.isEmpty());assertTrue(r.seats.stream().noneMatch(s->s.ready));
 }
 @Test void staleSwapIsClearedWhenTargetMovesAndCanBeDeclined(){
  Games g=game();var r=lobby(g,"team");g.join(r.code,2);action(g,r,1,"seat",1);
  g.command(r.code,2,((com.fasterxml.jackson.databind.node.ObjectNode)cmd("declineSwap",r.seq)).put("requester",1));assertTrue(r.swapRequests.isEmpty());
  action(g,r,1,"seat",1);action(g,r,2,"seat",3);assertTrue(r.swapRequests.isEmpty());
  assertThrows(IllegalArgumentException.class,()->g.command(r.code,2,((com.fasterxml.jackson.databind.node.ObjectNode)cmd("acceptSwap",r.seq)).put("requester",1)));
 }
 @Test void onlyHostManagesDistinctReadyBotsAndCanSwapWithBot(){
  Games g=game();var r=lobby(g,"team");g.join(r.code,2);
  assertThrows(IllegalArgumentException.class,()->action(g,r,2,"addBot",2));
  action(g,r,1,"addBot",2);g.command(r.code,1,((com.fasterxml.jackson.databind.node.ObjectNode)cmd("addBot",r.seq)).put("slot",3).put("level","pro"));
  assertEquals(-1,Games.seatAt(r,2).user.id());assertEquals(-2,Games.seatAt(r,3).user.id());assertEquals("pro",Games.seatAt(r,3).botLevel);assertTrue(Games.seatAt(r,2).ready);
  assertThrows(IllegalArgumentException.class,()->action(g,r,2,"seat",2));action(g,r,1,"seat",2);assertEquals(2,g.index(r,1));assertEquals(-1,Games.seatAt(r,0).user.id());
  action(g,r,1,"removeBot",3);action(g,r,1,"addBot",3);assertEquals(-3,Games.seatAt(r,3).user.id());
  assertThrows(IllegalArgumentException.class,()->action(g,r,1,"removeBot",1));assertThrows(IllegalArgumentException.class,()->action(g,r,1,"addBot",2));
 }
 @Test void readyStartsOnlyFullLayoutAndMatchForbidsEverySeatMutation(){
  Games g=game();var r=lobby(g,"team");action(g,r,1,"addBot",1);action(g,r,1,"addBot",2);g.command(r.code,1,cmd("ready",r.seq));assertEquals("LOBBY",r.phase);
  action(g,r,1,"addBot",3);assertFalse(Games.seatAt(r,0).ready);g.command(r.code,1,cmd("ready",r.seq));assertEquals("AIM",r.phase);
  for(String name:java.util.List.of("seat","addBot","removeBot","acceptSwap","declineSwap"))assertThrows(IllegalArgumentException.class,()->action(g,r,1,name,1));
 }
 @Test void eachBotInTeamUsesNormalAuthoritativeShotCommands(){
  Games g=game();var r=lobby(g,"team");for(int slot=1;slot<4;slot++)action(g,r,1,"addBot",slot);g.command(r.code,1,cmd("ready",r.seq));
  for(int slot=1;slot<4;slot++){r.turn=slot;r.phase="AIM";r.balls=Physics.rack();r.breaking=true;r.ballInHand=false;r.deadline=Games.now()+45000;int seq=r.seq;g.botTurn(r);assertEquals("SHOT",r.phase);assertEquals(seq+1,r.seq);assertEquals(-slot,r.lastShooterId);assertEquals(1,r.shot.firstContact());}
  g.closeBots();
 }
 @Test void newRackRotationKeepsTeamPartnerOrder(){
  Games g=game();var r=new Games.Room();r.mode="team";r.round=2;g.newRack(r);assertEquals(2,r.turn);g.advance(r);assertEquals(1,r.turn);g.advance(r);assertEquals(0,r.turn);g.advance(r);assertEquals(3,r.turn);
 }
 @Test void roundWinnerIsSeparateFromMatchWinnerAndForfeitNamesWinningTeam(){
  Games g=game();var r=room(g);r.target=3;r.phase="SHOT";r.winner=1;r.roundWinner=1;r.scores[1]=1;r.shotMessage="Поражение: восьмёрка попала не в заказанную лузу";r.deadline=0;g.tick();assertEquals("ROUND_END",r.phase);assertEquals(1,r.roundWinner);assertEquals(-1,r.matchWinner);
  g.newRack(r);assertEquals(-1,r.roundWinner);assertEquals(-1,r.winner);g.finish(r,0,"Соперник покинул матч");assertEquals(0,r.matchWinner);assertEquals(-1,r.roundWinner);
 }
 @Test void legacyRoomReceivesSlotsAndPersistsLayout()throws Exception {
  Games g=game();var r=room(g);Games.normalizeSeats(r);assertEquals(0,r.seats.get(0).slot);assertEquals(1,r.seats.get(1).slot);assertEquals(1,r.hostId);
  r.mode="team";r.phase="LOBBY";action(g,r,1,"seat",3);var restored=json.readValue(json.writeValueAsString(r),Games.Room.class);Games.normalizeSeats(restored);assertEquals(3,g.index(restored,1));assertEquals(1,g.index(restored,2));restored.phase="ROUND_END";restored.winner=1;restored.roundWinner=-1;Games.normalizeSeats(restored);assertEquals(1,restored.roundWinner);restored.phase="FINISHED";Games.normalizeSeats(restored);assertEquals(1,restored.matchWinner);
 }
}
