package ipn.escom.defensoria.notificaciones_service.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Este servicio nunca emite tokens, solo verifica los que ya emitieron auth-service (quejoso)
 * o admin-service (personal) -- mismo jwt.secret compartido entre todos los microservicios. */
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
