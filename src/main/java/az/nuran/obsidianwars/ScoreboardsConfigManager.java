package az.nuran.obsidianwars;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ScoreboardsConfigManager {

    private static File scoreboardsFile;
    private static FileConfiguration scoreboardsConfig;
    private static final Obsidianwars plugin = Obsidianwars.getInstance();

    public static void initialize() {
        // Create scoreboards.yml if it doesn't exist
        scoreboardsFile = new File(plugin.getDataFolder(), "scoreboards.yml");
        if (!scoreboardsFile.exists()) {
            plugin.saveResource("scoreboards.yml", false);
            plugin.getLogger().info("Scoreboards config file created: " + scoreboardsFile.getPath());
        }

        scoreboardsConfig = YamlConfiguration.loadConfiguration(scoreboardsFile);
    }

    public static void reloadScoreboardsConfig() {
        if (scoreboardsFile != null) {
            scoreboardsConfig = YamlConfiguration.loadConfiguration(scoreboardsFile);
            plugin.getLogger().info("Scoreboards config reloaded");
        }
    }

    public static FileConfiguration getScoreboardsConfig() {
        if (scoreboardsConfig == null) {
            initialize();
        }
        return scoreboardsConfig;
    }

    public static void saveScoreboardsConfig() {
        try {
            getScoreboardsConfig().save(scoreboardsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save scoreboards config", e);
        }
    }
}
