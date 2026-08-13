package com.example.demo.dto.request;

import com.example.demo.validation.annotation.ValidPassword;
import com.example.demo.validation.annotation.ValidPhone;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Body for POST /api/auth/register — the BRD registration form. */
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @ValidPassword
    private String password;

    @ValidPhone
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @Size(max = 10)
    private String gender;

    @Size(max = 100)
    private String education;

    @Size(max = 100)
    private String occupation;

    @PositiveOrZero(message = "Annual income cannot be negative")
    private BigDecimal annualIncome;

    /** BRD: consent to receive promotion and discount emails. */
    private Boolean marketingConsent = false;

    public RegisterRequest() {
    }

    public RegisterRequest(String firstName, String lastName, String email, String password, String phone, LocalDate dob, String gender, String education, String occupation, BigDecimal annualIncome, Boolean marketingConsent) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.dob = dob;
        this.gender = gender;
        this.education = education;
        this.occupation = occupation;
        this.annualIncome = annualIncome;
        this.marketingConsent = marketingConsent;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private String phone;
        private LocalDate dob;
        private String gender;
        private String education;
        private String occupation;
        private BigDecimal annualIncome;
        private Boolean marketingConsent = false;

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

        public Builder password(String password) {
            this.password = password;
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

        public RegisterRequest build() {
            return new RegisterRequest(firstName, lastName, email, password, phone, dob, gender, education, occupation, annualIncome, marketingConsent);
        }

    }
}
