package com.browserup.bup.rest.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { PatternConstraint.PatternValidator.class })
public @interface PatternConstraint {

  String message() default "";

  String paramName() default "";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class PatternValidator implements ConstraintValidator<PatternConstraint, String> {
    private static final Logger LOG = LoggerFactory.getLogger(PatternValidator.class);

    @Override
    public void initialize(PatternConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      if (StringUtils.isEmpty(value)) {
        return true;
      }

      try {
        Pattern.compile(value);
        return true;
      } catch (Exception ex) {
        String errorMessage = String.format("URL parameter '%s' is not a valid regexp", value);
        LOG.warn(errorMessage);

        context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
      }
      return false;
    }
  }
}