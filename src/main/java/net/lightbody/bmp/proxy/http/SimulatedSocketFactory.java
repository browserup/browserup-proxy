package net.lightbody.bmp.proxy.http;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.http.HttpHost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.java_bandwidthlimiter.StreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatedSocketFactory implements ConnectionSocketFactory {
	private static final int DEFAULT_SOCKET_TIMEOUT = 2000;
    private static Logger LOG = LoggerFactory.getLogger(SimulatedSocketFactory.class);

    private StreamManager streamManager;

    public SimulatedSocketFactory(StreamManager streamManager) {
        super();
        assert streamManager != null;
        this.streamManager = streamManager;
    }

    public static <T extends Socket> void configure(T sock) {
        // Configure the socket to be Load Test Friendly!
        // If we don't set these, we can easily use up too many sockets, even when we're cleaning/closing the sockets
        // responsibly. The reason is that they will stick around in TIME_WAIT for some time (ie: 1-4 minutes) and once
        // they get to 64K (on Linux) or 16K (on Mac) we can't make any more requests. While those limits can be raised
        // with a configuration setting in the OS, we really don't need to change things globally. We just need to make
        // sure that when we close a socket it gets ditched right away and doesn't stick around in TIME_WAIT.
        //
        // This problem is most easily noticable/problematic for load tests that use a single transaction to issue
        // one HTTP request and then end the transaction, thereby shutting down the HTTP socket. This can easily create
        // 64K+ sockets in TIME_WAIT state, preventing any other requests from going out and producing a false-negative
        // "connection refused" error message.
        //
        // For further reading, check out HttpClient's FAQ on this subject:
        // http://wiki.apache.org/HttpComponents/FrequentlyAskedConnectionManagementQuestions
        try {
            sock.setReuseAddress(true);
            sock.setSoLinger(true, 0);
        } catch (Exception e) {}
    }


	@Override
	public Socket createSocket(HttpContext context) throws IOException {
        Socket newSocket = new SimulatedSocket(streamManager);
        SimulatedSocketFactory.configure(newSocket);
		return newSocket;
	}

    /**
     * Prevent unnecessary class inspection at runtime.
     */
    private static Method getHostMethod;
    static {
        try {
            getHostMethod = InetSocketAddress.class.getDeclaredMethod("getHostString", new Class[]{});
            if (!Modifier.isPublic(getHostMethod.getModifiers())) {
                getHostMethod = null;
            }
        } catch (Exception e) {
            // ok to ignore, try the fall back
        }

        if (getHostMethod == null) {
            try {
                getHostMethod = InetSocketAddress.class.getDeclaredMethod("getHostName", new Class[]{});
                LOG.warn("Using InetSocketAddress.getHostName() rather than InetSocketAddress.getHostString(). Consider upgrading to Java 7 for faster performance!");
            } catch (NoSuchMethodException e) {
                String msg = "Something is wrong inside SimulatedSocketFactory and I don't know why!";
                LOG.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        getHostMethod.setAccessible(true);
    }

    /**
     * A minor optimization to prevent possible host resolution when inspecting a InetSocketAddress for a hostname....
     *
     * @param remoteAddress
     * @return
     * @throws IOException
     */
    private String resolveHostName(InetSocketAddress remoteAddress) {
        String hostString = null;
        try {
            hostString = (String) getHostMethod.invoke(remoteAddress, new Object[]{});
        } catch (InvocationTargetException ite) {
            throw new RuntimeException("Expecting InetSocketAddress to have a package scoped \"getHostString\" method which returns a String and takes no input");
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("Expecting InetSocketAddress to have a package scoped \"getHostString\" method which returns a String and takes no input");
        }
        return hostString;
    }

    @Override
	public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
    	if (remoteAddress == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }

        if (sock == null) {
            sock = createSocket(context);
        }

        if ((localAddress != null) ) {
            sock.bind( localAddress );
        }

        String hostName = resolveHostName(remoteAddress);

        InetSocketAddress remoteAddr = remoteAddress;
        if (host != null) {
            remoteAddr = new InetSocketAddress(hostName, remoteAddress.getPort());
        }

        try {
            sock.connect(remoteAddr, connectTimeout);
        } catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress + " timed out");
        }
        return sock;
    }
    
    public Socket connectSocket(Socket sock, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params) throws IOException {
        return this.connectSocket(DEFAULT_SOCKET_TIMEOUT, sock, null, remoteAddress, localAddress, new BasicHttpContext());
    }
}