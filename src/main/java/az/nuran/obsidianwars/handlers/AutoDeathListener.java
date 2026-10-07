package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for auto-death feature.
 * Kills players who fall below a configurable Y-level in arena worlds.
 */
public class AutoDeathListener implements Listener {

    private static final Set<UUID> recentlyDied = new HashSet<>();
    private static final long DEATH_COOLDOWN = 2000; // 2 seconds cooldown to prevent multiple deaths

    private final boolean enabled;
    private final int yLevel;
    private final String deathMessage;

    public AutoDeathListener(Obsidianwars plugin) {
        this.enabled = plugin.getConfig().getBoolean("auto-death.enabled", true);
        this.yLevel = plugin.getConfig().getInt("auto-death.y-level", -64);
        this.deathMessage = plugin.getConfig().getString("auto-death.death-message", "&cYou fell into the void!");

        plugin.getLogger().info("Auto-Death feature: " + (enabled ? "ENABLED" : "DISABLED") + " (Y-level: " + yLevel + ")");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check cooldown to prevent spam deaths
        if (recentlyDied.contains(uuid)) {
            return;
        }

        // Only check if player actually moved to a different block (not just looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        World world = player.getWorld();
        double y = player.getLocation().getY();

        // Check if player is below the Y-level
        if (y < yLevel) {
            // Check if this is an arena world by checking if any arena spawn is in this world
            boolean isArenaWorld = false;
            for (String arenaName : ArenaConfigManager.getArenaNames()) {
                Location lobbySpawn = ArenaConfigManager.getLobbySpawn(arenaName);
                if (lobbySpawn != null && lobbySpawn.getWorld().equals(world)) {
                    isArenaWorld = true;
                    break;
                }
                // Also check team spawns
                Location redSpawn = ArenaConfigManager.getTeamSpawn(arenaName, "red");
                if (redSpawn != null && redSpawn.getWorld().equals(world)) {
                    isArenaWorld = true;
                    break;
                }
                Location blueSpawn = ArenaConfigManager.getTeamSpawn(arenaName, "blue");
                if (blueSpawn != null && blueSpawn.getWorld().equals(world)) {
                    isArenaWorld = true;
                    break;
                }
            }

            // Only apply auto-death in arena worlds
            if (isArenaWorld) {
                killPlayer(player);
            }
        }
    }

    private void killPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        // Add to cooldown
        recentlyDied.add(uuid);
        Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
            recentlyDied.remove(uuid);
        }, DEATH_COOLDOWN / 50);

        // Kill the player
        player.setHealth(0);

        // Send death message
        String formattedMessage = deathMessage.replace("&", "§");
        player.sendMessage(formattedMessage);

        Obsidianwars.getInstance().getLogger().info("Player " + player.getName() + " died from auto-death at Y=" + player.getLocation().getY());
    }

    public static void cleanup() {
        recentlyDied.clear();
    }
}
