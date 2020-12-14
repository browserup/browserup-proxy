package com.browserup.bup.rest.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.ws.rs.core.Context;

import com.browserup.bup.proxy.ProxyManager;

import com.browserup.bup.rest.validation.util.MessageSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator.class })
public @interface PortWithExistingProxyConstraint {

  String message() default "";

  String paramName() default "port";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class PortWithExistingProxyConstraintValidator
      implements ConstraintValidator<PortWithExistingProxyConstraint, Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(PortWithExistingProxyConstraintValidator.class);
    private static final String PARAM_NAME = "proxy port";

    private final ProxyManager proxyManager;

    public PortWithExistingProxyConstraintValidator(@Context ProxyManager proxyManager) {
      this.proxyManager = proxyManager;
    }

    @Override
    public void initialize(PortWithExistingProxyConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
      if (proxyManager.get(value) != null) {
        return true;
      }

      String escapedValue = MessageSanitizer.escape(value.toString());
      String errorMessage = String.format("No proxy server found for specified port %s", escapedValue);
      LOG.warn(errorMessage);

      context.buildConstraintViolationWithTemplate(errorMessage).addPropertyNode(PARAM_NAME).addConstraintViolation();
      return false;
    }
  }
}
