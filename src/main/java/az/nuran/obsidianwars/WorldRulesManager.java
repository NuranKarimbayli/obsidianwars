package az.nuran.obsidianwars;

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

    public static void applyGameRules(String arenaName) {
        // Get the arena world from lobby spawn or team spawn
        World world = getArenaWorld(arenaName);
        if (world == null) return;

        // Save original world state
        WorldState originalState = new WorldState(
            world.getGameRuleValue("keepInventory") != null ? world.getGameRuleValue("keepInventory") : "false",
            world.getDifficulty(),
            world.isAutoSave(),
            world.getGameRuleValue("doDaylightCycle") != null ? world.getGameRuleValue("doDaylightCycle") : "true",
            world.getGameRuleValue("doWeatherCycle") != null ? world.getGameRuleValue("doWeatherCycle") : "true"
        );
        originalWorldStates.put(arenaName, originalState);

        // Apply game rules
        world.setGameRuleValue("keepInventory", "true");
        world.setDifficulty(Difficulty.HARD);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");

        Obsidianwars.getInstance().getLogger().info("Applied game rules for arena " + arenaName + " in world " + world.getName());
    }

    public static void restoreWorldRules(String arenaName) {
        WorldState originalState = originalWorldStates.remove(arenaName);
        if (originalState == null) return;

        World world = getArenaWorld(arenaName);
        if (world == null) return;

        // Restore original world state
        world.setGameRuleValue("keepInventory", String.valueOf(originalState.keepInventory));
        world.setDifficulty(originalState.difficulty);
        world.setAutoSave(originalState.autoSave);
        world.setGameRuleValue("doDaylightCycle", String.valueOf(originalState.doDaylightCycle));
        world.setGameRuleValue("doWeatherCycle", String.valueOf(originalState.doWeatherCycle));

        Obsidianwars.getInstance().getLogger().info("Restored world rules for arena " + arenaName + " in world " + world.getName());
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
}