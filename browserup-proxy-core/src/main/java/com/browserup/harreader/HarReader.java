package com.browserup.harreader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.browserup.harreader.jackson.DefaultMapperFactory;
import com.browserup.harreader.jackson.MapperFactory;
import com.browserup.harreader.model.Har;

import java.io.File;
import java.io.IOException;

public class HarReader {

    private final MapperFactory mapperFactory;

    public HarReader(MapperFactory mapperFactory) {
        if (mapperFactory == null) {
            throw new IllegalArgumentException("mapperFactory must not be null!");
        }
        this.mapperFactory = mapperFactory;
    }

    public HarReader() {
        this(new DefaultMapperFactory());
    }

    public Har readFromFile(File har) throws HarReaderException {
        return this.readFromFile(har, HarReaderMode.STRICT);
    }

    public Har readFromFile(File har, HarReaderMode mode) throws HarReaderException {
        ObjectMapper mapper = mapperFactory.instance(mode);
        try {
            return mapper.readValue(har, Har.class);
        } catch (IOException e) {
            throw new HarReaderException(e);
        }
    }

    public Har readFromString(String har) throws HarReaderException {
        return this.readFromString(har, HarReaderMode.STRICT);
    }

    public Har readFromString(String har, HarReaderMode mode) throws HarReaderException {
        ObjectMapper mapper = mapperFactory.instance(mode);
        try {
            return mapper.readValue(har, Har.class);
        } catch (IOException e) {
            throw new HarReaderException(e);
        }
    }

}
