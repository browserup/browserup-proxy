package com.browserup.bup.rest.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.browserup.bup.rest.validation.util.MessageSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { LongPositiveConstraint.LongPositiveValidator.class })
public @interface LongPositiveConstraint {

  String message() default "";

  String paramName() default "";

  Class<?>[] groups() default {};

  int value();

  Class<? extends Payload>[] payload() default {};

  class LongPositiveValidator implements ConstraintValidator<LongPositiveConstraint, String> {
    private static final Logger LOG = LoggerFactory.getLogger(LongPositiveValidator.class);

    @Override
    public void initialize(LongPositiveConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      long longValue = 0;
      boolean failed = false;
      String errorMessage = "";
      try {
        longValue = Long.parseLong(value);
      } catch (NumberFormatException ex) {
        failed = true;
        String escapedValue = MessageSanitizer.escape(value);
        errorMessage = String.format("Invalid integer value: '%s'", escapedValue);
      }

      if (!failed && longValue < 0) {
        failed = true;
        String escapedValue = MessageSanitizer.escape(value);
        errorMessage = String.format("Expected positive integer value, got: '%s'", escapedValue);
      }

      if (!failed) {
        return true;
      }

      LOG.warn(errorMessage);
      context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();

      return false;
    }
  }
}
