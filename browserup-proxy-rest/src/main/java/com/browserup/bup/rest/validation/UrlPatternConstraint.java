package com.browserup.bup.rest.validation;

import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.rest.mostrecent.MostRecentEntryProxyResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.ws.rs.core.Context;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { UrlPatternConstraint.UrlPatternConstraintValidator.class })
public @interface UrlPatternConstraint {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UrlPatternConstraintValidator implements ConstraintValidator<UrlPatternConstraint, String> {
        private static final Logger LOG = LoggerFactory.getLogger(UrlPatternConstraintValidator.class);
        private static final String PARAM_NAME = "urlPattern";

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            try {
                Pattern.compile(value);
                return true;
            } catch (Exception ex) {
                String errorMessage = String.format("URL parameter '%s' is not a valid regexp", value);
                LOG.warn(errorMessage);

                context.buildConstraintViolationWithTemplate(errorMessage)
                        .addPropertyNode(PARAM_NAME)
                        .addConstraintViolation();
            }
            return false;
        }
    }
}