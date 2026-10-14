package az.nuran.obsidianwars.handlers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.LanguageManager;

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
        String regularTitle = LanguageManager.getInstance().getGlobalMessage("gui_arena_selector_title");
        String adminTitle = LanguageManager.getInstance().getGlobalMessage("gui_admin_arena_selector_title");
        if (!ColorUtils.matches(title, regularTitle) && !ColorUtils.matches(title, adminTitle)) {
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
        if (displayName == null || !ColorUtils.startsWithFormatted(displayName, "&e")) {
            return;
        }

        String arenaName = ColorUtils.stripColors(displayName);

        if (arenaName.isEmpty()) {
            return;
        }

        // Check if arena is available
        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            player.closeInventory();
            return;
        }

        // Admin GUI - show arena info but don't join
        if (ColorUtils.matches(title, adminTitle)) {
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            int currentPlayers = ObsidianCommand.getArenaPlayerCount(arenaName);
            int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);
            LanguageManager.getInstance().sendMessage(player, "arena_info_label", "arena", arenaName);
            LanguageManager.getInstance().sendMessage(player, "status_label", "status", status);
            LanguageManager.getInstance().sendMessage(player, "players_label", "current", String.valueOf(currentPlayers), "max", String.valueOf(maxPlayers));
            player.closeInventory();
            return;
        }

        // Regular GUI - only allow joining READY or WAITING arenas (not STARTING or PLAYING)
        String status = ArenaConfigManager.getArenaStatus(arenaName);
        if (!"READY".equals(status) && !"WAITING".equals(status)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_available", "status", status);
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
