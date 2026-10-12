package az.nuran.obsidianwars.services;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Health check service for monitoring critical plugin components.
 * Provides periodic health checks for database, arena states, and other systems.
 */
public class HealthCheckService {

    private static HealthCheckService instance;
    private final Obsidianwars plugin;
    private final Map<String, HealthStatus> componentStatus;
    private final Map<String, Long> lastCheckTime;
    private boolean healthCheckEnabled = true;

    /**
     * Health status enum for component states.
     */
    public enum HealthStatus {
        HEALTHY("Healthy", true),
        DEGRADED("Degraded", true),
        UNHEALTHY("Unhealthy", false),
        UNKNOWN("Unknown", false);

        private final String displayName;
        private final boolean isOperational;

        HealthStatus(String displayName, boolean isOperational) {
            this.displayName = displayName;
            this.isOperational = isOperational;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isOperational() {
            return isOperational;
        }
    }

    /**
     * Health check result for a component.
     */
    public static class HealthCheckResult {
        private final String component;
        private final HealthStatus status;
        private final String message;
        private final long checkTime;

        public HealthCheckResult(String component, HealthStatus status, String message) {
            this.component = component;
            this.status = status;
            this.message = message;
            this.checkTime = System.currentTimeMillis();
        }

        public String getComponent() {
            return component;
        }

        public HealthStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public long getCheckTime() {
            return checkTime;
        }
    }

    private HealthCheckService(Obsidianwars plugin) {
        this.plugin = plugin;
        this.componentStatus = new ConcurrentHashMap<>();
        this.lastCheckTime = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the HealthCheckService singleton.
     *
     * @param plugin The plugin instance
     */
    public static void initialize(Obsidianwars plugin) {
        if (instance == null) {
            instance = new HealthCheckService(plugin);
            plugin.getLogger().info("HealthCheckService initialized");
        }
    }

    /**
     * Gets the HealthCheckService instance.
     *
     * @return The HealthCheckService instance
     */
    public static HealthCheckService getInstance() {
        return instance;
    }

    /**
     * Performs a health check on the database connection.
     *
     * @return Health check result
     */
    public HealthCheckResult checkDatabaseHealth() {
        String component = "database";
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn != null && conn.isValid(2)) {
                updateComponentStatus(component, HealthStatus.HEALTHY, "Database connection is valid");
                return new HealthCheckResult(component, HealthStatus.HEALTHY, "Database connection is valid");
            } else {
                updateComponentStatus(component, HealthStatus.UNHEALTHY, "Database connection is invalid");
                return new HealthCheckResult(component, HealthStatus.UNHEALTHY, "Database connection is invalid");
            }
        } catch (SQLException e) {
            updateComponentStatus(component, HealthStatus.UNHEALTHY, "Database connection failed: " + e.getMessage());
            return new HealthCheckResult(component, HealthStatus.UNHEALTHY, "Database connection failed: " + e.getMessage());
        }
    }

    /**
     * Performs a health check on arena state manager.
     *
     * @return Health check result
     */
    public HealthCheckResult checkArenaStateHealth() {
        String component = "arena-states";
        try {
            ArenaStateManager manager = ArenaStateManager.getInstance();
            if (manager == null) {
                updateComponentStatus(component, HealthStatus.UNHEALTHY, "ArenaStateManager is null");
                return new HealthCheckResult(component, HealthStatus.UNHEALTHY, "ArenaStateManager is null");
            }

            int totalArenas = manager.getTotalArenaCount();
            updateComponentStatus(component, HealthStatus.HEALTHY, "Tracking " + totalArenas + " arenas");
            return new HealthCheckResult(component, HealthStatus.HEALTHY, "Tracking " + totalArenas + " arenas");
        } catch (Exception e) {
            updateComponentStatus(component, HealthStatus.UNHEALTHY, "Arena state check failed: " + e.getMessage());
            return new HealthCheckResult(component, HealthStatus.UNHEALTHY, "Arena state check failed: " + e.getMessage());
        }
    }

    /**
     * Performs a health check on all critical components.
     *
     * @return Map of component names to health check results
     */
    public Map<String, HealthCheckResult> checkAllComponents() {
        Map<String, HealthCheckResult> results = new HashMap<>();

        results.put("database", checkDatabaseHealth());
        results.put("arena-states", checkArenaStateHealth());

        // Log overall health
        boolean allHealthy = results.values().stream()
            .allMatch(result -> result.getStatus() == HealthStatus.HEALTHY);

        if (allHealthy) {
            plugin.getLogger().info("Health check: All components are healthy");
        } else {
            plugin.getLogger().warning("Health check: Some components are unhealthy");
            for (HealthCheckResult result : results.values()) {
                if (result.getStatus() != HealthStatus.HEALTHY) {
                    plugin.getLogger().warning("  - " + result.getComponent() + ": " + result.getStatus().getDisplayName() + " - " + result.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * Gets the current status of a component.
     *
     * @param component The component name
     * @return The component's health status, or UNKNOWN if not checked
     */
    public HealthStatus getComponentStatus(String component) {
        return componentStatus.getOrDefault(component, HealthStatus.UNKNOWN);
    }

    /**
     * Gets the last check time for a component.
     *
     * @param component The component name
     * @return The last check time in milliseconds, or 0 if not checked
     */
    public long getLastCheckTime(String component) {
        return lastCheckTime.getOrDefault(component, 0L);
    }

    /**
     * Updates the status of a component.
     *
     * @param component The component name
     * @param status The health status
     * @param message Status message
     */
    private void updateComponentStatus(String component, HealthStatus status, String message) {
        componentStatus.put(component, status);
        lastCheckTime.put(component, System.currentTimeMillis());
    }

    /**
     * Enables or disables health checks.
     *
     * @param enabled Whether health checks should be enabled
     */
    public void setHealthCheckEnabled(boolean enabled) {
        this.healthCheckEnabled = enabled;
        plugin.getLogger().info("Health checks " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Checks if health checks are enabled.
     *
     * @return true if health checks are enabled
     */
    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    /**
     * Cleans up the HealthCheckService.
     */
    public void cleanup() {
        componentStatus.clear();
        lastCheckTime.clear();
        instance = null;
        plugin.getLogger().info("HealthCheckService cleaned up");
    }
}
