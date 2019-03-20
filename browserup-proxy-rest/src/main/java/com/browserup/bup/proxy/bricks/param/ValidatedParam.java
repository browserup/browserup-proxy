package com.browserup.bup.proxy.bricks.param;

import com.google.sitebricks.headless.Reply;

import java.util.List;
import java.util.Optional;

public class ValidatedParam<ParamType> {
    private static final ValidatedParam EMPTY = new ValidatedParam<>(null, null);

    private final ParamType param;
    private final Reply<?> errorReply;

    public ValidatedParam(ParamType param, Reply<?> errorReply) {
        this.param = param;
        this.errorReply = errorReply;
    }

    public ValidatedParam(ParamType param) {
        this.param = param;
        this.errorReply = null;
    }

    public ValidatedParam(Reply<?> errorReply) {
        this.param = null;
        this.errorReply = errorReply;
    }

    public ValidatedParam() {
        this.param = null;
        this.errorReply = null;
    }

    public Optional<ParamType> getParam() {
        return Optional.ofNullable(param);
    }

    public ParamType getRequredParam() {
        return param;
    }

    public Optional<Reply<?>> getErrorReply() {
        return Optional.ofNullable(errorReply);
    }

    public static <T> ValidatedParam<T> empty() {
        return (ValidatedParam<T>) EMPTY;
    }
}
