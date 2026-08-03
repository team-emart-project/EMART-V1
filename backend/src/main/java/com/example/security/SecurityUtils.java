package com.example.security;

import com.example.exception.UnauthorizedActionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for "who is calling right now".
 *
 * Every cart operation resolves the user through this class instead of
 * accepting a userId from the request body — otherwise anyone could edit
 * anyone else's cart just by changing a number in Postman.
 *
 * Module 2 (Authentication) will populate the SecurityContext from a JWT.
 * Until then, DevAuthFilter populates it from a header under the dev profile.
 */
@Component
public class SecurityUtils {

    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new UnauthorizedActionException("No authenticated user found in the security context");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Integer userId) {
            return userId;
        }

        try {
            return Integer.valueOf(principal.toString());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedActionException("Authenticated principal is not a valid user id");
        }
    }
}
