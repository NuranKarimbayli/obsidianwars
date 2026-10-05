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

    // Global flags for console and chat debug
    private static boolean isConsoleDebugEnabled = false;
    private static boolean isChatDebugEnabled = false;

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

        boolean shouldLogToConsole = false;
        boolean shouldLogToChat = false;

        // Check if any admin wants console or chat output
        for (DebugMode mode : debugModes.values()) {
            if (mode == DebugMode.CONSOLE || mode == DebugMode.BOTH) {
                shouldLogToConsole = true;
            }
            if (mode == DebugMode.CHAT || mode == DebugMode.BOTH) {
                shouldLogToChat = true;
            }
        }

        // Log to console once if any admin has console mode AND console debug is globally enabled
        if (shouldLogToConsole && isConsoleDebugEnabled) {
            Obsidianwars.getInstance().getLogger().info(formattedMessage);
        }

        // Send to chat for each admin with chat mode AND chat debug is globally enabled
        if (shouldLogToChat && isChatDebugEnabled) {
            for (Map.Entry<UUID, DebugMode> entry : debugModes.entrySet()) {
                DebugMode mode = entry.getValue();
                if (mode == DebugMode.CHAT || mode == DebugMode.BOTH) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.sendMessage("§7[DEBUG] §f" + formattedMessage);
                    }
                }
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

    /**
     * Gets the console debug enabled state.
     * @return true if console debug is enabled
     */
    public static boolean isConsoleDebugEnabled() {
        return isConsoleDebugEnabled;
    }

    /**
     * Sets the console debug enabled state.
     * @param enabled true to enable console debug
     */
    public static void setConsoleDebugEnabled(boolean enabled) {
        isConsoleDebugEnabled = enabled;
    }

    /**
     * Gets the chat debug enabled state.
     * @return true if chat debug is enabled
     */
    public static boolean isChatDebugEnabled() {
        return isChatDebugEnabled;
    }

    /**
     * Sets the chat debug enabled state.
     * @param enabled true to enable chat debug
     */
    public static void setChatDebugEnabled(boolean enabled) {
        isChatDebugEnabled = enabled;
    }
}
