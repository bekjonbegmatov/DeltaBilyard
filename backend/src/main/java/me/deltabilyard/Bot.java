package me.deltabilyard;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service public class Bot {
 final String token,url;final Auth auth;final ObjectMapper json;final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();long offset=0;
 Bot(Auth a,ObjectMapper j,@Value("${app.bot-token}")String t,@Value("${app.base-url}")String u){auth=a;json=j;token=t;url=u;}
 JsonNode call(String method,Object body)throws Exception {HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot"+token+"/"+method)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();JsonNode result=json.readTree(client.send(request,HttpResponse.BodyHandlers.ofString()).body());if(!result.path("ok").asBoolean())throw new IllegalArgumentException("Telegram не доставил сообщение. Игрок должен сначала запустить бота");return result.path("result");}
 void invite(long id,String name,String code){try{Boolean started=auth.db.queryForObject("SELECT bot_started FROM players WHERE id=?",Boolean.class,id);if(!Boolean.TRUE.equals(started))throw new IllegalArgumentException("Друг ещё не нажал /start в боте; отправьте ссылку вручную");call("sendMessage",Map.of("chat_id",id,"text",name+" приглашает в DeltaBilyard. Комната "+code,"reply_markup",Map.of("inline_keyboard",List.of(List.of(Map.of("text","Принять приглашение","web_app",Map.of("url",url+"?room="+code)))))));}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Не удалось отправить приглашение");}}
 // Reminder delivery never blocks the game's fixed tick or room monitor.
 final java.util.concurrent.ExecutorService reminders=new java.util.concurrent.ThreadPoolExecutor(1,1,0L,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<>(512),r->{Thread t=new Thread(r,"return-reminders");t.setDaemon(true);return t;},new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy());
 @jakarta.annotation.PreDestroy void closeReminders(){reminders.shutdownNow();}
 void remindReturn(long id,String code){if(token.isBlank()||id<0)return;reminders.execute(()->{try{
  Boolean started=auth.db.queryForObject("SELECT bot_started FROM players WHERE id=?",Boolean.class,id);
  if(!Boolean.TRUE.equals(started))return;
  call("sendMessage",Map.of("chat_id",id,"text","Связь со столом прервалась. Вернитесь в DeltaBilyard: на восстановление соединения даётся до 60 секунд.","reply_markup",Map.of("inline_keyboard",List.of(List.of(Map.of("text","Вернуться за стол","web_app",Map.of("url",url+"?room="+code)))))));
 }catch(Exception ignored){/* Bot may be blocked or chat never started; gameplay remains available. */}});}
 @Scheduled(fixedDelay=3000) void poll(){if(token.isBlank())return;try{JsonNode updates=call("getUpdates",Map.of("offset",offset,"timeout",0,"allowed_updates",List.of("message")));for(JsonNode update:updates){offset=update.path("update_id").asLong()+1;JsonNode message=update.path("message"),from=message.path("from");if(!message.path("chat").path("type").asText().equals("private")||!message.path("text").asText().startsWith("/start"))continue;String text=message.path("text").asText();String start=text.contains(" ")?text.substring(text.indexOf(" ")+1).trim():"";String destination=url;if(start.matches("room_[0-9]{4}"))destination=url+"?room="+start.substring(5);else if(start.matches("friend_[0-9]+"))destination=url+"?friend="+start.substring(7);long id=from.path("id").asLong();auth.db.update("INSERT INTO players(id,name,username,bot_started) VALUES (?,?,?,true) ON CONFLICT(id) DO UPDATE SET bot_started=true",id,from.path("first_name").asText(),from.path("username").asText(null));call("sendMessage",Map.of("chat_id",id,"text","Добро пожаловать в DeltaBilyard. Создайте стол, пригласите друзей и играйте вместе.","reply_markup",Map.of("inline_keyboard",List.of(List.of(Map.of("text","Открыть бильярд","web_app",Map.of("url",destination)))))));}}catch(Exception ignored){/* Do not log URLs containing bot credentials. */}}
}
