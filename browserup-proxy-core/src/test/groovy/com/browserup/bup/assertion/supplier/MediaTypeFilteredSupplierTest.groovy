package com.browserup.bup.assertion.supplier

import com.browserup.harreader.model.Har
import com.browserup.harreader.model.HarContent
import com.browserup.harreader.model.HarEntry
import com.browserup.harreader.model.HarResponse
import org.hamcrest.Matchers
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertThat

class MediaTypeFilteredSupplierTest {
    @Test
    void getFilteredMediaTypeEntries() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>
        def harEntry1 = new HarEntry()
        def harResponse1 = new HarResponse()
        def harContent1 = new HarContent()
        harContent1.setMimeType("image/png")
        harResponse1.setContent(harContent1)
        harEntry1.setResponse(harResponse1)
        harEntries.add(harEntry1)

        def harEntry2 = new HarEntry()
        def harResponse2 = new HarResponse()
        def harContent2 = new HarContent()
        harContent2.setMimeType("image/jpg")
        harResponse2.setContent(harContent2)
        harEntry2.setResponse(harResponse2)
        harEntries.add(harEntry2)

        def harEntry3 = new HarEntry()
        def harResponse3 = new HarResponse()
        def harContent3 = new HarContent()
        harContent3.setMimeType("text/plain")
        harResponse3.setContent(harContent3)
        harEntry3.setResponse(harResponse3)
        harEntries.add(harEntry3)

        har.getLog().setEntries(harEntries)

        def supplier = new MediaTypeFilteredSupplier(har, Pattern.compile("image/.*"))
        def result = supplier.get()

        assertThat("Expected to get 2 entries", result, Matchers.hasSize(2))
    }

    @Test
    void getFilteredAllResourcesEntries() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>
        def harEntry1 = new HarEntry()
        def harResponse1 = new HarResponse()
        def harContent1 = new HarContent()
        harContent1.setMimeType("image/png")
        harResponse1.setContent(harContent1)
        harEntry1.setResponse(harResponse1)
        harEntries.add(harEntry1)

        def harEntry2 = new HarEntry()
        def harResponse2 = new HarResponse()
        def harContent2 = new HarContent()
        harContent2.setMimeType("text/javascript")
        harResponse2.setContent(harContent2)
        harEntry2.setResponse(harResponse2)
        harEntries.add(harEntry2)

        def harEntry3 = new HarEntry()
        def harResponse3 = new HarResponse()
        def harContent3 = new HarContent()
        harContent3.setMimeType("text/css")
        harResponse3.setContent(harContent3)
        harEntry3.setResponse(harResponse3)
        harEntries.add(harEntry3)

        def harEntry4 = new HarEntry()
        def harResponse4 = new HarResponse()
        def harContent4 = new HarContent()
        harContent4.setMimeType("text/plain")
        harResponse4.setContent(harContent4)
        harEntry4.setResponse(harResponse4)
        harEntries.add(harEntry4)

        har.getLog().setEntries(harEntries)

        def supplier = new MediaTypeFilteredSupplier(har, Pattern.compile("image/.*|text/javascript|text/css"))
        def result = supplier.get()

        assertThat("Expected to get 3 entries", result, Matchers.hasSize(3))
    }

    @Test
    void getEmtpyEntriesIfNothingFoundByFilter() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>

        (1..3).each({ n ->
            def harEntry = new HarEntry()
            def harResponse = new HarResponse()
            def harContent = new HarContent()
            harContent.setMimeType("text/plain")
            harResponse.setContent(harContent)
            harEntry.setResponse(harResponse)
            harEntries.push(harEntry)
        })
        har.getLog().setEntries(harEntries)

        def supplier = new MediaTypeFilteredSupplier(har, Pattern.compile("text/javascript"))
        def result = supplier.get()

        assertThat("Expected to get empty array", result, Matchers.hasSize(0))
    }
}