package az.nuran.obsidianwars.models;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a cuboid region in a world.
 * Used for arena boundary detection and isolation.
 */
public class CuboidRegion {
    private final World world;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    public CuboidRegion(World world, Location pos1, Location pos2) {
        if (pos1.getWorld() != pos2.getWorld()) {
            throw new IllegalArgumentException("Positions must be in the same world");
        }

        this.world = world;
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public CuboidRegion(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    /**
     * Checks if a location is within this region.
     *
     * @param location The location to check
     * @return true if the location is within the region
     */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        if (!location.getWorld().equals(world)) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    /**
     * Checks if this region overlaps with another region.
     *
     * @param other The other region
     * @return true if the regions overlap
     */
    public boolean overlaps(CuboidRegion other) {
        if (!world.equals(other.world)) {
            return false;
        }

        return !(maxX < other.minX || minX > other.maxX ||
                 maxY < other.minY || minY > other.maxY ||
                 maxZ < other.minZ || minZ > other.maxZ);
    }

    /**
     * Gets the minimum corner of the region.
     *
     * @return The minimum corner location
     */
    public Location getMinCorner() {
        return new Location(world, minX, minY, minZ);
    }

    /**
     * Gets the maximum corner of the region.
     *
     * @return The maximum corner location
     */
    public Location getMaxCorner() {
        return new Location(world, maxX, maxY, maxZ);
    }

    /**
     * Gets the center of the region.
     *
     * @return The center location
     */
    public Location getCenter() {
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;
        return new Location(world, centerX, centerY, centerZ);
    }

    /**
     * Gets the volume of the region in blocks.
     *
     * @return The volume
     */
    public long getVolume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public World getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }
}
