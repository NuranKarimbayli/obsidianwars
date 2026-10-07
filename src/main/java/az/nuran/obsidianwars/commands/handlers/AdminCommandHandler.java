package az.nuran.obsidianwars.commands.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.EconomyManager;
import az.nuran.obsidianwars.managers.KillStreaksConfigManager;
import az.nuran.obsidianwars.managers.LevelManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.ParticleManager;
import az.nuran.obsidianwars.managers.ResourceBlocksConfigManager;
import az.nuran.obsidianwars.managers.ScoreboardsConfigManager;
import az.nuran.obsidianwars.managers.SuddenDeathManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.models.PlayerLevel;
import az.nuran.obsidianwars.models.PlayerStats;
import az.nuran.obsidianwars.services.DebugManager;
import az.nuran.obsidianwars.services.DeathMessagesConfigManager;
import az.nuran.obsidianwars.services.PerformanceMonitor;
import az.nuran.obsidianwars.services.StatsDAO;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles admin commands: admin, reload, debug, disablearena, enablearena.
 */
public class AdminCommandHandler implements CommandHandler {

    private final Obsidianwars plugin;

    public AdminCommandHandler(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("admin")) {
            handleAdminCommand(player, args);
            return true;
        }

        if (subCommand.equals("reload")) {
            handleReloadCommand(player, args);
            return true;
        }

        if (subCommand.equals("debug")) {
            if (!player.hasPermission("obsidianwars.command.debug")) {
                String noPermMessage = plugin.getConfig().getString("no-permission-message", "&cYou don't have permission to use this command.");
                player.sendMessage(noPermMessage.replace("&", "§"));
                return true;
            }
            handleDebugCommand(player, args);
            return true;
        }

        if (subCommand.equals("disablearena")) {
            if (!player.hasPermission("obsidianwars.command.disablearena")) {
                String noPermMessage = plugin.getConfig().getString("no-permission-message", "&cYou don't have permission to use this command.");
                player.sendMessage(noPermMessage.replace("&", "§"));
                return true;
            }
            handleDisableArenaCommand(player, args);
            return true;
        }

        if (subCommand.equals("enablearena")) {
            if (!player.hasPermission("obsidianwars.command.enablearena")) {
                String noPermMessage = plugin.getConfig().getString("no-permission-message", "&cYou don't have permission to use this command.");
                player.sendMessage(noPermMessage.replace("&", "§"));
                return true;
            }
            handleEnableArenaCommand(player, args);
            return true;
        }

        return false;
    }

    private void handleAdminCommand(Player player, String[] args) {
        // Check permission
        if (!player.hasPermission("obsidianwars.command.admin")) {
            String noPermMessage = plugin.getConfig().getString("no-permission-message", "&cYou don't have permission to use this command.");
            player.sendMessage(noPermMessage.replace("&", "§"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /o admin <level|coin|exp|stats|gui> [action] [player] [amount]");
            return;
        }

        String category = args[1].toLowerCase();

        // Handle admin gui command
        if (category.equals("gui")) {
            ObsidianCommand.getInstance().openArenaSelectorGUI(player, true);
            return;
        }

        // Handle stats reset command
        if (category.equals("stats")) {
            if (args.length < 4 || !args[2].equalsIgnoreCase("reset")) {
                player.sendMessage("§cUsage: /o admin stats reset <player>");
                return;
            }
            handleAdminStatsResetCommand(player, args);
            return;
        }

        // Handle currency/level commands: level, coin, exp
        if (category.equals("level") || category.equals("coin") || category.equals("exp")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o admin " + category + " <set|add|remove> <player> <amount>");
                return;
            }

            String action = args[2].toLowerCase();

            if (args.length < 5) {
                player.sendMessage("§cUsage: /o admin " + category + " " + action + " <player> <amount>");
                return;
            }

            String targetName = args[3];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null || !target.isOnline()) {
                player.sendMessage("§cPlayer '" + targetName + "' is not online.");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[4]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount: " + args[4]);
                return;
            }

            java.util.UUID targetUuid = target.getUniqueId();

            // Handle level commands
            if (category.equals("level")) {
                handleLevelCommand(player, target, targetUuid, action, (int) amount);
            }
            // Handle exp commands
            else if (category.equals("exp")) {
                handleExpCommand(player, target, targetUuid, action, (int) amount);
            }
            // Handle coin commands
            else if (category.equals("coin")) {
                handleCoinCommand(player, target, action, amount);
            }
        } else {
            player.sendMessage("§cUnknown admin category: " + category);
            player.sendMessage("§cAvailable categories: level, coin, exp, stats, gui");
        }
    }

    private void handleLevelCommand(Player player, Player target, java.util.UUID targetUuid, String action, int amount) {
        PlayerLevel playerLevel = LevelManager.getPlayerLevel(targetUuid);

        switch (action) {
            case "set":
                playerLevel.setLevel(Math.max(1, amount));
                player.sendMessage("§aSet " + target.getName() + "'s level to " + amount);
                target.sendMessage("§eYour level has been set to " + amount + " by an admin!");
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            case "add":
                int newLevel = playerLevel.getLevel() + amount;
                playerLevel.setLevel(newLevel);
                player.sendMessage("§aAdded " + amount + " levels to " + target.getName() + " (now level " + newLevel + ")");
                target.sendMessage("§eYou received " + amount + " levels from an admin! (now level " + newLevel + ")");
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            case "remove":
                int currentLevel = playerLevel.getLevel();
                int reducedLevel = Math.max(1, currentLevel - amount);
                playerLevel.setLevel(reducedLevel);
                player.sendMessage("§aRemoved " + (currentLevel - reducedLevel) + " levels from " + target.getName() + " (now level " + reducedLevel + ")");
                target.sendMessage("§e" + (currentLevel - reducedLevel) + " levels were removed by an admin! (now level " + reducedLevel + ")");
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            default:
                player.sendMessage("§cUnknown action: " + action);
                player.sendMessage("§cAvailable actions: set, add, remove");
                break;
        }
    }

    private void handleExpCommand(Player player, Player target, java.util.UUID targetUuid, String action, int amount) {
        PlayerLevel xpLevel = LevelManager.getPlayerLevel(targetUuid);

        switch (action) {
            case "set":
                xpLevel.setXp(Math.max(0, amount));
                player.sendMessage("§aSet " + target.getName() + "'s XP to " + amount);
                target.sendMessage("§eYour XP has been set to " + amount + " by an admin!");
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            case "add":
                LevelManager.addXp(target, amount);
                player.sendMessage("§aAdded " + amount + " XP to " + target.getName());
                target.sendMessage("§eYou received " + amount + " XP from an admin!");
                break;

            case "remove":
                int currentXp = xpLevel.getCurrentXp();
                int newXP = Math.max(0, currentXp - amount);
                xpLevel.setXp(newXP);
                player.sendMessage("§aRemoved " + (currentXp - newXP) + " XP from " + target.getName());
                target.sendMessage("§e" + (currentXp - newXP) + " XP was removed by an admin!");
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            default:
                player.sendMessage("§cUnknown action: " + action);
                player.sendMessage("§cAvailable actions: set, add, remove");
                break;
        }
    }

    private void handleCoinCommand(Player player, Player target, String action, double amount) {
        if (!EconomyManager.getInstance().isEnabled()) {
            player.sendMessage("§cEconomy is not enabled!");
            return;
        }

        switch (action) {
            case "set":
                // For set, we need to calculate the difference
                double currentBalance = EconomyManager.getInstance().getBalance(target);
                double difference = amount - currentBalance;
                if (difference > 0) {
                    EconomyManager.getInstance().awardPlayer(target, difference, "admin set");
                } else if (difference < 0) {
                    EconomyManager.getInstance().withdrawPlayer(target, -difference);
                }
                player.sendMessage("§aSet " + target.getName() + "'s coins to " + String.format("%.2f", amount));
                target.sendMessage("§eYour coins have been set to " + String.format("%.2f", amount) + " by an admin!");
                break;

            case "add":
                EconomyManager.getInstance().awardPlayer(target, amount, "admin add");
                player.sendMessage("§aAdded " + String.format("%.2f", amount) + " coins to " + target.getName());
                target.sendMessage("§eYou received " + String.format("%.2f", amount) + " coins from an admin!");
                break;

            case "remove":
                boolean success = EconomyManager.getInstance().withdrawPlayer(target, amount);
                if (success) {
                    player.sendMessage("§aRemoved " + String.format("%.2f", amount) + " coins from " + target.getName());
                    target.sendMessage("§e" + String.format("%.2f", amount) + " coins were removed by an admin!");
                } else {
                    player.sendMessage("§c" + target.getName() + " doesn't have enough coins!");
                }
                break;

            default:
                player.sendMessage("§cUnknown action: " + action);
                player.sendMessage("§cAvailable actions: set, add, remove");
                break;
        }
    }

    private void handleAdminStatsResetCommand(Player player, String[] args) {
        if (!player.hasPermission("obsidianwars.command.admin")) {
            String noPermMessage = plugin.getConfig().getString("no-permission-message", "&cYou don't have permission to use this command.");
            player.sendMessage(noPermMessage.replace("&", "§"));
            return;
        }

        if (args.length < 4) {
            player.sendMessage("§cUsage: /o admin stats reset <player>");
            return;
        }

        String targetName = args[3];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage("§cPlayer not found: " + targetName);
            return;
        }

        // Reset player stats
        PlayerStats stats = az.nuran.obsidianwars.managers.StatsManager.getPlayerStats(target);
        if (stats != null) {
            stats.reset();
            az.nuran.obsidianwars.managers.StatsManager.savePlayerStats(target.getUniqueId(), target.getName());
            player.sendMessage("§aStatistics reset for player " + targetName);
            target.sendMessage("§cYour statistics have been reset by an admin.");
        } else {
            player.sendMessage("§cCould not find statistics for player " + targetName);
        }
    }

    private void handleReloadCommand(Player player, String[] args) {
        if (!player.hasPermission("obsidianwars.command.reload")) {
            String noPermMessage = plugin.getConfig().getString("no-permission-message", "&cYou don't have permission to use this command.");
            player.sendMessage(noPermMessage.replace("&", "§"));
            return;
        }

        // Check if any arena is in an active game state
        if (hasActiveGames()) {
            player.sendMessage("§cCannot reload configuration while games are in progress!");
            player.sendMessage("§ePlease wait for all games to finish, or use /o forceend <arena> to end games manually.");
            return;
        }

        // Determine what to reload
        String target = "all";
        if (args.length > 1) {
            target = args[1].toLowerCase();
        }

        player.sendMessage("§eReloading ObsidianWars configuration...");

        if (target.equals("all") || target.equals("config")) {
            // Reload main config
            plugin.reloadConfig();
            player.sendMessage("§aconfig.yml reloaded");

            // Reload messages config
            MessagesConfigManager.initialize();
            player.sendMessage("§amessages.yml reloaded");

            // Reload kill streaks config
            KillStreaksConfigManager.initialize();
            player.sendMessage("§akill-streaks.yml reloaded");

            // Reload death messages config
            DeathMessagesConfigManager.initialize();
            player.sendMessage("§adeath-messages.yml reloaded");

            // Reload resource blocks config
            ResourceBlocksConfigManager.initialize();
            player.sendMessage("§aresource-blocks.yml reloaded");

            // Reload scoreboards config
            ScoreboardsConfigManager.initialize();
            player.sendMessage("§ascoreboards.yml reloaded");

            // Reload particles config
            ParticleManager.loadConfig();
            player.sendMessage("§aparticles reloaded");

            // Reload economy config
            EconomyManager.getInstance().reload();
            player.sendMessage("§aeconomy configuration reloaded");

            // Reload sudden death config
            SuddenDeathManager.getInstance().reload();
            player.sendMessage("§asudden death configuration reloaded");

            // Reload GameManager config (countdown, timers)
            GameManager.loadConfig();
            player.sendMessage("§agame manager configuration reloaded");

            // Reload performance thresholds (if any)
            PerformanceMonitor.getInstance().reset();
            player.sendMessage("§aperformance metrics reset");
        }

        if (target.equals("all") || target.equals("arenas")) {
            // Reload arena config
            ArenaConfigManager.initialize();
            player.sendMessage("§aarenas.yml reloaded");
        }

        if (target.equals("all")) {
            player.sendMessage("§a§lConfiguration reloaded successfully!");
            player.sendMessage("§eNote: Some changes may require a server restart to take full effect.");
        } else {
            player.sendMessage("§a§l" + target + " reloaded successfully!");
        }
    }

    private void handleDebugCommand(Player player, String[] args) {
        // If no arguments, show current status and usage
        if (args.length == 1) {
            DebugManager.DebugMode currentMode = DebugManager.getDebugMode(player);
            player.sendMessage("§6§l=== Debug Mode Status ===");
            player.sendMessage("§eCurrent mode: §f" + currentMode);
            player.sendMessage("§7Usage: /o debug <console|chat|both|off>");
            player.sendMessage("§7  console - Log to server console only");
            player.sendMessage("§7  chat - Log to your chat only");
            player.sendMessage("§7  both - Log to both console and chat");
            player.sendMessage("§7  off - Disable debug logging");
            return;
        }

        String modeArg = args[1].toLowerCase();
        DebugManager.DebugMode newMode;

        switch (modeArg) {
            case "console":
                // Toggle console debug mode
                boolean currentConsoleState = DebugManager.isConsoleDebugEnabled();
                boolean newConsoleState = !currentConsoleState;
                DebugManager.setConsoleDebugEnabled(newConsoleState);
                DebugManager.setDebugMode(player, newConsoleState ? DebugManager.DebugMode.CONSOLE : DebugManager.DebugMode.OFF);
                player.sendMessage("§aConsole debug mode: §f" + (newConsoleState ? "ENABLED" : "DISABLED"));
                return;
            case "chat":
                // Toggle chat debug mode
                boolean currentChatState = DebugManager.isChatDebugEnabled();
                boolean newChatState = !currentChatState;
                DebugManager.setChatDebugEnabled(newChatState);
                DebugManager.setDebugMode(player, newChatState ? DebugManager.DebugMode.CHAT : DebugManager.DebugMode.OFF);
                player.sendMessage("§aChat debug mode: §f" + (newChatState ? "ENABLED" : "DISABLED"));
                return;
            case "both":
                newMode = DebugManager.DebugMode.BOTH;
                DebugManager.setConsoleDebugEnabled(true);
                DebugManager.setChatDebugEnabled(true);
                break;
            case "off":
                newMode = DebugManager.DebugMode.OFF;
                // Ensure both console and chat debug are disabled
                DebugManager.setConsoleDebugEnabled(false);
                DebugManager.setChatDebugEnabled(false);
                break;
            default:
                player.sendMessage("§cInvalid debug mode. Use: console, chat, both, or off");
                return;
        }

        DebugManager.setDebugMode(player, newMode);
        player.sendMessage("§aDebug mode set to: §f" + newMode);

        if (newMode != DebugManager.DebugMode.OFF) {
            player.sendMessage("§eDebug logging is now enabled. You will see detailed game events.");
        } else {
            player.sendMessage("§eDebug logging disabled.");
        }
    }

    private void handleDisableArenaCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o disableArena <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        ArenaConfigManager.setArenaStatus(arenaName, "DISABLED");
        player.sendMessage("§aArena " + arenaName + " has been disabled.");
    }

    private void handleEnableArenaCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o enableArena <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Check mandatory requirements before enabling
        List<String> missingRequirements = new ArrayList<>();

        // 1. Lobby spawn
        if (ArenaConfigManager.getLobbySpawn(arenaName) == null) {
            missingRequirements.add("Lobby spawn point (setlobby)");
        }

        // 2. Waiting lobby spawn
        if (ArenaConfigManager.getWaitingSpawn(arenaName) == null) {
            missingRequirements.add("Waiting lobby spawn (setwaitingspawn)");
        }

        // 3. Red Team spawn
        if (ArenaConfigManager.getTeamSpawn(arenaName, "red") == null) {
            missingRequirements.add("Red Team spawn (setspawn red)");
        }

        // 4. Blue Team spawn
        if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
            missingRequirements.add("Blue Team spawn (setspawn blue)");
        }

        // 5. Red Team obsidian
        if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
            missingRequirements.add("Red Team obsidian (setobsidian red)");
        }

        // 6. Blue Team obsidian
        if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
            missingRequirements.add("Blue Team obsidian (setobsidian blue)");
        }

        // 7. Resource blocks
        if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
            missingRequirements.add("Resource blocks (setblocks)");
        }

        // 8. Wall regions
        if (!ArenaConfigManager.hasWallConfiguration(arenaName, "red") || !ArenaConfigManager.hasWallConfiguration(arenaName, "blue")) {
            missingRequirements.add("Wall regions (setwall red & setwall blue)");
        }

        // 9. Spectator spawn
        if (ArenaConfigManager.getSpectatorSpawn(arenaName) == null) {
            missingRequirements.add("Spectator spawn (setspectspawn)");
        }

        // If requirements are missing, block activation
        if (!missingRequirements.isEmpty()) {
            player.sendMessage("§cArena cannot be enabled. Missing requirements:");
            for (String req : missingRequirements) {
                player.sendMessage("§c- " + req);
            }
            // Play error sound
            org.bukkit.Sound errorSound = Obsidianwars.parseSound("ENTITY_VILLAGER_NO");
            if (errorSound == null) {
                errorSound = org.bukkit.Sound.valueOf("VILLAGER_NO");
            }
            if (errorSound != null) {
                player.playSound(player.getLocation(), errorSound, 1.0f, 1.0f);
            }
            return;
        }

        ArenaConfigManager.setArenaStatus(arenaName, "READY");
        player.sendMessage("§aArena " + arenaName + " has been enabled.");
    }

    private boolean hasActiveGames() {
        for (String arenaName : ArenaConfigManager.getArenaNames()) {
            ArenaStateManager.ArenaState state = ArenaStateManager.getInstance().getState(arenaName);
            if (state != null && (state == ArenaStateManager.ArenaState.STARTING ||
                               state == ArenaStateManager.ArenaState.PREPARATION ||
                               state == ArenaStateManager.ArenaState.PLAYING ||
                               state == ArenaStateManager.ArenaState.SUDDEN_DEATH)) {
                return true;
            }
        }
        return false;
    }
}
