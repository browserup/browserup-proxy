package net.lightbody.bmp.mitm.example;

import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

/**
 * This example creates an ImpersonatingMitmManager with all-default settings:
 *      - Dynamically-generated CA Root Certificate and Private Key (2048-bit RSA. SHA512 signature)
 *      - Server certificate impersonation by domain name (2048-bit RSA, SHA512 signature)
 *      - Default Java trust store (upstream servers' certificates validated against Java's trusted CAs)
 */
public class LittleProxyDefaultConfigExample {
    public static void main(String[] args) {
        // initialize an MitmManager with default settings
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder().build();

        // to save the generated CA certificate for installation in a browser, see SaveGeneratedCAExample.java

        // tell the HttpProxyServerBootstrap to use the new MitmManager
        HttpProxyServer proxyServer = DefaultHttpProxyServer.bootstrap()
                .withManInTheMiddle(mitmManager)
                .start();

        // make your requests to the proxy server
        //...

        proxyServer.abort();
    }
}
