package ipn.escom.defensoria.chatbot_service.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Igual que en catalogo-service: este servicio nunca emite tokens, solo verifica los que ya
 * emitió admin-service (mismo jwt.secret compartido entre los 7 microservicios). El chatbot es
 * público y de solo lectura para el quejoso; solo el CRUD de contenido bajo /admin exige un
 * token válido con rol ADMIN_SISTEMAS.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = this.secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extraerUsuario(String token) {
        return extraerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    public boolean validarToken(String token, String username) {
        String usuarioToken = extraerUsuario(token);
        return (usuarioToken.equals(username) && !isTokenExpirado(token));
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpirado(String token) {
        return extraerClaims(token).getExpiration().before(new Date());
    }
}
