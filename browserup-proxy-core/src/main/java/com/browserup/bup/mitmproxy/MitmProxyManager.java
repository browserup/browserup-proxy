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
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MitmProxyManager {
  public static final int ADDONS_MANAGER_API_PORT = 8088;
  private static final Logger LOGGER = LoggerFactory.getLogger(MitmProxyManager.class);

  private StartedProcess startedProcess = null;

  private HarCaptureFilterAddOn harCaptureFilterAddOn = new HarCaptureFilterAddOn();
  private ProxyManagerAddOn proxyManagerAddOn = new ProxyManagerAddOn();
  private AddonsManagerAddOn addonsManagerAddOn = new AddonsManagerAddOn(ADDONS_MANAGER_API_PORT);
  private WhiteListAddOn whiteListAddOn = new WhiteListAddOn();
  private BlackListAddOn blackListAddOn = new BlackListAddOn();
  private AuthBasicFilterAddOn authBasicFilterAddOn = new AuthBasicFilterAddOn();

  private AddonsManagerClient addonsManagerClient = new AddonsManagerClient(ADDONS_MANAGER_API_PORT);

  private HarCaptureFilterManager harCaptureFilterManager = new HarCaptureFilterManager(addonsManagerClient, this);
  private ProxyManager proxyManager = new ProxyManager(addonsManagerClient, this);
  private WhiteListManager whiteListManager = new WhiteListManager(addonsManagerClient, this);
  private BlackListManager blackListManager = new BlackListManager(addonsManagerClient, this);
  private AuthBasicFilterManager authBasicFilterManager = new AuthBasicFilterManager(addonsManagerClient, this);

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
      startProxy(port);
      this.isRunning = true;
      this.proxyPort = port;
      harCaptureFilterManager.setHarCaptureTypes(harCaptureFilterManager.getLastCaptureTypes());
      authBasicFilterManager.getCredentials().forEach((key, value) -> authBasicFilterManager.authAuthorization(key, value));
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

    try {
      pipedInputStream.close();
    } catch (IOException e) {
      LOGGER.warn("Couldn't close piped input stream", e);
    }
    startedProcess.getProcess().destroy();
    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(this::isProxyPortFreed);
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

    command.addAll(Arrays.asList(harCaptureFilterAddOn.getCommandParams()));
    command.addAll(Arrays.asList(addonsManagerAddOn.getCommandParams()));
    command.addAll(Arrays.asList(proxyManagerAddOn.getCommandParams()));
    command.addAll(Arrays.asList(whiteListAddOn.getCommandParams()));
    command.addAll(Arrays.asList(blackListAddOn.getCommandParams()));
    command.addAll(Arrays.asList(authBasicFilterAddOn.getCommandParams()));

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

  public HarCaptureFilterManager getHarCaptureFilterManager() {
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

  public AuthBasicFilterManager getAuthBasicFilterManager() {
    return authBasicFilterManager;
  }
}
