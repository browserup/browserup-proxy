package com.browserup.harreader.model;

import org.junit.Assert;

public class HarPostDataParamTest extends AbstractMapperTest<HarPostDataParam> {

    @Override
    public void testMapping() {
        HarPostDataParam postDataParam = map("{\"name\": \"aName\", \"value\": \"aValue\", \"fileName\": \"aFilename\", \"contentType\": \"aContentType\", \"comment\": \"My comment\"}", HarPostDataParam.class);

        Assert.assertEquals("aName", postDataParam.getName());
        Assert.assertEquals("aValue", postDataParam.getValue());
        Assert.assertEquals("aFilename", postDataParam.getFileName());
        Assert.assertEquals("aContentType", postDataParam.getContentType());
        Assert.assertEquals("My comment", postDataParam.getComment());


        postDataParam = map(UNKNOWN_PROPERTY, HarPostDataParam.class);
        Assert.assertNotNull(postDataParam);
    }

}