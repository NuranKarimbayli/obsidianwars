package az.nuran.obsidianwars.services;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.LanguageManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.services.TeamConfig;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Service for handling game feedback - sounds, titles, broadcasts, and notifications.
 * Extracted from GameManager to separate concerns.
 */
public class GameFeedbackService {

    /**
     * Broadcasts a message to all players in an arena.
     */
    public static void broadcastToArena(String arenaName, String message) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    arenaPlayer.sendMessage(ColorUtils.translate(message));
                }
            }
        }
    }

    /**
     * Plays a sound to all players in an arena.
     */
    public static void playSoundToArena(String arenaName, String soundName, float volume, float pitch) {
        Sound sound = Obsidianwars.parseSound(soundName);
        if (sound == null) return;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            }
        }
    }

    /**
     * Sends a title to all players in an arena.
     * Title and subtitle are retrieved from LanguageManager for each player based on their language.
     */
    public static void sendTitleToArena(String arenaName, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    String title = LanguageManager.getInstance().getMessage(player, titleKey);
                    String subtitle = LanguageManager.getInstance().getMessage(player, subtitleKey);
                    player.sendTitle(ColorUtils.translate(title), ColorUtils.translate(subtitle), fadeIn, stay, fadeOut);
                }
            }
        }
    }

    /**
     * Plays a success sound to a player.
     */
    public static void playSuccessSound(Player player) {
        Sound successSound = Obsidianwars.parseSound("ENTITY_PLAYER_LEVELUP");
        if (successSound == null) {
            try {
                successSound = Sound.valueOf("LEVEL_UP");
            } catch (IllegalArgumentException e) {
                return;
            }
        }
        if (successSound != null) {
            player.playSound(player.getLocation(), successSound, 1.0f, 1.0f);
        }
    }

    /**
     * Plays an error sound to a player.
     */
    public static void playErrorSound(Player player) {
        Sound errorSound = Obsidianwars.parseSound("ENTITY_VILLAGER_NO");
        if (errorSound == null) {
            try {
                errorSound = Sound.valueOf("VILLAGER_NO");
            } catch (IllegalArgumentException e) {
                return;
            }
        }
        if (errorSound != null) {
            player.playSound(player.getLocation(), errorSound, 1.0f, 1.0f);
        }
    }

    /**
     * Sends a victory notification to all players in an arena.
     * Includes title, sound, and message.
     */
    public static void sendVictoryNotification(String arenaName, String winningTeam) {
        String teamName = TeamConfig.getTeamName(winningTeam);
        String teamColor = TeamConfig.getTeamColor(winningTeam);

        // Victory message - send to each player in their language
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    String victoryMessage = LanguageManager.getInstance().getMessage(player, "victory_message", "teamColor", teamColor, "teamName", teamName);
                    String victoryPrefix = LanguageManager.getInstance().getMessage(player, "victory");
                    player.sendMessage(ColorUtils.translate(victoryPrefix + " " + victoryMessage));
                }
            }
        }

        // UI_TOAST_CHALLENGE_COMPLETE sound for all players
        Sound victorySound = Obsidianwars.parseSound("UI_TOAST_CHALLENGE_COMPLETE");
        if (victorySound == null) {
            // Fallback to configured sound
            String sound = MessagesConfigManager.getSound("win_victory");
            victorySound = Obsidianwars.parseSound(sound);
        }
        if (victorySound != null) {
            // Get title timing from config
            int fadeIn = Obsidianwars.getInstance().getConfig().getInt("titles.victory.fade-in", 10);
            int stay = Obsidianwars.getInstance().getConfig().getInt("titles.victory.stay", 60);
            int fadeOut = Obsidianwars.getInstance().getConfig().getInt("titles.victory.fade-out", 20);

            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.playSound(player.getLocation(), victorySound, 1.0f, 1.0f);
                        String victoryTitle = LanguageManager.getInstance().getMessage(player, "victory_title", "teamColor", teamColor, "teamName", teamName);
                        String subtitle = LanguageManager.getInstance().getMessage(player, "victory_subtitle", "teamColor", teamColor, "teamName", teamName);
                        player.sendTitle(ColorUtils.translate(victoryTitle), ColorUtils.translate(subtitle), fadeIn, stay, fadeOut);
                    }
                }
            }
        }
    }

    /**
     * Sends an obsidian destroyed notification to all players in an arena.
     * Dynamic team titles - GREEN for attackers, RED for victims.
     */
    public static void sendObsidianDestroyedNotification(String arenaName, String destroyedTeam, Player destroyer) {
        String teamName = TeamConfig.getTeamName(destroyedTeam);
        String teamColor = TeamConfig.getTeamColor(destroyedTeam);
        String attackerTeam = destroyedTeam.equals("red") ? "blue" : "red";
        String attackerColor = TeamConfig.getTeamColor(attackerTeam);

        // Get title timing from config
        int fadeIn = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.fade-in", 10);
        int stay = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.stay", 70);
        int fadeOut = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.fade-out", 20);

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    String playerTeam = az.nuran.obsidianwars.handlers.TeamListener.playerTeams.get(uuid);

                    if (playerTeam != null && playerTeam.equals(attackerTeam)) {
                        // Attacker team gets GREEN title
                        String title = LanguageManager.getInstance().getMessage(arenaPlayer, "obsidian_destroyed_title_attacker");
                        String subtitle = LanguageManager.getInstance().getMessage(arenaPlayer, "obsidian_destroyed_subtitle_attacker", "teamName", teamName);
                        arenaPlayer.sendTitle(ColorUtils.translate(title), ColorUtils.translate(subtitle), fadeIn, stay, fadeOut);
                    } else {
                        // Victim team gets RED title
                        String title = LanguageManager.getInstance().getMessage(arenaPlayer, "obsidian_destroyed_title_victim");
                        String subtitle = LanguageManager.getInstance().getMessage(arenaPlayer, "obsidian_destroyed_subtitle_victim", "teamName", teamName);
                        arenaPlayer.sendTitle(ColorUtils.translate(title), ColorUtils.translate(subtitle), fadeIn, stay, fadeOut);
                    }
                }
            }
        }

        // Broadcast message - send to each player in their language
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    String broadcastMessage = LanguageManager.getInstance().getMessage(arenaPlayer, "obsidian_team_destroyed",
                        "teamColor", teamColor, "teamName", teamName, "player", destroyer.getName());
                    arenaPlayer.sendMessage(ColorUtils.translate(broadcastMessage));
                }
            }
        }

        // Play sound
        String sound = MessagesConfigManager.getSound("obsidian_destroyed");
        Sound destroySound = Obsidianwars.parseSound(sound);
        if (destroySound != null) {
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                    Player arenaPlayer = Bukkit.getPlayer(uuid);
                    if (arenaPlayer != null) {
                        arenaPlayer.playSound(arenaPlayer.getLocation(), destroySound, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    /**
     * Sends a countdown tick sound to a player.
     */
    public static void playCountdownTick(Player player) {
        String sound = Obsidianwars.getInstance().getConfig().getString("sounds.countdown_tick", "BLOCK_NOTE_BLOCK_PLING");
        Sound tickSound = Obsidianwars.parseSound(sound);
        if (tickSound == null) {
            // Fallback to BLOCK_NOTE_BLOCK_PLING with high pitch
            tickSound = Sound.BLOCK_NOTE_BLOCK_PLING;
        }
        if (tickSound != null) {
            player.playSound(player.getLocation(), tickSound, 1.0f, 2.0f);
        }
    }

    /**
     * Sends a welcome message to a player.
     */
    public static void sendWelcomeMessage(Player player) {
        List<String> welcomeLines = LanguageManager.getInstance().getMessageList(player, "welcome_message");
        for (String line : welcomeLines) {
            player.sendMessage(ColorUtils.translate(line));
        }
    }

    /**
     * Sends a rules announcement to a player.
     */
    public static void sendRulesAnnouncement(Player player) {
        List<String> rulesLines = LanguageManager.getInstance().getMessageList(player, "rules_announcement");
        for (String line : rulesLines) {
            player.sendMessage(ColorUtils.translate(line));
        }
    }

    /**
     * Sends a post-game chat summary to all players in the arena.
     * Includes winner, MVP, and individual player stats.
     */
    public static void sendPostGameSummary(String arenaName, String winningTeam, int mvpKills, UUID mvpUuid) {
        String teamName = TeamConfig.getTeamName(winningTeam);
        String teamColor = TeamConfig.getTeamColor(winningTeam);

        // Broadcast winner - send to each player in their language
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(ColorUtils.translate("&6&l=== MATCH SUMMARY ==="));
                    String winnerMessage = LanguageManager.getInstance().getMessage(player, "victory_message", "teamColor", teamColor, "teamName", teamName);
                    player.sendMessage(ColorUtils.translate(winnerMessage));
                }
            }
        }

        // Broadcast MVP - send to each player in their language
        if (mvpUuid != null) {
            Player mvpPlayer = Bukkit.getPlayer(mvpUuid);
            if (mvpPlayer != null) {
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    String playerArena = ObsidianCommand.playersInArena.get(uuid);
                    if (playerArena != null && playerArena.equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            String mvpMessage = LanguageManager.getInstance().getMessage(player, "match_summary_mvp", "player", mvpPlayer.getName(), "kills", String.valueOf(mvpKills));
                            player.sendMessage(ColorUtils.translate(mvpMessage));
                        }
                    }
                }
            }
        }

        // Broadcast individual stats
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(ColorUtils.translate("&7--------------------"));
                    int kills = az.nuran.obsidianwars.managers.StatsManager.getKills(uuid);
                    int deaths = az.nuran.obsidianwars.managers.StatsManager.getDeaths(uuid);
                    String playerTeam = az.nuran.obsidianwars.handlers.TeamListener.playerTeams.get(uuid);
                    // Safe null-check for playerTeam (spectators or players with cleared team)
                    String teamPrefix = (playerTeam != null && playerTeam.equals("red")) ? "&c" : "&9";
                    String statsMessage = LanguageManager.getInstance().getMessage(player, "player_stats_summary", "teamPrefix", teamPrefix, "player", player.getName(), "kills", String.valueOf(kills), "deaths", String.valueOf(deaths));
                    player.sendMessage(ColorUtils.translate(statsMessage));
                }
            }
        }

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(ColorUtils.translate("&7--------------------"));
                }
            }
        }
    }
}
