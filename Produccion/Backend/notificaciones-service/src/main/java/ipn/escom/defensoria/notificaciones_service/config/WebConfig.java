package ipn.escom.defensoria.notificaciones_service.config;

import java.util.List;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HALLAZGO CORREGIDO: este servicio no tenía ningún SecurityFilterChain propio, así que con
 * spring-boot-starter-security en el classpath, Spring Security aplicaba su configuración por
 * defecto (todo autenticado con usuario/password aleatorios generados en cada arranque). Eso
 * significa que POST /enviar probablemente rechazaba TODAS las llamadas de otros
 * microservicios (ej. revision-service pidiendo el correo de rechazo) con 401 -- el error se
 * tragaba silenciosamente porque esa llamada está en un try/catch que solo loguea (ver
 * revision-service/service/NotificacionQuejaService). Ahora que este servicio sí tiene JWT
 * propio, se deja: /enviar y /registrar públicos (llamadas internas entre microservicios,
 * igual que el catálogo público de dependencias), y /mias + /{id}/leida protegidos (los
 * consume directamente el panel del quejoso).
 */
@Configuration
@EnableWebSecurity
public class WebConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    @SneakyThrows
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/notificaciones/enviar", "/api/notificaciones/registrar").permitAll()
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
