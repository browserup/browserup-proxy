package com.browserup.harreader.model;

import org.junit.Assert;

public class HarQueryParamTest extends AbstractMapperTest<HarQueryParam> {

    @Override
    public void testMapping() {
        HarQueryParam queryParam = map("{\"name\": \"aName\", \"value\":\"aValue\", \"comment\": \"My comment\"}", HarQueryParam.class);

        Assert.assertEquals("aName", queryParam.getName());
        Assert.assertEquals("aValue", queryParam.getValue());
        Assert.assertEquals("My comment", queryParam.getComment());
    }

}