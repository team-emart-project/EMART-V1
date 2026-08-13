package com.example.demo.validation.annotation;

import com.example.demo.validation.validator.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom Bean Validation annotation.
 *
 * A single regex could almost do this, but a validator class can return a
 * SPECIFIC message ("must contain a digit") instead of one generic failure,
 * which is far more useful on a registration form.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password does not meet the security requirements";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
