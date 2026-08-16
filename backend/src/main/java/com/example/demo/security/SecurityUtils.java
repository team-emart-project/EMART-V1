package com.example.demo.security;

import com.example.demo.exception.UnauthorizedActionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for "who is calling right now".
 *
 * Every module resolves the user through this instead of accepting a userId in
 * the request — otherwise anyone could act on anyone else's cart, addresses or
 * orders just by changing a number in Postman.
 */
@Component
public class SecurityUtils {

    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new UnauthorizedActionException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        // "anonymousUser" and anything else we do not recognise
        throw new UnauthorizedActionException("Authenticated principal is not a valid application user");
    }

    /**
     * Like getCurrentUserId() but returns null instead of throwing.
     *
     * Needed by the PUBLIC catalog endpoints: they must work for a signed-out
     * visitor, yet still tailor pricing when somebody IS signed in.
     */
    public Integer getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return (principal instanceof CustomUserDetails ud) ? ud.getUserId() : null;
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails ud) {
            return ud.getEmail();
        }
        throw new UnauthorizedActionException("No authenticated user found");
    }
}
