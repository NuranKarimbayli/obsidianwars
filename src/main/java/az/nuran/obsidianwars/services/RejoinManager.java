package az.nuran.obsidianwars.services;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.handlers.TeamListener;
import az.nuran.obsidianwars.handlers.WandListener;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.ScoreboardManager;
import az.nuran.obsidianwars.managers.TeamManager;
import az.nuran.obsidianwars.models.DisconnectedPlayerData;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player disconnect and rejoin logic.
 * Handles saving player state during disconnects and restoring it on rejoin.
 */
public class RejoinManager {
    private static final Map<UUID, Long> disconnectTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, DisconnectedPlayerData> disconnectedPlayers = new ConcurrentHashMap<>();
    private static final long REJOIN_GRACE_PERIOD = 30000; // 30 seconds in milliseconds

    /**
     * Handles player disconnect - saves state if in active game.
     */
    public static void handleDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (ObsidianCommand.playersInArena.containsKey(uuid)) {
            String arenaName = ObsidianCommand.playersInArena.get(uuid);
            String team = TeamListener.playerTeams.get(uuid);

            // Only save state if in PLAYING or PREPARATION state and player has a team
            if (team == null) {
                // Player without team - just record disconnect time for legacy
                disconnectTimes.put(uuid, System.currentTimeMillis());
                return;
            }

            // Only save state if in PLAYING or PREPARATION state
            GameManager.ArenaGame game = GameManager.getGame(arenaName);
            if (game != null && (game.getGameState() == ArenaStateManager.ArenaState.PLAYING || game.getGameState() == ArenaStateManager.ArenaState.PREPARATION)) {
                // Save player state for rejoin
                DisconnectedPlayerData data = new DisconnectedPlayerData(player, arenaName, team);
                disconnectedPlayers.put(uuid, data);
                disconnectTimes.put(uuid, System.currentTimeMillis());

                Obsidianwars.getInstance().getLogger().info("Player " + player.getName() + " disconnected from arena " + arenaName + " - state saved for rejoin");

                // Add bounds checking to prevent memory leak
                if (disconnectedPlayers.size() > 1000) {
                    cleanupExpiredDisconnectRecords();
                    Obsidianwars.getInstance().getLogger().warning(
                        "Disconnected players map exceeded 1000 entries - forced cleanup"
                    );
                }
            } else {
                // Not in active game - immediate cleanup if game ended
                if (game == null || game.getGameState() == ArenaStateManager.ArenaState.ENDED) {
                    // Game ended, clean up immediately
                    ObsidianCommand.playersInArena.remove(uuid);
                    TeamListener.playerTeams.remove(uuid);
                    WandListener.pos1Map.remove(uuid);
                    WandListener.pos2Map.remove(uuid);
                    Obsidianwars.getInstance().getLogger().info(
                        "Player " + player.getName() + " disconnected from ended arena " + arenaName + " - immediate cleanup"
                    );
                } else {
                    // Not in active game, just record disconnect time for legacy
                    disconnectTimes.put(uuid, System.currentTimeMillis());
                }
            }
        }
    }

    /**
     * Checks if a player can rejoin (within grace period).
     */
    public static boolean canRejoin(Player player) {
        UUID uuid = player.getUniqueId();
        Long disconnectTime = disconnectTimes.get(uuid);
        if (disconnectTime == null) return false;

        long elapsed = System.currentTimeMillis() - disconnectTime;
        if (elapsed < REJOIN_GRACE_PERIOD) {
            return true;
        }

        disconnectTimes.remove(uuid);
        return false;
    }

    /**
     * Attempts to restore a disconnected player to their game state.
     * Returns true if restoration was successful, false otherwise.
     */
    public static boolean restoreDisconnectedPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        DisconnectedPlayerData data = disconnectedPlayers.get(uuid);

        if (data == null) {
            return false;
        }

        // Check if still within grace period
        long elapsed = System.currentTimeMillis() - data.getDisconnectTime();
        if (elapsed >= REJOIN_GRACE_PERIOD) {
            // Grace period expired, clean up
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);
            return false;
        }

        // Check if game still exists and is in valid state
        GameManager.ArenaGame game = GameManager.getGame(data.getArenaName());
        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            // Game ended or invalid, clean up and return false
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);
            return false;
        }

        // Restore player state
        try {
            // Restore inventory
            player.getInventory().setContents(data.getInventoryContents());
            player.getInventory().setArmorContents(data.getArmorContents());

            // Restore health and hunger
            player.setHealth(data.getHealth());
            player.setFoodLevel(data.getFoodLevel());

            // Restore potion effects
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            for (org.bukkit.potion.PotionEffect effect : data.getPotionEffects()) {
                player.addPotionEffect(effect);
            }

            // Restore team (without armor to prevent leather armor on respawn)
            TeamManager.setPlayerTeam(player, data.getArenaName(), data.getTeam(), false);

            // Restore game mode
            if (data.wasSpectator()) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
                // Teleport to last location or obsidian
                Location restoreLoc = data.getLastLocation();
                if (restoreLoc != null) {
                    player.teleport(restoreLoc);
                }
            } else {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.setAllowFlight(false);
                player.setFlying(false);

                // Teleport to team spawn without spawn protection
                Location teamSpawn = ArenaConfigManager.getTeamSpawn(data.getArenaName(), data.getTeam());
                if (teamSpawn != null) {
                    player.teleport(teamSpawn);
                    // NO spawn protection - players can take damage immediately
                } else {
                    // Fallback to last location
                    if (data.getLastLocation() != null) {
                        player.teleport(data.getLastLocation());
                    }
                }
            }

            // Update scoreboard
            ScoreboardManager.updateScoreboard(player);

            // Clear disconnect records
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);

            Obsidianwars.getInstance().getLogger().info("Player " + player.getName() + " successfully restored to arena " + data.getArenaName());

            return true;
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Error restoring player " + player.getName(), e);
            // Clean up on error
            disconnectedPlayers.remove(uuid);
            disconnectTimes.remove(uuid);
            return false;
        }
    }

    /**
     * Permanently removes a disconnected player from the game after grace period expires.
     */
    public static void expireDisconnectedPlayer(UUID uuid) {
        DisconnectedPlayerData data = disconnectedPlayers.remove(uuid);
        disconnectTimes.remove(uuid);

        if (data != null) {
            // Remove from arena tracking
            ObsidianCommand.playersInArena.remove(uuid);
            TeamListener.playerTeams.remove(uuid);

            // Check if this causes team elimination
            GameManager.checkTeamEliminationOnLeave(data.getArenaName());

            Obsidianwars.getInstance().getLogger().info("Player " + uuid + " grace period expired - permanently removed from arena " + data.getArenaName());
        }
    }

    /**
     * Checks if a player is currently disconnected but within grace period.
     */
    public static boolean isPlayerDisconnected(UUID uuid) {
        return disconnectedPlayers.containsKey(uuid);
    }

    /**
     * Gets the arena name for a disconnected player.
     */
    public static String getDisconnectedPlayerArena(UUID uuid) {
        DisconnectedPlayerData data = disconnectedPlayers.get(uuid);
        return data != null ? data.getArenaName() : null;
    }

    /**
     * Gets the disconnected player data for a UUID.
     */
    public static DisconnectedPlayerData getDisconnectedPlayerData(UUID uuid) {
        return disconnectedPlayers.get(uuid);
    }

    /**
     * Clears disconnect record for a player (e.g., on manual leave).
     */
    public static void clearDisconnectRecord(UUID uuid) {
        disconnectTimes.remove(uuid);
        disconnectedPlayers.remove(uuid);
    }

    /**
     * Cleans up expired disconnect records.
     */
    public static void cleanupExpiredDisconnectRecords() {
        long currentTime = System.currentTimeMillis();

        // Find expired players
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : disconnectTimes.entrySet()) {
            long elapsed = currentTime - entry.getValue();
            if (elapsed >= REJOIN_GRACE_PERIOD) {
                expired.add(entry.getKey());
            }
        }

        // Process expired players
        for (UUID uuid : expired) {
            expireDisconnectedPlayer(uuid);
        }
    }

    /**
     * Gets all disconnected players for an arena.
     */
    public static List<DisconnectedPlayerData> getDisconnectedPlayersForArena(String arenaName) {
        List<DisconnectedPlayerData> result = new ArrayList<>();
        for (DisconnectedPlayerData data : disconnectedPlayers.values()) {
            if (data.getArenaName().equals(arenaName)) {
                result.add(data);
            }
        }
        return result;
    }

    /**
     * Cleans up all disconnect records for an arena.
     */
    public static void cleanupArena(String arenaName) {
        disconnectedPlayers.entrySet().removeIf(entry -> {
            DisconnectedPlayerData data = entry.getValue();
            if (data.getArenaName().equals(arenaName)) {
                // Also remove from playersInArena if still there
                ObsidianCommand.playersInArena.remove(entry.getKey());
                TeamListener.playerTeams.remove(entry.getKey());
                disconnectTimes.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Clears all disconnect records (for plugin shutdown).
     */
    public static void cleanupAll() {
        disconnectTimes.clear();
        disconnectedPlayers.clear();
    }
}
