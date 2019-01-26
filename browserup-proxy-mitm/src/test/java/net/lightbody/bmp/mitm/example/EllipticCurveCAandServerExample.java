package net.lightbody.bmp.mitm.example;

import net.lightbody.bmp.mitm.RootCertificateGenerator;
import net.lightbody.bmp.mitm.keys.ECKeyGenerator;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;

/**
 * This example creates a dynamically-generated Elliptic Curve CA Root Certificate and Private Key and saves them to
 * PEM files for import into a browser and later reuse. It also uses Elliptic Curve keys when impersonating server
 * certificates.
 */
public class EllipticCurveCAandServerExample {
    public static void main(String[] args) {
        // create a dyamic CA root certificate generator using Elliptic Curve keys
        RootCertificateGenerator ecRootCertificateGenerator = RootCertificateGenerator.builder()
                .keyGenerator(new ECKeyGenerator())     // use EC keys, instead of the default RSA
                .build();

        // save the dynamically-generated CA root certificate for installation in a browser
        ecRootCertificateGenerator.saveRootCertificateAsPemFile(new File("/tmp/my-dynamic-ca.cer"));

        // save the dynamically-generated CA private key for use in future LittleProxy executions
        // (see CustomCAPemFileExample.java for an example loading a previously-generated CA cert + key from a PEM file)
        ecRootCertificateGenerator.savePrivateKeyAsPemFile(new File("/tmp/my-ec-private-key.pem"), "secretPassword");

        // tell the MitmManager to use the root certificate we just generated, and to use EC keys when
        // creating impersonated server certs
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(ecRootCertificateGenerator)
                .serverKeyGenerator(new ECKeyGenerator())
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
