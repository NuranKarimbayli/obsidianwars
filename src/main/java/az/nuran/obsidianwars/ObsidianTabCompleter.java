package az.nuran.obsidianwars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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

        if (args.length == 1) {
            // Birinji Tab basylanda görünjek sub-komandalar
            List<String> subcommands = Arrays.asList("play", "team", "wand", "arena", "leave");
            return filterCompletions(subcommands, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("arena")) {
            // "/obsidian arena " ýazylanda görünjek opsiýalar
            List<String> arenaActions = Arrays.asList("create", "setlobby", "setplayers", "setspawn", "setobsidian", "setblocks", "setwall", "settimer", "setmobarea", "settimelimit", "delete", "finish", "forcestart", "end");
            return filterCompletions(arenaActions, args[1]);
        }

        // Arena adları üçün avtomatik tamamlaşdırma (3-cü arqument)
        if (args.length == 3 && args[0].equalsIgnoreCase("arena")) {
            String action = args[1].toLowerCase();
            if (Arrays.asList("setlobby", "setplayers", "setspawn", "setobsidian", "setblocks", "setwall", "settimer", "setmobarea", "delete", "finish", "forcestart", "end").contains(action)) {
                List<String> arenaNames = getArenaNames();
                return filterCompletions(arenaNames, args[2]);
            }
        }

        // setspawn üçün komanda seçimi (4-cü arqument)
        if (args.length == 4 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("setspawn")) {
            List<String> teams = Arrays.asList("red", "blue");
            return filterCompletions(teams, args[3]);
        }

        // setobsidian üçün komanda seçimi (4-cü arqument)
        if (args.length == 4 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("setobsidian")) {
            List<String> teams = Arrays.asList("red", "blue");
            return filterCompletions(teams, args[3]);
        }

        // setwall üçün komanda seçimi (4-cü arqument)
        if (args.length == 4 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("setwall")) {
            List<String> teams = Arrays.asList("red", "blue");
            return filterCompletions(teams, args[3]);
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
}