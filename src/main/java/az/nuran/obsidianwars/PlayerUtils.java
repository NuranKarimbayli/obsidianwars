package az.nuran.obsidianwars;

import org.bukkit.entity.Player;
import org.bukkit.Location;

public class PlayerUtils {

    /**
     * Resets a player's state to default values
     * Clears inventory, armor, health, hunger, potion effects, game mode, and fire
     */
    public static void resetPlayerState(Player player) {
        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);

        // Reset health and hunger
        player.setHealth(20);
        player.setFoodLevel(20);

        // Remove potion effects
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // Reset game mode
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);

        // Reset fire
        player.setFireTicks(0);
    }

    /**
     * Resets a player's state and additionally clears display name, player list name,
     * removes from teams, clears scoreboard, and teleports to spawn
     */
    public static void resetPlayerFull(Player player, Location spawnLocation) {
        // Reset basic state
        resetPlayerState(player);

        // Clear display name
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());

        // Remove from teams
        TeamManager.removePlayerFromTeams(player);

        // Clear scoreboard
        ScoreboardManager.removeScoreboard(player);

        // Teleport to spawn
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
        }
    }

    /**
     * Resets a player's state and additionally clears display name, player list name,
     * removes from teams, and clears scoreboard (but does not teleport)
     */
    public static void resetPlayerArenaLeave(Player player) {
        // Reset basic state
        resetPlayerState(player);

        // Clear display name
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());

        // Remove from teams
        TeamManager.removePlayerFromTeams(player);

        // Clear scoreboard
        ScoreboardManager.removeScoreboard(player);
    }
}
