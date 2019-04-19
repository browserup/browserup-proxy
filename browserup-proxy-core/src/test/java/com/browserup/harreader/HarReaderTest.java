package com.browserup.harreader;

import com.browserup.harreader.model.Har;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class HarReaderTest {

    private HarReader harReader = new HarReader();

    @Test
    public void test() throws HarReaderException {
        File harFile = new File("src/test/resources/sstoehr.har");
        Har har = harReader.readFromFile(harFile);
        Assert.assertNotNull(har);
    }

    @Test
    public void missingLog() throws HarReaderException {
        Har har = harReader.readFromString("{\"unknown\":\"!\"}");
        Assert.assertNotNull(har);
    }

    @Test(expected = HarReaderException.class)
    public void invalidDateStrict() throws HarReaderException {
        File harFile = new File("src/test/resources/sstoehr.invalid-date.har");
        harReader.readFromFile(harFile);
    }

    @Test
    public void invalidDateLax() throws HarReaderException {
        File harFile = new File("src/test/resources/sstoehr.invalid-date.har");
        Har har = harReader.readFromFile(harFile, HarReaderMode.LAX);
        Assert.assertNotNull(har);
    }

    @Test(expected = HarReaderException.class)
    public void invalidIntegerStrict() throws HarReaderException {
        File harFile = new File("src/test/resources/sstoehr.invalid-integer.har");
        harReader.readFromFile(harFile);
    }

    @Test
    public void invalidIntegerLax() throws HarReaderException {
        File harFile = new File("src/test/resources/sstoehr.invalid-integer.har");
        Har har = harReader.readFromFile(harFile, HarReaderMode.LAX);
        Assert.assertNotNull(har);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapperFactoryNotNull() {
        new HarReader(null);
    }

    @Test
    public void testEquals() throws HarReaderException {
        File harFile = new File("src/test/resources/sstoehr.har");
        Har har1 = harReader.readFromFile(harFile);
        Har har2 = harReader.readFromFile(harFile);
        Assert.assertTrue(har1.equals(har2));
    }

    @Test
    public void testHashCode() throws HarReaderException {
        File harFile = new File("src/test/resources/sstoehr.har");
        Har har1 = harReader.readFromFile(harFile);
        Har har2 = harReader.readFromFile(harFile);
        Assert.assertEquals(har1.hashCode(), har2.hashCode());
    }
}
