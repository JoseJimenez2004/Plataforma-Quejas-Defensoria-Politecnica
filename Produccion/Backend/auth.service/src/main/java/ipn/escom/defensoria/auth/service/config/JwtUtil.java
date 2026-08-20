package ipn.escom.defensoria.auth.service.config;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Mismo jwt.secret compartido con admin-service/catalogo-service/chatbot-service/
 * notificaciones-service/queja-service -- un token emitido aquí es válido en cualquier otro
 * microservicio (y viceversa). A diferencia de los tokens de personal administrativo (emitidos
 * por admin-service), los tokens de quejoso NO llevan claim "rol": quedan autenticados pero
 * sin ningún rol, tal como ya asumen los JwtAuthenticationFilter del resto de servicios.
 */
@Component
public class JwtUtil {

    private final Key key;
    private final long tiempoExpiracion = 3600000;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String correo) {
        return Jwts.builder()
                .setSubject(correo)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + tiempoExpiracion))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraerCorreo(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public boolean validarToken(String token, String correo) {
        final String correoExtraido = extraerCorreo(token);
        return (correoExtraido.equals(correo) && !isTokenExpirado(token));
    }

    private <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }
}
