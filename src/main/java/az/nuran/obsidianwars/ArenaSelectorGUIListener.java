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
        // Handle both regular and admin arena selector GUIs
        String title = event.getView().getTitle();
        if (!title.equals("§6§lArena Selector") && !title.equals("§c§lAdmin Arena Selector")) {
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

        // Admin GUI - show arena info but don't join
        if (title.equals("§c§lAdmin Arena Selector")) {
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            int currentPlayers = ObsidianCommand.getArenaPlayerCount(arenaName);
            int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);
            player.sendMessage("§eArena: §f" + arenaName);
            player.sendMessage("§eStatus: §f" + status);
            player.sendMessage("§ePlayers: §f" + currentPlayers + "/" + maxPlayers);
            player.closeInventory();
            return;
        }

        // Regular GUI - only allow joining READY or WAITING arenas (not STARTING or PLAYING)
        String status = ArenaConfigManager.getArenaStatus(arenaName);
        if (!"READY".equals(status) && !"WAITING".equals(status)) {
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
