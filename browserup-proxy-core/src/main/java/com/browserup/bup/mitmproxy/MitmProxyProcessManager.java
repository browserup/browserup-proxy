package com.browserup.bup.mitmproxy;

import com.browserup.bup.mitmproxy.addons.*;
import com.browserup.bup.mitmproxy.management.*;
import org.apache.commons.lang3.SystemUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MitmProxyProcessManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MitmProxyProcessManager.class);
  private static final String MITMPROXY_BINARY_PATH_PROPERTY = "MITMPROXY_BINARY_PATH";
  private static final String MITMPROXY_HOME_PATH = System.getProperty("user.home") + "/.browserup-mitmproxy";
  private static final String MITMPROXY_DEFAULT_BINARY_PATH = MITMPROXY_HOME_PATH + "/" + getMitmproxyBinaryFileName();

  public enum MitmProxyLoggingLevel {
    error,
    warn,
    info,
    alert,
    debug
  }

  private final int addonsManagerApiPort = NetworkUtils.getFreePort();
  private Process startedProcess = null;

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

  private static String getMitmproxyBinaryFileName() {
    return SystemUtils.IS_OS_WINDOWS ? "mitmdump.exe" : "mitmdump";
  }

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
      process.children().forEach(ProcessHandle::destroy);
      startedProcess.destroy();
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !startedProcess.isAlive());
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
    List<String> command = createCommand(port, addons);

    LOGGER.info("Starting proxy using command: " + String.join(" ", command));

    String logMessageFormat = "MitmProxy[" + this.proxyPort + "]: {}";

    try {
      startedProcess = new ProcessBuilder(command).redirectErrorStream(true).start();
      new StreamGobbler(startedProcess.getInputStream(), (line) -> {
        LOGGER.debug(logMessageFormat, line);
        proxyLog.append(line).append("\n");
      }, (ioException) -> {
        LOGGER.error("", ioException);
      }).start();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          stop();
        }
        catch (Throwable t) {
          LOGGER.error("Unable to terminate process during process shutdown");
        }
      }));
    } catch (Exception ex) {
      throw new RuntimeException("Couldn't start mitmproxy process", ex);
    }
    waitForReady();
  }

  private void waitForReady() {
    try {
      Awaitility.await()
              .atMost(5, TimeUnit.SECONDS)
              .until(this.proxyManager::callHealthCheck);
    } catch (ConditionTimeoutException ex) {
      handleHealthCheckFailure();
    }
  }

  @NotNull
  private ArrayList<String> createCommand(int port, List<AbstractAddon> addons) {
    ArrayList<String> command = new ArrayList<String>() {{
      add(getMitmproxyBinaryPath());
      add("-p");
      add(String.valueOf(port));
      add("--set");
      add("confdir=" + MITMPROXY_HOME_PATH);
    }};
    if (trustAll) {
      command.add("--ssl-insecure");
    }

    updateCommandWithUpstreamProxy(command);
    updateCommandWithLogLevel(command);
    updateCommandWithAddOns(addons, command);
    return command;
  }

  private String getMitmproxyBinaryPath() {
    String mitmproxyBinaryPathProperty = System.getProperty(MITMPROXY_BINARY_PATH_PROPERTY);
    if (mitmproxyBinaryPathProperty != null) {
      return mitmproxyBinaryPathProperty + "/" + getMitmproxyBinaryFileName();
    }
    return MITMPROXY_DEFAULT_BINARY_PATH;
  }

  private void handleHealthCheckFailure() {
    LOGGER.error("MitmProxy might not started properly, healthcheck failed for port: " + this.proxyPort);
    if (startedProcess == null) return;

    if (startedProcess.isAlive()) {
      LOGGER.error("MitmProxy's healthcheck failed but process is alive, killing mitmproxy process...");
      startedProcess.destroyForcibly();
      try {
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !startedProcess.isAlive());
        LOGGER.info("MitmProxy process was killed successfully.");
      } catch (ConditionTimeoutException ex2) {
        LOGGER.error("Didn't manage to kill MitmProxy in time, throwing error");
        throw new RuntimeException("Couldn't kill mitmproxy in time", ex2);
      }
    }
    if (!startedProcess.isAlive() && startedProcess.exitValue() > 0) {
      Throwable cause = null;
      if (proxyLog.toString().contains("Address already in use")) {
        cause = new BindException();
      }
      throw new RuntimeException(
              "Couldn't start mitmproxy process on port: " + this.proxyPort +
                      ", exit with code: " + startedProcess.exitValue(), cause);
    }
  }

  private void updateCommandWithAddOns(List<AbstractAddon> addons, List<String> command) {
    addons.forEach(addon -> command.addAll(Arrays.asList(addon.getCommandParams())));
  }

  private void updateCommandWithLogLevel(List<String> command) {
    MitmProxyLoggingLevel logLevel = getMitmProxyLoggingLevel();
    command.add("--set");
    command.add("termlog_verbosity=" + logLevel);
    if (logLevel.equals(MitmProxyLoggingLevel.debug)) {
      command.add("--set");
      command.add("flow_detail=3");
    }
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

  private static class StreamGobbler extends Thread {
    private final InputStream inputStream;
    private final Consumer<String> logger;
    private final Consumer<IOException> errorHandler;

    private StreamGobbler(InputStream inputStream, Consumer<String> logger, Consumer<IOException> errorHandler) {
      this.inputStream = inputStream;
      this.logger = logger;
      this.errorHandler = errorHandler;
    }

    @Override
    public void run() {
      try {
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null)
          logger.accept(line);
      }
      catch (IOException ioException) {
        errorHandler.accept(ioException);
      }
    }
  }
}
