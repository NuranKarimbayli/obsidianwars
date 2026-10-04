package az.nuran.obsidianwars;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Legacy compatibility class that delegates to ArenaFileManager.
 * This class maintains backward compatibility with existing code.
 */
public class ArenaConfigManager {

    private static final Obsidianwars plugin = Obsidianwars.getInstance();

    public static void initialize() {
        ArenaFileManager.initialize();
    }

    public static FileConfiguration getArenaConfig(String arenaName) {
        return ArenaFileManager.getArenaConfig(arenaName);
    }

    public static void createArenaConfig(String arenaName) {
        ArenaFileManager.createArenaFolder(arenaName);
    }

    public static void createArenaConfigWithParams(String arenaName, int minPlayers, int maxPlayers) {
        ArenaFileManager.createArenaWithParams(arenaName, minPlayers, maxPlayers);
    }

    public static void deleteArenaConfig(String arenaName) {
        ArenaFileManager.deleteArenaFolder(arenaName);
    }

    public static List<String> getArenaNames() {
        return ArenaFileManager.getArenaNames();
    }

    public static boolean arenaExists(String arenaName) {
        return ArenaFileManager.arenaExists(arenaName);
    }

    public static void setArenaPosition(String arenaName, String positionType, Location pos1, Location pos2) {
        ArenaFileManager.setArenaPosition(arenaName, positionType, pos1, pos2);
    }

    public static Location[] getArenaRegion(String arenaName, String positionType) {
        return ArenaFileManager.getArenaRegion(arenaName, positionType);
    }

    public static void setLobbySpawn(String arenaName, Location location) {
        ArenaFileManager.setLobbySpawn(arenaName, location);
    }

    public static Location getLobbySpawn(String arenaName) {
        return ArenaFileManager.getLobbySpawn(arenaName);
    }

    public static void setTeamSpawn(String arenaName, String team, Location location) {
        ArenaFileManager.setTeamSpawn(arenaName, team, location);
    }

    public static Location getTeamSpawn(String arenaName, String team) {
        return ArenaFileManager.getTeamSpawn(arenaName, team);
    }

    public static void setObsidianLocation(String arenaName, String team, Location location) {
        ArenaFileManager.setObsidianLocation(arenaName, team, location);
    }

    public static Location getObsidianLocation(String arenaName, String team) {
        return ArenaFileManager.getObsidianLocation(arenaName, team);
    }

    public static void setPlayerLimits(String arenaName, int minPlayers, int maxPlayers) {
        ArenaFileManager.setPlayerLimits(arenaName, minPlayers, maxPlayers);
    }

    public static int getMinPlayers(String arenaName) {
        return ArenaFileManager.getMinPlayers(arenaName);
    }

    public static int getMaxPlayers(String arenaName) {
        return ArenaFileManager.getMaxPlayers(arenaName);
    }

    public static void setArenaStatus(String arenaName, String status) {
        ArenaFileManager.setArenaStatus(arenaName, status);
    }

    public static String getArenaStatus(String arenaName) {
        return ArenaFileManager.getArenaStatus(arenaName);
    }

    public static void setWallRegion(String arenaName, String team, Location pos1, Location pos2) {
        ArenaFileManager.setWallRegion(arenaName, team, pos1, pos2);
    }

    public static Location[] getWallRegion(String arenaName, String team) {
        return ArenaFileManager.getWallRegion(arenaName, team);
    }

    public static void setPreparationTimer(String arenaName, int minutes) {
        ArenaFileManager.setPreparationTimer(arenaName, minutes);
    }

    public static int getPreparationTimer(String arenaName) {
        return ArenaFileManager.getPreparationTimer(arenaName);
    }

    public static boolean hasWallConfiguration(String arenaName, String team) {
        return ArenaFileManager.hasWallConfiguration(arenaName, team);
    }

    public static boolean hasResourceBlocks(String arenaName) {
        return ArenaFileManager.hasResourceBlocks(arenaName);
    }

    public static int getResourceBlockCount(String arenaName) {
        return ArenaFileManager.getResourceBlockCount(arenaName);
    }

    /**
     * @deprecated This method is deprecated as resource blocks are now managed in separate resource_blocks.yml files.
     * Use ArenaFileManager methods instead.
     */
    @Deprecated
    public static void saveArenaConfigDirectly(String arenaName) {
        // No-op - resource blocks are now managed separately
        plugin.getLogger().warning("saveArenaConfigDirectly is deprecated - resource blocks are now in resource_blocks.yml");
    }

    public static void setMobAreaLocation(String arenaName, Location location) {
        ArenaFileManager.setMobAreaLocation(arenaName, location);
    }

    public static Location getMobAreaLocation(String arenaName) {
        return ArenaFileManager.getMobAreaLocation(arenaName);
    }

    public static void setTimeLimit(String arenaName, int minutes) {
        ArenaFileManager.setTimeLimit(arenaName, minutes);
    }

    public static int getTimeLimit(String arenaName) {
        return ArenaFileManager.getTimeLimit(arenaName);
    }

    public static void setSuddenDeathTimer(String arenaName, int minutes) {
        ArenaFileManager.setSuddenDeathTimer(arenaName, minutes);
    }

    public static int getSuddenDeathTimer(String arenaName) {
        return ArenaFileManager.getSuddenDeathTimer(arenaName);
    }

    public static void setWaitingSpawn(String arenaName, Location location) {
        ArenaFileManager.setWaitingSpawn(arenaName, location);
    }

    public static Location getWaitingSpawn(String arenaName) {
        return ArenaFileManager.getWaitingSpawn(arenaName);
    }

    public static void setSpectatorSpawn(String arenaName, Location location) {
        ArenaFileManager.setSpectatorSpawn(arenaName, location);
    }

    public static Location getSpectatorSpawn(String arenaName) {
        return ArenaFileManager.getSpectatorSpawn(arenaName);
    }

    public static void setWaitingRegion(String arenaName, Location pos1, Location pos2) {
        ArenaFileManager.setWaitingRegion(arenaName, pos1, pos2);
    }

    public static Location[] getWaitingRegion(String arenaName) {
        return ArenaFileManager.getWaitingRegion(arenaName);
    }
}