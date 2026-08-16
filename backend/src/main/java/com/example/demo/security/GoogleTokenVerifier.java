package com.example.demo.security;

import com.example.demo.exception.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Verifies a Google ID token and extracts the identity from it.
 *
 * WHAT A GOOGLE ID TOKEN IS
 * -------------------------
 * It is a JWT, exactly like the one this project issues itself — three parts,
 * signed. The differences that matter:
 *
 *   ours          signed with a SHARED SECRET (HS256), verified with that same secret
 *   Google's      signed with Google's PRIVATE key (RS256), verified with their
 *                 PUBLIC key, which anybody can download
 *
 * That asymmetry is the whole point. We can prove Google issued a token without
 * Google having to share a secret with us, and without us calling Google on
 * every login. The public keys live at a well-known URL (the JWKS endpoint) and
 * Google rotates them periodically.
 *
 * WHAT WE CHECK, AND WHY EACH ONE MATTERS
 * ---------------------------------------
 *   1. signature  — proves Google minted it and nobody edited it
 *   2. expiry     — a stolen token is only useful for about an hour
 *   3. issuer     — proves it came from Google's identity service
 *   4. audience   — proves it was minted FOR OUR APP
 *
 * Check 4 is the one people forget, and skipping it is a full account-takeover
 * hole. Any Google token verifies against Google's keys, including one issued
 * to some completely unrelated app. Without the audience check, the owner of
 * any other Google-integrated site could take a token their own users handed
 * them and replay it here to log in as those users. `aud` is what binds a token
 * to this specific client id.
 *
 * We do NOT hold the client secret. Verification only needs the client id, so
 * the secret never has to exist on this server at all.
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    /** Google's public signing keys. Nimbus fetches and caches these for us. */
    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    /**
     * Google documents BOTH spellings as valid for the issuer claim, so
     * accepting only one would reject perfectly good tokens.
     */
    private static final Set<String> VALID_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder decoder;

    /**
     * The default of "" is deliberate. Without it, a missing or renamed property
     * makes the whole application fail to start with a placeholder-resolution
     * error — taking the catalogue, cart and orders down over an OPTIONAL
     * sign-in feature. With it, the app starts, this logs a clear warning, and
     * only Google sign-in is unavailable.
     */
    public GoogleTokenVerifier(@Value("${google.oauth.client-id:}") String clientId) {

        if (clientId == null || clientId.isBlank()) {
            log.warn("google.oauth.client-id is not set - Google sign-in will reject every token. "
                    + "Set it in application.properties or as the GOOGLE_CLIENT_ID environment variable.");
        }

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();

        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),          // exp / nbf
                new IssuerValidator(),                // iss
                new AudienceValidator(clientId)));    // aud  <- the critical one

        this.decoder = nimbus;
    }

    /**
     * Verifies the token and returns the claims we care about.
     *
     * @throws InvalidTokenException if anything at all is wrong. The message is
     *         deliberately vague to the caller: telling a client exactly which
     *         check failed helps someone probing the endpoint.
     */
    public GoogleUser verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidTokenException("Google sign-in failed. Please try again.");
        }

        final Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException ex) {
            // Logged in full for us, generic for them.
            log.warn("Rejected Google ID token: {}", ex.getMessage());
            throw new InvalidTokenException("Google sign-in failed. Please try again.");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        // Google should never issue a token without these, but a missing sub or
        // email would silently create a broken account, so fail loudly instead.
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            log.warn("Google ID token had no sub or no email claim");
            throw new InvalidTokenException("Google did not return an email address for this account.");
        }

        return new GoogleUser(
                subject,
                email.trim().toLowerCase(),
                Boolean.TRUE.equals(emailVerified),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture"));
    }

    /**
     * The verified identity, as Google asserts it.
     *
     * A record because it is immutable data with no identity of its own, and
     * because it must be obvious at every call site that these values came from
     * a VERIFIED token, not from the request body.
     */
    public record GoogleUser(String subject,
                             String email,
                             boolean emailVerified,
                             String givenName,
                             String familyName,
                             String fullName,
                             String pictureUrl) {

        /** Best-effort first name; Google omits given_name for some accounts. */
        public String firstNameOrFallback() {
            if (givenName != null && !givenName.isBlank()) return givenName.trim();
            if (fullName != null && !fullName.isBlank()) return fullName.trim().split("\\s+")[0];
            return email.split("@")[0];      // last resort, always non-empty
        }

        public String lastNameOrNull() {
            if (familyName != null && !familyName.isBlank()) return familyName.trim();
            if (fullName != null && fullName.trim().contains(" ")) {
                return fullName.trim().substring(fullName.trim().indexOf(' ') + 1);
            }
            return null;
        }
    }

    // ------------------------------------------------------------------

    private static final class IssuerValidator implements OAuth2TokenValidator<Jwt> {
        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            String issuer = token.getIssuer() == null ? null : token.getIssuer().toString();
            if (issuer != null && VALID_ISSUERS.contains(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", "Unexpected issuer: " + issuer, null));
        }
    }

    /**
     * Confirms the token was minted for OUR client id.
     *
     * Without this, a token issued to any other Google app would pass every
     * other check and log its bearer straight in.
     */
    private static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

        private final String clientId;

        private AudienceValidator(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            List<String> audience = token.getAudience();
            if (clientId != null && !clientId.isBlank()
                    && audience != null && audience.contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_audience",
                            "Token was not issued for this application", null));
        }
    }
}
