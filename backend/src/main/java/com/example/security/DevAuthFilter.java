package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================ TEMPORARY — DELETE ME ============================
 * Module 2 (Authentication) is not built yet, so there is no JWT to identify the
 * caller. This filter lets you test the cart endpoints TODAY by sending a header:
 *
 *     X-User-Id: 1
 *
 * It is annotated @Profile("dev") so it can never activate in test or prod.
 * When Module 2 lands, delete this class and register JwtAuthFilter instead —
 * nothing else in Module 6 has to change, because everything reads the user
 * through SecurityUtils, not through this filter.
 * ==============================================================================
 */
@Component
@Profile("dev")
public class DevAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DevAuthFilter.class);
    private static final String DEV_USER_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userIdHeader = request.getHeader(DEV_USER_HEADER);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Integer userId = Integer.valueOf(userIdHeader.trim());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("DEV auth: request authenticated as userId={}", userId);

            } catch (NumberFormatException ex) {
                log.warn("DEV auth: header {} is not a number: {}", DEV_USER_HEADER, userIdHeader);
            }
        }

        filterChain.doFilter(request, response);
    }
}
