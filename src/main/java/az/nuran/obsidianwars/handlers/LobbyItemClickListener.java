package az.nuran.obsidianwars.handlers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.LanguageManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LobbyItemClickListener implements Listener {

    private final ObsidianCommand obsidianCommand;

    public LobbyItemClickListener(ObsidianCommand obsidianCommand) {
        this.obsidianCommand = obsidianCommand;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String itemName = item.getItemMeta().getDisplayName();
        String teamSelectionName = LanguageManager.getInstance().getGlobalMessage("item_team_selection");
        String exitGameName = LanguageManager.getInstance().getGlobalMessage("item_exit_game");
        String leaveGameName = LanguageManager.getInstance().getGlobalMessage("item_leave_game");

        // Team Selection Compass
        if (ColorUtils.matches(itemName, teamSelectionName)) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                // Check if player is in arena
                if (ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                    // Open team selection menu
                    obsidianCommand.openTeamSelectionGUI(player);
                } else {
                    LanguageManager.getInstance().sendMessage(player, "not_in_arena");
                }
            }
        }

        // Leave Game Door
        if (ColorUtils.matches(itemName, exitGameName) || ColorUtils.matches(itemName, leaveGameName)) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                // Execute leave command
                player.performCommand("obsidian leave");
            }
        }
    }
}