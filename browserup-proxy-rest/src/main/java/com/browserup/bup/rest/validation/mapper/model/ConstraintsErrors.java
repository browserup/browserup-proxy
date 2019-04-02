package com.browserup.bup.rest.validation.mapper.model;

import java.util.*;

public class ConstraintsErrors {
    private List<ArgumentConstraintsErrors> errors = new ArrayList<>();

    public List<ArgumentConstraintsErrors> getErrors() {
        return errors;
    }

    public void setErrors(List<ArgumentConstraintsErrors> errors) {
        this.errors = errors;
    }

    private ArgumentConstraintsErrors getErrorsByArgumentName(String argumentName) {
        Optional<ArgumentConstraintsErrors> argErrors = this.errors.stream()
                .filter(e -> e.getName().equals(argumentName))
                .findFirst();
        if (!argErrors.isPresent()) {
            ArgumentConstraintsErrors newArgErrors = new ArgumentConstraintsErrors();
            newArgErrors.setName(argumentName);
            errors.add(newArgErrors);
            return newArgErrors;
        } else {
            return argErrors.get();
        }
    }

    public void addError(String argumentName, String error) {
        getErrorsByArgumentName(argumentName).getErrors().add(error);
    }
}
