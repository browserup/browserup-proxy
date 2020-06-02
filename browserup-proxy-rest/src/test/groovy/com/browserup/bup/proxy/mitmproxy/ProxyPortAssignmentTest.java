/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.exception.ProxyExistsException;
import com.browserup.bup.exception.ProxyPortsExhaustedException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProxyPortAssignmentTest extends ProxyManagerTest {
    @Override
    public String[] getArgs() {
        return new String[]{"--proxyPortRange", "9091-9093"};
    }

    @Test
    public void testAutoAssignment() {
        int[] ports = {9091, 9092, 9093};
        MitmProxyServer p;
        for(int port : ports){
            p = proxyManager.create();
            assertEquals(port, p.getPort());
        }
        try{
            proxyManager.create();
            fail();
        }catch(ProxyPortsExhaustedException e){
            proxyManager.delete(9093);
            p = proxyManager.create();
            assertEquals(9093, p.getPort());

            proxyManager.delete(9091);
            p = proxyManager.create();
            assertEquals(9091, p.getPort());

            for(int port : ports){
                proxyManager.delete(port);
            }
        }
    }

    @Test
    public void testManualAssignment() {
        MitmProxyServer p = proxyManager.create(9094);
        assertEquals(9094, p.getPort());
        try{
            proxyManager.create(9094);
            fail();
        }catch(ProxyExistsException e){
            assertEquals(9094, e.getPort());
            proxyManager.delete(9094);
        }
    }
}