package az.nuran.obsidianwars.handlers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.LanguageManager;
import az.nuran.obsidianwars.managers.SpectatorManager;
import az.nuran.obsidianwars.services.SpectatorTeleporterGUI;

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
        String teleporterName = LanguageManager.getInstance().getMessage(player, "item_teleporter_name");
        String flySpeedPrefix = "&e&lFly Speed"; // Keep prefix for matching
        String nightVisionPrefix = "&b&lToggle Night Vision"; // Keep prefix for matching
        String leaveName = LanguageManager.getInstance().getMessage(player, "item_leave_spectator_name");
        String guiTitle = LanguageManager.getInstance().getMessage(player, "gui_teleport_title");

        // Handle right-click actions (both air and block)
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
            event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {

            // Spectator teleporter compass - open GUI to teleport to alive players
            if (itemName != null && ColorUtils.matches(itemName, teleporterName)) {
                String arenaName = SpectatorManager.getSpectatorArena(player);
                if (arenaName != null) {
                    SpectatorTeleporterGUI.openTeleporterGUI(player, arenaName);
                }
                event.setCancelled(true);
                return;
            }

            // Fly speed feather - cycle speed
            if (itemName != null && ColorUtils.startsWithFormatted(itemName, flySpeedPrefix)) {
                SpectatorManager.cycleFlySpeed(player);
                event.setCancelled(true);
                return;
            }

            // Night vision toggle
            if (itemName != null && ColorUtils.startsWithFormatted(itemName, nightVisionPrefix)) {
                SpectatorManager.toggleNightVision(player);
                event.setCancelled(true);
                return;
            }

            // Leave item - return to lobby
            if (itemName != null && ColorUtils.matches(itemName, leaveName)) {
                String arenaName = SpectatorManager.getSpectatorArena(player);
                if (arenaName != null) {
                    Location lobbySpawn = ArenaConfigManager.getLobbySpawn(arenaName);
                    if (lobbySpawn != null) {
                        SpectatorManager.removeSpectatorMode(player);
                        player.teleport(lobbySpawn);
                        LanguageManager.getInstance().sendMessage(player, "returned_to_lobby");
                    } else {
                        SpectatorManager.removeSpectatorMode(player);
                        player.teleport(player.getWorld().getSpawnLocation());
                        LanguageManager.getInstance().sendMessage(player, "returned_to_spawn");
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
            String guiTitle = LanguageManager.getInstance().getMessage(player, "gui_teleport_title");
            if (!ColorUtils.matches(title, guiTitle)) {
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
