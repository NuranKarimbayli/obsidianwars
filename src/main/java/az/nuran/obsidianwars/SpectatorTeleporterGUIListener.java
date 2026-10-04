package az.nuran.obsidianwars;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles clicks in the Spectator Teleporter GUI.
 * Inventory restrictions for spectators are handled by SpectatorListener.
 */
public class SpectatorTeleporterGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Handle spectator teleporter GUI
        String title = event.getView().getTitle();
        if (!title.equals("§6§lTeleport to Player")) {
            return;
        }

        event.setCancelled(true); // Prevent taking items

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        // Get the target player name from the display name
        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName == null || !displayName.startsWith("§e")) {
            return;
        }

        String targetName = displayName.substring(2); // Remove "§e" prefix
        Player target = org.bukkit.Bukkit.getPlayerExact(targetName);

        if (target != null && target.isOnline() && !SpectatorManager.isSpectator(target)) {
            // Teleport spectator to target
            player.teleport(target.getLocation());
            player.sendMessage("§aTeleported to " + targetName + "!");
            player.closeInventory();
        } else {
            player.sendMessage("§cPlayer is no longer available!");
            player.closeInventory();
        }
    }
}
