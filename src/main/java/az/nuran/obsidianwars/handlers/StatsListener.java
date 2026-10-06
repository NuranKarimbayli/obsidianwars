package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.managers.LevelManager;
import az.nuran.obsidianwars.managers.StatsManager;
import az.nuran.obsidianwars.services.StatsDAO;

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
        // Save player level data asynchronously
        StatsDAO.savePlayerLevel(event.getPlayer().getUniqueId());
        // Remove level data from memory
        LevelManager.removePlayerLevel(event.getPlayer().getUniqueId());
    }
}
