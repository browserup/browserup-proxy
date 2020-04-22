package com.browserup.bup;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.mitm.TrustSource;
import com.browserup.bup.mitmproxy.MitmProxyManager;
import com.browserup.bup.mitmproxy.NetworkUtils;
import com.browserup.bup.mitmproxy.management.HarCaptureFilterManager;
import com.browserup.bup.proxy.BlacklistEntry;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.proxy.dns.AdvancedHostResolver;
import com.browserup.bup.util.HttpStatusClass;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.MitmManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MitmProxyServer implements BrowserUpProxy {
  private MitmProxyManager mitmProxyManager = MitmProxyManager.getInstance();

  @Override
  public void start() {
    mitmProxyManager.start(NetworkUtils.getFreePort());
  }

  @Override
  public void start(int port) {
    mitmProxyManager.start(port);
  }

  @Override
  public void start(int port, InetAddress bindAddress) {
    mitmProxyManager.start(port);
  }

  @Override
  public void start(int port, InetAddress clientBindAddress, InetAddress serverBindAddress) {
    mitmProxyManager.start(port);
  }

  @Override
  public boolean isStarted() {
    return mitmProxyManager.isRunning();
  }

  @Override
  public void stop() {
    mitmProxyManager.stop();
  }

  @Override
  public void abort() {
    mitmProxyManager.stop();
  }

  @Override
  public InetAddress getClientBindAddress() {
    return null;
  }

  @Override
  public int getPort() {
    return mitmProxyManager.getProxyPort();
  }

  @Override
  public InetAddress getServerBindAddress() {
    return null;
  }

  @Override
  public Har getHar() {
    return getHar(false);
  }

  @Override
  public Har getHar(boolean cleanHar) {
    return mitmProxyManager.getHarCaptureFilterManager().getHar(cleanHar);
  }

  @Override
  public Har newHar() {
    return mitmProxyManager.getHarCaptureFilterManager().newHar();
  }

  @Override
  public Har newHar(String initialPageRef) {
    return mitmProxyManager.getHarCaptureFilterManager().newHar(initialPageRef);
  }

  @Override
  public Har newHar(String initialPageRef, String initialPageTitle) {
    return mitmProxyManager.getHarCaptureFilterManager().newHar(initialPageRef, initialPageTitle);
  }

  @Override
  public void setHarCaptureTypes(Set<CaptureType> captureTypes) {
    HarCaptureFilterManager manager = mitmProxyManager.getHarCaptureFilterManager();
    if (captureTypes == null || captureTypes.isEmpty()) {
      manager.setHarCaptureTypes(EnumSet.noneOf(CaptureType.class));
    } else {
      manager.setHarCaptureTypes(EnumSet.copyOf(captureTypes));
    }
  }

  @Override
  public void setHarCaptureTypes(CaptureType... captureTypes) {
    HarCaptureFilterManager manager = mitmProxyManager.getHarCaptureFilterManager();
    if (captureTypes == null || captureTypes.length == 0) {
      manager.setHarCaptureTypes(EnumSet.noneOf(CaptureType.class));
    } else {
      manager.setHarCaptureTypes(EnumSet.copyOf(Arrays.asList(captureTypes)));
    }
  }

  @Override
  public EnumSet<CaptureType> getHarCaptureTypes() {
    return mitmProxyManager.getHarCaptureFilterManager().getLastCaptureTypes();
  }

  @Override
  public void enableHarCaptureTypes(Set<CaptureType> captureTypes) {

  }

  @Override
  public void enableHarCaptureTypes(CaptureType... captureTypes) {

  }

  @Override
  public void disableHarCaptureTypes(Set<CaptureType> captureTypes) {

  }

  @Override
  public void disableHarCaptureTypes(CaptureType... captureTypes) {

  }

  @Override
  public Har newPage() {
    return mitmProxyManager.getHarCaptureFilterManager().newPage();
  }

  @Override
  public Har newPage(String pageRef) {
    return mitmProxyManager.getHarCaptureFilterManager().newPage(pageRef);
  }

  @Override
  public Har newPage(String pageRef, String pageTitle) {
    return mitmProxyManager.getHarCaptureFilterManager().newPage(pageRef, pageTitle);
  }

  public void endPage() {
    mitmProxyManager.getHarCaptureFilterManager().endPage();
  }

  @Override
  public Har endHar() {
    return mitmProxyManager.getHarCaptureFilterManager().endHar();
  }

  @Override
  public void setReadBandwidthLimit(long bytesPerSecond) {

  }

  @Override
  public long getReadBandwidthLimit() {
    return 0;
  }

  @Override
  public void setWriteBandwidthLimit(long bytesPerSecond) {

  }

  @Override
  public long getWriteBandwidthLimit() {
    return 0;
  }

  @Override
  public void setLatency(long latency, TimeUnit timeUnit) {

  }

  @Override
  public void setConnectTimeout(int connectionTimeout, TimeUnit timeUnit) {

  }

  @Override
  public void setIdleConnectionTimeout(int idleConnectionTimeout, TimeUnit timeUnit) {

  }

  @Override
  public void setRequestTimeout(int requestTimeout, TimeUnit timeUnit) {

  }

  @Override
  public void autoAuthorization(String domain, String username, String password,
                                AuthType authType) {

  }

  @Override
  public void stopAutoAuthorization(String domain) {

  }

  @Override
  public void chainedProxyAuthorization(String username, String password, AuthType authType) {

  }

  @Override
  public void rewriteUrl(String urlPattern, String replacementExpression) {

  }

  @Override
  public void rewriteUrls(Map<String, String> rewriteRules) {

  }

  @Override
  public Map<String, String> getRewriteRules() {
    return null;
  }

  @Override
  public void removeRewriteRule(String urlPattern) {

  }

  @Override
  public void clearRewriteRules() {

  }

  @Override
  public void blacklistRequests(String urlPattern, int statusCode) {
    this.mitmProxyManager.getBlackListManager().blacklistRequests(urlPattern, statusCode);
  }

  @Override
  public void blacklistRequests(String urlPattern, int statusCode, String httpMethodPattern) {
    this.mitmProxyManager.getBlackListManager().blacklistRequests(urlPattern, statusCode, httpMethodPattern);
  }

  @Override
  public void setBlacklist(Collection<BlacklistEntry> blacklist) {
    this.mitmProxyManager.getBlackListManager().setBlacklist(blacklist);
  }

  @Override
  public Collection<BlacklistEntry> getBlacklist() {
    return null;
  }

  @Override
  public void clearBlacklist() {

  }

  @Override
  public void whitelistRequests(Collection<String> urlPatterns, int statusCode) {
    this.mitmProxyManager.getWhiteListManager().whitelistRequests(urlPatterns, statusCode);
  }

  @Override
  public void addWhitelistPattern(String urlPattern) {
    this.mitmProxyManager.getWhiteListManager().addWhitelistPattern(urlPattern);
  }

  @Override
  public void enableEmptyWhitelist(int statusCode) {
    this.mitmProxyManager.getWhiteListManager().enableEmptyWhitelist(statusCode);
  }

  @Override
  public void disableWhitelist() {
    this.mitmProxyManager.getWhiteListManager().disableWhitelist();
  }

  @Override
  public Collection<String> getWhitelistUrls() {
    return null;
  }

  @Override
  public int getWhitelistStatusCode() {
    return 0;
  }

  @Override
  public boolean isWhitelistEnabled() {
    return false;
  }

  @Override
  public void addHeaders(Map<String, String> headers) {

  }

  @Override
  public void addHeader(String name, String value) {

  }

  @Override
  public void removeHeader(String name) {

  }

  @Override
  public void removeAllHeaders() {

  }

  @Override
  public Map<String, String> getAllHeaders() {
    return null;
  }

  @Override
  public void setHostNameResolver(AdvancedHostResolver resolver) {

  }

  @Override
  public AdvancedHostResolver getHostNameResolver() {
    return null;
  }

  @Override
  public boolean waitForQuiescence(long quietPeriod, long timeout, TimeUnit timeUnit) {
    return false;
  }

  @Override
  public void setChainedProxy(InetSocketAddress chainedProxyAddress) {

  }

  @Override
  public void setChainedProxyHTTPS(boolean chainedProxyHTTPS) {

  }

  @Override
  public void setChainedProxyNonProxyHosts(List<String> upstreamNonProxyHosts) {

  }

  @Override
  public InetSocketAddress getChainedProxy() {
    return null;
  }

  @Override
  public void addFirstHttpFilterFactory(HttpFiltersSource filterFactory) {

  }

  @Override
  public void addLastHttpFilterFactory(HttpFiltersSource filterFactory) {

  }

  @Override
  public void addResponseFilter(ResponseFilter filter) {

  }

  @Override
  public void addRequestFilter(RequestFilter filter) {

  }

  @Override
  public void setMitmDisabled(boolean mitmDisabled) {

  }

  @Override
  public void setMitmManager(MitmManager mitmManager) {

  }

  @Override
  public void setTrustAllServers(boolean trustAllServers) {
    this.mitmProxyManager.setTrustAll(trustAllServers);
    if (isStarted()) {
      mitmProxyManager.getProxyManager().setTrustAll(trustAllServers);
    }
  }

  @Override
  public void setTrustSource(TrustSource trustSource) {

  }

  @Override
  public Optional<HarEntry> findMostRecentEntry(Pattern url) {
    return Optional.empty();
  }

  @Override
  public Collection<HarEntry> findEntries(Pattern url) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseTimeLessThanOrEqual(Pattern url,
                                                                     long milliseconds) {
    return null;
  }

  @Override
  public AssertionResult assertResponseTimeLessThanOrEqual(Pattern url, long milliseconds) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseContentContains(Pattern url, String text) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseContentDoesNotContain(Pattern url, String text) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseContentMatches(Pattern url,
                                                                Pattern contentPattern) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlContentLengthLessThanOrEquals(Pattern url, Long max) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlContentMatches(Pattern url, Pattern contentPattern) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlContentContains(Pattern url, String text) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlContentDoesNotContain(Pattern url, String text) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderContains(Pattern url, String value) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderContains(Pattern url, String name,
                                                            String value) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderDoesNotContain(Pattern url, String value) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderDoesNotContain(Pattern url, String name,
                                                                  String value) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderMatches(Pattern url, Pattern valuePattern) {
    return null;
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderMatches(Pattern url, Pattern namePattern,
                                                           Pattern valuePattern) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderContains(Pattern url, String name,
                                                                String value) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderContains(Pattern url, String value) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderDoesNotContain(Pattern url, String name,
                                                                      String value) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderDoesNotContain(Pattern url, String value) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderMatches(Pattern url, Pattern name,
                                                               Pattern value) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderMatches(Pattern url, Pattern value) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseContentLengthLessThanOrEqual(Pattern url,
                                                                              Long max) {
    return null;
  }

  @Override
  public AssertionResult assertResponseStatusCode(Integer status) {
    return null;
  }

  @Override
  public AssertionResult assertResponseStatusCode(HttpStatusClass clazz) {
    return null;
  }

  @Override
  public AssertionResult assertResponseStatusCode(Pattern url, Integer status) {
    return null;
  }

  @Override
  public AssertionResult assertResponseStatusCode(Pattern url, HttpStatusClass clazz) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseStatusCode(Integer status) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseStatusCode(HttpStatusClass clazz) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseStatusCode(Pattern url, Integer status) {
    return null;
  }

  @Override
  public AssertionResult assertMostRecentResponseStatusCode(Pattern url, HttpStatusClass clazz) {
    return null;
  }
}
