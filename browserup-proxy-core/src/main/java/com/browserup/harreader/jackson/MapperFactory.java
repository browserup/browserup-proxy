package com.browserup.harreader.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.browserup.harreader.HarReaderMode;

public interface MapperFactory {

    ObjectMapper instance(HarReaderMode mode);

}
