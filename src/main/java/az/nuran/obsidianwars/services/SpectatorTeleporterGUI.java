package az.nuran.obsidianwars.services;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.handlers.TeamListener;
import az.nuran.obsidianwars.managers.LanguageManager;
import az.nuran.obsidianwars.managers.SpectatorManager;
import az.nuran.obsidianwars.services.TeamConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for spectators to teleport to alive players in the arena.
 */
public class SpectatorTeleporterGUI {

    /**
     * Opens the teleporter GUI for a spectator.
     *
     * @param spectator The spectator player
     * @param arenaName The arena being spectated
     */
    public static void openTeleporterGUI(Player spectator, String arenaName) {
        // Get all alive players in the arena
        List<Player> alivePlayers = getAlivePlayersInArena(arenaName);

        if (alivePlayers.isEmpty()) {
            LanguageManager.getInstance().sendMessage(spectator, "no_alive_players");
            return;
        }

        // Calculate inventory size (9 slots per row, minimum 9)
        int size = Math.max(9, ((alivePlayers.size() + 8) / 9) * 9);
        String guiTitle = LanguageManager.getInstance().getMessage(spectator, "gui_teleport_title");
        Inventory gui = Bukkit.createInventory(null, size, ColorUtils.translate(guiTitle));

        // Add player heads to GUI
        for (int i = 0; i < alivePlayers.size(); i++) {
            Player target = alivePlayers.get(i);
            ItemStack playerHead = createPlayerHead(target);
            gui.setItem(i, playerHead);
        }

        spectator.openInventory(gui);
    }

    /**
     * Gets all alive (non-spectator) players in an arena.
     *
     * @param arenaName The arena name
     * @return List of alive players
     */
    private static List<Player> getAlivePlayersInArena(String arenaName) {
        List<Player> alivePlayers = new ArrayList<>();
        for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && !SpectatorManager.isSpectator(player)) {
                    alivePlayers.add(player);
                }
            }
        }
        return alivePlayers;
    }

    /**
     * Creates a player head ItemStack for the GUI.
     *
     * @param player The player
     * @return The player head ItemStack
     */
    private static ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            String displayName = LanguageManager.getInstance().getGlobalMessage("player_head_name", "player", player.getName());
            meta.setDisplayName(ColorUtils.translate(displayName));

            // Get player team for lore
            String team = TeamListener.playerTeams.get(player.getUniqueId());
            String teamColor = team != null ? TeamConfig.getTeamColor(team) : "&f";
            String teamName = team != null ? TeamConfig.getTeamName(team) : LanguageManager.getInstance().getGlobalMessage("no_team_label");

            String teamLine = LanguageManager.getInstance().getGlobalMessage("team_name_line", "teamColor", teamColor, "teamName", teamName);
            String teleportLine = LanguageManager.getInstance().getGlobalMessage("click_to_teleport_lore", "player", player.getName());

            meta.setLore(ColorUtils.translate(java.util.Arrays.asList(
                teamLine,
                teleportLine
            )));
            head.setItemMeta(meta);
        }
        return head;
    }
}
