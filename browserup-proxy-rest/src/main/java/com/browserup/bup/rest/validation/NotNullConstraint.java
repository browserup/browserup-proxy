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
import java.util.regex.Pattern;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { NotNullConstraint.NotNullValidator.class })
public @interface NotNullConstraint {

    String message() default "";

    String paramName() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class NotNullValidator implements ConstraintValidator<NotNullConstraint, Object> {
        private static final Logger LOG = LoggerFactory.getLogger(RegexpPatternValidator.class);

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value != null) {
                return true;
            }
            String errorMessage = "Expected not null value";
            LOG.warn(errorMessage);

            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addConstraintViolation();
            return false;
        }
    }
}