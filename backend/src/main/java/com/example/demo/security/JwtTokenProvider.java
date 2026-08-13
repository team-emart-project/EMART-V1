package com.example.demo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creates and validates JWT ACCESS TOKENS. There is no refresh token in this
 * project by design, so this is the only token type that exists.
 *
 * The token is SIGNED, not encrypted — anyone can read the payload, so it must
 * never contain a password or anything secret. The signature is what stops a
 * client forging one.
 */
@Component
public class JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);


    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration.ms}") long expirationMs) {
        // HS256 requires a key of at least 256 bits; the configured secret is
        // base64 and comfortably longer than that.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * The user id goes in the SUBJECT claim. Everything downstream
     * (SecurityUtils, cart, orders) identifies the caller from this.
     */
    public String generateToken(Integer userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Integer getUserIdFromToken(String token) {
        return Integer.valueOf(parseClaims(token).getSubject());
    }

    /**
     * Returns false rather than throwing, so JwtAuthFilter can simply leave the
     * request unauthenticated and let Spring Security return 401.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return false;
        }
    }

    /** Throws if the signature is wrong, the token is malformed, or it expired. */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
