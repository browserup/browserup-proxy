package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

public class HarRequestTest extends AbstractMapperTest<HarRequest> {

    @Override
    public void testMapping() {
        HarRequest request = map("{\"method\": \"GET\",\"url\": "
         + "\"http://www.sebastianstoehr.de/\",\"httpVersion\": "
         + "\"HTTP/1.1\",\"cookies\": [],\"headers\": [],\"queryString\": [],"
         + "\"headersSize\": 676,\"bodySize\": -1, \"postData\": {}, \"comment\":\"my comment\","
         + "\"_add\": \"additional info\"}", HarRequest.class);

        Assert.assertNotNull(request);
        Assert.assertEquals(HttpMethod.GET, request.getMethod());
        Assert.assertEquals("http://www.sebastianstoehr.de/", request.getUrl());
        Assert.assertEquals("HTTP/1.1", request.getHttpVersion());
        Assert.assertNotNull(request.getCookies());
        Assert.assertNotNull(request.getHeaders());
        Assert.assertNotNull(request.getQueryString());
        Assert.assertNotNull(request.getPostData());
        Assert.assertEquals(676L, (long) request.getHeadersSize());
        Assert.assertEquals(-1L, (long) request.getBodySize());
        Assert.assertEquals("my comment", request.getComment());
        Assert.assertEquals("additional info", request.getAdditional().get("_add"));
    }

    @Test
    public void testCookies() {
        HarRequest request = new HarRequest();
        request.setCookies(null);
        Assert.assertNotNull(request.getCookies());
    }

    @Test
    public void testHeaders() {
        HarRequest request = new HarRequest();
        request.setHeaders(null);
        Assert.assertNotNull(request.getHeaders());
    }

    @Test
    public void testQueryString() {
        HarRequest request = new HarRequest();
        request.setQueryString(null);
        Assert.assertNotNull(request.getQueryString());
    }

    @Test
    public void testPostData() {
        HarRequest request = new HarRequest();
        request.setPostData(null);
        Assert.assertNotNull(request.getPostData());
    }

    @Test
    public void testHeadersSize() {
        HarRequest request = new HarRequest();
        request.setHeadersSize(null);
        Assert.assertEquals(-1L, (long) request.getHeadersSize());
    }

    @Test
    public void testBodySize() {
        HarRequest request = new HarRequest();
        request.setBodySize(null);
        Assert.assertEquals(-1L, (long) request.getBodySize());
    }
}