package ar.utn.ba.dds.front_tp.Utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

public class JwtUtils {
  private static final String SECRET = System.getenv("JWT_SECRET") != null
      ? System.getenv("JWT_SECRET")
      : "mi_clave_super_secreta_123456789"; // o usar @Value en Spring

  private static final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

  public static String validarToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }
}