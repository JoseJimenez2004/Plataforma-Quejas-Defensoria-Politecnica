package ipn.escom.defensoria.cuenta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Seguimos sin CSRF para que Postman no te rebote
            .authorizeHttpRequests(auth -> auth
                // Solo pasamos el String de la ruta, Spring se encarga del resto
                .requestMatchers("/api/usuarios/registro").permitAll()
                .requestMatchers("/api/usuarios/login").permitAll()
                .requestMatchers("/api/usuarios/recuperar-password").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .httpBasic(basic -> {}) 
            .formLogin(form -> form.permitAll());

        return http.build();
    }
}