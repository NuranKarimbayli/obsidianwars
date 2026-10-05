package az.nuran.obsidianwars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ObsidianTabCompleter implements TabCompleter {

    private final Obsidianwars plugin;

    public ObsidianTabCompleter(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Handle alias commands
        if (command.getName().equalsIgnoreCase("join")) {
            if (args.length == 1) {
                return filterCompletions(getArenaNames(), args[0]);
            }
            return completions;
        }

        if (command.getName().equalsIgnoreCase("stats")) {
            if (args.length == 1) {
                // Return online player names
                return filterCompletions(getOnlinePlayerNames(), args[0]);
            }
            return completions;
        }

        if (command.getName().equalsIgnoreCase("spectate")) {
            if (args.length == 1) {
                // Return both active playing players and arena names
                List<String> suggestions = new ArrayList<>();
                suggestions.addAll(getActivePlayingPlayerNames());
                suggestions.addAll(getArenaNames());
                return filterCompletions(suggestions, args[0]);
            }
            return completions;
        }

        // Handle /o or /obsidian commands
        if (args.length == 1) {
            // First argument: subcommands
            List<String> subcommands = Arrays.asList(
                "play", "join", "leave", "rejoin", "stats", "gui", "cmds", "arenalist",
                "team", "wand", "arena", "create", "delete", "force", "forceend", "forcestart", "forceprep",
                "disableArena", "enableArena", "spectate", "admin", "debug"
            );
            return filterCompletions(subcommands, args[0]);
        }

        // ==================== ARENA SUBCOMMANDS ====================
        if (args[0].equalsIgnoreCase("arena") && args.length >= 2) {
            return handleArenaCompletions(args);
        }

        // ==================== CREATE COMMAND ====================
        if (args[0].equalsIgnoreCase("create") && args.length == 2) {
            return filterCompletions(Arrays.asList("arena"), args[1]);
        }

        if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("arena") && args.length == 3) {
            // For creating a new arena, show existing arena names to help avoid naming conflicts
            return filterCompletions(getArenaNames(), args[2]);
        }

        if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("arena") && args.length == 4) {
            return Arrays.asList("<min>");
        }

        if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("arena") && args.length == 5) {
            return Arrays.asList("<max>");
        }

        // ==================== DELETE COMMAND ====================
        if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
            return filterCompletions(Arrays.asList("arena"), args[1]);
        }

        if (args[0].equalsIgnoreCase("delete") && args[1].equalsIgnoreCase("arena") && args.length == 3) {
            return filterCompletions(getArenaNames(), args[2]);
        }

        // ==================== FORCEEND / FORCESTART / FORCEPREP COMMANDS ====================
        if ((args[0].equalsIgnoreCase("forceend") || args[0].equalsIgnoreCase("forcestart") || args[0].equalsIgnoreCase("forceprep")) && args.length == 2) {
            return filterCompletions(getArenaNames(), args[1]);
        }

        // ==================== FORCE COMMAND (NEW UNIFIED) ====================
        if (args[0].equalsIgnoreCase("force")) {
            if (args.length == 2) {
                return filterCompletions(Arrays.asList("start", "end"), args[1]);
            }
            if (args.length == 3) {
                String action = args[1].toLowerCase();
                if (action.equals("start")) {
                    return filterCompletions(Arrays.asList("arena"), args[2]);
                } else if (action.equals("end")) {
                    return filterCompletions(Arrays.asList("arena", "preperation"), args[2]);
                }
            }
            if (args.length == 4) {
                String action = args[1].toLowerCase();
                String subAction = args[2].toLowerCase();
                if ((action.equals("start") && subAction.equals("arena")) ||
                    (action.equals("end") && (subAction.equals("arena") || subAction.equals("preperation")))) {
                    return filterCompletions(getArenaNames(), args[3]);
                }
            }
        }

        // ==================== DISABLE/ENABLE ARENA COMMANDS ====================
        if ((args[0].equalsIgnoreCase("disableArena") || args[0].equalsIgnoreCase("enableArena")) && args.length == 2) {
            return filterCompletions(getArenaNames(), args[1]);
        }

        // ==================== STATS COMMAND ====================
        if (args[0].equalsIgnoreCase("stats") && args.length == 2) {
            return filterCompletions(getOnlinePlayerNames(), args[1]);
        }

        // ==================== PLAY/JOIN COMMAND ====================
        if ((args[0].equalsIgnoreCase("play") || args[0].equalsIgnoreCase("join")) && args.length == 2) {
            return filterCompletions(getArenaNames(), args[1]);
        }

        // ==================== SPECTATE COMMAND ====================
        if (args[0].equalsIgnoreCase("spectate") && args.length == 2) {
            // Return both active playing players and arena names
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(getActivePlayingPlayerNames());
            suggestions.addAll(getArenaNames());
            return filterCompletions(suggestions, args[1]);
        }

        // ==================== ADMIN COMMAND ====================
        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) {
                // Suggest admin subcommands
                List<String> adminSubcommands = Arrays.asList(
                    "addexp", "addlevel", "removexp", "removelevel", "setexp", "setlevel"
                );
                return filterCompletions(adminSubcommands, args[1]);
            }
            if (args.length == 3) {
                // Suggest player names
                return filterCompletions(getOnlinePlayerNames(), args[2]);
            }
            if (args.length == 4) {
                // Suggest amount placeholder
                return Arrays.asList("<amount>");
            }
        }

        // ==================== DEBUG COMMAND ====================
        if (args[0].equalsIgnoreCase("debug")) {
            if (args.length == 2) {
                // Suggest debug modes
                List<String> debugModes = Arrays.asList("console", "chat", "both", "off");
                return filterCompletions(debugModes, args[1]);
            }
        }

        return completions;
    }

    private List<String> handleArenaCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Second argument: arena subcommands
            List<String> arenaActions = Arrays.asList(
                "setlobby", "setwaitingspawn", "setwaitingregion", "setplayers", "setspawn", "setobsidian",
                "setblocks", "setwall", "settimer", "setmobarea", "settimelimit", "setspectspawn", "finish"
            );
            return filterCompletions(arenaActions, args[1]);
        }

        if (args.length == 3) {
            // Third argument: arena name (for most subcommands)
            String action = args[1].toLowerCase();
            if (Arrays.asList("setlobby", "setwaitingspawn", "setwaitingregion", "setplayers", "setspawn", "setobsidian",
                    "setblocks", "setwall", "settimer", "setmobarea", "settimelimit", "setspectspawn", "finish").contains(action)) {
                return filterCompletions(getArenaNames(), args[2]);
            }
        }

        // setspawn: 4th argument = team color
        if (args.length == 4 && args[1].equalsIgnoreCase("setspawn")) {
            return filterCompletions(Arrays.asList("red", "blue"), args[3]);
        }

        // setobsidian: 4th argument = team color
        if (args.length == 4 && args[1].equalsIgnoreCase("setobsidian")) {
            return filterCompletions(Arrays.asList("red", "blue"), args[3]);
        }

        // setwall: 4th argument = team color
        if (args.length == 4 && args[1].equalsIgnoreCase("setwall")) {
            return filterCompletions(Arrays.asList("red", "blue"), args[3]);
        }

        // setmobarea: 4th argument = team color
        if (args.length == 4 && args[1].equalsIgnoreCase("setmobarea")) {
            return filterCompletions(Arrays.asList("red", "blue"), args[3]);
        }

        // setplayers: 4th argument = min_players
        if (args.length == 4 && args[1].equalsIgnoreCase("setplayers")) {
            return Arrays.asList("<min>");
        }

        // setplayers: 5th argument = max_players
        if (args.length == 5 && args[1].equalsIgnoreCase("setplayers")) {
            return Arrays.asList("<max>");
        }

        // settimer: 4th argument = preparation_time
        if (args.length == 4 && args[1].equalsIgnoreCase("settimer")) {
            return Arrays.asList("<prep_time>");
        }

        // settimer: 5th argument = sudden_death_time (optional)
        if (args.length == 5 && args[1].equalsIgnoreCase("settimer")) {
            return Arrays.asList("<sudden_death>");
        }

        // settimelimit: 4th argument = time_limit
        if (args.length == 4 && args[1].equalsIgnoreCase("settimelimit")) {
            return Arrays.asList("<time_limit>");
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return options;
        }
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getArenaNames() {
        return ArenaConfigManager.getArenaNames();
    }

    private List<String> getOnlinePlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        return playerNames;
    }

    private List<String> getActivePlayingPlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (java.util.Map.Entry<java.util.UUID, String> entry : ObsidianCommand.playersInArena.entrySet()) {
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                playerNames.add(player.getName());
            }
        }
        return playerNames;
    }
}
