package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
        if (activeGames.containsKey(arenaName)) {
            return; // Oyun artıq başlayıb və ya saymaqdadır
        }

        int minPlayers = ArenaConfigManager.getMinPlayers(arenaName);
        int currentPlayers = getArenaPlayerCount(arenaName);

        if (currentPlayers >= minPlayers) {
            startCountdown(arenaName);
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

        // Auto-teaming
        autoTeamPlayers(arenaName);

        // Lobby items təmizlə
        clearLobbyItems(arenaName);

        // Oyunçuları komanda spawnlarına teleport et
        teleportPlayersToTeamSpawns(arenaName);

        // Oyunu başlat (startGame handles preparation timer if walls exist)
        startGame(arenaName);
    }

    private static void startCountdown(String arenaName) {
        ArenaGame game = new ArenaGame(arenaName);
        activeGames.put(arenaName, game);

        // Countdown başladırıq
        game.startCountdown();
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
                TeamListener.playerTeams.put(uuid, team);
                TeamListener.applyTeamColor(player, team);
                TeamManager.setupTeam(player, team);
                
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
            // Clear all non-player entities (mobs, animals, etc.) in the arena's world
            clearArenaMobs(arenaName);

            // Verify and force-place obsidian blocks before starting
            verifyAndPlaceObsidianBlocks(arenaName);

            // Check if walls are configured
            boolean hasWalls = WallManager.hasWallConfiguration(arenaName, "red") || WallManager.hasWallConfiguration(arenaName, "blue");

            if (hasWalls) {
                // Set to PREPARATION state and start preparation timer
                game.setGameState(GameState.PREPARATION);
                WallManager.buildWalls(arenaName);
                WallManager.startPreparationTimer(arenaName);
            } else {
                // No walls - go directly to PLAYING state
                game.setGameState(GameState.PLAYING);
                game.startGameTimer();

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

            // Stop all arena-related tasks
            ParticleManager.stopAllArenaTasks(arenaName);

            // Stop mob spawning
            MobSpawnerManager.stopMobSpawning(arenaName);

            // Restore world rules
            WorldRulesManager.restoreWorldRules(arenaName);

            // Stop timers and restore walls to BEDROCK
            WallManager.stopPreparationTimer(arenaName);
            WallManager.stopSuddenDeathCountdown(arenaName);
            WallManager.restoreWalls(arenaName);

            // Victory mesajı
            if (winningTeam != null) {
                String teamName = winningTeam.equals("red") ? "Qırmızı" : "Mavi";
                String teamColor = winningTeam.equals("red") ? "§c" : "§9";

                String victoryMessage = MessagesConfigManager.getMessage("victory_message", "teamColor", teamColor, "teamName", teamName);
                broadcastToArena(arenaName, MessagesConfigManager.getMessage("victory") + " " + victoryMessage);

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
        ArenaGame game = activeGames.get(arenaName);
        if (game != null) {
            game.cleanup();
        }

        // Bütün oyunçuları təmizləyirik
        for (UUID uuid : new ArrayList<>(ObsidianCommand.playersInArena.keySet())) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    // Inventory təmizlə
                    player.getInventory().clear();
                    player.getInventory().setHelmet(null);
                    player.getInventory().setChestplate(null);
                    player.getInventory().setLeggings(null);
                    player.getInventory().setBoots(null);

                    // Ad rəngini təmizlə
                    player.setDisplayName(player.getName());
                    player.setPlayerListName(player.getName());

                    // Komanda sistemindən çıxarırıq
                    TeamManager.removePlayerFromTeams(player);

                    // GameMode-u təmizlə
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);

                    // Scoreboard təmizlə
                    ScoreboardManager.removeScoreboard(player);

                    // Oyunçunu əsas spawn-a qaytar
                    Location mainSpawn = player.getWorld().getSpawnLocation();
                    player.teleport(mainSpawn);

                    player.sendMessage("§aArena bitdi, əsas spawn-a qayıtdınız!");
                }

                // Oyunçunu sistemdən çıxarırıq
                ObsidianCommand.playersInArena.remove(uuid);
                TeamListener.playerTeams.remove(uuid);

                // Clean up wand positions for this player
                WandListener.pos1Map.remove(uuid);
                WandListener.pos2Map.remove(uuid);

                // Clean up disconnect record
                clearDisconnectRecord(uuid);
            }
        }

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
        ArenaGame game = activeGames.get(arenaName);
        if (game != null && game.isCountdownPaused()) {
            if (checkTeamBalance(arenaName)) {
                game.resumeCountdown();
            }
        }
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
        // Get the arena's world from the lobby spawn or team spawn
        Location arenaLocation = ArenaConfigManager.getLobbySpawn(arenaName);
        if (arenaLocation == null) {
            arenaLocation = ArenaConfigManager.getTeamSpawn(arenaName, "red");
        }
        if (arenaLocation == null) {
            arenaLocation = ArenaConfigManager.getTeamSpawn(arenaName, "blue");
        }

        if (arenaLocation == null) {
            Obsidianwars.getInstance().getLogger().warning("Could not determine arena world for " + arenaName + " - skipping mob clear");
            return;
        }

        World world = arenaLocation.getWorld();
        if (world == null) {
            Obsidianwars.getInstance().getLogger().warning("Could not get world for arena " + arenaName + " - skipping mob clear");
            return;
        }

        // Remove all non-player entities (mobs, animals, dropped items, etc.)
        int clearedCount = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
                clearedCount++;
            }
        }

        if (clearedCount > 0) {
            Obsidianwars.getInstance().getLogger().info("Cleared " + clearedCount + " non-player entities from arena " + arenaName + " world");
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
    }

    public static int getCountdown(String arenaName) {
        ArenaGame game = activeGames.get(arenaName);
        return game != null ? game.getCountdown() : -1;
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
    private static final long REJOIN_GRACE_PERIOD = 30000; // 30 seconds in milliseconds

    public static void handleDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (ObsidianCommand.playersInArena.containsKey(uuid)) {
            disconnectTimes.put(uuid, System.currentTimeMillis());
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

    public static void clearDisconnectRecord(UUID uuid) {
        disconnectTimes.remove(uuid);
    }

    public static void cleanupExpiredDisconnectRecords() {
        long currentTime = System.currentTimeMillis();
        disconnectTimes.entrySet().removeIf(entry -> {
            long elapsed = currentTime - entry.getValue();
            return elapsed >= REJOIN_GRACE_PERIOD;
        });
    }

    static class ArenaGame {
        private final String arenaName;
        private GameState gameState;
        private int countdown;
        private BukkitTask countdownTask;
        private int gameTime;
        private BukkitTask gameTimerTask;
        private final Map<String, Boolean> obsidianDestroyed = new HashMap<>();
        private boolean countdownPaused = false;
        private boolean balanceWarningSent = false;

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
            countdownPaused = false;
            balanceWarningSent = false;

            Obsidianwars.getInstance().getLogger().info("Starting countdown for arena " + arenaName + " with " + COUNTDOWN_SECONDS + " seconds");

            String startMessage = MessagesConfigManager.getMessage("game_starting", "seconds", String.valueOf(COUNTDOWN_SECONDS));
            if (startMessage == null) startMessage = "§eGame starting in " + COUNTDOWN_SECONDS + " seconds!";
            broadcastToArena(arenaName, startMessage);

            countdownTask = Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), () -> {
                try {
                    // If countdown is paused, check if we can resume
                    if (countdownPaused) {
                        Obsidianwars.getInstance().getLogger().info("Countdown paused for arena " + arenaName + " - checking team balance");
                        if (checkTeamBalance(arenaName)) {
                            // Teams are balanced, resume countdown
                            countdownPaused = false;
                            balanceWarningSent = false;
                            broadcastToArena(arenaName, "§aGame start resumed! Both teams now have players.");
                            // Reset countdown to 5 seconds to give players time to prepare
                            countdown = 5;
                        }
                        return; // Skip this tick while paused
                    }

                    countdown--;
                    Obsidianwars.getInstance().getLogger().info("Countdown tick: " + countdown + " seconds remaining for arena " + arenaName);

                    if (countdown > 0) {
                        // Enhanced countdown broadcast at 5 seconds and below
                        if (countdown <= 5) {
                            // Team balance check at 5 seconds
                            if (countdown == 5) {
                                if (!checkTeamBalance(arenaName)) {
                                    // Pause countdown and reset
                                    pauseCountdown();
                                    return; // Skip this tick
                                }
                            }
                            
                            // Chat message
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
                        // Countdown bitdi
                        Obsidianwars.getInstance().getLogger().info("Countdown finished for arena " + arenaName + " - starting game");
                        
                        if (countdownTask != null) {
                            countdownTask.cancel();
                        }
                        
                        // Auto-teaming
                        autoTeamPlayers(arenaName);
                        
                        // Lobby items təmizlə
                        clearLobbyItems(arenaName);
                        
                        // Oyunçuları komanda spawnlarına teleport et
                        teleportPlayersToTeamSpawns(arenaName);
                        
                        // Oyunu başlat
                        startGame(arenaName);
                        
                        // Preparation phase başlat (walls və timer)
                        if (WallManager.hasWallConfiguration(arenaName, "red") || WallManager.hasWallConfiguration(arenaName, "blue")) {
                            WallManager.buildWalls(arenaName);
                            WallManager.startPreparationTimer(arenaName);
                        }
                    }
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().severe("Error in countdown for arena " + arenaName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, 20L, 20L); // Hər 1 saniyə (20 tick)
            
            Obsidianwars.getInstance().getLogger().info("Countdown task started for arena " + arenaName);
        }

        private void pauseCountdown() {
            countdownPaused = true;
            countdown = 6; // Reset to safe state
            
            // Send warning message only once
            if (!balanceWarningSent) {
                broadcastToArena(arenaName, "§cGame start paused: Opposing team needs at least 1 player!");
                balanceWarningSent = true;
                
                // Attempt auto-balance if only one team has players
                attemptAutoBalance(arenaName);
            }
        }

        private void attemptAutoBalance(String arenaName) {
            // Count players per team
            int redPlayers = 0;
            int bluePlayers = 0;
            UUID lastRedPlayer = null;
            UUID lastBluePlayer = null;
            
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    String team = TeamListener.playerTeams.get(uuid);
                    if (team != null) {
                        if (team.equals("red")) {
                            redPlayers++;
                            lastRedPlayer = uuid;
                        } else if (team.equals("blue")) {
                            bluePlayers++;
                            lastBluePlayer = uuid;
                        }
                    }
                }
            }
            
            // Auto-balance: if one team is empty and the other has players, move the last player
            if (redPlayers > 0 && bluePlayers == 0 && lastRedPlayer != null) {
                // Move last red player to blue team
                Player player = Bukkit.getPlayer(lastRedPlayer);
                if (player != null) {
                    TeamListener.playerTeams.put(lastRedPlayer, "blue");
                    TeamListener.applyTeamColor(player, "blue");
                    TeamManager.setupTeam(player, "blue");
                    ScoreboardManager.updateScoreboard(player);
                    player.sendMessage("§aYou were automatically moved to §9Mavi Team§a to balance the game!");
                    broadcastToArena(arenaName, "§eAuto-balance: Player moved to Mavi Team!");
                    
                    // Resume countdown after auto-balance
                    Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                        resumeCountdown();
                    }, 20L); // 1 second delay before resuming
                }
            } else if (bluePlayers > 0 && redPlayers == 0 && lastBluePlayer != null) {
                // Move last blue player to red team
                Player player = Bukkit.getPlayer(lastBluePlayer);
                if (player != null) {
                    TeamListener.playerTeams.put(lastBluePlayer, "red");
                    TeamListener.applyTeamColor(player, "red");
                    TeamManager.setupTeam(player, "red");
                    ScoreboardManager.updateScoreboard(player);
                    player.sendMessage("§aYou were automatically moved to §cQırmızı Team§a to balance the game!");
                    broadcastToArena(arenaName, "§eAuto-balance: Player moved to Qırmızı Team!");
                    
                    // Resume countdown after auto-balance
                    Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                        resumeCountdown();
                    }, 20L); // 1 second delay before resuming
                }
            }
        }

        public boolean isCountdownPaused() {
            return countdownPaused;
        }

        public void resumeCountdown() {
            if (countdownPaused) {
                countdownPaused = false;
                balanceWarningSent = false;
                countdown = 5; // Resume from 5 seconds
                broadcastToArena(arenaName, "§aGame start resumed! Both teams now have players.");
            }
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

        public void startGameTimer() {
            Obsidianwars.getInstance().getLogger().info("Starting game timer for arena " + arenaName);
            
            gameTimerTask = Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), () -> {
                try {
                    gameTime++;
                    Obsidianwars.getInstance().getLogger().info("Game time tick: " + gameTime + " seconds for arena " + arenaName);
                    
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

        public void cleanup() {
            stopGameTimer();
            stopCountdown();
        }

        public void triggerSuddenDeath() {
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
                }
                
                // Break blue obsidian
                if (blueObsidian != null && !isObsidianDestroyed("blue")) {
                    blueObsidian.getBlock().setType(Material.AIR);
                    setObsidianDestroyed("blue", true);
                    ParticleManager.stopObsidianParticles(arenaName, "blue");
                }
                
                // Broadcast message
                broadcastToArena(arenaName, "§cBütün obsidianlar qırıldı! Final eliminations başladı!");
                
                // Check win condition by calling the static method
                checkArenaWinCondition(arenaName);
            }, 200L); // 10 seconds (200 ticks)
        }
    }

    public static void checkArenaWinCondition(String arenaName) {
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null) return;

        // Hər komandanın canlı oyunçu sayını hesablayırıq
        int redAlive = getAliveTeamPlayers(arenaName, "red");
        int blueAlive = getAliveTeamPlayers(arenaName, "blue");

        // Əgər bir komanda tamamilə elimine edilibsə
        if (redAlive == 0 && game.isObsidianDestroyed("red")) {
            // Mavi komanda qalib gəldi
            GameManager.endGame(arenaName, "blue");
        } else if (blueAlive == 0 && game.isObsidianDestroyed("blue")) {
            // Qırmızı komanda qalib gəldi
            GameManager.endGame(arenaName, "red");
        }
    }

    private static int getAliveTeamPlayers(String arenaName, String team) {
        int count = 0;
        for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                String playerTeam = TeamListener.playerTeams.get(uuid);
                if (team.equals(playerTeam)) {
                    org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
                    if (player != null && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}