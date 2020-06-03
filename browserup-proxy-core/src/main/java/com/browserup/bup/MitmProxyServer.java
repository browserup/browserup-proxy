package com.browserup.bup;

import com.browserup.bup.assertion.HarEntryAssertion;
import com.browserup.bup.assertion.ResponseTimeLessThanOrEqualAssertion;
import com.browserup.bup.assertion.error.HarEntryAssertionError;
import com.browserup.bup.assertion.field.content.ContentContainsStringAssertion;
import com.browserup.bup.assertion.field.content.ContentDoesNotContainStringAssertion;
import com.browserup.bup.assertion.field.content.ContentMatchesAssertion;
import com.browserup.bup.assertion.field.content.ContentSizeLessThanOrEqualAssertion;
import com.browserup.bup.assertion.field.header.*;
import com.browserup.bup.assertion.field.status.StatusBelongsToClassAssertion;
import com.browserup.bup.assertion.field.status.StatusEqualsAssertion;
import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.assertion.supplier.*;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.mitm.TrustSource;
import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import com.browserup.bup.mitmproxy.NetworkUtils;
import com.browserup.bup.mitmproxy.addons.AbstractAddon;
import com.browserup.bup.mitmproxy.management.HarCaptureManager;
import com.browserup.bup.proxy.BlacklistEntry;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.RewriteRule;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.proxy.dns.AdvancedHostResolver;
import com.browserup.bup.util.BrowserUpHttpUtil;
import com.browserup.bup.util.HttpStatusClass;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.MitmManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MitmProxyServer implements BrowserUpProxy {
  private static final Logger log = LoggerFactory.getLogger(MitmProxyServer.class);

  private MitmProxyProcessManager mitmProxyManager = new MitmProxyProcessManager();

  public void start(List<AbstractAddon> addons) {
    mitmProxyManager.start(NetworkUtils.getFreePort(), addons);
  }

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
    try {
      return InetAddress.getByName("localhost");
    } catch (UnknownHostException e) {
      log.error("Couldn't obtain InetAddress", e);
      throw new RuntimeException(e);
    }
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
    HarCaptureManager manager = mitmProxyManager.getHarCaptureFilterManager();
    if (captureTypes == null || captureTypes.isEmpty()) {
      manager.setHarCaptureTypes(EnumSet.noneOf(CaptureType.class));
    } else {
      manager.setHarCaptureTypes(EnumSet.copyOf(captureTypes));
    }
  }

  @Override
  public void setHarCaptureTypes(CaptureType... captureTypes) {
    HarCaptureManager manager = mitmProxyManager.getHarCaptureFilterManager();
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
    if (captureTypes == null || captureTypes.isEmpty()) return;

    HarCaptureManager manager = mitmProxyManager.getHarCaptureFilterManager();

    EnumSet<CaptureType> lastCaptureTypes = manager.getLastCaptureTypes();
    lastCaptureTypes.addAll(captureTypes);

    manager.setHarCaptureTypes(EnumSet.copyOf(captureTypes));
  }

  @Override
  public void enableHarCaptureTypes(CaptureType... captureTypes) {
    enableHarCaptureTypes(EnumSet.copyOf(Arrays.asList(captureTypes)));
  }

  @Override
  public void disableHarCaptureTypes(Set<CaptureType> disableCaptureTypes) {
    if (disableCaptureTypes == null || disableCaptureTypes.isEmpty()) return;

    HarCaptureManager manager = mitmProxyManager.getHarCaptureFilterManager();
    EnumSet<CaptureType> lastCaptureTypes = manager.getLastCaptureTypes();

    Set<CaptureType> filteredTypes = lastCaptureTypes.stream()
            .filter(c -> !disableCaptureTypes.contains(c))
            .collect(Collectors.toSet());

    manager.setHarCaptureTypes(EnumSet.copyOf(filteredTypes));
  }

  @Override
  public void disableHarCaptureTypes(CaptureType... captureTypes) {
    disableHarCaptureTypes(EnumSet.copyOf(Arrays.asList(captureTypes)));
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
    this.mitmProxyManager.getLatencyManager().setLatency(latency, timeUnit);
  }

  @Override
  public void setConnectTimeout(int connectionTimeout, TimeUnit timeUnit) {

  }

  @Override
  public void setIdleConnectionTimeout(int idleConnectionTimeout, TimeUnit timeUnit) {
    long timeout = TimeUnit.SECONDS.convert(idleConnectionTimeout, timeUnit);
    this.mitmProxyManager.getProxyManager().setConnectionIdleTimeout(timeout);
  }

  @Override
  public void setRequestTimeout(int requestTimeout, TimeUnit timeUnit) {

  }

  @Override
  public void autoAuthorization(String domain, String username, String password,
                                AuthType authType) {
      String base64EncodedCredentials = BrowserUpHttpUtil.base64EncodeBasicCredentials(username, password);
      this.mitmProxyManager.getAuthBasicFilterManager().authAuthorization(domain, base64EncodedCredentials);
  }

  @Override
  public void stopAutoAuthorization(String domain) {
      this.mitmProxyManager.getAuthBasicFilterManager().stopAutoAuthorization(domain);
  }

  @Override
  public void chainedProxyAuthorization(String username, String password, AuthType authType) {
    switch (authType) {
      case BASIC:
        mitmProxyManager.getProxyManager().setChainedProxyAuthorization(
                BrowserUpHttpUtil.base64EncodeBasicCredentials(username, password));
        break;

      default:
        throw new UnsupportedOperationException("AuthType " + authType + " is not supported for Proxy Authorization");
    }
  }

  @Override
  public void rewriteUrl(String urlPattern, String replacementExpression) {
    this.mitmProxyManager.getRewriteUrlManager().rewriteUrl(urlPattern, replacementExpression);
  }

  @Override
  public void rewriteUrls(Map<String, String> rewriteRules) {
    this.mitmProxyManager.getRewriteUrlManager().rewriteUrls(rewriteRules);
  }

  @Override
  public Map<String, String> getRewriteRules() {
    CopyOnWriteArrayList<RewriteRule> rewriteRules = this.mitmProxyManager.getRewriteUrlManager().getRewriteRules();
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    rewriteRules.forEach(rewriteRule -> builder.put(rewriteRule.getPattern().pattern(), rewriteRule.getReplace()));

    return builder.build();
  }

  @Override
  public void removeRewriteRule(String urlPattern) {
    this.mitmProxyManager.getRewriteUrlManager().removeRewriteRule(urlPattern);
  }

  @Override
  public void clearRewriteRules() {
    this.mitmProxyManager.getRewriteUrlManager().clearRewriteRules();
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
    return this.mitmProxyManager.getBlackListManager().getBlacklist();
  }

  @Override
  public void clearBlacklist() {
    this.mitmProxyManager.getBlackListManager().clearBlackList();
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
    return mitmProxyManager.getWhiteListManager().getWhitelistUrls();
  }

  @Override
  public int getWhitelistStatusCode() {
    return mitmProxyManager.getWhiteListManager().getWhitelistStatusCode();
  }

  @Override
  public boolean isWhitelistEnabled() {
    return mitmProxyManager.getWhiteListManager().isWhitelistEnabled();
  }

  @Override
  public void addHeaders(Map<String, String> headers) {
      this.mitmProxyManager.getAdditionalHeadersManager().addHeaders(headers);
  }

  @Override
  public void addHeader(String name, String value) {
      this.mitmProxyManager.getAdditionalHeadersManager().addHeader(name, value);
  }

  @Override
  public void removeHeader(String name) {
      this.mitmProxyManager.getAdditionalHeadersManager().removeHeader(name);
  }

  @Override
  public void removeAllHeaders() {
      this.mitmProxyManager.getAdditionalHeadersManager().removeAllHeaders();
  }

  @Override
  public Map<String, String> getAllHeaders() {
      return this.mitmProxyManager.getAdditionalHeadersManager().getAllHeaders();
  }

  @Override
  public void setHostNameResolver(AdvancedHostResolver resolver) {
    // TODO: do we need this in mitmproxy?
  }

  public void setDnsResolvingDelayMs(Long delayMs) {
    this.mitmProxyManager.getProxyManager().setDnsResolvingDelayMs(delayMs);
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
    this.mitmProxyManager.getProxyManager().setChainedProxy(chainedProxyAddress);
  }

  @Override
  public void setChainedProxyHTTPS(boolean chainedProxyHTTPS) {
    this.mitmProxyManager.getProxyManager().setChainedHttpsProxy(chainedProxyHTTPS);
  }

  @Override
  public void setChainedProxyNonProxyHosts(List<String> upstreamNonProxyHosts) {
    // this.mitmProxyManager.getProxyManager().setChainedProxyNonProxyHosts(upstreamNonProxyHosts);
    // See notes in browserup-proxy-core/Mitmproxy_Integration_Notes.txt
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
    List<HarEntry> entries = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url).get();
    return Optional.ofNullable(entries.isEmpty() ? null : entries.get(0));
  }

  @Override
  public Collection<HarEntry> findEntries(Pattern url) {
    return new UrlFilteredHarEntriesSupplier(getHar(), url).get();
  }

  @Override
  public AssertionResult assertMostRecentResponseTimeLessThanOrEqual(Pattern url, long time) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = new ResponseTimeLessThanOrEqualAssertion(time);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertResponseTimeLessThanOrEqual(Pattern url, long time) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new ResponseTimeLessThanOrEqualAssertion(time);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseContentContains(Pattern url, String text) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentContainsStringAssertion(text);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseContentDoesNotContain(Pattern url, String text) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentDoesNotContainStringAssertion(text);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseContentMatches(Pattern url, Pattern contentPattern) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentMatchesAssertion(contentPattern);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlContentLengthLessThanOrEquals(Pattern url, Long maxSize) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentSizeLessThanOrEqualAssertion(maxSize);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlContentMatches(Pattern url, Pattern contentPattern) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentMatchesAssertion(contentPattern);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlContentContains(Pattern url, String text) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentContainsStringAssertion(text);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlContentDoesNotContain(Pattern url, String text) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentDoesNotContainStringAssertion(text);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderContains(Pattern url, String value) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new HeadersContainStringAssertion(value);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderContains(Pattern url, String name, String value) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = StringUtils.isEmpty(name) ?
            new HeadersContainStringAssertion(value) :
            new FilteredHeadersContainStringAssertion(name, value);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderDoesNotContain(Pattern url, String name, String value) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = StringUtils.isEmpty(name) ?
            new HeadersDoNotContainStringAssertion(value) :
            new FilteredHeadersDoNotContainStringAssertion(name, value);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderDoesNotContain(Pattern url, String value) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new HeadersDoNotContainStringAssertion(value);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertAnyUrlResponseHeaderMatches(Pattern url, Pattern namePattern, Pattern valuePattern) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = namePattern == null ?
            new HeadersMatchAssertion(valuePattern) :
            new FilteredHeadersMatchAssertion(namePattern, valuePattern);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertResponseStatusCode(Integer status) {
    HarEntriesSupplier supplier = new CurrentStepHarEntriesSupplier(getHar());
    HarEntryAssertion assertion = new StatusEqualsAssertion(status);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertResponseStatusCode(HttpStatusClass clazz) {
    HarEntriesSupplier supplier = new CurrentStepHarEntriesSupplier(getHar());
    HarEntryAssertion assertion = new StatusBelongsToClassAssertion(clazz);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertResponseStatusCode(Pattern url, Integer status) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new StatusEqualsAssertion(status);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertResponseStatusCode(Pattern url, HttpStatusClass clazz) {
    HarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(getHar(), url);
    HarEntryAssertion assertion = new StatusBelongsToClassAssertion(clazz);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseStatusCode(Integer status) {
    HarEntriesSupplier supplier = new MostRecentHarEntrySupplier(getHar());
    HarEntryAssertion assertion = new StatusEqualsAssertion(status);

    return checkAssertion(supplier, assertion);
  }


  @Override
  public AssertionResult assertMostRecentResponseStatusCode(HttpStatusClass clazz) {
    HarEntriesSupplier supplier = new MostRecentHarEntrySupplier(getHar());
    HarEntryAssertion assertion = new StatusBelongsToClassAssertion(clazz);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseStatusCode(Pattern url, Integer status) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = new StatusEqualsAssertion(status);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseStatusCode(Pattern url, HttpStatusClass clazz) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = new StatusBelongsToClassAssertion(clazz);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseContentLengthLessThanOrEqual(Pattern url, Long max) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = new ContentSizeLessThanOrEqualAssertion(max);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderContains(Pattern url, String name, String value) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = StringUtils.isEmpty(name) ?
            new HeadersContainStringAssertion(value) :
            new FilteredHeadersContainStringAssertion(name, value);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderDoesNotContain(Pattern url, String name, String value) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = StringUtils.isEmpty(name) ?
            new HeadersDoNotContainStringAssertion(value) :
            new FilteredHeadersDoNotContainStringAssertion(name, value);

    return checkAssertion(supplier, assertion);
  }

  @Override
  public AssertionResult assertMostRecentResponseHeaderMatches(Pattern url, Pattern name, Pattern value) {
    HarEntriesSupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(getHar(), url);
    HarEntryAssertion assertion = name == null ?
            new HeadersMatchAssertion(value) :
            new FilteredHeadersMatchAssertion(name, value);

    return checkAssertion(supplier, assertion);
  }

  private AssertionResult checkAssertion(HarEntriesSupplier harEntriesSupplier, HarEntryAssertion assertion) {
    AssertionResult.Builder result = new AssertionResult.Builder();

    List<HarEntry> entries = harEntriesSupplier.get();

    AtomicInteger failedCount = new AtomicInteger();

    entries.forEach(entry -> {
      AssertionEntryResult.Builder requestResult = new AssertionEntryResult.Builder();
      requestResult.setUrl(entry.getRequest().getUrl());

      Optional<HarEntryAssertionError> error = assertion.assertion(entry);
      requestResult.setFailed(error.isPresent());

      if (error.isPresent()) {
        requestResult.setMessage(error.get().getMessage());
        failedCount.getAndIncrement();
      }

      result.addRequest(requestResult.create());
    });

    String resultMessage = String.format("%d passed, %d total", entries.size() - failedCount.get(), entries.size());

    return result
            .setFilter(harEntriesSupplier.getFilterInfo())
            .setFailed(failedCount.get() > 0)
            .setMessage(resultMessage)
            .setPassed(failedCount.get() == 0)
            .create();
  }
}
