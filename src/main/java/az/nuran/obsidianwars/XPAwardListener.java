package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for awarding XP to players for various game events.
 * Also manages per-minute XP rewards for time played.
 */
public class XPAwardListener implements Listener {

    private static final Map<String, BukkitTask> perMinuteTasks = new HashMap<>();

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
        if (perMinuteTasks.containsKey(arenaName)) {
            return; // Task already running
        }

        int xpPerMinute = getXpReward("per-minute-played");
        if (xpPerMinute <= 0) {
            return; // Disabled or invalid
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Obsidianwars.getInstance(), () -> {
            GameManager.ArenaGame game = GameManager.getGame(arenaName);
            if (game == null || game.getGameState() != GameManager.GameState.PLAYING) {
                stopPerMinuteTask(arenaName);
                return;
            }

            // Award XP to all players in the arena
            for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                        LevelManager.addXp(player, xpPerMinute);
                        player.sendMessage("§e+§a" + xpPerMinute + " XP §efor playing!");
                    }
                }
            }
        }, 1200L, 1200L); // Every minute (1200 ticks)

        perMinuteTasks.put(arenaName, task);
    }

    /**
     * Stops the per-minute XP task for an arena.
     */
    public static void stopPerMinuteTask(String arenaName) {
        BukkitTask task = perMinuteTasks.remove(arenaName);
        if (task != null) {
            task.cancel();
        }
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
        for (BukkitTask task : perMinuteTasks.values()) {
            task.cancel();
        }
        perMinuteTasks.clear();
    }
}
