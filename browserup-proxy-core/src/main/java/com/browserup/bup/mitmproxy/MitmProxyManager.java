package com.browserup.bup.mitmproxy;

import com.browserup.bup.mitmproxy.addons.*;
import com.browserup.bup.mitmproxy.management.*;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MitmProxyManager {
  public static final int ADDONS_MANAGER_API_PORT = 8088;
  private static final Logger LOGGER = LoggerFactory.getLogger(MitmProxyManager.class);

  private StartedProcess startedProcess = null;

  private HarCaptureAddOn harCaptureFilterAddOn = new HarCaptureAddOn();
  private ProxyManagerAddOn proxyManagerAddOn = new ProxyManagerAddOn();
  private AddonsManagerAddOn addonsManagerAddOn = new AddonsManagerAddOn(ADDONS_MANAGER_API_PORT);
  private WhiteListAddOn whiteListAddOn = new WhiteListAddOn();
  private BlackListAddOn blackListAddOn = new BlackListAddOn();
  private AuthBasicAddOn authBasicFilterAddOn = new AuthBasicAddOn();
  private AdditionalHeadersAddOn additionalHeadersAddOn = new AdditionalHeadersAddOn();
  private RewriteUrlAddOn rewriteUrlAddOn = new RewriteUrlAddOn();
  private LatencyAddOn latencyAddOn = new LatencyAddOn();

  private AddonsManagerClient addonsManagerClient = new AddonsManagerClient(ADDONS_MANAGER_API_PORT);

  private HarCaptureManager harCaptureFilterManager = new HarCaptureManager(addonsManagerClient, this);
  private ProxyManager proxyManager = new ProxyManager(addonsManagerClient, this);
  private WhiteListManager whiteListManager = new WhiteListManager(addonsManagerClient, this);
  private BlackListManager blackListManager = new BlackListManager(addonsManagerClient, this);
  private AuthBasicManager authBasicFilterManager = new AuthBasicManager(addonsManagerClient, this);
  private AdditionalHeadersManager additionalHeadersManager = new AdditionalHeadersManager(addonsManagerClient, this);
  private RewriteUrlManager rewriteUrlManager = new RewriteUrlManager(addonsManagerClient, this);
  private LatencyManager latencyManager = new LatencyManager(addonsManagerClient, this);

  private Integer proxyPort = 0;

  private PipedInputStream pipedInputStream;

  private boolean isRunning = false;
  private boolean trustAll = false;

  private MitmProxyManager() {}

  public static MitmProxyManager getInstance() {
    return new MitmProxyManager();
  }

  public void start(int port) {
    try {
      //startProxy(port);
      this.isRunning = true;
      this.proxyPort = 8443;
      harCaptureFilterManager.setHarCaptureTypes(harCaptureFilterManager.getLastCaptureTypes());
      authBasicFilterManager.getCredentials().forEach((key, value) -> authBasicFilterManager.authAuthorization(key, value));
      additionalHeadersManager.addHeaders(additionalHeadersManager.getAllHeaders());
      rewriteUrlManager.rewriteUrls(rewriteUrlManager.getRewriteRulesMap());
      latencyManager.setLatency(latencyManager.getLatencyMs(), TimeUnit.MILLISECONDS);
      proxyManager.setConnectionIdleTimeout(proxyManager.getConnectionIdleTimeoutSeconds());
    } catch (Exception ex) {
      LOGGER.error("Failed to start proxy", ex);
      stop();
      throw ex;
    }
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public boolean isRunning() {
    return isRunning;
  }

  public void stop() {
    this.isRunning = false;
//
//    try {
//      pipedInputStream.close();
//    } catch (IOException e) {
//      LOGGER.warn("Couldn't close piped input stream", e);
//    }
//    startedProcess.getProcess().destroy();
//    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(this::isProxyPortFreed);
//    try {
//      Thread.sleep(100);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
  }

  private boolean isProxyPortFreed() {
    Socket s = null;
    try {
      s = new Socket("localhost", proxyPort);
      return false;
    } catch (IOException e) {
      return true;
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (IOException e) {
          throw new RuntimeException("Couldn't check port availability", e);
        }
      }
    }
  }

  public void setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
  }

  private void startProxy(int port) {
    List<String> command = new ArrayList<String>() {{
      add("mitmdump");
      add("-p");
      add(String.valueOf(port));
    }};
    if (trustAll) {
      command.add("--ssl-insecure");
    }

    command.addAll(Arrays.asList(rewriteUrlAddOn.getCommandParams()));
    command.addAll(Arrays.asList(harCaptureFilterAddOn.getCommandParams()));
    command.addAll(Arrays.asList(addonsManagerAddOn.getCommandParams()));
    command.addAll(Arrays.asList(proxyManagerAddOn.getCommandParams()));
    command.addAll(Arrays.asList(whiteListAddOn.getCommandParams()));
    command.addAll(Arrays.asList(blackListAddOn.getCommandParams()));
    command.addAll(Arrays.asList(authBasicFilterAddOn.getCommandParams()));
    command.addAll(Arrays.asList(additionalHeadersAddOn.getCommandParams()));
    command.addAll(Arrays.asList(latencyAddOn.getCommandParams()));

    LOGGER.info("Starting proxy using command: " + String.join(" ", command));

    this.pipedInputStream = new PipedInputStream();
    ProcessExecutor processExecutor;
    try {
      processExecutor = new ProcessExecutor(command)
          .readOutput(true)
          .redirectOutput(Slf4jStream.ofCaller().asInfo())
          .redirectOutputAlsoTo(new PipedOutputStream(pipedInputStream));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      startedProcess = processExecutor.start();
    } catch (Exception ex) {
      throw new RuntimeException("Couldn't start mitmproxy process", ex);
    }

    StringBuilder output = new StringBuilder();
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
    readOutputOfMimtproxy(pipedInputStream, output);

    try {
      Awaitility.await().
          atMost(10, TimeUnit.SECONDS).
          until(() -> output.toString().contains("Proxy server listening"));
    } catch (ConditionTimeoutException ex) {
      LOGGER.error("MitmProxy haven't started properly, output: " + output);
      throw new RuntimeException("Mitmproxy haven't started properly, output: " + output);
    }
  }

  private void readOutputOfMimtproxy(PipedInputStream pipedInputStream, StringBuilder output) {
    new Thread(() -> {
      BufferedReader reader = new BufferedReader(new InputStreamReader(pipedInputStream));
      Awaitility.await().atMost(5, TimeUnit.SECONDS).until(reader::ready);

      while (true) {
        try {
          if (!reader.ready())
            break;
          output.append(reader.readLine());
        } catch (IOException e) {
          LOGGER.error("Error while reading output of mitmproxy", e);
        }
      }
    }).start();
  }

  public HarCaptureManager getHarCaptureFilterManager() {
    return harCaptureFilterManager;
  }

  public ProxyManager getProxyManager() {
    return proxyManager;
  }

  public WhiteListManager getWhiteListManager() {
    return whiteListManager;
  }

  public BlackListManager getBlackListManager() {
    return blackListManager;
  }

  public AuthBasicManager getAuthBasicFilterManager() {
    return authBasicFilterManager;
  }

  public AdditionalHeadersManager getAdditionalHeadersManager() {
    return additionalHeadersManager;
  }

  public RewriteUrlManager getRewriteUrlManager() {
    return rewriteUrlManager;
  }

  public LatencyManager getLatencyManager() {
    return latencyManager;
  }
}
