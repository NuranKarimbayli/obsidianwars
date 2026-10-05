package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class TabListManager {

    private static BukkitTask updateTask;
    private static final long UPDATE_INTERVAL_TICKS = 20L; // Update every second

    public static void initialize() {
        // Start periodic update task
        updateTask = Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), TabListManager::updateAllTabLists, 0L, UPDATE_INTERVAL_TICKS);
    }

    public static void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private static void updateAllTabLists() {
        // Check if tablist is enabled in config
        if (!Obsidianwars.getInstance().getConfig().getBoolean("features.tablist", true)) {
            return;
        }

        int onlineCount = Bukkit.getOnlinePlayers().size();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Update player's health display in tab list
            updatePlayerHealthDisplay(player);

            // Get header and footer from config
            List<String> headerLines = Obsidianwars.getInstance().getConfig().getStringList("tab.header");
            List<String> footerLines = Obsidianwars.getInstance().getConfig().getStringList("tab.footer");

            // Replace placeholders and join lines
            String header = replacePlaceholders(String.join("\n", headerLines), player, onlineCount);
            String footer = replacePlaceholders(String.join("\n", footerLines), player, onlineCount);

            // Apply color codes
            header = ChatColor.translateAlternateColorCodes('&', header);
            footer = ChatColor.translateAlternateColorCodes('&', footer);

            // Update tab list
            player.setPlayerListHeader(header);
            player.setPlayerListFooter(footer);
        }
    }

    private static void updatePlayerHealthDisplay(Player player) {
        if (!Obsidianwars.getInstance().getConfig().getBoolean("health-display.tablist", true)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!ObsidianCommand.playersInArena.containsKey(uuid)) {
            // Not in arena, remove health display but keep level
            String levelFormatted = LevelManager.getFormattedLevel(uuid);
            player.setPlayerListName(levelFormatted + " " + player.getName());
            return;
        }

        String healthDisplay = getHealthDisplay(player);
        String levelFormatted = LevelManager.getFormattedLevel(uuid);
        player.setPlayerListName(levelFormatted + " " + player.getName() + " " + healthDisplay);
    }

    private static String getHealthDisplay(Player player) {
        String format = Obsidianwars.getInstance().getConfig().getString("health-display.format", "hearts");

        if (format.equals("hearts")) {
            double health = player.getHealth();
            int hearts = (int) Math.ceil(health / 2);
            StringBuilder sb = new StringBuilder();
            sb.append("§c");
            for (int i = 0; i < hearts; i++) {
                sb.append("❤");
            }
            if (health % 2 > 0) {
                sb.append("§e❤"); // Half heart
            }
            return sb.toString();
        } else {
            // Number format
            return "§c" + String.format("%.1f", player.getHealth());
        }
    }

    private static String replacePlaceholders(String text, Player player, int onlineCount) {
        UUID uuid = player.getUniqueId();

        // Get player stats
        int kills = StatsManager.getKills(uuid);
        int deaths = StatsManager.getDeaths(uuid);
        int ping = getPing(player);

        // Get player level info
        int level = LevelManager.getLevel(uuid);
        String levelFormatted = LevelManager.getFormattedLevel(uuid);
        int currentXp = LevelManager.getCurrentXp(uuid);
        int requiredXp = LevelManager.getRequiredXp(uuid);
        String progressBar = LevelManager.getProgressBar(uuid);

        // Get arena info if player is in arena
        String arenaName = "None";
        String phase = "Lobby";
        String time = "--:--";

        if (ObsidianCommand.playersInArena.containsKey(uuid)) {
            arenaName = ObsidianCommand.playersInArena.get(uuid);
            GameManager.ArenaGame game = GameManager.getGame(arenaName);

            if (game != null) {
                if (game.getGameState() == GameManager.GameState.COUNTDOWN) {
                    int countdown = GameManager.getCountdown(arenaName);
                    phase = "Countdown";
                    time = countdown + "s";
                } else if (game.getGameState() == GameManager.GameState.PREPARATION) {
                    int prepTime = WallManager.getTimer(arenaName);
                    phase = "Preparation";
                    time = prepTime + "m";
                } else if (game.getGameState() == GameManager.GameState.PLAYING) {
                    phase = "Playing";
                    int gameTime = game.getGameTime();
                    int minutes = gameTime / 60;
                    int seconds = gameTime % 60;
                    time = String.format("%02d:%02d", minutes, seconds);
                }
            }
        }

        // Replace all placeholders
        text = text.replace("%player%", player.getName());
        text = text.replace("%online%", String.valueOf(onlineCount));
        text = text.replace("%ping%", String.valueOf(ping));
        text = text.replace("%kills%", String.valueOf(kills));
        text = text.replace("%deaths%", String.valueOf(deaths));
        text = text.replace("%arena%", arenaName);
        text = text.replace("%phase%", phase);
        text = text.replace("%time%", time);
        text = text.replace("%level%", String.valueOf(level));
        text = text.replace("%level_formatted%", levelFormatted);
        text = text.replace("%xp%", String.valueOf(currentXp));
        text = text.replace("%req_xp%", String.valueOf(requiredXp));
        text = text.replace("%progress_bar%", progressBar);

        return text;
    }

    private static int getPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception e) {
            return 0;
        }
    }
}
