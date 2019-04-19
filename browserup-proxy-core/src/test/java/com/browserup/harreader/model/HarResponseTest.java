package com.browserup.harreader.model;

import org.junit.Assert;
import org.junit.Test;

public class HarResponseTest extends AbstractMapperTest<HarResponse> {

    @Override
    public void testMapping() {
        HarResponse response = map("{\"status\": 200,\"statusText\": \"OK\",\"httpVersion\": \"HTTP/1.1\","
        + "\"cookies\": [],\"headers\": [],\"content\": {},\"redirectURL\": \"redirectUrl\",\"headersSize\": 318,"
        + "\"bodySize\": 16997,\"comment\": \"My comment\", \"_add\": \"additional info\"}", HarResponse.class);

        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("OK", response.getStatusText());
        Assert.assertEquals("HTTP/1.1", response.getHttpVersion());
        Assert.assertNotNull(response.getCookies());
        Assert.assertNotNull(response.getHeaders());
        Assert.assertNotNull(response.getContent());
        Assert.assertEquals("redirectUrl", response.getRedirectURL());
        Assert.assertEquals(318L, (long) response.getHeadersSize());
        Assert.assertEquals(16997L, (long) response.getBodySize());
        Assert.assertEquals("My comment", response.getComment());
        Assert.assertEquals("additional info", response.getAdditional().get("_add"));
    }

    @Test
    public void testStatus() {
        HarResponse response = new HarResponse();
        Assert.assertEquals(0, response.getStatus());
    }

    @Test
    public void testCookies() {
        HarResponse response = new HarResponse();
        response.setCookies(null);
        Assert.assertNotNull(response.getCookies());
    }

    @Test
    public void testHeaders() {
        HarResponse response = new HarResponse();
        response.setHeaders(null);
        Assert.assertNotNull(response.getHeaders());
    }

    @Test
    public void testContent() {
        HarResponse response = new HarResponse();
        response.setContent(null);
        Assert.assertNotNull(response.getContent());
    }
    
    @Test
    public void testHeadersSize() {
        HarResponse response = new HarResponse();
        response.setHeadersSize(null);
        Assert.assertEquals(-1L, (long) response.getHeadersSize());
    }

    @Test
    public void testBodySize() {
        HarResponse response = new HarResponse();
        response.setBodySize(null);
        Assert.assertEquals(-1L, (long) response.getBodySize());
    }
}