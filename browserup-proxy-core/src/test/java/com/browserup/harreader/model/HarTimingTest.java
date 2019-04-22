package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

public class HarTimingTest extends AbstractMapperTest<HarTiming> {

    @Override
    public void testMapping() {
        HarTiming timing = map("{\"blocked\": 3804,\"dns\": 23,\"connect\": 5,\"send\": 9,\"wait\": 5209,"
        + "\"receive\": 79, \"ssl\": 123, \"comment\": \"my comment\"}", HarTiming.class);

        Assert.assertNotNull(timing);
        Assert.assertEquals(3804, (int) timing.getBlocked());
        Assert.assertEquals(23, (int) timing.getDns());
        Assert.assertEquals(5, (int) timing.getConnect());
        Assert.assertEquals(9, (int) timing.getSend());
        Assert.assertEquals(5209, (int) timing.getWait());
        Assert.assertEquals(79, (int) timing.getReceive());
        Assert.assertEquals(123, (int) timing.getSsl());
        Assert.assertEquals("my comment", timing.getComment());
    }

    @Test
    public void testBlocked() {
        HarTiming timing = new HarTiming();
        timing.setBlocked(null);
        Assert.assertEquals(-1, (int) timing.getBlocked());
    }

    @Test
    public void testDns() {
        HarTiming timing = new HarTiming();
        timing.setDns(null);
        Assert.assertEquals(-1, (int) timing.getDns());
    }

    @Test
    public void testConnect() {
        HarTiming timing = new HarTiming();
        timing.setConnect(null);
        Assert.assertEquals(-1, (int) timing.getConnect());
    }

    @Test
    public void testSsl() {
        HarTiming timing = new HarTiming();
        timing.setSsl(null);
        Assert.assertEquals(-1, (int) timing.getSsl());
    }
}