package com.example.demo.validation.validator;

import com.example.demo.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Deliberately permissive — 10 to 15 digits with an optional leading '+',
 * spaces and dashes ignored. Phone formats vary worldwide and over-strict
 * validation locks out legitimate users.
 */
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final Pattern PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        // null / blank is allowed: phone is optional. Use @NotBlank to require it.
        if (phone == null || phone.isBlank()) {
            return true;
        }
        return PATTERN.matcher(phone.replaceAll("[\\s-]", "")).matches();
    }
}
