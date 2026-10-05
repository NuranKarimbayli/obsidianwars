package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private static final Map<String, ArenaGame> activeGames = new HashMap<>();
    private static final int COUNTDOWN_SECONDS = 20;

    public static void checkGameStart(String arenaName) {
        ArenaGame existingGame = activeGames.get(arenaName);
        if (existingGame != null) {
            // If game is in COUNTDOWN state, check if we need to cancel
            if (existingGame.getGameState() == GameState.COUNTDOWN) {
                // Check if conditions are still met
                if (!canStartCountdown(arenaName)) {
                    // Cancel countdown immediately
                    cancelCountdown(arenaName);
                }
            }
            return; // Oyun artıq başlayıb və ya saymaqdadır
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
        if (game != null && game.getGameState() == GameState.COUNTDOWN) {
            // Cancel the countdown task
            game.stopCountdown();

            // Remove the game from active games
            activeGames.remove(arenaName);

            // Reset arena status to READY
            ArenaConfigManager.setArenaStatus(arenaName, "READY");

            // Reset XP bar for all players
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.setLevel(0);
                        player.setExp(0.0f);
                    }
                }
            }

            // Broadcast cancellation message
            broadcastToArena(arenaName, "§cCountdown cancelled! Waiting for players...");

            // Play warning sound
            String sound = Obsidianwars.getInstance().getConfig().getString("sounds.countdown_cancelled", "ENTITY_VILLAGER_NO");
            Sound cancelSound = Obsidianwars.parseSound(sound);
            if (cancelSound != null) {
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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

        // Məcburi başlatma - countdown olmadan birbaşa start
        ArenaGame game = new ArenaGame(arenaName);
        activeGames.put(arenaName, game);

        // Auto-teaming for any unassigned players
        autoTeamPlayers(arenaName);

        // Lobby items təmizlə
        clearLobbyItems(arenaName);

        // Oyunçuları komanda spawnlarına teleport et
        teleportPlayersToTeamSpawns(arenaName);

        // Oyunu başlat (startGame handles preparation timer if walls exist)
        startGame(arenaName);
    }

    public static void cancelCountdownAndForceStart(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        if (game != null && game.getGameState() == GameState.COUNTDOWN) {
            // Cancel the countdown task
            game.stopCountdown();

            // Remove the game from active games
            activeGames.remove(arenaName);

            // Reset XP bar for all players
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                return true;
            }
        }
        return false;
    }

    private static void startCountdown(String arenaName) {
        ArenaGame game = new ArenaGame(arenaName);
        activeGames.put(arenaName, game);

        // Set arena status to STARTING (orange color)
        ArenaConfigManager.setArenaStatus(arenaName, "STARTING");

        DebugManager.logDebug("State transition: WAITING -> STARTING", arenaName);

        // Countdown başladırıq
        game.startCountdown();
    }

    public static void autoTeamPlayers(String arenaName) {
        List<UUID> unassignedPlayers = new java.util.ArrayList<>();

        // Komandasız oyunçuları tapırıq
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                if (!TeamListener.playerTeams.containsKey(uuid)) {
                    unassignedPlayers.add(uuid);
                }
            }
        }

        // Oyunçuları bölüşdürürük
        for (int i = 0; i < unassignedPlayers.size(); i++) {
            UUID uuid = unassignedPlayers.get(i);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Cüt və tək indekslərə görə komanda bölüşdürürük
                String team = (i % 2 == 0) ? "red" : "blue";
                TeamManager.setPlayerTeam(player, arenaName, team);

                String teamName = team.equals("red") ? "Qırmızı" : "Mavi";
                String teamColor = team.equals("red") ? "§c" : "§9";
                String message = MessagesConfigManager.getMessage("auto_team", "teamColor", teamColor, "teamName", teamName);
                player.sendMessage(message);
            }
        }
    }

    public static void teleportPlayersToTeamSpawns(String arenaName) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    String team = TeamListener.playerTeams.get(uuid);
                    if (team != null) {
                        Location teamSpawn = getTeamSpawn(arenaName, team);
                        if (teamSpawn != null) {
                            player.teleport(teamSpawn);
                            // Give spawn protection
                            ParticleManager.giveSpawnProtection(player, 3);
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
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    // Lobby items təmizləyirik
                    player.getInventory().clear();
                    
                    // Kompas və qapı slotlarını təmizləyirik
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

            // Take snapshot of arena before game starts
            boolean snapshotSuccess = ArenaSnapshotManager.takeSnapshot(arenaName);
            if (snapshotSuccess) {
                DebugManager.logDebug("Arena snapshot taken successfully", arenaName);
            } else {
                Obsidianwars.getInstance().getLogger().severe("Failed to take snapshot for arena " + arenaName + " - blocks may not be restored after game ends!");
            }

            // Clear all non-player entities (mobs, animals, etc.) in the arena's world
            clearArenaMobs(arenaName);

            // Verify and force-place obsidian blocks before starting
            verifyAndPlaceObsidianBlocks(arenaName);

            // Check if walls are configured
            boolean hasWalls = WallManager.hasWallConfiguration(arenaName, "red") || WallManager.hasWallConfiguration(arenaName, "blue");

            if (hasWalls) {
                // Set to PREPARATION state and start preparation timer
                game.setGameState(GameState.PREPARATION);
                DebugManager.logDebug("State transition: STARTING -> PREPARATION", arenaName);
                WallManager.buildWalls(arenaName);
                WallManager.startPreparationTimer(arenaName);
            } else {
                // No walls - go directly to PLAYING state
                game.setGameState(GameState.PLAYING);
                DebugManager.logDebug("State transition: STARTING -> PLAYING", arenaName);
                game.startGameTimer();

                // Update arena status to PLAYING
                ArenaConfigManager.setArenaStatus(arenaName, "PLAYING");

                // Apply world game rules
                WorldRulesManager.applyGameRules(arenaName);

                // Start obsidian particles
                ParticleManager.startObsidianParticles(arenaName);
            }

            // Bütün oyunçulara xəbər veririk
            String message = hasWalls ?
                MessagesConfigManager.getMessage("preparation_phase_start") :
                MessagesConfigManager.getMessage("game_started");
            broadcastToArena(arenaName, message);

            // Play match start sound
            String sound = MessagesConfigManager.getSound("match_start");
            Sound startSound = Obsidianwars.parseSound(sound);
            if (startSound != null) {
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.playSound(player.getLocation(), startSound, 1.0f, 1.0f);
                        }
                    }
                }
            }

            // Send rules announcement
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        List<String> rulesLines = MessagesConfigManager.getMessagesConfig().getStringList("messages.rules_announcement");
                        for (String line : rulesLines) {
                            player.sendMessage(line);
                        }
                    }
                }
            }

            // Scoreboardları yeniləyirik
            updateArenaScoreboards(arenaName);
        }
    }

    public static void endGame(String arenaName, String winningTeam) {
        ArenaGame game = activeGames.get(arenaName);
        if (game != null) {
            game.setGameState(GameState.ENDED);
            DebugManager.logDebug("State transition: PLAYING -> ENDED (winner: " + winningTeam + ")", arenaName);

            // Restore walls to BEDROCK immediately
            WallManager.restoreWalls(arenaName);

            // Victory mesajı
            if (winningTeam != null) {
                String teamName = winningTeam.equals("red") ? "Qırmızı" : "Mavi";
                String teamColor = winningTeam.equals("red") ? "§c" : "§9";

                String victoryMessage = MessagesConfigManager.getMessage("victory_message", "teamColor", teamColor, "teamName", teamName);
                broadcastToArena(arenaName, MessagesConfigManager.getMessage("victory") + " " + victoryMessage);

                // Track stats - wins for winning team, losses for losing team
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            String playerTeam = TeamListener.playerTeams.get(uuid);
                            StatsManager.PlayerStats stats = StatsManager.getPlayerStats(player);
                            if (winningTeam.equals(playerTeam)) {
                                stats.addWin();
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
                    sendPostGameSummary(arenaName, winningTeam);
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().severe("Error sending post-game summary for arena " + arenaName + ": " + e.getMessage());
                    e.printStackTrace();
                }

                // UI_TOAST_CHALLENGE_COMPLETE sound for all players
                Sound victorySound = Obsidianwars.parseSound("UI_TOAST_CHALLENGE_COMPLETE");
                if (victorySound == null) {
                    // Fallback to configured sound
                    String sound = MessagesConfigManager.getSound("win_victory");
                    victorySound = Obsidianwars.parseSound(sound);
                }
                if (victorySound != null) {
                    for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                        if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                player.playSound(player.getLocation(), victorySound, 1.0f, 1.0f);
                                player.sendTitle("§a§lVICTORY!", teamColor + teamName + " Team won!", 10, 60, 20);
                            }
                        }
                    }
                }

                // Start victory fireworks for winning team
                ParticleManager.spawnVictoryFireworks(arenaName, winningTeam);
            } else {
                String message = MessagesConfigManager.getMessage("game_admin_ended");
                broadcastToArena(arenaName, message);
            }

            // 10 saniyə sonra təmizləmə
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                cleanupArena(arenaName);
            }, 200L); // 10 saniyə (200 tick)
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
        }

        // Update arena status back to READY
        ArenaConfigManager.setArenaStatus(arenaName, "READY");

        // Collect all players in arena first for visibility reset
        java.util.List<Player> arenaPlayers = new ArrayList<>();
        for (UUID uuid : new ArrayList<>(ObsidianCommand.playersInArena.keySet())) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    arenaPlayers.add(player);
                }
            }
        }

        // Bütün oyunçuları təmizləyirik
        for (UUID uuid : new ArrayList<>(ObsidianCommand.playersInArena.keySet())) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    // Remove spectator mode if player was a spectator
                    if (SpectatorManager.isSpectator(player)) {
                        SpectatorManager.removeSpectatorMode(player);
                    }

                    // Show player to all other players in the arena (fixes visibility issue)
                    for (Player otherPlayer : arenaPlayers) {
                        if (otherPlayer != player && otherPlayer.isOnline()) {
                            player.showPlayer(Obsidianwars.getInstance(), otherPlayer);
                            otherPlayer.showPlayer(Obsidianwars.getInstance(), player);
                        }
                    }

                    // Reset player state and teleport to spawn
                    Location mainSpawn = player.getWorld().getSpawnLocation();
                    PlayerUtils.resetPlayerFull(player, mainSpawn);

                    player.sendMessage("§aArena bitdi, əsas spawn-a qayıtdınız!");
                }

                // Oyunçunu sistemdən çıxarırıq
                ObsidianCommand.playersInArena.remove(uuid);
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
                clearDisconnectRecord(uuid);
            }
        }

        // Also clean up any disconnected players data for this arena
        disconnectedPlayers.entrySet().removeIf(entry -> {
            DisconnectedPlayerData data = entry.getValue();
            if (data.getArenaName().equals(arenaName)) {
                // Also remove from playersInArena if still there
                ObsidianCommand.playersInArena.remove(entry.getKey());
                TeamListener.playerTeams.remove(entry.getKey());
                return true;
            }
            return false;
        });

        // Game-i silirik
        activeGames.remove(arenaName);
    }

    public static void updateArenaScoreboards(String arenaName) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    ScoreboardManager.updateScoreboard(player);
                }
            }
        }
    }

    private static void broadcastToArena(String arenaName, String message) {
        ObsidianCommand.broadcastToArena(arenaName, message);
    }

    private static boolean checkTeamBalance(String arenaName) {
        int redPlayers = 0;
        int bluePlayers = 0;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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

    private static void clearArenaMobs(String arenaName) {
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
        disconnectTimes.clear();
        disconnectedPlayers.clear();
        // Clean up all team data
        TeamManager.cleanup();
    }

    public static int getCountdown(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        return game != null ? game.getCountdown() : -1;
    }

    /**
     * Sends a post-game chat summary to all players in the arena.
     * Includes winner, MVP, and individual player stats.
     */
    private static void sendPostGameSummary(String arenaName, String winningTeam) {
        // Find MVP (player with most kills)
        UUID mvpUuid = null;
        int maxKills = -1;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                int kills = StatsManager.getKills(uuid);
                if (kills > maxKills) {
                    maxKills = kills;
                    mvpUuid = uuid;
                }
            }
        }

        // Broadcast winner
        String teamName = winningTeam.equals("red") ? "Qırmızı" : "Mavi";
        String teamColor = winningTeam.equals("red") ? "§c" : "§9";
        broadcastToArena(arenaName, "§6§l=== MATCH SUMMARY ===");
        broadcastToArena(arenaName, teamColor + "§lWinner: " + teamName + " Team");

        // Broadcast MVP
        if (mvpUuid != null) {
            Player mvpPlayer = Bukkit.getPlayer(mvpUuid);
            if (mvpPlayer != null) {
                broadcastToArena(arenaName, "§e§lMVP: §f" + mvpPlayer.getName() + " §ewith §e" + maxKills + " §eKills");
            }
        }

        // Broadcast individual stats
        broadcastToArena(arenaName, "§7--------------------");
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    int kills = StatsManager.getKills(uuid);
                    int deaths = StatsManager.getDeaths(uuid);
                    String playerTeam = TeamListener.playerTeams.get(uuid);
                    // Safe null-check for playerTeam (spectators or players with cleared team)
                    String teamPrefix = (playerTeam != null && playerTeam.equals("red")) ? "§c" : "§9";
                    player.sendMessage(teamPrefix + player.getName() + " §7- §eKills: " + kills + " §7| §cDeaths: " + deaths);
                }
            }
        }
        broadcastToArena(arenaName, "§7--------------------");
    }

    enum GameState {
        WAITING,
        COUNTDOWN,
        PREPARATION,
        PLAYING,
        ENDED
    }

    // Disconnect handling
    private static final Map<UUID, Long> disconnectTimes = new HashMap<>();
    private static final Map<UUID, DisconnectedPlayerData> disconnectedPlayers = new HashMap<>();
    private static final long REJOIN_GRACE_PERIOD = 30000; // 30 seconds in milliseconds

    public static void handleDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (ObsidianCommand.playersInArena.containsKey(uuid)) {
            String arenaName = ObsidianCommand.playersInArena.get(uuid);
            String team = TeamListener.playerTeams.get(uuid);

            // Only save state if in PLAYING or PREPARATION state
            ArenaGame game = activeGames.get(arenaName);
            if (game != null && (game.getGameState() == GameState.PLAYING || game.getGameState() == GameState.PREPARATION)) {
                // Save player state for rejoin
                DisconnectedPlayerData data = new DisconnectedPlayerData(player, arenaName, team);
                disconnectedPlayers.put(uuid, data);
                disconnectTimes.put(uuid, System.currentTimeMillis());

                Obsidianwars.getInstance().getLogger().info("Player " + player.getName() + " disconnected from arena " + arenaName + " - state saved for rejoin");
            } else {
                // Not in active game, just record disconnect time for legacy
                disconnectTimes.put(uuid, System.currentTimeMillis());
            }
        }
    }

    public static boolean canRejoin(Player player) {
        UUID uuid = player.getUniqueId();
        Long disconnectTime = disconnectTimes.get(uuid);
        if (disconnectTime == null) return false;

        long elapsed = System.currentTimeMillis() - disconnectTime;
        if (elapsed < REJOIN_GRACE_PERIOD) {
            return true;
        }

        disconnectTimes.remove(uuid);
        return false;
    }

    /**
     * Attempts to restore a disconnected player to their game state.
     * Returns true if restoration was successful, false otherwise.
     */
    public static boolean restoreDisconnectedPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        DisconnectedPlayerData data = disconnectedPlayers.get(uuid);

        if (data == null) {
            return false;
        }

        // Check if still within grace period
        long elapsed = System.currentTimeMillis() - data.getDisconnectTime();
        if (elapsed >= REJOIN_GRACE_PERIOD) {
            // Grace period expired, clean up
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);
            return false;
        }

        // Check if game still exists and is in valid state
        ArenaGame game = activeGames.get(data.getArenaName());
        if (game == null || (game.getGameState() != GameState.PLAYING && game.getGameState() != GameState.PREPARATION)) {
            // Game ended or invalid, clean up and return false
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);
            return false;
        }

        // Restore player state
        try {
            // Restore inventory
            player.getInventory().setContents(data.getInventoryContents());
            player.getInventory().setArmorContents(data.getArmorContents());

            // Restore health and hunger
            player.setHealth(data.getHealth());
            player.setFoodLevel(data.getFoodLevel());

            // Restore potion effects
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            for (org.bukkit.potion.PotionEffect effect : data.getPotionEffects()) {
                player.addPotionEffect(effect);
            }

            // Restore team (without armor to prevent leather armor on respawn)
            TeamManager.setPlayerTeam(player, data.getArenaName(), data.getTeam(), false);

            // Restore game mode
            if (data.wasSpectator()) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
                // Teleport to last location or obsidian
                Location restoreLoc = data.getLastLocation();
                if (restoreLoc != null) {
                    player.teleport(restoreLoc);
                }
            } else {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.setAllowFlight(false);
                player.setFlying(false);

                // Teleport to team spawn with spawn protection
                Location teamSpawn = ArenaConfigManager.getTeamSpawn(data.getArenaName(), data.getTeam());
                if (teamSpawn != null) {
                    player.teleport(teamSpawn);
                    ParticleManager.giveSpawnProtection(player, 3);
                } else {
                    // Fallback to last location
                    if (data.getLastLocation() != null) {
                        player.teleport(data.getLastLocation());
                    }
                }
            }

            // Update scoreboard
            ScoreboardManager.updateScoreboard(player);

            // Clear disconnect records
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);

            Obsidianwars.getInstance().getLogger().info("Player " + player.getName() + " successfully restored to arena " + data.getArenaName());

            return true;
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().severe("Error restoring player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            // Clean up on error
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);
            return false;
        }
    }

    /**
     * Permanently removes a disconnected player from the game after grace period expires.
     */
    public static void expireDisconnectedPlayer(UUID uuid) {
        DisconnectedPlayerData data = disconnectedPlayers.remove(uuid);
        disconnectTimes.remove(uuid);

        if (data != null) {
            // Remove from arena tracking
            ObsidianCommand.playersInArena.remove(uuid);
            TeamListener.playerTeams.remove(uuid);

            // Check if this causes team elimination
            checkTeamEliminationOnDisconnect(data.getArenaName());

            Obsidianwars.getInstance().getLogger().info("Player " + uuid + " grace period expired - permanently removed from arena " + data.getArenaName());
        }
    }

    /**
     * Checks if a team has been eliminated due to all players being offline.
     * If so, declares the opposing team as winner.
     */
    private static void checkTeamEliminationOnDisconnect(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        if (game == null || game.getGameState() != GameState.PLAYING) {
            return;
        }

        // Count online players per team
        int redOnline = 0;
        int blueOnline = 0;

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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
        for (DisconnectedPlayerData data : disconnectedPlayers.values()) {
            if (data.getArenaName().equals(arenaName)) {
                long elapsed = System.currentTimeMillis() - data.getDisconnectTime();
                if (elapsed < REJOIN_GRACE_PERIOD) {
                    // Still in grace period, count as potentially returning
                    if (data.getTeam().equals("red")) redOnline++;
                    else if (data.getTeam().equals("blue")) blueOnline++;
                }
            }
        }

        // Check if both teams have at least one player (online or in grace period)
        if (redOnline == 0 && game.isObsidianDestroyed("red")) {
            // Red team eliminated
            DebugManager.logDebug("Team elimination: Red team eliminated (0 players alive, obsidian destroyed)", arenaName);
            broadcastToArena(arenaName, "§cQırmızı komanda tamamilə elimine edildi!");
            endGame(arenaName, "blue");
        } else if (blueOnline == 0 && game.isObsidianDestroyed("blue")) {
            // Blue team eliminated
            DebugManager.logDebug("Team elimination: Blue team eliminated (0 players alive, obsidian destroyed)", arenaName);
            broadcastToArena(arenaName, "§cMavi komanda tamamilə elimine edildi!");
            endGame(arenaName, "red");
        }
    }

    public static void clearDisconnectRecord(UUID uuid) {
        disconnectTimes.remove(uuid);
        disconnectedPlayers.remove(uuid);
    }

    public static void cleanupExpiredDisconnectRecords() {
        long currentTime = System.currentTimeMillis();

        // Find expired players
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : disconnectTimes.entrySet()) {
            long elapsed = currentTime - entry.getValue();
            if (elapsed >= REJOIN_GRACE_PERIOD) {
                expired.add(entry.getKey());
            }
        }

        // Process expired players
        for (UUID uuid : expired) {
            expireDisconnectedPlayer(uuid);
        }
    }

    /**
     * Checks if a player is currently disconnected but within grace period.
     */
    public static boolean isPlayerDisconnected(UUID uuid) {
        return disconnectedPlayers.containsKey(uuid);
    }

    /**
     * Gets the arena name for a disconnected player.
     */
    public static String getDisconnectedPlayerArena(UUID uuid) {
        DisconnectedPlayerData data = disconnectedPlayers.get(uuid);
        return data != null ? data.getArenaName() : null;
    }

    /**
     * Public method to check team elimination after a manual leave.
     * Called when a player uses /obsidian leave during an active game.
     */
    public static void checkTeamEliminationOnLeave(String arenaName) {
        checkTeamEliminationOnDisconnect(arenaName);
    }

    static class ArenaGame {
        private final String arenaName;
        private GameState gameState;
        private int countdown;
        BukkitTask countdownTask;
        private int gameTime;
        private BukkitTask gameTimerTask;
        private final Map<String, Boolean> obsidianDestroyed = new HashMap<>();
        private final Map<UUID, Integer> killStreaks = new HashMap<>();
        private boolean winDeclared = false; // Prevent duplicate win triggers

        public ArenaGame(String arenaName) {
            this.arenaName = arenaName;
            this.gameState = GameState.WAITING;
            this.countdown = COUNTDOWN_SECONDS;
            this.gameTime = 0;

            // Obsidian statuslarını init edirik
            obsidianDestroyed.put("red", false);
            obsidianDestroyed.put("blue", false);
        }

        public void startCountdown() {
            gameState = GameState.COUNTDOWN;
            countdown = COUNTDOWN_SECONDS;

            Obsidianwars.getInstance().getLogger().info("Starting countdown for arena " + arenaName + " with " + COUNTDOWN_SECONDS + " seconds");

            String startMessage = MessagesConfigManager.getMessage("game_starting", "seconds", String.valueOf(COUNTDOWN_SECONDS));
            if (startMessage == null) startMessage = "§eGame starting in " + COUNTDOWN_SECONDS + " seconds!";
            broadcastToArena(arenaName, startMessage);

            countdownTask = Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), () -> {
                try {
                    countdown--;
                    DebugManager.logDebug("Countdown tick: " + countdown + " seconds remaining", arenaName);

                    if (countdown > 0) {
                        // Update XP bar for all players in arena
                        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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
                                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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
                            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null && player.isOnline()) {
                                    player.setLevel(0);
                                    player.setExp(0.0f);
                                }
                            }
                        }

                        // Auto-teaming for any unassigned players
                        autoTeamPlayers(arenaName);

                        // Lobby items təmizlə
                        clearLobbyItems(arenaName);

                        // Oyunçuları komanda spawnlarına teleport et
                        teleportPlayersToTeamSpawns(arenaName);

                        // Oyunu başlat
                        startGame(arenaName);
                    }
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().severe("Error in countdown for arena " + arenaName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, 20L, 20L); // Hər 1 saniyə (20 tick)

            Obsidianwars.getInstance().getLogger().info("Countdown task started for arena " + arenaName);
        }

        public void setGameState(GameState gameState) {
            this.gameState = gameState;
        }

        public GameState getGameState() {
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
            
            gameTimerTask = Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), () -> {
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
                        broadcastToArena(arenaName, "§e5 dəqiqə qaldı!");
                    } else if (remaining == 60) { // 1 minute
                        broadcastToArena(arenaName, "§c1 dəqiqə qaldı!");
                    } else if (remaining == 30) { // 30 seconds
                        broadcastToArena(arenaName, "§c30 saniyə qaldı!");
                    }
                    
                    updateArenaScoreboards(arenaName);
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().severe("Error in game timer for arena " + arenaName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, 20L, 20L); // Hər 1 saniyə
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
            gameState = GameState.WAITING;
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

            if (!Obsidianwars.getInstance().getConfig().getBoolean("kill-streaks.enabled", true)) {
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
                var rewardsSection = Obsidianwars.getInstance().getConfig().getConfigurationSection("kill-streaks.rewards");
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
                e.printStackTrace();
            }
        }

        private void applyKillStreakReward(Player player, int threshold, int streak) {
            String path = "kill-streaks.rewards." + threshold;

            // Points reward
            int points = Obsidianwars.getInstance().getConfig().getInt(path + ".points", 0);
            if (points > 0) {
                // Assuming points will be used when shop is implemented
                player.sendMessage("§a+" + points + " points for " + streak + " kill streak!");
            }

            // Message to player
            String message = Obsidianwars.getInstance().getConfig().getString(path + ".message", "");
            if (!message.isEmpty()) {
                message = message.replace("%player%", player.getName()).replace("%streak%", String.valueOf(streak));
                player.sendMessage(message);
            }

            // Broadcast message
            boolean broadcast = Obsidianwars.getInstance().getConfig().getBoolean(path + ".broadcast", false);
            if (broadcast) {
                String broadcastMsg = Obsidianwars.getInstance().getConfig().getString(path + ".broadcast-message", "");
                if (!broadcastMsg.isEmpty()) {
                    broadcastMsg = broadcastMsg.replace("%player%", player.getName()).replace("%streak%", String.valueOf(streak));
                    broadcastToArena(arenaName, broadcastMsg);
                }
            }

            // Potion effect reward
            String effectType = Obsidianwars.getInstance().getConfig().getString(path + ".effect", "");
            if (!effectType.isEmpty()) {
                int duration = Obsidianwars.getInstance().getConfig().getInt(path + ".duration", 30);
                int amplifier = Obsidianwars.getInstance().getConfig().getInt(path + ".amplifier", 0);
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
                Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                    player.setGlowing(false);
                }, 200L);
            }
        }

        public void triggerSuddenDeath() {
            DebugManager.logDebug("SUDDEN DEATH triggered - breaking all obsidians in 10 seconds", arenaName);

            // Broadcast sudden death message
            broadcastToArena(arenaName, "§c§lSUDDEN DEATH! Bütün obsidianlar qırılacaq!");

            // Break both obsidians after 10 seconds
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
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
                broadcastToArena(arenaName, "§cBütün obsidianlar qırıldı! Final eliminations başladı!");

                // Check win condition by calling the static method after both obsidians are destroyed
                Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                    checkArenaWinCondition(arenaName);
                }, 2L); // Small delay to ensure spectator mode changes are processed
            }, 200L); // 10 seconds (200 ticks)
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

        // Hər komandanın canlı oyunçu sayını hesablayırıq
        int redAlive = getAliveTeamPlayers(arenaName, "red");
        int blueAlive = getAliveTeamPlayers(arenaName, "blue");

        DebugManager.logDebug("Win condition check: Red alive=" + redAlive + ", Blue alive=" + blueAlive +
            ", Red obsidian destroyed=" + game.isObsidianDestroyed("red") + ", Blue obsidian destroyed=" + game.isObsidianDestroyed("blue"), arenaName);

        // Əgər bir komanda tamamilə elimine edilibsə
        if (redAlive == 0 && game.isObsidianDestroyed("red")) {
            // Mavi komanda qalib gəldi
            DebugManager.logDebug("Blue team wins! Red team eliminated.", arenaName);
            game.setWinDeclared(true);
            GameManager.endGame(arenaName, "blue");
        } else if (blueAlive == 0 && game.isObsidianDestroyed("blue")) {
            // Qırmızı komanda qalib gəldi
            DebugManager.logDebug("Red team wins! Blue team eliminated.", arenaName);
            game.setWinDeclared(true);
            GameManager.endGame(arenaName, "red");
        }
    }

    private static int getAliveTeamPlayers(String arenaName, String team) {
        int count = 0;
        ArenaGame game = activeGames.get(arenaName);

        for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
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
                    else if (GameManager.isPlayerDisconnected(uuid) && game != null && !game.isObsidianDestroyed(team)) {
                        DisconnectedPlayerData data = disconnectedPlayers.get(uuid);
                        if (data != null && data.getTeam().equals(team)) {
                            long elapsed = System.currentTimeMillis() - data.getDisconnectTime();
                            if (elapsed < REJOIN_GRACE_PERIOD) {
                                count++; // Count as potentially alive only if obsidian is intact
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Data class to store disconnected player state for rejoin restoration.
     */
    public static class DisconnectedPlayerData {
        private final UUID uuid;
        private final String arenaName;
        private final String team;
        private final long disconnectTime;
        private final ItemStack[] inventoryContents;
        private final ItemStack[] armorContents;
        private final double health;
        private final int foodLevel;
        private final List<org.bukkit.potion.PotionEffect> potionEffects;
        private final Location lastLocation;
        private final boolean wasSpectator;

        public DisconnectedPlayerData(Player player, String arenaName, String team) {
            this.uuid = player.getUniqueId();
            this.arenaName = arenaName;
            this.team = team;
            this.disconnectTime = System.currentTimeMillis();
            this.inventoryContents = player.getInventory().getContents().clone();
            this.armorContents = player.getInventory().getArmorContents().clone();
            this.health = player.getHealth();
            this.foodLevel = player.getFoodLevel();
            this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
            this.lastLocation = player.getLocation().clone();
            this.wasSpectator = player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
        }

        public UUID getUuid() { return uuid; }
        public String getArenaName() { return arenaName; }
        public String getTeam() { return team; }
        public long getDisconnectTime() { return disconnectTime; }
        public ItemStack[] getInventoryContents() { return inventoryContents; }
        public ItemStack[] getArmorContents() { return armorContents; }
        public double getHealth() { return health; }
        public int getFoodLevel() { return foodLevel; }
        public List<org.bukkit.potion.PotionEffect> getPotionEffects() { return potionEffects; }
        public Location getLastLocation() { return lastLocation; }
        public boolean wasSpectator() { return wasSpectator; }
    }
}