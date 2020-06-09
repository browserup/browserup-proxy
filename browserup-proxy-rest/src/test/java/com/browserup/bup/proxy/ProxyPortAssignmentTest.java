/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.exception.AddressInUseException;
import com.browserup.bup.exception.ProxyExistsException;
import com.browserup.bup.exception.ProxyPortsExhaustedException;
import com.browserup.bup.proxy.test.util.ProxyManagerTest;
import org.junit.Test;

public class ProxyPortAssignmentTest extends ProxyManagerTest {
    @Override
    public String[] getArgs() {
        return new String[]{"--proxyPortRange", "9091-9093"};
    }

    @Test
    public void testAutoAssignment() {
        int[] ports = {9091, 9092, 9093};
        BrowserUpProxyServer p;
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
        BrowserUpProxyServer p = proxyManager.create(9094);
        assertEquals(9094, p.getPort());
        try{
            proxyManager.create(9094);
            fail();
        }catch(ProxyExistsException e){
            assertEquals(9094, e.getPort());
            proxyManager.delete(9094);
        }
    }

    @Test
    public void testBindFailure() {
    	
    	int testPort = 9094;
    	
        InetSocketAddress testClientBindSocket = new InetSocketAddress(testPort);
        
        // Use a test socket to attempt binding with
        try(Socket testSocket = new Socket()) {
        	testSocket.bind(testClientBindSocket);	
	        try{
	            proxyManager.create(9094);
	            fail();
	        }catch(AddressInUseException e){
	            assertEquals(9094, e.getPort());
	        }
        } catch (IOException e1) {
			e1.printStackTrace();
        	fail();
		}
    }
}
