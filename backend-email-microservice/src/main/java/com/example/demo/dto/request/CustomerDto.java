package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Who the email goes to, and how to greet them. */
public record CustomerDto(

        @NotBlank(message = "Customer name is required")
        String name,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email is not a valid address")
        String email,

        /** Printed on the invoice. Null for a non-member. */
        String membershipNo,

        /** Drives the "you earned e-Points" block. */
        Boolean cardholder

) {
    /** First word only — "Hi Rishiraj," reads better than "Hi Rishiraj Chhalotre,". */
    public String firstName() {
        if (name == null || name.isBlank()) return "there";
        return name.trim().split("\\s+")[0];
    }

    public boolean isCardholder() {
        return Boolean.TRUE.equals(cardholder);
    }

    /**
     * r****j@gmail.com — what goes in the log. A log file is a much easier
     * thing to leak than a database, so it should not be a list of every
     * customer address.
     */
    public String maskedEmail() {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "*".repeat(local.length()) + domain;
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }
}
