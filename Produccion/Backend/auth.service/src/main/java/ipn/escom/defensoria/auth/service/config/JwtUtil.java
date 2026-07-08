package ipn.escom.defensoria.auth.service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // La llave se inyecta desde application.yml, que a su vez toma el valor de la variable de entorno JWT_SECRET
    private final Key key;
    
    // Tiempo de expiración: 1 hora
    private final long tiempoExpiracion = 3600000;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // La llave debe tener al menos 32 caracteres para HS256
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