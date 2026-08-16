package com.example.demo.validation.annotation;

import com.example.demo.validation.validator.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Phone number must be 10 to 15 digits, optionally starting with +";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
