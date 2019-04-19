package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

public class HarPageTimingTest extends AbstractMapperTest<HarPageTiming> {

    private static final Integer EXPECTED_DEFAULT_DURATION = -1;

    @Test
    public void testOnContentLoad() {
        HarPageTiming pageTiming = new HarPageTiming();
        Assert.assertEquals(EXPECTED_DEFAULT_DURATION, pageTiming.getOnContentLoad());

        pageTiming.setOnContentLoad(1234);
        Assert.assertEquals(1234, (int) pageTiming.getOnContentLoad());

        pageTiming.setOnContentLoad(null);
        Assert.assertEquals(EXPECTED_DEFAULT_DURATION, pageTiming.getOnContentLoad());
    }

    @Test
    public void testOnLoad() {
        HarPageTiming pageTiming = new HarPageTiming();
        Assert.assertEquals(EXPECTED_DEFAULT_DURATION, pageTiming.getOnLoad());

        pageTiming.setOnLoad(1234);
        Assert.assertEquals(1234, (int) pageTiming.getOnLoad());

        pageTiming.setOnLoad(null);
        Assert.assertEquals(EXPECTED_DEFAULT_DURATION, pageTiming.getOnLoad());
    }

    @Override
    public void testMapping() {
        HarPageTiming pageTiming = map("{\"onContentLoad\": 1234, \"onLoad\": 5678, \"comment\": \"My comment\"}", HarPageTiming.class);

        Assert.assertEquals(1234, (int) pageTiming.getOnContentLoad());
        Assert.assertEquals(5678, (int) pageTiming.getOnLoad());
        Assert.assertEquals("My comment", pageTiming.getComment());

        pageTiming = map(UNKNOWN_PROPERTY, HarPageTiming.class);
        Assert.assertNotNull(pageTiming);
    }
}
