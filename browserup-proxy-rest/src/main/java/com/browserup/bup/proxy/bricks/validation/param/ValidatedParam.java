package com.browserup.bup.proxy.bricks.validation.param;

import com.browserup.bup.proxy.bricks.validation.param.raw.RawParam;

import java.util.Optional;

public class ValidatedParam<ParamType> {
    private static final ValidatedParam EMPTY = new ValidatedParam<>(null, null);

    private final RawParam rawParam;
    private final ParamType parsedParam;
    private final String errorMessage;

    public ValidatedParam(RawParam rawParam, String errorMessage) {
        this.rawParam = rawParam;
        this.parsedParam = null;
        this.errorMessage = errorMessage;
    }

    public ValidatedParam(RawParam rawParam, ParamType parsedParam) {
        this.rawParam = rawParam;
        this.parsedParam = parsedParam;
        this.errorMessage = null;
    }

    public ValidatedParam(RawParam rawParam, ParamType parsedParam, String errorMessage) {
        this.rawParam = rawParam;
        this.parsedParam = parsedParam;
        this.errorMessage = errorMessage;
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

    public static <T> ValidatedParam<T> empty() {
        return (ValidatedParam<T>) EMPTY;
    }
}
