package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class MessagesConfigManager {

    private static File messagesFile;
    private static FileConfiguration messagesConfig;
    private static final Obsidianwars plugin = Obsidianwars.getInstance();

    public static void initialize() {
        // Create messages.yml if it doesn't exist
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            plugin.getLogger().info("Messages config file created: " + messagesFile.getPath());
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static void reloadMessagesConfig() {
        if (messagesFile != null) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
            plugin.getLogger().info("Messages config reloaded");
        }
    }

    public static FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            initialize();
        }
        return messagesConfig;
    }

    public static String getMessage(String key) {
        return getMessagesConfig().getString("messages." + key, key);
    }

    public static String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }

    public static String getSound(String key) {
        // Read sounds from config.yml instead of messages.yml
        return plugin.getConfig().getString("sounds." + key);
    }

    public static void saveMessagesConfig() {
        try {
            getMessagesConfig().save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save messages config", e);
        }
    }
}