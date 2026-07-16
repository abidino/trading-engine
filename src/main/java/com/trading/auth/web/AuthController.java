package com.trading.auth.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.security.username}")
    private String validUsername;

    @Value("${app.security.password}")
    private String validPassword;

    @Value("${app.recaptcha.secret-key:}")
    private String recaptchaSecretKey;

    @Value("${app.recaptcha.enabled:false}")
    private boolean recaptchaEnabled;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Validate reCAPTCHA if enabled
        if (recaptchaEnabled && recaptchaSecretKey != null && !recaptchaSecretKey.isEmpty()) {
            if (request.recaptchaToken() == null || request.recaptchaToken().isEmpty()) {
                log.warn("Login attempt without reCAPTCHA token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "reCAPTCHA token is required"));
            }

            if (!verifyRecaptcha(request.recaptchaToken())) {
                log.warn("Login attempt with invalid reCAPTCHA token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "reCAPTCHA verification failed"));
            }
        }

        // Validate credentials
        if (!validUsername.equals(request.username()) || !validPassword.equals(request.password())) {
            log.warn("Login attempt with invalid credentials for user: {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        log.info("Successful login for user: {}", request.username());
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "username", request.username()
        ));
    }

    private boolean verifyRecaptcha(String token) {
        try {
            String url = String.format(
                    "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s",
                    recaptchaSecretKey, token
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Double score = (Double) response.get("score");
                // reCAPTCHA v3 score: 0.0 (likely bot) to 1.0 (likely human)
                // Threshold: 0.5 is recommended by Google
                return score != null && score >= 0.5;
            }
            return false;
        } catch (Exception e) {
            log.error("reCAPTCHA verification error", e);
            return false;
        }
    }

    record LoginRequest(String username, String password, String recaptchaToken) {}
}
