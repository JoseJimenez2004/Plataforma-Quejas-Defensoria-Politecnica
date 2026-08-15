package ipn.escom.defensoria.admin_service.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A diferencia del filtro equivalente en los otros microservicios, este SÍ agrega una
 * autoridad ROLE_&lt;rol&gt; (si el token trae el claim "rol") para poder usar
 * @PreAuthorize("hasRole('ADMIN_SISTEMAS')") en los controllers de este panel. Un token sin
 * ese claim (ej. de un quejoso) queda autenticado pero sin ningún rol -- no puede pasar los
 * @PreAuthorize de este servicio.
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
                        String rol = jwtUtil.extraerRol(token);
                        List<SimpleGrantedAuthority> authorities = (rol != null)
                                ? List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                                : List.of();

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                correo, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ex) {
                // Token corrupto/expirado/mal firmado -- lo dejamos sin autenticar, la cadena
                // de seguridad se encarga de responder 401/403 más adelante.
            }
        }
        filterChain.doFilter(request, response);
    }
}
