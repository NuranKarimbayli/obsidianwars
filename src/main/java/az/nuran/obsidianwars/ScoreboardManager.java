package az.nuran.obsidianwars;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private static final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private static final Map<UUID, Long> lastUpdateTime = new HashMap<>();
    private static final long UPDATE_COOLDOWN_MS = 500; // 500ms cooldown

    // Cache for team health calculations to avoid expensive iterations
    private static final Map<String, CachedTeamHealth> teamHealthCache = new HashMap<>();
    private static final long HEALTH_CACHE_TTL_MS = 1000; // Cache expires after 1 second

    public static void updateScoreboard(Player player) {
        // Check if scoreboard is enabled in config
        if (!Obsidianwars.getInstance().getConfig().getBoolean("features.scoreboard", true)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // If player is not in arena, remove game scoreboard and let lobby scoreboard handle it
        if (!ObsidianCommand.playersInArena.containsKey(uuid)) {
            removeScoreboard(player);
            LobbyScoreboardManager.updateLobbyScoreboard(player);
            return;
        }

        // Remove lobby scoreboard if player is in arena
        LobbyScoreboardManager.removeLobbyScoreboard(player);

        // Rate limiter check
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(uuid);

        if (lastUpdate != null && (currentTime - lastUpdate) < UPDATE_COOLDOWN_MS) {
            return; // Skip update due to cooldown
        }

        lastUpdateTime.put(uuid, currentTime);

        try {
            String arenaName = ObsidianCommand.playersInArena.get(uuid);
            if (arenaName == null) {
                return; // Player not in arena, skip scoreboard update
            }
            GameManager.ArenaGame game = GameManager.getGame(arenaName);

            // Use per-player scoreboard from TeamManager or create new one
            Scoreboard scoreboard = TeamManager.getPlayerScoreboard(player);
            if (scoreboard == null) {
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                playerScoreboards.put(uuid, scoreboard);
            }

            // Update scoreboard with dynamic configuration (handles null game gracefully)
            updateDynamicScoreboard(scoreboard, player, arenaName, game);

            player.setScoreboard(scoreboard);
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().warning("Error updating scoreboard for player " + player.getName() + ": " + e.getMessage());
        }
    }

    private static void updateDynamicScoreboard(Scoreboard scoreboard, Player player, String arenaName, GameManager.ArenaGame game) {
        // Clear existing objectives
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);

        Objective objective = scoreboard.getObjective("obsidianwars_dynamic");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("obsidianwars_dynamic", "dummy");
        }

        // Get title from scoreboards config
        String title = ScoreboardsConfigManager.getScoreboardsConfig().getString("scoreboard.title", "&d&lOBSIDIAN WARS");
        title = replacePlaceholders(title, player, arenaName, game);
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get lines from scoreboards config
        List<String> lines = ScoreboardsConfigManager.getScoreboardsConfig().getStringList("scoreboard.lines");

        // Set scores (reverse order to display correctly)
        int lineScore = lines.size();
        for (String line : lines) {
            String processedLine = replacePlaceholders(line, player, arenaName, game);

            // Parse PlaceholderAPI placeholders if PAPI is installed
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                processedLine = PlaceholderAPI.setPlaceholders(player, processedLine);
            }

            processedLine = ChatColor.translateAlternateColorCodes('&', processedLine);

            // Handle empty lines by using unique invisible characters
            if (processedLine.trim().isEmpty()) {
                // Use unique color codes for each empty line to avoid duplicates
                String emptyLine = getUniqueEmptyLine(lineScore);
                emptyLine = ChatColor.translateAlternateColorCodes('&', emptyLine);
                Score scoreLine = objective.getScore(emptyLine);
                scoreLine.setScore(lineScore--);
            } else {
                Score scoreLine = objective.getScore(processedLine);
                scoreLine.setScore(lineScore--);
            }
        }
    }

    private static String getUniqueEmptyLine(int lineScore) {
        // Use different invisible characters for each empty line to avoid duplicates
        // Each empty line gets a unique color code + space combination
        switch (lineScore % 15) {
            case 0: return "&0 ";
            case 1: return "&1 ";
            case 2: return "&2 ";
            case 3: return "&3 ";
            case 4: return "&4 ";
            case 5: return "&5 ";
            case 6: return "&6 ";
            case 7: return "&7 ";
            case 8: return "&8 ";
            case 9: return "&9 ";
            case 10: return "&a ";
            case 11: return "&b ";
            case 12: return "&c ";
            case 13: return "&d ";
            case 14: return "&e ";
            default: return "&f ";
        }
    }

    private static String replacePlaceholders(String text, Player player, String arenaName, GameManager.ArenaGame game) {
        UUID uuid = player.getUniqueId();

        // Get player stats
        int kills = StatsManager.getKills(uuid);
        int deaths = StatsManager.getDeaths(uuid);

        // Get player level info
        int level = LevelManager.getLevel(uuid);
        String levelFormatted = LevelManager.getFormattedLevel(uuid);
        int currentXp = LevelManager.getCurrentXp(uuid);
        int requiredXp = LevelManager.getRequiredXp(uuid);
        String progressBar = LevelManager.getProgressBar(uuid);

        // Get arena info
        String phase = getPhaseDisplay(arenaName, game);
        String time = getTimeDisplay(arenaName, game);

        // Get team health totals if enabled
        String redHealth = "0";
        String blueHealth = "0";
        if (Obsidianwars.getInstance().getConfig().getBoolean("health-display.scoreboard", true)) {
            redHealth = getTeamHealth(arenaName, "red");
            blueHealth = getTeamHealth(arenaName, "blue");
        }

        // Replace all placeholders
        text = text.replace("%player%", player.getName());
        text = text.replace("%arena%", arenaName);
        text = text.replace("%phase%", phase);
        text = text.replace("%time%", time);
        text = text.replace("%kills%", String.valueOf(kills));
        text = text.replace("%deaths%", String.valueOf(deaths));
        text = text.replace("%red_health%", redHealth);
        text = text.replace("%blue_health%", blueHealth);
        text = text.replace("%level%", String.valueOf(level));
        text = text.replace("%level_formatted%", levelFormatted);
        text = text.replace("%xp%", String.valueOf(currentXp));
        text = text.replace("%req_xp%", String.valueOf(requiredXp));
        text = text.replace("%progress_bar%", progressBar);

        return text;
    }

    private static String getTeamHealth(String arenaName, String team) {
        if (!Obsidianwars.getInstance().getConfig().getBoolean("health-display.scoreboard", true)) {
            return "0";
        }

        // Check cache first
        String cacheKey = arenaName + ":" + team;
        CachedTeamHealth cached = teamHealthCache.get(cacheKey);
        long currentTime = System.currentTimeMillis();

        if (cached != null && (currentTime - cached.timestamp) < HEALTH_CACHE_TTL_MS) {
            return cached.healthValue;
        }

        // Cache miss or expired - recalculate
        double totalHealth = 0;
        int playerCount = 0;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                String playerTeam = TeamListener.playerTeams.get(uuid);
                if (playerTeam != null && playerTeam.equals(team)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                        totalHealth += player.getHealth();
                        playerCount++;
                    }
                }
            }
        }

        if (playerCount == 0) {
            // Cache zero health result
            teamHealthCache.put(cacheKey, new CachedTeamHealth("0", currentTime));
            return "0";
        }

        // Return average health or total health based on config
        boolean useAverage = Obsidianwars.getInstance().getConfig().getBoolean("health-display.use-average", true);
        String healthValue;
        if (useAverage) {
            healthValue = String.format("%.1f", totalHealth / playerCount);
        } else {
            healthValue = String.format("%.1f", totalHealth);
        }

        // Update cache
        teamHealthCache.put(cacheKey, new CachedTeamHealth(healthValue, currentTime));

        return healthValue;
    }

    /**
     * Invalidates the team health cache for a specific arena.
     * Call this when a player dies, heals, or leaves the arena.
     */
    public static void invalidateTeamHealthCache(String arenaName) {
        String redKey = arenaName + ":red";
        String blueKey = arenaName + ":blue";
        teamHealthCache.remove(redKey);
        teamHealthCache.remove(blueKey);
    }

    /**
     * Data class for cached team health values.
     */
    private static class CachedTeamHealth {
        final String healthValue;
        final long timestamp;

        CachedTeamHealth(String healthValue, long timestamp) {
            this.healthValue = healthValue;
            this.timestamp = timestamp;
        }
    }

    private static String getPhaseDisplay(String arenaName, GameManager.ArenaGame game) {
        if (game == null) {
            // Check if in countdown
            int countdown = GameManager.getCountdown(arenaName);
            if (countdown > 0) {
                return "Countdown";
            }
            return "Waiting";
        }

        if (game.getGameState() == GameManager.GameState.COUNTDOWN) {
            return "Countdown";
        } else if (game.getGameState() == GameManager.GameState.PREPARATION) {
            return "Preparation";
        } else if (game.getGameState() == GameManager.GameState.PLAYING) {
            // Check if sudden death is active
            int suddenDeathRemaining = WallManager.getSuddenDeathRemainingTime(arenaName);
            if (suddenDeathRemaining > 0) {
                return "Sudden Death";
            }
            return "Playing";
        } else if (game.getGameState() == GameManager.GameState.ENDED) {
            return "Ended";
        }

        return "Waiting";
    }

    private static String getTimeDisplay(String arenaName, GameManager.ArenaGame game) {
        if (game == null) {
            int countdown = GameManager.getCountdown(arenaName);
            if (countdown > 0) {
                return countdown + "s";
            }
            return "--:--";
        }

        if (game.getGameState() == GameManager.GameState.COUNTDOWN) {
            int countdown = GameManager.getCountdown(arenaName);
            return countdown + "s";
        } else if (game.getGameState() == GameManager.GameState.PREPARATION) {
            int prepTime = WallManager.getTimer(arenaName);
            return prepTime + "m";
        } else if (game.getGameState() == GameManager.GameState.PLAYING) {
            // Check if sudden death is active
            int suddenDeathRemaining = WallManager.getSuddenDeathRemainingTime(arenaName);
            if (suddenDeathRemaining > 0) {
                int minutes = suddenDeathRemaining / 60;
                int seconds = suddenDeathRemaining % 60;
                return String.format("%02d:%02d", minutes, seconds);
            }

            // Return game time
            int gameTime = game.getGameTime();
            int minutes = gameTime / 60;
            int seconds = gameTime % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }

        return "--:--";
    }

    public static void removeScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        playerScoreboards.remove(uuid);
        lastUpdateTime.remove(uuid);
        TeamManager.removePlayerScoreboard(player);
    }

    public static void cleanup() {
        playerScoreboards.clear();
        lastUpdateTime.clear();
        teamHealthCache.clear();
    }
}