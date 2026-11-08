package me.deltabilyard;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.net.URLDecoder;

@Service public class Auth {
  final ObjectMapper json;final JdbcTemplate db;final StringRedisTemplate redis;final String botToken,secret;final boolean dev;
  public record User(long id,String name,String username,String avatar){}
  public Auth(ObjectMapper json,JdbcTemplate db,StringRedisTemplate redis,@Value("${app.bot-token}")String botToken,@Value("${app.session-secret}")String secret,@Value("${app.dev-auth}")boolean dev){this.json=json;this.db=db;this.redis=redis;this.botToken=botToken;this.secret=secret;this.dev=dev;if(secret.length()<32)throw new IllegalArgumentException("SESSION_SECRET must contain at least 32 characters");}
  static byte[] hmac(byte[] key,String input){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key,"HmacSHA256"));return m.doFinal(input.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
  static String b64(byte[] b){return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
  User login(String initData) throws Exception {
    User u;
    if(dev&&initData.startsWith("dev:")){long id=Long.parseLong(initData.substring(4));if(id<=0)throw new IllegalArgumentException("Invalid ID");u=new User(id,"Игрок "+id,"dev"+id,null);}
    else {
      TreeMap<String,String> p=new TreeMap<>();for(String pair:initData.split("&")){String[] a=pair.split("=",2);if(a.length!=2)throw new IllegalArgumentException("Telegram authentication required");String key=URLDecoder.decode(a[0],StandardCharsets.UTF_8);if(p.put(key,URLDecoder.decode(a[1],StandardCharsets.UTF_8))!=null)throw new IllegalArgumentException("Duplicate Telegram field");}
      String hash=p.remove("hash");if(hash==null||botToken.isBlank())throw new IllegalArgumentException("Откройте игру через Telegram");
      long age=System.currentTimeMillis()/1000-Long.parseLong(p.getOrDefault("auth_date","0"));if(age< -30||age>3600)throw new IllegalArgumentException("Откройте Mini App заново");
      String check=String.join("\n",p.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toList());
      byte[] key=hmac("WebAppData".getBytes(StandardCharsets.UTF_8),botToken);
      if(!MessageDigest.isEqual(HexFormat.of().parseHex(hash),hmac(key,check)))throw new IllegalArgumentException("Неверная подпись Telegram");
      JsonNode n=json.readTree(p.get("user"));u=new User(n.path("id").asLong(),(n.path("first_name").asText()+" "+n.path("last_name").asText()).trim(),n.path("username").asText(null),n.path("photo_url").asText(null));if(u.id<=0)throw new IllegalArgumentException("Нет пользователя Telegram");
    }
    db.update("INSERT INTO players(id,name,username,avatar) VALUES (?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,username=excluded.username,avatar=excluded.avatar,updated_at=now()",u.id,u.name,u.username,u.avatar);return u;
  }
  String token(User u){return jwt(Map.of("sub",String.valueOf(u.id),"exp",System.currentTimeMillis()/1000+86400),secret);}
  String jwt(Map<String,Object> claims,String key){try{String h=b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)),p=b64(json.writeValueAsBytes(claims));return h+"."+p+"."+b64(hmac(key.getBytes(StandardCharsets.UTF_8),h+"."+p));}catch(Exception e){throw new IllegalStateException(e);}}
  long verify(String token){try{String[] p=token.replaceFirst("^Bearer ","").split("\\.");if(p.length!=3||!MessageDigest.isEqual(Base64.getUrlDecoder().decode(p[2]),hmac(secret.getBytes(StandardCharsets.UTF_8),p[0]+"."+p[1])))throw new IllegalArgumentException();JsonNode n=json.readTree(Base64.getUrlDecoder().decode(p[1]));if(n.path("exp").asLong()<System.currentTimeMillis()/1000)throw new IllegalArgumentException();return Long.parseLong(n.path("sub").asText());}catch(Exception e){throw new IllegalArgumentException("Сессия истекла. Откройте игру заново");}}
  User user(long id){return db.queryForObject("SELECT * FROM players WHERE id=?",(r,i)->new User(r.getLong("id"),r.getString("name"),r.getString("username"),r.getString("avatar")),id);}
  void limit(String key,int max,int seconds){Long n=redis.opsForValue().increment("limit:"+key);if(n!=null&&n==1)redis.expire("limit:"+key,Duration.ofSeconds(seconds));if(n!=null&&n>max)throw new IllegalArgumentException("Слишком часто. Попробуйте позже");}
  void online(long id){redis.opsForValue().set("online:"+id,"1",Duration.ofSeconds(45));}
}
