package ipn.escom.defensoria.primercontacto.config;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String extraerCorreo(String token) {
        return extraerClaim(
                token,
                Claims::getSubject
        );
    }

    public String extraerRol(String token) {
        return extraerClaim(
                token,
                claims -> claims.get("rol", String.class)
        );
    }

    public boolean validarToken(
            String token,
            String correo
    ) {

        final String correoExtraido =
                extraerCorreo(token);

        return correoExtraido.equals(correo)
                && !isTokenExpirado(token);
    }

    private <T> T extraerClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {

        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpirado(
            String token
    ) {

        return extraerClaim(
                token,
                Claims::getExpiration
        ).before(new Date());
    }
}