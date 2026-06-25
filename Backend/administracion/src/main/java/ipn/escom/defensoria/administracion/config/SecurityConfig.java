package ipn.escom.defensoria.administracion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // 1. Filtro CORS Global: Actúa ANTES que cualquier regla de Spring Security
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // Permite que Angular en localhost:35959 se conecte
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Aplica a TODOS los endpoints del proyecto
        return new CorsFilter(source);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 2. Permitir explícitamente todas las peticiones OPTIONS del Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 3. Rutas públicas (Login, Errores y Tests)
                .requestMatchers("/api/admin/auth/**", "/api/public/**", "/error").permitAll()
                
                // 4. Rutas protegidas por roles
                .requestMatchers("/api/admin/ti/**").hasRole("ADMIN_TI")
                .requestMatchers("/api/admin/recepcion/**").hasAnyRole("RECEPCION", "ADMIN_TI")
                
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // NUEVO: Proveedor de usuarios en memoria para pruebas de desarrollo
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails adminTi = User.builder()
            .username("bicho_admin")
            .password("ti123") // Como usas NoOpPasswordEncoder, la contraseña va en texto plano
            .roles("ADMIN_TI")
            .build();

        UserDetails recepcion = User.builder()
            .username("usuario_recepcion")
            .password("recepcion123")
            .roles("RECEPCION")
            .build();

        return new InMemoryUserDetailsManager(adminTi, recepcion);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); 
        provider.setPasswordEncoder(passwordEncoder); 
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider); 
    }
}