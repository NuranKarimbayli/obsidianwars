package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the lobby scoreboard for players not in an arena.
 * Displays player stats, level progress, and server information.
 */
public class LobbyScoreboardManager {

    private static final Map<UUID, Scoreboard> lobbyScoreboards = new HashMap<>();
    private static final Map<UUID, Long> lastUpdateTime = new HashMap<>();
    private static final long UPDATE_COOLDOWN_MS = 1000; // 1 second cooldown for lobby

    /**
     * Updates the lobby scoreboard for a player.
     * Only displays if the player is not in an arena.
     */
    public static void updateLobbyScoreboard(Player player) {
        // Check if lobby scoreboard is enabled in config
        if (!Obsidianwars.getInstance().getConfig().getBoolean("lobby-scoreboard.enabled", true)) {
            removeLobbyScoreboard(player);
            return;
        }

        UUID uuid = player.getUniqueId();

        // If player is in an arena, don't show lobby scoreboard
        if (ObsidianCommand.playersInArena.containsKey(uuid)) {
            removeLobbyScoreboard(player);
            return;
        }

        // Rate limiter check
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(uuid);

        if (lastUpdate != null && (currentTime - lastUpdate) < UPDATE_COOLDOWN_MS) {
            return; // Skip update due to cooldown
        }

        lastUpdateTime.put(uuid, currentTime);

        try {
            // Get or create scoreboard
            Scoreboard scoreboard = lobbyScoreboards.get(uuid);
            if (scoreboard == null) {
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                lobbyScoreboards.put(uuid, scoreboard);
            }

            // Update scoreboard with dynamic configuration
            updateDynamicLobbyScoreboard(scoreboard, player);

            player.setScoreboard(scoreboard);
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().warning("Error updating lobby scoreboard for player " + player.getName() + ": " + e.getMessage());
        }
    }

    private static void updateDynamicLobbyScoreboard(Scoreboard scoreboard, Player player) {
        // Clear existing objectives
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);

        Objective objective = scoreboard.getObjective("obsidianwars_lobby");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("obsidianwars_lobby", "dummy");
        }

        // Get title from config
        String title = Obsidianwars.getInstance().getConfig().getString("lobby-scoreboard.title", "&e&lOBSIDIAN WARS");
        title = replaceLobbyPlaceholders(title, player);
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get lines from config
        List<String> lines = Obsidianwars.getInstance().getConfig().getStringList("lobby-scoreboard.lines");

        // Set scores (reverse order to display correctly)
        int lineScore = lines.size();
        for (String line : lines) {
            String processedLine = replaceLobbyPlaceholders(line, player);
            processedLine = ChatColor.translateAlternateColorCodes('&', processedLine);

            // Skip empty lines
            if (processedLine.trim().isEmpty()) {
                lineScore--;
                continue;
            }

            Score scoreLine = objective.getScore(processedLine);
            scoreLine.setScore(lineScore--);
        }
    }

    private static String replaceLobbyPlaceholders(String text, Player player) {
        UUID uuid = player.getUniqueId();

        // Get player stats
        int kills = StatsManager.getKills(uuid);
        int wins = StatsManager.getWins(uuid);
        int winstreak = StatsManager.getWinstreak(uuid);

        // Get player level info
        int level = LevelManager.getLevel(uuid);
        String levelFormatted = LevelManager.getFormattedLevel(uuid);
        int currentXp = LevelManager.getCurrentXp(uuid);
        int requiredXp = LevelManager.getRequiredXp(uuid);
        String progressBar = LevelManager.getProgressBar(uuid);

        // Format XP with k notation
        String currentXpFormatted = formatXp(currentXp);
        String requiredXpFormatted = formatXp(requiredXp);

        // Get date in MM/dd/yy format
        String date = getCurrentDate();

        // Get server ID from config
        String serverId = Obsidianwars.getInstance().getConfig().getString("lobby-scoreboard.server-id", "L26E");

        // Get server address from config
        String serverAddress = Obsidianwars.getInstance().getConfig().getString("lobby-scoreboard.server-address", "yourserver.net");

        // Replace all placeholders
        text = text.replace("{player}", player.getName());
        text = text.replace("{date}", date);
        text = text.replace("{server_id}", serverId);
        text = text.replace("{level}", String.valueOf(level));
        text = text.replace("{level_formatted}", levelFormatted);
        text = text.replace("{xp}", String.valueOf(currentXp));
        text = text.replace("{current_xp_k}", currentXpFormatted);
        text = text.replace("{req_xp}", String.valueOf(requiredXp));
        text = text.replace("{req_xp_k}", requiredXpFormatted);
        text = text.replace("{progress_bar}", progressBar);
        text = text.replace("{kills}", String.valueOf(kills));
        text = text.replace("{wins}", String.valueOf(wins));
        text = text.replace("{winstreak}", String.valueOf(winstreak));
        text = text.replace("{server_address}", serverAddress);

        return text;
    }

    private static String formatXp(int xp) {
        if (xp >= 1000) {
            double formatted = xp / 1000.0;
            if (formatted == Math.floor(formatted)) {
                return String.format("%.0fk", formatted);
            } else {
                return String.format("%.1fk", formatted);
            }
        }
        return String.valueOf(xp);
    }

    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
        return sdf.format(new Date());
    }

    /**
     * Removes the lobby scoreboard for a player.
     */
    public static void removeLobbyScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        lobbyScoreboards.remove(uuid);
        lastUpdateTime.remove(uuid);
    }

    /**
     * Starts the periodic update task for all lobby scoreboards.
     */
    public static void initialize() {
        // Update lobby scoreboards every second
        Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), LobbyScoreboardManager::updateAllLobbyScoreboards, 0L, 20L);
    }

    private static void updateAllLobbyScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                updateLobbyScoreboard(player);
            }
        }
    }

    /**
     * Cleanup method called on plugin disable.
     */
    public static void cleanup() {
        lobbyScoreboards.clear();
        lastUpdateTime.clear();
    }
}
