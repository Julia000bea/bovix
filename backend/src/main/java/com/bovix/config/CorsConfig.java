package com.bovix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Fonte de configuração de CORS usada diretamente pelo Spring Security (via SecurityConfig).
 * Precisa ser um CorsConfigurationSource (não só um WebMvcConfigurer) para que o Security
 * também libere corretamente os preflights (requisições OPTIONS) do navegador — sem isso,
 * toda chamada com JSON ou com o header Authorization é bloqueada antes mesmo de chegar no controller.
 */
@Configuration
public class CorsConfig {

    @Value("${bovix.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
