package ipn.escom.defensoria.admin_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Cliente HTTP simple para pedirle a catalogo-service el total de dependencias en el
     * resumen del dashboard -- no se justifica traer todo Spring Cloud OpenFeign para una
     * sola llamada de lectura. */
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
