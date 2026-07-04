package ipn.escom.defensoria.quejoso.config;

import ipn.escom.defensoria.quejoso.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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

    // Configuración explícita para evitar el bloqueo CORS desde el navegador
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite peticiones desde cualquier origen (ajusta si es necesario por seguridad)
        configuration.setAllowedOrigins(Arrays.asList("*")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                // Habilitamos CORS usando el Bean definido arriba
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // --- RUTAS PÚBLICAS ---
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/quejoso/auth/**").permitAll()
                        .requestMatchers("/api/quejoso/quejas/registrar").permitAll()
                        .requestMatchers("/api/quejoso/quejas/seguimiento/publico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quejoso/quejas/evidencias/*/publico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quejoso/quejas/*/acuse/publico").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // --- RUTAS PROTEGIDAS ---
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}