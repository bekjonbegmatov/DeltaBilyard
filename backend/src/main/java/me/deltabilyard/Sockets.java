package me.deltabilyard;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.*;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.*;

@Configuration @EnableWebSocket public class Sockets implements WebSocketConfigurer {
 final Auth auth;final Games games;final ObjectMapper json;final String origin;final Map<String,WebSocketSession> sessions=new ConcurrentHashMap<>();
 Sockets(Auth a,Games g,ObjectMapper j,@Value("${app.base-url}")String origin){auth=a;games=g;json=j;this.origin=origin;games.broadcast=this::broadcast;}
 public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(new Handler(),"/ws").setAllowedOrigins(origin,"http://localhost:5173").addInterceptors(new HandshakeInterceptor(){
  public boolean beforeHandshake(ServerHttpRequest req,ServerHttpResponse res,WebSocketHandler h,Map<String,Object> attrs){String q=req.getURI().getRawQuery();if(q==null||!q.matches("ticket=[a-f0-9-]{36}"))return false;String v=auth.redis.opsForValue().getAndDelete("ticket:"+q.substring(7));if(v==null)return false;String[] parts=v.split(":");long id=Long.parseLong(parts[0]);games.get(parts[1],id);attrs.put("id",id);attrs.put("code",parts[1]);return true;}
  public void afterHandshake(ServerHttpRequest r,ServerHttpResponse s,WebSocketHandler h,Exception e){}
 });}
 void send(WebSocketSession s,Object payload){try{if(s.isOpen())s.sendMessage(new TextMessage(payload instanceof String?(String)payload:json.writeValueAsString(payload)));}catch(Exception e){try{s.close();}catch(Exception ignored){}}}
 void broadcast(String code,Object payload){try{String encoded=json.writeValueAsString(payload);for(WebSocketSession s:sessions.values())if(code.equals(s.getAttributes().get("code"))&&(games.rooms.get(code)==null||games.rooms.get(code).seats.stream().anyMatch(seat->Objects.equals(seat.user.id(),s.getAttributes().get("id")))))send(s,encoded);}catch(Exception e){throw new IllegalStateException("State serialization failed",e);}}
 class Handler extends TextWebSocketHandler {
  @Override public void afterConnectionEstablished(WebSocketSession raw){WebSocketSession s=new ConcurrentWebSocketSessionDecorator(raw,5000,2_000_000);s.setTextMessageSizeLimit(4096);sessions.put(raw.getId(),s);long id=(long)s.getAttributes().get("id");String code=(String)s.getAttributes().get("code");games.connected(code,id);send(s,Map.of("type","state","room",games.get(code,id),"serverTime",Games.now()));}
  @Override protected void handleTextMessage(WebSocketSession raw,TextMessage message){WebSocketSession s=sessions.get(raw.getId());if(s==null)return;long id=(long)s.getAttributes().get("id");String code=(String)s.getAttributes().get("code");try{JsonNode n=json.readTree(message.getPayload());String type=n.path("type").asText();auth.online(id);
   if(type.equals("ping")){send(s,Map.of("type","pong","serverTime",Games.now(),"echo",n.path("time").asLong()));return;}
   if(type.equals("reaction")){games.get(code,id);auth.limit("reaction:"+id,5,10);String emoji=n.path("emoji").asText();if(!Set.of("👏","🔥","🎯","😎","🤝","😮","😂").contains(emoji))return;broadcast(code,Map.of("type","reaction","user",id,"emoji",emoji));return;}
   if(type.equals("aim")){auth.limit("aim:"+id,50,1);Games.Room r=games.get(code,id);synchronized(r){if(!r.phase.equals("AIM")||r.seats.get(r.turn).user.id()!=id)return;double angle=n.path("angle").asDouble();if(!Double.isFinite(angle))return;broadcast(code,Map.of("type","aim","angle",angle,"user",id));}return;}
   auth.limit("command:"+id,40,10);games.command(code,id,n);
  }catch(Exception e){send(s,Map.of("type","error","error",e instanceof IllegalArgumentException?String.valueOf(e.getMessage()):"Не удалось выполнить команду"));}}
  @Override public void afterConnectionClosed(WebSocketSession s,CloseStatus status){sessions.remove(s.getId());long id=(long)s.getAttributes().get("id");String code=(String)s.getAttributes().get("code");boolean other=sessions.values().stream().anyMatch(x->x.isOpen()&&code.equals(x.getAttributes().get("code"))&&Objects.equals(id,x.getAttributes().get("id")));if(!other)games.disconnected(code,id);}
 }
}
