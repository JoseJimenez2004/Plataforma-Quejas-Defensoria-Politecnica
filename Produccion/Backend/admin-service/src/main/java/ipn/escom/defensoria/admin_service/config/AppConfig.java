package ipn.escom.defensoria.admin_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
        return new RestTemplate();
    }
}
