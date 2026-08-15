package ipn.escom.defensoria.revision_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /** Para leer el catálogo de dependencias (área a la que se turna) de catalogo-service y
     * para pedirle a notificaciones-service que mande el correo de rechazo -- ambos son
     * llamadas simples de un solo servicio, no se justifica traer OpenFeign. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
