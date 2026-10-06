package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Sudden Death Manager for ObsidianWars.
 * Handles sudden death mode with gameplay modifiers to force game completion.
 */
public class SuddenDeathManager {

    private static SuddenDeathManager instance;
    private final Logger logger;

    // Map of arena names to their sudden death data
    private final Map<String, SuddenDeathData> suddenDeathArenas;

    // Gameplay modifiers (configurable)
    private boolean disableRespawns;
    private boolean applyBorderContraction;
    private boolean applyWitherEffect;
    private boolean applyStrengthEffect;
    private boolean applyGlowingEffect;
    private int borderContractionSpeed;
    private int witherEffectDuration;
    private int strengthEffectAmplifier;
    private int glowingEffectDuration;

    public SuddenDeathManager(Logger logger) {
        this.logger = logger;
        this.suddenDeathArenas = new HashMap<>();
        loadConfig();
    }

    /**
     * Initializes the SuddenDeathManager singleton.
     *
     * @param logger The plugin logger
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new SuddenDeathManager(logger);
        }
    }

    /**
     * Gets the SuddenDeathManager instance.
     *
     * @return The SuddenDeathManager instance
     */
    public static SuddenDeathManager getInstance() {
        return instance;
    }

    /**
     * Loads sudden death configuration from config.yml.
     */
    public void loadConfig() {
        if (!Obsidianwars.getInstance().getConfig().contains("sudden-death")) {
            logger.info("Sudden death configuration not found in config.yml - using defaults");
            this.disableRespawns = true;
            this.applyBorderContraction = false;
            this.applyWitherEffect = true;
            this.applyStrengthEffect = true;
            this.applyGlowingEffect = true;
            this.borderContractionSpeed = 10;
            this.witherEffectDuration = 10;
            this.strengthEffectAmplifier = 1;
            this.glowingEffectDuration = 999999;
            return;
        }

        this.disableRespawns = Obsidianwars.getInstance().getConfig().getBoolean("sudden-death.disable-respawns", true);
        this.applyBorderContraction = Obsidianwars.getInstance().getConfig().getBoolean("sudden-death.border-contraction", false);
        this.applyWitherEffect = Obsidianwars.getInstance().getConfig().getBoolean("sudden-death.wither-effect", true);
        this.applyStrengthEffect = Obsidianwars.getInstance().getConfig().getBoolean("sudden-death.strength-effect", true);
        this.applyGlowingEffect = Obsidianwars.getInstance().getConfig().getBoolean("sudden-death.glowing-effect", true);
        this.borderContractionSpeed = Obsidianwars.getInstance().getConfig().getInt("sudden-death.border-contraction-speed", 10);
        this.witherEffectDuration = Obsidianwars.getInstance().getConfig().getInt("sudden-death.wither-duration", 10);
        this.strengthEffectAmplifier = Obsidianwars.getInstance().getConfig().getInt("sudden-death.strength-amplifier", 1);
        this.glowingEffectDuration = Obsidianwars.getInstance().getConfig().getInt("sudden-death.glowing-duration", 999999);

        logger.info("Sudden death configuration loaded from config");
    }

    /**
     * Triggers sudden death mode for an arena.
     *
     * @param arenaName The arena name
     */
    public void triggerSuddenDeath(String arenaName) {
        if (suddenDeathArenas.containsKey(arenaName)) {
            logger.warning("Sudden death already active for arena " + arenaName);
            return;
        }

        logger.info("Triggering sudden death for arena " + arenaName);

        // Update arena state
        ArenaStateManager.getInstance().setState(arenaName, ArenaStateManager.ArenaState.SUDDEN_DEATH);

        // Create sudden death data
        SuddenDeathData data = new SuddenDeathData(arenaName);
        suddenDeathArenas.put(arenaName, data);

        // Apply gameplay modifiers
        applyGameplayModifiers(arenaName, data);

        // Broadcast sudden death message
        broadcastSuddenDeathMessage(arenaName);

        // Start sudden death effects
        startSuddenDeathEffects(arenaName, data);
    }

    /**
     * Ends sudden death mode for an arena.
     *
     * @param arenaName The arena name
     */
    public void endSuddenDeath(String arenaName) {
        SuddenDeathData data = suddenDeathArenas.remove(arenaName);
        if (data == null) {
            return;
        }

        logger.info("Ending sudden death for arena " + arenaName);

        // Remove gameplay modifiers
        removeGameplayModifiers(arenaName, data);

        // Stop effects
        stopSuddenDeathEffects(arenaName, data);
    }

    /**
     * Applies gameplay modifiers for sudden death.
     *
     * @param arenaName The arena name
     * @param data The sudden death data
     */
    private void applyGameplayModifiers(String arenaName, SuddenDeathData data) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    // Apply wither effect
                    if (applyWitherEffect) {
                        player.addPotionEffect(new PotionEffect(
                            PotionEffectType.WITHER,
                            witherEffectDuration * 20,  // Convert to ticks
                            0,  // Amplifier 0 (I)
                            false  // No particles (handled separately)
                        ));
                    }

                    // Apply strength effect
                    if (applyStrengthEffect) {
                        player.addPotionEffect(new PotionEffect(
                            PotionEffectType.STRENGTH,
                            glowingEffectDuration * 20,
                            strengthEffectAmplifier,
                            false
                        ));
                    }

                    // Apply glowing effect
                    if (applyGlowingEffect) {
                        player.addPotionEffect(new PotionEffect(
                            PotionEffectType.GLOWING,
                            glowingEffectDuration * 20,
                            0,
                            false
                        ));
                    }

                    // Add to boss bar
                    data.bossBar.addPlayer(player);
                }
            }
        }

        // Apply border contraction if enabled
        if (applyBorderContraction) {
            startBorderContraction(arenaName, data);
        }
    }

    /**
     * Removes gameplay modifiers after sudden death ends.
     *
     * @param arenaName The arena name
     * @param data The sudden death data
     */
    private void removeGameplayModifiers(String arenaName, SuddenDeathData data) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    // Remove potion effects
                    player.removePotionEffect(PotionEffectType.WITHER);
                    player.removePotionEffect(PotionEffectType.STRENGTH);
                    player.removePotionEffect(PotionEffectType.GLOWING);

                    // Remove from boss bar
                    data.bossBar.removePlayer(player);
                }
            }
        }

        // Stop border contraction
        if (data.borderTask != null) {
            data.borderTask.cancel();
        }
    }

    /**
     * Starts border contraction for the arena.
     *
     * @param arenaName The arena name
     * @param data The sudden death data
     */
    private void startBorderContraction(String arenaName, SuddenDeathData data) {
        // This would require implementing world border management
        // For now, we'll log a placeholder
        logger.info("Border contraction not yet implemented for arena " + arenaName);
    }

    /**
     * Broadcasts sudden death message to all players in the arena.
     *
     * @param arenaName The arena name
     */
    private void broadcastSuddenDeathMessage(String arenaName) {
        String title = MessagesConfigManager.getMessage("sudden_death_title");
        String subtitle = MessagesConfigManager.getMessage("sudden_death_subtitle");
        String message = MessagesConfigManager.getMessage("sudden_death_message");

        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendTitle(title, subtitle, 10, 60, 20);
                    player.sendMessage(message);

                    // Play sound
                    String soundName = MessagesConfigManager.getMessage("sudden_death_sound");
                    Sound sound = Obsidianwars.parseSound(soundName);
                    if (sound != null) {
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    /**
     * Starts sudden death visual effects.
     *
     * @param arenaName The arena name
     * @param data The sudden death data
     */
    private void startSuddenDeathEffects(String arenaName, SuddenDeathData data) {
        // Start particle effect task
        data.particleTask = TaskManager.getInstance().runTimer(
            "sudden-death-particles-" + arenaName,
            () -> spawnSuddenDeathParticles(arenaName),
            0L,
            10L,  // Every 0.5 seconds
            arenaName
        );
    }

    /**
     * Spawns sudden death particles for all players in the arena.
     *
     * @param arenaName The arena name
     */
    private void spawnSuddenDeathParticles(String arenaName) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    Location loc = player.getLocation();
                    player.spawnParticle(Particle.DRAGON_BREATH, loc, 10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }

    /**
     * Stops sudden death effects.
     *
     * @param arenaName The arena name
     * @param data The sudden death data
     */
    private void stopSuddenDeathEffects(String arenaName, SuddenDeathData data) {
        if (data.particleTask != null) {
            data.particleTask.cancel();
        }
        data.bossBar.removeAll();
    }

    /**
     * Checks if respawns are disabled in sudden death.
     *
     * @return true if respawns are disabled
     */
    public boolean isRespawnDisabled() {
        return disableRespawns;
    }

    /**
     * Checks if sudden death is active for an arena.
     *
     * @param arenaName The arena name
     * @return true if sudden death is active
     */
    public boolean isSuddenDeathActive(String arenaName) {
        return suddenDeathArenas.containsKey(arenaName);
    }

    /**
     * Reloads sudden death configuration.
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Cleans up the SuddenDeathManager (called on plugin disable).
     */
    public void cleanup() {
        for (SuddenDeathData data : suddenDeathArenas.values()) {
            stopSuddenDeathEffects(null, data);
        }
        suddenDeathArenas.clear();
        instance = null;
    }

    /**
     * Data class for sudden death arena state.
     */
    private static class SuddenDeathData {
        private final String arenaName;
        private final BossBar bossBar;
        private org.bukkit.scheduler.BukkitTask particleTask;
        private org.bukkit.scheduler.BukkitTask borderTask;

        public SuddenDeathData(String arenaName) {
            this.arenaName = arenaName;
            this.bossBar = Bukkit.createBossBar(
                "§c§lSUDDEN DEATH",
                BarColor.RED,
                BarStyle.SEGMENTED_10
            );
            this.bossBar.setProgress(1.0);
        }
    }
}
