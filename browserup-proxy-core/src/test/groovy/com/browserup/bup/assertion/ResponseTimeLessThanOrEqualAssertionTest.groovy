/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.assertion

import com.browserup.harreader.model.HarEntry
import org.junit.Assert;
import org.junit.Test;

class ResponseTimeLessThanOrEqualAssertionTest {

    @Test
    void testAssertionFailsIfTimeExceeds() {
        def expectedTime = 500
        def assertion = new ResponseTimeLessThanOrEqualAssertion(expectedTime)
        def entry = new HarEntry()
        def time = 1000
        entry.setTime(time)

        def result = assertion.assertion(entry)

        Assert.assertTrue("Expected assertion to return error", result.present)
    }

    @Test
    void testAssertionDoesNotFailIfTimeDoesNotExceed() {
        def expectedTime = 2000
        def assertion = new ResponseTimeLessThanOrEqualAssertion(expectedTime)
        def entry = new HarEntry()
        def time = 1000
        entry.setTime(time)

        def result = assertion.assertion(entry)

        Assert.assertFalse("Expected assertion not to return error", result.present)
    }
}