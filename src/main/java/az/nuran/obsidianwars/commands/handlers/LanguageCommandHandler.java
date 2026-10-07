package az.nuran.obsidianwars.commands.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.managers.LanguageManager;
import az.nuran.obsidianwars.managers.PlayerDataManager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles language-related commands: /lang, /language
 */
public class LanguageCommandHandler implements CommandHandler {

    private final Obsidianwars plugin;

    public LanguageCommandHandler(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (args.length == 0) {
            showCurrentLanguage(player);
            return true;
        }

        String langCode = args[0].toLowerCase();

        // Handle list command
        if (langCode.equals("list") || langCode.equals("help")) {
            showAvailableLanguages(player);
            return true;
        }

        // Handle reset command
        if (langCode.equals("reset") || langCode.equals("default")) {
            resetPlayerLanguage(player);
            return true;
        }

        // Set language
        setPlayerLanguage(player, langCode);
        return true;
    }

    /**
     * Shows the player's current language setting.
     */
    private void showCurrentLanguage(Player player) {
        String currentLang = PlayerDataManager.getPlayerLanguage(player);
        String globalLang = LanguageManager.getInstance().getGlobalLanguage();

        if (currentLang == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("language", globalLang);
            LanguageManager.getInstance().sendMessage(player, "language_global_info", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("language", currentLang);
            LanguageManager.getInstance().sendMessage(player, "language_current", placeholders);
        }
    }

    /**
     * Shows available languages to the player.
     */
    private void showAvailableLanguages(Player player) {
        String available = LanguageManager.getInstance().getAvailableLanguagesString();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("languages", available);
        LanguageManager.getInstance().sendMessage(player, "language_available", placeholders);
    }

    /**
     * Sets the player's language preference.
     */
    private void setPlayerLanguage(Player player, String langCode) {
        // Validate language exists
        if (!LanguageManager.getInstance().isLanguageAvailable(langCode)) {
            String available = LanguageManager.getInstance().getAvailableLanguagesString();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("language", langCode);
            placeholders.put("languages", available);
            LanguageManager.getInstance().sendMessage(player, "language_not_found", placeholders);
            return;
        }

        // Set player language
        PlayerDataManager.getInstance().setPlayerLanguage(player, langCode);

        // Send confirmation message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("language", langCode);
        LanguageManager.getInstance().sendMessage(player, "language_set", placeholders);
    }

    /**
     * Resets the player's language to the global default.
     */
    private void resetPlayerLanguage(Player player) {
        PlayerDataManager.getInstance().removePlayerLanguage(player);
        String globalLang = LanguageManager.getInstance().getGlobalLanguage();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("language", globalLang);
        LanguageManager.getInstance().sendMessage(player, "language_reset", placeholders);
    }
}
