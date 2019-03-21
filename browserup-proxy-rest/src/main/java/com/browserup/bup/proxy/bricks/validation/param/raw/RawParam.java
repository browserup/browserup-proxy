package com.browserup.bup.proxy.bricks.validation.param.raw;

public abstract class RawParam<ValueType> {
    private final String name;
    private final ValueType value;

    public RawParam(String name, ValueType value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public ValueType getValue() {
        return value;
    }
}
