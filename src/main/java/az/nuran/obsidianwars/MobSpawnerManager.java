package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MobSpawnerManager {

    // NOTE: Tasks are now managed by TaskManager for consistency
    private static final int SPAWN_INTERVAL_SECONDS = 120; // 2 minutes
    private static final int SPAWN_RADIUS = 10; // 10 blocks
    private static final Random random = new Random();

    public static void setMobArea(String arenaName, Location location) {
        ArenaFileManager.setMobAreaLocation(arenaName, location);
    }

    public static Location getMobArea(String arenaName) {
        return ArenaFileManager.getMobAreaLocation(arenaName);
    }

    public static void startMobSpawning(String arenaName) {
        // Stop existing task
        stopMobSpawning(arenaName);

        Location mobArea = getMobArea(arenaName);
        if (mobArea == null) {
            return; // No mob area configured
        }

        // Start spawning task every 2 minutes using TaskManager
        TaskManager.getInstance().runTimer(
            "mob-spawner-" + arenaName,
            () -> spawnWitherSkeleton(arenaName, mobArea),
            SPAWN_INTERVAL_SECONDS * 20L, // Initial delay
            SPAWN_INTERVAL_SECONDS * 20L, // Period
            arenaName
        );
    }

    public static void stopMobSpawning(String arenaName) {
        // Cancel task via TaskManager
        TaskManager.getInstance().cancelTask("mob-spawner-" + arenaName);
    }

    private static void spawnWitherSkeleton(String arenaName, Location centerLocation) {
        World world = centerLocation.getWorld();
        if (world == null) return;

        // Try to find a valid spawn location within 10 blocks
        Location spawnLocation = findValidSpawnLocation(centerLocation);
        if (spawnLocation == null) {
            Obsidianwars.getInstance().getLogger().warning("Could not find valid spawn location for Wither Skeleton in arena " + arenaName);
            return;
        }

        // Spawn the Wither Skeleton
        WitherSkeleton skeleton = (WitherSkeleton) world.spawnEntity(spawnLocation, EntityType.WITHER_SKELETON);
        
        // Give the skeleton a stone sword
        skeleton.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.STONE_SWORD));
        
        // Broadcast message to arena players
        String mobSpawnMsg = MessagesConfigManager.getMessage("mob_spawned");
        ObsidianCommand.broadcastToArena(arenaName, mobSpawnMsg);
    }

    private static Location findValidSpawnLocation(Location center) {
        World world = center.getWorld();
        if (world == null) return null;

        // Try random locations within the radius
        for (int attempt = 0; attempt < 20; attempt++) {
            int xOffset = random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
            int zOffset = random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
            
            Location testLoc = new Location(world, 
                center.getBlockX() + xOffset, 
                center.getBlockY(), 
                center.getBlockZ() + zOffset);
            
            // Find the highest valid ground block
            Location groundLoc = findGroundLocation(testLoc);
            if (groundLoc != null) {
                return groundLoc.add(0, 1, 0); // Spawn above the ground
            }
        }
        
        return null;
    }

    private static Location findGroundLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        // Search downward from the location to find a solid block
        for (int y = loc.getBlockY(); y >= loc.getBlockY() - 5; y--) {
            Location testLoc = new Location(world, loc.getBlockX(), y, loc.getBlockZ());
            Material blockType = testLoc.getBlock().getType();
            
            // Check if this is a solid block (ground)
            if (blockType.isSolid() && !blockType.isAir()) {
                // Check if the block above is air (valid spawn space)
                Location aboveLoc = testLoc.clone().add(0, 1, 0);
                if (aboveLoc.getBlock().getType().isAir()) {
                    Location aboveAboveLoc = testLoc.clone().add(0, 2, 0);
                    if (aboveAboveLoc.getBlock().getType().isAir()) {
                        return testLoc;
                    }
                }
            }
        }
        
        return null;
    }

    public static void cleanup() {
        // Cleanup is handled by TaskManager automatically
        // No manual cleanup needed
    }
}