package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player statistics for ObsidianWars.
 * Tracks kills, deaths, wins, losses, games played, and more.
 * Integrates with database for persistent storage.
 */
public class StatsManager {

    private static final Map<UUID, PlayerStats> playerStats = new HashMap<>();

    /**
     * Data class for player statistics.
     */
    public static class PlayerStats {
        private final UUID uuid;
        private int kills;
        private int deaths;
        private int finalKills;
        private int finalDeaths;
        private int wins;
        private int losses;
        private int gamesPlayed;
        private int obsidianBroken;
        private int obsidianLost;
        private int winstreak;
        private int longestKillStreak;

        // Time-framed stats
        private int dailyWins;
        private int dailyObsidianBroken;
        private int dailyFinalKills;
        private int weeklyWins;
        private int weeklyObsidianBroken;
        private int weeklyFinalKills;
        private int monthlyWins;
        private int monthlyObsidianBroken;
        private int monthlyFinalKills;

        public PlayerStats(UUID uuid) {
            this.uuid = uuid;
            this.kills = 0;
            this.deaths = 0;
            this.finalKills = 0;
            this.finalDeaths = 0;
            this.wins = 0;
            this.losses = 0;
            this.gamesPlayed = 0;
            this.obsidianBroken = 0;
            this.obsidianLost = 0;
            this.winstreak = 0;
            this.longestKillStreak = 0;

            this.dailyWins = 0;
            this.dailyObsidianBroken = 0;
            this.dailyFinalKills = 0;
            this.weeklyWins = 0;
            this.weeklyObsidianBroken = 0;
            this.weeklyFinalKills = 0;
            this.monthlyWins = 0;
            this.monthlyObsidianBroken = 0;
            this.monthlyFinalKills = 0;
        }

        // Getters
        public UUID getUuid() { return uuid; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public int getFinalKills() { return finalKills; }
        public int getFinalDeaths() { return finalDeaths; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        public int getGamesPlayed() { return gamesPlayed; }
        public int getObsidianBroken() { return obsidianBroken; }
        public int getObsidianLost() { return obsidianLost; }
        public int getWinstreak() { return winstreak; }
        public int getLongestKillStreak() { return longestKillStreak; }

        public int getDailyWins() { return dailyWins; }
        public int getDailyObsidianBroken() { return dailyObsidianBroken; }
        public int getDailyFinalKills() { return dailyFinalKills; }
        public int getWeeklyWins() { return weeklyWins; }
        public int getWeeklyObsidianBroken() { return weeklyObsidianBroken; }
        public int getWeeklyFinalKills() { return weeklyFinalKills; }
        public int getMonthlyWins() { return monthlyWins; }
        public int getMonthlyObsidianBroken() { return monthlyObsidianBroken; }
        public int getMonthlyFinalKills() { return monthlyFinalKills; }

        // Calculated fields
        public double getKDRatio() {
            return deaths == 0 ? (double) kills : (double) kills / deaths;
        }

        public double getFinalKDRatio() {
            return finalDeaths == 0 ? (double) finalKills : (double) finalKills / finalDeaths;
        }

        public double getWinRate() {
            return gamesPlayed == 0 ? 0.0 : (double) wins / gamesPlayed * 100;
        }

        // Setters for database loading
        public void setGamesPlayed(int value) { this.gamesPlayed = value; }
        public void setWins(int value) { this.wins = value; }
        public void setLosses(int value) { this.losses = value; }
        public void setKills(int value) { this.kills = value; }
        public void setDeaths(int value) { this.deaths = value; }
        public void setFinalKills(int value) { this.finalKills = value; }
        public void setFinalDeaths(int value) { this.finalDeaths = value; }
        public void setObsidianBroken(int value) { this.obsidianBroken = value; }
        public void setObsidianLost(int value) { this.obsidianLost = value; }
        public void setWinstreak(int value) { this.winstreak = value; }

        public void setDailyWins(int value) { this.dailyWins = value; }
        public void setDailyObsidianBroken(int value) { this.dailyObsidianBroken = value; }
        public void setDailyFinalKills(int value) { this.dailyFinalKills = value; }
        public void setWeeklyWins(int value) { this.weeklyWins = value; }
        public void setWeeklyObsidianBroken(int value) { this.weeklyObsidianBroken = value; }
        public void setWeeklyFinalKills(int value) { this.weeklyFinalKills = value; }
        public void setMonthlyWins(int value) { this.monthlyWins = value; }
        public void setMonthlyObsidianBroken(int value) { this.monthlyObsidianBroken = value; }
        public void setMonthlyFinalKills(int value) { this.monthlyFinalKills = value; }
        public void setLongestKillStreak(int value) { this.longestKillStreak = value; }

        // Increment methods
        public void addKill() { kills++; }
        public void addDeath() { deaths++; }
        public void addFinalKill() { finalKills++; }
        public void addFinalDeath() { finalDeaths++; }
        public void addWin() {
            wins++;
            gamesPlayed++;
            winstreak++;
            dailyWins++;
            weeklyWins++;
            monthlyWins++;
        }
        public void addLoss() {
            losses++;
            gamesPlayed++;
            winstreak = 0;
        }
        public void addObsidianBroken() {
            obsidianBroken++;
            dailyObsidianBroken++;
            weeklyObsidianBroken++;
            monthlyObsidianBroken++;
        }
        public void addObsidianLost() { obsidianLost++; }
    }

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
     * Saves player stats to database asynchronously.
     */
    public static void savePlayerStats(UUID uuid, String username) {
        PlayerStats stats = playerStats.get(uuid);
        if (stats != null) {
            StatsDAO.savePlayerStats(uuid, username, stats);
        }
    }

    /**
     * Saves all player stats to database synchronously.
     * Used during plugin shutdown to ensure all data is saved.
     */
    public static void saveAllStats() {
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            String username = player != null ? player.getName() : "Unknown";
            try (Connection conn = DatabaseManager.getConnection()) {
                StatsDAO.savePlayerStatsSync(conn, uuid, username, entry.getValue());
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().warning("Failed to save stats for " + uuid + " during shutdown: " + e.getMessage());
            }
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
        Inventory gui = Bukkit.createInventory(null, 54, "§6§l" + target.getName() + "'s Stats");

        // Info item
        ItemStack info = createInfoItem(target, stats);
        gui.setItem(13, info);

        // Kills item
        ItemStack kills = createStatItem("§cKills", String.valueOf(stats.getKills()), org.bukkit.Material.DIAMOND_SWORD);
        gui.setItem(20, kills);

        // Deaths item
        ItemStack deaths = createStatItem("§7Deaths", String.valueOf(stats.getDeaths()), org.bukkit.Material.PLAYER_HEAD);
        gui.setItem(21, deaths);

        // K/D Ratio item
        ItemStack kd = createStatItem("§eK/D Ratio", String.format("%.2f", stats.getKDRatio()), org.bukkit.Material.BOOK);
        gui.setItem(22, kd);

        // Final Kills item
        ItemStack finalKills = createStatItem("§4Final Kills", String.valueOf(stats.getFinalKills()), org.bukkit.Material.IRON_SWORD);
        gui.setItem(23, finalKills);

        // Final Deaths item
        ItemStack finalDeaths = createStatItem("§8Final Deaths", String.valueOf(stats.getFinalDeaths()), org.bukkit.Material.SKELETON_SKULL);
        gui.setItem(24, finalDeaths);

        // Final K/D Ratio item
        ItemStack finalKd = createStatItem("§cFinal K/D", String.format("%.2f", stats.getFinalKDRatio()), org.bukkit.Material.ENCHANTED_BOOK);
        gui.setItem(25, finalKd);

        // Wins item
        ItemStack wins = createStatItem("§aWins", String.valueOf(stats.getWins()), org.bukkit.Material.GOLD_INGOT);
        gui.setItem(30, wins);

        // Losses item
        ItemStack losses = createStatItem("§cLosses", String.valueOf(stats.getLosses()), org.bukkit.Material.REDSTONE);
        gui.setItem(31, losses);

        // Win Rate item
        ItemStack winRate = createStatItem("§bWin Rate", String.format("%.1f%%", stats.getWinRate()), org.bukkit.Material.EMERALD);
        gui.setItem(32, winRate);

        // Games Played item
        ItemStack games = createStatItem("§6Games Played", String.valueOf(stats.getGamesPlayed()), org.bukkit.Material.COMPASS);
        gui.setItem(33, games);

        // Obsidians Destroyed item
        ItemStack obsidians = createStatItem("§dObsidians", String.valueOf(stats.getObsidianBroken()), org.bukkit.Material.OBSIDIAN);
        gui.setItem(38, obsidians);

        // Obsidians Lost item
        ItemStack obsidiansLost = createStatItem("§8Obsidians Lost", String.valueOf(stats.getObsidianLost()), org.bukkit.Material.OBSIDIAN);
        gui.setItem(39, obsidiansLost);

        // Winstreak item
        ItemStack winstreak = createStatItem("§6Winstreak", String.valueOf(stats.getWinstreak()), org.bukkit.Material.BEACON);
        gui.setItem(40, winstreak);

        // Longest Kill Streak item
        ItemStack streak = createStatItem("§eBest Streak", String.valueOf(stats.getLongestKillStreak()), org.bukkit.Material.BLAZE_POWDER);
        gui.setItem(41, streak);

        viewer.openInventory(gui);
    }

    private static ItemStack createInfoItem(Player player, PlayerStats stats) {
        ItemStack item = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l" + player.getName());
            meta.setLore(java.util.Arrays.asList(
                "§7Games Played: §f" + stats.getGamesPlayed(),
                "§7Win Rate: §f" + String.format("%.1f%%", stats.getWinRate()),
                "§7K/D Ratio: §f" + String.format("%.2f", stats.getKDRatio()),
                "§7Final K/D: §f" + String.format("%.2f", stats.getFinalKDRatio()),
                "§7Winstreak: §f" + stats.getWinstreak(),
                "§7Best Streak: §f" + stats.getLongestKillStreak()
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createStatItem(String name, String value, org.bukkit.Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.Arrays.asList("§f" + value));
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
     */
    public static void cleanup() {
        saveAllStats();
        // Save all player levels synchronously
        for (UUID uuid : playerStats.keySet()) {
            try (Connection conn = DatabaseManager.getConnection()) {
                StatsDAO.savePlayerLevelSync(conn, uuid);
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().warning("Failed to save level for " + uuid + " during shutdown: " + e.getMessage());
            }
        }
        playerStats.clear();
        LevelManager.cleanup();
    }
}
