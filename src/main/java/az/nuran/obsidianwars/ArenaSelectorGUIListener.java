package az.nuran.obsidianwars;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles clicks in the Arena Selector GUI.
 */
public class ArenaSelectorGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle arena selector GUI
        if (!event.getView().getTitle().equals("§6§lArena Selector")) {
            return;
        }

        event.setCancelled(true); // Prevent taking items

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();
        if (displayName == null || !displayName.startsWith("§e")) {
            return;
        }

        String arenaName = displayName.replace("§e", "");

        if (arenaName.isEmpty()) {
            return;
        }

        // Check if arena is available
        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage("§cArena not found: " + arenaName);
            player.closeInventory();
            return;
        }

        String status = ArenaConfigManager.getArenaStatus(arenaName);
        if (!"READY".equals(status)) {
            player.sendMessage("§cThis arena is not available (status: " + status + ")");
            player.closeInventory();
            return;
        }

        // Join the arena using the command handler
        String[] args = {"play", arenaName};
        ObsidianCommand command = new ObsidianCommand(Obsidianwars.getInstance());
        command.handleJoinCommand(player, args);

        player.closeInventory();
    }
}
