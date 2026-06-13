package ipn.escom.defensoria.quejoso.config;

import ipn.escom.defensoria.quejoso.entity.Usuario;
import ipn.escom.defensoria.quejoso.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * NO lleva @Component para evitar auto-registro como servlet filter.
 * Se instancia como @Bean en SecurityConfig y se agrega SOLO a la cadena
 * de Spring Security via addFilterBefore().
 */
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public JwtRequestFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // ─── DEBUG: ver qué llega en cada petición ───
        System.out.println(">>> [JWT-FILTER] " + request.getMethod() + " " + request.getRequestURI());
        System.out.println(">>> [JWT-FILTER] Authorization header: " +
                (authorizationHeader != null ? authorizationHeader.substring(0, Math.min(30, authorizationHeader.length())) + "..." : "NULL"));

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extraerCorreo(jwt);
                System.out.println(">>> [JWT-FILTER] Email extraído del token: " + username);
            } catch (Exception e) {
                System.out.println(">>> [JWT-FILTER] ERROR al extraer correo: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            boolean tokenValido = jwtUtil.validarToken(jwt);
            System.out.println(">>> [JWT-FILTER] Token válido: " + tokenValido);

            if (tokenValido) {
                Usuario usuario = usuarioRepository.findByCorreoInstitucional(username).orElse(null);
                System.out.println(">>> [JWT-FILTER] Usuario encontrado en BD: " + (usuario != null ? "SÍ (id=" + usuario.getId() + ")" : "NO"));

                if (usuario != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(usuario, null, new ArrayList<>());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println(">>> [JWT-FILTER] ✅ Autenticación establecida en SecurityContext");
                }
            }
        } else if (username != null) {
            System.out.println(">>> [JWT-FILTER] SecurityContext YA tiene autenticación (se omite)");
        }

        chain.doFilter(request, response);
    }
}