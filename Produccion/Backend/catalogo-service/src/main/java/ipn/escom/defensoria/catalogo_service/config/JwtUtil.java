package ipn.escom.defensoria.catalogo_service.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Igual que en auth-service/queja-service: valida tokens firmados con el mismo
 * jwt.secret compartido entre microservicios. Hoy el catálogo es de solo lectura y
 * público, pero se deja listo para cuando existan endpoints de administración del
 * catálogo (crear/editar dependencias) que sí requieran sesión.
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

    /** Puede ser null (tokens de quejosos emitidos por auth.service no llevan este claim) --
     * solo los tokens de admin-service lo traen. */
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
