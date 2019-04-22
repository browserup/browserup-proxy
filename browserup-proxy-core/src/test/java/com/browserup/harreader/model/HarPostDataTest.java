package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HarPostDataTest extends AbstractMapperTest<HarPostData> {

    private static final List<HarPostDataParam> EXPECTED_LIST = new ArrayList<>();

    @Override
    public void testMapping() {
        HarPostData postData = map("{\"mimeType\": \"aMimeType\", \"params\": [], \"text\":\"aText\", \"comment\": \"My comment\"}", HarPostData.class);

        Assert.assertEquals("aMimeType", postData.getMimeType());
        Assert.assertEquals(EXPECTED_LIST, postData.getParams());
        Assert.assertEquals("aText", postData.getText());
        Assert.assertEquals("My comment", postData.getComment());

        postData = map(UNKNOWN_PROPERTY, HarPostData.class);
        Assert.assertNotNull(postData);
    }

    @Test
    public void testParams() {
        HarPostData postData = new HarPostData();
        postData.setParams(null);
        Assert.assertNotNull(postData.getParams());
    }
}
