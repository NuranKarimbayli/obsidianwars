package az.nuran.obsidianwars.commands.handlers;

import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.WallManager;

import org.bukkit.entity.Player;

/**
 * Handles force commands: force, forceend, forcestart, forceprep.
 */
public class ForceCommandHandler implements CommandHandler {

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("force")) {
            handleForceCommand(player, args);
            return true;
        }

        if (subCommand.equals("forceend")) {
            handleForceendCommand(player, args);
            return true;
        }

        if (subCommand.equals("forcestart")) {
            handleForcestartCommand(player, args);
            return true;
        }

        if (subCommand.equals("forceprep")) {
            handleForceprepCommand(player, args);
            return true;
        }

        return false;
    }

    private void handleForceCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o force <start|end> <arena|preperation> [arena]");
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("start")) {
            if (args.length < 4 || !args[2].equalsIgnoreCase("arena")) {
                player.sendMessage("§cUsage: /o force start arena <arena>");
                return;
            }
            String arenaName = args[3];
            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }
            // Check if there are players in the arena
            if (!GameManager.hasPlayersInArena(arenaName)) {
                player.sendMessage("§cNo players in arena " + arenaName + "!");
                return;
            }

            // Check current game state
            GameManager.ArenaGame game = GameManager.getGame(arenaName);
            if (game != null) {
                ArenaStateManager.ArenaState state = game.getGameState();
                // If in COUNTDOWN state, cancel countdown and immediately start
                if (state == ArenaStateManager.ArenaState.STARTING) {
                    GameManager.cancelCountdownAndForceStart(arenaName);
                    player.sendMessage("§aCountdown skipped! Game starting now for arena " + arenaName);
                    return;
                }
                // If already in PLAYING or PREPARATION state, return error
                if (state == ArenaStateManager.ArenaState.PLAYING || state == ArenaStateManager.ArenaState.PREPARATION) {
                    player.sendMessage("§cThis arena is already active!");
                    return;
                }
            }

            GameManager.forceStartGame(arenaName);
            player.sendMessage(MessagesConfigManager.getMessage("force_started", "arenaName", arenaName));
        } else if (action.equals("end")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o force end arena <arena> OR /o force end preperation <arena>");
                return;
            }
            String subAction = args[2].toLowerCase();
            if (subAction.equals("arena")) {
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /o force end arena <arena>");
                    return;
                }
                String arenaName = args[3];
                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
                    return;
                }
                GameManager.endGame(arenaName, null); // Message is broadcast to arena players by endGame
                player.sendMessage("§aGame ended for arena " + arenaName);
            } else if (subAction.equals("preperation")) {
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /o force end preperation <arena>");
                    return;
                }
                String arenaName = args[3];
                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
                    return;
                }
                GameManager.ArenaGame game = GameManager.getGame(arenaName);
                if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PREPARATION) {
                    player.sendMessage("§cThis arena is not in preparation phase!");
                    return;
                }
                WallManager.forceSkipPreparation(arenaName);
                player.sendMessage("§aPreparation phase skipped! Walls are breaking now!");
            } else {
                player.sendMessage("§cUsage: /o force end arena <arena> OR /o force end preperation <arena>");
            }
        } else {
            player.sendMessage("§cUsage: /o force <start|end> <arena|preperation> [arena]");
        }
    }

    private void handleForceendCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o forceend <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // First, try to end the game normally if it exists
        GameManager.endGame(arenaName, null); // null = admin ended, no winner

        // ALWAYS forcefully reset arena status to READY regardless of whether game was running
        // This handles the edge case where arena is stuck in PLAYING without an active game instance
        String currentStatus = ArenaConfigManager.getArenaStatus(arenaName);
        if (!currentStatus.equals("READY") && !currentStatus.equals("WAITING")) {
            player.sendMessage("§eForcefully resetting arena status from " + currentStatus + " to READY...");
            ArenaConfigManager.setArenaStatus(arenaName, "READY");
        }

        // Stop all arena-related tasks to ensure cleanup
        az.nuran.obsidianwars.managers.ParticleManager.stopAllArenaTasks(arenaName);
        az.nuran.obsidianwars.managers.MobSpawnerManager.stopMobSpawning(arenaName);
        WallManager.stopPreparationTimer(arenaName);
        WallManager.stopSuddenDeathCountdown(arenaName);
        az.nuran.obsidianwars.handlers.XPAwardListener.stopPerMinuteTask(arenaName);

        // Clear all dropped items in the arena
        GameManager.clearDroppedItems(arenaName);

        // Clear all non-player entities (mobs, dropped items, arrows, etc.) in the arena
        GameManager.clearArenaMobs(arenaName);

        // Restore world rules
        az.nuran.obsidianwars.managers.WorldRulesManager.restoreWorldRules(arenaName);

        // Restore arena snapshot if available
        boolean restoreSuccess = az.nuran.obsidianwars.managers.ArenaSnapshotManager.restoreSnapshot(arenaName);
        if (restoreSuccess) {
            player.sendMessage("§aArena snapshot restored.");
        } else {
            player.sendMessage("§7No arena snapshot found or restoration failed.");
        }

        // Clear any players still tracked in this arena
        java.util.List<java.util.UUID> playersToRemove = new java.util.ArrayList<>();
        for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                playersToRemove.add(uuid);
            }
        }

        for (java.util.UUID uuid : playersToRemove) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                // Remove spectator mode if player was a spectator
                if (az.nuran.obsidianwars.managers.SpectatorManager.isSpectator(p)) {
                    az.nuran.obsidianwars.managers.SpectatorManager.removeSpectatorMode(p);
                }

                // Reset player state and teleport to spawn
                org.bukkit.Location mainSpawn = p.getWorld().getSpawnLocation();
                az.nuran.obsidianwars.services.PlayerUtils.resetPlayerFull(p, mainSpawn);
                p.sendMessage("§aArena force-ended by admin. Teleported to spawn.");

                // Update lobby scoreboard
                az.nuran.obsidianwars.managers.LobbyScoreboardManager.updateLobbyScoreboard(p);
            }

            // Remove from tracking
            ObsidianCommand.playersInArena.remove(uuid);
            if (p != null) {
                GameManager.removePlayerFromArena(uuid, arenaName);
                az.nuran.obsidianwars.managers.TeamManager.removePlayerFromTeam(p);
            } else {
                // Player is offline, just remove from team map
                az.nuran.obsidianwars.handlers.TeamListener.playerTeams.remove(uuid);
            }

            // Clean up wand positions
            WandListener.pos1Map.remove(uuid);
            WandListener.pos2Map.remove(uuid);
        }

        player.sendMessage("§aGame force-ended for arena " + arenaName + ". Status reset to READY.");
    }

    private void handleForcestartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o forcestart <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Check if there are players in the arena
        if (!GameManager.hasPlayersInArena(arenaName)) {
            player.sendMessage("§cNo players in arena " + arenaName + "!");
            return;
        }

        // Check current game state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game != null) {
            ArenaStateManager.ArenaState state = game.getGameState();
            // If in COUNTDOWN state, cancel countdown and immediately start
            if (state == ArenaStateManager.ArenaState.STARTING) {
                GameManager.cancelCountdownAndForceStart(arenaName);
                player.sendMessage("§aCountdown skipped! Game starting now for arena " + arenaName);
                return;
            }
            // If already in PLAYING or PREPARATION state, return error
            if (state == ArenaStateManager.ArenaState.PLAYING || state == ArenaStateManager.ArenaState.PREPARATION) {
                player.sendMessage("§cThis arena is already active!");
                return;
            }
        }

        // Force start (no game exists or in WAITING state)
        GameManager.forceStartGame(arenaName);
        player.sendMessage(MessagesConfigManager.getMessage("force_started", "arenaName", arenaName));
    }

    private void handleForceprepCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o forceprep <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Check if game is in PREPARATION state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PREPARATION) {
            player.sendMessage("§cThis arena is not in preparation phase!");
            return;
        }

        // Force skip preparation - trigger wall removal immediately
        WallManager.forceSkipPreparation(arenaName);
        player.sendMessage("§aPreparation phase skipped! Walls are breaking now!");
    }
}
