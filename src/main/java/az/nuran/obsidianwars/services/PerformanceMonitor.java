package az.nuran.obsidianwars.services;

import az.nuran.obsidianwars.models.OperationMetrics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Performance monitoring system for ObsidianWars.
 * Tracks execution times of critical game operations and provides analytics.
 */
public class PerformanceMonitor {

    private static PerformanceMonitor instance;
    private final Logger logger;

    // Operation metrics storage
    private final Map<String, OperationMetrics> metrics;

    public PerformanceMonitor(Logger logger) {
        this.logger = logger;
        this.metrics = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the PerformanceMonitor singleton.
     *
     * @param logger The plugin logger
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new PerformanceMonitor(logger);
        }
    }

    /**
     * Gets the PerformanceMonitor instance.
     *
     * @return The PerformanceMonitor instance
     */
    public static PerformanceMonitor getInstance() {
        return instance;
    }

    /**
     * Records the execution time of an operation.
     *
     * @param operationName The name of the operation
     * @param durationMs The execution time in milliseconds
     */
    public void recordOperation(String operationName, long durationMs) {
        metrics.computeIfAbsent(operationName, k -> new OperationMetrics()).record(durationMs);
    }

    /**
     * Times an operation using a TimingContext.
     *
     * @param operationName The name of the operation
     * @return A TimingContext that should be closed when the operation completes
     */
    public TimingContext timeOperation(String operationName) {
        return new TimingContext(operationName);
    }

    /**
     * Gets metrics for a specific operation.
     *
     * @param operationName The operation name
     * @return The operation metrics, or null if not found
     */
    public OperationMetrics getMetrics(String operationName) {
        return metrics.get(operationName);
    }

    /**
     * Gets all recorded metrics.
     *
     * @return A map of operation names to their metrics
     */
    public Map<String, OperationMetrics> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * Generates a performance report.
     *
     * @return A formatted string containing the performance report
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== ObsidianWars Performance Report ===\n");
        report.append(String.format("%-40s %10s %10s %10s %10s %10s\n",
            "Operation", "Count", "Avg(ms)", "Min(ms)", "Max(ms)", "Total(ms)"));
        report.append("-".repeat(100)).append("\n");

        for (Map.Entry<String, OperationMetrics> entry : metrics.entrySet()) {
            OperationMetrics m = entry.getValue();
            report.append(String.format("%-40s %10d %10.2f %10d %10d %10d\n",
                entry.getKey(),
                m.getCount(),
                m.getAverageMs(),
                m.getMinTime(),
                m.getMaxTime(),
                m.getTotalTime()));
        }

        return report.toString();
    }

    /**
     * Logs the performance report to the console.
     */
    public void logReport() {
        logger.info(generateReport());
    }

    /**
     * Resets all metrics.
     */
    public void reset() {
        metrics.clear();
        logger.info("Performance metrics reset");
    }

    /**
     * Checks if an operation is taking too long based on threshold.
     *
     * @param operationName The operation name
     * @param thresholdMs The threshold in milliseconds
     * @return true if the average execution time exceeds the threshold
     */
    public boolean isSlowOperation(String operationName, long thresholdMs) {
        OperationMetrics m = metrics.get(operationName);
        return m != null && m.getAverageMs() > thresholdMs;
    }

    /**
     * Cleans up the PerformanceMonitor (called on plugin disable).
     */
    public void cleanup() {
        metrics.clear();
        instance = null;
    }

    /**
     * TimingContext for automatic operation timing.
     * Use try-with-resources for automatic timing.
     */
    public class TimingContext implements AutoCloseable {
        private final String operationName;
        private final long startTime;

        public TimingContext(String operationName) {
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            recordOperation(operationName, duration);
        }
    }

}
