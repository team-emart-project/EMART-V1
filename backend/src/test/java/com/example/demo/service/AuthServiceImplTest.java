package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entity.User;
import com.example.demo.enums.RoleType;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.implementation.AuthServiceImpl;
import com.example.demo.util.EmailUtil;
import com.example.demo.util.MembershipNumberGeneratorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private MembershipNumberGeneratorUtil membershipNumberGenerator;
    @Mock private EmailUtil emailUtil;
    @Spy  private UserMapper userMapper = new UserMapper();

    @InjectMocks private AuthServiceImpl authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .userId(1)
                .membershipNo("EMART00001")
                .firstName("Rishi")
                .email("rishi@example.com")
                .passwordHash("$2a$10$hashed")
                .role(RoleType.CUSTOMER)
                .isCardholder(false)
                .isActive(true)
                .marketingConsent(true)
                .build();
    }

    // ---------------- register ----------------

    @Test
    @DisplayName("register hashes the password and never stores it in plain text")
    void registerHashesPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(membershipNumberGenerator.generate()).thenReturn("EMART12345");
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(RegisterRequest.builder()
                .firstName("New").email("new@example.com")
                .password("Password@123").marketingConsent(true).build());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("Password@123");
        assertThat(captor.getValue().getRole()).isEqualTo(RoleType.CUSTOMER);
        assertThat(captor.getValue().getIsCardholder()).isFalse();
    }

    @Test
    @DisplayName("register lowercases the email so duplicates cannot slip through by case")
    void registerNormalisesEmail() {
        when(userRepository.existsByEmail("mixed@example.com")).thenReturn(false);
        when(membershipNumberGenerator.generate()).thenReturn("EMART12345");
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(RegisterRequest.builder()
                .firstName("A").email("  MiXeD@Example.COM  ")
                .password("Password@123").build());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("mixed@example.com");
    }

    @Test
    @DisplayName("registering an existing email is a 409 conflict")
    void duplicateEmailRejected() {
        when(userRepository.existsByEmail("rishi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(RegisterRequest.builder()
                .firstName("Rishi").email("rishi@example.com")
                .password("Password@123").build()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register emails the generated membership number")
    void registerEmailsMembershipNumber() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(membershipNumberGenerator.generate()).thenReturn("EMART99999");
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(RegisterRequest.builder()
                .firstName("A").email("a@example.com").password("Password@123").build());

        verify(emailUtil).sendMembershipNumber(eq("a@example.com"), eq("A"), eq("EMART99999"));
    }

    // ---------------- login ----------------

    @Test
    @DisplayName("valid credentials return a JWT and the user profile")
    void loginSuccess() {
        when(userRepository.findByEmail("rishi@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Password@123", "$2a$10$hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1, "rishi@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(7_200_000L);

        AuthResponse response = authService.login(
                LoginRequest.builder().email("rishi@example.com").password("Password@123").build());

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("rishi@example.com");
    }

    @Test
    @DisplayName("unknown email and wrong password give the SAME message (no user enumeration)")
    void loginFailuresAreIndistinguishable() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("rishi@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        String unknownEmailMsg = catchMessage(() -> authService.login(
                LoginRequest.builder().email("ghost@example.com").password("x").build()));
        String wrongPasswordMsg = catchMessage(() -> authService.login(
                LoginRequest.builder().email("rishi@example.com").password("wrong").build()));

        assertThat(unknownEmailMsg).isEqualTo(wrongPasswordMsg);
    }

    @Test
    @DisplayName("a deactivated account cannot log in")
    void inactiveUserCannotLogin() {
        existingUser.setIsActive(false);
        when(userRepository.findByEmail("rishi@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder().email("rishi@example.com").password("Password@123").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("deactivated");
    }

    // ---------------- forgot / reset ----------------

    @Test
    @DisplayName("forgot-password on an unknown email succeeds silently (no enumeration)")
    void forgotPasswordUnknownEmailIsSilent() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(ForgotPasswordRequest.builder()
                .email("ghost@example.com").build());   // must NOT throw

        verify(userRepository, never()).save(any());
        verify(emailUtil, never()).sendPasswordResetToken(anyString(), anyString());
    }

    @Test
    @DisplayName("forgot-password stores a token with a future expiry")
    void forgotPasswordIssuesToken() {
        when(userRepository.findByEmail("rishi@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(ForgotPasswordRequest.builder()
                .email("rishi@example.com").build());

        assertThat(existingUser.getResetPasswordToken()).isNotBlank();
        assertThat(existingUser.getResetPasswordTokenExpiry()).isAfter(LocalDateTime.now());
        verify(emailUtil).sendPasswordResetToken(eq("rishi@example.com"), anyString());
    }

    @Test
    @DisplayName("reset-password clears the token so the link cannot be replayed")
    void resetPasswordClearsToken() {
        existingUser.setResetPasswordToken("valid-token");
        existingUser.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByResetPasswordToken("valid-token")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("$2a$10$newhash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(ResetPasswordRequest.builder()
                .token("valid-token").newPassword("NewPass@123").build());

        assertThat(existingUser.getPasswordHash()).isEqualTo("$2a$10$newhash");
        assertThat(existingUser.getResetPasswordToken()).isNull();
        assertThat(existingUser.getResetPasswordTokenExpiry()).isNull();
    }

    @Test
    @DisplayName("an expired reset token is rejected")
    void expiredResetTokenRejected() {
        existingUser.setResetPasswordToken("old-token");
        existingUser.setResetPasswordTokenExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByResetPasswordToken("old-token")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordRequest.builder()
                .token("old-token").newPassword("NewPass@123").build()))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("an unknown reset token is rejected")
    void unknownResetTokenRejected() {
        when(userRepository.findByResetPasswordToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(ResetPasswordRequest.builder()
                .token("nope").newPassword("NewPass@123").build()))
                .isInstanceOf(InvalidTokenException.class);
    }

    private String catchMessage(Runnable action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }
}
