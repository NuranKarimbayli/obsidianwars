package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.util.ColorUtils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages multi-language support for ObsidianWars.
 * Supports per-player language preferences and global plugin language.
 */
public class LanguageManager {

    private static LanguageManager instance;
    private static final String DEFAULT_LANGUAGE = "en";

    private final Map<String, FileConfiguration> languageConfigs = new HashMap<>();
    private final Set<String> availableLanguages = new HashSet<>();
    private String globalLanguage;

    public LanguageManager() {
        this.globalLanguage = Obsidianwars.getInstance().getConfig().getString("language", DEFAULT_LANGUAGE);
    }

    public static void initialize() {
        instance = new LanguageManager();
        instance.loadAllLanguages();
        Obsidianwars.getInstance().getLogger().info("LanguageManager initialized with " + instance.availableLanguages.size() + " languages");
    }

    public static LanguageManager getInstance() {
        return instance;
    }

    /**
     * Loads all language files from the languages folder.
     */
    public void loadAllLanguages() {
        File languagesFolder = new File(Obsidianwars.getInstance().getDataFolder(), "languages");

        // Create languages folder if it doesn't exist
        if (!languagesFolder.exists()) {
            languagesFolder.mkdirs();
            Obsidianwars.getInstance().getLogger().info("Created languages folder");
        }

        // Load default en.yml if it doesn't exist
        File enFile = new File(languagesFolder, "en.yml");
        if (!enFile.exists()) {
            Obsidianwars.getInstance().saveResource("languages/en.yml", false);
            Obsidianwars.getInstance().getLogger().info("Created default en.yml");
        }

        // Load all .yml files from languages folder
        File[] languageFiles = languagesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (languageFiles != null) {
            for (File file : languageFiles) {
                String langCode = file.getName().replace(".yml", "");
                loadLanguage(langCode, file);
            }
        }

        // Ensure default language is loaded
        if (!languageConfigs.containsKey(DEFAULT_LANGUAGE)) {
            Obsidianwars.getInstance().getLogger().warning("Default language " + DEFAULT_LANGUAGE + " not found!");
        }
    }

    /**
     * Loads a specific language file.
     */
    public void loadLanguage(String langCode, File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        languageConfigs.put(langCode, config);
        availableLanguages.add(langCode);
        Obsidianwars.getInstance().getLogger().info("Loaded language: " + langCode);
    }

    /**
     * Reloads all language files.
     */
    public void reloadLanguages() {
        languageConfigs.clear();
        availableLanguages.clear();
        loadAllLanguages();
        Obsidianwars.getInstance().getLogger().info("Language system reloaded");
    }

    /**
     * Gets a message for a specific player based on their language preference.
     * Falls back to global language, then to English.
     */
    public String getMessage(Player player, String key) {
        return getMessage(player.getUniqueId(), key);
    }

    /**
     * Gets a message for a specific player UUID based on their language preference.
     * Falls back to global language, then to English.
     */
    public String getMessage(UUID uuid, String key) {
        String playerLang = PlayerDataManager.getPlayerLanguage(uuid);
        return getMessage(playerLang, key);
    }

    /**
     * Gets a message for a specific language code.
     * Falls back to global language, then to English.
     */
    public String getMessage(String langCode, String key) {
        return formatMessage(resolveMessage(langCode, key));
    }

    /**
     * Resolves a message without applying color translation, so placeholders can
     * still introduce {@code &} / HEX codes that are translated afterwards.
     */
    private String resolveMessage(UUID uuid, String key) {
        return resolveMessage(PlayerDataManager.getPlayerLanguage(uuid), key);
    }

    private String resolveMessage(String langCode, String key) {
        if (langCode == null || langCode.isEmpty()) {
            langCode = globalLanguage;
        }

        String message = getMessageFromLanguage(langCode, key);

        if (message == null && !langCode.equals(globalLanguage)) {
            message = getMessageFromLanguage(globalLanguage, key);
        }

        if (message == null && !globalLanguage.equals(DEFAULT_LANGUAGE)) {
            message = getMessageFromLanguage(DEFAULT_LANGUAGE, key);
        }

        return message != null ? message : key;
    }

    /**
     * Gets a message from a specific language file.
     */
    private String getMessageFromLanguage(String langCode, String key) {
        FileConfiguration config = languageConfigs.get(langCode);
        if (config == null) {
            return null;
        }
        return config.getString("messages." + key, null);
    }

    private String formatMessage(String message) {
        return ColorUtils.translate(message);
    }

    /**
     * Gets a message with placeholder replacements for a specific player.
     */
    public String getMessage(Player player, String key, Map<String, String> placeholders) {
        return formatMessage(replacePlaceholders(resolveMessage(player.getUniqueId(), key), placeholders));
    }

    /**
     * Gets a message with placeholder replacements for a specific language.
     */
    public String getMessage(String langCode, String key, Map<String, String> placeholders) {
        return formatMessage(replacePlaceholders(resolveMessage(langCode, key), placeholders));
    }

    /**
     * Gets a message with placeholder replacements for a specific language (varargs).
     */
    public String getMessage(String langCode, String key, String... replacements) {
        String message = resolveMessage(langCode, key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return formatMessage(message);
    }

    /**
     * Gets a message with placeholder replacements (legacy varargs support).
     */
    public String getMessage(Player player, String key, String... replacements) {
        String message = resolveMessage(player.getUniqueId(), key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return formatMessage(message);
    }

    /**
     * Replaces placeholders in a message string.
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) return null;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    /**
     * Gets a list of messages (e.g. welcome/rules) with color translation applied.
     */
    public List<String> getMessageList(Player player, String key) {
        return ColorUtils.translate(resolveMessageList(player.getUniqueId(), key));
    }

    /**
     * Gets a list of messages using the global language.
     */
    public List<String> getGlobalMessageList(String key) {
        return ColorUtils.translate(resolveMessageList(globalLanguage, key));
    }

    private List<String> resolveMessageList(UUID uuid, String key) {
        return resolveMessageList(PlayerDataManager.getPlayerLanguage(uuid), key);
    }

    private List<String> resolveMessageList(String langCode, String key) {
        if (langCode == null || langCode.isEmpty()) {
            langCode = globalLanguage;
        }

        List<String> messages = getMessageListFromLanguage(langCode, key);

        if (messages == null && !langCode.equals(globalLanguage)) {
            messages = getMessageListFromLanguage(globalLanguage, key);
        }

        if (messages == null && !globalLanguage.equals(DEFAULT_LANGUAGE)) {
            messages = getMessageListFromLanguage(DEFAULT_LANGUAGE, key);
        }

        return messages != null ? messages : java.util.Collections.emptyList();
    }

    private List<String> getMessageListFromLanguage(String langCode, String key) {
        FileConfiguration config = languageConfigs.get(langCode);
        if (config == null) {
            return null;
        }
        List<String> list = config.getStringList("messages." + key);
        return list == null || list.isEmpty() ? null : list;
    }

    /**
     * Sends a message to a player in their preferred language.
     */
    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(player, key));
    }

    /**
     * Sends a message to a player with placeholders in their preferred language.
     */
    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getMessage(player, key, placeholders));
    }

    /**
     * Sends a message to a player with placeholders (legacy varargs support).
     */
    public void sendMessage(Player player, String key, String... replacements) {
        player.sendMessage(getMessage(player, key, replacements));
    }

    /**
     * Gets the global language setting.
     */
    public String getGlobalLanguage() {
        return globalLanguage;
    }

    /**
     * Sets the global language setting.
     */
    public void setGlobalLanguage(String langCode) {
        if (!availableLanguages.contains(langCode)) {
            Obsidianwars.getInstance().getLogger().warning("Language " + langCode + " is not available!");
            return;
        }
        this.globalLanguage = langCode;
        Obsidianwars.getInstance().getConfig().set("language", langCode);
        Obsidianwars.getInstance().saveConfig();
        Obsidianwars.getInstance().getLogger().info("Global language set to: " + langCode);
    }

    /**
     * Gets all available languages.
     */
    public Set<String> getAvailableLanguages() {
        return new HashSet<>(availableLanguages);
    }

    /**
     * Checks if a language is available.
     */
    public boolean isLanguageAvailable(String langCode) {
        return availableLanguages.contains(langCode);
    }

    /**
     * Gets a list of available languages as a formatted string.
     */
    public String getAvailableLanguagesString() {
        return String.join(", ", availableLanguages);
    }

    /**
     * Gets a message using the global language (for console/non-player contexts).
     */
    public String getGlobalMessage(String key) {
        return getMessage(globalLanguage, key);
    }

    /**
     * Gets a message using the global language with placeholders.
     */
    public String getGlobalMessage(String key, Map<String, String> placeholders) {
        return getMessage(globalLanguage, key, placeholders);
    }

    /**
     * Gets a message using the global language with placeholder replacements (varargs).
     */
    public String getGlobalMessage(String key, String... replacements) {
        return getMessage(globalLanguage, key, replacements);
    }
}
