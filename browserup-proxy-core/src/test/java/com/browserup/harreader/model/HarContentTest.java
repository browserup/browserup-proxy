package com.browserup.harreader.model;

import org.junit.Assert;

public class HarContentTest extends AbstractMapperTest<HarContent> {

    @Override
    public void testMapping() {
        HarContent content = map("{\"size\":123,\"compression\":45,\"mimeType\":\"mime/type\"," +
        "\"text\":\"my content\",\"encoding\":\"base64\",\"comment\":\"my comment\"}", HarContent.class);

        Assert.assertEquals(123L, (long) content.getSize());
        Assert.assertEquals(45L, (long) content.getCompression());
        Assert.assertEquals("mime/type", content.getMimeType());
        Assert.assertEquals("my content", content.getText());
        Assert.assertEquals("base64", content.getEncoding());
        Assert.assertEquals("my comment", content.getComment());

        content = map(UNKNOWN_PROPERTY, HarContent.class);
        Assert.assertNotNull(content);
    }

}
