package az.nuran.obsidianwars.commands.handlers;

import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.QueueManager;
import az.nuran.obsidianwars.managers.SpectatorManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles spectate and queue commands.
 */
public class SpectateCommandHandler implements CommandHandler {

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("spectate")) {
            handleSpectateCommand(player, args);
            return true;
        }

        if (subCommand.equals("queue")) {
            handleQueueCommand(player, args);
            return true;
        }

        return false;
    }

    private void handleSpectateCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o spectate <arena|player>");
            return;
        }

        String target = args[1];

        // Priority 1: Check if target is an online player in an ongoing game
        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            String playerArena = ObsidianCommand.playersInArena.get(targetPlayer.getUniqueId());
            if (playerArena != null) {
                // Player is in a game - set spectator mode and teleport to their location
                SpectatorManager.setSpectatorMode(player, playerArena);
                player.teleport(targetPlayer.getLocation());
                player.sendMessage("§aNow spectating " + targetPlayer.getName() + " in arena " + playerArena);
                return;
            }
        }

        // Priority 2: Check if target is an arena name
        if (ArenaConfigManager.arenaExists(target)) {
            Location spectSpawn = ArenaConfigManager.getSpectatorSpawn(target);
            if (spectSpawn != null) {
                SpectatorManager.setSpectatorMode(player, target);
                player.teleport(spectSpawn);
                player.sendMessage("§aNow spectating arena " + target);
                return;
            } else {
                player.sendMessage("§cSpectator spawn not set for arena " + target);
                return;
            }
        }

        // Neither found
        player.sendMessage("§cTarget player or arena not found.");
    }

    private void handleQueueCommand(Player player, String[] args) {
        String arenaName = null;

        // Check if arena specified
        if (args.length > 1) {
            arenaName = args[1];
        }

        // Join queue
        QueueManager.getInstance().joinQueue(player, arenaName);
    }
}
