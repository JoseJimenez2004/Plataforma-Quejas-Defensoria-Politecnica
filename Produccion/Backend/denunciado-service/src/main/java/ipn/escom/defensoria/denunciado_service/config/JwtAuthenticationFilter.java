package ipn.escom.defensoria.denunciado_service.config;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length());
            try {
                String usuario = jwtUtil.extraerUsuario(token);
                if (usuario != null && SecurityContextHolder.getContext().getAuthentication() == null
                        && jwtUtil.validarToken(token, usuario)) {
                    String rol = jwtUtil.extraerRol(token);
                    List<SimpleGrantedAuthority> permisos = rol != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                            : List.of();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(usuario, null, permisos));
                }
            } catch (Exception ex) {
                log.debug("Token JWT inválido o expirado: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
