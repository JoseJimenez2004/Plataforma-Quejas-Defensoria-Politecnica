package ipn.escom.defensoria.notificaciones_service.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Por default, Spring Boot NO imprime en consola cada petición HTTP que recibe. Este filtro
 * registra método, ruta, IP de origen, status de respuesta y duración de cada petición.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("PETICIONES");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duracionMs = System.currentTimeMillis() - inicio;
            log.info("{} {} -> {} ({} ms) [{}]",
                    request.getMethod(),
                    request.getRequestURI() + nombresDeParametros(request),
                    response.getStatus(),
                    duracionMs,
                    request.getRemoteAddr());
        }
    }

    /**
     * Antes se imprimía getQueryString() completo (Kiuwan: "Avoid getQueryString()"). Ahora solo
     * se registran los NOMBRES de los parámetros, así no quedan en el log valores sensibles
     * (correos, tokens, folios).
     */
    private static String nombresDeParametros(HttpServletRequest request) {
        if (request.getParameterMap().isEmpty()) {
            return "";
        }
        return "?" + String.join("&", request.getParameterMap().keySet());
    }
}
