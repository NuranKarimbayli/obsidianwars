package az.nuran.obsidianwars;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ResourceBlocksConfigManager {

    private static File resourceBlocksFile;
    private static FileConfiguration resourceBlocksConfig;
    private static final Obsidianwars plugin = Obsidianwars.getInstance();

    public static void initialize() {
        // Create resource-blocks.yml if it doesn't exist
        resourceBlocksFile = new File(plugin.getDataFolder(), "resource-blocks.yml");
        if (!resourceBlocksFile.exists()) {
            plugin.saveResource("resource-blocks.yml", false);
            plugin.getLogger().info("Resource blocks config file created: " + resourceBlocksFile.getPath());
        }

        resourceBlocksConfig = YamlConfiguration.loadConfiguration(resourceBlocksFile);
    }

    public static void reloadResourceBlocksConfig() {
        if (resourceBlocksFile != null) {
            resourceBlocksConfig = YamlConfiguration.loadConfiguration(resourceBlocksFile);
            plugin.getLogger().info("Resource blocks config reloaded");
        }
    }

    public static FileConfiguration getResourceBlocksConfig() {
        if (resourceBlocksConfig == null) {
            initialize();
        }
        return resourceBlocksConfig;
    }

    public static void saveResourceBlocksConfig() {
        try {
            getResourceBlocksConfig().save(resourceBlocksFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save resource blocks config", e);
        }
    }
}
