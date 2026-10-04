package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages debug logging configuration and output routing.
 * Supports console, chat, both, or off modes for debug messages.
 */
public class DebugManager {

    public enum DebugMode {
        OFF,
        CONSOLE,
        CHAT,
        BOTH
    }

    // Maps admin UUID to their debug mode preference
    private static final Map<UUID, DebugMode> debugModes = new HashMap<>();

    /**
     * Sets the debug mode for a player.
     * @param player The admin player
     * @param mode The debug mode to set
     */
    public static void setDebugMode(Player player, DebugMode mode) {
        debugModes.put(player.getUniqueId(), mode);
    }

    /**
     * Gets the current debug mode for a player.
     * @param player The admin player
     * @return The current debug mode (OFF if not set)
     */
    public static DebugMode getDebugMode(Player player) {
        return debugModes.getOrDefault(player.getUniqueId(), DebugMode.OFF);
    }

    /**
     * Checks if debug is enabled for a player (any mode except OFF).
     * @param player The admin player
     * @return true if debug is enabled
     */
    public static boolean isDebugEnabled(Player player) {
        return getDebugMode(player) != DebugMode.OFF;
    }

    /**
     * Checks if any admin has debug enabled.
     * @return true if at least one admin has debug enabled
     */
    public static boolean isAnyDebugEnabled() {
        for (DebugMode mode : debugModes.values()) {
            if (mode != DebugMode.OFF) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs a debug message based on current debug configuration.
     * Routes to console, chat, both, or neither based on admin settings.
     *
     * @param message The debug message to log
     * @param arenaName The arena name (can be null)
     */
    public static void logDebug(String message, String arenaName) {
        if (!isAnyDebugEnabled()) {
            return;
        }

        String arenaStr = arenaName != null ? arenaName : "Unknown";
        String formattedMessage = "[DEBUG] " + arenaStr + ": " + message;

        for (Map.Entry<UUID, DebugMode> entry : debugModes.entrySet()) {
            DebugMode mode = entry.getValue();
            if (mode == DebugMode.OFF) {
                continue;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }

            // Console output
            if (mode == DebugMode.CONSOLE || mode == DebugMode.BOTH) {
                Obsidianwars.getInstance().getLogger().info(formattedMessage);
            }

            // Chat output
            if (mode == DebugMode.CHAT || mode == DebugMode.BOTH) {
                player.sendMessage("§7[DEBUG] §f" + formattedMessage);
            }
        }
    }

    /**
     * Logs a debug message without arena context.
     *
     * @param message The debug message to log
     */
    public static void logDebug(String message) {
        logDebug(message, (String) null);
    }

    /**
     * Clears debug mode for a player.
     * @param player The admin player
     */
    public static void clearDebugMode(Player player) {
        debugModes.remove(player.getUniqueId());
    }

    /**
     * Clears all debug modes (for cleanup on disable).
     */
    public static void cleanup() {
        debugModes.clear();
    }
}
