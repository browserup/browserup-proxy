package com.browserup.bup.rest.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.browserup.bup.rest.validation.util.MessageSanitizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { NotBlankConstraint.NotBlankValidator.class })
public @interface NotBlankConstraint {

  String message() default "";

  String paramName() default "";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class NotBlankValidator implements ConstraintValidator<NotBlankConstraint, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(NotBlankValidator.class);

    @Override
    public void initialize(NotBlankConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
      if (value != null && StringUtils.isNotEmpty(String.valueOf(value))) {
        return true;
      }

      String escapedValue = MessageSanitizer.escape(value == null ? null : value.toString());
      String errorMessage = String.format("Expected not empty value, got '%s'", escapedValue);
      LOG.warn(errorMessage);

      context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
      return false;
    }
  }
}
