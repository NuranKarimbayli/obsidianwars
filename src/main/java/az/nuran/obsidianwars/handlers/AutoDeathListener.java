package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.Obsidianwars;

/**
 * Deprecated: Auto-death functionality has been moved to MovementListener.
 * This class is kept for backward compatibility but does nothing.
 * All auto-death logic is now handled by MovementListener for better performance.
 */
@Deprecated
public class AutoDeathListener {

    public AutoDeathListener(Obsidianwars plugin) {
        plugin.getLogger().warning("AutoDeathListener is deprecated. Functionality moved to MovementListener.");
    }

    public static void cleanup() {
        // No-op - cleanup is now handled by MovementListener
    }
}
