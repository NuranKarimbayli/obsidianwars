package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.ScoreboardManager;
import az.nuran.obsidianwars.managers.SpectatorManager;
import az.nuran.obsidianwars.managers.TaskManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Consolidated movement listener that handles all player movement checks in a single optimized handler.
 * This replaces the redundant PlayerMoveEvent handlers in AutoDeathListener and GameListener.
 */
public class MovementListener implements Listener {

    private static final Set<UUID> recentlyDied = new HashSet<>();
    private static final long DEATH_COOLDOWN = 2000; // 2 seconds cooldown to prevent multiple deaths
    private static final Set<String> arenaWorldNames = new HashSet<>();

    private final boolean autoDeathEnabled;
    private final int autoDeathYLevel;
    private final String autoDeathMessage;

    public MovementListener(Obsidianwars plugin) {
        this.autoDeathEnabled = plugin.getConfig().getBoolean("auto-death.enabled", true);
        this.autoDeathYLevel = plugin.getConfig().getInt("auto-death.y-level", -64);
        this.autoDeathMessage = plugin.getConfig().getString("auto-death.death-message", "&cYou fell into the void!");

        initializeArenaWorldCache();
        plugin.getLogger().info("MovementListener initialized: Auto-Death " + (autoDeathEnabled ? "ENABLED" : "DISABLED") + " (Y-level: " + autoDeathYLevel + ")");
    }

    /**
     * Initialize the arena world names cache to avoid O(n) lookups during player movement.
     */
    private void initializeArenaWorldCache() {
        arenaWorldNames.clear();
        for (String arenaName : ArenaConfigManager.getArenaNames()) {
            Location lobbySpawn = ArenaConfigManager.getLobbySpawn(arenaName);
            if (lobbySpawn != null && lobbySpawn.getWorld() != null) {
                arenaWorldNames.add(lobbySpawn.getWorld().getName());
            }
            Location redSpawn = ArenaConfigManager.getTeamSpawn(arenaName, "red");
            if (redSpawn != null && redSpawn.getWorld() != null) {
                arenaWorldNames.add(redSpawn.getWorld().getName());
            }
            Location blueSpawn = ArenaConfigManager.getTeamSpawn(arenaName, "blue");
            if (blueSpawn != null && blueSpawn.getWorld() != null) {
                arenaWorldNames.add(blueSpawn.getWorld().getName());
            }
        }
        Obsidianwars.getInstance().getLogger().info("Cached " + arenaWorldNames.size() + " arena world names for movement checks");
    }

    /**
     * Rebuild the arena world cache (call after arena configuration changes).
     */
    public static void rebuildArenaWorldCache() {
        arenaWorldNames.clear();
        for (String arenaName : ArenaConfigManager.getArenaNames()) {
            Location lobbySpawn = ArenaConfigManager.getLobbySpawn(arenaName);
            if (lobbySpawn != null && lobbySpawn.getWorld() != null) {
                arenaWorldNames.add(lobbySpawn.getWorld().getName());
            }
            Location redSpawn = ArenaConfigManager.getTeamSpawn(arenaName, "red");
            if (redSpawn != null && redSpawn.getWorld() != null) {
                arenaWorldNames.add(redSpawn.getWorld().getName());
            }
            Location blueSpawn = ArenaConfigManager.getTeamSpawn(arenaName, "blue");
            if (blueSpawn != null && blueSpawn.getWorld() != null) {
                arenaWorldNames.add(blueSpawn.getWorld().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Early exit: only check if player actually moved to a different block (not just looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        World world = player.getWorld();
        double y = player.getLocation().getY();

        // Check if player is in an arena
        String arenaName = ObsidianCommand.playersInArena.get(uuid);
        boolean isInArena = arenaName != null;

        // Handle auto-death for players below Y-level in arena worlds
        if (autoDeathEnabled && y < autoDeathYLevel) {
            // Check if this is an arena world using cached world names (O(1) lookup)
            if (arenaWorldNames.contains(world.getName())) {
                handleAutoDeath(player);
            }
        }

        // Only process arena-specific checks if player is in an arena
        if (!isInArena) {
            return;
        }

        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // Check waiting region boundary during waiting/countdown phase
        if (game == null || game.getGameState() == ArenaStateManager.ArenaState.WAITING ||
            game.getGameState() == ArenaStateManager.ArenaState.STARTING) {
            checkWaitingRegionBoundary(player, arenaName);
            return;
        }

        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            return;
        }

        // Check if player fell into void (Y <= 0)
        if (y <= 0) {
            handleVoidDeath(player, arenaName);
        }
    }

    /**
     * Handle auto-death for players falling below the configured Y-level.
     */
    private void handleAutoDeath(Player player) {
        UUID uuid = player.getUniqueId();

        // Check cooldown to prevent spam deaths
        if (recentlyDied.contains(uuid)) {
            return;
        }

        // Add to cooldown
        recentlyDied.add(uuid);
        org.bukkit.Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
            recentlyDied.remove(uuid);
        }, DEATH_COOLDOWN / 50);

        // Kill the player
        player.setHealth(0);

        // Send death message
        String formattedMessage = autoDeathMessage.replace("&", "§");
        player.sendMessage(formattedMessage);

        Obsidianwars.getInstance().getLogger().info("Player " + player.getName() + " died from auto-death at Y=" + player.getLocation().getY());
    }

    /**
     * Checks if player is outside the waiting region and teleports them back if so.
     */
    private void checkWaitingRegionBoundary(Player player, String arenaName) {
        Location[] waitingRegion = ArenaConfigManager.getWaitingRegion(arenaName);
        if (waitingRegion == null) return; // No waiting region set, no enforcement

        Location playerLoc = player.getLocation();
        Location pos1 = waitingRegion[0];
        Location pos2 = waitingRegion[1];

        // Calculate region bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Check if player is outside bounds (with 1 block tolerance)
        int px = playerLoc.getBlockX();
        int py = playerLoc.getBlockY();
        int pz = playerLoc.getBlockZ();

        if (px < minX - 1 || px > maxX + 1 || py < minY - 1 || py > maxY + 1 || pz < minZ - 1 || pz > maxZ + 1) {
            // Player is outside waiting region - teleport back to waiting spawn
            Location waitingSpawn = ArenaConfigManager.getWaitingSpawn(arenaName);
            if (waitingSpawn == null) {
                waitingSpawn = ArenaConfigManager.getLobbySpawn(arenaName);
            }

            if (waitingSpawn != null) {
                player.teleport(waitingSpawn);
                player.sendMessage("§cYou cannot leave the lobby area!");
            }
        }
    }

    /**
     * Handle void death for players in arena.
     */
    private void handleVoidDeath(Player player, String arenaName) {
        String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
        if (playerTeam == null) return;

        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null) return;

        // During PREPARATION, just respawn at team spawn without spectating
        if (game.getGameState() == ArenaStateManager.ArenaState.PREPARATION) {
            Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
            if (teamSpawn != null) {
                player.teleport(teamSpawn);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.sendMessage("§eYou fell during preparation - respawned at team spawn!");
            }
            return;
        }

        // Get obsidian location for spectating
        Location obsidianLoc = getObsidianLocation(arenaName, playerTeam);
        if (obsidianLoc == null) {
            Obsidianwars.getInstance().getLogger().severe("Obsidian location is null for " + playerTeam + " in arena " + arenaName);
            return;
        }

        // Get team spawn location for respawn
        Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
        if (teamSpawn == null) {
            Obsidianwars.getInstance().getLogger().severe("Team spawn is null for " + playerTeam + " in arena " + arenaName);
            return;
        }

        // If obsidian is destroyed, final elimination
        if (game.isObsidianDestroyed(playerTeam)) {
            // Final elimination - spectator mode at spectator spawn with 1-tick delay
            TaskManager.getInstance().runLater("final-elim-" + player.getUniqueId(), () -> {
                if (player.isOnline()) {
                    SpectatorManager.setSpectatorMode(player, arenaName);

                    String diedTitle = MessagesConfigManager.getMessage("you_died");
                    String finalElim = MessagesConfigManager.getMessage("final_elimination");
                    int fadeIn = Obsidianwars.getInstance().getConfig().getInt("titles.death.fade-in", 10);
                    int stay = Obsidianwars.getInstance().getConfig().getInt("titles.death.stay", 40);
                    int fadeOut = Obsidianwars.getInstance().getConfig().getInt("titles.death.fade-out", 20);
                    player.sendTitle(diedTitle, finalElim, fadeIn, stay, fadeOut);
                    player.sendMessage("§cYou fell into the void and were finally eliminated!");

                    // Update scoreboard
                    ScoreboardManager.updateScoreboard(player);
                }
                // Check win condition AFTER spectator mode is set
                checkWinCondition(arenaName);
            }, 1L, arenaName);
        } else {
            // If obsidian is intact, respawn with countdown near obsidian (delayed by 1 tick)
            TaskManager.getInstance().runLater("void-respawn-" + player.getUniqueId(), () -> {
                if (player.isOnline()) {
                    startRespawnCountdown(player, arenaName, playerTeam, obsidianLoc, teamSpawn);
                }
            }, 1L, arenaName);
        }
    }

    private void checkWinCondition(String arenaName) {
        GameManager.checkArenaWinCondition(arenaName);
    }

    private Location getObsidianLocation(String arenaName, String team) {
        return ArenaConfigManager.getObsidianLocation(arenaName, team);
    }

    private void startRespawnCountdown(Player player, String arenaName, String playerTeam, Location obsidianLoc, Location teamSpawn) {
        // This method would need to be implemented with the respawn countdown logic from GameListener
        // For now, we'll do a simple respawn
        player.teleport(teamSpawn);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.sendMessage("§eYou fell into the void - respawned at team spawn!");
    }

    private Location getTeamSpawn(String arenaName, String team) {
        return ArenaConfigManager.getTeamSpawn(arenaName, team);
    }

    /**
     * Cleanup method to clear static data.
     */
    public static void cleanup() {
        recentlyDied.clear();
        arenaWorldNames.clear();
    }
}
