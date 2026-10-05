package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Handles team chat and global chat system.
 * Default chat: Only visible to teammates in the same arena/team.
 * Global chat: Messages starting with '!' are sent to all players in the arena.
 */
public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        // Check if player is in an arena
        String arenaName = ObsidianCommand.playersInArena.get(playerUuid);
        if (arenaName == null) {
            // Player not in arena, let normal chat handle it
            return;
        }

        // Check if player is on a team
        String playerTeam = TeamListener.playerTeams.get(playerUuid);
        if (playerTeam == null) {
            // Player in arena but not on a team yet, let normal chat handle it
            return;
        }

        // Check if arena is in waiting/lobby phase
        // This includes: READY, WAITING, or STARTING status
        String arenaStatus = ArenaConfigManager.getArenaStatus(arenaName);
        boolean isLobbyPhase = "READY".equals(arenaStatus) || "WAITING".equals(arenaStatus) || "STARTING".equals(arenaStatus);

        if (isLobbyPhase) {
            // Waiting lobby chat - broadcast to all players in the arena with team prefix/color
            event.setCancelled(true);

            String message = event.getMessage();
            // Format: [RED] PlayerName: Message or [BLUE] PlayerName: Message
            String teamPrefix = playerTeam.equals("red") ? "§c[RED]" : "§9[BLUE]";
            String formattedMessage = teamPrefix + " " + player.getDisplayName() + "§f: " + message;

            // Send to all players in the arena
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    Player arenaPlayer = Bukkit.getPlayer(uuid);
                    if (arenaPlayer != null && arenaPlayer.isOnline()) {
                        arenaPlayer.sendMessage(formattedMessage);
                    }
                }
            }

            // Also send to spectators in the arena
            for (Player spectator : SpectatorManager.getSpectatorsInArena(arenaName)) {
                spectator.sendMessage(formattedMessage);
            }

            return;
        }

        // Game is in progress - use team/global chat separation
        String message = event.getMessage();

        // Check for global chat prefix '!'
        if (message.startsWith("!")) {
            // Global chat - send to all players in the arena
            event.setCancelled(true);

            // Strip the leading '!'
            String globalMessage = message.substring(1).trim();
            if (globalMessage.isEmpty()) {
                player.sendMessage("§cPlease provide a message after the '!' prefix.");
                return;
            }

            // Format: [GLOBAL] [RED] PlayerName: Message or [GLOBAL] [BLUE] PlayerName: Message
            String teamPrefix = playerTeam.equals("red") ? "§c[RED]" : "§9[BLUE]";
            String formattedMessage = "§6[GLOBAL] " + teamPrefix + " " + player.getDisplayName() + "§f: " + globalMessage;

            // Send to all players in the arena
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    Player arenaPlayer = Bukkit.getPlayer(uuid);
                    if (arenaPlayer != null && arenaPlayer.isOnline()) {
                        arenaPlayer.sendMessage(formattedMessage);
                    }
                }
            }

            // Also send to spectators in the arena
            for (Player spectator : SpectatorManager.getSpectatorsInArena(arenaName)) {
                spectator.sendMessage(formattedMessage);
            }

            return;
        }

        // Team chat - only visible to teammates in the same arena/team
        event.setCancelled(true);

        // Format: [TEAM] PlayerName: Message
        String teamPrefix = playerTeam.equals("red") ? "§c[RED]" : "§9[BLUE]";
        String formattedMessage = teamPrefix + " " + player.getDisplayName() + "§f: " + message;

        // Send to all teammates in the same arena
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                String teammateTeam = TeamListener.playerTeams.get(uuid);
                if (teammateTeam != null && teammateTeam.equals(playerTeam)) {
                    Player teammate = Bukkit.getPlayer(uuid);
                    if (teammate != null && teammate.isOnline()) {
                        teammate.sendMessage(formattedMessage);
                    }
                }
            }
        }

        // Also send to spectators in the arena (they can see team chat)
        for (Player spectator : SpectatorManager.getSpectatorsInArena(arenaName)) {
            spectator.sendMessage(formattedMessage);
        }
    }
}
