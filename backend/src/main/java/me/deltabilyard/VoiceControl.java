package me.deltabilyard;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
@Service public class VoiceControl {
 private final Auth auth;private final String key,secret;private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 public VoiceControl(Auth auth,@Value("${app.livekit-key}")String key,@Value("${app.livekit-secret}")String secret){this.auth=auth;this.key=key;this.secret=secret;}
 void remove(String room,long id){call("RemoveParticipant",room,Map.of("room",room,"identity",String.valueOf(id)));}
 void close(String room){call("DeleteRoom",room,Map.of("room",room));}
 private void call(String method,String room,Object body){if(key.isBlank()||secret.isBlank())return;try{String jwt=auth.jwt(Map.of("iss",key,"exp",System.currentTimeMillis()/1000+60,"video",Map.of("roomAdmin",true,"roomCreate",true,"room",room)),secret);HttpRequest request=HttpRequest.newBuilder(URI.create("http://livekit:7880/twirp/livekit.RoomService/"+method)).timeout(Duration.ofSeconds(3)).header("Authorization","Bearer "+jwt).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(auth.json.writeValueAsString(body))).build();client.sendAsync(request,HttpResponse.BodyHandlers.discarding()).exceptionally(e->null);}catch(Exception ignored){}}
}
