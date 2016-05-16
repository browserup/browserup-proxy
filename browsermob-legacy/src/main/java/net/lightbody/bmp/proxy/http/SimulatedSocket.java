package net.lightbody.bmp.proxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import org.java_bandwidthlimiter.StreamManager;

/**
 * We extends socket in order to get some timings from connection process
 * 
 * we just need to override methods for connect to get some metrics
 * and get-in-out streams to provide throttling
 */
public class SimulatedSocket extends Socket {
    private final StreamManager streamManager;

    public SimulatedSocket(StreamManager streamManager) {
        this.streamManager = streamManager;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        long start = System.nanoTime();
        super.connect(endpoint);
   		long end = System.nanoTime();
   		// we simulate latency if necessary
   		simulateLatency(start, end, streamManager);
    }
    
    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        long start = System.nanoTime();
        super.connect(endpoint, timeout);
        long end = System.nanoTime();
        // we simulate latency if necessary
        simulateLatency(start, end, streamManager);
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        // whenever this socket is asked for its input stream
        // we get it ourselves via socket.getInputStream()
        // and register it to the stream manager so it will
        // automatically be throttled
        return streamManager.registerStream(super.getInputStream());
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        // whenever this socket is asked for its output stream
        // we get it ourselves via socket.getOutputStream()
        // and register it to the stream manager so it will
        // automatically be throttled
        return streamManager.registerStream(super.getOutputStream());
    }
    
    private void simulateLatency(long start, long end, StreamManager streamManager) {
		// the end before adding latency
        long realEnd = end;
		long connectReal = end - start;
		 
		// add latency
		if(connectReal < streamManager.getLatency()){
			try {
				Thread.sleep(streamManager.getLatency()-connectReal);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			// the end after adding latency
		    end = System.nanoTime();
		}
		// set real latency time
		RequestInfo.get().latency(start, realEnd);
		// set connect time
		RequestInfo.get().connect(start, end);
	}
}