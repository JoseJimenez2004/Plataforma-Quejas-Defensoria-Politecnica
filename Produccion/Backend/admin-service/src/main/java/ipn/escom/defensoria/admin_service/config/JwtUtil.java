package ipn.escom.defensoria.admin_service.config;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Mismo jwt.secret compartido con auth.service/queja-service/catalogo-service — un token
 * emitido aquí es válido en cualquier otro microservicio (y viceversa). A diferencia de los
 * tokens de quejosos, estos SÍ llevan un claim "rol" para poder restringir qué puede hacer
 * cada tipo de cuenta de personal (@PreAuthorize en los controllers).
 */
@Component
public class JwtUtil {

    private final Key key;
    private final long tiempoExpiracion = 3600000;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String correo, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(correo)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + tiempoExpiracion))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraerCorreo(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /** Puede ser null si el token no trae el claim "rol" (ej. tokens de quejosos emitidos por
     * auth.service) — quien llame esto debe tratar null como "sin permisos de este panel". */
    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
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
