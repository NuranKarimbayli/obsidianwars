package az.nuran.obsidianwars;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class DeathMessagesConfigManager {

    private static File deathMessagesFile;
    private static FileConfiguration deathMessagesConfig;
    private static final Obsidianwars plugin = Obsidianwars.getInstance();

    public static void initialize() {
        // Create death-messages.yml if it doesn't exist
        deathMessagesFile = new File(plugin.getDataFolder(), "death-messages.yml");
        if (!deathMessagesFile.exists()) {
            plugin.saveResource("death-messages.yml", false);
            plugin.getLogger().info("Death messages config file created: " + deathMessagesFile.getPath());
        }

        deathMessagesConfig = YamlConfiguration.loadConfiguration(deathMessagesFile);
    }

    public static void reloadDeathMessagesConfig() {
        if (deathMessagesFile != null) {
            deathMessagesConfig = YamlConfiguration.loadConfiguration(deathMessagesFile);
            plugin.getLogger().info("Death messages config reloaded");
        }
    }

    public static FileConfiguration getDeathMessagesConfig() {
        if (deathMessagesConfig == null) {
            initialize();
        }
        return deathMessagesConfig;
    }

    public static void saveDeathMessagesConfig() {
        try {
            getDeathMessagesConfig().save(deathMessagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save death messages config", e);
        }
    }
}
