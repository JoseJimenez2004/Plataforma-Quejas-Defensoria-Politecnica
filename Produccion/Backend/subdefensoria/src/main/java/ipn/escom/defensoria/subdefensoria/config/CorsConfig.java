package ipn.escom.defensoria.subdefensoria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS solo importa para llamadas hechas desde un navegador (el
<<<<<<< HEAD:Produccion/Backend/subdefensoria/src/main/java/ipn/escom/defensoria/subdefensoria/config/CorsConfig.java
 * front Angular de Subdefensoria). La llamada de ingesta que hace
 * Primer Contacto hacia este servicio es servidor-a-servidor y no
 * pasa por CORS.
=======
 * front Angular en localhost:4300). Las llamadas de Subdefensoría al
 * endpoint de ingesta son servidor-a-servidor y no pasan por CORS,
 * así que no hace falta agregar su puerto aquí a menos de que ellos
 * también tengan un front que golpee esta API directo desde el
 * navegador.
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/config/CorsConfig.java
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
