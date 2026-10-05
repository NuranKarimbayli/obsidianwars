package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages spectator mode for eliminated players.
 * Handles spectator state, inventory items, and teleportation.
 * Uses Adventure Mode with custom invisibility for full hotbar functionality.
 */
public class SpectatorManager {

    private static final Map<UUID, String> spectators = new HashMap<>();
    private static final Map<UUID, String> spectatorArenas = new HashMap<>();
    private static final Map<UUID, Integer> flySpeeds = new HashMap<>(); // 1=1x, 2=2x, 3=3x
    private static final Map<UUID, Boolean> nightVisionStates = new HashMap<>();
    private static final Map<UUID, Boolean> respawningPlayers = new HashMap<>(); // Tracks players waiting to respawn

    /**
     * Sets a player to spectator mode.
     *
     * @param player The player to set as spectator
     * @param arenaName The arena the player is spectating
     */
    public static void setSpectatorMode(Player player, String arenaName) {
        // Remove from team tracking
        TeamManager.removePlayerFromTeam(player);

        // Set Adventure mode for hotbar functionality
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);

        // Hide spectator from all alive players in the arena
        hideSpectatorFromAlivePlayers(player, arenaName);

        // Apply Night Vision
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        nightVisionStates.put(player.getUniqueId(), true);

        // Set default fly speed (1x)
        player.setFlySpeed(0.1f); // Default Minecraft fly speed
        flySpeeds.put(player.getUniqueId(), 1);

        // Clear inventory and give spectator items
        player.getInventory().clear();
        giveSpectatorItems(player);

        // Teleport to spectator spawn
        Location spectSpawn = ArenaConfigManager.getSpectatorSpawn(arenaName);
        if (spectSpawn != null) {
            player.teleport(spectSpawn);
        }

        // Track spectator
        spectators.put(player.getUniqueId(), arenaName);
        spectatorArenas.put(player.getUniqueId(), arenaName);

        player.sendMessage("§aYou are now spectating arena " + arenaName + "!");
    }

    /**
     * Removes spectator mode from a player.
     *
     * @param player The player to remove spectator mode from
     */
    public static void removeSpectatorMode(Player player) {
        String arenaName = spectatorArenas.get(player.getUniqueId());

        // Show spectator to all players again
        if (arenaName != null) {
            showSpectatorToAllPlayers(player, arenaName);
        }

        // Remove from tracking
        spectators.remove(player.getUniqueId());
        spectatorArenas.remove(player.getUniqueId());
        flySpeeds.remove(player.getUniqueId());
        nightVisionStates.remove(player.getUniqueId());

        // Reset game mode
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f); // Reset to default

        // Remove Night Vision
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);

        // Clear inventory
        player.getInventory().clear();

        // Reset player state
        PlayerUtils.resetPlayerArenaLeave(player);
    }

    /**
     * Hides spectator from all alive players in the arena.
     * Also makes spectators visible to each other.
     *
     * @param spectator The spectator player
     * @param arenaName The arena name
     */
    private static void hideSpectatorFromAlivePlayers(Player spectator, String arenaName) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null && arenaPlayer.isOnline()) {
                    if (!isSpectator(arenaPlayer)) {
                        // Hide from alive players
                        arenaPlayer.hidePlayer(Obsidianwars.getInstance(), spectator);
                    } else {
                        // Show to other spectators
                        arenaPlayer.showPlayer(Obsidianwars.getInstance(), spectator);
                        spectator.showPlayer(Obsidianwars.getInstance(), arenaPlayer);
                    }
                }
            }
        }
    }

    /**
     * Shows spectator to all players in the arena.
     *
     * @param spectator The spectator player
     * @param arenaName The arena name
     */
    private static void showSpectatorToAllPlayers(Player spectator, String arenaName) {
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null && arenaPlayer.isOnline()) {
                    arenaPlayer.showPlayer(Obsidianwars.getInstance(), spectator);
                }
            }
        }
        // Also show to all spectators
        for (Player otherSpectator : getSpectatorsInArena(arenaName)) {
            if (otherSpectator != spectator && otherSpectator.isOnline()) {
                otherSpectator.showPlayer(Obsidianwars.getInstance(), spectator);
                spectator.showPlayer(Obsidianwars.getInstance(), otherSpectator);
            }
        }
    }

    /**
     * Gives spectator items to a player.
     *
     * @param player The player to give items to
     */
    private static void giveSpectatorItems(Player player) {
        // Slot 0: Teleporter Compass
        ItemStack teleporter = createTeleporterCompass();
        player.getInventory().setItem(0, teleporter);

        // Slot 3: Fly Speed Feather
        ItemStack feather = createFlySpeedFeather(player);
        player.getInventory().setItem(3, feather);

        // Slot 4: Night Vision Toggle
        ItemStack enderEye = createNightVisionToggle(player);
        player.getInventory().setItem(4, enderEye);

        // Slot 8: Leave Item
        ItemStack leaveItem = createLeaveItem();
        player.getInventory().setItem(8, leaveItem);
    }

    /**
     * Creates the teleporter compass item.
     *
     * @return The teleporter compass ItemStack
     */
    public static ItemStack createTeleporterCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lPlayer Teleporter");
            meta.setLore(java.util.Arrays.asList(
                "§7Right-click to teleport to",
                "§7alive players in the game."
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the fly speed feather item.
     *
     * @param player The player
     * @return The fly speed feather ItemStack
     */
    public static ItemStack createFlySpeedFeather(Player player) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int speed = flySpeeds.getOrDefault(player.getUniqueId(), 1);
            meta.setDisplayName("§e§lFly Speed §7(Current: " + speed + "x)");
            meta.setLore(java.util.Arrays.asList(
                "§7Right-click to cycle speed:",
                "§7 1x §f→ §7 2x §f→ §7 3x §f→ §7 1x"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the night vision toggle item.
     *
     * @param player The player
     * @return The night vision toggle ItemStack
     */
    public static ItemStack createNightVisionToggle(Player player) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean hasNightVision = nightVisionStates.getOrDefault(player.getUniqueId(), true);
            String status = hasNightVision ? "§aON" : "§cOFF";
            meta.setDisplayName("§b§lToggle Night Vision §7(" + status + ")");
            meta.setLore(java.util.Arrays.asList(
                "§7Right-click to toggle",
                "§7Night Vision effect."
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the leave item.
     *
     * @return The leave item ItemStack
     */
    public static ItemStack createLeaveItem() {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lLeave Spectator");
            meta.setLore(java.util.Arrays.asList(
                "§7Right-click to return to",
                "§7the main lobby."
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Cycles fly speed for a spectator.
     *
     * @param player The spectator
     */
    public static void cycleFlySpeed(Player player) {
        int currentSpeed = flySpeeds.getOrDefault(player.getUniqueId(), 1);
        int newSpeed = (currentSpeed % 3) + 1; // 1 -> 2 -> 3 -> 1
        flySpeeds.put(player.getUniqueId(), newSpeed);

        // Set fly speed (0.1f = 1x, 0.2f = 2x, 0.3f = 3x)
        float flySpeed = newSpeed * 0.1f;
        player.setFlySpeed(flySpeed);

        // Update item in inventory
        ItemStack feather = createFlySpeedFeather(player);
        player.getInventory().setItem(3, feather);

        player.sendMessage("§aFly speed set to " + newSpeed + "x");
    }

    /**
     * Toggles Night Vision for a spectator.
     *
     * @param player The spectator
     */
    public static void toggleNightVision(Player player) {
        boolean currentState = nightVisionStates.getOrDefault(player.getUniqueId(), true);
        boolean newState = !currentState;
        nightVisionStates.put(player.getUniqueId(), newState);

        if (newState) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            player.sendMessage("§aNight Vision enabled");
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.sendMessage("§cNight Vision disabled");
        }

        // Update item in inventory
        ItemStack enderEye = createNightVisionToggle(player);
        player.getInventory().setItem(4, enderEye);
    }

    /**
     * Checks if a player is in spectator mode.
     *
     * @param player The player to check
     * @return true if the player is a spectator
     */
    public static boolean isSpectator(Player player) {
        return spectators.containsKey(player.getUniqueId());
    }

    /**
     * Gets the arena a spectator is watching.
     *
     * @param player The spectator
     * @return The arena name, or null if not spectating
     */
    public static String getSpectatorArena(Player player) {
        return spectatorArenas.get(player.getUniqueId());
    }

    /**
     * Gets all spectators in an arena.
     *
     * @param arenaName The arena name
     * @return List of spectator players
     */
    public static java.util.List<Player> getSpectatorsInArena(String arenaName) {
        java.util.List<Player> spectatorsList = new java.util.ArrayList<>();
        for (Map.Entry<UUID, String> entry : spectatorArenas.entrySet()) {
            if (entry.getValue().equals(arenaName)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    spectatorsList.add(player);
                }
            }
        }
        return spectatorsList;
    }

    /**
     * Cleans up all spectator data (called on plugin disable).
     */
    public static void cleanup() {
        spectators.clear();
        spectatorArenas.clear();
        flySpeeds.clear();
        nightVisionStates.clear();
        respawningPlayers.clear();
    }

    /**
     * Marks a player as waiting to respawn.
     * @param uuid The player's UUID
     */
    public static void setRespawning(UUID uuid) {
        respawningPlayers.put(uuid, true);
    }

    /**
     * Marks a player as no longer waiting to respawn.
     * @param uuid The player's UUID
     */
    public static void clearRespawning(UUID uuid) {
        respawningPlayers.remove(uuid);
    }

    /**
     * Checks if a player is currently waiting to respawn.
     * @param uuid The player's UUID
     * @return true if the player is waiting to respawn
     */
    public static boolean isRespawning(UUID uuid) {
        return respawningPlayers.containsKey(uuid);
    }
}
