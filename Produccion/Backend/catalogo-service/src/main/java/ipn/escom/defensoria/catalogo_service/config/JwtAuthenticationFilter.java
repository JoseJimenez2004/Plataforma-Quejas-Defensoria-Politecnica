package ipn.escom.defensoria.catalogo_service.config;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
                String username = jwtUtil.extraerUsuario(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (jwtUtil.validarToken(token, username)) {
                        // Si el token trae claim "rol" (tokens de admin-service), se agrega
                        // como autoridad ROLE_<rol> para que @PreAuthorize funcione en los
                        // endpoints de administración del catálogo. Tokens de quejosos (sin
                        // ese claim) quedan autenticados pero sin ningún rol.
                        String rol = jwtUtil.extraerRol(token);
                        List<SimpleGrantedAuthority> authorities = (rol != null)
                                ? List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                                : List.of();

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                username, null, authorities
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
