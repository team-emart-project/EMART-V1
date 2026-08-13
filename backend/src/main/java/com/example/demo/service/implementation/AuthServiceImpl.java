package com.example.demo.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.enums.AuthProvider;
import com.example.demo.enums.RoleType;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleTokenVerifier;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.interfaces.AuthService;
import com.example.demo.util.EmailUtil;
import com.example.demo.util.MembershipNumberGeneratorUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, MembershipNumberGeneratorUtil membershipNumberGenerator, EmailUtil emailUtil, UserMapper userMapper, GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.membershipNumberGenerator = membershipNumberGenerator;
        this.emailUtil = emailUtil;
        this.userMapper = userMapper;
        this.googleTokenVerifier = googleTokenVerifier;
    }


    /** Reset links are short-lived: a leaked token stays useful for 30 minutes. */
    private static final int RESET_TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MembershipNumberGeneratorUtil membershipNumberGenerator;
    private final EmailUtil emailUtil;
    private final UserMapper userMapper;
    private final GoogleTokenVerifier googleTokenVerifier;

    /**
     * Whether a Google login may attach itself to an existing password account
     * that shares the same email. Safe only because we additionally require
     * Google's email_verified claim — see loginWithGoogle().
     */
    @Value("${google.oauth.link-verified-accounts:true}")
    private boolean linkVerifiedAccounts;

    // ------------------------------------------------------------------

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = normaliseEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .membershipNo(membershipNumberGenerator.generate())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() == null ? null : request.getLastName().trim())
                .email(email)
                // NEVER store the raw password. BCrypt is slow by design and
                // salts every hash, so identical passwords store differently.
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .dob(request.getDob())
                .gender(request.getGender())
                .education(request.getEducation())
                .occupation(request.getOccupation())
                .annualIncome(request.getAnnualIncome())
                .marketingConsent(Boolean.TRUE.equals(request.getMarketingConsent()))
                .role(RoleType.CUSTOMER)
                .isCardholder(false)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);

        emailUtil.sendMembershipNumber(saved.getEmail(), saved.getFirstName(), saved.getMembershipNo());
        log.info("Registered userId={} membershipNo={}", saved.getUserId(), saved.getMembershipNo());

        return userMapper.toResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(normaliseEmail(request.getEmail()))
                // Same message for "no such email" and "wrong password" on purpose:
                // a different message would let an attacker enumerate valid emails.
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // A Google-only account has NO password hash. Passing null into
        // matches() would throw, so this branch is required, not defensive
        // padding. The message stays generic for the same anti-enumeration
        // reason as above — we do not confirm that this email exists, nor how
        // it signs in.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("This account has been deactivated");
        }

        return issueToken(user, "password");
    }

    // ------------------------------------------------------------------
    // Google sign-in (OAuth 2.0 / OpenID Connect)
    // ------------------------------------------------------------------

    /**
     * Three cases, resolved in this order:
     *
     *   1. We already know this Google account  -> log straight in
     *   2. The email matches an existing account -> LINK, if Google verified it
     *   3. Nobody matches                        -> register a new user
     *
     * Matching on google_sub BEFORE email is deliberate. `sub` is Google's
     * permanent id for the account; the email address on a Google account can
     * be changed. Leading with email would break the link the moment a user
     * renamed their Google address, and would silently create a duplicate
     * account.
     */
    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {

        // Everything below comes from a token whose signature, expiry, issuer
        // and audience have all been checked. Nothing here is caller-supplied.
        GoogleTokenVerifier.GoogleUser google = googleTokenVerifier.verify(request.getCredential());

        // --- case 1: known Google account -------------------------------
        Optional<User> bySub = userRepository.findByGoogleSub(google.subject());
        if (bySub.isPresent()) {
            User user = bySub.get();
            requireActive(user);
            refreshProfileFromGoogle(user, google);
            return issueToken(userRepository.save(user), "google");
        }

        // --- case 2: existing account with the same email ---------------
        Optional<User> byEmail = userRepository.findByEmail(google.email());
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            requireActive(user);

            // THE security gate for account linking.
            //
            // Without email_verified, anyone could create a Google account
            // claiming somebody else's address and take over their e-MART
            // account. Google only sets this claim once it has proved the user
            // controls that inbox, which is exactly the proof we need.
            if (!google.emailVerified()) {
                log.warn("Refused to link Google account to userId={} - email not verified by Google",
                        user.getUserId());
                throw new BusinessRuleViolationException(
                        "Google has not verified this email address, so we cannot link it to your "
                                + "existing e-MART account. Please sign in with your password.");
            }

            if (!linkVerifiedAccounts) {
                throw new BusinessRuleViolationException(
                        "This email is already registered with a password. Please sign in with your password.");
            }

            user.setGoogleSub(google.subject());
            user.setAuthProvider(user.getAuthProvider().withGoogleLinked());
            refreshProfileFromGoogle(user, google);

            log.info("Linked Google account to existing userId={} (provider now {})",
                    user.getUserId(), user.getAuthProvider());
            return issueToken(userRepository.save(user), "google-linked");
        }

        // --- case 3: brand new user -------------------------------------
        User user = User.builder()
                .membershipNo(membershipNumberGenerator.generate())
                .firstName(google.firstNameOrFallback())
                .lastName(google.lastNameOrNull())
                .email(google.email())
                // No password at all, rather than a random or empty one. A fake
                // hash would look like a usable credential to any future code
                // that reads this column.
                .passwordHash(null)
                .authProvider(AuthProvider.GOOGLE)
                .googleSub(google.subject())
                .profileImageUrl(google.pictureUrl())
                // Google gives us no phone, dob or income, and the BRD makes
                // them optional, so the user fills them in later on the profile
                // page. Consent defaults to false: we were never asked for it.
                .marketingConsent(false)
                .role(RoleType.CUSTOMER)
                .isCardholder(false)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);

        emailUtil.sendMembershipNumber(saved.getEmail(), saved.getFirstName(), saved.getMembershipNo());
        log.info("Registered userId={} via Google, membershipNo={}",
                saved.getUserId(), saved.getMembershipNo());

        return issueToken(saved, "google-new");
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        Optional<User> maybeUser = userRepository.findByEmail(normaliseEmail(request.getEmail()));

        // If the email is unknown we do nothing and STILL return success.
        // Reporting "no such user" here would turn this endpoint into a way to
        // discover which emails are registered.
        if (maybeUser.isEmpty()) {
            log.info("Password reset requested for an unknown email - ignoring silently");
            return;
        }

        User user = maybeUser.get();
        String token = UUID.randomUUID().toString();

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
        userRepository.save(user);

        emailUtil.sendPasswordResetToken(user.getEmail(), token);
        log.info("Password reset token issued for userId={}", user.getUserId());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or already used reset token"));

        if (user.getResetPasswordTokenExpiry() == null
                || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("This reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // A Google-only account now HAS a password, so it becomes BOTH.
        //
        // This is not cosmetic: the users table has a CHECK constraint saying a
        // GOOGLE account must have a NULL password_hash. Without this line the
        // save below would fail with a raw constraint violation.
        //
        // Allowing it at all is safe because the reset link was emailed to the
        // address Google itself verified, so whoever clicked it controls that
        // inbox. It also gives Google users a way to stop depending on Google.
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            user.setAuthProvider(AuthProvider.BOTH);
            log.info("userId={} added a password to a Google-only account", user.getUserId());
        }

        // Clear the token so the same link cannot be replayed.
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset completed for userId={}", user.getUserId());
    }

    // ------------------------------------------------------------------

    /** Emails are case-insensitive; storing them lowercased keeps uniqueness honest. */
    private String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /**
     * Builds the response every login path returns.
     *
     * One method so a password login and a Google login are indistinguishable
     * downstream: same token, same shape, same expiry. Nothing after this point
     * needs to know how the user signed in.
     */
    private AuthResponse issueToken(User user, String via) {
        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail());
        log.info("Login success for userId={} via {}", user.getUserId(), via);

        return AuthResponse.builder()
                .accessToken(token)
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .user(userMapper.toResponse(user))
                .build();
    }

    private void requireActive(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("This account has been deactivated");
        }
    }

    /**
     * Refreshes the picture, and fills in a name only if we do not already have
     * one.
     *
     * The name is NOT overwritten on every login on purpose: a user who edited
     * their name on the profile page would otherwise have it silently reverted
     * to their Google name each time they signed in.
     */
    private void refreshProfileFromGoogle(User user, GoogleTokenVerifier.GoogleUser google) {
        if (google.pictureUrl() != null && !google.pictureUrl().isBlank()) {
            user.setProfileImageUrl(google.pictureUrl());
        }
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            user.setFirstName(google.firstNameOrFallback());
        }
    }
}
