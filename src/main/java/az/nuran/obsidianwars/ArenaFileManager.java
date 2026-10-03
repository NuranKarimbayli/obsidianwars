package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ArenaFileManager {

    private static final String ARENAS_FOLDER = "arenas";
    private static final String CONFIG_FILE = "config.yml";
    private static final String RESOURCE_BLOCKS_FILE = "resource_blocks.yml";
    private static File arenasFolder;
    private static final Obsidianwars plugin = Obsidianwars.getInstance();
    private static final Object resourceBlockLock = new Object();

    public static void initialize() {
        // Create arenas folder if it doesn't exist
        arenasFolder = new File(plugin.getDataFolder(), ARENAS_FOLDER);
        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
            plugin.getLogger().info("Arenas folder created: " + arenasFolder.getPath());
        }
    }

    public static File getArenaFolder(String arenaName) {
        return new File(arenasFolder, arenaName);
    }

    public static File getArenaConfigFile(String arenaName) {
        return new File(getArenaFolder(arenaName), CONFIG_FILE);
    }

    public static File getResourceBlocksFile(String arenaName) {
        return new File(getArenaFolder(arenaName), RESOURCE_BLOCKS_FILE);
    }

    public static FileConfiguration getArenaConfig(String arenaName) {
        File arenaConfigFile = getArenaConfigFile(arenaName);
        if (!arenaConfigFile.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(arenaConfigFile);
    }

    public static FileConfiguration getResourceBlocksConfig(String arenaName) {
        File resourceBlocksFile = getResourceBlocksFile(arenaName);
        if (!resourceBlocksFile.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(resourceBlocksFile);
    }

    public static void createArenaFolder(String arenaName) {
        File arenaFolder = getArenaFolder(arenaName);
        if (arenaFolder.exists()) {
            return;
        }

        arenaFolder.mkdirs();
        plugin.getLogger().info("Arena folder created: " + arenaFolder.getPath());

        // Create config.yml
        createArenaConfigFile(arenaName);
        
        // Create empty resource_blocks.yml
        createResourceBlocksFile(arenaName);
    }

    private static void createArenaConfigFile(String arenaName) {
        File arenaConfigFile = getArenaConfigFile(arenaName);
        if (arenaConfigFile.exists()) {
            return;
        }

        try {
            arenaConfigFile.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(arenaConfigFile);
            
            // Set default arena structure
            config.set("arena.name", arenaName);
            config.set("arena.status", "SETUP");
            config.set("arena.world", "world");
            config.set("arena.min-players", 2);
            config.set("arena.max-players", 8);
            
            config.save(arenaConfigFile);
            plugin.getLogger().info("Created arena config: " + arenaName);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create arena config for " + arenaName, e);
        }
    }

    private static void createResourceBlocksFile(String arenaName) {
        File resourceBlocksFile = getResourceBlocksFile(arenaName);
        if (resourceBlocksFile.exists()) {
            return;
        }

        try {
            resourceBlocksFile.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(resourceBlocksFile);
            
            // Initialize empty resource blocks structure
            config.set("resource-blocks", new ArrayList<>());
            
            config.save(resourceBlocksFile);
            plugin.getLogger().info("Created resource blocks file: " + arenaName);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create resource blocks file for " + arenaName, e);
        }
    }

    public static void deleteArenaFolder(String arenaName) {
        File arenaFolder = getArenaFolder(arenaName);
        if (arenaFolder.exists()) {
            deleteDirectory(arenaFolder);
            plugin.getLogger().info("Deleted arena folder: " + arenaName);
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    public static List<String> getArenaNames() {
        List<String> arenaNames = new ArrayList<>();
        File[] files = arenasFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    arenaNames.add(file.getName());
                }
            }
        }
        return arenaNames;
    }

    public static boolean arenaExists(String arenaName) {
        return getArenaFolder(arenaName).exists();
    }

    private static void saveArenaConfig(String arenaName, FileConfiguration config) {
        Bukkit.getScheduler().runTaskAsynchronously(Obsidianwars.getInstance(), () -> {
            try {
                config.save(getArenaConfigFile(arenaName));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save arena config for " + arenaName, e);
            }
        });
    }

    private static void saveResourceBlocksConfig(String arenaName, FileConfiguration config) {
        Bukkit.getScheduler().runTaskAsynchronously(Obsidianwars.getInstance(), () -> {
            try {
                config.save(getResourceBlocksFile(arenaName));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save resource blocks config for " + arenaName, e);
            }
        });
    }

    public static void setArenaPosition(String arenaName, String positionType, Location pos1, Location pos2) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) {
            createArenaFolder(arenaName);
            config = getArenaConfig(arenaName);
        }

        String path = "arena.region." + positionType + ".";
        config.set(path + "world", pos1.getWorld().getName());
        config.set(path + "pos1.x", pos1.getBlockX());
        config.set(path + "pos1.y", pos1.getBlockY());
        config.set(path + "pos1.z", pos1.getBlockZ());
        config.set(path + "pos2.x", pos2.getBlockX());
        config.set(path + "pos2.y", pos2.getBlockY());
        config.set(path + "pos2.z", pos2.getBlockZ());
        
        saveArenaConfig(arenaName, config);
    }

    public static Location[] getArenaRegion(String arenaName, String positionType) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return null;

        String path = "arena.region." + positionType + ".";
        String worldName = config.getString(path + "world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for arena " + arenaName + " " + positionType + " region. Check if world is loaded.");
            return null;
        }

        int x1 = config.getInt(path + "pos1.x");
        int y1 = config.getInt(path + "pos1.y");
        int z1 = config.getInt(path + "pos1.z");
        int x2 = config.getInt(path + "pos2.x");
        int y2 = config.getInt(path + "pos2.y");
        int z2 = config.getInt(path + "pos2.z");

        return new Location[] {
            new Location(world, x1, y1, z1),
            new Location(world, x2, y2, z2)
        };
    }

    public static void setLobbySpawn(String arenaName, Location location) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        String path = "arena.lobby.";
        config.set(path + "world", location.getWorld().getName());
        config.set(path + "x", location.getX());
        config.set(path + "y", location.getY());
        config.set(path + "z", location.getZ());
        config.set(path + "yaw", location.getYaw());
        config.set(path + "pitch", location.getPitch());
        
        saveArenaConfig(arenaName, config);
    }

    public static Location getLobbySpawn(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return null;

        String path = "arena.lobby.";
        String worldName = config.getString(path + "world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for arena " + arenaName + " lobby spawn. Check if world is loaded.");
            return null;
        }

        double x = config.getDouble(path + "x");
        double y = config.getDouble(path + "y");
        double z = config.getDouble(path + "z");
        float yaw = (float) config.getDouble(path + "yaw");
        float pitch = (float) config.getDouble(path + "pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static void setTeamSpawn(String arenaName, String team, Location location) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        String path = "arena.spawns." + team + ".";
        config.set(path + "world", location.getWorld().getName());
        config.set(path + "x", location.getX());
        config.set(path + "y", location.getY());
        config.set(path + "z", location.getZ());
        config.set(path + "yaw", location.getYaw());
        config.set(path + "pitch", location.getPitch());
        
        saveArenaConfig(arenaName, config);
    }

    public static Location getTeamSpawn(String arenaName, String team) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return null;

        String path = "arena.spawns." + team + ".";
        String worldName = config.getString(path + "world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for arena " + arenaName + " " + team + " team spawn. Check if world is loaded.");
            return null;
        }

        double x = config.getDouble(path + "x");
        double y = config.getDouble(path + "y");
        double z = config.getDouble(path + "z");
        float yaw = (float) config.getDouble(path + "yaw");
        float pitch = (float) config.getDouble(path + "pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static void setObsidianLocation(String arenaName, String team, Location location) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        String path = "arena.obsidian." + team + ".";
        config.set(path + "world", location.getWorld().getName());
        config.set(path + "x", location.getBlockX());
        config.set(path + "y", location.getBlockY());
        config.set(path + "z", location.getBlockZ());
        
        saveArenaConfig(arenaName, config);
    }

    public static Location getObsidianLocation(String arenaName, String team) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return null;

        String path = "arena.obsidian." + team + ".";
        String worldName = config.getString(path + "world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for arena " + arenaName + " " + team + " obsidian. Check if world is loaded.");
            return null;
        }

        int x = config.getInt(path + "x");
        int y = config.getInt(path + "y");
        int z = config.getInt(path + "z");

        return new Location(world, x, y, z);
    }

    public static void setPlayerLimits(String arenaName, int minPlayers, int maxPlayers) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        config.set("arena.min-players", minPlayers);
        config.set("arena.max-players", maxPlayers);
        
        saveArenaConfig(arenaName, config);
    }

    public static int getMinPlayers(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return 2;
        return config.getInt("arena.min-players", 2);
    }

    public static int getMaxPlayers(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return 8;
        return config.getInt("arena.max-players", 8);
    }

    public static void setArenaStatus(String arenaName, String status) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        config.set("arena.status", status);
        saveArenaConfig(arenaName, config);
    }

    public static String getArenaStatus(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return "SETUP";
        return config.getString("arena.status", "SETUP");
    }

    public static void setWallRegion(String arenaName, String team, Location pos1, Location pos2) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        String path = "arena.walls." + team + ".";
        config.set(path + "world", pos1.getWorld().getName());
        config.set(path + "pos1.x", pos1.getBlockX());
        config.set(path + "pos1.y", pos1.getBlockY());
        config.set(path + "pos1.z", pos1.getBlockZ());
        config.set(path + "pos2.x", pos2.getBlockX());
        config.set(path + "pos2.y", pos2.getBlockY());
        config.set(path + "pos2.z", pos2.getBlockZ());
        
        saveArenaConfig(arenaName, config);
    }

    public static Location[] getWallRegion(String arenaName, String team) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return null;

        String path = "arena.walls." + team + ".";
        String worldName = config.getString(path + "world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for arena " + arenaName + " " + team + " wall region. Check if world is loaded.");
            return null;
        }

        int x1 = config.getInt(path + "pos1.x");
        int y1 = config.getInt(path + "pos1.y");
        int z1 = config.getInt(path + "pos1.z");
        int x2 = config.getInt(path + "pos2.x");
        int y2 = config.getInt(path + "pos2.y");
        int z2 = config.getInt(path + "pos2.z");

        return new Location[] {
            new Location(world, x1, y1, z1),
            new Location(world, x2, y2, z2)
        };
    }

    public static void setPreparationTimer(String arenaName, int minutes) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        config.set("arena.preparation.duration", minutes);
        saveArenaConfig(arenaName, config);
    }

    public static int getPreparationTimer(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return 10;
        return config.getInt("arena.preparation.duration", 10);
    }

    public static boolean hasWallConfiguration(String arenaName, String team) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return false;
        return config.contains("arena.walls." + team + ".world");
    }

    public static boolean hasResourceBlocks(String arenaName) {
        FileConfiguration config = getResourceBlocksConfig(arenaName);
        if (config == null) return false;
        
        List<?> resourceBlocks = config.getList("resource-blocks");
        return resourceBlocks != null && !resourceBlocks.isEmpty();
    }

    public static int getResourceBlockCount(String arenaName) {
        FileConfiguration config = getResourceBlocksConfig(arenaName);
        if (config == null) return 0;
        
        List<?> resourceBlocks = config.getList("resource-blocks");
        return resourceBlocks != null ? resourceBlocks.size() : 0;
    }

    public static void addResourceBlock(String arenaName, String blockType, Location location) {
        synchronized (resourceBlockLock) {
            FileConfiguration config = getResourceBlocksConfig(arenaName);
            if (config == null) {
                createResourceBlocksFile(arenaName);
                config = getResourceBlocksConfig(arenaName);
            }

            List<java.util.Map<String, Object>> resourceBlocks = (List<java.util.Map<String, Object>>) config.getList("resource-blocks", new ArrayList<>());

            java.util.Map<String, Object> blockData = new java.util.HashMap<>();
            blockData.put("type", blockType);
            blockData.put("world", location.getWorld().getName());
            blockData.put("x", location.getBlockX());
            blockData.put("y", location.getBlockY());
            blockData.put("z", location.getBlockZ());

            resourceBlocks.add(blockData);
            config.set("resource-blocks", resourceBlocks);

            // IMMEDIATELY flush to disk
            saveResourceBlocksConfig(arenaName, config);
        }
    }

    public static void removeResourceBlock(String arenaName, Location location) {
        synchronized (resourceBlockLock) {
            FileConfiguration config = getResourceBlocksConfig(arenaName);
            if (config == null) return;

            List<java.util.Map<String, Object>> resourceBlocks = (List<java.util.Map<String, Object>>) config.getList("resource-blocks", new ArrayList<>());

            boolean removed = false;
            java.util.Map<String, Object> toRemove = null;

            for (java.util.Map<String, Object> blockData : resourceBlocks) {
                String worldName = (String) blockData.get("world");
                int x = ((Number) blockData.get("x")).intValue();
                int y = ((Number) blockData.get("y")).intValue();
                int z = ((Number) blockData.get("z")).intValue();

                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                Location blockLoc = new Location(world, x, y, z);

                if (blockLoc.equals(location)) {
                    toRemove = blockData;
                    removed = true;
                    break;
                }
            }

            if (removed && toRemove != null) {
                resourceBlocks.remove(toRemove);
                config.set("resource-blocks", resourceBlocks);

                // IMMEDIATELY flush to disk
                saveResourceBlocksConfig(arenaName, config);
            }
        }
    }

    public static boolean isResourceBlock(String arenaName, Location blockLoc) {
        FileConfiguration config = getResourceBlocksConfig(arenaName);
        if (config == null) return false;

        List<?> resourceBlocks = config.getList("resource-blocks");
        if (resourceBlocks == null) return false;

        for (Object blockObj : resourceBlocks) {
            if (!(blockObj instanceof java.util.Map)) continue;

            java.util.Map<?, ?> blockData = (java.util.Map<?, ?>) blockObj;
            String worldName = (String) blockData.get("world");
            int x = ((Number) blockData.get("x")).intValue();
            int y = ((Number) blockData.get("y")).intValue();
            int z = ((Number) blockData.get("z")).intValue();

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for resource block in arena " + arenaName + ". Skipping block check.");
                continue;
            }

            Location resourceLoc = new Location(world, x, y, z);

            if (resourceLoc.equals(blockLoc)) {
                return true;
            }
        }

        return false;
    }

    public static java.util.List<java.util.Map<String, Object>> getResourceBlocks(String arenaName) {
        FileConfiguration config = getResourceBlocksConfig(arenaName);
        if (config == null) return new ArrayList<>();

        List<?> resourceBlocks = config.getList("resource-blocks");
        if (resourceBlocks == null) return new ArrayList<>();
        
        java.util.List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (Object blockObj : resourceBlocks) {
            if (blockObj instanceof java.util.Map) {
                result.add((java.util.Map<String, Object>) blockObj);
            }
        }
        
        return result;
    }

    public static void setMobAreaLocation(String arenaName, Location location) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        String path = "arena.mob-area.";
        config.set(path + "world", location.getWorld().getName());
        config.set(path + "x", location.getBlockX());
        config.set(path + "y", location.getBlockY());
        config.set(path + "z", location.getBlockZ());
        
        saveArenaConfig(arenaName, config);
    }

    public static Location getMobAreaLocation(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return null;

        String path = "arena.mob-area.";
        String worldName = config.getString(path + "world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for arena " + arenaName + " mob area. Check if world is loaded.");
            return null;
        }

        int x = config.getInt(path + "x");
        int y = config.getInt(path + "y");
        int z = config.getInt(path + "z");

        return new Location(world, x, y, z);
    }

    public static void setTimeLimit(String arenaName, int minutes) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        config.set("arena.time-limit", minutes);
        saveArenaConfig(arenaName, config);
    }

    public static int getTimeLimit(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return 30; // Default 30 minutes
        return config.getInt("arena.time-limit", 30);
    }

    public static void setSuddenDeathTimer(String arenaName, int minutes) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return;

        config.set("arena.sudden-death-duration", minutes);
        saveArenaConfig(arenaName, config);
    }

    public static int getSuddenDeathTimer(String arenaName) {
        FileConfiguration config = getArenaConfig(arenaName);
        if (config == null) return 5; // Default 5 minutes
        return config.getInt("arena.sudden-death-duration", 5);
    }
}