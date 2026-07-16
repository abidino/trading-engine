package com.trading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic security for the public API. Every endpoint requires authentication
 * <em>except</em> the unsecured health probes ({@code /api/v1/health} and Actuator's
 * {@code /actuator/health}), so a separately-deployed UI or an uptime monitor can
 * check liveness without credentials.
 *
 * <p>Credentials come from configuration (env vars in prod):
 * {@code app.security.username} / {@code app.security.password}. The API is stateless
 * (no server session) and CSRF is disabled, which is appropriate for a token/basic
 * REST API consumed by a browser UI on a different origin. CORS is delegated to the
 * {@link org.springframework.web.cors.CorsConfigurationSource} bean in {@link WebConfig}.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/health",
            "/api/v1/auth/login",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS pre-flight must never require credentials.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${app.security.username}") String username,
            @Value("${app.security.password}") String password) {
        UserDetails user = User.withUsername(username)
                .password(encoder.encode(password))
                .roles("API")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
