package ipn.escom.defensoria.subdefensoria.config;

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

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER_AUTORIZACION = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";
    private static final String PREFIJO_ROL = "ROLE_";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader(HEADER_AUTORIZACION);

        if (authHeader != null
                && authHeader.startsWith(PREFIJO_BEARER)) {

            String token =
                    authHeader.substring(PREFIJO_BEARER.length());

            try {

                String correo =
                        jwtUtil.extraerCorreo(token);

                if (correo != null
                        && SecurityContextHolder
                        .getContext()
                        .getAuthentication() == null) {

                    if (jwtUtil.validarToken(token, correo)) {

                        String rol =
                                jwtUtil.extraerRol(token);

                        List<SimpleGrantedAuthority> authorities =
                                rol != null
                                        ? List.of(
                                        new SimpleGrantedAuthority(
                                                PREFIJO_ROL + rol
                                        )
                                )
                                        : List.of();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        correo,
                                        null,
                                        authorities
                                );

                        SecurityContextHolder
                                .getContext()
                                .setAuthentication(authentication);
                    }
                }

            } catch (Exception ex) {

                log.debug(
                        "Token JWT inválido o expirado: {}",
                        ex.getMessage()
                );
            }
        }

        filterChain.doFilter(request, response);
    }
}