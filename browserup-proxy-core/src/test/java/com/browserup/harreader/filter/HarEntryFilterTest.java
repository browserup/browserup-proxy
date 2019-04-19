package com.browserup.harreader.filter;

import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class HarEntryFilterTest {

    private static final TestData[] TEST_DATA = new TestData[]{
        new TestData(
            "http://example.com/index.html",
            true,
            "^(http|https)://example\\.com/index\\.html$"),
        // URL path doesn't match
        new TestData(
            "http://example.com/index2.html",
            false,
            "^(http|https)://example\\.com/index\\.html$"),
        // Protocol doesn't match
        new TestData(
            "ftp://example.com/index.html",
            false,
            "^(http|https)://example\\.com/index\\.html$"),
        // Domain doesn't match
        new TestData(
            "http://example-abc.com/index.html",
            false,
            "^(http|https)://example\\.com/index\\.html$"),
        new TestData(
            "http://example.com/customer?id=123",
            true,
            "^http://example\\.com/customer\\?.*"),
        new TestData(
            "http://example.com/customer",
            true,
            "^http://example\\.com/customer\\??.*"),
        new TestData(
            "http://example.com/customer?",
            true,
            "^http://example\\.com/customer\\??.*"),
        // Protocol doesn't match
        new TestData(
            "https://example.com/customer?",
            false,
            "^http://example\\.com/customer\\??.*"),
        // URL path doesn't match
        new TestData(
            "https://example.com/custome",
            false,
            "^http://example\\.com/customer\\??.*"),
        new TestData(
            "https://example.com/products?id=" + UUID.randomUUID(),
            true,
            "^https://example\\.com/products\\?id=[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}$"),
        // UUID doesn't match (invalid UUID)
        new TestData(
            "https://example.com/products?id=" + UUID.randomUUID().toString().substring(1),
            false,
            "^http://example\\.com/products\\?id=[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}$")
    };

    private TestData data;

    public HarEntryFilterTest(TestData data) {
        this.data = data;
    }

    @Parameterized.Parameters
    public static Collection<TestData> data() {
        return Arrays.asList(TEST_DATA);
    }

    @Test
    public void testHarEntriesUrlPatternFilter() {
        HarEntriesFilter filter = new HarEntriesUrlPatternFilter(Pattern.compile(data.pattern));
        HarEntry harEntry = mock(HarEntry.class);
        HarRequest harRequest = mock(HarRequest.class);

        when(harEntry.getRequest()).thenReturn(harRequest);
        when(harRequest.getUrl()).thenReturn(data.url);

        Assert.assertEquals(
            String.format(
                "Expected to get matcher result: %s, for the following pattern: \n'%s\n' and input string: \n'%s'",
                data.result.toString(), data.pattern, harEntry.getRequest().getUrl()),
            data.result, filter.test(harEntry));
    }

    private static class TestData {
        private final String url;
        private final Boolean result;
        private final String pattern;

        TestData(String url, Boolean result, String pattern) {
            this.url = url;
            this.result = result;
            this.pattern = pattern;
        }
    }
}
