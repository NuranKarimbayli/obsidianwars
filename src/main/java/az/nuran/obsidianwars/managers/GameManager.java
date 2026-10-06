package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.handlers.TeamListener;
import az.nuran.obsidianwars.handlers.WandListener;
import az.nuran.obsidianwars.handlers.XPAwardListener;
import az.nuran.obsidianwars.models.DisconnectedPlayerData;
import az.nuran.obsidianwars.models.PlayerStats;
import az.nuran.obsidianwars.services.DebugManager;
import az.nuran.obsidianwars.services.GameFeedbackService;
import az.nuran.obsidianwars.services.PlayerUtils;
import az.nuran.obsidianwars.services.RejoinManager;
import az.nuran.obsidianwars.services.TeamConfig;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private static final Map<String, ArenaGame> activeGames = new ConcurrentHashMap<>();
    private static final Map<String, Set<UUID>> arenaPlayers = new ConcurrentHashMap<>();
    private static int COUNTDOWN_SECONDS = 20; // Now configurable via config

    /**
     * Loads configuration values from config.yml
     */
    public static void loadConfig() {
        COUNTDOWN_SECONDS = Obsidianwars.getInstance().getConfig().getInt("timers.default-countdown", 20);
        Obsidianwars.getInstance().getLogger().info("Loaded countdown duration: " + COUNTDOWN_SECONDS + " seconds");
    }

    public static int getCountdownSeconds() {
        return COUNTDOWN_SECONDS;
    }

    // ============================================
    // Arena-Specific Player Tracking (O(1) lookups)
    // ============================================

    /**
     * Adds a player to the specified arena's player set.
     * This provides O(1) lookup for arena players.
     *
     * @param uuid The player's UUID
     * @param arenaName The arena name
     */
    public static void addPlayerToArena(UUID uuid, String arenaName) {
        arenaPlayers.computeIfAbsent(arenaName, k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }

    /**
     * Removes a player from the specified arena's player set.
     *
     * @param uuid The player's UUID
     * @param arenaName The arena name
     */
    public static void removePlayerFromArena(UUID uuid, String arenaName) {
        Set<UUID> players = arenaPlayers.get(arenaName);
        if (players != null) {
            players.remove(uuid);
            if (players.isEmpty()) {
                arenaPlayers.remove(arenaName);
            }
        }
    }

    /**
     * Gets all players in a specific arena (O(1) lookup).
     *
     * @param arenaName The arena name
     * @return Set of player UUIDs in the arena (empty set if arena has no players)
     */
    public static Set<UUID> getPlayersInArena(String arenaName) {
        return arenaPlayers.getOrDefault(arenaName, Collections.emptySet());
    }

    /**
     * Checks if a player is in a specific arena (O(1) lookup).
     *
     * @param uuid The player's UUID
     * @param arenaName The arena name
     * @return true if player is in the arena, false otherwise
     */
    public static boolean isPlayerInArena(UUID uuid, String arenaName) {
        Set<UUID> players = arenaPlayers.get(arenaName);
        return players != null && players.contains(uuid);
    }

    public static void checkGameStart(String arenaName) {
        ArenaGame existingGame = activeGames.get(arenaName);
        if (existingGame != null) {
            // If game is in STARTING state, check if we need to cancel
            if (existingGame.getGameState() == ArenaStateManager.ArenaState.STARTING) {
                // Check if conditions are still met
                if (!canStartCountdown(arenaName)) {
                    // Cancel countdown immediately
                    cancelCountdown(arenaName);
                }
            }
            return; // Game has already started or is counting down
        }

        // Check if we can start countdown
        if (canStartCountdown(arenaName)) {
            startCountdown(arenaName);
        }
    }

    /**
     * Checks if countdown can start based on requirements:
     * - Total players >= minPlayers
     * - Both RED and BLUE teams have at least 1 player each
     *
     * @param arenaName The arena name
     * @return true if countdown can start, false otherwise
     */
    private static boolean canStartCountdown(String arenaName) {
        int minPlayers = ArenaConfigManager.getMinPlayers(arenaName);
        int totalPlayers = TeamManager.getTotalPlayerCount(arenaName);

        // Check minimum player requirement
        if (totalPlayers < minPlayers) {
            return false;
        }

        // Check that both teams have at least 1 player
        if (!TeamManager.bothTeamsHavePlayers(arenaName)) {
            return false;
        }

        return true;
    }

    /**
     * Cancels the countdown for an arena immediately.
     * Used when a player leaves during countdown and conditions are no longer met.
     *
     * @param arenaName The arena name
     */
    public static void cancelCountdown(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        if (game != null && game.getGameState() == ArenaStateManager.ArenaState.STARTING) {
            // Cancel the countdown task
            game.stopCountdown();

            // Remove the game from active games
            activeGames.remove(arenaName);

            // Reset arena state to WAITING (was set to STARTING when countdown started)
            ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.WAITING);

            // Reset XP bar for all players
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.setLevel(0);
                        player.setExp(0.0f);
                    }
                }
            }

            // Note: Players stay in arena lobby after countdown cancellation, so no lobby scoreboard update needed

            // Broadcast cancellation message
            broadcastToArena(arenaName, "§cCountdown cancelled! Waiting for players...");

            // Play warning sound
            String sound = Obsidianwars.getInstance().getConfig().getString("sounds.countdown_cancelled", "ENTITY_VILLAGER_NO");
            Sound cancelSound = Obsidianwars.parseSound(sound);
            if (cancelSound != null) {
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    String playerArena = ObsidianCommand.playersInArena.get(uuid);
                    if (playerArena != null && playerArena.equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.playSound(player.getLocation(), cancelSound, 1.0f, 1.0f);
                        }
                    }
                }
            }

            Obsidianwars.getInstance().getLogger().info("Countdown cancelled for arena " + arenaName + " - waiting for players");
        }
    }

    public static void forceStartGame(String arenaName) {
        if (activeGames.containsKey(arenaName)) {
            // Cancel existing game if any to prevent task leaks
            ArenaGame existingGame = activeGames.get(arenaName);
            if (existingGame != null) {
                existingGame.stopGameTimer();
                if (existingGame.countdownTask != null) {
                    existingGame.countdownTask.cancel();
                }
            }
            activeGames.remove(arenaName);
        }

        // Force start - direct start without countdown
        ArenaGame game = new ArenaGame(arenaName);
        activeGames.put(arenaName, game);

        // Initialize arena state if not already tracked
        if (ArenaStateManager.getInstance().getState(arenaName) == null) {
            ArenaStateManager.getInstance().forceSetState(arenaName, ArenaStateManager.ArenaState.READY);
        }

        // Auto-teaming for any unassigned players
        autoTeamPlayers(arenaName);

        // Clear lobby items
        clearLobbyItems(arenaName);

        // Teleport players to team spawns
        teleportPlayersToTeamSpawns(arenaName);

        // Start game (startGame handles preparation timer if walls exist)
        startGame(arenaName);
    }

    public static void cancelCountdownAndForceStart(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        if (game != null && game.getGameState() == ArenaStateManager.ArenaState.STARTING) {
            // Cancel the countdown task
            game.stopCountdown();

            // Remove the game from active games
            activeGames.remove(arenaName);

            // Reset XP bar for all players
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.setLevel(0);
                        player.setExp(0.0f);
                    }
                }
            }

            // Force start the game
            forceStartGame(arenaName);
        }
    }

    public static boolean hasPlayersInArena(String arenaName) {
        Set<UUID> players = arenaPlayers.get(arenaName);
        return players != null && !players.isEmpty();
    }

    private static void startCountdown(String arenaName) {
        ArenaGame game = new ArenaGame(arenaName);
        activeGames.put(arenaName, game);

        // Initialize arena state if not already tracked
        if (ArenaStateManager.getInstance().getState(arenaName) == null) {
            ArenaStateManager.getInstance().forceSetState(arenaName, ArenaStateManager.ArenaState.READY);
        }

        // Set arena state to STARTING while lobby countdown is running
        ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.STARTING);

        DebugManager.logDebug("State transition: WAITING -> STARTING (countdown started)", arenaName);

        // Start countdown
        game.startCountdown();
    }

    public static void autoTeamPlayers(String arenaName) {
        List<UUID> unassignedPlayers = new java.util.ArrayList<>();

        // Find players without teams
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                if (!TeamListener.playerTeams.containsKey(uuid)) {
                    unassignedPlayers.add(uuid);
                }
            }
        }

        // Distribute players
        for (int i = 0; i < unassignedPlayers.size(); i++) {
            UUID uuid = unassignedPlayers.get(i);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Distribute by even and odd indices
                String team = (i % 2 == 0) ? "red" : "blue";
                TeamManager.setPlayerTeam(player, arenaName, team);

                String teamName = TeamConfig.getTeamName(team);
                String teamColor = TeamConfig.getTeamColor(team);
                String message = MessagesConfigManager.getMessage("auto_team", "teamColor", teamColor, "teamName", teamName);
                player.sendMessage(message);
            }
        }
    }

    public static void teleportPlayersToTeamSpawns(String arenaName) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    String team = TeamListener.playerTeams.get(uuid);
                    if (team != null) {
                        Location teamSpawn = getTeamSpawn(arenaName, team);
                        if (teamSpawn != null) {
                            player.teleport(teamSpawn);
                            // NO spawn protection - players can take damage immediately
                        }
                    }
                }
            }
        }
    }

    private static Location getTeamSpawn(String arenaName, String team) {
        return ArenaConfigManager.getTeamSpawn(arenaName, team);
    }

    public static void clearLobbyItems(String arenaName) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    // Clear lobby items
                    player.getInventory().clear();
                    
                    // Clear compass and door slots
                    player.getInventory().setItem(0, null);
                    player.getInventory().setItem(8, null);
                }
            }
        }
    }

    public static void startGame(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        if (game != null) {
            DebugManager.logDebug("Starting game for arena " + arenaName);

            // Initialize arena state if not already tracked
            if (ArenaStateManager.getInstance().getState(arenaName) == null) {
                ArenaStateManager.getInstance().forceSetState(arenaName, ArenaStateManager.ArenaState.READY);
            }

            // Set arena state to PLAYING immediately when match starts
            ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.PLAYING);
            DebugManager.logDebug("State transition: STARTING -> PLAYING (match started)", arenaName);

            // Remove spawn protection from all players immediately so they can take damage
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        ParticleManager.removeSpawnProtection(player);
                    }
                }
            }

            // Clear all dropped items in the arena world
            clearDroppedItems(arenaName);

            // Take snapshot of arena before game starts (CRITICAL - must succeed)
            boolean snapshotSuccess = ArenaSnapshotManager.takeSnapshot(arenaName);
            if (!snapshotSuccess) {
                Obsidianwars.getInstance().getLogger().severe("Failed to take snapshot for arena " + arenaName + " - game start aborted!");
                broadcastToArena(arenaName, "§cGame start failed - please contact an administrator.");
                // Reset arena state
                ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.READY);
                return;
            }
            DebugManager.logDebug("Arena snapshot taken successfully", arenaName);

            // Clear all non-player entities (mobs, animals, etc.) in the arena's world
            clearArenaMobs(arenaName);

            // Verify and force-place obsidian blocks before starting
            verifyAndPlaceObsidianBlocks(arenaName);

            // Check if walls are configured
            boolean hasWalls = WallManager.hasWallConfiguration(arenaName, "red") || WallManager.hasWallConfiguration(arenaName, "blue");

            if (hasWalls) {
                // Set to PREPARATION state and start preparation timer
                game.setGameState(ArenaStateManager.ArenaState.PREPARATION);
                ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.PREPARATION);
                DebugManager.logDebug("State transition: PLAYING -> PREPARATION", arenaName);
                WallManager.buildWalls(arenaName);
                WallManager.startPreparationTimer(arenaName);
            } else {
                // No walls - go directly to PLAYING state
                game.setGameState(ArenaStateManager.ArenaState.PLAYING);
                DebugManager.logDebug("State transition: PLAYING -> PLAYING (no walls, game starts)", arenaName);
                game.startGameTimer();

                // Apply world game rules
                WorldRulesManager.applyGameRules(arenaName);

                // Start obsidian particles
                ParticleManager.startObsidianParticles(arenaName);

                // Start per-minute XP task
                XPAwardListener.startPerMinuteTask(arenaName);
            }

            // Notify all players
            String message = hasWalls ?
                MessagesConfigManager.getMessage("preparation_phase_start") :
                MessagesConfigManager.getMessage("game_started");
            broadcastToArena(arenaName, message);

            // Play match start sound
            String sound = MessagesConfigManager.getSound("match_start");
            Sound startSound = Obsidianwars.parseSound(sound);
            if (startSound != null) {
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    String playerArena = ObsidianCommand.playersInArena.get(uuid);
                    if (playerArena != null && playerArena.equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.playSound(player.getLocation(), startSound, 1.0f, 1.0f);
                        }
                    }
                }
            }

            // Send rules announcement
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        List<String> rulesLines = MessagesConfigManager.getMessagesConfig().getStringList("messages.rules_announcement");
                        for (String line : rulesLines) {
                            player.sendMessage(line);
                        }
                    }
                }
            }

            // Update scoreboards
            updateArenaScoreboards(arenaName);
        }
    }

    public static void endGame(String arenaName, String winningTeam) {
        ArenaGame game = activeGames.get(arenaName);
        if (game != null) {
            game.setGameState(ArenaStateManager.ArenaState.ENDED);
            if (ArenaStateManager.getInstance().getState(arenaName) == null) {
                ArenaStateManager.getInstance().forceSetState(arenaName, ArenaStateManager.ArenaState.ENDED);
            } else {
                ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.ENDED);
            }
            DebugManager.logDebug("State transition: PLAYING -> ENDED (winner: " + winningTeam + ")", arenaName);

            // Stop per-minute XP task
            XPAwardListener.stopPerMinuteTask(arenaName);

            // Restore walls to BEDROCK immediately
            WallManager.restoreWalls(arenaName);

            // Victory message
            if (winningTeam != null) {
                String teamName = TeamConfig.getTeamName(winningTeam);
                String teamColor = TeamConfig.getTeamColor(winningTeam);

                String victoryMessage = MessagesConfigManager.getMessage("victory_message", "teamColor", teamColor, "teamName", teamName);
                broadcastToArena(arenaName, MessagesConfigManager.getMessage("victory") + " " + victoryMessage);

                // Track stats - wins for winning team, losses for losing team
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    String playerArena = ObsidianCommand.playersInArena.get(uuid);
                    if (playerArena != null && playerArena.equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            String playerTeam = TeamListener.playerTeams.get(uuid);
                            PlayerStats stats = StatsManager.getPlayerStats(player);
                            if (winningTeam.equals(playerTeam)) {
                                stats.addWin();
                                // Award XP for winning
                                XPAwardListener.awardWinXP(player);
                            } else {
                                stats.addLoss();
                            }
                            // Save stats asynchronously
                            StatsManager.savePlayerStats(uuid, player.getName());
                        }
                    }
                }

                // Send post-game chat summary (wrapped in try-catch to ensure cleanup always completes)
                try {
                    // Find MVP first
                    UUID mvpUuid = null;
                    int maxKills = -1;

                    for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                        String playerArena = ObsidianCommand.playersInArena.get(uuid);
                        if (playerArena != null && playerArena.equals(arenaName)) {
                            int kills = StatsManager.getKills(uuid);
                            if (kills > maxKills) {
                                maxKills = kills;
                                mvpUuid = uuid;
                            }
                        }
                    }

                    GameFeedbackService.sendPostGameSummary(arenaName, winningTeam, maxKills, mvpUuid);
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Error sending post-game summary for arena " + arenaName, e);
                }

                // Send victory notification
                GameFeedbackService.sendVictoryNotification(arenaName, winningTeam);

                // Start victory fireworks for winning team
                ParticleManager.spawnVictoryFireworks(arenaName, winningTeam);
            } else {
                String message = MessagesConfigManager.getMessage("game_admin_ended");
                broadcastToArena(arenaName, message);
            }

            // Cleanup after 10 seconds
            TaskManager.getInstance().runLater("arena-cleanup-" + arenaName, () -> {
                cleanupArena(arenaName);
            }, 200L, arenaName); // 10 seconds (200 ticks)
        }
    }

    private static void cleanupArena(String arenaName) {
        Obsidianwars.getInstance().getLogger().info("Starting arena cleanup for " + arenaName);

        ArenaGame game = activeGames.get(arenaName);
        if (game != null) {
            game.cleanup();
        }

        // Stop all arena-related tasks including fireworks
        ParticleManager.stopAllArenaTasks(arenaName);

        // Stop mob spawning
        MobSpawnerManager.stopMobSpawning(arenaName);

        // Stop wall-related timers (preparation and sudden death)
        WallManager.stopPreparationTimer(arenaName);
        WallManager.stopSuddenDeathCountdown(arenaName);

        // Restore world rules
        WorldRulesManager.restoreWorldRules(arenaName);

        // Clear all dropped items in the arena
        clearDroppedItems(arenaName);

        // Clear all non-player entities (mobs, dropped items, arrows, etc.) in the arena
        clearArenaMobs(arenaName);

        // Clear any lingering fireworks entities in the arena
        clearArenaFireworks(arenaName);

        // Restore arena snapshot using efficient runtime tracking
        // This now iterates through tracked changes and restores essential elements
        DebugManager.logDebug("Restoring arena snapshot", arenaName);
        boolean restoreSuccess = ArenaSnapshotManager.restoreSnapshot(arenaName);
        if (restoreSuccess) {
            DebugManager.logDebug("Arena snapshot restored successfully", arenaName);
        } else {
            DebugManager.logDebug("Arena snapshot restoration failed or no snapshot found", arenaName);
            Obsidianwars.getInstance().getLogger().warning(
                "Arena snapshot restoration failed for " + arenaName + " - arena may be in corrupted state"
            );
            // Note: We don't disable the arena here to allow manual admin intervention
            // Arena can be manually disabled via /o disablearena command if needed
        }

        // Update arena state back to READY
        if (ArenaStateManager.getInstance().getState(arenaName) == null) {
            ArenaStateManager.getInstance().forceSetState(arenaName, ArenaStateManager.ArenaState.READY);
        } else {
            ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.READY);
        }

        // Collect all players in arena first for visibility reset (using O(1) lookup)
        java.util.List<Player> arenaPlayerList = new ArrayList<>();
        for (UUID uuid : getPlayersInArena(arenaName)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                arenaPlayerList.add(player);
            }
        }

        // Clean up all players (using O(1) lookup)
        for (UUID uuid : new ArrayList<>(getPlayersInArena(arenaName))) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Remove spectator mode if player was a spectator
                if (SpectatorManager.isSpectator(player)) {
                    SpectatorManager.removeSpectatorMode(player);
                }

                // Show player to all other players in the arena (fixes visibility issue)
                for (Player otherPlayer : arenaPlayerList) {
                    if (otherPlayer != player && otherPlayer.isOnline()) {
                        player.showPlayer(Obsidianwars.getInstance(), otherPlayer);
                        otherPlayer.showPlayer(Obsidianwars.getInstance(), player);
                    }
                }

                // Reset player state and teleport to spawn
                Location mainSpawn = player.getWorld().getSpawnLocation();
                PlayerUtils.resetPlayerFull(player, mainSpawn);

                player.sendMessage("§aArena ended, returned to main spawn!");

                // Update lobby scoreboard after game ends
                LobbyScoreboardManager.updateLobbyScoreboard(player);
            }

            // Remove player from system
            ObsidianCommand.playersInArena.remove(uuid);
            removePlayerFromArena(uuid, arenaName);
            if (player != null) {
                TeamManager.removePlayerFromTeam(player);
            } else {
                // Player is offline, just remove from team map
                TeamListener.playerTeams.remove(uuid);
            }

            // Clean up wand positions for this player
            WandListener.pos1Map.remove(uuid);
            WandListener.pos2Map.remove(uuid);

            // Clean up disconnect record
            RejoinManager.clearDisconnectRecord(uuid);
        }

        // Also clean up any disconnected players data for this arena
        RejoinManager.cleanupArena(arenaName);

        // Game-i silirik
        activeGames.remove(arenaName);
    }

    public static void updateArenaScoreboards(String arenaName) {
        for (UUID uuid : getPlayersInArena(arenaName)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                ScoreboardManager.updateScoreboard(player);
            }
        }
    }

    private static void broadcastToArena(String arenaName, String message) {
        GameFeedbackService.broadcastToArena(arenaName, message);
    }

    private static boolean checkTeamBalance(String arenaName) {
        int redPlayers = 0;
        int bluePlayers = 0;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                String team = TeamListener.playerTeams.get(uuid);
                if (team != null) {
                    if (team.equals("red")) {
                        redPlayers++;
                    } else if (team.equals("blue")) {
                        bluePlayers++;
                    }
                }
            }
        }

        // Both teams must have at least 1 player
        return redPlayers > 0 && bluePlayers > 0;
    }

    public static void checkCountdownResume(String arenaName) {
        // No longer needed with new countdown logic
        // Countdowns are cancelled immediately when conditions are not met
    }

    private static void verifyAndPlaceObsidianBlocks(String arenaName) {
        // Check and force-place red team obsidian
        Location redObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "red");
        if (redObsidian != null) {
            if (redObsidian.getBlock().getType() != Material.OBSIDIAN) {
                redObsidian.getBlock().setType(Material.OBSIDIAN);
                Obsidianwars.getInstance().getLogger().info("Force-placed obsidian for red team in arena " + arenaName);
            }
        }

        // Check and force-place blue team obsidian
        Location blueObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "blue");
        if (blueObsidian != null) {
            if (blueObsidian.getBlock().getType() != Material.OBSIDIAN) {
                blueObsidian.getBlock().setType(Material.OBSIDIAN);
                Obsidianwars.getInstance().getLogger().info("Force-placed obsidian for blue team in arena " + arenaName);
            }
        }
    }

    public static void clearDroppedItems(String arenaName) {
        // Get arena region bounds from config
        Location[] region = ArenaConfigManager.getArenaRegion(arenaName, "main");
        if (region == null) {
            Obsidianwars.getInstance().getLogger().warning("No arena region defined for " + arenaName + " - skipping item clear");
            return;
        }

        World world = region[0].getWorld();
        if (world == null) {
            Obsidianwars.getInstance().getLogger().warning("Could not get world for arena " + arenaName + " - skipping item clear");
            return;
        }

        // Calculate region bounds
        int minX = Math.min(region[0].getBlockX(), region[1].getBlockX());
        int maxX = Math.max(region[0].getBlockX(), region[1].getBlockX());
        int minY = Math.min(region[0].getBlockY(), region[1].getBlockY());
        int maxY = Math.max(region[0].getBlockY(), region[1].getBlockY());
        int minZ = Math.min(region[0].getBlockZ(), region[1].getBlockZ());
        int maxZ = Math.max(region[0].getBlockZ(), region[1].getBlockZ());

        // Remove dropped items only within arena region bounds
        int clearedCount = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Item) {
                Location loc = entity.getLocation();
                // Check if entity is within arena region
                if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                    loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                    loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                    entity.remove();
                    clearedCount++;
                }
            }
        }

        if (clearedCount > 0) {
            Obsidianwars.getInstance().getLogger().info("Cleared " + clearedCount + " dropped items from arena " + arenaName + " region");
        }
    }

    public static void clearArenaMobs(String arenaName) {
        // Get arena region bounds from config
        Location[] region = ArenaConfigManager.getArenaRegion(arenaName, "main");
        if (region == null) {
            Obsidianwars.getInstance().getLogger().warning("No arena region defined for " + arenaName + " - skipping mob clear");
            return;
        }

        World world = region[0].getWorld();
        if (world == null) {
            Obsidianwars.getInstance().getLogger().warning("Could not get world for arena " + arenaName + " - skipping mob clear");
            return;
        }

        // Calculate region bounds
        int minX = Math.min(region[0].getBlockX(), region[1].getBlockX());
        int maxX = Math.max(region[0].getBlockX(), region[1].getBlockX());
        int minY = Math.min(region[0].getBlockY(), region[1].getBlockY());
        int maxY = Math.max(region[0].getBlockY(), region[1].getBlockY());
        int minZ = Math.min(region[0].getBlockZ(), region[1].getBlockZ());
        int maxZ = Math.max(region[0].getBlockZ(), region[1].getBlockZ());

        // Remove non-player entities only within arena region bounds
        int clearedCount = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                Location loc = entity.getLocation();
                // Check if entity is within arena region
                if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                    loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                    loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                    entity.remove();
                    clearedCount++;
                }
            }
        }

        if (clearedCount > 0) {
            Obsidianwars.getInstance().getLogger().info("Cleared " + clearedCount + " entities from arena " + arenaName + " region");
        }
    }

    private static void clearArenaFireworks(String arenaName) {
        // Get arena region bounds from config
        Location[] region = ArenaConfigManager.getArenaRegion(arenaName, "main");
        if (region == null) {
            Obsidianwars.getInstance().getLogger().warning("No arena region defined for " + arenaName + " - skipping fireworks clear");
            return;
        }

        World world = region[0].getWorld();
        if (world == null) {
            Obsidianwars.getInstance().getLogger().warning("Could not get world for arena " + arenaName + " - skipping fireworks clear");
            return;
        }

        // Calculate region bounds
        int minX = Math.min(region[0].getBlockX(), region[1].getBlockX());
        int maxX = Math.max(region[0].getBlockX(), region[1].getBlockX());
        int minY = Math.min(region[0].getBlockY(), region[1].getBlockY());
        int maxY = Math.max(region[0].getBlockY(), region[1].getBlockY());
        int minZ = Math.min(region[0].getBlockZ(), region[1].getBlockZ());
        int maxZ = Math.max(region[0].getBlockZ(), region[1].getBlockZ());

        // Remove fireworks entities within arena region bounds
        int clearedCount = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Firework) {
                Location loc = entity.getLocation();
                // Check if entity is within arena region
                if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                    loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                    loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                    entity.remove();
                    clearedCount++;
                }
            }
        }

        if (clearedCount > 0) {
            Obsidianwars.getInstance().getLogger().info("Cleared " + clearedCount + " fireworks from arena " + arenaName + " region");
        }
    }

    public static ArenaGame getGame(String arenaName) {
        return activeGames.get(arenaName);
    }

    public static void removeGame(String arenaName) {
        ArenaGame game = activeGames.remove(arenaName);
        if (game != null) {
            game.cleanup();
        }
    }

    public static void cleanupAllGames() {
        for (ArenaGame game : activeGames.values()) {
            game.cleanup();
        }
        activeGames.clear();
        arenaPlayers.clear();
        RejoinManager.cleanupAll();
        // Clean up all team data
        TeamManager.cleanup();
    }

    public static int getCountdown(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        return game != null ? game.getCountdown() : -1;
    }

    // Disconnect handling - delegated to RejoinManager
    public static void handleDisconnect(Player player) {
        RejoinManager.handleDisconnect(player);
    }

    public static boolean canRejoin(Player player) {
        return RejoinManager.canRejoin(player);
    }

    public static boolean restoreDisconnectedPlayer(Player player) {
        return RejoinManager.restoreDisconnectedPlayer(player);
    }

    public static void clearDisconnectRecord(UUID uuid) {
        RejoinManager.clearDisconnectRecord(uuid);
    }

    public static void cleanupExpiredDisconnectRecords() {
        RejoinManager.cleanupExpiredDisconnectRecords();
    }

    public static boolean isPlayerDisconnected(UUID uuid) {
        return RejoinManager.isPlayerDisconnected(uuid);
    }

    public static String getDisconnectedPlayerArena(UUID uuid) {
        return RejoinManager.getDisconnectedPlayerArena(uuid);
    }

    public static void checkTeamEliminationOnLeave(String arenaName) {
        checkTeamEliminationOnDisconnect(arenaName);
    }

    /**
     * Checks if a team has been eliminated due to all players being offline.
     * If so, declares the opposing team as winner.
     */
    private static void checkTeamEliminationOnDisconnect(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PLAYING) {
            return;
        }

        // Count online players per team
        int redOnline = 0;
        int blueOnline = 0;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    String team = TeamListener.playerTeams.get(uuid);
                    if (team != null) {
                        if (team.equals("red")) redOnline++;
                        else if (team.equals("blue")) blueOnline++;
                    }
                }
            }
        }

        // Also check disconnected players still in grace period
        for (DisconnectedPlayerData data : RejoinManager.getDisconnectedPlayersForArena(arenaName)) {
            long elapsed = System.currentTimeMillis() - data.getDisconnectTime();
            if (elapsed < 30000) { // 30 seconds grace period
                // Still in grace period, count as potentially returning
                if (data.getTeam().equals("red")) redOnline++;
                else if (data.getTeam().equals("blue")) blueOnline++;
            }
        }

        // Check if both teams have at least one player (online or in grace period)
        if (redOnline == 0 && game.isObsidianDestroyed("red")) {
            // Red team eliminated
            DebugManager.logDebug("Team elimination: Red team eliminated (0 players alive, obsidian destroyed)", arenaName);
            GameFeedbackService.broadcastToArena(arenaName, "§cRed team completely eliminated!");
            endGame(arenaName, "blue");
        } else if (blueOnline == 0 && game.isObsidianDestroyed("blue")) {
            // Blue team eliminated
            DebugManager.logDebug("Team elimination: Blue team eliminated (0 players alive, obsidian destroyed)", arenaName);
            GameFeedbackService.broadcastToArena(arenaName, "§cBlue team completely eliminated!");
            endGame(arenaName, "red");
        }
    }

    public static class ArenaGame {
        private final String arenaName;
        private ArenaStateManager.ArenaState gameState;
        private int countdown;
        BukkitTask countdownTask;
        private int gameTime;
        private BukkitTask gameTimerTask;
        private final Map<String, Boolean> obsidianDestroyed = new ConcurrentHashMap<>();
        private final Map<UUID, Integer> killStreaks = new ConcurrentHashMap<>();
        private boolean winDeclared = false; // Prevent duplicate win triggers

        public ArenaGame(String arenaName) {
            this.arenaName = arenaName;
            this.gameState = ArenaStateManager.ArenaState.WAITING;
            this.countdown = COUNTDOWN_SECONDS;
            this.gameTime = 0;

            // Initialize obsidian statuses
            obsidianDestroyed.put("red", false);
            obsidianDestroyed.put("blue", false);
        }

        public void startCountdown() {
            gameState = ArenaStateManager.ArenaState.STARTING;
            countdown = COUNTDOWN_SECONDS;

            Obsidianwars.getInstance().getLogger().info("Starting countdown for arena " + arenaName + " with " + COUNTDOWN_SECONDS + " seconds");

            String startMessage = MessagesConfigManager.getMessage("game_starting", "seconds", String.valueOf(COUNTDOWN_SECONDS));
            if (startMessage == null) startMessage = "§eGame starting in " + COUNTDOWN_SECONDS + " seconds!";
            broadcastToArena(arenaName, startMessage);

            countdownTask = TaskManager.getInstance().runTimer("countdown-" + arenaName, () -> {
                try {
                    countdown--;
                    DebugManager.logDebug("Countdown tick: " + countdown + " seconds remaining", arenaName);

                    if (countdown > 0) {
                        // Update XP bar for all players in arena
                        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                            String playerArena = ObsidianCommand.playersInArena.get(uuid);
                            if (playerArena != null && playerArena.equals(arenaName)) {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null && player.isOnline()) {
                                    // Set XP level to remaining countdown seconds
                                    player.setLevel(countdown);
                                    // Set XP progress to full bar
                                    player.setExp(1.0f);

                                    // Play sound effect in last 5 seconds
                                    if (countdown <= 5) {
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
                                }
                            }
                        }

                        // Broadcast countdown messages
                        if (countdown <= 5) {
                            // Enhanced countdown broadcast at 5 seconds and below
                            String countdownMessage = MessagesConfigManager.getMessage("game_countdown", "seconds", String.valueOf(countdown));
                            if (countdownMessage == null) countdownMessage = "§eGame starting in " + countdown + " seconds!";
                            broadcastToArena(arenaName, countdownMessage);

                            // Title/Subtitle broadcast
                            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                                if (playerArena != null && playerArena.equals(arenaName)) {
                                    Player player = Bukkit.getPlayer(uuid);
                                    if (player != null && player.isOnline()) {
                                        try {
                                            player.sendTitle("§c" + countdown, "§eGame starting!", 0, 20, 0);
                                        } catch (Exception e) {
                                            Obsidianwars.getInstance().getLogger().warning("Failed to send title to player " + player.getName() + ": " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        } else if (countdown % 5 == 0) {
                            // Regular countdown for 10, 15, etc.
                            String countdownMessage = MessagesConfigManager.getMessage("game_countdown", "seconds", String.valueOf(countdown));
                            if (countdownMessage == null) countdownMessage = "§eGame starting in " + countdown + " seconds!";
                            broadcastToArena(arenaName, countdownMessage);
                        }
                        updateArenaScoreboards(arenaName);
                    } else {
                        // Countdown finished
                        Obsidianwars.getInstance().getLogger().info("Countdown finished for arena " + arenaName + " - starting game");

                        if (countdownTask != null) {
                            countdownTask.cancel();
                            countdownTask = null;
                        }

                        // Reset XP bar for all players
                        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                            String playerArena = ObsidianCommand.playersInArena.get(uuid);
                            if (playerArena != null && playerArena.equals(arenaName)) {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null && player.isOnline()) {
                                    player.setLevel(0);
                                    player.setExp(0.0f);
                                }
                            }
                        }

                        // Auto-teaming for any unassigned players
                        autoTeamPlayers(arenaName);

                        // Clear lobby items
                        clearLobbyItems(arenaName);

                        // Teleport players to team spawns
                        teleportPlayersToTeamSpawns(arenaName);

                        // Start game
                        startGame(arenaName);
                    }
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Error in countdown for arena " + arenaName, e);
                }
            }, 20L, 20L, arenaName); // Every 1 second (20 ticks)

            Obsidianwars.getInstance().getLogger().info("Countdown task started for arena " + arenaName);
        }

        public void setGameState(ArenaStateManager.ArenaState gameState) {
            this.gameState = gameState;
        }

        public ArenaStateManager.ArenaState getGameState() {
            return gameState;
        }

        public int getCountdown() {
            return countdown;
        }

        public int getGameTime() {
            return gameTime;
        }

        public String getArenaName() {
            return arenaName;
        }

        public boolean isObsidianDestroyed(String team) {
            return obsidianDestroyed.getOrDefault(team, false);
        }

        public void setObsidianDestroyed(String team, boolean destroyed) {
            obsidianDestroyed.put(team, destroyed);
        }

        public boolean isWinDeclared() {
            return winDeclared;
        }

        public void setWinDeclared(boolean declared) {
            this.winDeclared = declared;
        }

        public void startGameTimer() {
            Obsidianwars.getInstance().getLogger().info("Starting game timer for arena " + arenaName);

            gameTimerTask = TaskManager.getInstance().runTimer("gametimer-" + arenaName, () -> {
                try {
                    gameTime++;
                    DebugManager.logDebug("Game time tick: " + gameTime + " seconds", arenaName);

                    // Check time limit
                    int timeLimitMinutes = ArenaConfigManager.getTimeLimit(arenaName);
                    int timeLimitSeconds = timeLimitMinutes * 60;
                    
                    if (gameTime >= timeLimitSeconds) {
                        // Time limit reached - force end game as draw
                        Obsidianwars.getInstance().getLogger().info("Time limit reached for arena " + arenaName + " - ending game");
                        stopGameTimer();
                        endGame(arenaName, null); // null = draw/no winner
                        return;
                    }
                    
                    // Warn at 5 minutes, 1 minute, and 30 seconds remaining
                    int remaining = timeLimitSeconds - gameTime;
                    if (remaining == 300) { // 5 minutes
                        broadcastToArena(arenaName, "§e5 minutes remaining!");
                    } else if (remaining == 60) { // 1 minute
                        broadcastToArena(arenaName, "§c1 minute remaining!");
                    } else if (remaining == 30) { // 30 seconds
                        broadcastToArena(arenaName, "§c30 seconds remaining!");
                    }
                    
                    updateArenaScoreboards(arenaName);
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Error in game timer for arena " + arenaName, e);
                }
            }, 20L, 20L, arenaName); // Every 1 second
        }

        public void stopGameTimer() {
            if (gameTimerTask != null) {
                gameTimerTask.cancel();
                gameTimerTask = null;
            }
        }

        public void stopCountdown() {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }

        public boolean isCountdownPaused() {
            // No longer used with new countdown logic
            return false;
        }

        public void cleanup() {
            stopGameTimer();
            stopCountdown();
            // Clear obsidian status
            obsidianDestroyed.clear();
            // Clear kill streaks
            killStreaks.clear();
            // Reset game state to WAITING
            gameState = ArenaStateManager.ArenaState.WAITING;
            // Reset countdown and game time
            countdown = COUNTDOWN_SECONDS;
            gameTime = 0;
        }

        // Kill Streak Methods
        public void addKill(UUID uuid) {
            int streak = killStreaks.getOrDefault(uuid, 0) + 1;
            killStreaks.put(uuid, streak);
            DebugManager.logDebug("Kill streak updated for UUID " + uuid + ": now " + streak + " kills", getArenaName());
            checkKillStreakRewards(uuid, streak);
        }

        public void resetKillStreak(UUID uuid) {
            killStreaks.remove(uuid);
        }

        public int getKillStreak(UUID uuid) {
            return killStreaks.getOrDefault(uuid, 0);
        }

        private void checkKillStreakRewards(UUID uuid, int streak) {
            DebugManager.logDebug("Checking kill streak rewards for UUID " + uuid + " at streak " + streak, getArenaName());

            if (!KillStreaksConfigManager.getKillStreaksConfig().getBoolean("kill-streaks.enabled", true)) {
                DebugManager.logDebug("Kill streaks disabled in config", getArenaName());
                return;
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                DebugManager.logDebug("Player is null or offline", getArenaName());
                return;
            }

            // Check rewards from config
            try {
                var rewardsSection = KillStreaksConfigManager.getKillStreaksConfig().getConfigurationSection("kill-streaks.rewards");
                if (rewardsSection == null) {
                    DebugManager.logDebug("kill-streaks.rewards section not found in config", getArenaName());
                    return;
                }

                DebugManager.logDebug("Available reward thresholds: " + rewardsSection.getKeys(false), getArenaName());

                for (String key : rewardsSection.getKeys(false)) {
                    int threshold = Integer.parseInt(key);
                    DebugManager.logDebug("Checking threshold " + threshold + " against streak " + streak, getArenaName());
                    if (streak == threshold) {
                        DebugManager.logDebug("Applying reward for threshold " + threshold, getArenaName());
                        applyKillStreakReward(player, threshold, streak);
                    }
                }
            } catch (Exception e) {
                DebugManager.logDebug("Error checking kill streak rewards: " + e.getMessage(), getArenaName());
                Obsidianwars.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Error checking kill streak rewards for arena " + getArenaName(), e);
            }
        }

        private void applyKillStreakReward(Player player, int threshold, int streak) {
            String path = "kill-streaks.rewards." + threshold;

            // Points reward
            int points = KillStreaksConfigManager.getKillStreaksConfig().getInt(path + ".points", 0);
            if (points > 0) {
                // Assuming points will be used when shop is implemented
                player.sendMessage("§a+" + points + " points for " + streak + " kill streak!");
            }

            // Message to player
            String message = KillStreaksConfigManager.getKillStreaksConfig().getString(path + ".message", "");
            if (!message.isEmpty()) {
                message = message.replace("%player%", player.getName()).replace("%streak%", String.valueOf(streak));
                player.sendMessage(message);
            }

            // Broadcast message
            boolean broadcast = KillStreaksConfigManager.getKillStreaksConfig().getBoolean(path + ".broadcast", false);
            if (broadcast) {
                String broadcastMsg = KillStreaksConfigManager.getKillStreaksConfig().getString(path + ".broadcast-message", "");
                if (!broadcastMsg.isEmpty()) {
                    broadcastMsg = broadcastMsg.replace("%player%", player.getName()).replace("%streak%", String.valueOf(streak));
                    broadcastToArena(arenaName, broadcastMsg);
                }
            }

            // Potion effect reward
            String effectType = KillStreaksConfigManager.getKillStreaksConfig().getString(path + ".effect", "");
            if (!effectType.isEmpty()) {
                int duration = KillStreaksConfigManager.getKillStreaksConfig().getInt(path + ".duration", 30);
                int amplifier = KillStreaksConfigManager.getKillStreaksConfig().getInt(path + ".amplifier", 0);
                try {
                    org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(effectType);
                    if (type == null) {
                        Obsidianwars.getInstance().getLogger().warning("Invalid potion effect type: " + effectType);
                    } else {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration * 20, amplifier));
                        player.sendMessage("§eYou received " + effectType + " for " + duration + " seconds!");
                    }
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().warning("Invalid potion effect type: " + effectType);
                }
            }

            // Glow effect for high streaks
            if (streak >= 5) {
                player.setGlowing(true);
                // Remove glow after 10 seconds
                TaskManager.getInstance().runLater("glow-" + player.getUniqueId(), () -> {
                    player.setGlowing(false);
                }, 200L, arenaName);
            }
        }

        public void triggerSuddenDeath() {
            DebugManager.logDebug("SUDDEN DEATH triggered - breaking all obsidians in 10 seconds", arenaName);

            // Broadcast sudden death message
            broadcastToArena(arenaName, "§c§lSUDDEN DEATH! All obsidians will break!");

            // Break both obsidians after 10 seconds
            TaskManager.getInstance().runLater("sudden-death-" + arenaName, () -> {
                // Get obsidian locations
                Location redObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "red");
                Location blueObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "blue");
                
                // Break red obsidian
                if (redObsidian != null && !isObsidianDestroyed("red")) {
                    redObsidian.getBlock().setType(Material.AIR);
                    setObsidianDestroyed("red", true);
                    ParticleManager.stopObsidianParticles(arenaName, "red");
                    DebugManager.logDebug("Sudden death: Red team obsidian destroyed", arenaName);
                }

                // Break blue obsidian
                if (blueObsidian != null && !isObsidianDestroyed("blue")) {
                    blueObsidian.getBlock().setType(Material.AIR);
                    setObsidianDestroyed("blue", true);
                    ParticleManager.stopObsidianParticles(arenaName, "blue");
                    DebugManager.logDebug("Sudden death: Blue team obsidian destroyed", arenaName);
                }
                
                // Broadcast message
                broadcastToArena(arenaName, "§cAll obsidians destroyed! Final eliminations started!");

                // Check win condition by calling the static method after both obsidians are destroyed
                TaskManager.getInstance().runLater("win-check-" + arenaName, () -> {
                    checkArenaWinCondition(arenaName);
                }, 2L, arenaName); // Small delay to ensure spectator mode changes are processed
            }, 200L, arenaName); // 10 seconds (200 ticks)
        }
    }

    public static void checkArenaWinCondition(String arenaName) {
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null) return;

        // Prevent duplicate win triggers
        if (game.isWinDeclared()) {
            DebugManager.logDebug("Win condition check skipped - win already declared", arenaName);
            return;
        }

        // Calculate alive player count for each team
        int redAlive = getAliveTeamPlayers(arenaName, "red");
        int blueAlive = getAliveTeamPlayers(arenaName, "blue");

        DebugManager.logDebug("Win condition check: Red alive=" + redAlive + ", Blue alive=" + blueAlive +
            ", Red obsidian destroyed=" + game.isObsidianDestroyed("red") + ", Blue obsidian destroyed=" + game.isObsidianDestroyed("blue"), arenaName);

        // If a team is completely eliminated
        if (redAlive == 0 && game.isObsidianDestroyed("red")) {
            // Blue team wins
            DebugManager.logDebug("Blue team wins! Red team eliminated.", arenaName);
            game.setWinDeclared(true);
            GameManager.endGame(arenaName, "blue");
        } else if (blueAlive == 0 && game.isObsidianDestroyed("blue")) {
            // Red team wins
            DebugManager.logDebug("Red team wins! Blue team eliminated.", arenaName);
            game.setWinDeclared(true);
            GameManager.endGame(arenaName, "red");
        }
    }

    private static int getAliveTeamPlayers(String arenaName, String team) {
        int count = 0;
        ArenaGame game = activeGames.get(arenaName);

        for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                String playerTeam = TeamListener.playerTeams.get(uuid);
                if (team.equals(playerTeam)) {
                    org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
                    // Count online players who are not spectators (actively playing)
                    if (player != null && player.isOnline() && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        count++;
                    }
                    // Count players who are spectators but have an active respawn timer (waiting to respawn)
                    // This ensures players waiting to respawn are counted even if their obsidian is destroyed
                    else if (player != null && player.isOnline() && player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                        // Check if this player is waiting to respawn (has respawn protection or is in death cycle)
                        // SpectatorManager tracks respawn state - count them as alive
                        if (SpectatorManager.isRespawning(uuid)) {
                            count++;
                            DebugManager.logDebug("Counting respawning spectator: " + player.getName(), arenaName);
                        }
                    }
                    // Only count disconnected players in grace period if their obsidian is NOT destroyed
                    // If obsidian is destroyed, they can't respawn even if they rejoin, so they're eliminated
                    else if (RejoinManager.isPlayerDisconnected(uuid) && game != null && !game.isObsidianDestroyed(team)) {
                        DisconnectedPlayerData data = RejoinManager.getDisconnectedPlayerData(uuid);
                        if (data != null && data.getTeam().equals(team)) {
                            long elapsed = System.currentTimeMillis() - data.getDisconnectTime();
                            if (elapsed < 30000) { // 30 seconds grace period
                                count++; // Count as potentially alive only if obsidian is intact
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

}