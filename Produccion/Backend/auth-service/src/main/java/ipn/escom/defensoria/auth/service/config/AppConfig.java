package ipn.escom.defensoria.auth.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Mismo patrón que admin-service/config/AppConfig.java -- faltaba este bean aquí, por eso
 * UsuarioService (que ya dependía de PasswordEncoder) no podía arrancar. */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
