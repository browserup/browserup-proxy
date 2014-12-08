package net.lightbody.bmp.proxy.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClonedInputStream extends InputStream {
    private InputStream is;
    private ByteArrayOutputStream os = new ByteArrayOutputStream();

    public ClonedInputStream(InputStream is) {
        this.is = is;
    }

    public int read() throws IOException {
        int byteRead = is.read();
        if (byteRead > -1) {
        	os.write(byteRead);
        }

        return byteRead;
    }

    public int read(byte[] b) throws IOException {
        int respLen = is.read(b);
        if (respLen > 0) {
        	os.write(b, 0, respLen);
        }

        return respLen;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int respLen = is.read(b, off, len);
        if (respLen > 0) {
        	os.write(b, off, respLen);
        }

        return respLen;
    }

    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    public int available() throws IOException {
        return is.available();
    }

    public void close() throws IOException {
        os.close();
        is.close();
    }

    public void mark(int readlimit) {
        is.mark(readlimit);
    }

    public void reset() throws IOException {
        os.reset();
        is.reset();
    }

    public boolean markSupported() {
        return is.markSupported();
    }

    public ByteArrayOutputStream getOutput() {
        return os;
    }
}
