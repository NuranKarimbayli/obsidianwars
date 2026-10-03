package az.nuran.obsidianwars;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamListener implements Listener {

    // Oyunçuların komanda seçimlərini saxlayırıq
    public static final Map<UUID, String> playerTeams = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Yalnız komanda seçimi menyusu üçün
        if (event.getView().getTitle().equals("§6Komanda Seçimi")) {
            event.setCancelled(true); // Əşyaları götürməyi qadağan edirik

            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                
                // Check if countdown is 6 seconds or fewer - prevent team changes
                String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
                if (arenaName != null) {
                    int countdown = GameManager.getCountdown(arenaName);
                    GameManager.ArenaGame game = GameManager.getGame(arenaName);
                    
                    // Allow team switching if countdown is paused (for team balancing)
                    if (game != null && game.isCountdownPaused()) {
                        // Team switching is allowed when paused
                        // Continue with team change logic
                    } else if (countdown > 0 && countdown <= 6) {
                        player.sendMessage("§cKomanda dəyişdirilməsi son 6 saniyədə qadağandır!");
                        player.closeInventory();
                        return;
                    }
                }
                
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }

                String itemName = clickedItem.getItemMeta().getDisplayName();
                String currentTeam = playerTeams.get(player.getUniqueId());

                // Qırmızı Komanda seçimi
                if (itemName.equals("§cQırmızı Komanda")) {
                    if (currentTeam != null && currentTeam.equals("red")) {
                        player.sendMessage("§cSiz artıq Qırmızı Komandasındasınız!");
                        player.closeInventory();
                        return;
                    }
                    
                    // Əgər oyunçu başqa komandada idi, oradan çıxarırıq
                    if (currentTeam != null && currentTeam.equals("blue")) {
                        player.sendMessage("§eMavi Komandasından çıxarıldınız.");
                    }
                    
                    playerTeams.put(player.getUniqueId(), "red");
                    applyTeamColor(player, "red");
                    TeamManager.setupTeam(player, "red");
                    ScoreboardManager.updateScoreboard(player);
                    player.sendMessage("§aSiz §cQırmızı Komandaya§a qoşuldunuz!");
                    player.closeInventory();
                    
                    // Oyun başlama şəraitini yoxlayırıq
                    if (arenaName != null) {
                        GameManager.checkGameStart(arenaName);
                        // Check if we should resume paused countdown
                        GameManager.checkCountdownResume(arenaName);
                    }
                }

                // Mavi Komanda seçimi
                if (itemName.equals("§9Mavi Komanda")) {
                    if (currentTeam != null && currentTeam.equals("blue")) {
                        player.sendMessage("§cSiz artıq Mavi Komandasındasınız!");
                        player.closeInventory();
                        return;
                    }
                    
                    // Əgər oyunçu başqa komandada idi, oradan çıxarırıq
                    if (currentTeam != null && currentTeam.equals("red")) {
                        player.sendMessage("§eQırmızı Komandasından çıxarıldınız.");
                    }
                    
                    playerTeams.put(player.getUniqueId(), "blue");
                    applyTeamColor(player, "blue");
                    TeamManager.setupTeam(player, "blue");
                    ScoreboardManager.updateScoreboard(player);
                    player.sendMessage("§aSiz §9Mavi Komandaya§a qoşuldunuz!");
                    player.closeInventory();
                    
                    // Oyun başlama şəraitini yoxlayırıq
                    if (arenaName != null) {
                        GameManager.checkGameStart(arenaName);
                        // Check if we should resume paused countdown
                        GameManager.checkCountdownResume(arenaName);
                    }
                }
            }
        }
    }

    public static void applyTeamColor(Player player, String team) {
        if (team.equals("red")) {
            // Ad rəngi - Qırmızı
            String coloredName = "§c" + player.getName();
            player.setDisplayName(coloredName);
            player.setPlayerListName(coloredName);
            
            // Dəri zireh - Qırmızı
            equipLeatherArmor(player, Color.RED);
        } else if (team.equals("blue")) {
            // Ad rəngi - Mavi
            String coloredName = "§9" + player.getName();
            player.setDisplayName(coloredName);
            player.setPlayerListName(coloredName);
            
            // Dəri zireh - Mavi
            equipLeatherArmor(player, Color.BLUE);
        }
    }

    public static void equipLeatherArmor(Player player, Color color) {
        // Leather Helmet
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        if (helmetMeta != null) {
            helmetMeta.setColor(color);
            helmet.setItemMeta(helmetMeta);
        }
        player.getInventory().setHelmet(helmet);

        // Leather Chestplate
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setColor(color);
            chestplate.setItemMeta(chestMeta);
        }
        player.getInventory().setChestplate(chestplate);

        // Leather Leggings
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        if (leggingsMeta != null) {
            leggingsMeta.setColor(color);
            leggings.setItemMeta(leggingsMeta);
        }
        player.getInventory().setLeggings(leggings);

        // Leather Boots
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.setColor(color);
            boots.setItemMeta(bootsMeta);
        }
        player.getInventory().setBoots(boots);
    }
}
