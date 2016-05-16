package net.lightbody.bmp.proxy.http;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientInterrupter {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientInterrupter.class);

    private static final int TIMEOUT_POLL_INTERVAL_MS = 1000;

    private static final ScheduledExecutorService timeoutPollExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("browsermob-client-timeout-thread");
            thread.setDaemon(true);
            return thread;
        }
    });

    private static class PollHttpClientTask implements Runnable {
        private final WeakReference<BrowserMobHttpClient> clientWrapper;

        public PollHttpClientTask(BrowserMobHttpClient client) {
            this.clientWrapper = new WeakReference<BrowserMobHttpClient>(client);
        }

        @Override
        public void run() {
            BrowserMobHttpClient client = clientWrapper.get();
            if (client != null && !client.isShutdown()) {
                try {
                    client.checkTimeout();
                } catch (RuntimeException e) {
                    LOG.warn("Unexpected problem while checking timeout on a client", e);
                }
            } else {
                // the client was garbage collected or it was shut down, so it no longer needs to check for timeout. throw an exception
                // to prevent the scheduled executor from re-scheduling the cleanup task for this instance
                throw new RuntimeException("Exiting PollHttpClientTask because BrowserMobHttpClient was garbage collected or shut down");
            }
        }
    }

    public static void watch(BrowserMobHttpClient client) {
        timeoutPollExecutor.scheduleWithFixedDelay(new PollHttpClientTask(client), TIMEOUT_POLL_INTERVAL_MS, TIMEOUT_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

}
