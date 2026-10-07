package az.nuran.obsidianwars.commands.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.handlers.ChatListener;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.LobbyScoreboardManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.ParticleManager;
import az.nuran.obsidianwars.managers.ScoreboardManager;
import az.nuran.obsidianwars.managers.SpectatorManager;
import az.nuran.obsidianwars.managers.TeamManager;
import az.nuran.obsidianwars.services.GameFeedbackService;
import az.nuran.obsidianwars.services.PlayerUtils;
import az.nuran.obsidianwars.services.RejoinManager;
import az.nuran.obsidianwars.services.TeamConfig;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles join, leave, and rejoin commands.
 */
public class JoinLeaveCommandHandler implements CommandHandler {

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("play") || subCommand.equals("join")) {
            if (!player.hasPermission("obsidianwars.command.join")) {
                player.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            handleJoinCommand(player, args);
            return true;
        }

        if (subCommand.equals("leave")) {
            if (!player.hasPermission("obsidianwars.command.leave")) {
                player.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            handleLeaveCommand(player);
            return true;
        }

        if (subCommand.equals("rejoin")) {
            if (!player.hasPermission("obsidianwars.command.rejoin")) {
                player.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            handleRejoinCommand(player);
            return true;
        }

        return false;
    }

    private void handleJoinCommand(Player player, String[] args) {
        List<String> arenaNames = ArenaConfigManager.getArenaNames();

        if (arenaNames.isEmpty()) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_no_arenas"));
            return;
        }

        String arenaName = null;

        // If arena specified, try to join that specific arena
        if (args.length > 1) {
            arenaName = args[1];
            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            if ("DISABLED".equals(status)) {
                player.sendMessage("§cThis arena is currently disabled.");
                return;
            }
            if (!"READY".equals(status) && !"WAITING".equals(status)) {
                player.sendMessage("§cThis arena is not available (status: " + status + ")");
                return;
            }
            if ("STARTING".equals(status)) {
                player.sendMessage("§cThis arena is currently starting. Please wait for it to finish.");
                return;
            }
        } else {
            // Join random available arena
            for (String name : arenaNames) {
                String status = ArenaConfigManager.getArenaStatus(name);
                if ("READY".equals(status) || "WAITING".equals(status)) {
                    arenaName = name;
                    break;
                }
            }
        }

        if (arenaName == null) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_ready"));
            return;
        }

        // Delegate to static joinArena method
        ObsidianCommand.joinArena(player, arenaName);
    }

    private void handleLeaveCommand(Player player) {
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            player.sendMessage(MessagesConfigManager.getMessage("not_in_arena"));
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        if (arenaName == null) {
            player.sendMessage(MessagesConfigManager.getMessage("not_in_arena"));
            return;
        }

        // Check if in countdown state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        boolean wasInCountdown = (game != null && game.getGameState() == az.nuran.obsidianwars.managers.ArenaStateManager.ArenaState.STARTING);
        boolean wasInGame = (game != null && (game.getGameState() == az.nuran.obsidianwars.managers.ArenaStateManager.ArenaState.PLAYING || game.getGameState() == az.nuran.obsidianwars.managers.ArenaStateManager.ArenaState.PREPARATION));

        // Clear disconnect record for this player (manual leave, not disconnect)
        RejoinManager.clearDisconnectRecord(player.getUniqueId());

        // Remove player from arena
        ObsidianCommand.playersInArena.remove(player.getUniqueId());
        GameManager.removePlayerFromArena(player.getUniqueId(), arenaName);
        TeamManager.removePlayerFromTeam(player);
        ParticleManager.removeSpawnProtection(player);

        // Invalidate team health cache for this arena
        ScoreboardManager.invalidateTeamHealthCache(arenaName);

        // Invalidate chat cache for this arena
        ChatListener.invalidateTeamCache(arenaName);

        // Reset player state and teleport to spawn
        Location mainSpawn = player.getWorld().getSpawnLocation();
        PlayerUtils.resetPlayerFull(player, mainSpawn);

        // Broadcast message
        ObsidianCommand.broadcastToArena(arenaName, MessagesConfigManager.getMessage("player_left", "player", player.getName()));

        player.sendMessage(MessagesConfigManager.getMessage("game_ended"));

        // Update lobby scoreboard after leaving arena
        LobbyScoreboardManager.updateLobbyScoreboard(player);

        // CANCELLATION LOGIC: If player left during countdown, check if we need to cancel
        if (wasInCountdown) {
            if (TeamManager.isAnyTeamEmpty(arenaName) ||
                TeamManager.getTotalPlayerCount(arenaName) < ArenaConfigManager.getMinPlayers(arenaName)) {
                GameManager.cancelCountdown(arenaName);
            }
        }

        // WIN CONDITION: If player left during active game, check for team elimination
        if (wasInGame) {
            GameManager.checkTeamEliminationOnLeave(arenaName);
        }

        // Check game status
        GameManager.checkGameStart(arenaName);
    }

    private void handleRejoinCommand(Player player) {
        // Check if player can rejoin
        if (RejoinManager.canRejoin(player)) {
            // Attempt to restore player state
            boolean restored = RejoinManager.restoreDisconnectedPlayer(player);

            if (restored) {
                String arenaName = RejoinManager.getDisconnectedPlayerArena(player.getUniqueId());
                String rejoinMessage = "§aYou have reconnected to the arena!";
                player.sendMessage(rejoinMessage);

                // Broadcast to arena
                String broadcastMessage = "§a" + player.getName() + " has reconnected!";
                ObsidianCommand.broadcastToArena(arenaName, broadcastMessage);

                // Play rejoin sound
                String sound = MessagesConfigManager.getSound("join_lobby");
                Sound rejoinSound = Obsidianwars.parseSound(sound);
                if (rejoinSound != null) {
                    player.playSound(player.getLocation(), rejoinSound, 1.0f, 1.0f);
                }
            } else {
                player.sendMessage("§cCould not restore your game session. It may have expired.");
            }
        } else {
            player.sendMessage("§cNo active game session found to rejoin.");
        }
    }
}
