package ipn.escom.defensoria.quejoso.config;

import ipn.escom.defensoria.quejoso.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtRequestFilter jwtRequestFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        return new JwtRequestFilter(jwtUtil, usuarioRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // --- RUTAS PÚBLICAS DE SWAGGER ---
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        
                        // 1. RUTAS PÚBLICAS EXISTENTES
                        .requestMatchers("/api/quejoso/auth/**").permitAll()
                        .requestMatchers("/api/quejoso/quejas/registrar").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/quejoso/quejas/seguimiento/publico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quejoso/quejas/evidencias/*/publico").permitAll()

                        // 2. RUTAS PROTEGIDAS
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}