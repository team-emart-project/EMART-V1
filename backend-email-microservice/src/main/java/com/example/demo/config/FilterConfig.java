package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link ApiKeyAuthFilter} against /api/* only.
 *
 * Declaring the filter with a plain @Bean would apply it to EVERY path,
 * including /actuator/health — a health check that needs a secret is not much
 * of a health check.
 */
@Configuration
public class FilterConfig {

    private static final Logger log = LoggerFactory.getLogger(FilterConfig.class);

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyFilterRegistration(EmailProperties properties) {

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("emart.email.api-key is blank - /api/** is OPEN to anyone who can reach this port. "
                    + "Set it here and in both backends before this leaves localhost.");
        }

        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiKeyAuthFilter(properties.getApiKey()));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("apiKeyAuthFilter");
        return registration;
    }
}
