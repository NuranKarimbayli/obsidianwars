package az.nuran.obsidianwars.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {

    // Memory to store players' selected 1st and 2nd corners (Map)
    public static final Map<UUID, Location> pos1Map = new HashMap<>();
    public static final Map<UUID, Location> pos2Map = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // If player has an item and it's our Wand:
        if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§6Obsidian Wars Wand")) {

            // Left click -> Pos1 (Corner 1)
            if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true); // Prevent block breaking (e.g., in survival mode)
                Location loc = event.getClickedBlock().getLocation();
                pos1Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos1] First corner set: X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            } else if (event.getAction() == Action.LEFT_CLICK_AIR) {
                // Left click in air -> select block under feet
                event.setCancelled(true);
                Location loc = player.getLocation().subtract(0, 1, 0); // Block under feet
                pos1Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos1] First corner set (ground): X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            }

            // Right click -> Pos2 (Corner 2)
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true); // Prevent block placement
                Location loc = event.getClickedBlock().getLocation();
                pos2Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos2] Second corner set: X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                // Right click in air -> select block under feet
                event.setCancelled(true);
                Location loc = player.getLocation().subtract(0, 1, 0); // Block under feet
                pos2Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos2] Second corner set (ground): X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            }
        }
    }

    public static void cleanup() {
        pos1Map.clear();
        pos2Map.clear();
    }
}