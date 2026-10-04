package az.nuran.obsidianwars;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Dedicated listener for spectator mode restrictions and interactions.
 * Handles all spectator-specific events with strict priority.
 */
public class SpectatorListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if player is a spectator
        if (!SpectatorManager.isSpectator(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            // Spectators cannot interact with anything without a special item
            event.setCancelled(true);
            return;
        }

        String itemName = item.getItemMeta().getDisplayName();

        // Handle right-click actions (both air and block)
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
            event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {

            // Spectator teleporter compass - open GUI to teleport to alive players
            if (itemName != null && itemName.equals("§a§lPlayer Teleporter")) {
                String arenaName = SpectatorManager.getSpectatorArena(player);
                if (arenaName != null) {
                    SpectatorTeleporterGUI.openTeleporterGUI(player, arenaName);
                }
                event.setCancelled(true);
                return;
            }

            // Fly speed feather - cycle speed
            if (itemName != null && itemName.startsWith("§e§lFly Speed")) {
                SpectatorManager.cycleFlySpeed(player);
                event.setCancelled(true);
                return;
            }

            // Night vision toggle
            if (itemName != null && itemName.startsWith("§b§lToggle Night Vision")) {
                SpectatorManager.toggleNightVision(player);
                event.setCancelled(true);
                return;
            }

            // Leave item - return to lobby
            if (itemName != null && itemName.equals("§c§lLeave Spectator")) {
                String arenaName = SpectatorManager.getSpectatorArena(player);
                if (arenaName != null) {
                    Location lobbySpawn = ArenaConfigManager.getLobbySpawn(arenaName);
                    if (lobbySpawn != null) {
                        SpectatorManager.removeSpectatorMode(player);
                        player.teleport(lobbySpawn);
                        player.sendMessage("§aReturned to lobby!");
                    } else {
                        SpectatorManager.removeSpectatorMode(player);
                        player.teleport(player.getWorld().getSpawnLocation());
                        player.sendMessage("§aReturned to spawn!");
                    }
                }
                event.setCancelled(true);
                return;
            }
        }

        // Cancel any other interaction
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Spectators cannot drop items
        if (SpectatorManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Spectators cannot break blocks
        if (SpectatorManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Spectators cannot place blocks
        if (SpectatorManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Prevent spectators from interacting with their own inventory
        // (except the teleporter GUI which is handled separately)
        if (SpectatorManager.isSpectator(player)) {
            String title = event.getView().getTitle();
            if (!title.equals("§6§lTeleport to Player")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Spectators cannot take damage
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (SpectatorManager.isSpectator(victim)) {
                event.setCancelled(true);
                return;
            }
        }

        // Spectators cannot deal damage
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (SpectatorManager.isSpectator(damager)) {
                event.setCancelled(true);
            }
        }
    }
}
