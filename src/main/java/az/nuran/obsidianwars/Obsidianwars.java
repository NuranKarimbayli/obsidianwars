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

        saveDefaultConfig();

        // Initialize managers
        ArenaConfigManager.initialize();
        MessagesConfigManager.initialize();
        ParticleManager.loadConfig();

        // Komanda we TabCompleter Registrasiýasy
        obsidianCommand = new ObsidianCommand(this);
        if (getCommand("obsidian") != null) {
            getCommand("obsidian").setExecutor(obsidianCommand);
            getCommand("obsidian").setTabCompleter(new ObsidianTabCompleter(this));
        }

        // Event-i regisrasion etmek
        getServer().getPluginManager().registerEvents(new WandListener(), this);
        getServer().getPluginManager().registerEvents(new TeamListener(), this);
        getServer().getPluginManager().registerEvents(new LobbyItemClickListener(obsidianCommand), this);
        getServer().getPluginManager().registerEvents(new GameListener(), this);
        getServer().getPluginManager().registerEvents(new ResourceBlockManager(), this);
        getServer().getPluginManager().registerEvents(new WorldRulesManager(), this);

        // Start periodic cleanup task for expired disconnect records
        startCleanupTask();
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

        // Cleanup all games to prevent task leaks
        GameManager.cleanupAllGames();

        // Cleanup managers
        ParticleManager.cleanup();
        WallManager.cleanup();
        MobSpawnerManager.cleanup();
        ScoreboardManager.cleanup();
        ResourceBlockManager.cleanup();
        WorldRulesManager.cleanup();
        TeamManager.cleanup();
    }

    private void startCleanupTask() {
        // Run cleanup task every minute to clear expired disconnect records
        getServer().getScheduler().runTaskTimer(this, () -> {
            GameManager.cleanupExpiredDisconnectRecords();
        }, 1200L, 1200L); // Every minute (1200 ticks)
    }

    public static Obsidianwars getInstance() {
        return instance;
    }
}