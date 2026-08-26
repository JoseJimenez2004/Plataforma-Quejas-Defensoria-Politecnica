package ipn.escom.defensoria.primercontacto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http

                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf.disable())

                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Permitir preflight CORS
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Consola local de desarrollo
                        .requestMatchers(
                                "/h2-console/**"
                        ).permitAll()

                        /*
                         * Comunicación interna:
                         *
                         * Revisión
                         *      ↓
                         * Primer Contacto
                         *
                         * Este endpoint actualmente no envía JWT,
                         * por lo que debe mantenerse disponible.
                         */
                        .requestMatchers(
                                "/api/primer-contacto/ingesta/**"
                        ).permitAll()

                        /*
                         * Todas las operaciones normales de
                         * Primer Contacto requieren un JWT
                         * con rol ANALISTA_PRIMER_CONTACTO.
                         */
                        .requestMatchers(
                                "/api/primer-contacto/**"
                        ).hasRole("ANALISTA_PRIMER_CONTACTO")

                        .anyRequest().permitAll()
                )

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }
}