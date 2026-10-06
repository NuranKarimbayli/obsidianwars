package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility class for arena-related operations.
 * Provides centralized methods for iterating over arena players efficiently.
 */
public class ArenaUtils {

    /**
     * Executes an action for each player in a specific arena.
     * Uses O(1) lookup via GameManager's arena-specific player tracking.
     *
     * @param arenaName The arena name
     * @param action The action to perform on each player
     */
    public static void forEachArenaPlayer(String arenaName, Consumer<Player> action) {
        for (UUID uuid : GameManager.getPlayersInArena(arenaName)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    /**
     * Gets a list of all players in a specific arena.
     * Uses O(1) lookup via GameManager's arena-specific player tracking.
     *
     * @param arenaName The arena name
     * @return List of players in the arena (empty list if arena has no players)
     */
    public static List<Player> getArenaPlayers(String arenaName) {
        List<Player> players = new ArrayList<>();
        forEachArenaPlayer(arenaName, players::add);
        return players;
    }

    /**
     * Sends a message to all players in a specific arena.
     *
     * @param arenaName The arena name
     * @param message The message to send
     */
    public static void broadcastToArena(String arenaName, String message) {
        forEachArenaPlayer(arenaName, player -> player.sendMessage(message));
    }

    /**
     * Sends a message to all players in a specific arena (CommandSender version).
     *
     * @param arenaName The arena name
     * @param message The message to send
     */
    public static void broadcastToArenaSender(String arenaName, String message) {
        forEachArenaPlayer(arenaName, sender -> sender.sendMessage(message));
    }
}
