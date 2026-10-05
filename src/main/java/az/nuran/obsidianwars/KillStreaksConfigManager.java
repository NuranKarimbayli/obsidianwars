package az.nuran.obsidianwars;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class KillStreaksConfigManager {

    private static File killStreaksFile;
    private static FileConfiguration killStreaksConfig;
    private static final Obsidianwars plugin = Obsidianwars.getInstance();

    public static void initialize() {
        // Create kill-streaks.yml if it doesn't exist
        killStreaksFile = new File(plugin.getDataFolder(), "kill-streaks.yml");
        if (!killStreaksFile.exists()) {
            plugin.saveResource("kill-streaks.yml", false);
            plugin.getLogger().info("Kill streaks config file created: " + killStreaksFile.getPath());
        }

        killStreaksConfig = YamlConfiguration.loadConfiguration(killStreaksFile);
    }

    public static void reloadKillStreaksConfig() {
        if (killStreaksFile != null) {
            killStreaksConfig = YamlConfiguration.loadConfiguration(killStreaksFile);
            plugin.getLogger().info("Kill streaks config reloaded");
        }
    }

    public static FileConfiguration getKillStreaksConfig() {
        if (killStreaksConfig == null) {
            initialize();
        }
        return killStreaksConfig;
    }

    public static void saveKillStreaksConfig() {
        try {
            getKillStreaksConfig().save(killStreaksFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kill streaks config", e);
        }
    }
}
