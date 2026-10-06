package az.nuran.obsidianwars;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Utility class for armor-related operations.
 * Provides centralized methods for equipping colored leather armor.
 */
public class ArmorUtils {

    /**
     * Equips colored leather armor to a player.
     * Sets all four armor pieces (helmet, chestplate, leggings, boots) with the specified color.
     *
     * @param player The player to equip
     * @param color The color for the leather armor
     */
    public static void equipColoredLeatherArmor(Player player, Color color) {
        // Leather Helmet
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        if (helmetMeta != null) {
            helmetMeta.setColor(color);
            helmet.setItemMeta(helmetMeta);
        }
        player.getInventory().setHelmet(helmet);

        // Leather Chestplate
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setColor(color);
            chestplate.setItemMeta(chestMeta);
        }
        player.getInventory().setChestplate(chestplate);

        // Leather Leggings
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        if (leggingsMeta != null) {
            leggingsMeta.setColor(color);
            leggings.setItemMeta(leggingsMeta);
        }
        player.getInventory().setLeggings(leggings);

        // Leather Boots
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.setColor(color);
            boots.setItemMeta(bootsMeta);
        }
        player.getInventory().setBoots(boots);
    }
}
