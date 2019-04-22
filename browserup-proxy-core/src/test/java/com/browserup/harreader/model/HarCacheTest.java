package com.browserup.harreader.model;

import org.junit.Assert;

import java.util.Date;

public class HarCacheTest extends AbstractMapperTest<HarCache> {

    private final static Date EXPECTED_EXPIRES = new Date() {{
        setTime(1388577600000L);
    }};
    private final static Date EXPECTED_LAST_ACCESS = new Date() {{
        setTime(1370088000000L);
    }};

    @Override
    public void testMapping() {
        HarCache cache = map("{\"beforeRequest\":{\"expires\":\"2014-01-01T12:00:00\",\"lastAccess\":\"2013-06-01T12:00:00\",\"eTag\":\"abc123\"," +
        "\"hitCount\":3,\"comment\":\"my comment\"},\"afterRequest\":{},\"comment\":\"my comment 2\"}", HarCache.class);

        Assert.assertNotNull(cache.getBeforeRequest());
        Assert.assertEquals(EXPECTED_EXPIRES, cache.getBeforeRequest().getExpires());
        Assert.assertEquals(EXPECTED_LAST_ACCESS, cache.getBeforeRequest().getLastAccess());
        Assert.assertEquals("abc123", cache.getBeforeRequest().geteTag());
        Assert.assertEquals(3, (long) cache.getBeforeRequest().getHitCount());
        Assert.assertEquals("my comment", cache.getBeforeRequest().getComment());

        Assert.assertNotNull(cache.getAfterRequest());

        Assert.assertEquals("my comment 2", cache.getComment());

        cache = map(UNKNOWN_PROPERTY, HarCache.class);
        Assert.assertNotNull(cache);

    }

}
