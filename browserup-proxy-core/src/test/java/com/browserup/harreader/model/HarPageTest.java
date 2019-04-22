package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class HarPageTest extends AbstractMapperTest<HarPage> {

    private final static Date EXPECTED_STARTED = new Date() {{
        setTime(1388577600000L);
    }};

    @Override
    public void testMapping() {
        HarPage page = map("{\"startedDateTime\":\"2014-01-01T12:00:00\",\"id\":\"anId\","
        + "\"title\":\"aTitle\",\"pageTimings\":{},\"comment\":\"my comment\", \"_add\": \"additional info\"}", HarPage.class);

        Assert.assertNotNull(page);
        Assert.assertEquals(EXPECTED_STARTED, page.getStartedDateTime());
        Assert.assertEquals("anId", page.getId());
        Assert.assertEquals("aTitle", page.getTitle());
        Assert.assertNotNull(page.getPageTimings());
        Assert.assertEquals("my comment", page.getComment());
        Assert.assertEquals("additional info", page.getAdditional().get("_add"));

        page = map(UNKNOWN_PROPERTY, HarPage.class);
        Assert.assertNotNull(page);
    }

    @Test
    public void testPageTimingsNull() {
        HarPage page = new HarPage();
        page.setPageTimings(null);
        Assert.assertNotNull(page.getPageTimings());
    }
}
