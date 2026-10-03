package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class WallManager {

    private static final Map<String, BukkitTask> wallTimerTasks = new HashMap<>();
    private static final Map<String, BukkitTask> suddenDeathTasks = new HashMap<>();
    private static final Map<String, Integer> wallDurations = new HashMap<>();
    private static final Map<String, Integer> currentRemainingTime = new HashMap<>();
    private static final Map<String, Integer> suddenDeathRemainingTime = new HashMap<>();

    public static void setWall(String arenaName, String team, Location pos1, Location pos2) {
        ArenaConfigManager.setWallRegion(arenaName, team, pos1, pos2);
    }

    public static void setTimer(String arenaName, int minutes) {
        ArenaConfigManager.setPreparationTimer(arenaName, minutes);
        wallDurations.put(arenaName, minutes);
    }

    public static int getTimer(String arenaName) {
        return ArenaConfigManager.getPreparationTimer(arenaName);
    }

    public static int getRemainingTime(String arenaName) {
        return currentRemainingTime.getOrDefault(arenaName, 0);
    }

    public static int getSuddenDeathRemainingTime(String arenaName) {
        return suddenDeathRemainingTime.getOrDefault(arenaName, 0);
    }

    public static void buildWalls(String arenaName) {
        buildTeamWall(arenaName, "red");
        buildTeamWall(arenaName, "blue");
    }

    private static void buildTeamWall(String arenaName, String team) {
        Location[] wallRegion = ArenaConfigManager.getWallRegion(arenaName, team);
        if (wallRegion == null) return;

        Location pos1 = wallRegion[0];
        Location pos2 = wallRegion[1];
        World world = pos1.getWorld();
        if (world == null) return;

        // Calculate min/max coordinates
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Calculate chunk boundaries
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        // Iterate chunk-by-chunk directly without allocating Location objects
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                if (!chunk.isLoaded()) {
                    chunk.load();
                }

                // Calculate block boundaries within this chunk
                int chunkMinX = Math.max(minX, chunkX << 4);
                int chunkMaxX = Math.min(maxX, (chunkX << 4) + 15);
                int chunkMinZ = Math.max(minZ, chunkZ << 4);
                int chunkMaxZ = Math.min(maxZ, (chunkZ << 4) + 15);

                // Set blocks directly within chunk
                for (int x = chunkMinX; x <= chunkMaxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                            chunk.getBlock(x & 15, y, z & 15).setType(Material.BEDROCK);
                        }
                    }
                }
            }
        }
    }

    public static void removeWalls(String arenaName) {
        removeTeamWall(arenaName, "red");
        removeTeamWall(arenaName, "blue");
    }

    private static void removeTeamWall(String arenaName, String team) {
        Location[] wallRegion = ArenaConfigManager.getWallRegion(arenaName, team);
        if (wallRegion == null) return;

        Location pos1 = wallRegion[0];
        Location pos2 = wallRegion[1];
        World world = pos1.getWorld();
        if (world == null) return;

        // Calculate min/max coordinates
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Calculate chunk boundaries
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        // Iterate chunk-by-chunk directly without allocating Location objects
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                if (!chunk.isLoaded()) {
                    chunk.load();
                }

                // Calculate block boundaries within this chunk
                int chunkMinX = Math.max(minX, chunkX << 4);
                int chunkMaxX = Math.min(maxX, (chunkX << 4) + 15);
                int chunkMinZ = Math.max(minZ, chunkZ << 4);
                int chunkMaxZ = Math.min(maxZ, (chunkZ << 4) + 15);

                // Set blocks directly within chunk
                for (int x = chunkMinX; x <= chunkMaxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                            chunk.getBlock(x & 15, y, z & 15).setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    public static void restoreWalls(String arenaName) {
        restoreTeamWall(arenaName, "red");
        restoreTeamWall(arenaName, "blue");
    }

    private static void restoreTeamWall(String arenaName, String team) {
        Location[] wallRegion = ArenaConfigManager.getWallRegion(arenaName, team);
        if (wallRegion == null) return;

        Location pos1 = wallRegion[0];
        Location pos2 = wallRegion[1];
        World world = pos1.getWorld();
        if (world == null) return;

        // Calculate min/max coordinates
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Calculate chunk boundaries
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        // Iterate chunk-by-chunk directly without allocating Location objects
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                if (!chunk.isLoaded()) {
                    chunk.load();
                }

                // Calculate block boundaries within this chunk
                int chunkMinX = Math.max(minX, chunkX << 4);
                int chunkMaxX = Math.min(maxX, (chunkX << 4) + 15);
                int chunkMinZ = Math.max(minZ, chunkZ << 4);
                int chunkMaxZ = Math.min(maxZ, (chunkZ << 4) + 15);

                // Set blocks directly within chunk
                for (int x = chunkMinX; x <= chunkMaxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                            chunk.getBlock(x & 15, y, z & 15).setType(Material.BEDROCK);
                        }
                    }
                }
            }
        }
    }

    public static void startPreparationTimer(String arenaName) {
        // Stop existing timer
        stopPreparationTimer(arenaName);

        int durationMinutes = getTimer(arenaName);
        int durationSeconds = durationMinutes * 60;

        Obsidianwars.getInstance().getLogger().info("Starting preparation timer for arena " + arenaName + " with " + durationSeconds + " seconds");

        // Broadcast preparation phase start
        String prepStartMsg = MessagesConfigManager.getMessage("preparation_phase_start");
        if (prepStartMsg == null) prepStartMsg = "§ePreparation phase started! Gather resources!";
        ObsidianCommand.broadcastToArena(arenaName, prepStartMsg);

        // Start countdown task using BukkitRunnable with robust error handling
        BukkitRunnable timerTask = new BukkitRunnable() {
            int remaining = durationSeconds;

            @Override
            public void run() {
                try {
                    remaining--;
                    currentRemainingTime.put(arenaName, remaining);
                    
                    Obsidianwars.getInstance().getLogger().info("Preparation timer tick: " + remaining + " seconds remaining for arena " + arenaName);
                    
                    // Update scoreboard with preparation time (MM:SS format)
                    int minutes = remaining / 60;
                    int seconds = remaining % 60;
                    String timeStr = String.format("%02d:%02d", minutes, seconds);
                    
                    // Send action bar to all players in arena
                    String actionBar = "§ePreparation Time: §f" + timeStr;
                    
                    for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                        if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                            org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                try {
                                    player.sendActionBar(actionBar);
                                } catch (Exception e) {
                                    Obsidianwars.getInstance().getLogger().warning("Failed to send action bar to player " + player.getName() + ": " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    if (remaining <= 0) {
                        Obsidianwars.getInstance().getLogger().info("Preparation timer reached 0 for arena " + arenaName + " - removing walls");
                        
                        // Preparation phase over - GUARANTEE wall removal on main thread
                        Bukkit.getScheduler().runTask(Obsidianwars.getInstance(), () -> {
                            try {
                                removeWalls(arenaName);
                                Obsidianwars.getInstance().getLogger().info("Wall removal completed for arena " + arenaName);
                            } catch (Exception e) {
                                Obsidianwars.getInstance().getLogger().severe("Error in wall removal: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                        
                        stopPreparationTimer(arenaName);
                        
                        // Transition to PLAYING state
                        GameManager.ArenaGame game = GameManager.getGame(arenaName);
                        if (game != null) {
                            game.setGameState(GameManager.GameState.PLAYING);
                            game.startGameTimer();
                            
                            // Apply world game rules
                            WorldRulesManager.applyGameRules(arenaName);
                            
                            // Start obsidian particles
                            ParticleManager.startObsidianParticles(arenaName);
                        }
                        
                        // Start mob spawning (combat phase)
                        MobSpawnerManager.startMobSpawning(arenaName);
                        
                        // Play sound and broadcast message
                        String sound = MessagesConfigManager.getSound("wall_fall");
                        Sound wallSound = Obsidianwars.parseSound(sound);
                        if (wallSound != null) {
                            for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                                    org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
                                    if (player != null && player.isOnline()) {
                                        try {
                                            player.playSound(player.getLocation(), wallSound, 1.0f, 1.0f);
                                            String title = MessagesConfigManager.getMessage("wall_fallen_title");
                                            String subtitle = MessagesConfigManager.getMessage("wall_fallen_subtitle");
                                            if (title == null) title = "§aWALLS FALLEN!";
                                            if (subtitle == null) subtitle = "§eCombat phase begins!";
                                            player.sendTitle(title, subtitle, 10, 60, 20);
                                        } catch (Exception e) {
                                            Obsidianwars.getInstance().getLogger().warning("Failed to play sound for player " + player.getName() + ": " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                        
                        String message = MessagesConfigManager.getMessage("preparation_over");
                        if (message == null) message = "§aPreparation phase over! Combat begins!";
                        ObsidianCommand.broadcastToArena(arenaName, message);
                        
                        // Start sudden death countdown
                        startSuddenDeathCountdown(arenaName);
                    }
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().severe("Error in preparation timer for arena " + arenaName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // Start the task using runTaskTimer
        BukkitTask task = timerTask.runTaskTimer(Obsidianwars.getInstance(), 0L, 20L); // Start immediately, every second (20 ticks)
        wallTimerTasks.put(arenaName, task);
        Obsidianwars.getInstance().getLogger().info("Preparation timer task started for arena " + arenaName);
    }

    public static void startSuddenDeathCountdown(String arenaName) {
        // Stop existing sudden death timer
        stopSuddenDeathCountdown(arenaName);

        int durationMinutes = ArenaConfigManager.getSuddenDeathTimer(arenaName);
        int durationSeconds = durationMinutes * 60;

        Obsidianwars.getInstance().getLogger().info("Starting sudden death countdown for arena " + arenaName + " with " + durationSeconds + " seconds");

        // Start sudden death countdown task
        BukkitRunnable suddenDeathTask = new BukkitRunnable() {
            int remaining = durationSeconds;

            @Override
            public void run() {
                try {
                    remaining--;
                    suddenDeathRemainingTime.put(arenaName, remaining);
                    
                    Obsidianwars.getInstance().getLogger().info("Sudden death tick: " + remaining + " seconds remaining for arena " + arenaName);
                    
                    // Update scoreboard with sudden death time (MM:SS format)
                    int minutes = remaining / 60;
                    int seconds = remaining % 60;
                    String timeStr = String.format("%02d:%02d", minutes, seconds);
                    
                    // Update scoreboard for all players in arena (do NOT send action bar)
                    for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                        if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                            org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                try {
                                    ScoreboardManager.updateScoreboard(player);
                                } catch (Exception e) {
                                    Obsidianwars.getInstance().getLogger().warning("Failed to update scoreboard for player " + player.getName() + ": " + e.getMessage());
                                }
                            }
                        }
                    }

                    if (remaining <= 0) {
                        Obsidianwars.getInstance().getLogger().info("Sudden death countdown reached 0 for arena " + arenaName);
                        stopSuddenDeathCountdown(arenaName);
                        
                        // Trigger actual sudden death in GameManager
                        GameManager.ArenaGame game = GameManager.getGame(arenaName);
                        if (game != null) {
                            game.triggerSuddenDeath();
                        }
                    }
                } catch (Exception e) {
                    Obsidianwars.getInstance().getLogger().severe("Error in sudden death countdown for arena " + arenaName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // Start the task
        BukkitTask task = suddenDeathTask.runTaskTimer(Obsidianwars.getInstance(), 0L, 20L);
        suddenDeathTasks.put(arenaName, task);
        Obsidianwars.getInstance().getLogger().info("Sudden death countdown task started for arena " + arenaName);
    }

    public static void stopSuddenDeathCountdown(String arenaName) {
        BukkitTask task = suddenDeathTasks.remove(arenaName);
        if (task != null) {
            task.cancel();
        }
        suddenDeathRemainingTime.remove(arenaName);
    }

    public static void stopPreparationTimer(String arenaName) {
        BukkitTask task = wallTimerTasks.remove(arenaName);
        if (task != null) {
            task.cancel();
        }
        currentRemainingTime.remove(arenaName);
    }

    public static boolean hasWallConfiguration(String arenaName, String team) {
        return ArenaConfigManager.hasWallConfiguration(arenaName, team);
    }

    public static void cleanup() {
        // Stop all wall timer tasks
        for (BukkitTask task : wallTimerTasks.values()) {
            task.cancel();
        }
        wallTimerTasks.clear();
        
        // Stop all sudden death tasks
        for (BukkitTask task : suddenDeathTasks.values()) {
            task.cancel();
        }
        suddenDeathTasks.clear();
        
        wallDurations.clear();
        currentRemainingTime.clear();
        suddenDeathRemainingTime.clear();
    }
}