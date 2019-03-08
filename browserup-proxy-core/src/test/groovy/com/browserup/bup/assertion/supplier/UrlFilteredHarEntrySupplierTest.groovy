package com.browserup.bup.assertion.supplier

import com.browserup.harreader.model.Har
import com.browserup.harreader.model.HarEntry
import com.browserup.harreader.model.HarRequest
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test

import java.time.Instant
import java.util.regex.Pattern

class UrlFilteredHarEntrySupplierTest {

    @Test
    void get() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>
        def fromIndex = 1
        def maxIndexToBeFiltered = 5
        def toIndex = 10
        def urlPattern = Pattern.compile("^http://abc[${fromIndex}-${maxIndexToBeFiltered}]\\.com?")

        (fromIndex..toIndex).each({ n ->
            def harEntry = new HarEntry()
            def harRequest = new HarRequest()
            def url = "http://abc${n}.com"
            harRequest.setUrl(url)
            harEntry.setTime(n)
            harEntry.setStartedDateTime(Date.from(Instant.ofEpochSecond(n)))
            harEntry.setRequest(harRequest)
            harEntries.push(harEntry)
        })
        har.getLog().setEntries(harEntries)

        def supplier = new UrlFilteredHarEntriesSupplier(har, urlPattern)
        def result = supplier.get()

        Assert.assertThat("Expected to get one entry", result, Matchers.hasSize(maxIndexToBeFiltered))
        result.each {
            Assert.assertTrue("Expected that found entry can be matched using url pattern",
                    urlPattern.matcher(it.request.url).matches())
        }
    }
}