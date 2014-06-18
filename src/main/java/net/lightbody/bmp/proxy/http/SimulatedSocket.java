package net.lightbody.bmp.proxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;

import org.java_bandwidthlimiter.StreamManager;

/**
 * We extends socket in order to get some timings from connection process
 * 
 * we just need to override methods for connect to get some metrics
 * and get-in-out streams to provide throttling
 */
public class SimulatedSocket extends Socket {
    private StreamManager streamManager;

    public SimulatedSocket(StreamManager streamManager) {
        this.streamManager = streamManager;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        Date start = new Date();
        super.connect(endpoint);
   		Date end = new Date();
   		// we simulate latency if necessary
   		simulateLatency(start, end, streamManager);
    }
    
    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        Date start = new Date();
        super.connect(endpoint, timeout);
        Date end = new Date();
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
    
    private void simulateLatency (Date start, Date end, StreamManager streamManager) {
    	System.out.println("simulate latency");
		// the end before adding latency
        Date realEnd = end;
		long connectReal = end.getTime() - start.getTime();
		 
		// add latency
		if(connectReal < streamManager.getLatency()){
			try {
				Thread.sleep(streamManager.getLatency()-connectReal);
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
			// the end after adding latency
		    end = new Date();
		}
		// set real latency time
		RequestInfo.get().latency(start, realEnd);
		// set connect time
		RequestInfo.get().connect(start, end);
	}
}