package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.SpectatorManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles team chat and global chat system.
 * Default chat: Only visible to teammates in the same arena/team.
 * Global chat: Messages starting with '!' are sent to all players in the arena.
 * Optimized with team member caching to avoid expensive iterations.
 */
public class ChatListener implements Listener {

    // Cache for team member lists per arena to avoid expensive iterations
    private static final Map<String, Set<UUID>> redTeamCache = new ConcurrentHashMap<>();
    private static final Map<String, Set<UUID>> blueTeamCache = new ConcurrentHashMap<>();
    private static final Map<String, Set<UUID>> allPlayersCache = new ConcurrentHashMap<>();

    /**
     * Invalidates the team member cache for a specific arena.
     * Call this when a player joins, leaves, or switches teams.
     */
    public static void invalidateTeamCache(String arenaName) {
        redTeamCache.remove(arenaName);
        blueTeamCache.remove(arenaName);
        allPlayersCache.remove(arenaName);
    }

    /**
     * Rebuilds the team member cache for a specific arena.
     */
    private static void rebuildTeamCache(String arenaName) {
        Set<UUID> redMembers = ConcurrentHashMap.newKeySet();
        Set<UUID> blueMembers = ConcurrentHashMap.newKeySet();
        Set<UUID> allMembers = ConcurrentHashMap.newKeySet();

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                allMembers.add(uuid);
                String playerTeam = TeamListener.playerTeams.get(uuid);
                if (playerTeam != null) {
                    if (playerTeam.equals("red")) {
                        redMembers.add(uuid);
                    } else if (playerTeam.equals("blue")) {
                        blueMembers.add(uuid);
                    }
                }
            }
        }

        redTeamCache.put(arenaName, redMembers);
        blueTeamCache.put(arenaName, blueMembers);
        allPlayersCache.put(arenaName, allMembers);
    }

    /**
     * Gets cached team members for an arena, rebuilding if cache is stale.
     */
    private static Set<UUID> getTeamMembers(String arenaName, String team) {
        Map<String, Set<UUID>> cache = team.equals("red") ? redTeamCache : blueTeamCache;
        Set<UUID> members = cache.get(arenaName);

        if (members == null) {
            rebuildTeamCache(arenaName);
            members = cache.get(arenaName);
        }

        return members != null ? members : ConcurrentHashMap.newKeySet();
    }

    /**
     * Gets cached all players for an arena, rebuilding if cache is stale.
     */
    private static Set<UUID> getAllArenaPlayers(String arenaName) {
        Set<UUID> players = allPlayersCache.get(arenaName);

        if (players == null) {
            rebuildTeamCache(arenaName);
            players = allPlayersCache.get(arenaName);
        }

        return players != null ? players : ConcurrentHashMap.newKeySet();
    }

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

            // Send to all players in the arena (using cached list)
            for (UUID uuid : getAllArenaPlayers(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null && arenaPlayer.isOnline()) {
                    arenaPlayer.sendMessage(formattedMessage);
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
                player.sendMessage(MessagesConfigManager.getMessage("chat_empty_global"));
                return;
            }

            // Format: [GLOBAL] [RED] PlayerName: Message or [GLOBAL] [BLUE] PlayerName: Message
            String teamPrefix = playerTeam.equals("red") ? "§c[RED]" : "§9[BLUE]";
            String formattedMessage = "§6[GLOBAL] " + teamPrefix + " " + player.getDisplayName() + "§f: " + globalMessage;

            // Send to all players in the arena (using cached list)
            for (UUID uuid : getAllArenaPlayers(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null && arenaPlayer.isOnline()) {
                    arenaPlayer.sendMessage(formattedMessage);
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

        // Send to all teammates in the same arena (using cached list)
        for (UUID uuid : getTeamMembers(arenaName, playerTeam)) {
            Player teammate = Bukkit.getPlayer(uuid);
            if (teammate != null && teammate.isOnline()) {
                teammate.sendMessage(formattedMessage);
            }
        }

        // Also send to spectators in the arena (they can see team chat)
        for (Player spectator : SpectatorManager.getSpectatorsInArena(arenaName)) {
            spectator.sendMessage(formattedMessage);
        }
    }
}
