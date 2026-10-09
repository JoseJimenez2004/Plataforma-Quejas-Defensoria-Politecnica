package ipn.escom.defensoria.revision_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /** Para leer el catálogo de dependencias (área a la que se turna) de catalogo-service y
     * para pedirle a notificaciones-service que mande el correo de rechazo -- ambos son
     * llamadas simples de un solo servicio, no se justifica traer OpenFeign. */
    @Bean
    public RestTemplate restTemplate() {
        // Sin timeouts, si el otro servicio no responde (caído o bloqueado por firewall) la
        // petición de este servicio se queda colgada hasta que nginx corta con 504.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(fabrica);
    }
}
