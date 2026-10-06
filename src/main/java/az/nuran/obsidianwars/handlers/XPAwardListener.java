package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.LevelManager;
import az.nuran.obsidianwars.managers.TaskManager;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listener for awarding XP to players for various game events.
 * Also manages per-minute XP rewards for time played.
 */
public class XPAwardListener implements Listener {

    // Tasks are now managed by TaskManager - no local tracking needed

    /**
     * Gets the XP reward amount from config for a specific action.
     */
    private static int getXpReward(String rewardType) {
        return Obsidianwars.getInstance().getConfig().getInt("xp-rewards." + rewardType, 0);
    }

    /**
     * Starts the per-minute XP task for an arena.
     */
    public static void startPerMinuteTask(String arenaName) {
        int xpPerMinute = getXpReward("per-minute-played");
        if (xpPerMinute <= 0) {
            return; // Disabled or invalid
        }

        // Use TaskManager for centralized task management
        TaskManager.getInstance().runTimer(
            "xp-per-minute-" + arenaName,
            () -> {
                GameManager.ArenaGame game = GameManager.getGame(arenaName);
                if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PLAYING) {
                    stopPerMinuteTask(arenaName);
                    return;
                }

                // Award XP to all players in the arena
                for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                    String playerArena = ObsidianCommand.playersInArena.get(uuid);
                    if (playerArena != null && playerArena.equals(arenaName)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline() && player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                            LevelManager.addXp(player, xpPerMinute);
                            player.sendMessage("§e+§a" + xpPerMinute + " XP §efor playing!");
                        }
                    }
                }
            },
            1200L,  // Initial delay: 1 minute (1200 ticks)
            1200L, // Period: Every minute (1200 ticks)
            arenaName
        );
    }

    /**
     * Stops the per-minute XP task for an arena.
     */
    public static void stopPerMinuteTask(String arenaName) {
        TaskManager.getInstance().cancelTask("xp-per-minute-" + arenaName);
    }

    /**
     * Awards XP for winning a game.
     */
    public static void awardWinXP(Player player) {
        int xp = getXpReward("win");
        if (xp > 0) {
            LevelManager.addXp(player, xp);
            player.sendMessage("§6+§a" + xp + " XP §efor winning the game!");
        }
    }

    /**
     * Awards XP for breaking obsidian.
     */
    public static void awardObsidianXP(Player player) {
        int xp = getXpReward("obsidian-broken");
        if (xp > 0) {
            LevelManager.addXp(player, xp);
            player.sendMessage("§d+§a" + xp + " XP §efor breaking obsidian!");
        }
    }

    /**
     * Awards XP for a regular kill.
     */
    public static void awardKillXP(Player player) {
        int xp = getXpReward("kill");
        if (xp > 0) {
            LevelManager.addXp(player, xp);
            player.sendMessage("§c+§a" + xp + " XP §efor a kill!");
        }
    }

    /**
     * Awards XP for a final kill.
     */
    public static void awardFinalKillXP(Player player) {
        int xp = getXpReward("final-kill");
        if (xp > 0) {
            LevelManager.addXp(player, xp);
            player.sendMessage("§4+§a" + xp + " XP §efor a final kill!");
        }
    }

    /**
     * Cleanup method called on plugin disable.
     */
    public static void cleanup() {
        // Tasks are now managed by TaskManager - no manual cleanup needed
        // TaskManager.cancelAll() will handle all tasks on plugin disable
    }
}
