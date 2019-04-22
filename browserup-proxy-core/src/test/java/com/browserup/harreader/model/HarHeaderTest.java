package com.browserup.harreader.model;

import org.junit.Assert;

public class HarHeaderTest extends AbstractMapperTest<HarHeader> {

    @Override
    public void testMapping() {
        HarHeader header = map("{\"name\":\"aName\",\"value\":\"aValue\",\"comment\":\"my comment\"}", HarHeader.class);

        Assert.assertNotNull(header);
        Assert.assertEquals("aName", header.getName());
        Assert.assertEquals("aValue", header.getValue());
        Assert.assertEquals("my comment", header.getComment());

        header = map(UNKNOWN_PROPERTY, HarHeader.class);
        Assert.assertNotNull(header);
    }

}
