package az.nuran.obsidianwars;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * PlaceholderAPI expansion for ObsidianWars.
 * Provides placeholders for player stats, arena information, and game state.
 * This is a soft-depend - will only load if PlaceholderAPI is installed.
 */
public class ObsidianWarsExpansion extends PlaceholderExpansion {

    private final Obsidianwars plugin;

    public ObsidianWarsExpansion(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "obsidianwars";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nuran";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Player stats placeholders
        if (params.startsWith("player_")) {
            return handlePlayerPlaceholder(player, params);
        }

        // Arena placeholders
        if (params.startsWith("arena_")) {
            return handleArenaPlaceholder(params);
        }

        return null;
    }

    /**
     * Handles player-related placeholders.
     *
     * @param player The player
     * @param params The placeholder parameters
     * @return The placeholder value
     */
    private String handlePlayerPlaceholder(Player player, String params) {
        UUID uuid = player.getUniqueId();
        StatsManager.PlayerStats stats = StatsManager.getPlayerStats(player);

        if (stats == null) {
            return "0";
        }

        switch (params) {
            case "player_kills":
                return String.valueOf(stats.getKills());

            case "player_deaths":
                return String.valueOf(stats.getDeaths());

            case "player_wins":
                return String.valueOf(stats.getWins());

            case "player_losses":
                return String.valueOf(stats.getLosses());

            case "player_level":
                return String.valueOf(stats.getLevel());

            case "player_xp":
                return String.valueOf(stats.getXp());

            case "player_team":
                String arenaName = ObsidianCommand.playersInArena.get(uuid);
                if (arenaName != null) {
                    String team = TeamListener.playerTeams.get(uuid);
                    if (team != null) {
                        return team.equals("red") ? "Red" : "Blue";
                    }
                }
                return "None";

            case "player_kdr":
                double kdr = stats.getDeaths() > 0 ? (double) stats.getKills() / stats.getDeaths() : stats.getKills();
                return String.format("%.2f", kdr);

            case "player_winrate":
                int totalGames = stats.getWins() + stats.getLosses();
                double winRate = totalGames > 0 ? (double) stats.getWins() / totalGames * 100 : 0;
                return String.format("%.1f", winRate);

            case "player_arena":
                String currentArena = ObsidianCommand.playersInArena.get(uuid);
                return currentArena != null ? currentArena : "None";

            default:
                return null;
        }
    }

    /**
     * Handles arena-related placeholders.
     *
     * @param params The placeholder parameters
     * @return The placeholder value
     */
    private String handleArenaPlaceholder(String params) {
        // Format: obsidianwars_arena_<stat>_<arena_name>
        String[] parts = params.split("_");
        if (parts.length < 4) {
            return null;
        }

        String statType = parts[2];
        String arenaName = parts[3];

        if (!ArenaConfigManager.arenaExists(arenaName)) {
            return "Unknown";
        }

        switch (statType) {
            case "status":
                ArenaStateManager.ArenaState state = ArenaStateManager.getInstance().getState(arenaName);
                return state != null ? state.name() : "Unknown";

            case "players":
                return String.valueOf(TeamManager.getTotalPlayerCount(arenaName));

            case "max_players":
                return String.valueOf(ArenaConfigManager.getMaxPlayers(arenaName));

            case "red_players":
                return String.valueOf(TeamManager.getTeamPlayerCount(arenaName, "red"));

            case "blue_players":
                return String.valueOf(TeamManager.getTeamPlayerCount(arenaName, "blue"));

            case "phase":
                ArenaStateManager.ArenaState gameState = ArenaStateManager.getInstance().getState(arenaName);
                if (gameState == null) return "Unknown";
                switch (gameState) {
                    case WAITING:
                    case READY:
                        return "Lobby";
                    case STARTING:
                        return "Starting";
                    case PREPARATION:
                        return "Preparation";
                    case PLAYING:
                        return "Playing";
                    case SUDDEN_DEATH:
                        return "Sudden Death";
                    case ENDED:
                        return "Ended";
                    default:
                        return gameState.name();
                }

            case "time":
                GameManager.ArenaGame game = GameManager.getGame(arenaName);
                if (game != null) {
                    int timeLimitMinutes = ArenaConfigManager.getTimeLimit(arenaName);
                    int timeLimitSeconds = timeLimitMinutes * 60;
                    int gameTime = game.getGameTime();
                    int remainingTime = timeLimitSeconds - gameTime;
                    int minutes = remainingTime / 60;
                    int seconds = remainingTime % 60;
                    return String.format("%02d:%02d", minutes, seconds);
                }
                return "00:00";

            case "preparation_time":
                int prepTime = WallManager.getRemainingTime(arenaName);
                int prepMinutes = prepTime / 60;
                int prepSeconds = prepTime % 60;
                return String.format("%02d:%02d", prepMinutes, prepSeconds);

            case "sudden_death_time":
                int sdTime = WallManager.getSuddenDeathRemainingTime(arenaName);
                int sdMinutes = sdTime / 60;
                int sdSeconds = sdTime % 60;
                return String.format("%02d:%02d", sdMinutes, sdSeconds);

            default:
                return null;
        }
    }

    /**
     * Registers the expansion if PlaceholderAPI is installed.
     *
     * @param plugin The plugin instance
     */
    public static void registerIfAvailable(Obsidianwars plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            ObsidianWarsExpansion expansion = new ObsidianWarsExpansion(plugin);
            expansion.register();
            plugin.getLogger().info("PlaceholderAPI expansion registered successfully!");
        } else {
            plugin.getLogger().info("PlaceholderAPI not found - placeholders will not be available.");
        }
    }
}
