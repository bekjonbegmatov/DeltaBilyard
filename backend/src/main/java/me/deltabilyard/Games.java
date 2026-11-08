package me.deltabilyard;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.security.SecureRandom;
import java.util.function.BiConsumer;

@Service public class Games {
 public static class Seat { public Auth.User user;public boolean ready;public Seat(){}public Seat(Auth.User u){user=u;} }
 public static class Room {
  public String cloth="green",venue="club",ballStyle="classic",botLevel="medium";public Map<Long,Integer> potted=new HashMap<>();public Set<Long> returnReminded=new HashSet<>();public long botDue;public String code,id=UUID.randomUUID().toString(),phase="LOBBY",message="Ждём игроков",mode="duel";public int seconds=45,target=1,turn=0,seq=0,round=1,winner=-1;public boolean hints=true,breaking=true,ballInHand=false;
  public List<Seat> seats=new ArrayList<>();public List<Physics.Ball> balls=Physics.rack();public int[] groups={0,0},scores={0,0},nextSeat={0,1};
  public long deadline,shotAt,lastActivity=System.currentTimeMillis(),lastTick=System.currentTimeMillis();public Physics.Shot shot;
  public Map<Long,Long> disconnected=new HashMap<>();public Set<Long> graceUsed=new HashSet<>();public LinkedHashSet<String> commands=new LinkedHashSet<>();
  public int capacity(){return mode.equals("team")?4:mode.equals("practice")?1:2;}
  public int team(){return turn%2;}
 }
 @org.springframework.beans.factory.annotation.Autowired VoiceControl voice;
 @org.springframework.beans.factory.annotation.Autowired Bot bot;
 private final Set<String> botJobs=ConcurrentHashMap.newKeySet();
 private final ExecutorService botExecutor=new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(128),r->{Thread t=new Thread(r,"billiards-ai");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 @jakarta.annotation.PreDestroy void closeBots(){botExecutor.shutdownNow();}
 final ConcurrentMap<String,Room> rooms=new ConcurrentHashMap<>(); final JdbcTemplate db;final ObjectMapper json; final Auth auth;final SecureRandom random=new SecureRandom(); volatile boolean draining;final java.util.concurrent.ConcurrentLinkedQueue<Double> commandTimes=new java.util.concurrent.ConcurrentLinkedQueue<>();
 BiConsumer<String,Object> broadcast=(c,s)->{};
 public Games(JdbcTemplate db,ObjectMapper json,Auth auth){this.db=db;this.json=json;this.auth=auth;}
 @PostConstruct void restore(){db.query("SELECT state::text FROM rooms WHERE updated_at > now()-interval '24 hours'",r->{try{Room room=json.readValue(r.getString(1),Room.class);long now=System.currentTimeMillis();room.lastTick=now;room.deadline=Math.max(room.deadline,now+10000);for(Seat seat:room.seats)if(seat.user.id()>0)room.disconnected.put(seat.user.id(),now);rooms.put(room.code,room);}catch(Exception e){throw new IllegalStateException("Cannot recover room",e);}});}
 Room get(String code,long id){Room r=rooms.get(code);if(r==null)throw new IllegalArgumentException("Комната не найдена");if(r.seats.stream().noneMatch(s->s.user.id()==id))throw new IllegalArgumentException("Нет доступа к комнате");return r;}
 synchronized Room create(long id,JsonNode n){if(draining)throw new IllegalArgumentException("Обновление сервера, новые матчи временно недоступны");if(rooms.values().stream().anyMatch(r->!r.phase.equals("FINISHED")&&r.seats.stream().anyMatch(s->s.user.id()==id)))throw new IllegalArgumentException("Сначала выйдите из текущей комнаты");
  Room r=new Room();r.cloth=n.path("cloth").asText("green");r.venue=n.path("venue").asText("club");if(!Set.of("green","blue").contains(r.cloth)||!Set.of("club","loft").contains(r.venue))throw new IllegalArgumentException("Неизвестный стол");r.ballStyle=n.path("ballStyle").asText("classic");r.botLevel=n.path("botLevel").asText("medium");if(!Set.of("classic","black","ivory").contains(r.ballStyle)||!Set.of("beginner","medium","pro").contains(r.botLevel))throw new IllegalArgumentException("Неизвестное оформление или уровень бота");r.mode=n.path("mode").asText("duel");if(!Set.of("duel","team","practice","bot").contains(r.mode))throw new IllegalArgumentException("Неверный режим");r.seconds=n.path("seconds").asInt(45);r.target=n.path("target").asInt(1);r.hints=n.path("hints").asBoolean(true);
  if(!Set.of(30,45,60).contains(r.seconds)||!Set.of(1,3,5).contains(r.target))throw new IllegalArgumentException("Неверные настройки");
  if(rooms.size()>=9500)throw new IllegalArgumentException("Все комнаты заняты");do{r.code=String.format("%04d",random.nextInt(10000));}while(rooms.containsKey(r.code));r.seats.add(new Seat(auth.user(id)));if(r.mode.equals("bot")){String name=r.botLevel.equals("beginner")?"Бот · новичок":r.botLevel.equals("pro")?"Бот · профи":"Бот · средний";Seat ai=new Seat(new Auth.User(-1,name,null,null));ai.ready=true;r.seats.add(ai);}rooms.put(r.code,r);save(r);return r;
 }
 Room join(String code,long id){auth.limit("join:"+id,12,60);Room r=rooms.get(code);if(r==null)throw new IllegalArgumentException("Комната не найдена");synchronized(r){if(r.seats.stream().anyMatch(s->s.user.id()==id))return r;if(!r.phase.equals("LOBBY")||r.seats.size()>=r.capacity())throw new IllegalArgumentException("Комната заполнена или игра уже идёт");if(rooms.values().stream().anyMatch(other->other!=r&&!other.phase.equals("FINISHED")&&other.seats.stream().anyMatch(s->s.user.id()==id)))throw new IllegalArgumentException("Сначала выйдите из текущей комнаты");r.seats.add(new Seat(auth.user(id)));changed(r);return r;}}
 void command(String code,long id,JsonNode n){long started=System.nanoTime();Room r=get(code,id);synchronized(r){String action=n.path("action").asText();String command=n.path("id").asText();if(command.isBlank()||command.length()>80)throw new IllegalArgumentException("Нет ID команды");if(r.commands.contains(id+":"+command))return;
  if(n.path("seq").asInt(-1)!=r.seq)throw new IllegalArgumentException("Состояние изменилось. Повторите действие");
  if(action.equals("ready")){if(draining)throw new IllegalArgumentException("Обновление сервера, дождитесь завершения");if(!r.phase.equals("LOBBY"))throw new IllegalArgumentException("Игра уже началась");r.seats.stream().filter(s->s.user.id()==id).findFirst().orElseThrow().ready=true;if(r.seats.size()==r.capacity()&&r.seats.stream().allMatch(s->s.ready)){r.phase="AIM";r.deadline=now()+r.seconds*1000L;r.message="Разбой";r.disconnected.replaceAll((who,at)->now());r.graceUsed.addAll(r.disconnected.keySet());}}
  else if(action.equals("leave")){if(voice!=null)voice.remove(r.id,id);if(r.phase.equals("LOBBY")){r.seats.removeIf(s->s.user.id()==id);if(r.seats.stream().noneMatch(s->s.user.id()>0)){rooms.remove(code);db.update("DELETE FROM rooms WHERE code=?",code);broadcast.accept(code,Map.of("type","closed"));return;}}else if(!r.phase.equals("FINISHED")){int seat=index(r,id);finish(r,1-seat%2,"Соперник покинул матч");}}
  else if(action.equals("next")){if(!r.phase.equals("ROUND_END"))throw new IllegalArgumentException("Партия ещё идёт");newRack(r);}
  else if(action.equals("place")){checkTurn(r,id);if(!r.ballInHand)throw new IllegalArgumentException("Биток нельзя переставить");double x=n.path("x").asDouble(-1),y=n.path("y").asDouble(-1);if(!Physics.canPlace(r.balls,x,y))throw new IllegalArgumentException("Здесь нельзя поставить биток");Physics.Ball b=r.balls.get(0);b.x=x;b.y=y;b.pocketed=false;r.ballInHand=false;}
  else if(action.equals("shoot")){checkTurn(r,id);if(r.ballInHand)throw new IllegalArgumentException("Сначала поставьте биток");if(r.capacity()>1&&r.deadline<now())throw new IllegalArgumentException("Время вышло");
   int called=n.path("pocket").asInt(-1);if(r.groups[r.team()]!=0&&Rules.cleared(r.balls,r.groups[r.team()])&&(called<0||called>5))throw new IllegalArgumentException("Выберите лузу для восьмёрки");
   List<Physics.Ball> before=r.balls;Physics.Shot shot=Physics.simulate(before,n.path("angle").asDouble(),n.path("power").asDouble(),n.path("spinX").asDouble(),n.path("spinY").asDouble());
   r.shot=shot;r.shotAt=now()+180;Rules.Result result=Rules.evaluate(before,shot,r.team(),r.groups[r.team()],r.breaking,called);r.balls=new ArrayList<>(shot.balls());r.breaking=false;r.message=result.message();
   if(result.assignedGroup()!=0&&r.groups[r.team()]==0){r.groups[r.team()]=result.assignedGroup();r.groups[1-r.team()]=3-result.assignedGroup();}
   int pots=(int)shot.events().stream().filter(e->e.type().equals("pocket")&&e.ball()!=0).count();if(pots>0)r.potted.merge(id,pots,Integer::sum);r.deadline=r.shotAt+(long)(shot.duration()*1000)+replayMillis(shot);r.phase="SHOT";
   if(result.winner()>=0){r.winner=result.winner();r.scores[r.winner]++;}
   else {if(!result.continues())advance(r);if(result.rerack()){r.potted.clear();r.balls=Physics.rack();r.breaking=true;r.groups=new int[]{0,0};}r.ballInHand=result.foul();if(r.ballInHand)r.balls.get(0).pocketed=true;}
  } else throw new IllegalArgumentException("Неизвестная команда");
  r.commands.add(id+":"+command);while(r.commands.size()>256)r.commands.remove(r.commands.iterator().next());changed(r);commandTimes.add((System.nanoTime()-started)/1_000_000.0);while(commandTimes.size()>10000)commandTimes.poll();
 }}
 void checkTurn(Room r,long id){if(!r.phase.equals("AIM")||r.seats.get(r.turn).user.id()!=id)throw new IllegalArgumentException("Сейчас не ваш ход");if(!r.disconnected.isEmpty())throw new IllegalArgumentException("Ожидаем возвращения игрока");}
 int index(Room r,long id){for(int i=0;i<r.seats.size();i++)if(r.seats.get(i).user.id()==id)return i;throw new IllegalArgumentException("Игрок не найден");}
 void advance(Room r){r.botDue=0;if(r.capacity()==1)return;int team=r.team();r.nextSeat[team]=r.capacity()==4?(r.turn+2)%4:r.turn;r.turn=r.nextSeat[1-team];}
 void newRack(Room r){r.round++;r.potted.clear();r.botDue=0;r.balls=Physics.rack();r.groups=new int[]{0,0};r.breaking=true;r.ballInHand=false;r.winner=-1;r.shot=null;r.phase="AIM";r.turn=(r.round-1)%r.capacity();r.nextSeat=new int[]{0,1};r.deadline=now()+r.seconds*1000L;r.message="Новая партия · разбой";}
 void finish(Room r,int winner,String message){r.phase="FINISHED";if(voice!=null)voice.close(r.id);r.winner=winner;r.message=message;archive(r);}
 void archive(Room r){try{db.update("INSERT INTO matches(id,room_code,state) VALUES (?::uuid,?,?::jsonb) ON CONFLICT(id) DO UPDATE SET state=excluded.state",r.id,r.code,json.writeValueAsString(r));}catch(Exception e){throw new IllegalStateException(e);}}
 void save(Room r){try{db.update("INSERT INTO rooms(code,state) VALUES (?,?::jsonb) ON CONFLICT(code) DO UPDATE SET state=excluded.state,updated_at=now()",r.code,json.writeValueAsString(r));}catch(Exception e){throw new IllegalStateException(e);}}
 void changed(Room r){r.seq++;r.lastActivity=now();save(r);broadcast.accept(r.code,Map.of("type","state","room",r,"serverTime",now()));}
 void connected(String code,long id){Room r=get(code,id);synchronized(r){if(r.disconnected.remove(id)!=null)changed(r);auth.online(id);}}
 void disconnected(String code,long id){Room r=rooms.get(code);if(r==null)return;synchronized(r){if(id<0||r.phase.equals("FINISHED")||r.seats.stream().noneMatch(s->s.user.id()==id))return;if(!r.disconnected.containsKey(id)){boolean used=!r.phase.equals("LOBBY")&&!r.graceUsed.add(id);r.disconnected.put(id,now()-(used?60000:0));changed(r);if(!used&&Set.of("AIM","SHOT","ROUND_END").contains(r.phase)&&r.returnReminded.add(id)&&bot!=null){save(r);bot.remindReturn(id,r.code);} }}}
 @Scheduled(fixedDelay=250) void tick(){long now=now();for(Room r:rooms.values()){synchronized(r){try{
  long delta=Math.min(2000,now-r.lastTick);r.lastTick=now;
  if((r.phase.equals("LOBBY")||r.phase.equals("FINISHED"))&&now-r.lastActivity>3600000){rooms.remove(r.code);db.update("DELETE FROM rooms WHERE code=?",r.code);continue;}
  if(Set.of("AIM","SHOT","ROUND_END").contains(r.phase)&&!r.disconnected.isEmpty()){
   var lost=r.disconnected.entrySet().stream().filter(e->now-e.getValue()>=60000).findFirst();if(lost.isPresent()){finish(r,1-index(r,lost.get().getKey())%2,"Техническое поражение: игрок не вернулся");changed(r);continue;}
   if(r.phase.equals("AIM")){r.deadline+=delta;continue;}
  }
  if(r.phase.equals("AIM")&&now<r.deadline&&r.mode.equals("bot")&&r.seats.get(r.turn).user.id()<0&&r.disconnected.isEmpty()){scheduleBot(r,now);continue;}
  if(r.phase.equals("SHOT")&&now>=r.deadline){r.shot=null;if(r.winner>=0){if(r.scores[r.winner]>=r.target)finish(r,r.winner,"Матч завершён");else r.phase="ROUND_END";}else{r.phase="AIM";r.deadline=now+r.seconds*1000L;}changed(r);}
  else if(r.capacity()>1&&r.phase.equals("AIM")&&now>=r.deadline){advance(r);r.ballInHand=true;r.balls.get(0).pocketed=true;r.message="Время вышло — биток с руки";r.deadline=now+r.seconds*1000L;changed(r);}
 }catch(Exception e){System.err.println("Room tick failed: "+r.code+" "+e.getClass().getSimpleName());}}}}
 static long replayMillis(Physics.Shot shot){long pots=shot.events().stream().filter(e->e.type().equals("pocket")&&e.ball()!=0).count();return pots==0?400:Math.min(3,Math.max(2,pots))*2400;}
 void scheduleBot(Room r,long now){
  if(r.botDue==0){r.botDue=now+850;return;}if(now<r.botDue||!botJobs.add(r.id))return;
  try{botExecutor.execute(()->{try{synchronized(r){if(!r.phase.equals("AIM")||r.seats.get(r.turn).user.id()>=0||!r.disconnected.isEmpty())return;botTurn(r);}}catch(Exception e){System.err.println("Bot turn failed: "+e.getClass().getSimpleName());}finally{botJobs.remove(r.id);}});}catch(RejectedExecutionException busy){botJobs.remove(r.id);}
 }
 void botTurn(Room r){
  long id=r.seats.get(r.turn).user.id();if(id>=0||!r.mode.equals("bot"))throw new IllegalArgumentException("Not a bot turn");
  // Planning and execution share the room lock, so reconnects or other commands cannot race it.
  if(r.ballInHand){double[] p=AimBot.placement(r.balls,r.groups[r.team()],r.breaking);command(r.code,id,json.createObjectNode().put("action","place").put("id",UUID.randomUUID().toString()).put("seq",r.seq).put("x",p[0]).put("y",p[1]));}
  AimBot.Plan plan=AimBot.choose(r.balls,r.groups[r.team()],r.breaking,r.botLevel,Objects.hash(r.id,r.seq,r.round));
  command(r.code,id,json.createObjectNode().put("action","shoot").put("id",UUID.randomUUID().toString()).put("seq",r.seq).put("angle",plan.angle()).put("power",plan.power()).put("spinX",0).put("spinY",0).put("pocket",plan.pocket()));r.botDue=0;
 }
 static long now(){return System.currentTimeMillis();}
}
