package com.example.demo.entity;

import com.example.demo.enums.AuthProvider;
import com.example.demo.enums.RoleType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps the `users` table.
 *
 * Owned by Module 2 (Authentication), but defined here because Module 6 (Cart)
 * needs it: a cart belongs to a user, and pricing depends on users.is_cardholder.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "membership_no", nullable = false, unique = true, length = 20)
    private String membershipNo;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * NULL for a Google-only account. Anything that compares a password must
     * null-check this first — see AuthServiceImpl.login().
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** LOCAL | GOOGLE | BOTH — which sign-in routes this account accepts. */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 10)
    private AuthProvider authProvider;

    /**
     * Google's "sub" claim: an immutable id for the Google account.
     * Matched on BEFORE email, because a Google user can change their email
     * address but never their sub.
     */
    @Column(name = "google_sub", length = 64, unique = true)
    private String googleSub;

    /** Google profile picture URL, shown in the navbar. */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "reset_password_token", length = 255)
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiry")
    private LocalDateTime resetPasswordTokenExpiry;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "education", length = 100)
    private String education;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "annual_income", precision = 12, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RoleType role;

    /** Drives which price the cart charges: cardholder_price vs mrp_price. */
    @Column(name = "is_cardholder", nullable = false)
    private Boolean isCardholder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(Integer userId, String membershipNo, String firstName, String lastName, String email,
                String passwordHash, AuthProvider authProvider, String googleSub, String profileImageUrl,
                String resetPasswordToken, LocalDateTime resetPasswordTokenExpiry,
                String phone, LocalDate dob, String gender, String education, String occupation,
                BigDecimal annualIncome, Boolean marketingConsent, RoleType role, Boolean isCardholder,
                Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.membershipNo = membershipNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authProvider = authProvider;
        this.googleSub = googleSub;
        this.profileImageUrl = profileImageUrl;
        this.resetPasswordToken = resetPasswordToken;
        this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
        this.phone = phone;
        this.dob = dob;
        this.gender = gender;
        this.education = education;
        this.occupation = occupation;
        this.annualIncome = annualIncome;
        this.marketingConsent = marketingConsent;
        this.role = role;
        this.isCardholder = isCardholder;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getMembershipNo() {
        return membershipNo;
    }

    public void setMembershipNo(String membershipNo) {
        this.membershipNo = membershipNo;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiry() {
        return resetPasswordTokenExpiry;
    }

    public void setResetPasswordTokenExpiry(LocalDateTime resetPasswordTokenExpiry) {
        this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }

    public Boolean getMarketingConsent() {
        return marketingConsent;
    }

    public void setMarketingConsent(Boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public Boolean getIsCardholder() {
        return isCardholder;
    }

    public void setIsCardholder(Boolean isCardholder) {
        this.isCardholder = isCardholder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer userId;
        private String membershipNo;
        private String firstName;
        private String lastName;
        private String email;
        private String passwordHash;
        private AuthProvider authProvider = AuthProvider.LOCAL;
        private String googleSub;
        private String profileImageUrl;
        private String resetPasswordToken;
        private LocalDateTime resetPasswordTokenExpiry;
        private String phone;
        private LocalDate dob;
        private String gender;
        private String education;
        private String occupation;
        private BigDecimal annualIncome;
        private Boolean marketingConsent;
        private RoleType role;
        private Boolean isCardholder;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder membershipNo(String membershipNo) {
            this.membershipNo = membershipNo;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder authProvider(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder googleSub(String googleSub) {
            this.googleSub = googleSub;
            return this;
        }

        public Builder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        public Builder resetPasswordToken(String resetPasswordToken) {
            this.resetPasswordToken = resetPasswordToken;
            return this;
        }

        public Builder resetPasswordTokenExpiry(LocalDateTime resetPasswordTokenExpiry) {
            this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder dob(LocalDate dob) {
            this.dob = dob;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder education(String education) {
            this.education = education;
            return this;
        }

        public Builder occupation(String occupation) {
            this.occupation = occupation;
            return this;
        }

        public Builder annualIncome(BigDecimal annualIncome) {
            this.annualIncome = annualIncome;
            return this;
        }

        public Builder marketingConsent(Boolean marketingConsent) {
            this.marketingConsent = marketingConsent;
            return this;
        }

        public Builder role(RoleType role) {
            this.role = role;
            return this;
        }

        public Builder isCardholder(Boolean isCardholder) {
            this.isCardholder = isCardholder;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            return new User(userId, membershipNo, firstName, lastName, email, passwordHash,
                    authProvider, googleSub, profileImageUrl,
                    resetPasswordToken, resetPasswordTokenExpiry, phone, dob, gender, education,
                    occupation, annualIncome, marketingConsent, role, isCardholder, isActive,
                    createdAt, updatedAt);
        }
    }
}
