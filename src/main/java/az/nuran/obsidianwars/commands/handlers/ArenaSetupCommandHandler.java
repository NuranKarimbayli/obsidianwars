package az.nuran.obsidianwars.commands.handlers;


import az.nuran.obsidianwars.util.ColorUtils;
import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.handlers.MovementListener;
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

import org.bukkit.Bukkit;
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
            if (!player.hasPermission("obsidianwars.command.arena")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            if (args.length < 2) {
                LanguageManager.getInstance().sendMessage(player, "usage_arena");
                return true;
            }
            handleArenaCommand(player, args);
            return true;
        }

        if (subCommand.equals("create")) {
            if (!player.hasPermission("obsidianwars.command.create")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            handleCreateCommand(player, args);
            return true;
        }

        if (subCommand.equals("delete")) {
            if (!player.hasPermission("obsidianwars.command.delete")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            handleDeleteCommand(player, args);
            return true;
        }

        if (subCommand.equals("wand")) {
            if (!player.hasPermission("obsidianwars.command.wand")) {
                LanguageManager.getInstance().sendMessage(player, "no_permission");
                return true;
            }
            handleWandCommand(player);
            return true;
        }

        return false;
    }

    private void handleWandCommand(Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.translate("&6Obsidian Wars Wand"));
            meta.setLore(ColorUtils.translate(Collections.singletonList("&eLeft and right click on blocks to select corners.")));
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        LanguageManager.getInstance().sendMessage(player, "wand_received");
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
            LanguageManager.getInstance().sendMessage(player, "unknown_arena_subcommand");
        }
    }

    private void handleSetLobby(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_setlobby");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setLobbySpawn(arenaName, loc);

        // Rebuild arena world cache for MovementListener
        MovementListener.rebuildArenaWorldCache();

        LanguageManager.getInstance().sendMessage(player, "lobby_spawn_set", "arenaName", arenaName);
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "lobby");
    }

    private void handleSetWaitingSpawn(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_setwaitingspawn");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setWaitingSpawn(arenaName, loc);

        LanguageManager.getInstance().sendMessage(player, "waiting_spawn_set", "arenaName", arenaName);
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "waiting");
    }

    private void handleSetWaitingRegion(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_setwaitingregion");
            LanguageManager.getInstance().sendMessage(player, "wand_select_bounds");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
        Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            LanguageManager.getInstance().sendMessage(player, "wand_need_positions");
            LanguageManager.getInstance().sendMessage(player, "wand_usage_corners");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        ArenaConfigManager.setWaitingRegion(arenaName, pos1, pos2);

        LanguageManager.getInstance().sendMessage(player, "waiting_region_set", "arenaName", arenaName);
        LanguageManager.getInstance().sendMessage(player, "waiting_region_info");
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "waitingregion");
    }

    private void handleSetPlayers(Player player, String[] args) {
        if (args.length < 4) {
            LanguageManager.getInstance().sendMessage(player, "usage_setplayers");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        int min, max;
        try {
            min = Integer.parseInt(args[3]);
            max = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            LanguageManager.getInstance().sendMessage(player, "invalid_number_format");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (min < 2) {
            LanguageManager.getInstance().sendMessage(player, "min_players_at_least_2");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (max < min) {
            LanguageManager.getInstance().sendMessage(player, "max_players_greater_than_min");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        ArenaConfigManager.setPlayerLimits(arenaName, min, max);

        LanguageManager.getInstance().sendMessage(player, "player_limits_set", "arenaName", arenaName, "min", String.valueOf(min), "max", String.valueOf(max));
    }

    private void handleSetSpawn(Player player, String[] args) {
        if (args.length < 4) {
            LanguageManager.getInstance().sendMessage(player, "usage_setspawn");
            return;
        }
        String arenaName = args[2];
        String teamColor = args[3].toLowerCase();

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        if (!teamColor.equals("red") && !teamColor.equals("blue")) {
            LanguageManager.getInstance().sendMessage(player, "invalid_team_color");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setTeamSpawn(arenaName, teamColor, loc);

        // Rebuild arena world cache for MovementListener
        MovementListener.rebuildArenaWorldCache();

        String teamName = teamColor.equals("red") ? "Red" : "Blue";
        String teamColorCode = teamColor.equals("red") ? "&c" : "&9";
        LanguageManager.getInstance().sendMessage(player, "team_spawn_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName);
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, teamColor + "spawn");
    }

    private void handleSetObsidian(Player player, String[] args) {
        if (args.length < 4) {
            LanguageManager.getInstance().sendMessage(player, "usage_setobsidian");
            return;
        }
        String arenaName = args[2];
        String teamColor = args[3].toLowerCase();

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        if (!teamColor.equals("red") && !teamColor.equals("blue")) {
            LanguageManager.getInstance().sendMessage(player, "invalid_team_color");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        org.bukkit.block.Block targetBlock = player.rayTraceBlocks(5) != null ? player.rayTraceBlocks(5).getHitBlock() : null;
        if (targetBlock == null || targetBlock.getType() != Material.OBSIDIAN) {
            LanguageManager.getInstance().sendMessage(player, "invalid_obsidian_target");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        Location obsidianLoc = targetBlock.getLocation();
        ArenaConfigManager.setObsidianLocation(arenaName, teamColor, obsidianLoc);

        String teamName = teamColor.equals("red") ? "Red" : "Blue";
        String teamColorCode = teamColor.equals("red") ? "&c" : "&9";
        LanguageManager.getInstance().sendMessage(player, "obsidian_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName);
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, teamColor + "obsidian");
    }

    private void handleSetBlocks(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_setblocks");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
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
            LanguageManager.getInstance().sendMessage(player, "usage_setwall");
            return;
        }
        String arenaName = args[2];
        String teamColor = args[3].toLowerCase();

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        if (!teamColor.equals("red") && !teamColor.equals("blue")) {
            LanguageManager.getInstance().sendMessage(player, "invalid_team_color");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
        Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            LanguageManager.getInstance().sendMessage(player, "wand_need_positions");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        WallManager.setWall(arenaName, teamColor, pos1, pos2);

        String teamName = teamColor.equals("red") ? "Red" : "Blue";
        String teamColorCode = teamColor.equals("red") ? "&c" : "&9";
        LanguageManager.getInstance().sendMessage(player, "wall_set", "teamColor", teamColorCode, "teamName", teamName);
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, teamColor + "wall");
    }

    private void handleSetTimer(Player player, String[] args) {
        if (args.length < 4) {
            LanguageManager.getInstance().sendMessage(player, "usage_settimer");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        int prepMinutes;
        try {
            prepMinutes = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            LanguageManager.getInstance().sendMessage(player, "preparation_time_must_be_number");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (prepMinutes < 1) {
            LanguageManager.getInstance().sendMessage(player, "preparation_time_at_least_1");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        WallManager.setTimer(arenaName, prepMinutes);
        LanguageManager.getInstance().sendMessage(player, "timer_set", "arenaName", arenaName, "minutes", String.valueOf(prepMinutes));

        // Optional sudden death timer
        if (args.length >= 5) {
            int suddenDeathMinutes;
            try {
                suddenDeathMinutes = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                LanguageManager.getInstance().sendMessage(player, "sudden_death_time_must_be_number");
                GameFeedbackService.playErrorSound(player);
                return;
            }

            if (suddenDeathMinutes < 1) {
                LanguageManager.getInstance().sendMessage(player, "sudden_death_time_at_least_1");
                GameFeedbackService.playErrorSound(player);
                return;
            }

            ArenaConfigManager.setSuddenDeathTimer(arenaName, suddenDeathMinutes);
            LanguageManager.getInstance().sendMessage(player, "sudden_death_time_set", "minutes", String.valueOf(suddenDeathMinutes));
        }
    }

    private void handleSetMobArea(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_setmobarea");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        Location loc = player.getLocation();
        MobSpawnerManager.setMobArea(arenaName, loc);

        LanguageManager.getInstance().sendMessage(player, "mob_area_set", "arenaName", arenaName);
        LanguageManager.getInstance().sendMessage(player, "mob_area_info");
    }

    private void handleSetSpectSpawn(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_setspectspawn");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        Location loc = player.getLocation();
        ArenaConfigManager.setSpectatorSpawn(arenaName, loc);

        LanguageManager.getInstance().sendMessage(player, "spectator_spawn_set", "arenaName", arenaName);
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName, "spectator");
    }

    private void handleArenaTeleportCommand(Player player, String[] args) {
        if (!player.hasPermission("obsidianwars.command.arena")) {
            LanguageManager.getInstance().sendMessage(player, "no_permission");
            return;
        }

        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_arena_teleport");
            return;
        }

        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        Location spectSpawn = ArenaConfigManager.getSpectatorSpawn(arenaName);
        if (spectSpawn == null) {
            LanguageManager.getInstance().sendMessage(player, "spectator_spawn_not_set", "arenaName", arenaName);
            return;
        }

        player.teleport(spectSpawn);
        LanguageManager.getInstance().sendMessage(player, "teleported_to_arena", "arenaName", arenaName);
    }

    private void handleArenaFinishCommand(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_arena_finish");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
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
            LanguageManager.getInstance().sendMessage(player, "arena_incomplete");
            for (String req : missingRequirements) {
                player.sendMessage(ColorUtils.translate("&c- " + req));
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

        LanguageManager.getInstance().sendMessage(player, "arena_finished", "arenaName", arenaName);
        LanguageManager.getInstance().sendMessage(player, "arena_ready");
        LanguageManager.getInstance().sendMessage(player, "resource_block_count", "count", String.valueOf(ArenaConfigManager.getResourceBlockCount(arenaName)));

        if (!optionalFeatures.isEmpty()) {
            LanguageManager.getInstance().sendMessage(player, "optional_features");
            for (String feature : optionalFeatures) {
                player.sendMessage(ColorUtils.translate("&7- " + feature));
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
            LanguageManager.getInstance().sendMessage(player, "usage_create_arena");
            return;
        }

        String arenaName = args[2];
        int minPlayers, maxPlayers;

        try {
            minPlayers = Integer.parseInt(args[3]);
            maxPlayers = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            LanguageManager.getInstance().sendMessage(player, "invalid_numbers");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (minPlayers < 2) {
            LanguageManager.getInstance().sendMessage(player, "min_players_at_least_2");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (maxPlayers < minPlayers) {
            LanguageManager.getInstance().sendMessage(player, "max_players_greater_than_min");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        if (ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_already_exists");
            GameFeedbackService.playErrorSound(player);
            return;
        }

        // Create arena config with parameters immediately (no wand positions required)
        ArenaConfigManager.createArenaConfigWithParams(arenaName, minPlayers, maxPlayers);

        // Rebuild arena world cache for MovementListener
        MovementListener.rebuildArenaWorldCache();

        LanguageManager.getInstance().sendMessage(player, "arena_created", "arenaName", arenaName);
        LanguageManager.getInstance().sendMessage(player, "player_limits_info", "min", String.valueOf(minPlayers), "max", String.valueOf(maxPlayers));
        LanguageManager.getInstance().sendMessage(player, "default_timers_info");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_guide_header");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_1");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_2");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_3");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_4");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_5");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_6");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_7");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_8");
        LanguageManager.getInstance().sendMessage(player, "arena_setup_step_9");
        GameFeedbackService.playSuccessSound(player);
        ObsidianCommand.suggestNextSetupStep(player, arenaName);
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 3) {
            LanguageManager.getInstance().sendMessage(player, "usage_delete_arena");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            LanguageManager.getInstance().sendMessage(player, "arena_not_found", "arenaName", arenaName);
            return;
        }

        // Delete arena
        ArenaConfigManager.deleteArenaConfig(arenaName);

        // Rebuild arena world cache for MovementListener
        MovementListener.rebuildArenaWorldCache();

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

                    LanguageManager.getInstance().sendMessage(arenaPlayer, "arena_deleted_removed");

                    // Update lobby scoreboard
                    az.nuran.obsidianwars.managers.LobbyScoreboardManager.updateLobbyScoreboard(arenaPlayer);
                }
            }
        }

        LanguageManager.getInstance().sendMessage(player, "arena_deleted", "arenaName", arenaName);
    }
}
