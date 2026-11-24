package com.bovix.config;

import com.bovix.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Segurança da API: stateless (sem sessão/cookies), autenticação via JWT no header Authorization.
 * Públicos: login, registro, esqueci/redefinir senha, GET /api/health e as fotos (URL com chave aleatória).
 * Todo o resto (inclusive /api/auth/me) exige token válido.
 * CORS é aplicado aqui (via CorsConfig.corsConfigurationSource) e as requisições OPTIONS
 * de preflight são liberadas explicitamente — senão o navegador bloqueia toda chamada
 * com JSON ou com o header Authorization antes mesmo dela chegar no controller.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // só login e registro são públicos; /api/auth/me exige token (sem ele → 401)
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registrar",
                        "/api/auth/esqueci-senha", "/api/auth/redefinir-senha").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/fotos/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .anyRequest().authenticated()
            )
            // token ausente/expirado/inválido → 401 (o padrão do Spring sem formLogin seria 403),
            // para o frontend saber que precisa voltar ao login.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
