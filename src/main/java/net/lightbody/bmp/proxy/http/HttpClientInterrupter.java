package net.lightbody.bmp.proxy.http;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientInterrupter {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientInterrupter.class);
    private static Set<BrowserMobHttpClient> clients = new CopyOnWriteArraySet<BrowserMobHttpClient>();

    static {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    for (BrowserMobHttpClient client : clients) {
                        try {
                            client.checkTimeout();
                        } catch (RuntimeException e) {
                            LOG.error("Unexpected problem while checking timeout on a client", e);
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // this is OK
                    }
                }
            }
        }, "HttpClientInterrupter Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public static void watch(BrowserMobHttpClient client) {
        clients.add(client);
    }

    public static void release(BrowserMobHttpClient client) {
        clients.remove(client);
    }
}
