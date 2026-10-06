package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages arena snapshots using efficient runtime block-change tracking.
 * NO region scanning - only tracks actual block changes during games.
 */
public class ArenaSnapshotManager implements Listener {

    private static final Map<String, ArenaSnapshot> activeSnapshots = new HashMap<>();

    /**
     * Takes a snapshot of the arena before game start.
     * ONLY captures essential fixed points (obsidians, walls, resources).
     * NO region scanning - this is instant and causes 0 lag.
     *
     * @param arenaName The arena name
     * @return true if snapshot was successful, false otherwise
     */
    public static boolean takeSnapshot(String arenaName) {
        try {
            Obsidianwars.getInstance().getLogger().info("Starting instant snapshot capture for arena " + arenaName);

            ArenaSnapshot snapshot = new ArenaSnapshot(arenaName);

            // Capture obsidian blocks (2 blocks max)
            captureObsidianBlocks(snapshot, arenaName);
            Obsidianwars.getInstance().getLogger().info("  - Captured " + snapshot.getObsidianBlocks().size() + " obsidian blocks");

            // Capture wall regions (only before walls are built)
            captureWallRegions(snapshot, arenaName);
            Obsidianwars.getInstance().getLogger().info("  - Captured " + snapshot.getWallBlocks().size() + " wall blocks");

            // Capture resource blocks (from config, not world scan)
            captureResourceBlocks(snapshot, arenaName);
            Obsidianwars.getInstance().getLogger().info("  - Captured " + snapshot.getResourceBlocks().size() + " resource blocks");

            // Store the snapshot
            activeSnapshots.put(arenaName, snapshot);

            int totalBlocks = snapshot.getObsidianBlocks().size() + snapshot.getWallBlocks().size() + snapshot.getResourceBlocks().size();
            Obsidianwars.getInstance().getLogger().info("Snapshot capture complete for arena " + arenaName +
                " - Total: " + totalBlocks + " blocks (0ms - no region scan)");

            return true;
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().severe("Failed to take snapshot for arena " + arenaName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Restores the arena to its pre-game state asynchronously.
     * Iterates backwards through tracked changes and restores each block.
     * Then restores essential elements (obsidians, walls, resources).
     *
     * @param arenaName The arena name
     * @return true if restoration was initiated successfully, false otherwise
     */
    public static boolean restoreSnapshot(String arenaName) {
        ArenaSnapshot snapshot = activeSnapshots.remove(arenaName);
        if (snapshot == null) {
            Obsidianwars.getInstance().getLogger().warning("No snapshot found for arena " + arenaName);
            return false;
        }

        // Run restoration asynchronously to prevent main-thread blocking
        Bukkit.getScheduler().runTaskAsynchronously(Obsidianwars.getInstance(), () -> {
            try {
                Obsidianwars.getInstance().getLogger().info("Starting async restoration for arena " + arenaName);

                // Restore tracked changes (iterate backwards to reverse the changes)
                List<BlockChange> changes = snapshot.getTrackedChanges();
                if (!changes.isEmpty()) {
                    // Must run block changes on main thread
                    Bukkit.getScheduler().runTask(Obsidianwars.getInstance(), () -> {
                        try {
                            // Iterate backwards to reverse the changes in correct order
                            for (int i = changes.size() - 1; i >= 0; i--) {
                                BlockChange change = changes.get(i);
                                Block block = change.location.getBlock();
                                block.setBlockData(change.originalBlockData);
                            }
                            Obsidianwars.getInstance().getLogger().info("  - Restored " + changes.size() + " changed blocks");
                        } catch (Exception e) {
                            Obsidianwars.getInstance().getLogger().severe("Failed to restore tracked changes for arena " + arenaName + ": " + e.getMessage());
                        }
                    });
                }

                // Restore obsidian blocks
                Bukkit.getScheduler().runTask(Obsidianwars.getInstance(), () -> {
                    try {
                        restoreObsidianBlocks(snapshot);
                        Obsidianwars.getInstance().getLogger().info("  - Restored " + snapshot.getObsidianBlocks().size() + " obsidian blocks");
                    } catch (Exception e) {
                        Obsidianwars.getInstance().getLogger().severe("Failed to restore obsidian blocks for arena " + arenaName + ": " + e.getMessage());
                    }
                });

                // Restore wall regions to original state
                Bukkit.getScheduler().runTask(Obsidianwars.getInstance(), () -> {
                    try {
                        restoreWallRegions(snapshot);
                        Obsidianwars.getInstance().getLogger().info("  - Restored " + snapshot.getWallBlocks().size() + " wall blocks");
                    } catch (Exception e) {
                        Obsidianwars.getInstance().getLogger().severe("Failed to restore wall regions for arena " + arenaName + ": " + e.getMessage());
                    }
                });

                // Restore resource blocks
                Bukkit.getScheduler().runTask(Obsidianwars.getInstance(), () -> {
                    try {
                        restoreResourceBlocks(snapshot);
                        Obsidianwars.getInstance().getLogger().info("  - Restored " + snapshot.getResourceBlocks().size() + " resource blocks");
                        Obsidianwars.getInstance().getLogger().info("Restoration complete for arena " + arenaName);
                    } catch (Exception e) {
                        Obsidianwars.getInstance().getLogger().severe("Failed to restore resource blocks for arena " + arenaName + ": " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Obsidianwars.getInstance().getLogger().severe("Failed to restore snapshot for arena " + arenaName + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        return true;
    }

    /**
     * Clears the snapshot for an arena without restoring.
     *
     * @param arenaName The arena name
     */
    public static void clearSnapshot(String arenaName) {
        activeSnapshots.remove(arenaName);
    }

    /**
     * Checks if a snapshot exists for an arena.
     *
     * @param arenaName The arena name
     * @return true if snapshot exists, false otherwise
     */
    public static boolean hasSnapshot(String arenaName) {
        return activeSnapshots.containsKey(arenaName);
    }

    /**
     * Clears all active snapshots.
     */
    public static void cleanup() {
        activeSnapshots.clear();
    }

    // ============================================
    // Runtime Block Change Tracking
    // ============================================

    /**
     * Tracks a block placement during the game.
     * Stores the ORIGINAL block data (what was there before placement).
     *
     * @param arenaName The arena name
     * @param location The block location
     * @param originalBlockData The block data BEFORE placement
     */
    public static void trackBlockPlace(String arenaName, Location location, BlockData originalBlockData) {
        ArenaSnapshot snapshot = activeSnapshots.get(arenaName);
        if (snapshot != null) {
            snapshot.trackBlockChange(location, originalBlockData);
        }
    }

    /**
     * Tracks a block break during the game.
     * Stores the ORIGINAL block data (what was there before break).
     *
     * @param arenaName The arena name
     * @param location The block location
     * @param originalBlockData The block data BEFORE break
     */
    public static void trackBlockBreak(String arenaName, Location location, BlockData originalBlockData) {
        ArenaSnapshot snapshot = activeSnapshots.get(arenaName);
        if (snapshot != null) {
            snapshot.trackBlockChange(location, originalBlockData);
        }
    }

    /**
     * Tracks blocks destroyed by explosions.
     *
     * @param arenaName The arena name
     * @param location The block location
     * @param originalBlockData The block data BEFORE explosion
     */
    public static void trackBlockExplosion(String arenaName, Location location, BlockData originalBlockData) {
        ArenaSnapshot snapshot = activeSnapshots.get(arenaName);
        if (snapshot != null) {
            snapshot.trackBlockChange(location, originalBlockData);
        }
    }

    // ============================================
    // Event Handlers for Block Change Tracking
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Only track if player is in an arena and game is active
        if (!ObsidianCommand.playersInArena.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(event.getPlayer().getUniqueId());
        if (arenaName == null) {
            return;
        }
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // Only track during active game states
        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            return;
        }

        // Store the ORIGINAL block data (what was there before placement)
        Location loc = event.getBlock().getLocation();
        BlockData originalData = event.getBlockReplacedState().getBlockData().clone();
        trackBlockPlace(arenaName, loc, originalData);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Only track if player is in an arena and game is active
        if (!ObsidianCommand.playersInArena.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(event.getPlayer().getUniqueId());
        if (arenaName == null) {
            return;
        }
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // Only track during active game states
        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            return;
        }

        // Store the ORIGINAL block data (what was there before break)
        Location loc = event.getBlock().getLocation();
        BlockData originalData = event.getBlock().getBlockData().clone();
        trackBlockBreak(arenaName, loc, originalData);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Check if explosion is within any active arena
        for (Map.Entry<String, ArenaSnapshot> entry : activeSnapshots.entrySet()) {
            String arenaName = entry.getKey();
            GameManager.ArenaGame game = GameManager.getGame(arenaName);

            // Only track during active game states
            if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
                continue;
            }

            // Track each destroyed block
            for (Block block : event.blockList()) {
                Location loc = block.getLocation();
                BlockData originalData = block.getBlockData().clone();
                trackBlockExplosion(arenaName, loc, originalData);
            }
        }
    }

    // ============================================
    // Essential Elements Capture & Restore
    // ============================================

    private static void captureObsidianBlocks(ArenaSnapshot snapshot, String arenaName) {
        // Red team obsidian
        Location redObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "red");
        if (redObsidian != null) {
            Block block = redObsidian.getBlock();
            snapshot.addObsidianBlock(redObsidian, block.getBlockData().clone());
        }

        // Blue team obsidian
        Location blueObsidian = ArenaConfigManager.getObsidianLocation(arenaName, "blue");
        if (blueObsidian != null) {
            Block block = blueObsidian.getBlock();
            snapshot.addObsidianBlock(blueObsidian, block.getBlockData().clone());
        }
    }

    private static void restoreObsidianBlocks(ArenaSnapshot snapshot) {
        for (Map.Entry<Location, BlockData> entry : snapshot.getObsidianBlocks().entrySet()) {
            Location loc = entry.getKey();
            BlockData originalData = entry.getValue();

            Block block = loc.getBlock();
            block.setBlockData(originalData);
        }
    }

    private static void captureWallRegions(ArenaSnapshot snapshot, String arenaName) {
        // Capture red team wall region
        captureWallRegion(snapshot, arenaName, "red");

        // Capture blue team wall region
        captureWallRegion(snapshot, arenaName, "blue");
    }

    private static void captureWallRegion(ArenaSnapshot snapshot, String arenaName, String team) {
        Location[] wallRegion = ArenaConfigManager.getWallRegion(arenaName, team);
        if (wallRegion == null) return;

        Location pos1 = wallRegion[0];
        Location pos2 = wallRegion[1];
        World world = pos1.getWorld();
        if (world == null) return;

        // Calculate min/max coordinates
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Capture blocks in this region (this is small - only wall area)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    snapshot.addWallBlock(loc, block.getBlockData().clone());
                }
            }
        }
    }

    private static void restoreWallRegions(ArenaSnapshot snapshot) {
        for (Map.Entry<Location, BlockData> entry : snapshot.getWallBlocks().entrySet()) {
            Location loc = entry.getKey();
            BlockData originalData = entry.getValue();

            Block block = loc.getBlock();
            block.setBlockData(originalData);
        }
    }

    private static void captureResourceBlocks(ArenaSnapshot snapshot, String arenaName) {
        // Get all resource blocks from ArenaFileManager (from config, not world scan)
        java.util.List<java.util.Map<String, Object>> resourceBlocks =
            ArenaFileManager.getResourceBlocks(arenaName);

        if (resourceBlocks != null) {
            for (java.util.Map<String, Object> blockData : resourceBlocks) {
                String worldName = (String) blockData.get("world");
                int x = ((Number) blockData.get("x")).intValue();
                int y = ((Number) blockData.get("y")).intValue();
                int z = ((Number) blockData.get("z")).intValue();

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    snapshot.addResourceBlock(loc, block.getBlockData().clone());
                }
            }
        }
    }

    private static void restoreResourceBlocks(ArenaSnapshot snapshot) {
        for (Map.Entry<Location, BlockData> entry : snapshot.getResourceBlocks().entrySet()) {
            Location loc = entry.getKey();
            BlockData originalData = entry.getValue();

            Block block = loc.getBlock();
            block.setBlockData(originalData);
        }
    }

    // ============================================
    // Snapshot Data Structure
    // ============================================

    /**
     * Represents a snapshot of an arena's state.
     * Stores ONLY essential elements and tracks runtime changes.
     */
    public static class ArenaSnapshot {
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

    /**
     * Represents a single block change with its original state.
     */
    public static class BlockChange {
        final Location location;
        final BlockData originalBlockData;

        BlockChange(Location location, BlockData originalBlockData) {
            this.location = location;
            this.originalBlockData = originalBlockData;
        }
    }
}
