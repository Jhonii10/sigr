package co.sigr.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey clave;
  private final long expiracionMinutos;

  public JwtService(@Value("${sigr.jwt.secret}") String secreto,
                    @Value("${sigr.jwt.expiracion-minutos:480}") long expiracionMinutos) {
    if (secreto.length() < 32) {
      throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
    }
    this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    this.expiracionMinutos = expiracionMinutos;
  }

  public String generar(String email, String rol) {
    Instant ahora = Instant.now();
    return Jwts.builder()
        .subject(email)
        .claim("rol", rol)
        .issuedAt(Date.from(ahora))
        .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
        .signWith(clave)
        .compact();
  }

  /** Devuelve el email (subject) si el token es válido. */
  public Optional<String> validar(String token) {
    try {
      Claims claims = Jwts.parser().verifyWith(clave).build()
          .parseSignedClaims(token).getPayload();
      return Optional.ofNullable(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
