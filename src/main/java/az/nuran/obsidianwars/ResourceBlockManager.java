package az.nuran.obsidianwars;

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

    // İcazə verilən resource blok növləri (loaded from config)
    private static final Set<Material> ALLOWED_RESOURCE_BLOCKS = new HashSet<>();

    /**
     * Loads allowed resource blocks from config.yml
     */
    public static void loadAllowedResourceBlocks() {
        ALLOWED_RESOURCE_BLOCKS.clear();

        List<String> allowedBlocks = Obsidianwars.getInstance().getConfig().getStringList("allowed-resource-blocks");
        for (String blockName : allowedBlocks) {
            try {
                Material material = Material.valueOf(blockName);
                ALLOWED_RESOURCE_BLOCKS.add(material);
            } catch (IllegalArgumentException e) {
                Obsidianwars.getInstance().getLogger().warning("Invalid material in allowed-resource-blocks: " + blockName);
            }
        }

        Obsidianwars.getInstance().getLogger().info("Loaded " + ALLOWED_RESOURCE_BLOCKS.size() + " allowed resource blocks from config");
    }

    public static void enterSetupMode(Player player, String arenaName) {
        UUID uuid = player.getUniqueId();
        playersInSetupMode.put(uuid, arenaName);
        
        // BossBar yaratırıq
        BossBar bossBar = Bukkit.createBossBar("§eSetup Mode: Place resource blocks for " + arenaName + ". Type /o arena setblocks to exit.", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        setupModeBossBars.put(uuid, bossBar);
        
        // Oyunçunu Creative mode-a keçiririk
        player.setGameMode(org.bukkit.GameMode.CREATIVE);

        player.sendMessage("§aResource Block Setup Mode-a daxil oldunuz!");
        player.sendMessage("§eİcazə verilən bloklar (config.yml-dən yüklənib):");
        for (Material material : ALLOWED_RESOURCE_BLOCKS) {
            player.sendMessage("§7- " + material.name());
        }
        player.sendMessage("§eHansı bloku qoymaq istəyirsinizsə, yerləşdirin!");
    }

    public static void exitSetupMode(Player player) {
        UUID uuid = player.getUniqueId();

        if (playersInSetupMode.containsKey(uuid)) {
            String arenaName = playersInSetupMode.remove(uuid);

            // BossBar silirik
            BossBar bossBar = setupModeBossBars.remove(uuid);
            if (bossBar != null) {
                bossBar.removeAll();
            }

            // Oyunçunu Creative mode-a qaytarırıq
            player.setGameMode(org.bukkit.GameMode.CREATIVE);

            player.sendMessage("§aResource Block Setup Mode-dan çıxdınız!");

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

        // Yalnız icazə verilən resource bloklarını qəbul edirik
        if (!ALLOWED_RESOURCE_BLOCKS.contains(blockType)) {
            event.setCancelled(true);
            player.sendMessage("§cBu blok resource kimi qeyd edilə bilməz!");
            player.sendMessage("§eİcazə verilən bloklar (config.yml-dən):");
            for (Material material : ALLOWED_RESOURCE_BLOCKS) {
                player.sendMessage("§7- " + material.name());
            }
            return;
        }

        // Blok məlumatlarını resource_blocks.yml faylına yazırıq
        Location loc = event.getBlock().getLocation();
        
        // Use ArenaFileManager to add resource block with immediate disk flush
        ArenaFileManager.addResourceBlock(arenaName, blockType.name(), loc);
        
        int totalBlocks = ArenaFileManager.getResourceBlockCount(arenaName);

        player.sendMessage("§a" + blockType.name() + " bloku resource kimi qeyd edildi! (" + totalBlocks + " blok)");
        player.sendActionBar("§aResource block saved! Total: " + totalBlocks);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Əvvəlcə setup mode-u yoxlayırıq
        if (isInSetupMode(player)) {
            String arenaName = getSetupArena(player);
            Location blockLoc = event.getBlock().getLocation();
            
            // Bu blokun resource bloku olub-olmadığını yoxlayırıq
            if (ArenaFileManager.isResourceBlock(arenaName, blockLoc)) {
                // Resource blokudur - resource_blocks.yml faylından silirik
                ArenaFileManager.removeResourceBlock(arenaName, blockLoc);
                int totalBlocks = ArenaFileManager.getResourceBlockCount(arenaName);
                player.sendMessage("§cResource bloku silindi! (" + totalBlocks + " blok qaldı)");
                player.sendActionBar("§cResource block removed! Total: " + totalBlocks);
            }
            return;
        }
        
        // Yalnız arenadakı oyunçular üçün
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // Əgər oyun aktiv deyilsə və ya preparation deyilsə, blok qırmağı qadağan edirik
        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            return;
        }

        Location blockLoc = event.getBlock().getLocation();
        
        // Bu blokun resource bloku olub-olmadığını yoxlayırıq
        if (ArenaFileManager.isResourceBlock(arenaName, blockLoc)) {
            // Resource blokudur - delayed respawn based on material type
            Material originalType = event.getBlock().getType();
            long respawnDelay = getRespawnDelay(originalType);

            DebugManager.logDebug("Resource block broken: " + originalType.name() + " by " + player.getName() + " (respawn in " + (respawnDelay/20.0) + "s)", arenaName);

            // Bloğu qırmağı icazə veririk (drop vermək üçün)
            // Respawn delay-dən sonra
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                event.getBlock().setType(originalType);
                DebugManager.logDebug("Resource block respawned: " + originalType.name(), arenaName);
            }, respawnDelay); // Material tipinə görə respawn
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