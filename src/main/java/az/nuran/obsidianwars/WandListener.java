package az.nuran.obsidianwars;

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

    // Oyunçuların seçdiyi 1-ci və 2-ci küncləri saxlayacaq yaddaş (Map)
    public static final Map<UUID, Location> pos1Map = new HashMap<>();
    public static final Map<UUID, Location> pos2Map = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Oyunçunun əlində əşya var və bu bizim Wand-dırsa:
        if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§6Obsidian Wars Wand")) {

            // Sol klik -> Pos1 (Künc 1)
            if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true); // Bloku qırmağı (məsələn survival-da) engəlləyirik
                Location loc = event.getClickedBlock().getLocation();
                pos1Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos1] Birinci künc təyin olundu: X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            } else if (event.getAction() == Action.LEFT_CLICK_AIR) {
                // Havaya sol klik -> ayaqların altındakı bloku seç
                event.setCancelled(true);
                Location loc = player.getLocation().subtract(0, 1, 0); // Ayaqların altındakı blok
                pos1Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos1] Birinci künc təyin olundu (yer): X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            }

            // Sağ klik -> Pos2 (Künc 2)
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true); // Blok qoymağı engəlləyirik
                Location loc = event.getClickedBlock().getLocation();
                pos2Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos2] İkinci künc təyin olundu: X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                // Havaya sağ klik -> ayaqların altındakı bloku seç
                event.setCancelled(true);
                Location loc = player.getLocation().subtract(0, 1, 0); // Ayaqların altındakı blok
                pos2Map.put(player.getUniqueId(), loc);
                player.sendMessage("§a[Pos2] İkinci künc təyin olundu (yer): X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
            }
        }
    }

    public static void cleanup() {
        pos1Map.clear();
        pos2Map.clear();
    }
}