package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.commands.ObsidianCommand;

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

        // Team Selection Compass
        if (itemName.equals("§eTeam Selection")) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                // Check if player is in arena
                if (ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                    // Open team selection menu
                    obsidianCommand.openTeamSelectionGUI(player);
                } else {
                    player.sendMessage("§cYou are not in an arena!");
                }
            }
        }

        // Leave Game Door
        if (itemName.equals("§cLeave Game")) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                // Execute leave command
                player.performCommand("obsidian leave");
            }
        }
    }
}