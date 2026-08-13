package com.example.demo.validation.validator;

import com.example.demo.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** At least 8 chars, one upper, one lower, one digit. */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        if (password == null || password.isBlank()) {
            return fail(context, "Password is required");
        }
        if (password.length() < MIN_LENGTH) {
            return fail(context, "Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            return fail(context, "Password must contain at least one uppercase letter");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            return fail(context, "Password must contain at least one lowercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            return fail(context, "Password must contain at least one digit");
        }
        return true;
    }

    /** Replaces the default message with a specific reason. */
    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
