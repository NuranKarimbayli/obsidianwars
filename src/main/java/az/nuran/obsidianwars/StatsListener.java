package az.nuran.obsidianwars;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for loading and saving player statistics.
 */
public class StatsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player stats asynchronously when they join
        StatsManager.loadPlayerStats(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player stats asynchronously when they quit
        StatsManager.savePlayerStats(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
