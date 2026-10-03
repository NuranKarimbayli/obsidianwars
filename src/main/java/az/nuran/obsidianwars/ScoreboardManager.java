package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private static final String SCOREBOARD_TITLE = "§d§lObsidianWars";
    private static final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    public static void updateScoreboard(Player player) {
        try {
            if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                removeScoreboard(player);
                return;
            }

            String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
            GameManager.ArenaGame game = GameManager.getGame(arenaName);

            // Reuse existing scoreboard or create new one
            Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
            if (scoreboard == null) {
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                playerScoreboards.put(player.getUniqueId(), scoreboard);
            }

            // Əgər oyun aktivdirsə, oyun scoreboard-u göstəririk
            if (game != null && game.getGameState() == GameManager.GameState.PLAYING) {
                updateGameScoreboard(scoreboard, player, arenaName, game);
            } else {
                // Əks halda lobby scoreboard-u
                updateLobbyScoreboard(scoreboard, player, arenaName);
            }

            player.setScoreboard(scoreboard);
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().warning("Error updating scoreboard for player " + player.getName() + ": " + e.getMessage());
        }
    }

    private static void updateLobbyScoreboard(Scoreboard scoreboard, Player player, String arenaName) {
        // Clear existing objectives
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);

        Objective objective = scoreboard.getObjective("obsidianwars_lobby");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("obsidianwars_lobby", "dummy");
        }
        objective.setDisplayName(SCOREBOARD_TITLE);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Line 1: Empty space
        Score empty1 = objective.getScore(" ");
        empty1.setScore(8);

        // Line 2: Arena Name
        Score arenaLine = objective.getScore("§6Map: §f" + arenaName);
        arenaLine.setScore(7);

        // Line 3: Empty space
        Score empty2 = objective.getScore("  ");
        empty2.setScore(6);

        // Line 4: Player Count
        int currentPlayers = getArenaPlayerCount(arenaName);
        int maxPlayers = getMaxPlayers(arenaName);
        Score playersLine = objective.getScore("§eOyunçular: §f" + currentPlayers + "/" + maxPlayers);
        playersLine.setScore(5);

        // Line 5: Empty space
        Score empty3 = objective.getScore("   ");
        empty3.setScore(4);

        // Line 6: Timer (countdown or preparation)
        String timerDisplay = getTimerDisplay(arenaName);
        Score timerLine = objective.getScore("§cVaxt: §f" + timerDisplay);
        timerLine.setScore(3);

        // Line 7: Empty space
        Score empty4 = objective.getScore("    ");
        empty4.setScore(2);

        // Line 8: Selected Team
        String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
        String teamDisplay = playerTeam == null ? "§7Yoxdur" : (playerTeam.equals("red") ? "§cQırmızı" : "§9Mavi");
        Score teamLine = objective.getScore("§bKomanda: " + teamDisplay);
        teamLine.setScore(1);

        // Line 9: Footer
        Score footer = objective.getScore("§7play.obsidianwars.com");
        footer.setScore(0);
    }

    private static void updateGameScoreboard(Scoreboard scoreboard, Player player, String arenaName, GameManager.ArenaGame game) {
        // Clear existing objectives
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);

        Objective objective = scoreboard.getObjective("obsidianwars_game");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("obsidianwars_game", "dummy");
        }
        objective.setDisplayName(SCOREBOARD_TITLE);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Check if sudden death is active
        int suddenDeathRemaining = WallManager.getSuddenDeathRemainingTime(arenaName);
        boolean isSuddenDeath = suddenDeathRemaining > 0;

        int lineScore = 10;

        // Line 1: Empty space
        Score empty1 = objective.getScore(" ");
        empty1.setScore(lineScore--);

        // Line 2: Arena Name
        Score arenaLine = objective.getScore("§6Map: §f" + arenaName);
        arenaLine.setScore(lineScore--);

        // Line 3: Empty space
        Score empty2 = objective.getScore("  ");
        empty2.setScore(lineScore--);

        // Line 4: Sudden Death Timer (if active) or Game Timer
        if (isSuddenDeath) {
            int minutes = suddenDeathRemaining / 60;
            int seconds = suddenDeathRemaining % 60;
            String timeStr = String.format("%02d:%02d", minutes, seconds);
            Score suddenDeathLine = objective.getScore("§c§lSUDDEN DEATH: §f" + timeStr);
            suddenDeathLine.setScore(lineScore--);
        } else {
            Score timerLine = objective.getScore("§eVaxt: §f" + getGameTime(game));
            timerLine.setScore(lineScore--);
        }

        // Line 5: Empty space
        Score empty3 = objective.getScore("   ");
        empty3.setScore(lineScore--);

        // Line 6: Red Team Status
        String redStatus = game.isObsidianDestroyed("red") ? "§c✘" : "§a✔";
        int redPlayers = getTeamPlayerCount(arenaName, "red");
        Score redLine = objective.getScore("§cQırmızı: " + redStatus + " §f(" + redPlayers + ")");
        redLine.setScore(lineScore--);

        // Line 7: Blue Team Status
        String blueStatus = game.isObsidianDestroyed("blue") ? "§c✘" : "§a✔";
        int bluePlayers = getTeamPlayerCount(arenaName, "blue");
        Score blueLine = objective.getScore("§9Mavi: " + blueStatus + " §f(" + bluePlayers + ")");
        blueLine.setScore(lineScore--);

        // Line 8: Empty space
        Score empty4 = objective.getScore("    ");
        empty4.setScore(lineScore--);

        // Line 9: Footer
        Score footer = objective.getScore("§7play.obsidianwars.com");
        footer.setScore(lineScore--);
    }

    public static void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public static void cleanup() {
        playerScoreboards.clear();
    }

    private static int getArenaPlayerCount(String arenaName) {
        int count = 0;
        for (String arena : ObsidianCommand.playersInArena.values()) {
            if (arena.equals(arenaName)) {
                count++;
            }
        }
        return count;
    }

    private static int getTeamPlayerCount(String arenaName, String team) {
        int count = 0;
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                String playerTeam = TeamListener.playerTeams.get(uuid);
                if (team.equals(playerTeam)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int getMaxPlayers(String arenaName) {
        return ArenaConfigManager.getMaxPlayers(arenaName);
    }

    private static String getGameCountdown(String arenaName) {
        int countdown = GameManager.getCountdown(arenaName);
        if (countdown > 0) {
            int minutes = countdown / 60;
            int seconds = countdown % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
        return "Gözləyir...";
    }

    private static String getTimerDisplay(String arenaName) {
        // Check if game is in countdown phase
        int countdown = GameManager.getCountdown(arenaName);
        if (countdown > 0) {
            return getGameCountdown(arenaName);
        }
        
        // Check if game is in preparation phase
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game != null && game.getGameState() == GameManager.GameState.PLAYING) {
            // Check if preparation timer is running (can be determined from WallManager)
            int prepTime = WallManager.getTimer(arenaName);
            if (prepTime > 0) {
                // Format as MM:SS (assuming prep time is in minutes)
                return String.format("%02d:00", prepTime);
            }
            // Return game time
            return getGameTime(game);
        }
        
        return "Gözləyir...";
    }

    private static String getGameTime(GameManager.ArenaGame game) {
        // Oyun vaxtını hesablayırıq (MM:SS format)
        int totalSeconds = game.getGameTime();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}