package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The user, as the API exposes them.
 *
 * Critically this has NO passwordHash and NO resetPasswordToken. That is the
 * whole reason we never return the User entity directly from a controller.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Integer userId;
    private String membershipNo;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String education;
    private String occupation;
    private BigDecimal annualIncome;
    private Boolean marketingConsent;
    private String role;
    private Boolean cardholder;
    private Boolean active;

    /** LOCAL | GOOGLE | BOTH. The UI hides "change password" for GOOGLE. */
    private String authProvider;

    /** Google profile picture, shown in the navbar. Null for password accounts. */
    private String profileImageUrl;

    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(Integer userId, String membershipNo, String firstName, String lastName, String email, String phone, LocalDate dob, String gender, String education, String occupation, BigDecimal annualIncome, Boolean marketingConsent, String role, Boolean cardholder, Boolean active, String authProvider, String profileImageUrl, LocalDateTime createdAt) {
        this.userId = userId;
        this.membershipNo = membershipNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dob = dob;
        this.gender = gender;
        this.education = education;
        this.occupation = occupation;
        this.annualIncome = annualIncome;
        this.marketingConsent = marketingConsent;
        this.role = role;
        this.cardholder = cardholder;
        this.active = active;
        this.authProvider = authProvider;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getCardholder() {
        return cardholder;
    }

    public void setCardholder(Boolean cardholder) {
        this.cardholder = cardholder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
        private String phone;
        private LocalDate dob;
        private String gender;
        private String education;
        private String occupation;
        private BigDecimal annualIncome;
        private Boolean marketingConsent;
        private String role;
        private Boolean cardholder;
        private Boolean active;
        private String authProvider;
        private String profileImageUrl;
        private LocalDateTime createdAt;

        public Builder authProvider(String authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

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

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder cardholder(Boolean cardholder) {
            this.cardholder = cardholder;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(userId, membershipNo, firstName, lastName, email, phone, dob, gender, education, occupation, annualIncome, marketingConsent, role, cardholder, active, authProvider, profileImageUrl, createdAt);
        }

    }
}
