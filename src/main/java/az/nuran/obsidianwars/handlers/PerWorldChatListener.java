package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Listener for per-world chat feature.
 * Players in different worlds cannot see each other's chat messages.
 */
public class PerWorldChatListener implements Listener {

    private final boolean enabled;

    public PerWorldChatListener(Obsidianwars plugin) {
        this.enabled = plugin.getConfig().getBoolean("per-world.chat", false);
        plugin.getLogger().info("Per-World Chat: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;

        Player sender = event.getPlayer();
        String senderWorld = sender.getWorld().getName();

        // Cancel the original event
        event.setCancelled(true);

        // Get all players in the same world
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.getWorld().getName().equals(senderWorld)) {
                // Send the message with original format
                String format = event.getFormat();
                String message = event.getMessage();
                recipient.sendMessage(String.format(format, sender.getDisplayName(), message));
            }
        }

        // Log to console
        Bukkit.getConsoleSender().sendMessage(String.format(event.getFormat(), sender.getName(), event.getMessage()));
    }
}
