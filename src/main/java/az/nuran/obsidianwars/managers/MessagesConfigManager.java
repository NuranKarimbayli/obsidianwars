package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

/**
 * Legacy compatibility class that delegates to LanguageManager.
 * This class maintains backward compatibility with existing code.
 */
public class MessagesConfigManager {

    private static File messagesFile;
    private static FileConfiguration messagesConfig;
    private static final Obsidianwars plugin = Obsidianwars.getInstance();

    public static void initialize() {
        // Create messages.yml if it doesn't exist (for backward compatibility)
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
        // Also reload language system
        LanguageManager.getInstance().reloadLanguages();
    }

    public static FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            initialize();
        }
        return messagesConfig;
    }

    /**
     * Gets a message using the global language (for non-player contexts).
     * Delegates to LanguageManager.
     */
    public static String getMessage(String key) {
        if (LanguageManager.getInstance() != null) {
            return LanguageManager.getInstance().getGlobalMessage(key);
        }
        return getMessagesConfig().getString("messages." + key, key);
    }

    /**
     * Gets a message with placeholder replacements using global language.
     * Delegates to LanguageManager.
     */
    public static String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }

    /**
     * Gets a message for a specific player based on their language preference.
     * Delegates to LanguageManager.
     */
    public static String getMessage(Player player, String key) {
        if (LanguageManager.getInstance() != null) {
            return LanguageManager.getInstance().getMessage(player, key);
        }
        return getMessage(key);
    }

    /**
     * Gets a message for a specific player with placeholder replacements.
     * Delegates to LanguageManager.
     */
    public static String getMessage(Player player, String key, Map<String, String> placeholders) {
        if (LanguageManager.getInstance() != null) {
            return LanguageManager.getInstance().getMessage(player, key, placeholders);
        }
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    /**
     * Gets a message for a specific player with placeholder replacements (varargs).
     * Delegates to LanguageManager.
     */
    public static String getMessage(Player player, String key, String... replacements) {
        if (LanguageManager.getInstance() != null) {
            return LanguageManager.getInstance().getMessage(player, key, replacements);
        }
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