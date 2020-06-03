package com.browserup.bup.mitmproxy;

import com.browserup.bup.mitmproxy.addons.*;
import com.browserup.bup.mitmproxy.management.*;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MitmProxyProcessManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MitmProxyProcessManager.class);

  private final int addonsManagerApiPort = NetworkUtils.getFreePort();
  private StartedProcess startedProcess = null;

  private HarCaptureAddOn harCaptureFilterAddOn = new HarCaptureAddOn();
  private ProxyManagerAddOn proxyManagerAddOn = new ProxyManagerAddOn();
  private AddonsManagerAddOn addonsManagerAddOn = new AddonsManagerAddOn(addonsManagerApiPort);
  private WhiteListAddOn whiteListAddOn = new WhiteListAddOn();
  private BlackListAddOn blackListAddOn = new BlackListAddOn();
  private AuthBasicAddOn authBasicFilterAddOn = new AuthBasicAddOn();
  private AdditionalHeadersAddOn additionalHeadersAddOn = new AdditionalHeadersAddOn();
  private HttpConnectCaptureAddOn httpConnectCaptureAddOn = new HttpConnectCaptureAddOn();
  private RewriteUrlAddOn rewriteUrlAddOn = new RewriteUrlAddOn();
  private LatencyAddOn latencyAddOn = new LatencyAddOn();

  private AddonsManagerClient addonsManagerClient = new AddonsManagerClient(addonsManagerApiPort);

  private HarCaptureManager harCaptureFilterManager = new HarCaptureManager(addonsManagerClient, this);
  private ProxyManager proxyManager = new ProxyManager(addonsManagerClient, this);
  private WhiteListManager whiteListManager = new WhiteListManager(addonsManagerClient, this);
  private BlackListManager blackListManager = new BlackListManager(addonsManagerClient, this);
  private AuthBasicManager authBasicFilterManager = new AuthBasicManager(addonsManagerClient, this);
  private AdditionalHeadersManager additionalHeadersManager = new AdditionalHeadersManager(addonsManagerClient, this);
  private RewriteUrlManager rewriteUrlManager = new RewriteUrlManager(addonsManagerClient, this);
  private LatencyManager latencyManager = new LatencyManager(addonsManagerClient, this);

  private Integer proxyPort = 0;

  private boolean isRunning = false;
  private boolean trustAll = false;

  private StringBuilder proxyLog = new StringBuilder();

  public void start(int port) {
    start(port == 0 ? NetworkUtils.getFreePort() : port, defaultAddons());
  }

  public void start(int port, List<AbstractAddon> addons) {
    try {
      this.proxyPort = port;

      startProxy(port, addons);

      this.isRunning = true;

      if (!addons.isEmpty()) {
        configureProxy();
      }

    } catch (Exception ex) {
      LOGGER.error("Failed to start proxy", ex);
      stop();
      throw ex;
    }
  }

  private void configureProxy() {
    harCaptureFilterManager.setHarCaptureTypes(harCaptureFilterManager.getLastCaptureTypes());
    authBasicFilterManager.getCredentials().forEach((key, value) -> authBasicFilterManager.authAuthorization(key, value));
    additionalHeadersManager.addHeaders(additionalHeadersManager.getAllHeaders());
    rewriteUrlManager.rewriteUrls(rewriteUrlManager.getRewriteRulesMap());
    latencyManager.setLatency(latencyManager.getLatencyMs(), TimeUnit.MILLISECONDS);
    proxyManager.setConnectionIdleTimeout(proxyManager.getConnectionIdleTimeoutSeconds());
    proxyManager.setDnsResolvingDelayMs(proxyManager.getDnsResolutionDelayMs());
    proxyManager.setChainedProxyAuthorization(proxyManager.getUpstreamProxyCredentials());
    proxyManager.setChainedProxyNonProxyHosts(proxyManager.getUpstreamNonProxyHosts());
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public boolean isRunning() {
    return isRunning;
  }

  public void stop() {
    this.isRunning = false;

    Process process = startedProcess.getProcess();
    process.destroyForcibly();

    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !process.isAlive());
  }

  public void setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
  }

  private List<AbstractAddon> defaultAddons() {
    AbstractAddon[] addonsArray = new AbstractAddon[]{
            rewriteUrlAddOn,
            httpConnectCaptureAddOn,
            harCaptureFilterAddOn,
            addonsManagerAddOn,
            proxyManagerAddOn,
            whiteListAddOn,
            blackListAddOn,
            authBasicFilterAddOn,
            additionalHeadersAddOn,
            latencyAddOn,
    };
    return Arrays.asList(addonsArray);
  }

  private void startProxy(int port, List<AbstractAddon> addons) {
    List<String> command = new ArrayList<String>() {{
      add("mitmdump");
      add("-p");
      add(String.valueOf(port));
    }};
    if (trustAll) {
      command.add("--ssl-insecure");
    }

    InetSocketAddress upstreamProxyAddress = proxyManager.getUpstreamProxyAddress();
    if (upstreamProxyAddress != null) {
      String schema = "http";
      if (proxyManager.isUseHttpsUpstreamProxy()) {
        schema = "https";
      }
      command.add("--mode");
      command.add("upstream:" + schema + "://" + upstreamProxyAddress.getHostName() + ":" + upstreamProxyAddress.getPort());
    }

    addons.forEach(addon -> command.addAll(Arrays.asList(addon.getCommandParams())));

    LOGGER.info("Starting proxy using command: " + String.join(" ", command));

    ProcessExecutor processExecutor = new ProcessExecutor(command)
            .readOutput(true)
            .redirectOutput(Slf4jStream.ofCaller().asInfo())
            .redirectOutput(new LogOutputStream() {
              @Override
              protected void processLine(String line) {
                proxyLog.append(line).append("\n");
              }
            });

    try {
      startedProcess = processExecutor.start();
    } catch (Exception ex) {
      throw new RuntimeException("Couldn't start mitmproxy process", ex);
    }

    try {
      Awaitility.await()
              .atMost(5, TimeUnit.SECONDS)
              .until(this.proxyManager::callHealthCheck);
    } catch (ConditionTimeoutException ex) {
      LOGGER.error("MitmProxy might not started properly, healthcheck failed for port: " + this.proxyPort);
      if (startedProcess != null && startedProcess.getProcess().exitValue() > 0) {
        Throwable cause = null;
        if (proxyLog.toString().contains("Address already in use")) {
          cause = new java.net.BindException();
        }
        throw new RuntimeException("Couldn't start mitmproxy process on port: " + this.proxyPort + ", exit with code: " + startedProcess.getProcess().exitValue(), cause);
      }
    }
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

    public int getAddonsManagerApiPort() {
        return addonsManagerApiPort;
    }
}
