package az.nuran.obsidianwars.commands.handlers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles GUI-related commands: gui, cmds, arenalist, team.
 */
public class GUICommandHandler implements CommandHandler {

    private final Obsidianwars plugin;

    public GUICommandHandler(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("gui")) {
            if (!player.hasPermission("obsidianwars.command.gui")) {
                player.sendMessage(ColorUtils.translate("&cYou don't have permission to use this command."));
                return true;
            }
            handleGUICommand(player);
            return true;
        }

        if (subCommand.equals("cmds")) {
            if (!player.hasPermission("obsidianwars.command.cmds")) {
                player.sendMessage(ColorUtils.translate("&cYou don't have permission to use this command."));
                return true;
            }
            handleCmdsCommand(player);
            return true;
        }

        if (subCommand.equals("arenalist")) {
            if (!player.hasPermission("obsidianwars.command.arenalist")) {
                player.sendMessage(ColorUtils.translate("&cYou don't have permission to use this command."));
                return true;
            }
            handleArenalistCommand(player);
            return true;
        }

        if (subCommand.equals("team")) {
            if (!player.hasPermission("obsidianwars.command.team")) {
                player.sendMessage(ColorUtils.translate("&cYou don't have permission to use this command."));
                return true;
            }
            handleTeamCommand(player);
            return true;
        }

        return false;
    }

    private void handleGUICommand(Player player) {
        ObsidianCommand.getInstance().openArenaSelectorGUI(player, false);
    }

    private void handleCmdsCommand(Player player) {
        player.sendMessage(ColorUtils.translate("&6&l=== ObsidianWars Commands ==="));
        player.sendMessage(ColorUtils.translate("&ePlayer Commands:"));
        player.sendMessage(ColorUtils.translate("  &a/o play [arena] &7- Join an arena (or random)"));
        player.sendMessage(ColorUtils.translate("  &a/o join [arena] &7- Alias for play"));
        player.sendMessage(ColorUtils.translate("  &a/o queue [arena] &7- Join queue for arena (or auto-assign)"));
        player.sendMessage(ColorUtils.translate("  &a/o leave &7- Leave current arena"));
        player.sendMessage(ColorUtils.translate("  &a/o rejoin &7- Rejoin after disconnect"));
        player.sendMessage(ColorUtils.translate("  &a/o stats [player] &7- View player statistics"));
        player.sendMessage(ColorUtils.translate("  &a/o gui &7- Open arena selector GUI"));
        player.sendMessage(ColorUtils.translate("  &a/o team &7- Open team selection GUI"));
        player.sendMessage(ColorUtils.translate("&eAdmin Commands:"));
        player.sendMessage(ColorUtils.translate("  &a/o reload [all|config|arenas] &7- Reload plugin configurations"));
        player.sendMessage(ColorUtils.translate("  &a/o arena teleport <arena> &7- Teleport to arena spectator spawn"));
        player.sendMessage(ColorUtils.translate("  &a/o admin level <set|add|remove> <player> <amount> &7- Manage player levels"));
        player.sendMessage(ColorUtils.translate("  &a/o admin coin <set|add|remove> <player> <amount> &7- Manage player coins"));
        player.sendMessage(ColorUtils.translate("  &a/o admin exp <set|add|remove> <player> <amount> &7- Manage player XP"));
        player.sendMessage(ColorUtils.translate("  &a/o admin stats reset <player> &7- Reset player statistics"));
        player.sendMessage(ColorUtils.translate("  &a/o admin gui &7- Open admin arena selector (all statuses)"));
        player.sendMessage(ColorUtils.translate("  &a/o create arena <name> <min> <max> &7- Create arena"));
        player.sendMessage(ColorUtils.translate("  &a/o delete arena <name> &7- Delete arena"));
        player.sendMessage(ColorUtils.translate("  &a/o disableArena <arena> &7- Disable arena"));
        player.sendMessage(ColorUtils.translate("  &a/o enableArena <arena> &7- Enable arena"));
        player.sendMessage(ColorUtils.translate("  &a/o force start arena <arena> &7- Force start game"));
        player.sendMessage(ColorUtils.translate("  &a/o force end arena <arena> &7- Force end game"));
        player.sendMessage(ColorUtils.translate("  &a/o force end preperation <arena> &7- Skip preparation phase"));
        player.sendMessage(ColorUtils.translate("  &a/o forcestart <arena> &7- Force start game (legacy)"));
        player.sendMessage(ColorUtils.translate("  &a/o forceend <arena> &7- Force end game (legacy)"));
        player.sendMessage(ColorUtils.translate("  &a/o forceprep <arena> &7- Skip preparation phase (legacy)"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setlobby <arena> &7- Set main lobby"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setwaitingspawn <arena> &7- Set waiting lobby"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setwaitingregion <arena> &7- Set waiting boundary"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setspawn <arena> <red|blue> &7- Set team spawn"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setobsidian <arena> <red|blue> &7- Set obsidian"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setblocks <arena> &7- Setup resource blocks"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setwall <arena> <red|blue> &7- Set defense wall"));
        player.sendMessage(ColorUtils.translate("  &a/o arena settimer <arena> <time> &7- Set prep time"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setplayers <arena> <min> <max> &7- Set player limits"));
        player.sendMessage(ColorUtils.translate("  &a/o arena finish <arena> &7- Finish arena setup"));
        player.sendMessage(ColorUtils.translate("  &a/o arena setspectspawn <arena> &7- Set spectator spawn"));
        player.sendMessage(ColorUtils.translate("  &a/o wand &7- Get arena setup wand"));
        player.sendMessage(ColorUtils.translate("  &a/o arenalist &7- List all arenas"));
        player.sendMessage(ColorUtils.translate("  &a/o debug <console|chat|both|off> &7- Toggle debug logging"));
        player.sendMessage(ColorUtils.translate("  &a/o cmds &7- Show this command list"));
    }

    private void handleArenalistCommand(Player player) {
        List<String> arenaNames = ArenaConfigManager.getArenaNames();

        if (arenaNames.isEmpty()) {
            player.sendMessage(ColorUtils.translate("&cNo arenas created yet."));
            return;
        }

        player.sendMessage(ColorUtils.translate("&6&l=== Arena List ==="));
        for (String arenaName : arenaNames) {
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            String statusColor;
            if (status.equals("READY") || status.equals("WAITING")) {
                statusColor = "&a";
            } else if (status.equals("STARTING")) {
                statusColor = "&6";
            } else if (status.equals("STARTED")) {
                statusColor = "&c";
            } else if (status.equals("PLAYING")) {
                statusColor = "&c";
            } else if (status.equals("DISABLED")) {
                statusColor = "&8";
            } else {
                statusColor = "&7";
            }
            int currentPlayers = ObsidianCommand.getArenaPlayerCount(arenaName);
            int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);

            player.sendMessage(ColorUtils.translate(statusColor + arenaName + " &7[" + currentPlayers + "/" + maxPlayers + "] &8- " + statusColor + status));
        }
    }

    private void handleTeamCommand(Player player) {
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            player.sendMessage(ColorUtils.translate(MessagesConfigManager.getMessage("join_first")));
            return;
        }

        // Check if countdown is 5 seconds or fewer - prevent team changes
        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        if (arenaName == null) {
            player.sendMessage(ColorUtils.translate(MessagesConfigManager.getMessage("not_in_arena")));
            return;
        }
        int countdown = GameManager.getCountdown(arenaName);

        if (countdown > 0 && countdown <= 5) {
            player.sendMessage(ColorUtils.translate("&cTeam switching is prohibited in the last 5 seconds!"));
            return;
        }

        ObsidianCommand.getInstance().openTeamSelectionGUI(player);
    }
}
