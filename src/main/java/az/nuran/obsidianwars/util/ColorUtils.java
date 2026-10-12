package az.nuran.obsidianwars.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central color translation for chat, titles, action bars, scoreboards, and item text.
 * Supports legacy ampersand codes ({@code &a}, {@code &l}, etc.) and HEX RGB
 * ({@code &#RRGGBB} or {@code #RRGGBB}).
 */
public final class ColorUtils {

    private static final Pattern AMPERSAND_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HASH_HEX = Pattern.compile("(?<!&)#([A-Fa-f0-9]{6})");
    private static final Pattern STRIP_AMPERSAND_HEX = Pattern.compile("&#[A-Fa-f0-9]{6}");
    private static final Pattern STRIP_HASH_HEX = Pattern.compile("(?<!&)#[A-Fa-f0-9]{6}");
    private static final Pattern STRIP_HEX_SEQUENCE = Pattern.compile("(?i)[&\u00A7]x(?:[&\u00A7][0-9A-F]){6}");
    private static final Pattern STRIP_LEGACY = Pattern.compile("(?i)[&\u00A7][0-9A-FK-ORX]");

    private ColorUtils() {
    }

    /**
     * Translates {@code &} / HEX color codes into Minecraft formatting.
     * Section signs are normalized to {@code &} first so leftover section-sign codes
     * and already-translated text are handled the same way.
     */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = text.replace('\u00A7', '&');
        text = translateHexPattern(text, AMPERSAND_HEX);
        text = translateHexPattern(text, HASH_HEX);
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String translateHexPattern(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            ChatColor color = ChatColor.of("#" + matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(color.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Translates color codes for each string in a list.
     */
    public static List<String> translate(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return texts;
        }

        List<String> translated = new ArrayList<>(texts.size());
        for (String text : texts) {
            translated.add(translate(text));
        }
        return translated;
    }

    /**
     * Strips HEX and legacy color codes from a string.
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = STRIP_AMPERSAND_HEX.matcher(text).replaceAll("");
        text = STRIP_HASH_HEX.matcher(text).replaceAll("");
        text = STRIP_HEX_SEQUENCE.matcher(text).replaceAll("");
        text = STRIP_LEGACY.matcher(text).replaceAll("");
        return ChatColor.stripColor(text);
    }

    /**
     * True if {@code actual} matches {@code expected} as either raw {@code &}/HEX codes
     * or already-translated Minecraft text.
     */
    public static boolean matches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        if (actual.equals(expected) || actual.equals(translate(expected))) {
            return true;
        }
        return stripColors(actual).equals(stripColors(expected));
    }

    /**
     * True if {@code actual} starts with {@code rawPrefix} as raw codes or translated text.
     */
    public static boolean startsWithFormatted(String actual, String rawPrefix) {
        if (actual == null || rawPrefix == null) {
            return false;
        }
        if (actual.startsWith(rawPrefix) || actual.startsWith(translate(rawPrefix))) {
            return true;
        }
        return stripColors(actual).startsWith(stripColors(rawPrefix));
    }
}
