package com.browserup.bup.proxy.bricks.validation;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

public class ParamConstraintViolation<T> implements ConstraintViolation<T> {
    private final String message;

    public ParamConstraintViolation(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getMessageTemplate() {
        return null;
    }

    @Override
    public T getRootBean() {
        return null;
    }

    @Override
    public Class<T> getRootBeanClass() {
        return null;
    }

    @Override
    public Object getLeafBean() {
        return null;
    }

    @Override
    public Path getPropertyPath() {
        return null;
    }

    @Override
    public Object getInvalidValue() {
        return null;
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return null;
    }
}
