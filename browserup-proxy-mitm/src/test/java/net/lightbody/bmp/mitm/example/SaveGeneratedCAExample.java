package net.lightbody.bmp.mitm.example;

import net.lightbody.bmp.mitm.RootCertificateGenerator;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;

/**
 * This example creates an ImpersonatingMitmManager with all-default settings and saves the dynamically generated
 * CA Root Certificate as a PEM file for installation in a browser.
 */
public class SaveGeneratedCAExample {
    public static void main(String[] args) {
        // create a dynamic CA root certificate generator using default settings (2048-bit RSA keys)
        RootCertificateGenerator rootCertificateGenerator = RootCertificateGenerator.builder().build();

        // save the dynamically-generated CA root certificate for installation in a browser
        rootCertificateGenerator.saveRootCertificateAsPemFile(new File("/tmp/my-dynamic-ca.cer"));

        // tell the MitmManager to use the root certificate we just generated
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(rootCertificateGenerator)
                .build();

        // tell the HttpProxyServerBootstrap to use the new MitmManager
        HttpProxyServer proxyServer = DefaultHttpProxyServer.bootstrap()
                .withManInTheMiddle(mitmManager)
                .start();

        // make your requests to the proxy server
        //...

        proxyServer.abort();
    }
}
