package az.nuran.obsidianwars;

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

        // Initialize managers
        ArenaConfigManager.initialize();
        MessagesConfigManager.initialize();
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

        // Start periodic cleanup task for expired disconnect records
        startCleanupTask();

        // Start periodic task to reset expired time-framed stats (every hour)
        startStatsResetTask();
    }

    @Override
    public void onDisable() {
        getLogger().info("ObsidianWars plugini dayandirildi!");

        // Clear lobby items and reset state for all players in arenas
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
    }

    private void startCleanupTask() {
        // Run cleanup task every minute to clear expired disconnect records
        getServer().getScheduler().runTaskTimer(this, () -> {
            GameManager.cleanupExpiredDisconnectRecords();
        }, 1200L, 1200L); // Every minute (1200 ticks)
    }

    private void startStatsResetTask() {
        // Run stats reset task every hour to clear expired time-framed stats
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            StatsManager.resetExpiredTimeFramedStats();
        }, 72000L, 72000L); // Every hour (72000 ticks)
    }

    public static Obsidianwars getInstance() {
        return instance;
    }
}