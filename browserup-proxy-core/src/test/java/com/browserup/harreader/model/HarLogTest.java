package com.browserup.harreader.model;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HarLogTest extends AbstractMapperTest<HarLog> {

    private static final String EXPECTED_DEFAULT_VERSION = "1.1";
    private static final List<HarPage> EXPECTED_PAGES_LIST = new ArrayList<>();
    private static final List<HarEntry> EXPECTED_ENTRIES_LIST = new ArrayList<>();

    @Test
    public void testVersion() {
        HarLog log = new HarLog();
        Assert.assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());

        log.setVersion("1.2");
        Assert.assertEquals("1.2", log.getVersion());

        log.setVersion(null);
        Assert.assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());

        log.setVersion("");
        Assert.assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());

        log.setVersion("  ");
        Assert.assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());
    }

    @Test
    public void testPages() {
        HarLog log = new HarLog();
        Assert.assertEquals(EXPECTED_PAGES_LIST, log.getPages());

        log.setPages(null);
        Assert.assertEquals(EXPECTED_PAGES_LIST, log.getPages());
    }

    @Test
    public void testEntries() {
        HarLog log = new HarLog();
        Assert.assertEquals(EXPECTED_ENTRIES_LIST, log.getEntries());

        log.setEntries(null);
        Assert.assertEquals(EXPECTED_ENTRIES_LIST, log.getEntries());
    }

    @Test
    public void testCreatorNull() {
        HarLog log = new HarLog();
        log.setCreator(null);
        Assert.assertNotNull(log.getCreator());
    }

    @Test
    public void testBrowserNull() {
        HarLog log = new HarLog();
        log.setBrowser(null);
        Assert.assertNotNull(log.getBrowser());
    }

    @Test
    public void testFindEntries() {
        Pattern urlPattern = Pattern.compile("http://abc\\.com\\?param=\\d?");
        HarLog log = new HarLog();
        int entriesNumber = 10;

        for (int i = 0; i < entriesNumber; i++) {
            HarEntry entry = new HarEntry();
            HarRequest request1 = new HarRequest();
            request1.setUrl("http://abc.com?param=" + i);
            entry.setRequest(request1);
            log.getEntries().add(entry);
        }

        Assert.assertEquals("Expected to find all entries",
            log.findEntries(urlPattern).size(), entriesNumber);
    }

    @Test
    public void testFindEntryReturnsEmpty() {
        String url = "http://abc.com";
        Pattern urlPattern = Pattern.compile("^doesnotmatch?");

        HarLog log = new HarLog();

        HarEntry entry = new HarEntry();
        HarRequest req = new HarRequest();
        req.setUrl(url);
        entry.setRequest(req);

        log.getEntries().add(entry);

        Assert.assertFalse("Expected to get empty entry",
            log.findMostRecentEntry(urlPattern).isPresent());
    }

    @Test
    public void testFindEntryReturnsMostRecentEntry() {
        String url = "http://abc.com";
        Date firstDate = Date.from(Instant.ofEpochSecond(1000));
        Date secondDate = Date.from(Instant.ofEpochSecond(2000));

        HarLog log = new HarLog();

        HarEntry entry1 = new HarEntry();
        HarRequest request1 = new HarRequest();
        request1.setUrl(url);
        entry1.setRequest(request1);
        entry1.setStartedDateTime(firstDate);

        HarEntry entry2 = new HarEntry();
        HarRequest request2 = new HarRequest();
        request2.setUrl(url);
        entry2.setRequest(request2);
        entry2.setStartedDateTime(secondDate);

        log.getEntries().add(entry1);
        log.getEntries().add(entry2);

        Optional<HarEntry> entry = log.findMostRecentEntry(Pattern.compile("^http://abc\\.com?"));
        Assert.assertTrue("Expected to find entry", entry.isPresent());
        Assert.assertEquals("Expected to find the most recent entry",
            entry.get().getStartedDateTime(), secondDate);
    }

    @Override
    public void testMapping() {
        HarLog log = map("{\"creator\": {}, \"browser\": {}, \"comment\": \"My comment\"}", HarLog.class);

        Assert.assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());
        Assert.assertNotNull(log.getCreator());
        Assert.assertNotNull(log.getBrowser());
        Assert.assertEquals(EXPECTED_PAGES_LIST, log.getPages());
        Assert.assertEquals(EXPECTED_ENTRIES_LIST, log.getEntries());
        Assert.assertEquals("My comment", log.getComment());

        log = map(UNKNOWN_PROPERTY, HarLog.class);
        Assert.assertNotNull(log);
    }
}
