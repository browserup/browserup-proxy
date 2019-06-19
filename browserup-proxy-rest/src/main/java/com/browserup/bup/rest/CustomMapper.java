package com.browserup.bup.rest;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(prepareMessage(exception))
                .type("text/plain")
                .build();
    }

    private String prepareMessage(ConstraintViolationException exception) {
        StringBuilder message = new StringBuilder();
        if (exception.getConstraintViolations().size() > 0) {
            message.append("Constraints violations:\n");
        }
        Map<String, List<String>> argToErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            if (StringUtils.isEmpty(violation.getMessage())) {
                continue;
            }
            String argumentIdentifier = getArgumentIdentifier(violation);
            argToErrors.putIfAbsent(argumentIdentifier, new ArrayList<>());
            argToErrors.get(argumentIdentifier).add(violation.getMessage());
        }
        return message.toString();
    }

    private String getArgumentIdentifier(ConstraintViolation<?> cv) {
        String argumentIdentifier;
        Object paramName = cv.getConstraintDescriptor().getAttributes().get("paramName");
        if (paramName instanceof String && paramName.toString().length() > 0) {
            argumentIdentifier = (String) paramName;
        } else {
            argumentIdentifier = StringUtils.substringAfterLast(cv.getPropertyPath().toString(), ".");
        }
        return argumentIdentifier;
    }
}