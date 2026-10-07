package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages per-player data including language preferences.
 * Stores data in player-data.yml file.
 */
public class PlayerDataManager {

    private static PlayerDataManager instance;
    private static File playerDataFile;
    private static FileConfiguration playerDataConfig;

    // In-memory cache for faster access
    private final Map<UUID, String> playerLanguages = new ConcurrentHashMap<>();

    public PlayerDataManager() {
        loadPlayerData();
    }

    public static void initialize() {
        instance = new PlayerDataManager();
        Obsidianwars.getInstance().getLogger().info("PlayerDataManager initialized");
    }

    public static PlayerDataManager getInstance() {
        return instance;
    }

    /**
     * Loads player data from file.
     */
    private void loadPlayerData() {
        playerDataFile = new File(Obsidianwars.getInstance().getDataFolder(), "player-data.yml");

        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
                Obsidianwars.getInstance().getLogger().info("Created player-data.yml");
            } catch (IOException e) {
                Obsidianwars.getInstance().getLogger().log(Level.SEVERE, "Failed to create player-data.yml", e);
            }
        }

        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);

        // Load player languages into memory
        if (playerDataConfig.contains("languages")) {
            for (String uuidStr : playerDataConfig.getConfigurationSection("languages").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String lang = playerDataConfig.getString("languages." + uuidStr);
                    playerLanguages.put(uuid, lang);
                } catch (IllegalArgumentException e) {
                    Obsidianwars.getInstance().getLogger().warning("Invalid UUID in player-data.yml: " + uuidStr);
                }
            }
        }

        Obsidianwars.getInstance().getLogger().info("Loaded " + playerLanguages.size() + " player language preferences");
    }

    /**
     * Saves player data to file.
     */
    public void savePlayerData() {
        try {
            // Save player languages
            for (Map.Entry<UUID, String> entry : playerLanguages.entrySet()) {
                playerDataConfig.set("languages." + entry.getKey().toString(), entry.getValue());
            }

            playerDataConfig.save(playerDataFile);
            Obsidianwars.getInstance().getLogger().info("Player data saved");
        } catch (IOException e) {
            Obsidianwars.getInstance().getLogger().log(Level.SEVERE, "Failed to save player-data.yml", e);
        }
    }

    /**
     * Reloads player data from file.
     */
    public void reloadPlayerData() {
        playerLanguages.clear();
        loadPlayerData();
    }

    /**
     * Gets a player's language preference.
     * Returns null if not set (will use global language).
     */
    public static String getPlayerLanguage(UUID uuid) {
        if (instance == null) return null;
        return instance.playerLanguages.get(uuid);
    }

    /**
     * Gets a player's language preference.
     * Returns null if not set (will use global language).
     */
    public static String getPlayerLanguage(org.bukkit.entity.Player player) {
        return getPlayerLanguage(player.getUniqueId());
    }

    /**
     * Sets a player's language preference.
     */
    public void setPlayerLanguage(UUID uuid, String langCode) {
        playerLanguages.put(uuid, langCode);
        savePlayerData();
    }

    /**
     * Sets a player's language preference.
     */
    public void setPlayerLanguage(org.bukkit.entity.Player player, String langCode) {
        setPlayerLanguage(player.getUniqueId(), langCode);
    }

    /**
     * Removes a player's language preference (reverts to global).
     */
    public void removePlayerLanguage(UUID uuid) {
        playerLanguages.remove(uuid);
        playerDataConfig.set("languages." + uuid.toString(), null);
        savePlayerData();
    }

    /**
     * Removes a player's language preference (reverts to global).
     */
    public void removePlayerLanguage(org.bukkit.entity.Player player) {
        removePlayerLanguage(player.getUniqueId());
    }

    /**
     * Gets all player language preferences.
     */
    public Map<UUID, String> getAllPlayerLanguages() {
        return new ConcurrentHashMap<>(playerLanguages);
    }

    /**
     * Cleanup method.
     */
    public void cleanup() {
        savePlayerData();
        playerLanguages.clear();
    }
}
