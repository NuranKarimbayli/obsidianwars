package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.models.ArenaData;
import az.nuran.obsidianwars.models.CuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Arena Manager for ObsidianWars.
 * Handles multi-arena per world support with strict region isolation.
 */
public class ArenaManager {

    private static ArenaManager instance;
    private final Logger logger;

    // Map of arena names to their arena data
    private final Map<String, ArenaData> arenas;

    // Map of worlds to their contained arenas (for fast world-based queries)
    private final Map<String, Set<String>> worldArenas;

    public ArenaManager(Logger logger) {
        this.logger = logger;
        this.arenas = new ConcurrentHashMap<>();
        this.worldArenas = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the ArenaManager singleton.
     *
     * @param logger The plugin logger
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new ArenaManager(logger);
        }
    }

    /**
     * Gets the ArenaManager instance.
     *
     * @return The ArenaManager instance
     */
    public static ArenaManager getInstance() {
        return instance;
    }

    /**
     * Registers an arena with its region boundaries.
     *
     * @param arenaName The arena name
     * @param world The world
     * @param region The arena region
     * @return true if registration was successful
     */
    public boolean registerArena(String arenaName, World world, CuboidRegion region) {
        if (arenas.containsKey(arenaName)) {
            logger.warning("Arena " + arenaName + " is already registered");
            return false;
        }

        // Check for region overlap with existing arenas in the same world
        if (hasRegionOverlap(world, region)) {
            logger.warning("Arena " + arenaName + " region overlaps with existing arena in world " + world.getName());
            return false;
        }

        ArenaData arenaData = new ArenaData(arenaName, world, region);
        arenas.put(arenaName, arenaData);
        worldArenas.computeIfAbsent(world.getName(), k -> ConcurrentHashMap.newKeySet()).add(arenaName);

        logger.info("Arena " + arenaName + " registered in world " + world.getName());
        return true;
    }

    /**
     * Unregisters an arena.
     *
     * @param arenaName The arena name
     */
    public void unregisterArena(String arenaName) {
        ArenaData arenaData = arenas.remove(arenaName);
        if (arenaData != null) {
            Set<String> worldSet = worldArenas.get(arenaData.world.getName());
            if (worldSet != null) {
                worldSet.remove(arenaName);
                if (worldSet.isEmpty()) {
                    worldArenas.remove(arenaData.world.getName());
                }
            }
            logger.info("Arena " + arenaName + " unregistered");
        }
    }

    /**
     * Checks if a location is within any arena region.
     *
     * @param location The location to check
     * @return The arena name if within an arena, null otherwise
     */
    public String getArenaAtLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        String worldName = location.getWorld().getName();
        Set<String> arenasInWorld = worldArenas.get(worldName);

        if (arenasInWorld == null || arenasInWorld.isEmpty()) {
            return null;
        }

        for (String arenaName : arenasInWorld) {
            ArenaData arenaData = arenas.get(arenaName);
            if (arenaData != null && arenaData.region.contains(location)) {
                return arenaName;
            }
        }

        return null;
    }

    /**
     * Checks if a location is within a specific arena region.
     *
     * @param location The location to check
     * @param arenaName The arena name
     * @return true if the location is within the arena region
     */
    public boolean isInArena(Location location, String arenaName) {
        ArenaData arenaData = arenas.get(arenaName);
        return arenaData != null && arenaData.region.contains(location);
    }

    /**
     * Checks if a player is within any arena region.
     *
     * @param player The player
     * @return The arena name if in an arena, null otherwise
     */
    public String getArenaForPlayer(Player player) {
        return getArenaAtLocation(player.getLocation());
    }

    /**
     * Checks if a player is within a specific arena region.
     *
     * @param player The player
     * @param arenaName The arena name
     * @return true if the player is within the arena region
     */
    public boolean isPlayerInArena(Player player, String arenaName) {
        return isInArena(player.getLocation(), arenaName);
    }

    /**
     * Checks for region overlap with existing arenas.
     *
     * @param world The world
     * @param region The region to check
     * @return true if there is an overlap
     */
    private boolean hasRegionOverlap(World world, CuboidRegion region) {
        Set<String> arenasInWorld = worldArenas.get(world.getName());
        if (arenasInWorld == null || arenasInWorld.isEmpty()) {
            return false;
        }

        for (String arenaName : arenasInWorld) {
            ArenaData arenaData = arenas.get(arenaName);
            if (arenaData != null && arenaData.region.overlaps(region)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets all arenas in a specific world.
     *
     * @param worldName The world name
     * @return Set of arena names in the world
     */
    public Set<String> getArenasInWorld(String worldName) {
        Set<String> worldSet = worldArenas.get(worldName);
        return worldSet != null ? new HashSet<>(worldSet) : Collections.emptySet();
    }

    /**
     * Gets the region for an arena.
     *
     * @param arenaName The arena name
     * @return The arena region, or null if not found
     */
    public CuboidRegion getArenaRegion(String arenaName) {
        ArenaData arenaData = arenas.get(arenaName);
        return arenaData != null ? arenaData.region : null;
    }

    /**
     * Gets the world for an arena.
     *
     * @param arenaName The arena name
     * @return The arena world, or null if not found
     */
    public World getArenaWorld(String arenaName) {
        ArenaData arenaData = arenas.get(arenaName);
        return arenaData != null ? arenaData.world : null;
    }

    /**
     * Gets all registered arena names.
     *
     * @return Set of arena names
     */
    public Set<String> getAllArenas() {
        return new HashSet<>(arenas.keySet());
    }

    /**
     * Checks if an arena is registered.
     *
     * @param arenaName The arena name
     * @return true if the arena is registered
     */
    public boolean isArenaRegistered(String arenaName) {
        return arenas.containsKey(arenaName);
    }

    /**
     * Cleans up the ArenaManager (called on plugin disable).
     */
    public void cleanup() {
        arenas.clear();
        worldArenas.clear();
        instance = null;
    }


}
