package az.nuran.obsidianwars.commands.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.handlers.WandListener;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.MobSpawnerManager;
import az.nuran.obsidianwars.managers.ResourceBlockManager;
import az.nuran.obsidianwars.managers.TeamManager;
import az.nuran.obsidianwars.managers.WallManager;
import az.nuran.obsidianwars.services.GameFeedbackService;
import az.nuran.obsidianwars.services.PlayerUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles arena setup commands: arena subcommands, create, delete, wand.
 * Note: This is a partial extraction - the full arena setup wizard is complex
 * and could be further refactored into smaller sub-handlers.
 */
public class ArenaSetupCommandHandler implements CommandHandler {

    private final Obsidianwars plugin;

    public ArenaSetupCommandHandler(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("arena")) {
            if (args.length < 2) {
                player.sendMessage(MessagesConfigManager.getMessage("usage_arena"));
                return true;
            }
            handleArenaCommand(player, args);
            return true;
        }

        if (subCommand.equals("create")) {
            handleCreateCommand(player, args);
            return true;
        }

        if (subCommand.equals("delete")) {
            handleDeleteCommand(player, args);
            return true;
        }

        if (subCommand.equals("wand")) {
            handleWandCommand(player);
            return true;
        }

        return false;
    }

    private void handleArenaCommand(Player player, String[] args) {
        String action = args[1].toLowerCase();

        // Simplified arena setup - only basic setup commands
        // Full arena setup wizard could be further refactored
        if (action.equals("teleport")) {
            handleArenaTeleportCommand(player, args);
        } else if (action.equals("finish")) {
            handleArenaFinishCommand(player, args);
        } else {
            // For now, delegate back to ObsidianCommand for complex setup
            // This can be further refactored later
            // We'll use the original implementation directly here
            handleArenaSetupSubcommands(player, args);
        }
    }

    /**
     * Handles complex arena setup subcommands.
     * This is a temporary method during refactoring.
     */
    private void handleArenaSetupSubcommands(Player player, String[] args) {
        String action = args[1].toLowerCase();

        if (action.equals("setlobby")) {
            handleSetLobby(player, args);
        } else if (action.equals("setwaitingspawn")) {
            handleSetWaitingSpawn(player, args);
        } else if (action.equals("setwaitingregion")) {
            handleSetWaitingRegion(player, args);
        } else if (action.equals("setplayers")) {
            handleSetPlayers(player, args);
        } else if (action.equals("setspawn")) {
            handleSetSpawn(player, args);
        } else if (action.equals("setobsidian")) {
            handleSetObsidian(player, args);
        } else if (action.equals("setblocks")) {
            handleSetBlocks(player, args);
        } else if (action.equals("setwall")) {
            handleSetWall(player, args);
        } else if (action.equals("settimer")) {
            handleSetTimer(player, args);
        } else if (action.equals("setmobarea")) {
            handleSetMobArea(player, args);
        } else if (action.equals("setspectspawn")) {
            handleSetSpectSpawn(player, args);
        } else {
            player.sendMessage("§cUnknown arena subcommand. Use /o cmds for help.");
        }
    }

    private void handleSetLobby(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena setlobby <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setLobbySpawn(arenaName, loc);

        player.sendMessage(MessagesConfigManager.getMessage("lobby_spawn_set", "arenaName", arenaName));
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "lobby");
    }

    private void handleSetWaitingSpawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena setwaitingspawn <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setWaitingSpawn(arenaName, loc);

        player.sendMessage("§aWaiting lobby spawn set for arena " + arenaName + "!");
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "waiting");
    }

    private void handleSetWaitingRegion(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena setwaitingregion <arenaName>");
            player.sendMessage("§eSelect the waiting area bounds with your wand (Pos1 and Pos2) first!");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
        Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            player.sendMessage(MessagesConfigManager.getMessage("wand_need_positions"));
            player.sendMessage("§eUse the wand to select Corner 1 (left-click) and Corner 2 (right-click) of the waiting area.");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        ArenaConfigManager.setWaitingRegion(arenaName, pos1, pos2);

        player.sendMessage("§aWaiting region set for arena " + arenaName + "!");
        player.sendMessage("§ePlayers will be teleported back if they leave this area during the waiting phase.");
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "waitingregion");
    }

    private void handleSetPlayers(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /o arena setplayers <arenaName> <min> <max>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        int min, max;
        try {
            min = Integer.parseInt(args[3]);
            max = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number format! Usage: /o arena setplayers <arena> <min> <max>");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (min < 2) {
            player.sendMessage("§cMinimum players must be at least 2!");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (max < min) {
            player.sendMessage("§cMaximum players must be greater than or equal to minimum players!");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        ArenaConfigManager.setPlayerLimits(arenaName, min, max);

        player.sendMessage(MessagesConfigManager.getMessage("player_limits_set", "arenaName", arenaName, "min", String.valueOf(min), "max", String.valueOf(max)));
    }

    private void handleSetSpawn(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /o arena setspawn <arenaName> <red|blue>");
            return;
        }
        String arenaName = args[2];
        String teamColor = args[3].toLowerCase();

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        if (!teamColor.equals("red") && !teamColor.equals("blue")) {
            player.sendMessage(MessagesConfigManager.getMessage("invalid_team_color"));
            GameFeedbackService.playErrorSound(player);
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setTeamSpawn(arenaName, teamColor, loc);

        String teamName = teamColor.equals("red") ? "Red" : "Blue";
        String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
        player.sendMessage(MessagesConfigManager.getMessage("team_spawn_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName));
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, teamColor + "spawn");
    }

    private void handleSetObsidian(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /o arena setobsidian <arenaName> <red|blue>");
            return;
        }
        String arenaName = args[2];
        String teamColor = args[3].toLowerCase();

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        if (!teamColor.equals("red") && !teamColor.equals("blue")) {
            player.sendMessage(MessagesConfigManager.getMessage("invalid_team_color"));
            GameFeedbackService.playErrorSound(player);
            return;
        }

        org.bukkit.block.Block targetBlock = player.rayTraceBlocks(5) != null ? player.rayTraceBlocks(5).getHitBlock() : null;
        if (targetBlock == null || targetBlock.getType() != Material.OBSIDIAN) {
            player.sendMessage(MessagesConfigManager.getMessage("invalid_obsidian_target"));
            GameFeedbackService.playErrorSound(player);
            return;
        }

        Location obsidianLoc = targetBlock.getLocation();
        ArenaConfigManager.setObsidianLocation(arenaName, teamColor, obsidianLoc);

        String teamName = teamColor.equals("red") ? "Red" : "Blue";
        String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
        player.sendMessage(MessagesConfigManager.getMessage("obsidian_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName));
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, teamColor + "obsidian");
    }

    private void handleSetBlocks(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena setblocks <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // If player is already in setup mode, exit them
        if (ResourceBlockManager.isInSetupMode(player)) {
            ResourceBlockManager.exitSetupMode(player);
            return;
        }

        // Enter setup mode
        ResourceBlockManager.enterSetupMode(player, arenaName);
    }

    private void handleSetWall(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /o arena setwall <arenaName> <red|blue>");
            return;
        }
        String arenaName = args[2];
        String teamColor = args[3].toLowerCase();

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        if (!teamColor.equals("red") && !teamColor.equals("blue")) {
            player.sendMessage(MessagesConfigManager.getMessage("invalid_team_color"));
            GameFeedbackService.playErrorSound(player);
            return;
        }

        Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
        Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            player.sendMessage(MessagesConfigManager.getMessage("wand_need_positions"));
            GameFeedbackService.playErrorSound(player);
            return;
        }

        WallManager.setWall(arenaName, teamColor, pos1, pos2);

        String teamName = teamColor.equals("red") ? "Red" : "Blue";
        String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
        player.sendMessage(MessagesConfigManager.getMessage("wall_set", "teamColor", teamColorCode, "teamName", teamName));
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, teamColor + "wall");
    }

    private void handleSetTimer(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /o arena settimer <arenaName> <preparation_time> [sudden_death_time]");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        int prepMinutes;
        try {
            prepMinutes = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cError: Preparation time must be a number!");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (prepMinutes < 1) {
            player.sendMessage("§cPreparation time must be at least 1 minute!");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        WallManager.setTimer(arenaName, prepMinutes);
        player.sendMessage(MessagesConfigManager.getMessage("timer_set", "arenaName", arenaName, "minutes", String.valueOf(prepMinutes)));

        // Optional sudden death timer
        if (args.length >= 5) {
            int suddenDeathMinutes;
            try {
                suddenDeathMinutes = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cError: Sudden death time must be a number!");
                GameFeedbackService.playErrorSound(player);
                return;
            }

            if (suddenDeathMinutes < 1) {
                player.sendMessage("§cSudden death time must be at least 1 minute!");
                GameFeedbackService.playErrorSound(player);
                return;
            }

            ArenaConfigManager.setSuddenDeathTimer(arenaName, suddenDeathMinutes);
            player.sendMessage("§aSudden death time set to " + suddenDeathMinutes + " minutes!");
        }
    }

    private void handleSetMobArea(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena setmobarea <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        Location loc = player.getLocation();
        MobSpawnerManager.setMobArea(arenaName, loc);

        player.sendMessage("§aMob spawn area set for arena " + arenaName + " at your current location!");
        player.sendMessage("§eWither Skeletons will spawn every 2 minutes during combat phase.");
    }

    private void handleSetSpectSpawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena setspectspawn <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setSpectatorSpawn(arenaName, loc);

        player.sendMessage("§aSpectator spawn location set for arena " + arenaName + "!");
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "spectator");
    }

    private void handleArenaTeleportCommand(Player player, String[] args) {
        if (!player.hasPermission("obsidianwars.admin")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena teleport <arena>");
            return;
        }

        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        Location spectSpawn = ArenaConfigManager.getSpectatorSpawn(arenaName);
        if (spectSpawn == null) {
            player.sendMessage("§cSpectator spawn not set for arena " + arenaName);
            return;
        }

        player.teleport(spectSpawn);
        player.sendMessage("§aTeleported to arena " + arenaName + " spectator spawn");
    }

    private void handleArenaFinishCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena finish <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Checklist
        List<String> missingRequirements = new ArrayList<>();

        // 1. Min/Max player limit
        if (ArenaConfigManager.getMinPlayers(arenaName) == 0 || ArenaConfigManager.getMaxPlayers(arenaName) == 0) {
            missingRequirements.add("Player limits (setplayers)");
        }

        // 2. Lobby spawn point
        if (ArenaConfigManager.getLobbySpawn(arenaName) == null) {
            missingRequirements.add("Lobby spawn point (setlobby)");
        }

        // 3. Waiting lobby spawn
        if (ArenaConfigManager.getWaitingSpawn(arenaName) == null) {
            missingRequirements.add("Waiting lobby spawn (setwaitingspawn)");
        }

        // 4. Red Team spawn
        if (ArenaConfigManager.getTeamSpawn(arenaName, "red") == null) {
            missingRequirements.add("Red Team spawn (setspawn red)");
        }

        // 5. Blue Team spawn
        if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
            missingRequirements.add("Blue Team spawn (setspawn blue)");
        }

        // 6. Red Team obsidian
        if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
            missingRequirements.add("Red Team obsidian (setobsidian red)");
        }

        // 7. Blue Team obsidian
        if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
            missingRequirements.add("Blue Team obsidian (setobsidian blue)");
        }

        // 8. Resource blocks
        if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
            missingRequirements.add("Resource blocks (setblocks)");
        }

        // 9. Wall regions (both red and blue)
        if (!ArenaConfigManager.hasWallConfiguration(arenaName, "red") || !ArenaConfigManager.hasWallConfiguration(arenaName, "blue")) {
            missingRequirements.add("Wall regions (setwall red & setwall blue)");
        }

        // 10. Spectator spawn
        if (ArenaConfigManager.getSpectatorSpawn(arenaName) == null) {
            missingRequirements.add("Spectator spawn (setspectspawn)");
        }

        // If requirements are missing, show error
        if (!missingRequirements.isEmpty()) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_incomplete"));
            for (String req : missingRequirements) {
                player.sendMessage("§c- " + req);
            }
            // Play error sound
            Sound errorSound = Obsidianwars.parseSound("ENTITY_VILLAGER_NO");
            if (errorSound == null) {
                errorSound = Sound.valueOf("VILLAGER_NO");
            }
            if (errorSound != null) {
                player.playSound(player.getLocation(), errorSound, 1.0f, 1.0f);
            }
            return;
        }

        // Check for optional features
        List<String> optionalFeatures = new ArrayList<>();
        if (ArenaConfigManager.getMobAreaLocation(arenaName) == null) {
            optionalFeatures.add("Mob spawn area (setmobarea) - Optional");
        }

        // All checks passed, set status to READY
        ArenaConfigManager.setArenaStatus(arenaName, "READY");

        player.sendMessage(MessagesConfigManager.getMessage("arena_finished", "arenaName", arenaName));
        player.sendMessage("§aArena is now ready for players to use.");
        player.sendMessage("§eResource block count: " + ArenaConfigManager.getResourceBlockCount(arenaName));

        if (!optionalFeatures.isEmpty()) {
            player.sendMessage("§eOptional features:");
            for (String feature : optionalFeatures) {
                player.sendMessage("§7- " + feature);
            }
        }

        // Play success sound
        Sound successSound = Obsidianwars.parseSound("ENTITY_PLAYER_LEVELUP");
        if (successSound == null) {
            successSound = Sound.valueOf("LEVEL_UP");
        }
        if (successSound != null) {
            player.playSound(player.getLocation(), successSound, 1.0f, 1.0f);
        }
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage("§cUsage: /o create arena <arena_name> <min_players> <max_players>");
            return;
        }

        String arenaName = args[2];
        int minPlayers, maxPlayers;

        try {
            minPlayers = Integer.parseInt(args[3]);
            maxPlayers = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid numbers! Min and max must be integers.");
            return;
        }

        if (minPlayers < 2) {
            player.sendMessage("§cMinimum players must be at least 2!");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (maxPlayers < minPlayers) {
            player.sendMessage("§cMaximum players must be >= minimum players!");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_already_exists"));
            GameFeedbackService.playErrorSound(player);
            return;
        }

        // Create arena config with parameters immediately (no wand positions required)
        ArenaConfigManager.createArenaConfigWithParams(arenaName, minPlayers, maxPlayers);

        player.sendMessage(MessagesConfigManager.getMessage("arena_created", "arenaName", arenaName));
        player.sendMessage("§ePlayer limits set: Min " + minPlayers + ", Max " + maxPlayers);
        player.sendMessage("§eDefault timers set: Preparation 10min, Game Time 30min, Sudden Death 15min");
        player.sendMessage("§e§l=== Arena Setup Guide ===");
        player.sendMessage("§eFollow the steps below to complete arena setup:");
        player.sendMessage("§7Step 1: Set main lobby spawn");
        player.sendMessage("§7Step 2: Set waiting lobby spawn");
        player.sendMessage("§7Step 3: Set waiting region boundary (optional but recommended)");
        player.sendMessage("§7Step 4: Set team spawns (red & blue)");
        player.sendMessage("§7Step 5: Set obsidian targets (red & blue)");
        player.sendMessage("§7Step 6: Setup resource blocks");
        player.sendMessage("§7Step 7: Set wall regions (red & blue)");
        player.sendMessage("§7Step 8: Set spectator spawn");
        player.sendMessage("§7Step 9: Finish arena setup");
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName);
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o delete arena <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Delete arena
        ArenaConfigManager.deleteArenaConfig(arenaName);

        // Remove game from GameManager
        GameManager.removeGame(arenaName);

        // Clear players in arena
        for (java.util.UUID uuid : new ArrayList<>(ObsidianCommand.playersInArena.keySet())) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    // Remove player from arena
                    ObsidianCommand.playersInArena.remove(uuid);
                    TeamManager.removePlayerFromTeam(arenaPlayer);

                    // Reset player state and teleport to spawn
                    Location mainSpawn = arenaPlayer.getWorld().getSpawnLocation();
                    PlayerUtils.resetPlayerFull(arenaPlayer, mainSpawn);

                    arenaPlayer.sendMessage("§cArena deleted, you have been removed from the arena!");

                    // Update lobby scoreboard
                    az.nuran.obsidianwars.managers.LobbyScoreboardManager.updateLobbyScoreboard(arenaPlayer);
                }
            }
        }

        player.sendMessage(MessagesConfigManager.getMessage("arena_deleted", "arenaName", arenaName));
    }

    private void handleWandCommand(Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Obsidian Wars Wand");
            meta.setLore(Collections.singletonList("§eLeft and right click on blocks to select corners."));
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        player.sendMessage(MessagesConfigManager.getMessage("wand_received"));
    }
}
