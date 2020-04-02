package com.browserup.bup.mitmproxy;

import com.browserup.bup.filters.HttpConnectHarCaptureFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class NetworkUtils {
  private static final Logger log = LoggerFactory
    .getLogger(HttpConnectHarCaptureFilter.class);


  public static int getFreePort() {
    ServerSocket s = null;
    try {
      s = new ServerSocket(0);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't find free port", e);
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (IOException e) {
          //
        }
      }
    }
    return s.getLocalPort();
  }
}
