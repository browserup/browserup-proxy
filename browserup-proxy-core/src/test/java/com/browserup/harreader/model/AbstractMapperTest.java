package com.browserup.harreader.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractMapperTest<T> {

    protected final static String UNKNOWN_PROPERTY = "{\"unknownProperty\":\"value\"}";

    @Test
    public abstract void testMapping();

    public T map(String input, Class<T> tClass) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(input, tClass);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }
}
