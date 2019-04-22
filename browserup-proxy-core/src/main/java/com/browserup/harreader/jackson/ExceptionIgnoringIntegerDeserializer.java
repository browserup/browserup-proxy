package com.browserup.harreader.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;

import java.io.IOException;

public class ExceptionIgnoringIntegerDeserializer extends JsonDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        try {
            NumberDeserializers.IntegerDeserializer integerDeserializer = new NumberDeserializers.IntegerDeserializer(Integer.class, null);
            return integerDeserializer.deserialize(jp, ctxt);
        } catch (IOException e) {
            //ignore
        }
        return null;
    }
}
