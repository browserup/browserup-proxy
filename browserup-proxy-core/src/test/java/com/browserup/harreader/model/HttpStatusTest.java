package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

public class HttpStatusTest {

    @Test
    public void testByCode() {
        for (HttpStatus status : HttpStatus.values()) {
            Assert.assertEquals(status, HttpStatus.byCode(status.getCode()));
        }
    }

    @Test
    public void test302() {
        Assert.assertEquals(HttpStatus.FOUND, HttpStatus.byCode(302));
    }

    @Test
    public void testInvalidCode() {
        Assert.assertEquals(HttpStatus.UNKNOWN_HTTP_STATUS, HttpStatus.byCode(0));
        Assert.assertEquals(HttpStatus.UNKNOWN_HTTP_STATUS, HttpStatus.byCode(1000));
        Assert.assertEquals(HttpStatus.UNKNOWN_HTTP_STATUS, HttpStatus.byCode(-999));
    }

}
