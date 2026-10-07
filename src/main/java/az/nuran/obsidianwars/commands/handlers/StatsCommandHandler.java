package az.nuran.obsidianwars.commands.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.StatsManager;
import az.nuran.obsidianwars.services.DebugManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles stats commands.
 */
public class StatsCommandHandler implements CommandHandler {

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("stats")) {
            if (!player.hasPermission("obsidianwars.command.stats")) {
                player.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            handleStatsCommand(player, args);
            return true;
        }

        return false;
    }

    private void handleStatsCommand(Player player, String[] args) {
        Player target = player;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[0]);
                return;
            }
        }

        DebugManager.logDebug("Opening stats GUI: viewer=" + player.getName() + ", target=" + target.getName(), null);
        StatsManager.openStatsGUI(player, target);
    }
}
