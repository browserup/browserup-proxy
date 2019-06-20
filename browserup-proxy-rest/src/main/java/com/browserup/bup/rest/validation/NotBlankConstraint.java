package com.browserup.bup.rest.validation;

import com.browserup.bup.rest.validation.validator.RegexpPatternValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { NotBlankConstraint.NotBlankValidator.class })
public @interface NotBlankConstraint {

    String message() default "";

    String paramName() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class NotBlankValidator implements ConstraintValidator<NotBlankConstraint, String> {
        private static final Logger LOG = LoggerFactory.getLogger(RegexpPatternValidator.class);

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (!StringUtils.isEmpty(value)) {
                return true;
            }
            String errorMessage = String.format("Expected not empty value, got '%s'", value);
            LOG.warn(errorMessage);

            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addConstraintViolation();
            return false;
        }
    }
}