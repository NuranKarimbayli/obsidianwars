package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for per-world tablist feature.
 * Players only see other players who are in the same world.
 */
public class PerWorldTablistManager implements Listener {

    private static PerWorldTablistManager instance;
    private final boolean enabled;
    private final Obsidianwars plugin;

    public PerWorldTablistManager(Obsidianwars plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("per-world.tablist", true);
        this.instance = this;

        if (enabled) {
            plugin.getLogger().info("Per-World Tablist: ENABLED");
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } else {
            plugin.getLogger().info("Per-World Tablist: DISABLED");
        }
    }

    public static PerWorldTablistManager getInstance() {
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();

        // Update visibility for all players
        updateVisibilityForAll();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled) return;

        // No action needed - player is removed from server
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();

        // Update visibility for all players
        updateVisibilityForAll();
    }

    /**
     * Updates visibility for all players on the server.
     * Players can only see others in the same world.
     */
    public void updateVisibilityForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerVisibility(player);
        }
    }

    /**
     * Updates a single player's visibility.
     * They will only see players in their current world.
     */
    public void updatePlayerVisibility(Player player) {
        String playerWorld = player.getWorld().getName();

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (player.equals(other)) continue;

            String otherWorld = other.getWorld().getName();

            if (playerWorld.equals(otherWorld)) {
                // Same world - show each other
                player.showPlayer(plugin, other);
                other.showPlayer(plugin, player);
            } else {
                // Different worlds - hide each other
                player.hidePlayer(plugin, other);
                other.hidePlayer(plugin, player);
            }
        }
    }

    /**
     * Resets visibility for a player (shows all players).
     * Useful when disabling the feature temporarily.
     */
    public void resetPlayerVisibility(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!player.equals(other)) {
                player.showPlayer(plugin, other);
            }
        }
    }

    /**
     * Resets visibility for all players (shows everyone).
     * Useful when disabling the feature.
     */
    public void resetAllVisibility() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayerVisibility(player);
        }
    }
}
