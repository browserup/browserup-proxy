package net.lightbody.bmp.proxy.http;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.protocol.HttpContext;
import org.java_bandwidthlimiter.StreamManager;

public class TrustingSSLSocketFactory extends SSLConnectionSocketFactory {

	public enum SSLAlgorithm {
        SSLv3,
        TLSv1
    }

    private static SSLContext sslContext;
    private StreamManager streamManager;

    static {
        TrustManager easyTrustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            @Override
            public void checkServerTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        };
        
        sslContext = SSLContexts.createDefault();
		try {
			sslContext = SSLContexts.custom().loadTrustMaterial(null, 
				new TrustStrategy() {
					@Override
				    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				        return true;
				    }
				}
			).build();
			
			sslContext.init(null, new TrustManager[]{easyTrustManager}, null);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new RuntimeException("Unexpected key management error", e);
		}
    }

    public TrustingSSLSocketFactory(StreamManager streamManager) {
    	this(new AllowAllHostnameVerifier(), streamManager);
    }
    
    public TrustingSSLSocketFactory(X509HostnameVerifier hostnameVerifier, StreamManager streamManager) {
    	 super(sslContext, hostnameVerifier);
         assert streamManager != null;
         this.streamManager = streamManager;
    }
    
    //just an helper function to wrap a normal sslSocket into a simulated one so we can do throttling
    private SSLSocket createSimulatedSocket(SSLSocket socket) {
    	SimulatedSocketFactory.configure(socket);
    	socket.setEnabledProtocols(new String[] { SSLAlgorithm.SSLv3.name(), SSLAlgorithm.TLSv1.name() } );
		socket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5" });
        return new SimulatedSSLSocket(socket, streamManager);
    }
    
    @Override
    public Socket createLayeredSocket(final Socket socket, final String target, final int port, final HttpContext context) throws IOException {
    	SSLSocket sslSocket = (SSLSocket) super.createLayeredSocket(socket, target, port, context);
    	return createSimulatedSocket(sslSocket);
    }
}