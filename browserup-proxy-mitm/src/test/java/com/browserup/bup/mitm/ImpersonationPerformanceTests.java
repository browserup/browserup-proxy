/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.mitm;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import com.browserup.bup.mitm.keys.ECKeyGenerator;
import com.browserup.bup.mitm.keys.KeyGenerator;
import com.browserup.bup.mitm.keys.RSAKeyGenerator;
import com.browserup.bup.mitm.manager.ImpersonatingMitmManager;
import com.browserup.bup.mitm.tools.BouncyCastleSecurityProviderTool;
import com.browserup.bup.mitm.tools.DefaultSecurityProviderTool;
import com.browserup.bup.mitm.util.MitmConstants;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
        IntStream.range(0, WARM_UP_ITERATIONS).forEach(i -> {
            KeyPair serverCertKeyPair = serverCertKeyGen.generate();
            CertificateAndKey serverCert = new BouncyCastleSecurityProviderTool().createServerCertificate(
                    createCertificateInfo("warnmup-" + i + ".com"),
                    rootCert.getCertificate(),
                    rootCert.getPrivateKey(),
                    serverCertKeyPair,
                    serverCertDigest);
            new DefaultSecurityProviderTool().createServerKeyStore(MitmConstants.DEFAULT_KEYSTORE_TYPE, serverCert, rootCert.getCertificate(), "alias", "password");
        });

        log.info("Executing {} performance test iterations", ITERATIONS);

        long start = System.currentTimeMillis();

        IntStream.range(0, ITERATIONS).forEach(i -> {
            KeyPair serverCertKeyPair = serverCertKeyGen.generate();
            CertificateAndKey serverCert = new BouncyCastleSecurityProviderTool().createServerCertificate(
                    createCertificateInfo(i + ".com"),
                    rootCert.getCertificate(),
                    rootCert.getPrivateKey(),
                    serverCertKeyPair,
                    serverCertDigest);
            new DefaultSecurityProviderTool().createServerKeyStore(MitmConstants.DEFAULT_KEYSTORE_TYPE, serverCert, rootCert.getCertificate(), "alias", "password");
        });

        long finish = System.currentTimeMillis();

        log.info("Finished performance test:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);
        log.info("Assembled {} Key Stores in {}ms. Average time per Key Store: {}ms", ITERATIONS, finish - start, (finish - start) / ITERATIONS);
    }

    private static CertificateInfo createCertificateInfo(String hostname) {
        return new CertificateInfo().commonName(hostname).notBefore(Instant.now()).notAfter(Instant.now());
    }
}
