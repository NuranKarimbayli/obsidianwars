package az.nuran.obsidianwars;

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

        // Komanda Seçimi Kompası
        if (itemName.equals("§eKomanda Seçimi")) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                // Oyunçu arenada olub-olmadığını yoxlayırıq
                if (ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                    // Komanda seçimi menyusunu açırıq
                    obsidianCommand.openTeamSelectionGUI(player);
                } else {
                    player.sendMessage("§cSiz arenada deyilsiniz!");
                }
            }
        }

        // Oyundan Çıxış Qapısı
        if (itemName.equals("§cOyundan Çıx")) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                // Komanda ilə leave əmrini icra edirik
                player.performCommand("obsidian leave");
            }
        }
    }
}