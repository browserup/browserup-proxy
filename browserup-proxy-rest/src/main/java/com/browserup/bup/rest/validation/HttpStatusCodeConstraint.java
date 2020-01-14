package com.browserup.bup.rest.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { HttpStatusCodeConstraint.HttpStatusCodeValidator.class })
public @interface HttpStatusCodeConstraint {

  String message() default "";

  String paramName() default "";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class HttpStatusCodeValidator implements ConstraintValidator<HttpStatusCodeConstraint, String> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpStatusCodeValidator.class);

    @Override
    public void initialize(HttpStatusCodeConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      String errorMessage = "";
      boolean failed = false;
      if (StringUtils.isEmpty(value)) {
        errorMessage = "Expected not empty value";
        failed = true;
      }

      int status = 0;
      if (!failed) {
        try {
          status = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
          errorMessage = "Expected integer value";
          failed = true;
        }
      }

      if (!failed) {
        if (status < 100 || status > 599) {
          errorMessage = "Expected valid HTTP status code";
          failed = true;
        }
      }

      if (!failed) {
        return true;
      } else {
        LOG.warn(errorMessage);
        context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        return false;
      }
    }
  }
}