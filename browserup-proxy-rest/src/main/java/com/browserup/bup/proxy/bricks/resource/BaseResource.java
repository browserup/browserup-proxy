package com.browserup.bup.proxy.bricks.resource;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.ProxyManager;
import com.browserup.bup.proxy.bricks.resource.entries.EntriesProxyResource;
import com.browserup.bup.proxy.bricks.validation.ParamConstraintViolation;
import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BaseResource  {
    private static final Logger LOG = LoggerFactory.getLogger(EntriesProxyResource.class);

    private final ProxyManager proxyManager;

    @Inject
    public BaseResource(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    protected ValidatedParam<BrowserUpProxyServer> parseProxyServer(IntRawParam rawParam) throws IllegalArgumentException {
        return Optional.of(proxyManager.get(rawParam.getValue()))
                .map(proxy -> new ValidatedParam<>(rawParam, proxy))
                .orElse(new ValidatedParam<>(rawParam, "No proxy server found for port"));
    }

    protected ValidatedParam<Pattern> parsePatternParam(StringRawParam rawParam) throws IllegalArgumentException {
        if (StringUtils.isEmpty(rawParam.getValue())) {
            return new ValidatedParam<>(rawParam, "Empty string is not valid regexp");
        }
        Pattern result;
        try {
            result = Pattern.compile(rawParam.getValue());
        } catch (Exception ex) {
            return new ValidatedParam<>(rawParam, "URL parameter '%s' is not a valid regexp");
        }
        return new ValidatedParam<>(rawParam, result);
    }

    protected ValidatedParam<Long> parseLongParam(StringRawParam rawParam) throws IllegalArgumentException {
        if (StringUtils.isEmpty(rawParam.getValue())) {
            return new ValidatedParam<>(rawParam, "Empty string is not valid number");
        }
        Long result;
        try {
            result = Long.valueOf(rawParam.getValue());
        } catch (Exception ex) {
            return new ValidatedParam<>(rawParam, "Not valid Long number provided");
        }
        return new ValidatedParam<>(rawParam, result);
    }

    protected ValidatedParam<String> parseNonEmptyStringParam(StringRawParam rawParam) throws IllegalArgumentException {
        if (StringUtils.isEmpty(rawParam.getValue())) {
            return new ValidatedParam<>(rawParam, "Empty string is not valid");
        }
        return new ValidatedParam<>(rawParam, rawParam.getValue(), null);
    }

    protected ValidatedParam<Integer> parseIntParam(StringRawParam rawParam) throws IllegalArgumentException {
        if (StringUtils.isEmpty(rawParam.getValue())) {
            return new ValidatedParam<>(rawParam, "Empty string is not valid number");
        }
        Integer result;
        try {
            result = Integer.valueOf(rawParam.getValue());
        } catch (Exception ex) {
            return new ValidatedParam<>(rawParam, "Not valid Integer number provided");
        }
        return new ValidatedParam<>(rawParam, result);
    }

    protected void checkRequiredParams(ValidatedParam<?>... params) {
        List<ValidatedParam<?>> failedParams = filterFailedParams(params);
        if (!failedParams.isEmpty()) {
            throw createValidationExceptionForFailedParams(failedParams);
        }
    }

    protected void checkOptionalParams(ValidatedParam<?>... params) {
        List<ValidatedParam<?>> failedParams = filterFailedOptionalParams(params);
        if (!failedParams.isEmpty()) {
            throw createValidationExceptionForFailedParams(failedParams);
        }
    }

    private List<ValidatedParam<?>> filterFailedOptionalParams(ValidatedParam<?>... params) {
        return Arrays.stream(params)
                .filter(validatedParam -> !validatedParam.isEmpty() && validatedParam.isFailedToParse())
                .collect(Collectors.toList());
    }

    private List<ValidatedParam<?>> filterFailedParams(ValidatedParam<?>... params) {
        return Arrays.stream(params)
                .filter(ValidatedParam::isFailedToParse)
                .collect(Collectors.toList());
    }

    private ValidationException createValidationExceptionForFailedParams(List<ValidatedParam<?>> failedParams) {
        Set<ConstraintViolation<?>> constraintViolations = failedParams.stream()
                .map(this::createConstraintViolationFromFailedParam)
                .collect(Collectors.toSet());

        return new ValidationException(new ConstraintViolationException(constraintViolations));
    }

    private ConstraintViolation<?> createConstraintViolationFromFailedParam(ValidatedParam<?> param) {
        String errMessage = String.format(
                "Field: '%s' is invalid: %s",
                param.getRawParam().getName(),
                param.getErrorMessage().orElse(""));
        return new ParamConstraintViolation(errMessage);
    }
}
