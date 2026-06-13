package ipn.escom.defensoria.quejoso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    /**
     * Configura CORS a nivel de Spring MVC (DispatcherServlet).
     * Cubre los requests que llegan a los controllers normalmente.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    /**
     * Bean explícito de CorsConfigurationSource requerido por Spring Security 6.x.
     *
     * Problema raíz: cuando http.cors(Customizer.withDefaults()) no encuentra este bean,
     * delega al HandlerMappingIntrospector de Spring MVC. Esto crea un CompositeFilterChainProxy
     * que propaga excepciones (como AuthorizationDeniedException) sin cerrar bien la respuesta,
     * causando ERR_INCOMPLETE_CHUNKED_ENCODING en el navegador incluso para errores legítimos
     * como 401/403. Las respuestas de error de Spring Security llegaban sin headers CORS porque
     * se generaban antes de llegar al DispatcherServlet donde actúa el WebMvcConfigurer.
     *
     * Con este bean, SecurityConfig.cors(Customizer.withDefaults()) lo detecta directamente
     * y aplica los headers CORS en su propia cadena de filtros, garantizando que TODAS las
     * respuestas (incluyendo 401, 403 y errores del GlobalExceptionHandler) los incluyan.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // allowedOriginPattern("*") es equivalente a allowedOrigins("*") pero compatible
        // con configuraciones que puedan requerir credenciales en el futuro
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
