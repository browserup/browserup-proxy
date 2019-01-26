package net.lightbody.bmp.mitm.example;

import net.lightbody.bmp.mitm.KeyStoreFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;

/**
 * This example creates an ImpersonatingMitmManager which loads the CA Root Certificate and Private Key
 * from a custom KeyStore.
 */
public class CustomCAKeyStoreExample {
    public static void main(String[] args) {
        // load the root certificate and private key from an existing KeyStore
        KeyStoreFileCertificateSource fileCertificateSource = new KeyStoreFileCertificateSource(
                "PKCS12",                               // KeyStore type. for .jks files (Java KeyStore), use "JKS"
                new File("/path/to/my/keystore.p12"),
                "keyAlias",                             // alias of the private key in the KeyStore; if you did not specify an alias when creating it, use "1"
                "keystorePassword");


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
