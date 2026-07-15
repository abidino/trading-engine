package com.trading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig {

    /**
     * CORS for the separately-deployed UI (different origin/host). Origins are
     * configurable via {@code app.cors.allowed-origins} (env
     * {@code APP_CORS_ALLOWED_ORIGINS}, comma-separated). Uses
     * {@code allowedOriginPatterns} + {@code allowCredentials} so the browser UI
     * can send HTTP Basic credentials. Consumed by Spring Security's {@code cors()}.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:*}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(
                Arrays.stream(origins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        source.registerCorsConfiguration("/actuator/**", cfg);
        return source;
    }
}
