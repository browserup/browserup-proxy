package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

public class HarTest extends AbstractMapperTest<Har>{

    @Test
    public void testLogNull() {
        Har har = new Har();
        har.setLog(null);
        Assert.assertNotNull(har.getLog());
    }

    @Override
    public void testMapping() {
        Har har = map("{\"log\": {}}", Har.class);
        Assert.assertNotNull(har.getLog());

        har = map(UNKNOWN_PROPERTY, Har.class);
        Assert.assertNotNull(har);
    }

}
