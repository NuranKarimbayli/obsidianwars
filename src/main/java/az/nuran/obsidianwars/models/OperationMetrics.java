package az.nuran.obsidianwars.models;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores metrics for a single operation.
 */
public class OperationMetrics {
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong totalTime = new AtomicLong(0);
    private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxTime = new AtomicLong(0);

    public void record(long durationMs) {
        count.incrementAndGet();
        totalTime.addAndGet(durationMs);

        // Update min (atomic compare-and-set loop)
        long currentMin = minTime.get();
        while (durationMs < currentMin) {
            if (minTime.compareAndSet(currentMin, durationMs)) {
                break;
            }
            currentMin = minTime.get();
        }

        // Update max (atomic compare-and-set loop)
        long currentMax = maxTime.get();
        while (durationMs > currentMax) {
            if (maxTime.compareAndSet(currentMax, durationMs)) {
                break;
            }
            currentMax = maxTime.get();
        }
    }

    public long getCount() {
        return count.get();
    }

    public long getTotalTime() {
        return totalTime.get();
    }

    public long getMinTime() {
        return minTime.get() == Long.MAX_VALUE ? 0 : minTime.get();
    }

    public long getMaxTime() {
        return maxTime.get();
    }

    public double getAverageMs() {
        long c = count.get();
        return c > 0 ? (double) totalTime.get() / c : 0.0;
    }
}
