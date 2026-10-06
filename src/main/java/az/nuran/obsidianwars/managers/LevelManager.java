package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.models.PlayerLevel;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player levels and XP progression for ObsidianWars.
 * Follows BedWars1058/Hypixel-style progression curves.
 */
public class LevelManager {

    private static final Map<UUID, PlayerLevel> playerLevels = new ConcurrentHashMap<>();

    /**
     * Gets or creates a PlayerLevel for the given UUID.
     */
    public static PlayerLevel getPlayerLevel(UUID uuid) {
        return playerLevels.computeIfAbsent(uuid, PlayerLevel::new);
    }

    /**
     * Gets or creates a PlayerLevel for the given player.
     */
    public static PlayerLevel getPlayerLevel(Player player) {
        return getPlayerLevel(player.getUniqueId());
    }

    /**
     * Loads player level data from database.
     */
    public static void loadPlayerLevel(UUID uuid, int level, int xp) {
        PlayerLevel playerLevel = new PlayerLevel(uuid, level, xp);
        playerLevels.put(uuid, playerLevel);
    }

    /**
     * Removes a player's level data from memory (on quit).
     */
    public static void removePlayerLevel(UUID uuid) {
        playerLevels.remove(uuid);
    }

    /**
     * Gets the formatted level string for a player.
     */
    public static String getFormattedLevel(UUID uuid) {
        PlayerLevel playerLevel = playerLevels.get(uuid);
        return playerLevel != null ? playerLevel.getLevelName() : "§7[1★]";
    }

    /**
     * Gets the formatted level string for a player.
     */
    public static String getFormattedLevel(Player player) {
        return getFormattedLevel(player.getUniqueId());
    }

    /**
     * Gets the level number for a player.
     */
    public static int getLevel(UUID uuid) {
        PlayerLevel playerLevel = playerLevels.get(uuid);
        return playerLevel != null ? playerLevel.getLevel() : 1;
    }

    /**
     * Gets the level number for a player.
     */
    public static int getLevel(Player player) {
        return getLevel(player.getUniqueId());
    }

    /**
     * Gets the current XP for a player.
     */
    public static int getCurrentXp(UUID uuid) {
        PlayerLevel playerLevel = playerLevels.get(uuid);
        return playerLevel != null ? playerLevel.getCurrentXp() : 0;
    }

    /**
     * Gets the current XP for a player.
     */
    public static int getCurrentXp(Player player) {
        return getCurrentXp(player.getUniqueId());
    }

    /**
     * Gets the required XP for next level for a player.
     */
    public static int getRequiredXp(UUID uuid) {
        PlayerLevel playerLevel = playerLevels.get(uuid);
        return playerLevel != null ? playerLevel.getNextLevelCost() : 1000;
    }

    /**
     * Gets the required XP for next level for a player.
     */
    public static int getRequiredXp(Player player) {
        return getRequiredXp(player.getUniqueId());
    }

    /**
     * Gets the progress bar for a player.
     */
    public static String getProgressBar(UUID uuid) {
        PlayerLevel playerLevel = playerLevels.get(uuid);
        return playerLevel != null ? playerLevel.getProgressBar() : "§8[§7■■■■■■■■■■§8]";
    }

    /**
     * Gets the progress bar for a player.
     */
    public static String getProgressBar(Player player) {
        return getProgressBar(player.getUniqueId());
    }

    /**
     * Adds XP to a player.
     */
    public static void addXp(UUID uuid, int xp) {
        PlayerLevel playerLevel = getPlayerLevel(uuid);
        playerLevel.addXp(xp);
    }

    /**
     * Adds XP to a player.
     */
    public static void addXp(Player player, int xp) {
        addXp(player.getUniqueId(), xp);
    }

    /**
     * Cleanup method called on plugin disable.
     */
    public static void cleanup() {
        playerLevels.clear();
    }
}
