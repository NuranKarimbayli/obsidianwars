package az.nuran.obsidianwars;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleManager {

    // NOTE: Arena tasks are now managed by TaskManager for consistency
    // Spawn protection tasks remain local as they are per-player
    private static final Map<UUID, BukkitTask> spawnProtectionTasks = new HashMap<>();
    private static final Map<String, Map<String, Location>> obsidianLocations = new HashMap<>(); // Track obsidian locations per arena

    // Obsidian target particle settings
    private static String obsidianParticleType = "dust_color_transition";
    private static double obsidianFromR = 0.7, obsidianFromG = 0.0, obsidianFromB = 1.0;
    private static double obsidianToR = 1.0, obsidianToG = 0.0, obsidianToB = 0.2;
    private static double obsidianScale = 2.0;

    // Resource break particle settings
    private static String resourceParticleType = "dust_color_transition";
    private static double resourceFromR = 0.9, resourceFromG = 0.8, resourceFromB = 0.7;
    private static double resourceToR = 1.0, resourceToG = 1.0, resourceToB = 1.0;
    private static double resourceScale = 1.0;

    public static void loadConfig() {
        // Load from config.yml
        if (Obsidianwars.getInstance().getConfig() == null) return;

        // Load obsidian particle settings
        obsidianParticleType = Obsidianwars.getInstance().getConfig().getString("particles.obsidian_target.type", "dust_color_transition");
        obsidianFromR = Obsidianwars.getInstance().getConfig().getDouble("particles.obsidian_target.from_color.0", 0.7);
        obsidianFromG = Obsidianwars.getInstance().getConfig().getDouble("particles.obsidian_target.from_color.1", 0.0);
        obsidianFromB = Obsidianwars.getInstance().getConfig().getDouble("particles.obsidian_target.from_color.2", 1.0);
        obsidianToR = Obsidianwars.getInstance().getConfig().getDouble("particles.obsidian_target.to_color.0", 1.0);
        obsidianToG = Obsidianwars.getInstance().getConfig().getDouble("particles.obsidian_target.to_color.1", 0.0);
        obsidianToB = Obsidianwars.getInstance().getConfig().getDouble("particles.obsidian_target.to_color.2", 0.2);
        obsidianScale = Obsidianwars.getInstance().getConfig().getDouble("particles.obsidian_target.scale", 2.0);

        // Load resource particle settings
        resourceParticleType = Obsidianwars.getInstance().getConfig().getString("particles.resource_break.type", "dust_color_transition");
        resourceFromR = Obsidianwars.getInstance().getConfig().getDouble("particles.resource_break.from_color.0", 0.9);
        resourceFromG = Obsidianwars.getInstance().getConfig().getDouble("particles.resource_break.from_color.1", 0.8);
        resourceFromB = Obsidianwars.getInstance().getConfig().getDouble("particles.resource_break.from_color.2", 0.7);
        resourceToR = Obsidianwars.getInstance().getConfig().getDouble("particles.resource_break.to_color.0", 1.0);
        resourceToG = Obsidianwars.getInstance().getConfig().getDouble("particles.resource_break.to_color.1", 1.0);
        resourceToB = Obsidianwars.getInstance().getConfig().getDouble("particles.resource_break.to_color.2", 1.0);
        resourceScale = Obsidianwars.getInstance().getConfig().getDouble("particles.resource_break.scale", 1.0);
    }

    public static void startObsidianParticles(String arenaName) {
        // Stop existing particles for this arena
        stopObsidianParticles(arenaName);

        // Get obsidian locations from new arena config system
        Location redObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "red");
        Location blueObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "blue");

        if (redObsidian == null && blueObsidian == null) return;

        // Store obsidian locations for this arena
        Map<String, Location> arenaObsidians = new HashMap<>();
        if (redObsidian != null) arenaObsidians.put("red", redObsidian);
        if (blueObsidian != null) arenaObsidians.put("blue", blueObsidian);
        obsidianLocations.put(arenaName, arenaObsidians);

        // Start particle task using TaskManager
        TaskManager.getInstance().runTimer(
            "obsidian-particles-" + arenaName,
            () -> {
                Map<String, Location> currentObsidians = obsidianLocations.get(arenaName);
                if (currentObsidians != null) {
                    Location currentRed = currentObsidians.get("red");
                    Location currentBlue = currentObsidians.get("blue");

                    if (currentRed != null) {
                        spawnObsidianParticle(currentRed);
                    }
                    if (currentBlue != null) {
                        spawnObsidianParticle(currentBlue);
                    }
                }
            },
            0L,  // Start immediately
            10L, // Every 0.5 seconds (10 ticks)
            arenaName
        );
    }

    public static void stopObsidianParticles(String arenaName) {
        // Cancel task via TaskManager
        TaskManager.getInstance().cancelTask("obsidian-particles-" + arenaName);
        obsidianLocations.remove(arenaName);
    }

    public static void stopObsidianParticles(String arenaName, String team) {
        // Stop particles for a specific team's obsidian
        Map<String, Location> arenaObsidians = obsidianLocations.get(arenaName);
        if (arenaObsidians != null) {
            arenaObsidians.remove(team);
        }
    }

    private static void spawnObsidianParticle(Location location) {
        // Create dust color transition particle
        Particle.DustTransition dustTransition = new Particle.DustTransition(
            Color.fromRGB((int)(obsidianFromR * 255), (int)(obsidianFromG * 255), (int)(obsidianFromB * 255)),
            Color.fromRGB((int)(obsidianToR * 255), (int)(obsidianToG * 255), (int)(obsidianToB * 255)),
            (float) obsidianScale
        );

        // Spawn particles around the full 1x1x1 bounding box of the block
        World world = location.getWorld();
        if (world != null) {
            for (int i = 0; i < 8; i++) {
                // Generate random offsets that cover the full block (0 to 1 in each dimension)
                double offsetX = Math.random();
                double offsetY = Math.random();
                double offsetZ = Math.random();

                world.spawnParticle(
                    Particle.DUST_COLOR_TRANSITION,
                    location.clone().add(offsetX, offsetY, offsetZ),
                    1,
                    dustTransition
                );
            }
        }
    }

    public static void spawnResourceBreakParticle(Location location) {
        // Create dust color transition particle for resource break
        Particle.DustTransition dustTransition = new Particle.DustTransition(
            Color.fromRGB((int)(resourceFromR * 255), (int)(resourceFromG * 255), (int)(resourceFromB * 255)),
            Color.fromRGB((int)(resourceToR * 255), (int)(resourceToG * 255), (int)(resourceToB * 255)),
            (float) resourceScale
        );

        World world = location.getWorld();
        if (world != null) {
            // Spawn burst of particles
            for (int i = 0; i < 15; i++) {
                double offsetX = (Math.random() - 0.5) * 2.0;
                double offsetY = (Math.random() - 0.5) * 2.0;
                double offsetZ = (Math.random() - 0.5) * 2.0;
                
                world.spawnParticle(
                    Particle.DUST_COLOR_TRANSITION,
                    location.clone().add(offsetX, offsetY, offsetZ),
                    1,
                    dustTransition
                );
            }
        }
    }

    public static void spawnResourceBreakParticle(Location location, Material material) {
        // Get color-matched particle for the specific resource type
        Color particleColor = getResourceParticleColor(material);
        
        // Create dust particle with the matching color
        Particle.DustOptions dustOptions = new Particle.DustOptions(particleColor, 1.5f);

        World world = location.getWorld();
        if (world != null) {
            // Spawn burst of color-matched particles
            for (int i = 0; i < 20; i++) {
                double offsetX = (Math.random() - 0.5) * 2.5;
                double offsetY = (Math.random() - 0.5) * 2.5;
                double offsetZ = (Math.random() - 0.5) * 2.5;
                
                world.spawnParticle(
                    Particle.DUST,
                    location.clone().add(offsetX, offsetY, offsetZ),
                    1,
                    dustOptions
                );
            }
        }
    }

    private static Color getResourceParticleColor(Material material) {
        // Return color-matched particles for each resource type
        switch (material) {
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
                return Color.fromRGB(139, 69, 19); // Brown/Wood
            case STONE:
            case COBBLESTONE:
                return Color.fromRGB(128, 128, 128); // Gray
            case COAL_ORE:
                return Color.fromRGB(64, 64, 64); // Dark gray
            case IRON_ORE:
                return Color.fromRGB(192, 192, 192); // Silver/Light gray
            case GOLD_ORE:
                return Color.fromRGB(255, 215, 0); // Gold/Yellow
            case DIAMOND_ORE:
                return Color.fromRGB(0, 204, 255); // Light blue
            case LAPIS_ORE:
                return Color.fromRGB(0, 51, 204); // Dark blue
            case EMERALD_ORE:
                return Color.fromRGB(0, 255, 51); // Green
            default:
                return Color.fromRGB(230, 230, 230); // Default white/light gray
        }
    }

    private static Location getObsidianLocation(String arenaName, String team) {
        return ArenaConfigManager.getObsidianLocation(arenaName, team);
    }

    public static void giveSpawnProtection(Player player, int seconds) {
        UUID uuid = player.getUniqueId();
        
        // Cancel existing protection task
        BukkitTask existingTask = spawnProtectionTasks.remove(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Add glowing effect
        player.setGlowing(true);

        // Send initial message
        String message = MessagesConfigManager.getMessage("spawn_protection", "seconds", String.valueOf(seconds));
        player.sendMessage(message);

        // Start protection task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), new Runnable() {
            int remaining = seconds;

            @Override
            public void run() {
                remaining--;
                
                if (remaining > 0) {
                    // Show action bar with remaining time
                    String actionBar = MessagesConfigManager.getMessage("spawn_protection", "seconds", String.valueOf(remaining));
                    player.sendActionBar(actionBar);
                } else {
                    // Protection expired
                    String expiredMsg = MessagesConfigManager.getMessage("protection_expired");
                    player.sendMessage(expiredMsg);
                    
                    // Remove glowing effect
                    player.setGlowing(false);
                    
                    // Remove from map and cancel - the task will handle itself
                    BukkitTask currentTask = spawnProtectionTasks.remove(uuid);
                    if (currentTask != null) {
                        currentTask.cancel();
                    }
                }
            }
        }, 20L, 20L); // Every second (20 ticks)

        spawnProtectionTasks.put(uuid, task);
    }

    public static void removeSpawnProtection(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = spawnProtectionTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        // Remove glowing effect
        player.setGlowing(false);
    }

    public static boolean hasSpawnProtection(Player player) {
        return spawnProtectionTasks.containsKey(player.getUniqueId());
    }

    public static void cleanup() {
        // Arena tasks are now managed by TaskManager - no manual cleanup needed
        obsidianLocations.clear();

        // Stop all spawn protection tasks (still managed locally)
        for (BukkitTask task : spawnProtectionTasks.values()) {
            task.cancel();
        }
        spawnProtectionTasks.clear();
    }

    public static void spawnVictoryFireworks(String arenaName, String winningTeam) {
        // Start continuous fireworks for winning team using TaskManager
        TaskManager.getInstance().runTimer(
            "victory-fireworks-" + arenaName,
            () -> {
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    String playerArena = ObsidianCommand.playersInArena.get(uuid);
                    if (playerArena != null && playerArena.equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            String playerTeam = TeamListener.playerTeams.get(uuid);
                            if (playerTeam != null && playerTeam.equals(winningTeam)) {
                                spawnFireworkAroundPlayer(player);
                            }
                        }
                    }
                }
            },
            0L,  // Start immediately
            20L, // Every second (20 ticks)
            arenaName
        );
    }

    public static void stopVictoryFireworks(String arenaName) {
        // Cancel task via TaskManager
        TaskManager.getInstance().cancelTask("victory-fireworks-" + arenaName);
    }

    public static void stopAllArenaTasks(String arenaName) {
        // Stop obsidian particles
        stopObsidianParticles(arenaName);

        // Stop victory fireworks
        stopVictoryFireworks(arenaName);
    }

    private static void spawnFireworkAroundPlayer(Player player) {
        // Create colorful firework around the player
        org.bukkit.entity.Firework firework = player.getWorld().spawn(
            player.getLocation().add(
                (Math.random() - 0.5) * 5,
                1 + Math.random() * 2,
                (Math.random() - 0.5) * 5
            ),
            org.bukkit.entity.Firework.class
        );

        // Set firework effects with random colors
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
            .withColor(
                org.bukkit.Color.fromRGB((int)(Math.random() * 255), (int)(Math.random() * 255), (int)(Math.random() * 255)),
                org.bukkit.Color.fromRGB((int)(Math.random() * 255), (int)(Math.random() * 255), (int)(Math.random() * 255))
            )
            .withFade(
                org.bukkit.Color.fromRGB((int)(Math.random() * 255), (int)(Math.random() * 255), (int)(Math.random() * 255))
            )
            .with(org.bukkit.FireworkEffect.Type.BALL)
            .trail(true)
            .flicker(true)
            .build();

        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);

        // Detonate after 1 second
        Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), firework::detonate, 20L);
    }
}