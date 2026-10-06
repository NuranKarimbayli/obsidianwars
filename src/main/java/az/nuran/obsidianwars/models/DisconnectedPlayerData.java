package az.nuran.obsidianwars.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data class to store disconnected player state for rejoin restoration.
 */
public class DisconnectedPlayerData {
    private final UUID uuid;
    private final String arenaName;
    private final String team;
    private final long disconnectTime;
    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final double health;
    private final int foodLevel;
    private final List<org.bukkit.potion.PotionEffect> potionEffects;
    private final Location lastLocation;
    private final boolean wasSpectator;

    public DisconnectedPlayerData(Player player, String arenaName, String team) {
        this.uuid = player.getUniqueId();
        this.arenaName = arenaName;
        this.team = team;
        this.disconnectTime = System.currentTimeMillis();
        this.inventoryContents = player.getInventory().getContents().clone();
        this.armorContents = player.getInventory().getArmorContents().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
        this.lastLocation = player.getLocation().clone();
        this.wasSpectator = player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
    }

    public UUID getUuid() { return uuid; }
    public String getArenaName() { return arenaName; }
    public String getTeam() { return team; }
    public long getDisconnectTime() { return disconnectTime; }
    public ItemStack[] getInventoryContents() { return inventoryContents; }
    public ItemStack[] getArmorContents() { return armorContents; }
    public double getHealth() { return health; }
    public int getFoodLevel() { return foodLevel; }
    public List<org.bukkit.potion.PotionEffect> getPotionEffects() { return potionEffects; }
    public Location getLastLocation() { return lastLocation; }
    public boolean wasSpectator() { return wasSpectator; }
}
