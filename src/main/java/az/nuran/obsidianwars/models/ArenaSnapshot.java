package az.nuran.obsidianwars.models;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a snapshot of an arena's state.
 * Stores ONLY essential elements and tracks runtime changes.
 */
public class ArenaSnapshot {
    private final String arenaName;

    // Essential elements (always restored)
    private final Map<Location, BlockData> obsidianBlocks;
    private final Map<Location, BlockData> wallBlocks;
    private final Map<Location, BlockData> resourceBlocks;

    // Runtime tracked changes (order matters for reverse restoration)
    private final List<BlockChange> trackedChanges;

    public ArenaSnapshot(String arenaName) {
        this.arenaName = arenaName;
        this.obsidianBlocks = new HashMap<>();
        this.wallBlocks = new HashMap<>();
        this.resourceBlocks = new HashMap<>();
        this.trackedChanges = new ArrayList<>();
    }

    public String getArenaName() {
        return arenaName;
    }

    public Map<Location, BlockData> getObsidianBlocks() {
        return obsidianBlocks;
    }

    public Map<Location, BlockData> getWallBlocks() {
        return wallBlocks;
    }

    public Map<Location, BlockData> getResourceBlocks() {
        return resourceBlocks;
    }

    public List<BlockChange> getTrackedChanges() {
        return trackedChanges;
    }

    public void addObsidianBlock(Location loc, BlockData blockData) {
        obsidianBlocks.put(loc, blockData);
    }

    public void addWallBlock(Location loc, BlockData blockData) {
        wallBlocks.put(loc, blockData);
    }

    public void addResourceBlock(Location loc, BlockData blockData) {
        resourceBlocks.put(loc, blockData);
    }

    /**
     * Tracks a block change during the game.
     * Stores the ORIGINAL block data so it can be restored later.
     *
     * @param loc The block location
     * @param originalBlockData The block data BEFORE the change
     */
    public void trackBlockChange(Location loc, BlockData originalBlockData) {
        trackedChanges.add(new BlockChange(loc, originalBlockData));
    }
}
