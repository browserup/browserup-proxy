package com.browserup.bup.har

import com.browserup.harreader.model.HarEntry
import com.browserup.harreader.model.HarRequest
import org.apache.commons.lang3.tuple.Pair
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@RunWith(Parameterized.class)
class HarEntriesUrlPatternFilterTest {
    private static final String PATTERN = "^(http|https)://abc(\\d?).com?"
    private static final String[] URLS_TO_MATCH = [
            'http://abc.com',
            'http://abc1.com',
            'https://abc.com'
    ]
    private static final String[] URLS_NOT_TO_MATCH = [
            'ftp://abc.com',
            'http://abcd.com',
            'http:/abc.com'
    ]

    @Parameterized.Parameters
    static Collection<Pair<String, Boolean>> data() {
        return URLS_TO_MATCH.collect {Pair.of(it, true)} + URLS_NOT_TO_MATCH.collect {Pair.of(it, false)}
    }

    private Pair<String, Boolean> testData

    HarEntriesUrlPatternFilterTest(Pair<String, Boolean> data) {
        this.testData = data
    }

    @Test
    void testHarEntriesUrlPatternFilter() {
        def filter = new HarEntriesUrlPatternFilter(PATTERN)
        HarEntry harEntry = mock(HarEntry)
        HarRequest harRequest = mock(HarRequest)

        when(harEntry.getRequest()).thenReturn(harRequest)
        when(harRequest.getUrl()).thenReturn(testData.left)

        Assert.assertEquals(testData.right, filter.test(harEntry))
    }
}
