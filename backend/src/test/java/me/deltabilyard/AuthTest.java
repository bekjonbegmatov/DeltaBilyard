package me.deltabilyard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
class AuthTest {
 final Auth auth=new Auth(new ObjectMapper(),null,null,"", "123456789012345678901234567890123456",false);
 @Test void tokenCannotBeForged(){String token=auth.token(new Auth.User(42,"Test",null,null));assertEquals(42,auth.verify(token));assertThrows(IllegalArgumentException.class,()->auth.verify(token+"x"));}
 @Test void expiredTokenRejected(){String token=auth.jwt(Map.of("sub","42","exp",1),auth.secret);assertThrows(IllegalArgumentException.class,()->auth.verify(token));}
 @Test void developmentIdentityDisabled(){assertThrows(Exception.class,()->auth.login("dev:42"));}
}
