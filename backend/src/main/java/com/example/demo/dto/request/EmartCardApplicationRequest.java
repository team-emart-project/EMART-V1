package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
/**
 * Body for POST /api/emart-card/apply.
 *
 * The BRD says the card application collects more than plain registration:
 * employment, bank account and PAN.
 */
public class EmartCardApplicationRequest {

    @NotBlank(message = "Employment details are required")
    @Size(max = 255)
    private String employmentDetails;

    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Bank account number must be 9 to 18 digits")
    private String bankAccountNo;

    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$",
             message = "PAN must be in the format ABCDE1234F")
    private String panNumber;

    public EmartCardApplicationRequest() {
    }

    public EmartCardApplicationRequest(String employmentDetails, String bankAccountNo, String panNumber) {
        this.employmentDetails = employmentDetails;
        this.bankAccountNo = bankAccountNo;
        this.panNumber = panNumber;
    }

    public String getEmploymentDetails() {
        return employmentDetails;
    }

    public void setEmploymentDetails(String employmentDetails) {
        this.employmentDetails = employmentDetails;
    }

    public String getBankAccountNo() {
        return bankAccountNo;
    }

    public void setBankAccountNo(String bankAccountNo) {
        this.bankAccountNo = bankAccountNo;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String employmentDetails;
        private String bankAccountNo;
        private String panNumber;

        public Builder employmentDetails(String employmentDetails) {
            this.employmentDetails = employmentDetails;
            return this;
        }

        public Builder bankAccountNo(String bankAccountNo) {
            this.bankAccountNo = bankAccountNo;
            return this;
        }

        public Builder panNumber(String panNumber) {
            this.panNumber = panNumber;
            return this;
        }

        public EmartCardApplicationRequest build() {
            return new EmartCardApplicationRequest(employmentDetails, bankAccountNo, panNumber);
        }

    }
}
