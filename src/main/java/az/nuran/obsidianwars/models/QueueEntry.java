package az.nuran.obsidianwars.models;

import java.util.UUID;

/**
 * Represents a player's queue entry.
 */
public class QueueEntry {
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
