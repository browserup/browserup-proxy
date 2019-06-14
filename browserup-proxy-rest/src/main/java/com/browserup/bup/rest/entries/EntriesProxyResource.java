//package com.browserup.bup.rest.entries;
//
//import com.browserup.bup.BrowserUpProxyServer;
//import com.browserup.bup.proxy.ProxyManager;
//import com.browserup.bup.rest.BaseResource;
//import com.browserup.bup.proxy.bricks.validation.param.raw.IntRawParam;
//import com.browserup.bup.proxy.bricks.validation.param.ValidatedParam;
//import com.browserup.bup.proxy.bricks.validation.param.raw.StringRawParam;
//import com.browserup.harreader.model.HarEntry;
//import com.google.inject.Inject;
//import com.google.inject.name.Named;
//import com.google.sitebricks.At;
//import com.google.sitebricks.client.transport.Json;
//import com.google.sitebricks.headless.Reply;
//import com.google.sitebricks.headless.Service;
//import com.google.sitebricks.http.Get;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Collection;
//import java.util.regex.Pattern;
//
//public class EntriesProxyResource extends BaseResource {
//    private static final Logger LOG = LoggerFactory.getLogger(EntriesProxyResource.class);
//
//    protected ValidatedParam<Pattern> urlPattern = ValidatedParam.empty("urlPattern");
//    protected ValidatedParam<BrowserUpProxyServer> proxy = ValidatedParam.empty("proxy");
//
//    @Inject
//    public EntriesProxyResource(ProxyManager proxyManager) {
//        super(proxyManager);
//    }
//
//    public void setUrlPattern(String urlPattern) {
//        this.urlPattern = parsePatternParam(new StringRawParam("urlPattern", urlPattern));
//    }
//
//    @Get
//    public Reply<?> entries(@Named("port") int port) {
//        LOG.info("GET /" + port + "/har/entries");
//
//        ValidatedParam<BrowserUpProxyServer> proxy = parseProxyServer(new IntRawParam("proxy port", port));
//        checkRequiredParams(urlPattern, proxy);
//
//        Collection<HarEntry> result = proxy.getParsedParam().findEntries(urlPattern.getParsedParam());
//
//        return Reply.with(result.toArray()).as(Json.class);
//    }
//}
