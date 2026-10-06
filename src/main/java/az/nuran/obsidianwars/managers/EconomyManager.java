package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Economy Manager for ObsidianWars.
 * Supports both Vault economy and built-in economy system.
 * Manages reward distribution for various game actions.
 */
public class EconomyManager {

    private static EconomyManager instance;
    private final Logger logger;
    private Economy vaultEconomy;
    private boolean vaultEnabled;
    private boolean builtInEnabled;

    // Built-in economy storage (player UUID -> balance)
    private final Map<UUID, Double> builtInBalances;

    // Economy type: "VAULT" or "BUILT_IN"
    private String economyType;

    // Reward amounts (configurable in config.yml)
    private double obsidianBrokenReward;
    private double killReward;
    private double killstreakReward;
    private double winReward;
    private double playReward;

    public EconomyManager(Logger logger) {
        this.logger = logger;
        this.vaultEnabled = false;
        this.builtInEnabled = false;
        this.builtInBalances = new ConcurrentHashMap<>();
        loadConfig();
    }

    /**
     * Initializes the EconomyManager singleton.
     *
     * @param logger The plugin logger
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new EconomyManager(logger);
        }
    }

    /**
     * Gets the EconomyManager instance.
     *
     * @return The EconomyManager instance
     */
    public static EconomyManager getInstance() {
        return instance;
    }

    /**
     * Loads economy configuration from config.yml.
     */
    public void loadConfig() {
        if (!Obsidianwars.getInstance().getConfig().contains("economy")) {
            logger.info("Economy configuration not found in config.yml - using built-in economy");
            this.economyType = "BUILT_IN";
        } else {
            this.economyType = Obsidianwars.getInstance().getConfig().getString("economy.type", "BUILT_IN").toUpperCase();
        }

        this.obsidianBrokenReward = Obsidianwars.getInstance().getConfig().getDouble("economy.rewards.obsidian-broken", 10.0);
        this.killReward = Obsidianwars.getInstance().getConfig().getDouble("economy.rewards.kill", 5.0);
        this.killstreakReward = Obsidianwars.getInstance().getConfig().getDouble("economy.rewards.killstreak", 15.0);
        this.winReward = Obsidianwars.getInstance().getConfig().getDouble("economy.rewards.win", 50.0);
        this.playReward = Obsidianwars.getInstance().getConfig().getDouble("economy.rewards.play", 25.0);

        logger.info("Economy type: " + economyType);
        logger.info("Economy rewards loaded from config");
    }

    /**
     * Hooks into Vault economy if available and configured.
     *
     * @return true if hook was successful
     */
    public boolean hookVault() {
        // Only attempt Vault if configured to use it
        if (!"VAULT".equals(economyType)) {
            logger.info("Economy type is BUILT_IN - skipping Vault hook");
            builtInEnabled = true;
            return false;
        }

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warning("Vault plugin not found - falling back to built-in economy");
            this.economyType = "BUILT_IN";
            builtInEnabled = true;
            return false;
        }

        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager()
                .getRegistration(Economy.class);

            if (rsp == null) {
                logger.warning("No economy provider found - falling back to built-in economy");
                this.economyType = "BUILT_IN";
                builtInEnabled = true;
                return false;
            }

            vaultEconomy = rsp.getProvider();
            vaultEnabled = true;
            logger.info("Successfully hooked into Vault economy: " + vaultEconomy.getName());
            return true;
        } catch (Exception e) {
            logger.warning("Failed to hook into Vault economy: " + e.getMessage());
            logger.info("Falling back to built-in economy");
            this.economyType = "BUILT_IN";
            builtInEnabled = true;
            return false;
        }
    }

    /**
     * Checks if economy is enabled.
     *
     * @return true if economy is enabled
     */
    public boolean isEnabled() {
        return vaultEnabled || builtInEnabled;
    }

    /**
     * Awards a player for breaking an obsidian target.
     *
     * @param player The player
     */
    public void awardObsidianBroken(Player player) {
        if (!isEnabled()) return;
        awardPlayer(player, obsidianBrokenReward, "obsidian broken");
    }

    /**
     * Awards a player for a kill.
     *
     * @param player The player
     */
    public void awardKill(Player player) {
        if (!isEnabled()) return;
        awardPlayer(player, killReward, "kill");
    }

    /**
     * Awards a player for a killstreak.
     *
     * @param player The player
     */
    public void awardKillstreak(Player player) {
        if (!isEnabled()) return;
        awardPlayer(player, killstreakReward, "killstreak");
    }

    /**
     * Awards a player for winning a match.
     *
     * @param player The player
     */
    public void awardWin(Player player) {
        if (!isEnabled()) return;
        awardPlayer(player, winReward, "win");
    }

    /**
     * Awards a player for playing a full match.
     *
     * @param player The player
     */
    public void awardPlay(Player player) {
        if (!isEnabled()) return;
        awardPlayer(player, playReward, "play");
    }

    /**
     * Awards a player a custom amount.
     *
     * @param player The player
     * @param amount The amount to award
     * @param reason The reason for the award
     */
    public void awardPlayer(Player player, double amount, String reason) {
        if (!isEnabled()) return;

        if (amount <= 0) return;

        if (vaultEnabled && vaultEconomy != null) {
            // Use Vault economy
            vaultEconomy.depositPlayer(player, amount);
            logger.fine("Awarded " + amount + " (Vault) to " + player.getName() + " for " + reason);

            // Send message to player
            String currencyName = vaultEconomy.currencyNamePlural();
            player.sendMessage(MessagesConfigManager.getMessage("economy_reward", "amount", String.format("%.2f", amount), "currency", currencyName, "reason", reason));
        } else if (builtInEnabled) {
            // Use built-in economy
            UUID uuid = player.getUniqueId();
            builtInBalances.merge(uuid, amount, Double::sum);
            logger.fine("Awarded " + amount + " (Built-in) to " + player.getName() + " for " + reason);

            // Send message to player
            player.sendMessage(MessagesConfigManager.getMessage("economy_reward", "amount", String.format("%.2f", amount), "currency", "coins", "reason", reason));

            // Save to database asynchronously
            saveBuiltInBalance(uuid);
        }
    }

    /**
     * Gets a player's balance.
     *
     * @param player The player
     * @return The player's balance
     */
    public double getBalance(Player player) {
        if (vaultEnabled && vaultEconomy != null) {
            return vaultEconomy.getBalance(player);
        } else if (builtInEnabled) {
            return builtInBalances.getOrDefault(player.getUniqueId(), 0.0);
        }
        return 0;
    }

    /**
     * Checks if a player has enough balance.
     *
     * @param player The player
     * @param amount The amount to check
     * @return true if the player has enough balance
     */
    public boolean hasBalance(Player player, double amount) {
        if (vaultEnabled && vaultEconomy != null) {
            return vaultEconomy.has(player, amount);
        } else if (builtInEnabled) {
            return builtInBalances.getOrDefault(player.getUniqueId(), 0.0) >= amount;
        }
        return false;
    }

    /**
     * Withdraws an amount from a player.
     *
     * @param player The player
     * @param amount The amount to withdraw
     * @return true if withdrawal was successful
     */
    public boolean withdrawPlayer(Player player, double amount) {
        if (vaultEnabled && vaultEconomy != null) {
            return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
        } else if (builtInEnabled) {
            UUID uuid = player.getUniqueId();
            double currentBalance = builtInBalances.getOrDefault(uuid, 0.0);
            if (currentBalance >= amount) {
                builtInBalances.put(uuid, currentBalance - amount);
                saveBuiltInBalance(uuid);
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Saves built-in balance to database asynchronously.
     *
     * @param uuid The player UUID
     */
    private void saveBuiltInBalance(UUID uuid) {
        DatabaseManager.executeAsync(conn -> {
            try {
                // Save balance to stats database
                // This would need to be implemented in StatsDAO
                // For now, we'll just log it
                logger.fine("Built-in balance saved for " + uuid);
            } catch (Exception e) {
                logger.warning("Failed to save built-in balance for " + uuid + ": " + e.getMessage());
            }
        });
    }

    /**
     * Loads built-in balance from database.
     *
     * @param uuid The player UUID
     */
    public void loadBuiltInBalance(UUID uuid) {
        DatabaseManager.executeAsync(conn -> {
            try {
                // Load balance from stats database
                // This would need to be implemented in StatsDAO
                // For now, initialize with 0
                builtInBalances.putIfAbsent(uuid, 0.0);
            } catch (Exception e) {
                logger.warning("Failed to load built-in balance for " + uuid + ": " + e.getMessage());
                builtInBalances.putIfAbsent(uuid, 0.0);
            }
        });
    }

    /**
     * Gets the economy instance (Vault only).
     *
     * @return The Economy instance, or null if not available
     */
    public Economy getEconomy() {
        return vaultEconomy;
    }

    /**
     * Gets the current economy type.
     *
     * @return "VAULT" or "BUILT_IN"
     */
    public String getEconomyType() {
        return economyType;
    }

    /**
     * Reloads economy configuration.
     */
    public void reload() {
        loadConfig();
        if ("VAULT".equals(economyType) && !vaultEnabled) {
            hookVault();
        }
    }

    /**
     * Cleans up the EconomyManager (called on plugin disable).
     */
    public void cleanup() {
        vaultEconomy = null;
        vaultEnabled = false;
        builtInEnabled = false;
        builtInBalances.clear();
        instance = null;
    }
}
