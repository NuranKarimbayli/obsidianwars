package az.nuran.obsidianwars;

import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.commands.ObsidianTabCompleter;
import az.nuran.obsidianwars.handlers.ArenaSelectorGUIListener;
import az.nuran.obsidianwars.handlers.ChatListener;
import az.nuran.obsidianwars.handlers.GameListener;
import az.nuran.obsidianwars.handlers.LobbyItemClickListener;
import az.nuran.obsidianwars.handlers.SpectatorListener;
import az.nuran.obsidianwars.handlers.SpectatorTeleporterGUIListener;
import az.nuran.obsidianwars.handlers.StatsGUIListener;
import az.nuran.obsidianwars.handlers.StatsListener;
import az.nuran.obsidianwars.handlers.TeamListener;
import az.nuran.obsidianwars.handlers.WandListener;
import az.nuran.obsidianwars.handlers.XPAwardListener;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaFileManager;
import az.nuran.obsidianwars.managers.ArenaManager;
import az.nuran.obsidianwars.managers.ArenaSnapshotManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.DatabaseManager;
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
import az.nuran.obsidianwars.managers.TabListManager;
import az.nuran.obsidianwars.managers.TaskManager;
import az.nuran.obsidianwars.managers.TeamManager;
import az.nuran.obsidianwars.managers.WallManager;
import az.nuran.obsidianwars.managers.WorldRulesManager;
import az.nuran.obsidianwars.services.DeathMessagesConfigManager;
import az.nuran.obsidianwars.services.DebugManager;
import az.nuran.obsidianwars.services.ObsidianWarsExpansion;
import az.nuran.obsidianwars.services.ObsidianWarsMetrics;
import az.nuran.obsidianwars.services.PerformanceMonitor;
import az.nuran.obsidianwars.services.PlayerUtils;
import az.nuran.obsidianwars.services.TeamConfig;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Obsidianwars extends JavaPlugin {

    private static Obsidianwars instance;
    private ObsidianCommand obsidianCommand;

    /**
     * Parse a sound name from config, handling both old (minecraft:entity.ender_dragon.growl)
     * and new (ENTITY_ENDER_DRAGON_GROWL) formats
     */
    public static Sound parseSound(String soundName) {
        if (soundName == null) return null;

        try {
            // Handle namespaced format (minecraft:entity.ender_dragon.growl)
            if (soundName.contains(":")) {
                soundName = soundName.split(":")[1];
            }

            // Convert to enum format (ENTITY_ENDER_DRAGON_GROWL)
            soundName = soundName.toUpperCase().replace(".", "_");

            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            getInstance().getLogger().warning("Invalid sound: " + soundName);
            return null;
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("ObsidianWars plugini aktivlesdirildi!");

        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        // Reload config to ensure defaults are loaded
        reloadConfig();

        // Initialize database
        DatabaseManager.initialize();

        // Initialize core managers
        TaskManager.initialize(this);
        ArenaStateManager.initialize(getLogger());
        PerformanceMonitor.initialize(getLogger());
        QueueManager.initialize(getLogger());
        ArenaManager.initialize(getLogger());
        EconomyManager.initialize(getLogger());
        SuddenDeathManager.initialize(getLogger());

        // Load GameManager configuration
        GameManager.loadConfig();

        // Initialize configuration managers
        ArenaConfigManager.initialize();
        MessagesConfigManager.initialize();
        ScoreboardsConfigManager.initialize();
        KillStreaksConfigManager.initialize();
        DeathMessagesConfigManager.initialize();
        ResourceBlocksConfigManager.initialize();
        ParticleManager.loadConfig();
        ResourceBlockManager.loadAllowedResourceBlocks();

        // Komanda we TabCompleter Registrasiýasy
        obsidianCommand = new ObsidianCommand(this);
        if (getCommand("obsidian") != null) {
            getCommand("obsidian").setExecutor(obsidianCommand);
            getCommand("obsidian").setTabCompleter(new ObsidianTabCompleter(this));
        }

        // Register alias commands
        if (getCommand("join") != null) {
            getCommand("join").setExecutor(obsidianCommand);
            getCommand("join").setTabCompleter(new ObsidianTabCompleter(this));
        }
        if (getCommand("leave") != null) {
            getCommand("leave").setExecutor(obsidianCommand);
        }
        if (getCommand("stats") != null) {
            getCommand("stats").setExecutor(obsidianCommand);
        }
        if (getCommand("rejoin") != null) {
            getCommand("rejoin").setExecutor(obsidianCommand);
        }
        if (getCommand("spectate") != null) {
            getCommand("spectate").setExecutor(obsidianCommand);
            getCommand("spectate").setTabCompleter(new ObsidianTabCompleter(this));
        }

        // Event-i regisrasion etmek
        getServer().getPluginManager().registerEvents(new WandListener(), this);
        getServer().getPluginManager().registerEvents(new TeamListener(), this);
        getServer().getPluginManager().registerEvents(new LobbyItemClickListener(obsidianCommand), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(), this);
        getServer().getPluginManager().registerEvents(new ResourceBlockManager(), this);
        getServer().getPluginManager().registerEvents(new WorldRulesManager(), this);
        getServer().getPluginManager().registerEvents(new ArenaSelectorGUIListener(), this);
        getServer().getPluginManager().registerEvents(new StatsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new SpectatorTeleporterGUIListener(), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(), this);
        getServer().getPluginManager().registerEvents(new ArenaSnapshotManager(), this);
        getServer().getPluginManager().registerEvents(new StatsListener(), this);
        getServer().getPluginManager().registerEvents(new XPAwardListener(), this);

        // Initialize TabListManager
        TabListManager.initialize();

        // Initialize LobbyScoreboardManager
        LobbyScoreboardManager.initialize();

        // Cleanup any stuck arena statuses from server crash/shutdown
        cleanupStuckArenaStatuses();

        // Start periodic cleanup task for expired disconnect records
        startCleanupTask();

        // Start periodic task to reset expired time-framed stats (every hour)
        startStatsResetTask();

        // Hook into Vault economy (with fallback to built-in)
        try {
            EconomyManager.getInstance().hookVault();
        } catch (Exception e) {
            getLogger().warning("Failed to hook into Vault economy: " + e.getMessage());
            getLogger().info("Falling back to built-in economy system.");
        }

        // Register PlaceholderAPI expansion if available (with class loading protection)
        try {
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            ObsidianWarsExpansion.registerIfAvailable(this);
        } catch (ClassNotFoundException e) {
            getLogger().info("PlaceholderAPI not installed - placeholders will not be available.");
        } catch (NoClassDefFoundError e) {
            getLogger().info("PlaceholderAPI not installed - placeholders will not be available.");
        } catch (Exception e) {
            getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
        }

        // Initialize bStats metrics (with error protection - should never crash plugin)
        try {
            ObsidianWarsMetrics.initialize(this);
        } catch (Exception e) {
            getLogger().warning("Failed to initialize bStats metrics: " + e.getMessage());
            getLogger().info("Plugin will continue to function normally without metrics.");
        }

        // Start queue matchmaking
        if (getConfig().getBoolean("queue.enabled", true)) {
            QueueManager.getInstance().startMatchmaking();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ObsidianWars plugini dayandirildi!");

        // Stop queue matchmaking
        QueueManager.getInstance().stopMatchmaking();

        // Clear lobby items and reset state for all players in arenas
        if (ObsidianCommand.playersInArena != null) {
            for (java.util.UUID uuid : new java.util.ArrayList<>(ObsidianCommand.playersInArena.keySet())) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    // Remove from team tracking
                    TeamManager.removePlayerFromTeam(player);
                    ParticleManager.removeSpawnProtection(player);

                    // Reset player state (clears inventory, armor, effects, etc.)
                    PlayerUtils.resetPlayerArenaLeave(player);

                    getLogger().info("Cleared lobby items for player: " + player.getName());
                }
            }

            // Clear arena player tracking
            ObsidianCommand.playersInArena.clear();
        }

        // Cleanup static maps to prevent memory leaks
        WandListener.cleanup();
        TeamListener.cleanup();
        ObsidianCommand.cleanup();
        TabListManager.shutdown();

        // Cleanup all games to prevent task leaks
        GameManager.cleanupAllGames();

        // Cleanup managers
        ParticleManager.cleanup();
        WallManager.cleanup();
        MobSpawnerManager.cleanup();
        ScoreboardManager.cleanup();
        LobbyScoreboardManager.cleanup();
        ResourceBlockManager.cleanup();
        WorldRulesManager.cleanup();
        TeamManager.cleanup();
        SpectatorManager.cleanup();
        ArenaSnapshotManager.cleanup();
        DebugManager.cleanup();
        StatsManager.cleanup();
        XPAwardListener.cleanup();
        DatabaseManager.close();

        // Cleanup new managers
        TaskManager.getInstance().cleanup();
        ArenaStateManager.getInstance().cleanup();
        PerformanceMonitor.getInstance().cleanup();
        QueueManager.getInstance().cleanup();
        ArenaManager.getInstance().cleanup();
        EconomyManager.getInstance().cleanup();
        SuddenDeathManager.getInstance().cleanup();
    }

    private void startCleanupTask() {
        // Run cleanup task every minute to clear expired disconnect records (using TaskManager)
        TaskManager.getInstance().runTimer(
            "disconnect-cleanup",
            GameManager::cleanupExpiredDisconnectRecords,
            1200L,  // Initial delay: 1 minute (1200 ticks)
            1200L  // Period: Every minute (1200 ticks)
        );
    }

    private void startStatsResetTask() {
        // Run stats reset task every hour to clear expired time-framed stats (using TaskManager)
        TaskManager.getInstance().runAsyncTimer(
            "stats-reset",
            StatsManager::resetExpiredTimeFramedStats,
            72000L,  // Initial delay: 1 hour (72000 ticks)
            72000L  // Period: Every hour (72000 ticks)
        );
    }

    /**
     * Cleanup any stuck arena statuses from server crash/shutdown.
     * Resets arenas that are stuck in PLAYING, IN_GAME, or STARTING back to READY.
     */
    private void cleanupStuckArenaStatuses() {
        getLogger().info("Checking for stuck arena statuses from previous shutdown...");
        java.util.List<String> arenaNames = ArenaConfigManager.getArenaNames();
        int resetCount = 0;

        for (String arenaName : arenaNames) {
            String currentStatus = ArenaConfigManager.getArenaStatus(arenaName);
            getLogger().info("Arena " + arenaName + " status: " + currentStatus);

            // Check if arena is stuck in an active game state
            if (currentStatus != null && (currentStatus.equals("PLAYING") ||
                currentStatus.equals("IN_GAME") ||
                currentStatus.equals("STARTING"))) {

                getLogger().warning("Arena " + arenaName + " is stuck in " + currentStatus + " status. Resetting to READY...");
                ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.READY);
                resetCount++;

                // Stop any lingering tasks for this arena
                ParticleManager.stopAllArenaTasks(arenaName);
                MobSpawnerManager.stopMobSpawning(arenaName);
                WallManager.stopPreparationTimer(arenaName);
                WallManager.stopSuddenDeathCountdown(arenaName);
                XPAwardListener.stopPerMinuteTask(arenaName);
                TaskManager.getInstance().cancelArenaTasks(arenaName);

                // Restore world rules
                WorldRulesManager.restoreWorldRules(arenaName);

                // Restore arena snapshot if available
                boolean restoreSuccess = ArenaSnapshotManager.restoreSnapshot(arenaName);
                if (restoreSuccess) {
                    getLogger().info("Arena snapshot restored for " + arenaName);
                } else {
                    getLogger().warning("Arena snapshot restoration failed for " + arenaName);
                }
            }
        }

        if (resetCount > 0) {
            getLogger().info("Reset " + resetCount + " stuck arena(s) to READY status.");
        } else {
            getLogger().info("No stuck arena statuses found.");
        }
    }

    public static Obsidianwars getInstance() {
        return instance;
    }
}