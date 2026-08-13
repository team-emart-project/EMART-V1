package com.example.demo.config;

import com.example.demo.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

/**
 * Security rules for the API.
 *
 * STATELESS: no HTTP session, no server-side login state. Identity travels in
 * the Authorization header on every request. CSRF is disabled for the same
 * reason — CSRF protects cookie-based sessions, which we do not use.
 *
 * The constructor is written BY HAND rather than with Lombok. This is the very
 * first bean Spring creates, so if annotation processing ever fails, the whole
 * application dies here with a confusing error.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Module 2 — register / login / forgot / reset are public
                    .requestMatchers("/api/auth/**").permitAll()

                    // Module 1 + Module 5 — anyone may browse without an account.
                    // The BRD says login is only needed in order to BUY.
                    .requestMatchers(HttpMethod.GET,
                            "/api/home/**", "/api/categories/**", "/api/products/**").permitAll()

                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                    // Modules 3, 4, 6 (profile, card, cart) and everything else
                    .anyRequest().authenticated()
            )
            // Return clean JSON instead of Spring's default HTML error page.
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                    "Unauthorized",
                                    "Authentication required. Send a valid Bearer token.",
                                    request.getRequestURI()))
                    .accessDeniedHandler((request, response, deniedException) ->
                            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                    "Forbidden",
                                    "You do not have permission to perform this action.",
                                    request.getRequestURI()))
            )
            // Our filter runs BEFORE the username/password filter so the request
            // is already authenticated by the time authorization is evaluated.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Writes the error JSON by hand.
     *
     * Deliberately NOT using ObjectMapper here: Spring Boot 4 ships Jackson 3
     * (package tools.jackson), so importing the Jackson 2 ObjectMapper would be
     * a compile risk for a three-field payload. Controllers still serialise
     * normally through Spring — this is only the security-filter path, which
     * runs before Spring MVC's message converters exist.
     */
    private void writeError(HttpServletResponse response, int status, String error,
                            String message, String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"status":%d,"error":"%s","message":"%s","path":"%s","timestamp":"%s"}"""
                .formatted(status, escape(error), escape(message), escape(path), LocalDateTime.now()));
    }

    /** Minimal JSON string escaping so a quote in a path cannot break the payload. */
    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * BCrypt: deliberately slow, and salts every hash so two identical
     * passwords store differently.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
