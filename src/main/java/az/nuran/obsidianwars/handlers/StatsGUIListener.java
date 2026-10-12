package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.util.ColorUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks in the Stats GUI.
 */
public class StatsGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle stats GUI (check if title contains player name or is stats-related)
        String title = event.getView().getTitle();
        String stripped = ColorUtils.stripColors(title);
        if ((stripped == null || !stripped.contains("'s Stats")) && !ColorUtils.matches(title, "&6&lStats")) {
            return;
        }

        event.setCancelled(true); // Prevent taking items
        // Stats GUI is read-only, no actions needed
    }
}
