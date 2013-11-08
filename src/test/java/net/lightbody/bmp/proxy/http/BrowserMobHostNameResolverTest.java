package net.lightbody.bmp.proxy.http;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrowserMobHostNameResolverTest {

    private BrowserMobHostNameResolver browserMobHostNameResolver;

    @Mock
    private Cache cache;

    @Mock
    private Resolver resolver;

    @Mock
    private SetResponse setResponse;

    @Before
    public void setUp() throws Exception {
        browserMobHostNameResolver = new BrowserMobHostNameResolver(cache, resolver);
    }

    @Test
    public void shouldRemapHost() {
        String original = "foo";
        String remapped = "bar";

        browserMobHostNameResolver.remap(original, remapped);

        assertEquals(remapped, browserMobHostNameResolver.remapping(original));
    }

    @Test
    public void shouldRetainReferenceToOriginalMapping() {
        String original = "foo";
        String remapped = "bar";

        browserMobHostNameResolver.remap(original, remapped);

        assertTrue(browserMobHostNameResolver.original(remapped).contains(original));
    }

    @Test
    public void shouldClearCache() {
        browserMobHostNameResolver.clearCache();

        verify(cache, times(1)).clearCache();
    }

    @Test
    public void shouldSetCacheTimeout() {
        int timeout = 5;

        browserMobHostNameResolver.setCacheTimeout(timeout);

        verify(cache, times(1)).setMaxCache(timeout);
    }

    @Test
    public void shouldDeferToCacheWhenCacheStateQueried() throws Exception {
        boolean expected = true;
        when(cache.lookupRecords(any(Name.class), eq(Type.ANY), eq(3))).thenReturn(setResponse);
        when(setResponse.isSuccessful()).thenReturn(expected);

        boolean result = browserMobHostNameResolver.isCached("localhost");

        assertEquals(expected, result);
    }

    @Test
    public void shouldResolveIPAddress() throws Exception {
        String hostname = "127.0.0.1";

        InetAddress result = browserMobHostNameResolver.resolve(hostname);

        assertNotNull(result);

        // should not consult DNS for an IP address
        verify(cache, never()).lookupRecords(any(Name.class), anyInt(), anyInt());
    }

    @Test
    public void shouldResolveHostNameWithDNSJava() throws Exception {
        String hostname = "localhost";

        ARecord record = mock(ARecord.class);
        RRset rrset = mock(RRset.class);
        List<Record> recordList = new ArrayList<Record>();
        recordList.add(record);
        when(rrset.rrs()).thenReturn(recordList.iterator());
        RRset[] answers = new RRset[]{
                rrset
        };
        when(record.getAddress()).thenReturn(InetAddress.getByName("127.0.0.1"));

        when(cache.lookupRecords(any(Name.class), anyInt(), anyInt())).thenReturn(setResponse);
        when(setResponse.isSuccessful()).thenReturn(true);
        when(setResponse.answers()).thenReturn(answers);

        final Message response = mock(Message.class);
        Header header = mock(Header.class);
        final Record question = mock(Record.class);

        when(response.getHeader()).thenReturn(header);
        when(response.getQuestion()).thenReturn(question);

        when(resolver.send(any(Message.class))).thenAnswer(new Answer<Message>() {

            @Override
            public Message answer(InvocationOnMock invocationOnMock) throws Throwable {
                Message query = (Message)invocationOnMock.getArguments()[0];
                Record outgoing = query.getQuestion();

                Whitebox.setInternalState(question, "type", Whitebox.getInternalState(outgoing, "type"));
                Whitebox.setInternalState(question, "dclass", Whitebox.getInternalState(outgoing, "dclass"));
                Whitebox.setInternalState(question, "name", Whitebox.getInternalState(outgoing, "name"));

                byte[] canonical = outgoing.rdataToWireCanonical();

                when(question.rdataToWireCanonical()).thenReturn(canonical);

                return response;
            }
        });

        InetAddress result = browserMobHostNameResolver.resolve(hostname);

        assertNotNull(result);
    }

    @Test
    public void shouldFallbackToResolvingHostNameWithNativeJava() throws Exception {
        String hostname = "localhost";

        when(cache.lookupRecords(any(Name.class), anyInt(), anyInt())).thenReturn(setResponse);
        when(setResponse.isSuccessful()).thenReturn(false);
        when(setResponse.isNXDOMAIN()).thenReturn(true);

        InetAddress result = browserMobHostNameResolver.resolve(hostname);

        assertNotNull(result);

        // should never have asked the lookup any answers as it's unsuccessful
        verify(setResponse, never()).answers();
    }
}
