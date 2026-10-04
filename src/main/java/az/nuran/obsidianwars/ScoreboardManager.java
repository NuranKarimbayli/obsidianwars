package az.nuran.obsidianwars;

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

    public static void updateScoreboard(Player player) {
        // Check if scoreboard is enabled in config
        if (!Obsidianwars.getInstance().getConfig().getBoolean("features.scoreboard", true)) {
            return;
        }

        // Rate limiter check
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(uuid);

        if (lastUpdate != null && (currentTime - lastUpdate) < UPDATE_COOLDOWN_MS) {
            return; // Skip update due to cooldown
        }

        lastUpdateTime.put(uuid, currentTime);

        try {
            if (!ObsidianCommand.playersInArena.containsKey(uuid)) {
                removeScoreboard(player);
                return;
            }

            String arenaName = ObsidianCommand.playersInArena.get(uuid);
            GameManager.ArenaGame game = GameManager.getGame(arenaName);

            // Use per-player scoreboard from TeamManager or create new one
            Scoreboard scoreboard = TeamManager.getPlayerScoreboard(player);
            if (scoreboard == null) {
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                playerScoreboards.put(uuid, scoreboard);
            }

            // Update scoreboard with dynamic configuration
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

        // Get title from config
        String title = Obsidianwars.getInstance().getConfig().getString("scoreboard.title", "&d&lOBSIDIAN WARS");
        title = replacePlaceholders(title, player, arenaName, game);
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get lines from config
        List<String> lines = Obsidianwars.getInstance().getConfig().getStringList("scoreboard.lines");

        // Set scores (reverse order to display correctly)
        int lineScore = lines.size();
        for (String line : lines) {
            String processedLine = replacePlaceholders(line, player, arenaName, game);
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

    private static String replacePlaceholders(String text, Player player, String arenaName, GameManager.ArenaGame game) {
        UUID uuid = player.getUniqueId();

        // Get player stats
        int kills = StatsManager.getKills(uuid);
        int deaths = StatsManager.getDeaths(uuid);

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

        return text;
    }

    private static String getTeamHealth(String arenaName, String team) {
        if (!Obsidianwars.getInstance().getConfig().getBoolean("health-display.scoreboard", true)) {
            return "0";
        }

        double totalHealth = 0;
        int playerCount = 0;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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

        if (playerCount == 0) return "0";

        // Return average health or total health based on config
        boolean useAverage = Obsidianwars.getInstance().getConfig().getBoolean("health-display.use-average", true);
        if (useAverage) {
            return String.format("%.1f", totalHealth / playerCount);
        } else {
            return String.format("%.1f", totalHealth);
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
    }
}