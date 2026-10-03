package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized team management for ObsidianWars.
 * Handles all team-related operations including assignment, switching, balance checking, and visual effects.
 */
public class TeamManager {

    private static final String RED_TEAM_NAME = "obsidian_red";
    private static final String BLUE_TEAM_NAME = "obsidian_blue";
    private static final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    /**
     * Auto-assigns a player to the team with fewer players upon joining the arena.
     * This is called immediately when a player joins via /obsidian play.
     *
     * @param player The player to auto-assign
     * @param arenaName The arena the player joined
     * @return The assigned team ("red" or "blue")
     */
    public static String autoAssignTeam(Player player, String arenaName) {
        int redCount = getTeamPlayerCount(arenaName, "red");
        int blueCount = getTeamPlayerCount(arenaName, "blue");

        // Assign to the team with fewer players
        String assignedTeam = (redCount <= blueCount) ? "red" : "blue";

        // Apply team assignment
        setPlayerTeam(player, arenaName, assignedTeam);

        return assignedTeam;
    }

    /**
     * Sets a player's team with full visual effects (armor, scoreboard, tablist).
     *
     * @param player The player
     * @param arenaName The arena name
     * @param team The team to assign ("red" or "blue")
     */
    public static void setPlayerTeam(Player player, String arenaName, String team) {
        // Store in TeamListener's map (legacy compatibility)
        TeamListener.playerTeams.put(player.getUniqueId(), team);

        // Apply visual effects
        applyTeamColor(player, team);
        setupScoreboardTeam(player, team);

        // Update scoreboard
        ScoreboardManager.updateScoreboard(player);
    }

    /**
     * Checks if a player can switch to a target team without creating imbalance.
     * Imbalance is defined as: switching would make the target team have more than 1 player
     * more than the other team, or the target team would become full.
     *
     * @param arenaName The arena name
     * @param currentTeam The player's current team (null if no team)
     * @param targetTeam The team the player wants to switch to
     * @return true if the switch is allowed, false otherwise
     */
    public static boolean canSwitchTeam(String arenaName, String currentTeam, String targetTeam) {
        int redCount = getTeamPlayerCount(arenaName, "red");
        int blueCount = getTeamPlayerCount(arenaName, "blue");

        // Adjust counts if player is already on a team
        if (currentTeam != null) {
            if (currentTeam.equals("red")) {
                redCount--;
            } else if (currentTeam.equals("blue")) {
                blueCount--;
            }
        }

        // Check if switching would create imbalance
        // Rule: Cannot switch if it would make the difference > 1
        if (targetTeam.equals("red")) {
            redCount++;
            return Math.abs(redCount - blueCount) <= 1;
        } else {
            blueCount++;
            return Math.abs(redCount - blueCount) <= 1;
        }
    }

    /**
     * Gets the number of players on a specific team in an arena.
     *
     * @param arenaName The arena name
     * @param team The team ("red" or "blue")
     * @return The player count
     */
    public static int getTeamPlayerCount(String arenaName, String team) {
        int count = 0;
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                String playerTeam = TeamListener.playerTeams.get(uuid);
                if (team.equals(playerTeam)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Gets the total number of players in an arena.
     *
     * @param arenaName The arena name
     * @return The total player count
     */
    public static int getTotalPlayerCount(String arenaName) {
        int count = 0;
        for (String arena : ObsidianCommand.playersInArena.values()) {
            if (arena.equals(arenaName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if both teams have at least 1 player each.
     *
     * @param arenaName The arena name
     * @return true if both teams have players, false otherwise
     */
    public static boolean bothTeamsHavePlayers(String arenaName) {
        return getTeamPlayerCount(arenaName, "red") > 0 && getTeamPlayerCount(arenaName, "blue") > 0;
    }

    /**
     * Checks if any team is empty (has 0 players).
     *
     * @param arenaName The arena name
     * @return true if at least one team is empty, false otherwise
     */
    public static boolean isAnyTeamEmpty(String arenaName) {
        return getTeamPlayerCount(arenaName, "red") == 0 || getTeamPlayerCount(arenaName, "blue") == 0;
    }

    /**
     * Removes a player from all teams and cleans up their team state.
     *
     * @param player The player to remove
     */
    public static void removePlayerFromTeam(Player player) {
        // Remove from team map
        TeamListener.playerTeams.remove(player.getUniqueId());

        // Remove from scoreboard teams
        removePlayerFromScoreboardTeams(player);

        // Remove scoreboard reference
        removePlayerScoreboard(player);
    }

    /**
     * Applies team color effects (leather armor, colored name, tablist).
     *
     * @param player The player
     * @param team The team ("red" or "blue")
     */
    private static void applyTeamColor(Player player, String team) {
        if (team.equals("red")) {
            // Name color - Red
            String coloredName = "§c" + player.getName();
            player.setDisplayName(coloredName);
            player.setPlayerListName(coloredName);

            // Leather armor - Red
            equipLeatherArmor(player, Color.RED);
        } else if (team.equals("blue")) {
            // Name color - Blue
            String coloredName = "§9" + player.getName();
            player.setDisplayName(coloredName);
            player.setPlayerListName(coloredName);

            // Leather armor - Blue
            equipLeatherArmor(player, Color.BLUE);
        }
    }

    /**
     * Equips a player with colored leather armor.
     *
     * @param player The player
     * @param color The armor color
     */
    private static void equipLeatherArmor(Player player, Color color) {
        // Leather Helmet
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        if (helmetMeta != null) {
            helmetMeta.setColor(color);
            helmet.setItemMeta(helmetMeta);
        }
        player.getInventory().setHelmet(helmet);

        // Leather Chestplate
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setColor(color);
            chestplate.setItemMeta(chestMeta);
        }
        player.getInventory().setChestplate(chestplate);

        // Leather Leggings
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        if (leggingsMeta != null) {
            leggingsMeta.setColor(color);
            leggings.setItemMeta(leggingsMeta);
        }
        player.getInventory().setLeggings(leggings);

        // Leather Boots
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.setColor(color);
            boots.setItemMeta(bootsMeta);
        }
        player.getInventory().setBoots(boots);
    }

    /**
     * Sets up the player's scoreboard team for name tag coloring.
     *
     * @param player The player
     * @param team The team ("red" or "blue")
     */
    private static void setupScoreboardTeam(Player player, String team) {
        // Get or create per-player scoreboard
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), scoreboard);
        }

        Team redTeam = scoreboard.getTeam(RED_TEAM_NAME);
        if (redTeam == null) {
            redTeam = scoreboard.registerNewTeam(RED_TEAM_NAME);
            redTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
            redTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        }

        Team blueTeam = scoreboard.getTeam(BLUE_TEAM_NAME);
        if (blueTeam == null) {
            blueTeam = scoreboard.registerNewTeam(BLUE_TEAM_NAME);
            blueTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
            blueTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        }

        // Remove player from all teams first
        redTeam.removeEntry(player.getName());
        blueTeam.removeEntry(player.getName());

        // Add to the specified team
        if (team.equals("red")) {
            redTeam.addEntry(player.getName());
        } else if (team.equals("blue")) {
            blueTeam.addEntry(player.getName());
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Removes a player from all scoreboard teams.
     *
     * @param player The player
     */
    private static void removePlayerFromScoreboardTeams(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Team redTeam = scoreboard.getTeam(RED_TEAM_NAME);
        if (redTeam != null) {
            redTeam.removeEntry(player.getName());
        }

        Team blueTeam = scoreboard.getTeam(BLUE_TEAM_NAME);
        if (blueTeam != null) {
            blueTeam.removeEntry(player.getName());
        }
    }

    /**
     * Gets a player's scoreboard.
     *
     * @param player The player
     * @return The player's scoreboard, or null if not set
     */
    public static Scoreboard getPlayerScoreboard(Player player) {
        return playerScoreboards.get(player.getUniqueId());
    }

    /**
     * Removes a player's scoreboard and resets them to the main scoreboard.
     *
     * @param player The player
     */
    private static void removePlayerScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * Cleans up all team-related data (called on plugin disable).
     */
    public static void cleanup() {
        for (Scoreboard scoreboard : playerScoreboards.values()) {
            Team redTeam = scoreboard.getTeam(RED_TEAM_NAME);
            if (redTeam != null) {
                redTeam.unregister();
            }

            Team blueTeam = scoreboard.getTeam(BLUE_TEAM_NAME);
            if (blueTeam != null) {
                blueTeam.unregister();
            }
        }
        playerScoreboards.clear();
    }
}