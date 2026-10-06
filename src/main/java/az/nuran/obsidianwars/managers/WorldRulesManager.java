package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashMap;
import java.util.Map;

public class WorldRulesManager implements Listener {

    private static final Map<String, WorldState> originalWorldStates = new HashMap<>();
    private static final Map<String, Integer> worldReferenceCount = new HashMap<>();

    public static void applyGameRules(String arenaName) {
        // Get the arena world from lobby spawn or team spawn
        World world = getArenaWorld(arenaName);
        if (world == null) return;

        String worldName = world.getName();

        // Check if world state is already saved (prevent overwrites)
        if (originalWorldStates.containsKey(worldName)) {
            // Increment reference count
            worldReferenceCount.put(worldName, worldReferenceCount.get(worldName) + 1);
            Obsidianwars.getInstance().getLogger().info("World " + worldName + " already has game rules applied (reference count: " + worldReferenceCount.get(worldName) + ")");
            return;
        }

        // Save original world state
        WorldState originalState = new WorldState(
            world.getGameRuleValue("keepInventory") != null ? world.getGameRuleValue("keepInventory") : "false",
            world.getDifficulty(),
            world.isAutoSave(),
            world.getGameRuleValue("doDaylightCycle") != null ? world.getGameRuleValue("doDaylightCycle") : "true",
            world.getGameRuleValue("doWeatherCycle") != null ? world.getGameRuleValue("doWeatherCycle") : "true"
        );
        originalWorldStates.put(worldName, originalState);
        worldReferenceCount.put(worldName, 1);

        // Apply game rules
        world.setGameRuleValue("keepInventory", "true");
        world.setDifficulty(Difficulty.HARD);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");

        Obsidianwars.getInstance().getLogger().info("Applied game rules for arena " + arenaName + " in world " + worldName);
    }

    public static void restoreWorldRules(String arenaName) {
        World world = getArenaWorld(arenaName);
        if (world == null) return;

        String worldName = world.getName();

        // Decrement reference count
        Integer refCount = worldReferenceCount.get(worldName);
        if (refCount == null) return;

        if (refCount > 1) {
            // Still other arenas using this world
            worldReferenceCount.put(worldName, refCount - 1);
            Obsidianwars.getInstance().getLogger().info("World " + worldName + " still in use (reference count: " + (refCount - 1) + "), not restoring rules");
            return;
        }

        // Last reference - restore original world state
        WorldState originalState = originalWorldStates.remove(worldName);
        worldReferenceCount.remove(worldName);

        if (originalState == null) return;

        // Restore original world state
        world.setGameRuleValue("keepInventory", String.valueOf(originalState.keepInventory));
        world.setDifficulty(originalState.difficulty);
        world.setAutoSave(originalState.autoSave);
        world.setGameRuleValue("doDaylightCycle", String.valueOf(originalState.doDaylightCycle));
        world.setGameRuleValue("doWeatherCycle", String.valueOf(originalState.doWeatherCycle));

        Obsidianwars.getInstance().getLogger().info("Restored world rules for arena " + arenaName + " in world " + worldName);
    }

    private static World getArenaWorld(String arenaName) {
        // Try to get world from lobby spawn
        org.bukkit.Location lobbySpawn = ArenaConfigManager.getLobbySpawn(arenaName);
        if (lobbySpawn != null && lobbySpawn.getWorld() != null) {
            return lobbySpawn.getWorld();
        }

        // Try to get world from team spawn
        org.bukkit.Location redSpawn = ArenaConfigManager.getTeamSpawn(arenaName, "red");
        if (redSpawn != null && redSpawn.getWorld() != null) {
            return redSpawn.getWorld();
        }

        // Fallback to default world
        return Bukkit.getWorlds().get(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Cancel block damage for Creepers in arena worlds
        if (event.getEntityType() == EntityType.CREEPER) {
            // Check if this is happening in an arena world
            for (String arenaName : ArenaConfigManager.getArenaNames()) {
                World arenaWorld = getArenaWorld(arenaName);
                if (arenaWorld != null && event.getLocation().getWorld().equals(arenaWorld)) {
                    // Keep entity damage but cancel block destruction
                    event.blockList().clear();
                    break;
                }
            }
        }
    }

    private static class WorldState {
        final String keepInventory;
        final Difficulty difficulty;
        final boolean autoSave;
        final String doDaylightCycle;
        final String doWeatherCycle;

        WorldState(String keepInventory, Difficulty difficulty, boolean autoSave,
                   String doDaylightCycle, String doWeatherCycle) {
            this.keepInventory = keepInventory;
            this.difficulty = difficulty;
            this.autoSave = autoSave;
            this.doDaylightCycle = doDaylightCycle;
            this.doWeatherCycle = doWeatherCycle;
        }
    }

    public static void cleanup() {
        originalWorldStates.clear();
        worldReferenceCount.clear();
    }
}