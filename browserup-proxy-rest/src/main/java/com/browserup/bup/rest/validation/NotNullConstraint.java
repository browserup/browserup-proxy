package com.browserup.bup.rest.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { NotNullConstraint.NotNullValidator.class })
public @interface NotNullConstraint {

  String message() default "";

  String paramName() default "";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class NotNullValidator implements ConstraintValidator<NotNullConstraint, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(NotNullValidator.class);

    @Override
    public void initialize(NotNullConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
      if (value != null) {
        return true;
      }
      String errorMessage = "Expected not null value";
      LOG.warn(errorMessage);

      context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
      return false;
    }
  }
}