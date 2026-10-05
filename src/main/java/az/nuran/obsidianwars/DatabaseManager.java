package az.nuran.obsidianwars;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Manages database connections and operations for ObsidianWars statistics.
 * Supports both SQLite (local file) and MySQL (remote) storage.
 */
public class DatabaseManager {

    private static HikariDataSource dataSource;
    private static String storageType;

    /**
     * Initializes the database connection pool based on config.
     */
    public static void initialize() {
        storageType = Obsidianwars.getInstance().getConfig().getString("database.storage", "sqlite").toLowerCase();

        HikariConfig config = new HikariConfig();

        if (storageType.equals("mysql")) {
            // MySQL configuration
            String host = Obsidianwars.getInstance().getConfig().getString("database.mysql.host", "localhost");
            int port = Obsidianwars.getInstance().getConfig().getInt("database.mysql.port", 3306);
            String database = Obsidianwars.getInstance().getConfig().getString("database.mysql.database", "obsidianwars");
            String username = Obsidianwars.getInstance().getConfig().getString("database.mysql.username", "root");
            String password = Obsidianwars.getInstance().getConfig().getString("database.mysql.password", "password");
            int poolSize = Obsidianwars.getInstance().getConfig().getInt("database.mysql.pool-size", 10);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(Math.max(1, poolSize / 2)); // Keep at least half the pool ready
            config.setConnectionTimeout(30000); // 30 seconds connection timeout
            config.setIdleTimeout(600000); // 10 minutes idle timeout
            config.setMaxLifetime(1800000); // 30 minutes max connection lifetime
            config.setPoolName("ObsidianWars-MySQL-Pool");

            Obsidianwars.getInstance().getLogger().info("Database: Using MySQL connection to " + host + ":" + port + "/" + database);
        } else {
            // SQLite configuration (default)
            String fileName = Obsidianwars.getInstance().getConfig().getString("database.sqlite.file-name", "stats.db");
            String dbPath = Obsidianwars.getInstance().getDataFolder().getAbsolutePath() + "/" + fileName;

            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setMaximumPoolSize(1); // SQLite doesn't support concurrent writes
            config.setMinimumIdle(1); // Keep one connection ready
            config.setConnectionTimeout(30000); // 30 seconds connection timeout
            config.setIdleTimeout(600000); // 10 minutes idle timeout
            config.setMaxLifetime(1800000); // 30 minutes max connection lifetime
            config.setPoolName("ObsidianWars-SQLite-Pool");

            Obsidianwars.getInstance().getLogger().info("Database: Using SQLite file at " + dbPath);
        }

        dataSource = new HikariDataSource(config);

        // Create tables
        createTables();
    }

    /**
     * Creates the necessary database tables.
     */
    private static void createTables() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                // Create player_stats table
                String sql = """
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(16),
                        games_played INT DEFAULT 0,
                        wins INT DEFAULT 0,
                        losses INT DEFAULT 0,
                        kills INT DEFAULT 0,
                        deaths INT DEFAULT 0,
                        final_kills INT DEFAULT 0,
                        final_deaths INT DEFAULT 0,
                        obsidian_broken INT DEFAULT 0,
                        obsidian_lost INT DEFAULT 0,
                        winstreak INT DEFAULT 0,
                        longest_kill_streak INT DEFAULT 0,
                        level INT DEFAULT 1,
                        xp INT DEFAULT 0,
                        last_updated BIGINT
                    )
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }

                // Add level and xp columns if they don't exist (for existing databases)
                try {
                    String alterSql = "ALTER TABLE player_stats ADD COLUMN level INT DEFAULT 1";
                    try (PreparedStatement alterStmt = conn.prepareStatement(alterSql)) {
                        alterStmt.execute();
                    }
                } catch (SQLException e) {
                    // Column already exists, ignore
                }

                try {
                    String alterSql = "ALTER TABLE player_stats ADD COLUMN xp INT DEFAULT 0";
                    try (PreparedStatement alterStmt = conn.prepareStatement(alterSql)) {
                        alterStmt.execute();
                    }
                } catch (SQLException e) {
                    // Column already exists, ignore
                }

                // Create time-framed stats table
                String timeSql = """
                    CREATE TABLE IF NOT EXISTS time_framed_stats (
                        uuid VARCHAR(36),
                        period VARCHAR(10),
                        wins INT DEFAULT 0,
                        obsidian_broken INT DEFAULT 0,
                        final_kills INT DEFAULT 0,
                        last_updated BIGINT,
                        PRIMARY KEY (uuid, period)
                    )
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(timeSql)) {
                    stmt.execute();
                }

                Obsidianwars.getInstance().getLogger().info("Database tables created successfully");
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().severe("Failed to create database tables: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Gets a database connection from the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Closes the database connection pool.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Obsidianwars.getInstance().getLogger().info("Database connection pool closed");
        }
    }

    /**
     * Executes an async database operation.
     */
    public static void executeAsync(Consumer<Connection> operation) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                operation.accept(conn);
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().severe("Database operation failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Executes an async database operation with a callback.
     */
    public static <T> void executeAsync(Consumer<Connection> operation, Consumer<T> callback, T defaultValue) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                operation.accept(conn);
                if (callback != null) {
                    callback.accept(defaultValue);
                }
            } catch (SQLException e) {
                Obsidianwars.getInstance().getLogger().severe("Database operation failed: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.accept(defaultValue);
                }
            }
        });
    }

    /**
     * Gets the storage type (sqlite or mysql).
     */
    public static String getStorageType() {
        return storageType;
    }
}
