package az.nuran.obsidianwars.managers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.models.PlayerStats;
import az.nuran.obsidianwars.services.StatsDAO;
import az.nuran.obsidianwars.managers.LanguageManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Manages player statistics for ObsidianWars.
 * Tracks kills, deaths, wins, losses, games played, and more.
 * Integrates with database for persistent storage.
 */
public class StatsManager {

    private static final Map<UUID, PlayerStats> playerStats = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Gets or creates player stats for a UUID.
     * Only returns stats from memory (fast, non-blocking).
     * Database loading happens asynchronously via StatsListener on player join.
     */
    public static PlayerStats getPlayerStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, PlayerStats::new);
    }

    /**
     * Gets player stats for a player.
     */
    public static PlayerStats getPlayerStats(Player player) {
        return getPlayerStats(player.getUniqueId());
    }

    /**
     * Loads player stats from database asynchronously.
     */
    public static void loadPlayerStats(UUID uuid) {
        DatabaseManager.executeAsync(conn -> {
            try {
                PlayerStats stats = StatsDAO.loadPlayerStats(conn, uuid);
                playerStats.put(uuid, stats);
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().warning("Failed to load stats for " + uuid + ": " + e.getMessage());
                // Initialize default stats on error
                playerStats.put(uuid, new PlayerStats(uuid));
                LevelManager.loadPlayerLevel(uuid, 1, 0);
            }
        });
    }

    /**
     * Forces a synchronous reload of player stats from database.
     * Use this when you need to ensure fresh data immediately (e.g., after database reset).
     */
    public static void reloadPlayerStatsSync(UUID uuid) {
        try {
            PlayerStats stats = StatsDAO.loadPlayerStats(uuid);
            playerStats.put(uuid, stats);
            Obsidianwars.getInstance().getLogger().info("Reloaded stats for " + uuid + " from database");
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().warning("Failed to reload stats for " + uuid + ": " + e.getMessage());
            // Initialize default stats on error
            playerStats.put(uuid, new PlayerStats(uuid));
            LevelManager.loadPlayerLevel(uuid, 1, 0);
        }
    }

    /**
     * Saves player stats to database asynchronously.
     */
    public static void savePlayerStats(UUID uuid, String username) {
        PlayerStats stats = playerStats.get(uuid);
        if (stats != null) {
            StatsDAO.savePlayerStats(uuid, username, stats);
        }
    }

    /**
     * Saves all player stats to database asynchronously with timeout.
     * Used during plugin shutdown to ensure all data is saved without blocking main thread.
     */
    public static void saveAllStats() {
        CompletableFuture<Void> saveFuture = CompletableFuture.runAsync(() -> {
            for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
                UUID uuid = entry.getKey();
                Player player = Bukkit.getPlayer(uuid);
                String username = player != null ? player.getName() : "Unknown";
                try (Connection conn = DatabaseManager.getConnection()) {
                    StatsDAO.savePlayerStatsSync(conn, uuid, username, entry.getValue());
                } catch (SQLException e) {
                    Obsidianwars.getInstance().getLogger().warning(
                        "Failed to save stats for " + username + " (" + uuid + "): " + e.getMessage()
                    );
                }
            }
        }, DatabaseManager.getDbExecutor());

        try {
            // Wait max 5 seconds for save to complete
            saveFuture.get(5, TimeUnit.SECONDS);
            Obsidianwars.getInstance().getLogger().info("All player stats saved successfully");
        } catch (TimeoutException e) {
            Obsidianwars.getInstance().getLogger().warning(
                "Stats save incomplete - timed out after 5 seconds. Some data may not be saved."
            );
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().log(
                java.util.logging.Level.SEVERE,
                "Error saving stats",
                e
            );
        }
    }

    /**
     * Resets expired time-framed stats (daily, weekly, monthly).
     */
    public static void resetExpiredTimeFramedStats() {
        StatsDAO.resetExpiredTimeFramedStats();
    }

    /**
     * Opens the stats GUI for a player.
     */
    public static void openStatsGUI(Player player) {
        openStatsGUI(player, player);
    }

    /**
     * Opens the stats GUI for a target player (viewed by another player).
     */
    public static void openStatsGUI(Player viewer, Player target) {
        PlayerStats stats = getPlayerStats(target);
        String titleSuffix = LanguageManager.getInstance().getMessage(viewer, "gui_stats_title_suffix");
        String title = LanguageManager.getInstance().getMessage(viewer, "gui_stats_title");
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtils.translate(title + " " + target.getName() + titleSuffix));

        // Info item
        ItemStack info = createInfoItem(viewer, target, stats);
        gui.setItem(13, info);

        // Kills item
        String killsName = LanguageManager.getInstance().getMessage(viewer, "stat_kills_name");
        ItemStack kills = createStatItem(killsName, String.valueOf(stats.getKills()), org.bukkit.Material.DIAMOND_SWORD);
        gui.setItem(20, kills);

        // Deaths item
        String deathsName = LanguageManager.getInstance().getMessage(viewer, "stat_deaths_name");
        ItemStack deaths = createStatItem(deathsName, String.valueOf(stats.getDeaths()), org.bukkit.Material.PLAYER_HEAD);
        gui.setItem(21, deaths);

        // K/D Ratio item
        String kdName = LanguageManager.getInstance().getMessage(viewer, "stat_kd_name");
        ItemStack kd = createStatItem(kdName, String.format("%.2f", stats.getKDRatio()), org.bukkit.Material.BOOK);
        gui.setItem(22, kd);

        // Final Kills item
        String finalKillsName = LanguageManager.getInstance().getMessage(viewer, "stat_final_kills_name");
        ItemStack finalKills = createStatItem(finalKillsName, String.valueOf(stats.getFinalKills()), org.bukkit.Material.IRON_SWORD);
        gui.setItem(23, finalKills);

        // Final Deaths item
        String finalDeathsName = LanguageManager.getInstance().getMessage(viewer, "stat_final_deaths_name");
        ItemStack finalDeaths = createStatItem(finalDeathsName, String.valueOf(stats.getFinalDeaths()), org.bukkit.Material.SKELETON_SKULL);
        gui.setItem(24, finalDeaths);

        // Final K/D Ratio item
        String finalKdName = LanguageManager.getInstance().getMessage(viewer, "stat_final_kd_name");
        ItemStack finalKd = createStatItem(finalKdName, String.format("%.2f", stats.getFinalKDRatio()), org.bukkit.Material.ENCHANTED_BOOK);
        gui.setItem(25, finalKd);

        // Wins item
        String winsName = LanguageManager.getInstance().getMessage(viewer, "stat_wins_name");
        ItemStack wins = createStatItem(winsName, String.valueOf(stats.getWins()), org.bukkit.Material.GOLD_INGOT);
        gui.setItem(30, wins);

        // Losses item
        String lossesName = LanguageManager.getInstance().getMessage(viewer, "stat_losses_name");
        ItemStack losses = createStatItem(lossesName, String.valueOf(stats.getLosses()), org.bukkit.Material.REDSTONE);
        gui.setItem(31, losses);

        // Win Rate item
        String winRateName = LanguageManager.getInstance().getMessage(viewer, "stat_win_rate_name");
        ItemStack winRate = createStatItem(winRateName, String.format("%.1f%%", stats.getWinRate()), org.bukkit.Material.EMERALD);
        gui.setItem(32, winRate);

        // Games Played item
        String gamesName = LanguageManager.getInstance().getMessage(viewer, "stat_games_played_name");
        ItemStack games = createStatItem(gamesName, String.valueOf(stats.getGamesPlayed()), org.bukkit.Material.COMPASS);
        gui.setItem(33, games);

        // Obsidians Destroyed item
        String obsidiansName = LanguageManager.getInstance().getMessage(viewer, "stat_obsidians_name");
        ItemStack obsidians = createStatItem(obsidiansName, String.valueOf(stats.getObsidianBroken()), org.bukkit.Material.OBSIDIAN);
        gui.setItem(38, obsidians);

        // Obsidians Lost item
        String obsidiansLostName = LanguageManager.getInstance().getMessage(viewer, "stat_obsidians_lost_name");
        ItemStack obsidiansLost = createStatItem(obsidiansLostName, String.valueOf(stats.getObsidianLost()), org.bukkit.Material.OBSIDIAN);
        gui.setItem(39, obsidiansLost);

        // Winstreak item
        String winstreakName = LanguageManager.getInstance().getMessage(viewer, "stat_winstreak_name");
        ItemStack winstreak = createStatItem(winstreakName, String.valueOf(stats.getWinstreak()), org.bukkit.Material.BEACON);
        gui.setItem(40, winstreak);

        // Longest Kill Streak item
        String streakName = LanguageManager.getInstance().getMessage(viewer, "stat_best_streak_name");
        ItemStack streak = createStatItem(streakName, String.valueOf(stats.getLongestKillStreak()), org.bukkit.Material.BLAZE_POWDER);
        gui.setItem(41, streak);

        // Level item
        String levelName = LanguageManager.getInstance().getMessage(viewer, "stat_level_name");
        ItemStack level = createStatItem(levelName, String.valueOf(stats.getLevel()) + " \u2605", org.bukkit.Material.NETHER_STAR);
        gui.setItem(47, level);

        // XP item
        String xpName = LanguageManager.getInstance().getMessage(viewer, "stat_xp_name");
        ItemStack xp = createStatItem(xpName, String.valueOf(stats.getXp()), org.bukkit.Material.EXPERIENCE_BOTTLE);
        gui.setItem(48, xp);

        viewer.openInventory(gui);
    }

    private static ItemStack createInfoItem(Player viewer, Player player, PlayerStats stats) {
        ItemStack item = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String headName = LanguageManager.getInstance().getMessage(viewer, "player_head_name", "player", player.getName());
            meta.setDisplayName(ColorUtils.translate("&6&l" + headName));
            meta.setLore(ColorUtils.translate(java.util.Arrays.asList(
                "&7Level: &f" + stats.getLevel() + " \u2605",
                "&7XP: &f" + stats.getXp(),
                "&7Games Played: &f" + stats.getGamesPlayed(),
                "&7Win Rate: &f" + String.format("%.1f%%", stats.getWinRate()),
                "&7K/D Ratio: &f" + String.format("%.2f", stats.getKDRatio()),
                "&7Final K/D: &f" + String.format("%.2f", stats.getFinalKDRatio()),
                "&7Winstreak: &f" + stats.getWinstreak(),
                "&7Best Streak: &f" + stats.getLongestKillStreak()
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createStatItem(String name, String value, org.bukkit.Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.translate(name));
            meta.setLore(ColorUtils.translate(java.util.Arrays.asList("&f" + value)));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Clears all stats (for testing/reset).
     */
    public static void clearAllStats() {
        playerStats.clear();
    }

    /**
     * Clears stats for a specific player.
     */
    public static void clearPlayerStats(UUID uuid) {
        playerStats.remove(uuid);
    }

    /**
     * Gets the number of kills for a player.
     */
    public static int getKills(UUID uuid) {
        PlayerStats stats = playerStats.get(uuid);
        return stats != null ? stats.getKills() : 0;
    }

    /**
     * Gets the number of deaths for a player.
     */
    public static int getDeaths(UUID uuid) {
        PlayerStats stats = playerStats.get(uuid);
        return stats != null ? stats.getDeaths() : 0;
    }

    /**
     * Gets the number of wins for a player.
     */
    public static int getWins(UUID uuid) {
        PlayerStats stats = playerStats.get(uuid);
        return stats != null ? stats.getWins() : 0;
    }

    /**
     * Gets the current winstreak for a player.
     */
    public static int getWinstreak(UUID uuid) {
        PlayerStats stats = playerStats.get(uuid);
        return stats != null ? stats.getWinstreak() : 0;
    }

    /**
     * Cleanup method called on plugin disable.
     * Saves all stats and levels asynchronously with timeout.
     */
    public static void cleanup() {
        saveAllStats();

        // Save all player levels asynchronously with timeout
        CompletableFuture<Void> levelSaveFuture = CompletableFuture.runAsync(() -> {
            for (UUID uuid : playerStats.keySet()) {
                try (Connection conn = DatabaseManager.getConnection()) {
                    StatsDAO.savePlayerLevelSync(conn, uuid);
                } catch (SQLException e) {
                    Obsidianwars.getInstance().getLogger().warning(
                        "Failed to save level for " + uuid + " during shutdown: " + e.getMessage()
                    );
                }
            }
        }, DatabaseManager.getDbExecutor());

        try {
            // Wait max 5 seconds for level save to complete
            levelSaveFuture.get(5, TimeUnit.SECONDS);
            Obsidianwars.getInstance().getLogger().info("All player levels saved successfully");
        } catch (TimeoutException e) {
            Obsidianwars.getInstance().getLogger().warning(
                "Level save incomplete - timed out after 5 seconds. Some data may not be saved."
            );
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().log(
                java.util.logging.Level.SEVERE,
                "Error saving levels",
                e
            );
        }

        playerStats.clear();
        LevelManager.cleanup();
    }
}
