package az.nuran.obsidianwars;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Data Access Object for player statistics.
 * Handles all database operations for loading and saving player stats.
 */
public class StatsDAO {

    /**
     * Loads player stats from the database.
     * This version takes a Connection parameter and is used by async operations.
     */
    public static StatsManager.PlayerStats loadPlayerStats(Connection conn, UUID uuid) throws SQLException {
        StatsManager.PlayerStats stats = new StatsManager.PlayerStats(uuid);

        String sql = """
            SELECT games_played, wins, losses, kills, deaths, final_kills, final_deaths,
                   obsidian_broken, obsidian_lost, winstreak, longest_kill_streak, level, xp
            FROM player_stats
            WHERE uuid = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.setGamesPlayed(rs.getInt("games_played"));
                    stats.setWins(rs.getInt("wins"));
                    stats.setLosses(rs.getInt("losses"));
                    stats.setKills(rs.getInt("kills"));
                    stats.setDeaths(rs.getInt("deaths"));
                    stats.setFinalKills(rs.getInt("final_kills"));
                    stats.setFinalDeaths(rs.getInt("final_deaths"));
                    stats.setObsidianBroken(rs.getInt("obsidian_broken"));
                    stats.setObsidianLost(rs.getInt("obsidian_lost"));
                    stats.setWinstreak(rs.getInt("winstreak"));
                    stats.setLongestKillStreak(rs.getInt("longest_kill_streak"));

                    // Load level data
                    int level = rs.getInt("level");
                    int xp = rs.getInt("xp");
                    LevelManager.loadPlayerLevel(uuid, level, xp);
                } else {
                    // New player, initialize level to 1 with 0 XP
                    LevelManager.loadPlayerLevel(uuid, 1, 0);
                }
            }
        }

        // Load time-framed stats
        loadTimeFramedStats(conn, uuid, stats, "daily");
        loadTimeFramedStats(conn, uuid, stats, "weekly");
        loadTimeFramedStats(conn, uuid, stats, "monthly");

        return stats;
    }

    /**
     * Loads player stats from the database (synchronous version).
     * Creates its own connection - use with caution.
     */
    public static StatsManager.PlayerStats loadPlayerStats(UUID uuid) {
        StatsManager.PlayerStats stats = new StatsManager.PlayerStats(uuid);

        try (Connection conn = DatabaseManager.getConnection()) {
            stats = loadPlayerStats(conn, uuid);
        } catch (SQLException e) {
            Obsidianwars.getInstance().getLogger().warning("Failed to load stats for " + uuid + ": " + e.getMessage());
            // Initialize level to 1 with 0 XP on error
            LevelManager.loadPlayerLevel(uuid, 1, 0);
        }

        return stats;
    }

    /**
     * Loads time-framed stats for a specific period.
     */
    private static void loadTimeFramedStats(Connection conn, UUID uuid, StatsManager.PlayerStats stats, String period) throws SQLException {
        String sql = """
            SELECT wins, obsidian_broken, final_kills
            FROM time_framed_stats
            WHERE uuid = ? AND period = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, period);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (period.equals("daily")) {
                        stats.setDailyWins(rs.getInt("wins"));
                        stats.setDailyObsidianBroken(rs.getInt("obsidian_broken"));
                        stats.setDailyFinalKills(rs.getInt("final_kills"));
                    } else if (period.equals("weekly")) {
                        stats.setWeeklyWins(rs.getInt("wins"));
                        stats.setWeeklyObsidianBroken(rs.getInt("obsidian_broken"));
                        stats.setWeeklyFinalKills(rs.getInt("final_kills"));
                    } else if (period.equals("monthly")) {
                        stats.setMonthlyWins(rs.getInt("wins"));
                        stats.setMonthlyObsidianBroken(rs.getInt("obsidian_broken"));
                        stats.setMonthlyFinalKills(rs.getInt("final_kills"));
                    }
                }
            }
        }
    }

    /**
     * Saves player stats to the database asynchronously.
     */
    public static void savePlayerStats(UUID uuid, String username, StatsManager.PlayerStats stats) {
        DatabaseManager.executeAsync(conn -> {
            try {
                savePlayerStatsSync(conn, uuid, username, stats);
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().severe("Failed to save stats for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Saves player stats to the database synchronously using the provided connection.
     */
    public static void savePlayerStatsSync(Connection conn, UUID uuid, String username, StatsManager.PlayerStats stats) throws SQLException {
        // Use different SQL based on database type
        if (DatabaseManager.getStorageType().equals("mysql")) {
            savePlayerStatsMySQL(conn, uuid, username, stats);
        } else {
            savePlayerStatsSQLite(conn, uuid, username, stats);
        }

        // Save time-framed stats
        saveTimeFramedStats(conn, uuid, stats, "daily");
        saveTimeFramedStats(conn, uuid, stats, "weekly");
        saveTimeFramedStats(conn, uuid, stats, "monthly");
    }

    /**
     * Saves player level data asynchronously.
     */
    public static void savePlayerLevel(UUID uuid) {
        DatabaseManager.executeAsync(conn -> {
            try {
                savePlayerLevelSync(conn, uuid);
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().severe("Failed to save level for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Saves player level data synchronously using the provided connection.
     */
    public static void savePlayerLevelSync(Connection conn, UUID uuid) throws SQLException {
        LevelManager.PlayerLevel playerLevel = LevelManager.getPlayerLevel(uuid);
        if (playerLevel == null) return;

        if (DatabaseManager.getStorageType().equals("mysql")) {
            savePlayerLevelMySQL(conn, uuid, playerLevel);
        } else {
            savePlayerLevelSQLite(conn, uuid, playerLevel);
        }
    }

    /**
     * Saves player level data using MySQL syntax.
     */
    private static void savePlayerLevelMySQL(Connection conn, UUID uuid, LevelManager.PlayerLevel playerLevel) throws SQLException {
        String sql = """
            UPDATE player_stats
            SET level = ?, xp = ?, last_updated = ?
            WHERE uuid = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            stmt.setInt(1, playerLevel.getLevel());
            stmt.setInt(2, playerLevel.getCurrentXp());
            stmt.setLong(3, now);
            stmt.setString(4, uuid.toString());
            stmt.executeUpdate();
        }
    }

    /**
     * Saves player level data using SQLite syntax.
     */
    private static void savePlayerLevelSQLite(Connection conn, UUID uuid, LevelManager.PlayerLevel playerLevel) throws SQLException {
        String sql = """
            UPDATE player_stats
            SET level = ?, xp = ?, last_updated = ?
            WHERE uuid = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            stmt.setInt(1, playerLevel.getLevel());
            stmt.setInt(2, playerLevel.getCurrentXp());
            stmt.setLong(3, now);
            stmt.setString(4, uuid.toString());
            stmt.executeUpdate();
        }
    }

    /**
     * Saves player stats using MySQL syntax.
     */
    private static void savePlayerStatsMySQL(Connection conn, UUID uuid, String username, StatsManager.PlayerStats stats) throws SQLException {
        String sql = """
            INSERT INTO player_stats (uuid, username, games_played, wins, losses, kills, deaths,
                                          final_kills, final_deaths, obsidian_broken, obsidian_lost,
                                          winstreak, longest_kill_streak, level, xp, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = ?, games_played = ?, wins = ?, losses = ?, kills = ?, deaths = ?,
                final_kills = ?, final_deaths = ?, obsidian_broken = ?, obsidian_lost = ?,
                winstreak = ?, longest_kill_streak = ?, level = ?, xp = ?, last_updated = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            LevelManager.PlayerLevel playerLevel = LevelManager.getPlayerLevel(uuid);

            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setInt(3, stats.getGamesPlayed());
            stmt.setInt(4, stats.getWins());
            stmt.setInt(5, stats.getLosses());
            stmt.setInt(6, stats.getKills());
            stmt.setInt(7, stats.getDeaths());
            stmt.setInt(8, stats.getFinalKills());
            stmt.setInt(9, stats.getFinalDeaths());
            stmt.setInt(10, stats.getObsidianBroken());
            stmt.setInt(11, stats.getObsidianLost());
            stmt.setInt(12, stats.getWinstreak());
            stmt.setInt(13, stats.getLongestKillStreak());
            stmt.setInt(14, playerLevel != null ? playerLevel.getLevel() : 1);
            stmt.setInt(15, playerLevel != null ? playerLevel.getCurrentXp() : 0);
            stmt.setLong(16, now);

            // Update values
            stmt.setString(17, username);
            stmt.setInt(18, stats.getGamesPlayed());
            stmt.setInt(19, stats.getWins());
            stmt.setInt(20, stats.getLosses());
            stmt.setInt(21, stats.getKills());
            stmt.setInt(22, stats.getDeaths());
            stmt.setInt(23, stats.getFinalKills());
            stmt.setInt(24, stats.getFinalDeaths());
            stmt.setInt(25, stats.getObsidianBroken());
            stmt.setInt(26, stats.getObsidianLost());
            stmt.setInt(27, stats.getWinstreak());
            stmt.setInt(28, stats.getLongestKillStreak());
            stmt.setInt(29, playerLevel != null ? playerLevel.getLevel() : 1);
            stmt.setInt(30, playerLevel != null ? playerLevel.getCurrentXp() : 0);
            stmt.setLong(31, now);

            stmt.executeUpdate();
        }
    }

    /**
     * Saves player stats using SQLite syntax.
     */
    private static void savePlayerStatsSQLite(Connection conn, UUID uuid, String username, StatsManager.PlayerStats stats) throws SQLException {
        String sql = """
            INSERT INTO player_stats (uuid, username, games_played, wins, losses, kills, deaths,
                                          final_kills, final_deaths, obsidian_broken, obsidian_lost,
                                          winstreak, longest_kill_streak, level, xp, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                username = ?, games_played = ?, wins = ?, losses = ?, kills = ?, deaths = ?,
                final_kills = ?, final_deaths = ?, obsidian_broken = ?, obsidian_lost = ?,
                winstreak = ?, longest_kill_streak = ?, level = ?, xp = ?, last_updated = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            LevelManager.PlayerLevel playerLevel = LevelManager.getPlayerLevel(uuid);

            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setInt(3, stats.getGamesPlayed());
            stmt.setInt(4, stats.getWins());
            stmt.setInt(5, stats.getLosses());
            stmt.setInt(6, stats.getKills());
            stmt.setInt(7, stats.getDeaths());
            stmt.setInt(8, stats.getFinalKills());
            stmt.setInt(9, stats.getFinalDeaths());
            stmt.setInt(10, stats.getObsidianBroken());
            stmt.setInt(11, stats.getObsidianLost());
            stmt.setInt(12, stats.getWinstreak());
            stmt.setInt(13, stats.getLongestKillStreak());
            stmt.setInt(14, playerLevel != null ? playerLevel.getLevel() : 1);
            stmt.setInt(15, playerLevel != null ? playerLevel.getCurrentXp() : 0);
            stmt.setLong(16, now);

            // Update values
            stmt.setString(17, username);
            stmt.setInt(18, stats.getGamesPlayed());
            stmt.setInt(19, stats.getWins());
            stmt.setInt(20, stats.getLosses());
            stmt.setInt(21, stats.getKills());
            stmt.setInt(22, stats.getDeaths());
            stmt.setInt(23, stats.getFinalKills());
            stmt.setInt(24, stats.getFinalDeaths());
            stmt.setInt(25, stats.getObsidianBroken());
            stmt.setInt(26, stats.getObsidianLost());
            stmt.setInt(27, stats.getWinstreak());
            stmt.setInt(28, stats.getLongestKillStreak());
            stmt.setInt(29, playerLevel != null ? playerLevel.getLevel() : 1);
            stmt.setInt(30, playerLevel != null ? playerLevel.getCurrentXp() : 0);
            stmt.setLong(31, now);

            stmt.executeUpdate();
        }
    }

    /**
     * Saves time-framed stats for a specific period.
     */
    private static void saveTimeFramedStats(Connection conn, UUID uuid, StatsManager.PlayerStats stats, String period) throws SQLException {
        String sql;

        if (DatabaseManager.getStorageType().equals("mysql")) {
            sql = """
                INSERT INTO time_framed_stats (uuid, period, wins, obsidian_broken, final_kills, last_updated)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    wins = ?, obsidian_broken = ?, final_kills = ?, last_updated = ?
                """;
        } else {
            sql = """
                INSERT INTO time_framed_stats (uuid, period, wins, obsidian_broken, final_kills, last_updated)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid, period) DO UPDATE SET
                    wins = ?, obsidian_broken = ?, final_kills = ?, last_updated = ?
                """;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            int wins, obsidianBroken, finalKills;

            if (period.equals("daily")) {
                wins = stats.getDailyWins();
                obsidianBroken = stats.getDailyObsidianBroken();
                finalKills = stats.getDailyFinalKills();
            } else if (period.equals("weekly")) {
                wins = stats.getWeeklyWins();
                obsidianBroken = stats.getWeeklyObsidianBroken();
                finalKills = stats.getWeeklyFinalKills();
            } else { // monthly
                wins = stats.getMonthlyWins();
                obsidianBroken = stats.getMonthlyObsidianBroken();
                finalKills = stats.getMonthlyFinalKills();
            }

            stmt.setString(1, uuid.toString());
            stmt.setString(2, period);
            stmt.setInt(3, wins);
            stmt.setInt(4, obsidianBroken);
            stmt.setInt(5, finalKills);
            stmt.setLong(6, now);

            // Update values
            stmt.setInt(7, wins);
            stmt.setInt(8, obsidianBroken);
            stmt.setInt(9, finalKills);
            stmt.setLong(10, now);

            stmt.executeUpdate();
        }
    }

    /**
     * Resets time-framed stats that are older than their period.
     * Should be called periodically or on server start.
     */
    public static void resetExpiredTimeFramedStats() {
        DatabaseManager.executeAsync(conn -> {
            long now = System.currentTimeMillis();
            long dayMs = 24 * 60 * 60 * 1000L;
            long weekMs = 7 * dayMs;
            long monthMs = 30 * dayMs;

            try {
                // Reset daily stats older than 1 day
                String dailySql = "UPDATE time_framed_stats SET wins = 0, obsidian_broken = 0, final_kills = 0 WHERE period = 'daily' AND last_updated < ?";
                try (PreparedStatement stmt = conn.prepareStatement(dailySql)) {
                    stmt.setLong(1, now - dayMs);
                    stmt.executeUpdate();
                }

                // Reset weekly stats older than 1 week
                String weeklySql = "UPDATE time_framed_stats SET wins = 0, obsidian_broken = 0, final_kills = 0 WHERE period = 'weekly' AND last_updated < ?";
                try (PreparedStatement stmt = conn.prepareStatement(weeklySql)) {
                    stmt.setLong(1, now - weekMs);
                    stmt.executeUpdate();
                }

                // Reset monthly stats older than 1 month
                String monthlySql = "UPDATE time_framed_stats SET wins = 0, obsidian_broken = 0, final_kills = 0 WHERE period = 'monthly' AND last_updated < ?";
                try (PreparedStatement stmt = conn.prepareStatement(monthlySql)) {
                    stmt.setLong(1, now - monthMs);
                    stmt.executeUpdate();
                }

                Obsidianwars.getInstance().getLogger().info("Expired time-framed stats reset successfully");
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().severe("Failed to reset expired time-framed stats: " + e.getMessage());
            }
        });
    }
}
