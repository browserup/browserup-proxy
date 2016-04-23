package net.lightbody.bmp.mitm;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import net.lightbody.bmp.mitm.keys.ECKeyGenerator;
import net.lightbody.bmp.mitm.keys.KeyGenerator;
import net.lightbody.bmp.mitm.keys.RSAKeyGenerator;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.mitm.tools.BouncyCastleSecurityProviderTool;
import net.lightbody.bmp.mitm.tools.DefaultSecurityProviderTool;
import net.lightbody.bmp.mitm.util.MitmConstants;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

// ignored as a quick work-around to running these tests with unit tests
@Ignore
@RunWith(Parameterized.class)
public class ImpersonationPerformanceTests {
    private static final Logger log = LoggerFactory.getLogger(ImpersonationPerformanceTests.class);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new RSAKeyGenerator(), "SHA384", new RSAKeyGenerator(), "SHA384"},
                {new RSAKeyGenerator(), "SHA384", new RSAKeyGenerator(1024), "SHA384"},
                {new RSAKeyGenerator(1024), "SHA384", new RSAKeyGenerator(1024), "SHA384"},
                {new RSAKeyGenerator(), "SHA384", new ECKeyGenerator(), "SHA384"},
                {new ECKeyGenerator(), "SHA384", new ECKeyGenerator(), "SHA384"},
                {new ECKeyGenerator(), "SHA384", new RSAKeyGenerator(), "SHA384"}
        });
    }

    @Parameter
    public KeyGenerator rootCertKeyGen;

    @Parameter(1)
    public String rootCertDigest;

    @Parameter(2)
    public KeyGenerator serverCertKeyGen;

    @Parameter(3)
    public String serverCertDigest;

    private static final int WARM_UP_ITERATIONS = 5;

    private static final int ITERATIONS = 50;

    @Test
    public void testImpersonatingMitmManagerPerformance() {
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(RootCertificateGenerator.builder()
                        .keyGenerator(rootCertKeyGen)
                        .messageDigest(rootCertDigest)
                        .build())
                .serverKeyGenerator(serverCertKeyGen)
                .serverMessageDigest(serverCertDigest)
                .build();

        final AtomicInteger iteration = new AtomicInteger();

        SSLSession mockSession = Mockito.mock(SSLSession.class);

        log.info("Test parameters:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);

        // warm up, init root cert, etc.
        log.info("Executing {} warm up iterations", WARM_UP_ITERATIONS);
        for (iteration.set(0); iteration.get() < WARM_UP_ITERATIONS; iteration.incrementAndGet()) {
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "https://warmup-" + iteration.get() + ".com");
            mitmManager.clientSslEngineFor(request, mockSession);
        }

        log.info("Executing {} performance test iterations", ITERATIONS);

        long start = System.currentTimeMillis();

        for (iteration.set(0); iteration.get() < ITERATIONS; iteration.incrementAndGet()) {
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "https://" + iteration.get() + ".com");
            mitmManager.clientSslEngineFor(request, mockSession);
        }

        long finish = System.currentTimeMillis();

        log.info("Finished performance test:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);
        log.info("Generated {} certificates in {}ms. Average time per certificate: {}ms", iteration.get(), finish - start, (finish - start) / iteration.get());
    }

    @Test
    public void testServerCertificateCreationAndAssembly() {
        CertificateAndKey rootCert = RootCertificateGenerator.builder()
                .keyGenerator(rootCertKeyGen)
                .messageDigest(rootCertDigest)
                .build()
                .load();

        log.info("Test parameters:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);

        log.info("Executing {} warm up iterations", WARM_UP_ITERATIONS);
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            KeyPair serverCertKeyPair = serverCertKeyGen.generate();
            CertificateAndKey serverCert = new BouncyCastleSecurityProviderTool().createServerCertificate(
                    createCertificateInfo("warnmup-" + i + ".com"),
                    rootCert.getCertificate(),
                    rootCert.getPrivateKey(),
                    serverCertKeyPair,
                    serverCertDigest);

            new DefaultSecurityProviderTool().createServerKeyStore(MitmConstants.DEFAULT_KEYSTORE_TYPE, serverCert, rootCert.getCertificate(), "alias", "password");
        }

        log.info("Executing {} performance test iterations", ITERATIONS);

        long start = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            KeyPair serverCertKeyPair = serverCertKeyGen.generate();
            CertificateAndKey serverCert = new BouncyCastleSecurityProviderTool().createServerCertificate(
                    createCertificateInfo(i + ".com"),
                    rootCert.getCertificate(),
                    rootCert.getPrivateKey(),
                    serverCertKeyPair,
                    serverCertDigest);

            new DefaultSecurityProviderTool().createServerKeyStore(MitmConstants.DEFAULT_KEYSTORE_TYPE, serverCert, rootCert.getCertificate(), "alias", "password");
        }

        long finish = System.currentTimeMillis();

        log.info("Finished performance test:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);
        log.info("Assembled {} Key Stores in {}ms. Average time per Key Store: {}ms", ITERATIONS, finish - start, (finish - start) / ITERATIONS);
    }

    private static CertificateInfo createCertificateInfo(String hostname) {
        return new CertificateInfo().commonName(hostname).notBefore(new Date()).notAfter(new Date());
    }
}
