package az.nuran.obsidianwars.models;

import org.bukkit.World;

/**
 * Represents an arena's data including its region.
 */
public class ArenaData {
    private final String name;
    public final World world;
    public final CuboidRegion region;

    public ArenaData(String name, World world, CuboidRegion region) {
        this.name = name;
        this.world = world;
        this.region = region;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public CuboidRegion getRegion() {
        return region;
    }
}
