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
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MitmProxyProcessManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MitmProxyProcessManager.class);

  public enum MitmProxyLoggingLevel {
    error,
    warn,
    info,
    alert,
    debug
  }

  private final int addonsManagerApiPort = NetworkUtils.getFreePort();
  private StartedProcess startedProcess = null;

  private HarCaptureAddOn harCaptureFilterAddOn = new HarCaptureAddOn();
  private ProxyManagerAddOn proxyManagerAddOn = new ProxyManagerAddOn();
  private AddonsManagerAddOn addonsManagerAddOn = new AddonsManagerAddOn(addonsManagerApiPort);
  private AllowListAddOn allowListAddOn = new AllowListAddOn();
  private BlockListAddOn blockListAddOn = new BlockListAddOn();
  private AuthBasicAddOn authBasicFilterAddOn = new AuthBasicAddOn();
  private AdditionalHeadersAddOn additionalHeadersAddOn = new AdditionalHeadersAddOn();
  private HttpConnectCaptureAddOn httpConnectCaptureAddOn = new HttpConnectCaptureAddOn();
  private RewriteUrlAddOn rewriteUrlAddOn = new RewriteUrlAddOn();
  private LatencyAddOn latencyAddOn = new LatencyAddOn();
  private InitFlowAddOn initFlowAddOn = new InitFlowAddOn();

  private AddonsManagerClient addonsManagerClient = new AddonsManagerClient(addonsManagerApiPort);

  private HarCaptureManager harCaptureFilterManager = new HarCaptureManager(addonsManagerClient, this);
  private ProxyManager proxyManager = new ProxyManager(addonsManagerClient, this);
  private AllowListManager allowListManager = new AllowListManager(addonsManagerClient, this);
  private BlockListManager blockListManager = new BlockListManager(addonsManagerClient, this);
  private AuthBasicManager authBasicFilterManager = new AuthBasicManager(addonsManagerClient, this);
  private AdditionalHeadersManager additionalHeadersManager = new AdditionalHeadersManager(addonsManagerClient, this);
  private RewriteUrlManager rewriteUrlManager = new RewriteUrlManager(addonsManagerClient, this);
  private LatencyManager latencyManager = new LatencyManager(addonsManagerClient, this);

  private Integer proxyPort = 0;

  private boolean isRunning = false;
  private boolean trustAll = false;
  private MitmProxyLoggingLevel mitmProxyLoggingLevel = MitmProxyLoggingLevel.info;

  private StringBuilder proxyLog = new StringBuilder();

  public void start(int port) {
    start(port == 0 ? NetworkUtils.getFreePort() : port, defaultAddons());
  }

  public void start(int port, List<AbstractAddon> addons) {
    try {
      this.proxyPort = port;

      startProxyWithRetries(port, addons, 3);

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

  public MitmProxyLoggingLevel getMitmProxyLoggingLevel() {
    return mitmProxyLoggingLevel;
  }

  public void setMitmProxyLoggingLevel(MitmProxyLoggingLevel mitmProxyLoggingLevel) {
    this.mitmProxyLoggingLevel = mitmProxyLoggingLevel;
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

    if (startedProcess != null) {
      Process process = startedProcess.getProcess();
      process.children().forEach(ProcessHandle::destroy);
      process.destroy();
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !process.isAlive());
    }
  }

  public void setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
  }

  private List<AbstractAddon> defaultAddons() {
    AbstractAddon[] addonsArray = new AbstractAddon[]{
            initFlowAddOn,
            rewriteUrlAddOn,
            allowListAddOn,
            blockListAddOn,
            httpConnectCaptureAddOn,
            harCaptureFilterAddOn,
            addonsManagerAddOn,
            proxyManagerAddOn,
            authBasicFilterAddOn,
            additionalHeadersAddOn,
            latencyAddOn,
    };
    return Arrays.asList(addonsArray);
  }

  private void startProxyWithRetries(int port, List<AbstractAddon> addons, int retryCount) {
    for (int attempt = 1; attempt <= retryCount; attempt++) {
      try {
        startProxy(port, addons);
        break;
      } catch (Exception ex) {
        // For binding exception not going to retry (let driver to try another port)
        if (ex.getCause() != null && ex.getCause() instanceof BindException) {
          throw ex;
        }
        if (attempt < retryCount) {
          LOGGER.error("Failed to start proxy, attempt: {}, retries count: {}, going to retry...", attempt, retryCount, ex);
        } else {
          LOGGER.error("Failed to start proxy, no retries left, throwing exception", ex);
          throw ex;
        }
      }
    }
  }

  private void startProxy(int port, List<AbstractAddon> addons) {
    List<String> command = new ArrayList<String>() {{
      add("mitmdump");
      add("-p");
      add(String.valueOf(port));
      add("--set");
      add("flow_detail=3");
    }};
    if (trustAll) {
      command.add("--ssl-insecure");
    }

    updateCommandWithUpstreamProxy(command);
    updateCommandWithLogLevel(command);
    updateCommandWithAddOns(addons, command);

    LOGGER.info("Starting proxy using command: " + String.join(" ", command));

    ProcessExecutor processExecutor = createProcessExecutor(command);
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
      handleHealthCheckFailure();
    }
  }

  private void handleHealthCheckFailure() {
    LOGGER.error("MitmProxy might not started properly, healthcheck failed for port: " + this.proxyPort);
    if (startedProcess == null) return;

    if (startedProcess.getProcess().isAlive()) {
      LOGGER.error("MitmProxy's healthcheck failed but process is alive, killing mitmproxy process...");
      startedProcess.getProcess().destroyForcibly();
      try {
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !startedProcess.getProcess().isAlive());
        LOGGER.info("MitmProxy process was killed successfully.");
      } catch (ConditionTimeoutException ex2) {
        LOGGER.error("Didn't manage to kill MitmProxy in time, throwing error");
        throw new RuntimeException("Couldn't kill mitmproxy in time", ex2);
      }
    }
    if (!startedProcess.getProcess().isAlive() && startedProcess.getProcess().exitValue() > 0) {
      Throwable cause = null;
      if (proxyLog.toString().contains("Address already in use")) {
        cause = new BindException();
      }
      throw new RuntimeException(
              "Couldn't start mitmproxy process on port: " + this.proxyPort +
                      ", exit with code: " + startedProcess.getProcess().exitValue(), cause);
    }
  }

  private ProcessExecutor createProcessExecutor(List<String> command) {
    String logPrefix = "MitmProxy[" + this.proxyPort + "]: ";

    return new ProcessExecutor(command)
            .readOutput(true)
            .destroyOnExit()
            .redirectOutput(Slf4jStream.ofCaller().asInfo())
            .redirectOutput(new LogOutputStream() {
              @Override
              protected void processLine(String line) {
                LOGGER.debug(logPrefix + line);
                proxyLog.append(line).append("\n");
              }
            });
  }

  private void updateCommandWithAddOns(List<AbstractAddon> addons, List<String> command) {
    addons.forEach(addon -> command.addAll(Arrays.asList(addon.getCommandParams())));
  }

  private void updateCommandWithLogLevel(List<String> command) {
    command.add("--set");
    command.add("termlog_verbosity=" + getMitmProxyLoggingLevel());
  }

  private void updateCommandWithUpstreamProxy(List<String> command) {
    InetSocketAddress upstreamProxyAddress = proxyManager.getUpstreamProxyAddress();
    if (upstreamProxyAddress != null) {
      String schema = "http";
      if (proxyManager.isUseHttpsUpstreamProxy()) {
        schema = "https";
      }
      command.add("--mode");
      command.add("upstream:" + schema + "://" + upstreamProxyAddress.getHostName() + ":" + upstreamProxyAddress.getPort());
    }
  }

  public HarCaptureManager getHarCaptureFilterManager() {
    return harCaptureFilterManager;
  }

  public ProxyManager getProxyManager() {
    return proxyManager;
  }

  public AllowListManager getAllowListManager() {
    return allowListManager;
  }

  public BlockListManager getBlockListManager() {
    return blockListManager;
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
