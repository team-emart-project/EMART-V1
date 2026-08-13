package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * The only gate on this service: a shared secret in the X-API-Key header.
 *
 * WHY NOT JWT — the callers are the two backends, not browsers. There is no
 * user to authenticate here and no session to carry; the question is only
 * "is this our own server calling?". A shared secret answers that, and it is
 * the one mechanism that is equally trivial to send from Spring's RestClient
 * and from .NET's HttpClient.
 *
 * Registered in {@link FilterConfig} against /api/* only, so /actuator/health
 * stays open for container health checks.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    public static final String API_KEY_HEADER = "X-API-Key";

    private final String expectedApiKey;

    public ApiKeyAuthFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey == null ? "" : expectedApiKey.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Blank key = check disabled. AppStartupLogger warns about this at boot.
        if (expectedApiKey.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String presented = request.getHeader(API_KEY_HEADER);

        // Constant-time comparison. Overkill for a college project, but a
        // plain equals() on a secret is the kind of habit worth not forming.
        if (presented != null && java.security.MessageDigest.isEqual(
                presented.trim().getBytes(StandardCharsets.UTF_8),
                expectedApiKey.getBytes(StandardCharsets.UTF_8))) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rejected {} {} - {} header missing or wrong (caller: {})",
                request.getMethod(), request.getRequestURI(), API_KEY_HEADER,
                request.getRemoteAddr());

        writeUnauthorized(request, response);
    }

    /**
     * Hand-written because this runs BEFORE the DispatcherServlet, so
     * @RestControllerAdvice never sees it. The shape still matches
     * ErrorResponse so a caller only has to parse one error format.
     */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        String body = """
                {"success":false,"status":401,"error":"Unauthorized",\
                "message":"A valid %s header is required","path":"%s","timestamp":"%s"}"""
                .formatted(API_KEY_HEADER, request.getRequestURI(), LocalDateTime.now());

        response.getWriter().write(body);
    }
}
