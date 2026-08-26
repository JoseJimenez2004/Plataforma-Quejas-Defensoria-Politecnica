package ipn.escom.defensoria.subdefensoria.config;

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

                        // Preflight CORS
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Consola H2 local
                        .requestMatchers(
                                "/h2-console/**"
                        ).permitAll()

                        /*
                         * Comunicación interna:
                         *
                         * Primer Contacto
                         *      ↓
                         * Subdefensoría
                         *
                         * Actualmente esta llamada server-to-server
                         * no envía JWT.
                         */
                        .requestMatchers(
                                "/api/subdefensoria/ingesta/**"
                        ).permitAll()

                        /*
                         * Operaciones normales de Subdefensoría.
                         */
                        .requestMatchers(
                                "/api/subdefensoria/**"
                        ).hasRole("SUBDEFENSOR")

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