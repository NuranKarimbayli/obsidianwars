package az.nuran.obsidianwars;

/**
 * Utility class for accessing team configuration from config.yml.
 * Provides centralized access to team colors, names, and wool types.
 */
public class TeamConfig {

    /**
     * Gets the team color code from config.
     *
     * @param team The team name ("red" or "blue")
     * @return The color code (e.g., "§c" for red, "§9" for blue)
     */
    public static String getTeamColor(String team) {
        return Obsidianwars.getInstance().getConfig().getString("teams." + team + ".color", "§f");
    }

    /**
     * Gets the team display name from config.
     *
     * @param team The team name ("red" or "blue")
     * @return The team name (e.g., "Qırmızı" for red, "Mavi" for blue)
     */
    public static String getTeamName(String team) {
        return Obsidianwars.getInstance().getConfig().getString("teams." + team + ".name", team);
    }

    /**
     * Gets the wool material type for the team from config.
     *
     * @param team The team name ("red" or "blue")
     * @return The wool type (e.g., "RED_WOOL" for red, "BLUE_WOOL" for blue)
     */
    public static String getWoolType(String team) {
        return Obsidianwars.getInstance().getConfig().getString("teams." + team + ".wool_type", "WHITE_WOOL");
    }
}
