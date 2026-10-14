package az.nuran.obsidianwars.handlers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.LanguageManager;
import az.nuran.obsidianwars.managers.TeamManager;
import az.nuran.obsidianwars.services.ArmorUtils;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles team selection GUI interactions with strict balance checking.
 * Players cannot switch teams if it would create imbalance or if countdown is <= 5 seconds.
 */
public class TeamListener implements Listener {

    // We store player team selections (thread-safe for concurrent access)
    public static final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Only for team selection menu
        String guiTitle = LanguageManager.getInstance().getGlobalMessage("gui_team_selection_title");
        if (ColorUtils.matches(event.getView().getTitle(), guiTitle)) {
            event.setCancelled(true); // We prohibit taking items

            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();

                // Check if player is in an arena
                String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
                if (arenaName == null) {
                    player.closeInventory();
                    return;
                }

                // STRICT RULE: Lock team switching when countdown <= 5 seconds
                int countdown = GameManager.getCountdown(arenaName);
                if (countdown > 0 && countdown <= 5) {
                    LanguageManager.getInstance().sendMessage(player, "team_switch_blocked");
                    player.closeInventory();
                    return;
                }

                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    return;
                }

                String itemName = clickedItem.getItemMeta().getDisplayName();
                String currentTeam = playerTeams.get(player.getUniqueId());
                String redTeamName = LanguageManager.getInstance().getGlobalMessage("item_red_team");
                String blueTeamName = LanguageManager.getInstance().getGlobalMessage("item_blue_team");

                // Red Team selection
                if (ColorUtils.matches(itemName, redTeamName)) {
                    if (currentTeam != null && currentTeam.equals("red")) {
                        LanguageManager.getInstance().sendMessage(player, "already_in_red_team");
                        player.closeInventory();
                        return;
                    }

                    // STRICT RULE: Check if switching would create imbalance
                    if (!TeamManager.canSwitchTeam(arenaName, currentTeam, "red")) {
                        LanguageManager.getInstance().sendMessage(player, "team_balance_error");
                        player.closeInventory();
                        return;
                    }

                    // If player was in another team, we remove them from there
                    if (currentTeam != null && currentTeam.equals("blue")) {
                        LanguageManager.getInstance().sendMessage(player, "removed_from_blue");
                    }

                    // Use TeamManager to set team
                    TeamManager.setPlayerTeam(player, arenaName, "red");
                    LanguageManager.getInstance().sendMessage(player, "joined_red_team");
                    player.closeInventory();

                    // We check game start condition
                    GameManager.checkGameStart(arenaName);
                }

                // Blue Team selection
                if (ColorUtils.matches(itemName, blueTeamName)) {
                    if (currentTeam != null && currentTeam.equals("blue")) {
                        LanguageManager.getInstance().sendMessage(player, "already_in_blue_team");
                        player.closeInventory();
                        return;
                    }

                    // STRICT RULE: Check if switching would create imbalance
                    if (!TeamManager.canSwitchTeam(arenaName, currentTeam, "blue")) {
                        LanguageManager.getInstance().sendMessage(player, "team_balance_error");
                        player.closeInventory();
                        return;
                    }

                    // If player was in another team, we remove them from there
                    if (currentTeam != null && currentTeam.equals("red")) {
                        LanguageManager.getInstance().sendMessage(player, "removed_from_red");
                    }

                    // Use TeamManager to set team
                    TeamManager.setPlayerTeam(player, arenaName, "blue");
                    LanguageManager.getInstance().sendMessage(player, "joined_blue_team");
                    player.closeInventory();

                    // We check game start condition
                    GameManager.checkGameStart(arenaName);
                }
            }
        }
    }

    public static void applyTeamColor(Player player, String team) {
        applyTeamColor(player, team, true);
    }

    public static void applyTeamColor(Player player, String team, boolean equipArmor) {
        if (team.equals("red")) {
            // Name color - Red
            String coloredName = ColorUtils.translate(LanguageManager.getInstance().getGlobalMessage("name_color_red", "player", player.getName()));
            player.setDisplayName(coloredName);
            player.setPlayerListName(coloredName);

            // Leather armor - Red (only if equipArmor is true)
            if (equipArmor) {
                ArmorUtils.equipColoredLeatherArmor(player, Color.RED);
            }
        } else if (team.equals("blue")) {
            // Name color - Blue
            String coloredName = ColorUtils.translate(LanguageManager.getInstance().getGlobalMessage("name_color_blue", "player", player.getName()));
            player.setDisplayName(coloredName);
            player.setPlayerListName(coloredName);

            // Leather armor - Blue (only if equipArmor is true)
            if (equipArmor) {
                ArmorUtils.equipColoredLeatherArmor(player, Color.BLUE);
            }
        }
    }

    public static void cleanup() {
        playerTeams.clear();
    }
}
