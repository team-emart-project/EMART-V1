package com.example.config;

import com.example.security.DevAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
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

/**
 * Security rules for the API.
 *
 * STATELESS because the API will be token-based (Module 2) — no HTTP session,
 * no server-side login state. CSRF is disabled for the same reason: it protects
 * cookie-based sessions, which we do not use.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Present only under the dev profile; empty otherwise. */
    private final ObjectProvider<DevAuthFilter> devAuthFilterProvider;

    public SecurityConfig(ObjectProvider<DevAuthFilter> devAuthFilterProvider) {
        this.devAuthFilterProvider = devAuthFilterProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Module 2 — public auth endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    // Module 1 + Module 5 — public browsing
                    .requestMatchers(HttpMethod.GET,
                            "/api/home/**", "/api/categories/**", "/api/products/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    // Module 6 (cart) and everything else requires a logged-in user
                    .anyRequest().authenticated()
            );

        // Only registered when the dev profile is active.
        DevAuthFilter devAuthFilter = devAuthFilterProvider.getIfAvailable();
        if (devAuthFilter != null) {
            http.addFilterBefore(devAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * BCrypt is the industry default for password hashing: it is deliberately
     * slow and salts every hash, so two identical passwords store differently.
     * Declared here now because Module 2 will need it.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
