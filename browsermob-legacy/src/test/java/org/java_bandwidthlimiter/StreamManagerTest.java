package org.java_bandwidthlimiter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class StreamManagerTest {
    private final int BUFFER = 256;
    private final long LIMIT_KB = 8;
    private byte[] data;
            
    @Before
    public void setUp(){
        data = new byte[10*1024];
        for(int i = 0; i < data.length; i++){
            data[i] = 127;
        }        
    }
        
    @Test
    public void testDownstreamDataLimit(){        
        StreamManager sm = new StreamManager( BandwidthLimiter.OneMbps );
        sm.setDownstreamMaxKB(LIMIT_KB);
        sm.enable();        
        InputStream in = sm.registerStream(new ByteArrayInputStream(data));
        byte[] buffer = new byte[BUFFER];
        int read = 0;
        try{
            while(read < data.length){
                read += in.read(buffer);                
            }
            
            fail();
            
        }catch(IOException ex){            
            assertThat(ex, instanceOf(MaximumTransferExceededException.class));
            
            MaximumTransferExceededException ex2 = (MaximumTransferExceededException)ex;
            
            assertFalse(ex2.isUpstream());            
            assertEquals(LIMIT_KB, ex2.getLimit());            
            assertTrue(read < LIMIT_KB*1000 + BUFFER);
            assertTrue(read >= LIMIT_KB*1000 - BUFFER);
        }
    }
    
    @Test
    public void testUpstreamDataLimit(){        
        StreamManager sm = new StreamManager( BandwidthLimiter.OneMbps );
        sm.setUpstreamMaxKB(LIMIT_KB);
        sm.enable();        
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out1 = new ByteArrayOutputStream(data.length);
        OutputStream out = sm.registerStream(out1);
        byte[] buffer = new byte[BUFFER];
        int read = 0;
        try{
            while(read < data.length){
                read += in.read(buffer);
                out.write(buffer);
            }
            
            fail();
            
        }catch(IOException ex){            
            assertThat(ex, instanceOf(MaximumTransferExceededException.class));
            
            MaximumTransferExceededException ex2 = (MaximumTransferExceededException)ex;
            
            assertTrue(ex2.isUpstream());            
            assertEquals(LIMIT_KB, ex2.getLimit());            
            assertTrue(out1.size() < LIMIT_KB*1000 + BUFFER);
            assertTrue(out1.size() >= LIMIT_KB*1000 - BUFFER);
        }
    }
    
}
