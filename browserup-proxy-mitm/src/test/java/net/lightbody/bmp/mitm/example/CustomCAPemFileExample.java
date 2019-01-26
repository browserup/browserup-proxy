package net.lightbody.bmp.mitm.example;

import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;

/**
 * This example creates an ImpersonatingMitmManager which loads the CA Root Certificate and Private Key
 * from a PEM-encoded certificate and a PEM-encoded private key file.
 */
public class CustomCAPemFileExample {
    public static void main(String[] args) {
        // load the root certificate and private key from existing PEM-encoded certificate and private key files
        PemFileCertificateSource fileCertificateSource = new PemFileCertificateSource(
                new File("/path/to/my/certificate.cer"),    // the PEM-encoded certificate file
                new File("/path/to/my/private-key.pem"),    // the PEM-encoded private key file
                "privateKeyPassword");                      // the password for the private key -- can be null if the private key is not encrypted


        // tell the MitmManager to use the custom certificate and private key
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(fileCertificateSource)
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
