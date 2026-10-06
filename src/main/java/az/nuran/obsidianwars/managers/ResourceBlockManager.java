package az.nuran.obsidianwars.managers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.services.DebugManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ResourceBlockManager implements Listener {

    private static final Map<UUID, String> playersInSetupMode = new HashMap<>();
    private static final Map<UUID, BossBar> setupModeBossBars = new HashMap<>();

    // Allowed resource block types (loaded from config)
    private static final Set<Material> ALLOWED_RESOURCE_BLOCKS = new HashSet<>();

    /**
     * Loads allowed resource blocks from resource-blocks.yml
     */
    public static void loadAllowedResourceBlocks() {
        ALLOWED_RESOURCE_BLOCKS.clear();

        List<String> allowedBlocks = ResourceBlocksConfigManager.getResourceBlocksConfig().getStringList("allowed-resource-blocks");
        for (String blockName : allowedBlocks) {
            try {
                Material material = Material.valueOf(blockName);
                ALLOWED_RESOURCE_BLOCKS.add(material);
            } catch (IllegalArgumentException e) {
                Obsidianwars.getInstance().getLogger().warning("Invalid material in allowed-resource-blocks: " + blockName);
            }
        }

        Obsidianwars.getInstance().getLogger().info("Loaded " + ALLOWED_RESOURCE_BLOCKS.size() + " allowed resource blocks from resource-blocks.yml");
    }

    public static void enterSetupMode(Player player, String arenaName) {
        UUID uuid = player.getUniqueId();
        playersInSetupMode.put(uuid, arenaName);
        
        // We create BossBar
        BossBar bossBar = Bukkit.createBossBar("§eSetup Mode: Place resource blocks for " + arenaName + ". Type /o arena setblocks to exit.", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        setupModeBossBars.put(uuid, bossBar);

        // We switch player to Creative mode
        player.setGameMode(org.bukkit.GameMode.CREATIVE);

        player.sendMessage("§aYou have entered Resource Block Setup Mode!");
        player.sendMessage("§eAllowed blocks (loaded from config.yml):");
        for (Material material : ALLOWED_RESOURCE_BLOCKS) {
            player.sendMessage("§7- " + material.name());
        }
        player.sendMessage("§ePlace whichever block you want to place!");
    }

    public static void exitSetupMode(Player player) {
        UUID uuid = player.getUniqueId();

        if (playersInSetupMode.containsKey(uuid)) {
            String arenaName = playersInSetupMode.remove(uuid);

            // We remove BossBar
            BossBar bossBar = setupModeBossBars.remove(uuid);
            if (bossBar != null) {
                bossBar.removeAll();
            }

            // We return player to Creative mode
            player.setGameMode(org.bukkit.GameMode.CREATIVE);

            player.sendMessage("§aYou have exited Resource Block Setup Mode!");

            // Suggest next setup step
            ObsidianCommand.suggestNextSetupStep(player, arenaName, "resourceblocks");
        }
    }

    public static boolean isInSetupMode(Player player) {
        return playersInSetupMode.containsKey(player.getUniqueId());
    }

    public static String getSetupArena(Player player) {
        return playersInSetupMode.get(player.getUniqueId());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        if (!isInSetupMode(player)) {
            return;
        }

        String arenaName = getSetupArena(player);
        Material blockType = event.getBlock().getType();

        // We only accept allowed resource blocks
        if (!ALLOWED_RESOURCE_BLOCKS.contains(blockType)) {
            event.setCancelled(true);
            player.sendMessage("§cThis block cannot be registered as a resource!");
            player.sendMessage("§eAllowed blocks (from config.yml):");
            for (Material material : ALLOWED_RESOURCE_BLOCKS) {
                player.sendMessage("§7- " + material.name());
            }
            return;
        }

        // We write block data to resource_blocks.yml file
        Location loc = event.getBlock().getLocation();
        
        // Use ArenaFileManager to add resource block with immediate disk flush
        ArenaFileManager.addResourceBlock(arenaName, blockType.name(), loc);
        
        int totalBlocks = ArenaFileManager.getResourceBlockCount(arenaName);

        player.sendMessage("§a" + blockType.name() + " block registered as resource! (" + totalBlocks + " blocks)");
        player.sendActionBar("§aResource block saved! Total: " + totalBlocks);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // First we check setup mode
        if (isInSetupMode(player)) {
            String arenaName = getSetupArena(player);
            Location blockLoc = event.getBlock().getLocation();

            // We check if this block is a resource block
            if (ArenaFileManager.isResourceBlock(arenaName, blockLoc)) {
                // It is a resource block - we remove it from resource_blocks.yml file
                ArenaFileManager.removeResourceBlock(arenaName, blockLoc);
                int totalBlocks = ArenaFileManager.getResourceBlockCount(arenaName);
                player.sendMessage("§cResource block removed! (" + totalBlocks + " blocks remaining)");
                player.sendActionBar("§cResource block removed! Total: " + totalBlocks);
            }
            return;
        }

        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // If game is not active or not in preparation, we prohibit block breaking
        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            return;
        }

        Location blockLoc = event.getBlock().getLocation();

        // We check if this block is a resource block
        if (ArenaFileManager.isResourceBlock(arenaName, blockLoc)) {
            // Resource blokudur - delayed respawn based on material type
            Material originalType = event.getBlock().getType();
            long respawnDelay = getRespawnDelay(originalType);

            DebugManager.logDebug("Resource block broken: " + originalType.name() + " by " + player.getName() + " (respawn in " + (respawnDelay/20.0) + "s)", arenaName);

            // We allow block breaking (to give drops)
            // After respawn delay (using TaskManager for centralized management)
            TaskManager.getInstance().runLater(
                "resource-respawn-" + arenaName + "-" + blockLoc.getBlockX() + "-" + blockLoc.getBlockY() + "-" + blockLoc.getBlockZ(),
                () -> {
                    event.getBlock().setType(originalType);
                    DebugManager.logDebug("Resource block respawned: " + originalType.name(), arenaName);
                },
                respawnDelay,
                arenaName
            ); // Respawn by material type
        }
    }

    private static long getRespawnDelay(Material material) {
        // Respawn delay in ticks (20 ticks = 1 second)
        switch (material) {
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
                return 2L; // 0.1 seconds
            case STONE:
            case COBBLESTONE:
                return 10L; // 0.5 seconds
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return 20L; // 1 second
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return 30L; // 1.5 seconds
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return 40L; // 2 seconds
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return 50L; // 2.5 seconds
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return 60L; // 3 seconds
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return 80L; // 4 seconds
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return 30L; // 1.5 seconds
            default:
                return 20L; // Default 1 second
        }
    }

    public static int getResourceBlockCount(String arenaName) {
        return ArenaFileManager.getResourceBlockCount(arenaName);
    }

    public static void cleanup() {
        playersInSetupMode.clear();
        for (BossBar bossBar : setupModeBossBars.values()) {
            bossBar.removeAll();
        }
        setupModeBossBars.clear();
    }
}