package ipn.escom.defensoria.auth.service.config;

import java.io.IOException;
import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * auth-service nunca había necesitado verificar tokens (solo los emite en /login); todo lo
 * demás en /api/auth/** era público. Se agrega este filtro únicamente para proteger los
 * nuevos endpoints de perfil (GET /me, PUT /perfil), reutilizando el JwtUtil ya existente
 * (mismo jwt.secret compartido con el resto de microservicios).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String correo = jwtUtil.extraerCorreo(token);

                if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (jwtUtil.validarToken(token, correo)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                correo, null, Collections.emptyList()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ex) {
                // Token corrupto/expirado -- se deja sin autenticar.
            }
        }
        filterChain.doFilter(request, response);
    }
}
