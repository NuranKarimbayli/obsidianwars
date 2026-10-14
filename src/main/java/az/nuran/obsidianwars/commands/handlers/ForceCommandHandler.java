package az.nuran.obsidianwars.commands.handlers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.handlers.WandListener;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.WallManager;
import az.nuran.obsidianwars.managers.LanguageManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles force commands: force, forceend, forcestart, forceprep.
 */
public class ForceCommandHandler implements CommandHandler {

    private final Obsidianwars plugin;

    public ForceCommandHandler() {
        this.plugin = Obsidianwars.getInstance();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("force")) {
            if (!player.hasPermission("obsidianwars.command.force")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            handleForceCommand(player, args);
            return true;
        }

        if (subCommand.equals("forceend")) {
            if (!player.hasPermission("obsidianwars.command.forceend")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            handleForceendCommand(player, args);
            return true;
        }

        if (subCommand.equals("forcestart")) {
            if (!player.hasPermission("obsidianwars.command.forcestart")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            handleForcestartCommand(player, args);
            return true;
        }

        if (subCommand.equals("forceprep")) {
            if (!player.hasPermission("obsidianwars.command.forceprep")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            handleForceprepCommand(player, args);
            return true;
        }

        return false;
    }

    private void handleForceCommand(Player player, String[] args) {
        if (args.length < 2) {
            LanguageManager.getInstance().sendMessage(player, "usage_force");
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("start")) {
            if (args.length < 4 || !args[2].equalsIgnoreCase("arena")) {
                LanguageManager.getInstance().sendMessage(player, "usage_force_start");
                return;
            }
            String arenaName = args[3];
            if (!ArenaConfigManager.arenaExists(arenaName)) {
                LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
                return;
            }
            // Check if there are players in the arena
            if (!GameManager.hasPlayersInArena(arenaName)) {
                LanguageManager.getInstance().sendMessage(player, "no_players_in_arena", "arenaName", arenaName);
                return;
            }

            // Check current game state
            GameManager.ArenaGame game = GameManager.getGame(arenaName);
            if (game != null) {
                ArenaStateManager.ArenaState state = game.getGameState();
                // If in COUNTDOWN state, cancel countdown and immediately start
                if (state == ArenaStateManager.ArenaState.STARTING) {
                    GameManager.cancelCountdownAndForceStart(arenaName);
                    LanguageManager.getInstance().sendMessage(player, "countdown_skipped", "arenaName", arenaName);
                    return;
                }
                // If already in PLAYING or PREPARATION state, return error
                if (state == ArenaStateManager.ArenaState.PLAYING || state == ArenaStateManager.ArenaState.PREPARATION) {
                    LanguageManager.getInstance().sendMessage(player, "arena_already_active");
                    return;
                }
            }

            GameManager.forceStartGame(arenaName);
            LanguageManager.getInstance().sendMessage(player, "force_started", "arenaName", arenaName);
        } else if (action.equals("end")) {
            if (args.length < 3) {
                LanguageManager.getInstance().sendMessage(player, "usage_force_end");
                return;
            }
            String subAction = args[2].toLowerCase();
            if (subAction.equals("arena")) {
                if (args.length < 4) {
                    LanguageManager.getInstance().sendMessage(player, "usage_force_end_arena");
                    return;
                }
                String arenaName = args[3];
                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
                    return;
                }
                GameManager.endGame(arenaName, null); // Message is broadcast to arena players by endGame
                LanguageManager.getInstance().sendMessage(player, "game_ended", "arenaName", arenaName);
            } else if (subAction.equals("preperation")) {
                if (args.length < 4) {
                    LanguageManager.getInstance().sendMessage(player, "usage_force_end_preparation");
                    return;
                }
                String arenaName = args[3];
                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
                    return;
                }
                GameManager.ArenaGame game = GameManager.getGame(arenaName);
                if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PREPARATION) {
                    LanguageManager.getInstance().sendMessage(player, "arena_not_in_preparation");
                    return;
                }
                WallManager.forceSkipPreparation(arenaName);
                LanguageManager.getInstance().sendMessage(player, "preparation_skipped");
            } else {
                LanguageManager.getInstance().sendMessage(player, "usage_force_end");
            }
        } else {
            LanguageManager.getInstance().sendMessage(player, "usage_force");
        }
    }

    private void handleForceendCommand(Player player, String[] args) {
        if (args.length < 2) {
            LanguageManager.getInstance().sendMessage(player, "usage_forceend");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        // First, try to end the game normally if it exists
        GameManager.endGame(arenaName, null); // null = admin ended, no winner

        // ALWAYS forcefully reset arena status to READY regardless of whether game was running
        // This handles the edge case where arena is stuck in PLAYING without an active game instance
        String currentStatus = ArenaConfigManager.getArenaStatus(arenaName);
        if (!currentStatus.equals("READY") && !currentStatus.equals("WAITING")) {
            LanguageManager.getInstance().sendMessage(player, "force_resetting_status", "oldStatus", currentStatus);
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
            LanguageManager.getInstance().sendMessage(player, "arena_snapshot_restored");
        } else {
            LanguageManager.getInstance().sendMessage(player, "arena_snapshot_failed");
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
                LanguageManager.getInstance().sendMessage(p, "arena_force_ended");

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

        LanguageManager.getInstance().sendMessage(player, "game_force_ended", "arenaName", arenaName);
    }

    private void handleForcestartCommand(Player player, String[] args) {
        if (args.length < 2) {
            LanguageManager.getInstance().sendMessage(player, "usage_forcestart");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        // Check if there are players in the arena
        if (!GameManager.hasPlayersInArena(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "no_players_in_arena", "arenaName", arenaName);
            return;
        }

        // Check current game state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game != null) {
            ArenaStateManager.ArenaState state = game.getGameState();
            // If in COUNTDOWN state, cancel countdown and immediately start
            if (state == ArenaStateManager.ArenaState.STARTING) {
                GameManager.cancelCountdownAndForceStart(arenaName);
                LanguageManager.getInstance().sendMessage(player, "countdown_skipped", "arenaName", arenaName);
                return;
            }
            // If already in PLAYING or PREPARATION state, return error
            if (state == ArenaStateManager.ArenaState.PLAYING || state == ArenaStateManager.ArenaState.PREPARATION) {
                LanguageManager.getInstance().sendMessage(player, "arena_already_active");
                return;
            }
        }

        // Force start (no game exists or in WAITING state)
        GameManager.forceStartGame(arenaName);
        LanguageManager.getInstance().sendMessage(player, "force_started", "arenaName", arenaName);
    }

    private void handleForceprepCommand(Player player, String[] args) {
        if (args.length < 2) {
            LanguageManager.getInstance().sendMessage(player, "usage_forceprep");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        // Check if game is in PREPARATION state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PREPARATION) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_preparation");
            return;
        }

        // Force skip preparation - trigger wall removal immediately
        WallManager.forceSkipPreparation(arenaName);
        LanguageManager.getInstance().sendMessage(player, "preparation_skipped");
    }
}
