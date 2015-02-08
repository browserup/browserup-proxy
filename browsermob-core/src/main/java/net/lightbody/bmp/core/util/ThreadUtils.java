package net.lightbody.bmp.core.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    /**
     * Functional interface to specify a condition to wait for. The checkCondition() method should take a minimal amount of time to execute.
     */
    public static interface WaitCondition {
        public boolean checkCondition();
    }

    private static final int DEFAULT_POLL_INTERVAL_MS = 500;

    // use a  single threaded executor to poll for all conditions -- WaitConditions should be very fast!
    private static final ScheduledExecutorService waitConditionPollingExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("wait-condition-polling-thread");
            thread.setDaemon(true);
            return thread;
        }
    });

    /**
     * Waits for the specified condition to be true. The condition will be evaluated when the method is invoked, and then <i>may be</i> polled
     * periodically, until the condition returns true or the specified timeout has elapsed. If the condition is not true after the timeout
     * has elapsed, it is guaranteed to be evaluated at least once more before the method returns.
     *
     * @param condition condition to wait on
     * @param timeout time to wait for condition to be true
     * @param timeUnit unit of time to wait
     * @return true if the condition was satisfied within the specified time, otherwise false
     */
    public static boolean pollForCondition(WaitCondition condition, long timeout, TimeUnit timeUnit) {
        return pollForCondition(condition, timeout, timeUnit, DEFAULT_POLL_INTERVAL_MS);
    }

    /**
     * Waits for the specified condition to be true. The condition will be evaluated when the method is invoked, and then <i>may be</i> polled
     * periodically, until the condition returns true or the specified timeout has elapsed. If the condition is not true after the timeout
     * has elapsed, it is guaranteed to be evaluated at least once more before the method returns.
     *
     * @param condition condition to wait on
     * @param timeout time to wait for condition to be true
     * @param timeUnit unit of time to wait
     * @param pollIntervalMs interval at which to poll, in milliseconds
     * @return true if the condition was satisfied within the specified time, otherwise false
     */
    public static boolean pollForCondition(WaitCondition condition, final long timeout, final TimeUnit timeUnit, long pollIntervalMs) {
        if (condition.checkCondition()) {
            return true;
        }

        // wrap the WaitCondition in a WeakReference, just to make sure it hasn't been orphaned
        final WeakReference<WaitCondition> conditionWrapper = new WeakReference<WaitCondition>(condition);
        final CountDownLatch latch = new CountDownLatch(1);

        ScheduledFuture<?> pollingTask = waitConditionPollingExecutor.scheduleWithFixedDelay(new Runnable() {
            private final long taskStart = System.currentTimeMillis();

            @Override
            public void run() {
                WaitCondition condition = conditionWrapper.get();
                if (condition == null || (System.currentTimeMillis() - taskStart > TimeUnit.MILLISECONDS.convert(timeout, timeUnit))) {
                    // max time limit elapsed or condition was garbage collected, so throw an exception to prevent this task from being rescheduled again
                    throw new RuntimeException("Stopped polling for condition because time limit elapsed or condition was garbage collected");
                }

                if (condition.checkCondition()) {
                    latch.countDown();
                }
            }
        }, pollIntervalMs, pollIntervalMs, TimeUnit.MILLISECONDS);

        try {
            boolean conditionMet = latch.await(timeout, timeUnit);

            if (conditionMet || condition.checkCondition()) {
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // cancel the polling task so it is not executed anymore
            pollingTask.cancel(true);
        }
    }
}