package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player statistics for ObsidianWars.
 * Tracks kills, deaths, wins, losses, games played, and more.
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
        private int wins;
        private int losses;
        private int gamesPlayed;
        private int obsidiansDestroyed;
        private int longestKillStreak;

        public PlayerStats(UUID uuid) {
            this.uuid = uuid;
            this.kills = 0;
            this.deaths = 0;
            this.wins = 0;
            this.losses = 0;
            this.gamesPlayed = 0;
            this.obsidiansDestroyed = 0;
            this.longestKillStreak = 0;
        }

        public UUID getUuid() { return uuid; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        public int getGamesPlayed() { return gamesPlayed; }
        public int getObsidiansDestroyed() { return obsidiansDestroyed; }
        public int getLongestKillStreak() { return longestKillStreak; }

        public double getKDRatio() {
            return deaths == 0 ? (double) kills : (double) kills / deaths;
        }

        public double getWinRate() {
            return gamesPlayed == 0 ? 0.0 : (double) wins / gamesPlayed * 100;
        }

        public void addKill() { kills++; }
        public void addDeath() { deaths++; }
        public void addWin() { wins++; gamesPlayed++; }
        public void addLoss() { losses++; gamesPlayed++; }
        public void addObsidianDestroyed() { obsidiansDestroyed++; }
        public void setLongestKillStreak(int streak) { this.longestKillStreak = streak; }
    }

    /**
     * Gets or creates player stats for a UUID.
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

        // Wins item
        ItemStack wins = createStatItem("§aWins", String.valueOf(stats.getWins()), org.bukkit.Material.GOLD_INGOT);
        gui.setItem(23, wins);

        // Losses item
        ItemStack losses = createStatItem("§cLosses", String.valueOf(stats.getLosses()), org.bukkit.Material.REDSTONE);
        gui.setItem(24, losses);

        // Win Rate item
        ItemStack winRate = createStatItem("§bWin Rate", String.format("%.1f%%", stats.getWinRate()), org.bukkit.Material.EMERALD);
        gui.setItem(29, winRate);

        // Games Played item
        ItemStack games = createStatItem("§6Games Played", String.valueOf(stats.getGamesPlayed()), org.bukkit.Material.COMPASS);
        gui.setItem(30, games);

        // Obsidians Destroyed item
        ItemStack obsidians = createStatItem("§dObsidians", String.valueOf(stats.getObsidiansDestroyed()), org.bukkit.Material.OBSIDIAN);
        gui.setItem(31, obsidians);

        // Longest Kill Streak item
        ItemStack streak = createStatItem("§eBest Streak", String.valueOf(stats.getLongestKillStreak()), org.bukkit.Material.BLAZE_POWDER);
        gui.setItem(32, streak);

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
}
