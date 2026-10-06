package az.nuran.obsidianwars.models;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

/**
 * Represents a single block change with its original state.
 */
public class BlockChange {
    public final Location location;
    public final BlockData originalBlockData;

    public BlockChange(Location location, BlockData originalBlockData) {
        this.location = location;
        this.originalBlockData = originalBlockData;
    }

    public Location getLocation() {
        return location;
    }

    public BlockData getOriginalBlockData() {
        return originalBlockData;
    }
}
