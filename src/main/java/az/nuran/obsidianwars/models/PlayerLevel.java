package az.nuran.obsidianwars.models;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.UUID;

/**
 * Data class for player level and XP.
 */
public class PlayerLevel {
    private final UUID uuid;
    private int level;
    private int currentXp;
    private int nextLevelCost;
    private String levelName;
    private String progressBar;
    private String formattedCurrentXp;
    private String formattedRequiredXp;
    private boolean modified;

    public PlayerLevel(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.currentXp = 0;
        this.modified = false;
        setNextLevelCost();
        updateLevelName();
        updateProgressBar();
    }

    public PlayerLevel(UUID uuid, int level, int currentXp) {
        this.uuid = uuid;
        this.level = Math.max(1, level);
        this.currentXp = Math.max(0, currentXp);
        this.modified = false;
        setNextLevelCost();
        updateLevelName();
        updateProgressBar();
    }

    /**
     * Calculates the XP required for the next level based on BedWars1058 formula.
     * Levels 1-4: increasing costs (1000, 2000, 3000, 3500)
     * Levels 5-10: 5000 XP per level
     * Level 11+: 5000 XP per level (consistent progression)
     */
    private void setNextLevelCost() {
        if (level == 1) {
            this.nextLevelCost = 1000;
        } else if (level == 2) {
            this.nextLevelCost = 2000;
        } else if (level == 3) {
            this.nextLevelCost = 3000;
        } else if (level == 4) {
            this.nextLevelCost = 3500;
        } else {
            this.nextLevelCost = 5000;
        }
    }

    /**
     * Updates the level display name with colors and stars based on level bracket.
     */
    private void updateLevelName() {
        ChatColor color;
        String prefix;

        if (level >= 1000) {
            color = ChatColor.GOLD;
            prefix = "§6§l";
        } else if (level >= 500) {
            color = ChatColor.AQUA;
            prefix = "§b§l";
        } else if (level >= 200) {
            color = ChatColor.GREEN;
            prefix = "§a§l";
        } else if (level >= 100) {
            color = ChatColor.YELLOW;
            prefix = "§e§l";
        } else if (level >= 50) {
            color = ChatColor.LIGHT_PURPLE;
            prefix = "§d§l";
        } else if (level >= 20) {
            color = ChatColor.RED;
            prefix = "§c§l";
        } else if (level >= 10) {
            color = ChatColor.BLUE;
            prefix = "§9§l";
        } else if (level >= 5) {
            color = ChatColor.GRAY;
            prefix = "§7§l";
        } else {
            color = ChatColor.GRAY;
            prefix = "§7";
        }

        this.levelName = prefix + "[" + level + "★]";
    }

    /**
     * Updates the progress bar (10-segment display).
     */
    private void updateProgressBar() {
        double progress = (double) currentXp / nextLevelCost;
        int unlocked = (int) (progress * 10);
        int locked = 10 - unlocked;

        if (unlocked < 0) unlocked = 0;
        if (unlocked > 10) unlocked = 10;
        if (locked < 0) locked = 0;

        String unlockedSection = ChatColor.AQUA + String.valueOf(new char[unlocked]).replace("\0", "■");
        String lockedSection = ChatColor.DARK_GRAY + String.valueOf(new char[locked]).replace("\0", "■");

        this.progressBar = ChatColor.DARK_GRAY + "[" + unlockedSection + lockedSection + ChatColor.DARK_GRAY + "]";
        this.formattedCurrentXp = formatNumber(currentXp);
        this.formattedRequiredXp = formatNumber(nextLevelCost);
    }

    private String formatNumber(int number) {
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);

        if (number >= 1000) {
            return format.format(number / 1000.0) + "k";
        }
        return format.format(number);
    }

    /**
     * Adds XP to the player and checks for level up.
     */
    public void addXp(int xp) {
        if (xp <= 0) return;

        this.currentXp += xp;
        this.modified = true;

        int oldLevel = this.level;
        checkLevelUp();

        if (this.level > oldLevel) {
            sendLevelUpNotification(oldLevel);
        }

        updateProgressBar();
    }

    /**
     * Checks if the player has enough XP to level up.
     */
    private void checkLevelUp() {
        while (currentXp >= nextLevelCost) {
            currentXp -= nextLevelCost;
            level++;
            setNextLevelCost();
            updateLevelName();
        }
    }

    /**
     * Sends level-up notification (title and sound).
     */
    private void sendLevelUpNotification(int oldLevel) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            // Get settings from config
            boolean broadcast = az.nuran.obsidianwars.Obsidianwars.getInstance().getConfig().getBoolean("level-up.broadcast", false);
            String title = az.nuran.obsidianwars.Obsidianwars.getInstance().getConfig().getString("level-up.title", "&a&lLEVEL UP!");
            String subtitle = az.nuran.obsidianwars.Obsidianwars.getInstance().getConfig().getString("level-up.subtitle", "&7You are now Level &e{level}");
            String soundName = az.nuran.obsidianwars.Obsidianwars.getInstance().getConfig().getString("level-up.sound", "ENTITY_PLAYER_LEVELUP");
            String message = az.nuran.obsidianwars.Obsidianwars.getInstance().getConfig().getString("level-up.message", "&aCongratulations! You are now Level &e{level}!");
            String broadcastMessage = az.nuran.obsidianwars.Obsidianwars.getInstance().getConfig().getString("level-up.broadcast-message", "&e{player} &ahas reached Level &e{level}!");

            // Replace placeholders
            title = title.replace("{level}", String.valueOf(level)).replace("{player}", player.getName());
            subtitle = subtitle.replace("{level}", String.valueOf(level)).replace("{player}", player.getName());
            message = message.replace("{level}", String.valueOf(level)).replace("{player}", player.getName());
            broadcastMessage = broadcastMessage.replace("{level}", String.valueOf(level)).replace("{player}", player.getName());

            // Translate color codes
            title = ChatColor.translateAlternateColorCodes('&', title);
            subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
            message = ChatColor.translateAlternateColorCodes('&', message);
            broadcastMessage = ChatColor.translateAlternateColorCodes('&', broadcastMessage);

            // Send title and subtitle
            player.sendTitle(title, subtitle, 10, 70, 20);

            // Play sound
            Sound sound = az.nuran.obsidianwars.Obsidianwars.parseSound(soundName);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }

            // Send chat message to player
            player.sendMessage(message);

            // Broadcast to server if enabled
            if (broadcast) {
                Bukkit.broadcastMessage(broadcastMessage);
            }
        }
    }

    /**
     * Sets the player's XP directly.
     */
    public void setXp(int xp) {
        this.currentXp = Math.max(0, xp);
        this.modified = true;
        checkLevelUp();
        updateProgressBar();
    }

    /**
     * Sets the player's level directly.
     */
    public void setLevel(int level) {
        this.level = Math.max(1, level);
        this.modified = true;
        setNextLevelCost();
        updateLevelName();
        updateProgressBar();
    }

    // Getters
    public UUID getUuid() { return uuid; }
    public int getLevel() { return level; }
    public int getCurrentXp() { return currentXp; }
    public int getNextLevelCost() { return nextLevelCost; }
    public String getLevelName() { return levelName; }
    public String getProgressBar() { return progressBar; }
    public String getFormattedCurrentXp() { return formattedCurrentXp; }
    public String getFormattedRequiredXp() { return formattedRequiredXp; }
    public boolean isModified() { return modified; }
    public void setModified(boolean modified) { this.modified = modified; }
}
