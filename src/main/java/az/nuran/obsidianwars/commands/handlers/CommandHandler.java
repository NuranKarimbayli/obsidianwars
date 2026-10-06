package az.nuran.obsidianwars.commands.handlers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Base interface for command handlers.
 * All command handlers should implement this interface.
 */
public interface CommandHandler {
    /**
     * Handles the command execution.
     *
     * @param player The player who executed the command
     * @param args   The command arguments (subcommand and parameters)
     * @return true if the command was handled successfully
     */
    boolean handle(Player player, String[] args);
}
