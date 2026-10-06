package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Smart Queue System for ObsidianWars.
 * Manages player queuing for arenas with auto-matchmaking.
 */
public class QueueManager {

    private static QueueManager instance;
    private final Logger logger;

    // Map of player UUIDs to their queue entry
    private final Map<UUID, QueueEntry> playerQueues;

    // Map of arena names to their queued players
    private final Map<String, Set<UUID>> arenaQueues;

    // Auto-matchmaking task
    private BukkitTask matchmakingTask;

    public QueueManager(Logger logger) {
        this.logger = logger;
        this.playerQueues = new ConcurrentHashMap<>();
        this.arenaQueues = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the QueueManager singleton.
     *
     * @param logger The plugin logger
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new QueueManager(logger);
        }
    }

    /**
     * Gets the QueueManager instance.
     *
     * @return The QueueManager instance
     */
    public static QueueManager getInstance() {
        return instance;
    }

    /**
     * Starts the auto-matchmaking task.
     */
    public void startMatchmaking() {
        if (matchmakingTask != null) {
            return;
        }

        matchmakingTask = TaskManager.getInstance().runTimer(
            "queue-matchmaking",
            this::processMatchmaking,
            20L,  // 1 second initial delay
            20L   // Check every second
        );

        logger.info("Queue matchmaking task started");
    }

    /**
     * Stops the auto-matchmaking task.
     */
    public void stopMatchmaking() {
        if (matchmakingTask != null) {
            matchmakingTask.cancel();
            matchmakingTask = null;
            logger.info("Queue matchmaking task stopped");
        }
    }

    /**
     * Processes auto-matchmaking for all queues.
     */
    private void processMatchmaking() {
        for (String arenaName : new ArrayList<>(arenaQueues.keySet())) {
            processArenaMatchmaking(arenaName);
        }
    }

    /**
     * Processes matchmaking for a specific arena.
     *
     * @param arenaName The arena name
     */
    private void processArenaMatchmaking(String arenaName) {
        Set<UUID> queuedPlayers = arenaQueues.get(arenaName);
        if (queuedPlayers == null || queuedPlayers.isEmpty()) {
            return;
        }

        int minPlayers = ArenaConfigManager.getMinPlayers(arenaName);
        int currentPlayers = TeamManager.getTotalPlayerCount(arenaName);
        int queuedCount = queuedPlayers.size();

        // Check if we have enough players to start
        if (currentPlayers + queuedCount >= minPlayers) {
            // Add queued players to the arena
            for (UUID uuid : new ArrayList<>(queuedPlayers)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    removeFromQueue(uuid);
                    // Add player to arena
                    handleArenaJoin(player, arenaName);
                } else {
                    // Player offline, remove from queue
                    removeFromQueue(uuid);
                }
            }
        }
    }

    /**
     * Adds a player to the queue for a specific arena.
     *
     * @param player The player
     * @param arenaName The arena name (null for auto-assignment)
     * @return true if the player was queued successfully
     */
    public boolean joinQueue(Player player, String arenaName) {
        UUID uuid = player.getUniqueId();

        // Check if player is already in a queue
        if (playerQueues.containsKey(uuid)) {
            player.sendMessage(MessagesConfigManager.getMessage("queue_already_queued"));
            return false;
        }

        // Check if player is already in an arena
        if (ObsidianCommand.playersInArena.containsKey(uuid)) {
            player.sendMessage(MessagesConfigManager.getMessage("not_in_arena"));
            return false;
        }

        // Auto-assign arena if not specified
        if (arenaName == null) {
            arenaName = ArenaStateManager.getInstance().getBestAvailableArena();
            if (arenaName == null) {
                player.sendMessage(MessagesConfigManager.getMessage("arena_not_ready"));
                return false;
            }
        }

        // Check if arena exists and can accept players
        if (!ArenaConfigManager.arenaExists(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_found", "arenaName", arenaName));
            return false;
        }

        if (!ArenaStateManager.getInstance().canAcceptPlayers(arenaName)) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_not_available", "arenaName", arenaName));
            return false;
        }

        // Check arena capacity
        int currentPlayers = TeamManager.getTotalPlayerCount(arenaName);
        int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);
        if (currentPlayers >= maxPlayers) {
            player.sendMessage(MessagesConfigManager.getMessage("arena_full", "current", String.valueOf(currentPlayers), "max", String.valueOf(maxPlayers)));
            return false;
        }

        // Add to queue
        QueueEntry entry = new QueueEntry(uuid, arenaName, System.currentTimeMillis());
        playerQueues.put(uuid, entry);
        arenaQueues.computeIfAbsent(arenaName, k -> ConcurrentHashMap.newKeySet()).add(uuid);

        player.sendMessage(MessagesConfigManager.getMessage("queue_joined", "arenaName", arenaName));

        // If arena is in READY state, try to join immediately
        if (ArenaStateManager.getInstance().isState(arenaName, ArenaStateManager.ArenaState.READY)) {
            removeFromQueue(uuid);
            handleArenaJoin(player, arenaName);
        }

        return true;
    }

    /**
     * Removes a player from the queue.
     *
     * @param uuid The player UUID
     * @return true if the player was removed from queue
     */
    public boolean removeFromQueue(UUID uuid) {
        QueueEntry entry = playerQueues.remove(uuid);
        if (entry != null) {
            Set<UUID> arenaQueue = arenaQueues.get(entry.arenaName);
            if (arenaQueue != null) {
                arenaQueue.remove(uuid);
                if (arenaQueue.isEmpty()) {
                    arenaQueues.remove(entry.arenaName);
                }
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(MessagesConfigManager.getMessage("queue_left"));
            }

            return true;
        }
        return false;
    }

    /**
     * Gets the queue entry for a player.
     *
     * @param uuid The player UUID
     * @return The queue entry, or null if not queued
     */
    public QueueEntry getQueueEntry(UUID uuid) {
        return playerQueues.get(uuid);
    }

    /**
     * Gets the number of players queued for an arena.
     *
     * @param arenaName The arena name
     * @return The number of queued players
     */
    public int getQueueCount(String arenaName) {
        Set<UUID> queue = arenaQueues.get(arenaName);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Gets all queued players for an arena.
     *
     * @param arenaName The arena name
     * @return Set of player UUIDs
     */
    public Set<UUID> getQueuedPlayers(String arenaName) {
        Set<UUID> queue = arenaQueues.get(arenaName);
        return queue != null ? new HashSet<>(queue) : Collections.emptySet();
    }

    /**
     * Gets the total number of queued players across all arenas.
     *
     * @return The total queue count
     */
    public int getTotalQueueCount() {
        return playerQueues.size();
    }

    /**
     * Clears all queues for an arena.
     *
     * @param arenaName The arena name
     */
    public void clearArenaQueue(String arenaName) {
        Set<UUID> queue = arenaQueues.remove(arenaName);
        if (queue != null) {
            for (UUID uuid : queue) {
                playerQueues.remove(uuid);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendMessage(MessagesConfigManager.getMessage("queue_arena_closed"));
                }
            }
        }
    }

    /**
     * Clears all queues.
     */
    public void clearAllQueues() {
        for (UUID uuid : new ArrayList<>(playerQueues.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(MessagesConfigManager.getMessage("queue_cleared"));
            }
        }
        playerQueues.clear();
        arenaQueues.clear();
    }

    /**
     * Handles joining a player to an arena (called after queue processing).
     *
     * @param player The player
     * @param arenaName The arena name
     */
    private void handleArenaJoin(Player player, String arenaName) {
        // This delegates to the existing ObsidianCommand.handleJoinCommand logic
        // We'll need to refactor ObsidianCommand to expose this logic as a separate method
        // For now, we'll use the existing command handler
        ObsidianCommand command = new ObsidianCommand(Obsidianwars.getInstance());
        command.handleJoinCommand(player, new String[]{"join", arenaName});
    }

    /**
     * Cleans up the QueueManager (called on plugin disable).
     */
    public void cleanup() {
        stopMatchmaking();
        clearAllQueues();
        instance = null;
    }

    /**
     * Represents a player's queue entry.
     */
    public static class QueueEntry {
        private final UUID playerUuid;
        private final String arenaName;
        private final long queueTime;

        public QueueEntry(UUID playerUuid, String arenaName, long queueTime) {
            this.playerUuid = playerUuid;
            this.arenaName = arenaName;
            this.queueTime = queueTime;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getArenaName() {
            return arenaName;
        }

        public long getQueueTime() {
            return queueTime;
        }

        public long getQueueDuration() {
            return System.currentTimeMillis() - queueTime;
        }
    }
}
