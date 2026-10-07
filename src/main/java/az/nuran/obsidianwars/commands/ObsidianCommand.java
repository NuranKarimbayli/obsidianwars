package az.nuran.obsidianwars.commands;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.handlers.AdminCommandHandler;
import az.nuran.obsidianwars.commands.handlers.ArenaSetupCommandHandler;
import az.nuran.obsidianwars.commands.handlers.CommandHandler;
import az.nuran.obsidianwars.commands.handlers.ForceCommandHandler;
import az.nuran.obsidianwars.commands.handlers.GUICommandHandler;
import az.nuran.obsidianwars.commands.handlers.JoinLeaveCommandHandler;
import az.nuran.obsidianwars.commands.handlers.SpectateCommandHandler;
import az.nuran.obsidianwars.commands.handlers.StatsCommandHandler;
import az.nuran.obsidianwars.handlers.ChatListener;
import az.nuran.obsidianwars.handlers.MovementListener;
import az.nuran.obsidianwars.handlers.TeamListener;
import az.nuran.obsidianwars.handlers.WandListener;
import az.nuran.obsidianwars.handlers.XPAwardListener;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaSnapshotManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.EconomyManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.KillStreaksConfigManager;
import az.nuran.obsidianwars.managers.LevelManager;
import az.nuran.obsidianwars.managers.LobbyScoreboardManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.MobSpawnerManager;
import az.nuran.obsidianwars.managers.ParticleManager;
import az.nuran.obsidianwars.managers.QueueManager;
import az.nuran.obsidianwars.managers.ResourceBlockManager;
import az.nuran.obsidianwars.managers.ResourceBlocksConfigManager;
import az.nuran.obsidianwars.managers.ScoreboardManager;
import az.nuran.obsidianwars.managers.ScoreboardsConfigManager;
import az.nuran.obsidianwars.managers.SpectatorManager;
import az.nuran.obsidianwars.managers.StatsManager;
import az.nuran.obsidianwars.managers.SuddenDeathManager;
import az.nuran.obsidianwars.managers.TaskManager;
import az.nuran.obsidianwars.managers.TeamManager;
import az.nuran.obsidianwars.managers.WallManager;
import az.nuran.obsidianwars.managers.WorldRulesManager;
import az.nuran.obsidianwars.models.PlayerLevel;
import az.nuran.obsidianwars.models.PlayerStats;
import az.nuran.obsidianwars.services.DeathMessagesConfigManager;
import az.nuran.obsidianwars.services.DebugManager;
import az.nuran.obsidianwars.services.ObsidianWarsExpansion;
import az.nuran.obsidianwars.services.ObsidianWarsMetrics;
import az.nuran.obsidianwars.services.PerformanceMonitor;
import az.nuran.obsidianwars.services.PlayerUtils;
import az.nuran.obsidianwars.services.StatsDAO;
import az.nuran.obsidianwars.services.TeamConfig;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refactored command handler for ObsidianWars.
 * Supports new command structure with aliases and improved usability.
 */
public class ObsidianCommand implements CommandExecutor {

    private final Obsidianwars plugin;
    private static ObsidianCommand instance;

    // To track players in the arena (thread-safe for concurrent access)
    public static final Map<UUID, String> playersInArena = new ConcurrentHashMap<>();

    // Command handler dispatch map
    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();

    public ObsidianCommand(Obsidianwars plugin) {
        this.plugin = plugin;
        this.instance = this;
        registerHandlers();
    }

    public static ObsidianCommand getInstance() {
        return instance;
    }

    private void registerHandlers() {
        commandHandlers.put("play", new JoinLeaveCommandHandler());
        commandHandlers.put("join", new JoinLeaveCommandHandler());
        commandHandlers.put("leave", new JoinLeaveCommandHandler());
        commandHandlers.put("rejoin", new JoinLeaveCommandHandler());
        commandHandlers.put("stats", new StatsCommandHandler());
        commandHandlers.put("gui", new GUICommandHandler(plugin));
        commandHandlers.put("cmds", new GUICommandHandler(plugin));
        commandHandlers.put("arenalist", new GUICommandHandler(plugin));
        commandHandlers.put("team", new GUICommandHandler(plugin));
        commandHandlers.put("spectate", new SpectateCommandHandler());
        commandHandlers.put("queue", new SpectateCommandHandler());
        commandHandlers.put("arena", new ArenaSetupCommandHandler(plugin));
        commandHandlers.put("create", new ArenaSetupCommandHandler(plugin));
        commandHandlers.put("delete", new ArenaSetupCommandHandler(plugin));
        commandHandlers.put("wand", new ArenaSetupCommandHandler(plugin));
        commandHandlers.put("force", new ForceCommandHandler());
        commandHandlers.put("forceend", new ForceCommandHandler());
        commandHandlers.put("forcestart", new ForceCommandHandler());
        commandHandlers.put("forceprep", new ForceCommandHandler());
        commandHandlers.put("disablearena", new AdminCommandHandler(plugin));
        commandHandlers.put("enablearena", new AdminCommandHandler(plugin));
        commandHandlers.put("admin", new AdminCommandHandler(plugin));
        commandHandlers.put("reload", new AdminCommandHandler(plugin));
        commandHandlers.put("debug", new AdminCommandHandler(plugin));
    }

    private String getMessage(String key) {
        return MessagesConfigManager.getMessage(key);
    }

    private String getMessage(String key, String... replacements) {
        return MessagesConfigManager.getMessage(key, replacements);
    }

    /**
     * Checks if a player has the required permission.
     * If not, sends the configured no-permission message and returns false.
     *
     * @param player The player to check
     * @param permission The permission node (e.g., "obsidianwars.command.join")
     * @return true if the player has permission, false otherwise
     */
    private boolean checkPermission(Player player, String permission) {
        if (player.hasPermission(permission)) {
            return true;
        }

        String noPermMessage = plugin.getConfig().getString("no-permission-message", "&cYou don't have permission to use this command.");
        player.sendMessage(noPermMessage.replace("&", "§"));
        return false;
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
            if (!checkPermission(player, "obsidianwars.command.arena")) {
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(getMessage("usage_arena"));
                return true;
            }
            // Delegate to ArenaSetupCommandHandler
            commandHandlers.get("arena").handle(player, args);
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

        // ==================== QUEUE COMMAND ====================
        if (subCommand.equals("queue")) {
            handleQueueCommand(player, args);
            return true;
        }

        // ==================== DEBUG COMMAND ====================
        if (subCommand.equals("debug")) {
            handleDebugCommand(player, args);
            return true;
        }

        // ==================== RELOAD COMMAND ====================
        if (subCommand.equals("reload")) {
            handleReloadCommand(player, args);
            return true;
        }

        // ==================== ADMIN COMMANDS ====================
        if (subCommand.equals("admin")) {
            if (!checkPermission(player, "obsidianwars.command.admin")) {
                return true;
            }
            handleAdminCommand(player, args);
            return true;
        }

        // ==================== UNKNOWN COMMAND ====================
        player.sendMessage(getMessage("unknown_command"));
        return true;
    }

    // ==================== COMMAND HANDLERS ====================

    /**
     * Static method to handle player joining an arena.
     * This can be called from QueueManager without instantiating a new ObsidianCommand.
     *
     * @param player The player joining
     * @param arenaName The arena name to join (must be pre-validated)
     */
    public static void joinArena(Player player, String arenaName) {
        // Check if player is already in an active game
        UUID playerUuid = player.getUniqueId();
        String currentArena = playersInArena.get(playerUuid);
        if (currentArena != null) {
            GameManager.ArenaGame currentGame = GameManager.getGame(currentArena);
            if (currentGame != null && (currentGame.getGameState() == ArenaStateManager.ArenaState.PLAYING ||
                                        currentGame.getGameState() == ArenaStateManager.ArenaState.PREPARATION ||
                                        currentGame.getGameState() == ArenaStateManager.ArenaState.STARTING)) {
                player.sendMessage(MessagesConfigManager.getMessage("cannot_join_active_game"));
                return;
            }
        }

        // Use waiting spawn if available, otherwise fallback to lobby spawn
        Location lobbyLocation = ArenaConfigManager.getWaitingSpawn(arenaName);
        if (lobbyLocation == null) {
            lobbyLocation = ArenaConfigManager.getLobbySpawn(arenaName);
        }

        if (lobbyLocation == null) {
            player.sendMessage(MessagesConfigManager.getMessage("missing_lobby"));
            return;
        }

        World world = lobbyLocation.getWorld();
        if (world == null) {
            player.sendMessage(MessagesConfigManager.getMessage("world_not_found", "worldName", "Unknown"));
            return;
        }

        // Check player limit
        int currentPlayers = getArenaPlayerCount(arenaName);
        int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);
        if (currentPlayers >= maxPlayers) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_full", "current", String.valueOf(currentPlayers), "max", String.valueOf(maxPlayers)));
            return;
        }

        player.teleport(lobbyLocation);

        // Reset player state
        resetPlayerState(player);

        // Register player in arena
        playersInArena.put(player.getUniqueId(), arenaName);
        GameManager.addPlayerToArena(player.getUniqueId(), arenaName);

        // IMMEDIATE AUTO-ASSIGNMENT: Assign to balanced team right away
        String assignedTeam = TeamManager.autoAssignTeam(player, arenaName);
        String teamName = TeamConfig.getTeamName(assignedTeam);
        String teamColor = TeamConfig.getTeamColor(assignedTeam);
        player.sendMessage(MessagesConfigManager.getMessage("auto_team", "teamColor", teamColor, "teamName", teamName));

        // Invalidate team health cache for this arena
        ScoreboardManager.invalidateTeamHealthCache(arenaName);

        // Invalidate chat cache for this arena
        ChatListener.invalidateTeamCache(arenaName);

        // Give lobby items
        giveLobbyItems(player);

        // Set up scoreboard
        ScoreboardManager.updateScoreboard(player);

        // Broadcast message
        broadcastToArena(arenaName, MessagesConfigManager.getMessage("player_joined", "player", player.getName(), "current", String.valueOf(getArenaPlayerCount(arenaName)), "max", String.valueOf(getMaxPlayers(arenaName))));

        player.sendMessage(MessagesConfigManager.getMessage("join_lobby", "arenaName", arenaName));

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
        TaskManager.getInstance().runLater("rules-announce-" + player.getUniqueId(), () -> {
            sendRulesAnnouncement(player);
        }, 100L); // 5 seconds (100 ticks)

        // Check game start condition
        GameManager.checkGameStart(arenaName);
    }

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
            if ("DISABLED".equals(status)) {
                player.sendMessage("§cThis arena is currently disabled.");
                return;
            }
            if (!"READY".equals(status) && !"WAITING".equals(status)) {
                player.sendMessage("§cThis arena is not available (status: " + status + ")");
                return;
            }
            if ("STARTING".equals(status)) {
                player.sendMessage("§cThis arena is currently starting. Please wait for it to finish.");
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

        // Delegate to static joinArena method
        joinArena(player, arenaName);
    }

    private void handleLeaveCommand(Player player) {
        if (!playersInArena.containsKey(player.getUniqueId())) {
            player.sendMessage(getMessage("not_in_arena"));
            return;
        }

        String arenaName = playersInArena.get(player.getUniqueId());
        if (arenaName == null) {
            player.sendMessage(getMessage("not_in_arena"));
            return;
        }

        // Check if in countdown state
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        boolean wasInCountdown = (game != null && game.getGameState() == ArenaStateManager.ArenaState.STARTING);
        boolean wasInGame = (game != null && (game.getGameState() == ArenaStateManager.ArenaState.PLAYING || game.getGameState() == ArenaStateManager.ArenaState.PREPARATION));

        // Clear disconnect record for this player (manual leave, not disconnect)
        GameManager.clearDisconnectRecord(player.getUniqueId());

        // Remove player from arena
        playersInArena.remove(player.getUniqueId());
        GameManager.removePlayerFromArena(player.getUniqueId(), arenaName);
        TeamManager.removePlayerFromTeam(player);
        ParticleManager.removeSpawnProtection(player);

        // Invalidate team health cache for this arena
        ScoreboardManager.invalidateTeamHealthCache(arenaName);

        // Invalidate chat cache for this arena
        ChatListener.invalidateTeamCache(arenaName);

        // Reset player state and teleport to spawn
        Location mainSpawn = player.getWorld().getSpawnLocation();
        PlayerUtils.resetPlayerFull(player, mainSpawn);

        // Broadcast message
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

        // Check game status
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
        openArenaSelectorGUI(player, false);
    }

    private void handleAdminGUICommand(Player player) {
        if (!player.hasPermission("obsidianwars.admin")) {
            player.sendMessage("§cYou don't have permission to use admin GUI.");
            return;
        }
        openArenaSelectorGUI(player, true);
    }

    private void handleCmdsCommand(Player player) {
        player.sendMessage("§6§l=== ObsidianWars Commands ===");
        player.sendMessage("§ePlayer Commands:");
        player.sendMessage("  §a/o play [arena] §7- Join an arena (or random)");
        player.sendMessage("  §a/o join [arena] §7- Alias for play");
        player.sendMessage("  §a/o queue [arena] §7- Join queue for arena (or auto-assign)");
        player.sendMessage("  §a/o leave §7- Leave current arena");
        player.sendMessage("  §a/o rejoin §7- Rejoin after disconnect");
        player.sendMessage("  §a/o stats [player] §7- View player statistics");
        player.sendMessage("  §a/o gui §7- Open arena selector GUI");
        player.sendMessage("  §a/o team §7- Open team selection GUI");
        player.sendMessage("§eAdmin Commands:");
        player.sendMessage("  §a/o reload [all|config|arenas] §7- Reload plugin configurations");
        player.sendMessage("  §a/o arena teleport <arena> §7- Teleport to arena spectator spawn");
        player.sendMessage("  §a/o admin level <set|add|remove> <player> <amount> §7- Manage player levels");
        player.sendMessage("  §a/o admin coin <set|add|remove> <player> <amount> §7- Manage player coins");
        player.sendMessage("  §a/o admin exp <set|add|remove> <player> <amount> §7- Manage player XP");
        player.sendMessage("  §a/o admin stats reset <player> §7- Reset player statistics");
        player.sendMessage("  §a/o admin gui §7- Open admin arena selector (all statuses)");
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
        if (arenaName == null) {
            player.sendMessage(getMessage("not_in_arena"));
            return;
        }
        int countdown = GameManager.getCountdown(arenaName);

        if (countdown > 0 && countdown <= 5) {
            player.sendMessage("§cTeam switching is prohibited in the last 5 seconds!");
            return;
        }

        openTeamSelectionGUI(player);
    }

    private void handleWandCommand(Player player) {
        if (!checkPermission(player, "obsidianwars.command.wand")) {
            return;
        }

        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Obsidian Wars Wand");
            meta.setLore(Collections.singletonList("§eLeft and right click on blocks to select corners."));
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        player.sendMessage(getMessage("wand_received"));
    }

    private void handleDebugCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.debug")) {
            return;
        }

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

    private void handleReloadCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.reload")) {
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



    private void handleCreateCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.create")) {
            return;
        }

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

        // Rebuild arena world cache for MovementListener
        MovementListener.rebuildArenaWorldCache();

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
        if (!checkPermission(player, "obsidianwars.command.delete")) {
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /o delete arena <arenaName>");
            return;
        }
        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // Delete arena
        ArenaConfigManager.deleteArenaConfig(arenaName);

        // Rebuild arena world cache for MovementListener
        MovementListener.rebuildArenaWorldCache();

        // Remove game from GameManager
        GameManager.removeGame(arenaName);

        // Clear players in arena
        for (UUID uuid : new ArrayList<>(playersInArena.keySet())) {
            String playerArena = playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    // Remove player from arena
                    playersInArena.remove(uuid);
                    TeamManager.removePlayerFromTeam(arenaPlayer);

                    // Reset player state and teleport to spawn
                    Location mainSpawn = arenaPlayer.getWorld().getSpawnLocation();
                    PlayerUtils.resetPlayerFull(arenaPlayer, mainSpawn);

                    arenaPlayer.sendMessage("§cArena deleted, you have been removed from the arena!");

                    // Update lobby scoreboard
                    LobbyScoreboardManager.updateLobbyScoreboard(arenaPlayer);
                }
            }
        }

        player.sendMessage(getMessage("arena_deleted", "arenaName", arenaName));
    }

    private void handleForceendCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.forceend")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /o forceend <arenaName>");
            return;
        }
        String arenaName = args[1];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
            return;
        }

        // First, try to end the game normally if it exists
        GameManager.endGame(arenaName, null); // null = admin ended, no winner

        // ALWAYS forcefully reset arena status to READY regardless of whether game was running
        // This handles the edge case where arena is stuck in PLAYING without an active game instance
        String currentStatus = ArenaConfigManager.getArenaStatus(arenaName);
        if (!currentStatus.equals("READY") && !currentStatus.equals("WAITING")) {
            player.sendMessage("§eForcefully resetting arena status from " + currentStatus + " to READY...");
            ArenaConfigManager.setArenaStatus(arenaName, "READY");
        }

        // Stop all arena-related tasks to ensure cleanup
        ParticleManager.stopAllArenaTasks(arenaName);
        MobSpawnerManager.stopMobSpawning(arenaName);
        WallManager.stopPreparationTimer(arenaName);
        WallManager.stopSuddenDeathCountdown(arenaName);
        XPAwardListener.stopPerMinuteTask(arenaName);

        // Clear all dropped items in the arena
        GameManager.clearDroppedItems(arenaName);

        // Clear all non-player entities (mobs, dropped items, arrows, etc.) in the arena
        GameManager.clearArenaMobs(arenaName);

        // Restore world rules
        WorldRulesManager.restoreWorldRules(arenaName);

        // Restore arena snapshot if available
        boolean restoreSuccess = ArenaSnapshotManager.restoreSnapshot(arenaName);
        if (restoreSuccess) {
            player.sendMessage("§aArena snapshot restored.");
        } else {
            player.sendMessage("§7No arena snapshot found or restoration failed.");
        }

        // Clear any players still tracked in this arena
        java.util.List<java.util.UUID> playersToRemove = new java.util.ArrayList<>();
        for (java.util.UUID uuid : playersInArena.keySet()) {
            String playerArena = playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                playersToRemove.add(uuid);
            }
        }

        for (java.util.UUID uuid : playersToRemove) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                // Remove spectator mode if player was a spectator
                if (SpectatorManager.isSpectator(p)) {
                    SpectatorManager.removeSpectatorMode(p);
                }

                // Reset player state and teleport to spawn
                Location mainSpawn = p.getWorld().getSpawnLocation();
                PlayerUtils.resetPlayerFull(p, mainSpawn);
                p.sendMessage("§aArena force-ended by admin. Teleported to spawn.");

                // Update lobby scoreboard
                LobbyScoreboardManager.updateLobbyScoreboard(p);
            }

            // Remove from tracking
            playersInArena.remove(uuid);
            if (p != null) {
                TeamManager.removePlayerFromTeam(p);
            } else {
                // Player is offline, just remove from team map
                TeamListener.playerTeams.remove(uuid);
            }

            // Clean up wand positions
            WandListener.pos1Map.remove(uuid);
            WandListener.pos2Map.remove(uuid);
        }

        player.sendMessage("§aGame force-ended for arena " + arenaName + ". Status reset to READY.");
    }

    private void handleForcestartCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.forcestart")) {
            return;
        }

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
            ArenaStateManager.ArenaState state = game.getGameState();
            // If in COUNTDOWN state, cancel countdown and immediately start
            if (state == ArenaStateManager.ArenaState.STARTING) {
                GameManager.cancelCountdownAndForceStart(arenaName);
                player.sendMessage("§aCountdown skipped! Game starting now for arena " + arenaName);
                return;
            }
            // If already in PLAYING or PREPARATION state, return error
            if (state == ArenaStateManager.ArenaState.PLAYING || state == ArenaStateManager.ArenaState.PREPARATION) {
                player.sendMessage("§cThis arena is already active!");
                return;
            }
        }

        // Force start (no game exists or in WAITING state)
        GameManager.forceStartGame(arenaName);
        player.sendMessage(getMessage("force_started", "arenaName", arenaName));
    }

    private void handleForceprepCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.forceprep")) {
            return;
        }

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
        if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PREPARATION) {
            player.sendMessage("§cThis arena is not in preparation phase!");
            return;
        }

        // Force skip preparation - trigger wall removal immediately
        WallManager.forceSkipPreparation(arenaName);
        player.sendMessage("§aPreparation phase skipped! Walls are breaking now!");
    }

    private void handleForceCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.force")) {
            return;
        }

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
                ArenaStateManager.ArenaState state = game.getGameState();
                // If in COUNTDOWN state, cancel countdown and immediately start
                if (state == ArenaStateManager.ArenaState.STARTING) {
                    GameManager.cancelCountdownAndForceStart(arenaName);
                    player.sendMessage("§aCountdown skipped! Game starting now for arena " + arenaName);
                    return;
                }
                // If already in PLAYING or PREPARATION state, return error
                if (state == ArenaStateManager.ArenaState.PLAYING || state == ArenaStateManager.ArenaState.PREPARATION) {
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
                if (game == null || game.getGameState() != ArenaStateManager.ArenaState.PREPARATION) {
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
        if (!checkPermission(player, "obsidianwars.command.disablearena")) {
            return;
        }

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
        if (!checkPermission(player, "obsidianwars.command.enablearena")) {
            return;
        }

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

    private void handleArenaTeleportCommand(Player player, String[] args) {
        if (!checkPermission(player, "obsidianwars.command.arena")) {
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /o arena teleport <arena>");
            return;
        }

        String arenaName = args[2];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
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

    private void handleAdminStatsResetCommand(Player player, String[] args) {
        if (!player.hasPermission("obsidianwars.admin")) {
            player.sendMessage("§cYou don't have permission to use this command.");
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
        PlayerStats stats = StatsManager.getPlayerStats(target);
        if (stats != null) {
            stats.reset();
            StatsManager.savePlayerStats(target.getUniqueId(), target.getName());
            player.sendMessage("§aStatistics reset for player " + targetName);
            target.sendMessage("§cYour statistics have been reset by an admin.");
        } else {
            player.sendMessage("§cCould not find statistics for player " + targetName);
        }
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

    private void handleQueueCommand(Player player, String[] args) {
        String arenaName = null;

        // Check if arena specified
        if (args.length > 1) {
            arenaName = args[1];
        }

        // Join queue
        QueueManager.getInstance().joinQueue(player, arenaName);
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
    public static void suggestNextSetupStep(Player player, String arenaName, String completedStep) {
        // Optimized flow: After setting a team spawn, suggest setting that team's obsidian (you're already there)
        // After setting obsidian, suggest the opposite team's spawn

        // Check what's already set and suggest the next missing step
        // Skip the check for the step that was just completed to avoid race condition with async saves
        if (completedStep == null || !completedStep.equals("lobby")) {
            if (ArenaConfigManager.getLobbySpawn(arenaName) == null) {
                sendClickableSuggestion(player, "§eNext step: Set main lobby spawn", "/o arena setlobby " + arenaName);
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("waiting")) {
            if (ArenaConfigManager.getWaitingSpawn(arenaName) == null) {
                sendClickableSuggestion(player, "§eNext step: Set waiting lobby spawn", "/o arena setwaitingspawn " + arenaName);
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("waitingregion")) {
            if (ArenaConfigManager.getWaitingRegion(arenaName) == null) {
                sendClickableSuggestion(player, "§eNext step: Set waiting area boundary", "/o arena setwaitingregion " + arenaName);
                return;
            }
        }

        // Optimized team setup flow
        // If red spawn was just set, suggest red obsidian (you're at red base)
        if (completedStep != null && completedStep.equals("redspawn")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
                sendClickableSuggestion(player, "§eNext step: Set red team obsidian", "/o arena setobsidian " + arenaName + " red");
                return;
            }
        }

        // If red obsidian was just set, suggest blue spawn (go to other base)
        if (completedStep != null && completedStep.equals("redobsidian")) {
            if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNext step: Set blue team spawn", "/o arena setspawn " + arenaName + " blue");
                return;
            }
        }

        // If blue spawn was just set, suggest blue obsidian (you're at blue base)
        if (completedStep != null && completedStep.equals("bluespawn")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNext step: Set blue team obsidian", "/o arena setobsidian " + arenaName + " blue");
                return;
            }
        }

        // If blue obsidian was just set, suggest resource blocks
        if (completedStep != null && completedStep.equals("blueobsidian")) {
            if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
                sendClickableSuggestion(player, "§eNext step: Set up resource blocks", "/o arena setblocks " + arenaName);
                return;
            }
        }

        // Default checks for when no specific completed step or continuing from general flow
        if (completedStep == null || !completedStep.equals("redspawn")) {
            if (ArenaConfigManager.getTeamSpawn(arenaName, "red") == null) {
                sendClickableSuggestion(player, "§eNext step: Set red team spawn", "/o arena setspawn " + arenaName + " red");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("bluespawn")) {
            if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNext step: Set blue team spawn", "/o arena setspawn " + arenaName + " blue");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("redobsidian")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
                sendClickableSuggestion(player, "§eNext step: Set red team obsidian", "/o arena setobsidian " + arenaName + " red");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("blueobsidian")) {
            if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
                sendClickableSuggestion(player, "§eNext step: Set blue team obsidian", "/o arena setobsidian " + arenaName + " blue");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("resourceblocks")) {
            if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
                sendClickableSuggestion(player, "§eNext step: Set up resource blocks", "/o arena setblocks " + arenaName);
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("redwall")) {
            if (!ArenaConfigManager.hasWallConfiguration(arenaName, "red")) {
                sendClickableSuggestion(player, "§eNext step: Set red team wall", "/o arena setwall " + arenaName + " red");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("bluewall")) {
            if (!ArenaConfigManager.hasWallConfiguration(arenaName, "blue")) {
                sendClickableSuggestion(player, "§eNext step: Set blue team wall", "/o arena setwall " + arenaName + " blue");
                return;
            }
        }
        if (completedStep == null || !completedStep.equals("spectator")) {
            if (ArenaConfigManager.getSpectatorSpawn(arenaName) == null) {
                sendClickableSuggestion(player, "§eNext step: Set spectator spawn", "/o arena setspectspawn " + arenaName);
                return;
            }
        }

        // All steps complete
        sendClickableSuggestion(player, "§aAll setup steps completed! Finish arena", "/o arena finish " + arenaName);
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
    public static void suggestNextSetupStep(Player player, String arenaName) {
        suggestNextSetupStep(player, arenaName, null);
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eUse /o cmds for a full command list.");
        player.sendMessage("§eQuick start: /o play - Join an arena");
    }

    private static void resetPlayerState(Player player) {
        PlayerUtils.resetPlayerState(player);
    }

    public static int getArenaPlayerCount(String arenaName) {
        int count = 0;
        for (String arena : playersInArena.values()) {
            if (arena.equals(arenaName)) {
                count++;
            }
        }
        return count;
    }

    private static int getMaxPlayers(String arenaName) {
        return ArenaConfigManager.getMaxPlayers(arenaName);
    }

    private static void giveLobbyItems(Player player) {
        // Team Selection Compass (Slot 0)
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName("§eTeam Selection");
            compass.setItemMeta(compassMeta);
        }
        player.getInventory().setItem(0, compass);

        // Exit Game Door (Slot 8)
        ItemStack door = new ItemStack(Material.OAK_DOOR);
        ItemMeta doorMeta = door.getItemMeta();
        if (doorMeta != null) {
            doorMeta.setDisplayName("§cExit Game");
            door.setItemMeta(doorMeta);
        }
        player.getInventory().setItem(8, door);
    }

    public static void broadcastToArena(String arenaName, String message) {
        for (UUID uuid : playersInArena.keySet()) {
            String playerArena = playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    arenaPlayer.sendMessage(message);
                }
            }
        }
    }

    private static void sendWelcomeMessage(Player player) {
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
            player.sendMessage("§cUsage: /o admin <level|coin|exp|stats|gui> [action] [player] [amount]");
            return;
        }

        String category = args[1].toLowerCase();

        // Handle admin gui command
        if (category.equals("gui")) {
            handleAdminGUICommand(player);
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

            UUID targetUuid = target.getUniqueId();

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

    private void handleLevelCommand(Player player, Player target, UUID targetUuid, String action, int amount) {
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

    private void handleExpCommand(Player player, Player target, UUID targetUuid, String action, int amount) {
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

    private static void sendRulesAnnouncement(Player player) {
        List<String> rulesLines = MessagesConfigManager.getMessagesConfig().getStringList("messages.rules_announcement");
        for (String line : rulesLines) {
            player.sendMessage(line);
        }
    }

    public void openTeamSelectionGUI(Player player) {
        // Create 9-slot inventory
        Inventory teamGUI = plugin.getServer().createInventory(null, 9, "§6Team Selection");

        // Red Team - Red Wool
        ItemStack redTeam = new ItemStack(org.bukkit.Material.RED_WOOL);
        ItemMeta redMeta = redTeam.getItemMeta();
        if (redMeta != null) {
            redMeta.setDisplayName("§cRed Team");

            // List of players in red team
            List<String> redLore = new ArrayList<>();
            redLore.add("§7Members:");

            List<String> redPlayers = getPlayersInTeam("red");
            if (redPlayers.isEmpty()) {
                redLore.add("§cNo one (0 players)");
            } else {
                for (String p : redPlayers) {
                    redLore.add("§7- " + p);
                }
                redLore.add("§7Total: " + redPlayers.size() + " players");
            }

            redMeta.setLore(redLore);
            redTeam.setItemMeta(redMeta);
        }
        teamGUI.setItem(3, redTeam); // Left side

        // Blue Team - Blue Wool
        ItemStack blueTeam = new ItemStack(org.bukkit.Material.BLUE_WOOL);
        ItemMeta blueMeta = blueTeam.getItemMeta();
        if (blueMeta != null) {
            blueMeta.setDisplayName("§9Blue Team");

            // List of players in blue team
            List<String> blueLore = new ArrayList<>();
            blueLore.add("§7Members:");

            List<String> bluePlayers = getPlayersInTeam("blue");
            if (bluePlayers.isEmpty()) {
                blueLore.add("§9No one (0 players)");
            } else {
                for (String p : bluePlayers) {
                    blueLore.add("§7- " + p);
                }
                blueLore.add("§7Total: " + bluePlayers.size() + " players");
            }

            blueMeta.setLore(blueLore);
            blueTeam.setItemMeta(blueMeta);
        }
        teamGUI.setItem(5, blueTeam); // Right side

        player.openInventory(teamGUI);
    }

    private List<String> getPlayersInTeam(String team) {
        List<String> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : TeamListener.playerTeams.entrySet()) {
            if (entry.getValue().equals(team)) {
                // Get player name
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player.getName());
                }
            }
        }
        return players;
    }

    public void openArenaSelectorGUI(Player player, boolean isAdmin) {
        List<String> arenaNames = ArenaConfigManager.getArenaNames();

        if (arenaNames.isEmpty()) {
            player.sendMessage("§cNo arenas available.");
            return;
        }

        // Filter arenas based on admin mode
        List<String> filteredArenas = new ArrayList<>();
        for (String arenaName : arenaNames) {
            String status = ArenaConfigManager.getArenaStatus(arenaName);
            if (isAdmin) {
                // Admin mode: show all arenas regardless of status
                filteredArenas.add(arenaName);
            } else {
                // Player mode: only show READY and WAITING arenas (not STARTING or PLAYING)
                if (status.equals("READY") || status.equals("WAITING")) {
                    filteredArenas.add(arenaName);
                }
            }
        }

        if (filteredArenas.isEmpty()) {
            if (isAdmin) {
                player.sendMessage("§cNo arenas available.");
            } else {
                player.sendMessage("§cNo arenas are currently ready to join.");
            }
            return;
        }

        // Calculate inventory size (9 slots per row, minimum 9)
        int size = Math.max(9, ((filteredArenas.size() + 8) / 9) * 9);
        String title = isAdmin ? "§c§lAdmin Arena Selector" : "§6§lArena Selector";
        Inventory gui = plugin.getServer().createInventory(null, size, title);

        for (int i = 0; i < filteredArenas.size(); i++) {
            String arenaName = filteredArenas.get(i);
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

                List<String> lore = new ArrayList<>();
                lore.add("§7Status: " + statusColor + status);
                lore.add("§7Players: §f" + currentPlayers + "/" + maxPlayers);
                lore.add("");
                if (isAdmin) {
                    lore.add("§eClick to manage arena!");
                } else {
                    lore.add("§eClick to join!");
                }

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
    private static void hideExistingSpectatorsFromPlayer(Player player, String arenaName) {
        for (Player spectator : SpectatorManager.getSpectatorsInArena(arenaName)) {
            player.hidePlayer(Obsidianwars.getInstance(), spectator);
        }
    }

    /**
     * Checks if any arena is currently in an active game state.
     * Used to prevent reload during active games.
     *
     * @return true if any arena is in an active game state
     */
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

    public static void cleanup() {
        if (playersInArena != null) {
            playersInArena.clear();
        }
    }

    /**
     * Legacy method for handling complex arena setup commands.
     * This will be further refactored in the future.
     */
    public void handleArenaCommandLegacy(Player player, String[] args) {
        // For now, this delegates to the original arena command handling
        // This is a temporary measure during the refactoring process
        String action = args[1].toLowerCase();
        // Handle arena setup subcommands that weren't extracted yet
        // This includes: setlobby, setwaitingspawn, setwaitingregion, setplayers, setspawn, setobsidian, setblocks, setwall, settimer, setmobarea, setspectspawn
        // For brevity, we'll keep the original implementation for now
        // This can be further refactored into individual sub-handlers later
        // Note: Arena subcommands are now handled by ArenaSetupCommandHandler
    }
}
