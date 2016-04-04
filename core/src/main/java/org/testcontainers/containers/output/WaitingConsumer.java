package org.testcontainers.containers.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A consumer for container output that buffers lines in a {@link java.util.concurrent.BlockingDeque} and enables tests
 * to wait for a matching condition.
 */
public class WaitingConsumer implements Consumer<OutputFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingConsumer.class);

    private LinkedBlockingDeque<OutputFrame> frames = new LinkedBlockingDeque<>();

    @Override
    public void accept(OutputFrame frame) {
        frames.add(frame);
    }

    /**
     * Get access to the underlying frame buffer. Modifying the buffer contents is likely to cause problems if the
     * waitUntil() methods are also being used, as they feed on the same data.
     *
     * @return
     */
    public LinkedBlockingDeque<OutputFrame> getFrames() {
        return frames;
    }

    /**
     * Wait until any frame (usually, line) of output matches the provided predicate.
     * <p>
     * Note that lines will often have a trailing newline character, and this is not stripped off before the
     * predicate is tested.
     *
     * @param predicate a predicate to test against each frame
     */
    public void waitUntil(Predicate<OutputFrame> predicate) throws TimeoutException {
        // ~2.9 million centuries ought to be enough for anyone
        waitUntil(predicate, Long.MAX_VALUE);
    }

    /**
     * Wait until any frame (usually, line) of output matches the provided predicate.
     * <p>
     * Note that lines will often have a trailing newline character, and this is not stripped off before the
     * predicate is tested.
     *
     * @param predicate a predicate to test against each frame
     * @param limit     maximum time to wait
     * @param limitUnit maximum time to wait (units)
     */
    public void waitUntil(Predicate<OutputFrame> predicate, long limit, TimeUnit limitUnit) throws TimeoutException {
        long expiry = limitUnit.toMillis(limit) + System.currentTimeMillis();

        waitUntil(predicate, expiry);
    }

    private void waitUntil(Predicate<OutputFrame> predicate, long expiry) throws TimeoutException {
        while (System.currentTimeMillis() < expiry) {
            try {
                OutputFrame frame = frames.pollLast(100, TimeUnit.MILLISECONDS);

                if (frame != null) {
                    LOGGER.debug("{}: {}", frame.getType(), frame.getUtf8String());

                    if (predicate.test(frame)) {
                        return;
                    }
                }

                if (frames.isEmpty()) {
                    // sleep for a moment to avoid excessive CPU spinning
                    Thread.sleep(10L);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // did not return before expiry was reached
        throw new TimeoutException();
    }

    /**
     * Wait until Docker closes the stream of output.
     */
    public void waitUntilEnd() throws TimeoutException {
        waitUntilEnd(Long.MAX_VALUE);
    }

    /**
     * Wait until Docker closes the stream of output.
     *
     * @param limit     maximum time to wait
     * @param limitUnit maximum time to wait (units)
     */
    public void waitUntilEnd(long limit, TimeUnit limitUnit) throws TimeoutException {
        long expiry = limitUnit.toMillis(limit) + System.currentTimeMillis();

        waitUntilEnd(expiry);
    }

    private void waitUntilEnd(Long expiry) {
        while (System.currentTimeMillis() < expiry) {
            try {
                OutputFrame frame = frames.pollLast(100, TimeUnit.MILLISECONDS);

                if (frame == OutputFrame.END) {
                    return;
                }

                if (frames.isEmpty()) {
                    // sleep for a moment to avoid excessive CPU spinning
                    Thread.sleep(10L);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}