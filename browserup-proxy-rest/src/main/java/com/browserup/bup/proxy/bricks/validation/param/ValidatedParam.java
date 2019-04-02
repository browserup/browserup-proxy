package com.browserup.bup.proxy.bricks.validation.param;

import com.browserup.bup.proxy.bricks.validation.param.raw.RawParam;
import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;

import java.util.Optional;

public class ValidatedParam<ParamType> {
    private static final ValidatedParam EMPTY = new ValidatedParam<>(null, null);

    private final boolean isEmpty;
    private final RawParam rawParam;
    private final ParamType parsedParam;
    private final String errorMessage;

    public ValidatedParam(RawParam rawParam, String errorMessage) {
        this.rawParam = rawParam;
        this.parsedParam = null;
        this.errorMessage = errorMessage;
        this.isEmpty = false;
    }

    public ValidatedParam(RawParam rawParam, ParamType parsedParam) {
        this.rawParam = rawParam;
        this.parsedParam = parsedParam;
        this.errorMessage = null;
        this.isEmpty = false;
    }

    public ValidatedParam(RawParam rawParam, ParamType parsedParam, String errorMessage) {
        this.rawParam = rawParam;
        this.parsedParam = parsedParam;
        this.errorMessage = errorMessage;
        this.isEmpty = false;
    }

    private ValidatedParam(String paramName) {
        this.rawParam = new StringRawParam(paramName, "");
        this.parsedParam = null;
        this.errorMessage = "Parameter not provided.";
        this.isEmpty = true;
    }

    public boolean isFailedToParse() {
        return parsedParam == null;
    }

    public ParamType getParsedParam() {
        return parsedParam;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public RawParam getRawParam() {
        return rawParam;
    }

    public static <T> ValidatedParam<T> empty(String name) {
        return new ValidatedParam<>(name);
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }
}
