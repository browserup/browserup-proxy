package com.browserup.harreader.jackson;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;

public class ExceptionIgnoringDateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws java.io.IOException {
        try {
            DateDeserializers.DateDeserializer dateDeserializer = new DateDeserializers.DateDeserializer();
            return dateDeserializer.deserialize(jp, ctxt);
        } catch (IOException e) {
            //ignore
        }
        return new Date(0L);
    }

}
