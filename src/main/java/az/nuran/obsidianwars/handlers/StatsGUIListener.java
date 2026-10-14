package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.managers.LanguageManager;
import org.bukkit.entity.Player;
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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String stripped = ColorUtils.stripColors(title);
        String titleSuffix = LanguageManager.getInstance().getMessage(player, "gui_stats_title_suffix");
        String statsTitle = LanguageManager.getInstance().getMessage(player, "gui_stats_title");
        if ((stripped == null || !stripped.contains(titleSuffix)) && !ColorUtils.matches(title, statsTitle)) {
            return;
        }

        event.setCancelled(true); // Prevent taking items
        // Stats GUI is read-only, no actions needed
    }
}
