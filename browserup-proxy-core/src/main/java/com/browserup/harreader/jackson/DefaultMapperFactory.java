package com.browserup.harreader.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.browserup.harreader.HarReaderMode;

import java.util.Date;

public class DefaultMapperFactory implements MapperFactory {

    public ObjectMapper instance(HarReaderMode mode) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        if (mode == HarReaderMode.LAX) {
            module.addDeserializer(Date.class, new ExceptionIgnoringDateDeserializer());
            module.addDeserializer(Integer.class, new ExceptionIgnoringIntegerDeserializer());
        }
        mapper.registerModule(module);
        return mapper;
    }

}
