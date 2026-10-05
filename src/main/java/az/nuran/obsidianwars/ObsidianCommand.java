package az.nuran.obsidianwars;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Refactored command handler for ObsidianWars.
 * Supports new command structure with aliases and improved usability.
 */
public class ObsidianCommand implements CommandExecutor {

    private final Obsidianwars plugin;

    // Arenada olan oyunçuları izləmək üçün
    public static final Map<UUID, String> playersInArena = new HashMap<>();

    public ObsidianCommand(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String key) {
        return MessagesConfigManager.getMessage(key);
    }

    private String getMessage(String key, String... replacements) {
        return MessagesConfigManager.getMessage(key, replacements);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;

        // Handle alias commands directly
        if (command.getName().equalsIgnoreCase("join")) {
            handleJoinCommand(player, args);
            return true;
        }

        if (command.getName().equalsIgnoreCase("leave")) {
            handleLeaveCommand(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("stats")) {
            handleStatsCommand(player, args);
            return true;
        }

        if (command.getName().equalsIgnoreCase("rejoin")) {
            handleRejoinCommand(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("spectate")) {
            handleSpectateCommand(player, args);
            return true;
        }

        // Handle /o or /obsidian commands
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // ==================== PLAY & JOIN COMMANDS ====================
        if (subCommand.equals("play") || subCommand.equals("join")) {
            handleJoinCommand(player, args);
            return true;
        }

        // ==================== LEAVE COMMAND ====================
        if (subCommand.equals("leave")) {
            handleLeaveCommand(player);
            return true;
        }

        // ==================== REJOIN COMMAND ====================
        if (subCommand.equals("rejoin")) {
            handleRejoinCommand(player);
            return true;
        }

        // ==================== STATS COMMAND ====================
        if (subCommand.equals("stats")) {
            handleStatsCommand(player, args);
            return true;
        }

        // ==================== GUI COMMAND ====================
        if (subCommand.equals("gui")) {
            handleGUICommand(player);
            return true;
        }

        // ==================== CMDS COMMAND ====================
        if (subCommand.equals("cmds")) {
            handleCmdsCommand(player);
            return true;
        }

        // ==================== ARENALIST COMMAND ====================
        if (subCommand.equals("arenalist")) {
            handleArenalistCommand(player);
            return true;
        }

        // ==================== TEAM COMMAND ====================
        if (subCommand.equals("team")) {
            handleTeamCommand(player);
            return true;
        }

        // ==================== WAND COMMAND ====================
        if (subCommand.equals("wand")) {
            handleWandCommand(player);
            return true;
        }

        // ==================== ARENA MANAGEMENT COMMANDS ====================
        if (subCommand.equals("arena")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /o arena <subcommand>");
                return true;
            }
            handleArenaCommand(player, args);
            return true;
        }

        // ==================== CREATE COMMAND (NEW) ====================
        if (subCommand.equals("create")) {
            handleCreateCommand(player, args);
            return true;
        }

        // ==================== DELETE COMMAND (NEW) ====================
        if (subCommand.equals("delete")) {
            handleDeleteCommand(player, args);
            return true;
        }

        // ==================== FORCE COMMANDS (NEW UNIFIED) ====================
        if (subCommand.equals("force")) {
            handleForceCommand(player, args);
            return true;
        }

        // ==================== FORCEEND COMMAND (LEGACY) ====================
        if (subCommand.equals("forceend")) {
            handleForceendCommand(player, args);
            return true;
        }

        // ==================== FORCESTART COMMAND (LEGACY) ====================
        if (subCommand.equals("forcestart")) {
            handleForcestartCommand(player, args);
            return true;
        }

        // ==================== FORCEPREP COMMAND (LEGACY) ====================
        if (subCommand.equals("forceprep")) {
            handleForceprepCommand(player, args);
            return true;
        }

        // ==================== DISABLE/ENABLE ARENA COMMANDS ====================
        if (subCommand.equals("disablearena")) {
            handleDisableArenaCommand(player, args);
            return true;
        }

        if (subCommand.equals("enablearena")) {
            handleEnableArenaCommand(player, args);
            return true;
        }

        // ==================== SPECTATE COMMAND ====================
        if (subCommand.equals("spectate")) {
            handleSpectateCommand(player, args);
            return true;
        }

        // ==================== DEBUG COMMAND ====================
        if (subCommand.equals("debug")) {
            handleDebugCommand(player, args);
            return true;
        }

        // ==================== ADMIN COMMANDS ====================
        if (subCommand.equals("admin")) {
            handleAdminCommand(player, args);
            return true;
        }

        // ==================== UNKNOWN COMMAND ====================
        player.sendMessage("§cUnknown command. Use /o cmds for help.");
        return true;
    }

    // ==================== COMMAND HANDLERS ====================

    public void handleJoinCommand(Player player, String[] args) {
        List<String> arenaNames = ArenaConfigManager.getArenaNames();

        if (arenaNames.isEmpty()) {
            player.sendMessage(getMessage("arena_no_arenas"));
            return;
        }

        String arenaName = null;

        // If arena specified, try to join that specific arena
        if (args.length > 1) {
            arenaName = args[1];
            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            if (!"READY".equals(status) && !"WAITING".equals(status) && !"DISABLED".equals(status)) {
                player.sendMessage("§cThis arena is not available (status: " + status + ")");
                return;
            }
            if ("DISABLED".equals(status)) {
                player.sendMessage("§cThis arena is currently disabled.");
                return;
            }
            if ("PLAYING".equals(status) || "STARTED".equals(status)) {
                player.sendMessage("§cThis arena is currently in progress. Please wait for it to finish.");
                return;
            }
        } else {
            // Join random available arena
            for (String name : arenaNames) {
                String status = ArenaConfigManager.getArenaStatus(name);
                if ("READY".equals(status) || "WAITING".equals(status)) {
                    arenaName = name;
                    break;
                }
            }
        }

        if (arenaName == null) {
            player.sendMessage(getMessage("arena_not_ready"));
            return;
        }

        // Use waiting spawn if available, otherwise fallback to lobby spawn
        Location lobbyLocation = ArenaConfigManager.getWaitingSpawn(arenaName);
        if (lobbyLocation == null) {
            lobbyLocation = ArenaConfigManager.getLobbySpawn(arenaName);
        }

        if (lobbyLocation == null) {
            player.sendMessage(getMessage("missing_lobby"));
            return;
        }

        World world = lobbyLocation.getWorld();
        if (world == null) {
            player.sendMessage(getMessage("world_not_found", "worldName", "Unknown"));
            return;
        }

        // Oyunçu limitini yoxlayırıq
        int currentPlayers = getArenaPlayerCount(arenaName);
        int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);
        if (currentPlayers >= maxPlayers) {
            player.sendMessage(getMessage("arena_full", "current", String.valueOf(currentPlayers), "max", String.valueOf(maxPlayers)));
            return;
        }

        player.teleport(lobbyLocation);

        // Reset player state
        resetPlayerState(player);

        // Oyunçunu arenada qeyd edirik
        playersInArena.put(player.getUniqueId(), arenaName);

        // IMMEDIATE AUTO-ASSIGNMENT: Assign to balanced team right away
        String assignedTeam = TeamManager.autoAssignTeam(player, arenaName);
        String teamName = assignedTeam.equals("red") ? "Qırmızı" : "Mavi";
        String teamColor = assignedTeam.equals("red") ? "§c" : "§9";
        player.sendMessage(MessagesConfigManager.getMessage("auto_team", "teamColor", teamColor, "teamName", teamName));

        // Lobby items veririk
        giveLobbyItems(player);

        // Scoreboard qururuq
        ScoreboardManager.updateScoreboard(player);

        // Broadcast mesajı
        broadcastToArena(arenaName, getMessage("player_joined", "player", player.getName(), "current", String.valueOf(getArenaPlayerCount(arenaName)), "max", String.valueOf(getMaxPlayers(arenaName))));

        player.sendMessage(getMessage("join_lobby", "arenaName", arenaName));

        // Play join sound
        String sound = MessagesConfigManager.getSound("join_lobby");
        Sound joinSound = Obsidianwars.parseSound(sound);
        if (joinSound != null) {
            player.playSound(player.getLocation(), joinSound, 1.0f, 1.0f);
        }

        // Hide existing spectators from the joining player
        hideExistingSpectatorsFromPlayer(player, arenaName);

        // Send welcome message
        sendWelcomeMessage(player);

        // Schedule rules announcement after 5 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendRulesAnnouncement(player);
        }, 100L); // 5 seconds (100 ticks)

        // Oyun başlama şəraitini yoxlayırıq
        GameManager.checkGameStart(arenaName);
    }

    private void handleLeaveCommand(Player player) {
        if (!playersInArena.containsKey(player.getUniqueId())) {
            player.sendMessage(getMessage("not_in_arena"));
            return;
        }

        String arenaName = playersInArena.get(player.getUniqueId());

        // Check if in countdown state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        boolean wasInCountdown = (game != null && game.getGameState() == GameManager.GameState.COUNTDOWN);
        boolean wasInGame = (game != null && (game.getGameState() == GameManager.GameState.PLAYING || game.getGameState() == GameManager.GameState.PREPARATION));

        // Clear disconnect record for this player (manual leave, not disconnect)
        GameManager.clearDisconnectRecord(player.getUniqueId());

        // Oyunçunu arenadan çıxarırıq
        playersInArena.remove(player.getUniqueId());
        TeamManager.removePlayerFromTeam(player);
        ParticleManager.removeSpawnProtection(player);

        // Reset player state and teleport to spawn
        Location mainSpawn = player.getWorld().getSpawnLocation();
        PlayerUtils.resetPlayerFull(player, mainSpawn);

        // Broadcast mesajı
        broadcastToArena(arenaName, getMessage("player_left", "player", player.getName()));

        player.sendMessage(getMessage("game_ended"));

        // Update lobby scoreboard after leaving arena
        LobbyScoreboardManager.updateLobbyScoreboard(player);

        // CANCELLATION LOGIC: If player left during countdown, check if we need to cancel
        if (wasInCountdown) {
            if (TeamManager.isAnyTeamEmpty(arenaName) ||
                TeamManager.getTotalPlayerCount(arenaName) < ArenaConfigManager.getMinPlayers(arenaName)) {
                GameManager.cancelCountdown(arenaName);
            }
        }

        // WIN CONDITION: If player left during active game, check for team elimination
        if (wasInGame) {
            GameManager.checkTeamEliminationOnLeave(arenaName);
        }

        // Oyun statusunu yoxlayırıq
        GameManager.checkGameStart(arenaName);
    }

    private void handleRejoinCommand(Player player) {
        // Check if player can rejoin
        if (GameManager.canRejoin(player)) {
            // Attempt to restore player state
            boolean restored = GameManager.restoreDisconnectedPlayer(player);

            if (restored) {
                String arenaName = GameManager.getDisconnectedPlayerArena(player.getUniqueId());
                String rejoinMessage = "§aYou have reconnected to the arena!";
                player.sendMessage(rejoinMessage);

                // Broadcast to arena
                String broadcastMessage = "§a" + player.getName() + " has reconnected!";
                ObsidianCommand.broadcastToArena(arenaName, broadcastMessage);

                // Play rejoin sound
                String sound = MessagesConfigManager.getSound("join_lobby");
                Sound rejoinSound = Obsidianwars.parseSound(sound);
                if (rejoinSound != null) {
                    player.playSound(player.getLocation(), rejoinSound, 1.0f, 1.0f);
                }
            } else {
                player.sendMessage("§cCould not restore your game session. It may have expired.");
            }
        } else {
            player.sendMessage("§cNo active game session found to rejoin.");
        }
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

    private void handleGUICommand(Player player) {
        openArenaSelectorGUI(player);
    }

    private void handleCmdsCommand(Player player) {
        player.sendMessage("§6§l=== ObsidianWars Commands ===");
        player.sendMessage("§ePlayer Commands:");
        player.sendMessage("  §a/o play [arena] §7- Join an arena (or random)");
        player.sendMessage("  §a/o join [arena] §7- Alias for play");
        player.sendMessage("  §a/o leave §7- Leave current arena");
        player.sendMessage("  §a/o rejoin §7- Rejoin after disconnect");
        player.sendMessage("  §a/o stats [player] §7- View player statistics");
        player.sendMessage("  §a/o gui §7- Open arena selector GUI");
        player.sendMessage("  §a/o team §7- Open team selection GUI");
        player.sendMessage("§eAdmin Commands:");
        player.sendMessage("  §a/o create arena <name> <min> <max> §7- Create arena");
        player.sendMessage("  §a/o delete arena <name> §7- Delete arena");
        player.sendMessage("  §a/o disableArena <arena> §7- Disable arena");
        player.sendMessage("  §a/o enableArena <arena> §7- Enable arena");
        player.sendMessage("  §a/o force start arena <arena> §7- Force start game");
        player.sendMessage("  §a/o force end arena <arena> §7- Force end game");
        player.sendMessage("  §a/o force end preperation <arena> §7- Skip preparation phase");
        player.sendMessage("  §a/o forcestart <arena> §7- Force start game (legacy)");
        player.sendMessage("  §a/o forceend <arena> §7- Force end game (legacy)");
        player.sendMessage("  §a/o forceprep <arena> §7- Skip preparation phase (legacy)");
        player.sendMessage("  §a/o arena setlobby <arena> §7- Set main lobby");
        player.sendMessage("  §a/o arena setwaitingspawn <arena> §7- Set waiting lobby");
        player.sendMessage("  §a/o arena setwaitingregion <arena> §7- Set waiting boundary");
        player.sendMessage("  §a/o arena setspawn <arena> <red|blue> §7- Set team spawn");
        player.sendMessage("  §a/o arena setobsidian <arena> <red|blue> §7- Set obsidian");
        player.sendMessage("  §a/o arena setblocks <arena> §7- Setup resource blocks");
        player.sendMessage("  §a/o arena setwall <arena> <red|blue> §7- Set defense wall");
        player.sendMessage("  §a/o arena settimer <arena> <time> §7- Set prep time");
        player.sendMessage("  §a/o arena setplayers <arena> <min> <max> §7- Set player limits");
        player.sendMessage("  §a/o arena finish <arena> §7- Finish arena setup");
        player.sendMessage("  §a/o arena setspectspawn <arena> §7- Set spectator spawn");
        player.sendMessage("  §a/o wand §7- Get arena setup wand");
        player.sendMessage("  §a/o arenalist §7- List all arenas");
        player.sendMessage("  §a/o admin addexp <player> <amount> §7- Add XP to player");
        player.sendMessage("  §a/o admin addlevel <player> <amount> §7- Add levels to player");
        player.sendMessage("  §a/o admin removexp <player> <amount> §7- Remove XP from player");
        player.sendMessage("  §a/o admin removelevel <player> <amount> §7- Remove levels from player");
        player.sendMessage("  §a/o debug <console|chat|both|off> §7- Toggle debug logging");
        player.sendMessage("  §a/o cmds §7- Show this command list");
    }

    private void handleArenalistCommand(Player player) {
        List<String> arenaNames = ArenaConfigManager.getArenaNames();

        if (arenaNames.isEmpty()) {
            player.sendMessage("§cNo arenas created yet.");
            return;
        }

        player.sendMessage("§6§l=== Arena List ===");
        for (String arenaName : arenaNames) {
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            String statusColor;
            if (status.equals("READY") || status.equals("WAITING")) {
                statusColor = "§a";
            } else if (status.equals("STARTING")) {
                statusColor = "§6";
            } else if (status.equals("STARTED")) {
                statusColor = "§c";
            } else if (status.equals("PLAYING")) {
                statusColor = "§c";
            } else if (status.equals("DISABLED")) {
                statusColor = "§8";
            } else {
                statusColor = "§7";
            }
            int currentPlayers = getArenaPlayerCount(arenaName);
            int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);

            player.sendMessage(statusColor + arenaName + " §7[" + currentPlayers + "/" + maxPlayers + "] §8- " + statusColor + status);
        }
    }

    private void handleTeamCommand(Player player) {
        if (!playersInArena.containsKey(player.getUniqueId())) {
            player.sendMessage(getMessage("join_first"));
            return;
        }

        // Check if countdown is 5 seconds or fewer - prevent team changes
        String arenaName = playersInArena.get(player.getUniqueId());
        int countdown = GameManager.getCountdown(arenaName);

        if (countdown > 0 && countdown <= 5) {
            player.sendMessage("§cKomanda dəyişdirilməsi son 5 saniyədə qadağandır!");
            return;
        }

        openTeamSelectionGUI(player);
    }

    private void handleWandCommand(Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Obsidian Wars Wand");
            meta.setLore(Collections.singletonList("§eKüncləri seçmək üçün bloklara sol və sağ klikləyin."));
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        player.sendMessage(getMessage("wand_received"));
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
                newMode = DebugManager.DebugMode.CONSOLE;
                break;
            case "chat":
                newMode = DebugManager.DebugMode.CHAT;
                break;
            case "both":
                newMode = DebugManager.DebugMode.BOTH;
                break;
            case "off":
                newMode = DebugManager.DebugMode.OFF;
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

    private void handleArenaCommand(Player player, String[] args) {
        String action = args[1].toLowerCase();

        if (action.equals("setlobby")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o arena setlobby <arenaName>");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            Location loc = player.getLocation();
            ArenaConfigManager.setLobbySpawn(arenaName, loc);

            player.sendMessage(getMessage("lobby_spawn_set", "arenaName", arenaName));
            playSuccessSound(player);
            suggestNextSetupStep(player, arenaName, "lobby");
        } else if (action.equals("setwaitingspawn")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o arena setwaitingspawn <arenaName>");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            Location loc = player.getLocation();
            ArenaConfigManager.setWaitingSpawn(arenaName, loc);

            player.sendMessage("§aWaiting lobby spawn set for arena " + arenaName + "!");
            playSuccessSound(player);
            suggestNextSetupStep(player, arenaName, "waiting");
        } else if (action.equals("setwaitingregion")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o arena setwaitingregion <arenaName>");
                player.sendMessage("§eSelect the waiting area bounds with your wand (Pos1 and Pos2) first!");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
            Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

            if (pos1 == null || pos2 == null) {
                player.sendMessage(getMessage("wand_need_positions"));
                player.sendMessage("§eUse the wand to select Corner 1 (left-click) and Corner 2 (right-click) of the waiting area.");
                playErrorSound(player);
                return;
            }

            ArenaConfigManager.setWaitingRegion(arenaName, pos1, pos2);

            player.sendMessage("§aWaiting region set for arena " + arenaName + "!");
            player.sendMessage("§ePlayers will be teleported back if they leave this area during the waiting phase.");
            playSuccessSound(player);
            suggestNextSetupStep(player, arenaName, "waitingregion");
        } else if (action.equals("setplayers")) {
            if (args.length < 4) {
                player.sendMessage("§cUsage: /o arena setplayers <arenaName> <min> <max>");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            int min, max;
            try {
                min = Integer.parseInt(args[3]);
                max = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number format! Usage: /o arena setplayers <arena> <min> <max>");
                playErrorSound(player);
                return;
            }

            if (min < 2) {
                player.sendMessage("§cMinimum players must be at least 2!");
                playErrorSound(player);
                return;
            }

            if (max < min) {
                player.sendMessage("§cMaximum players must be greater than or equal to minimum players!");
                playErrorSound(player);
                return;
            }

            ArenaConfigManager.setPlayerLimits(arenaName, min, max);

            player.sendMessage(getMessage("player_limits_set", "arenaName", arenaName, "min", String.valueOf(min), "max", String.valueOf(max)));
        } else if (action.equals("setspawn")) {
            if (args.length < 4) {
                player.sendMessage("§cUsage: /o arena setspawn <arenaName> <red|blue>");
                return;
            }
            String arenaName = args[2];
            String teamColor = args[3].toLowerCase();

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            if (!teamColor.equals("red") && !teamColor.equals("blue")) {
                player.sendMessage(getMessage("invalid_team_color"));
                playErrorSound(player);
                return;
            }

            Location loc = player.getLocation();
            ArenaConfigManager.setTeamSpawn(arenaName, teamColor, loc);

            String teamName = teamColor.equals("red") ? "Qırmızı" : "Mavi";
            String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
            player.sendMessage(getMessage("team_spawn_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName));
            playSuccessSound(player);
            suggestNextSetupStep(player, arenaName, teamColor + "spawn");
        } else if (action.equals("setobsidian")) {
            if (args.length < 4) {
                player.sendMessage("§cUsage: /o arena setobsidian <arenaName> <red|blue>");
                return;
            }
            String arenaName = args[2];
            String teamColor = args[3].toLowerCase();

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            if (!teamColor.equals("red") && !teamColor.equals("blue")) {
                player.sendMessage(getMessage("invalid_team_color"));
                playErrorSound(player);
                return;
            }

            org.bukkit.block.Block targetBlock = player.rayTraceBlocks(5) != null ? player.rayTraceBlocks(5).getHitBlock() : null;
            if (targetBlock == null || targetBlock.getType() != Material.OBSIDIAN) {
                player.sendMessage(getMessage("invalid_obsidian_target"));
                playErrorSound(player);
                return;
            }

            Location obsidianLoc = targetBlock.getLocation();
            ArenaConfigManager.setObsidianLocation(arenaName, teamColor, obsidianLoc);

            String teamName = teamColor.equals("red") ? "Qırmızı" : "Mavi";
            String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
            player.sendMessage(getMessage("obsidian_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName));
            playSuccessSound(player);
            suggestNextSetupStep(player, arenaName, teamColor + "obsidian");
        } else if (action.equals("finish")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o arena finish <arenaName>");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            // Yoxlama siyahısı
            List<String> missingRequirements = new ArrayList<>();

            // 1. Min/Max oyunçu limiti
            if (ArenaConfigManager.getMinPlayers(arenaName) == 0 || ArenaConfigManager.getMaxPlayers(arenaName) == 0) {
                missingRequirements.add("Oyunçu limitləri (setplayers)");
            }

            // 2. Lobby spawn nöqtəsi
            if (ArenaConfigManager.getLobbySpawn(arenaName) == null) {
                missingRequirements.add("Lobi spawn nöqtəsi (setlobby)");
            }

            // 3. Waiting lobby spawn
            if (ArenaConfigManager.getWaitingSpawn(arenaName) == null) {
                missingRequirements.add("Waiting lobby spawn (setwaitingspawn)");
            }

            // 3.5. Waiting region (optional, not required)
            // Skip this check as it's optional

            // 4. Red Team spawn
            if (ArenaConfigManager.getTeamSpawn(arenaName, "red") == null) {
                missingRequirements.add("Qırmızı Komanda spawn (setspawn red)");
            }

            // 5. Blue Team spawn
            if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
                missingRequirements.add("Mavi Komanda spawn (setspawn blue)");
            }

            // 6. Red Team obsidian
            if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
                missingRequirements.add("Qırmızı Komanda obsidian (setobsidian red)");
            }

            // 7. Blue Team obsidian
            if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
                missingRequirements.add("Mavi Komanda obsidian (setobsidian blue)");
            }

            // 8. Resource blokları
            if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
                missingRequirements.add("Resource blokları (setblocks)");
            }

            // 9. Wall regions (both red and blue)
            if (!ArenaConfigManager.hasWallConfiguration(arenaName, "red") || !ArenaConfigManager.hasWallConfiguration(arenaName, "blue")) {
                missingRequirements.add("Wall regions (setwall red & setwall blue)");
            }

            // 10. Spectator spawn
            if (ArenaConfigManager.getSpectatorSpawn(arenaName) == null) {
                missingRequirements.add("Spectator spawn (setspectspawn)");
            }

            // Əgər məcburiyyətlər varsa, xəta göstəririk
            if (!missingRequirements.isEmpty()) {
                player.sendMessage(getMessage("arena_incomplete"));
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
                optionalFeatures.add("Mob spawn area (setmobarea) - İstəyə bağlı");
            }

            // Bütün yoxlamalar keçdi, statusu READY edirik
            ArenaConfigManager.setArenaStatus(arenaName, "READY");

            player.sendMessage(getMessage("arena_finished", "arenaName", arenaName));
            player.sendMessage("§aArena artıq oyunçular tərəfindən istifadə edilə bilər.");
            player.sendMessage("§eResource blok sayı: " + ArenaConfigManager.getResourceBlockCount(arenaName));

            if (!optionalFeatures.isEmpty()) {
                player.sendMessage("§eİstəyə bağlı xüsusiyyətlər:");
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
        } else if (action.equals("setblocks")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o arena setblocks <arenaName>");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            // Əgər oyunçu artıq setup mode-dadırsa, çıxarırıq
            if (ResourceBlockManager.isInSetupMode(player)) {
                ResourceBlockManager.exitSetupMode(player);
                return;
            }

            // Setup mode-a daxil edirik
            ResourceBlockManager.enterSetupMode(player, arenaName);
        } else if (action.equals("setwall")) {
            if (args.length < 4) {
                player.sendMessage("§cUsage: /o arena setwall <arenaName> <red|blue>");
                return;
            }
            String arenaName = args[2];
            String teamColor = args[3].toLowerCase();

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            if (!teamColor.equals("red") && !teamColor.equals("blue")) {
                player.sendMessage(getMessage("invalid_team_color"));
                playErrorSound(player);
                return;
            }

            Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
            Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

            if (pos1 == null || pos2 == null) {
                player.sendMessage(getMessage("wand_need_positions"));
                playErrorSound(player);
                return;
            }

            WallManager.setWall(arenaName, teamColor, pos1, pos2);

            String teamName = teamColor.equals("red") ? "Qırmızı" : "Mavi";
            String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
            player.sendMessage(getMessage("wall_set", "teamColor", teamColorCode, "teamName", teamName));
            playSuccessSound(player);
            suggestNextSetupStep(player, arenaName, teamColor + "wall");
        } else if (action.equals("settimer")) {
            if (args.length < 4) {
                player.sendMessage("§cUsage: /o arena settimer <arenaName> <preparation_time> [sudden_death_time]");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            int prepMinutes;
            try {
                prepMinutes = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cXəta: Hazırlıq müddəti rəqəm olmalıdır!");
                playErrorSound(player);
                return;
            }

            if (prepMinutes < 1) {
                player.sendMessage("§cPreparation time must be at least 1 minute!");
                playErrorSound(player);
                return;
            }

            WallManager.setTimer(arenaName, prepMinutes);
            player.sendMessage(getMessage("timer_set", "arenaName", arenaName, "minutes", String.valueOf(prepMinutes)));

            // Optional sudden death timer
            if (args.length >= 5) {
                int suddenDeathMinutes;
                try {
                    suddenDeathMinutes = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cXəta: Sudden death müddəti rəqəm olmalıdır!");
                    playErrorSound(player);
                    return;
                }

                if (suddenDeathMinutes < 1) {
                    player.sendMessage("§cSudden death time must be at least 1 minute!");
                    playErrorSound(player);
                    return;
                }

                ArenaConfigManager.setSuddenDeathTimer(arenaName, suddenDeathMinutes);
                player.sendMessage("§aSudden death müddəti " + suddenDeathMinutes + " dəqiqə olaraq təyin edildi!");
            }
        } else if (action.equals("setmobarea")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o arena setmobarea <arenaName>");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            Location loc = player.getLocation();
            MobSpawnerManager.setMobArea(arenaName, loc);

            player.sendMessage("§aMob spawn area set for arena " + arenaName + " at your current location!");
            player.sendMessage("§eWither Skeletons will spawn every 2 minutes during combat phase.");
        } else if (action.equals("setspectspawn")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o arena setspectspawn <arenaName>");
                return;
            }
            String arenaName = args[2];

            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }

            Location loc = player.getLocation();
            ArenaConfigManager.setSpectatorSpawn(arenaName, loc);

            player.sendMessage("§aSpectator spawn location set for arena " + arenaName + "!");
            playSuccessSound(player);
            suggestNextSetupStep(player, arenaName, "spectator");
        } else {
            player.sendMessage("§cUnknown arena subcommand. Use /o cmds for help.");
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
            playErrorSound(player);
            return;
        }

        if (maxPlayers < minPlayers) {
            player.sendMessage("§cMaximum players must be >= minimum players!");
            playErrorSound(player);
            return;
        }

        if (ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_already_exists"));
            playErrorSound(player);
            return;
        }

        // Create arena config with parameters immediately (no wand positions required)
        ArenaConfigManager.createArenaConfigWithParams(arenaName, minPlayers, maxPlayers);

        player.sendMessage(getMessage("arena_created", "arenaName", arenaName));
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
        playSuccessSound(player);
        suggestNextSetupStep(player, arenaName);
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /o delete arena <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Arenanı silirik
        ArenaConfigManager.deleteArenaConfig(arenaName);

        // GameManager-dən oyunu silirik
        GameManager.removeGame(arenaName);

        // Arenadakı oyunçuları təmizləyirik
        for (UUID uuid : new ArrayList<>(playersInArena.keySet())) {
            if (playersInArena.get(uuid).equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    // Oyunçunu arenadan çıxarırıq
                    playersInArena.remove(uuid);
                    TeamManager.removePlayerFromTeam(arenaPlayer);

                    // Reset player state and teleport to spawn
                    Location mainSpawn = arenaPlayer.getWorld().getSpawnLocation();
                    PlayerUtils.resetPlayerFull(arenaPlayer, mainSpawn);

                    arenaPlayer.sendMessage("§cArena silindi, siz arenadan çıxarıldınız!");

                    // Update lobby scoreboard
                    LobbyScoreboardManager.updateLobbyScoreboard(arenaPlayer);
                }
            }
        }

        player.sendMessage(getMessage("arena_deleted", "arenaName", arenaName));
    }

    private void handleForceendCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o forceend <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Oyunu bitiririk - message is broadcast to arena players by endGame
        GameManager.endGame(arenaName, null); // null = admin ended, no winner
        player.sendMessage("§aGame ended for arena " + arenaName);
    }

    private void handleForcestartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o forcestart <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Check if there are players in the arena
        if (!GameManager.hasPlayersInArena(arenaName)) {
            player.sendMessage("§cNo players in arena " + arenaName + "!");
            return;
        }

        // Check current game state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game != null) {
            GameManager.GameState state = game.getGameState();
            // If in COUNTDOWN state, cancel countdown and immediately start
            if (state == GameManager.GameState.COUNTDOWN) {
                GameManager.cancelCountdownAndForceStart(arenaName);
                player.sendMessage("§aCountdown skipped! Game starting now for arena " + arenaName);
                return;
            }
            // If already in PLAYING or PREPARATION state, return error
            if (state == GameManager.GameState.PLAYING || state == GameManager.GameState.PREPARATION) {
                player.sendMessage("§cThis arena is already active!");
                return;
            }
        }

        // Force start (no game exists or in WAITING state)
        GameManager.forceStartGame(arenaName);
        player.sendMessage(getMessage("force_started", "arenaName", arenaName));
    }

    private void handleForceprepCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o forceprep <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Check if game is in PREPARATION state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null || game.getGameState() != GameManager.GameState.PREPARATION) {
            player.sendMessage("§cThis arena is not in preparation phase!");
            return;
        }

        // Force skip preparation - trigger wall removal immediately
        WallManager.forceSkipPreparation(arenaName);
        player.sendMessage("§aPreparation phase skipped! Walls are breaking now!");
    }

    private void handleForceCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o force <start|end> <arena|preperation> [arena]");
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("start")) {
            if (args.length < 4 || !args[2].equalsIgnoreCase("arena")) {
                player.sendMessage("§cUsage: /o force start arena <arena>");
                return;
            }
            String arenaName = args[3];
            if (!ArenaConfigManager.arenaExists(arenaName)) {
                player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                return;
            }
            // Check if there are players in the arena
            if (!GameManager.hasPlayersInArena(arenaName)) {
                player.sendMessage("§cNo players in arena " + arenaName + "!");
                return;
            }

            // Check current game state
            GameManager.ArenaGame game = GameManager.getGame(arenaName);
            if (game != null) {
                GameManager.GameState state = game.getGameState();
                // If in COUNTDOWN state, cancel countdown and immediately start
                if (state == GameManager.GameState.COUNTDOWN) {
                    GameManager.cancelCountdownAndForceStart(arenaName);
                    player.sendMessage("§aCountdown skipped! Game starting now for arena " + arenaName);
                    return;
                }
                // If already in PLAYING or PREPARATION state, return error
                if (state == GameManager.GameState.PLAYING || state == GameManager.GameState.PREPARATION) {
                    player.sendMessage("§cThis arena is already active!");
                    return;
                }
            }

            GameManager.forceStartGame(arenaName);
            player.sendMessage(getMessage("force_started", "arenaName", arenaName));
        } else if (action.equals("end")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /o force end arena <arena> OR /o force end preperation <arena>");
                return;
            }
            String subAction = args[2].toLowerCase();
            if (subAction.equals("arena")) {
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /o force end arena <arena>");
                    return;
                }
                String arenaName = args[3];
                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return;
                }
                GameManager.endGame(arenaName, null); // Message is broadcast to arena players by endGame
                player.sendMessage("§aGame ended for arena " + arenaName);
            } else if (subAction.equals("preperation")) {
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /o force end preperation <arena>");
                    return;
                }
                String arenaName = args[3];
                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return;
                }
                GameManager.ArenaGame game = GameManager.getGame(arenaName);
                if (game == null || game.getGameState() != GameManager.GameState.PREPARATION) {
                    player.sendMessage("§cThis arena is not in preparation phase!");
                    return;
                }
                WallManager.forceSkipPreparation(arenaName);
                player.sendMessage("§aPreparation phase skipped! Walls are breaking now!");
            } else {
                player.sendMessage("§cUsage: /o force end arena <arena> OR /o force end preperation <arena>");
            }
        } else {
            player.sendMessage("§cUsage: /o force <start|end> <arena|preperation> [arena]");
        }
    }

    private void handleDisableArenaCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /o disableArena <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
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
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Check mandatory requirements before enabling
        List<String> missingRequirements = new ArrayList<>();

        // 1. Lobby spawn
        if (ArenaConfigManager.getLobbySpawn(arenaName) == null) {
            missingRequirements.add("Lobi spawn nöqtəsi (setlobby)");
        }

        // 2. Waiting lobby spawn
        if (ArenaConfigManager.getWaitingSpawn(arenaName) == null) {
            missingRequirements.add("Waiting lobby spawn (setwaitingspawn)");
        }

        // 3. Red Team spawn
        if (ArenaConfigManager.getTeamSpawn(arenaName, "red") == null) {
            missingRequirements.add("Qırmızı Komanda spawn (setspawn red)");
        }

        // 4. Blue Team spawn
        if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
            missingRequirements.add("Mavi Komanda spawn (setspawn blue)");
        }

        // 5. Red Team obsidian
        if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
            missingRequirements.add("Qırmızı Komanda obsidian (setobsidian red)");
        }

        // 6. Blue Team obsidian
        if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
            missingRequirements.add("Mavi Komanda obsidian (setobsidian blue)");
        }

        // 7. Resource blocks
        if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
            missingRequirements.add("Resource blokları (setblocks)");
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
            Sound errorSound = Obsidianwars.parseSound("ENTITY_VILLAGER_NO");
            if (errorSound == null) {
                errorSound = Sound.valueOf("VILLAGER_NO");
            }
            if (errorSound != null) {
                player.playSound(player.getLocation(), errorSound, 1.0f, 1.0f);
            }
            return;
        }

        ArenaConfigManager.setArenaStatus(arenaName, "READY");
        player.sendMessage("§aArena " + arenaName + " has been enabled.");
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
            String playerArena = playersInArena.get(targetPlayer.getUniqueId());
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

    // ==================== HELPER METHODS ====================

    /**
     * Plays a success sound to the player
     */
    private void playSuccessSound(Player player) {
        Sound successSound = Obsidianwars.parseSound("ENTITY_PLAYER_LEVELUP");
        if (successSound == null) {
            try {
                successSound = Sound.valueOf("LEVEL_UP");
            } catch (IllegalArgumentException e) {
                return;
            }
        }
        if (successSound != null) {
            player.playSound(player.getLocation(), successSound, 1.0f, 1.0f);
        }
    }

    /**
     * Plays an error sound to the player
     */
    private void playErrorSound(Player player) {
        Sound errorSound = Obsidianwars.parseSound("ENTITY_VILLAGER_NO");
        if (errorSound == null) {
            try {
                errorSound = Sound.valueOf("VILLAGER_NO");
            } catch (IllegalArgumentException e) {
                return;
            }
        }
        if (errorSound != null) {
            player.playSound(player.getLocation(), errorSound, 1.0f, 1.0f);
        }
    }

    /**
     * Suggests the next setup step for an arena with clickable chat suggestion
     * @param completedStep The step that was just completed (null if none)
     */
    static void suggestNextSetupStep(Player player, String arenaName, String completedStep) {
        // Optimized flow: After setting a team spawn, suggest setting that team's obsidian (you're already there)
        // After setting obsidian, suggest the opposite team's spawn

        // Check what's already set and suggest the next missing step
        // Skip the check for the step that was just completed to avoid race condition with async saves
        if (completedStep == null || !completedStep.equals("lobby")) {
            if (ArenaConfigManager.getLobbySpawn(arenaName) == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Əsas lobby spawnunu təyin et", "/o arena setlobby " + arenaName);
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("waiting")) {
            if (ArenaConfigManager.getWaitingSpawn(arenaName) == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Gözləmə lobby spawnunu təyin et", "/o arena setwaitingspawn " + arenaName);
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("waitingregion")) {
            if (ArenaConfigManager.getWaitingRegion(arenaName) == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Gözləmə sahəsi sərhədini təyin et", "/o arena setwaitingregion " + arenaName);
                return;
            }
        }

        // Optimized team setup flow
        // If red spawn was just set, suggest red obsidian (you're at red base)
        if (completedStep != null && completedStep.equals("redspawn")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Qırmızı komanda obsidianını təyin et", "/o arena setobsidian " + arenaName + " red");
                return;
            }
        }

        // If red obsidian was just set, suggest blue spawn (go to other base)
        if (completedStep != null && completedStep.equals("redobsidian")) {
            if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Mavi komanda spawnını təyin et", "/o arena setspawn " + arenaName + " blue");
                return;
            }
        }

        // If blue spawn was just set, suggest blue obsidian (you're at blue base)
        if (completedStep != null && completedStep.equals("bluespawn")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Mavi komanda obsidianını təyin et", "/o arena setobsidian " + arenaName + " blue");
                return;
            }
        }

        // If blue obsidian was just set, suggest resource blocks
        if (completedStep != null && completedStep.equals("blueobsidian")) {
            if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
                sendClickableSuggestion(player, "§eNövbəti addım: Resurs bloklarını qur", "/o arena setblocks " + arenaName);
                return;
            }
        }

        // Default checks for when no specific completed step or continuing from general flow
        if (completedStep == null || !completedStep.equals("redspawn")) {
            if (ArenaConfigManager.getTeamSpawn(arenaName, "red") == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Qırmızı komanda spawnını təyin et", "/o arena setspawn " + arenaName + " red");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("bluespawn")) {
            if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Mavi komanda spawnını təyin et", "/o arena setspawn " + arenaName + " blue");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("redobsidian")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Qırmızı komanda obsidianını təyin et", "/o arena setobsidian " + arenaName + " red");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("blueobsidian")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Mavi komanda obsidianını təyin et", "/o arena setobsidian " + arenaName + " blue");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("resourceblocks")) {
            if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
                sendClickableSuggestion(player, "§eNövbəti addım: Resurs bloklarını qur", "/o arena setblocks " + arenaName);
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("redwall")) {
            if (!ArenaConfigManager.hasWallConfiguration(arenaName, "red")) {
                sendClickableSuggestion(player, "§eNövbəti addım: Qırmızı komanda divarını təyin et", "/o arena setwall " + arenaName + " red");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("bluewall")) {
            if (!ArenaConfigManager.hasWallConfiguration(arenaName, "blue")) {
                sendClickableSuggestion(player, "§eNövbəti addım: Mavi komanda divarını təyin et", "/o arena setwall " + arenaName + " blue");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("spectator")) {
            if (ArenaConfigManager.getSpectatorSpawn(arenaName) == null) {
                sendClickableSuggestion(player, "§eNövbəti addım: Spectator spawnını təyin et", "/o arena setspectspawn " + arenaName);
                return;
            }
        }

        // All steps complete
        sendClickableSuggestion(player, "§aBütün quruluş addımları tamamlandı! Arenanı bitir", "/o arena finish " + arenaName);
    }

    /**
     * Sends a clickable chat suggestion to the player
     * @param message The message to display
     * @param command The command to suggest when clicked
     */
    private static void sendClickableSuggestion(Player player, String message, String command) {
        TextComponent component = new TextComponent(message + " §f" + command);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[]{
            new net.md_5.bungee.api.chat.TextComponent("§eClick to insert: "),
            new net.md_5.bungee.api.chat.TextComponent("§f" + command)
        }));
        player.spigot().sendMessage(component);
    }

    /**
     * Suggests the next setup step for an arena (no completed step)
     */
    static void suggestNextSetupStep(Player player, String arenaName) {
        suggestNextSetupStep(player, arenaName, null);
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eUse /o cmds for a full command list.");
        player.sendMessage("§eQuick start: /o play - Join an arena");
    }

    private void resetPlayerState(Player player) {
        PlayerUtils.resetPlayerState(player);
    }

    private int getArenaPlayerCount(String arenaName) {
        int count = 0;
        for (String arena : playersInArena.values()) {
            if (arena.equals(arenaName)) {
                count++;
            }
        }
        return count;
    }

    private int getMaxPlayers(String arenaName) {
        return ArenaConfigManager.getMaxPlayers(arenaName);
    }

    private void giveLobbyItems(Player player) {
        // Komanda Seçimi Kompası (Slot 0)
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName("§eKomanda Seçimi");
            compass.setItemMeta(compassMeta);
        }
        player.getInventory().setItem(0, compass);

        // Oyundan Çıxış Qapısı (Slot 8)
        ItemStack door = new ItemStack(Material.OAK_DOOR);
        ItemMeta doorMeta = door.getItemMeta();
        if (doorMeta != null) {
            doorMeta.setDisplayName("§cOyundan Çıx");
            door.setItemMeta(doorMeta);
        }
        player.getInventory().setItem(8, door);
    }

    public static void broadcastToArena(String arenaName, String message) {
        for (UUID uuid : playersInArena.keySet()) {
            if (playersInArena.get(uuid).equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    arenaPlayer.sendMessage(message);
                }
            }
        }
    }

    private void sendWelcomeMessage(Player player) {
        List<String> welcomeLines = MessagesConfigManager.getMessagesConfig().getStringList("messages.welcome_message");
        for (String line : welcomeLines) {
            player.sendMessage(line);
        }
    }

    private void handleAdminCommand(Player player, String[] args) {
        // Check permission
        if (!player.hasPermission("obsidianwars.admin")) {
            player.sendMessage("§cYou don't have permission to use admin commands.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /o admin <addexp|addlevel|removexp|removelevel|setexp|setlevel> <player> <amount>");
            return;
        }

        String action = args[1].toLowerCase();

        if (args.length < 4) {
            player.sendMessage("§cUsage: /o admin " + action + " <player> <amount>");
            return;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer '" + targetName + "' is not online.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount: " + args[3]);
            return;
        }

        UUID targetUuid = target.getUniqueId();

        switch (action) {
            case "addexp":
                LevelManager.addXp(target, amount);
                player.sendMessage("§aAdded " + amount + " XP to " + target.getName());
                target.sendMessage("§eYou received " + amount + " XP from an admin!");
                break;

            case "addlevel":
                LevelManager.PlayerLevel playerLevel = LevelManager.getPlayerLevel(targetUuid);
                int newLevel = playerLevel.getLevel() + amount;
                playerLevel.setLevel(newLevel);
                player.sendMessage("§aSet " + target.getName() + "'s level to " + newLevel);
                target.sendMessage("§eYour level has been set to " + newLevel + " by an admin!");
                // Save to database
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            case "removexp":
                LevelManager.PlayerLevel xpLevel = LevelManager.getPlayerLevel(targetUuid);
                int currentXp = xpLevel.getCurrentXp();
                int newXP = Math.max(0, currentXp - amount);
                xpLevel.setXp(newXP);
                player.sendMessage("§aRemoved " + (currentXp - newXP) + " XP from " + target.getName());
                target.sendMessage("§e" + (currentXp - newXP) + " XP was removed by an admin!");
                // Save to database
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            case "removelevel":
                LevelManager.PlayerLevel levelData = LevelManager.getPlayerLevel(targetUuid);
                int currentLevel = levelData.getLevel();
                int newLevel2 = Math.max(1, currentLevel - amount);
                levelData.setLevel(newLevel2);
                player.sendMessage("§aReduced " + target.getName() + "'s level to " + newLevel2);
                target.sendMessage("§eYour level has been reduced to " + newLevel2 + " by an admin!");
                // Save to database
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            case "setexp":
                LevelManager.PlayerLevel setXpLevel = LevelManager.getPlayerLevel(targetUuid);
                setXpLevel.setXp(Math.max(0, amount));
                player.sendMessage("§aSet " + target.getName() + "'s XP to " + amount);
                target.sendMessage("§eYour XP has been set to " + amount + " by an admin!");
                // Save to database
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            case "setlevel":
                LevelManager.PlayerLevel setLevelData = LevelManager.getPlayerLevel(targetUuid);
                setLevelData.setLevel(Math.max(1, amount));
                player.sendMessage("§aSet " + target.getName() + "'s level to " + amount);
                target.sendMessage("§eYour level has been set to " + amount + " by an admin!");
                // Save to database
                StatsDAO.savePlayerLevel(targetUuid);
                break;

            default:
                player.sendMessage("§cUnknown admin action: " + action);
                player.sendMessage("§cAvailable actions: addexp, addlevel, removexp, removelevel, setexp, setlevel");
                break;
        }
    }

    private void sendRulesAnnouncement(Player player) {
        List<String> rulesLines = MessagesConfigManager.getMessagesConfig().getStringList("messages.rules_announcement");
        for (String line : rulesLines) {
            player.sendMessage(line);
        }
    }

    public void openTeamSelectionGUI(Player player) {
        // 9-slotalı inventory yaradırıq
        Inventory teamGUI = plugin.getServer().createInventory(null, 9, "§6Komanda Seçimi");

        // Qırmızı Komanda (Red Team) - Red Wool
        ItemStack redTeam = new ItemStack(org.bukkit.Material.RED_WOOL);
        ItemMeta redMeta = redTeam.getItemMeta();
        if (redMeta != null) {
            redMeta.setDisplayName("§cQırmızı Komanda");

            // Qırmızı komandadakı oyunçuların siyahısı
            List<String> redLore = new ArrayList<>();
            redLore.add("§7Üzvlər:");

            List<String> redPlayers = getPlayersInTeam("red");
            if (redPlayers.isEmpty()) {
                redLore.add("§cHeç kim yoxdur (0 oyunçu)");
            } else {
                for (String p : redPlayers) {
                    redLore.add("§7- " + p);
                }
                redLore.add("§7Cəmi: " + redPlayers.size() + " oyunçu");
            }

            redMeta.setLore(redLore);
            redTeam.setItemMeta(redMeta);
        }
        teamGUI.setItem(3, redTeam); // Sol tərəf

        // Mavi Komanda (Blue Team) - Blue Wool
        ItemStack blueTeam = new ItemStack(org.bukkit.Material.BLUE_WOOL);
        ItemMeta blueMeta = blueTeam.getItemMeta();
        if (blueMeta != null) {
            blueMeta.setDisplayName("§9Mavi Komanda");

            // Mavi komandadakı oyunçuların siyahısı
            List<String> blueLore = new ArrayList<>();
            blueLore.add("§7Üzvlər:");

            List<String> bluePlayers = getPlayersInTeam("blue");
            if (bluePlayers.isEmpty()) {
                blueLore.add("§9Heç kim yoxdur (0 oyunçu)");
            } else {
                for (String p : bluePlayers) {
                    blueLore.add("§7- " + p);
                }
                blueLore.add("§7Cəmi: " + bluePlayers.size() + " oyunçu");
            }

            blueMeta.setLore(blueLore);
            blueTeam.setItemMeta(blueMeta);
        }
        teamGUI.setItem(5, blueTeam); // Sağ tərəf

        player.openInventory(teamGUI);
    }

    private List<String> getPlayersInTeam(String team) {
        List<String> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : TeamListener.playerTeams.entrySet()) {
            if (entry.getValue().equals(team)) {
                // Oyunçunun adını əldə edirik
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player.getName());
                }
            }
        }
        return players;
    }

    private void openArenaSelectorGUI(Player player) {
        List<String> arenaNames = ArenaConfigManager.getArenaNames();

        if (arenaNames.isEmpty()) {
            player.sendMessage("§cNo arenas available.");
            return;
        }

        // Calculate inventory size (9 slots per row, minimum 9)
        int size = Math.max(9, ((arenaNames.size() + 8) / 9) * 9);
        Inventory gui = plugin.getServer().createInventory(null, size, "§6§lArena Selector");

        for (int i = 0; i < arenaNames.size(); i++) {
            String arenaName = arenaNames.get(i);
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            int currentPlayers = getArenaPlayerCount(arenaName);
            int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);

            ItemStack arenaItem = new ItemStack(Material.COMPASS);
            ItemMeta meta = arenaItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + arenaName);

                String statusColor;
                if (status.equals("READY") || status.equals("WAITING")) {
                    statusColor = "§a";
                } else if (status.equals("STARTED")) {
                    statusColor = "§c";
                } else if (status.equals("PLAYING")) {
                    statusColor = "§c";
                } else if (status.equals("DISABLED")) {
                    statusColor = "§8";
                } else {
                    statusColor = "§7";
                }

                List<String> lore = new ArrayList<>();
                lore.add("§7Status: " + statusColor + status);
                lore.add("§7Players: §f" + currentPlayers + "/" + maxPlayers);
                lore.add("");
                lore.add("§eClick to join!");

                meta.setLore(lore);
                arenaItem.setItemMeta(meta);
            }

            gui.setItem(i, arenaItem);
        }

        player.openInventory(gui);
    }

    /**
     * Hides all existing spectators from a player who just joined the arena.
     *
     * @param player The player who joined
     * @param arenaName The arena name
     */
    private void hideExistingSpectatorsFromPlayer(Player player, String arenaName) {
        for (Player spectator : SpectatorManager.getSpectatorsInArena(arenaName)) {
            player.hidePlayer(Obsidianwars.getInstance(), spectator);
        }
    }

    public static void cleanup() {
        playersInArena.clear();
    }
}
