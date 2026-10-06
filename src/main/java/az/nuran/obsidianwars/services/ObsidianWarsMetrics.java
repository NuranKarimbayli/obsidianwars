package az.nuran.obsidianwars.services;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.EconomyManager;
import az.nuran.obsidianwars.managers.QueueManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

/**
 * bStats Metrics integration for ObsidianWars.
 * Tracks anonymous usage statistics for plugin improvement.
 * This will never crash the plugin - all errors are caught and logged.
 */
public class ObsidianWarsMetrics {

    private static Metrics metrics;

    /**
     * Initializes bStats metrics with full error protection.
     *
     * @param plugin The plugin instance
     */
    public static void initialize(Obsidianwars plugin) {
        try {
            int pluginId = 12345; // Replace with actual bStats plugin ID
            metrics = new Metrics(plugin, pluginId);

            // Add custom charts
            addCustomCharts(plugin);

            plugin.getLogger().info("bStats metrics enabled. Thank you for supporting plugin development!");
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().warning("bStats library not found - metrics disabled");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize bStats metrics: " + e.getMessage());
            plugin.getLogger().info("Plugin will continue to function normally without metrics.");
        }
    }

    /**
     * Adds custom charts to metrics with error protection.
     *
     * @param plugin The plugin instance
     */
    private static void addCustomCharts(Obsidianwars plugin) {
        try {
            // Active games chart
            metrics.addCustomChart(new SimplePie("active_games", () -> {
                try {
                    int activeGames = 0;
                    for (String arenaName : ArenaConfigManager.getArenaNames()) {
                        ArenaStateManager.ArenaState state = ArenaStateManager.getInstance().getState(arenaName);
                        if (state == ArenaStateManager.ArenaState.PLAYING ||
                            state == ArenaStateManager.ArenaState.PREPARATION ||
                            state == ArenaStateManager.ArenaState.SUDDEN_DEATH) {
                            activeGames++;
                        }
                    }
                    return String.valueOf(activeGames);
                } catch (Exception e) {
                    return "0";
                }
            }));

            // Total players in arenas chart
            metrics.addCustomChart(new SimplePie("total_players", () -> {
                try {
                    int totalPlayers = ObsidianCommand.playersInArena.size();
                    if (totalPlayers == 0) return "0";
                    if (totalPlayers <= 5) return "1-5";
                    if (totalPlayers <= 10) return "6-10";
                    if (totalPlayers <= 20) return "11-20";
                    if (totalPlayers <= 50) return "21-50";
                    return "50+";
                } catch (Exception e) {
                    return "0";
                }
            }));

            // Arena status distribution chart
            metrics.addCustomChart(new AdvancedPie("arena_status_distribution", () -> {
                try {
                    Map<String, Integer> statusMap = new HashMap<>();
                    for (String arenaName : ArenaConfigManager.getArenaNames()) {
                        ArenaStateManager.ArenaState state = ArenaStateManager.getInstance().getState(arenaName);
                        if (state != null) {
                            statusMap.put(state.name(), statusMap.getOrDefault(state.name(), 0) + 1);
                        }
                    }
                    return statusMap;
                } catch (Exception e) {
                    return new HashMap<>();
                }
            }));

            // Database type chart
            metrics.addCustomChart(new SimplePie("database_type", () -> {
                try {
                    String storageType = plugin.getConfig().getString("database.storage", "sqlite");
                    return storageType != null ? storageType.toLowerCase() : "sqlite";
                } catch (Exception e) {
                    return "sqlite";
                }
            }));

            // Queue system usage chart
            metrics.addCustomChart(new SimplePie("queue_usage", () -> {
                try {
                    int queuedPlayers = QueueManager.getInstance().getTotalQueueCount();
                    return queuedPlayers > 0 ? "enabled" : "disabled";
                } catch (Exception e) {
                    return "disabled";
                }
            }));

            // Economy integration chart
            metrics.addCustomChart(new SimplePie("economy_enabled", () -> {
                try {
                    return EconomyManager.getInstance().isEnabled() ? "enabled" : "disabled";
                } catch (Exception e) {
                    return "disabled";
                }
            }));

            // PlaceholderAPI integration chart
            metrics.addCustomChart(new SimplePie("placeholderapi_enabled", () -> {
                try {
                    return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "enabled" : "disabled";
                } catch (Exception e) {
                    return "disabled";
                }
            }));

            // Sudden death enabled chart
            metrics.addCustomChart(new SimplePie("sudden_death_enabled", () -> {
                try {
                    boolean sdEnabled = plugin.getConfig().getBoolean("sudden-death.disable-respawns", true);
                    return sdEnabled ? "enabled" : "disabled";
                } catch (Exception e) {
                    return "disabled";
                }
            }));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add custom charts: " + e.getMessage());
        }
    }

    /**
     * Gets the metrics instance.
     *
     * @return The Metrics instance, or null if not initialized
     */
    public static Metrics getMetrics() {
        return metrics;
    }
}
